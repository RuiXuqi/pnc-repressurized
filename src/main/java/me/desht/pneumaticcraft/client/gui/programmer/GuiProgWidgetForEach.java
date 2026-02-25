package me.desht.pneumaticcraft.client.gui.programmer;

import me.desht.pneumaticcraft.client.gui.GuiProgrammer;
import me.desht.pneumaticcraft.client.gui.widget.WidgetComboBox;
import me.desht.pneumaticcraft.common.progwidgets.IProgWidget;
import me.desht.pneumaticcraft.common.progwidgets.IVariableSetWidget;
import net.minecraft.client.resources.I18n;

import java.io.IOException;

public class GuiProgWidgetForEach extends GuiProgWidgetAreaShow {

    private WidgetComboBox variableField;

    public GuiProgWidgetForEach(IProgWidget widget, GuiProgrammer guiProgrammer) {
        super(widget, guiProgrammer);
    }

    @Override
    public void initGui() {
        super.initGui();

        this.variableField = new WidgetComboBox(this.fontRenderer, this.guiLeft + 10, this.guiTop + 42, 160, this.fontRenderer.FONT_HEIGHT + 1);
        this.variableField.setElements(this.guiProgrammer.te.getAllVariables());
        this.addWidget(this.variableField);
        this.variableField.setText(((IVariableSetWidget) this.widget).getVariable());
        this.variableField.setFocused(true);
    }

    @Override
    public void keyTyped(char chr, int keyCode) throws IOException {
        if (keyCode == 1) {
            ((IVariableSetWidget) this.widget).setVariable(this.variableField.getText());
        }
        super.keyTyped(chr, keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.fontRenderer.drawString(I18n.format("gui.progWidget.coordinate.variableName"), this.guiLeft + 10, this.guiTop + 30, 0xFF000000);
    }
}
