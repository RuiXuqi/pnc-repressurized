package me.desht.pneumaticcraft.common.tileentity;

import me.desht.pneumaticcraft.common.network.DescSynced;
import me.desht.pneumaticcraft.common.recipes.AssemblyRecipe;
import me.desht.pneumaticcraft.common.recipes.programs.AssemblyProgram;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;

public class TileEntityAssemblyLaser extends TileEntityAssemblyRobot {
    @DescSynced
    public boolean isLaserOn;
    private int laserStep; //used to progressively draw a circle.
    private static final float ITEM_SIZE = 10F;

    @Override
    public void update() {
        super.update();
        if (this.laserStep > 0) {
            EnumFacing[] platformDirection = this.getPlatformDirection();
            if (platformDirection == null) {
                this.laserStep = 105;
            }
            switch (this.laserStep) {
                case 1:
                    //                    isLaserOn = false;
                    this.slowMode = false;
                    //                    gotoHomePosition();
                    break;
                case 2:
                    this.hoverOverNeighbour(platformDirection[0], platformDirection[1]);
                    break;
                case 3:
                    this.slowMode = true;
                    this.gotoNeighbour(platformDirection[0], platformDirection[1]);
                    break;
                case 104:
                    this.hoverOverNeighbour(platformDirection[0], platformDirection[1]);
                    this.isLaserOn = false;
                    this.slowMode = true;
                    TileEntity te = this.getTileEntityForCurrentDirection();
                    if (te instanceof TileEntityAssemblyPlatform) {
                        TileEntityAssemblyPlatform platform = (TileEntityAssemblyPlatform) te;
                        ItemStack output = getLaseredOutputForItem(platform.getHeldStack());
                        if (!output.isEmpty()) {
                            platform.setHeldStack(output);
                        }
                    }
                    break;
                case 105:
                    this.slowMode = false;
                    this.isLaserOn = false;
                    this.gotoHomePosition();
                    break;
                default: //4-103
                    this.isLaserOn = true;
                    this.slowMode = false;
                    this.targetAngles[EnumAngles.BASE.ordinal()] = 100F - (float) PneumaticCraftUtils.sin[(this.laserStep - 4) * PneumaticCraftUtils.CIRCLE_POINTS / 100] * ITEM_SIZE;
                    this.targetAngles[EnumAngles.MIDDLE.ordinal()] = -10F + (float) PneumaticCraftUtils.sin[(this.laserStep - 4) * PneumaticCraftUtils.CIRCLE_POINTS / 100] * ITEM_SIZE;
                    this.targetAngles[EnumAngles.TAIL.ordinal()] = 0F;
                    this.targetAngles[EnumAngles.TURN.ordinal()] += (float) PneumaticCraftUtils.sin[(this.laserStep - 4) * PneumaticCraftUtils.CIRCLE_POINTS / 100] * ITEM_SIZE * 0.03D;
                    break;
            }
            if (this.isDoneInternal() || this.laserStep >= 4 && this.laserStep <= 103) {
                this.laserStep++;
                if (this.laserStep > 105) this.laserStep = 0;
            }
        }
    }

    public void startLasering() {
        if (this.laserStep == 0) {
            this.laserStep = 1;
        }
    }

    @Override
    public boolean gotoNeighbour(EnumFacing primaryDir, EnumFacing secondaryDir) {
        boolean diagonal = super.gotoNeighbour(primaryDir, secondaryDir);
        this.targetAngles[EnumAngles.TURN.ordinal()] -= ITEM_SIZE * 0.45D;
        return diagonal;
    }

    private boolean isDoneInternal() {
        return super.isDoneMoving();
    }

    @Override
    public boolean isIdle() {
        return this.laserStep == 0 && this.isDoneInternal();
    }

    @Override
    public AssemblyProgram.EnumMachine getAssemblyType() {
        return AssemblyProgram.EnumMachine.LASER;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setBoolean("laser", this.isLaserOn);
        tag.setInteger("laserStep", this.laserStep);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.isLaserOn = tag.getBoolean("laser");
        this.laserStep = tag.getInteger("laserStep");
    }

    @Override
    public boolean canMoveToDiagonalNeighbours() {
        return false;
    }

    @Nonnull
    private static ItemStack getLaseredOutputForItem(ItemStack input) {
        for (AssemblyRecipe recipe : AssemblyRecipe.laserRecipes) {
            if (AssemblyProgram.isValidInput(recipe, input)) return recipe.getOutput().copy();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean reset() {
        if (this.isIdle()) {
            return true;
        } else {
            this.isLaserOn = false;
            this.laserStep = 105;
            return false;
        }
    }
}
