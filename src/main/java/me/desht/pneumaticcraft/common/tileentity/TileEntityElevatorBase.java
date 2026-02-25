package me.desht.pneumaticcraft.common.tileentity;

import com.google.common.collect.ImmutableList;
import me.desht.pneumaticcraft.api.item.IItemRegistry.EnumUpgrade;
import me.desht.pneumaticcraft.api.tileentity.IAirHandler;
import me.desht.pneumaticcraft.api.tileentity.IAirListener;
import me.desht.pneumaticcraft.client.sound.MovingSounds;
import me.desht.pneumaticcraft.client.util.ClientUtils;
import me.desht.pneumaticcraft.common.block.BlockElevatorBase;
import me.desht.pneumaticcraft.common.block.Blockss;
import me.desht.pneumaticcraft.common.config.ConfigHandler;
import me.desht.pneumaticcraft.common.network.*;
import me.desht.pneumaticcraft.common.thirdparty.computercraft.LuaMethod;
import me.desht.pneumaticcraft.common.thirdparty.computercraft.LuaMethodRegistry;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.Log;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import me.desht.pneumaticcraft.lib.Sounds;
import me.desht.pneumaticcraft.lib.TileEntityConstants;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemHandlerHelper;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class TileEntityElevatorBase extends TileEntityPneumaticBase
        implements IGUITextFieldSensitive, IRedstoneControlled, IMinWorkingPressure, IAirListener, ICamouflageableTE {

    private static final List<String> REDSTONE_LABELS = ImmutableList.of(
            "gui.tab.redstoneBehaviour.elevator.button.redstone",
            "gui.tab.redstoneBehaviour.elevator.button.elevatorCallers"
    );

    @DescSynced
    public final boolean[] sidesConnected = new boolean[6];
    public float oldExtension;
    @DescSynced
    @LazySynced
    public float extension;
    @DescSynced
    private float targetExtension;
    private boolean isStopped = true;  //used for sounds
    private TileEntityElevatorBase coreElevator;
    private List<TileEntityElevatorBase> multiElevators; //initialized when multiple elevators are connected in a multiblock manner.
    @DescSynced
    public int multiElevatorCount;
    @GuiSynced
    private int redstoneMode;
    public int[] floorHeights = new int[0]; //list of every floor of Elevator Callers.
    private HashMap<Integer, String> floorNames = new HashMap<>();
    @GuiSynced
    private int maxFloorHeight;
    private int redstoneInputLevel; // current redstone input level
    @DescSynced
    private ItemStack camoStack = ItemStack.EMPTY;
    private IBlockState camoState;
    public double[] fakeFloorTextureUV;

    public TileEntityElevatorBase() {
        super(PneumaticValues.DANGER_PRESSURE_ELEVATOR, PneumaticValues.MAX_PRESSURE_ELEVATOR, PneumaticValues.VOLUME_ELEVATOR, 4);
        this.addApplicableUpgrade(EnumUpgrade.SPEED, EnumUpgrade.CHARGING);
    }

    @Override
    public void update() {
        this.oldExtension = this.extension;
        if (this.getWorld().isRemote && (this.getWorld().getTotalWorldTime() & 0x3f) == 0)
            this.coreElevator = null;//reset this because the client doesn't get notified of neighbor block updates.
        if (this.isCoreElevator()) {
            super.update();
            if (!this.getWorld().isRemote && this.isControlledByRedstone()) {
                float oldTargetExtension = this.targetExtension;
                float maxExtension = this.getMaxElevatorHeight();

                int redstoneInput = this.redstoneInputLevel;
                if (this.multiElevators != null) {
                    for (TileEntityElevatorBase base : this.multiElevators) {
                        redstoneInput = Math.max(this.redstoneInputLevel, base.redstoneInputLevel);
                    }
                }

                this.targetExtension = redstoneInput * maxExtension / 15;
                if (this.targetExtension > this.oldExtension && this.getPressure() < PneumaticValues.MIN_PRESSURE_ELEVATOR)
                    this.targetExtension = this.oldExtension; // only ascent when there's enough pressure
                if (oldTargetExtension != this.targetExtension) this.sendDescPacketFromAllElevators();
            }
            float speedMultiplier = this.getSpeedMultiplierFromUpgrades();
            if (this.getWorld().isRemote) {
                speedMultiplier = (float) (speedMultiplier * PacketServerTickTime.tickTimeMultiplier);
            }

            SoundEvent soundName = null;
            if (this.extension < this.targetExtension) {
                if (!this.getWorld().isRemote && this.getPressure() < PneumaticValues.MIN_PRESSURE_ELEVATOR) {
                    this.targetExtension = this.extension;
                    this.sendDescriptionPacket(256D);
                }

                float moveBy;
                if (this.extension < this.targetExtension - TileEntityConstants.ELEVATOR_SLOW_EXTENSION) {
                    moveBy = TileEntityConstants.ELEVATOR_SPEED_FAST * speedMultiplier;
                } else {
                    moveBy = TileEntityConstants.ELEVATOR_SPEED_SLOW * speedMultiplier;
                }
                if (this.extension + moveBy > this.targetExtension) {
                    this.extension = this.targetExtension;
                    if (!this.getWorld().isRemote) this.updateFloors();
                }
                if (this.isStopped) {
                    soundName = Sounds.ELEVATOR_START;
                    this.isStopped = false;
                    if (!this.world.isRemote) {
                        NetworkRegistry.TargetPoint tp = new NetworkRegistry.TargetPoint(this.world.provider.getDimension(), this.pos.getX(), this.pos.getY(), this.pos.getZ(), 32);
                        NetworkHandler.sendToAllAround(new PacketPlayMovingSound(MovingSounds.Sound.ELEVATOR, this.getCoreElevator()), tp);
                    }
                }
                float startingExtension = this.extension;

                while (this.extension < startingExtension + moveBy) {
                    this.extension += TileEntityConstants.ELEVATOR_SPEED_SLOW;
                    /*
                    if(extension > startingExtension + moveBy) {
                        extension = startingExtension + moveBy;
                    }
                    */
                    // moveEntities(TileEntityConstants.ELEVATOR_SPEED_SLOW);
                }
                this.addAir((int) ((this.oldExtension - this.extension) * PneumaticValues.USAGE_ELEVATOR * (this.getSpeedUsageMultiplierFromUpgrades() / speedMultiplier)));// substract the ascended distance from the air reservoir.
            }
            if (this.extension > this.targetExtension) {
                float chargingSlowdown = 1.0f - Math.min(4, this.getUpgrades(EnumUpgrade.CHARGING)) * 0.1f;
                if (this.extension > this.targetExtension + TileEntityConstants.ELEVATOR_SLOW_EXTENSION) {
                    this.extension -= TileEntityConstants.ELEVATOR_SPEED_FAST * speedMultiplier * chargingSlowdown;
                } else {
                    this.extension -= TileEntityConstants.ELEVATOR_SPEED_SLOW * speedMultiplier * chargingSlowdown;
                }
                if (this.extension < this.targetExtension) {
                    this.extension = this.targetExtension;
                    if (!this.getWorld().isRemote) this.updateFloors();
                }
                if (this.isStopped) {
                    soundName = Sounds.ELEVATOR_START;
                    this.isStopped = false;
                    if (!this.world.isRemote) {
                        NetworkRegistry.TargetPoint tp = new NetworkRegistry.TargetPoint(this.world.provider.getDimension(), this.pos.getX(), this.pos.getY(), this.pos.getZ(), 32);
                        NetworkHandler.sendToAllAround(new PacketPlayMovingSound(MovingSounds.Sound.ELEVATOR, this.getCoreElevator()), tp);
                    }
                }
                if (this.getUpgrades(EnumUpgrade.CHARGING) > 0) {
                    float mul = 0.15f * Math.min(4, this.getUpgrades(EnumUpgrade.CHARGING));
                    this.addAir((int) ((this.oldExtension - this.extension) * PneumaticValues.USAGE_ELEVATOR * mul * (this.getSpeedUsageMultiplierFromUpgrades() / speedMultiplier)));
                }
                //  movePlayerDown();
            }
            if (this.oldExtension == this.extension && !this.isStopped) {
                soundName = Sounds.ELEVATOR_STOP;
                this.isStopped = true;
            }

            if (soundName != null && this.getWorld().isRemote) {
                this.getWorld().playSound(this.getPos().getX() + 0.5, this.getPos().getY() + 0.5, this.getPos().getZ() + 0.5, soundName, SoundCategory.BLOCKS, 0.5F, 1.0F, true);
            }

        } else {
            this.extension = 0;
        }
        if (!this.getWorld().isRemote && this.oldExtension != this.extension) {
            this.sendDescriptionPacket(256);
        }
    }

    @Override
    public void handleGUIButtonPress(int buttonID, EntityPlayer player) {
        if (buttonID == 0) {
            this.redstoneMode++;
            if (this.redstoneMode > 1) this.redstoneMode = 0;

            if (this.multiElevators != null) {
                for (TileEntityElevatorBase base : this.multiElevators) {
                    while (base.redstoneMode != this.redstoneMode) {
                        base.handleGUIButtonPress(buttonID, player);
                    }
                }
            }

            int i = -1;
            TileEntity te = this.getWorld().getTileEntity(this.getPos().offset(EnumFacing.DOWN));
            while (te instanceof TileEntityElevatorBase) {
                ((TileEntityElevatorBase) te).redstoneMode = this.redstoneMode;
                i--;
                te = this.getWorld().getTileEntity(this.getPos().add(0, i, 0));
            }
        }
    }

    private boolean isControlledByRedstone() {
        return this.redstoneMode == 0;
    }

    @Override
    public boolean redstoneAllows() {
        return true;
    }

    private void updateRedstoneInputLevel() {
        if (this.multiElevators == null) return;

        int maxRedstone = 0;
        for (TileEntityElevatorBase base : this.multiElevators) {
            int i = 0;
            while (this.getWorld().getBlockState(base.getPos().add(0, i, 0)).getBlock() == Blockss.ELEVATOR_BASE) {
                maxRedstone = Math.max(maxRedstone, PneumaticCraftUtils.getRedstoneLevel(this.getWorld(), base.getPos().add(0, i, 0)));
                i--;
            }
        }
        for (TileEntityElevatorBase base : this.multiElevators) {
            base.redstoneInputLevel = maxRedstone;
        }
    }

    public float getMaxElevatorHeight() {
        int max = this.maxFloorHeight;
        if (this.multiElevators != null) {
            for (TileEntityElevatorBase base : this.multiElevators) {
                max = Math.max(max, base.maxFloorHeight);
            }
        }
        return max;
    }

    public void updateMaxElevatorHeight() {
        int i = -1;
        do {
            i++;
        } while (this.getWorld().getBlockState(this.getPos().add(0, i + 1, 0)).getBlock() == Blockss.ELEVATOR_FRAME);
        int elevatorBases = 0;
        do {
            elevatorBases++;
        } while (this.getWorld().getBlockState(this.getPos().add(0, -elevatorBases, 0)).getBlock() == Blockss.ELEVATOR_BASE);

        this.maxFloorHeight = Math.min(i, elevatorBases * ConfigHandler.machineProperties.elevatorBaseBlocksPerBase);
    }

    // NBT methods-----------------------------------------------
    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.extension = tag.getFloat("extension");
        this.targetExtension = tag.getFloat("targetExtension");
        this.redstoneMode = tag.getInteger("redstoneMode");
        if (!tag.hasKey("maxFloorHeight")) {//backwards compatibility implementation.
            this.updateMaxElevatorHeight();
        } else {
            this.maxFloorHeight = tag.getInteger("maxFloorHeight");
        }
        for (int i = 0; i < 6; i++) {
            this.sidesConnected[i] = tag.getBoolean("sideConnected" + i);
        }
        this.camoStack = ICamouflageableTE.readCamoStackFromNBT(tag);
        this.camoState = ICamouflageableTE.getStateForStack(this.camoStack);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setFloat("extension", this.extension);
        tag.setFloat("targetExtension", this.targetExtension);
        tag.setInteger("redstoneMode", this.redstoneMode);
        tag.setInteger("maxFloorHeight", this.maxFloorHeight);
        for (int i = 0; i < 6; i++) {
            tag.setBoolean("sideConnected" + i, this.sidesConnected[i]);
        }
        ICamouflageableTE.writeCamoStackToNBT(this.camoStack, tag);
        return tag;
    }

    @Override
    public void readFromPacket(NBTTagCompound tag) {
        super.readFromPacket(tag);
        this.floorHeights = tag.getIntArray("floorHeights");

        this.floorNames.clear();
        NBTTagList floorNameList = tag.getTagList("floorNames", 10);
        for (int i = 0; i < floorNameList.tagCount(); i++) {
            NBTTagCompound floorName = floorNameList.getCompoundTagAt(i);
            this.floorNames.put(floorName.getInteger("floorHeight"), floorName.getString("floorName"));
        }
    }

    @Override
    public void writeToPacket(NBTTagCompound tag) {
        super.writeToPacket(tag);
        tag.setIntArray("floorHeights", this.floorHeights);

        NBTTagList floorNameList = new NBTTagList();
        for (int key : this.floorNames.keySet()) {
            NBTTagCompound floorNameTag = new NBTTagCompound();
            floorNameTag.setInteger("floorHeight", key);
            floorNameTag.setString("floorName", this.floorNames.get(key));
            floorNameList.appendTag(floorNameTag);
        }
        tag.setTag("floorNames", floorNameList);
    }

    @Override
    public void onNeighborTileUpdate() {
        super.onNeighborTileUpdate();
        this.updateConnections();
    }

    private void connectAsMultiblock() {
        this.multiElevators = null;
        if (this.isCoreElevator()) {
            this.multiElevators = new ArrayList<>();
            Stack<TileEntityElevatorBase> todo = new Stack<>();
            todo.add(this);
            while (!todo.isEmpty()) {
                TileEntityElevatorBase curElevator = todo.pop();
                if (curElevator.isCoreElevator() && !this.multiElevators.contains(curElevator)) {
                    this.multiElevators.add(curElevator);
                    curElevator.multiElevators = this.multiElevators;
                    for (EnumFacing face : EnumFacing.HORIZONTALS) {
                        TileEntity te = curElevator.getCachedNeighbor(face);
                        if (te instanceof TileEntityElevatorBase && !te.isInvalid()) {
                            todo.push((TileEntityElevatorBase) te);
                        }
                    }
                }
            }
            this.multiElevatorCount = this.multiElevators.size();
        }
    }

    @Override
    public void onNeighborBlockUpdate() {
        super.onNeighborBlockUpdate();
        this.getCoreElevator().updateRedstoneInputLevel();
        this.connectAsMultiblock();
        this.updateConnections();
    }

    @Override
    public void onDescUpdate() {
        IBlockState oldCamo = this.camoState;
        this.camoState = ICamouflageableTE.getStateForStack(this.camoStack);
        if (oldCamo != this.camoState) {
            // cache the UV's for the camouflaged texture (top face of the camo block)
            // for efficiently rendering it on the moving elevator floor
            this.fakeFloorTextureUV = ClientUtils.getTextureUV(this.camoState, EnumFacing.UP);
        }

        super.onDescUpdate();
    }

    private void updateConnections() {
        List<Pair<EnumFacing, IAirHandler>> connections = this.getAirHandler(null).getConnectedPneumatics();
        Arrays.fill(this.sidesConnected, false);
        for (Pair<EnumFacing, IAirHandler> entry : connections) {
            this.sidesConnected[entry.getKey().ordinal()] = true;
        }

        if (this.getWorld().getBlockState(this.getPos().offset(EnumFacing.UP)).getBlock() != Blockss.ELEVATOR_BASE) {
            this.coreElevator = this;
            int i = -1;
            TileEntity te = this.getWorld().getTileEntity(this.getPos().offset(EnumFacing.DOWN));
            while (te instanceof TileEntityElevatorBase) {
                ((TileEntityElevatorBase) te).coreElevator = this;
                i--;
                te = this.getWorld().getTileEntity(this.getPos().add(0, i, 0));
            }
        }
    }

    public void moveInventoryToThis() {
        TileEntity te = this.getWorld().getTileEntity(this.getPos().offset(EnumFacing.UP));
        if (te instanceof TileEntityElevatorBase) {
            this.camoStack = ((TileEntityElevatorBase) te).camoStack;
            this.sendDescriptionPacket();
            for (int i = 0; i < this.upgradeHandler.getSlots(); i++) {
                ItemStack stack = ((TileEntityElevatorBase) te).getUpgradesInventory().getStackInSlot(i);
                ItemStack excess = ItemHandlerHelper.insertItem(this.upgradeHandler, stack, false);
                if (!excess.isEmpty()) PneumaticCraftUtils.dropItemOnGround(excess, this.world, this.getPos());
                ((TileEntityElevatorBase) te).getUpgradesInventory().setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    public void updateFloors() {
        List<Integer> floorList = new ArrayList<>();
        List<BlockPos> callerList = new ArrayList<>();

        if (this.multiElevators != null) {
            int yOffset = 0;
            boolean shouldBreak = false;
            while (!shouldBreak) {
                boolean registeredThisFloor = false;
                for (TileEntityElevatorBase base : this.multiElevators) {
                    for (EnumFacing dir : EnumFacing.HORIZONTALS) {
                        BlockPos checkPos = base.getPos().offset(dir).up(yOffset + 2);
                        if (base.world.getBlockState(checkPos).getBlock() == Blockss.ELEVATOR_CALLER) {
                            callerList.add(checkPos);
                            if (!registeredThisFloor) floorList.add(yOffset);
                            registeredThisFloor = true;
                        }
                    }
                }

                yOffset++;
                for (TileEntityElevatorBase base : this.multiElevators) {
                    if (base.world.getBlockState(base.getPos().up(yOffset)).getBlock() != Blockss.ELEVATOR_FRAME) {
                        shouldBreak = true;
                        break;
                    }
                }
            }

            for (TileEntityElevatorBase base : this.multiElevators) {
                base.floorHeights = floorList.stream().mapToInt(Integer::intValue).toArray();
            }
        }

        double buttonHeight = 0.06D;
        double buttonSpacing = 0.02D;
        TileEntityElevatorCaller.ElevatorButton[] elevatorButtons = new TileEntityElevatorCaller.ElevatorButton[this.floorHeights.length];
        int columns = (elevatorButtons.length - 1) / 12 + 1;
        for (int j = 0; j < columns; j++) {
            for (int i = j * 12; i < this.floorHeights.length && i < j * 12 + 12; i++) {
                elevatorButtons[i] = new TileEntityElevatorCaller.ElevatorButton(0.2D + 0.6D / columns * j, 0.5D + (Math.min(this.floorHeights.length, 12) - 2) * (buttonSpacing + buttonHeight) / 2 - i % 12 * (buttonHeight + buttonSpacing), 0.58D / columns, buttonHeight, i, this.floorHeights[i]);
                elevatorButtons[i].setColor(this.floorHeights[i] == this.targetExtension ? 0 : 1, 1, this.floorHeights[i] == this.targetExtension ? 0 : 1);
                String floorName = this.floorNames.get(this.floorHeights[i]);
                if (floorName != null) {
                    elevatorButtons[i].buttonText = floorName;
                } else {
                    this.floorNames.put(this.floorHeights[i], elevatorButtons[i].buttonText);
                }
            }
        }

        if (this.multiElevators != null) {
            for (TileEntityElevatorBase base : this.multiElevators) {
                base.floorNames = new HashMap<>(this.floorNames);
            }
        }

        for (BlockPos p : callerList) {
            TileEntity te = this.getWorld().getTileEntity(p);
            if (te instanceof TileEntityElevatorCaller) {
                int callerFloorHeight = p.getY() - this.getPos().getY() - 2;
                int callerFloor = -1;
                for (TileEntityElevatorCaller.ElevatorButton floor : elevatorButtons) {
                    if (floor.floorHeight == callerFloorHeight) {
                        callerFloor = floor.floorNumber;
                        break;
                    }
                }
                if (callerFloor == -1) {
                    Log.error("Error while updating elevator floors! This will cause a indexOutOfBoundsException, index = -1");
                }
                ((TileEntityElevatorCaller) te).setEmittingRedstone(PneumaticCraftUtils.areFloatsEqual(this.targetExtension, this.extension, 0.1F) && PneumaticCraftUtils.areFloatsEqual(this.extension, callerFloorHeight, 0.1F));
                ((TileEntityElevatorCaller) te).setFloors(elevatorButtons, callerFloor);
            }
        }
    }

    public void goToFloor(int floor) {
        if (this.getCoreElevator().isControlledByRedstone()) this.getCoreElevator().handleGUIButtonPress(0, null);
        if (floor >= 0 && floor < this.floorHeights.length) this.setTargetHeight(this.floorHeights[floor]);
        this.updateFloors();
        this.sendDescPacketFromAllElevators();
    }

    private void setTargetHeight(float height) {
        height = Math.min(height, this.getMaxElevatorHeight());
        if (this.multiElevators != null) {
            for (TileEntityElevatorBase base : this.multiElevators) {
                base.targetExtension = height;
            }
        }
    }

    public float getTargetExtension() {
        return this.targetExtension;
    }

    private void sendDescPacketFromAllElevators() {
        if (this.multiElevators != null) {
            for (TileEntityElevatorBase base : this.multiElevators) {
                base.sendDescriptionPacket(256);
            }
        } else {
            this.sendDescriptionPacket(256);
        }
    }

    @Override
    public String getName() {
        return Blockss.ELEVATOR_BASE.getTranslationKey();
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(this.getPos().getX(), this.getPos().getY(), this.getPos().getZ(), this.getPos().getX() + 1, this.getPos().getY() + 1 + this.extension, this.getPos().getZ() + 1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() {
        return 65536D;
    }

    private TileEntityElevatorBase getCoreElevator() {
        if (this.coreElevator == null) {
            this.coreElevator = BlockElevatorBase.getCoreTileEntity(this.getWorld(), this.getPos());
        }
        return this.coreElevator;
    }

    public boolean isCoreElevator() {
        return this.getCoreElevator() == this;
    }

    @Override
    public boolean isConnectedTo(EnumFacing side) {
        return side != EnumFacing.UP && side != EnumFacing.DOWN || this.getWorld().getBlockState(this.getPos().offset(side)).getBlock() != Blockss.ELEVATOR_BASE;
    }

    @Override
    public IAirHandler getAirHandler(EnumFacing sideRequested) {
        if (this.isCoreElevator()) {
            return super.getAirHandler(sideRequested);
        } else {
            return this.getCoreElevator().getAirHandler(sideRequested);
        }
    }

    @Override
    public void addConnectedPneumatics(List<Pair<EnumFacing, IAirHandler>> connectedMachines) {
        TileEntity te = this.getTileCache()[EnumFacing.DOWN.ordinal()].getTileEntity();
        if (te instanceof TileEntityElevatorBase) {
            connectedMachines.addAll(((TileEntityElevatorBase) te).airHandler.getConnectedPneumatics());
        }
    }

    @Override
    public void onAirDispersion(IAirHandler handler, EnumFacing dir, int airAdded) {
    }

    @Override
    public int getMaxDispersion(IAirHandler handler, EnumFacing dir) {
        return Integer.MAX_VALUE;
    }

    @Override
    public void setText(int textFieldID, String text) {
        this.setFloorName(textFieldID, text);
    }

    @Override
    public String getText(int textFieldID) {
        return this.getFloorName(textFieldID);
    }

    public String getFloorName(int floor) {
        return floor < this.floorHeights.length ? this.floorNames.get(this.floorHeights[floor]) : "";
    }

    public void setFloorName(int floor, String name) {
        if (floor < this.floorHeights.length) {
            this.floorNames.put(this.floorHeights[floor], name);
            this.updateFloors();
        }
    }

    @Override
    public boolean isGuiUseableByPlayer(EntityPlayer par1EntityPlayer) {
        return this.getWorld().getTileEntity(this.getPos()) == this;
    }

    /*
     * COMPUTERCRAFT API
     */

    @Override
    public String getType() {
        return "elevator";
    }

    @Override
    protected void addLuaMethods(LuaMethodRegistry registry) {
        super.addLuaMethods(registry);

        registry.registerLuaMethod(new LuaMethod("setHeight") {
            @Override
            public Object[] call(Object[] args) {
                this.requireArgs(args, 1, "height (in blocks)");
                TileEntityElevatorBase.this.setTargetHeight(((Double) args[0]).floatValue());
                if (TileEntityElevatorBase.this.getCoreElevator().isControlledByRedstone()) {
                    TileEntityElevatorBase.this.getCoreElevator().handleGUIButtonPress(0, null);
                }
                TileEntityElevatorBase.this.getCoreElevator().sendDescPacketFromAllElevators();
                return null;
            }
        });

        registry.registerLuaMethod(new LuaMethod("getCurrentHeight") {
            @Override
            public Object[] call(Object[] args) {
                this.requireNoArgs(args);
                return new Object[]{TileEntityElevatorBase.this.getCoreElevator().extension};
            }
        });
        registry.registerLuaMethod(new LuaMethod("getTargetHeight") {
            @Override
            public Object[] call(Object[] args) {
                this.requireNoArgs(args);
                return new Object[]{TileEntityElevatorBase.this.getCoreElevator().targetExtension};
            }
        });

        registry.registerLuaMethod(new LuaMethod("setExternalControl") {
            @Override
            public Object[] call(Object[] args) {
                this.requireArgs(args, 1, "true/false");
                if ((Boolean) args[0] && TileEntityElevatorBase.this.getCoreElevator().isControlledByRedstone()
                        || !(Boolean) args[0] && !TileEntityElevatorBase.this.getCoreElevator().isControlledByRedstone()) {
                    TileEntityElevatorBase.this.getCoreElevator().handleGUIButtonPress(0, null);
                }
                return null;
            }
        });
    }

    @Override
    public int getRedstoneMode() {
        return this.redstoneMode;
    }

    @Override
    public float getMinWorkingPressure() {
        return PneumaticValues.MIN_PRESSURE_ELEVATOR;
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
    public String getRedstoneTabTitle() {
        return "gui.tab.redstoneBehaviour.elevator.controlBy";
    }

    @Override
    protected List<String> getRedstoneButtonLabels() {
        return REDSTONE_LABELS;
    }
}
