package me.desht.pneumaticcraft.common.tileentity;

import me.desht.pneumaticcraft.common.network.DescSynced;
import me.desht.pneumaticcraft.common.network.LazySynced;
import me.desht.pneumaticcraft.common.recipes.AssemblyRecipe;
import me.desht.pneumaticcraft.common.recipes.programs.AssemblyProgram;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.TileEntityConstants;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

public class TileEntityAssemblyDrill extends TileEntityAssemblyRobot {
    @DescSynced
    private boolean isDrillOn;
    @DescSynced
    @LazySynced
    private float drillSpeed;
    public float drillRotation;
    public float oldDrillRotation;
    private int drillStep;

    @Override
    public void update() {
        this.oldDrillRotation = this.drillRotation;
        super.update();
        if (this.isDrillOn) {
            this.drillSpeed = Math.min(this.drillSpeed + TileEntityConstants.ASSEMBLY_DRILL_ACCELERATION * this.speed, TileEntityConstants.ASSEMBLY_DRILL_MAX_SPEED);
        } else {
            this.drillSpeed = Math.max(this.drillSpeed - TileEntityConstants.ASSEMBLY_DRILL_ACCELERATION, 0);
        }
        this.drillRotation += this.drillSpeed;
        while (this.drillRotation >= 360) {
            this.drillRotation -= 360;
        }

        if (!this.getWorld().isRemote && this.drillStep > 0) {
            EnumFacing[] platformDirection = this.getPlatformDirection();
            if (platformDirection == null) this.drillStep = 1;
            switch (this.drillStep) {
                case 1:
                    this.slowMode = false;
                    this.gotoHomePosition();
                    break;
                case 2:
                    this.hoverOverNeighbour(platformDirection[0], platformDirection[1]);
                    break;
                case 3:
                    this.isDrillOn = true;
                    break;
                case 4:
                    this.slowMode = true;
                    this.gotoNeighbour(platformDirection[0], platformDirection[1]);
                    break;
                case 5:
                    this.hoverOverNeighbour(platformDirection[0], platformDirection[1]);
                    this.isDrillOn = false;
                    TileEntity te = this.getTileEntityForCurrentDirection();
                    if (te instanceof TileEntityAssemblyPlatform) {
                        TileEntityAssemblyPlatform platform = (TileEntityAssemblyPlatform) te;
                        ItemStack output = getDrilledOutputForItem(platform.getHeldStack());
                        if (!output.isEmpty()) {
                            platform.setHeldStack(output);
                        }
                    }
                    break;
                case 6:
                    this.slowMode = false;
                    this.gotoHomePosition();
                    break;
            }
            if (this.isDoneInternal()) {
                this.drillStep++;
                if (this.drillStep > 6) this.drillStep = 0;
            }
        }

    }

    public void goDrilling() {
        if (this.drillStep == 0) {
            this.drillStep = 1;
            this.markDirty();
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setBoolean("drill", this.isDrillOn);
        tag.setFloat("drillSpeed", this.drillSpeed);
        tag.setInteger("drillStep", this.drillStep);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.isDrillOn = tag.getBoolean("drill");
        this.drillSpeed = tag.getFloat("drillSpeed");
        this.drillStep = tag.getInteger("drillStep");
    }

    @Override
    public boolean isIdle() {
        return this.drillStep == 0 && this.isDoneInternal();
    }

    @Override
    public AssemblyProgram.EnumMachine getAssemblyType() {
        return AssemblyProgram.EnumMachine.DRILL;
    }

    private boolean isDoneInternal() {
        if (super.isDoneMoving()) {
            return this.isDrillOn ? this.drillSpeed > TileEntityConstants.ASSEMBLY_DRILL_MAX_SPEED - 1F : PneumaticCraftUtils.areFloatsEqual(this.drillSpeed, 0F);
        } else {
            return false;
        }
    }

    @Override
    public boolean canMoveToDiagonalNeighbours() {
        return false;
    }

    private static ItemStack getDrilledOutputForItem(ItemStack input) {
        for (AssemblyRecipe recipe : AssemblyRecipe.drillRecipes) {
            if (AssemblyProgram.isValidInput(recipe, input)) return recipe.getOutput().copy();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean reset() {
        if (this.isIdle()) return true;
        else {
            this.isDrillOn = false;
            this.drillStep = 6;
            return false;
        }
    }

}
