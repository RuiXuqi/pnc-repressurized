package me.desht.pneumaticcraft.client.gui;

import me.desht.pneumaticcraft.api.item.IItemRegistry.EnumUpgrade;
import me.desht.pneumaticcraft.common.inventory.ContainerChargingStationItemInventory;
import me.desht.pneumaticcraft.common.item.ItemDrone;
import me.desht.pneumaticcraft.common.recipes.CraftingRegistrator;
import me.desht.pneumaticcraft.common.tileentity.TileEntityChargingStation;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.item.ItemStack;

public class GuiDrone extends GuiPneumaticInventoryItem {

    public GuiDrone(ContainerChargingStationItemInventory container, TileEntityChargingStation te) {
        super(container, te);
    }

    @Override
    public void initGui() {
        super.initGui();

        if (!(this.itemStack.getItem() instanceof ItemDrone)) {
            return; // should never happen...
        }
        ItemDrone itemDrone = (ItemDrone) this.itemStack.getItem();

        this.maybeAddUpgradeTab(itemDrone, EnumUpgrade.SPEED, false);
        this.maybeAddUpgradeTab(itemDrone, EnumUpgrade.DISPENSER, false);
        this.maybeAddUpgradeTab(itemDrone, EnumUpgrade.ITEM_LIFE, false);
        this.maybeAddUpgradeTab(itemDrone, EnumUpgrade.MAGNET, false);
        this.addAnimatedStat("gui.tab.info", Textures.GUI_INFO_LOCATION, 0xFF8888FF, true).setText("gui.tab.info.item.drone");
        this.maybeAddUpgradeTab(itemDrone, EnumUpgrade.SECURITY, true);
        this.maybeAddUpgradeTab(itemDrone, EnumUpgrade.VOLUME, true);
        this.maybeAddUpgradeTab(itemDrone, EnumUpgrade.ENTITY_TRACKER, true);
        this.maybeAddUpgradeTab(itemDrone, EnumUpgrade.RANGE, true);

    }

    private void maybeAddUpgradeTab(ItemDrone itemDrone, EnumUpgrade upgrade, boolean leftSided) {
        if (itemDrone.upgradeApplies(upgrade)) {
            ItemStack upgradeStack = CraftingRegistrator.getUpgrade(upgrade);
            this.addAnimatedStat(upgradeStack.getDisplayName(), upgradeStack,
                    0xFF4040FF, leftSided).setText("gui.tab.info.item.drone." + upgrade.getName() + "Upgrade");
        }
    }

    @Override
    protected int getDefaultVolume() {
        return PneumaticValues.DRONE_VOLUME;
    }
}
