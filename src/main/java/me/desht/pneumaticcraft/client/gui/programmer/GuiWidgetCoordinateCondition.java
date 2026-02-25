package me.desht.pneumaticcraft.client.gui.programmer;

import me.desht.pneumaticcraft.client.gui.GuiProgrammer;
import me.desht.pneumaticcraft.client.gui.widget.GuiCheckBox;
import me.desht.pneumaticcraft.client.gui.widget.GuiRadioButton;
import me.desht.pneumaticcraft.client.gui.widget.IGuiWidget;
import me.desht.pneumaticcraft.common.progwidgets.ICondition;
import me.desht.pneumaticcraft.common.progwidgets.ICondition.Operator;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetCoordinateCondition;

import java.util.ArrayList;
import java.util.List;

public class GuiWidgetCoordinateCondition extends GuiProgWidgetOptionBase<ProgWidgetCoordinateCondition> {
    private final GuiCheckBox[] checkingAxis = new GuiCheckBox[3];
    private List<GuiRadioButton> radioButtons;

    public GuiWidgetCoordinateCondition(ProgWidgetCoordinateCondition widget, GuiProgrammer guiProgrammer) {
        super(widget, guiProgrammer);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.checkingAxis[0] = new GuiCheckBox(0, this.guiLeft + 10, this.guiTop + 30, 0xFF404040, "X");
        this.checkingAxis[1] = new GuiCheckBox(1, this.guiLeft + 10, this.guiTop + 42, 0xFF404040, "Y");
        this.checkingAxis[2] = new GuiCheckBox(2, this.guiLeft + 10, this.guiTop + 54, 0xFF404040, "Z");
        for (int i = 0; i < 3; i++) {
            this.checkingAxis[i] = new GuiCheckBox(i, this.guiLeft + 10, this.guiTop + 30 + i * 12, 0xFF404040, i == 0 ? "X" : i == 1 ? "Y" : "Z");
            this.addWidget(this.checkingAxis[i]);
            this.checkingAxis[i].setChecked(this.widget.checkingAxis[i]);
        }

        this.radioButtons = new ArrayList<>();
        for (int i = 0; i < ICondition.Operator.values().length; i++) {
            GuiRadioButton radioButton = new GuiRadioButton(3 + i, this.guiLeft + 80, this.guiTop + 30 + i * 12, 0xFF404040, ICondition.Operator.values()[i].toString());
            radioButton.checked = this.widget.getOperator().ordinal() == i;
            this.addWidget(radioButton);
            this.radioButtons.add(radioButton);
            radioButton.otherChoices = this.radioButtons;
        }
    }

    @Override
    public void actionPerformed(IGuiWidget w) {
        super.actionPerformed(w);
        for (int i = 0; i < 3; i++) {
            this.widget.checkingAxis[i] = this.checkingAxis[i].checked;
        }
        for (int i = 0; i < ICondition.Operator.values().length; i++) {
            if (this.radioButtons.get(i).checked) this.widget.setOperator(Operator.values()[i]);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        String condition = this.widget.getCondition();
        this.fontRenderer.drawString(condition, this.width / 2 - this.fontRenderer.getStringWidth(condition) / 2, this.guiTop + 70, 0xFF404060);
    }
}
