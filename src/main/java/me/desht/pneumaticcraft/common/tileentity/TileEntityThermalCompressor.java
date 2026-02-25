package me.desht.pneumaticcraft.common.tileentity;

import me.desht.pneumaticcraft.api.PneumaticRegistry;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import me.desht.pneumaticcraft.api.tileentity.IHeatExchanger;
import me.desht.pneumaticcraft.common.block.Blockss;
import me.desht.pneumaticcraft.common.config.ConfigHandler;
import me.desht.pneumaticcraft.common.heat.HeatUtil;
import me.desht.pneumaticcraft.common.network.DescSynced;
import me.desht.pneumaticcraft.common.network.GuiSynced;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

public class TileEntityThermalCompressor extends TileEntityPneumaticBase implements IHeatExchanger, IHeatTinted, IRedstoneControlled {
    private static final double AIR_GEN_MULTIPLIER = 0.05;  // mL per degree of difference

    private final double[] generated = new double[2];

    @GuiSynced
    private final IHeatExchangerLogic[] heatExchangers = new IHeatExchangerLogic[4];
    private final IHeatExchangerLogic connector1;
    private final IHeatExchangerLogic connector2;

    private final IHeatExchangerLogic dummyExchanger;  // never does anything; gets returned from the "null" face

    @DescSynced
    private final int[] heatLevel = new int[4];  // S-W-N-E
    @GuiSynced
    private int redstoneMode;

    public TileEntityThermalCompressor() {
        super(PneumaticValues.DANGER_PRESSURE_THERMAL_COMPRESSOR, PneumaticValues.MAX_PRESSURE_THERMAL_COMPRESSOR, PneumaticValues.VOLUME_THERMAL_COMPRESSOR, 4);

        for (int i = 0; i < this.heatExchangers.length; i++) {
            this.heatExchangers[i] = PneumaticRegistry.getInstance().getHeatRegistry().getHeatExchangerLogic();
            this.heatExchangers[i].setThermalCapacity(2);
        }

        this.connector1 = this.makeConnector(EnumFacing.NORTH);
        this.connector2 = this.makeConnector(EnumFacing.EAST);

        this.dummyExchanger = PneumaticRegistry.getInstance().getHeatRegistry().getHeatExchangerLogic();
    }

    private IHeatExchangerLogic makeConnector(EnumFacing side) {
        IHeatExchangerLogic connector = PneumaticRegistry.getInstance().getHeatRegistry().getHeatExchangerLogic();
        connector.setThermalResistance(ConfigHandler.machineProperties.thermalCompressorThermalResistance);
        connector.addConnectedExchanger(this.heatExchangers[side.getHorizontalIndex()]);
        connector.addConnectedExchanger(this.heatExchangers[side.getOpposite().getHorizontalIndex()]);
        return connector;
    }

    @Override
    protected void initializeIfHeatExchanger() {
        super.initializeIfHeatExchanger();

        for (int i = 0; i < this.heatExchangers.length; i++) {
            this.initializeHeatExchanger(this.heatExchangers[i], EnumFacing.byHorizontalIndex(i));
        }
    }

    @Override
    public void update() {
        super.update();

        if (!this.world.isRemote) {
            for (IHeatExchangerLogic heatExchanger : this.heatExchangers) {
                heatExchanger.update();
            }

            if (this.redstoneAllows()) {
                this.connector1.setThermalResistance(ConfigHandler.machineProperties.thermalCompressorThermalResistance);
                this.connector2.setThermalResistance(ConfigHandler.machineProperties.thermalCompressorThermalResistance);
            } else {
                this.connector1.setThermalResistance(ConfigHandler.machineProperties.thermalCompressorThermalResistance * 100);
                this.connector2.setThermalResistance(ConfigHandler.machineProperties.thermalCompressorThermalResistance * 100);
            }

            this.connector1.update();
            this.connector2.update();

            if (this.redstoneAllows()) {
                this.generatePressure(0);  // south and north
                this.generatePressure(1);  // west and east
            }

            for (int i = 0; i < 4; i++) {
                this.heatLevel[i] = HeatUtil.getHeatLevelForTemperature(this.heatExchangers[i].getTemperature());
            }
        }
    }


    private void generatePressure(int side) {
        double diff = Math.abs(this.heatExchangers[side].getTemperature() - this.heatExchangers[side + 2].getTemperature());
        this.generated[side] += diff * AIR_GEN_MULTIPLIER;

        if (this.generated[side] > 1.0) {
            int toAdd = (int) this.generated[side];
            this.addAir(toAdd);
            this.generated[side] -= toAdd;
        }
    }

    @Override
    public boolean isConnectedTo(EnumFacing side) {
        return side.getAxis() == EnumFacing.Axis.Y;
    }

    @Override
    public String getName() {
        return Blockss.THERMAL_COMPRESSOR.getTranslationKey();
    }

    @Override
    public IHeatExchangerLogic getHeatExchangerLogic(EnumFacing side) {
        if (side == null)
            return this.dummyExchanger;
        else
            return side.getAxis() == EnumFacing.Axis.Y ? null : this.heatExchangers[side.getHorizontalIndex()];
    }

    @Override
    public int getHeatLevelForTintIndex(int tintIndex) {
        if (tintIndex >= 0 && tintIndex <= 3) {
            return this.heatLevel[tintIndex];
        } else {
            return 10;
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        for (int i = 0; i < 4; i++) {
            NBTTagCompound t1 = new NBTTagCompound();
            this.heatExchangers[i].writeToNBT(t1);
            tag.setTag("side" + i, t1);
        }
        tag.setInteger("redstoneMode", this.redstoneMode);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        for (int i = 0; i < 4; i++) {
            this.heatExchangers[i].readFromNBT(tag.getCompoundTag("side" + i));
        }
        this.redstoneMode = tag.getInteger("redstoneMode");
    }

    @Override
    protected boolean shouldRerenderChunkOnDescUpdate() {
        return true;
    }

    @Override
    public void handleGUIButtonPress(int guiID, EntityPlayer player) {
        if (guiID == 0) {
            this.redstoneMode++;
            if (this.redstoneMode > 2) this.redstoneMode = 0;
        }
    }

    @Override
    public int getRedstoneMode() {
        return this.redstoneMode;
    }
}
