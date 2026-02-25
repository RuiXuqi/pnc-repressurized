package me.desht.pneumaticcraft.common.thirdparty.jei;

import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.List;

public class PositionedStack {
    private final List<ItemStack> stacks;
    private final int x, y;
    private String tooltipKey;

    public PositionedStack(ItemStack stack, int x, int y) {
        this(Collections.singletonList(stack), x, y);
    }

    public PositionedStack(List<ItemStack> stacks, int x, int y) {
        this.stacks = stacks;
        this.x = x;
        this.y = y;
    }

    public PositionedStack setTooltipKey(String tooltipKey) {
        this.tooltipKey = tooltipKey;
        return this;
    }

    public String getTooltipKey() {
        return this.tooltipKey;
    }

    public List<ItemStack> getStacks() {
        return this.stacks;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }
}
