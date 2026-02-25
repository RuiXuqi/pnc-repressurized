package me.desht.pneumaticcraft.common.heat.behaviour;

import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import me.desht.pneumaticcraft.common.heat.HeatExchangerManager;
import me.desht.pneumaticcraft.common.heat.HeatExtractionTracker;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketPlaySound;
import me.desht.pneumaticcraft.common.network.PacketSpawnParticle;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public abstract class HeatBehaviourTransition extends HeatBehaviourLiquid {
    private double maxExchangedHeat;
    private double blockTemp = -1;
    private IHeatExchangerLogic logic;
    private HeatExtractionTracker tracker;

    @Override
    public boolean isApplicable() {
        this.logic = HeatExchangerManager.getInstance().getLogic(this.getWorld(), this.getPos(), null);
        return this.logic != null;
    }

    @Override
    public void initialize(String id, IHeatExchangerLogic connectedHeatLogic, World world, BlockPos pos, EnumFacing direction) {
        super.initialize(id, connectedHeatLogic, world, pos, direction);

        this.tracker = HeatExtractionTracker.getInstance(this.getWorld());
    }

    protected abstract int getMaxExchangedHeat();

    protected boolean transformBlockHot() {
        return false;
    }

    protected boolean transformBlockCold() {
        return false;
    }

    @Override
    public void update() {
        if (this.blockTemp == -1) {
            this.blockTemp = this.logic.getTemperature();
            this.maxExchangedHeat = this.getMaxExchangedHeat() * (this.logic.getThermalResistance() + this.getHeatExchanger().getThermalResistance());
        }
        double extractedHeat = this.tracker.getHeatExtracted(this.getPos());
        if (extractedHeat < Math.abs(this.maxExchangedHeat)) {
            double toExtract = this.blockTemp - this.getHeatExchanger().getTemperature();
            this.tracker.extractHeat(this.getPos(), toExtract);
            extractedHeat += toExtract;
        }
        if (extractedHeat >= this.maxExchangedHeat) {
            if (this.transformBlockCold()) this.tracker.extractHeat(this.getPos(), -this.maxExchangedHeat);
        } else if (extractedHeat <= -this.maxExchangedHeat) {
            if (this.transformBlockHot()) this.tracker.extractHeat(this.getPos(), this.maxExchangedHeat);
        }
    }

    void onTransition(BlockPos pos) {
        NetworkHandler.sendToAllAround(new PacketPlaySound(SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.AMBIENT, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.5F, 2.6F + (this.getWorld().rand.nextFloat() - this.getWorld().rand.nextFloat()) * 0.8F, true), this.getWorld());
        NetworkHandler.sendToAllAround(new PacketSpawnParticle(EnumParticleTypes.SMOKE_LARGE, pos.getX(), pos.getY() + 1, pos.getZ(),
                        0, 0, 0, 8, 1, 0, 1),
                this.getWorld());
    }
}
