package me.desht.pneumaticcraft.client.gui.programmer;

import me.desht.pneumaticcraft.client.gui.GuiProgrammer;
import me.desht.pneumaticcraft.client.gui.widget.GuiRadioButton;
import me.desht.pneumaticcraft.client.gui.widget.IGuiWidget;
import me.desht.pneumaticcraft.common.progwidgets.IProgWidget;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetGoToLocation;

import java.util.ArrayList;
import java.util.List;

public class GuiProgWidgetGoto extends GuiProgWidgetAreaShow {

    public GuiProgWidgetGoto(IProgWidget widget, GuiProgrammer guiProgrammer) {
        super(widget, guiProgrammer);
    }

    @Override
    public void initGui() {
        super.initGui();

        List<GuiRadioButton> radioButtons = new ArrayList<>();
        GuiRadioButton radioButton = new GuiRadioButton(0, this.guiLeft + 4, this.guiTop + 44, 0xFF404040, "Done when arrived");
        radioButton.checked = !((ProgWidgetGoToLocation) this.widget).doneWhenDeparting;
        this.addWidget(radioButton);
        radioButtons.add(radioButton);
        radioButton.otherChoices = radioButtons;

        GuiRadioButton radioButton2 = new GuiRadioButton(1, this.guiLeft + 4, this.guiTop + 58, 0xFF404040, "Done when departing");
        radioButton2.checked = ((ProgWidgetGoToLocation) this.widget).doneWhenDeparting;
        this.addWidget(radioButton2);
        radioButtons.add(radioButton2);
        radioButton2.otherChoices = radioButtons;
    }

    @Override
    public void actionPerformed(IGuiWidget guiWidget) {
        if (guiWidget.getID() == 0 || guiWidget.getID() == 1) {
            ((ProgWidgetGoToLocation) this.widget).doneWhenDeparting = guiWidget.getID() == 1;
        }
        super.actionPerformed(guiWidget);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.fontRenderer.drawString("Move to the next puzzle piece", this.guiLeft + 8, this.guiTop + 20, 0xFF404060);
        this.fontRenderer.drawString("when arrived or right away?", this.guiLeft + 8, this.guiTop + 30, 0xFF404060);
    }

}
