package me.desht.pneumaticcraft.client.gui.programmer;

import me.desht.pneumaticcraft.client.gui.GuiProgrammer;
import me.desht.pneumaticcraft.client.gui.widget.GuiCheckBox;
import me.desht.pneumaticcraft.client.gui.widget.IGuiWidget;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetHarvest;
import net.minecraft.client.resources.I18n;

public class GuiProgWidgetHarvest extends GuiProgWidgetDigAndPlace<ProgWidgetHarvest> {

    private GuiCheckBox requiresHoe;

    public GuiProgWidgetHarvest(ProgWidgetHarvest widget, GuiProgrammer guiProgrammer) {
        super(widget, guiProgrammer);
    }

    @Override
    public void initGui() {
        super.initGui();

        this.requiresHoe = new GuiCheckBox(125, this.guiLeft + 4, this.guiTop + 85, 0xFF404040, I18n.format("gui.progWidget.harvest.requiresHoe"));
        this.requiresHoe.setTooltip("gui.progWidget.harvest.requiresHoe.tooltip");
        this.requiresHoe.checked = this.widget.requiresTool();
        this.addWidget(this.requiresHoe);
    }

    @Override
    public void actionPerformed(IGuiWidget guiWidget) {
        if (guiWidget == this.requiresHoe) {
            this.widget.setRequiresTool(this.requiresHoe.checked);
        }
        super.actionPerformed(guiWidget);
    }
}
