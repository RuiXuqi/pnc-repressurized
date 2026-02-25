package me.desht.pneumaticcraft.client.gui;

import me.desht.pneumaticcraft.api.tileentity.IHeatExchanger;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTemperature;
import me.desht.pneumaticcraft.common.inventory.ContainerAdvancedLiquidCompressor;
import me.desht.pneumaticcraft.common.tileentity.TileEntityAdvancedLiquidCompressor;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;

import java.util.List;

public class GuiAdvancedLiquidCompressor extends GuiLiquidCompressor {

    public GuiAdvancedLiquidCompressor(InventoryPlayer player, TileEntityAdvancedLiquidCompressor te) {
        super(new ContainerAdvancedLiquidCompressor(player, te), te, Textures.GUI_ADVANCED_LIQUID_COMPRESSOR);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.addWidget(new WidgetTemperature(0, this.guiLeft + 92, this.guiTop + 20, 273, 675, ((IHeatExchanger) this.te).getHeatExchangerLogic(null), 325, 625));
    }

    @Override
    protected int getFluidOffset() {
        return 72;
    }

    @Override
    public void addWarnings(List<String> curInfo) {
        super.addWarnings(curInfo);
        if (this.te.getEfficiency() < 100) {
            curInfo.add(I18n.format("gui.tab.problems.advancedAirCompressor.efficiency", this.te.getEfficiency() + "%%"));
        }
    }
}
