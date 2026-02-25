package me.desht.pneumaticcraft.common.inventory;

import me.desht.pneumaticcraft.api.item.IPressurizable;
import me.desht.pneumaticcraft.common.tileentity.TileEntityChargingStation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

public class ContainerChargingStation extends ContainerPneumaticBase<TileEntityChargingStation> {

    public ContainerChargingStation(InventoryPlayer inventoryPlayer, TileEntityChargingStation te) {
        super(te);

        this.addSlotToContainer(new SlotItemHandler(te.getPrimaryInventory(), 0, 91, 39) {
            @Override
            public int getSlotStackLimit() {
                return 1;
            }
        });

        this.addUpgradeSlots(42, 29);

        this.addArmorSlots(inventoryPlayer, 9, 8);

        this.addPlayerSlots(inventoryPlayer, 94);
    }

    @Override
    @Nonnull
    public ItemStack transferStackInSlot(EntityPlayer player, int slot) {
        Slot srcSlot = this.inventorySlots.get(slot);
        if (srcSlot == null || !srcSlot.getHasStack()) {
            return ItemStack.EMPTY;
        }
        ItemStack srcStack = srcSlot.getStack().copy();
        ItemStack copyOfSrcStack = srcStack.copy();

        if (slot == 0 && srcStack.getItem() instanceof ItemArmor) {
            // chargeable slot - move to armor if appropriate, player inv otherwise
            if (!this.mergeItemStack(srcStack, 5, 9, false)
                    && !this.mergeItemStack(srcStack, this.playerSlotsStart, this.playerSlotsStart + 36, false))
                return ItemStack.EMPTY;
        } else if (slot >= 5 && slot < 9 && srcStack.getItem() instanceof IPressurizable) {
            // armor slots - try to move to the charging slot if possible
            if (!this.mergeItemStack(srcStack, 0, 1, false)
                    && !this.mergeItemStack(srcStack, this.playerSlotsStart, this.playerSlotsStart + 36, false))
                return ItemStack.EMPTY;
        } else if (slot < this.playerSlotsStart) {
            if (!this.mergeItemStack(srcStack, this.playerSlotsStart, this.playerSlotsStart + 36, false))
                return ItemStack.EMPTY;
        } else {
            if (!this.mergeItemStack(srcStack, 0, this.playerSlotsStart, false))
                return ItemStack.EMPTY;
        }

        srcSlot.putStack(srcStack);
        srcSlot.onSlotChange(srcStack, copyOfSrcStack);
        srcSlot.onTake(player, srcStack);

        return copyOfSrcStack;
    }
}
