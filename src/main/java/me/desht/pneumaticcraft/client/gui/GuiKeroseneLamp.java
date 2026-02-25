package me.desht.pneumaticcraft.client.gui;

import me.desht.pneumaticcraft.client.gui.widget.WidgetLabel;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTank;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTextFieldNumber;
import me.desht.pneumaticcraft.common.inventory.ContainerKeroseneLamp;
import me.desht.pneumaticcraft.common.tileentity.TileEntityKeroseneLamp;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.List;

public class GuiKeroseneLamp extends GuiPneumaticContainerBase<TileEntityKeroseneLamp> {
    private WidgetLabel rangeTextWidget;
    //   private WidgetLabel timeLeftWidget;
    private WidgetTextFieldNumber rangeWidget;

    public GuiKeroseneLamp(InventoryPlayer player, TileEntityKeroseneLamp te) {
        super(new ContainerKeroseneLamp(player, te), te, Textures.GUI_KEROSENE_LAMP);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.addWidget(new WidgetTank(-1, this.guiLeft + 152, this.guiTop + 15, this.te.getTank()));
        this.addWidget(this.rangeTextWidget = new WidgetLabel(this.guiLeft + 5, this.guiTop + 38, ""));
        //  addWidget(timeLeftWidget = new WidgetLabel(guiLeft + 5, guiTop + 26, ""));
        String maxRange = I18n.format("gui.keroseneLamp.maxRange");
        int maxRangeLength = this.fontRenderer.getStringWidth(maxRange);
        this.addLabel(maxRange, this.guiLeft + 5, this.guiTop + 50);
        this.addLabel(I18n.format("gui.keroseneLamp.blocks"), this.guiLeft + maxRangeLength + 40, this.guiTop + 50);
        this.addWidget(this.rangeWidget = new WidgetTextFieldNumber(this.fontRenderer, this.guiLeft + 7 + maxRangeLength, this.guiTop + 50, 30, this.fontRenderer.FONT_HEIGHT));
        this.rangeWidget.minValue = 1;
        this.rangeWidget.maxValue = TileEntityKeroseneLamp.MAX_RANGE;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (!this.rangeWidget.isFocused()) {
            this.rangeWidget.setValue(this.te.getTargetRange());
        }
        this.rangeTextWidget.text = I18n.format("gui.keroseneLamp.currentRange", this.te.getRange());

        /* if(te.getRange() > 0) {
             int ticksLeft = (int)(te.getTank().getFluidAmount() * TileEntityKeroseneLamp.FUEL_PER_MB * 5 / Math.pow(te.getRange(), 3));
             timeLeftWidget.text = I18n.format("gui.keroseneLamp.timeLeft", PneumaticCraftUtils.convertTicksToMinutesAndSeconds(ticksLeft, false));
         } else {
             timeLeftWidget.text = "";
         }*/
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (!this.rangeWidget.isFocused()) {
            this.sendPacketToServer(this.rangeWidget.getValue());
        }
    }

    @Override
    protected void addProblems(List<String> curInfo) {
        super.addProblems(curInfo);
        if (this.te.getTank().getFluidAmount() == 0) {
            curInfo.add("gui.tab.problems.keroseneLamp.noFuel");
        } else if (this.te.getFuelQuality() == 0) {
            curInfo.add("gui.tab.problems.keroseneLamp.badFuel");
        }
    }

    @Override
    protected void addWarnings(List<String> curInfo) {
        super.addWarnings(curInfo);
        if (this.te.getTank().getFluidAmount() < 30 && this.te.getTank().getFluidAmount() > 0) {
            curInfo.add("gui.tab.problems.keroseneLamp.lowFuel");
        }
    }

    @Override
    protected void keyTyped(char key, int keyCode) throws IOException {
        if ((keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_TAB) && this.rangeWidget.isFocused()) {
            this.sendPacketToServer(this.rangeWidget.getValue());
        } else {
            super.keyTyped(key, keyCode);
        }
    }
}
