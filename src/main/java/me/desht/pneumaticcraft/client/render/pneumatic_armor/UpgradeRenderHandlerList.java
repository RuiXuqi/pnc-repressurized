package me.desht.pneumaticcraft.client.render.pneumatic_armor;

import me.desht.pneumaticcraft.api.client.pneumaticHelmet.IUpgradeRenderHandler;
import me.desht.pneumaticcraft.api.item.IItemRegistry;
import me.desht.pneumaticcraft.client.render.pneumatic_armor.upgrade_handler.*;
import me.desht.pneumaticcraft.common.pneumatic_armor.CommonArmorHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpgradeRenderHandlerList {
    private static UpgradeRenderHandlerList INSTANCE;

    private final List<List<IUpgradeRenderHandler>> upgradeRenderers;

    public static UpgradeRenderHandlerList instance() {
        return INSTANCE;
    }

    public static void init() {
        INSTANCE = new UpgradeRenderHandlerList();
    }

    private final Map<Class<? extends IUpgradeRenderHandler>, IUpgradeRenderHandler> classMap = new HashMap<>();

    // convenience
    public static final EntityEquipmentSlot[] ARMOR_SLOTS = new EntityEquipmentSlot[4];

    static {
        ARMOR_SLOTS[0] = EntityEquipmentSlot.HEAD;
        ARMOR_SLOTS[1] = EntityEquipmentSlot.CHEST;
        ARMOR_SLOTS[2] = EntityEquipmentSlot.LEGS;
        ARMOR_SLOTS[3] = EntityEquipmentSlot.FEET;
    }

    private UpgradeRenderHandlerList() {
        this.upgradeRenderers = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            this.upgradeRenderers.add(new ArrayList<>());
        }
        this.addUpgradeRenderer(new MainHelmetHandler());  // always keep this first
        this.addUpgradeRenderer(new BlockTrackUpgradeHandler());
        this.addUpgradeRenderer(new EntityTrackUpgradeHandler());
        this.addUpgradeRenderer(new SearchUpgradeHandler());
        this.addUpgradeRenderer(new CoordTrackUpgradeHandler());
        this.addUpgradeRenderer(new DroneDebugUpgradeHandler());
        this.addUpgradeRenderer(new NightVisionUpgradeHandler());
        this.addUpgradeRenderer(new ScubaUpgradeHandler());

        this.addUpgradeRenderer(new MagnetUpgradeHandler());
        this.addUpgradeRenderer(new ChargingUpgradeHandler());
        this.addUpgradeRenderer(new ChestplateLauncherHandler());
        this.addUpgradeRenderer(new AirConUpgradeHandler());

        this.addUpgradeRenderer(new RunSpeedUpgradeHandler());
        this.addUpgradeRenderer(new JumpBoostUpgradeHandler());

        this.addUpgradeRenderer(new JetBootsUpgradeHandler());
        this.addUpgradeRenderer(new StepAssistUpgradeHandler());
        this.addUpgradeRenderer(new KickUpgradeHandler());
    }

    public void addUpgradeRenderer(IUpgradeRenderHandler handler) {
        this.upgradeRenderers.get(handler.getEquipmentSlot().getIndex()).add(handler);
        this.classMap.put(handler.getClass(), handler);
    }

    public <T extends IUpgradeRenderHandler> T getRenderHandler(Class<T> clazz) {
        return (T) this.classMap.get(clazz);
    }

    public List<IUpgradeRenderHandler> getHandlersForSlot(EntityEquipmentSlot slot) {
        return this.upgradeRenderers.get(slot.getIndex());
    }

    public float getAirUsage(EntityPlayer player, EntityEquipmentSlot slot, boolean countDisabled) {
        float totalUsage = 0;
        for (int i = 0; i < this.upgradeRenderers.get(slot.getIndex()).size(); i++) {
            CommonArmorHandler handler = CommonArmorHandler.getHandlerForPlayer(player);
            if (handler.isUpgradeRendererInserted(slot, i) && (countDisabled || handler.isUpgradeRendererEnabled(slot, i)))
                totalUsage += this.upgradeRenderers.get(slot.getIndex()).get(i).getEnergyUsage(handler.getUpgradeCount(slot, IItemRegistry.EnumUpgrade.RANGE), player);
        }
        return totalUsage;
    }
}
