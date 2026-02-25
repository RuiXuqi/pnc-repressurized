package me.desht.pneumaticcraft.client.gui.programmer;

import me.desht.pneumaticcraft.client.gui.GuiProgrammer;
import me.desht.pneumaticcraft.client.gui.widget.GuiCheckBox;
import me.desht.pneumaticcraft.client.gui.widget.IGuiWidget;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetEmitRedstone;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import net.minecraft.util.EnumFacing;

public class GuiProgWidgetEmitRedstone extends GuiProgWidgetOptionBase<ProgWidgetEmitRedstone> {

    public GuiProgWidgetEmitRedstone(ProgWidgetEmitRedstone widget, GuiProgrammer guiProgrammer) {
        super(widget, guiProgrammer);
    }

    @Override
    public void initGui() {
        super.initGui();

        for (int i = 0; i < 6; i++) {
            String sideName = PneumaticCraftUtils.getOrientationName(EnumFacing.byIndex(i));
            GuiCheckBox checkBox = new GuiCheckBox(i, this.guiLeft + 4, this.guiTop + 30 + i * 12, 0xFF404040, sideName);
            checkBox.checked = this.widget.getSides()[i];
            this.addWidget(checkBox);
        }
    }

    @Override
    public void actionPerformed(IGuiWidget checkBox) {
        if (checkBox.getID() < 6 && checkBox.getID() >= 0) {
            this.widget.getSides()[checkBox.getID()] = ((GuiCheckBox) checkBox).checked;
        }
        super.actionPerformed(checkBox);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.fontRenderer.drawString("Affecting sides:", this.guiLeft + 4, this.guiTop + 20, 0xFF604040);
    }
}
