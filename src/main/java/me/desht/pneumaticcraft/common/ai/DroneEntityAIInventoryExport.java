package me.desht.pneumaticcraft.common.ai;

import me.desht.pneumaticcraft.common.progwidgets.ICountWidget;
import me.desht.pneumaticcraft.common.progwidgets.ISidedWidget;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetAreaItemBase;
import me.desht.pneumaticcraft.common.util.IOHelper;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class DroneEntityAIInventoryExport extends DroneAIImExBase {

    public DroneEntityAIInventoryExport(IDroneBase drone, ProgWidgetAreaItemBase widget) {
        super(drone, widget);
    }

    @Override
    protected boolean isValidPosition(BlockPos pos) {
        return this.export(pos, true);
    }

    @Override
    protected boolean doBlockInteraction(BlockPos pos, double distToBlock) {
        return this.export(pos, false) && super.doBlockInteraction(pos, distToBlock);
    }

    private boolean export(BlockPos pos, boolean simulate) {
        TileEntity te = this.drone.world().getTileEntity(pos);
        if (te != null) {
            for (int i = 0; i < this.drone.getInv().getSlots(); i++) {
                ItemStack droneStack = this.drone.getInv().getStackInSlot(i);
                if (!droneStack.isEmpty()) {
                    if (this.widget.isItemValidForFilters(droneStack)) {
                        for (int side = 0; side < 6; side++) {
                            if (((ISidedWidget) this.widget).getSides()[side]) {
                                droneStack = droneStack.copy();
                                int oldCount = droneStack.getCount();
                                if (((ICountWidget) this.widget).useCount()) {
                                    droneStack.setCount(Math.min(droneStack.getCount(), this.getRemainingCount()));
                                }
                                ItemStack remainder = IOHelper.insert(te, droneStack.copy(), EnumFacing.byIndex(side), simulate);
                                int stackSize = this.drone.getInv().getStackInSlot(i).getCount() - (remainder.isEmpty() ? droneStack.getCount() : droneStack.getCount() - remainder.getCount());
                                droneStack.setCount(stackSize);
                                int exportedItems = oldCount - stackSize;
                                if (!simulate) {
                                    this.drone.getInv().setStackInSlot(i, stackSize > 0 ? droneStack : ItemStack.EMPTY);
                                    this.decreaseCount(exportedItems);
                                }
                                if (simulate && exportedItems > 0) return true;
//                                if (!(inv instanceof ISidedInventory))
//                                    break; //doing it for every side for no side sensitive inventories would be a waste.
                            }
                        }
                        if (droneStack.isEmpty() && !simulate) this.drone.addAir(null, -PneumaticValues.DRONE_USAGE_INV);
                        else this.drone.addDebugEntry("gui.progWidget.inventoryExport.debug.filledToMax", pos);
                    } else {
                        this.drone.addDebugEntry("gui.progWidget.inventoryExport.debug.stackdoesntPassFilter", pos);
                    }
                }
            }
        } else {
            this.drone.addDebugEntry("gui.progWidget.inventory.debug.noInventory", pos);
        }
        return false;
    }
}
