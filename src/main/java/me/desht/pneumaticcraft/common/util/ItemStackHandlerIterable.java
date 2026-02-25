package me.desht.pneumaticcraft.common.util;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ItemStackHandlerIterable implements Iterable<ItemStack> {

    private final ItemStackHandler itemStackHandler;

    public ItemStackHandlerIterable(ItemStackHandler itemStackHandler) {
        this.itemStackHandler = itemStackHandler;
    }

    @Override
    public Iterator<ItemStack> iterator() {
        return new Iterator<ItemStack>() {
            private int curIndex = 0;

            @Override
            public boolean hasNext() {
                return this.curIndex < ItemStackHandlerIterable.this.itemStackHandler.getSlots();
            }

            @Override
            public ItemStack next() {
                if (!this.hasNext()) throw new NoSuchElementException();
                return ItemStackHandlerIterable.this.itemStackHandler.getStackInSlot(this.curIndex++);
            }

            @Override
            public void remove() {
                if (this.curIndex == 0) throw new IllegalStateException("First call next()!");
                ItemStackHandlerIterable.this.itemStackHandler.setStackInSlot(this.curIndex - 1, ItemStack.EMPTY);
            }
        };
    }

    public Stream<ItemStack> stream() {
        return StreamSupport.stream(this.spliterator(), false);
    }

}
