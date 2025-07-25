package me.desht.pneumaticcraft.common.thirdparty.ic2;

import me.desht.pneumaticcraft.api.recipe.TemperatureRange;
import me.desht.pneumaticcraft.client.gui.GuiPneumaticContainerBase;
import me.desht.pneumaticcraft.client.gui.widget.GuiAnimatedStat;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTemperature;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTemperature1;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.TextFormatting;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GuiElectricCompressor extends GuiPneumaticContainerBase<TileEntityElectricCompressor> {
    private GuiAnimatedStat inputStat;
    private WidgetTemperature tempWidget;

    public GuiElectricCompressor(InventoryPlayer inventory, TileEntityElectricCompressor te) {
        super(new ContainerElectricCompressor(inventory, te), te, Textures.GUI_4UPGRADE_SLOTS);
    }

    @Override
    public void initGui() {
        super.initGui();
        inputStat = addAnimatedStat(PneumaticCraftUtils.xlate("gui.tab.input"), IC2.glassFibreCable, 0xFF555555, false);

        addWidget(tempWidget = new WidgetTemperature(-1, guiLeft + 97, guiTop + 20, TemperatureRange.of(273, 673), 273, 50)
                .setOperatingRange(TemperatureRange.of(323, 625)).setShowOperatingRange(false));
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        inputStat.setText(getOutputStat());

        tempWidget.setTemperature(te.getHeatExchangerLogic(null).getTemperatureAsInt());
        tempWidget.autoScaleForTemperature();
    }

    private List<String> getOutputStat() {
        List<String> textList = new ArrayList<>();
        textList.add(TextFormatting.GRAY + PneumaticCraftUtils.xlate("gui.tab.status.fluxCompressor.maxInputRate"));
        textList.add(TextFormatting.BLACK.toString() + te.getMaxSafeInput() + " EU/t");
        return textList;
    }

    @Override
    protected Point getGaugeLocation() {
        return getGaugeLocation(10, 0);
    }

    @Override
    protected void addPressureStatInfo(List<String> pressureStatText) {
        super.addPressureStatInfo(pressureStatText);
        pressureStatText.add(TextFormatting.BLACK + I18n.format("gui.tooltip.maxProduction", PneumaticCraftUtils.roundNumberTo(te.lastEnergyProduction,2)));
    }

    @Override
    protected void addProblems(List<String> textList) {
        super.addProblems(textList);
        if (te.lastEnergyProduction == 0) {
            textList.add(PneumaticCraftUtils.xlate("gui.tab.problems.electricCompressor.noEU"));
        }
    }

    @Override
    protected void addWarnings(List<String> curInfo) {
        super.addWarnings(curInfo);
        if (te.getEfficiency() < 100) {
            curInfo.add(I18n.format("gui.tab.problems.advancedAirCompressor.efficiency", te.getEfficiency() + "%%"));
        }
    }
}
