package me.desht.pneumaticcraft.common.remote;

import me.desht.pneumaticcraft.client.gui.GuiButtonSpecial;
import me.desht.pneumaticcraft.client.gui.GuiRemoteEditor;
import me.desht.pneumaticcraft.client.gui.remote.GuiRemoteButton;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketSetGlobalVariable;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

public class ActionWidgetButton extends ActionWidgetVariable<GuiButtonSpecial> implements IActionWidgetLabeled {

    public BlockPos settingCoordinate = BlockPos.ORIGIN; // The coordinate the variable is set to when the button is pressed.

    public ActionWidgetButton() {
        super();
    }

    public ActionWidgetButton(GuiButtonSpecial widget) {
        super(widget);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag, int guiLeft, int guiTop) {
        super.readFromNBT(tag, guiLeft, guiTop);
        this.widget = new GuiButtonSpecial(-1, tag.getInteger("x") + guiLeft, tag.getInteger("y") + guiTop, tag.getInteger("width"), tag.getInteger("height"), tag.getString("text"));
        this.settingCoordinate = new BlockPos(tag.getInteger("settingX"), tag.getInteger("settingY"), tag.getInteger("settingZ"));
        this.widget.setTooltipText(tag.getString("tooltip"));
    }

    @Override
    public NBTTagCompound toNBT(int guiLeft, int guiTop) {
        NBTTagCompound tag = super.toNBT(guiLeft, guiTop);
        tag.setInteger("x", this.widget.x - guiLeft);
        tag.setInteger("y", this.widget.y - guiTop);
        tag.setInteger("width", this.widget.width);
        tag.setInteger("height", this.widget.height);
        tag.setString("text", this.widget.displayString);
        tag.setInteger("settingX", this.settingCoordinate.getX());
        tag.setInteger("settingY", this.settingCoordinate.getY());
        tag.setInteger("settingZ", this.settingCoordinate.getZ());
        tag.setString("tooltip", this.widget.getTooltip());
        return tag;
    }

    @Override
    public String getId() {
        return "button";
    }

    @Override
    public void setText(String text) {
        this.widget.displayString = text;
    }

    @Override
    public String getText() {
        return this.widget.displayString;
    }

    @Override
    public void onActionPerformed() {
        NetworkHandler.sendToServer(new PacketSetGlobalVariable(this.getVariableName(), this.settingCoordinate));
    }

    @Override
    public void onVariableChange() {
        // widget.checked = GlobalVariableManager.getBoolean(getVariableName());
    }

    @Override
    public GuiScreen getGui(GuiRemoteEditor guiRemote) {
        return new GuiRemoteButton(this, guiRemote);
    }

    @Override
    public void setWidgetPos(int x, int y) {
        this.widget.x = x;
        this.widget.y = y;
    }

    public void setWidth(int width) {
        this.widget.width = width;
    }

    public int getWidth() {
        return this.widget.width;
    }

    public void setHeight(int height) {
        this.widget.height = height;
    }

    public int getHeight() {
        return this.widget.height;
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
