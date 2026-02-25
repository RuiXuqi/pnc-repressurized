package me.desht.pneumaticcraft.common.tileentity;

import me.desht.pneumaticcraft.api.PneumaticRegistry;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import me.desht.pneumaticcraft.api.tileentity.IAirHandler;
import me.desht.pneumaticcraft.api.tileentity.IHeatExchanger;
import me.desht.pneumaticcraft.common.heat.HeatUtil;
import me.desht.pneumaticcraft.common.network.DescSynced;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;

public class TileEntityVortexTube extends TileEntityPneumaticBase implements IHeatExchanger, IHeatTinted {
    private final IHeatExchangerLogic coldHeatExchanger = PneumaticRegistry.getInstance().getHeatRegistry().getHeatExchangerLogic();
    private final IHeatExchangerLogic hotHeatExchanger = PneumaticRegistry.getInstance().getHeatRegistry().getHeatExchangerLogic();
    private final IHeatExchangerLogic connectingExchanger = PneumaticRegistry.getInstance().getHeatRegistry().getHeatExchangerLogic();
    private int visualizationTimer = 30;

    @DescSynced
    public final boolean[] sidesConnected = new boolean[6];
    @DescSynced
    private boolean visualize;
    @DescSynced
    private int coldHeatLevel = 10, hotHeatLevel = 10;

    public TileEntityVortexTube() {
        super(20, 25, 2000, 0);
        this.coldHeatExchanger.setThermalResistance(0.01);
        this.hotHeatExchanger.setThermalResistance(0.01);
        this.connectingExchanger.setThermalResistance(100);
        this.connectingExchanger.addConnectedExchanger(this.coldHeatExchanger);
        this.connectingExchanger.addConnectedExchanger(this.hotHeatExchanger);
    }

    @Override
    public IHeatExchangerLogic getHeatExchangerLogic(EnumFacing side) {
        if (side == null || side == this.getRotation().getOpposite()) {
            return this.hotHeatExchanger;
        } else if (side == this.getRotation()) {
            return this.coldHeatExchanger;
        } else {
            return null;
        }
    }

    @Override
    protected EnumFacing[] getConnectedHeatExchangerSides() {
        return new EnumFacing[]{this.getRotation().getOpposite()};
    }

    @Override
    protected void initializeIfHeatExchanger() {
        super.initializeIfHeatExchanger();
        this.initializeHeatExchanger(this.coldHeatExchanger, this.getRotation());
    }

    @Override
    public boolean isConnectedTo(EnumFacing side) {
        return side != this.getRotation() && side != this.getRotation().getOpposite();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        NBTTagCompound coldHeatTag = new NBTTagCompound();
        this.coldHeatExchanger.writeToNBT(coldHeatTag);
        tag.setTag("coldHeat", coldHeatTag);
        for (int i = 0; i < 6; i++) {
            tag.setBoolean("sideConnected" + i, this.sidesConnected[i]);
        }
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.coldHeatExchanger.readFromNBT(tag.getCompoundTag("coldHeat"));
        for (int i = 0; i < 6; i++) {
            this.sidesConnected[i] = tag.getBoolean("sideConnected" + i);
        }
    }

    public int getColdHeatLevel() {
        return this.visualize ? 0 : this.coldHeatLevel;
    }

    public int getHotHeatLevel() {
        return this.visualize ? 20 : this.hotHeatLevel;
    }

    @Override
    public void update() {
        super.update();
        if (!this.getWorld().isRemote) {
            // Only update the cold and connecting side, the hot side is handled in TileEntityBase.
            this.connectingExchanger.update();
            this.coldHeatExchanger.update();
            int usedAir = (int) (this.getPressure() * 10);
            if (usedAir > 0) {
                this.addAir(-usedAir);
                double generatedHeat = usedAir / 10D;
                this.coldHeatExchanger.addHeat(-generatedHeat);
                this.hotHeatExchanger.addHeat(generatedHeat);
            }
            this.visualize = this.visualizationTimer > 0;
            if (this.visualize) this.visualizationTimer--;
            this.coldHeatLevel = HeatUtil.getHeatLevelForTemperature(this.coldHeatExchanger.getTemperature());
            this.hotHeatLevel = HeatUtil.getHeatLevelForTemperature(this.hotHeatExchanger.getTemperature());
        }
    }

    @Override
    public void onBlockRotated() {
        this.visualizationTimer = 60;
    }

    @Override
    public void onNeighborBlockUpdate() {
        super.onNeighborBlockUpdate();
        this.updateConnections();
    }

    private void updateConnections() {
        List<Pair<EnumFacing, IAirHandler>> connections = this.getAirHandler(null).getConnectedPneumatics();
        Arrays.fill(this.sidesConnected, false);
        for (Pair<EnumFacing, IAirHandler> entry : connections) {
            this.sidesConnected[entry.getKey().ordinal()] = true;
        }
    }

    @Override
    protected boolean shouldRerenderChunkOnDescUpdate() {
        return true;
    }

    @Override
    public int getHeatLevelForTintIndex(int tintIndex) {
        switch (tintIndex) {
            case 0:
                return this.visualize ? 20 : this.hotHeatLevel;
            case 1:
                return this.visualize ? 0 : this.coldHeatLevel;
            default:
                return 0xFFFFFFFF;
        }
    }
}
