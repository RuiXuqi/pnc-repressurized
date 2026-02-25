package me.desht.pneumaticcraft.common.block.tubes;

import me.desht.pneumaticcraft.client.model.module.ModelFlowDetector;
import me.desht.pneumaticcraft.client.model.module.ModelModuleBase;
import me.desht.pneumaticcraft.common.GuiHandler.EnumGuiId;
import me.desht.pneumaticcraft.lib.Names;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;

import java.util.List;

public class ModuleFlowDetector extends TubeModuleRedstoneEmitting implements IInfluenceDispersing {
    public float rotation, oldRotation;
    private int flow;
    private int oldFlow;

    @Override
    public void update() {
        super.update();
        this.oldRotation = this.rotation;
        this.rotation += this.getRedstoneLevel() / 100F;

        if (!this.pressureTube.world().isRemote) {
            if (this.setRedstone(this.flow / 5)) {
                this.sendDescriptionPacket();
            }
            this.oldFlow = this.flow;
            this.flow = 0;
        }
    }

    @Override
    public String getType() {
        return Names.MODULE_FLOW_DETECTOR;
    }

    @Override
    public int getMaxDispersion() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void onAirDispersion(int amount) {
        this.flow += amount;
    }

    @Override
    public void addInfo(List<String> curInfo) {
        super.addInfo(curInfo);
        curInfo.add("Flow: " + TextFormatting.WHITE + this.oldFlow + " mL/tick");
        super.addInfo(curInfo);
    }

    @Override
    public boolean isInline() {
        return true;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.rotation = tag.getFloat("rotation");
        this.oldFlow = tag.getInteger("flow");//taggin it for waila purposes.
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setFloat("rotation", this.rotation);
        tag.setInteger("flow", this.oldFlow);
    }

    @Override
    public boolean canUpgrade() {
        return false;
    }

    @Override
    protected EnumGuiId getGuiId() {
        return null;
    }

    @Override
    public Class<? extends ModelModuleBase> getModelClass() {
        return ModelFlowDetector.class;
    }
}
