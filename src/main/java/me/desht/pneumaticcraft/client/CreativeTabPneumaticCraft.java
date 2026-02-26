package me.desht.pneumaticcraft.client;

import me.desht.pneumaticcraft.common.item.Itemss;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class CreativeTabPneumaticCraft extends CreativeTabs {
    public CreativeTabPneumaticCraft(String label) {
        super(label);
    }

    @Nonnull
    @Override
    public ItemStack createIcon() {
        return new ItemStack(Itemss.PRESSURE_GAUGE);
    }
}
