package me.desht.pneumaticcraft.common.thirdparty.ic2;

import me.desht.pneumaticcraft.client.gui.GuiPneumaticContainerBase;
import me.desht.pneumaticcraft.client.gui.widget.GuiAnimatedStat;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTemperature;
import me.desht.pneumaticcraft.common.inventory.Container4UpgradeSlots;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class GuiPneumaticGenerator extends GuiPneumaticContainerBase<TileEntityPneumaticGenerator> {
    private GuiAnimatedStat outputStat;

    public GuiPneumaticGenerator(InventoryPlayer inventory, TileEntityPneumaticGenerator te) {
        super(new Container4UpgradeSlots(inventory, te), te, Textures.GUI_4UPGRADE_SLOTS);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.outputStat = this.addAnimatedStat("Output", IC2.glassFibreCable, 0xFF555555, false);
        this.addWidget(new WidgetTemperature(0, this.guiLeft + 87, this.guiTop + 20, 273, 675,
                this.te.getHeatExchangerLogic(null), 325, 625));
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int x, int y) {
        super.drawGuiContainerForegroundLayer(x, y);
        this.fontRenderer.drawString("Upgr.", 53, 19, 0xFF404040);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        this.outputStat.setText(this.getOutputStat());
    }

    private List<String> getOutputStat() {
        List<String> textList = new ArrayList<>();
        textList.add(TextFormatting.GRAY + "Output configuration");
        textList.add(TextFormatting.BLACK.toString() + this.te.getEnergyPacketSize() + " EU/tick");
        textList.add("\u00a77Currently producing:");
        textList.add("\u00a70" + this.te.curEnergyProduction + " EU/tick.");
        return textList;
    }

    @Override
    public void addProblems(List<String> curInfo) {
        super.addProblems(curInfo);
        if (this.te.getEfficiency() < 100) {
            curInfo.add(I18n.format("gui.tab.problems.advancedAirCompressor.efficiency", this.te.getEfficiency() + "%%"));
        }
        if (this.te.getPressure() < PneumaticValues.MIN_PRESSURE_PNEUMATIC_GENERATOR) {
            curInfo.add(I18n.format("gui.tab.problems.notEnoughPressure"));
            curInfo.add(I18n.format("gui.tab.problems.applyPressure", PneumaticValues.MIN_PRESSURE_PNEUMATIC_GENERATOR));
        }
    }
}
