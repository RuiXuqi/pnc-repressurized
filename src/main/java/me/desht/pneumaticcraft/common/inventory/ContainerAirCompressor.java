package me.desht.pneumaticcraft.common.inventory;

import me.desht.pneumaticcraft.common.tileentity.TileEntityAirCompressor;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerAirCompressor extends ContainerPneumaticBase<TileEntityAirCompressor> {

    public ContainerAirCompressor(InventoryPlayer inventoryPlayer, TileEntityAirCompressor te) {
        super(te);

        // Add the burn slot.
        this.addSlotToContainer(new SlotItemHandler(te.getPrimaryInventory(), 0, this.getFuelSlotXOffset(), 54));

        this.addUpgradeSlots(23, 29);
        this.addPlayerSlots(inventoryPlayer, 84);
    }

    protected int getFuelSlotXOffset() {
        return 80;
    }

}
