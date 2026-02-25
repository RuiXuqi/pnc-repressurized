package me.desht.pneumaticcraft.common.thirdparty.computercraft;

import me.desht.pneumaticcraft.common.entity.living.EntityDrone;
import me.desht.pneumaticcraft.common.progwidgets.IProgWidget;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import java.util.Set;

class DroneAICC extends EntityAIBase {
    private final EntityDrone drone;
    private final ProgWidgetCC widget;
    private EntityAIBase curAction;
    private boolean curActionActive;
    private final TileEntityDroneInterface droneInterface;
    private boolean newAction;

    DroneAICC(EntityDrone drone, ProgWidgetCC widget, boolean targetAI) {
        this.drone = drone;
        this.widget = widget;
        Set<BlockPos> area = widget.getInterfaceArea();
        for (BlockPos pos : area) {
            TileEntity te = drone.world.getTileEntity(pos);
            if (te instanceof TileEntityDroneInterface) {
                TileEntityDroneInterface inter = (TileEntityDroneInterface) te;
                if (targetAI) {
                    if (inter.getDrone() == drone) {
                        this.droneInterface = inter;
                        return;
                    }
                } else {
                    if (inter.getDrone() == null) {
                        this.droneInterface = inter;
                        this.droneInterface.setDrone(drone);
                        return;
                    }
                }
            }
        }
        this.droneInterface = null;
    }

    public ProgWidgetCC getWidget() {
        return this.widget;
    }

    @Override
    public synchronized boolean shouldExecute() {
        this.newAction = false;
        if (this.curAction != null) {
            this.curActionActive = this.curAction.shouldExecute();
            if (this.curActionActive) this.curAction.startExecuting();
        }
        return this.droneInterface != null && !this.droneInterface.isInvalid() && this.droneInterface.getDrone() == this.drone;
    }

    @Override
    public synchronized boolean shouldContinueExecuting() {
        if (!this.newAction && this.curActionActive && this.curAction != null) {
            boolean shouldContinue = this.curAction.shouldContinueExecuting();
            if (!shouldContinue) this.curAction.resetTask();
            return shouldContinue;
        } else {
            return false;
        }
    }

    @Override
    public synchronized void updateTask() {
        if (this.curActionActive && this.curAction != null) this.curAction.updateTask();
    }

    synchronized void setAction(IProgWidget widget, EntityAIBase ai) throws IllegalArgumentException {
        this.curAction = ai;
        this.newAction = true;
        this.curActionActive = true;
    }

    synchronized void abortAction() {
        this.curAction = null;
    }

    synchronized boolean isActionDone() {
        if (this.curAction == null) throw new IllegalStateException("There's no action active!");
        return !this.curActionActive;
    }
}
