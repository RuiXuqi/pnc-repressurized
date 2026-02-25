package me.desht.pneumaticcraft.common.tileentity;

import com.google.common.collect.ImmutableMap;
import me.desht.pneumaticcraft.api.item.IItemRegistry.EnumUpgrade;
import me.desht.pneumaticcraft.client.util.ClientUtils;
import me.desht.pneumaticcraft.common.PneumaticCraftAPIHandler;
import me.desht.pneumaticcraft.common.block.Blockss;
import me.desht.pneumaticcraft.common.inventory.handler.BaseItemStackHandler;
import me.desht.pneumaticcraft.common.network.DescSynced;
import me.desht.pneumaticcraft.common.network.GuiSynced;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public class TileEntityLiquidCompressor extends TileEntityPneumaticBase implements IRedstoneControlled, ISerializableTanks {
    public static final int INVENTORY_SIZE = 2;

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;

    @GuiSynced
    private final FluidTank tank = new FluidTank(PneumaticValues.NORMAL_TANK_CAPACITY);
    private final ItemStackHandler inventory = new BaseItemStackHandler(this, INVENTORY_SIZE) {
        @Override
        public boolean isItemValid(int slot, ItemStack itemStack) {
            return itemStack.isEmpty() || FluidUtil.getFluidHandler(itemStack) != null;
        }
    };
    @GuiSynced
    public int redstoneMode;
    private double internalFuelBuffer;
    @DescSynced
    @GuiSynced
    public boolean isProducing;

    public TileEntityLiquidCompressor() {
        this(5, 7, 5000);
    }

    public TileEntityLiquidCompressor(float dangerPressure, float criticalPressure, int volume) {
        super(dangerPressure, criticalPressure, volume, 4);
        this.addApplicableUpgrade(EnumUpgrade.SPEED);
    }

    public FluidTank getTank() {
        return this.tank;
    }

    @Override
    public IItemHandlerModifiable getPrimaryInventory() {
        return this.inventory;
    }

    private int getFuelValue(FluidStack fluid) {
        return fluid == null ? 0 : this.getFuelValue(fluid.getFluid());
    }

    private int getFuelValue(Fluid fluid) {
        Integer value = PneumaticCraftAPIHandler.getInstance().liquidFuels.get(fluid.getName());
        return value == null ? 0 : value;
    }

    @Override
    public void update() {
        super.update();

        if (!this.getWorld().isRemote) {
            this.processFluidItem(INPUT_SLOT, OUTPUT_SLOT);

            this.isProducing = false;
            if (this.redstoneAllows()) {
                int usageRate = (int) (this.getBaseProduction() * this.getSpeedUsageMultiplierFromUpgrades());
                if (this.internalFuelBuffer < usageRate) {
                    double fuelValue = this.getFuelValue(this.tank.getFluid()) / 1000D;
                    if (fuelValue > 0) {
                        int usedFuel = Math.min(this.tank.getFluidAmount(), (int) (usageRate / fuelValue) + 1);
                        this.tank.drain(usedFuel, true);
                        this.internalFuelBuffer += usedFuel * fuelValue;
                    }
                }
                if (this.internalFuelBuffer >= usageRate) {
                    this.isProducing = true;
                    this.internalFuelBuffer -= usageRate;
                    this.onFuelBurn(usageRate);
                    this.addAir((int) (this.getBaseProduction() * this.getSpeedMultiplierFromUpgrades() * this.getEfficiency() / 100));
                }
            }
        } else {
            if (this.isProducing && this.world.rand.nextInt(5) == 0) {
                ClientUtils.emitParticles(this.getWorld(), this.getPos(), EnumParticleTypes.SMOKE_NORMAL);
            }
        }
    }

    protected void onFuelBurn(int burnedFuel) {
    }

    public int getEfficiency() {
        return 100;
    }

    public int getBaseProduction() {
        return 10;
    }

    @Override
    public boolean isConnectedTo(EnumFacing dir) {
        EnumFacing orientation = this.getRotation();
        return orientation == dir || orientation == dir.getOpposite() || dir == EnumFacing.UP;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setTag("Items", this.inventory.serializeNBT());
        tag.setByte("redstoneMode", (byte) this.redstoneMode);

        NBTTagCompound tankTag = new NBTTagCompound();
        this.tank.writeToNBT(tankTag);
        tag.setTag("tank", tankTag);

        tag.setDouble("internalFuelBuffer", this.internalFuelBuffer);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.inventory.deserializeNBT(tag.getCompoundTag("Items"));
        this.redstoneMode = tag.getByte("redstoneMode");
        this.tank.readFromNBT(tag.getCompoundTag("tank"));

        this.internalFuelBuffer = tag.getDouble("internalFuelBuffer");
    }

    @Override
    public String getName() {
        return Blockss.LIQUID_COMPRESSOR.getTranslationKey();
    }

    @Override
    public void handleGUIButtonPress(int buttonID, EntityPlayer player) {
        if (buttonID == 0) {
            this.redstoneMode++;
            if (this.redstoneMode > 2) this.redstoneMode = 0;
        }
    }

    @Override
    public int getRedstoneMode() {
        return this.redstoneMode;
    }


    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this.tank);
        } else {
            return super.getCapability(capability, facing);
        }
    }

    @Nonnull
    @Override
    public Map<String, FluidTank> getSerializableTanks() {
        return ImmutableMap.of("Tank", this.tank);
    }
}
