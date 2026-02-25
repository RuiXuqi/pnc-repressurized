package me.desht.pneumaticcraft.common.progwidgets;

import me.desht.pneumaticcraft.common.util.EntityFilter;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a pair of entity filters: a whitelist and a blacklist, as used by programming puzzle pieces.
 */
class EntityFilterPair {
    private final IProgWidget widget;
    private final EntityFilter entityWhitelist;
    private final EntityFilter entityBlacklist;
    private String errorWhite = "", errorBlack = "";

    EntityFilterPair(IProgWidget widget) {
        this.widget = widget;
        this.entityWhitelist = this.getFilter(widget, true);
        this.entityBlacklist = this.getFilter(widget, false);
    }

    public static void addErrors(IProgWidget widget, List<String> errors) {
        EntityFilterPair filter = new EntityFilterPair(widget);
        if (!filter.errorWhite.isEmpty()) {
            errors.add("Invalid whitelist filter: " + filter.errorWhite);
        }
        if (!filter.errorBlack.isEmpty()) {
            errors.add("Invalid blacklist filter: " + filter.errorBlack);
        }
    }

    private EntityFilter getFilter(IProgWidget widget, boolean whitelist) {
        try {
            return EntityFilter.fromProgWidget(widget, whitelist);
        } catch (IllegalArgumentException e) {
            if (whitelist) {
                this.errorWhite = e.getMessage();
                return EntityFilter.allow();
            } else {
                this.errorBlack = e.getMessage();
                return EntityFilter.deny();
            }
        }
    }

    boolean isEntityValid(Entity e) {
        return this.entityWhitelist.test(e) && !this.entityBlacklist.test(e);
    }

    List<Entity> getValidEntities(World world) {
        return this.getEntitiesInArea(
                (ProgWidgetArea) this.widget.getConnectedParameters()[0],
                (ProgWidgetArea) this.widget.getConnectedParameters()[this.widget.getParameters().length],
                world
        );
    }

    private List<Entity> getEntitiesInArea(ProgWidgetArea whitelistWidget, ProgWidgetArea blacklistWidget, World world) {
        if (whitelistWidget == null) {
            return new ArrayList<>();
        }
        Set<Entity> entities = new HashSet<>();
        ProgWidgetArea widget = whitelistWidget;
        while (widget != null) {
            entities.addAll(widget.getEntitiesWithinArea(world, this.entityWhitelist));
            widget = (ProgWidgetArea) widget.getConnectedParameters()[0];
        }
        widget = blacklistWidget;
        while (widget != null) {
            entities.removeAll(widget.getEntitiesWithinArea(world, this.entityWhitelist));
            widget = (ProgWidgetArea) widget.getConnectedParameters()[0];
        }
        if (this.entityBlacklist != null) {
            entities.removeIf(this.entityBlacklist);
        }
        return new ArrayList<>(entities);
    }

}
