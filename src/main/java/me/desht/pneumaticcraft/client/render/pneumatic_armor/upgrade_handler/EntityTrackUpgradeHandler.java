package me.desht.pneumaticcraft.client.render.pneumatic_armor.upgrade_handler;

import me.desht.pneumaticcraft.api.client.pneumaticHelmet.EntityTrackEvent;
import me.desht.pneumaticcraft.api.client.pneumaticHelmet.IOptionPage;
import me.desht.pneumaticcraft.api.client.pneumaticHelmet.IUpgradeRenderHandler;
import me.desht.pneumaticcraft.api.item.IItemRegistry.EnumUpgrade;
import me.desht.pneumaticcraft.client.gui.pneumatic_armor.GuiEntityTrackOptions;
import me.desht.pneumaticcraft.client.gui.widget.GuiAnimatedStat;
import me.desht.pneumaticcraft.client.gui.widget.GuiKeybindCheckBox;
import me.desht.pneumaticcraft.client.render.pneumatic_armor.ArmorMessage;
import me.desht.pneumaticcraft.client.render.pneumatic_armor.HUDHandler;
import me.desht.pneumaticcraft.client.render.pneumatic_armor.RenderEntityTarget;
import me.desht.pneumaticcraft.common.ai.StringFilterEntitySelector;
import me.desht.pneumaticcraft.common.config.ArmorHUDLayout;
import me.desht.pneumaticcraft.common.item.ItemPneumaticArmor;
import me.desht.pneumaticcraft.common.item.Itemss;
import me.desht.pneumaticcraft.common.pneumatic_armor.CommonArmorHandler;
import me.desht.pneumaticcraft.common.recipes.CraftingRegistrator;
import me.desht.pneumaticcraft.common.util.EntityFilter;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Stream;

public class EntityTrackUpgradeHandler implements IUpgradeRenderHandler {
    private static final int ENTITY_TRACK_THRESHOLD = 7;
    private static final float ENTITY_TRACKING_RANGE = 16F;
    private static final String UPGRADE_NAME = "entityTracker";

    private final Map<Integer, RenderEntityTarget> targets = new HashMap<>();
    private boolean shouldStopSpamOnEntityTracking = false;

    @SideOnly(Side.CLIENT)
    private GuiAnimatedStat entityTrackInfo;
    @Nonnull
    private EntityFilter entityFilter = new EntityFilter("");

    @Override
    @SideOnly(Side.CLIENT)
    public String getUpgradeName() {
        return UPGRADE_NAME;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void update(EntityPlayer player, int rangeUpgrades) {
        SearchUpgradeHandler searchHandler = HUDHandler.instance().getSpecificRenderer(SearchUpgradeHandler.class);

        if (searchHandler != null && (Minecraft.getMinecraft().world.getTotalWorldTime() & 0xf) == 0) {
            searchHandler.trackItemEntities(player, rangeUpgrades, GuiKeybindCheckBox.isHandlerEnabled(searchHandler));
        }

        ItemStack helmetStack = player.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
        String filterStr = helmetStack.isEmpty() ? "" : ItemPneumaticArmor.getEntityFilter(helmetStack);
        if (!this.entityFilter.toString().equals(filterStr)) {
            EntityFilter newFilter = EntityFilter.fromString(filterStr);
            if (newFilter != null) {
                this.entityFilter = newFilter;
            }
        }

        double entityTrackRange = ENTITY_TRACKING_RANGE + rangeUpgrades * PneumaticValues.RANGE_UPGRADE_HELMET_RANGE_INCREASE;
        AxisAlignedBB bbBox = getAABBFromRange(player, rangeUpgrades);
        List<Entity> entities = player.world.getEntitiesWithinAABB(Entity.class, bbBox,
                new EntityTrackerSelector(player, this.entityFilter, entityTrackRange));
        for (Entity entity : entities) {
            RenderEntityTarget target = this.targets.get(entity.getEntityId());
            if (target != null) {
                target.ticksExisted = Math.abs(target.ticksExisted); // cancel lost targets
            } else {
                this.targets.put(entity.getEntityId(), new RenderEntityTarget(entity));
            }
        }

        List<Integer> toRemove = new ArrayList<>();
        for (Map.Entry<Integer, RenderEntityTarget> entry : this.targets.entrySet()) {
            RenderEntityTarget target = entry.getValue();
            if (target.entity.isDead || player.getDistance(target.entity) > entityTrackRange + 5 || !this.entityFilter.test(target.entity)) {
                if (target.ticksExisted > 0) {
                    target.ticksExisted = -60;
                } else if (target.ticksExisted == -1) {
                    toRemove.add(entry.getKey());
                }
            }
        }
        toRemove.forEach(this.targets::remove);

        if (this.targets.size() > ENTITY_TRACK_THRESHOLD) {
            if (!this.shouldStopSpamOnEntityTracking) {
                this.shouldStopSpamOnEntityTracking = true;
                HUDHandler.instance().addMessage(new ArmorMessage("Stopped spam on Entity Tracker", new ArrayList<>(), 60, 0x7700AA00));
            }
        } else {
            this.shouldStopSpamOnEntityTracking = false;
        }
        List<String> text = new ArrayList<>();
        for (RenderEntityTarget target : this.targets.values()) {
            boolean wasNegative = target.ticksExisted < 0;
            target.ticksExisted += CommonArmorHandler.getHandlerForPlayer(player).getSpeedFromUpgrades(EntityEquipmentSlot.HEAD);
            if (target.ticksExisted >= 0 && wasNegative) target.ticksExisted = -1;
            target.update();
            if (target.isLookingAtTarget) {
                if (target.isInitialized()) {
                    text.add(TextFormatting.GRAY + target.entity.getDisplayName().getFormattedText());
                    text.addAll(target.getEntityText());
                } else {
                    text.add(TextFormatting.GRAY + "Acquiring target...");
                }
            }
        }
        if (text.size() == 0) {
            text.add("Filter mode: " + (this.entityFilter.toString().isEmpty() ? "None" : this.entityFilter.toString()));
        }
        this.entityTrackInfo.setText(text);
    }

    static AxisAlignedBB getAABBFromRange(EntityPlayer player, int rangeUpgrades) {
        double entityTrackRange = ENTITY_TRACKING_RANGE + Math.min(10, rangeUpgrades) * PneumaticValues.RANGE_UPGRADE_HELMET_RANGE_INCREASE;

        return new AxisAlignedBB(player.posX - entityTrackRange, player.posY - entityTrackRange, player.posZ - entityTrackRange, player.posX + entityTrackRange, player.posY + entityTrackRange, player.posZ + entityTrackRange);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void render3D(float partialTicks) {
        this.targets.values().forEach(target -> target.render(partialTicks, this.shouldStopSpamOnEntityTracking));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void render2D(float partialTicks, boolean upgradeEnabled) {
    }

    @Override
    public Item[] getRequiredUpgrades() {
        return new Item[]{Itemss.upgrades.get(EnumUpgrade.ENTITY_TRACKER)};
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void reset() {
        this.targets.clear();
    }

    @Override
    public float getEnergyUsage(int rangeUpgrades, EntityPlayer player) {
        return PneumaticValues.USAGE_ENTITY_TRACKER * (1 + (float) Math.min(10, rangeUpgrades) * PneumaticValues.RANGE_UPGRADE_HELMET_RANGE_INCREASE / ENTITY_TRACKING_RANGE) * CommonArmorHandler.getHandlerForPlayer(player).getSpeedFromUpgrades(EntityEquipmentSlot.HEAD);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IOptionPage getGuiOptionsPage() {
        return new GuiEntityTrackOptions(this);
    }

    @Override
    public EntityEquipmentSlot getEquipmentSlot() {
        return EntityEquipmentSlot.HEAD;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiAnimatedStat getAnimatedStat() {
        if (this.entityTrackInfo == null) {
            GuiAnimatedStat.StatIcon icon = GuiAnimatedStat.StatIcon.of(CraftingRegistrator.getUpgrade(EnumUpgrade.ENTITY_TRACKER));
            this.entityTrackInfo = new GuiAnimatedStat(null, "Current tracked entities:", icon,
                    0x3000AA00, null, ArmorHUDLayout.INSTANCE.entityTrackerStat);
            this.entityTrackInfo.setMinDimensionsAndReset(0, 0);
        }
        return this.entityTrackInfo;

    }

    public Stream<RenderEntityTarget> getTargetsStream() {
        return this.targets.values().stream();
    }

    public RenderEntityTarget getTargetForEntity(Entity entity) {
        return this.getTargetsStream().filter(target -> target.entity == entity).findFirst().orElse(null);
    }

    public void hack() {
        this.getTargetsStream().forEach(RenderEntityTarget::hack);
    }

    public void selectAsDebuggingTarget() {
        this.getTargetsStream().forEach(RenderEntityTarget::selectAsDebuggingTarget);
    }

    public boolean scroll(MouseEvent event) {
        return this.getTargetsStream().anyMatch(target -> target.scroll(event));
    }

    private class EntityTrackerSelector extends StringFilterEntitySelector {
        private final EntityPlayer player;
        private final double threshold;

        private EntityTrackerSelector(EntityPlayer player, EntityFilter filter, double threshold) {
            this.player = player;
            this.threshold = threshold;
            this.setFilter(Collections.singletonList(filter));
        }

        @Override
        public boolean apply(Entity entity) {
            return entity != this.player
                    && (entity instanceof EntityLivingBase || entity instanceof EntityHanging)
                    && !entity.isDead
                    && this.player.getDistance(entity) < this.threshold
                    && !MinecraftForge.EVENT_BUS.post(new EntityTrackEvent(entity))
                    && super.apply(entity);
        }
    }

    @Override
    public void onResolutionChanged() {
        this.entityTrackInfo = null;
    }
}
