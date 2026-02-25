package me.desht.pneumaticcraft.common.inventory;

import me.desht.pneumaticcraft.common.tileentity.TileEntityPneumaticDoorBase;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerPneumaticDoor extends ContainerPneumaticBase<TileEntityPneumaticDoorBase> {

    public ContainerPneumaticDoor(InventoryPlayer inventoryPlayer, TileEntityPneumaticDoorBase te) {
        super(te);

        this.addUpgradeSlots(23, 29);

        this.addPlayerSlots(inventoryPlayer, 84);
    }
}
