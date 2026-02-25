package me.desht.pneumaticcraft.common.inventory;

import me.desht.pneumaticcraft.common.tileentity.TileEntityElevatorBase;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerElevator extends ContainerPneumaticBase<TileEntityElevatorBase> {

    public ContainerElevator(InventoryPlayer inventoryPlayer, TileEntityElevatorBase te) {
        super(te);

        this.addUpgradeSlots(23, 29);

        this.addPlayerSlots(inventoryPlayer, 84);

    }

}
