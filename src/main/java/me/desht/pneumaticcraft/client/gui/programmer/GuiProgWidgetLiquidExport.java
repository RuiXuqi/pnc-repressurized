package me.desht.pneumaticcraft.client.gui.programmer;

import me.desht.pneumaticcraft.client.gui.GuiProgrammer;
import me.desht.pneumaticcraft.client.gui.widget.GuiCheckBox;
import me.desht.pneumaticcraft.client.gui.widget.IGuiWidget;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetLiquidExport;

public class GuiProgWidgetLiquidExport extends GuiProgWidgetImportExport<ProgWidgetLiquidExport> {

    public GuiProgWidgetLiquidExport(ProgWidgetLiquidExport widget, GuiProgrammer guiProgrammer) {
        super(widget, guiProgrammer);
    }

    @Override
    public void initGui() {
        super.initGui();

        GuiCheckBox checkbox = new GuiCheckBox(300, this.guiLeft + 70, this.guiTop + 70, 0xFF404040, "gui.progWidget.liquidExport.placeFluidInWorld");
        checkbox.setChecked(this.widget.isPlacingFluidBlocks());
        this.addWidget(checkbox);
    }

    @Override
    public void actionPerformed(IGuiWidget w) {
        if (w.getID() == 300) {
            this.widget.setPlaceFluidBlocks(((GuiCheckBox) w).checked);
        }
        super.actionPerformed(w);
    }
}
