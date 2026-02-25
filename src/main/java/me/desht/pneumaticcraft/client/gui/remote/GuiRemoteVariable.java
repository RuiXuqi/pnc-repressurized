package me.desht.pneumaticcraft.client.gui.remote;

import me.desht.pneumaticcraft.client.gui.GuiRemoteEditor;
import me.desht.pneumaticcraft.client.gui.widget.WidgetComboBox;
import me.desht.pneumaticcraft.common.inventory.ContainerRemote;
import me.desht.pneumaticcraft.common.remote.ActionWidgetVariable;
import net.minecraft.client.resources.I18n;

public class GuiRemoteVariable<Widget extends ActionWidgetVariable> extends GuiRemoteOptionBase<Widget> {

    private WidgetComboBox variableField;

    public GuiRemoteVariable(Widget widget, GuiRemoteEditor guiRemote) {
        super(widget, guiRemote);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.addLabel(I18n.format("gui.progWidget.coordinate.variableName"), this.guiLeft + 10, this.guiTop + 70);
        this.addLabel("#", this.guiLeft + 10, this.guiTop + 81);

        this.variableField = new WidgetComboBox(this.fontRenderer, this.guiLeft + 18, this.guiTop + 80, 152, 10);
        this.variableField.setElements(((ContainerRemote) this.guiRemote.inventorySlots).variables);
        this.variableField.setText(this.widget.getVariableName());
        this.variableField.setTooltip(I18n.format("gui.remote.variable.tooltip"));
        this.addWidget(this.variableField);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        this.widget.setVariableName(this.variableField.getText());
    }
}
