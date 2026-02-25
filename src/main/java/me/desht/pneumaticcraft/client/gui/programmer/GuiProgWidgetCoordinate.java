package me.desht.pneumaticcraft.client.gui.programmer;

import me.desht.pneumaticcraft.api.item.IPositionProvider;
import me.desht.pneumaticcraft.client.gui.GuiButtonSpecial;
import me.desht.pneumaticcraft.client.gui.GuiInventorySearcher;
import me.desht.pneumaticcraft.client.gui.GuiProgrammer;
import me.desht.pneumaticcraft.client.gui.widget.*;
import me.desht.pneumaticcraft.common.item.ItemGPSTool;
import me.desht.pneumaticcraft.common.item.Itemss;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetCoordinate;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.client.FMLClientHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiProgWidgetCoordinate extends GuiProgWidgetAreaShow<ProgWidgetCoordinate> {
    private GuiInventorySearcher invSearchGui;
    private WidgetTextFieldNumber[] coordFields;
    private WidgetComboBox variableField;
    private GuiButtonSpecial gpsButton;

    public GuiProgWidgetCoordinate(ProgWidgetCoordinate widget, GuiProgrammer guiProgrammer) {
        super(widget, guiProgrammer);
    }

    @Override
    public void initGui() {
        super.initGui();

        if (this.invSearchGui != null) {
            BlockPos pos = !this.invSearchGui.getSearchStack().isEmpty() ? ItemGPSTool.getGPSLocation(this.invSearchGui.getSearchStack()) : null;
            this.widget.setCoordinate(pos);
        }

        List<GuiRadioButton> radioButtons = new ArrayList<>();
        GuiRadioButton radioButton = new GuiRadioButton(0, this.guiLeft + 7, this.guiTop + 51, 0xFF404040, I18n.format("gui.progWidget.coordinate.constant"));
        if (!this.widget.isUsingVariable()) radioButton.checked = true;
        radioButtons.add(radioButton);
        radioButton.otherChoices = radioButtons;
        this.addWidget(radioButton);
        radioButton = new GuiRadioButton(1, this.guiLeft + 7, this.guiTop + 100, 0xFF404040, I18n.format("gui.progWidget.coordinate.variable"));
        if (this.widget.isUsingVariable()) radioButton.checked = true;
        radioButtons.add(radioButton);
        radioButton.otherChoices = radioButtons;
        this.addWidget(radioButton);

        this.gpsButton = new GuiButtonSpecial(0, this.guiLeft + 100, this.guiTop + 20, 20, 20, "");
        this.gpsButton.setRenderStacks(new ItemStack(Itemss.GPS_TOOL));
        this.gpsButton.setTooltipText(I18n.format("gui.progWidget.coordinate.selectFromGPS"));
        this.gpsButton.enabled = !this.widget.isUsingVariable();
        this.buttonList.add(this.gpsButton);
        this.coordFields = new WidgetTextFieldNumber[3];
        for (int i = 0; i < 3; i++) {
            this.coordFields[i] = new WidgetTextFieldNumber(this.fontRenderer, this.guiLeft + 100, this.guiTop + 50 + 13 * i, 40, this.fontRenderer.FONT_HEIGHT + 1);
            this.addWidget(this.coordFields[i]);
            this.coordFields[i].setEnabled(this.gpsButton.enabled);
        }
        this.coordFields[0].setValue(this.widget.getRawCoordinate().getX());
        this.coordFields[1].setValue(this.widget.getRawCoordinate().getY());
        this.coordFields[2].setValue(this.widget.getRawCoordinate().getZ());

        this.variableField = new WidgetComboBox(this.fontRenderer, this.guiLeft + 90, this.guiTop + 112, 80, this.fontRenderer.FONT_HEIGHT + 1);
        this.variableField.setElements(this.guiProgrammer.te.getAllVariables());
        this.addWidget(this.variableField);
        this.variableField.setText(this.widget.getVariable());
        this.variableField.setEnabled(this.widget.isUsingVariable());
    }

    @Override
    public void actionPerformed(IGuiWidget guiWidget) {
        if (guiWidget.getID() == 0 || guiWidget.getID() == 1) {
            this.widget.setUsingVariable(guiWidget.getID() == 1);
            this.gpsButton.enabled = guiWidget.getID() == 0;
            for (WidgetTextField textField : this.coordFields) {
                textField.setEnabled(this.gpsButton.enabled);
            }

            this.variableField.setEnabled(!this.gpsButton.enabled);
        }
        // super.actionPerformed(guiWidget);
    }

    @Override
    public void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            this.invSearchGui = new GuiInventorySearcher(FMLClientHandler.instance().getClient().player);
            this.invSearchGui.setStackPredicate(itemStack -> itemStack.getItem() instanceof IPositionProvider);
            BlockPos area = this.widget.getRawCoordinate();
            ItemStack gps = new ItemStack(Itemss.GPS_TOOL);
            ItemGPSTool.setGPSLocation(gps, area);
            this.invSearchGui.setSearchStack(ItemGPSTool.getGPSLocation(gps) != null ? gps : ItemStack.EMPTY);
            FMLClientHandler.instance().showGuiScreen(this.invSearchGui);
        }
        super.actionPerformed(button);
    }

    @Override
    public void keyTyped(char chr, int keyCode) throws IOException {
        if (keyCode == 1) {
            this.widget.setCoordinate(new BlockPos(this.coordFields[0].getValue(), this.coordFields[1].getValue(), this.coordFields[2].getValue()));
            this.widget.setVariable(this.variableField.getText());
        }
        super.keyTyped(chr, keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.fontRenderer.drawString("x:", this.guiLeft + 90, this.guiTop + 51, 0xFF404040);
        this.fontRenderer.drawString("y:", this.guiLeft + 90, this.guiTop + 64, 0xFF404040);
        this.fontRenderer.drawString("z:", this.guiLeft + 90, this.guiTop + 77, 0xFF404040);
        this.fontRenderer.drawString(I18n.format("gui.progWidget.coordinate.variableName"), this.guiLeft + 90, this.guiTop + 100, 0xFF404060);
    }
}
