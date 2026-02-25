package me.desht.pneumaticcraft.client.gui.remote;

import me.desht.pneumaticcraft.client.gui.GuiPneumaticScreenBase;
import me.desht.pneumaticcraft.client.gui.GuiRemoteEditor;
import me.desht.pneumaticcraft.client.gui.widget.WidgetComboBox;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTextField;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTextFieldNumber;
import me.desht.pneumaticcraft.common.inventory.ContainerRemote;
import me.desht.pneumaticcraft.common.remote.ActionWidget;
import me.desht.pneumaticcraft.common.remote.IActionWidgetLabeled;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;

public class GuiRemoteOptionBase<Widget extends ActionWidget> extends GuiPneumaticScreenBase {
    protected final Widget widget;
    protected final GuiRemoteEditor guiRemote;
    private WidgetTextField labelField, tooltipField;
    private WidgetComboBox enableField;
    private WidgetTextFieldNumber xValueField, yValueField, zValueField;

    public GuiRemoteOptionBase(Widget widget, GuiRemoteEditor guiRemote) {
        this.widget = widget;
        this.guiRemote = guiRemote;
        this.xSize = 183;
        this.ySize = 202;
    }

    @Override
    public void keyTyped(char key, int keyCode) throws IOException {
        if (keyCode == 1) {
            this.onGuiClosed();
            this.mc.displayGuiScreen(this.guiRemote);
        } else {
            super.keyTyped(key, keyCode);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);

    }

    @Override
    protected ResourceLocation getTexture() {
        return Textures.GUI_WIDGET_OPTIONS;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void initGui() {
        super.initGui();

        String title = I18n.format("remote." + this.widget.getId() + ".name");
        this.addLabel(I18n.format("gui.remote.enable"), this.guiLeft + 10, this.guiTop + 150);
        this.addLabel(title, this.width / 2 - this.fontRenderer.getStringWidth(title) / 2, this.guiTop + 5);
        this.addLabel("#", this.guiLeft + 10, this.guiTop + 161);

        if (this.widget instanceof IActionWidgetLabeled) {
            this.addLabel(I18n.format("gui.remote.text"), this.guiLeft + 10, this.guiTop + 20);
            this.addLabel(I18n.format("gui.remote.tooltip"), this.guiLeft + 10, this.guiTop + 46);
        }

        this.addLabel(I18n.format("gui.remote.enableValue"), this.guiLeft + 10, this.guiTop + 175);
        this.addLabel("X:", this.guiLeft + 10, this.guiTop + 186);
        this.addLabel("Y:", this.guiLeft + 67, this.guiTop + 186);
        this.addLabel("Z:", this.guiLeft + 124, this.guiTop + 186);

        this.enableField = new WidgetComboBox(this.fontRenderer, this.guiLeft + 18, this.guiTop + 160, 152, 10);
        this.enableField.setElements(((ContainerRemote) this.guiRemote.inventorySlots).variables);
        this.enableField.setText(this.widget.getEnableVariable());
        this.enableField.setTooltip(I18n.format("gui.remote.enable.tooltip"));
        this.addWidget(this.enableField);

        String valueTooltip = I18n.format("gui.remote.enableValue.tooltip");

        this.xValueField = new WidgetTextFieldNumber(this.fontRenderer, this.guiLeft + 20, this.guiTop + 185, 38, 10);
        this.xValueField.setValue(this.widget.getEnablingValue().getX());
        this.xValueField.setTooltip(valueTooltip);
        this.addWidget(this.xValueField);

        this.yValueField = new WidgetTextFieldNumber(this.fontRenderer, this.guiLeft + 78, this.guiTop + 185, 38, 10);
        this.yValueField.setValue(this.widget.getEnablingValue().getY());
        this.yValueField.setTooltip(valueTooltip);
        this.addWidget(this.yValueField);

        this.zValueField = new WidgetTextFieldNumber(this.fontRenderer, this.guiLeft + 136, this.guiTop + 185, 38, 10);
        this.zValueField.setValue(this.widget.getEnablingValue().getZ());
        this.zValueField.setTooltip(valueTooltip);
        this.addWidget(this.zValueField);

        if (this.widget instanceof IActionWidgetLabeled) {
            this.labelField = new WidgetTextField(this.fontRenderer, this.guiLeft + 10, this.guiTop + 30, 160, 10);
            this.labelField.setText(((IActionWidgetLabeled) this.widget).getText());
            this.labelField.setTooltip(I18n.format("gui.remote.label.tooltip"));
            this.labelField.setMaxStringLength(1000);
            this.addWidget(this.labelField);

            this.tooltipField = new WidgetTextField(this.fontRenderer, this.guiLeft + 10, this.guiTop + 56, 160, 10);
            this.tooltipField.setText(((IActionWidgetLabeled) this.widget).getTooltip());
            this.addWidget(this.tooltipField);
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        this.widget.setEnableVariable(this.enableField.getText());
        this.widget.setEnablingValue(this.xValueField.getValue(), this.yValueField.getValue(), this.zValueField.getValue());
        if (this.widget instanceof IActionWidgetLabeled) {
            ((IActionWidgetLabeled) this.widget).setText(this.labelField.getText());
            ((IActionWidgetLabeled) this.widget).setTooltip(this.tooltipField.getText());
        }
    }
}
