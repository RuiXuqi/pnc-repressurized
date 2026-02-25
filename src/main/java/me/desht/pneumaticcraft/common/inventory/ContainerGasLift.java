package me.desht.pneumaticcraft.common.inventory;

import me.desht.pneumaticcraft.common.tileentity.TileEntityGasLift;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerGasLift extends ContainerPneumaticBase<TileEntityGasLift> {

    public ContainerGasLift(InventoryPlayer inventoryPlayer, TileEntityGasLift te) {
        super(te);

        this.addUpgradeSlots(11, 29);

        this.addSlotToContainer(new SlotItemHandler(te.getPrimaryInventory(), 0, 55, 48));

        this.addPlayerSlots(inventoryPlayer, 84);
    }
}
