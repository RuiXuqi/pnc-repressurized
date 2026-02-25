package me.desht.pneumaticcraft.common.block.tubes;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;

import java.util.List;

public abstract class TubeModuleRedstoneEmitting extends TubeModule {
    protected int redstone;

    /**
     * @param level signal level
     * @return true if the redstone has changed compared to last time.
     */
    public boolean setRedstone(int level) {
        level = MathHelper.clamp(level, 0, 15);
        if (this.redstone != level) {
            this.redstone = level;
            this.updateNeighbors();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getRedstoneLevel() {
        return this.redstone;
    }

    @Override
    public void addInfo(List<String> curInfo) {
        super.addInfo(curInfo);
        curInfo.add("Emitting redstone: " + TextFormatting.WHITE + this.redstone);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("redstone", this.redstone);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.redstone = tag.getInteger("redstone");
    }

    @Override
    public void update() {
        if (this.upgraded && !this.advancedConfig) {
            if (this.higherBound < this.lowerBound) {
                if (this.higherBound != this.lowerBound - 0.1F) {
                    this.higherBound = this.lowerBound - 0.1F;
                    this.sendDescriptionPacket();
                }
            } else {
                if (this.higherBound != this.lowerBound + 0.1F) {
                    this.higherBound = this.lowerBound + 0.1F;
                    this.sendDescriptionPacket();
                }
            }
        }
    }
}
