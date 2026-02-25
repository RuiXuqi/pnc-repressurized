package me.desht.pneumaticcraft.common.tileentity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import me.desht.pneumaticcraft.PneumaticCraftRepressurized;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import me.desht.pneumaticcraft.api.item.IItemRegistry;
import me.desht.pneumaticcraft.api.item.IUpgradeAcceptor;
import me.desht.pneumaticcraft.api.tileentity.IHeatExchanger;
import me.desht.pneumaticcraft.common.block.BlockPneumaticCraft;
import me.desht.pneumaticcraft.common.config.ConfigHandler;
import me.desht.pneumaticcraft.common.inventory.handler.BaseItemStackHandler;
import me.desht.pneumaticcraft.common.item.ItemMachineUpgrade;
import me.desht.pneumaticcraft.common.item.Itemss;
import me.desht.pneumaticcraft.common.network.*;
import me.desht.pneumaticcraft.common.thirdparty.IHeatDisperser;
import me.desht.pneumaticcraft.common.thirdparty.computercraft.LuaMethod;
import me.desht.pneumaticcraft.common.thirdparty.computercraft.LuaMethodRegistry;
import me.desht.pneumaticcraft.common.thirdparty.mekanism.Mekanism;
import me.desht.pneumaticcraft.common.util.NBTUtil;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.common.util.TileEntityCache;
import me.desht.pneumaticcraft.lib.ModIds;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nullable;
import java.util.*;

@Optional.InterfaceList({
        @Optional.Interface(iface = "dan200.computercraft.api.peripheral.IPeripheral", modid = ModIds.COMPUTERCRAFT)
})
public class TileEntityBase extends TileEntity implements IGUIButtonSensitive, IDescSynced, IUpgradeAcceptor, IPeripheral {
    private static final List<String> REDSTONE_LABELS = ImmutableList.of(
            "gui.tab.redstoneBehaviour.button.anySignal",
            "gui.tab.redstoneBehaviour.button.highSignal",
            "gui.tab.redstoneBehaviour.button.lowSignal"
    );

    private static final List<IHeatDisperser> moddedDispersers = new ArrayList<>();

    private final Set<Item> applicableUpgrades = new HashSet<>();
    private final Set<String> applicableCustomUpgrades = new HashSet<>();
    private final UpgradeCache upgradeCache = new UpgradeCache(this);

    UpgradeHandler upgradeHandler;
    boolean firstRun = true;  // True only the first time updateEntity invokes in a session
    int poweredRedstone; // The redstone strength currently applied to the block.
    private boolean descriptionPacketScheduled;
    private List<SyncedField> descriptionFields;
    private TileEntityCache[] tileCache;
    private IBlockState cachedBlockState;
    private boolean preserveStateOnBreak = false; // set to true if shift-wrenched to keep upgrades in the block
    private float actualSpeedMult = PneumaticValues.DEF_SPEED_UPGRADE_MULTIPLIER;
    private float actualUsageMult = PneumaticValues.DEF_SPEED_UPGRADE_USAGE_MULTIPLIER;
    private LuaMethodRegistry luaMethodRegistry = null;

    public TileEntityBase() {
        this(0);
    }

    public TileEntityBase(int upgradeSize) {
        this.upgradeHandler = new UpgradeHandler(upgradeSize);
    }

    private static String makeUpgradeKey(ItemStack stack) {
        return stack.getItem().getRegistryName() + ":" + stack.getMetadata();
    }

    protected void addApplicableUpgrade(IItemRegistry.EnumUpgrade... upgrades) {
        for (IItemRegistry.EnumUpgrade upgrade : upgrades)
            this.addApplicableUpgrade(Itemss.upgrades.get(upgrade));
    }

    protected void addApplicableUpgrade(Item upgrade) {
        this.applicableUpgrades.add(upgrade);
    }

    protected void addApplicableCustomUpgrade(ItemStack... upgrades) {
        for (ItemStack upgrade : upgrades) {
            this.applicableCustomUpgrades.add(makeUpgradeKey(upgrade));
        }
    }

    // server side, chunk sending
    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound compound = super.getUpdateTag();
        return new PacketDescription(this).writeNBT(compound);
    }

    // client side, chunk sending
    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        super.handleUpdateTag(tag);
        PacketDescription packet = new PacketDescription(tag);
        packet.handleClientSide(packet, PneumaticCraftRepressurized.proxy.getClientPlayer());
    }

    /***********
     We don't override getUpdatePacket() or onDataPacket() because TE sync'ing is all handled
     by our custom PacketDescription and the @DescSynced system
     ***********/

    @Override
    public BlockPos getPosition() {
        return this.getPos();
    }

    @Override
    public List<SyncedField> getDescriptionFields() {
        if (this.descriptionFields == null) {
            this.descriptionFields = NetworkUtils.getSyncedFields(this, DescSynced.class);
            for (SyncedField field : this.descriptionFields) {
                field.update();
            }
        }
        return this.descriptionFields;
    }

    public void sendDescriptionPacket() {
        this.sendDescriptionPacket(256);
    }

    void sendDescriptionPacket(double maxPacketDistance) {
        NetworkHandler.sendToAllAround(new PacketDescription(this), this.world, maxPacketDistance);
    }

    /**
     * A way to safely mark a block for an update from another thread (like the CC Lua thread).
     */
    void scheduleDescriptionPacket() {
        this.descriptionPacketScheduled = true;
    }

    /**
     * A way of dispersing heat to other mods which have their own heat API.
     *
     * @param disperser a heat disperser adapter object
     */
    public static void registerHeatDisperser(IHeatDisperser disperser) {
        moddedDispersers.add(disperser);
    }

    /*
     * Even though this class doesn't implement ITickable, we'll keep the base update() logic here; classes
     * which extend non-tickable subclasses might need it (e.g. TileEntityPressureChamberInterface)
     */
    void updateImpl() {
        if (this.firstRun && !this.world.isRemote) {
            this.onFirstServerUpdate();
            this.onNeighborTileUpdate();
            this.onNeighborBlockUpdate();
        }
        this.firstRun = false;

        this.upgradeCache.validate();

        if (!this.world.isRemote) {
            if (this instanceof IHeatExchanger) {
                ((IHeatExchanger) this).getHeatExchangerLogic(null).update();
                for (IHeatDisperser disperser : moddedDispersers) {
                    disperser.disperseHeat(this, this.tileCache);
                }
            }

            if (this instanceof IAutoFluidEjecting && this.getUpgrades(IItemRegistry.EnumUpgrade.DISPENSER) > 0) {
                ((IAutoFluidEjecting) this).autoExportFluid(this);
            }

            if (this.descriptionFields == null) this.descriptionPacketScheduled = true;
            for (SyncedField field : this.getDescriptionFields()) {
                if (field.update()) {
                    this.descriptionPacketScheduled = true;
                }
            }

            if (this.descriptionPacketScheduled) {
                this.descriptionPacketScheduled = false;
                this.sendDescriptionPacket();
            }
        }
    }

    protected void onFirstServerUpdate() {
        this.initializeIfHeatExchanger();
    }

    protected void updateNeighbours() {
        this.world.notifyNeighborsOfStateChange(this.getPos(), this.getBlockType(), true);
    }

    public void onBlockRotated() {
        if (this instanceof ISideConfigurable) {
            for (SideConfigurator sc : ((ISideConfigurable) this).getSideConfigurators()) {
                sc.setupFacingMatrix();
            }
        }
    }

    void rerenderTileEntity() {
        this.world.markBlockRangeForRenderUpdate(this.getPos(), this.getPos());
    }

    protected boolean shouldRerenderChunkOnDescUpdate() {
        return this instanceof ICamouflageableTE;
    }

    /**
     * Encoded into the description packet. Also included in saved data written by writeToNBT().
     * <p>
     * Prefer to use @DescSynced - only use this for complex fields not handled by @DescSynced,
     * or for non-ticking tile entities.
     *
     * @param tag NBT tag
     */
    @Override
    public void writeToPacket(NBTTagCompound tag) {
        if (this instanceof ISideConfigurable) {
            NBTTagCompound sides = new NBTTagCompound();
            for (SideConfigurator sc : ((ISideConfigurable) this).getSideConfigurators()) {
                sides.setTag(sc.getID(), sc.serializeNBT());
            }
            tag.setTag("SideConfigurator", sides);
        }
    }

    /**
     * Encoded into the description packet. Also included in saved data read by readFromNBT().
     * <p>
     * Prefer to use @DescSynced - only use this for complex fields not handled by @DescSynced,
     * or for non-ticking tile entities.
     *
     * @param tag NBT tag
     */
    @Override
    public void readFromPacket(NBTTagCompound tag) {
        if (this instanceof ISideConfigurable) {
            NBTTagCompound sides = tag.getCompoundTag("SideConfigurator");
            for (SideConfigurator sc : ((ISideConfigurable) this).getSideConfigurators()) {
                sc.deserializeNBT(sides.getCompoundTag(sc.getID()));
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (this.upgradeHandler != null && this.upgradeHandler.getSlots() > 0) {
            tag.setTag("Upgrades", this.upgradeHandler.serializeNBT());
        }
        this.writeToPacket(tag);
        if (this instanceof IHeatExchanger) {
            ((IHeatExchanger) this).getHeatExchangerLogic(null).writeToNBT(tag);
        }
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (tag.hasKey("Upgrades") && this.upgradeHandler != null) {
            this.upgradeHandler = new UpgradeHandler(this.upgradeHandler.getSlots());
            this.upgradeHandler.deserializeNBT(tag.getCompoundTag("Upgrades"));
            this.upgradeCache.validate();
        }
        this.readFromPacket(tag);
        if (this instanceof IHeatExchanger) {
            ((IHeatExchanger) this).getHeatExchangerLogic(null).readFromNBT(tag);
        }
    }

    @Override
    public void validate() {
        super.validate();
        this.scheduleDescriptionPacket();
    }

    @Override
    public void onDescUpdate() {
        if (this.shouldRerenderChunkOnDescUpdate()) {
            this.rerenderTileEntity();
        }
    }

    /**
     * Called when a key is synced in the container.
     */
    public void onGuiUpdate() {
    }

    public EnumFacing getRotation() {
        if (this.cachedBlockState == null) {
            this.cachedBlockState = this.world.getBlockState(this.getPos());
        }
        return this.cachedBlockState.getValue(BlockPneumaticCraft.ROTATION);
    }

    @Override
    public void updateContainingBlockInfo() {
        this.cachedBlockState = null;
        super.updateContainingBlockInfo();
    }

    public int getUpgrades(Item upgrade) {
        int upgrades = 0;
        for (int i = 0; i < this.upgradeHandler.getSlots(); i++) {
            ItemStack stack = this.upgradeHandler.getStackInSlot(i);
            if (stack.getItem() == upgrade) {
                upgrades += stack.getCount();
            }
        }
        return upgrades;
    }

    public int getUpgrades(IItemRegistry.EnumUpgrade upgrade) {
        return this.upgradeCache.getUpgrades(upgrade);
    }

    protected int getCustomUpgrades(ItemStack upgradeStack) {
        return this.upgradeCache.getUpgrades(upgradeStack);
    }

    public float getSpeedMultiplierFromUpgrades() {
        return this.actualSpeedMult;
    }

    public float getSpeedUsageMultiplierFromUpgrades() {
        return this.actualUsageMult;
    }

    @Override
    public void handleGUIButtonPress(int guiID, EntityPlayer player) {
    }

    public boolean isGuiUseableByPlayer(EntityPlayer player) {
        return this.getWorld().getTileEntity(this.getPos()) == this && player.getDistanceSq(this.getPos().getX() + 0.5D, this.getPos().getY() + 0.5D, this.getPos().getZ() + 0.5D) <= 64.0D;
    }

    public void onNeighborTileUpdate() {
        this.initializeIfHeatExchanger();
        for (TileEntityCache cache : this.getTileCache()) {
            cache.update();
        }
    }

    public TileEntityCache[] getTileCache() {
        if (this.tileCache == null) this.tileCache = TileEntityCache.getDefaultCache(this.getWorld(), this.getPos());
        return this.tileCache;
    }

    TileEntity getCachedNeighbor(EnumFacing dir) {
        return this.getTileCache()[dir.getIndex()].getTileEntity();
    }

    public void onNeighborBlockUpdate() {
        this.poweredRedstone = PneumaticCraftUtils.getRedstoneLevel(this.getWorld(), this.getPos());
        this.initializeIfHeatExchanger();
        for (TileEntityCache cache : this.getTileCache()) {
            cache.update();
        }
    }

    public boolean redstoneAllows() {
        if (this.getWorld().isRemote) this.onNeighborBlockUpdate();
        switch (((IRedstoneControl) this).getRedstoneMode()) {
            case 0:
                return true;
            case 1:
                return this.poweredRedstone > 0;
            case 2:
                return this.poweredRedstone == 0;
        }
        return false;
    }

    protected void initializeIfHeatExchanger() {
        if (this instanceof IHeatExchanger) {
            this.initializeHeatExchanger(((IHeatExchanger) this).getHeatExchangerLogic(null), this.getConnectedHeatExchangerSides());
        }
    }

    void initializeHeatExchanger(IHeatExchangerLogic heatExchanger, EnumFacing... connectedSides) {
        heatExchanger.initializeAsHull(this.getWorld(), this.getPos(), connectedSides);
    }

    /**
     * Gets the valid sides for heat exchanging to be allowed. returning an empty array will allow any side.
     *
     * @return an array of valid sides
     */
    protected EnumFacing[] getConnectedHeatExchangerSides() {
        return new EnumFacing[0];
    }

    @Override
    public Type getSyncType() {
        return Type.TILE_ENTITY;
    }

    /**
     * Take a fluid-containing from the input slot, use it to fill the primary input tank of the tile entity,
     * and place the resulting emptied container in the output slot.
     *
     * @param inputSlot  input slot
     * @param outputSlot output slot
     */
    void processFluidItem(int inputSlot, int outputSlot) {
        if (!this.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)
                || !this.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null))
            return;
        IItemHandler itemHandler = this.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);

        ItemStack fluidContainer = itemHandler.getStackInSlot(inputSlot);
        IFluidHandlerItem fluidHandlerItem = FluidUtil.getFluidHandler(fluidContainer);
        if (fluidHandlerItem == null) {
            return;
        }
        if (fluidContainer.getCount() > 1) {
            FluidStack stack = fluidHandlerItem.drain(1, false);
            if (stack != null && stack.amount > 0) {
                // disallow multiple filled items (shouldn't normally happen anyway but let's be paranoid)
                return;
            } else {
                // multiple empty items OK, but be sure to only fill one of them...
                ItemStack itemToFill = fluidContainer.copy();
                itemToFill.setCount(1);
                fluidHandlerItem = FluidUtil.getFluidHandler(fluidContainer);
            }
        }

        IFluidHandler fluidHandler = this.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);

        FluidStack itemContents = fluidHandlerItem.drain(1000, false);
        if (itemContents != null && itemContents.amount > 0) {
            // input item contains fluid: drain from input item into tank, move to output if empty
            FluidStack transferred = FluidUtil.tryFluidTransfer(fluidHandler, fluidHandlerItem, itemContents.amount, true);
            if (transferred != null && transferred.amount == itemContents.amount) {
                // all transferred; move empty container to output if possible
                ItemStack emptyContainerStack = fluidHandlerItem.getContainer();
                ItemStack excess = itemHandler.insertItem(outputSlot, emptyContainerStack, false);
                if (excess.isEmpty()) {
                    itemHandler.extractItem(inputSlot, 1, false);
                }
            }
        } else if (itemHandler.getStackInSlot(outputSlot).isEmpty()) {
            // input item(s) is/are empty: drain from tank to one input item, move to output
            FluidStack transferred = FluidUtil.tryFluidTransfer(fluidHandlerItem, fluidHandler, Integer.MAX_VALUE, true);
            if (transferred != null && transferred.amount > 0) {
                itemHandler.extractItem(inputSlot, 1, false);
                ItemStack filledContainerStack = fluidHandlerItem.getContainer();
                itemHandler.insertItem(outputSlot, filledContainerStack, false);
            }
        }
    }

    @Override
    public ITextComponent getDisplayName() {
        return this.getName() == null ? new TextComponentString("???") : new TextComponentTranslation(this.getName());
    }

    @Override
    public Set<Item> getApplicableUpgrades() {
        return this.applicableUpgrades;
    }

    @Override
    public String getName() {
        return null; //Is called directly from the block instead.
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate) {
        return oldState.getBlock() != newSate.getBlock();
    }

    protected void addLuaMethods(LuaMethodRegistry registry) {
        if (this instanceof IHeatExchanger) {
            final IHeatExchanger exchanger = (IHeatExchanger) this;
            registry.registerLuaMethod(new LuaMethod("getTemperature") {
                @Override
                public Object[] call(Object[] args) {
                    this.requireArgs(args, 0, 1, "face? (down/up/north/south/west/east)");
                    if (args.length == 0) {
                        return new Object[]{exchanger.getHeatExchangerLogic(null).getTemperature()};
                    } else {
                        IHeatExchangerLogic logic = exchanger.getHeatExchangerLogic(this.getDirForString((String) args[0]));
                        return new Object[]{logic != null ? logic.getTemperature() : 0};
                    }
                }
            });
        }
    }

    private LuaMethodRegistry getLuaMethodRegistry() {
        if (this.luaMethodRegistry == null) {
            this.luaMethodRegistry = new LuaMethodRegistry();
            this.addLuaMethods(this.luaMethodRegistry);
        }
        return this.luaMethodRegistry;
    }

    @Override
    public String getType() {
        return this.getBlockType().getTranslationKey().substring(5);
    }

    @Override
    public String[] getMethodNames() {
        return this.getLuaMethodRegistry().getMethodNames();
    }

    public Object[] callLuaMethod(String methodName, Object... args) throws Exception {
        return this.getLuaMethodRegistry().getMethod(methodName).call(args);
    }

    @Override
    @Optional.Method(modid = ModIds.COMPUTERCRAFT)
    public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) throws LuaException {
        try {
            return this.getLuaMethodRegistry().getMethod(method).call(arguments);
        } catch (Exception e) {
            throw new LuaException(e.getMessage());
        }
    }

    @Override
    @Optional.Method(modid = ModIds.COMPUTERCRAFT)
    public void attach(IComputerAccess computer) {
    }

    @Override
    @Optional.Method(modid = ModIds.COMPUTERCRAFT)
    public void detach(IComputerAccess computer) {
    }

    @Override
    @Optional.Method(modid = ModIds.COMPUTERCRAFT)
    public boolean equals(IPeripheral other) {
        if (other == null) {
            return false;
        }
        if (this == other) {
            return true;
        }
        if (other instanceof TileEntity) {
            TileEntity otherTE = (TileEntity) other;
            return otherTE.getWorld().equals(this.getWorld()) && otherTE.getPos().equals(this.getPos());
        }

        return false;
    }

    public IItemHandlerModifiable getPrimaryInventory() {
        return null;
    }

    public UpgradeHandler getUpgradesInventory() {
        return this.upgradeHandler;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return this.getPrimaryInventory() != null;
        } else if (capability == Mekanism.CAPABILITY_HEAT_TRANSFER && ConfigHandler.integration.mekHeatEfficiency > 0) {
            return this instanceof IHeatExchanger && ((IHeatExchanger) this).getHeatExchangerLogic(facing) != null;
        } else {
            return super.hasCapability(capability, facing);
        }
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && this.getPrimaryInventory() != null) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.getPrimaryInventory());
        } else if (capability == Mekanism.CAPABILITY_HEAT_TRANSFER && this instanceof IHeatExchanger) {
            return Mekanism.CAPABILITY_HEAT_TRANSFER.cast(Mekanism.getHeatAdapter(this, facing));
        }
        return super.getCapability(capability, facing);
    }

    /**
     * Collect all items which should be dropped when this TE is broken.  Override and extend this in subclassing
     * TE's which have extra inventories to be dropped.
     *
     * @param drops list in which to collect dropped items
     */
    public void getContentsToDrop(NonNullList<ItemStack> drops) {
        if (this.getPrimaryInventory() != null) {
            for (int i = 0; i < this.getPrimaryInventory().getSlots(); i++) {
                drops.add(this.getPrimaryInventory().getStackInSlot(i));
            }
        }

        if (!this.shouldPreserveStateOnBreak()) {
            IItemHandler upgrades = this.getUpgradesInventory();
            if (upgrades != null) {
                for (int i = 0; i < upgrades.getSlots(); i++) {
                    if (!upgrades.getStackInSlot(i).isEmpty()) {
                        drops.add(upgrades.getStackInSlot(i));
                    }
                }
            }
        }

        if (this instanceof ICamouflageableTE) {
            IBlockState camoState = ((ICamouflageableTE) this).getCamouflage();
            if (camoState != null) {
                drops.add(ICamouflageableTE.getStackForState(camoState));
            }
        }
    }

    /**
     * Carry out any tasks which need a world object (the world is null in the TE constructor)
     */
    public void onTileEntityCreated() {
    }

    public final String getRedstoneButtonText(int mode) {
        try {
            return this.getRedstoneButtonLabels().get(mode);
        } catch (ArrayIndexOutOfBoundsException e) {
            return "<ERROR>";
        }
    }

    protected List<String> getRedstoneButtonLabels() {
        return REDSTONE_LABELS;
    }

    public int getRedstoneModeCount() {
        return this.getRedstoneButtonLabels().size();
    }

    public String getRedstoneTabTitle() {
        return this instanceof IRedstoneControlled ? "gui.tab.redstoneBehaviour.enableOn" : "gui.tab.redstoneBehaviour.emitRedstoneWhen";
    }

    /**
     * Should this tile entity preserve its state (currently: upgrades and stored air) when broken?
     * By default this is true when sneak-wrenched, and false when broken by pick.
     *
     * @return true if state should be preserved, false otherwise
     */
    public boolean shouldPreserveStateOnBreak() {
        return this.preserveStateOnBreak;
    }

    public void setPreserveStateOnBreak(boolean preserveStateOnBreak) {
        this.preserveStateOnBreak = preserveStateOnBreak;
    }

    /**
     * Called when a machine's upgrades have changed in any way.  This is also called from readNBT() when saved upgrades
     * are deserialized, so it is not guaranteed that the world field is non-null - beware.  If you override this,
     * remember to call the super method!
     */
    protected void onUpgradesChanged() {
        this.actualSpeedMult = (float) Math.pow(ConfigHandler.machineProperties.speedUpgradeSpeedMultiplier, Math.min(10, this.getUpgrades(IItemRegistry.EnumUpgrade.SPEED)));
        this.actualUsageMult = (float) Math.pow(ConfigHandler.machineProperties.speedUpgradeUsageMultiplier, Math.min(10, this.getUpgrades(IItemRegistry.EnumUpgrade.SPEED)));
    }

    public UpgradeCache getUpgradeCache() {
        return this.upgradeCache;
    }

    public class UpgradeHandler extends BaseItemStackHandler {
        UpgradeHandler(int upgradeSize) {
            super(TileEntityBase.this, upgradeSize);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack itemStack) {
            return itemStack.isEmpty()
                    || TileEntityBase.this.applicableUpgrades.contains(itemStack.getItem())
                    || TileEntityBase.this.applicableCustomUpgrades.contains(makeUpgradeKey(itemStack));
        }

        @Override
        protected void onContentsChanged(int slot) {
            TileEntityBase.this.upgradeCache.invalidate();
        }
    }

    public class UpgradeCache {
        private final int[] upgradeCount = new int[IItemRegistry.EnumUpgrade.values().length];
        private Map<String, Integer> customUpgradeCount;
        private final TileEntityBase te;
        private boolean isValid = false;
        private EnumFacing ejectDirection;

        UpgradeCache(TileEntityBase te) {
            this.te = te;
        }

        void validate() {
            if (this.isValid) return;

            Arrays.fill(this.upgradeCount, 0);
            this.customUpgradeCount = null;
            this.ejectDirection = null;
            IItemHandler inv = this.te.getUpgradesInventory();
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (stack.getItem() instanceof ItemMachineUpgrade) {
                    // native upgrade
                    IItemRegistry.EnumUpgrade type = ((ItemMachineUpgrade) stack.getItem()).getUpgradeType();
                    this.upgradeCount[type.ordinal()] += inv.getStackInSlot(i).getCount();
                    if (type == IItemRegistry.EnumUpgrade.DISPENSER && stack.hasTagCompound()) {
                        this.ejectDirection = EnumFacing.byName(NBTUtil.getString(stack, ItemMachineUpgrade.NBT_DIRECTION));
                    }
                } else if (!inv.getStackInSlot(i).isEmpty()) {
                    // custom upgrade from another mod
                    if (this.customUpgradeCount == null)
                        this.customUpgradeCount = Maps.newHashMap();
                    String key = makeUpgradeKey(stack);
                    this.customUpgradeCount.put(key, this.customUpgradeCount.getOrDefault(key, 0) + stack.getCount());
                }
            }
            this.te.onUpgradesChanged();
            this.isValid = true;
        }

        /**
         * Mark the upgrade cache as invalid.  It will be revalidated at the start of the next update tick for the TE.
         */
        public void invalidate() {
            this.isValid = false;
        }

        public int getUpgrades(IItemRegistry.EnumUpgrade type) {
            return this.upgradeCount[type.ordinal()];
        }

        public int getUpgrades(ItemStack stack) {
            return this.customUpgradeCount == null ? 0 : this.customUpgradeCount.getOrDefault(makeUpgradeKey(stack), 0);
        }

        EnumFacing getEjectDirection() {
            return this.ejectDirection;
        }
    }
}
