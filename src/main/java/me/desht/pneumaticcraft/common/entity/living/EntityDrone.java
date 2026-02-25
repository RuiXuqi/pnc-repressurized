package me.desht.pneumaticcraft.common.entity.living;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.ByteBuf;
import me.desht.pneumaticcraft.api.block.IPneumaticWrenchable;
import me.desht.pneumaticcraft.api.client.pneumaticHelmet.IHackableEntity;
import me.desht.pneumaticcraft.api.drone.IDrone;
import me.desht.pneumaticcraft.api.drone.IPathNavigator;
import me.desht.pneumaticcraft.api.drone.IPathfindHandler;
import me.desht.pneumaticcraft.api.event.SemiblockEvent;
import me.desht.pneumaticcraft.api.item.IItemRegistry.EnumUpgrade;
import me.desht.pneumaticcraft.api.tileentity.IManoMeasurable;
import me.desht.pneumaticcraft.client.render.RenderDroneHeldItem;
import me.desht.pneumaticcraft.client.render.RenderProgressingLine;
import me.desht.pneumaticcraft.common.DamageSourcePneumaticCraft.DamageSourceDroneOverload;
import me.desht.pneumaticcraft.common.DroneRegistry;
import me.desht.pneumaticcraft.common.ai.*;
import me.desht.pneumaticcraft.common.ai.DroneAIManager.EntityAITaskEntry;
import me.desht.pneumaticcraft.common.block.Blockss;
import me.desht.pneumaticcraft.common.config.ConfigHandler;
import me.desht.pneumaticcraft.common.inventory.handler.ChargeableItemHandler;
import me.desht.pneumaticcraft.common.item.ItemGPSTool;
import me.desht.pneumaticcraft.common.item.ItemGunAmmo;
import me.desht.pneumaticcraft.common.item.ItemProgrammingPuzzle;
import me.desht.pneumaticcraft.common.item.Itemss;
import me.desht.pneumaticcraft.common.minigun.Minigun;
import me.desht.pneumaticcraft.common.network.*;
import me.desht.pneumaticcraft.common.progwidgets.IProgWidget;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetGoToLocation;
import me.desht.pneumaticcraft.common.recipes.AmadronOffer;
import me.desht.pneumaticcraft.common.recipes.AmadronOfferCustom;
import me.desht.pneumaticcraft.common.tileentity.PneumaticEnergyStorage;
import me.desht.pneumaticcraft.common.tileentity.TileEntityProgrammer;
import me.desht.pneumaticcraft.common.util.NBTUtil;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.common.util.fakeplayer.DroneFakePlayer;
import me.desht.pneumaticcraft.common.util.fakeplayer.DroneItemHandler;
import me.desht.pneumaticcraft.common.util.fakeplayer.FakeNetHandlerPlayerServer;
import me.desht.pneumaticcraft.common.util.fakeplayer.InventoryFakePlayer;
import me.desht.pneumaticcraft.lib.Log;
import me.desht.pneumaticcraft.lib.NBTKeys;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import me.desht.pneumaticcraft.lib.Sounds;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.passive.EntityFlying;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.oredict.DyeUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class EntityDrone extends EntityDroneBase implements
        IManoMeasurable, IPneumaticWrenchable, IEntityAdditionalSpawnData,
        IHackableEntity, IDroneBase, EntityFlying {

    private static final HashMap<String, Integer> colorMap = new HashMap<>();

    static {
        colorMap.put("aureylian", 0xff69b4);
        colorMap.put("loneztar", 0x00a0a0);
        colorMap.put("jadedcat", 0xa020f0);
        colorMap.put("desht", 0xff6000);
    }

    private EntityDroneItemHandler inventory = new EntityDroneItemHandler(1, this);
    private final FluidTank tank = new FluidTank(Integer.MAX_VALUE);
    private final ItemStackHandler upgradeInventory = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            EntityDrone.this.energy.setCapacity(100000 + 100000 * EntityDrone.this.getUpgrades(EnumUpgrade.VOLUME));
        }
    };
    private final int[] emittingRedstoneValues = new int[6];
    private float propSpeed;
    private static final float LASER_EXTEND_SPEED = 0.05F;
    private final PneumaticEnergyStorage energy = new PneumaticEnergyStorage(100000);

    protected float currentAir; //the current held energy of the Drone;
    private float volume;
    private RenderProgressingLine targetLine;
    private RenderProgressingLine oldTargetLine;
    public List<IProgWidget> progWidgets = new ArrayList<>();

    private DroneFakePlayer fakePlayer;
    public String playerName = "Drone";
    private String playerUUID;

    public DroneGoToChargingStation chargeAI;
    public DroneGoToOwner gotoOwnerAI;
    private final DroneAIManager aiManager = new DroneAIManager(this);

    private boolean firstTick = true;
    public boolean naturallySpawned = true; //determines if it should drop a drone when it dies.
    private double speed;
    private int lifeUpgrades;
    private int suffocationCounter = 40; //Drones are invincible for suffocation for this time.
    private boolean isSuffocating;
    private boolean disabledByHacking;
    private boolean standby; //If true, the drone's propellors stop, the drone will fall down, and won't use pressure.
    private Minigun minigun;

    private AmadronOffer handlingOffer;
    private int offerTimes;
    private ItemStack usedTablet;//Tablet used to place the order.
    private String buyingPlayer;
    private final DroneDebugList debugList = new DroneDebugList();
    private final Set<EntityPlayerMP> syncedPlayers = new HashSet<>();
    private final boolean heldItemChanged = true;  // if true, force a check of item attribute modifiers in onUpdate()

    private int securityUpgradeCount; // for liquid immunity: 1 = breathe in water, 2 = temporary air bubble, 3+ = permanent water removal
    private final Map<BlockPos, IBlockState> displacedLiquids = new HashMap<>();  // liquid blocks displaced by security upgrade

    // Although this is only used by DroneAILogistics, it is here rather than there
    // so it can persist, for performance reasons; DroneAILogistics is a short-lived object
    private LogisticsManager logisticsManager;

    public EntityDrone(World world) {
        super(world);
        this.setSize(0.7F, 0.35F);
        this.moveHelper = new DroneMoveHelper(this);
        this.tasks.addTask(1, this.chargeAI = new DroneGoToChargingStation(this));
    }

    public EntityDrone(World world, EntityPlayer player) {
        this(world);
        if (player != null) {
            this.playerUUID = player.getGameProfile().getId().toString();
            this.playerName = player.getName();
        } else {
            this.playerUUID = this.getUniqueID().toString(); //Anonymous drone used for Amadron or spawned with a Dispenser
        }
    }

    @SubscribeEvent
    public void onSemiblockEvent(SemiblockEvent event) {
        if (!event.getWorld().isRemote && event.getWorld() == this.getEntityWorld()) {
            this.logisticsManager = null;
        }
    }

    @Override
    protected PathNavigate createNavigator(World worldIn) {
        EntityPathNavigateDrone nav = new EntityPathNavigateDrone(this, worldIn);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanEnterDoors(true);
        return nav;
    }

    private void initializeFakePlayer() {
        this.fakePlayer = new DroneFakePlayer((WorldServer) this.world, new GameProfile(UUID.fromString(this.getOwnerUUID()), this.playerName), this);
        this.fakePlayer.connection = new FakeNetHandlerPlayerServer(FMLCommonHandler.instance().getMinecraftServerInstance(), this.fakePlayer);
        this.fakePlayer.inventory = new InventoryFakePlayer(this.fakePlayer) {
            @Override
            public IItemHandlerModifiable getUnderlyingItemHandler() {
                return EntityDrone.this.inventory;
            }
        };
    }

    private static final DataParameter<Boolean> ACCELERATING = EntityDataManager.createKey(EntityDrone.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Float> PRESSURE = EntityDataManager.createKey(EntityDrone.class, DataSerializers.FLOAT);
    private static final DataParameter<String> PROGRAM_KEY = EntityDataManager.createKey(EntityDrone.class, DataSerializers.STRING);
    private static final DataParameter<BlockPos> DUG_POS = EntityDataManager.createKey(EntityDrone.class, DataSerializers.BLOCK_POS);
    private static final DataParameter<Boolean> GOING_TO_OWNER = EntityDataManager.createKey(EntityDrone.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> DRONE_COLOR = EntityDataManager.createKey(EntityDrone.class, DataSerializers.VARINT);
    private static final DataParameter<Boolean> MINIGUN_ACTIVE = EntityDataManager.createKey(EntityDrone.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> HAS_MINIGUN = EntityDataManager.createKey(EntityDrone.class, DataSerializers.BOOLEAN);
    private static final DataParameter<ItemStack> AMMO = EntityDataManager.createKey(EntityDrone.class, DataSerializers.ITEM_STACK);
    private static final DataParameter<String> LABEL = EntityDataManager.createKey(EntityDrone.class, DataSerializers.STRING);
    private static final DataParameter<Integer> ACTIVE_WIDGET = EntityDataManager.createKey(EntityDrone.class, DataSerializers.VARINT);
    private static final DataParameter<BlockPos> TARGET_POS = EntityDataManager.createKey(EntityDrone.class, DataSerializers.BLOCK_POS);
    private static final DataParameter<ItemStack> HELD_ITEM = EntityDataManager.createKey(EntityDrone.class, DataSerializers.ITEM_STACK);
    private static final DataParameter<Integer> TARGET_ID = EntityDataManager.createKey(EntityDrone.class, DataSerializers.VARINT);

    @Override
    protected void entityInit() {
        super.entityInit();

        this.dataManager.register(PRESSURE, 0.0f);
        this.dataManager.register(ACCELERATING, false);
        this.dataManager.register(PROGRAM_KEY, "");
        this.dataManager.register(DUG_POS, BlockPos.ORIGIN);
        this.dataManager.register(GOING_TO_OWNER, false);
        this.dataManager.register(DRONE_COLOR, 0);
        this.dataManager.register(MINIGUN_ACTIVE, false);
        this.dataManager.register(HAS_MINIGUN, false);
        this.dataManager.register(AMMO, ItemStack.EMPTY);
        this.dataManager.register(LABEL, "");
        this.dataManager.register(ACTIVE_WIDGET, 0);
        this.dataManager.register(TARGET_POS, BlockPos.ORIGIN);
        this.dataManager.register(HELD_ITEM, ItemStack.EMPTY);
        this.dataManager.register(TARGET_ID, 0);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(3.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(40F);
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(this.getRange());
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
                || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY
                || capability == CapabilityEnergy.ENERGY
                || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.inventory);
        } else if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this.tank);
        } else if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(this.energy);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void writeSpawnData(ByteBuf data) {
        ByteBufUtils.writeUTF8String(data, this.getFakePlayer().getName());
    }

    @Override
    public void readSpawnData(ByteBuf data) {
        this.playerName = ByteBufUtils.readUTF8String(data);
    }

    /**
     * Determines if an entity can be despawned, used on idle far away entities
     */
    @Override
    protected boolean canDespawn() {
        return false;
    }

    @Override
    protected float getSoundVolume() {
        return 0.2F;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource p_184601_1_) {
        return Sounds.DRONE_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return Sounds.DRONE_DEATH;
    }

    @Override
    public void onUpdate() {
        if (this.firstTick) {
            this.firstTick = false;
            this.volume = PneumaticValues.DRONE_VOLUME + this.getUpgrades(EnumUpgrade.VOLUME) * PneumaticValues.VOLUME_VOLUME_UPGRADE;
            this.securityUpgradeCount = this.getUpgrades(EnumUpgrade.SECURITY);
            if (this.securityUpgradeCount > 0) {
                ((EntityPathNavigateDrone) this.getPathNavigator()).pathThroughLiquid = true;
            }
            this.setPathPriority(PathNodeType.WATER, this.securityUpgradeCount > 0 ? 0.0f : -1.0f);
            this.speed = 0.15 + Math.min(10, this.getUpgrades(EnumUpgrade.SPEED)) * 0.015;
            this.lifeUpgrades = this.getUpgrades(EnumUpgrade.ITEM_LIFE);
            if (!this.world.isRemote) {
                this.setHasMinigun(this.getUpgrades(EnumUpgrade.ENTITY_TRACKER) > 0);
                MinecraftForge.EVENT_BUS.register(this);
                this.aiManager.setWidgets(this.progWidgets);
            }
            this.energy.setCapacity(100000 + 100000 * this.getUpgrades(EnumUpgrade.VOLUME));
        }
        boolean enabled = !this.disabledByHacking && this.getPressure(null) > 0.01F;
        if (!this.world.isRemote) {
            this.inventory.updateHeldItem();
            this.setAccelerating(!this.standby && enabled);
            if (this.isAccelerating()) {
                this.fallDistance = 0;
            }
            if (this.lifeUpgrades > 0) {
                int interval = 10 / this.lifeUpgrades;
                if (interval == 0 || this.ticksExisted % interval == 0) {
                    this.heal(1);
                }
            }
            if (!this.isSuffocating) {
                this.suffocationCounter = 40;
            }
            this.isSuffocating = false;
            Path path = this.getNavigator().getPath();
            if (path != null) {
                PathPoint target = path.getFinalPathPoint();
                if (target != null) {
                    this.setTargetedBlock(new BlockPos(target.x, target.y, target.z));
                } else {
                    this.setTargetedBlock(null);
                }
            } else {
                this.setTargetedBlock(null);
            }
            if (this.world.getTotalWorldTime() % 20 == 0) {
                this.updateSyncedPlayers();
            }
            DroneFakePlayer fp = this.getFakePlayer();
            fp.posX = this.posX;
            fp.posY = this.posY;
            fp.posZ = this.posZ;
            fp.onUpdate();
        } else {
            if (this.digLaser != null) this.digLaser.update();
            this.oldLaserExtension = this.laserExtension;
            if (this.getActiveProgramKey().equals("dig")) {
                this.laserExtension = Math.min(1, this.laserExtension + LASER_EXTEND_SPEED);
            } else {
                this.laserExtension = Math.max(0, this.laserExtension - LASER_EXTEND_SPEED);
            }

            if (this.isAccelerating() && this.rand.nextBoolean()) {
                int x = (int) Math.floor(this.posX);
                int y = (int) Math.floor(this.posY - 1);
                int z = (int) Math.floor(this.posZ);
                BlockPos pos = new BlockPos(x, y, z);
                IBlockState state = null;
                for (int i = 0; i < 3; i++) {
                    state = this.world.getBlockState(pos);
                    if (state.getMaterial() != Material.AIR) break;
                    y--;
                }

                if (state.getMaterial() != Material.AIR) {
                    Vec3d vec = new Vec3d(this.posY - y, 0, 0);
                    vec = vec.rotateYaw((float) (this.rand.nextFloat() * Math.PI * 2));
                    this.world.spawnParticle(EnumParticleTypes.BLOCK_CRACK, this.posX + vec.x, y + 1, this.posZ + vec.z, vec.x, 0, vec.z, Block.getStateId(this.world.getBlockState(pos)));
                }
            }
        }
        if (this.securityUpgradeCount > 1 && this.getHealth() > 0F) {
            this.restoreLiquids(true);

            for (int x = (int) this.posX - 1; x <= (int) (this.posX + this.width); x++) {
                for (int y = (int) this.posY - 1; y <= (int) (this.posY + this.height + 1); y++) {
                    for (int z = (int) this.posZ - 2; z <= (int) (this.posZ + this.width); z++) {
                        if (PneumaticCraftUtils.isBlockLiquid(this.world.getBlockState(new BlockPos(x, y, z)).getBlock())) {
                            BlockPos pos = new BlockPos(x, y, z);
                            if (this.securityUpgradeCount == 2) this.displacedLiquids.put(pos, this.world.getBlockState(pos));
                            this.world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
                        }
                    }
                }
            }
        }
        if (this.isAccelerating()) {
            this.motionX *= 0.3D;
            this.motionY *= 0.3D;
            this.motionZ *= 0.3D;
            this.propSpeed = Math.min(1, this.propSpeed + 0.04F);
            this.addAir(null, -1);
        } else {
            this.propSpeed = Math.max(0, this.propSpeed - 0.04F);
        }
        this.oldPropRotation = this.propRotation;
        this.propRotation += this.propSpeed;

        if (!this.world.isRemote && this.isEntityAlive()) {
            for (int i = 0; i < 4; i++) {
                this.getFakePlayer().interactionManager.updateBlockRemoving();
            }
        }
        super.onUpdate();
        if (this.hasMinigun()) this.getMinigun().setAttackTarget(this.getAttackTarget()).update(this.posX, this.posY, this.posZ);
        if (!this.world.isRemote && this.isEntityAlive()) {
            if (enabled) this.aiManager.onUpdateTasks();
            for (EnumFacing d : EnumFacing.VALUES) {
                if (this.getEmittingRedstone(d) > 0) {
                    if (this.world.isAirBlock(new BlockPos((int) Math.floor(this.posX + this.width / 2), (int) Math.floor(this.posY), (int) Math.floor(this.posZ + this.width / 2)))) {
                        this.world.setBlockState(new BlockPos((int) Math.floor(this.posX + this.width / 2), (int) Math.floor(this.posY), (int) Math.floor(this.posZ + this.width / 2)), Blockss.DRONE_REDSTONE_EMITTER.getDefaultState());
                    }
                    break;
                }
            }
        }
    }

    @Override
    public boolean canBreatheUnderwater() {
        return this.securityUpgradeCount > 0;
    }

    public BlockPos getTargetedBlock() {
        BlockPos pos = this.dataManager.get(TARGET_POS);
        return pos.equals(BlockPos.ORIGIN) ? null : pos;
    }

    private void setTargetedBlock(BlockPos pos) {
        this.dataManager.set(TARGET_POS, pos == null ? BlockPos.ORIGIN : pos);
    }

    @Override
    public int getLaserColor() {
        if (colorMap.containsKey(this.getCustomNameTag().toLowerCase())) {
            return colorMap.get(this.getCustomNameTag().toLowerCase());
        } else if (colorMap.containsKey(this.playerName.toLowerCase())) {
            return colorMap.get(this.playerName.toLowerCase());
        }
        return super.getLaserColor();
    }

    @Override
    protected BlockPos getDugBlock() {
        BlockPos pos = this.dataManager.get(DUG_POS);
        return pos.equals(BlockPos.ORIGIN) ? null : pos;
    }

    @Override
    public ItemStack getDroneHeldItem() {
        return ConfigHandler.client.dronesRenderHeldItem ? this.dataManager.get(HELD_ITEM) : ItemStack.EMPTY;
    }

    @Override
    public void setDugBlock(BlockPos pos) {
        this.dataManager.set(DUG_POS, pos == null ? BlockPos.ORIGIN : pos);
    }

    public List<EntityAITaskEntry> getRunningTasks() {
        return this.aiManager.getRunningTasks();
    }

    public EntityAIBase getRunningTargetAI() {
        return this.aiManager.getTargetAI();
    }

    public void setVariable(String varName, BlockPos pos) {
        this.aiManager.setCoordinate(varName, pos);
    }

    public BlockPos getVariable(String varName) {
        return this.aiManager.getCoordinate(varName);
    }

    @Nonnull
    public ItemStack getActiveProgram() {
        String key = this.getActiveProgramKey();
        if (key.equals("")) {
            return ItemStack.EMPTY;
        } else {
            return ItemProgrammingPuzzle.getStackForWidgetKey(key);
        }
    }

    private String getActiveProgramKey() {
        return this.dataManager.get(PROGRAM_KEY);
    }

    /**
     * Can only be called when the drone is being debugged, so the client has a synced progWidgets array.
     *
     * @return
     */
    public IProgWidget getActiveWidget() {
        int index = this.getActiveWidgetIndex();
        if (index >= 0 && index < this.progWidgets.size()) {
            return this.progWidgets.get(index);
        } else {
            return null;
        }
    }

    private int getActiveWidgetIndex() {
        return this.dataManager.get(ACTIVE_WIDGET);
    }

    @Override
    public void setActiveProgram(IProgWidget widget) {
        this.dataManager.set(PROGRAM_KEY, widget.getWidgetString());
        this.dataManager.set(ACTIVE_WIDGET, this.progWidgets.indexOf(widget));
    }

    private void setAccelerating(boolean accelerating) {
        this.dataManager.set(ACCELERATING, accelerating);
    }

    @Override
    public boolean isAccelerating() {
        return this.dataManager.get(ACCELERATING);
    }

    private void setDroneColor(int color) {
        this.dataManager.set(DRONE_COLOR, color);
    }

    @Override
    public int getDroneColor() {
        return this.dataManager.get(DRONE_COLOR);
    }

    private void setMinigunActivated(boolean activated) {
        this.dataManager.set(MINIGUN_ACTIVE, activated);
    }

    private boolean isMinigunActivated() {
        return this.dataManager.get(MINIGUN_ACTIVE);
    }

    private void setHasMinigun(boolean hasMinigun) {
        this.dataManager.set(HAS_MINIGUN, hasMinigun);
    }

    public boolean hasMinigun() {
        return this.dataManager.get(HAS_MINIGUN);
    }

    public int getAmmoColor() {
        ItemStack ammo = this.dataManager.get(AMMO);
        if (ammo.getItem() instanceof ItemGunAmmo) {
            return ((ItemGunAmmo) ammo.getItem()).getAmmoColor(ammo);
        }
        return 0x808080;  // shouldn't happen
    }

    public void setAmmoColor(ItemStack color) {
        this.dataManager.set(AMMO, color);
    }

    /**
     * Decrements the entity's air supply when underwater
     */
    @Override
    protected int decreaseAirSupply(int par1) {
        return -20; // make drones insta drown
    }

    /**
     * Moves the entity based on the specified heading.  Args: strafe, forward
     */
    @Override
    public void travel(float par1, float par2, float par3) {
        if (this.world.isRemote) {
            EntityLivingBase targetEntity = this.getAttackTarget();
            if (targetEntity != null && targetEntity.isDead) {
                this.setAttackTarget(null);
                targetEntity = null;
            }
            if (targetEntity != null) {
                if (this.targetLine == null) this.targetLine = new RenderProgressingLine(0, -this.height / 2, 0, 0, 0, 0);
                if (this.oldTargetLine == null) this.oldTargetLine = new RenderProgressingLine(0, -this.height / 2, 0, 0, 0, 0);

                this.targetLine.endX = targetEntity.posX - this.posX;
                this.targetLine.endY = targetEntity.posY + targetEntity.height / 2 - this.posY;
                this.targetLine.endZ = targetEntity.posZ - this.posZ;
                this.oldTargetLine.endX = targetEntity.prevPosX - this.prevPosX;
                this.oldTargetLine.endY = targetEntity.prevPosY + targetEntity.height / 2 - this.prevPosY;
                this.oldTargetLine.endZ = targetEntity.prevPosZ - this.prevPosZ;

                this.oldTargetLine.setProgress(this.targetLine.getProgress());
                this.targetLine.incProgressByDistance(0.3D);
                this.ignoreFrustumCheck = true; //don't stop rendering the drone when it goes out of the camera frustrum, as we need to render the target lines as well.
            } else {
                this.targetLine = this.oldTargetLine = null;
                this.ignoreFrustumCheck = false; //don't stop rendering the drone when it goes out of the camera frustrum, as we need to render the target lines as well.
            }
        }
        if (this.getRidingEntity() == null && this.isAccelerating()) {
            double d3 = this.motionY;
            super.travel(par1, par2, par3);
            this.motionY = d3 * 0.60D;
        } else {
            super.travel(par1, par2, par3);
        }
        this.onGround = true; //set onGround to true so AI pathfinding will keep updating.
    }

    /**
     * Method that's being called to render anything that has to for the Drone. The matrix is already translated to the drone's position.
     *
     * @param partialTicks
     */
    @Override
    @SideOnly(Side.CLIENT)
    public void renderExtras(double transX, double transY, double transZ, float partialTicks) {
        super.renderExtras(transX, transY, transZ, partialTicks);

        if (this.targetLine != null && this.oldTargetLine != null) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(1, -1, 1);
            GlStateManager.disableTexture2D();
            GlStateManager.color(1, 0, 0, 1);
            this.targetLine.renderInterpolated(this.oldTargetLine, partialTicks);
            GlStateManager.color(1, 1, 1, 1);
            GlStateManager.enableTexture2D();
            GlStateManager.popMatrix();
        }

        double x = this.lastTickPosX + (this.posX - this.lastTickPosX) * partialTicks;
        double y = this.lastTickPosY + (this.posY - this.lastTickPosY) * partialTicks;
        double z = this.lastTickPosZ + (this.posZ - this.lastTickPosZ) * partialTicks;
        this.getMinigun().render(x, y, z, 0.6);

        ItemStack held = this.getDroneHeldItem();
        if (!held.isEmpty() && !(held.getItem() instanceof ItemGunAmmo && this.hasMinigun())) {
            if (this.renderDroneHeldItem == null) {
                this.renderDroneHeldItem = new RenderDroneHeldItem(this.world);
            }
            this.renderDroneHeldItem.render(held);
        }
    }

    public double getRange() {
        return 75;
    }

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        ItemStack equippedItem = player.getHeldItem(hand);
        if (!this.world.isRemote && !equippedItem.isEmpty()) {
            if (equippedItem.getItem() == Itemss.GPS_TOOL) {
                BlockPos gpsLoc = ItemGPSTool.getGPSLocation(equippedItem);
                if (gpsLoc != null) {
                    this.getNavigator().tryMoveToXYZ(gpsLoc.getX(), gpsLoc.getY(), gpsLoc.getZ(), 0.1D);
                }
            } else {
                OptionalInt dyeIndex = DyeUtils.dyeDamageFromStack(equippedItem);
                if (dyeIndex.isPresent()) {
                    this.setDroneColor(ItemDye.DYE_COLORS[dyeIndex.getAsInt()]);
                    if (ConfigHandler.general.useUpDyesWhenColoring && !player.capabilities.isCreativeMode) {
                        equippedItem.shrink(1);
                        if (equippedItem.getCount() <= 0) {
                            player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Called when a drone is hit by a Pneumatic Wrench.
     */
    @Override
    public boolean rotateBlock(World world, EntityPlayer player, BlockPos pos, EnumFacing side, EnumHand hand) {
        if (!this.naturallySpawned) {
            if (player.capabilities.isCreativeMode) this.naturallySpawned = true;//don't drop the drone in creative.
            this.attackEntityFrom(new DamageSourceDroneOverload("wrenched"), 2000.0F);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Restore any liquids that may have been displaced by the drone (security upgrade)
     *
     * @param distCheck if true, only restore liquids in blocks > 1 block distance away from the drone
     */
    private void restoreLiquids(boolean distCheck) {
        Iterator<Map.Entry<BlockPos, IBlockState>> iter = this.displacedLiquids.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<BlockPos, IBlockState> entry = iter.next();
            BlockPos pos = entry.getKey();
            if (!distCheck || pos.distanceSqToCenter(this.posX, this.posY, this.posZ) > 1) {
                if (this.world.isAirBlock(pos) || PneumaticCraftUtils.isBlockLiquid(this.world.getBlockState(pos).getBlock())) {
                    this.world.setBlockState(pos, entry.getValue(), 2);
                }
                iter.remove();
            }
        }
    }

    @Nullable
    @Override
    public Entity changeDimension(int dimensionIn, ITeleporter teleporter) {
        Entity entity = super.changeDimension(dimensionIn, teleporter);
        if (entity != null) {
            this.restoreLiquids(false);
        }
        return entity;
    }

    @Override
    public void onDeath(DamageSource par1DamageSource) {
        for (int i = 0; i < this.inventory.getSlots(); i++) {
            if (!this.inventory.getStackInSlot(i).isEmpty()) {
                this.entityDropItem(this.inventory.getStackInSlot(i), 0);
                this.inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        this.restoreLiquids(false);
        if (!this.naturallySpawned) {
            ItemStack drone = this.getDroppedStack();
            if (this.hasCustomName()) drone.setStackDisplayName(this.getCustomNameTag());
            this.entityDropItem(drone, 0);

            if (!this.world.isRemote) {
                EntityPlayer owner = this.getOwner();
                if (owner != null) {
                    int x = (int) Math.floor(this.posX);
                    int y = (int) Math.floor(this.posY);
                    int z = (int) Math.floor(this.posZ);
                    ITextComponent msg = this.hasCustomName() ?
                            new TextComponentTranslation("death.drone.named", this.getCustomNameTag(), x, y, z) :
                            new TextComponentTranslation("death.drone", x, y, z);
                    msg = msg.appendSibling(new TextComponentString(" - ")).appendSibling(par1DamageSource.getDeathMessage(this));
                    owner.sendStatusMessage(msg, false);
                }
            }
        }
        if (!this.world.isRemote) this.getFakePlayer().interactionManager.cancelDestroyingBlock();
        this.setCustomNameTag("");  // keep other mods (like CoFH Core) quiet about death message broadcasts
        super.onDeath(par1DamageSource);
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    protected ItemStack getDroppedStack() {
        NBTTagCompound tag = new NBTTagCompound();
        this.writeEntityToNBT(tag);
        ItemStack drone = new ItemStack(Itemss.DRONE);
        drone.setTagCompound(tag);
        return drone;
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        if (this.world.isRemote && TARGET_ID.equals(key)) {
            int id = this.dataManager.get(TARGET_ID);
            if (id > 0) {
                Entity e = this.getEntityWorld().getEntityByID(id);
                if (e instanceof EntityLivingBase) {
                    this.setAttackTarget((EntityLivingBase) e);
                }
            }
            if (this.targetLine != null && this.oldTargetLine != null) {
                this.targetLine.setProgress(0);
                this.oldTargetLine.setProgress(0);
            }
        } else {
            super.notifyDataManagerChange(key);
        }
    }

    @Override
    public void setAttackTarget(EntityLivingBase entity) {
        super.setAttackTarget(entity);
        if (!this.world.isRemote) {
            this.dataManager.set(TARGET_ID, entity == null ? 0 : entity.getEntityId());
        }
    }

    @Override
    public float getPressure(ItemStack iStack) {
        return this.dataManager.get(PRESSURE);
    }

    @Override
    public void addAir(ItemStack iStack, int amount) {
        if (!this.world().isRemote) {
            this.currentAir += amount;
            this.dataManager.set(PRESSURE, this.currentAir / this.volume);
        }
    }

    @Override
    public float maxPressure(ItemStack iStack) {
        return PneumaticValues.DRONE_MAX_PRESSURE;
    }

    @Override
    public int getVolume(ItemStack iStack) {
        return (int) this.volume;
    }

    @Override
    public void printManometerMessage(EntityPlayer player, List<String> curInfo) {
        if (this.hasCustomName()) curInfo.add(TextFormatting.AQUA + this.getCustomNameTag());
        curInfo.add("Owner: " + this.getFakePlayer().getName());
        curInfo.add("Current pressure: " + PneumaticCraftUtils.roundNumberTo(this.getPressure(null), 1) + " bar.");
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        TileEntityProgrammer.setWidgetsToNBT(this.progWidgets, tag);
        tag.setBoolean("naturallySpawned", this.naturallySpawned);
        tag.setFloat("currentAir", this.currentAir);
        tag.setFloat("propSpeed", this.propSpeed);
        tag.setBoolean("disabledByHacking", this.disabledByHacking);
        tag.setBoolean("hackedByOwner", this.gotoOwnerAI != null);
        tag.setInteger("color", this.getDroneColor());
        tag.setBoolean("standby", this.standby);
        tag.setFloat("volume", this.volume);

        NBTTagCompound variableTag = new NBTTagCompound();
        this.aiManager.writeToNBT(variableTag);
        tag.setTag("variables", variableTag);

        tag.setTag("Inventory", this.inventory.serializeNBT());
        tag.setTag(ChargeableItemHandler.NBT_UPGRADE_TAG, this.upgradeInventory.serializeNBT());

        this.tank.writeToNBT(tag);

        if (this.handlingOffer != null) {
            NBTTagCompound subTag = new NBTTagCompound();
            subTag.setBoolean("isCustom", this.handlingOffer instanceof AmadronOfferCustom);
            this.handlingOffer.writeToNBT(subTag);
            tag.setTag("amadronOffer", subTag);
            tag.setInteger("offerTimes", this.offerTimes);
            if (!this.usedTablet.isEmpty()) this.usedTablet.writeToNBT(subTag);
            tag.setString("buyingPlayer", this.buyingPlayer);
        }

        if (!this.displacedLiquids.isEmpty()) {
            NBTTagList disp = new NBTTagList();
            for (Map.Entry<BlockPos, IBlockState> entry : this.displacedLiquids.entrySet()) {
                NBTTagCompound p = net.minecraft.nbt.NBTUtil.createPosTag(entry.getKey());
                NBTTagCompound s = new NBTTagCompound();
                net.minecraft.nbt.NBTUtil.writeBlockState(s, entry.getValue());
                NBTTagList l = new NBTTagList();
                l.appendTag(p);
                l.appendTag(s);
                disp.appendTag(l);
            }
            tag.setTag("displacedLiquids", disp);
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        this.progWidgets = TileEntityProgrammer.getWidgetsFromNBT(tag);
        this.naturallySpawned = tag.getBoolean("naturallySpawned");
        this.currentAir = tag.getFloat("currentAir");
        this.volume = tag.getFloat("volume");
        this.dataManager.set(PRESSURE, this.currentAir / this.volume);
        this.propSpeed = tag.getFloat("propSpeed");
        this.disabledByHacking = tag.getBoolean("disabledByHacking");
        this.setGoingToOwner(tag.getBoolean("hackedByOwner"));
        this.setDroneColor(tag.getInteger("color"));
        this.aiManager.readFromNBT(tag.getCompoundTag("variables"));
        this.standby = tag.getBoolean("standby");

        this.upgradeInventory.deserializeNBT(tag.getCompoundTag(ChargeableItemHandler.NBT_UPGRADE_TAG));

        // we can't just deserialize the saved inv directly into the inventory, since that
        // also affects its size, meaning any added dispenser upgrades wouldn't work
        this.inventory = new EntityDroneItemHandler(1 + this.getUpgrades(EnumUpgrade.DISPENSER), this);
        ItemStackHandler tmpInv = new ItemStackHandler();
        tmpInv.deserializeNBT(tag.getCompoundTag("Inventory"));
        for (int i = 0; i < tmpInv.getSlots() && i < this.inventory.getSlots(); i++) {
            this.inventory.setStackInSlot(i, tmpInv.getStackInSlot(i).copy());
        }

        this.tank.setCapacity(PneumaticValues.DRONE_TANK_SIZE * (1 + this.getUpgrades(EnumUpgrade.DISPENSER)));
        this.tank.readFromNBT(tag);

        this.energy.setCapacity(100000 + 100000 * this.getUpgrades(EnumUpgrade.VOLUME));

        if (tag.hasKey("amadronOffer")) {
            NBTTagCompound subTag = tag.getCompoundTag("amadronOffer");
            this.handlingOffer = subTag.getBoolean("isCustom") ? AmadronOfferCustom.loadFromNBT(subTag) : AmadronOffer.loadFromNBT(subTag);
            this.usedTablet = subTag.hasKey("id") ? new ItemStack(subTag) : ItemStack.EMPTY;
            this.buyingPlayer = subTag.getString("buyingPlayer");
        } else {
            this.handlingOffer = null;
            this.usedTablet = ItemStack.EMPTY;
            this.buyingPlayer = null;
        }
        this.offerTimes = tag.getInteger("offerTimes");

        if (tag.hasKey("displacedLiquids")) {
            NBTTagList disp = tag.getTagList("displacedLiquids", Constants.NBT.TAG_LIST);
            for (int i = 0; i < disp.tagCount(); i++) {
                NBTTagList l = (NBTTagList) disp.get(i);
                NBTTagCompound p = l.getCompoundTagAt(0);
                NBTTagCompound s = l.getCompoundTagAt(1);
                BlockPos pos = net.minecraft.nbt.NBTUtil.getPosFromTag(p);
                IBlockState state = net.minecraft.nbt.NBTUtil.readBlockState(s);
                this.displacedLiquids.put(pos, state);
            }
        }
    }

    public String getPlayerName() {
        return this.playerName;
    }

    public String getOwnerUUID() {
        if (this.playerUUID == null) {
            Log.warning(String.format("Drone with owner '%s' has no UUID! Substituting the Drone's UUID (%s).", this.playerName, this.getUniqueID()));
            this.playerUUID = this.getUniqueID().toString();
        }
        return this.playerUUID;
    }

    /**
     * This and readFromNBT are _not_ being transfered from/to the Drone item.
     */
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        // this can be called client-side, e.g. TheOneProbe
        // but this data isn't sync'd to the client
        if (!this.getEntityWorld().isRemote) {
            if (this.playerName != null) {
                tag.setString("owner", this.playerName);
                tag.setString("ownerUUID", this.getOwnerUUID());
            }
        }
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        // see writeToNBT() above
        if (!this.getEntityWorld().isRemote) {
            if (tag.hasKey("owner")) {
                this.playerName = tag.getString("owner");
                this.playerUUID = tag.hasKey("ownerUUID") ? tag.getString("ownerUUID") : null;
            }
        }
    }

    public int getUpgrades(EnumUpgrade upgrade) {
        return this.getUpgrades(Itemss.upgrades.get(upgrade));
    }

    @Override
    public int getUpgrades(Item upgrade) {
        int upgrades = 0;
        for (int i = 0; i < this.upgradeInventory.getSlots(); i++) {
            if (this.upgradeInventory.getStackInSlot(i).getItem() == upgrade) {
                upgrades += this.upgradeInventory.getStackInSlot(i).getCount();
            }
        }
        return upgrades;
    }

    @Override
    public DroneFakePlayer getFakePlayer() {
        if (this.fakePlayer == null && !this.world.isRemote) {
            this.initializeFakePlayer();
        }
        return this.fakePlayer;
    }

    public Minigun getMinigun() {
        if (this.minigun == null) {
            this.minigun = new MinigunDrone(this).setPlayer(this.getFakePlayer()).setWorld(this.world).setPressurizable(this, PneumaticValues.DRONE_USAGE_ATTACK);
        }
        return this.minigun;
    }

    @Override
    public boolean attackEntityAsMob(Entity entity) {
        this.getFakePlayer().attackTargetEntityWithCurrentItem(entity);
        this.addAir(null, -PneumaticValues.DRONE_USAGE_ATTACK);
        return true;
    }

    @Override
    public boolean attackEntityFrom(DamageSource damageSource, float damage) {
        if (damageSource == DamageSource.IN_WALL) {
            this.isSuffocating = true;
            if (this.suffocationCounter-- > 0 || !ConfigHandler.general.enableDroneSuffocationDamage) {
                return false;
            }
        }
        return super.attackEntityFrom(damageSource, damage);
    }

    @Override
    public IItemHandlerModifiable getInv() {
        return this.inventory;
    }

    public double getSpeed() {
        return this.speed;
    }

    public int getEmittingRedstone(EnumFacing side) {
        return this.emittingRedstoneValues[side.ordinal()];
    }

    @Override
    public void setEmittingRedstone(EnumFacing side, int value) {
        if (this.emittingRedstoneValues[side.ordinal()] != value) {
            this.emittingRedstoneValues[side.ordinal()] = value;
            BlockPos pos = new BlockPos((int) Math.floor(this.posX + this.width / 2), (int) Math.floor(this.posY), (int) Math.floor(this.posZ + this.width / 2));
            IBlockState state = this.world.getBlockState(pos);
            this.world.notifyBlockUpdate(pos, state, state, 3);
        }
    }

    @Override
    public boolean isBlockValidPathfindBlock(BlockPos pos) {
        if (this.world.isAirBlock(pos)) return true;
        Block block = this.world.getBlockState(pos).getBlock();
        if (PneumaticCraftUtils.isBlockLiquid(block)) {
            return this.securityUpgradeCount > 0;
        }
        if (block.isPassable(this.world, pos) && block != Blocks.LADDER) return true;
        if (DroneRegistry.getInstance().pathfindableBlocks.containsKey(block)) {
            IPathfindHandler pathfindHandler = DroneRegistry.getInstance().pathfindableBlocks.get(block);
            return pathfindHandler == null || pathfindHandler.canPathfindThrough(this.world, pos);
        } else {
            return false;
        }
    }

    @Override
    public void sendWireframeToClient(BlockPos pos) {
        NetworkHandler.sendToAllAround(new PacketShowWireframe(this, pos), this.world);
    }

    /**
     * IHackableEntity
     */

    @Override
    public String getId() {
        return null;
    }

    @Override
    public boolean canHack(Entity entity, EntityPlayer player) {
        return this.isAccelerating();
    }

    @Override
    public void addInfo(Entity entity, List<String> curInfo, EntityPlayer player) {
        if (this.playerName.equals(player.getName())) {
            if (this.isGoingToOwner()) {
                curInfo.add("pneumaticHelmet.hacking.result.resumeTasks");
            } else {
                curInfo.add("pneumaticHelmet.hacking.result.callBack");
            }
        } else {
            curInfo.add("pneumaticHelmet.hacking.result.disable");
        }
    }

    @Override
    public void addPostHackInfo(Entity entity, List<String> curInfo, EntityPlayer player) {
        if (this.playerName.equals(player.getName())) {
            if (this.isGoingToOwner()) {
                curInfo.add("pneumaticHelmet.hacking.finished.calledBack");
            } else {
                curInfo.add("pneumaticHelmet.hacking.finished.resumedTasks");
            }
        } else {
            curInfo.add("pneumaticHelmet.hacking.finished.disabled");
        }
    }

    @Override
    public int getHackTime(Entity entity, EntityPlayer player) {
        return this.playerName.equals(player.getName()) ? 20 : 100;
    }

    @Override
    public void onHackFinished(Entity entity, EntityPlayer player) {
        if (!this.world.isRemote && player.getGameProfile().equals(this.getFakePlayer().getGameProfile())) {
            this.setGoingToOwner(this.gotoOwnerAI == null);//toggle the state
        } else {
            this.disabledByHacking = true;
        }
    }

    @Override
    public boolean afterHackTick(Entity entity) {
        return false;
    }

    private void setGoingToOwner(boolean state) {
        if (!this.world.isRemote) {
            if (state && this.gotoOwnerAI == null) {
                this.gotoOwnerAI = new DroneGoToOwner(this);
                this.tasks.addTask(2, this.gotoOwnerAI);
                this.dataManager.set(GOING_TO_OWNER, true);
                this.setActiveProgram(new ProgWidgetGoToLocation());
            } else if (!state && this.gotoOwnerAI != null) {
                this.tasks.removeTask(this.gotoOwnerAI);
                this.gotoOwnerAI = null;
                this.dataManager.set(GOING_TO_OWNER, false);
            }
        }
    }

    private boolean isGoingToOwner() {
        return this.dataManager.get(GOING_TO_OWNER);
    }

    @Override
    public IFluidTank getTank() {
        return this.tank;
    }

    /**
     * Returns the owning player. Returns null when the player is not online.
     *
     * @return the owning player
     */
    public EntityPlayer getOwner() {
        return FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayerByUsername(this.playerName);
    }

    public void setStandby(boolean standby) {
        this.standby = standby;
    }

    @Override
    public World world() {
        return this.world;
    }

    @Override
    public Vec3d getDronePos() {
        return new Vec3d(this.posX, this.posY, this.posZ);
    }

    @Override
    public void dropItem(ItemStack stack) {
        this.entityDropItem(stack, 0);
    }

    @Override
    public List<IProgWidget> getProgWidgets() {
        return this.progWidgets;
    }

    @Override
    public EntityAITasks getTargetAI() {
        return this.targetTasks;
    }

    @Override
    public boolean isProgramApplicable(IProgWidget widget) {
        return true;
    }

    @Override
    public void setName(String string) {
        this.setCustomNameTag(string);
    }

    @Override
    public void setCarryingEntity(Entity entity) {
        if (entity == null) {
            for (Entity e : this.getCarryingEntities()) {
                e.dismountRidingEntity();
                if (e instanceof EntityMinecart || e instanceof EntityBoat) {
                    // little kludge to prevent the dropped minecart/boat immediately picking up the drone
                    e.posY -= 2;
                    if (this.world.getBlockState(e.getPosition()).isBlockNormalCube()) {
                        e.posY++;
                    }
                    // minecarts have their own onUpdate() which doesn't decrement rideCooldown
                    if (e instanceof EntityMinecart) e.rideCooldown = 0;
                }
            }
        } else {
            entity.startRiding(this);
        }
    }

    @Override
    public List<Entity> getCarryingEntities() {
        return this.getPassengers();
    }

    @Override
    public boolean isAIOverriden() {
        return this.chargeAI.isExecuting || this.gotoOwnerAI != null;
    }

    @Override
    public void onItemPickupEvent(EntityItem curPickingUpEntity, int stackSize) {
        this.onItemPickup(curPickingUpEntity, stackSize);
    }

    @Override
    public IPathNavigator getPathNavigator() {
        return (IPathNavigator) this.getNavigator();
    }

    public void tryFireMinigun(EntityLivingBase target) {
        ItemStack ammo = this.getAmmo();
        if (this.getMinigun().setAmmoStack(ammo).tryFireMinigun(target)) {
            for (int i = 0; i < this.inventory.getSlots(); i++) {
                if (this.inventory.getStackInSlot(i) == ammo) {
                    this.inventory.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        }
    }

    public ItemStack getAmmo() {
        for (int i = 0; i < this.inventory.getSlots(); i++) {
            ItemStack stack = this.inventory.getStackInSlot(i);
            if (stack.getItem() instanceof ItemGunAmmo) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public void setHandlingOffer(AmadronOffer offer, int times, @Nonnull ItemStack usedTablet, String buyingPlayer) {
        this.handlingOffer = offer;
        this.offerTimes = times;
        this.usedTablet = usedTablet.copy();
        this.buyingPlayer = buyingPlayer;
    }

    public AmadronOffer getHandlingOffer() {
        return this.handlingOffer;
    }

    public int getOfferTimes() {
        return this.offerTimes;
    }

    public ItemStack getUsedTablet() {
        return this.usedTablet;
    }

    public String getBuyingPlayer() {
        return this.buyingPlayer;
    }

    @Override
    public void overload(String msgKey, Object... params) {
        this.attackEntityFrom(new DamageSourceDroneOverload(msgKey, params), 2000.0F);
    }

    @Override
    public DroneAIManager getAIManager() {
        return this.aiManager;
    }

    @Override
    public LogisticsManager getLogisticsManager() {
        return this.logisticsManager;
    }

    @Override
    public void setLogisticsManager(LogisticsManager logisticsManager) {
        this.logisticsManager = logisticsManager;
    }

    @Override
    public void updateLabel() {
        this.dataManager.set(LABEL, this.getAIManager() != null ? this.getAIManager().getLabel() : "Main");
    }

    public String getLabel() {
        return this.dataManager.get(LABEL);
    }

    public DebugEntry getCurrentDebugEntry() {
        return this.debugList.getCurrent();
    }

    public DebugEntry getDebugEntry(int widgetID) {
        return this.debugList.get(widgetID);
    }

    @Override
    public void addDebugEntry(String message) {
        this.addDebugEntry(message, null);
    }

    @Override
    public void addDebugEntry(String message, BlockPos pos) {
        DebugEntry entry = new DebugEntry(message, this.getActiveWidgetIndex(), pos);

        // add the entry server-side
        this.addDebugEntry(entry);

        // add the entry client-side
        PacketSendDroneDebugEntry packet = new PacketSendDroneDebugEntry(entry, this);
        for (EntityPlayerMP player : this.syncedPlayers) {
            NetworkHandler.sendTo(packet, player);
        }
    }

    public void addDebugEntry(DebugEntry entry) {
        this.debugList.addEntry(entry);
    }

    public void trackAsDebugged(EntityPlayerMP player) {
        NetworkHandler.sendTo(new PacketSyncDroneEntityProgWidgets(this), player);

        for (DebugEntry entry : this.debugList.getAll()) {
            NetworkHandler.sendTo(new PacketSendDroneDebugEntry(entry, this), player);
        }

        this.syncedPlayers.add(player);
    }

    private void updateSyncedPlayers() {
        this.syncedPlayers.removeIf(player -> player.isDead
                || player.getItemStackFromSlot(EntityEquipmentSlot.HEAD).isEmpty()
                || NBTUtil.getInteger(player.getItemStackFromSlot(EntityEquipmentSlot.HEAD), NBTKeys.PNEUMATIC_HELMET_DEBUGGING_DRONE) != this.getEntityId());
    }

    private class MinigunDrone extends Minigun {
        private final EntityDrone drone;

        MinigunDrone(EntityDrone drone) {
            super(true);
            this.drone = drone;
        }

        @Override
        public Object getSoundSource() {
            return this.drone;
        }

        @Override
        public boolean isMinigunActivated() {
            return EntityDrone.this.isMinigunActivated();
        }

        @Override
        public void setMinigunActivated(boolean activated) {
            EntityDrone.this.setMinigunActivated(activated);
        }

        @Override
        public void setAmmoColorStack(@Nonnull ItemStack ammo) {
            EntityDrone.this.setAmmoColor(ammo);
        }

        @Override
        public int getAmmoColor() {
            return EntityDrone.this.getAmmoColor();
        }

        @Override
        public void playSound(SoundEvent soundName, float volume, float pitch) {
            NetworkHandler.sendToAllAround(new PacketPlaySound(soundName, SoundCategory.NEUTRAL, EntityDrone.this.posX, EntityDrone.this.posY, EntityDrone.this.posZ, volume, pitch, true), this.world);
        }
    }

    private class EntityDroneItemHandler extends DroneItemHandler {
        public EntityDroneItemHandler(int size, IDrone holder) {
            super(size, holder);
        }

        @Override
        public void updateHeldItem() {
            if (this.heldItemChanged && ConfigHandler.client.dronesRenderHeldItem)
                EntityDrone.this.dataManager.set(HELD_ITEM, this.getStackInSlot(0));

            super.updateHeldItem();
        }
    }

    private class DroneDebugList {
        private final Map<Integer, DebugEntry> debugEntries = new HashMap<>();

        private DroneDebugList() {
        }

        void addEntry(DebugEntry entry) {
            this.debugEntries.put(EntityDrone.this.getActiveWidgetIndex(), entry);
        }

        public Collection<DebugEntry> getAll() {
            return this.debugEntries.values();
        }

        public DebugEntry get(int widgetId) {
            return this.debugEntries.get(widgetId);
        }

        public DebugEntry getCurrent() {
            return this.debugEntries.get(EntityDrone.this.getActiveWidgetIndex());
        }
    }
}
