package me.desht.pneumaticcraft.common.remote;

import me.desht.pneumaticcraft.client.gui.widget.GuiCheckBox;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketSetGlobalVariable;
import net.minecraft.nbt.NBTTagCompound;

public class ActionWidgetCheckBox extends ActionWidgetVariable<GuiCheckBox> implements IActionWidgetLabeled {

    public ActionWidgetCheckBox() {
        super();
    }

    public ActionWidgetCheckBox(GuiCheckBox widget) {
        super(widget);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag, int guiLeft, int guiTop) {
        super.readFromNBT(tag, guiLeft, guiTop);
        this.widget = new GuiCheckBox(-1, tag.getInteger("x") + guiLeft, tag.getInteger("y") + guiTop, 0xFF404040, tag.getString("text"));
        this.setTooltip(tag.getString("tooltip"));
    }

    @Override
    public NBTTagCompound toNBT(int guiLeft, int guiTop) {
        NBTTagCompound tag = super.toNBT(guiLeft, guiTop);
        tag.setInteger("x", this.widget.x - guiLeft);
        tag.setInteger("y", this.widget.y - guiTop);
        tag.setString("text", this.widget.text);
        tag.setString("tooltip", this.widget.getTooltip());
        return tag;
    }

    @Override
    public String getId() {
        return "checkbox";
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
    public void onActionPerformed() {
        NetworkHandler.sendToServer(new PacketSetGlobalVariable(this.getVariableName(), this.widget.checked));
    }

    @Override
    public void onVariableChange() {
        this.widget.checked = GlobalVariableManager.getInstance().getBoolean(this.getVariableName());
    }

    @Override
    public void setWidgetPos(int x, int y) {
        this.widget.x = x;
        this.widget.y = y;
    }

    @Override
    public void setTooltip(String text) {
        this.widget.setTooltip(text);
    }

    @Override
    public String getTooltip() {
        return this.widget.getTooltip();
    }
}
