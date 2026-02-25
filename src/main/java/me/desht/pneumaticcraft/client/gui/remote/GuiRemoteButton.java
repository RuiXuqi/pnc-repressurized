package me.desht.pneumaticcraft.client.gui.remote;

import me.desht.pneumaticcraft.client.gui.GuiRemoteEditor;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTextFieldNumber;
import me.desht.pneumaticcraft.common.remote.ActionWidgetButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;

public class GuiRemoteButton extends GuiRemoteVariable<ActionWidgetButton> {
    private WidgetTextFieldNumber widthField;
    private WidgetTextFieldNumber heightField;
    private WidgetTextFieldNumber xValueField, yValueField, zValueField;

    public GuiRemoteButton(ActionWidgetButton widget, GuiRemoteEditor guiRemote) {
        super(widget, guiRemote);
    }

    @Override
    public void initGui() {
        super.initGui();

        this.addLabel(I18n.format("gui.remote.button.settingValue"), this.guiLeft + 10, this.guiTop + 95);
        this.addLabel("X:", this.guiLeft + 10, this.guiTop + 106);
        this.addLabel("Y:", this.guiLeft + 67, this.guiTop + 106);
        this.addLabel("Z:", this.guiLeft + 124, this.guiTop + 106);
        this.addLabel(I18n.format("gui.remote.button.width"), this.guiLeft + 10, this.guiTop + 123);
        this.addLabel(I18n.format("gui.remote.button.height"), this.guiLeft + 10, this.guiTop + 138);

        String valueTooltip = I18n.format("gui.remote.button.value.tooltip");

        this.xValueField = new WidgetTextFieldNumber(this.fontRenderer, this.guiLeft + 20, this.guiTop + 105, 38, 10);
        this.xValueField.setValue(this.widget.settingCoordinate.getX());
        this.xValueField.setTooltip(valueTooltip);
        this.addWidget(this.xValueField);

        this.yValueField = new WidgetTextFieldNumber(this.fontRenderer, this.guiLeft + 78, this.guiTop + 105, 38, 10);
        this.yValueField.setValue(this.widget.settingCoordinate.getY());
        this.yValueField.setTooltip(valueTooltip);
        this.addWidget(this.yValueField);

        this.zValueField = new WidgetTextFieldNumber(this.fontRenderer, this.guiLeft + 136, this.guiTop + 105, 38, 10);
        this.zValueField.setValue(this.widget.settingCoordinate.getZ());
        this.zValueField.setTooltip(valueTooltip);
        this.addWidget(this.zValueField);

        this.widthField = new WidgetTextFieldNumber(this.fontRenderer, this.guiLeft + 100, this.guiTop + 123, 60, 10);
        this.widthField.setValue(this.widget.getWidth());
        this.widthField.minValue = 10;
        this.addWidget(this.widthField);

        this.heightField = new WidgetTextFieldNumber(this.fontRenderer, this.guiLeft + 100, this.guiTop + 138, 60, 10);
        this.heightField.setValue(this.widget.getHeight());
        this.heightField.minValue = 10;
        this.heightField.maxValue = 100;
        this.addWidget(this.heightField);

    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        this.widget.settingCoordinate = new BlockPos(this.xValueField.getValue(), this.yValueField.getValue(), this.zValueField.getValue());
        this.widget.setWidth(this.widthField.getValue());
        this.widget.setHeight(this.heightField.getValue());
    }
}
