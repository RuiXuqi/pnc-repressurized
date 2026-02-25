package me.desht.pneumaticcraft.common.ai;

import me.desht.pneumaticcraft.api.drone.IPathNavigator;
import me.desht.pneumaticcraft.common.entity.living.EntityDrone;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketPlaySound;
import me.desht.pneumaticcraft.common.network.PacketSpawnParticle;
import me.desht.pneumaticcraft.lib.Sounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.init.SoundEvents;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.pathfinding.PathNavigateFlying;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Random;

public class EntityPathNavigateDrone extends PathNavigateFlying implements IPathNavigator {

    private final EntityDrone pathfindingEntity;
    public boolean pathThroughLiquid;
    private boolean forceTeleport;
    private int teleportCounter = -1;
    private BlockPos telPos;
    private static final int TELEPORT_TICKS = 120;

    public EntityPathNavigateDrone(EntityDrone pathfindingEntity, World par2World) {
        super(pathfindingEntity, par2World);
        this.pathfindingEntity = pathfindingEntity;
    }

    @Override
    public boolean tryMoveToEntityLiving(Entity p_75497_1_, double p_75497_2_) {
        return super.tryMoveToEntityLiving(p_75497_1_, p_75497_2_) || this.isGoingToTeleport();
    }

    /**
     * Returns the path to the given EntityLiving
     */
    @Override
    public Path getPathToEntityLiving(Entity par1Entity) {
        BlockPos pos = new BlockPos(par1Entity.posX, par1Entity.getEntityBoundingBox().minY, par1Entity.posZ);

        if ((par1Entity instanceof EntityItem && !this.pathfindingEntity.isBlockValidPathfindBlock(pos)) || par1Entity instanceof EntityMinecart) {
            // items can end up with a blockpos of the ground they're sitting on,
            // which will prevent the drone pathfinding to them
            // minecarts apparently prevent the drone moving to the same blockpos
            if (this.pathfindingEntity.isBlockValidPathfindBlock(pos.up())) {
                pos = pos.up();
            }
        }
        return this.getPathToPos(pos);
    }

    public void setForceTeleport(boolean forceTeleport) {
        this.forceTeleport = forceTeleport;
    }


    @Nullable
    @Override
    public Path getPathToPos(BlockPos pos) {
        // When the destination is not a valid block, we can stop right away
        if (!this.pathfindingEntity.isBlockValidPathfindBlock(pos))
            return null;

        // 0.75 is the squared dist from a block corner to its center (0.5^2 + 0.5^2 + 0.5^2)
        if (this.pathfindingEntity.getDistanceSqToCenter(pos) < 0.75) {
            return new Path(new PathPoint[]{
                    new PathPoint(pos.getX(), pos.getY(), pos.getZ())
            });
        }

        //Store the potential teleport destination
        this.telPos = pos;

        //If we are forced to teleport, trigger right away
        if (this.forceTeleport) {
            this.teleportCounter = 0;
            return null;
        }

        this.pathfindingEntity.setStandby(false);
        Path path = super.getPathToPos(pos);

        // Only paths that actually end up where we want to are valid, not just halfway.
        if (path != null) {
            PathPoint lastPoint = path.getFinalPathPoint();
            if (lastPoint != null && (lastPoint.x != pos.getX() || lastPoint.y != pos.getY() || lastPoint.z != pos.getZ())) {
                path = null;
            }
        }

        if (path == null) {
            // No valid flight path: teleport instead, but don't reset the teleport counter if it's already in progress
            if (this.teleportCounter == -1) this.teleportCounter = 0;
        } else {
            // Valid path: cancel any teleport in-progress
            this.teleportCounter = -1;
        }

        return path;
    }

    @Override
    public float getPathSearchRange() {
        return (float) this.pathfindingEntity.getRange();
    }

    @Override
    public boolean isGoingToTeleport() {
        return this.teleportCounter >= 0;
    }

    @Override
    public boolean noPath() {
        return super.noPath() && !this.isGoingToTeleport();
    }

    @Override
    public void onUpdateNavigation() {
        if (this.isGoingToTeleport()) {
            if (this.teleportCounter == 0 || this.teleportCounter == 60) {
                NetworkHandler.sendToAllAround(new PacketPlaySound(Sounds.HUD_INIT, SoundCategory.PLAYERS, this.pathfindingEntity.posX, this.pathfindingEntity.posY, this.pathfindingEntity.posZ, 0.1F, this.teleportCounter == 0 ? 0.7F : 1F, true), this.pathfindingEntity.world);
            }

            if (this.teleportCounter < TELEPORT_TICKS - 40) {
                Random rand = this.pathfindingEntity.getRNG();
                float f = (rand.nextFloat() - 0.5F) * 0.02F * this.teleportCounter;
                float f1 = (rand.nextFloat() - 0.5F) * 0.02F * this.teleportCounter;
                float f2 = (rand.nextFloat() - 0.5F) * 0.02F * this.teleportCounter;
                NetworkHandler.sendToAllAround(new PacketSpawnParticle(EnumParticleTypes.PORTAL, this.pathfindingEntity.posX, this.pathfindingEntity.posY, this.pathfindingEntity.posZ, f, f1, f2), this.pathfindingEntity.world);
            }

            if (++this.teleportCounter > TELEPORT_TICKS) {
                if (this.pathfindingEntity.isBlockValidPathfindBlock(this.telPos)) {
                    this.teleport();
                }
                this.teleportCounter = -1;
                this.setPath(null, 0);
                this.pathfindingEntity.getMoveHelper().setMoveTo(this.telPos.getX(), this.telPos.getY(), this.telPos.getZ(), this.pathfindingEntity.getSpeed());
                this.pathfindingEntity.addAir(null, -10000);
            }
        } else {
            // super.onUpdateNavigation();
            if (!this.noPath()) {
                this.pathFollow();

                if (!this.noPath()) {
                    Vec3d vec32 = this.currentPath.getPosition(this.entity);

                    if (vec32 != null) {
                        this.entity.getMoveHelper().setMoveTo(vec32.x, vec32.y, vec32.z, this.speed);
                    }
                }
            }
        }
    }

    public void teleport() {

        Random rand = this.pathfindingEntity.getRNG();
        double width = this.pathfindingEntity.width;
        double height = this.pathfindingEntity.height;

        short short1 = 128;

        for (int l = 0; l < short1; ++l) {
            double d6 = l / (short1 - 1.0D);
            float f = (rand.nextFloat() - 0.5F) * 0.2F;
            float f1 = (rand.nextFloat() - 0.5F) * 0.2F;
            float f2 = (rand.nextFloat() - 0.5F) * 0.2F;
            double d7 = this.pathfindingEntity.posX + (this.telPos.getX() + 0.5 - this.pathfindingEntity.posX) * d6 + (rand.nextDouble() - 0.5D) * width * 2.0D;
            double d8 = this.pathfindingEntity.posY + (this.telPos.getY() - this.pathfindingEntity.posY) * d6 + rand.nextDouble() * height;
            double d9 = this.pathfindingEntity.posZ + (this.telPos.getZ() + 0.5 - this.pathfindingEntity.posZ) * d6 + (rand.nextDouble() - 0.5D) * width * 2.0D;
            NetworkHandler.sendToAllAround(new PacketSpawnParticle(EnumParticleTypes.PORTAL, d7, d8, d9, f, f1, f2), this.pathfindingEntity.world);
        }

        this.pathfindingEntity.playSound(SoundEvents.ENTITY_ENDERMEN_TELEPORT, 1.0F, 1.0F);
        this.pathfindingEntity.setPosition(this.telPos.getX() + 0.5, this.telPos.getY() + 0.5, this.telPos.getZ() + 0.5);
    }

    @Override
    public boolean moveToXYZ(double x, double y, double z) {
        boolean success = this.tryMoveToXYZ(x, y, z, this.pathfindingEntity.getSpeed());
        if (success) this.forceRidingEntityPaths();
        return success;
    }

    @Override
    public boolean moveToEntity(Entity entity) {
        boolean success = this.tryMoveToEntityLiving(entity, this.pathfindingEntity.getSpeed());
        if (success) this.forceRidingEntityPaths();
        return success;
    }

    /**
     * Override to prevent {@link net.minecraft.entity.EntityLiving#updateEntityActionState()} to assign a path with a higher speed.
     */
    @Override
    public boolean setPath(Path pathentityIn, double speedIn) {
        return super.setPath(pathentityIn, this.pathfindingEntity.getSpeed());
    }

    /**
     * Hack to prevent riding entities to override the Drone's path (instead they will assign the Drone's path)
     */
    private void forceRidingEntityPaths() {
        for (Entity ridingEntity : this.pathfindingEntity.getPassengers()) {
            if (ridingEntity instanceof EntityLiving) {
                EntityLiving ridingLiving = (EntityLiving) ridingEntity;
                ridingLiving.getNavigator().setPath(this.pathfindingEntity.getNavigator().getPath(), this.pathfindingEntity.getSpeed());
            }
        }
    }

    @Override
    public boolean hasNoPath() {
        return this.noPath();
    }

    @Override
    public boolean isDirectPathBetweenPoints(Vec3d p_75493_1_, Vec3d p_75493_2_, int p_75493_3_, int p_75493_4_, int p_75493_5_) {
        return false;
    }

    @Override
    protected PathFinder getPathFinder() {
        this.nodeProcessor = new NodeProcessorDrone();
        return new PathfinderDrone(this.nodeProcessor);
    }

    @Override
    protected Vec3d getEntityPosition() {
        return this.pathfindingEntity.getDronePos();
    }

    @Override
    protected boolean canNavigate() {
        return true;
    }
}
