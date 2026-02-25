package me.desht.pneumaticcraft.common.tileentity;

import me.desht.pneumaticcraft.api.tileentity.IManoMeasurable;
import me.desht.pneumaticcraft.common.block.BlockPressureChamberWall;
import me.desht.pneumaticcraft.common.block.Blockss;
import me.desht.pneumaticcraft.common.thirdparty.waila.IInfoForwarder;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class TileEntityPressureChamberWall extends TileEntityBase implements IManoMeasurable, IInfoForwarder {

    private TileEntityPressureChamberValve teValve;
    private int valveX;
    private int valveY;
    private int valveZ;

    public TileEntityPressureChamberWall() {
        super();
    }

    public TileEntityPressureChamberWall(int upgradeSize) {
        super(upgradeSize);
    }

    public TileEntityPressureChamberValve getCore() {
        if (this.teValve == null && (this.valveX != 0 || this.valveY != 0 || this.valveZ != 0)) {
            // when the saved TE equals null, check if we can
            // retrieve the TE from the NBT saved coords.
            TileEntity te = this.getWorld().getTileEntity(new BlockPos(this.valveX, this.valveY, this.valveZ));
            this.setCore(te instanceof TileEntityPressureChamberValve ? (TileEntityPressureChamberValve) te : null);
        }
        return this.teValve;
    }

    public void onBlockBreak() {
        this.teValve = this.getCore();
        if (this.teValve != null) {
            this.teValve.onMultiBlockBreak();
        }
    }

    void setCore(TileEntityPressureChamberValve te) {
        if (!this.getWorld().isRemote) {
            if (te != null) {
                this.valveX = te.getPos().getX();
                this.valveY = te.getPos().getY();
                this.valveZ = te.getPos().getZ();
            } else {
                this.valveX = 0;
                this.valveY = 0;
                this.valveZ = 0;
            }
        }
        boolean hasChanged = this.teValve != te;
        this.teValve = te;
        if (hasChanged && !this.getWorld().isRemote) {
            IBlockState curState = this.getWorld().getBlockState(this.getPos());
            if (curState.getBlock() == Blockss.PRESSURE_CHAMBER_WALL) {
                IBlockState newState = ((BlockPressureChamberWall) Blockss.PRESSURE_CHAMBER_WALL).updateState(curState, this.getWorld(), this.getPos());
                this.getWorld().setBlockState(this.getPos(), newState, 2);
            }
        }
    }

    @Override
    public void onDescUpdate() {
        super.onDescUpdate();
        this.teValve = null;
    }

    /**
     * Reads a tile entity from NBT.
     */
    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.valveX = tag.getInteger("valveX");
        this.valveY = tag.getInteger("valveY");
        this.valveZ = tag.getInteger("valveZ");
        this.teValve = null;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("valveX", this.valveX);
        tag.setInteger("valveY", this.valveY);
        tag.setInteger("valveZ", this.valveZ);
        return tag;
    }

    @Override
    public void printManometerMessage(EntityPlayer player, List<String> curInfo) {
        if (this.getCore() != null) {
            this.teValve.getAirHandler(null).printManometerMessage(player, curInfo);
        }
    }

    @Override
    protected boolean shouldRerenderChunkOnDescUpdate() {
        return true;
    }

    @Override
    public TileEntity getInfoTileEntity() {
        return this.getCore();
    }

}
