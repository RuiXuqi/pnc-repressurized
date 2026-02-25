package me.desht.pneumaticcraft.client.gui.pneumatic_armor;

import me.desht.pneumaticcraft.api.client.pneumaticHelmet.IGuiScreen;
import me.desht.pneumaticcraft.api.client.pneumaticHelmet.IOptionPage;
import me.desht.pneumaticcraft.api.client.pneumaticHelmet.IUpgradeRenderHandler;
import me.desht.pneumaticcraft.common.item.ItemPneumaticArmor;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketUpdateArmorExtraData;
import me.desht.pneumaticcraft.common.pneumatic_armor.CommonArmorHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPageButtonList;
import net.minecraft.client.gui.GuiSlider;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;

public abstract class GuiSliderOptions extends IOptionPage.SimpleToggleableOptions implements GuiPageButtonList.GuiResponder {
    private GuiSlider slider;
    private Integer pendingVal = null;

    GuiSliderOptions(IUpgradeRenderHandler handler) {
        super(handler);
    }

    protected Point getSliderPos() {
        return new Point(30, 60);
    }

    protected Pair<Integer, Integer> getRange() {
        return Pair.of(0, 100);
    }

    protected abstract String getTagName();

    protected abstract EntityEquipmentSlot getSlot();

    protected abstract GuiSlider.FormatHelper getFormatHelper();

    public void initGui(IGuiScreen gui) {
        Pair<Integer, Integer> range = this.getRange();
        int initVal = range.getRight();
        if (Minecraft.getMinecraft().player != null) {
            ItemStack leggings = Minecraft.getMinecraft().player.getItemStackFromSlot(this.getSlot());
            initVal = ItemPneumaticArmor.getIntData(leggings, this.getTagName(), range.getRight());
        }
        Point pos = this.getSliderPos();
        this.slider = new GuiSlider(this, 1000, pos.x, pos.y,
                "slider", range.getLeft(), range.getRight(), initVal, this.getFormatHelper());
        gui.getButtonList().add(this.slider);
    }

    @Override
    public void updateScreen() {
        if (this.pendingVal != null && !this.slider.isMouseDown) {
            // avoid sending a stream of update packets if player is dragging slider
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger(this.getTagName(), this.pendingVal);
            NetworkHandler.sendToServer(new PacketUpdateArmorExtraData(this.getSlot(), tag));
            // also update the clientside handler
            CommonArmorHandler.getHandlerForPlayer().onDataFieldUpdated(this.getSlot(), this.getTagName(), tag.getTag(this.getTagName()));
            this.pendingVal = null;
        }
    }

    @Override
    public void setEntryValue(int id, boolean value) {
    }

    @Override
    public void setEntryValue(int id, float value) {
        this.pendingVal = (int) value;
    }

    @Override
    public void setEntryValue(int id, String value) {

    }
}
