package me.desht.pneumaticcraft.common.block.tubes;

import me.desht.pneumaticcraft.client.model.module.ModelGauge;
import me.desht.pneumaticcraft.client.model.module.ModelModuleBase;
import me.desht.pneumaticcraft.common.GuiHandler.EnumGuiId;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketUpdatePressureBlock;
import me.desht.pneumaticcraft.common.tileentity.TileEntityPneumaticBase;
import me.desht.pneumaticcraft.lib.Names;

public class ModulePressureGauge extends TubeModuleRedstoneEmitting {
    public ModulePressureGauge() {
        this.lowerBound = 0;
        this.higherBound = 7.5F;
    }

    @Override
    public void update() {
        super.update();

        if (!this.pressureTube.world().isRemote) {
            if (this.pressureTube.world().getTotalWorldTime() % 20 == 0)
                NetworkHandler.sendToAllAround(new PacketUpdatePressureBlock((TileEntityPneumaticBase) this.getTube()), this.getTube().world());
            this.setRedstone(this.getRedstone(this.pressureTube.getAirHandler(null).getPressure()));
        }
    }

    private int getRedstone(float pressure) {
        return (int) ((pressure - this.lowerBound) / (this.higherBound - this.lowerBound) * 15);
    }

    @Override
    public String getType() {
        return Names.MODULE_GAUGE;
    }

    @Override
    public double getWidth() {
        return 0.5;
    }

    @Override
    protected double getHeight() {
        return 0.25;
    }

    @Override
    protected EnumGuiId getGuiId() {
        return EnumGuiId.PRESSURE_MODULE;
    }

    @Override
    public Class<? extends ModelModuleBase> getModelClass() {
        return ModelGauge.class;
    }
}
