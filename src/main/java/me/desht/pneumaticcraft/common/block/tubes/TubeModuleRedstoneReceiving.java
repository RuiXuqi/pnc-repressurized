package me.desht.pneumaticcraft.common.block.tubes;

import me.desht.pneumaticcraft.common.GuiHandler.EnumGuiId;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;

import java.util.List;

public abstract class TubeModuleRedstoneReceiving extends TubeModule {
    private int redstoneLevel;

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        redstoneLevel = tag.getInteger("redstone");
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("redstone", redstoneLevel);
    }

    @Override
    public void addInfo(List<String> curInfo) {
        super.addInfo(curInfo);
        curInfo.add(I18n.format("waila.redstoneModule.emitting", redstoneLevel));
    }

    @Override
    public void onNeighborBlockUpdate() {
        redstoneLevel = pressureTube.world().getRedstonePowerFromNeighbors(pressureTube.pos());
    }

    public int getReceivingRedstoneLevel() {
        return redstoneLevel;
    }

    public float getThreshold() {
        return getThreshold(redstoneLevel);
    }

    @Override
    protected EnumGuiId getGuiId() {
        return EnumGuiId.PRESSURE_MODULE;
    }

    @Override
    public void update() {
        if (upgraded && !advancedConfig && higherBound != lowerBound) {
            higherBound = lowerBound;
            if (!getTube().world().isRemote) sendDescriptionPacket();
        }
    }
}
