package me.desht.pneumaticcraft.common.tileentity;

import me.desht.pneumaticcraft.api.item.IItemRegistry.EnumUpgrade;
import me.desht.pneumaticcraft.api.tileentity.IAirHandler;
import me.desht.pneumaticcraft.common.block.BlockAirCompressor;
import me.desht.pneumaticcraft.common.block.Blockss;
import me.desht.pneumaticcraft.common.inventory.handler.BaseItemStackHandler;
import me.desht.pneumaticcraft.common.network.DescSynced;
import me.desht.pneumaticcraft.common.network.GuiSynced;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class TileEntityAirCompressor extends TileEntityPneumaticBase implements IRedstoneControlled {

    private static final int INVENTORY_SIZE = 1;
    private AirCompressorHandler inventory = new AirCompressorHandler();

    private static final int FUEL_SLOT = 0;

    @GuiSynced
    public int burnTime;
    @GuiSynced
    private int maxBurnTime; // in here the total burn time of the current burning item is stored.
    @GuiSynced
    public int redstoneMode = 0; // determines how the compressor responds to redstone.

    @DescSynced
    private boolean isActive;

    @GuiSynced
    public int curFuelUsage;

    private class AirCompressorHandler extends BaseItemStackHandler {
        AirCompressorHandler() {
            super(TileEntityAirCompressor.this, INVENTORY_SIZE);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack itemStack) {
            return slot == FUEL_SLOT &&
                    (itemStack.isEmpty() || TileEntityFurnace.isItemFuel(itemStack) && FluidUtil.getFluidContained(itemStack) == null);
        }

    }

    public TileEntityAirCompressor() {
        this(PneumaticValues.DANGER_PRESSURE_AIR_COMPRESSOR, PneumaticValues.MAX_PRESSURE_AIR_COMPRESSOR, PneumaticValues.VOLUME_AIR_COMPRESSOR);
    }

    public TileEntityAirCompressor(float dangerPressure, float criticalPressure, int volume) {
        super(dangerPressure, criticalPressure, volume, 4);
        this.addApplicableUpgrade(EnumUpgrade.SPEED);
    }

    public boolean isActive() {
        return this.isActive;
    }

    @Override
    public void update() {
        if (!this.getWorld().isRemote) {
            if (this.redstoneAllows() && this.burnTime < this.curFuelUsage && TileEntityFurnace.isItemFuel(this.inventory.getStackInSlot(FUEL_SLOT))) {
                ItemStack fuelStack = this.inventory.getStackInSlot(FUEL_SLOT);
                this.burnTime += TileEntityFurnace.getItemBurnTime(fuelStack);
                this.maxBurnTime = this.burnTime;
                fuelStack.shrink(1);
            }

            this.curFuelUsage = (int) (this.getBaseProduction() * this.getSpeedUsageMultiplierFromUpgrades() / 10);
            if (this.burnTime >= this.curFuelUsage) {
                this.burnTime -= this.curFuelUsage;
                if (!this.getWorld().isRemote) {
                    this.addAir((int) (this.getBaseProduction() * this.getSpeedMultiplierFromUpgrades() * this.getEfficiency() / 100D));
                    this.onFuelBurn(this.curFuelUsage);
                }
            }
            boolean wasActive = this.isActive;
            this.isActive = this.burnTime > this.curFuelUsage;
            if (wasActive != this.isActive) {
                this.getWorld().setBlockState(this.getPos(), this.getWorld().getBlockState(this.getPos()).withProperty(BlockAirCompressor.ON, this.isActive));
            }
        } else if (this.isActive) this.spawnBurningParticle();

        super.update();

        if (!this.getWorld().isRemote) {
            List<Pair<EnumFacing, IAirHandler>> teList = this.getAirHandler(null).getConnectedPneumatics();
            if (teList.size() == 0) this.getAirHandler(null).airLeak(this.getRotation());
        }
    }

    protected void onFuelBurn(int burnedFuel) {
    }

    public int getEfficiency() {
        return 100;
    }

    public int getBaseProduction() {
        return PneumaticValues.PRODUCTION_COMPRESSOR;
    }

    private void spawnBurningParticle() {
        if (this.getWorld().rand.nextInt(3) != 0) return;
        float px = this.getPos().getX() + 0.5F;
        float py = this.getPos().getY() + this.getWorld().rand.nextFloat() * 6.0F / 16.0F;
        float pz = this.getPos().getZ() + 0.5F;
        float f3 = 0.5F;
        float f4 = this.getWorld().rand.nextFloat() * 0.4F - 0.2F;
        switch (this.getRotation()) {
            case EAST:
                this.getWorld().spawnParticle(EnumParticleTypes.SMOKE_NORMAL, px - f3, py, pz + f4, 0.0D, 0.0D, 0.0D);
                this.getWorld().spawnParticle(EnumParticleTypes.FLAME, px - f3, py, pz + f4, 0.0D, 0.0D, 0.0D);
                break;
            case WEST:
                this.getWorld().spawnParticle(EnumParticleTypes.SMOKE_NORMAL, px + f3, py, pz + f4, 0.0D, 0.0D, 0.0D);
                this.getWorld().spawnParticle(EnumParticleTypes.FLAME, px + f3, py, pz + f4, 0.0D, 0.0D, 0.0D);
                break;
            case SOUTH:
                this.getWorld().spawnParticle(EnumParticleTypes.SMOKE_NORMAL, px + f4, py, pz - f3, 0.0D, 0.0D, 0.0D);
                this.getWorld().spawnParticle(EnumParticleTypes.FLAME, px + f4, py, pz - f3, 0.0D, 0.0D, 0.0D);
                break;
            case NORTH:
                this.getWorld().spawnParticle(EnumParticleTypes.SMOKE_NORMAL, px + f4, py, pz + f3, 0.0D, 0.0D, 0.0D);
                this.getWorld().spawnParticle(EnumParticleTypes.FLAME, px + f4, py, pz + f3, 0.0D, 0.0D, 0.0D);
                break;
        }
    }

    @Override
    public boolean isConnectedTo(EnumFacing side) {
        return this.getRotation() == side;
    }

    public int getBurnTimeRemainingScaled(int parts) {
        if (this.maxBurnTime == 0 || this.burnTime < this.curFuelUsage) return 0;
        return parts * this.burnTime / this.maxBurnTime;
    }

    @Override
    public void handleGUIButtonPress(int buttonID, EntityPlayer player) {
        if (buttonID == 0) {
            this.redstoneMode++;
            if (this.redstoneMode > 2) this.redstoneMode = 0;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(this.getPos().getX(), this.getPos().getY(), this.getPos().getZ(), this.getPos().getX() + 1, this.getPos().getY() + 1, this.getPos().getZ() + 1);
    }

    @Override
    public String getName() {
        return Blockss.AIR_COMPRESSOR.getTranslationKey();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbtTagCompound) {
        super.readFromNBT(nbtTagCompound);
        this.burnTime = nbtTagCompound.getInteger("burnTime");
        this.maxBurnTime = nbtTagCompound.getInteger("maxBurn");
        this.redstoneMode = nbtTagCompound.getInteger("redstoneMode");
        this.inventory = new AirCompressorHandler();
        this.inventory.deserializeNBT(nbtTagCompound.getCompoundTag("Items"));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbtTagCompound) {
        super.writeToNBT(nbtTagCompound);
        nbtTagCompound.setInteger("burnTime", this.burnTime);
        nbtTagCompound.setInteger("maxBurn", this.maxBurnTime);
        nbtTagCompound.setInteger("redstoneMode", this.redstoneMode);
        nbtTagCompound.setTag("Items", this.inventory.serializeNBT());
        return nbtTagCompound;
    }

    @Override
    public IItemHandlerModifiable getPrimaryInventory() {
        return this.inventory;
    }

    @Override
    public int getRedstoneMode() {
        return this.redstoneMode;
    }
}
