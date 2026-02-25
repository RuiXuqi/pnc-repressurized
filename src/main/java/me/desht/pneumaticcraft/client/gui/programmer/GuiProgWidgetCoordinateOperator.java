package me.desht.pneumaticcraft.client.gui.programmer;

import me.desht.pneumaticcraft.client.gui.GuiProgrammer;
import me.desht.pneumaticcraft.client.gui.widget.GuiRadioButton;
import me.desht.pneumaticcraft.client.gui.widget.IGuiWidget;
import me.desht.pneumaticcraft.client.gui.widget.WidgetComboBox;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetCoordinateOperator;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetCoordinateOperator.EnumOperator;
import net.minecraft.client.resources.I18n;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiProgWidgetCoordinateOperator extends GuiProgWidgetAreaShow<ProgWidgetCoordinateOperator> {

    private WidgetComboBox variableField;

    public GuiProgWidgetCoordinateOperator(ProgWidgetCoordinateOperator widget, GuiProgrammer guiProgrammer) {
        super(widget, guiProgrammer);
    }

    @Override
    public void initGui() {
        super.initGui();

        List<GuiRadioButton> radioButtons = new ArrayList<>();
        for (int i = 0; i < EnumOperator.values().length; i++) {
            String key = EnumOperator.values()[i].getTranslationKey();
            GuiRadioButton radioButton = new GuiRadioButton(i, this.guiLeft + 7, this.guiTop + 42 + 12 * i, 0xFF404040, I18n.format(key));
            radioButtons.add(radioButton);
            radioButton.checked = this.widget.getOperator().ordinal() == i;
            radioButton.otherChoices = radioButtons;
            radioButton.setTooltip(I18n.format(key + ".hint"));
            this.addWidget(radioButton);
        }

        this.variableField = new WidgetComboBox(this.fontRenderer, this.guiLeft + 7, this.guiTop + 100, 80, this.fontRenderer.FONT_HEIGHT + 1);
        this.variableField.setElements(this.guiProgrammer.te.getAllVariables());
        this.addWidget(this.variableField);
        this.variableField.setText(this.widget.getVariable());
    }

    @Override
    public void actionPerformed(IGuiWidget guiWidget) {
        if (guiWidget.getID() >= 0 && guiWidget.getID() < EnumOperator.values().length) {
            this.widget.setOperator(EnumOperator.values()[guiWidget.getID()]);
        }
        super.actionPerformed(guiWidget);
    }

    @Override
    public void keyTyped(char chr, int keyCode) throws IOException {
        if (keyCode == 1) {
            this.widget.setVariable(this.variableField.getText());
        }
        super.keyTyped(chr, keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.fontRenderer.drawString(I18n.format("gui.progWidget.coordinate.variableName"), this.guiLeft + 7, this.guiTop + 88, 0xFF404060);
        this.fontRenderer.drawString(I18n.format("gui.progWidget.coordinateOperator.operator"), this.guiLeft + 7, this.guiTop + 30, 0xFF404060);
    }
}
