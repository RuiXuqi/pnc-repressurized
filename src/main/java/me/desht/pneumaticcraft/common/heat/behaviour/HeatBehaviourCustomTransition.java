package me.desht.pneumaticcraft.common.heat.behaviour;

import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import me.desht.pneumaticcraft.common.config.BlockHeatPropertiesConfig;
import me.desht.pneumaticcraft.common.util.FluidUtils;
import me.desht.pneumaticcraft.lib.Names;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class HeatBehaviourCustomTransition extends HeatBehaviourTransition {
    private BlockHeatPropertiesConfig.CustomHeatEntry heatEntry;

    @Override
    public void initialize(String id, IHeatExchangerLogic connectedHeatLogic, World world, BlockPos pos, EnumFacing direction) {
        super.initialize(id, connectedHeatLogic, world, pos, direction);

        this.heatEntry = BlockHeatPropertiesConfig.INSTANCE.getCustomHeatEntry(this.getBlockState());
    }

    @Override
    public String getId() {
        return Names.MOD_ID + ":customTransition";
    }

    @Override
    public boolean isApplicable() {
        if (!super.isApplicable()) return false;

        BlockHeatPropertiesConfig.CustomHeatEntry entry = this.getHeatEntry();
        return this.getHeatEntry() != null && this.getHeatEntry().getTotalHeat() != 0;
    }

    @Override
    protected int getMaxExchangedHeat() {
        return this.getHeatEntry().getTotalHeat();
    }

    @Override
    protected boolean transformBlockHot() {
        IBlockState hot = this.getHeatEntry().getTransformHot();
        if (hot == null) return false;
        if (this.getFluid() != null) {
            this.transformFluidBlocks(hot, this.getHeatEntry().getTransformHotFlowing());
            return true;
        } else {
            return this.getWorld().setBlockState(this.getPos(), hot);
        }
    }

    @Override
    protected boolean transformBlockCold() {
        IBlockState cold = this.getHeatEntry().getTransformCold();
        if (cold == null) return false;
        if (this.getFluid() != null) {
            this.transformFluidBlocks(cold, this.getHeatEntry().getTransformColdFlowing());
            return true;
        } else {
            return this.getWorld().setBlockState(this.getPos(), cold);
        }
    }

    private BlockHeatPropertiesConfig.CustomHeatEntry getHeatEntry() {
        return this.heatEntry;
    }

    /**
     * Transform a fluid block into some other block, following flowing fluids back to the source block where
     * necessary.
     *
     * @param turningBlockSource  blockstate to transform the source block to
     * @param turningBlockFlowing blockstate to transform any flowing blocks to
     */
    private void transformFluidBlocks(IBlockState turningBlockSource, IBlockState turningBlockFlowing) {
        if (FluidUtils.isSourceBlock(this.getWorld(), this.getPos())) {
            this.getWorld().setBlockState(this.getPos(), turningBlockSource);
        } else {
            Set<BlockPos> traversed = new HashSet<>();
            Stack<BlockPos> pending = new Stack<>();
            pending.push(this.getPos());
            traversed.add(this.getPos());
            while (!pending.isEmpty()) {
                BlockPos pos = pending.pop();
                for (EnumFacing d : EnumFacing.VALUES) {
                    BlockPos newPos = pos.offset(d);
                    Block checkingBlock = this.getWorld().getBlockState(newPos).getBlock();
                    if (this.blocksSame(checkingBlock, this.getBlockState().getBlock()) && traversed.add(newPos)) {
                        if (FluidUtils.isSourceBlock(this.getWorld(), newPos)) {
                            this.getWorld().setBlockState(newPos, turningBlockSource);
                            this.onTransition(newPos);
                            return;
                        } else {
                            this.getWorld().setBlockState(newPos, turningBlockFlowing);
                            this.onTransition(newPos);
                            pending.push(newPos);
                        }
                    }
                }
            }
        }
    }

    private boolean blocksSame(Block b1, Block b2) {
        return b1 == b2
                || b1 == Blocks.FLOWING_LAVA && b2 == Blocks.LAVA
                || b1 == Blocks.LAVA && b2 == Blocks.FLOWING_LAVA
                || b1 == Blocks.FLOWING_WATER && b2 == Blocks.WATER
                || b1 == Blocks.WATER && b2 == Blocks.FLOWING_WATER;
    }
}
