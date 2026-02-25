package me.desht.pneumaticcraft.common.tileentity;

import me.desht.pneumaticcraft.common.block.BlockPneumaticDoor;
import me.desht.pneumaticcraft.common.network.DescSynced;
import me.desht.pneumaticcraft.common.network.LazySynced;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityPneumaticDoor extends TileEntityTickableBase {
    @DescSynced
    @LazySynced
    public float rotationAngle;
    public float oldRotationAngle;
    @DescSynced
    public boolean rightGoing;

    public void setRotationAngle(float rotationAngle) {
        this.oldRotationAngle = this.rotationAngle;
        this.rotationAngle = rotationAngle;

        if (rotationAngle != this.oldRotationAngle &&
                (this.oldRotationAngle == 0f || this.oldRotationAngle == 90f || rotationAngle == 0f || rotationAngle == 90f)) {
            if (this.getWorld().isRemote) {
                // force a redraw to make the static door model appear or disappear
                this.getWorld().markBlockRangeForRenderUpdate(this.pos, this.pos);
            }
        }

        // also rotate the TE for the other half of the door
        TileEntity otherTE = this.getWorld().getTileEntity(this.getPos().offset(this.isTopDoor() ? EnumFacing.DOWN : EnumFacing.UP));
        if (otherTE instanceof TileEntityPneumaticDoor) {
            TileEntityPneumaticDoor otherDoorHalf = (TileEntityPneumaticDoor) otherTE;
            otherDoorHalf.rightGoing = this.rightGoing;
            if (rotationAngle != otherDoorHalf.rotationAngle) {
                otherDoorHalf.setRotationAngle(rotationAngle);
            }
        }
    }

    public boolean isTopDoor() {
        return BlockPneumaticDoor.isTopDoor(this.getWorld().getBlockState(this.getPos()));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setBoolean("rightGoing", this.rightGoing);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.rightGoing = tag.getBoolean("rightGoing");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(this.getPos().getX(), this.getPos().getY(), this.getPos().getZ(), this.getPos().getX() + 1, this.getPos().getY() + 2, this.getPos().getZ() + 1);
    }

    @Override
    public boolean canRenderBreaking() {
        return true;
    }
}
