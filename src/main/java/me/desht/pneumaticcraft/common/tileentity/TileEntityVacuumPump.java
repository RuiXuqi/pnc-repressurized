package me.desht.pneumaticcraft.common.tileentity;

import me.desht.pneumaticcraft.api.PneumaticRegistry;
import me.desht.pneumaticcraft.api.item.IItemRegistry.EnumUpgrade;
import me.desht.pneumaticcraft.api.tileentity.IAirHandler;
import me.desht.pneumaticcraft.api.tileentity.IManoMeasurable;
import me.desht.pneumaticcraft.common.block.Blockss;
import me.desht.pneumaticcraft.common.network.DescSynced;
import me.desht.pneumaticcraft.common.network.GuiSynced;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class TileEntityVacuumPump extends TileEntityPneumaticBase implements IRedstoneControlled, IManoMeasurable {
    @GuiSynced
    private final IAirHandler vacuumHandler = PneumaticRegistry.getInstance().getAirHandlerSupplier().createTierOneAirHandler(PneumaticValues.VOLUME_VACUUM_PUMP);
    public int rotation;
    public int oldRotation;
    private int turnTimer = -1;
    @DescSynced
    public boolean turning = false;
    private int rotationSpeed;
    @GuiSynced
    public int redstoneMode;

    public TileEntityVacuumPump() {
        super(PneumaticValues.DANGER_PRESSURE_VACUUM_PUMP, PneumaticValues.MAX_PRESSURE_VACUUM_PUMP, PneumaticValues.VOLUME_VACUUM_PUMP, 4);
        this.addApplicableUpgrade(EnumUpgrade.SPEED);
    }

    @Override
    public IAirHandler getAirHandler(EnumFacing side) {
        if (side == null || side == this.getInputSide()) {
            return super.getAirHandler(side);
        } else if (side == this.getVacuumSide()) {
            return this.vacuumHandler;
        } else {
            return null;
        }
    }

    @Override
    public void validate() {
        super.validate();
        this.vacuumHandler.validate(this);
    }

    @Override
    public void onNeighborTileUpdate() {
        super.onNeighborTileUpdate();
        this.vacuumHandler.onNeighborChange();
    }

    public EnumFacing getInputSide() {
        return this.getVacuumSide().getOpposite();
    }

    public EnumFacing getVacuumSide() {
        return this.getRotation();
    }

    @Override
    public void update() {
        if (!this.getWorld().isRemote && this.turnTimer >= 0) {
            this.turnTimer--;
        }
        if (!this.getWorld().isRemote
                && this.getAirHandler(this.getInputSide()).getPressure() > PneumaticValues.MIN_PRESSURE_VACUUM_PUMP
                && this.getAirHandler(this.getVacuumSide()).getPressure() > -1F
                && this.redstoneAllows()) {
            if (!this.getWorld().isRemote && this.turnTimer == -1) {
                this.turning = true;
            }
            this.getAirHandler(this.getVacuumSide()).addAir((int) (-PneumaticValues.PRODUCTION_VACUUM_PUMP * this.getSpeedMultiplierFromUpgrades())); // negative because it's pulling a vacuum.
            this.getAirHandler(this.getInputSide()).addAir((int) (-PneumaticValues.USAGE_VACUUM_PUMP * this.getSpeedUsageMultiplierFromUpgrades()));
            this.turnTimer = 40;
        }
        if (this.turnTimer == 0) {
            this.turning = false;
        }
        this.oldRotation = this.rotation;
        if (this.getWorld().isRemote) {
            if (this.turning) {
                this.rotationSpeed = Math.min(this.rotationSpeed + 1, 20);
            } else {
                this.rotationSpeed = Math.max(this.rotationSpeed - 1, 0);
            }
            this.rotation += this.rotationSpeed;
        }

        super.update();
        this.vacuumHandler.update();

        IAirHandler inputHandler = this.getAirHandler(this.getInputSide());
        List<Pair<EnumFacing, IAirHandler>> teList = inputHandler.getConnectedPneumatics();
        if (teList.size() == 0) inputHandler.airLeak(this.getInputSide());
        teList = this.vacuumHandler.getConnectedPneumatics();
        if (teList.size() == 0) this.vacuumHandler.airLeak(this.getVacuumSide());

    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(this.getPos().getX(), this.getPos().getY(), this.getPos().getZ(), this.getPos().getX() + 1, this.getPos().getY() + 1, this.getPos().getZ() + 1);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        NBTTagCompound vacuum = new NBTTagCompound();
        this.vacuumHandler.writeToNBT(vacuum);
        tag.setTag("vacuum", vacuum);
        tag.setBoolean("turning", this.turning);
        tag.setInteger("redstoneMode", this.redstoneMode);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.vacuumHandler.readFromNBT(tag.getCompoundTag("vacuum"));
        this.turning = tag.getBoolean("turning");
        this.redstoneMode = tag.getInteger("redstoneMode");
    }

    @Override
    public void handleGUIButtonPress(int buttonID, EntityPlayer player) {
        if (buttonID == 0) {
            this.redstoneMode++;
            if (this.redstoneMode > 2) this.redstoneMode = 0;
        }
    }

    @Override
    public void printManometerMessage(EntityPlayer player, List<String> curInfo) {
        curInfo.add(TextFormatting.GREEN + "Input pressure: " + PneumaticCraftUtils.roundNumberTo(this.getAirHandler(this.getInputSide()).getPressure(), 1) + " bar. Vacuum pressure: " + PneumaticCraftUtils.roundNumberTo(this.getAirHandler(this.getVacuumSide()).getPressure(), 1) + " bar.");
    }

    @Override
    public String getName() {
        return Blockss.VACUUM_PUMP.getTranslationKey();
    }

    @Override
    public int getRedstoneMode() {
        return this.redstoneMode;
    }

}
