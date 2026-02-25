package me.desht.pneumaticcraft.common.progwidgets;

import me.desht.pneumaticcraft.client.gui.GuiProgrammer;
import me.desht.pneumaticcraft.client.gui.programmer.GuiProgWidgetDigAndPlace;
import me.desht.pneumaticcraft.common.ai.DroneAIBlockInteraction;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public abstract class ProgWidgetDigAndPlace extends ProgWidgetAreaItemBase implements IBlockOrdered, IMaxActions {
    private EnumOrder order;
    private int maxActions = 1;
    private boolean useMaxActions;

    @Override
    public EnumOrder getOrder() {
        return this.order;
    }

    @Override
    public void setOrder(EnumOrder order) {
        this.order = order;
    }

    @Override
    public void getTooltip(List<String> curTooltip) {
        super.getTooltip(curTooltip);
        curTooltip.add("Order: " + this.order.getLocalizedName());
    }

    public ProgWidgetDigAndPlace(EnumOrder order) {
        this.order = order;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiScreen getOptionWindow(GuiProgrammer guiProgrammer) {
        return new GuiProgWidgetDigAndPlace<>(this, guiProgrammer);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("order", this.order.ordinal());
        tag.setBoolean("useMaxActions", this.useMaxActions);
        tag.setInteger("maxActions", this.maxActions);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.order = EnumOrder.values()[tag.getInteger("order")];
        this.useMaxActions = tag.getBoolean("useMaxActions");
        this.maxActions = tag.getInteger("maxActions");
    }

    @Override
    public String getExtraStringInfo() {
        return this.order.getLocalizedName();
    }

    @Override
    public void setMaxActions(int maxActions) {
        this.maxActions = maxActions;
    }

    @Override
    public int getMaxActions() {
        return this.maxActions;
    }

    @Override
    public void setUseMaxActions(boolean useMaxActions) {
        this.useMaxActions = useMaxActions;
    }

    @Override
    public boolean useMaxActions() {
        return this.useMaxActions;
    }

    protected DroneAIBlockInteraction setupMaxActions(DroneAIBlockInteraction ai, IMaxActions widget) {
        if (widget.useMaxActions()) {
            ai.setMaxActions(widget.getMaxActions());
        }
        return ai;
    }
}
