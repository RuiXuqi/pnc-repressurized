package me.desht.pneumaticcraft.common.thirdparty.mekanism;

import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import me.desht.pneumaticcraft.api.tileentity.IHeatExchanger;
import me.desht.pneumaticcraft.common.config.ConfigHandler;
import me.desht.pneumaticcraft.common.tileentity.TileEntityBase;
import mekanism.api.IHeatTransfer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

public class MekanismHeatAdapter implements IHeatTransfer {

    private TileEntityBase te;
    private IHeatExchangerLogic logic;
    private double heat;

    public IHeatTransfer setup(TileEntityBase te, EnumFacing side) {
        this.te = te;
        this.logic = ((IHeatExchanger) te).getHeatExchangerLogic(side);
        return this.logic == null ? null : this;
    }

    @Override
    public double getTemp() {
        return this.logic.getTemperature();
    }

    @Override
    public double getInverseConductionCoefficient() {
        return this.logic.getThermalResistance() * ConfigHandler.integration.mekThermalResistanceMult;
    }

    @Override
    public double getInsulationCoefficient(EnumFacing enumFacing) {
        return 1000;
    }

    @Override
    public void transferHeatTo(double v) {
        this.logic.addHeat(v * ConfigHandler.integration.mekHeatEfficiency);
    }

    @Override
    public double[] simulateHeat() {
        // this doesn't seem to get called
        return new double[0];
    }

    @Override
    public double applyTemperatureChange() {
        // this doesn't seem to get called
        return this.logic.getTemperature();
    }

    @Override
    public boolean canConnectHeat(EnumFacing enumFacing) {
        return ((IHeatExchanger) this.te).getHeatExchangerLogic(enumFacing) != null;
    }

    @Override
    public IHeatTransfer getAdjacent(EnumFacing enumFacing) {
        TileEntity neighbour = this.te.getWorld().getTileEntity(this.te.getPos().offset(enumFacing));
        if (neighbour != null && neighbour.hasCapability(Mekanism.CAPABILITY_HEAT_TRANSFER, enumFacing.getOpposite())) {
            return neighbour.getCapability(Mekanism.CAPABILITY_HEAT_TRANSFER, enumFacing.getOpposite());
        }
        return null;
    }
}
