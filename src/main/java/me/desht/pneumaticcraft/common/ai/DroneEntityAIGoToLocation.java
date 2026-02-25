package me.desht.pneumaticcraft.common.ai;

import me.desht.pneumaticcraft.common.progwidgets.IAreaProvider;
import me.desht.pneumaticcraft.common.progwidgets.IGotoWidget;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidget;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DroneEntityAIGoToLocation extends EntityAIBase {
    protected final IDroneBase drone;
    private final ProgWidget gotoWidget;
    private final ChunkPositionSorter positionSorter;
    private final List<BlockPos> validArea;

    public DroneEntityAIGoToLocation(IDroneBase drone, ProgWidget gotoWidget) {
        this.drone = drone;
        this.setMutexBits(63);//binary 111111, so it won't run along with other AI tasks.
        this.gotoWidget = gotoWidget;
        Set<BlockPos> set = new HashSet<>();
        ((IAreaProvider) gotoWidget).getArea(set);
        this.validArea = new ArrayList<>(set);
        this.positionSorter = new ChunkPositionSorter(drone);
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    @Override
    public boolean shouldExecute() {
        this.validArea.sort(this.positionSorter);
        for (BlockPos c : this.validArea) {
            // 0.75 is the squared dist from a block corner to its center (0.5^2 + 0.5^2 + 0.5^2)
            if (this.drone.getDronePos().squareDistanceTo(new Vec3d(c.getX() + 0.5, c.getY() + 0.5, c.getZ() + 0.5)) < 0.75)
                return false;
            if (this.drone.getPathNavigator().moveToXYZ(c.getX(), c.getY(), c.getZ())) {
                return !((IGotoWidget) this.gotoWidget).doneWhenDeparting();
            }
        }
        boolean teleport = this.drone.getPathNavigator().isGoingToTeleport();
        if (teleport) {
            return true;
        } else {
            for (BlockPos c : this.validArea) {
                this.drone.addDebugEntry("gui.progWidget.goto.debug.cantNavigate", c);
            }
            return false;
        }
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    @Override
    public boolean shouldContinueExecuting() {
        return !this.drone.getPathNavigator().hasNoPath();
    }
}
