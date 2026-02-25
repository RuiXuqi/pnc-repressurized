package me.desht.pneumaticcraft.common.inventory;

import me.desht.pneumaticcraft.common.tileentity.TileEntityOmnidirectionalHopper;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerOmnidirectionalHopper extends ContainerPneumaticBase<TileEntityOmnidirectionalHopper> {

    public ContainerOmnidirectionalHopper(InventoryPlayer inventoryPlayer, TileEntityOmnidirectionalHopper te) {
        super(te);

        this.addUpgradeSlots(23, 29);

        for (int i = 0; i < TileEntityOmnidirectionalHopper.INVENTORY_SIZE; i++) {
            this.addSlotToContainer(new SlotItemHandler(te.getPrimaryInventory(), i, 68 + i * 18, 36));
        }

        this.addPlayerSlots(inventoryPlayer, 84);
    }
}
