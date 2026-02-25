package me.desht.pneumaticcraft.common.thirdparty.ic2;

import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergyEmitter;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.tile.IWrenchable;
import me.desht.pneumaticcraft.api.PneumaticRegistry;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import me.desht.pneumaticcraft.api.item.IItemRegistry;
import me.desht.pneumaticcraft.api.tileentity.IHeatExchanger;
import me.desht.pneumaticcraft.common.config.ConfigHandler;
import me.desht.pneumaticcraft.common.heat.HeatUtil;
import me.desht.pneumaticcraft.common.network.GuiSynced;
import me.desht.pneumaticcraft.common.tileentity.IRedstoneControlled;
import me.desht.pneumaticcraft.common.tileentity.TileEntityPneumaticBase;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import java.util.Collections;
import java.util.List;

public class TileEntityElectricCompressor extends TileEntityPneumaticBase implements IEnergySink, IWrenchable, IRedstoneControlled, IHeatExchanger {
    private int outputTimer;//set to 20 when receiving energy, and decreased to 0 when not. Acts as a buffer before sending packets to update the client's rotation logic.

    private boolean redstoneAllows;
    @GuiSynced
    public int redstoneMode = 0;
    private int curEnergyProduction;
    @GuiSynced
    public int lastEnergyProduction;
    @GuiSynced
    private final IHeatExchangerLogic heatExchanger = PneumaticRegistry.getInstance().getHeatRegistry().getHeatExchangerLogic();

    public float turbineRotation;
    public float oldTurbineRotation;
    public float turbineSpeed;

    public TileEntityElectricCompressor() {
        super(PneumaticValues.DANGER_PRESSURE_ELECTRIC_COMPRESSOR, PneumaticValues.MAX_PRESSURE_ELECTRIC_COMPRESSOR, PneumaticValues.VOLUME_ELECTRIC_COMPRESSOR, 4);
        this.addApplicableUpgrade(IItemRegistry.EnumUpgrade.SPEED);
        this.addApplicableCustomUpgrade(IC2.overclockerUpgrade, IC2.energyStorageUpgrade, IC2.transformerUpgrade);
        this.heatExchanger.setThermalCapacity(100);
    }

    public int getEfficiency() {
        return HeatUtil.getEfficiency(this.heatExchanger.getTemperatureAsInt());
    }

    @Override
    public void update() {
        this.redstoneAllows = this.redstoneAllows();

        this.oldTurbineRotation = this.turbineRotation;
        if (this.outputTimer > 0) {
            this.turbineSpeed = Math.min(this.turbineSpeed + 0.2F, 10);
        } else {
            this.turbineSpeed = Math.max(this.turbineSpeed - 0.2F, 0);
        }
        this.turbineRotation += this.turbineSpeed;

        if (!this.getWorld().isRemote) {
            this.lastEnergyProduction = this.curEnergyProduction;
            this.curEnergyProduction = 0;
        }

        super.update();

        if (!this.getWorld().isRemote) {
            this.outputTimer--;
            if (this.outputTimer == 0) {
                this.sendDescriptionPacket();
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
        return side == this.getRotation() || side == this.getRotation().getOpposite();
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
        return IC2.ELECTRIC_COMPRESSOR.getTranslationKey();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbtTagCompound) {
        super.readFromNBT(nbtTagCompound);

        this.redstoneMode = nbtTagCompound.getInteger("redstoneMode");
        this.outputTimer = nbtTagCompound.getBoolean("outputTimer") ? 20 : 0;
        this.turbineSpeed = nbtTagCompound.getFloat("turbineSpeed");
        this.lastEnergyProduction = nbtTagCompound.getInteger("energyProduction");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbtTagCompound) {
        super.writeToNBT(nbtTagCompound);

        nbtTagCompound.setInteger("redstoneMode", this.redstoneMode);
        nbtTagCompound.setBoolean("outputTimer", this.outputTimer > 0);
        nbtTagCompound.setFloat("turbineSpeed", this.turbineSpeed);
        nbtTagCompound.setInteger("energyProduction", this.lastEnergyProduction);

        return nbtTagCompound;
    }

    @Override
    public double getDemandedEnergy() {
        return this.redstoneAllows ? Double.MAX_VALUE : 0;
    }

    @Override
    public int getSinkTier() {
        int upgradesInserted = this.getCustomUpgrades(IC2.transformerUpgrade);
        return 1 + upgradesInserted;
    }

    int getMaxSafeInput() {
        int upgradesInserted = this.getCustomUpgrades(IC2.transformerUpgrade);
        return 32 * (int) Math.pow(4, upgradesInserted);
    }

    @Override
    public double injectEnergy(EnumFacing enumFacing, double amount, double voltage) {
        int efficiency = ConfigHandler.machineProperties.electricCompressorEfficiency;
        int airProduction = (int) (amount / 0.25F * efficiency / 100F * this.getEfficiency() / 100);
        this.heatExchanger.addHeat(amount / 16);
        this.addAir(airProduction);
        this.curEnergyProduction += airProduction;
        boolean clientNeedsUpdate = this.outputTimer <= 0;
        this.outputTimer = 20;
        if (clientNeedsUpdate) this.sendDescriptionPacket();
        return 0;
    }

    @Override
    public boolean acceptsEnergyFrom(IEnergyEmitter iEnergyEmitter, EnumFacing enumFacing) {
        return enumFacing == EnumFacing.UP;
    }

    @Override
    public EnumFacing getFacing(World world, BlockPos blockPos) {
        return this.getRotation();
    }

    @Override
    public boolean setFacing(World world, BlockPos blockPos, EnumFacing enumFacing, EntityPlayer entityPlayer) {
        Block b = this.getBlockType();
        if (b instanceof BlockElectricCompressor) {
            ((BlockElectricCompressor) b).rotateBlock(world, entityPlayer, blockPos, enumFacing, EnumHand.MAIN_HAND);
            return true;
        }
        return false;
    }

    @Override
    public boolean wrenchCanRemove(World world, BlockPos blockPos, EntityPlayer entityPlayer) {
        return true;
    }

    @Override
    public List<ItemStack> getWrenchDrops(World world, BlockPos blockPos, IBlockState iBlockState, TileEntity tileEntity, EntityPlayer entityPlayer, int i) {
        return Collections.singletonList(new ItemStack(IC2.ELECTRIC_COMPRESSOR));
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
