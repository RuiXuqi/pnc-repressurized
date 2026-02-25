package me.desht.pneumaticcraft.common.block.tubes;

import me.desht.pneumaticcraft.client.model.module.ModelModuleBase;
import me.desht.pneumaticcraft.client.model.module.ModelSafetyValve;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.Names;
import net.minecraft.util.text.TextFormatting;

import java.util.List;

public class ModuleSafetyValve extends TubeModuleRedstoneReceiving {

    @Override
    public void update() {
        super.update();
        if (!this.pressureTube.world().isRemote) {
            if (this.pressureTube.getAirHandler(null).getPressure() > this.getThreshold()) {
                this.pressureTube.getAirHandler(null).airLeak(this.dir);
            }
        }
    }

    @Override
    public void addInfo(List<String> curInfo) {
        super.addInfo(curInfo);
        curInfo.add("Threshold: " + TextFormatting.WHITE + PneumaticCraftUtils.roundNumberTo(this.getThreshold(), 1) + " bar");
    }

    @Override
    public String getType() {
        return Names.MODULE_SAFETY_VALVE;
    }

    @Override
    public Class<? extends ModelModuleBase> getModelClass() {
        return ModelSafetyValve.class;
    }
}
