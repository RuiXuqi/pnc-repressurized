package me.desht.pneumaticcraft.common.progwidgets;

import me.desht.pneumaticcraft.client.gui.GuiProgrammer;
import me.desht.pneumaticcraft.client.gui.programmer.GuiProgWidgetImportExport;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public abstract class ProgWidgetInventoryBase extends ProgWidgetAreaItemBase implements ISidedWidget, ICountWidget {
    private boolean[] accessingSides = new boolean[]{true, true, true, true, true, true};
    private boolean useCount;
    private int count = 1;

    @Override
    public void addErrors(List<String> curInfo, List<IProgWidget> widgets) {
        super.addErrors(curInfo, widgets);

        boolean sideActive = false;
        for (boolean bool : this.accessingSides) {
            sideActive |= bool;
        }
        if (!sideActive) curInfo.add("gui.progWidget.general.error.noSideActive");
    }

    @Override
    public void setSides(boolean[] sides) {
        this.accessingSides = sides;
    }

    @Override
    public boolean[] getSides() {
        return this.accessingSides;
    }

    @Override
    public boolean useCount() {
        return this.useCount;
    }

    @Override
    public void setUseCount(boolean useCount) {
        this.useCount = useCount;
    }

    @Override
    public int getCount() {
        return this.count;
    }

    @Override
    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public void getTooltip(List<String> curTooltip) {
        super.getTooltip(curTooltip);
        if (this.isUsingSides()) curTooltip.add("Accessing sides:");
        curTooltip.add(this.getExtraStringInfo());
        if (this.useCount) curTooltip.add("Using count (" + this.count + ")");
    }

    protected boolean isUsingSides() {
        return true;
    }

    @Override
    public String getExtraStringInfo() {
        boolean allSides = true;
        boolean noSides = true;
        for (boolean bool : this.accessingSides) {
            if (bool) {
                noSides = false;
            } else {
                allSides = false;
            }
        }
        if (allSides) {
            return "All sides";
        } else if (noSides) {
            return "No Sides";
        } else {
            StringBuilder tip = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                if (this.accessingSides[i]) {
                    switch (EnumFacing.byIndex(i)) {
                        case UP:
                            tip.append("top, ");
                            break;
                        case DOWN:
                            tip.append("bottom, ");
                            break;
                        case NORTH:
                            tip.append("north, ");
                            break;
                        case SOUTH:
                            tip.append("south, ");
                            break;
                        case EAST:
                            tip.append("east, ");
                            break;
                        case WEST:
                            tip.append("west, ");
                            break;
                    }
                }
            }
            return tip.substring(0, tip.length() - 2);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        for (int i = 0; i < 6; i++) {
            tag.setBoolean(EnumFacing.byIndex(i).name(), this.accessingSides[i]);
        }
        tag.setBoolean("useCount", this.useCount);
        tag.setInteger("count", this.count);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        for (int i = 0; i < 6; i++) {
            this.accessingSides[i] = tag.getBoolean(EnumFacing.byIndex(i).name());
        }
        this.useCount = tag.getBoolean("useCount");
        this.count = tag.getInteger("count");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiScreen getOptionWindow(GuiProgrammer guiProgrammer) {
        return new GuiProgWidgetImportExport(this, guiProgrammer);
    }
}
