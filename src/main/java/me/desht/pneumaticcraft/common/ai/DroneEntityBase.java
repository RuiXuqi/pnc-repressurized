package me.desht.pneumaticcraft.common.ai;

import me.desht.pneumaticcraft.common.progwidgets.IEntityProvider;
import me.desht.pneumaticcraft.common.progwidgets.IProgWidget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public abstract class DroneEntityBase<Widget extends IProgWidget, E extends Entity> extends EntityAIBase {
    protected final IDroneBase drone;
    protected final Widget widget;
    protected E targetedEntity;

    public DroneEntityBase(IDroneBase drone, Widget widget) {
        this.drone = drone;
        this.setMutexBits(63);//binary 111111, so it won't run along with other AI tasks.
        this.widget = widget;
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    @Override
    public boolean shouldExecute() {
        List<Entity> pickableItems = ((IEntityProvider) this.widget).getValidEntities(this.drone.world());

        pickableItems.sort(new DistanceEntitySorter(this.drone));
        for (Entity ent : pickableItems) {
            if (ent != this.drone && this.isEntityValid(ent)) {
                if (this.drone.getPathNavigator().moveToEntity(ent)) {
                    this.targetedEntity = (E) ent;
                    return true;
                }
            }
        }
        return false; // 

    }

    protected abstract boolean isEntityValid(Entity entity);

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    @Override
    public boolean shouldContinueExecuting() {
        if (this.targetedEntity.isDead) return false;
        if (new Vec3d(this.targetedEntity.posX, this.targetedEntity.posY, this.targetedEntity.posZ).squareDistanceTo(this.drone.getDronePos()) < 2.25) {
            return this.doAction();
        }
        return !this.drone.getPathNavigator().hasNoPath();
    }

    protected abstract boolean doAction();
}
