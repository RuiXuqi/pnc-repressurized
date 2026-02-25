package me.desht.pneumaticcraft.client.gui.remote;

import me.desht.pneumaticcraft.client.gui.GuiRemoteEditor;
import me.desht.pneumaticcraft.client.gui.widget.GuiCheckBox;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTextField;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTextFieldNumber;
import me.desht.pneumaticcraft.common.remote.ActionWidgetDropdown;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;

public class GuiRemoteDropdown extends GuiRemoteVariable<ActionWidgetDropdown> {
    private WidgetTextField dropDownElementsField;
    private WidgetTextFieldNumber widthField;
    private GuiCheckBox sortCheckBox;

    public GuiRemoteDropdown(ActionWidgetDropdown widget, GuiRemoteEditor guiRemote) {
        super(widget, guiRemote);
    }

    @Override
    public void initGui() {
        super.initGui();

        Keyboard.enableRepeatEvents(true);

        this.addLabel(I18n.format("gui.remote.button.width"), this.guiLeft + 10, this.guiTop + 100);
        this.addLabel(I18n.format("gui.remote.dropdown.dropDownElements"), this.guiLeft + 10, this.guiTop + 40);

        this.dropDownElementsField = new WidgetTextField(this.fontRenderer, this.guiLeft + 10, this.guiTop + 50, 160, 10);
        this.dropDownElementsField.setMaxStringLength(32768);
        this.dropDownElementsField.setText(this.widget.getDropDownElements());
        this.dropDownElementsField.setTooltip(I18n.format("gui.remote.dropdown.dropDownElements.tooltip"));
        this.addWidget(this.dropDownElementsField);

        this.widthField = new WidgetTextFieldNumber(this.fontRenderer, this.guiLeft + 50, this.guiTop + 99, 30, 10);
        this.widthField.setValue(this.widget.getWidth());
        this.widthField.minValue = 10;
        this.addWidget(this.widthField);

        this.sortCheckBox = new GuiCheckBox(1, this.guiLeft + 10, this.guiTop + 120, 0x404040, I18n.format("gui.remote.dropdown.sort"));
        this.sortCheckBox.checked = this.widget.getSorted();
        this.sortCheckBox.setTooltip(I18n.format("gui.remote.dropdown.sort.tooltip"));
        this.addWidget(this.sortCheckBox);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();

        Keyboard.enableRepeatEvents(false);
        this.widget.setDropDownElements(this.dropDownElementsField.getText());
        this.widget.setWidth(this.widthField.getValue());
        this.widget.setSorted(this.sortCheckBox.checked);
    }
}
