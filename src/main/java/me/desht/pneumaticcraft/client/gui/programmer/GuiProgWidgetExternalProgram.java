package me.desht.pneumaticcraft.client.gui.programmer;

import me.desht.pneumaticcraft.client.gui.GuiProgrammer;
import me.desht.pneumaticcraft.client.gui.widget.GuiCheckBox;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetExternalProgram;
import net.minecraft.client.resources.I18n;

import java.io.IOException;

public class GuiProgWidgetExternalProgram extends GuiProgWidgetAreaShow<ProgWidgetExternalProgram> {

    private GuiCheckBox shareVariables;

    public GuiProgWidgetExternalProgram(ProgWidgetExternalProgram widget, GuiProgrammer guiProgrammer) {
        super(widget, guiProgrammer);
    }

    @Override
    public void initGui() {
        super.initGui();

        this.shareVariables = new GuiCheckBox(-1, this.guiLeft + 10, this.guiTop + 22, 0xFF404040, I18n.format("gui.progWidget.externalProgram.shareVariables"));
        this.addWidget(this.shareVariables);
        this.shareVariables.setTooltip(I18n.format("gui.progWidget.externalProgram.shareVariables.tooltip"));
        this.shareVariables.setChecked(this.widget.shareVariables);
    }

    @Override
    public void keyTyped(char chr, int keyCode) throws IOException {
        if (keyCode == 1) {
            this.widget.shareVariables = this.shareVariables.checked;
        }
        super.keyTyped(chr, keyCode);
    }
}
