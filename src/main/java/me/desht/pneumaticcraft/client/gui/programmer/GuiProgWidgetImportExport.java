package me.desht.pneumaticcraft.client.gui.programmer;

import me.desht.pneumaticcraft.client.gui.GuiProgrammer;
import me.desht.pneumaticcraft.client.gui.widget.GuiCheckBox;
import me.desht.pneumaticcraft.client.gui.widget.IGuiWidget;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTextFieldNumber;
import me.desht.pneumaticcraft.common.progwidgets.ICountWidget;
import me.desht.pneumaticcraft.common.progwidgets.IProgWidget;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetInventoryBase;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumFacing;

public class GuiProgWidgetImportExport<Widget extends IProgWidget> extends GuiProgWidgetAreaShow<Widget> {

    private WidgetTextFieldNumber textField;

    public GuiProgWidgetImportExport(Widget widget, GuiProgrammer guiProgrammer) {
        super(widget, guiProgrammer);
    }

    @Override
    public void initGui() {
        super.initGui();

        if (this.showSides()) {
            for (int i = 0; i < 6; i++) {
                String sideName = PneumaticCraftUtils.getOrientationName(EnumFacing.byIndex(i));
                GuiCheckBox checkBox = new GuiCheckBox(i, this.guiLeft + 4, this.guiTop + 30 + i * 12, 0xFF404040, sideName);
                checkBox.checked = ((ProgWidgetInventoryBase) this.widget).getSides()[i];
                this.addWidget(checkBox);
            }
        }

        GuiCheckBox useItemCount = new GuiCheckBox(6, this.guiLeft + 4, this.guiTop + (this.showSides() ? 115 : 30), 0xFF404040, I18n.format("gui.progWidget.itemFilter.useItemCount"));
        useItemCount.setTooltip("gui.progWidget.itemFilter.useItemCount.tooltip");
        useItemCount.checked = ((ICountWidget) this.widget).useCount();
        this.addWidget(useItemCount);

        this.textField = new WidgetTextFieldNumber(Minecraft.getMinecraft().fontRenderer, this.guiLeft + 7, this.guiTop + (this.showSides() ? 128 : 43), 50, 11);
        this.textField.setValue(((ICountWidget) this.widget).getCount());
        this.textField.setEnabled(useItemCount.checked);
        this.addWidget(this.textField);
    }

    protected boolean showSides() {
        return true;
    }

    @Override
    public void actionPerformed(IGuiWidget checkBox) {
        if (checkBox.getID() < 6 && checkBox.getID() >= 0) {
            ((ProgWidgetInventoryBase) this.widget).getSides()[checkBox.getID()] = ((GuiCheckBox) checkBox).checked;
        } else if (checkBox.getID() == 6) {
            ((ICountWidget) this.widget).setUseCount(((GuiCheckBox) checkBox).checked);
            this.textField.setEnabled(((GuiCheckBox) checkBox).checked);
        }
        super.actionPerformed(checkBox);
    }

    @Override
    public void onKeyTyped(IGuiWidget widget) {
        ((ICountWidget) this.widget).setCount(this.textField.getValue());
        super.onKeyTyped(widget);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (this.showSides())
            this.fontRenderer.drawString("Accessing sides:", this.guiLeft + 4, this.guiTop + 20, 0xFF404060);
    }

}
