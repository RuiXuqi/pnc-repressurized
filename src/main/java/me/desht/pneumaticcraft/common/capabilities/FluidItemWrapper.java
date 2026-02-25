package me.desht.pneumaticcraft.common.capabilities;

import me.desht.pneumaticcraft.common.tileentity.ISerializableTanks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FluidItemWrapper implements ICapabilityProvider {
    private static final IFluidTankProperties[] EMPTY = new IFluidTankProperties[0];
    private final ItemStack stack;
    private final String tankName;
    private final int capacity;

    public FluidItemWrapper(ItemStack stack, String tankName, int capacity) {
        this.stack = stack;
        this.tankName = tankName;
        this.capacity = capacity;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY;
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (!this.hasCapability(capability, facing)) {
            return null;
        }
        return CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY.cast(new IFluidHandlerItem() {
            @Nonnull
            @Override
            public ItemStack getContainer() {
                return FluidItemWrapper.this.stack;
            }

            @Override
            public IFluidTankProperties[] getTankProperties() {
                FluidTank tank = ISerializableTanks.deserializeTank(FluidItemWrapper.this.stack, FluidItemWrapper.this.tankName, FluidItemWrapper.this.capacity);
                return tank == null ? EMPTY : tank.getTankProperties();
            }

            @Override
            public int fill(FluidStack resource, boolean doFill) {
                FluidTank tank = ISerializableTanks.deserializeTank(FluidItemWrapper.this.stack, FluidItemWrapper.this.tankName, FluidItemWrapper.this.capacity);
                if (tank == null) return 0;
                int filled = tank.fill(resource, doFill);
                if (filled > 0 && doFill) {
                    ISerializableTanks.serializeTank(tank, FluidItemWrapper.this.stack, FluidItemWrapper.this.tankName);
                }
                return filled;
            }

            @Nullable
            @Override
            public FluidStack drain(FluidStack resource, boolean doDrain) {
                FluidTank tank = ISerializableTanks.deserializeTank(FluidItemWrapper.this.stack, FluidItemWrapper.this.tankName, FluidItemWrapper.this.capacity);
                if (tank == null) return null;
                FluidStack drained = tank.drain(resource, doDrain);
                if (drained != null && drained.amount > 0 && doDrain) {
                    ISerializableTanks.serializeTank(tank, FluidItemWrapper.this.stack, FluidItemWrapper.this.tankName);
                }
                return drained;
            }

            @Nullable
            @Override
            public FluidStack drain(int maxDrain, boolean doDrain) {
                FluidTank tank = ISerializableTanks.deserializeTank(FluidItemWrapper.this.stack, FluidItemWrapper.this.tankName, FluidItemWrapper.this.capacity);
                if (tank == null) return null;
                FluidStack drained = tank.drain(maxDrain, doDrain);
                if (drained != null && drained.amount > 0 && doDrain) {
                    ISerializableTanks.serializeTank(tank, FluidItemWrapper.this.stack, FluidItemWrapper.this.tankName);
                }
                return drained;
            }
        });
    }
}
