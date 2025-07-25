package me.desht.pneumaticcraft.client.gui;

import me.desht.pneumaticcraft.api.recipe.TemperatureRange;
import me.desht.pneumaticcraft.client.gui.widget.GuiAnimatedStat;
import me.desht.pneumaticcraft.client.gui.widget.WidgetEnergy;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTemperature;
import me.desht.pneumaticcraft.common.inventory.ContainerEnergy;
import me.desht.pneumaticcraft.common.tileentity.TileEntityFluxCompressor;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GuiFluxCompressor extends GuiPneumaticContainerBase<TileEntityFluxCompressor> {
    private GuiAnimatedStat inputStat;
    private WidgetTemperature tempWidget;

    public GuiFluxCompressor(Container container, TileEntityFluxCompressor te) {
        super(container, te, Textures.GUI_4UPGRADE_SLOTS);
    }

    public GuiFluxCompressor(InventoryPlayer inventoryPlayer, TileEntityFluxCompressor te) {
        super(new ContainerEnergy(inventoryPlayer, te), te, Textures.GUI_4UPGRADE_SLOTS);
    }

    @Override
    public void initGui() {
        super.initGui();
        inputStat = addAnimatedStat(PneumaticCraftUtils.xlate("gui.tab.input"), Textures.GUI_BUILDCRAFT_ENERGY, 0xFF555555, false);
        IEnergyStorage storage = te.getCapability(CapabilityEnergy.ENERGY, null);

        addWidget(new WidgetEnergy(guiLeft + 20, guiTop + 20, storage));
        addWidget(tempWidget = new WidgetTemperature(-1, guiLeft + 97, guiTop + 20, TemperatureRange.of(223, 673), 273, 50)
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
        textList.add(TextFormatting.GRAY + PneumaticCraftUtils.xlate("gui.tab.status.fluxCompressor.maxEnergyUsage"));
        textList.add(TextFormatting.BLACK.toString() + te.getInfoEnergyPerTick() + " FE/t");
        textList.add(TextFormatting.GRAY + PneumaticCraftUtils.xlate("gui.tab.status.fluxCompressor.maxInputRate"));
        textList.add(TextFormatting.BLACK.toString() + te.getInfoEnergyPerTick() * 2 + " FE/t");
        textList.add(TextFormatting.GRAY + PneumaticCraftUtils.xlate("gui.tab.status.fluxCompressor.storedEnergy"));
        textList.add(TextFormatting.BLACK.toString() + te.getInfoEnergyStored() + " FE");
        return textList;
    }

    @Override
    protected Point getGaugeLocation() {
        return getGaugeLocation(10, 0);
    }

    @Override
    protected void addPressureStatInfo(List<String> pressureStatText) {
        super.addPressureStatInfo(pressureStatText);
        pressureStatText.add(TextFormatting.BLACK + I18n.format("gui.tooltip.maxProduction", PneumaticCraftUtils.roundNumberTo(te.getAirRate(),2)));
    }

    @Override
    protected void addProblems(List<String> textList) {
        super.addProblems(textList);
        if (te.getInfoEnergyPerTick() > te.getInfoEnergyStored()) {
            textList.add(PneumaticCraftUtils.xlate("gui.tab.problems.fluxCompressor.noRF"));
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
