package me.desht.pneumaticcraft.common.remote;

import me.desht.pneumaticcraft.client.gui.GuiRemoteEditor;
import me.desht.pneumaticcraft.client.gui.remote.GuiRemoteOptionBase;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;

public class ActionWidgetLabel extends ActionWidget<WidgetLabelVariable> implements IActionWidgetLabeled {

    public ActionWidgetLabel(WidgetLabelVariable widget) {
        super(widget);
    }

    public ActionWidgetLabel() {
    }

    @Override
    public NBTTagCompound toNBT(int guiLeft, int guiTop) {
        NBTTagCompound tag = super.toNBT(guiLeft, guiTop);
        tag.setString("text", this.widget.text);
        tag.setInteger("x", this.widget.getBounds().x - guiLeft);
        tag.setInteger("y", this.widget.getBounds().y - guiTop);
        tag.setString("tooltip", this.widget.getTooltip());
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag, int guiLeft, int guiTop) {
        super.readFromNBT(tag, guiLeft, guiTop);
        this.widget = new WidgetLabelVariable(tag.getInteger("x") + guiLeft, tag.getInteger("y") + guiTop, tag.getString("text"));
        this.widget.setTooltipText(tag.getString("tooltip"));
    }

    @Override
    public String getId() {
        return "label";
    }

    @Override
    public void setText(String text) {
        this.widget.text = text;
    }

    @Override
    public String getText() {
        return this.widget.text;
    }

    @Override
    public GuiScreen getGui(GuiRemoteEditor guiRemote) {
        return new GuiRemoteOptionBase(this, guiRemote);
    }

    @Override
    public void setWidgetPos(int x, int y) {
        this.widget.x = x;
        this.widget.y = y;
    }

    @Override
    public void setTooltip(String text) {
        this.widget.setTooltipText(text);
    }

    @Override
    public String getTooltip() {
        return this.widget.getTooltip();
    }
}
