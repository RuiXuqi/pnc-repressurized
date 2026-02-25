package me.desht.pneumaticcraft.common.tileentity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import me.desht.pneumaticcraft.common.PneumaticCraftAPIHandler;
import me.desht.pneumaticcraft.common.block.Blockss;
import me.desht.pneumaticcraft.common.config.ConfigHandler;
import me.desht.pneumaticcraft.common.fluid.Fluids;
import me.desht.pneumaticcraft.common.inventory.handler.BaseItemStackHandler;
import me.desht.pneumaticcraft.common.network.DescSynced;
import me.desht.pneumaticcraft.common.network.GuiSynced;
import me.desht.pneumaticcraft.common.network.LazySynced;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumSkyBlock;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class TileEntityKeroseneLamp extends TileEntityTickableBase implements IRedstoneControlled, ISerializableTanks, ISmartFluidSync {

    private static final List<String> REDSTONE_LABELS = ImmutableList.of(
            "gui.tab.redstoneBehaviour.button.anySignal",
            "gui.tab.redstoneBehaviour.button.highSignal",
            "gui.tab.redstoneBehaviour.button.lowSignal",
            "gui.tab.redstoneBehaviour.keroseneLamp.button.interpolate"
    );

    public static final int INVENTORY_SIZE = 2;

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;

    private final Set<BlockPos> managingLights = new HashSet<>();
    @DescSynced
    private boolean isOn;
    @GuiSynced
    private int range;
    @GuiSynced
    private int targetRange = 10;
    @GuiSynced
    private int redstoneMode;
    @GuiSynced
    private int fuel;
    private static final int LIGHT_SPACING = 3;
    public static final int MAX_RANGE = 30;
    private int checkingX, checkingY, checkingZ;
    @DescSynced
    private EnumFacing sideConnected = EnumFacing.DOWN;
    @LazySynced
    @DescSynced
    @GuiSynced
    private final SmartSyncTank tank = new SmartSyncTank(this, 2000) {
        private FluidStack prevFluid;

        @Override
        protected void onContentsChanged() {
            super.onContentsChanged();
            if (this.prevFluid == null && this.fluid != null || this.prevFluid != null && this.fluid == null) {
                TileEntityKeroseneLamp.this.recalculateFuelQuality();
            }
            this.prevFluid = this.fluid;
        }
    };
    @SuppressWarnings("unused")
    @DescSynced
    private int fluidAmountScaled;  // sync the lazy tank in a network-friendly way
    @DescSynced
    private float fuelQuality = -1f; // the quality of the liquid currently in the tank; basically, its burn time

    private final ItemStackHandler inventory = new BaseItemStackHandler(this, INVENTORY_SIZE) {
        @Override
        public boolean isItemValid(int slot, ItemStack itemStack) {
            return itemStack.isEmpty() || FluidUtil.getFluidHandler(itemStack) != null;
        }
    };

    @Override
    public IItemHandlerModifiable getPrimaryInventory() {
        return this.inventory;
    }

    @Override
    public void update() {
        super.update();
        if (!this.getWorld().isRemote) {
            if (this.fuelQuality < 0) this.recalculateFuelQuality();
            this.processFluidItem(INPUT_SLOT, OUTPUT_SLOT);
            if (this.getWorld().getTotalWorldTime() % 5 == 0) {
                int realTargetRange = this.redstoneAllows() && this.fuel > 0 ? this.targetRange : 0;
                if (this.redstoneMode == 3) realTargetRange = (int) (this.poweredRedstone / 15D * this.targetRange);
                this.updateRange(Math.min(realTargetRange, this.tank.getFluidAmount())); //Fade out the lamp when almost empty.
                this.updateLights();
                this.useFuel();
            }
        } else {
            if (this.isOn && this.getWorld().getTotalWorldTime() % 5 == 0) {
                this.getWorld().spawnParticle(EnumParticleTypes.FLAME, this.getPos().getX() + 0.4 + 0.2 * this.getWorld().rand.nextDouble(), this.getPos().getY() + 0.2 + this.tank.getFluidAmount() / 1000D * 3 / 16D, this.getPos().getZ() + 0.4 + 0.2 * this.getWorld().rand.nextDouble(), 0, 0, 0);
            }
        }
    }

    private void recalculateFuelQuality() {
        if (this.tank.getFluid() != null && this.tank.getFluid().amount > 0) {
            if (ConfigHandler.machineProperties.keroseneLampCanUseAnyFuel) {
                Fluid f = this.tank.getFluid().getFluid();
                // 110 comes from kerosene's fuel value of 1,100,000 divided by the old FUEL_PER_MB value (10000)
                this.fuelQuality = PneumaticCraftAPIHandler.getInstance().liquidFuels.getOrDefault(f.getName(), 0) / 110f;
            } else {
                this.fuelQuality = Fluids.areFluidsEqual(this.tank.getFluid().getFluid(), Fluids.KEROSENE) ? 10000f : 0f;
            }
            this.fuelQuality *= ConfigHandler.machineProperties.keroseneLampFuelEfficiency;
        }
    }

    private void useFuel() {
        if (this.fuelQuality == 0) return; // tank is empty or a non-burnable liquid in the tank
        this.fuel -= this.range * this.range * this.range;
        while (this.fuel <= 0 && this.tank.drain(1, true) != null) {
            this.fuel += this.fuelQuality;
        }
        if (this.fuel < 0) this.fuel = 0;
    }

    @Override
    public void validate() {
        super.validate();
        this.checkingX = this.getPos().getX();
        this.checkingY = this.getPos().getY();
        this.checkingZ = this.getPos().getZ();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        for (BlockPos pos : this.managingLights) {
            if (this.isLampLight(pos)) {
                this.getWorld().setBlockToAir(pos);
            }
        }
    }

    private boolean isLampLight(BlockPos pos) {
        return this.getWorld().getBlockState(pos).getBlock() == Blockss.KEROSENE_LAMP_LIGHT;
    }

    private void updateLights() {
        int roundedRange = this.range / LIGHT_SPACING * LIGHT_SPACING;
        this.checkingX += LIGHT_SPACING;
        if (this.checkingX > this.getPos().getX() + roundedRange) {
            this.checkingX = this.getPos().getX() - roundedRange;
            this.checkingY += LIGHT_SPACING;
            if (this.checkingY > this.getPos().getY() + roundedRange) {
                this.checkingY = this.getPos().getY() - roundedRange;
                this.checkingZ += LIGHT_SPACING;
                if (this.checkingZ > this.getPos().getZ() + roundedRange) this.checkingZ = this.getPos().getZ() - roundedRange;
            }
        }
        BlockPos pos = new BlockPos(this.checkingX, this.checkingY, this.checkingZ);
        BlockPos lampPos = new BlockPos(this.getPos().getX(), this.getPos().getY(), this.getPos().getZ());
        if (this.managingLights.contains(pos)) {
            if (this.isLampLight(pos)) {
                if (!this.passesRaytraceTest(pos, lampPos)) {
                    this.getWorld().setBlockToAir(pos);
                    this.managingLights.remove(pos);
                }
            } else {
                this.managingLights.remove(pos);
            }
        } else {
            this.tryAddLight(pos, lampPos);
        }
    }

    private void updateRange(int targetRange) {
        if (targetRange > this.range) {
            this.range++;
            BlockPos lampPos = new BlockPos(this.getPos().getX(), this.getPos().getY(), this.getPos().getZ());
            int roundedRange = this.range / LIGHT_SPACING * LIGHT_SPACING;
            for (int x = -roundedRange; x <= roundedRange; x += LIGHT_SPACING) {
                for (int y = -roundedRange; y <= roundedRange; y += LIGHT_SPACING) {
                    for (int z = -roundedRange; z <= roundedRange; z += LIGHT_SPACING) {
                        BlockPos pos = new BlockPos(x + this.getPos().getX(), y + this.getPos().getY(), z + this.getPos().getZ());
                        if (!this.managingLights.contains(pos)) {
                            this.tryAddLight(pos, lampPos);
                        }
                    }
                }
            }
        } else if (targetRange < this.range) {
            this.range--;
            Iterator<BlockPos> iterator = this.managingLights.iterator();
            BlockPos lampPos = new BlockPos(this.getPos().getX(), this.getPos().getY(), this.getPos().getZ());
            while (iterator.hasNext()) {
                BlockPos pos = iterator.next();
                if (!this.isLampLight(pos)) {
                    iterator.remove();
                } else if (PneumaticCraftUtils.distBetween(pos, lampPos) > this.range) {
                    this.getWorld().setBlockToAir(pos);
                    iterator.remove();
                }
            }
        }
        boolean oldIsOn = this.isOn;
        this.isOn = this.range > 0;
        if (this.isOn != oldIsOn) {
            this.getWorld().checkLightFor(EnumSkyBlock.BLOCK, this.getPos());
            this.sendDescriptionPacket();
        }
    }

    public boolean isOn() {
        return this.isOn;
    }

    private boolean passesRaytraceTest(BlockPos pos, BlockPos lampPos) {
        RayTraceResult mop = this.getWorld().rayTraceBlocks(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), new Vec3d(lampPos.getX() + 0.5, lampPos.getY() + 0.5, lampPos.getZ() + 0.5));
        return mop != null && lampPos.equals(mop.getBlockPos());
    }

    private void tryAddLight(BlockPos pos, BlockPos lampPos) {
        if (PneumaticCraftUtils.distBetween(pos, lampPos) <= this.range) {
            if (this.getWorld().isAirBlock(pos) && !this.isLampLight(pos)) {
                if (this.passesRaytraceTest(pos, lampPos)) {
                    this.getWorld().setBlockState(pos, Blockss.KEROSENE_LAMP_LIGHT.getDefaultState());
                    this.managingLights.add(pos);
                }
            }
        }
    }

    @Override
    public void onNeighborBlockUpdate() {
        super.onNeighborBlockUpdate();
        EnumFacing oldSideConnected = this.sideConnected;
        this.sideConnected = EnumFacing.DOWN;
        for (EnumFacing d : EnumFacing.VALUES) {
            BlockPos neighborPos = this.getPos().offset(d);
            IBlockState state = this.getWorld().getBlockState(neighborPos);
            if (state.isSideSolid(this.getWorld(), neighborPos, d.getOpposite())) {
                this.sideConnected = d;
                break;
            }
        }
        if (this.sideConnected != oldSideConnected) {
            this.sendDescriptionPacket();
        }
    }

    @Override
    public void onDescUpdate() {
        this.getWorld().checkLightFor(EnumSkyBlock.BLOCK, this.getPos());
        this.getWorld().markBlockRangeForRenderUpdate(this.getPos(), this.getPos());
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        NBTTagList lights = new NBTTagList();
        for (BlockPos pos : this.managingLights) {
            NBTTagCompound t = new NBTTagCompound();
            t.setInteger("x", pos.getX());
            t.setInteger("y", pos.getY());
            t.setInteger("z", pos.getZ());
            lights.appendTag(t);
        }
        tag.setTag("lights", lights);

        NBTTagCompound tankTag = new NBTTagCompound();
        this.tank.writeToNBT(tankTag);
        tag.setTag("tank", tankTag);
        tag.setByte("redstoneMode", (byte) this.redstoneMode);
        tag.setByte("targetRange", (byte) this.targetRange);
        tag.setByte("range", (byte) this.range);
        tag.setByte("sideConnected", (byte) this.sideConnected.ordinal());
        tag.setTag("Items", this.inventory.serializeNBT());
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.managingLights.clear();
        NBTTagList lights = tag.getTagList("lights", 10);
        for (int i = 0; i < lights.tagCount(); i++) {
            NBTTagCompound t = lights.getCompoundTagAt(i);
            this.managingLights.add(new BlockPos(t.getInteger("x"), t.getInteger("y"), t.getInteger("z")));
        }
        this.tank.readFromNBT(tag.getCompoundTag("tank"));
        this.fluidAmountScaled = this.tank.getScaledFluidAmount();
        this.recalculateFuelQuality();
        this.redstoneMode = tag.getByte("redstoneMode");
        this.targetRange = tag.getByte("targetRange");
        this.range = tag.getByte("range");
        this.sideConnected = EnumFacing.byIndex(tag.getByte("sideConnected"));
        this.inventory.deserializeNBT(tag.getCompoundTag("Items"));
    }

    @Override
    public boolean redstoneAllows() {
        return this.redstoneMode == 3 || super.redstoneAllows();
    }

    @Override
    public int getRedstoneMode() {
        return this.redstoneMode;
    }

    @Override
    public void handleGUIButtonPress(int buttonID, EntityPlayer player) {
        if (buttonID == 0) {
            this.redstoneMode++;
            if (this.redstoneMode > 3) this.redstoneMode = 0;
        } else if (buttonID > 0 && buttonID <= MAX_RANGE) {
            this.targetRange = buttonID;
        }
    }

    public FluidTank getTank() {
        return this.tank;
    }

    public int getRange() {
        return this.range;
    }

    public int getTargetRange() {
        return this.targetRange;
    }

    public int getFuel() {
        return this.fuel;
    }

    public EnumFacing getSideConnected() {
        return this.sideConnected;
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

    /**
     * Returns the name of the inventory.
     */
    @Override
    public String getName() {
        return Blockss.KEROSENE_LAMP.getTranslationKey();
    }

    public float getFuelQuality() {
        return this.fuelQuality;
    }

    @Override
    protected List<String> getRedstoneButtonLabels() {
        return REDSTONE_LABELS;
    }

    @Nonnull
    @Override
    public Map<String, FluidTank> getSerializableTanks() {
        return ImmutableMap.of("Tank", this.tank);
    }

    @Override
    public void updateScaledFluidAmount(int tankIndex, int amount) {
        this.fluidAmountScaled = amount;
    }
}
