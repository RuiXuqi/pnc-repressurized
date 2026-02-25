package me.desht.pneumaticcraft.common.ai;

import me.desht.pneumaticcraft.api.drone.IBlockInteractHandler;
import me.desht.pneumaticcraft.common.progwidgets.ICountWidget;
import me.desht.pneumaticcraft.common.progwidgets.ISidedWidget;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetAreaItemBase;
import net.minecraft.util.math.BlockPos;

public abstract class DroneAIImExBase extends DroneAIBlockInteraction implements IBlockInteractHandler {
    private int transportCount;

    protected DroneAIImExBase(IDroneBase drone, ProgWidgetAreaItemBase widget) {
        super(drone, widget);
        this.transportCount = ((ICountWidget) widget).getCount();
    }

    @Override
    public boolean shouldExecute() {
        boolean countReached = this.transportCount <= 0;
        this.transportCount = ((ICountWidget) this.widget).getCount();
        return !(countReached && this.useCount()) && super.shouldExecute();
    }

    @Override
    public void decreaseCount(int count) {
        this.transportCount -= count;
    }

    @Override
    public int getRemainingCount() {
        return this.transportCount;
    }

    @Override
    protected boolean doBlockInteraction(BlockPos pos, double distToBlock) {
        return !this.useCount() || this.transportCount > 0;
    }

    @Override
    public boolean[] getSides() {
        return ((ISidedWidget) this.widget).getSides();
    }

    @Override
    public boolean useCount() {
        return ((ICountWidget) this.widget).useCount();
    }

}
