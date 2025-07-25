package me.desht.pneumaticcraft.common.thirdparty.ic2;

import me.desht.pneumaticcraft.api.recipe.TemperatureRange;
import me.desht.pneumaticcraft.client.gui.GuiPneumaticContainerBase;
import me.desht.pneumaticcraft.client.gui.widget.GuiAnimatedStat;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTemperature;
import me.desht.pneumaticcraft.common.inventory.Container4UpgradeSlots;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class GuiPneumaticGenerator extends GuiPneumaticContainerBase<TileEntityPneumaticGenerator> {
    private GuiAnimatedStat outputStat;
    private WidgetTemperature tempWidget;

    public GuiPneumaticGenerator(InventoryPlayer inventory, TileEntityPneumaticGenerator te) {
        super(new Container4UpgradeSlots(inventory, te), te, Textures.GUI_4UPGRADE_SLOTS);
    }

    @Override
    public void initGui() {
        super.initGui();
        outputStat = addAnimatedStat(PneumaticCraftUtils.xlate("gui.tab.output"), IC2.glassFibreCable, 0xFF555555, false);

        addWidget(tempWidget = new WidgetTemperature(-1, guiLeft + 97, guiTop + 20, TemperatureRange.of(273, 673), 273, 50)
                .setOperatingRange(TemperatureRange.of(323, 625)).setShowOperatingRange(false));
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        outputStat.setText(getOutputStat());

        tempWidget.setTemperature(te.getHeatExchangerLogic(null).getTemperatureAsInt());
        tempWidget.autoScaleForTemperature();
    }

    private List<String> getOutputStat() {
        List<String> textList = new ArrayList<>();
        textList.add(TextFormatting.GRAY + PneumaticCraftUtils.xlate("gui.tab.status.pneumaticGenerator.maxEnergyProduction"));
        textList.add(TextFormatting.BLACK.toString() + te.curEnergyProduction + " EU/t");
        textList.add(TextFormatting.GRAY + PneumaticCraftUtils.xlate("gui.tab.status.pneumaticGenerator.maxOutputRate"));
        textList.add(TextFormatting.BLACK.toString() + te.getEnergyPacketSize() + " EU/t");
        return textList;
    }

    @Override
    protected Point getGaugeLocation() {
        return super.getGaugeLocation(10, 0);
    }

    @Override
    public void addProblems(List<String> curInfo) {
        super.addProblems(curInfo);
        if (te.getEfficiency() < 100) {
            curInfo.add(I18n.format("gui.tab.problems.advancedAirCompressor.efficiency", te.getEfficiency() + "%%"));
        }
        if (te.getPressure() < PneumaticValues.MIN_PRESSURE_PNEUMATIC_GENERATOR) {
            curInfo.add(I18n.format("gui.tab.problems.notEnoughPressure"));
            curInfo.add(I18n.format("gui.tab.problems.applyPressure", PneumaticValues.MIN_PRESSURE_PNEUMATIC_GENERATOR));
        }
    }
}
