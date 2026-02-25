package me.desht.pneumaticcraft.common.tileentity;

import me.desht.pneumaticcraft.api.PneumaticRegistry;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import me.desht.pneumaticcraft.api.tileentity.IHeatExchanger;
import me.desht.pneumaticcraft.common.heat.HeatUtil;
import me.desht.pneumaticcraft.common.network.DescSynced;
import net.minecraft.util.EnumFacing;

public class TileEntityCompressedIronBlock extends TileEntityTickableBase implements IHeatExchanger, IComparatorSupport, IHeatTinted {

    protected final IHeatExchangerLogic heatExchanger = PneumaticRegistry.getInstance().getHeatRegistry().getHeatExchangerLogic();
    @DescSynced
    private int heatLevel = 10;
    private int oldComparatorOutput = 0;

    public TileEntityCompressedIronBlock() {
        this.heatExchanger.setThermalResistance(0.01);
    }

    @Override
    public IHeatExchangerLogic getHeatExchangerLogic(EnumFacing side) {
        return this.heatExchanger;
    }

    public int getHeatLevel() {
        return this.heatLevel;
    }

    @Override
    public void update() {
        super.update();

        if (!this.getWorld().isRemote) {
            this.heatLevel = HeatUtil.getHeatLevelForTemperature(this.heatExchanger.getTemperature());

            int comparatorOutput = HeatUtil.getComparatorOutput((int) this.heatExchanger.getTemperature());
            if (this.oldComparatorOutput != comparatorOutput) {
                this.oldComparatorOutput = comparatorOutput;
                this.updateNeighbours();
            }
        }
    }

    @Override
    protected boolean shouldRerenderChunkOnDescUpdate() {
        return true;
    }

    @Override
    public int getComparatorValue() {
        return HeatUtil.getComparatorOutput((int) this.heatExchanger.getTemperature());
    }

    @Override
    public int getHeatLevelForTintIndex(int tintIndex) {
        return this.heatLevel;
    }
}
