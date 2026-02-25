package me.desht.pneumaticcraft.common.remote;

import me.desht.pneumaticcraft.client.gui.GuiRemoteEditor;
import me.desht.pneumaticcraft.client.gui.widget.IGuiWidget;
import me.desht.pneumaticcraft.lib.Log;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

public abstract class ActionWidget<Widget extends IGuiWidget> {
    protected Widget widget;
    private String enableVariable = "";
    private BlockPos enablingValue = BlockPos.ORIGIN;

    public ActionWidget(Widget widget) {
        this.widget = widget;
    }

    public ActionWidget() {
    }

    public void readFromNBT(NBTTagCompound tag, int guiLeft, int guiTop) {
        this.enableVariable = tag.getString("enableVariable");
        this.enablingValue = tag.hasKey("enablingX") ? new BlockPos(tag.getInteger("enablingX"), tag.getInteger("enablingY"), tag.getInteger("enablingZ")) : new BlockPos(1, 0, 0);
    }

    public NBTTagCompound toNBT(int guiLeft, int guitTop) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", this.getId());
        tag.setString("enableVariable", this.enableVariable);
        tag.setInteger("enablingX", this.enablingValue.getX());
        tag.setInteger("enablingY", this.enablingValue.getY());
        tag.setInteger("enablingZ", this.enablingValue.getZ());
        return tag;
    }

    public ActionWidget copy() {
        try {
            ActionWidget widget = this.getClass().newInstance();
            widget.readFromNBT(this.toNBT(0, 0), 0, 0);
            return widget;
        } catch (Exception e) {
            Log.error("Error occured when trying to copy an " + this.getId() + " action widget.");
            e.printStackTrace();
            return null;
        }
    }

    public Widget getWidget() {
        return this.widget;
    }

    public abstract void setWidgetPos(int x, int y);

    public abstract String getId();

    public GuiScreen getGui(GuiRemoteEditor guiRemote) {
        return null;
    }

    public void setEnableVariable(String varName) {
        this.enableVariable = varName;
    }

    public String getEnableVariable() {
        return this.enableVariable;
    }

    public boolean isEnabled() {
        return this.enableVariable.equals("") || GlobalVariableManager.getInstance().getPos(this.enableVariable).equals(this.enablingValue);
    }

    public void setEnablingValue(int x, int y, int z) {
        this.enablingValue = new BlockPos(x, y, z);
    }

    public BlockPos getEnablingValue() {
        return this.enablingValue;
    }
}
