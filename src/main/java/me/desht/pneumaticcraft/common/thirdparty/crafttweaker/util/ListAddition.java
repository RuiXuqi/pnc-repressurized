package me.desht.pneumaticcraft.common.thirdparty.crafttweaker.util;

import java.util.List;

public class ListAddition<T> extends ListModification<T> {

    public ListAddition(String name, List<T> recipes) {
        super(name, recipes);
    }

    public ListAddition(String name, List<T> recipes, List<T> entries) {
        super(name, recipes, entries);
    }

    public ListAddition(String name, List<T> recipes, T entry) {
        super(name, recipes, entry);
    }

    @Override
    public void apply() {
        if (this.entries.isEmpty()) {
            return;
        }

        for (T recipe : this.entries) {
            if (recipe != null) {
                if (!this.recipes.add(recipe)) {
                    Helper.logError(String.format("Error adding %s recipe to list.", this.name));
                }
            } else {
                Helper.logError(String.format("Error adding %s recipe: null object", this.name));
            }
        }
    }

    @Override
    public String describe() {
        return String.format("Adding %d %s recipe(s)", this.entries.size(), this.name);
    }
}
