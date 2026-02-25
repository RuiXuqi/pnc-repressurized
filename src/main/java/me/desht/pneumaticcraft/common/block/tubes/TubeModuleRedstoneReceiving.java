package me.desht.pneumaticcraft.common.block.tubes;

import me.desht.pneumaticcraft.common.GuiHandler.EnumGuiId;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;

import java.util.List;

public abstract class TubeModuleRedstoneReceiving extends TubeModule {
    private int redstoneLevel;

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.redstoneLevel = tag.getInteger("redstone");
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("redstone", this.redstoneLevel);
    }

    @Override
    public void addInfo(List<String> curInfo) {
        super.addInfo(curInfo);
        curInfo.add("Applied redstone: " + TextFormatting.WHITE + this.redstoneLevel);
    }

    @Override
    public void onNeighborBlockUpdate() {
        this.redstoneLevel = this.pressureTube.world().getRedstonePowerFromNeighbors(this.pressureTube.pos());
    }

    public int getReceivingRedstoneLevel() {
        return this.redstoneLevel;
    }

    public float getThreshold() {
        return this.getThreshold(this.redstoneLevel);
    }

    @Override
    protected EnumGuiId getGuiId() {
        return EnumGuiId.PRESSURE_MODULE;
    }

    @Override
    public void update() {
        if (this.upgraded && !this.advancedConfig && this.higherBound != this.lowerBound) {
            this.higherBound = this.lowerBound;
            if (!this.getTube().world().isRemote) this.sendDescriptionPacket();
        }
    }
}
