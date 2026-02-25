package me.desht.pneumaticcraft.common.tileentity;

import net.minecraft.item.EnumDyeColor;
import net.minecraft.nbt.NBTTagCompound;

public class TileEntityAphorismTile extends TileEntityBase {
    private String[] textLines = new String[]{""};

    public int textRotation;
    private int borderColor = EnumDyeColor.BLUE.getDyeDamage();
    private int backgroundColor = EnumDyeColor.WHITE.getDyeDamage();

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
    }

    @Override
    public void writeToPacket(NBTTagCompound tag) {
        super.writeToPacket(tag);
        tag.setInteger("lines", this.textLines.length);
        for (int i = 0; i < this.textLines.length; i++) {
            tag.setString("line" + i, this.textLines[i]);
        }
        tag.setInteger("textRot", this.textRotation);
        tag.setInteger("border", this.borderColor);
        tag.setInteger("background", this.backgroundColor);
    }

    @Override
    public void readFromPacket(NBTTagCompound tag) {
        super.readFromPacket(tag);
        int lines = tag.getInteger("lines");
        this.textLines = new String[lines];
        for (int i = 0; i < lines; i++) {
            this.textLines[i] = tag.getString("line" + i);
        }
        this.textRotation = tag.getInteger("textRot");
        if (tag.hasKey("border")) {
            this.borderColor = tag.getInteger("border");
            this.backgroundColor = tag.getInteger("background");
        } else {
            this.borderColor = EnumDyeColor.BLUE.getDyeDamage();
            this.backgroundColor = EnumDyeColor.WHITE.getDyeDamage();
        }
        if (this.world != null) this.rerenderTileEntity();
    }

    public String[] getTextLines() {
        return this.textLines;
    }

    public void setTextLines(String[] textLines) {
        this.textLines = textLines;
        if (!this.world.isRemote) this.sendDescriptionPacket();
    }

    public void setBorderColor(int color) {
        this.borderColor = color;
        if (!this.world.isRemote) this.sendDescriptionPacket();
    }

    public int getBorderColor() {
        return this.borderColor;
    }

    public int getBackgroundColor() {
        return this.backgroundColor;
    }

    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
        if (!this.world.isRemote) this.sendDescriptionPacket();
    }
}
