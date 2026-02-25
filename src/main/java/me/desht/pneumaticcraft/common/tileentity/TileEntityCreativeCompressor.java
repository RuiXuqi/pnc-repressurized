package me.desht.pneumaticcraft.common.tileentity;

import me.desht.pneumaticcraft.common.block.Blockss;
import me.desht.pneumaticcraft.common.network.GuiSynced;
import me.desht.pneumaticcraft.common.pressure.AirHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

public class TileEntityCreativeCompressor extends TileEntityPneumaticBase {
    @GuiSynced
    private float pressureSetpoint;

    public TileEntityCreativeCompressor() {
        super(30, 30, 50000, 0);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.pressureSetpoint = nbt.getFloat("setpoint");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setFloat("setpoint", this.pressureSetpoint);
        return nbt;
    }

    @Override
    public void update() {
        super.update();
        if (!this.world.isRemote) {
            ((AirHandler) this.getAirHandler(null)).setPressure(this.pressureSetpoint);
        }
    }

    @Override
    public void handleGUIButtonPress(int guiID, EntityPlayer player) {
        switch (guiID) {
            case 0:
                this.pressureSetpoint -= 1;
                break;
            case 1:
                this.pressureSetpoint -= 0.1F;
                break;
            case 2:
                this.pressureSetpoint += 0.1F;
                break;
            case 3:
                this.pressureSetpoint += 1.0F;
                break;
        }
        if (this.pressureSetpoint > 30) this.pressureSetpoint = 30;
        if (this.pressureSetpoint < -1) this.pressureSetpoint = -1;
    }

    @Override
    public String getName() {
        return Blockss.CREATIVE_COMPRESSOR.getTranslationKey();
    }
}
