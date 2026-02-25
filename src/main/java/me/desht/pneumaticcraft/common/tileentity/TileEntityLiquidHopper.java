package me.desht.pneumaticcraft.common.tileentity;

import com.google.common.collect.ImmutableMap;
import me.desht.pneumaticcraft.api.item.IItemRegistry.EnumUpgrade;
import me.desht.pneumaticcraft.common.block.Blockss;
import me.desht.pneumaticcraft.common.config.ConfigHandler;
import me.desht.pneumaticcraft.common.network.DescSynced;
import me.desht.pneumaticcraft.common.network.GuiSynced;
import me.desht.pneumaticcraft.common.network.LazySynced;
import me.desht.pneumaticcraft.common.util.FluidUtils;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public class TileEntityLiquidHopper extends TileEntityOmnidirectionalHopper implements ISerializableTanks, ISmartFluidSync {
    private int comparatorValue = -1;

    @LazySynced
    @DescSynced
    @GuiSynced
    private final HopperTank tank;

    @SuppressWarnings("unused")
    @DescSynced
    private int fluidAmountScaled;

    private final WrappedFluidTank inputWrapper, outputWrapper;

    public TileEntityLiquidHopper() {
        super();

        if (ConfigHandler.machineProperties.liquidHopperDispenser) {
            this.addApplicableUpgrade(EnumUpgrade.DISPENSER);
        }
        this.tank = new HopperTank(this, PneumaticValues.NORMAL_TANK_CAPACITY);
        this.inputWrapper = new WrappedFluidTank(this.tank, true);
        this.outputWrapper = new WrappedFluidTank(this.tank, false);
    }

    @Override
    protected int getInvSize() {
        return 0;
    }

    @Override
    public String getName() {
        return Blockss.LIQUID_HOPPER.getTranslationKey();
    }

    @Override
    protected int getComparatorValueInternal() {
        if (this.comparatorValue < 0) {
            if (this.tank.getFluidAmount() == 0) return 0;
            FluidStack fluidStack = this.tank.getFluid();
            this.comparatorValue = (int) (1 + ((float) fluidStack.amount / this.tank.getCapacity() * 14f));
        }
        return this.comparatorValue;
    }

    @Override
    protected boolean doExport(int maxItems) {
        EnumFacing dir = this.getRotation();

        if (this.tank.getFluid() != null) {
            TileEntity neighbor = this.getCachedNeighbor(dir);
            if (neighbor != null && neighbor.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, dir.getOpposite())) {
                IFluidHandler fluidHandler = neighbor.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, dir.getOpposite());
                int amount = Math.min(maxItems * 100, this.tank.getFluid().amount - this.leaveMaterialCount * 1000);
//                FluidStack transferred;
//                if (isCreative) {
//                    transferred = FluidUtil.tryFluidTransfer(fluidHandler, tank, amount, false);
//                    if (transferred != null) fluidHandler.fill(transferred, true);
//                } else {
//                }
                FluidStack transferred = FluidUtil.tryFluidTransfer(fluidHandler, this.tank, amount, true);
                return transferred != null && transferred.amount > 0;
            }
        }

        if (this.getWorld().isAirBlock(this.getPos().offset(dir))) {
            for (EntityItem entity : getNeighborItems(this, dir)) {
                NonNullList<ItemStack> returnedItems = NonNullList.create();
                if (FluidUtils.tryFluidExtraction(this.tank, entity.getItem(), returnedItems)) {
                    if (entity.getItem().getCount() <= 0) entity.setDead();
                    for (ItemStack stack : returnedItems) {
                        EntityItem item = new EntityItem(this.getWorld(), entity.posX, entity.posY, entity.posZ, stack);
                        item.motionX = entity.motionX;
                        item.motionY = entity.motionY;
                        item.motionZ = entity.motionZ;
                        this.getWorld().spawnEntity(item);
                    }
                    return true;
                }
            }
        }

        if (ConfigHandler.machineProperties.liquidHopperDispenser && this.getUpgrades(EnumUpgrade.DISPENSER) > 0) {
            if (this.getWorld().isAirBlock(this.getPos().offset(dir))) {
                FluidStack extractedFluid = this.tank.drain(1000, false);
                if (extractedFluid != null && extractedFluid.amount == 1000) {
                    Block fluidBlock = extractedFluid.getFluid().getBlock();
                    if (fluidBlock != null) {
                        this.tank.drain(1000, true);
                        this.getWorld().setBlockState(this.getPos().offset(dir), fluidBlock.getDefaultState());
                    }
                }
            }
        }

        return false;
    }

    @Override
    protected boolean doImport(int maxItems) {
        TileEntity inputInv = this.getCachedNeighbor(this.inputDir);

        if (inputInv != null && inputInv.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, this.inputDir.getOpposite())) {
            IFluidHandler fluidHandler = inputInv.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, this.inputDir.getOpposite());
            FluidStack fluid = fluidHandler.drain(maxItems * 100, false);
            if (fluid != null) {
                int filledFluid = this.tank.fill(fluid, true);
                if (filledFluid > 0) {
                    fluidHandler.drain(filledFluid, true);
                    return true;
                }
            }
        }

        if (this.getWorld().isAirBlock(this.getPos().offset(this.inputDir))) {
            for (EntityItem entity : getNeighborItems(this, this.inputDir)) {
                NonNullList<ItemStack> returnedItems = NonNullList.create();
                if (FluidUtils.tryFluidInsertion(this.tank, entity.getItem(), returnedItems)) {
                    if (entity.getItem().isEmpty()) entity.setDead();
                    for (ItemStack stack : returnedItems) {
                        EntityItem item = new EntityItem(this.getWorld(), entity.posX, entity.posY, entity.posZ, stack);
                        item.motionX = entity.motionX;
                        item.motionY = entity.motionY;
                        item.motionZ = entity.motionZ;
                        this.getWorld().spawnEntity(item);
                    }
                    return true;
                }
            }
        }

        if (ConfigHandler.machineProperties.liquidHopperDispenser && this.getUpgrades(EnumUpgrade.DISPENSER) > 0) {
            BlockPos neighborPos = this.getPos().offset(this.inputDir);
            FluidStack fluidStack = FluidUtils.getFluidAt(this.getWorld(), neighborPos, false);
            if (fluidStack != null && fluidStack.amount == Fluid.BUCKET_VOLUME) {
                if (this.tank.fill(fluidStack, false) == Fluid.BUCKET_VOLUME) {
                    this.tank.fill(fluidStack, true);
                    FluidUtils.getFluidAt(this.getWorld(), neighborPos, true);
                    return true;
                }
            }
        }

        return false;
    }

    public FluidTank getTank() {
        return this.tank;
    }

    public EnumFacing getInputDirection() {
        return this.inputDir;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);

        NBTTagCompound tankTag = new NBTTagCompound();
        this.tank.writeToNBT(tankTag);
        tag.setTag("tank", tankTag);

        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.tank.readFromNBT(tag.getCompoundTag("tank"));
        this.fluidAmountScaled = this.tank.getScaledFluidAmount();
        this.comparatorValue = -1;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            if (facing == this.inputDir) {
                return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this.inputWrapper);
            } else if (facing == this.getRotation()) {
                return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this.outputWrapper);
            } else {
                return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this.tank);
            }
        } else {
            return super.getCapability(capability, facing);
        }
    }

    @Override
    public IItemHandlerModifiable getPrimaryInventory() {
        return null;
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

    @Override
    protected void onUpgradesChanged() {
        super.onUpgradesChanged();

        if (this.world != null && !this.world.isRemote && this.getUpgrades(EnumUpgrade.CREATIVE) > 0) {
            FluidStack fluidStack = this.tank.getFluid();
            if (fluidStack != null && fluidStack.amount > 0) {
                this.tank.setFluid(new FluidStack(fluidStack.getFluid(), PneumaticValues.NORMAL_TANK_CAPACITY));
            }
        }
    }

    class HopperTank extends SmartSyncTank {
        HopperTank(ISmartFluidSync holder, int capacity) {
            super(holder, capacity);
        }

        @Override
        protected void onContentsChanged() {
            super.onContentsChanged();
            TileEntityLiquidHopper.this.comparatorValue = -1;
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            int filled = super.fill(resource, doFill);
            if (TileEntityLiquidHopper.this.isCreative && this.getFluidAmount() > 0 && this.getFluid().getFluid() == resource.getFluid()) {
                return resource.amount;   // acts like an infinite fluid sink
            } else {
                return filled;
            }
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            return super.drain(resource, !TileEntityLiquidHopper.this.isCreative && doDrain);
        }

        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            return super.drain(maxDrain, !TileEntityLiquidHopper.this.isCreative && doDrain);
        }
    }

    class WrappedFluidTank implements IFluidTank, IFluidHandler {
        private final FluidTank wrappedTank;
        private final boolean inbound;

        WrappedFluidTank(FluidTank wrappedTank, boolean inbound) {
            // inbound == true: fill *only*, inbound == false: drain *only*
            this.wrappedTank = wrappedTank;
            this.inbound = inbound;
        }

        @Nullable
        @Override
        public FluidStack getFluid() {
            return this.wrappedTank.getFluid();
        }

        @Override
        public int getFluidAmount() {
            return this.wrappedTank.getFluidAmount();
        }

        @Override
        public int getCapacity() {
            return this.wrappedTank.getCapacity();
        }

        @Override
        public FluidTankInfo getInfo() {
            return this.wrappedTank.getInfo();
        }

        @Override
        public IFluidTankProperties[] getTankProperties() {
            return this.wrappedTank.getTankProperties();
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            return this.inbound ? this.wrappedTank.fill(resource, doFill) : 0;
        }

        @Nullable
        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            return this.inbound ? null : TileEntityLiquidHopper.this.tank.drain(resource, doDrain);
        }

        @Nullable
        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            return this.inbound ? null : this.wrappedTank.drain(maxDrain, doDrain);
        }
    }
}
