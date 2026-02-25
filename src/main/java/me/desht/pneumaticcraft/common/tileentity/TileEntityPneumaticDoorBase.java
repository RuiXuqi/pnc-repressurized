package me.desht.pneumaticcraft.common.tileentity;

import com.google.common.collect.ImmutableList;
import me.desht.pneumaticcraft.api.item.IItemRegistry.EnumUpgrade;
import me.desht.pneumaticcraft.common.block.BlockPneumaticDoor;
import me.desht.pneumaticcraft.common.block.Blockss;
import me.desht.pneumaticcraft.common.network.*;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import me.desht.pneumaticcraft.lib.Sounds;
import me.desht.pneumaticcraft.lib.TileEntityConstants;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class TileEntityPneumaticDoorBase extends TileEntityPneumaticBase
        implements IRedstoneControl, IMinWorkingPressure, ICamouflageableTE {
    private static final List<String> REDSTONE_LABELS = ImmutableList.of(
            "gui.tab.redstoneBehaviour.pneumaticDoor.button.playerNearby",
            "gui.tab.redstoneBehaviour.pneumaticDoor.button.playerNearbyAndLooking",
            "gui.tab.redstoneBehaviour.pneumaticDoor.button.woodenDoor"
    );

    public static final int INVENTORY_SIZE = 1;

    private TileEntityPneumaticDoor door;
    private TileEntityPneumaticDoorBase doubleDoor;
    @DescSynced
    public boolean rightGoing;
    public float oldProgress;
    @DescSynced
    @LazySynced
    public float progress;
    @DescSynced
    private boolean opening;
    public boolean wasPowered;
    @DescSynced
    private ItemStack camoStack = ItemStack.EMPTY;
    private IBlockState camoState;
    @GuiSynced
    public int redstoneMode;

    public TileEntityPneumaticDoorBase() {
        super(PneumaticValues.DANGER_PRESSURE_PNEUMATIC_DOOR, PneumaticValues.MAX_PRESSURE_PNEUMATIC_DOOR, PneumaticValues.VOLUME_PNEUMATIC_DOOR, 4);
        this.addApplicableUpgrade(EnumUpgrade.SPEED, EnumUpgrade.RANGE);
    }

    @Override
    public void update() {
        super.update();
        this.oldProgress = this.progress;
        if (!this.getWorld().isRemote) {
            if (this.getPressure() >= PneumaticValues.MIN_PRESSURE_PNEUMATIC_DOOR) {
                if ((this.getWorld().getTotalWorldTime() & 0x3f) == 0) {
                    TileEntity te = this.getWorld().getTileEntity(this.getPos().offset(this.getRotation(), 3));
                    if (te instanceof TileEntityPneumaticDoorBase) {
                        this.doubleDoor = (TileEntityPneumaticDoorBase) te;
                    } else {
                        this.doubleDoor = null;
                    }
                }
                this.setOpening(this.shouldOpen() || this.isNeighborOpening());
                this.setNeighborOpening(this.isOpening());
            } else {
                this.setOpening(true);
            }
        }
        float targetProgress = this.opening ? 1F : 0F;
        float speedMultiplier = this.getSpeedMultiplierFromUpgrades();
        if (this.progress < targetProgress) {
            if (this.progress < targetProgress - TileEntityConstants.PNEUMATIC_DOOR_EXTENSION) {
                this.progress += TileEntityConstants.PNEUMATIC_DOOR_SPEED_FAST * speedMultiplier;
            } else {
                this.progress += TileEntityConstants.PNEUMATIC_DOOR_SPEED_SLOW * speedMultiplier;
            }
            if (this.progress > targetProgress) this.progress = targetProgress;
        }
        if (this.progress > targetProgress) {
            if (this.progress > targetProgress + TileEntityConstants.PNEUMATIC_DOOR_EXTENSION) {
                this.progress -= TileEntityConstants.PNEUMATIC_DOOR_SPEED_FAST * speedMultiplier;
            } else {
                this.progress -= TileEntityConstants.PNEUMATIC_DOOR_SPEED_SLOW * speedMultiplier;
            }
            if (this.progress < targetProgress) this.progress = targetProgress;
        }
        if (!this.getWorld().isRemote) {
            this.addAir((int) (-Math.abs(this.oldProgress - this.progress) * PneumaticValues.USAGE_PNEUMATIC_DOOR * (this.getSpeedUsageMultiplierFromUpgrades() / speedMultiplier)));
        }
        this.door = this.getDoor();
        if (this.door != null) {
            this.door.setRotationAngle(this.progress * 90);
            if (!this.getWorld().isRemote) this.rightGoing = this.door.rightGoing;
        }
    }

    private boolean shouldOpen() {
        switch (this.redstoneMode) {
            case 0:
            case 1:
                int range = TileEntityConstants.RANGE_PNEUMATIC_DOOR_BASE + this.getUpgrades(EnumUpgrade.RANGE);
                AxisAlignedBB aabb = new AxisAlignedBB(this.getPos().getX() - range, this.getPos().getY() - range, this.getPos().getZ() - range, this.getPos().getX() + range + 1, this.getPos().getY() + range + 1, this.getPos().getZ() + range + 1);
                List<EntityPlayer> players = this.getWorld().getEntitiesWithinAABB(EntityPlayer.class, aabb);
                for (EntityPlayer player : players) {
                    if (TileEntitySecurityStation.getProtectingSecurityStations(this.getWorld(), this.getPos(), player, false, false) == 0) {
                        if (this.redstoneMode == 0) {
                            return true;
                        } else {
                            ((BlockPneumaticDoor) Blockss.PNEUMATIC_DOOR).isTrackingPlayerEye = true;
                            BlockPos lookedPosition = PneumaticCraftUtils.getEntityLookedBlock(player, range * 1.41F); //max range = range * sqrt(2).
                            ((BlockPneumaticDoor) Blockss.PNEUMATIC_DOOR).isTrackingPlayerEye = false;
                            if (lookedPosition != null) {
                                if (lookedPosition.equals(new BlockPos(this.getPos().getX(), this.getPos().getY(), this.getPos().getZ()))) {
                                    return true;
                                } else {
                                    if (this.door != null) {
                                        if (lookedPosition.equals(new BlockPos(this.door.getPos().getX(), this.door.getPos().getY(), this.door.getPos().getZ())))
                                            return true;
                                        if (lookedPosition.equals(new BlockPos(this.door.getPos().getX(), this.door.getPos().getY() + (this.door.isTopDoor() ? -1 : 1), this.door.getPos().getZ())))
                                            return true;
                                    }
                                }
                            }
                        }
                    }
                }
                return false;
            case 2:
                return this.opening;
        }
        return false;
    }

    public void setOpening(boolean opening) {
        boolean wasOpening = this.opening;
        this.opening = opening;
        if (this.opening != wasOpening) {
            NetworkHandler.sendToAllAround(new PacketPlaySound(Sounds.PNEUMATIC_DOOR, SoundCategory.BLOCKS, this.getPos(), 1.0F, 1.0F, false), this.getWorld());
            this.sendDescriptionPacket();
        }
    }

    public boolean isOpening() {
        return this.opening;
    }

    private boolean isNeighborOpening() {
        return this.doubleDoor != null && this.doubleDoor.shouldOpen();
    }

    public void setNeighborOpening(boolean opening) {
        if (this.doubleDoor != null && this.doubleDoor.getPressure() >= PneumaticValues.MIN_PRESSURE_PNEUMATIC_DOOR) {
            this.doubleDoor.setOpening(opening);
        }
    }

    @Override
    public boolean isConnectedTo(EnumFacing side) {
        return side != EnumFacing.UP;
    }

    private TileEntityPneumaticDoor getDoor() {
        TileEntity te = this.getWorld().getTileEntity(this.getPos().offset(this.getRotation()).add(0, -1, 0));
        if (te instanceof TileEntityPneumaticDoor) {
            TileEntityPneumaticDoor teDoor = (TileEntityPneumaticDoor) te;
            if (this.getRotation().rotateY() == teDoor.getRotation() && !teDoor.rightGoing) {
                return (TileEntityPneumaticDoor) te;
            } else if (this.getRotation().rotateYCCW() == teDoor.getRotation() && teDoor.rightGoing) {
                return (TileEntityPneumaticDoor) te;
            }
        }
        return null;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.progress = tag.getFloat("extension");
        this.opening = tag.getBoolean("opening");
        this.redstoneMode = tag.getInteger("redstoneMode");
        this.rightGoing = tag.getBoolean("rightGoing");
        this.camoStack = ICamouflageableTE.readCamoStackFromNBT(tag);
        this.camoState = ICamouflageableTE.getStateForStack(this.camoStack);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setFloat("extension", this.progress);
        tag.setBoolean("opening", this.opening);
        tag.setInteger("redstoneMode", this.redstoneMode);
        tag.setBoolean("rightGoing", this.rightGoing);
        ICamouflageableTE.writeCamoStackToNBT(this.camoStack, tag);
        return tag;
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
        return Blockss.PNEUMATIC_DOOR_BASE.getTranslationKey();
    }

    @Override
    public float getMinWorkingPressure() {
        return PneumaticValues.MIN_PRESSURE_PNEUMATIC_DOOR;
    }

    @Override
    public int getRedstoneMode() {
        return this.redstoneMode;
    }

    @Override
    public IBlockState getCamouflage() {
        return this.camoState;
    }

    @Override
    public void setCamouflage(IBlockState state) {
        this.camoState = state;
        this.camoStack = ICamouflageableTE.getStackForState(state);
        this.sendDescriptionPacket();
        this.markDirty();
    }

    @Override
    public void onDescUpdate() {
        this.camoState = ICamouflageableTE.getStateForStack(this.camoStack);

        super.onDescUpdate();
    }

    @Override
    public String getRedstoneTabTitle() {
        return "gui.tab.redstoneBehaviour.pneumaticDoor.openWhen";
    }


    @Override
    protected List<String> getRedstoneButtonLabels() {
        return REDSTONE_LABELS;
    }
}
