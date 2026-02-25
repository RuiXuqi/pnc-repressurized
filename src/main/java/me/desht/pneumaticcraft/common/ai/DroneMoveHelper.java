package me.desht.pneumaticcraft.common.ai;

import me.desht.pneumaticcraft.common.entity.living.EntityDroneBase;
import net.minecraft.entity.ai.EntityMoveHelper;

public class DroneMoveHelper extends EntityMoveHelper {
    private final EntityDroneBase entity;
    private double x, y, z, speed;
    private int timeoutTimer;
    private int timeoutCounter;//counts the times the drone timed out.

    public DroneMoveHelper(EntityDroneBase par1EntityLiving) {
        super(par1EntityLiving);
        this.entity = par1EntityLiving;
        this.x = this.entity.posX;
        this.y = this.entity.posY;
        this.z = this.entity.posZ;
    }

    @Override
    public void setMoveTo(double x, double y, double z, double speed) {
        double newY = y + 0.5 - 0.17;
        if (x != this.x || newY != this.y || z != this.z) {
            this.x = x;
            this.y = newY;
            this.z = z;
            this.timeoutTimer = 0;
        } else {
            this.timeoutCounter = 0;
        }
        this.speed = speed;
    }

    @Override
    public void read(EntityMoveHelper that) {
        //NOOP
    }

    @Override
    public void onUpdateMoveHelper() {
        if (this.entity.isAccelerating()) {
            this.entity.motionX = Math.max(-this.speed, Math.min(this.speed, this.x - this.entity.posX));
            this.entity.motionY = Math.max(-this.speed, Math.min(this.speed, this.y - this.entity.posY));
            this.entity.motionZ = Math.max(-this.speed, Math.min(this.speed, this.z - this.entity.posZ));

            EntityPathNavigateDrone navigator = (EntityPathNavigateDrone) this.entity.getNavigator();

            //When teleporting already, the drone stands still for a bit, so don't expect movement in this case.
            if (!navigator.isGoingToTeleport() && this.timeoutTimer++ > 40) {
                this.entity.getNavigator().clearPath();
                this.timeoutTimer = 0;
                this.timeoutCounter++;
                if (this.timeoutCounter > 1 && this.entity.hasPath()) { //Teleport when after re-acquiring a new path, the drone still doesn't move.
                    navigator.teleport();
                }
            }
        }
    }

}
