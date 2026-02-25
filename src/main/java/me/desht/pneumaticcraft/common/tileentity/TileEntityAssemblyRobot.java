package me.desht.pneumaticcraft.common.tileentity;

import me.desht.pneumaticcraft.common.network.DescSynced;
import me.desht.pneumaticcraft.common.network.LazySynced;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.TileEntityConstants;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class TileEntityAssemblyRobot extends TileEntityTickableBase implements IAssemblyMachine, IResettable {
    public final float[] oldAngles = new float[5];
    @DescSynced
    @LazySynced
    public final float[] angles = new float[5];
    @DescSynced
    final float[] targetAngles = new float[5];
    EnumFacing[] targetDirection = new EnumFacing[]{null, null};
    @DescSynced
    boolean slowMode; //used for the drill when drilling, the slowmode moves the arm 10x as slow as normal.
    @DescSynced
    protected float speed = 1.0F;
    private BlockPos controllerPos;

    protected enum EnumAngles {
        TURN, BASE, MIDDLE, TAIL, HEAD
    }

    public TileEntityAssemblyRobot() {
        this.gotoHomePosition();
        for (int i = 0; i < 5; i++) {
            this.angles[i] = this.targetAngles[i];
            this.oldAngles[i] = this.targetAngles[i];
        }
    }


    @Override
    public void setControllerPos(BlockPos controllerPos) {
        this.controllerPos = controllerPos;
    }

    @Override
    public void onNeighborBlockUpdate() {
        super.onNeighborBlockUpdate();
        this.invalidateSystem();
    }

    void invalidateSystem() {
        if (this.controllerPos != null) {
            TileEntity te = this.getWorld().getTileEntity(this.controllerPos);
            if (te instanceof TileEntityAssemblyController) {
                ((TileEntityAssemblyController) te).invalidateAssemblySystem();
            }
        }
    }

    @Override
    public void update() {
        super.update();
        //set the old angles to the last tick calculated angles (used in rendering)
        // while(isDone()) {
        // gotoNeighbour(ForgeDirection.SOUTH, ForgeDirection.EAST);
        //     if(!isDone()) break;
        //     gotoHomePosition();
        // 

        System.arraycopy(this.angles, 0, this.oldAngles, 0, 5);
        //move the arms and claw more to their destination
        for (int i = 0; i < 5; i++) {
            if (this.angles[i] > this.targetAngles[i]) {
                this.angles[i] = Math.max(this.angles[i] - TileEntityConstants.ASSEMBLY_IO_UNIT_ARM_SPEED * (this.slowMode ? 0.1F : 1) * this.speed, this.targetAngles[i]);
            } else if (this.angles[i] < this.targetAngles[i]) {
                this.angles[i] = Math.min(this.angles[i] + TileEntityConstants.ASSEMBLY_IO_UNIT_ARM_SPEED * (this.slowMode ? 0.1F : 1) * this.speed, this.targetAngles[i]);
            }
        }
    }

    public void gotoHomePosition() {
        this.targetAngles[EnumAngles.TURN.ordinal()] = 0F;
        this.targetAngles[EnumAngles.BASE.ordinal()] = 0F;
        this.targetAngles[EnumAngles.MIDDLE.ordinal()] = 55F;
        this.targetAngles[EnumAngles.TAIL.ordinal()] = 35F;
        this.targetAngles[EnumAngles.HEAD.ordinal()] = 0F;
    }

    boolean gotoTarget() {
        if (this.targetDirection == null) return false;

        this.gotoNeighbour(this.targetDirection[0], this.targetDirection[1]);
        return this.isDoneMoving();
    }

    public void gotoNeighbour(EnumFacing direction) {
        this.gotoNeighbour(direction, null);
    }

    /**
     * Goes to the neighbour in the given direction(s).
     *
     * @param primaryDir   the first horizontal direction to move in
     * @param secondaryDir the second horizontal direction to move in (may be null)
     * @return true if the neighbour is diagonal to this arm
     */
    @SuppressWarnings("incomplete-switch")
    public boolean gotoNeighbour(EnumFacing primaryDir, EnumFacing secondaryDir) {
        this.targetDirection = new EnumFacing[]{primaryDir, secondaryDir};
        boolean diagonal = true;
        boolean diagonalAllowed = this.canMoveToDiagonalNeighbours();
        switch (primaryDir) {
            case SOUTH:
                if (secondaryDir == EnumFacing.EAST && diagonalAllowed) {
                    this.targetAngles[EnumAngles.TURN.ordinal()] = -45F;
                    this.targetAngles[EnumAngles.HEAD.ordinal()] = 40F;
                } else if (secondaryDir == EnumFacing.WEST && diagonalAllowed) {
                    this.targetAngles[EnumAngles.TURN.ordinal()] = 45F;
                    this.targetAngles[EnumAngles.HEAD.ordinal()] = -40F;
                } else {
                    this.targetAngles[EnumAngles.TURN.ordinal()] = 0F;
                    this.targetAngles[EnumAngles.HEAD.ordinal()] = 90F;
                    diagonal = false;
                }
                break;
            case EAST:
                this.targetAngles[EnumAngles.TURN.ordinal()] = -90F;
                this.targetAngles[EnumAngles.HEAD.ordinal()] = 0F;
                diagonal = false;
                break;
            case NORTH:
                if (secondaryDir == EnumFacing.EAST && diagonalAllowed) {
                    this.targetAngles[EnumAngles.TURN.ordinal()] = -135F;
                    this.targetAngles[EnumAngles.HEAD.ordinal()] = -40F;
                } else if (secondaryDir == EnumFacing.WEST && diagonalAllowed) {
                    this.targetAngles[EnumAngles.TURN.ordinal()] = 135F;
                    this.targetAngles[EnumAngles.HEAD.ordinal()] = 40F;
                } else {
                    this.targetAngles[EnumAngles.TURN.ordinal()] = 180F;
                    this.targetAngles[EnumAngles.HEAD.ordinal()] = 90F;
                    diagonal = false;
                }
                break;
            case WEST:
                this.targetAngles[EnumAngles.TURN.ordinal()] = 90F;
                this.targetAngles[EnumAngles.HEAD.ordinal()] = 0F;
                diagonal = false;
                break;
        }
        if (diagonal) {
            this.targetAngles[EnumAngles.BASE.ordinal()] = 160F;
            this.targetAngles[EnumAngles.MIDDLE.ordinal()] = -85F;
            this.targetAngles[EnumAngles.TAIL.ordinal()] = -20F;
        } else {
            this.targetAngles[EnumAngles.BASE.ordinal()] = 100F;
            this.targetAngles[EnumAngles.MIDDLE.ordinal()] = -10F;
            this.targetAngles[EnumAngles.TAIL.ordinal()] = 0F;
        }
        return diagonal;
    }

    boolean hoverOverTarget() {
        if (this.targetDirection == null) return false;

        return this.hoverOverNeighbour(this.targetDirection);
    }

    private boolean hoverOverNeighbour(EnumFacing[] directions) {
        this.hoverOverNeighbour(directions[0], directions[1]);
        return this.isDoneMoving();
    }

    void hoverOverNeighbour(EnumFacing primaryDir, EnumFacing secondaryDir) {
        boolean diagonal = this.gotoNeighbour(primaryDir, secondaryDir);
        if (diagonal) {
            this.targetAngles[EnumAngles.BASE.ordinal()] = 160F;
            this.targetAngles[EnumAngles.MIDDLE.ordinal()] = -95F;
            this.targetAngles[EnumAngles.TAIL.ordinal()] = -10F;
        } else {
            this.targetAngles[EnumAngles.BASE.ordinal()] = 100F;
            this.targetAngles[EnumAngles.MIDDLE.ordinal()] = -20F;
            this.targetAngles[EnumAngles.TAIL.ordinal()] = 10F;
        }
    }

    TileEntity getTileEntityForCurrentDirection() {
        return this.getTileEntityForDirection(this.targetDirection[0], this.targetDirection[1]);
    }

    public TileEntity getTileEntityForDirection(EnumFacing[] directions) {
        return this.getTileEntityForDirection(directions[0], directions[1]);
    }

    private TileEntity getTileEntityForDirection(EnumFacing firstDir, EnumFacing secondDir) {
        BlockPos pos = this.getPos().offset(firstDir);
        return this.getWorld().getTileEntity(secondDir == null ? pos : pos.offset(secondDir));
    }

    boolean isDoneMoving() {
        for (int i = 0; i < 5; i++) {
            if (!PneumaticCraftUtils.areFloatsEqual(this.angles[i], this.targetAngles[i])) return false;
        }
        return true;
    }

    public boolean isDoneRotatingYaw() {
        return PneumaticCraftUtils.areFloatsEqual(this.angles[EnumAngles.TURN.ordinal()], this.targetAngles[EnumAngles.TURN.ordinal()]);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        for (int i = 0; i < 5; i++) {
            this.angles[i] = tag.getFloat("angle" + i);
            this.targetAngles[i] = tag.getFloat("targetAngle" + i);
        }
        this.slowMode = tag.getBoolean("slowMode");
        this.speed = tag.getFloat("speed");
        this.targetDirection[0] = tag.hasKey("targetDir1") ? EnumFacing.VALUES[tag.getInteger("targetDir1")] : null;
        this.targetDirection[1] = tag.hasKey("targetDir2") ? EnumFacing.VALUES[tag.getInteger("targetDir2")] : null;

    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        for (int i = 0; i < 5; i++) {
            tag.setFloat("angle" + i, this.angles[i]);
            tag.setFloat("targetAngle" + i, this.targetAngles[i]);
        }
        tag.setBoolean("slowMode", this.slowMode);
        tag.setFloat("speed", this.speed);

        if (this.targetDirection != null) {
            if (this.targetDirection[0] != null) tag.setInteger("targetDir1", this.targetDirection[0].ordinal());

            if (this.targetDirection[1] != null) tag.setInteger("targetDir2", this.targetDirection[1].ordinal());
        }
        return tag;
    }

    public abstract boolean canMoveToDiagonalNeighbours();

    EnumFacing[] getPlatformDirection() {
        for (EnumFacing dir : EnumFacing.HORIZONTALS) {
            if (this.getWorld().getTileEntity(this.getPos().offset(dir)) instanceof TileEntityAssemblyPlatform)
                return new EnumFacing[]{dir, null};
        }
        if (this.canMoveToDiagonalNeighbours()) {
            for (EnumFacing secDir : new EnumFacing[]{EnumFacing.WEST, EnumFacing.EAST}) {
                for (EnumFacing primDir : new EnumFacing[]{EnumFacing.NORTH, EnumFacing.SOUTH}) {
                    if (this.getWorld().getTileEntity(this.getPos().offset(primDir).offset(secDir)) instanceof TileEntityAssemblyPlatform) {
                        return new EnumFacing[]{primDir, secDir};
                    }
                }
            }
        }
        return null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(this.getPos().getX() - 1, this.getPos().getY() - 1, this.getPos().getZ() - 1, this.getPos().getX() + 2, this.getPos().getY() + 2, this.getPos().getZ() + 2);
    }

    @Override
    public void setSpeed(float speed) {
        this.speed = speed;
    }
}
