package me.desht.pneumaticcraft.common.tileentity;

import com.google.common.collect.ImmutableSet;
import com.mojang.authlib.GameProfile;
import me.desht.pneumaticcraft.PneumaticCraftRepressurized;
import me.desht.pneumaticcraft.api.drone.DroneConstructingEvent;
import me.desht.pneumaticcraft.api.drone.IPathNavigator;
import me.desht.pneumaticcraft.api.event.SemiblockEvent;
import me.desht.pneumaticcraft.api.item.IItemRegistry.EnumUpgrade;
import me.desht.pneumaticcraft.api.item.IProgrammable;
import me.desht.pneumaticcraft.common.ai.DroneAIManager;
import me.desht.pneumaticcraft.common.ai.IDroneBase;
import me.desht.pneumaticcraft.common.ai.LogisticsManager;
import me.desht.pneumaticcraft.common.block.Blockss;
import me.desht.pneumaticcraft.common.entity.EntityProgrammableController;
import me.desht.pneumaticcraft.common.inventory.handler.BaseItemStackHandler;
import me.desht.pneumaticcraft.common.item.Itemss;
import me.desht.pneumaticcraft.common.network.DescSynced;
import me.desht.pneumaticcraft.common.network.LazySynced;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketSpawnParticle;
import me.desht.pneumaticcraft.common.progwidgets.IProgWidget;
import me.desht.pneumaticcraft.common.tileentity.SideConfigurator.RelativeFace;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.common.util.fakeplayer.DroneFakePlayer;
import me.desht.pneumaticcraft.common.util.fakeplayer.DroneItemHandler;
import me.desht.pneumaticcraft.common.util.fakeplayer.FakeNetHandlerPlayerServer;
import me.desht.pneumaticcraft.common.util.fakeplayer.InventoryFakePlayer;
import me.desht.pneumaticcraft.lib.Log;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.*;

public class TileEntityProgrammableController extends TileEntityPneumaticBase implements IMinWorkingPressure, IDroneBase, ISideConfigurable {
    private static final int ENERGY_CAPACITY = 100000;

    private static final int INVENTORY_SIZE = 1;
    private static final String FALLBACK_NAME = "[ProgController]";
    private static final UUID FALLBACK_UUID = UUID.nameUUIDFromBytes(FALLBACK_NAME.getBytes());

    private final PneumaticEnergyStorage energyStorage;
    private final ProgrammableItemStackHandler inventory;
    private EntityProgrammableController drone;
    private final FluidTank tank = new FluidTank(16000);
    private DroneAIManager aiManager;
    private DroneFakePlayer fakePlayer;
    private DroneItemHandler droneInventory = new DroneItemHandler(1, this);
    private List<IProgWidget> progWidgets = new ArrayList<>();
    private final int[] redstoneLevels = new int[6];
    private int dispenserUpgrades;
    private final SideConfigurator<IItemHandler> itemHandlerSideConfigurator;

    @DescSynced
    private double targetX, targetY, targetZ;
    @DescSynced
    @LazySynced
    private double curX, curY, curZ;
    @DescSynced
    private int diggingX, diggingY, diggingZ;
    @DescSynced
    private int speedUpgrades;

    public static final Set<String> BLACKLISTED_WIDGETS = ImmutableSet.of(
            "computerCraft",
            "entityAttack",
            "droneConditionEntity",
            "standby",
            "suicide",
            "teleport",
            "entityExport",
            "entityImport"
    );

    private UUID ownerID;
    private String ownerName;
    private boolean updateNeighbours;
    // Although this is only used by DroneAILogistics, it is here rather than there
    // so it can persist, for performance reasons; DroneAILogistics is a short-lived object
    private LogisticsManager logisticsManager;

    public TileEntityProgrammableController() {
        super(5, 7, 5000, 4);
        this.inventory = new ProgrammableItemStackHandler(this);
        this.addApplicableUpgrade(EnumUpgrade.SPEED, EnumUpgrade.DISPENSER);
        MinecraftForge.EVENT_BUS.post(new DroneConstructingEvent(this));

        this.energyStorage = new PneumaticEnergyStorage(ENERGY_CAPACITY);

        this.itemHandlerSideConfigurator = new SideConfigurator<>("items", this, 5);
        this.itemHandlerSideConfigurator.registerHandler("droneInv", new ItemStack(Itemss.DRONE),
                CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, this.droneInventory,
                RelativeFace.TOP, RelativeFace.FRONT, RelativeFace.BACK, RelativeFace.LEFT, RelativeFace.RIGHT);
        this.itemHandlerSideConfigurator.registerHandler("programmableInv", new ItemStack(Itemss.NETWORK_COMPONENT, 1, 1),
                CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, this.inventory,
                RelativeFace.BOTTOM);
        this.itemHandlerSideConfigurator.setNullFaceHandler("droneInv");
    }

    @SubscribeEvent
    public void onSemiblockEvent(SemiblockEvent event) {
        if (!event.getWorld().isRemote && event.getWorld() == this.getWorld()) {
            this.logisticsManager = null;
        }
    }

    @Override
    public void update() {
        super.update();

        if (!this.getWorld().isRemote && this.updateNeighbours) {
            this.updateNeighbours();
            this.updateNeighbours = false;
        }

        double speed = this.getSpeed();
        if (PneumaticCraftUtils.distBetweenSq(this.getPos(), this.targetX, this.targetY, this.targetZ) <= speed * speed) {
            this.curX = this.targetX;
            this.curY = this.targetY;
            this.curZ = this.targetZ;
        } else if (PneumaticCraftUtils.distBetweenSq(this.curX, this.curY, this.curZ, this.targetX, this.targetY, this.targetZ) > 0.25) {
            // dist-between check here avoids drone "jitter" when it's very near its target
            Vec3d vec = new Vec3d(this.targetX - this.curX, this.targetY - this.curY, this.targetZ - this.curZ).normalize().scale(speed);
            this.curX += vec.x;
            this.curY += vec.y;
            this.curZ += vec.z;
        }

        if (!this.getWorld().isRemote) {
            this.droneInventory.updateHeldItem();
            DroneFakePlayer fp = this.getFakePlayer();
            for (int i = 0; i < 4; i++) {
                fp.interactionManager.updateBlockRemoving();
            }
            fp.posX = this.curX;
            fp.posY = this.curY;
            fp.posZ = this.curZ;
            fp.onUpdate();

            if (this.getPressure() >= this.getMinWorkingPressure()) {
                if (!this.aiManager.isIdling()) this.addAir(-PneumaticValues.USAGE_PROGRAMMABLE_CONTROLLER);
                this.aiManager.onUpdateTasks();
            }
        } else {
            if (this.drone == null || this.drone.isDead) {
                this.drone = new EntityProgrammableController(this.getWorld(), this);
                this.drone.posX = this.curX;
                this.drone.posY = this.curY;
                this.drone.posZ = this.curZ;
                this.getWorld().spawnEntity(this.drone);
            }
            this.drone.setPosition(this.curX, this.curY, this.curZ);
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @Override
    public void onDescUpdate() {
        super.onDescUpdate();
        if (this.drone != null) {
            this.drone.setDead();
        }
    }

    private double getSpeed() {
        return Math.min(10, this.speedUpgrades) * 0.1 + 0.1;
    }

    private UUID getOwnerUUID() {
        if (this.ownerID == null) {
            this.ownerID = UUID.randomUUID();
            Log.warning(String.format("Programmable controller with owner '%s' has no UUID! Substituting a random UUID (%s).", this.ownerName, this.ownerID));
        }
        return this.ownerID;
    }

    private void initializeFakePlayer() {
        this.fakePlayer = new DroneFakePlayer((WorldServer) this.getWorld(), new GameProfile(this.getOwnerUUID(), this.ownerName), this);
        this.fakePlayer.connection = new FakeNetHandlerPlayerServer(FMLCommonHandler.instance().getMinecraftServerInstance(), this.fakePlayer);
        this.fakePlayer.inventory = new InventoryFakePlayer(this.fakePlayer) {
            @Override
            public IItemHandlerModifiable getUnderlyingItemHandler() {
                return TileEntityProgrammableController.this.droneInventory;
            }
        };
    }

    @Override
    public void handleGUIButtonPress(int buttonID, EntityPlayer player) {
        if (this.itemHandlerSideConfigurator.handleButtonPress(buttonID)) {
            this.updateNeighbours = true;
        }
    }

    @Override
    public IItemHandlerModifiable getPrimaryInventory() {
        return this.inventory;
    }

    public void setOwner(EntityPlayer ownerID) {
        this.ownerID = ownerID.getUniqueID();
        this.ownerName = ownerID.getName();
    }

    @Override
    public List<SideConfigurator> getSideConfigurators() {
        return Collections.singletonList(this.itemHandlerSideConfigurator);
    }

    @Override
    public EnumFacing byIndex() {
        return this.getRotation();
    }

    private class ProgrammableItemStackHandler extends BaseItemStackHandler {
        ProgrammableItemStackHandler(TileEntity te) {
            super(te, INVENTORY_SIZE);
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            ItemStack stack = this.getStackInSlot(slot);
            if (!stack.isEmpty() && isProgrammableAndValidForDrone(TileEntityProgrammableController.this, stack)) {
                TileEntityProgrammableController.this.progWidgets = TileEntityProgrammer.getProgWidgets(stack);
            } else {
                TileEntityProgrammableController.this.progWidgets.clear();
                TileEntityProgrammableController.this.setDugBlock(null);
                TileEntityProgrammableController.this.targetX = TileEntityProgrammableController.this.getPos().getX() + 0.5;
                TileEntityProgrammableController.this.targetY = TileEntityProgrammableController.this.getPos().getY() + 0.6;
                TileEntityProgrammableController.this.targetZ = TileEntityProgrammableController.this.getPos().getZ() + 0.5;
                boolean updateNeighbours = false;
                for (int i = 0; i < TileEntityProgrammableController.this.redstoneLevels.length; i++) {
                    if (TileEntityProgrammableController.this.redstoneLevels[i] > 0) {
                        TileEntityProgrammableController.this.redstoneLevels[i] = 0;
                        updateNeighbours = true;
                    }
                }
                if (updateNeighbours) TileEntityProgrammableController.this.updateNeighbours();
            }
            if (!TileEntityProgrammableController.this.getWorld().isRemote) {
                TileEntityProgrammableController.this.getAIManager().setWidgets(TileEntityProgrammableController.this.progWidgets);
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack itemStack) {
            return itemStack.isEmpty() || isProgrammableAndValidForDrone(TileEntityProgrammableController.this, itemStack);
        }
    }

    @Override
    public String getName() {
        return Blockss.PROGRAMMABLE_CONTROLLER.getTranslationKey();
    }

    @Override
    protected void onUpgradesChanged() {
        super.onUpgradesChanged();
        if (this.getWorld() != null && !this.getWorld().isRemote) {
            this.calculateUpgrades();
        }
    }

    private void calculateUpgrades() {
        int oldDispenserUpgrades = this.dispenserUpgrades;
        this.dispenserUpgrades = Math.min(35, this.getUpgrades(EnumUpgrade.DISPENSER));
        if (!this.getWorld().isRemote && oldDispenserUpgrades != this.dispenserUpgrades) {
            this.resizeDroneInventory(oldDispenserUpgrades + 1, this.dispenserUpgrades + 1);

            this.tank.setCapacity((this.dispenserUpgrades + 1) * 16000);
            if (this.tank.getFluidAmount() > this.tank.getCapacity()) {
                this.tank.getFluid().amount = this.tank.getCapacity();
            }
        }

        this.speedUpgrades = this.getUpgrades(EnumUpgrade.SPEED);
    }

    private void resizeDroneInventory(int oldSize, int newSize) {
        DroneItemHandler tmpHandler = new DroneItemHandler(newSize, this);

        for (int i = 0; i < oldSize && i < newSize; i++) {
            tmpHandler.setStackInSlot(i, this.droneInventory.getStackInSlot(i));
        }

        // if the inventory has shrunk, eject any excess items
        for (int i = newSize; i < oldSize; i++) {
            ItemStack stack = this.droneInventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                PneumaticCraftUtils.dropItemOnGround(stack, this.getWorld(), this.getPos().up());
            }
        }

        this.droneInventory = tmpHandler;
        this.itemHandlerSideConfigurator.updateHandler("droneInv", this.droneInventory);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);

        this.inventory.deserializeNBT(tag.getCompoundTag("Items"));
        this.tank.readFromNBT(tag.getCompoundTag("tank"));
        this.droneInventory = new DroneItemHandler(this.getDroneSlots(), this);
        this.droneInventory.deserializeNBT(tag.getCompoundTag("droneItems"));
        this.ownerID = tag.hasKey("ownerID") ? UUID.fromString(tag.getString("ownerID")) : FALLBACK_UUID;
        this.ownerName = tag.hasKey("ownerName") ? tag.getString("ownerName") : FALLBACK_NAME;
        this.itemHandlerSideConfigurator.updateHandler("droneInv", this.droneInventory);
        this.energyStorage.readFromNBT(tag);

        if (this.getDroneSlots() != this.droneInventory.getSlots() && PneumaticCraftRepressurized.proxy.getClientWorld() == null) {
            Log.warning("drone inventory size mismatch: dispenser upgrades = " + this.getDroneSlots() + ", saved inv size = " + this.droneInventory.getSlots());
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);

        tag.setTag("Items", this.inventory.serializeNBT());

        NBTTagCompound tankTag = new NBTTagCompound();
        this.tank.writeToNBT(tankTag);
        tag.setTag("tank", tankTag);

        ItemStackHandler handler = new ItemStackHandler(this.getFakePlayer().inventory.getSizeInventory());
        for (int i = 0; i < handler.getSlots(); i++) {
            handler.setStackInSlot(i, this.getFakePlayer().inventory.getStackInSlot(i));
        }
        tag.setTag("droneItems", handler.serializeNBT());

        tag.setString("ownerID", this.ownerID.toString());
        tag.setString("ownerName", this.ownerName);

        this.energyStorage.writeToNBT(tag);

        return tag;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return true;
        } else if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return this.itemHandlerSideConfigurator.getHandler(facing) != null;
        } else if (capability == CapabilityEnergy.ENERGY) {
            return true;
        } else {
            return super.hasCapability(capability, facing);
        }
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this.tank);
        } else if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.itemHandlerSideConfigurator.getHandler(facing));
        } else if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(this.energyStorage);
        } else {
            return super.getCapability(capability, facing);
        }
    }

    @Override
    protected void onFirstServerUpdate() {
        super.onFirstServerUpdate();
        SideConfigurator.validateBlockRotation(this);
        this.calculateUpgrades();
        this.inventory.onContentsChanged(0);  // force initial read of any installed drone/network api
        this.curX = this.targetX = this.getPos().getX() + 0.5;
        this.curY = this.targetY = this.getPos().getY() + 0.6;
        this.curZ = this.targetZ = this.getPos().getZ() + 0.5;

        MinecraftForge.EVENT_BUS.register(this);
    }

    private int getDroneSlots() {
        return this.world != null && this.world.isRemote ? 0 : Math.min(36, 1 + this.dispenserUpgrades);
    }

    private static boolean isProgrammableAndValidForDrone(IDroneBase drone, ItemStack programmable) {
        if (programmable.getItem() instanceof IProgrammable && ((IProgrammable) programmable.getItem()).canProgram(programmable) && ((IProgrammable) programmable.getItem()).usesPieces(programmable)) {
            List<IProgWidget> widgets = TileEntityProgrammer.getProgWidgets(programmable);
            for (IProgWidget widget : widgets) {
                if (!drone.isProgramApplicable(widget)) return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public float getMinWorkingPressure() {
        return 3;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }

    @Override
    public float getPressure(ItemStack iStack) {
        return this.getPressure();
    }

    @Override
    public void addAir(ItemStack iStack, int amount) {
        this.addAir(amount);
    }

    @Override
    public float maxPressure(ItemStack iStack) {
        return 7;
    }

    @Override
    public int getVolume(ItemStack itemStack) {
        return this.getAirHandler(null).getVolume();
    }

    @Override
    public World world() {
        return this.getWorld();
    }

    @Override
    public IFluidTank getTank() {
        return this.tank;
    }

    @Override
    public IItemHandlerModifiable getInv() {
        return this.droneInventory;
    }

    @Override
    public Vec3d getDronePos() {
        if (this.curX == 0 && this.curY == 0 && this.curZ == 0) {
            this.curX = this.getPos().getX() + 0.5;
            this.curY = this.getPos().getY() + 0.6;
            this.curZ = this.getPos().getZ() + 0.5;
            this.targetX = this.curX;
            this.targetY = this.curY;
            this.targetZ = this.curZ;
        }
        return new Vec3d(this.curX, this.curY, this.curZ);
    }

    @Override
    public IPathNavigator getPathNavigator() {
        return new IPathNavigator() {

            @Override
            public boolean moveToXYZ(double x, double y, double z) {
                if (TileEntityProgrammableController.this.isBlockValidPathfindBlock(new BlockPos(x, y, z))) {
                    TileEntityProgrammableController.this.targetX = x + 0.5;
                    TileEntityProgrammableController.this.targetY = y - 0.3;
                    TileEntityProgrammableController.this.targetZ = z + 0.5;
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public boolean moveToEntity(Entity entity) {
                return this.moveToXYZ(entity.posX, entity.posY + 0.3, entity.posZ);
            }

            @Override
            public boolean hasNoPath() {
                return PneumaticCraftUtils.distBetweenSq(TileEntityProgrammableController.this.curX, TileEntityProgrammableController.this.curY, TileEntityProgrammableController.this.curZ, TileEntityProgrammableController.this.targetX, TileEntityProgrammableController.this.targetY, TileEntityProgrammableController.this.targetZ) < 0.5;
            }

            @Override
            public boolean isGoingToTeleport() {
                return false;
            }

        };
    }

    @Override
    public void sendWireframeToClient(BlockPos pos) {
    }

    @Override
    public DroneFakePlayer getFakePlayer() {
        if (this.fakePlayer == null) {
            this.initializeFakePlayer();
        }
        return this.fakePlayer;
    }

    @Override
    public boolean isBlockValidPathfindBlock(BlockPos pos) {
        return this.getWorld().isAirBlock(pos);
    }

    @Override
    public void dropItem(ItemStack stack) {
        Vec3d pos = this.getDronePos();
        this.getWorld().spawnEntity(new EntityItem(this.getWorld(), pos.x, pos.y, pos.z, stack));
    }

    @Override
    public void getContentsToDrop(NonNullList<ItemStack> drops) {
        super.getContentsToDrop(drops);

        for (int i = 0; i < this.getDroneSlots(); i++) {
            if (!this.fakePlayer.inventory.getStackInSlot(i).isEmpty()) {
                drops.add(this.fakePlayer.inventory.getStackInSlot(i).copy());
            }
        }
    }

    @Override
    public void setDugBlock(BlockPos pos) {
        if (pos != null) {
            this.diggingX = pos.getX();
            this.diggingY = pos.getY();
            this.diggingZ = pos.getZ();
        } else {
            this.diggingX = this.diggingY = this.diggingZ = 0;
        }
    }

    public BlockPos getDugPosition() {
        return this.diggingX != 0 || this.diggingY != 0 || this.diggingZ != 0 ? new BlockPos(this.diggingX, this.diggingY, this.diggingZ) : null;
    }

    @Override
    public List<IProgWidget> getProgWidgets() {
        return this.progWidgets;
    }

    @Override
    public void setActiveProgram(IProgWidget widget) {
    }

    @Override
    public boolean isProgramApplicable(IProgWidget widget) {
        return !BLACKLISTED_WIDGETS.contains(widget.getWidgetString());
    }

    @Override
    public EntityAITasks getTargetAI() {
        return null;
    }

    @Override
    public void setEmittingRedstone(EnumFacing orientation, int emittingRedstone) {
        this.redstoneLevels[orientation.ordinal()] = emittingRedstone;
        this.updateNeighbours();
    }

    public int getEmittingRedstone(EnumFacing direction) {
        return this.redstoneLevels[direction.ordinal()];
    }

    @Override
    public void setName(String string) {
        if (this.drone != null) {
            this.drone.setCustomNameTag(string);
        }
        ItemStack stack = this.inventory.getStackInSlot(0).copy();
        if (!stack.isEmpty()) {
            stack.setStackDisplayName(string);
            this.inventory.setStackInSlot(0, stack);
        }
    }

    @Override
    public void setCarryingEntity(Entity entity) {
        Log.warning("Drone AI setting carrying entity. However a Programmable Controller can't carry entities!");
        new Throwable().printStackTrace();
    }

    @Override
    public List<Entity> getCarryingEntities() {
        return Collections.emptyList();
    }

    @Override
    public boolean isAIOverriden() {
        return false;
    }

    @Override
    public void onItemPickupEvent(EntityItem curPickingUpEntity, int stackSize) {
    }

    @Override
    public EntityPlayer getOwner() {
        if (this.ownerID == null) return null;
        if (this.getWorld().isRemote) return PneumaticCraftRepressurized.proxy.getClientPlayer();

        return PneumaticCraftUtils.getPlayerFromId(this.ownerID);
    }

    @Override
    public void overload(String msgKey, Object... params) {
        NetworkHandler.sendToAllAround(
                new PacketSpawnParticle(EnumParticleTypes.SMOKE_LARGE,
                        this.getPos().getX() - 0.5, this.getPos().getY() + 1, this.getPos().getZ() - 0.5,
                        0, 0, 0, 10, 1, 1, 1),
                this.getWorld());
    }

    @Override
    public DroneAIManager getAIManager() {
        if (!this.getWorld().isRemote) {
            if (this.aiManager == null) {
                this.aiManager = new DroneAIManager(this, new ArrayList<>());
                this.aiManager.dontStopWhenEndReached();
            }
        }
        return this.aiManager;
    }

    @Override
    public void updateLabel() {
    }

    @Override
    public void addDebugEntry(String message) {
    }

    @Override
    public void addDebugEntry(String message, BlockPos pos) {
    }

    @Override
    public LogisticsManager getLogisticsManager() {
        return this.logisticsManager;
    }

    @Override
    public void setLogisticsManager(LogisticsManager logisticsManager) {
        this.logisticsManager = logisticsManager;
    }
}
