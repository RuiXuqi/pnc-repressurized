package me.desht.pneumaticcraft.common.inventory;

import me.desht.pneumaticcraft.common.tileentity.TileEntityThermalCompressor;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerThermalCompressor extends ContainerPneumaticBase<TileEntityThermalCompressor> {

    public ContainerThermalCompressor(InventoryPlayer inventoryPlayer, TileEntityThermalCompressor te) {
        super(te);

        this.addUpgradeSlots(23, 29);
        this.addPlayerSlots(inventoryPlayer, 84);
    }

}
