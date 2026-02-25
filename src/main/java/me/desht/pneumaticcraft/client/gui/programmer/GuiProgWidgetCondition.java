package me.desht.pneumaticcraft.client.gui.programmer;

import me.desht.pneumaticcraft.client.gui.GuiProgrammer;
import me.desht.pneumaticcraft.client.gui.widget.GuiCheckBox;
import me.desht.pneumaticcraft.client.gui.widget.GuiRadioButton;
import me.desht.pneumaticcraft.client.gui.widget.IGuiWidget;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTextField;
import me.desht.pneumaticcraft.common.progwidgets.ICondition;
import me.desht.pneumaticcraft.common.progwidgets.ISidedWidget;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidget;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.util.EnumFacing;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.ArrayList;
import java.util.List;

public class GuiProgWidgetCondition extends GuiProgWidgetAreaShow<ProgWidget> {

    private WidgetTextField textField;

    public GuiProgWidgetCondition(ProgWidget widget, GuiProgrammer guiProgrammer) {
        super(widget, guiProgrammer);
    }

    @Override
    public void initGui() {
        super.initGui();

        if (this.isSidedWidget()) {
            for (int i = 0; i < 6; i++) {
                String sideName = PneumaticCraftUtils.getOrientationName(EnumFacing.byIndex(i));
                GuiCheckBox checkBox = new GuiCheckBox(i, this.guiLeft + 4, this.guiTop + 30 + i * 12, 0xFF404040, sideName);
                checkBox.checked = ((ISidedWidget) this.widget).getSides()[i];
                this.addWidget(checkBox);
            }
        }

        int baseX = this.isSidedWidget() ? 90 : 4;
        int baseY = this.isUsingAndOr() ? 60 : 30;

        List<GuiRadioButton> radioButtons;
        GuiRadioButton radioButton;
        if (this.isUsingAndOr()) {
            radioButtons = new ArrayList<>();
            radioButton = new GuiRadioButton(6, this.guiLeft + baseX, this.guiTop + 30, 0xFF404040, "Any block");
            radioButton.checked = !((ICondition) this.widget).isAndFunction();
            this.addWidget(radioButton);
            radioButtons.add(radioButton);
            radioButton.otherChoices = radioButtons;

            radioButton = new GuiRadioButton(7, this.guiLeft + baseX, this.guiTop + 42, 0xFF404040, "All blocks");
            radioButton.checked = ((ICondition) this.widget).isAndFunction();
            this.addWidget(radioButton);
            radioButtons.add(radioButton);
            radioButton.otherChoices = radioButtons;
        }

        if (this.requiresNumber()) {
            radioButtons = new ArrayList<>();
            for (int i = 0; i < ICondition.Operator.values().length; i++) {
                radioButton = new GuiRadioButton(8 + i, this.guiLeft + baseX, this.guiTop + baseY + i * 12, 0xFF404040, ICondition.Operator.values()[i].toString());
                radioButton.checked = ((ICondition) this.widget).getOperator().ordinal() == i;
                this.addWidget(radioButton);
                radioButtons.add(radioButton);
                radioButton.otherChoices = radioButtons;
            }

            this.textField = new WidgetTextField(Minecraft.getMinecraft().fontRenderer, this.guiLeft + baseX, this.guiTop + baseY + 40, 50, 11);
            this.textField.setText(((ICondition) this.widget).getRequiredCount() + "");
            this.textField.setFocused(true);
            this.addWidget(this.textField);
        }
    }

    protected boolean isSidedWidget() {
        return this.widget instanceof ISidedWidget;
    }

    protected boolean isUsingAndOr() {
        return true;
    }

    protected boolean requiresNumber() {
        return true;
    }

    @Override
    public void actionPerformed(IGuiWidget checkBox) {
        if (!(checkBox instanceof GuiLabel)) {
            if (checkBox.getID() < 6) {
                ((ISidedWidget) this.widget).getSides()[checkBox.getID()] = ((GuiCheckBox) checkBox).checked;
            } else {
                switch (checkBox.getID()) {
                    case 6:
                        ((ICondition) this.widget).setAndFunction(false);
                        break;
                    case 7:
                        ((ICondition) this.widget).setAndFunction(true);
                        break;
                    default:
                        ((ICondition) this.widget).setOperator(ICondition.Operator.values()[checkBox.getID() - 8]);
                }
            }
        }
        super.actionPerformed(checkBox);
    }

    @Override
    public void onKeyTyped(IGuiWidget widget) {
        if (this.requiresNumber()) {
            ((ICondition) this.widget).setRequiredCount(NumberUtils.toInt(this.textField.getText()));
        }
        super.onKeyTyped(widget);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (this.isSidedWidget()) this.fontRenderer.drawString("Accessing sides:", this.guiLeft + 4, this.guiTop + 20, 0xFF404060);
        this.fontRenderer.drawString(this.widget.getExtraStringInfo(), this.guiLeft + this.xSize / 2 - this.fontRenderer.getStringWidth(this.widget.getExtraStringInfo()) / 2, this.guiTop + 120, 0xFF404060);
    }

}
