package me.desht.pneumaticcraft.common.ai;

import me.desht.pneumaticcraft.common.progwidgets.ICountWidget;
import me.desht.pneumaticcraft.common.progwidgets.ILiquidFiltered;
import me.desht.pneumaticcraft.common.progwidgets.ISidedWidget;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetAreaItemBase;
import me.desht.pneumaticcraft.common.util.FluidUtils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class DroneAILiquidImport extends DroneAIImExBase {

    public DroneAILiquidImport(IDroneBase drone, ProgWidgetAreaItemBase widget) {
        super(drone, widget);
    }

    @Override
    protected boolean isValidPosition(BlockPos pos) {
        return this.emptyTank(pos, true);
    }

    @Override
    protected boolean doBlockInteraction(BlockPos pos, double distToBlock) {
        return this.emptyTank(pos, false) && super.doBlockInteraction(pos, distToBlock);
    }

    private boolean emptyTank(BlockPos pos, boolean simulate) {
        if (this.drone.getTank().getFluidAmount() == this.drone.getTank().getCapacity()) {
            this.drone.addDebugEntry("gui.progWidget.liquidImport.debug.fullDroneTank");
            this.abort();
            return false;
        } else {
            TileEntity te = this.drone.world().getTileEntity(pos);
            if (te != null) {
                for (int i = 0; i < 6; i++) {
                    if (((ISidedWidget) this.widget).getSides()[i] && te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.byIndex(i))) {
                        IFluidHandler handler = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.byIndex(i));
                        FluidStack importedFluid = handler.drain(Integer.MAX_VALUE, false);
                        if (importedFluid != null && ((ILiquidFiltered) this.widget).isFluidValid(importedFluid.getFluid())) {
                            int filledAmount = this.drone.getTank().fill(importedFluid, false);
                            if (filledAmount > 0) {
                                if (((ICountWidget) this.widget).useCount())
                                    filledAmount = Math.min(filledAmount, this.getRemainingCount());
                                if (!simulate) {
                                    this.decreaseCount(this.drone.getTank().fill(handler.drain(filledAmount, true), true));
                                }
                                return true;
                            }
                        }
                    }
                }
                this.drone.addDebugEntry("gui.progWidget.liquidImport.debug.emptiedToMax", pos);
            }

            // fall through to fluid-in-world check here; it's possible for a fluid block to be a TE (with no
            // fluid capability) and also a fluid block which can be drained directly
            if (!((ICountWidget) this.widget).useCount() || this.getRemainingCount() >= Fluid.BUCKET_VOLUME) {
                FluidStack fluidStack = FluidUtils.getFluidAt(this.drone.world(), pos, false);
                if (fluidStack != null && fluidStack.amount == Fluid.BUCKET_VOLUME
                        && ((ILiquidFiltered) this.widget).isFluidValid(fluidStack.getFluid())
                        && this.drone.getTank().fill(fluidStack, false) == Fluid.BUCKET_VOLUME) {
                    if (!simulate) {
                        this.decreaseCount(Fluid.BUCKET_VOLUME);
                        FluidStack fluidStack1 = FluidUtils.getFluidAt(this.drone.world(), pos, true);
                        this.drone.getTank().fill(fluidStack1, true);
                    }
                    return true;
                }
            }

            return false;
        }
    }
}
