package me.desht.pneumaticcraft.common.entity.projectile;

import me.desht.pneumaticcraft.PneumaticCraftRepressurized;
import me.desht.pneumaticcraft.common.config.ConfigHandler;
import me.desht.pneumaticcraft.common.entity.living.EntityDrone;
import me.desht.pneumaticcraft.common.item.ItemMicromissiles;
import me.desht.pneumaticcraft.common.item.ItemMicromissiles.FireMode;
import me.desht.pneumaticcraft.common.util.EntityFilter;
import me.desht.pneumaticcraft.lib.EnumCustomParticleType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Comparator;
import java.util.List;

public class EntityMicromissile extends EntityThrowable {
    private static final double SEEK_RANGE = 24;

    private static final DataParameter<Integer> TARGET_ID = EntityDataManager.createKey(EntityMicromissile.class, DataSerializers.VARINT);
    private static final DataParameter<Float> MAX_VEL_SQ = EntityDataManager.createKey(EntityMicromissile.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> ACCEL = EntityDataManager.createKey(EntityMicromissile.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> TURN_SPEED = EntityDataManager.createKey(EntityMicromissile.class, DataSerializers.FLOAT);

    private Entity targetEntity = null;

    private float maxVelocitySq = 0.5f;
    private float accel = 1.05f; // straight line acceleration
    private float turnSpeed = 0.1f;
    private float explosionPower = 2f;
    private EntityFilter entityFilter;
    private boolean outOfFuel = false;
    private FireMode fireMode = FireMode.SMART;

    public EntityMicromissile(World worldIn) {
        super(worldIn);
    }

    public EntityMicromissile(World worldIn, EntityLivingBase thrower, ItemStack iStack) {
        super(worldIn, thrower);

        if (iStack.hasTagCompound()) {
            NBTTagCompound tag = iStack.getTagCompound();
            this.entityFilter = EntityFilter.fromString(tag.getString(ItemMicromissiles.NBT_FILTER));
            this.fireMode = FireMode.fromString(tag.getString(ItemMicromissiles.NBT_FIRE_MODE));
            switch (this.fireMode) {
                case SMART:
                    this.accel = Math.max(1.02f, 1.0f + tag.getFloat(ItemMicromissiles.NBT_TOP_SPEED) / 10f);
                    this.maxVelocitySq = (float) Math.pow(0.25 + tag.getFloat(ItemMicromissiles.NBT_TOP_SPEED) * 3.75f, 2);
                    this.turnSpeed = 0.4f * tag.getFloat(ItemMicromissiles.NBT_TURN_SPEED);
                    this.explosionPower = Math.max(1f, 5 * tag.getFloat(ItemMicromissiles.NBT_DAMAGE));
                    break;
                case DUMB:
                    this.accel = 1.02f;
                    this.maxVelocitySq = 2f;
                    this.turnSpeed = 0f;
                    this.explosionPower = 3f;
                    break;
            }
        }
    }

    public EntityMicromissile(World worldIn, double x, double y, double z) {
        this(worldIn);
        this.setPosition(x, y, z);
    }

    @Override
    protected void entityInit() {
        super.entityInit();

        this.dataManager.register(TARGET_ID, 0);
        this.dataManager.register(MAX_VEL_SQ, 0.5f);
        this.dataManager.register(ACCEL, 1.05f);
        this.dataManager.register(TURN_SPEED, 0.4f);
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        if (this.getEntityWorld().isRemote) {
            if (key.equals(MAX_VEL_SQ)) {
                this.maxVelocitySq = this.dataManager.get(MAX_VEL_SQ);
            } else if (key.equals(TARGET_ID)) {
                int id = this.dataManager.get(TARGET_ID);
                this.targetEntity = id > 0 ? this.getEntityWorld().getEntityByID(this.dataManager.get(TARGET_ID)) : null;
            } else if (key.equals(ACCEL)) {
                this.accel = this.dataManager.get(ACCEL);
            } else if (key.equals(TURN_SPEED)) {
                this.turnSpeed = this.dataManager.get(TURN_SPEED);
            }
        }
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (this.ticksExisted == 1) {
            if (this.getEntityWorld().isRemote) {
                this.getEntityWorld().playSound(this.posX, this.posY, this.posZ, SoundEvents.ENTITY_FIREWORK_LAUNCH, SoundCategory.PLAYERS, 1.0f, 0.8f, true);
            } else {
                this.dataManager.set(MAX_VEL_SQ, this.maxVelocitySq);
                this.dataManager.set(ACCEL, this.accel);
                this.dataManager.set(TURN_SPEED, this.turnSpeed);
            }
        }

        if (this.ticksExisted > ConfigHandler.microMissile.lifetime) {
            this.outOfFuel = true;
        }

        if (!this.outOfFuel) {
            // negate default slowdown of projectiles applied in superclass
            if (this.isInWater()) {
                this.motionX *= 1.25;
                this.motionY *= 1.25;
                this.motionZ *= 1.25;
            } else {
                this.motionX *= 1 / 0.99;
                this.motionY *= 1 / 0.99;
                this.motionZ *= 1 / 0.99;
            }

            if ((this.targetEntity == null || this.targetEntity.isDead) && this.fireMode == FireMode.SMART && !this.getEntityWorld().isRemote && (this.ticksExisted & 0x3) == 0) {
                this.targetEntity = this.tryFindNewTarget();
            }

            if (this.targetEntity != null) {
                // turn toward the target
                Vec3d diff = this.targetEntity.getPositionVector().add(0, this.targetEntity.getEyeHeight(), 0).subtract(this.getPositionVector()).normalize().scale(this.turnSpeed);
                this.motionX += diff.x;
                this.motionY += diff.y;
                this.motionZ += diff.z;
            }

            // accelerate up to max velocity but cap there
            double velSq = this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ;
            double mul = velSq > this.maxVelocitySq ? this.maxVelocitySq / velSq : this.accel;
            this.motionX *= mul;
            this.motionY *= mul;
            this.motionZ *= mul;

            if (this.getEntityWorld().isRemote) {
                PneumaticCraftRepressurized.proxy.playCustomParticle(EnumCustomParticleType.AIR_PARTICLE_DENSE, this.getEntityWorld(), this.posX, this.posY, this.posZ, -this.motionX / 2, -this.motionY / 2, -this.motionZ / 2);
            }
        }
    }

    private Entity tryFindNewTarget() {
        AxisAlignedBB aabb = new AxisAlignedBB(this.posX, this.posY, this.posZ, this.posX, this.posY, this.posZ).grow(SEEK_RANGE);
        List<Entity> l = this.getEntityWorld().getEntitiesWithinAABB(EntityLivingBase.class, aabb, EntitySelectors.IS_ALIVE);
        l.sort(new TargetSorter());
        Entity tgt = null;
        // find the closest entity which matches this missile's entity filter
        for (Entity e : l) {
            if (this.isValidTarget(e) && e.getDistanceSq(this) < SEEK_RANGE * SEEK_RANGE) {
                RayTraceResult res = this.getEntityWorld().rayTraceBlocks(this.getPositionVector(), e.getPositionVector().add(0, e.getEyeHeight(), 0), false, false, true);
                if (res == null || res.typeOfHit == RayTraceResult.Type.MISS || res.typeOfHit == RayTraceResult.Type.ENTITY) {
                    tgt = e;
                    break;
                }
            }
        }
        this.dataManager.set(TARGET_ID, tgt == null ? 0 : tgt.getEntityId());
        return tgt;
    }

    public boolean isValidTarget(Entity e) {
        // never target the player who fired the missile or any of their pets/drones
        if (this.thrower != null) {
            if (e.equals(this.thrower)
                    || e instanceof EntityTameable && this.thrower.equals(((EntityTameable) e).getOwner())
                    || e instanceof EntityDrone && this.thrower.getUniqueID().toString().equals(((EntityDrone) e).getOwnerUUID())
                    || e instanceof EntityHorse && this.thrower.getUniqueID().equals(((EntityHorse) e).getOwnerUniqueId())) {
                return false;
            }
        }

        if (this.entityFilter != null && !this.entityFilter.test(e)) {
            return false;
        }

        return e instanceof EntityLivingBase || e instanceof EntityBoat || e instanceof EntityMinecart;
    }

    @Override
    protected void onImpact(RayTraceResult result) {
        if (this.ticksExisted > 5 && !this.getEntityWorld().isRemote && !this.isDead) {
            this.explode();
        }
    }

    private void explode() {
        this.setDead();
        this.getEntityWorld().createExplosion(this, this.posX, this.posY, this.posZ, ConfigHandler.microMissile.baseExplosionDamage * this.explosionPower, ConfigHandler.microMissile.damageTerrain);
    }

    @Override
    public void shoot(Entity entityThrower, float pitch, float yaw, float pitchOffset, float velocity, float inaccuracy) {
        float x = -MathHelper.sin(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F);
        float y = -MathHelper.sin(pitch * 0.017453292F);
        float z = MathHelper.cos(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F);
        this.shoot(x, y, z, velocity, 0f);
        this.motionX += entityThrower.motionX;
        this.motionZ += entityThrower.motionZ;
    }

    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        double f = Math.sqrt(x * x + y * y + z * z);
        x = x / f * velocity;
        y = y / f * velocity;
        z = z / f * velocity;
        this.motionX = x;
        this.motionY = y;
        this.motionZ = z;

        float f1 = MathHelper.sqrt(x * x + z * z);
        this.rotationYaw = (float) (MathHelper.atan2(x, z) * (180D / Math.PI));
        this.rotationPitch = (float) (MathHelper.atan2(y, f1) * (180D / Math.PI));
        this.prevRotationYaw = this.rotationYaw;
        this.prevRotationPitch = this.rotationPitch;
    }

    @Override
    protected float getGravityVelocity() {
        return this.outOfFuel ? super.getGravityVelocity() : 0f;
    }

    @Override
    public boolean hasNoGravity() {
        return !this.outOfFuel;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setFloat("turnSpeed", this.turnSpeed);
        compound.setFloat("explosionScaling", this.explosionPower);
        compound.setFloat("topSpeedSq", this.maxVelocitySq);
        compound.setString("filter", this.entityFilter.toString());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        this.turnSpeed = compound.getFloat("turnSpeed");
        this.explosionPower = compound.getFloat("explosionScaling");
        this.maxVelocitySq = compound.getFloat("topSpeedSq");
        this.entityFilter = EntityFilter.fromString(compound.getString("filter"));
    }

    public void setTarget(Entity target) {
        this.targetEntity = target;
    }

    private class TargetSorter implements Comparator<Entity> {
        private final Vec3d vec;

        TargetSorter() {
            this.vec = new Vec3d(EntityMicromissile.this.posX, EntityMicromissile.this.posY, EntityMicromissile.this.posZ);
        }

        @Override
        public int compare(Entity e1, Entity e2) {
            return Double.compare(this.vec.squareDistanceTo(e1.getPositionVector()), this.vec.squareDistanceTo(e2.getPositionVector()));
        }
    }
}
