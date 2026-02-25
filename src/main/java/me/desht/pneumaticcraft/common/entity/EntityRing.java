package me.desht.pneumaticcraft.common.entity;

import me.desht.pneumaticcraft.client.render.RenderRing;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class EntityRing extends Entity {

    public RenderRing ring, oldRing;
    private final Entity targetEntity;
    public final int color;

    public EntityRing(World world) {
        this(world, 0, 0, 0, null, 0);
    }

    public EntityRing(World par1World, double startX, double startY, double startZ, Entity targetEntity, int color) {
        super(par1World);
        this.posX = this.lastTickPosX = startX;
        this.posY = this.lastTickPosY = startY;
        this.posZ = this.lastTickPosZ = startZ;
        this.targetEntity = targetEntity;
        this.color = color;

        double dx = targetEntity.posX - this.posX;
        double dy = targetEntity.posY - this.posY;
        double dz = targetEntity.posZ - this.posZ;
        float f = MathHelper.sqrt(dx * dx + dz * dz);
        this.prevRotationYaw = this.rotationYaw = (float) (Math.atan2(dx, dz) * 180.0D / Math.PI);
        this.prevRotationPitch = this.rotationPitch = (float) (Math.atan2(dy, f) * 180.0D / Math.PI);
//        renderDistanceWeight = 10.0D;
        this.ignoreFrustumCheck = true;
        if (par1World.isRemote) {
            setRenderDistanceWeight(10.0D);
        }
    }

    @Override
    public void onUpdate() {
        if (this.targetEntity == null) return;

        double endX = this.targetEntity.posX;
        double endY = this.targetEntity.posY;
        double endZ = this.targetEntity.posZ;
        this.prevRotationYaw = this.rotationYaw;
        this.prevRotationPitch = this.rotationPitch;

        if (this.ring == null) {
            this.ring = new RenderRing(this.posX, this.posY, this.posZ, endX, endY, endZ, this.color);
        } else {
            if (this.oldRing == null) {
                this.oldRing = new RenderRing(this.ring.startX, this.ring.startY, this.ring.startZ, this.ring.endX, this.ring.endY, this.ring.endZ, this.color);
            } else {
                this.oldRing.endX = this.ring.endX;
                this.oldRing.endY = this.ring.endY;
                this.oldRing.endZ = this.ring.endZ;
            }
            this.ring.endX = endX;
            this.ring.endY = endY;
            this.ring.endZ = endZ;

            double dx = endX - this.posX;
            double dy = endY - this.posY;
            double dz = endZ - this.posZ;
            float f = MathHelper.sqrt(dx * dx + dz * dz);
            this.rotationYaw = (float) (Math.atan2(dx, dz) * 180.0D / Math.PI);
            this.rotationPitch = (float) (Math.atan2(dy, f) * 180.0D / Math.PI);

            this.oldRing.setProgress(this.ring.getProgress());
            if (this.ring.incProgress(0.05F)) {
                this.setDead();
            }
        }
    }

    @Override
    protected void entityInit() {
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound var1) {
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound var1) {
    }

}
