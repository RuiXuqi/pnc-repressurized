package me.desht.pneumaticcraft.common.heat.behaviour;

import me.desht.pneumaticcraft.api.heat.HeatBehaviour;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import me.desht.pneumaticcraft.common.heat.HeatExchangerLogicTicking;
import me.desht.pneumaticcraft.common.semiblock.SemiBlockHeatFrame;
import me.desht.pneumaticcraft.common.semiblock.SemiBlockManager;
import me.desht.pneumaticcraft.lib.Names;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class HeatBehaviourHeatFrame extends HeatBehaviour<TileEntity> {
    private SemiBlockHeatFrame semiBlock;

    @Override
    public void initialize(String id, IHeatExchangerLogic connectedHeatLogic, World world, BlockPos pos, EnumFacing direction) {
        super.initialize(id, connectedHeatLogic, world, pos, direction);
        this.semiBlock = null;
    }

    @Override
    public String getId() {
        return Names.MOD_ID + ":heatFrame";
    }

    private SemiBlockHeatFrame getSemiBlock() {
        if (this.semiBlock == null) {
            this.semiBlock = SemiBlockManager.getInstance(this.getWorld()).getSemiBlock(SemiBlockHeatFrame.class, this.getWorld(), this.getPos());
        }
        return this.semiBlock;
    }

    @Override
    public boolean isApplicable() {
        return this.getSemiBlock() != null;
    }

    @Override
    public void update() {
        HeatExchangerLogicTicking.exchange(this.getSemiBlock().getHeatExchangerLogic(null), this.getHeatExchanger());
    }

}
