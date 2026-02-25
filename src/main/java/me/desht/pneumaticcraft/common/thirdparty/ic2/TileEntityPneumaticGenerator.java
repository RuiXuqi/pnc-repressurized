package me.desht.pneumaticcraft.common.thirdparty.ic2;

import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.tile.IWrenchable;
import me.desht.pneumaticcraft.api.PneumaticRegistry;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import me.desht.pneumaticcraft.api.item.IItemRegistry.EnumUpgrade;
import me.desht.pneumaticcraft.api.tileentity.IHeatExchanger;
import me.desht.pneumaticcraft.common.config.ConfigHandler;
import me.desht.pneumaticcraft.common.heat.HeatUtil;
import me.desht.pneumaticcraft.common.network.GuiSynced;
import me.desht.pneumaticcraft.common.tileentity.IRedstoneControlled;
import me.desht.pneumaticcraft.common.tileentity.TileEntityPneumaticBase;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import java.util.Collections;
import java.util.List;

public class TileEntityPneumaticGenerator extends TileEntityPneumaticBase implements IEnergySource, IWrenchable, IRedstoneControlled, IHeatExchanger {
    public boolean outputting; //true when fully dispersed all the EU's it can possibly do.
    @GuiSynced
    public int curEnergyProduction;
    @GuiSynced
    private final IHeatExchangerLogic heatExchanger = PneumaticRegistry.getInstance().getHeatRegistry().getHeatExchangerLogic();
    @GuiSynced
    public int redstoneMode = 0;

    public TileEntityPneumaticGenerator() {
        super(PneumaticValues.DANGER_PRESSURE_PNEUMATIC_GENERATOR, PneumaticValues.MAX_PRESSURE_PNEUMATIC_GENERATOR, PneumaticValues.VOLUME_PNEUMATIC_GENERATOR, 4);
        this.addApplicableUpgrade(EnumUpgrade.SPEED);
        this.heatExchanger.setThermalCapacity(100);
    }

    public int getEfficiency() {
        return HeatUtil.getEfficiency(this.heatExchanger.getTemperatureAsInt());
    }

    @Override
    public void update() {
        super.update();
        if (!this.getWorld().isRemote) {
            if (this.outputting) {
                this.outputting = false;
            } else {
                this.curEnergyProduction = 0;
            }
        }
    }

    @Override
    protected void onFirstServerUpdate() {
        super.onFirstServerUpdate();
        MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
    }

    @Override
    public void invalidate() {
        if (this.getWorld() != null && !this.getWorld().isRemote) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
        }
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {
        if (this.getWorld() != null && !this.getWorld().isRemote) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
        }
        super.onChunkUnload();
    }

    @Override
    public boolean redstoneAllows() {
        switch (this.redstoneMode) {
            case 0:
                return true;
            case 1:
                return this.getWorld().getRedstonePowerFromNeighbors(this.getPos()) > 0;
            case 2:
                return this.getWorld().getRedstonePowerFromNeighbors(this.getPos()) == 0;
        }
        return false;
    }

    @Override
    public boolean isConnectedTo(EnumFacing side) {
        return this.getRotation() == side.getOpposite();
    }

    @Override
    public void handleGUIButtonPress(int buttonID, EntityPlayer player) {
        if (buttonID == 0) {
            this.redstoneMode++;
            if (this.redstoneMode > 2) this.redstoneMode = 0;
        }
    }

    @Override
    public String getName() {
        return IC2.PNEUMATIC_GENERATOR.getTranslationKey();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbtTagCompound) {
        super.readFromNBT(nbtTagCompound);

        this.redstoneMode = nbtTagCompound.getInteger("redstoneMode");
        this.outputting = nbtTagCompound.getBoolean("outputting");
        this.curEnergyProduction = nbtTagCompound.getInteger("energyProduction");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbtTagCompound) {
        super.writeToNBT(nbtTagCompound);

        nbtTagCompound.setInteger("redstoneMode", this.redstoneMode);
        nbtTagCompound.setBoolean("outputting", this.outputting);
        nbtTagCompound.setInteger("energyProduction", this.curEnergyProduction);

        return nbtTagCompound;
    }

    @Override
    public boolean emitsEnergyTo(IEnergyAcceptor iEnergyAcceptor, EnumFacing enumFacing) {
        return enumFacing == this.getRotation();
    }

    @Override
    public double getOfferedEnergy() {
        return this.getPressure() > PneumaticValues.MIN_PRESSURE_PNEUMATIC_GENERATOR && this.redstoneAllows() ? this.getEnergyPacketSize() : 0;
    }

    public int getEnergyPacketSize() {
        int upgradesInserted = this.getUpgrades(EnumUpgrade.SPEED);
        int energyAmount = 32 * (int) Math.pow(4, Math.min(3, upgradesInserted));
        return energyAmount * this.getEfficiency() / 100;
    }

    @Override
    public void drawEnergy(double amount) {
        int efficiency = ConfigHandler.machineProperties.pneumaticGeneratorEfficiency;
        if (efficiency < 1) efficiency = 1;
        int airUsage = (int) (amount / 0.25F * 100F / efficiency);
        this.addAir(-airUsage);
        this.heatExchanger.addHeat(airUsage / 40.0);
        this.outputting = true;
        this.curEnergyProduction = (int) amount;
    }

    @Override
    public int getSourceTier() {
        return 1 + this.getUpgrades(EnumUpgrade.SPEED);
    }

    @Override
    public EnumFacing getFacing(World world, BlockPos blockPos) {
        return this.getRotation();
    }

    @Override
    public boolean setFacing(World world, BlockPos blockPos, EnumFacing enumFacing, EntityPlayer entityPlayer) {
        return false; // FIXME
    }

    @Override
    public boolean wrenchCanRemove(World world, BlockPos blockPos, EntityPlayer entityPlayer) {
        return true;
    }

    @Override
    public List<ItemStack> getWrenchDrops(World world, BlockPos blockPos, IBlockState iBlockState, TileEntity tileEntity, EntityPlayer entityPlayer, int i) {
        return Collections.singletonList(new ItemStack(IC2.PNEUMATIC_GENERATOR));
    }

    @Override
    public int getRedstoneMode() {
        return this.redstoneMode;
    }

    @Override
    public IHeatExchangerLogic getHeatExchangerLogic(EnumFacing side) {
        return this.heatExchanger;
    }
}
