package me.desht.pneumaticcraft.common.recipes;

import me.desht.pneumaticcraft.api.recipe.IThermopneumaticProcessingPlantRecipe;
import me.desht.pneumaticcraft.common.fluid.Fluids;
import me.desht.pneumaticcraft.common.heat.HeatExchangerLogicAmbient;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class BasicThermopneumaticProcessingPlantRecipe implements IThermopneumaticProcessingPlantRecipe {
    public static final List<IThermopneumaticProcessingPlantRecipe> recipes = new ArrayList<>();

    private final FluidStack inputLiquid, outputLiquid;
    private final ItemStack inputItem;
    private final float requiredPressure;
    private final double requiredTemperature;

    public BasicThermopneumaticProcessingPlantRecipe(FluidStack inputLiquid, @Nonnull ItemStack inputItem,
                                                     FluidStack outputLiquid, double requiredTemperature, float requiredPressure) {
        this.inputItem = inputItem;
        this.inputLiquid = inputLiquid;
        this.outputLiquid = outputLiquid;
        this.requiredTemperature = requiredTemperature;
        this.requiredPressure = requiredPressure;
    }

    @Override
    public boolean isValidRecipe(FluidStack fluidStack, @Nonnull ItemStack inputItem) {
        if (this.inputLiquid != null) {
            if (fluidStack == null) return false;
            if (!Fluids.areFluidsEqual(fluidStack.getFluid(), this.inputLiquid.getFluid())) return false;
            if (fluidStack.amount < this.inputLiquid.amount) return false;
        }
        if (!this.inputItem.isEmpty()) {
            if (inputItem.isEmpty()) return false;
            if (!inputItem.isItemEqual(this.inputItem) && !PneumaticCraftUtils.isSameOreDictStack(inputItem, this.inputItem))
                return false;
            return inputItem.getCount() >= this.inputItem.getCount();
        }
        return true;
    }

    @Override
    public boolean isValidInput(FluidStack inputFluid) {
        return this.inputLiquid != null && Fluids.areFluidsEqual(inputFluid.getFluid(), this.inputLiquid.getFluid());
    }

    @Override
    public boolean isValidInput(ItemStack inputItem) {
        return !this.inputItem.isEmpty() && inputItem.isItemEqual(this.inputItem) || PneumaticCraftUtils.isSameOreDictStack(inputItem, this.inputItem);
    }

    @Override
    public FluidStack getRecipeOutput(FluidStack inputTank, @Nonnull ItemStack inputItem) {
        return this.outputLiquid;
    }

    @Override
    public void useRecipeItems(FluidStack inputTank, @Nonnull ItemStack inputItem) {
        if (this.inputLiquid != null) inputTank.amount -= this.inputLiquid.amount;
        if (!this.inputItem.isEmpty()) inputItem.shrink(this.inputItem.getCount());
    }

    @Override
    public void useResources(IFluidHandler fluidHandler, IItemHandler itemHandler) {
        if (this.inputLiquid != null) fluidHandler.drain(this.inputLiquid.amount, true);
        itemHandler.extractItem(0, this.inputItem.getCount(), false);
    }

    @Override
    public double getRequiredTemperature(FluidStack inputTank, @Nonnull ItemStack inputItem) {
        return this.requiredTemperature;
    }

    @Override
    public float getRequiredPressure(FluidStack inputTank, @Nonnull ItemStack inputItem) {
        return this.requiredPressure;
    }

    @Override
    public double heatUsed(FluidStack inputTank, @Nonnull ItemStack inputItem) {
        return (this.requiredTemperature - HeatExchangerLogicAmbient.BASE_AMBIENT_TEMP) / 10D;
    }

    @Override
    public int airUsed(FluidStack inputTank, @Nonnull ItemStack inputItem) {
        return (int) (this.requiredPressure * 50);
    }

    public FluidStack getInputLiquid() {
        return this.inputLiquid;
    }

    public FluidStack getOutputLiquid() {
        return this.outputLiquid;
    }

    @Nonnull
    public ItemStack getInputItem() {
        return this.inputItem;
    }
}
