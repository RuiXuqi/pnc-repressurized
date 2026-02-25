package me.desht.pneumaticcraft.common.ai;

import me.desht.pneumaticcraft.common.progwidgets.ICondition;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetAreaItemBase;
import net.minecraft.util.math.BlockPos;

public abstract class DroneAIBlockCondition extends DroneAIBlockInteraction {

    private boolean result;

    public DroneAIBlockCondition(IDroneBase drone, ProgWidgetAreaItemBase widget) {
        super(drone, widget);
    }

    @Override
    public boolean shouldExecute() {
        if (super.shouldExecute()) {
            this.result = ((ICondition) this.widget).isAndFunction();//set the initial value, so it can be modified by the 'evaluate' method later.
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected boolean isValidPosition(BlockPos pos) {
        if (this.evaluate(pos) != ((ICondition) this.widget).isAndFunction()) {
            this.result = !this.result;
            if (this.result) {
                this.drone.addDebugEntry("gui.progWidget.blockCondition.debug.blockMatches", pos);
            } else {
                this.drone.addDebugEntry("gui.progWidget.blockCondition.debug.blockDoesNotMatch", pos);
            }
            this.abort();
        }
        return false;
    }

    @Override
    protected void addEndingDebugEntry() {
        if (this.result) {
            this.drone.addDebugEntry("gui.progWidget.blockCondition.debug.allBlocksMatch");
        } else {
            this.drone.addDebugEntry("gui.progWidget.blockCondition.debug.noBlocksMatch");
        }
    }

    protected abstract boolean evaluate(BlockPos pos);

    @Override
    protected boolean doBlockInteraction(BlockPos pos, double distToBlock) {
        return false;
    }

    public boolean getResult() {
        return this.result;
    }

}
