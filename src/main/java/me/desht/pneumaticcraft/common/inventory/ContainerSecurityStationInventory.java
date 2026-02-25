package me.desht.pneumaticcraft.common.inventory;

import me.desht.pneumaticcraft.common.item.Itemss;
import me.desht.pneumaticcraft.common.tileentity.TileEntitySecurityStation;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerSecurityStationInventory extends ContainerPneumaticBase<TileEntitySecurityStation> {

    public ContainerSecurityStationInventory(InventoryPlayer inventoryPlayer, TileEntitySecurityStation te) {
        super(te);

        //add the network slots
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 5; j++) {
                this.addSlotToContainer(new SlotItemSpecific(te.getPrimaryInventory(), Itemss.NETWORK_COMPONENT, j + i * 5, 17 + j * 18, 22 + i * 18));
            }
        }

        this.addUpgradeSlots(128, 62);

        this.addPlayerSlots(inventoryPlayer, 157);
    }

}
