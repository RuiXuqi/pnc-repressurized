package me.desht.pneumaticcraft.common.thirdparty.crafttweaker.util;

import java.util.List;

public class RemoveAllRecipes<T> extends ListRemoval<T> {

    public RemoveAllRecipes(String name, List<T> recipes) {
        super(name, recipes);
    }

    @Override
    public void apply() {
        this.entries.addAll(this.recipes);

        super.apply();
    }

    @Override
    public String describe() {
        return String.format("Removing all %d %s recipe(s)", this.recipes.size(), this.name);
    }
}
