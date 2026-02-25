package me.desht.pneumaticcraft.common.ai;

import me.desht.pneumaticcraft.common.ai.LogisticsManager.LogisticsTask;
import me.desht.pneumaticcraft.common.progwidgets.ICountWidget;
import me.desht.pneumaticcraft.common.progwidgets.ILiquidFiltered;
import me.desht.pneumaticcraft.common.progwidgets.ISidedWidget;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetAreaItemBase;
import me.desht.pneumaticcraft.common.semiblock.ISemiBlock;
import me.desht.pneumaticcraft.common.semiblock.SemiBlockLogistics;
import me.desht.pneumaticcraft.common.semiblock.SemiBlockManager;
import me.desht.pneumaticcraft.common.util.StreamUtils;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Stream;

public class DroneAILogistics extends EntityAIBase {
    private EntityAIBase curAI;
    private final IDroneBase drone;
    private final ProgWidgetAreaItemBase widget;
    private LogisticsTask curTask;

    public DroneAILogistics(IDroneBase drone, ProgWidgetAreaItemBase widget) {
        this.drone = drone;
        this.widget = widget;
    }

    private LogisticsManager getLogisticsManager() {
        if (this.drone.getLogisticsManager() == null) {
            Set<BlockPos> area = this.widget.getCachedAreaSet();
            if (!area.isEmpty()) {
                AxisAlignedBB aabb = ProgWidgetAreaItemBase.getExtents(area);
                Stream<ISemiBlock> semiBlocksInArea = SemiBlockManager.getInstance(this.drone.world()).getSemiBlocksInArea(this.drone.world(), aabb);
                Stream<SemiBlockLogistics> logisticFrames = StreamUtils.ofType(SemiBlockLogistics.class, semiBlocksInArea);

                LogisticsManager manager = new LogisticsManager();
                logisticFrames.filter(frame -> area.contains(frame.getPos())).forEach(manager::addLogisticFrame);
                this.drone.setLogisticsManager(manager);
            }
        }
        return this.drone.getLogisticsManager();
    }

    @Override
    public boolean shouldExecute() {
        if (this.getLogisticsManager() == null) return false;
        this.curTask = null;
        return this.doLogistics();
    }

    private boolean doLogistics() {
        ItemStack item = this.drone.getInv().getStackInSlot(0);
        FluidStack fluid = this.drone.getTank().getFluid();
        PriorityQueue<LogisticsTask> tasks = this.getLogisticsManager().getTasks(item.isEmpty() ? fluid : item);
        if (tasks.size() > 0) {
            this.curTask = tasks.poll();
            return this.execute(this.curTask);
        }
        return false;
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (this.curTask == null) return false;
        if (!this.curAI.shouldContinueExecuting()) {
            if (this.curAI instanceof DroneEntityAIInventoryImport) {
                this.curTask.requester.clearIncomingStack(this.curTask.transportingItem);
                return this.clearAIAndProvideAgain();
            } else if (this.curAI instanceof DroneAILiquidImport) {
                this.curTask.requester.clearIncomingStack(this.curTask.transportingFluid);
                return this.clearAIAndProvideAgain();
            } else {
                this.curAI = null;
                return false;
            }
        } else {
            this.curTask.informRequester();
            return true;
        }
    }

    private boolean clearAIAndProvideAgain() {
        this.curAI = null;
        if (this.curTask.isStillValid(this.drone.getInv().getStackInSlot(0).isEmpty() ? this.drone.getTank().getFluid() : this.drone.getInv().getStackInSlot(0)) && this.execute(this.curTask)) {
            return true;
        } else {
            this.curTask = null;
            return this.doLogistics();
        }
    }

    public boolean execute(LogisticsTask task) {
        if (!this.drone.getInv().getStackInSlot(0).isEmpty()) {
            if (this.hasNoPathTo(task.requester.getPos())) return false;
            this.curAI = new DroneEntityAIInventoryExport(this.drone,
                    new FakeWidgetLogistics(task.requester.getPos(), task.requester.getSide(), task.transportingItem));
        } else if (this.drone.getTank().getFluidAmount() > 0) {
            if (this.hasNoPathTo(task.requester.getPos())) return false;
            this.curAI = new DroneAILiquidExport(this.drone,
                    new FakeWidgetLogistics(task.requester.getPos(), task.requester.getSide(), task.transportingFluid.stack));
        } else if (!task.transportingItem.isEmpty()) {
            if (this.hasNoPathTo(task.provider.getPos())) return false;
            this.curAI = new DroneEntityAIInventoryImport(this.drone,
                    new FakeWidgetLogistics(task.provider.getPos(), task.provider.getSide(), task.transportingItem));
        } else {
            if (this.hasNoPathTo(task.provider.getPos())) return false;
            this.curAI = new DroneAILiquidImport(this.drone,
                    new FakeWidgetLogistics(task.provider.getPos(), task.provider.getSide(), task.transportingFluid.stack));
        }
        if (this.curAI.shouldExecute()) {
            task.informRequester();
            return true;
        } else {
            return false;
        }
    }

    private boolean hasNoPathTo(BlockPos pos) {
        for (EnumFacing d : EnumFacing.VALUES) {
            if (this.drone.isBlockValidPathfindBlock(pos.offset(d))) return false;
        }
        this.drone.addDebugEntry("gui.progWidget.general.debug.cantNavigate", pos);
        return true;
    }

    private static class FakeWidgetLogistics extends ProgWidgetAreaItemBase implements ISidedWidget, ICountWidget,
            ILiquidFiltered {
        @Nonnull
        private final ItemStack stack;
        private final FluidStack fluid;
        private final Set<BlockPos> area;
        private final boolean[] sides = new boolean[6];

        FakeWidgetLogistics(BlockPos pos, EnumFacing side, @Nonnull ItemStack stack) {
            this.stack = stack;
            this.fluid = null;
            this.area = new HashSet<>();
            this.area.add(pos);
            this.sides[side.getIndex()] = true;
        }

        FakeWidgetLogistics(BlockPos pos, EnumFacing side, FluidStack fluid) {
            this.stack = ItemStack.EMPTY;
            this.fluid = fluid;
            this.area = new HashSet<>();
            this.area.add(pos);
            this.sides[side.getIndex()] = true;
        }

        @Override
        public String getWidgetString() {
            return null;
        }

        @Override
        public int getCraftingColorIndex() {
            return 0;
        }

        @Override
        public void getArea(Set<BlockPos> area) {
            area.addAll(this.area);
        }

        @Override
        public void setSides(boolean[] sides) {
        }

        @Override
        public boolean[] getSides() {
            return this.sides;
        }

        @Override
        public boolean isItemValidForFilters(@Nonnull ItemStack item) {
            return !item.isEmpty() && item.isItemEqual(this.stack);
        }

        @Override
        public ResourceLocation getTexture() {
            return null;
        }

        @Override
        public boolean useCount() {
            return true;
        }

        @Override
        public void setUseCount(boolean useCount) {
        }

        @Override
        public int getCount() {
            return !this.stack.isEmpty() ? this.stack.getCount() : this.fluid.amount;
        }

        @Override
        public void setCount(int count) {
        }

        @Override
        public boolean isFluidValid(Fluid fluid) {
            return fluid == this.fluid.getFluid();
        }

    }

}
