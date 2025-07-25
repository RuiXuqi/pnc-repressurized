package me.desht.pneumaticcraft.client.gui;

import me.desht.pneumaticcraft.api.recipe.TemperatureRange;
import me.desht.pneumaticcraft.api.tileentity.IHeatExchanger;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTemperature;
import me.desht.pneumaticcraft.common.inventory.ContainerAdvancedAirCompressor;
import me.desht.pneumaticcraft.common.tileentity.TileEntityAdvancedAirCompressor;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;

import java.awt.*;
import java.util.List;

public class GuiAdvancedAirCompressor extends GuiAirCompressor {
    private WidgetTemperature tempWidget;

    public GuiAdvancedAirCompressor(InventoryPlayer player, TileEntityAdvancedAirCompressor te) {
        super(new ContainerAdvancedAirCompressor(player, te), te, Textures.GUI_ADVANCED_AIR_COMPRESSOR_LOCATION);
    }

    @Override
    public void initGui() {
        super.initGui();

        addWidget(tempWidget = new WidgetTemperature(-1, guiLeft + 97, guiTop + 20, TemperatureRange.of(273, 673), 273, 50)
                .setOperatingRange(TemperatureRange.of(323, 625)).setShowOperatingRange(false));
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        tempWidget.setTemperature(((IHeatExchanger) te).getHeatExchangerLogic(null).getTemperatureAsInt());
        tempWidget.autoScaleForTemperature();
    }

    @Override
    protected int getFuelSlotXOffset() {
        return 69;
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
