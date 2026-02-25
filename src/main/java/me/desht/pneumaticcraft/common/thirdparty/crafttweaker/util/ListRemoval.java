package me.desht.pneumaticcraft.common.thirdparty.crafttweaker.util;

import java.util.List;

public class ListRemoval<T> extends ListModification<T> {

    public ListRemoval(String name, List<T> recipes) {
        super(name, recipes);
    }

    public ListRemoval(String name, List<T> recipes, List<T> entries) {
        super(name, recipes, entries);
    }

    public ListRemoval(String name, List<T> recipes, T entry) {
        super(name, recipes, entry);
    }

    @Override
    public void apply() {
        if (this.entries.isEmpty()) {
            return;
        }

        for (T recipe : this.entries) {
            if (recipe != null) {
                if (!this.recipes.remove(recipe)) {
                    Helper.logError(String.format("Error removing %s recipe to list", this.name));
                }
            } else {
                Helper.logError(String.format("Error removing %s recipe: null object", this.name));
            }
        }
    }

    @Override
    public String describe() {
        return String.format("Removing %d %s recipe(s)", this.entries.size(), this.name);
    }
}
