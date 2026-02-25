package me.desht.pneumaticcraft.common.tileentity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import me.desht.pneumaticcraft.api.PneumaticRegistry;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import me.desht.pneumaticcraft.api.tileentity.IHeatExchanger;
import me.desht.pneumaticcraft.common.block.Blockss;
import me.desht.pneumaticcraft.common.inventory.handler.BaseItemStackHandler;
import me.desht.pneumaticcraft.common.network.DescSynced;
import me.desht.pneumaticcraft.common.network.GuiSynced;
import me.desht.pneumaticcraft.common.network.LazySynced;
import me.desht.pneumaticcraft.common.recipes.PlasticMixerRegistry;
import me.desht.pneumaticcraft.common.recipes.PlasticMixerRegistry.PlasticMixerRecipe;
import me.desht.pneumaticcraft.common.thirdparty.computercraft.LuaMethod;
import me.desht.pneumaticcraft.common.thirdparty.computercraft.LuaMethodRegistry;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.oredict.DyeUtils;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class TileEntityPlasticMixer extends TileEntityTickableBase implements IHeatExchanger, IRedstoneControlled, ISerializableTanks, ISmartFluidSync {
    private static final List<String> REDSTONE_LABELS = ImmutableList.of(
            "gui.tab.redstoneBehaviour.button.anySignal",
            "gui.tab.redstoneBehaviour.button.highSignal",
            "gui.tab.redstoneBehaviour.button.lowSignal",
            "gui.tab.redstoneBehaviour.plasticMixer.button.selectOnSignal"
    );
    public static final int INVENTORY_SIZE = 5;
    public static final int DYE_BUFFER_MAX = 0xFF * 2 * PneumaticValues.NORMAL_TANK_CAPACITY / 1000;
    private static final int DYE_PER_DYE = 0xFF * 10;
    public static final int INV_INPUT = 0, INV_OUTPUT = 1, INV_DYE_RED = 2, INV_DYE_GREEN = 3, INV_DYE_BLUE = 4;

    @LazySynced
    @DescSynced
    @GuiSynced
    private final PlasticFluidTank tank = new PlasticFluidTank(PneumaticValues.NORMAL_TANK_CAPACITY);
    @SuppressWarnings("unused")
    @DescSynced
    private int fluidAmountScaled;

    private final ItemStackHandler inventory = new PlasticItemStackHandler();
    private final IItemHandlerModifiable inventoryPublic = new PlasticItemStackHandlerPublic();

    private int lastTickInventoryStacksize;
    @GuiSynced
    private final IHeatExchangerLogic hullHeatLogic = PneumaticRegistry.getInstance().getHeatRegistry().getHeatExchangerLogic();
    @GuiSynced
    private final IHeatExchangerLogic itemHeatLogic = PneumaticRegistry.getInstance().getHeatRegistry().getHeatExchangerLogic();
    @GuiSynced
    public int selectedPlastic = -1;
    @GuiSynced
    private int redstoneMode;
    @GuiSynced
    public boolean lockSelection;
    @GuiSynced
    public final int[] dyeBuffers = new int[3];

    public TileEntityPlasticMixer() {
        super(4);
        this.hullHeatLogic.addConnectedExchanger(this.itemHeatLogic);
        this.hullHeatLogic.setThermalCapacity(100);
    }

    @SideOnly(Side.CLIENT)
    public IHeatExchangerLogic getLogic(int index) {
        switch (index) {
            case 0:
                return this.hullHeatLogic;
            case 1:
                return this.itemHeatLogic;
        }
        throw new IllegalArgumentException("Invalid index: " + index);
    }

    public IFluidTank getTank() {
        return this.tank;
    }

    @Override
    public IItemHandlerModifiable getPrimaryInventory() {
        return this.inventory;
    }

    @Override
    public String getName() {
        return Blockss.PLASTIC_MIXER.getTranslationKey();
    }

    @Override
    protected void onFirstServerUpdate() {
        super.onFirstServerUpdate();


        this.itemHeatLogic.initializeAmbientTemperature(this.world, this.pos);
    }

    @Override
    public void update() {
        super.update();

        if (!this.getWorld().isRemote) {
            this.refillDyeBuffers();
            this.itemHeatLogic.update();

            ItemStack inputStack = this.inventory.getStackInSlot(INV_INPUT);
            if (this.getWorld().getTotalWorldTime() % 20 == 0) { // We don't need to run _that_ often.
                if (inputStack.getCount() > this.lastTickInventoryStacksize) {
                    int stackIncrease = inputStack.getCount() - this.lastTickInventoryStacksize;
                    double heatingRatio = (double) inputStack.getCount() / (inputStack.getCount() + stackIncrease);
                    this.itemHeatLogic.setTemperature((int) (heatingRatio * this.itemHeatLogic.getTemperature() + (1 - heatingRatio) * this.hullHeatLogic.getAmbientTemperature()));
                }

                if (!inputStack.isEmpty()) {
                    this.tryMeltPlastic(inputStack);
                }

                this.lastTickInventoryStacksize = this.inventory.getStackInSlot(INV_INPUT).getCount();
                this.itemHeatLogic.setThermalCapacity(this.lastTickInventoryStacksize);
            }
            if (this.tank.getFluid() != null && this.selectedPlastic >= 0 && this.redstoneAllows()) {
                this.trySolidifyPlastic();
            }
            if (!this.lockSelection) {
                this.selectedPlastic = -1;
            }
            if (this.redstoneMode == 3) {
                this.selectedPlastic = this.poweredRedstone;
            }
        }
    }

    private void tryMeltPlastic(ItemStack inputStack) {
        PlasticMixerRecipe recipe = PlasticMixerRegistry.INSTANCE.getRecipe(inputStack);
        if (recipe != null && recipe.allowMelting() && this.itemHeatLogic.getTemperature() >= recipe.getTemperature()) {
            FluidStack moltenPlastic = recipe.getFluidStack().copy();
            int filled = this.tank.fill(moltenPlastic, false);
            if (filled == moltenPlastic.amount) {
                this.inventory.extractItem(INV_INPUT, 1, false);
                this.tank.fill(moltenPlastic, true);
                this.itemHeatLogic.addHeat(-1);
            }
        }
    }

    private void trySolidifyPlastic() {
        PlasticMixerRecipe recipe = PlasticMixerRegistry.INSTANCE.getRecipe(this.tank.getFluid());
        if (recipe != null && recipe.allowSolidifying() && (recipe.getMeta() < 0 || recipe.getMeta() == this.selectedPlastic)) {
            ItemStack solidifiedStack = ItemHandlerHelper.copyStackWithSize(recipe.getItemStack(), 1);
            solidifiedStack.setItemDamage(this.selectedPlastic);
            if (this.inventory.getStackInSlot(INV_OUTPUT).isEmpty()) {
                solidifiedStack.setCount(recipe.useDye() ? this.useDye(solidifiedStack.getCount()) : solidifiedStack.getCount());
                if (solidifiedStack.getCount() > 0) {
                    this.inventory.setStackInSlot(INV_OUTPUT, solidifiedStack);
                    this.tank.drain(solidifiedStack.getCount() * recipe.getFluidStack().amount, true);
                }
            } else if (solidifiedStack.isItemEqual(this.inventory.getStackInSlot(INV_OUTPUT))) {
                int solidifiedItems = Math.min(solidifiedStack.getMaxStackSize() - this.inventory.getStackInSlot(INV_OUTPUT).getCount(), solidifiedStack.getCount());
                if (recipe.useDye()) solidifiedItems = this.useDye(solidifiedItems);
                ItemStack newStack = this.inventory.getStackInSlot(INV_OUTPUT);
                newStack.grow(solidifiedItems);
                this.tank.drain(solidifiedItems * recipe.getFluidStack().amount, true);
            }
        }
    }

    private void refillDyeBuffers() {
        for (int i = 0; i < 3; i++) {
            if (!this.inventory.getStackInSlot(INV_DYE_RED + i).isEmpty() && this.dyeBuffers[i] <= DYE_BUFFER_MAX - DYE_PER_DYE) {
                this.inventory.extractItem(INV_DYE_RED + i, 1, false);
                this.dyeBuffers[i] += DYE_PER_DYE;
            }
        }
    }

    private int useDye(int maxItems) {
        if (this.selectedPlastic == EnumDyeColor.WHITE.getDyeDamage())
            return maxItems; // Converting to white plastic is free.

        int desiredColor = ItemDye.DYE_COLORS[this.selectedPlastic];

        // see how much we *can* make
        for (int i = 0; i < 3; i++) {
            int colorComponent = desiredColor >> 8 * i & 0xFF;
            colorComponent = 0xFF - colorComponent; // Invert, because we start out with white and darken the plastic
            if (colorComponent > 0) {
                maxItems = Math.min(maxItems, this.dyeBuffers[i] / colorComponent);
            }
        }

        // if we can make any, use up some dye now
        if (maxItems > 0) {
            for (int i = 0; i < 3; i++) {
                int colorComponent = desiredColor >> 8 * i & 0xFF;
                colorComponent = 0xFF - colorComponent; // see above
                this.dyeBuffers[i] -= colorComponent * maxItems;
            }
        }
        return maxItems;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.inventory.deserializeNBT(tag.getCompoundTag("Items"));
        this.lastTickInventoryStacksize = tag.getInteger("lastTickInventoryStacksize");
        this.selectedPlastic = tag.getInteger("selectedPlastic");
        this.lockSelection = tag.getBoolean("lockSelection");
        this.dyeBuffers[0] = tag.getInteger("dyeBuffer0");
        this.dyeBuffers[1] = tag.getInteger("dyeBuffer1");
        this.dyeBuffers[2] = tag.getInteger("dyeBuffer2");
        this.redstoneMode = tag.getInteger("redstoneMode");

        this.itemHeatLogic.readFromNBT(tag.getCompoundTag("itemLogic"));

        this.tank.setFluid(null);
        this.tank.readFromNBT(tag.getCompoundTag("fluid"));
        this.fluidAmountScaled = this.tank.getScaledFluidAmount();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setTag("Items", this.inventory.serializeNBT());
        tag.setInteger("lastTickInventoryStacksize", this.lastTickInventoryStacksize);
        tag.setInteger("selectedPlastic", this.selectedPlastic);
        tag.setBoolean("lockSelection", this.lockSelection);
        tag.setInteger("dyeBuffer0", this.dyeBuffers[0]);
        tag.setInteger("dyeBuffer1", this.dyeBuffers[1]);
        tag.setInteger("dyeBuffer2", this.dyeBuffers[2]);
        tag.setInteger("redstoneMode", this.redstoneMode);

        NBTTagCompound heatTag = new NBTTagCompound();
        this.itemHeatLogic.writeToNBT(heatTag);
        tag.setTag("itemLogic", heatTag);

        NBTTagCompound tankTag = new NBTTagCompound();
        this.tank.writeToNBT(tankTag);
        tag.setTag("fluid", tankTag);

        return tag;
    }

    @Nonnull
    @Override
    public Map<String, FluidTank> getSerializableTanks() {
        return ImmutableMap.of("Tank", this.tank);
    }

    @Override
    public void updateScaledFluidAmount(int tankIndex, int amount) {
        this.fluidAmountScaled = amount;
    }

    @Override
    public IHeatExchangerLogic getHeatExchangerLogic(EnumFacing side) {
        return this.hullHeatLogic;
    }

    @Override
    public void handleGUIButtonPress(int guiID, EntityPlayer player) {
        super.handleGUIButtonPress(guiID, player);
        if (guiID == 0) {
            if (++this.redstoneMode > 3) {
                this.redstoneMode = 0;
            }
        } else if (guiID >= 1 && guiID < 17) {
            if (this.selectedPlastic != guiID) {
                this.selectedPlastic = guiID - 1;
            } else {
                this.selectedPlastic = -1;
            }
        } else if (guiID == 17) {
            this.lockSelection = !this.lockSelection;
        }
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
        } else if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.inventoryPublic);
        } else {
            return super.getCapability(capability, facing);
        }
    }

    @Override
    public int getRedstoneMode() {
        return this.redstoneMode;
    }

    @Override
    public boolean redstoneAllows() {
        return this.redstoneMode == 3 || super.redstoneAllows();
    }

    @Override
    protected void addLuaMethods(LuaMethodRegistry registry) {
        super.addLuaMethods(registry);
        registry.registerLuaMethod(new LuaMethod("selectColor") {
            @Override
            public Object[] call(Object[] args) {
                this.requireArgs(args, 1, "color_index (0-16)");
                int selection = ((Double) args[0]).intValue();
                Validate.isTrue(selection >= 0 && selection <= 16, "color index " + selection + " out of range 0-16!");
                TileEntityPlasticMixer.this.selectedPlastic = selection - 1;
                return null;
            }
        });
    }

    @Override
    protected List<String> getRedstoneButtonLabels() {
        return REDSTONE_LABELS;
    }

    private class PlasticFluidTank extends SmartSyncTank {
        PlasticFluidTank(int capacity) {
            super(TileEntityPlasticMixer.this, capacity);
        }

        @Override
        public boolean canFillFluidType(FluidStack fluid) {
            return PlasticMixerRegistry.INSTANCE.isValidFluid(fluid);
        }
    }

    private class PlasticItemStackHandler extends BaseItemStackHandler {
        PlasticItemStackHandler() {
            super(TileEntityPlasticMixer.this, INVENTORY_SIZE);
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            if (stack.isEmpty()) return true;
            switch (slot) {
                case INV_INPUT:
                    return PlasticMixerRegistry.INSTANCE.isValidInputItem(stack);
                case INV_OUTPUT:
                    return PlasticMixerRegistry.INSTANCE.isValidOutputItem(stack);
                case INV_DYE_RED:
                    return DyeUtils.rawDyeDamageFromStack(stack) == EnumDyeColor.RED.getDyeDamage();
                case INV_DYE_GREEN:
                    return DyeUtils.rawDyeDamageFromStack(stack) == EnumDyeColor.GREEN.getDyeDamage();
                case INV_DYE_BLUE:
                    return DyeUtils.rawDyeDamageFromStack(stack) == EnumDyeColor.BLUE.getDyeDamage();
            }
            return false;
        }
    }

    private class PlasticItemStackHandlerPublic implements IItemHandlerModifiable {
        @Override
        public int getSlots() {
            return INVENTORY_SIZE;
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return TileEntityPlasticMixer.this.inventory.getStackInSlot(slot);
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            return slot == INV_OUTPUT ? stack : TileEntityPlasticMixer.this.inventory.insertItem(slot, stack, simulate);
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == INV_OUTPUT ? TileEntityPlasticMixer.this.inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return TileEntityPlasticMixer.this.inventory.getSlotLimit(slot);
        }

        @Override
        public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
            TileEntityPlasticMixer.this.inventory.setStackInSlot(slot, stack);
        }
    }
}
