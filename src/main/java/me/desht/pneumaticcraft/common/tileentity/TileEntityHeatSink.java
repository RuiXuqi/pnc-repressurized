package me.desht.pneumaticcraft.common.tileentity;

import me.desht.pneumaticcraft.api.PneumaticRegistry;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import me.desht.pneumaticcraft.api.tileentity.IHeatExchanger;
import me.desht.pneumaticcraft.common.heat.HeatExchangerLogicAmbient;
import me.desht.pneumaticcraft.lib.TileEntityConstants;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityHeatSink extends TileEntityCompressedIronBlock implements IHeatExchanger {

    private final IHeatExchangerLogic airExchanger = PneumaticRegistry.getInstance().getHeatRegistry().getHeatExchangerLogic();

    private double ambientTemp = -1;

    public TileEntityHeatSink() {
        this.airExchanger.addConnectedExchanger(this.heatExchanger);
        this.airExchanger.setThermalResistance(TileEntityConstants.HEAT_SINK_THERMAL_RESISTANCE);
    }

    @Override
    public IHeatExchangerLogic getHeatExchangerLogic(EnumFacing side) {
        return side == null || side == this.getRotation() ? super.getHeatExchangerLogic(side) : null;
    }

    @Override
    protected EnumFacing[] getConnectedHeatExchangerSides() {
        return new EnumFacing[]{this.getRotation()};
    }

    @Override
    protected boolean shouldRerenderChunkOnDescUpdate() {
        return true;
    }

    @Override
    public void update() {
        if (this.ambientTemp < 0) {
            this.ambientTemp = HeatExchangerLogicAmbient.atPosition(this.getWorld(), this.getPos()).getTemperature();
            this.airExchanger.setTemperature(this.ambientTemp);
        }

        super.update();

        this.airExchanger.update();
        this.airExchanger.setTemperature(this.ambientTemp);
    }

    public void onFannedByAirGrate() {
        this.heatExchanger.update();
        this.airExchanger.setTemperature(this.ambientTemp);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(
                this.getPos().getX(), this.getPos().getY(), this.getPos().getZ(),
                this.getPos().getX() + 1, this.getPos().getY() + 1, this.getPos().getZ() + 1
        );
    }

}
