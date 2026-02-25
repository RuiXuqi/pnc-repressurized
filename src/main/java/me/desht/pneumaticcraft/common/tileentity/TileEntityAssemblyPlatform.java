package me.desht.pneumaticcraft.common.tileentity;

import me.desht.pneumaticcraft.common.inventory.handler.BaseItemStackHandler;
import me.desht.pneumaticcraft.common.network.DescSynced;
import me.desht.pneumaticcraft.common.network.LazySynced;
import me.desht.pneumaticcraft.common.recipes.programs.AssemblyProgram;
import me.desht.pneumaticcraft.lib.TileEntityConstants;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;

public class TileEntityAssemblyPlatform extends TileEntityTickableBase implements IAssemblyMachine, IResettable {
    @DescSynced
    private boolean shouldClawClose;
    @DescSynced
    @LazySynced
    public float clawProgress;
    public float oldClawProgress;
    @DescSynced
    private final ItemStackHandler inventory = new BaseItemStackHandler(this, 1);
    private float speed = 1.0F;
    private BlockPos controllerPos;

    @Override
    public void update() {
        super.update();
        this.oldClawProgress = this.clawProgress;
        if (!this.shouldClawClose && this.clawProgress > 0F) {
            this.clawProgress = Math.max(this.clawProgress - TileEntityConstants.ASSEMBLY_IO_UNIT_CLAW_SPEED * this.speed, 0);
        } else if (this.shouldClawClose && this.clawProgress < 1F) {
            this.clawProgress = Math.min(this.clawProgress + TileEntityConstants.ASSEMBLY_IO_UNIT_CLAW_SPEED * this.speed, 1);
        }
    }

    private boolean isClawDone() {
        return this.clawProgress == (this.shouldClawClose ? 1F : 0F);
    }

    @Override
    public boolean isIdle() {
        return !this.shouldClawClose && this.isClawDone() && this.getHeldStack().isEmpty();
    }

    @Override
    public boolean reset() {
        this.openClaw();
        return this.isIdle();
    }

    boolean closeClaw() {
        this.shouldClawClose = true;
        this.sendDescriptionPacket();
        return this.isClawDone();
    }

    boolean openClaw() {
        this.shouldClawClose = false;
        this.sendDescriptionPacket();
        return this.isClawDone();
    }

    @Nonnull
    public ItemStack getHeldStack() {
        return this.inventory.getStackInSlot(0);
    }

    public void setHeldStack(@Nonnull ItemStack stack) {
        this.inventory.setStackInSlot(0, stack);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setBoolean("clawClosing", this.shouldClawClose);
        tag.setFloat("clawProgress", this.clawProgress);
        tag.setFloat("speed", this.speed);
        tag.setTag("Items", this.inventory.serializeNBT());
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.shouldClawClose = tag.getBoolean("clawClosing");
        this.clawProgress = tag.getFloat("clawProgress");
        this.speed = tag.getFloat("speed");
        this.inventory.deserializeNBT(tag.getCompoundTag("Items"));
    }

    @Override
    public void setSpeed(float speed) {
        this.speed = speed;
    }

    @Override
    public AssemblyProgram.EnumMachine getAssemblyType() {
        return AssemblyProgram.EnumMachine.PLATFORM;
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

    private void invalidateSystem() {
        if (this.controllerPos != null) {
            TileEntity te = this.getWorld().getTileEntity(this.controllerPos);
            if (te instanceof TileEntityAssemblyController) {
                ((TileEntityAssemblyController) te).invalidateAssemblySystem();
            }
        }
    }
}
