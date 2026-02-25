package me.desht.pneumaticcraft.common.inventory;

import me.desht.pneumaticcraft.common.tileentity.TileEntityLiquidCompressor;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerLiquidCompressor extends ContainerPneumaticBase<TileEntityLiquidCompressor> {

    public ContainerLiquidCompressor(InventoryPlayer inventoryPlayer, TileEntityLiquidCompressor te) {
        super(te);

        this.addUpgradeSlots(11, 29);

        this.addSlotToContainer(new SlotFullFluidContainer(te.getPrimaryInventory(), 0, this.getFluidContainerOffset(), 22));
        this.addSlotToContainer(new SlotOutput(te.getPrimaryInventory(), 1, this.getFluidContainerOffset(), 55));

        this.addPlayerSlots(inventoryPlayer, 84);
    }

    protected int getFluidContainerOffset() {
        return 62;
    }

}
