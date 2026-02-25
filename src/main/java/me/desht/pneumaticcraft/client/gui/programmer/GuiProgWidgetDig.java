package me.desht.pneumaticcraft.client.gui.programmer;

import me.desht.pneumaticcraft.client.gui.GuiProgrammer;
import me.desht.pneumaticcraft.client.gui.widget.GuiCheckBox;
import me.desht.pneumaticcraft.client.gui.widget.IGuiWidget;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetDig;
import net.minecraft.client.resources.I18n;

public class GuiProgWidgetDig extends GuiProgWidgetDigAndPlace<ProgWidgetDig> {

    private GuiCheckBox requiresDiggingTool;

    public GuiProgWidgetDig(ProgWidgetDig widget, GuiProgrammer guiProgrammer) {
        super(widget, guiProgrammer);
    }

    @Override
    public void initGui() {
        super.initGui();

        this.requiresDiggingTool = new GuiCheckBox(125, this.guiLeft + 4, this.guiTop + 85, 0xFF404040, I18n.format("gui.progWidget.dig.requiresDiggingTool"));
        this.requiresDiggingTool.setTooltip("gui.progWidget.dig.requiresDiggingTool.tooltip");
        this.requiresDiggingTool.checked = this.widget.requiresTool();
        this.addWidget(this.requiresDiggingTool);
    }

    @Override
    public void actionPerformed(IGuiWidget guiWidget) {
        if (guiWidget == this.requiresDiggingTool) {
            this.widget.setRequiresTool(this.requiresDiggingTool.checked);
        }
        super.actionPerformed(guiWidget);
    }
}
