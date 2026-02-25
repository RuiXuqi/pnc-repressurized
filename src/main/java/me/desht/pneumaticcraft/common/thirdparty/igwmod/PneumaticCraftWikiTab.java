package me.desht.pneumaticcraft.common.thirdparty.igwmod;

import igwmod.gui.GuiWiki;
import igwmod.gui.tabs.BaseWikiTab;
import me.desht.pneumaticcraft.common.block.Blockss;
import me.desht.pneumaticcraft.lib.Names;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

public class PneumaticCraftWikiTab extends BaseWikiTab {

    public PneumaticCraftWikiTab() {
        this.pageEntries.add("base_concepts");
        this.pageEntries.add("generating_pressure");
        this.pageEntries.add("heat");
        this.pageEntries.add("pressure_tubes");
        this.pageEntries.add("pressure_chamber");
        this.pageEntries.add("machine_upgrades");
        this.pageEntries.add("block/pneumatic_dynamo");
        this.skipLine();
        this.pageEntries.add("block/omnidirectional_hopper");
        this.pageEntries.add("block/liquid_hopper");
        this.pageEntries.add("block/air_cannon");
        this.pageEntries.add("pneumatic_door");
        this.pageEntries.add("block/charging_station");
        this.skipLine();
        this.pageEntries.add("oil");
        this.pageEntries.add("block/plastic_mixer");
        this.pageEntries.add("elevator");
        this.pageEntries.add("block/universal_sensor");
        this.pageEntries.add("item/logistics_module");
        this.pageEntries.add("item/logistic_drone");
        this.skipLine();
        this.pageEntries.add("pneumatic_armor");
        this.pageEntries.add("block/programmer");
        this.pageEntries.add("item/drone");
        this.pageEntries.add("block/programmable_controller");
        this.pageEntries.add("item/remote");
        this.skipLine();
        this.pageEntries.add("printed_circuit_boards");
        this.pageEntries.add("assembly_machines");
        this.pageEntries.add("block/aphorism_tile");
        this.pageEntries.add("block/security_station");
        this.pageEntries.add("block/aerial_interface");
        this.skipLine();
        this.pageEntries.add("ic2integration");
        this.pageEntries.add("cofh_integration");
        this.pageEntries.add("cc_integration");

    }

    @Override
    public String getName() {
        return Names.MOD_NAME;
    }

    @Override
    public ItemStack renderTabIcon(GuiWiki gui) {
        return new ItemStack(Blockss.AIR_CANNON);
    }

    @Override
    protected String getPageName(String pageEntry) {
        if (pageEntry.startsWith("item") || pageEntry.startsWith("block")) {
            return I18n.format(pageEntry.replace("/", ".").replace("block", "tile") + ".name");
        } else {
            return I18n.format("igwtab.entry." + pageEntry);
        }
    }

    @Override
    protected String getPageLocation(String pageEntry) {
        if (pageEntry.startsWith("item") || pageEntry.startsWith("block")) return "pneumaticcraft:" + pageEntry;
        return "pneumaticcraft:menu/" + pageEntry;
    }

}
