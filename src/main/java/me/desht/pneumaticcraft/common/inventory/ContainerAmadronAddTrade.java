package me.desht.pneumaticcraft.common.inventory;

import me.desht.pneumaticcraft.common.item.Itemss;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;

public class ContainerAmadronAddTrade extends ContainerPneumaticBase {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;

    private final ItemStackHandler inv = new ItemStackHandler(2);

    public ContainerAmadronAddTrade() {
        super(null);
        this.addSlotToContainer(new SlotUntouchable(this.inv, INPUT_SLOT, 10, 90));
        this.addSlotToContainer(new SlotUntouchable(this.inv, OUTPUT_SLOT, 99, 90));
    }

    public void setStack(int index, @Nonnull ItemStack stack) {
        this.inv.setStackInSlot(index, stack);
    }

    @Nonnull
    public ItemStack getStack(int index) {
        return this.inv.getStackInSlot(index);
    }

    @Nonnull
    public ItemStack getInputStack() {
        return this.inv.getStackInSlot(INPUT_SLOT);
    }

    @Nonnull
    public ItemStack getOutputStack() {
        return this.inv.getStackInSlot(OUTPUT_SLOT);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return player.getHeldItemMainhand().getItem() == Itemss.AMADRON_TABLET;
    }

    @Override
    public void putStackInSlot(int slot, @Nonnull ItemStack stack) {
    }

}
