package me.desht.pneumaticcraft.common.tileentity;

import com.google.common.collect.ImmutableMap;
import me.desht.pneumaticcraft.api.PneumaticRegistry;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import me.desht.pneumaticcraft.api.item.IItemRegistry.EnumUpgrade;
import me.desht.pneumaticcraft.api.recipe.IThermopneumaticProcessingPlantRecipe;
import me.desht.pneumaticcraft.api.tileentity.IHeatExchanger;
import me.desht.pneumaticcraft.client.util.ClientUtils;
import me.desht.pneumaticcraft.common.block.Blockss;
import me.desht.pneumaticcraft.common.inventory.handler.BaseItemStackHandler;
import me.desht.pneumaticcraft.common.network.DescSynced;
import me.desht.pneumaticcraft.common.network.GuiSynced;
import me.desht.pneumaticcraft.common.network.LazySynced;
import me.desht.pneumaticcraft.common.recipes.BasicThermopneumaticProcessingPlantRecipe;
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
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public class TileEntityThermopneumaticProcessingPlant extends TileEntityPneumaticBase
        implements IHeatExchanger, IMinWorkingPressure, IRedstoneControlled, ISerializableTanks, ISmartFluidSync, IAutoFluidEjecting {

    private static final int INVENTORY_SIZE = 1;
    private static final int CRAFTING_TIME = 60 * 100;
    private static final double MAX_SPEED_UP = 2.5;

    @GuiSynced
    @DescSynced
    @LazySynced
    private final ThermopneumaticFluidTankInput inputTank = new ThermopneumaticFluidTankInput(PneumaticValues.NORMAL_TANK_CAPACITY);
    @GuiSynced
    @DescSynced
    @LazySynced
    private final ThermopneumaticFluidTankOutput outputTank = new ThermopneumaticFluidTankOutput(PneumaticValues.NORMAL_TANK_CAPACITY);
    @GuiSynced
    private final IHeatExchangerLogic heatExchanger = PneumaticRegistry.getInstance().getHeatRegistry().getHeatExchangerLogic();
    @GuiSynced
    public int redstoneMode;
    @GuiSynced
    private int craftingProgress;
    @GuiSynced
    public boolean hasRecipe;
    @GuiSynced
    private float requiredPressure;
    @GuiSynced
    public double requiredTemperature;
    @SuppressWarnings("unused")
    @DescSynced
    private int inputAmountScaled, outputAmountScaled;
    @DescSynced
    private boolean didWork;
    private IThermopneumaticProcessingPlantRecipe currentRecipe;
    private boolean searchForRecipe = true;

    private final ItemStackHandler handler = new BaseItemStackHandler(this, INVENTORY_SIZE) {
        @Override
        public boolean isItemValid(int slot, ItemStack itemStack) {
            return itemStack.isEmpty()
                    || BasicThermopneumaticProcessingPlantRecipe.recipes.stream().anyMatch(r -> r.isValidInput(itemStack));
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            TileEntityThermopneumaticProcessingPlant.this.searchForRecipe = true;
        }
    };
    private final ThermopneumaticFluidHandler fluidHandler = new ThermopneumaticFluidHandler();

    public TileEntityThermopneumaticProcessingPlant() {
        super(5, 7, 3000, 4);
        this.addApplicableUpgrade(EnumUpgrade.DISPENSER);
        this.heatExchanger.setThermalResistance(10);
    }

    @Override
    public boolean isConnectedTo(EnumFacing dir) {
        return this.getRotation().getOpposite() != dir && dir != EnumFacing.UP;
    }

    @Override
    public void update() {
        super.update();
        if (!this.getWorld().isRemote) {
            // bit of a kludge since inv/fluid changes aren't always reliably detected
            if (this.searchForRecipe || (this.getWorld().getTotalWorldTime() & 0xf) == 0) {
                this.currentRecipe = this.getValidRecipe();
                this.searchForRecipe = false;
            }
            this.hasRecipe = this.currentRecipe != null;
            this.didWork = false;
            if (this.hasRecipe) {
                ItemStack stackInSlot = this.handler.getStackInSlot(0);
                this.requiredPressure = this.currentRecipe.getRequiredPressure(this.inputTank.getFluid(), stackInSlot);
                this.requiredTemperature = this.currentRecipe.getRequiredTemperature(this.inputTank.getFluid(), stackInSlot);
                if (this.redstoneAllows() && this.heatExchanger.getTemperature() >= this.requiredTemperature && this.hasEnoughPressure()) {
                    double inc = this.requiredTemperature > 0 ? Math.min(MAX_SPEED_UP, this.heatExchanger.getTemperature() / this.requiredTemperature) : 1.0;
                    this.craftingProgress += inc * 100;
                    if (this.craftingProgress >= CRAFTING_TIME) {
                        this.outputTank.fill(this.currentRecipe.getRecipeOutput(this.inputTank.getFluid(), stackInSlot).copy(), true);
                        this.currentRecipe.useResources(this.inputTank, this.handler);
                        this.addAir(-this.currentRecipe.airUsed(this.inputTank.getFluid(), stackInSlot));
                        this.heatExchanger.addHeat(-this.currentRecipe.heatUsed(this.inputTank.getFluid(), stackInSlot) * inc * 0.75);
                        this.craftingProgress -= CRAFTING_TIME;
                    }
                    this.didWork = true;
                }
            } else {
                this.craftingProgress = 0;
                this.requiredTemperature = 0;
                this.requiredPressure = 0;
            }
        } else {
            if (this.didWork && this.getWorld().rand.nextBoolean()) {
                ClientUtils.emitParticles(this.getWorld(), this.getPos(), EnumParticleTypes.SMOKE_NORMAL);
            }
        }
    }

    private boolean hasEnoughPressure() {
        if (this.getMinWorkingPressure() == 0) {
            return true;
        } else if (this.getMinWorkingPressure() > 0) {
            return this.getPressure() >= this.getMinWorkingPressure();
        } else {
            return this.getPressure() <= this.getMinWorkingPressure();
        }
    }

    private IThermopneumaticProcessingPlantRecipe getValidRecipe() {
        for (IThermopneumaticProcessingPlantRecipe recipe : BasicThermopneumaticProcessingPlantRecipe.recipes) {
            if (recipe.isValidRecipe(this.inputTank.getFluid(), this.handler.getStackInSlot(0))) {
                if (this.outputTank.getFluid() == null) {
                    return recipe;
                } else {
                    FluidStack output = recipe.getRecipeOutput(this.inputTank.getFluid(), this.handler.getStackInSlot(0));
                    if (output.getFluid() == this.outputTank.getFluid().getFluid() && output.amount <= this.outputTank.getCapacity() - this.outputTank.getFluidAmount()) {
                        return recipe;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public IItemHandlerModifiable getPrimaryInventory() {
        return this.handler;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY
                || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this.fluidHandler);
        }
        return super.getCapability(capability, facing);
    }

    public FluidTank getInputTank() {
        return this.inputTank;
    }

    public FluidTank getOutputTank() {
        return this.outputTank;
    }

    @SideOnly(Side.CLIENT)
    public double getCraftingPercentage() {
        return (double) this.craftingProgress / CRAFTING_TIME;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);

        tag.setTag("Items", this.handler.serializeNBT());
        tag.setByte("redstoneMode", (byte) this.redstoneMode);
        tag.setInteger("craftingProgress", this.craftingProgress);

        NBTTagCompound tankTag = new NBTTagCompound();
        this.inputTank.writeToNBT(tankTag);
        tag.setTag("inputTank", tankTag);

        tankTag = new NBTTagCompound();
        this.outputTank.writeToNBT(tankTag);
        tag.setTag("outputTank", tankTag);

        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.handler.deserializeNBT(tag.getCompoundTag("Items"));
        this.redstoneMode = tag.getByte("redstoneMode");
        this.craftingProgress = tag.getInteger("craftingProgress");
        this.inputTank.readFromNBT(tag.getCompoundTag("inputTank"));
        this.inputAmountScaled = this.inputTank.getScaledFluidAmount();
        this.outputTank.readFromNBT(tag.getCompoundTag("outputTank"));
        this.outputAmountScaled = this.outputTank.getScaledFluidAmount();
    }

    @Override
    public IHeatExchangerLogic getHeatExchangerLogic(EnumFacing side) {
        return this.heatExchanger;
    }

    @Override
    public void handleGUIButtonPress(int buttonID, EntityPlayer player) {
        if (buttonID == 0) {
            this.redstoneMode++;
            if (this.redstoneMode > 2) this.redstoneMode = 0;
        } else if (buttonID == 1) {
            // move input fluid to output if poss.
            FluidUtil.tryFluidTransfer(this.outputTank, this.inputTank, this.inputTank.getFluidAmount(), true);
        } else if (buttonID == 2) {
            // dump input fluid
            this.inputTank.drain(this.inputTank.getCapacity(), true);
        }
    }

    @Override
    public int getRedstoneMode() {
        return this.redstoneMode;
    }

    @Override
    public float getMinWorkingPressure() {
        return this.requiredPressure;
    }

    @Override
    public String getName() {
        return Blockss.THERMOPNEUMATIC_PROCESSING_PLANT.getTranslationKey();
    }

    @Nonnull
    @Override
    public Map<String, FluidTank> getSerializableTanks() {
        return ImmutableMap.of("InputTank", this.inputTank, "OutputTank", this.outputTank);
    }

    @Override
    public void updateScaledFluidAmount(int tankIndex, int amount) {
        if (tankIndex == 1) {
            this.inputAmountScaled = amount;
        } else if (tankIndex == 2) {
            this.outputAmountScaled = amount;
        }
    }

    private class ThermopneumaticFluidTankInput extends SmartSyncTank {
        private Fluid prevFluid;

        ThermopneumaticFluidTankInput(int capacity) {
            super(TileEntityThermopneumaticProcessingPlant.this, capacity, 1);
        }

        @Override
        public boolean canFillFluidType(FluidStack fluid) {
            return fluid == null || BasicThermopneumaticProcessingPlantRecipe.recipes.stream().anyMatch(r -> r.isValidInput(fluid));
        }

        @Override
        protected void onContentsChanged() {
            super.onContentsChanged();
            Fluid newFluid = this.getFluid() == null ? null : this.getFluid().getFluid();
            if (this.prevFluid != newFluid) {
                TileEntityThermopneumaticProcessingPlant.this.searchForRecipe = true;
                this.prevFluid = newFluid;
            }
        }
    }

    private class ThermopneumaticFluidTankOutput extends SmartSyncTank {

        ThermopneumaticFluidTankOutput(int capacity) {
            super(TileEntityThermopneumaticProcessingPlant.this, capacity, 2);
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            FluidStack res = super.drain(resource, doDrain);
            if (doDrain && res != null && res.amount > 0) TileEntityThermopneumaticProcessingPlant.this.searchForRecipe = true;
            return res;
        }

        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            FluidStack res = super.drain(maxDrain, doDrain);
            if (doDrain && res != null && res.amount > 0) TileEntityThermopneumaticProcessingPlant.this.searchForRecipe = true;
            return res;
        }
    }

    private class ThermopneumaticFluidHandler implements IFluidHandler {
        @Override
        public IFluidTankProperties[] getTankProperties() {
            return ArrayUtils.addAll(TileEntityThermopneumaticProcessingPlant.this.inputTank.getTankProperties(), TileEntityThermopneumaticProcessingPlant.this.outputTank.getTankProperties());
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            return TileEntityThermopneumaticProcessingPlant.this.inputTank.fill(resource, doFill);
        }

        @Nullable
        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            return TileEntityThermopneumaticProcessingPlant.this.outputTank.getFluid() != null && TileEntityThermopneumaticProcessingPlant.this.outputTank.getFluid().isFluidEqual(resource) ? TileEntityThermopneumaticProcessingPlant.this.outputTank.drain(resource.amount, doDrain) : null;
        }

        @Nullable
        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            return TileEntityThermopneumaticProcessingPlant.this.outputTank.drain(maxDrain, doDrain);
        }
    }
}
