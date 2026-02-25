package me.desht.pneumaticcraft.common.remote;

import me.desht.pneumaticcraft.client.gui.GuiRemoteEditor;
import me.desht.pneumaticcraft.client.gui.remote.GuiRemoteDropdown;
import me.desht.pneumaticcraft.client.gui.widget.WidgetComboBox;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketSetGlobalVariable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.text.WordUtils;

public class ActionWidgetDropdown extends ActionWidgetVariable<WidgetComboBox> {

    private int x, y, width, height;
    private String dropDownElements = "";
    private String selectedElement = "";
    private boolean sorted;

    public ActionWidgetDropdown() {
        super();
    }

    public ActionWidgetDropdown(WidgetComboBox widget) {
        super(widget);
        this.width = widget.width;
        this.height = widget.height;
        widget.setText(I18n.format("remote.dropdown.name"));
        widget.setTooltip(WordUtils.wrap(I18n.format("remote.dropdown.tooltip"), 50).split(System.getProperty("line.separator")));
    }

    @Override
    public void readFromNBT(NBTTagCompound tag, int guiLeft, int guiTop) {
        super.readFromNBT(tag, guiLeft, guiTop);
        this.x = tag.getInteger("x") + guiLeft;
        this.y = tag.getInteger("y") + guiTop;
        this.width = tag.getInteger("width");
        this.height = tag.getInteger("height");
        this.dropDownElements = tag.getString("dropDownElements");
        this.sorted = tag.getBoolean("sorted");
        this.updateWidget();
    }

    @Override
    public NBTTagCompound toNBT(int guiLeft, int guiTop) {
        NBTTagCompound tag = super.toNBT(guiLeft, guiTop);
        tag.setInteger("x", this.x - guiLeft);
        tag.setInteger("y", this.y - guiTop);
        tag.setInteger("width", this.width);
        tag.setInteger("height", this.height);
        tag.setString("dropDownElements", this.dropDownElements);
        tag.setBoolean("sorted", this.sorted);

        return tag;
    }

    @Override
    public String getId() {
        return "dropdown";
    }

    @Override
    public void onKeyTyped() {
        String[] elements = this.getDropdownElements();
        this.selectedElement = this.getWidget().getText();
        for (int i = 0; i < elements.length; i++) {
            if (elements[i].equals(this.selectedElement)) {
                NetworkHandler.sendToServer(new PacketSetGlobalVariable(this.getVariableName(), i));
                break;
            }
        }
    }

    @Override
    public void onVariableChange() {
        this.updateWidget();
    }

    @Override
    public void setWidgetPos(int x, int y) {
        this.x = x;
        this.y = y;
        this.updateWidget();
    }

    @Override
    public WidgetComboBox getWidget() {
        if (this.widget == null) {
            this.widget = new WidgetComboBox(Minecraft.getMinecraft().fontRenderer, this.x, this.y, this.width, this.height);
            this.widget.setElements(this.getDropdownElements());
            this.widget.setFixedOptions();
            this.widget.setShouldSort(this.sorted);
            this.updateWidget();
        }
        return this.widget;
    }

    private String[] getDropdownElements() {
        return this.dropDownElements.split(",");
    }

    private void updateWidget() {
        String[] elements = this.getDropdownElements();
        this.selectedElement = elements[MathHelper.clamp(GlobalVariableManager.getInstance().getInteger(this.getVariableName()), 0, elements.length - 1)];

        if (this.widget != null) {
            this.widget.x = this.x;
            this.widget.y = this.y;
            this.widget.width = this.width;
            this.widget.height = this.height;
            this.widget.setElements(this.getDropdownElements());
            this.widget.setText(this.selectedElement);
            this.widget.setShouldSort(this.sorted);
        }
    }

    @Override
    public void onActionPerformed() {
    }

    public void setDropDownElements(String dropDownElements) {
        this.dropDownElements = dropDownElements;
        this.updateWidget();
    }

    public String getDropDownElements() {
        return this.dropDownElements;
    }

    public boolean getSorted() {
        return this.sorted;
    }

    public void setSorted(boolean sorted) {
        this.sorted = sorted;
    }

    public void setWidth(int width) {
        this.width = width;
        this.updateWidget();
    }

    public int getWidth() {
        return this.width;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiScreen getGui(GuiRemoteEditor guiRemote) {
        return new GuiRemoteDropdown(this, guiRemote);
    }
}
