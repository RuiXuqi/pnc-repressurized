package me.desht.pneumaticcraft.common.tileentity;

import me.desht.pneumaticcraft.api.PneumaticRegistry;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import me.desht.pneumaticcraft.api.item.IItemRegistry;
import me.desht.pneumaticcraft.api.tileentity.IAirHandler;
import me.desht.pneumaticcraft.api.tileentity.IHeatExchanger;
import me.desht.pneumaticcraft.common.block.Blockss;
import me.desht.pneumaticcraft.common.config.ConfigHandler;
import me.desht.pneumaticcraft.common.heat.HeatUtil;
import me.desht.pneumaticcraft.common.network.GuiSynced;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.List;

public class TileEntityFluxCompressor extends TileEntityPneumaticBase implements IRedstoneControlled, IHeatExchanger {
    private final PneumaticEnergyStorage energy = new PneumaticEnergyStorage(100000);
    @GuiSynced
    private int rfPerTick;
    @GuiSynced
    private int airPerTick;
    @GuiSynced
    private int redstoneMode;
    @GuiSynced
    private final IHeatExchangerLogic heatExchanger = PneumaticRegistry.getInstance().getHeatRegistry().getHeatExchangerLogic();

    public TileEntityFluxCompressor() {
        this(PneumaticValues.DANGER_PRESSURE_FLUX_COMPRESSOR,
                PneumaticValues.MAX_PRESSURE_FLUX_COMPRESSOR,
                PneumaticValues.VOLUME_FLUX_COMPRESSOR, 4);
    }

    public TileEntityFluxCompressor(float dangerPressure, float criticalPressure, int volume, int upgradeSlots) {
        super(dangerPressure, criticalPressure, volume, 4);
        this.addApplicableUpgrade(IItemRegistry.EnumUpgrade.SPEED);
        this.heatExchanger.setThermalCapacity(100);
    }

    public int getEfficiency() {
        return HeatUtil.getEfficiency(this.heatExchanger.getTemperatureAsInt());
    }

    @Override
    public void update() {
        super.update();

        if (!this.world.isRemote) {
            if (this.world.getTotalWorldTime() % 5 == 0) {
                this.airPerTick = (int) (40 * this.getSpeedUsageMultiplierFromUpgrades() * this.getEfficiency() * ConfigHandler.machineProperties.fluxCompressorEfficiency / 100 / 100);
                this.rfPerTick = (int) (40 * this.getSpeedUsageMultiplierFromUpgrades());
            }
            if (this.redstoneAllows() && this.energy.getEnergyStored() >= this.rfPerTick) {
                this.addAir(this.airPerTick);
                this.energy.extractEnergy(this.rfPerTick, false);
                this.heatExchanger.addHeat(this.rfPerTick / 100D);
            }
        }

        if (!this.getWorld().isRemote) {
            List<Pair<EnumFacing, IAirHandler>> teList = this.getAirHandler(null).getConnectedPneumatics();
            if (teList.size() == 0) this.getAirHandler(null).airLeak(this.getRotation().getOpposite());
        }
    }

    @Override
    public String getName() {
        return Blockss.FLUX_COMPRESSOR.getTranslationKey();
    }

    @Override
    public boolean isConnectedTo(EnumFacing side) {
        return side == this.getRotation().getOpposite();
    }

    @Override
    public int getRedstoneMode() {
        return this.redstoneMode;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        // back face is where pneumatics connect
        return (capability == CapabilityEnergy.ENERGY && facing != this.getRotation().getOpposite())
                || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY && facing != this.getRotation().getOpposite()) {
            return CapabilityEnergy.ENERGY.cast(this.energy);
        } else {
            return super.getCapability(capability, facing);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        this.energy.writeToNBT(tag);
        tag.setByte("redstoneMode", (byte) this.redstoneMode);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.energy.readFromNBT(tag);
        this.redstoneMode = tag.getByte("redstoneMode");
    }

    @Override
    public void handleGUIButtonPress(int buttonID, EntityPlayer player) {
        if (buttonID == 0 && ++this.redstoneMode > 2) this.redstoneMode = 0;
    }

    @Override
    public IHeatExchangerLogic getHeatExchangerLogic(EnumFacing side) {
        return this.heatExchanger;
    }

    public int getInfoEnergyPerTick() {
        return this.rfPerTick;
    }

    public int getInfoEnergyStored() {
        return this.energy.getEnergyStored();
    }

    public int getAirRate() {
        return this.airPerTick;
    }
}
