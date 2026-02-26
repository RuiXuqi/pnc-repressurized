package me.desht.pneumaticcraft.common.tileentity;

import com.google.common.collect.ImmutableList;
import me.desht.pneumaticcraft.api.item.IItemRegistry.EnumUpgrade;
import me.desht.pneumaticcraft.common.PneumaticCraftAPIHandler;
import me.desht.pneumaticcraft.common.block.Blockss;
import me.desht.pneumaticcraft.common.config.ConfigHandler;
import me.desht.pneumaticcraft.common.item.Itemss;
import me.desht.pneumaticcraft.common.network.DescSynced;
import me.desht.pneumaticcraft.common.network.GuiSynced;
import me.desht.pneumaticcraft.common.thirdparty.baubles.Baubles;
import me.desht.pneumaticcraft.common.tileentity.SideConfigurator.RelativeFace;
import me.desht.pneumaticcraft.common.util.EnchantmentUtils;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.PlayerArmorInvWrapper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;
import net.minecraftforge.items.wrapper.PlayerOffhandInvWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TileEntityAerialInterface extends TileEntityPneumaticBase
        implements IMinWorkingPressure, IRedstoneControl, IComparatorSupport, ISideConfigurable {

    private static final int ENERGY_CAPACITY = 100000;
    private static final int RF_PER_TICK = 1000;

    @GameRegistry.ObjectHolder("baubles:ring")
    private static final Item BAUBLES_RING = Items.AIR;

    @GuiSynced
    @DescSynced
    public String playerName = "";
    @DescSynced
    private String playerUUID = "";

    private Fluid curXpFluid;
    @DescSynced
    public int curXPFluidIndex = -1;  // index into PneumaticCraftAPIHandler.availableLiquidXPs, -1 = disabled

    @GuiSynced
    public int redstoneMode;
    @GuiSynced
    public int feedMode = 0;
    private boolean oldRedstoneStatus;
    private boolean updateNeighbours;
    @GuiSynced
    public boolean isConnectedToPlayer = false;
    @GuiSynced
    public boolean dispenserUpgradeInserted;

    private final SideConfigurator<IItemHandler> itemHandlerSideConfigurator;

    private final PlayerExperienceHandler playerExperienceHandler;
    private final PlayerFoodHandler playerFoodHandler;
    private WeakReference<EntityPlayer> playerRef = new WeakReference<>(null);

    private final PneumaticEnergyStorage energyStorage;
    private final List<Integer> chargeableSlots = new ArrayList<>();

    public TileEntityAerialInterface() {
        super(PneumaticValues.DANGER_PRESSURE_AERIAL_INTERFACE, PneumaticValues.MAX_PRESSURE_AERIAL_INTERFACE, PneumaticValues.VOLUME_AERIAL_INTERFACE, 4);
        this.addApplicableUpgrade(EnumUpgrade.DISPENSER);

        PlayerMainInvHandler playerMainInvHandler = new PlayerMainInvHandler();
        PlayerArmorInvHandler playerArmorInvHandler = new PlayerArmorInvHandler();
        PlayerOffhandInvHandler playerOffhandInvHandler = new PlayerOffhandInvHandler();
        PlayerEnderInvHandler playerEnderInvHandler = new PlayerEnderInvHandler();
        this.playerExperienceHandler = new PlayerExperienceHandler();
        this.playerFoodHandler = new PlayerFoodHandler();

        this.itemHandlerSideConfigurator = new SideConfigurator<>("items", this, 5);
        this.itemHandlerSideConfigurator.registerHandler("mainInv", new ItemStack(Blocks.CHEST),
                CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, playerMainInvHandler,
                RelativeFace.FRONT, RelativeFace.BACK, RelativeFace.LEFT, RelativeFace.RIGHT);
        this.itemHandlerSideConfigurator.registerHandler("armorInv", new ItemStack(Itemss.PNEUMATIC_CHESTPLATE),
                CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, playerArmorInvHandler,
                RelativeFace.TOP, RelativeFace.BOTTOM);
        this.itemHandlerSideConfigurator.registerHandler("offhandInv", new ItemStack(Items.SHIELD),
                CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, playerOffhandInvHandler);
        this.itemHandlerSideConfigurator.registerHandler("enderInv", new ItemStack(Blocks.ENDER_CHEST),
                CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, playerEnderInvHandler);
        if (Baubles.available) {
            this.itemHandlerSideConfigurator.registerHandler("baublesInv", new ItemStack(BAUBLES_RING),
                    CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, new PlayerBaublesHandler());
        }

        this.energyStorage = new PneumaticEnergyStorage(ENERGY_CAPACITY);
    }

    public void setPlayer(EntityPlayer player) {
        this.playerRef = new WeakReference<>(player);
        boolean old = this.isConnectedToPlayer;
        if (player == null) {
            this.isConnectedToPlayer = false;
        } else {
            this.setPlayer(player.getGameProfile().getName(), player.getGameProfile().getId().toString());
            this.isConnectedToPlayer = true;
        }
        if (old != this.isConnectedToPlayer) {
            this.updateNeighbours = true;
            this.scanForChargeableItems();
        }
    }

    private void setPlayer(String username, String uuid) {
        if (!this.playerUUID.equals(uuid)) {
            this.updateNeighbours = true;
        }
        this.playerName = username;
        this.playerUUID = uuid;
    }

    @Override
    protected void onUpgradesChanged() {
        super.onUpgradesChanged();
        boolean old = this.dispenserUpgradeInserted;
        this.dispenserUpgradeInserted = this.getUpgrades(EnumUpgrade.DISPENSER) > 0;
        if (old != this.dispenserUpgradeInserted) {
            this.updateNeighbours = true;
        }
    }

    @Override
    protected void onFirstServerUpdate() {
        super.onFirstServerUpdate();
        SideConfigurator.validateBlockRotation(this);
    }

    @Override
    public void update() {
        if (!this.getWorld().isRemote && this.updateNeighbours) {
            this.updateNeighbours = false;
            this.updateNeighbours();
        }
        if (!this.getWorld().isRemote) {
            if (this.getPressure() >= this.getMinWorkingPressure() && this.isConnectedToPlayer) {
                this.addAir(-PneumaticValues.USAGE_AERIAL_INTERFACE);

                if ((this.getWorld().getTotalWorldTime() & 0x3f) == 0) {
                    this.scanForChargeableItems();
                }
                this.supplyEnergyToPlayer();

                // check every 16 ticks
                if ((this.getWorld().getTotalWorldTime() & 0xf) == 0) {
                    EntityPlayer player = this.getPlayer();
                    if (player != null && player.getAir() <= 280) {
                        player.setAir(player.getAir() + 16);
                        this.addAir(-80);  // 5 pneumatic air per player air
                    }
                }
            }
            if ((this.getWorld().getTotalWorldTime() & 0xf) == 0 && !this.playerUUID.isEmpty()) {
                this.setPlayer(PneumaticCraftUtils.getPlayerFromId(this.playerUUID));
            }
        }

        if (this.oldRedstoneStatus != this.shouldEmitRedstone()) {
            this.oldRedstoneStatus = this.shouldEmitRedstone();
            this.updateNeighbours = true;
        }

        super.update();
    }

    @Override
    public void handleGUIButtonPress(int buttonID, EntityPlayer player) {
        if (buttonID == 0) {
            this.redstoneMode++;
            if (this.redstoneMode > 1) this.redstoneMode = 0;
        } else if (buttonID >= 1 && buttonID < 4) {
            this.feedMode = buttonID - 1;
        } else if (buttonID == 4) {
            this.curXPFluidIndex++;
            List<Fluid> available = PneumaticCraftAPIHandler.getInstance().availableLiquidXPs;
            if (this.curXPFluidIndex >= available.size()) {
                this.curXPFluidIndex = -1;
            }
            if (this.curXPFluidIndex >= 0 && this.curXPFluidIndex < available.size()) {
                this.curXpFluid = available.get(this.curXPFluidIndex);
            } else {
                this.curXpFluid = null;
            }
        } else if (this.itemHandlerSideConfigurator.handleButtonPress(buttonID)) {
            this.updateNeighbours = true;
        }
    }

    public boolean shouldEmitRedstone() {
        switch (this.redstoneMode) {
            case 0:
                return false;
            case 1:
                return this.isConnectedToPlayer;
        }
        return false;
    }

    private EntityPlayer getPlayer() {
        return this.playerRef.get();
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return ((this.isConnectedToPlayer &&
                (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY && this.dispenserUpgradeInserted && this.curXpFluid != null)
                || capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) && this.itemHandlerSideConfigurator.getHandler(facing) != null)
                || capability == CapabilityEnergy.ENERGY
                || super.hasCapability(capability, facing);

    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            if (this.dispenserUpgradeInserted) {
                if (facing == EnumFacing.UP && ConfigHandler.machineProperties.aerialInterfaceArmorCompat) {
                    // https://github.com/TeamPneumatic/pnc-repressurized/issues/278
                    IItemHandler handler = this.itemHandlerSideConfigurator.getHandler(EnumFacing.UP);
                    if (handler instanceof PlayerArmorInvHandler) {
                        return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(handler);
                    }
                }
                return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.playerFoodHandler);
            } else {
                return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.itemHandlerSideConfigurator.getHandler(facing));
            }
        } else if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY && this.dispenserUpgradeInserted && this.curXpFluid != null) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this.playerExperienceHandler);
        } else if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(this.energyStorage);
        } else {
            return super.getCapability(capability, facing);
        }
    }

    @Override
    public String getName() {
        return Blockss.AERIAL_INTERFACE.getTranslationKey();
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.redstoneMode = tag.getInteger("redstoneMode");
        this.feedMode = tag.getInteger("feedMode");
        this.setPlayer(tag.getString("playerName"), tag.getString("playerUUID"));
        this.curXpFluid = tag.hasKey("curXpFluid") ? FluidRegistry.getFluid(tag.getString("curXpFluid")) : null;
        this.energyStorage.readFromNBT(tag);

        this.curXPFluidIndex = this.curXpFluid == null ? -1 : PneumaticCraftAPIHandler.getInstance().availableLiquidXPs.indexOf(this.curXpFluid);
        this.dispenserUpgradeInserted = this.getUpgrades(EnumUpgrade.DISPENSER) > 0;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        // Write the ItemStacks in the inventory to NBT
        tag.setInteger("redstoneMode", this.redstoneMode);
        tag.setInteger("feedMode", this.feedMode);
        tag.setString("playerName", this.playerName);
        tag.setString("playerUUID", this.playerUUID);
        if (this.curXpFluid != null) tag.setString("curXpFluid", this.curXpFluid.getName());
        this.energyStorage.writeToNBT(tag);
        return tag;
    }

    @Override
    public float getMinWorkingPressure() {
        return PneumaticValues.MIN_PRESSURE_AERIAL_INTERFACE;
    }

    @Override
    public int getRedstoneMode() {
        return this.redstoneMode;
    }

    @Override
    public int getComparatorValue() {
        return this.shouldEmitRedstone() ? 15 : 0;
    }

    private void scanForChargeableItems() {
        this.chargeableSlots.clear();
        if (this.isConnectedToPlayer) {
            InventoryPlayer inv = this.playerRef.get().inventory;
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                if (inv.getStackInSlot(i).hasCapability(CapabilityEnergy.ENERGY, null)) {
                    this.chargeableSlots.add(i);
                }
            }
        }
    }

    private void supplyEnergyToPlayer() {
        if (!this.isConnectedToPlayer) return;

        InventoryPlayer inv = this.playerRef.get().inventory;
        for (int slot : this.chargeableSlots) {
            ItemStack stack = inv.getStackInSlot(slot);
            if (stack.hasCapability(CapabilityEnergy.ENERGY, null)) {
                IEnergyStorage receivingStorage = stack.getCapability(CapabilityEnergy.ENERGY, null);
                int energyLeft = this.energyStorage.getEnergyStored();
                if (energyLeft > 0) {
                    this.energyStorage.extractEnergy(receivingStorage.receiveEnergy(Math.min(energyLeft, RF_PER_TICK), false), false);
                }
                if (this.energyStorage.getEnergyStored() == 0) {
                    break;
                }
            }
        }

        if (Baubles.available && this.energyStorage.getEnergyStored() > 0) {
            Baubles.chargeBaubles(this.getPlayer(), this.energyStorage, RF_PER_TICK);
        }
    }

    private static final List<String> REDSTONE_LABELS = ImmutableList.of(
            "gui.tab.redstoneBehaviour.button.never",
            "gui.tab.redstoneBehaviour.aerialInterface.button.playerConnected"
    );

    @Override
    protected List<String> getRedstoneButtonLabels() {
        return REDSTONE_LABELS;
    }

    @Override
    public List<SideConfigurator> getSideConfigurators() {
        return Collections.singletonList(this.itemHandlerSideConfigurator);
    }

    @Override
    public EnumFacing byIndex() {
        return this.getRotation();
    }

    private abstract class PlayerInvHandler implements IItemHandler {
        /**
         * Get an item handler for the current player, which must be non-null.
         *
         * @return an item handler for the appropriate part of the player's inventory
         */
        protected abstract IItemHandler getInvWrapper();

        @Override
        public int getSlots() {
            return TileEntityAerialInterface.this.playerRef.get() == null ? 0 : this.getInvWrapper().getSlots();
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return TileEntityAerialInterface.this.playerRef.get() == null ? ItemStack.EMPTY : this.getInvWrapper().getStackInSlot(slot);
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            if (TileEntityAerialInterface.this.playerRef.get() == null || TileEntityAerialInterface.this.getPressure() < TileEntityAerialInterface.this.getMinWorkingPressure()) return stack;

            return this.getInvWrapper().insertItem(slot, stack, simulate);
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (TileEntityAerialInterface.this.playerRef.get() == null || TileEntityAerialInterface.this.getPressure() < TileEntityAerialInterface.this.getMinWorkingPressure()) return ItemStack.EMPTY;

            return this.getInvWrapper().extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return TileEntityAerialInterface.this.playerRef.get() == null ? 1 : this.getInvWrapper().getSlotLimit(slot);
        }
    }

    private class PlayerMainInvHandler extends PlayerInvHandler {
        @Override
        protected IItemHandler getInvWrapper() {
            return new PlayerMainInvWrapper(TileEntityAerialInterface.this.getPlayer().inventory);
        }
    }

    private class PlayerArmorInvHandler extends PlayerInvHandler {
        @Override
        protected IItemHandler getInvWrapper() {
            return new PlayerArmorInvWrapper(TileEntityAerialInterface.this.getPlayer().inventory);
        }
    }

    private class PlayerOffhandInvHandler extends PlayerInvHandler {
        @Override
        protected IItemHandler getInvWrapper() {
            return new PlayerOffhandInvWrapper(TileEntityAerialInterface.this.getPlayer().inventory);
        }
    }

    private class PlayerEnderInvHandler extends PlayerInvHandler {
        @Override
        protected IItemHandler getInvWrapper() {
            return new InvWrapper(TileEntityAerialInterface.this.getPlayer().getInventoryEnderChest());
        }
    }

    private class PlayerBaublesHandler extends PlayerInvHandler {
        @Override
        protected IItemHandler getInvWrapper() {
            return TileEntityAerialInterface.this.getPlayer().getCapability(Baubles.CAPABILITY_BAUBLES, null);
        }
    }

    private class PlayerFoodHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            if (TileEntityAerialInterface.this.getPressure() < TileEntityAerialInterface.this.getMinWorkingPressure()) return stack;

            EntityPlayer player = TileEntityAerialInterface.this.getPlayer();
            if (player == null || this.getFoodValue(stack) <= 0 || !this.okToFeed(stack, player)) {
                return stack;
            }

            if (simulate) return ItemStack.EMPTY;

            int startValue = stack.getCount();
            ItemStack remainingItem = stack;
            while (stack.getCount() > 0) {
                remainingItem = stack.onItemUseFinish(player.world, player);
                remainingItem = ForgeEventFactory.onItemUseFinish(player, stack, 0, remainingItem);
                if (remainingItem.getCount() > 0 && (remainingItem != stack || remainingItem.getCount() != startValue)) {
                    if (!player.inventory.addItemStackToInventory(remainingItem) && remainingItem.getCount() > 0) {
                        player.dropItem(remainingItem, false);
                    }
                }
                if (stack.getCount() == startValue) break;
            }
            return remainingItem.getCount() > 0 ? remainingItem : ItemStack.EMPTY;
        }

        private boolean okToFeed(@Nonnull ItemStack stack, EntityPlayer player) {
            int foodValue = this.getFoodValue(stack);
            int curFoodLevel = player.getFoodStats().getFoodLevel();
            int tmpFeedMode = TileEntityAerialInterface.this.feedMode;
            if (tmpFeedMode == 2) {
                tmpFeedMode = player.getMaxHealth() - player.getHealth() > 0 ? 1 : 0;
            }
            switch (tmpFeedMode) {
                case 0:
                    return 20 - curFoodLevel >= foodValue * stack.getCount();
                case 1:
                    return 20 - curFoodLevel >= foodValue * (stack.getCount() - 1) + 1;
            }
            return false;
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        private int getFoodValue(ItemStack item) {
            return item.getItem() instanceof ItemFood ? ((ItemFood) item.getItem()).getHealAmount(item) : 0;
        }
    }

    private class PlayerExperienceHandler implements IFluidHandler {

        @Override
        public IFluidTankProperties[] getTankProperties() {
            if (TileEntityAerialInterface.this.curXpFluid != null) {
                EntityPlayer player = TileEntityAerialInterface.this.getPlayer();
                if (player != null) {
                    return new FluidTankProperties[]{
                            new FluidTankProperties(
                                    new FluidStack(TileEntityAerialInterface.this.curXpFluid, EnchantmentUtils.getPlayerXP(player) * PneumaticCraftAPIHandler.getInstance().liquidXPs.get(TileEntityAerialInterface.this.curXpFluid)),
                                    Integer.MAX_VALUE)
                    };
                }
            }
            return null;
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            if (resource != null && this.canFill(resource.getFluid())) {
                EntityPlayer player = TileEntityAerialInterface.this.getPlayer();
                if (player != null) {
                    int liquidToXP = PneumaticCraftAPIHandler.getInstance().liquidXPs.get(resource.getFluid());
                    int pointsAdded = resource.amount / liquidToXP;
                    if (doFill) {
                        player.addExperience(pointsAdded);
                    }
                    return pointsAdded * liquidToXP;
                }
            }
            return 0;
        }

        private boolean canFill(Fluid fluid) {
            return TileEntityAerialInterface.this.dispenserUpgradeInserted && fluid != null && fluid == TileEntityAerialInterface.this.curXpFluid
                    && PneumaticCraftAPIHandler.getInstance().liquidXPs.containsKey(fluid)
                    && TileEntityAerialInterface.this.getPlayer() != null
                    && TileEntityAerialInterface.this.getPressure() >= TileEntityAerialInterface.this.getMinWorkingPressure();
        }

        @Nullable
        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            if (resource != null && this.canDrain(resource.getFluid())) {
                EntityPlayer player = TileEntityAerialInterface.this.getPlayer();
                if (player != null) {
                    int liquidToXP = PneumaticCraftAPIHandler.getInstance().liquidXPs.get(resource.getFluid());
                    int pointsDrained = Math.min(EnchantmentUtils.getPlayerXP(player), resource.amount / liquidToXP);
                    if (doDrain) EnchantmentUtils.addPlayerXP(player, -pointsDrained);
                    return new FluidStack(resource.getFluid(), pointsDrained * liquidToXP);
                }
            }
            return null;
        }

        private boolean canDrain(Fluid fluid) {
            return TileEntityAerialInterface.this.dispenserUpgradeInserted
                    && (fluid == null || PneumaticCraftAPIHandler.getInstance().liquidXPs.containsKey(fluid))
                    && TileEntityAerialInterface.this.getPlayer() != null
                    && TileEntityAerialInterface.this.getPressure() >= TileEntityAerialInterface.this.getMinWorkingPressure();
        }

        @Nullable
        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            if (TileEntityAerialInterface.this.curXpFluid == null) return null;
            return this.drain(new FluidStack(TileEntityAerialInterface.this.curXpFluid, maxDrain), doDrain);
        }
    }
}
