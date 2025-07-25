package me.desht.pneumaticcraft.client.gui;

import me.desht.pneumaticcraft.api.recipe.TemperatureRange;
import me.desht.pneumaticcraft.api.tileentity.IHeatExchanger;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTemperature;
import me.desht.pneumaticcraft.common.inventory.ContainerAdvancedLiquidCompressor;
import me.desht.pneumaticcraft.common.tileentity.TileEntityAdvancedLiquidCompressor;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;

import java.awt.*;
import java.util.List;

public class GuiAdvancedLiquidCompressor extends GuiLiquidCompressor {
    private WidgetTemperature tempWidget;

    public GuiAdvancedLiquidCompressor(InventoryPlayer player, TileEntityAdvancedLiquidCompressor te) {
        super(new ContainerAdvancedLiquidCompressor(player, te), te, Textures.GUI_ADVANCED_LIQUID_COMPRESSOR);
    }

    @Override
    public void initGui() {
        super.initGui();

        addWidget(tempWidget = new WidgetTemperature(-1, guiLeft + 100, guiTop + 20, TemperatureRange.of(273, 673), 273, 50)
                .setOperatingRange(TemperatureRange.of(323, 625)).setShowOperatingRange(false));
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        tempWidget.setTemperature(((IHeatExchanger) te).getHeatExchangerLogic(null).getTemperatureAsInt());
        tempWidget.autoScaleForTemperature();
    }

    @Override
    protected int getFluidOffset() {
        return 72;
    }

    @Override
    protected Point getGaugeLocation() {
        return getGaugeLocation(10, 0);
    }

    @Override
    public void addWarnings(List<String> curInfo) {
        super.addWarnings(curInfo);
        if (te.getEfficiency() < 100) {
            curInfo.add(I18n.format("gui.tab.problems.advancedAirCompressor.efficiency", te.getEfficiency() + "%%"));
        }
    }
}
