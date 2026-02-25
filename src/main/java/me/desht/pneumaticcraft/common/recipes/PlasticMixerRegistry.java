package me.desht.pneumaticcraft.common.recipes;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import java.util.*;

public enum PlasticMixerRegistry {
    INSTANCE;

    private final Set<String> validFluids = new HashSet<>();
    private final Map<Item, Boolean> validItems = new HashMap<>();

    private final List<PlasticMixerRecipe> recipes = new ArrayList<>();

    public void addPlasticMixerRecipe(@Nonnull FluidStack fluid, @Nonnull ItemStack stack, int temperature, boolean allowMelting, boolean allowSolidifying, boolean useDye, int meta) {
        if (fluid.amount > 0 && !stack.isEmpty()) {
            this.recipes.add(new PlasticMixerRecipe(fluid, ItemHandlerHelper.copyStackWithSize(stack, 1), temperature, allowMelting, allowSolidifying, useDye, meta));
            this.validItems.put(stack.getItem(), allowMelting);
            this.validFluids.add(fluid.getFluid().getName());
        } else {
            this.recipes.removeIf(record -> record.getFluidStack().getFluid() == fluid.getFluid());
            this.validItems.remove(stack.getItem());
            this.validFluids.remove(fluid.getFluid().getName());
        }
    }

    public PlasticMixerRecipe getRecipe(FluidStack input) {
        if (input == null || input.amount == 0) return null;

        for (PlasticMixerRecipe recipe : this.recipes) {
            if (recipe.allowSolidifying && recipe.fluidStack.getFluid() == input.getFluid() && recipe.fluidStack.amount <= input.amount) {
                return recipe;
            }
        }

        return null;
    }

    public PlasticMixerRecipe getRecipe(ItemStack stack) {
        for (PlasticMixerRecipe recipe : this.recipes) {
            if (recipe.allowMelting && stack.getItem() == recipe.itemStack.getItem()) {
                return recipe;
            }
        }
        return null;
    }

    public void clear() {
        this.recipes.clear();
        this.validFluids.clear();
        this.validItems.clear();
    }

    public Iterable<? extends PlasticMixerRecipe> allRecipes() {
        return this.recipes;
    }

    public boolean isValidInputItem(ItemStack stack) {
        return this.validItems.getOrDefault(stack.getItem(), false);
    }

    public boolean isValidOutputItem(ItemStack stack) {
        return this.validItems.containsKey(stack.getItem());
    }

    public boolean isValidFluid(FluidStack stack) {
        return this.validFluids.contains(stack.getFluid().getName());
    }

    public static class PlasticMixerRecipe {
        private final FluidStack fluidStack;
        private final ItemStack itemStack;
        private final int temperature;
        private final boolean allowMelting;
        private final boolean allowSolidifying;
        private final boolean useDye;
        private final int meta;

        PlasticMixerRecipe(FluidStack fluidStack, ItemStack itemStack, int temperature, boolean allowMelting, boolean allowSolidifying, boolean useDye, int meta) {
            this.fluidStack = fluidStack;
            this.itemStack = itemStack;
            this.temperature = temperature;
            this.allowMelting = allowMelting;
            this.allowSolidifying = allowSolidifying;
            this.useDye = useDye;
            this.meta = meta;
        }

        public FluidStack getFluidStack() {
            return this.fluidStack;
        }

        public ItemStack getItemStack() {
            return this.itemStack;
        }

        public boolean allowMelting() {
            return this.allowMelting;
        }

        public boolean allowSolidifying() {
            return this.allowSolidifying;
        }

        public int getTemperature() {
            return this.temperature;
        }

        public boolean useDye() {
            return this.useDye;
        }

        public int getMeta() {
            return this.meta;
        }

        public int getNumSubTypes() {
            Item item = this.getItemStack().getItem();
            if (item.getCreativeTab() == null) return 1;
            NonNullList<ItemStack> subs = NonNullList.create();
            item.getSubItems(item.getCreativeTab(), subs);
            return Math.max(1, subs.size());
        }

    }
}
