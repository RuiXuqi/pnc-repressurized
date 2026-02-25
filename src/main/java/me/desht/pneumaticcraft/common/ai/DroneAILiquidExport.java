package me.desht.pneumaticcraft.common.ai;

import me.desht.pneumaticcraft.common.progwidgets.*;
import me.desht.pneumaticcraft.common.util.FluidUtils;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class DroneAILiquidExport extends DroneAIImExBase {

    public DroneAILiquidExport(IDroneBase drone, ProgWidgetAreaItemBase widget) {
        super(drone, widget);
    }

    @Override
    protected boolean isValidPosition(BlockPos pos) {
        return this.fillTank(pos, true);
    }

    @Override
    protected boolean doBlockInteraction(BlockPos pos, double distToBlock) {
        return this.fillTank(pos, false) && super.doBlockInteraction(pos, distToBlock);
    }

    private boolean fillTank(BlockPos pos, boolean simulate) {
        IFluidTank droneTank = this.drone.getTank();
        if (droneTank.getFluidAmount() == 0) {
            this.drone.addDebugEntry("gui.progWidget.liquidExport.debug.emptyDroneTank");
            this.abort();
            return false;
        } else {
            TileEntity te = this.drone.world().getTileEntity(pos);
            if (te != null) {
                FluidStack exportedFluid = droneTank.drain(Integer.MAX_VALUE, false);
                if (exportedFluid != null && ((ILiquidFiltered) this.widget).isFluidValid(exportedFluid.getFluid())) {
                    for (int i = 0; i < 6; i++) {
                        if (((ISidedWidget) this.widget).getSides()[i] && te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.byIndex(i))) {
                            IFluidHandler tank = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.byIndex(i));
                            int filledAmount = tank.fill(exportedFluid, false);
                            if (filledAmount > 0) {
                                if (((ICountWidget) this.widget).useCount()) {
                                    filledAmount = Math.min(filledAmount, this.getRemainingCount());
                                }
                                if (!simulate) {
                                    this.decreaseCount(tank.fill(droneTank.drain(filledAmount, true), true));
                                }
                                return true;
                            }
                        }
                    }
                    this.drone.addDebugEntry("gui.progWidget.liquidExport.debug.filledToMax", pos);
                } else {
                    this.drone.addDebugEntry("gui.progWidget.liquidExport.debug.noValidFluid");
                }
            } else if (((ILiquidExport) this.widget).isPlacingFluidBlocks() && (!((ICountWidget) this.widget).useCount() || this.getRemainingCount() >= 1000)) {
                Block fluidBlock = droneTank.getFluid().getFluid().getBlock();
                World w = this.drone.world();
                if (droneTank.getFluidAmount() >= 1000 && fluidBlock != null && this.isBlockSuitableForExport(w, pos)) {
                    if (!simulate) {
                        this.decreaseCount(1000);
                        droneTank.drain(1000, true);
                        w.setBlockState(pos, fluidBlock.getDefaultState());
                    }
                    return true;
                }
            }
            return false;
        }
    }

    private boolean isBlockSuitableForExport(World w, BlockPos pos) {
        return !FluidUtils.isSourceBlock(w, pos) && w.getBlockState(pos).getBlock().isReplaceable(w, pos);
    }
}
