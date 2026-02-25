package me.desht.pneumaticcraft.client.gui;

import me.desht.pneumaticcraft.client.gui.widget.WidgetTank;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTemperature;
import me.desht.pneumaticcraft.common.heat.HeatUtil;
import me.desht.pneumaticcraft.common.inventory.ContainerThermopneumaticProcessingPlant;
import me.desht.pneumaticcraft.common.tileentity.TileEntityThermopneumaticProcessingPlant;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.TextFormatting;

import java.awt.*;
import java.util.Collections;
import java.util.List;

public class GuiThermopneumaticProcessingPlant extends
        GuiPneumaticContainerBase<TileEntityThermopneumaticProcessingPlant> {
    private GuiButtonSpecial dumpButton;
    private GuiButtonSpecial moveButton;
    private WidgetTemperature tempWidget;
    private int nExposedFaces;

    public GuiThermopneumaticProcessingPlant(InventoryPlayer player, TileEntityThermopneumaticProcessingPlant te) {
        super(new ContainerThermopneumaticProcessingPlant(player, te), te, Textures.GUI_THERMOPNEUMATIC_PROCESSING_PLANT);
        this.ySize = 197;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.addWidget(new WidgetTank(-1, this.guiLeft + 13, this.guiTop + 15, this.te.getInputTank()));
        this.addWidget(new WidgetTank(-1, this.guiLeft + 79, this.guiTop + 15, this.te.getOutputTank()));
        int min, max;
        if (this.te.requiredTemperature == 0) {
            min = 273;
            max = 673;
        } else if (this.te.requiredTemperature < 0) {
            min = 0;
            max = 273;
        } else {
            min = 273;
            max = (int) this.te.requiredTemperature + 100;
        }
        this.tempWidget = new WidgetTemperature(-1, this.guiLeft + 98, this.guiTop + 15, min, max, this.te.getHeatExchangerLogic(null), (int) this.te.requiredTemperature) {
            @Override
            public void addTooltip(int mouseX, int mouseY, List<String> curTip, boolean shift) {
                super.addTooltip(mouseX, mouseY, curTip, shift);
                if (GuiThermopneumaticProcessingPlant.this.te.requiredTemperature > 0) {
                    TextFormatting tf = GuiThermopneumaticProcessingPlant.this.te.requiredTemperature < GuiThermopneumaticProcessingPlant.this.te.getHeatExchangerLogic(null).getTemperatureAsInt() ? TextFormatting.GREEN : TextFormatting.GOLD;
                    curTip.add(tf + "Required Temperature: " + (GuiThermopneumaticProcessingPlant.this.te.requiredTemperature - 273) + "\u00b0C");
                }
            }
        };

        this.addWidget(this.tempWidget);

        this.moveButton = new GuiButtonSpecial(1, this.guiLeft + 12, this.guiTop + 81, 18, 20, "");
        this.moveButton.setRenderedIcon(Textures.GUI_RIGHT_ARROW);
        this.moveButton.setTooltipText(PneumaticCraftUtils.convertStringIntoList(I18n.format("gui.thermopneumatic.moveInput")));
        this.addWidget(this.moveButton);

        this.dumpButton = new GuiButtonSpecial(2, this.guiLeft + 12, this.guiTop + 81, 18, 20, "");
        this.dumpButton.setRenderedIcon(Textures.GUI_X_BUTTON);
        this.dumpButton.setTooltipText(PneumaticCraftUtils.convertStringIntoList(I18n.format("gui.thermopneumatic.dumpInput")));
        this.dumpButton.setVisible(false);
        this.addWidget(this.dumpButton);

        this.nExposedFaces = HeatUtil.countExposedFaces(Collections.singletonList(this.te));
    }

    @Override
    public void updateScreen() {
        this.tempWidget.setScales((int) this.te.requiredTemperature);
        super.updateScreen();
        boolean shift = GuiScreen.isShiftKeyDown();
        this.dumpButton.setVisible(shift);
        this.dumpButton.visible = shift;
        this.moveButton.setVisible(!shift);
        this.moveButton.visible = !shift;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int x, int y) {
        super.drawGuiContainerBackgroundLayer(partialTicks, x, y);
        double progress = this.te.getCraftingPercentage();
        int progressWidth = (int) (progress * 48);
        this.bindGuiTexture();
        this.drawTexturedModalRect(this.guiLeft + 30, this.guiTop + 31, this.xSize, 0, progressWidth, 22);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int x, int y) {
        this.fontRenderer.drawString(I18n.format("gui.tab.upgrades"), 91, 83, 4210752);
        String containerName = I18n.format(this.te.getName() + ".name");
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.95, 0.97, 1);
        this.fontRenderer.drawString(containerName, this.xSize / 2 - this.fontRenderer.getStringWidth(containerName) / 2 + 1, 5, 4210752);
        GlStateManager.popMatrix();
        super.drawGuiContainerForegroundLayer(x, y);

    }

    @Override
    protected Point getInvNameOffset() {
        return null;
    }

    @Override
    protected Point getGaugeLocation() {
        int xStart = (this.width - this.xSize) / 2;
        int yStart = (this.height - this.ySize) / 2;
        return new Point(xStart + this.xSize * 3 / 4 + 10, yStart + this.ySize / 4);
    }

    @Override
    public void addProblems(List<String> curInfo) {
        super.addProblems(curInfo);

        if (!this.te.hasRecipe) {
            curInfo.add("gui.tab.problems.thermopneumaticProcessingPlant.noSufficientIngredients");
        } else if (this.te.getHeatExchangerLogic(null).getTemperatureAsInt() < this.te.requiredTemperature) {
            curInfo.add("gui.tab.problems.notEnoughHeat");
        }
    }

    @Override
    protected void addWarnings(List<String> curInfo) {
        super.addWarnings(curInfo);

        if (this.nExposedFaces > 0) {
            curInfo.add(I18n.format("gui.tab.problems.exposedFaces", this.nExposedFaces, 6));
        }
    }
}
