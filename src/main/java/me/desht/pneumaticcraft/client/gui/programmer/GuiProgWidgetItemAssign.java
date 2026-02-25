package me.desht.pneumaticcraft.client.gui.programmer;

import me.desht.pneumaticcraft.client.gui.GuiProgrammer;
import me.desht.pneumaticcraft.client.gui.widget.WidgetComboBox;
import me.desht.pneumaticcraft.client.gui.widget.WidgetLabel;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetItemAssign;

public class GuiProgWidgetItemAssign extends GuiProgWidgetOptionBase<ProgWidgetItemAssign> {
    private WidgetComboBox textfield;

    public GuiProgWidgetItemAssign(ProgWidgetItemAssign widget, GuiProgrammer guiProgrammer) {
        super(widget, guiProgrammer);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.textfield = new WidgetComboBox(this.fontRenderer, this.guiLeft + 10, this.guiTop + 40, 160, 10);
        this.textfield.setElements(this.guiProgrammer.te.getAllVariables());
        this.textfield.setMaxStringLength(1000);
        this.textfield.setText(this.widget.getVariable());
        this.addWidget(this.textfield);

        this.addWidget(new WidgetLabel(this.guiLeft + 10, this.guiTop + 30, "Setting variable:"));
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        this.widget.setVariable(this.textfield.getText());
    }
}
