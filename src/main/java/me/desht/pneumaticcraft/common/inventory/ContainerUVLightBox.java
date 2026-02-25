package me.desht.pneumaticcraft.common.inventory;

import me.desht.pneumaticcraft.common.item.Itemss;
import me.desht.pneumaticcraft.common.tileentity.TileEntityUVLightBox;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerUVLightBox extends ContainerPneumaticBase<TileEntityUVLightBox> {

    public ContainerUVLightBox(InventoryPlayer inventoryPlayer, TileEntityUVLightBox te) {
        super(te);

        this.addSlotToContainer(new SlotItemSpecific(te.getPrimaryInventory(), Itemss.EMPTY_PCB, 0, 71, 36));

        this.addUpgradeSlots(21, 29);

        this.addPlayerSlots(inventoryPlayer, 84);

    }

}
