package me.desht.pneumaticcraft.common.inventory;

import me.desht.pneumaticcraft.common.tileentity.TileEntityVacuumPump;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerVacuumPump extends ContainerPneumaticBase<TileEntityVacuumPump> {

    public ContainerVacuumPump(InventoryPlayer inventoryPlayer, TileEntityVacuumPump te) {
        super(te);

        this.addUpgradeSlots(71, 29);

        this.addPlayerSlots(inventoryPlayer, 84);
    }
}
