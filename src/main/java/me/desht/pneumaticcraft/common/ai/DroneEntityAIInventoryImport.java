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
import net.minecraftforge.items.IItemHandler;

public class DroneEntityAIInventoryImport extends DroneAIImExBase {

    public DroneEntityAIInventoryImport(IDroneBase drone, ProgWidgetAreaItemBase widget) {
        super(drone, widget);
    }

    @Override
    protected boolean isValidPosition(BlockPos pos) {
        return this.importItems(pos, true);
    }

    @Override
    protected boolean doBlockInteraction(BlockPos pos, double distToBlock) {
        return this.importItems(pos, false) && super.doBlockInteraction(pos, distToBlock);
    }

    private boolean importItems(BlockPos pos, boolean simulate) {
        TileEntity te = this.drone.world().getTileEntity(pos);
        boolean[] sides = ((ISidedWidget) this.widget).getSides();
        for (int d = 0; d < sides.length; d++) {
            if (!sides[d]) {
                continue;
            }
            IItemHandler inv = IOHelper.getInventoryForTE(te, EnumFacing.byIndex(d));
            if (inv == null) {
                continue;
            }
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    if (this.widget.isItemValidForFilters(stack)) {
                        ItemStack importedStack = inv.extractItem(i, stack.getCount(), true);
                        if (importedStack.isEmpty()) {
                            continue;
                        }
                        importedStack = importedStack.copy();
                        if (((ICountWidget) this.widget).useCount()) {
                            importedStack.setCount(Math.min(importedStack.getCount(), this.getRemainingCount()));
                        }
                        ItemStack remainder = IOHelper.insert(this.drone, importedStack, EnumFacing.UP, simulate);
                        int removedItems = importedStack.getCount() - remainder.getCount();
                        if (!simulate) {
                            inv.extractItem(i, removedItems, false);
                            this.decreaseCount(removedItems);
                            this.drone.addAir(null, -PneumaticValues.DRONE_USAGE_INV);
                            if (((ICountWidget) this.widget).useCount() && this.getRemainingCount() <= 0) {
                                return false;
                            }
                        } else if (removedItems > 0) {
                            return true;
                        } else {
                            this.drone.addDebugEntry("gui.progWidget.inventoryImport.debug.filledToMax", pos);
                        }
                    } else {
                        this.drone.addDebugEntry("gui.progWidget.inventoryImport.debug.stackdoesntPassFilter", pos);
                    }
                }
            }
        }
//        } else {
//            drone.addDebugEntry("gui.progWidget.inventory.debug.noInventory", pos);
//        }

        return false;
    }

}
