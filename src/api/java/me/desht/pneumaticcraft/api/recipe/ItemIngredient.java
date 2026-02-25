package me.desht.pneumaticcraft.api.recipe;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.commons.lang3.Validate;

/**
 * Represents an item-based ingredient for various PneumaticCraft machine recipes.
 * Can be a simple Itemstack, or a an Oredict key with associated quantity.
 */
public class ItemIngredient {
    private final ItemStack stack;
    private final String oredictKey;
    private final int amount;
    private String tooltipKey;

    public ItemIngredient(ItemStack stack) {
        this.stack = stack;
        this.oredictKey = null;
        this.amount = 0;
    }

    public ItemIngredient(Item item, int amount, int meta) {
        this(new ItemStack(item, amount, meta));
    }

    public ItemIngredient(String oredictKey, int amount) {
        Validate.isTrue(OreDictionary.doesOreNameExist(oredictKey), "invalid oredict key '" + oredictKey + "'");
        this.oredictKey = oredictKey;
        this.amount = amount;
        this.stack = ItemStack.EMPTY;
    }

    public ItemStack getSingleStack() {
        return this.oredictKey != null ?
                ItemHandlerHelper.copyStackWithSize(OreDictionary.getOres(this.oredictKey).get(0), this.amount) :
                this.stack;
    }

    public NonNullList<ItemStack> getStacks() {
        NonNullList<ItemStack> res = this.oredictKey != null ? OreDictionary.getOres(this.oredictKey) : NonNullList.from(ItemStack.EMPTY, this.stack);
        if (this.oredictKey != null) {
            res.forEach(stack -> stack.setCount(this.amount));
        }
        return res;
    }

    public int getItemAmount() {
        return this.oredictKey != null ? this.amount : this.stack.getCount();
    }

    public boolean isItemEqual(ItemStack stack) {
        if (this.oredictKey != null) {
            for (ItemStack s : OreDictionary.getOres(this.oredictKey)) {
                if (OreDictionary.itemMatches(s, stack, false))
                    return true;
            }
            return false;
        } else {
            return OreDictionary.itemMatches(this.stack, stack, false);
        }
    }

    public ItemIngredient setTooltip(String key) {
        this.tooltipKey = key;
        return this;
    }

    public String getTooltipKey() {
        return this.tooltipKey;
    }
}
