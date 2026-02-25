package me.desht.pneumaticcraft.client.gui.programmer;

import me.desht.pneumaticcraft.client.gui.GuiProgrammer;
import me.desht.pneumaticcraft.client.gui.widget.GuiCheckBox;
import me.desht.pneumaticcraft.client.gui.widget.GuiRadioButton;
import me.desht.pneumaticcraft.client.gui.widget.IGuiWidget;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTextFieldNumber;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetDigAndPlace;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.List;

public class GuiProgWidgetDigAndPlace<Widget extends ProgWidgetDigAndPlace> extends GuiProgWidgetAreaShow<Widget> {

    private WidgetTextFieldNumber textField;

    public GuiProgWidgetDigAndPlace(Widget widget, GuiProgrammer guiProgrammer) {
        super(widget, guiProgrammer);
    }

    @Override
    public void initGui() {
        super.initGui();

        List<GuiRadioButton> radioButtons = new ArrayList<>();
        ProgWidgetDigAndPlace.EnumOrder[] orders = ProgWidgetDigAndPlace.EnumOrder.values();
        for (int i = 0; i < orders.length; i++) {
            GuiRadioButton radioButton = new GuiRadioButton(i, this.guiLeft + 4, this.guiTop + 30 + i * 12, 0xFF404040, orders[i].getLocalizedName());
            radioButton.checked = orders[i] == this.widget.getOrder();
            this.addWidget(radioButton);
            radioButtons.add(radioButton);
            radioButton.otherChoices = radioButtons;
        }

        GuiCheckBox useMaxActions = new GuiCheckBox(16, this.guiLeft + (this.moveActionsToSide() ? 54 : 4), this.guiTop + 115, 0xFF404040, I18n.format("gui.progWidget.digAndPlace.useMaxActions"));
        useMaxActions.setTooltip("gui.progWidget.digAndPlace.useMaxActions.tooltip");
        useMaxActions.checked = this.widget.useMaxActions();
        this.addWidget(useMaxActions);

        this.textField = new WidgetTextFieldNumber(Minecraft.getMinecraft().fontRenderer, this.guiLeft + (this.moveActionsToSide() ? 57 : 7), this.guiTop + 128, 50, 11);
        this.textField.setValue(this.widget.getMaxActions());
        this.textField.setVisible(useMaxActions.checked);
        this.addWidget(this.textField);
    }

    protected boolean moveActionsToSide() {
        return false;
    }

    @Override
    public void actionPerformed(IGuiWidget guiWidget) {
        if (guiWidget.getID() >= 0 && guiWidget.getID() < ProgWidgetDigAndPlace.EnumOrder.values().length)
            this.widget.setOrder(ProgWidgetDigAndPlace.EnumOrder.values()[guiWidget.getID()]);
        if (guiWidget.getID() == 16) {
            this.widget.setUseMaxActions(((GuiCheckBox) guiWidget).checked);
            this.textField.setVisible(this.widget.useMaxActions());
        }
        super.actionPerformed(guiWidget);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        this.widget.setMaxActions(this.textField.getValue());
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.fontRenderer.drawString(TextFormatting.UNDERLINE + "Order", this.guiLeft + 6, this.guiTop + 20, 0xFF404060);
    }

}
