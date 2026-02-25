package me.desht.pneumaticcraft.common.minigun;

import me.desht.pneumaticcraft.api.item.IItemRegistry.EnumUpgrade;
import me.desht.pneumaticcraft.api.item.IPressurizable;
import me.desht.pneumaticcraft.client.render.RenderProgressingLine;
import me.desht.pneumaticcraft.client.sound.MovingSounds;
import me.desht.pneumaticcraft.client.util.RenderUtils;
import me.desht.pneumaticcraft.common.config.ConfigHandler;
import me.desht.pneumaticcraft.common.entity.living.EntityDrone;
import me.desht.pneumaticcraft.common.item.ItemGunAmmo;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketPlayMovingSound;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.Sounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import java.util.Random;

public abstract class Minigun {
    public static final double MAX_GUN_SPEED = 0.4;
    private static final double MAX_GUN_YAW_CHANGE = 10;
    private static final double MAX_GUN_PITCH_CHANGE = 10;

    private final boolean requiresTarget;

    private double minigunSpeed;
    private int minigunTriggerTimeOut;
    private int minigunSoundCounter = -1;
    private final Random rand = new Random();
    private double minigunRotation, oldMinigunRotation;
    public double minigunYaw, oldMinigunYaw;
    public double minigunPitch, oldMinigunPitch;
    private final RenderProgressingLine minigunFire = new RenderProgressingLine().setProgress(1);
    private boolean sweeping; //When true, the yaw of the minigun will sweep with a sinus pattern when not targeting.
    private double sweepingProgress;

    private boolean gunAimedAtTarget;

    private IPressurizable pressurizable;
    private int airUsage;
    protected ItemStack minigunStack = ItemStack.EMPTY;
    private ItemStack ammoStack = ItemStack.EMPTY;
    protected EntityPlayer player;
    protected World world;
    private EntityLivingBase attackTarget;

    public Minigun(boolean requiresTarget) {
        this.requiresTarget = requiresTarget;
    }

    public Minigun setPressurizable(IPressurizable pressurizable, int airUsage) {
        this.pressurizable = pressurizable;
        this.airUsage = airUsage;
        return this;
    }

    public Minigun setItemStack(@Nonnull ItemStack stack) {
        this.minigunStack = stack;
        return this;
    }

    public Minigun setAmmoStack(@Nonnull ItemStack ammoStack) {
        this.ammoStack = ammoStack;
        return this;
    }

    @Nonnull
    public ItemStack getAmmoStack() {
        return this.ammoStack;
    }

    public Minigun setPlayer(EntityPlayer player) {
        this.player = player;
        return this;
    }

    public EntityPlayer getPlayer() {
        return this.player;
    }

    public Minigun setWorld(World world) {
        this.world = world;
        return this;
    }

    public World getWorld() {
        return this.world;
    }

    public Minigun setAttackTarget(EntityLivingBase entity) {
        this.attackTarget = entity;
        return this;
    }

    public abstract boolean isMinigunActivated();

    public abstract void setMinigunActivated(boolean activated);

    public abstract void setAmmoColorStack(@Nonnull ItemStack ammo);

    public abstract int getAmmoColor();

    public abstract void playSound(SoundEvent soundName, float volume, float pitch);

    protected int getAmmoColor(@Nonnull ItemStack stack) {
        return stack.isEmpty() ? 0xFF313131 : Minecraft.getMinecraft().getItemColors().colorMultiplier(stack, 1);
    }

    /**
     * Get the source for this sound, where the client should play the sound loop at.  Can be an Entity, a
     * TileEntity, or a BlockPos; anything else will cause an exception to be thrown.
     *
     * @return the sound's source
     */
    public Object getSoundSource() {
        return this.player;
    }

    public double getMinigunSpeed() {
        return this.minigunSpeed;
    }

    public void setMinigunSpeed(double minigunSpeed) {
        this.minigunSpeed = minigunSpeed;
    }

    public int getMinigunTriggerTimeOut() {
        return this.minigunTriggerTimeOut;
    }

    public void setMinigunTriggerTimeOut(int minigunTriggerTimeOut) {
        this.minigunTriggerTimeOut = minigunTriggerTimeOut;
    }

    public int getMinigunSoundCounter() {
        return this.minigunSoundCounter;
    }

    public void setMinigunSoundCounter(int minigunSoundCounter) {
        this.minigunSoundCounter = minigunSoundCounter;
    }

    public double getMinigunRotation() {
        return this.minigunRotation;
    }

    public void setMinigunRotation(double minigunRotation) {
        this.minigunRotation = minigunRotation;
    }

    public double getOldMinigunRotation() {
        return this.oldMinigunRotation;
    }

    public void setOldMinigunRotation(double oldMinigunRotation) {
        this.oldMinigunRotation = oldMinigunRotation;
    }

    public EntityLivingBase getAttackTarget() {
        return this.attackTarget;
    }

    public void setSweeping(boolean sweeping) {
        this.sweeping = sweeping;
    }

    public boolean isSweeping() {
        return this.sweeping;
    }

    public boolean tryFireMinigun(Entity target) {
        boolean lastShotOfAmmo = false;
        if (!this.ammoStack.isEmpty() && (this.pressurizable == null || this.pressurizable.getPressure(this.minigunStack) > 0)) {
            this.setMinigunTriggerTimeOut(Math.max(10, this.getMinigunSoundCounter()));
            if (this.getMinigunSpeed() == MAX_GUN_SPEED && (!this.requiresTarget || this.gunAimedAtTarget)) {
                RayTraceResult rtr = null;
                ItemGunAmmo ammoItem = (ItemGunAmmo) this.ammoStack.getItem();
                if (!this.requiresTarget) {
                    rtr = PneumaticCraftUtils.getMouseOverServer(this.player, this.getRange());
                    target = rtr.entityHit;
                }
                if (this.pressurizable != null) {
                    int usage = (int) Math.ceil(this.airUsage * ammoItem.getAirUsageMultiplier(this, this.ammoStack));
                    usage += this.getUpgrades(EnumUpgrade.RANGE);
                    if (this.getUpgrades(EnumUpgrade.SPEED) > 0) {
                        usage *= this.getUpgrades(EnumUpgrade.SPEED) + 1;
                    }
                    this.pressurizable.addAir(this.minigunStack, -usage);
                }
                int roundsUsed = 1;
                if (target != null) {
                    if (this.getUpgrades(EnumUpgrade.SECURITY) == 0 || !this.securityProtectedTarget(target)) {
                        roundsUsed = ammoItem.onTargetHit(this, this.ammoStack, target);
                    }
                } else if (rtr != null && rtr.typeOfHit == RayTraceResult.Type.BLOCK) {
                    roundsUsed = ammoItem.onBlockHit(this, this.ammoStack, rtr.getBlockPos(), rtr.sideHit, rtr.hitVec);
                }
                int ammoCost = roundsUsed * ammoItem.getAmmoCost(this.ammoStack);
                lastShotOfAmmo = this.ammoStack.attemptDamageItem(ammoCost, this.rand, this.player instanceof EntityPlayerMP ? (EntityPlayerMP) this.player : null);
            }
        }
        return lastShotOfAmmo;
    }

    private boolean securityProtectedTarget(Entity target) {
        if (target instanceof EntityTameable) {
            return ((EntityTameable) target).getOwner() != null;
        } else if (target instanceof EntityDrone) {
            return ((EntityDrone) target).getOwner().getUniqueID().equals(this.getPlayer().getUniqueID());
        } else {
            return target instanceof EntityPlayer;
        }
    }

    public void update(double posX, double posY, double posZ) {
        this.setOldMinigunRotation(this.getMinigunRotation());
        this.oldMinigunYaw = this.minigunYaw;
        this.oldMinigunPitch = this.minigunPitch;
        if (this.attackTarget != null && this.attackTarget.isDead) this.attackTarget = null;
        if (!this.world.isRemote) {
            this.setMinigunActivated(this.getMinigunTriggerTimeOut() > 0);

            this.setAmmoColorStack(this.ammoStack);

            if (this.getMinigunTriggerTimeOut() > 0) {
                this.setMinigunTriggerTimeOut(this.getMinigunTriggerTimeOut() - 1);
                if (this.getMinigunSpeed() == 0) {
                    this.playSound(Sounds.HUD_INIT, 3, 0.9F);
                }
            }
            if (this.getMinigunSoundCounter() == 0 && this.getMinigunTriggerTimeOut() == 0) {
                this.playSound(Sounds.MINIGUN_STOP, 3, 0.5F);
                this.setMinigunSoundCounter(-1);
            }
        }
        if (this.isMinigunActivated()) {
            double speedBonus = this.getUpgrades(EnumUpgrade.SPEED) * 0.0033D;
            double lastSpeed = this.getMinigunSpeed();
            this.setMinigunSpeed(Math.min(this.getMinigunSpeed() + 0.01D + speedBonus, MAX_GUN_SPEED));
            if (this.getMinigunSpeed() > lastSpeed && this.getMinigunSpeed() >= MAX_GUN_SPEED && !this.world.isRemote) {
                NetworkHandler.sendToDimension(new PacketPlayMovingSound(MovingSounds.Sound.MINIGUN, this.getSoundSource()), this.world.provider.getDimension());
            }
        } else {
            this.setMinigunSpeed(Math.max(0, this.getMinigunSpeed() - 0.003D));
        }

        this.setMinigunRotation(this.getMinigunRotation() + this.getMinigunSpeed());

        double targetYaw;
        double targetPitch = 0;
        if (this.attackTarget != null) {
            double deltaX = posX - this.attackTarget.posX;
            double deltaZ = posZ - this.attackTarget.posZ;

            if (deltaX >= 0 && deltaZ < 0) {
                targetYaw = Math.atan(Math.abs(deltaX / deltaZ)) / Math.PI * 180D;
            } else if (deltaX >= 0 && deltaZ >= 0) {
                targetYaw = Math.atan(Math.abs(deltaZ / deltaX)) / Math.PI * 180D + 90;
            } else if (deltaX < 0 && deltaZ >= 0) {
                targetYaw = Math.atan(Math.abs(deltaX / deltaZ)) / Math.PI * 180D + 180;
            } else {
                targetYaw = Math.atan(Math.abs(deltaZ / deltaX)) / Math.PI * 180D + 270;
            }
            if (targetYaw - this.minigunYaw > 180) {
                targetYaw -= 360;
            } else if (this.minigunYaw - targetYaw > 180) {
                targetYaw += 360;
            }
            targetPitch = Math.toDegrees(Math.atan((posY - this.attackTarget.posY - this.attackTarget.height / 2) / PneumaticCraftUtils.distBetween(posX, posZ, this.attackTarget.posX, this.attackTarget.posZ)));

            this.minigunPitch = this.moveToward(this.minigunPitch, targetPitch, MAX_GUN_PITCH_CHANGE);
            this.minigunYaw = this.minigunPitch < -80 || this.minigunPitch > 80 ? targetYaw : this.moveToward(this.minigunYaw, targetYaw, MAX_GUN_YAW_CHANGE);
            this.gunAimedAtTarget = this.minigunYaw == targetYaw && this.minigunPitch == targetPitch;
        } else if (this.isSweeping()) {
            this.minigunYaw -= Math.cos(this.sweepingProgress) * 22;
            this.sweepingProgress += 0.05D;
            this.minigunYaw += Math.cos(this.sweepingProgress) * 22;

            this.minigunPitch = this.moveToward(this.minigunPitch, targetPitch, MAX_GUN_PITCH_CHANGE);
        }

        if (!this.world.isRemote && this.isMinigunActivated() && this.getMinigunSpeed() == MAX_GUN_SPEED
                && (!this.requiresTarget || this.gunAimedAtTarget && this.attackTarget != null)) {
            if (this.getMinigunSoundCounter() <= 0) {
                this.setMinigunSoundCounter(20);
            }
        }
        if (this.getMinigunSoundCounter() > 0) this.setMinigunSoundCounter(this.getMinigunSoundCounter() - 1);
    }

    private double moveToward(double val, double target, double amount) {
        if (val > target) {
            val = Math.max(val - amount, target);
        } else {
            val = Math.min(val + amount, target);
        }
        return val;
    }

    @SideOnly(Side.CLIENT)
    public void render(double x, double y, double z, double gunRadius) {
        if (this.isMinigunActivated() && this.getMinigunSpeed() == MAX_GUN_SPEED && this.gunAimedAtTarget && this.attackTarget != null) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(1, 1, 1);
            GlStateManager.translate(-x, -y, -z);
            GlStateManager.disableTexture2D();
            GL11.glEnable(GL11.GL_LINE_STIPPLE);
            RenderUtils.glColorHex(0xFF000000 | this.getAmmoColor());
            for (int i = 0; i < 5; i++) {
                int stipple = 0xFFFF & ~(2 << this.rand.nextInt(16));
                GL11.glLineStipple(4, (short) stipple);
                Vec3d vec = new Vec3d(this.attackTarget.posX - x, this.attackTarget.posY - y, this.attackTarget.posZ - z).normalize();
                this.minigunFire.startX = x + vec.x * gunRadius;
                this.minigunFire.startY = y + vec.y * gunRadius;
                this.minigunFire.startZ = z + vec.z * gunRadius;
                this.minigunFire.endX = this.attackTarget.posX + this.rand.nextDouble() - 0.5;
                this.minigunFire.endY = this.attackTarget.posY + this.attackTarget.height / 2 + this.rand.nextDouble() - 0.5;
                this.minigunFire.endZ = this.attackTarget.posZ + this.rand.nextDouble() - 0.5;
                this.minigunFire.render();
            }
            GlStateManager.color(1, 1, 1, 1);
            GL11.glDisable(GL11.GL_LINE_STIPPLE);
            GlStateManager.enableTexture2D();
            GlStateManager.popMatrix();
        }
    }

    public int getUpgrades(EnumUpgrade upgrade) {
        return 0;
    }

    public double getRange() {
        double mul = this.getAmmoStack().getItem() instanceof ItemGunAmmo ? ((ItemGunAmmo) this.ammoStack.getItem()).getRangeMultiplier(this.ammoStack) : 1;
        return (ConfigHandler.minigun.baseRange + 5 * this.getUpgrades(EnumUpgrade.RANGE)) * mul;
    }

    public boolean dispenserWeightedPercentage(int basePct) {
        return this.dispenserWeightedPercentage(basePct, 0.1f);
    }

    public boolean dispenserWeightedPercentage(int basePct, float dispenserWeight) {
        return this.getWorld().rand.nextInt(100) < basePct * (1 + this.getUpgrades(EnumUpgrade.DISPENSER) * dispenserWeight);
    }
}
