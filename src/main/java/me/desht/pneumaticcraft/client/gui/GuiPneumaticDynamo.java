package me.desht.pneumaticcraft.client.gui;

import me.desht.pneumaticcraft.api.recipe.TemperatureRange;
import me.desht.pneumaticcraft.client.gui.widget.GuiAnimatedStat;
import me.desht.pneumaticcraft.client.gui.widget.WidgetEnergy;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTemperature;
import me.desht.pneumaticcraft.common.inventory.ContainerEnergy;
import me.desht.pneumaticcraft.common.tileentity.TileEntityPneumaticDynamo;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GuiPneumaticDynamo extends GuiPneumaticContainerBase<TileEntityPneumaticDynamo> {
    private GuiAnimatedStat inputStat;
    private WidgetTemperature tempWidget;

    public GuiPneumaticDynamo(InventoryPlayer inventoryPlayer, TileEntityPneumaticDynamo te) {
        super(new ContainerEnergy(inventoryPlayer, te), te, Textures.GUI_4UPGRADE_SLOTS);
    }

    @Override
    public void initGui() {
        super.initGui();
        inputStat = addAnimatedStat(PneumaticCraftUtils.xlate("gui.tab.output"), Textures.GUI_BUILDCRAFT_ENERGY, 0xFF555555, false);
        IEnergyStorage storage = te.getCapability(CapabilityEnergy.ENERGY, null);

        addWidget(new WidgetEnergy(guiLeft + 20, guiTop + 20, storage));
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
        textList.add(TextFormatting.GRAY + I18n.format("gui.tab.status.pneumaticDynamo.maxEnergyProduction"));
        textList.add(TextFormatting.BLACK.toString() + te.getRFRate() + " FE/t");
        textList.add(TextFormatting.GRAY + I18n.format("gui.tab.status.pneumaticDynamo.maxOutputRate"));
        textList.add(TextFormatting.BLACK.toString() + te.getRFRate() * 2 + " FE/t");
        textList.add(TextFormatting.GRAY + I18n.format("gui.tab.status.fluxCompressor.storedEnergy"));
        textList.add(TextFormatting.BLACK.toString() + te.getInfoEnergyStored() + " FE");
        return textList;
    }

    @Override
    protected Point getGaugeLocation() {
        return super.getGaugeLocation(10, 0);
    }

    @Override
    protected void addPressureStatInfo(List<String> pressureStatText) {
        super.addPressureStatInfo(pressureStatText);
        pressureStatText.add(TextFormatting.BLACK + I18n.format("gui.tooltip.maxUsage", te.getAirRate()));
    }

    @Override
    public void addProblems(List<String> curInfo) {
        super.addProblems(curInfo);
        if (te.getEfficiency() < 100) {
            curInfo.add(I18n.format("gui.tab.problems.advancedAirCompressor.efficiency", te.getEfficiency() + "%%"));
        }
    }
}
