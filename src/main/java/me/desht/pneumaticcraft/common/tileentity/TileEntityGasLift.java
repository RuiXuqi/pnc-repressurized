package me.desht.pneumaticcraft.common.tileentity;

import com.google.common.collect.ImmutableMap;
import me.desht.pneumaticcraft.api.item.IItemRegistry.EnumUpgrade;
import me.desht.pneumaticcraft.api.tileentity.IAirHandler;
import me.desht.pneumaticcraft.common.ai.ChunkPositionSorter;
import me.desht.pneumaticcraft.common.block.BlockPressureTube;
import me.desht.pneumaticcraft.common.block.Blockss;
import me.desht.pneumaticcraft.common.inventory.handler.BaseItemStackHandler;
import me.desht.pneumaticcraft.common.network.DescSynced;
import me.desht.pneumaticcraft.common.network.GuiSynced;
import me.desht.pneumaticcraft.common.util.FluidUtils;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class TileEntityGasLift extends TileEntityPneumaticBase
        implements IMinWorkingPressure, IRedstoneControlled, ISerializableTanks, IAutoFluidEjecting {
    private static final int INVENTORY_SIZE = 1;

    public enum Status {
        IDLE("idling"), PUMPING("pumping"), DIGGING("diggingDown"), RETRACTING("retracting"), STUCK("stuck");
        public final String desc;

        Status(String desc) {
            this.desc = desc;
        }
    }

    @GuiSynced
    private final FluidTank tank = new FluidTank(PneumaticValues.NORMAL_TANK_CAPACITY);
    private final ItemStackHandler inventory = new BaseItemStackHandler(this, INVENTORY_SIZE) {
        @Override
        public boolean isItemValid(int slot, ItemStack itemStack) {
            return itemStack.isEmpty()
                    || itemStack.getItem() instanceof ItemBlock && ((ItemBlock) itemStack.getItem()).getBlock() instanceof BlockPressureTube;
        }
    };
    @GuiSynced
    public int currentDepth;
    @GuiSynced
    public int redstoneMode, mode;
    @GuiSynced
    public Status status = Status.IDLE;
    @DescSynced
    public final boolean[] sidesConnected = new boolean[6];
    private int workTimer;
    private int ticker;
    private List<BlockPos> pumpingLake;
    private static final int MAX_PUMP_RANGE_SQUARED = 15 * 15;

    public TileEntityGasLift() {
        super(5, 7, 3000, 4);
        this.addApplicableUpgrade(EnumUpgrade.SPEED, EnumUpgrade.DISPENSER);
    }

    @Override
    public boolean isConnectedTo(EnumFacing d) {
        return d != EnumFacing.DOWN;
    }

    private void updateConnections() {
        List<Pair<EnumFacing, IAirHandler>> connections = this.getAirHandler(null).getConnectedPneumatics();
        Arrays.fill(this.sidesConnected, false);
        for (Pair<EnumFacing, IAirHandler> entry : connections) {
            this.sidesConnected[entry.getKey().ordinal()] = true;
        }
    }

    @Override
    public void onNeighborBlockUpdate() {
        super.onNeighborBlockUpdate();
        this.updateConnections();
    }

    @Override
    public void onNeighborTileUpdate() {
        super.onNeighborTileUpdate();
        this.updateConnections();
    }

    @Override
    protected boolean shouldRerenderChunkOnDescUpdate() {
        return true;
    }

    @Override
    public void update() {
        super.update();
        if (!this.getWorld().isRemote) {
            this.ticker++;
            if (this.currentDepth > 0) {
                int curCheckingPipe = this.ticker % this.currentDepth;
                if (curCheckingPipe > 0 && !this.isPipe(this.world, this.getPos().offset(EnumFacing.DOWN, curCheckingPipe))) {
                    this.currentDepth = curCheckingPipe - 1;
                }
            }
            if (this.ticker == 400) {
                this.pumpingLake = null;
                this.ticker = 0;
            }

            if (this.redstoneAllows() && this.getPressure() >= this.getMinWorkingPressure()) {
                this.workTimer += this.getSpeedMultiplierFromUpgrades();
                while (this.workTimer > 20) {
                    this.workTimer -= 20;
                    this.status = Status.IDLE;
                    if (this.mode == 2) {
                        this.retractPipes();
                    } else {
                        if (!this.suckLiquid() && !this.tryDigDown()) {
                            break;
                        }
                    }
                }
            } else {
                this.status = Status.IDLE;
            }
        }
    }

    private void retractPipes() {
        if (this.currentDepth > 0) {
            this.status = Status.RETRACTING;
            if (this.isPipe(this.world, this.getPos().add(0, -this.currentDepth, 0))) {
                BlockPos pos1 = this.getPos().offset(EnumFacing.DOWN, this.currentDepth);
                ItemStack toInsert = new ItemStack(this.world.getBlockState(pos1).getBlock());
                if (this.inventory.insertItem(0, toInsert, true).isEmpty()) {
                    this.inventory.insertItem(0, toInsert, false);
                    this.world.destroyBlock(pos1, false);
                    this.addAir(-100);
                    this.currentDepth--;
                } else {
                    this.status = Status.IDLE;
                }
            } else {
                this.currentDepth--;
            }
        }
    }

    private boolean tryDigDown() {
        if (this.isUnbreakable(this.getPos().offset(EnumFacing.DOWN, this.currentDepth + 1))) {
            this.status = Status.STUCK;
        } else if (this.getPos().getY() - this.currentDepth >= 0) {
            this.status = Status.DIGGING;
            this.currentDepth++;
            BlockPos pos1 = this.getPos().offset(EnumFacing.DOWN, this.currentDepth);
            if (!this.isPipe(this.world, pos1)) {
                ItemStack extracted = this.inventory.extractItem(0, 1, true);
                if (extracted.getItem() instanceof ItemBlock) {
                    IBlockState currentState = this.world.getBlockState(pos1);
                    IBlockState newState = ((ItemBlock) extracted.getItem()).getBlock().getDefaultState();

                    int airRequired = Math.round(66.66f * currentState.getBlockHardness(this.world, pos1));
                    if (this.getPipeTier(newState) > 1) airRequired /= 2;

                    if (this.getAirHandler(null).getAir() > airRequired) {
                        this.inventory.extractItem(0, 1, false);
                        this.world.destroyBlock(pos1, false);
                        this.world.setBlockState(pos1, newState);
                        // kludge: don't permit placing more than one tube per tick
                        // causes TE cache problems - root cause to be determined
                        this.workTimer = 19;
                        this.addAir(-airRequired);
                    } else {
                        this.status = Status.IDLE;
                        this.currentDepth--;
                    }
                } else {
                    this.status = Status.IDLE;
                    this.currentDepth--;
                }
            }
        } else {
            this.status = Status.IDLE;
        }
        return this.status == Status.DIGGING;
    }

    private boolean isPipe(World world, BlockPos pos) {
        return this.getPipeTier(world.getBlockState(pos)) >= 1;
    }

    private int getPipeTier(IBlockState state) {
        Block b = state.getBlock();
        return b instanceof BlockPressureTube ? ((BlockPressureTube) b).getTier() : 0;
    }

    private boolean isUnbreakable(BlockPos pos) {
        return this.world().getBlockState(pos).getBlockHardness(this.world, pos) < 0;
    }


    private boolean suckLiquid() {
        BlockPos pos = this.getPos().offset(EnumFacing.DOWN, this.currentDepth + 1);

        FluidStack fluidStack = FluidUtils.getFluidAt(this.world, pos, false);
        if (fluidStack == null || fluidStack.amount < Fluid.BUCKET_VOLUME) {
            this.pumpingLake = null;
            return false;
        }

        if (this.tank.fill(fluidStack, false) == Fluid.BUCKET_VOLUME) {
            if (this.pumpingLake == null) {
                this.findLake(fluidStack.getFluid());
            }
            boolean foundSource = false;
            BlockPos curPos = null;
            while (this.pumpingLake.size() > 0) {
                curPos = this.pumpingLake.get(0);
                if (FluidUtils.isSourceBlock(this.getWorld(), curPos, fluidStack.getFluid())) {
                    foundSource = true;
                    break;
                }
                this.pumpingLake.remove(0);
            }
            if (this.pumpingLake.isEmpty()) {
                this.pumpingLake = null;
            } else if (foundSource) {
                FluidStack fluidStack1 = FluidUtils.getFluidAt(this.world, curPos, true);
                if (fluidStack1 != null && fluidStack1.amount == Fluid.BUCKET_VOLUME) {
                    this.tank.fill(fluidStack1, true);
                    this.addAir(-100);
                    this.status = Status.PUMPING;
                }
            }
        }
        return true;
    }

    private void findLake(Fluid fluid) {
        this.pumpingLake = new ArrayList<>();
        Stack<BlockPos> pendingPositions = new Stack<>();
        BlockPos thisPos = this.getPos().offset(EnumFacing.DOWN, this.currentDepth + 1);
        pendingPositions.add(thisPos);
        this.pumpingLake.add(thisPos);
        while (!pendingPositions.empty()) {
            BlockPos checkingPos = pendingPositions.pop();
            for (EnumFacing d : EnumFacing.VALUES) {
                if (d == EnumFacing.DOWN) continue;
                BlockPos newPos = checkingPos.offset(d);
                if (PneumaticCraftUtils.distBetweenSq(newPos, thisPos) <= MAX_PUMP_RANGE_SQUARED
                        && FluidUtils.isSourceBlock(this.getWorld(), newPos, fluid)
                        && !this.pumpingLake.contains(newPos)) {
                    pendingPositions.add(newPos);
                    this.pumpingLake.add(newPos);
                }
            }
        }
        this.pumpingLake.sort(new ChunkPositionSorter(this.getPos().getX() + 0.5, this.getPos().getY() - this.currentDepth - 1, this.getPos().getZ() + 0.5));
        Collections.reverse(this.pumpingLake);
    }

    @Override
    public void handleGUIButtonPress(int buttonID, EntityPlayer player) {
        if (buttonID == 0) {
            this.redstoneMode++;
            if (this.redstoneMode > 2) this.redstoneMode = 0;
        } else if (buttonID > 0 && buttonID < 4) {
            this.mode = buttonID - 1;
        }
    }

    @Override
    public int getRedstoneMode() {
        return this.redstoneMode;
    }

    @Override
    public float getMinWorkingPressure() {
        return 0.5F + this.currentDepth * 0.05F;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setTag("Items", this.inventory.serializeNBT());
        tag.setByte("redstoneMode", (byte) this.redstoneMode);
        tag.setByte("mode", (byte) this.mode);

        NBTTagCompound tankTag = new NBTTagCompound();
        this.tank.writeToNBT(tankTag);
        tag.setTag("tank", tankTag);
        tag.setInteger("currentDepth", this.currentDepth);

        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.inventory.deserializeNBT(tag.getCompoundTag("Items"));
        this.redstoneMode = tag.getByte("redstoneMode");
        this.mode = tag.getByte("mode");
        this.tank.readFromNBT(tag.getCompoundTag("tank"));
        this.currentDepth = tag.getInteger("currentDepth");
    }

    @Override
    public IItemHandlerModifiable getPrimaryInventory() {
        return this.inventory;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this.tank);
        } else {
            return super.getCapability(capability, facing);
        }
    }

    public FluidTank getTank() {
        return this.tank;
    }

    /**
     * Returns the name of the inventory.
     */
    @Override
    public String getName() {
        return Blockss.GAS_LIFT.getTranslationKey();
    }

    @Nonnull
    @Override
    public Map<String, FluidTank> getSerializableTanks() {
        return ImmutableMap.of("Tank", this.tank);
    }
}
