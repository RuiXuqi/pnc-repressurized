package me.desht.pneumaticcraft.client.gui.programmer;

import me.desht.pneumaticcraft.client.gui.GuiProgrammer;
import me.desht.pneumaticcraft.client.gui.widget.GuiCheckBox;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetBlockRightClick;
import net.minecraft.client.resources.I18n;

public class GuiProgWidgetBlockRightClick extends GuiProgWidgetPlace<ProgWidgetBlockRightClick> {
    private GuiCheckBox checkboxSneaking;

    public GuiProgWidgetBlockRightClick(ProgWidgetBlockRightClick widget, GuiProgrammer guiProgrammer) {
        super(widget, guiProgrammer);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.checkboxSneaking = new GuiCheckBox(15, this.guiLeft + 100, this.guiTop + 20, 0xFF404040, I18n.format("gui.progWidget.blockRightClick.sneaking"));
        this.checkboxSneaking.setChecked(this.widget.isSneaking());
        this.checkboxSneaking.setTooltip(I18n.format("gui.progWidget.blockRightClick.sneaking.tooltip"));
        this.addWidget(this.checkboxSneaking);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        this.widget.setSneaking(this.checkboxSneaking.checked);
    }
}
