package me.desht.pneumaticcraft.common.pneumatic_armor;

import me.desht.pneumaticcraft.PneumaticCraftRepressurized;
import me.desht.pneumaticcraft.api.client.pneumaticHelmet.IHackableBlock;
import me.desht.pneumaticcraft.api.client.pneumaticHelmet.IHackableEntity;
import me.desht.pneumaticcraft.api.client.pneumaticHelmet.IUpgradeRenderHandler;
import me.desht.pneumaticcraft.api.item.IItemRegistry.EnumUpgrade;
import me.desht.pneumaticcraft.api.item.IPressurizable;
import me.desht.pneumaticcraft.client.render.pneumatic_armor.UpgradeRenderHandlerList;
import me.desht.pneumaticcraft.client.render.pneumatic_armor.upgrade_handler.*;
import me.desht.pneumaticcraft.client.sound.MovingSounds;
import me.desht.pneumaticcraft.common.advancements.AdvancementTriggers;
import me.desht.pneumaticcraft.common.config.ConfigHandler;
import me.desht.pneumaticcraft.common.hacking.HackableHandler;
import me.desht.pneumaticcraft.common.item.ItemMachineUpgrade;
import me.desht.pneumaticcraft.common.item.ItemPneumaticArmor;
import me.desht.pneumaticcraft.common.item.ItemRegistry;
import me.desht.pneumaticcraft.common.network.*;
import me.desht.pneumaticcraft.common.util.UpgradableItemUtils;
import me.desht.pneumaticcraft.common.util.WorldAndCoord;
import me.desht.pneumaticcraft.lib.Names;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import me.desht.pneumaticcraft.lib.Sounds;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumDifficulty;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommonArmorHandler {
    private static final CommonArmorHandler clientHandler = new CommonArmorHandler(null);
    private static final CommonArmorHandler serverHandler = new CommonArmorHandler(null);

    private static Potion nightVisionPotion;

    private final HashMap<String, CommonArmorHandler> playerHudHandlers = new HashMap<>();
    private EntityPlayer player;
    private int magnetRadius;
    private int magnetRadiusSq;
    private final boolean[][] upgradeRenderersInserted = new boolean[4][];
    private final boolean[][] upgradeRenderersEnabled = new boolean[4][];
    private final int[] ticksSinceEquip = new int[4];
    public final float[] armorPressure = new float[4];
    private final int[][] upgradeMatrix = new int[4][];
    private final int[] startupTimes = new int[4];

    private boolean isValid; // true if the handler is valid; gets invalidated if player disconnects

    private int hackTime;
    private WorldAndCoord hackedBlock;
    private Entity hackedEntity;

    private boolean armorEnabled;
    private boolean magnetEnabled;
    private boolean chargingEnabled;
    private boolean stepAssistEnabled;
    private boolean runSpeedEnabled;
    private boolean jumpBoostEnabled;
    private boolean entityTrackerEnabled;
    private boolean nightVisionEnabled;
    private boolean scubaEnabled;
    private boolean airConEnabled;
    private boolean jetBootsEnabled;  // are jet boots switched on?
    private boolean jetBootsActive;  // are jet boots actually firing (player rising) ?
    private float flightAccel = 1.0F;  // increases while diving, decreases while climbing
    private int prevJetBootsAirUsage;  // so we know when the jet boots are starting up
    private int jetBootsActiveTicks;
    private boolean wasNightVisionEnabled;
    private float speedBoostMult;
    private boolean jetBootsBuilderMode;

    private CommonArmorHandler(EntityPlayer player) {
        this.player = player;
        for (EntityEquipmentSlot slot : UpgradeRenderHandlerList.ARMOR_SLOTS) {
            List<IUpgradeRenderHandler> renderHandlers = UpgradeRenderHandlerList.instance().getHandlersForSlot(slot);
            this.upgradeRenderersInserted[slot.getIndex()] = new boolean[renderHandlers.size()];
            this.upgradeRenderersEnabled[slot.getIndex()] = new boolean[renderHandlers.size()];
            this.upgradeMatrix[slot.getIndex()] = new int[EnumUpgrade.values().length];
        }
        Arrays.fill(this.startupTimes, 200);
        this.isValid = true;
    }

    private static CommonArmorHandler getManagerInstance(EntityPlayer player) {
        return player.world.isRemote ? clientHandler : serverHandler;
    }

    public static CommonArmorHandler getHandlerForPlayer(EntityPlayer player) {
        return getManagerInstance(player).playerHudHandlers.computeIfAbsent(player.getName(), v -> new CommonArmorHandler(player));
    }

    @SideOnly(Side.CLIENT)
    public static CommonArmorHandler getHandlerForPlayer() {
        return getHandlerForPlayer(FMLClientHandler.instance().getClient().player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            getHandlerForPlayer(event.player).tick();
        }
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        // called server side when player logs off
        clearHUDHandlerForPlayer(event.player);
    }

    @SubscribeEvent
    public static void onPlayerJoinWorld(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntity();
            CommonArmorHandler handler = getManagerInstance(player).playerHudHandlers.get(player.getName());
            if (handler != null) handler.player = player;
        }
    }

    @SubscribeEvent
    public static void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        // called client side when client disconnects
        EntityPlayer player = PneumaticCraftRepressurized.proxy.getClientPlayer();
        if (player != null) {
            clearHUDHandlerForPlayer(player);
        }
    }

    private static void clearHUDHandlerForPlayer(EntityPlayer player) {
        CommonArmorHandler h = getManagerInstance(player);
        h.playerHudHandlers.computeIfPresent(player.getName(), (name, val) -> {
            val.invalidate();
            return null;
        });
    }

    private static Potion getNightVisionPotion() {
        if (nightVisionPotion == null) nightVisionPotion = Potion.getPotionFromResourceLocation("night_vision");
        return nightVisionPotion;
    }

    private void tick() {
        for (EntityEquipmentSlot slot : UpgradeRenderHandlerList.ARMOR_SLOTS) {
            this.tickArmorPiece(slot);
        }
        if (!this.player.world.isRemote) {
            this.handleHacking();
        }
    }

    private void tickArmorPiece(EntityEquipmentSlot slot) {
        ItemStack armorStack = this.player.getItemStackFromSlot(slot);
        boolean armorActive = false;
        if (armorStack.getItem() instanceof ItemPneumaticArmor) {
            this.armorPressure[slot.getIndex()] = ((IPressurizable) armorStack.getItem()).getPressure(armorStack);
            if (this.ticksSinceEquip[slot.getIndex()] == 0) {
                this.initArmorInventory(slot);
            }
            this.ticksSinceEquip[slot.getIndex()]++;
            if (this.armorEnabled && this.armorPressure[slot.getIndex()] > 0F) {
                armorActive = true;
                if (!this.player.world.isRemote) {
                    if (this.isArmorReady(slot) && !this.player.capabilities.isCreativeMode) {
                        // use up air in the armor piece
                        float airUsage = UpgradeRenderHandlerList.instance().getAirUsage(this.player, slot, false);
                        if (airUsage != 0) {
                            float oldPressure = this.addAir(slot, (int) -airUsage);
                            if (oldPressure > 0F && this.armorPressure[slot.getIndex()] == 0F) {
                                // out of air!
                                NetworkHandler.sendTo(new PacketPlaySound(Sounds.MINIGUN_STOP, SoundCategory.PLAYERS, this.player.posX, this.player.posY, this.player.posZ, 1.0f, 2.0f, false), (EntityPlayerMP) this.player);
                            }
                        }
                    }
                }
                this.doArmorActions(slot);
            }
        }
        if (!armorActive) {
            if (this.ticksSinceEquip[slot.getIndex()] > 0) {
                this.onArmorRemoved(slot);
            }
            this.ticksSinceEquip[slot.getIndex()] = 0;
        }
    }

    /*
     * Called when an armor piece is removed, or otherwise disabled - out of air, armor disabled
     */
    private void onArmorRemoved(EntityEquipmentSlot slot) {
        switch (slot) {
            case HEAD:
                if (this.nightVisionEnabled) this.player.removeActivePotionEffect(getNightVisionPotion());
                break;
            case FEET:
                this.player.stepHeight = 0.6F;
                break;
        }
    }

    public float addAir(EntityEquipmentSlot slot, int air) {
        ItemStack armorStack = this.player.getItemStackFromSlot(slot);
        float oldPressure = this.armorPressure[slot.getIndex()];
        if (armorStack.getItem() instanceof IPressurizable) {
            ((IPressurizable) armorStack.getItem()).addAir(armorStack, air);
            this.armorPressure[slot.getIndex()] = ((IPressurizable) armorStack.getItem()).getPressure(armorStack);
        }
        return oldPressure;
    }

    private void doArmorActions(EntityEquipmentSlot slot) {
        if (!this.isArmorReady(slot)) return;

        switch (slot) {
            case HEAD:
                this.handleNightVision();
                this.handleScuba();
                break;
            case CHEST:
                this.handleChestplateMagnet();
                this.handleChestplateCharging();
                break;
            case LEGS:
                this.handleLeggingsSpeedBoost();
                break;
            case FEET:
                if (this.getArmorPressure(EntityEquipmentSlot.FEET) > 0.0F && this.isStepAssistEnabled()) {
                    this.player.stepHeight = this.player.isSneaking() ? 0.6001F : 1.25F;
                } else {
                    this.player.stepHeight = 0.6F;
                }
                this.handleJetBoots();
                break;
        }

        if (!this.player.world.isRemote && this.getUpgradeCount(slot, EnumUpgrade.ITEM_LIFE) > 0) {
            this.handleItemRepair(slot);
        }
    }

    private void handleNightVision() {
        // checking every 8 ticks should be enough
        if (!this.player.world.isRemote && (this.getTicksSinceEquipped(EntityEquipmentSlot.HEAD) & 0x7) == 0) {
            boolean shouldEnable = this.getArmorPressure(EntityEquipmentSlot.HEAD) > 0.0f
                    && this.getUpgradeCount(EntityEquipmentSlot.HEAD, EnumUpgrade.NIGHT_VISION) > 0
                    && this.nightVisionEnabled;
            if (shouldEnable) {
                ItemStack helmetStack = this.player.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
                this.player.addPotionEffect(new PotionEffect(getNightVisionPotion(), 500, 0, false, false));
                this.addAir(EntityEquipmentSlot.HEAD, -PneumaticValues.PNEUMATIC_NIGHT_VISION_USAGE * 8);
            } else if (!shouldEnable && this.wasNightVisionEnabled) {
                this.player.removePotionEffect(getNightVisionPotion());
            }
            this.wasNightVisionEnabled = shouldEnable;
        }
    }

    private void handleScuba() {
        // checking every 16 ticks
        if (!this.player.world.isRemote
                && this.scubaEnabled && this.getUpgradeCount(EntityEquipmentSlot.HEAD, EnumUpgrade.SCUBA) > 0
                && this.getArmorPressure(EntityEquipmentSlot.HEAD) > 0.1f
                && this.player.getAir() < 200) {

            ItemStack helmetStack = this.player.getItemStackFromSlot(EntityEquipmentSlot.HEAD);

            int vol = ((ItemPneumaticArmor) helmetStack.getItem()).getBaseVolume() + PneumaticValues.VOLUME_VOLUME_UPGRADE * this.getUpgradeCount(EntityEquipmentSlot.HEAD, EnumUpgrade.VOLUME);
            float airInHelmet = this.getArmorPressure(EntityEquipmentSlot.HEAD) * vol;
            int playerAir = (int) Math.min(300 - this.player.getAir(), airInHelmet / PneumaticValues.PNEUMATIC_HELMET_SCUBA_MULTIPLIER);
            this.player.setAir(this.player.getAir() + playerAir);

            int airUsed = playerAir * PneumaticValues.PNEUMATIC_HELMET_SCUBA_MULTIPLIER;
            this.addAir(EntityEquipmentSlot.HEAD, -airUsed);
            NetworkHandler.sendTo(new PacketPlaySound(Sounds.SCUBA, SoundCategory.PLAYERS, this.player.getPosition(), 1.5f, 1.0f, false), (EntityPlayerMP) this.player);
            Vec3d eyes = this.player.getPositionEyes(1.0f).add(this.player.getLookVec().scale(0.5));
            NetworkHandler.sendToAllAround(new PacketSpawnParticle(EnumParticleTypes.WATER_BUBBLE, eyes.x - 0.5, eyes.y, eyes.z - 0.5, 0.0, 0.2, 0.0, 10, 1.0, 1.0, 1.0), this.player.world);
        }
    }

    // track player movement across ticks on the server - very transient, a capability would be overkill here
    private static final Map<String, Vec3d> moveMap = new HashMap<>();

    private void handleLeggingsSpeedBoost() {
        double speedBoost = this.getSpeedBoostFromLegs();
        if (this.player.world.isRemote) {
            // doing this client-side only appears to be effective
            if (this.player.moveForward > 0) {
                if (!this.player.onGround && this.isJetBootsEnabled() && this.jetBootsBuilderMode) {
                    this.player.moveRelative(0, 0, 1, this.getUpgradeCount(EntityEquipmentSlot.FEET, EnumUpgrade.JET_BOOTS) / 400f);
                }
                if (this.player.onGround && !this.player.isInsideOfMaterial(Material.WATER)) {
                    this.player.moveRelative(0, 0, 1, (float) speedBoost);
                }
            }
        }
        if (!this.player.world.isRemote && speedBoost > 0) {
            Vec3d prev = moveMap.get(this.player.getName());
            boolean moved = prev != null && (Math.abs(this.player.posX - prev.x) > 0.0001 || Math.abs(this.player.posZ - prev.z) > 0.0001);
            if (moved && this.player.onGround && !this.player.isInsideOfMaterial(Material.WATER)) {
                int airUsage = (int) Math.ceil(PneumaticValues.PNEUMATIC_LEGS_SPEED_USAGE * speedBoost * 4);
                ItemStack legsStack = this.player.getItemStackFromSlot(EntityEquipmentSlot.LEGS);
                this.addAir(EntityEquipmentSlot.LEGS, -airUsage);
            }
            moveMap.put(this.player.getName(), new Vec3d(this.player.posX, this.player.posY, this.player.posZ));
        }
    }

    public double getSpeedBoostFromLegs() {
        int speedUpgrades = this.getUpgradeCount(EntityEquipmentSlot.LEGS, EnumUpgrade.SPEED, PneumaticValues.PNEUMATIC_LEGS_MAX_SPEED);
        if (this.isArmorReady(EntityEquipmentSlot.LEGS) && speedUpgrades > 0 && this.isRunSpeedEnabled() && this.getArmorPressure(EntityEquipmentSlot.LEGS) > 0.0F) {
            return PneumaticValues.PNEUMATIC_LEGS_BOOST_PER_UPGRADE * speedUpgrades * this.speedBoostMult;
        } else {
            return 0.0;
        }
    }

    private void handleJetBoots() {
        int jetbootsCount = this.getUpgradeCount(EntityEquipmentSlot.FEET, EnumUpgrade.JET_BOOTS, PneumaticValues.PNEUMATIC_JET_BOOTS_MAX_UPGRADES);
        if (jetbootsCount == 0) return;

        int jetbootsAirUsage = 0;
        if (this.getArmorPressure(EntityEquipmentSlot.FEET) > 0.0F) {
            if (this.isJetBootsActive()) {
                if (this.jetBootsBuilderMode && jetbootsCount >= 8) {
                    // builder mode - rise vertically (or hover if sneaking and firing)
                    this.player.motionY = this.player.isSneaking() ? 0 : 0.15 + 0.15 * (jetbootsCount - 8);
                    jetbootsAirUsage = (int) (ConfigHandler.pneumaticArmor.jetbootsAirUsage * jetbootsCount / 5F);
                } else {
                    // jetboots firing - move in direction of looking
                    Vec3d lookVec = this.player.getLookVec().scale(0.15 * jetbootsCount);
                    this.flightAccel += lookVec.y / -20.0;
                    this.flightAccel = MathHelper.clamp(this.flightAccel, 0.8F, 4.0F);
                    lookVec = lookVec.scale(this.flightAccel);
                    if (this.jetBootsActiveTicks < 10) lookVec = lookVec.scale(this.jetBootsActiveTicks * 0.1);
                    this.player.motionX = lookVec.x;
                    this.player.motionY = this.player.onGround ? 0 : lookVec.y;
                    this.player.motionZ = lookVec.z;
                    jetbootsAirUsage = ConfigHandler.pneumaticArmor.jetbootsAirUsage * jetbootsCount;
                }
                this.jetBootsActiveTicks++;
            } else if (this.isJetBootsEnabled() && !this.player.onGround) {
                // jetboots not firing, but enabled - slowly descend (or hover if enough upgrades)
                if (jetbootsCount > 6 && !this.player.isSneaking()) this.player.motionY = 0;
                else this.player.motionY = this.player.isSneaking() ? -0.45 : -0.15 + 0.015 * jetbootsCount;
                this.player.fallDistance = 0;
                jetbootsAirUsage = (int) (ConfigHandler.pneumaticArmor.jetbootsAirUsage * (this.player.isSneaking() ? 0.25F : 0.5F));
                this.flightAccel = 1.0F;
            } else {
                this.flightAccel = 1.0F;
            }
        }
        if (jetbootsAirUsage != 0 && !this.player.world.isRemote) {
            if (this.prevJetBootsAirUsage == 0) {
                NetworkHandler.sendToDimension(new PacketPlayMovingSound(MovingSounds.Sound.JET_BOOTS, this.player), this.player.world.provider.getDimension());
                AdvancementTriggers.FLIGHT.trigger((EntityPlayerMP) this.player);
            }
            if (this.player.collidedHorizontally) {
                double vel = Math.sqrt(this.player.motionZ * this.player.motionZ + this.player.motionX * this.player.motionX);
                if (this.player.world.getDifficulty() == EnumDifficulty.HARD) {
                    vel *= 2;
                } else if (this.player.world.getDifficulty() == EnumDifficulty.NORMAL) {
                    vel *= 1.5;
                }
                if (vel > 2) {
                    this.player.playSound(vel > 2.5 ? SoundEvents.ENTITY_GENERIC_BIG_FALL : SoundEvents.ENTITY_GENERIC_SMALL_FALL, 1.0F, 1.0F);
                    this.player.attackEntityFrom(DamageSource.FLY_INTO_WALL, (float) vel);
                    AdvancementTriggers.FLY_INTO_WALL.trigger((EntityPlayerMP) this.player);
                }
            }
            this.addAir(EntityEquipmentSlot.FEET, -jetbootsAirUsage);
        }
        this.prevJetBootsAirUsage = jetbootsAirUsage;
    }

    private void handleChestplateCharging() {
        if (this.player.world.isRemote || !this.chargingEnabled
                || this.getUpgradeCount(EntityEquipmentSlot.CHEST, EnumUpgrade.CHARGING) == 0
                || this.getTicksSinceEquipped(EntityEquipmentSlot.CHEST) % PneumaticValues.ARMOR_CHARGER_INTERVAL != 5)
            return;

        int upgrades = this.getUpgradeCount(EntityEquipmentSlot.CHEST, EnumUpgrade.CHARGING, PneumaticValues.ARMOR_CHARGING_MAX_UPGRADES);
        int airAmount = upgrades * 100 + 100;

        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            if (slot == EntityEquipmentSlot.CHEST) continue;
            if (this.armorPressure[EntityEquipmentSlot.CHEST.getIndex()] < 0.1F) return;
            ItemStack stack = this.player.getItemStackFromSlot(slot);
            this.tryPressurize(airAmount, stack);
        }
        for (ItemStack stack : this.player.inventory.mainInventory) {
            if (this.armorPressure[EntityEquipmentSlot.CHEST.getIndex()] < 0.1F) return;
            this.tryPressurize(airAmount, stack);
        }
    }

    private void tryPressurize(int airAmount, ItemStack destStack) {
        if (destStack.getItem() instanceof IPressurizable) {
            IPressurizable p = (IPressurizable) destStack.getItem();
            float pressure = p.getPressure(destStack);
            if (pressure < p.maxPressure(destStack) && pressure < this.armorPressure[EntityEquipmentSlot.CHEST.getIndex()]) {
                float currentAir = pressure * p.getVolume(destStack);
                float targetAir = this.armorPressure[EntityEquipmentSlot.CHEST.getIndex()] * p.getVolume(destStack);
                int amountToMove = Math.min((int) (targetAir - currentAir), airAmount);
                p.addAir(destStack, amountToMove);
                this.addAir(EntityEquipmentSlot.CHEST, -amountToMove);
            }
        }
    }

    private void handleItemRepair(EntityEquipmentSlot slot) {
        int upgrades = this.getUpgradeCount(slot, EnumUpgrade.ITEM_LIFE, PneumaticValues.ARMOR_REPAIR_MAX_UPGRADES);
        int interval = 120 - (20 * upgrades);
        int airUsage = PneumaticValues.PNEUMATIC_ARMOR_REPAIR_USAGE * upgrades;

        ItemStack armorStack = this.player.getItemStackFromSlot(slot);
        if (armorStack.getItemDamage() > 0
                && this.armorPressure[slot.getIndex()] > 0.1F
                && this.ticksSinceEquip[slot.getIndex()] % interval == 0) {
            this.addAir(slot, -airUsage);
            armorStack.setItemDamage(armorStack.getItemDamage() - 1);
        }
    }

    private void handleChestplateMagnet() {
        if (this.player.world.isRemote || !this.magnetEnabled || (this.getTicksSinceEquipped(EntityEquipmentSlot.CHEST) & 0x3) != 0
                || this.getUpgradeCount(EntityEquipmentSlot.CHEST, EnumUpgrade.MAGNET) == 0)
            return;

        AxisAlignedBB box = new AxisAlignedBB(this.player.getPosition()).grow(this.magnetRadius);
        List<Entity> itemList = this.player.getEntityWorld().getEntitiesWithinAABB(Entity.class, box,
                e -> (e instanceof EntityXPOrb || e instanceof EntityItem) && e.isEntityAlive());

        Vec3d playerVec = this.player.getPositionVector();
        for (Entity item : itemList) {
            if (item instanceof EntityItem && ((EntityItem) item).cannotPickup()) continue;

            if (item.getPositionVector().squareDistanceTo(playerVec) <= this.magnetRadiusSq
                    && !ItemRegistry.getInstance().shouldSuppressMagnet(item)
                    && !item.getEntityData().getBoolean(Names.PREVENT_REMOTE_MOVEMENT)) {
                if (this.armorPressure[EntityEquipmentSlot.CHEST.getIndex()] < 0.1F) break;
                item.setPosition(this.player.posX, this.player.posY, this.player.posZ);
                if (item instanceof EntityItem) ((EntityItem) item).setPickupDelay(0);
                this.addAir(EntityEquipmentSlot.CHEST, -PneumaticValues.MAGNET_AIR_USAGE);
            }
        }
    }

    private void handleHacking() {
        if (this.hackedBlock != null) {
            IHackableBlock hackableBlock = HackableHandler.getHackableForCoord(this.hackedBlock, this.player);
            if (hackableBlock != null) {
                if (++this.hackTime >= hackableBlock.getHackTime(this.hackedBlock.world, this.hackedBlock.pos, this.player)) {
                    hackableBlock.onHackFinished(this.player.world, this.hackedBlock.pos, this.player);
                    PneumaticCraftRepressurized.proxy.getHackTickHandler().trackBlock(this.hackedBlock, hackableBlock);
                    NetworkHandler.sendToAllAround(new PacketHackingBlockFinish(this.hackedBlock), this.player.world);
                    this.setHackedBlock(null);
                    AdvancementTriggers.BLOCK_HACK.trigger((EntityPlayerMP) this.player);  // safe to cast, this is server-side
                }
            } else {
                this.setHackedBlock(null);
            }
        } else if (this.hackedEntity != null) {
            IHackableEntity hackableEntity = HackableHandler.getHackableForEntity(this.hackedEntity, this.player);
            if (hackableEntity != null) {
                if (++this.hackTime >= hackableEntity.getHackTime(this.hackedEntity, this.player)) {
                    hackableEntity.onHackFinished(this.hackedEntity, this.player);
                    PneumaticCraftRepressurized.proxy.getHackTickHandler().trackEntity(this.hackedEntity, hackableEntity);
                    NetworkHandler.sendToAllAround(new PacketHackingEntityFinish(this.hackedEntity), new TargetPoint(this.hackedEntity.world.provider.getDimension(), this.hackedEntity.posX, this.hackedEntity.posY, this.hackedEntity.posZ, 64));
                    this.setHackedEntity(null);
                    AdvancementTriggers.ENTITY_HACK.trigger((EntityPlayerMP) this.player);  // safe to cast, this is server-side
                }
            } else {
                this.setHackedEntity(null);
            }
        }
    }

    /**
     * Called on the first tick after the armor piece is equipped.
     * <p>
     * Scan the armor piece in the given slot, and record all installed upgrades for fast access later on.  Upgrades
     * can't be changed without removing and re-equipping the piece, so we can cache quite a lot of useful info.
     *
     * @param slot the equipment slot
     */
    public void initArmorInventory(EntityEquipmentSlot slot) {
        // armorStack has already been validated as a pneumatic armor piece at this point
        ItemStack armorStack = this.player.getItemStackFromSlot(slot);

        // record which upgrades / render-handlers are inserted
        ItemStack[] upgradeStacks = UpgradableItemUtils.getUpgradeStacks(armorStack);
        Arrays.fill(this.upgradeRenderersInserted[slot.getIndex()], false);
        for (int i = 0; i < this.upgradeRenderersInserted[slot.getIndex()].length; i++) {
            this.upgradeRenderersInserted[slot.getIndex()][i] = this.isModuleEnabled(upgradeStacks, UpgradeRenderHandlerList.instance().getHandlersForSlot(slot).get(i));
        }

        // record the number of upgrades of every type
        Arrays.fill(this.upgradeMatrix[slot.getIndex()], 0);
        for (ItemStack stack : upgradeStacks) {
            if (stack.getItem() instanceof ItemMachineUpgrade) {
                this.upgradeMatrix[slot.getIndex()][((ItemMachineUpgrade) stack.getItem()).getUpgradeType().ordinal()] += stack.getCount();
            }
        }
        this.startupTimes[slot.getIndex()] = (int) (ConfigHandler.pneumaticArmor.armorStartupTime * Math.pow(0.8, this.getSpeedFromUpgrades(slot) - 1));

        // some slot-specific setup
        switch (slot) {
            case CHEST:
                this.magnetRadius = PneumaticValues.MAGNET_BASE_RANGE
                        + Math.min(this.getUpgradeCount(EntityEquipmentSlot.CHEST, EnumUpgrade.MAGNET), PneumaticValues.MAGNET_MAX_UPGRADES);
                this.magnetRadiusSq = this.magnetRadius * this.magnetRadius;
                break;
            case LEGS:
                this.speedBoostMult = ItemPneumaticArmor.getIntData(armorStack, ItemPneumaticArmor.NBT_SPEED_BOOST, 100) / 100f;
                break;
            case FEET:
                this.jetBootsBuilderMode = ItemPneumaticArmor.getBooleanData(armorStack, ItemPneumaticArmor.NBT_BUILDER_MODE, false);
                JetBootsStateTracker.getTracker(this.player).setJetBootsState(this.player, this.isJetBootsEnabled(), this.isJetBootsActive(), this.jetBootsBuilderMode);
                break;
        }
    }

    public int getUpgradeCount(EntityEquipmentSlot slot, EnumUpgrade upgrade) {
        return this.upgradeMatrix[slot.getIndex()][upgrade.ordinal()];
    }

    public int getUpgradeCount(EntityEquipmentSlot slot, EnumUpgrade upgrade, int max) {
        return Math.min(max, this.upgradeMatrix[slot.getIndex()][upgrade.ordinal()]);
    }

    public boolean isUpgradeRendererInserted(EntityEquipmentSlot slot, int i) {
        return this.upgradeRenderersInserted[slot.getIndex()][i];
    }

    public boolean isUpgradeRendererEnabled(EntityEquipmentSlot slot, int i) {
        return this.upgradeRenderersEnabled[slot.getIndex()][i];
    }

    public void setUpgradeRenderEnabled(EntityEquipmentSlot slot, byte featureIndex, boolean state) {
        this.upgradeRenderersEnabled[slot.getIndex()][featureIndex] = state;
        IUpgradeRenderHandler handler = UpgradeRenderHandlerList.instance().getHandlersForSlot(slot).get(featureIndex);
        // bit of a code smell here, but caching the enablement of various features is important for performance
        if (handler instanceof MagnetUpgradeHandler) {
            this.magnetEnabled = state;
        } else if (handler instanceof ChargingUpgradeHandler) {
            this.chargingEnabled = state;
        } else if (handler instanceof StepAssistUpgradeHandler) {
            this.stepAssistEnabled = state;
        } else if (handler instanceof RunSpeedUpgradeHandler) {
            this.runSpeedEnabled = state;
        } else if (handler instanceof JumpBoostUpgradeHandler) {
            this.jumpBoostEnabled = state;
        } else if (handler instanceof JetBootsUpgradeHandler) {
            this.jetBootsEnabled = state;
            JetBootsStateTracker.getTracker(this.player).setJetBootsState(this.player, this.jetBootsEnabled, this.isJetBootsActive(), this.isJetBootsBuilderMode());
        } else if (handler instanceof MainHelmetHandler) {
            this.armorEnabled = state;
        } else if (handler instanceof EntityTrackUpgradeHandler) {
            this.entityTrackerEnabled = state;
        } else if (handler instanceof NightVisionUpgradeHandler) {
            this.nightVisionEnabled = state;
        } else if (handler instanceof ScubaUpgradeHandler) {
            this.scubaEnabled = state;
        } else if (handler instanceof AirConUpgradeHandler) {
            this.airConEnabled = state;
        }
    }

    public int getTicksSinceEquipped(EntityEquipmentSlot slot) {
        return this.ticksSinceEquip[slot.getIndex()];
    }

    private boolean isModuleEnabled(ItemStack[] helmetStacks, IUpgradeRenderHandler handler) {
        for (Item requiredUpgrade : handler.getRequiredUpgrades()) {
            boolean found = false;
            for (ItemStack stack : helmetStacks) {
                if (stack.getItem() == requiredUpgrade) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    public int getSpeedFromUpgrades(EntityEquipmentSlot slot) {
        return 1 + this.getUpgradeCount(slot, EnumUpgrade.SPEED);
    }

    public int getStartupTime(EntityEquipmentSlot slot) {
        return this.startupTimes[slot.getIndex()];
    }

    public void setHackedBlock(WorldAndCoord blockPos) {
        this.hackedBlock = blockPos;
        this.hackedEntity = null;
        this.hackTime = 0;
    }

    public void setHackedEntity(Entity entity) {
        this.hackedEntity = entity;
        this.hackedBlock = null;
        this.hackTime = 0;
    }

    public boolean isArmorReady(EntityEquipmentSlot slot) {
        return this.getTicksSinceEquipped(slot) > this.getStartupTime(slot);
    }

    public boolean isStepAssistEnabled() {
        return this.stepAssistEnabled;
    }

    public boolean isRunSpeedEnabled() {
        return this.runSpeedEnabled;
    }

    public boolean isJumpBoostEnabled() {
        return this.jumpBoostEnabled;
    }

    public boolean isAirConEnabled() {
        return this.airConEnabled;
    }

    public float getArmorPressure(EntityEquipmentSlot slot) {
        return this.armorPressure[slot.getIndex()];
    }

    public void setJetBootsActive(boolean jetBootsActive) {
        if (!jetBootsActive) {
            this.jetBootsActiveTicks = 0;
        }
        this.jetBootsActive = jetBootsActive;
        JetBootsStateTracker.getTracker(this.player).setJetBootsState(this.player, this.isJetBootsEnabled(), jetBootsActive, this.isJetBootsBuilderMode());
    }

    public boolean isJetBootsActive() {
        return this.jetBootsActive;
    }

    public boolean isJetBootsEnabled() {
        return this.jetBootsEnabled;
    }

    public boolean isArmorEnabled() {
        return this.armorEnabled;
    }

    public boolean isEntityTrackerEnabled() {
        return this.entityTrackerEnabled;
    }

    public boolean isScubaEnabled() {
        return this.scubaEnabled;
    }

    public boolean isValid() {
        return this.isValid;
    }

    public void invalidate() {
        this.isValid = false;
    }

    public boolean isJetBootsBuilderMode() {
        return this.jetBootsBuilderMode;
    }

    /**
     * Called both client- and server-side when a custom NBT field in an armor item has been updated.  Used to
     * cache data (e.g. legs speed boost %) for performance reasons.
     *
     * @param slot    the armor slot
     * @param key     the data key
     * @param dataTag the data item, to be interpreted depending on the key
     */
    public void onDataFieldUpdated(EntityEquipmentSlot slot, String key, NBTBase dataTag) {
        switch (key) {
            case ItemPneumaticArmor.NBT_SPEED_BOOST:
                this.speedBoostMult = MathHelper.clamp(((NBTTagInt) dataTag).getInt() / 100f, 0.0f, 1.0f);
                break;
            case ItemPneumaticArmor.NBT_BUILDER_MODE:
                this.jetBootsBuilderMode = ((NBTTagByte) dataTag).getByte() == 1;
                JetBootsStateTracker.getTracker(this.player).getJetBootsState(this.player).setBuilderMode(this.jetBootsBuilderMode);
                break;
        }
    }
}
