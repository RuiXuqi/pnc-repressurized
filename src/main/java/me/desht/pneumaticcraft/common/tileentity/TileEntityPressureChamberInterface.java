package me.desht.pneumaticcraft.common.tileentity;

import me.desht.pneumaticcraft.api.item.IItemRegistry.EnumUpgrade;
import me.desht.pneumaticcraft.common.block.Blockss;
import me.desht.pneumaticcraft.common.inventory.handler.BaseItemStackHandler;
import me.desht.pneumaticcraft.common.network.DescSynced;
import me.desht.pneumaticcraft.common.network.GuiSynced;
import me.desht.pneumaticcraft.common.network.LazySynced;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import me.desht.pneumaticcraft.lib.Sounds;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class TileEntityPressureChamberInterface extends TileEntityPressureChamberWall implements ITickable, IGUITextFieldSensitive, IRedstoneControlled {
    public static final int MAX_PROGRESS = 40;
    public static final int INVENTORY_SIZE = 1;
    private static final int FILTER_SIZE = 9;
    private static final int MIN_SOUND_INTERVAL = 500;  // ticks - the sound effect is ~2.5s long

    @DescSynced
    private final PressureChamberInterfaceHandler inventory = new PressureChamberInterfaceHandler();
    @DescSynced
    @LazySynced
    public int inputProgress;
    public int oldInputProgress;
    @DescSynced
    @LazySynced
    public int outputProgress;
    public int oldOutputProgress;
    @GuiSynced
    public EnumInterfaceMode interfaceMode = EnumInterfaceMode.NONE;
    @GuiSynced
    private boolean enoughAir = true;
    @DescSynced
    public EnumFilterMode filterMode = EnumFilterMode.ITEM;
    @GuiSynced
    public int creativeTabID;
    @DescSynced
    public String itemNameFilter = "";
    private boolean isOpeningI; // used to determine sounds.
    private boolean isOpeningO; // used to determine sounds.
    private int soundTimer;
    @DescSynced
    private boolean shouldOpenInput, shouldOpenOutput;
    @GuiSynced
    public int redstoneMode;
    private int inputTimeOut;
    private int oldItemCount;
    private final PressureChamberFilterHandler filterHandler = new PressureChamberFilterHandler();

    public enum EnumInterfaceMode {
        NONE, IMPORT, EXPORT
    }

    public enum EnumFilterMode {
        ITEM, CREATIVE_TAB, NAME_BEGINS, NAME_CONTAINS
    }

    public TileEntityPressureChamberInterface() {
        super(4);
        this.addApplicableUpgrade(EnumUpgrade.SPEED);
    }

    @Override
    public void update() {
        this.updateImpl();

        boolean wasOpeningI = this.isOpeningI;
        boolean wasOpeningO = this.isOpeningO;
        this.oldInputProgress = this.inputProgress;
        this.oldOutputProgress = this.outputProgress;
        TileEntityPressureChamberValve core = this.getCore();

        if (!this.getWorld().isRemote) {
            int itemCount = this.inventory.getStackInSlot(0).getCount();
            if (this.oldItemCount != itemCount) {
                this.oldItemCount = itemCount;
                this.inputTimeOut = 0;
            }

            this.interfaceMode = this.getInterfaceMode(core);
            this.enoughAir = true;

            if (this.interfaceMode != EnumInterfaceMode.NONE) {
                if (!this.inventory.getStackInSlot(0).isEmpty() && ++this.inputTimeOut > 10) {
                    this.shouldOpenInput = false;
                    if (this.inputProgress == 0) {
                        this.shouldOpenOutput = true;
                        if (this.outputProgress == MAX_PROGRESS) {
                            if (this.interfaceMode == EnumInterfaceMode.IMPORT) {
                                this.outputInChamber();
                            } else {
                                this.exportToInventory();
                            }
                        }
                    }
                } else {
                    this.shouldOpenOutput = false;
                    if (this.outputProgress == 0) {
                        this.shouldOpenInput = true;
                        if (this.interfaceMode == EnumInterfaceMode.EXPORT && this.inputProgress == MAX_PROGRESS && this.redstoneAllows()) {
                            this.importFromChamber(core);
                        }
                    }
                }
            } else {
                this.shouldOpenInput = false;
                this.shouldOpenOutput = false;
            }
        }

        int speed = (int) this.getSpeedMultiplierFromUpgrades();

        if (this.shouldOpenInput) {
            this.inputProgress = Math.min(this.inputProgress + speed, MAX_PROGRESS);
            this.isOpeningI = true;
        } else {
            this.inputProgress = Math.max(this.inputProgress - speed, 0);
            this.isOpeningI = false;
        }

        if (this.shouldOpenOutput) {
            this.outputProgress = Math.min(this.outputProgress + speed, MAX_PROGRESS);
            this.isOpeningO = true;
        } else {
            this.outputProgress = Math.max(this.outputProgress - speed, 0);
            this.isOpeningO = false;
        }

        if (this.getWorld().isRemote && this.soundTimer++ >= MIN_SOUND_INTERVAL && (wasOpeningI != this.isOpeningI || wasOpeningO != this.isOpeningO)) {
            this.getWorld().playSound(this.getPos().getX() + 0.5, this.getPos().getY() + 0.5, this.getPos().getZ() + 0.5, Sounds.INTERFACE_DOOR, SoundCategory.BLOCKS, 0.5F, 1.0F, true);
            this.soundTimer = 0;
        }
    }

    public ItemStack getStackInInterface() {
        return this.inventory.getStackInSlot(0);
    }

    private void exportToInventory() {
        EnumFacing facing = this.getRotation();
        TileEntity te = this.getCachedNeighbor(facing);
        if (te != null) {
            ItemStack stack = this.inventory.getStackInSlot(0);
            int count = stack.getCount();
            ItemStack leftoverStack = PneumaticCraftUtils.exportStackToInventory(te, stack.copy(), facing.getOpposite());
            stack.shrink(count - leftoverStack.getCount());
        }
    }

    private void importFromChamber(TileEntityPressureChamberValve core) {
        ItemStackHandler chamberStacks = core.getStacksInChamber();
        for (int i = 0; i < chamberStacks.getSlots(); i++) {
            ItemStack chamberStack = chamberStacks.getStackInSlot(i);
            if (chamberStack.isEmpty()) {
                continue;
            }
            ItemStack inputStack = this.inventory.getStackInSlot(0);
            if ((inputStack.isEmpty() || inputStack.isItemEqual(chamberStack)) && this.filterHandler.doesItemMatchFilter(chamberStack)) {
                int maxAllowedItems = Math.abs(core.getAirHandler(null).getAir()) / PneumaticValues.USAGE_CHAMBER_INTERFACE;
                if (maxAllowedItems > 0) {
                    if (!inputStack.isEmpty()) {
                        maxAllowedItems = Math.min(maxAllowedItems, chamberStack.getMaxStackSize() - inputStack.getCount());
                    }
                    int transferredItems = Math.min(chamberStack.getCount(), maxAllowedItems);
                    ItemStack toTransferStack = chamberStack.copy().splitStack(transferredItems);
                    ItemStack excess = this.inventory.insertItem(0, toTransferStack, true);
                    if (excess.getCount() < toTransferStack.getCount()) {
                        // we can transfer at least some of the items
                        transferredItems = toTransferStack.getCount() - excess.getCount();
                        core.addAir((core.getAirHandler(null).getAir() > 0 ? -1 : 1) * transferredItems * PneumaticValues.USAGE_CHAMBER_INTERFACE);
                        toTransferStack.setCount(transferredItems);
                        this.inventory.insertItem(0, toTransferStack, false);
                        chamberStacks.extractItem(i, transferredItems, false);
                    }
                }
            }
        }
    }

    private void outputInChamber() {
        // place items from the interface block into the pressure chamber
        // all items in the interface will be moved at once, but the pressure chamber must have enough pressure to do so
        TileEntityPressureChamberValve valve = this.getCore();
        if (valve != null) {
            ItemStack inputStack = this.inventory.getStackInSlot(0);
            this.enoughAir = Math.abs(valve.getAirHandler(null).getAir()) > inputStack.getCount() * PneumaticValues.USAGE_CHAMBER_INTERFACE;
            if (this.enoughAir) {
                ItemStack leftover = ItemHandlerHelper.insertItem(valve.getStacksInChamber(), inputStack.copy(), false);
                int inserted = inputStack.getCount() - leftover.getCount();
                valve.addAir((valve.getAirHandler(null).getAir() > 0 ? -1 : 1) * inserted * PneumaticValues.USAGE_CHAMBER_INTERFACE);
                this.inventory.setStackInSlot(0, leftover);
            }
        }
    }

    // Figure out whether the Interface is exporting or importing.
    private EnumInterfaceMode getInterfaceMode(TileEntityPressureChamberValve core) {
        if (core != null) {
            boolean xMid = this.getPos().getX() != core.multiBlockX && this.getPos().getX() != core.multiBlockX + core.multiBlockSize - 1;
            boolean yMid = this.getPos().getY() != core.multiBlockY && this.getPos().getY() != core.multiBlockY + core.multiBlockSize - 1;
            boolean zMid = this.getPos().getZ() != core.multiBlockZ && this.getPos().getZ() != core.multiBlockZ + core.multiBlockSize - 1;
            EnumFacing rotation = this.getRotation();
            if (xMid && yMid && rotation == EnumFacing.NORTH || xMid && zMid && rotation == EnumFacing.DOWN || yMid && zMid && rotation == EnumFacing.WEST) {
                if (this.getPos().getX() == core.multiBlockX || this.getPos().getY() == core.multiBlockY || this.getPos().getZ() == core.multiBlockZ) {
                    return EnumInterfaceMode.EXPORT;
                } else {
                    return EnumInterfaceMode.IMPORT;
                }
            } else if (xMid && yMid && rotation == EnumFacing.SOUTH || xMid && zMid && rotation == EnumFacing.UP || yMid && zMid && rotation == EnumFacing.EAST) {
                if (this.getPos().getX() == core.multiBlockX || this.getPos().getY() == core.multiBlockY || this.getPos().getZ() == core.multiBlockZ) {
                    return EnumInterfaceMode.IMPORT;
                } else {
                    return EnumInterfaceMode.EXPORT;
                }
            }
        }
        return EnumInterfaceMode.NONE;
    }

    public List<String> getProblemStat() {
        List<String> textList = new ArrayList<>();
        if (this.interfaceMode == EnumInterfaceMode.NONE) {
            textList.addAll(PneumaticCraftUtils.convertStringIntoList("\u00a77The Interface can't work:"));
            textList.addAll(PneumaticCraftUtils.convertStringIntoList("\u00a70\u2022 The Interface is not in a properly formed Pressure Chamber, and/or"));
            textList.addAll(PneumaticCraftUtils.convertStringIntoList("\u00a70\u2022 The Interface is not adjacent to an air block of the Pressure Chamber, and/or"));
            textList.addAll(PneumaticCraftUtils.convertStringIntoList("\u00a70\u2022 The Interface isn't oriented properly"));
        } else if (!this.enoughAir) {
            textList.addAll(PneumaticCraftUtils.convertStringIntoList("\u00a70Not enough pressure in the Pressure Chamber to move the items."));
            textList.addAll(PneumaticCraftUtils.convertStringIntoList("\u00a70Apply more pressure to the Pressure Chamber. The required pressure is dependent on the amount of items being transported."));
        }
        return textList;
    }

    public boolean hasEnoughPressure() {
        return this.enoughAir;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);

        this.inventory.deserializeNBT(tag.getCompoundTag("Items"));
        this.filterHandler.deserializeNBT(tag.getCompoundTag("filter"));
        this.outputProgress = tag.getInteger("outputProgress");
        this.inputProgress = tag.getInteger("inputProgress");
        this.interfaceMode = EnumInterfaceMode.values()[tag.getInteger("interfaceMode")];
        this.filterMode = EnumFilterMode.values()[tag.getInteger("filterMode")];
        this.creativeTabID = tag.getInteger("creativeTabID");
        this.itemNameFilter = tag.getString("itemNameFilter");
        this.redstoneMode = tag.getInteger("redstoneMode");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setTag("Items", this.inventory.serializeNBT());
        tag.setTag("filter", this.filterHandler.serializeNBT());
        tag.setInteger("outputProgress", this.outputProgress);
        tag.setInteger("inputProgress", this.inputProgress);
        tag.setInteger("interfaceMode", this.interfaceMode.ordinal());
        tag.setInteger("filterMode", this.filterMode.ordinal());
        tag.setInteger("creativeTabID", this.creativeTabID);
        tag.setString("itemNameFilter", this.itemNameFilter);
        tag.setInteger("redstoneMode", this.redstoneMode);
        return tag;
    }

    @Override
    public String getName() {
        return Blockss.PRESSURE_CHAMBER_INTERFACE.getTranslationKey();
    }

    @Override
    public boolean isGuiUseableByPlayer(EntityPlayer player) {
        return this.getWorld().getTileEntity(this.getPos()) == this
                && player.getDistanceSq(this.getPos().getX() + 0.5D, this.getPos().getY() + 0.5D, this.getPos().getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void handleGUIButtonPress(int guiID, EntityPlayer player) {
        if (guiID == 1) {
            if (this.filterMode.ordinal() >= EnumFilterMode.values().length - 1) {
                this.filterMode = EnumFilterMode.ITEM;
            } else {
                this.filterMode = EnumFilterMode.values()[this.filterMode.ordinal() + 1];
            }
            //when an SideOnly exception is thrown this method automatically will set the filter mode to Item.
            this.filterHandler.doesItemMatchFilter(new ItemStack(Items.STICK));

        } else if (guiID == 2) {
            this.creativeTabID++;
            if (this.creativeTabID == 5 || this.creativeTabID == 11) this.creativeTabID++;
            if (this.creativeTabID >= CreativeTabs.CREATIVE_TAB_ARRAY.length) {
                this.creativeTabID = 0;
            }
        } else if (guiID == 0) {
            this.redstoneMode++;
            if (this.redstoneMode > 2) this.redstoneMode = 0;
        }
    }

    @Override
    public void setText(int textFieldID, String text) {
        this.itemNameFilter = text;
    }

    @Override
    public String getText(int textFieldID) {
        return this.itemNameFilter;
    }

    @Override
    public int getRedstoneMode() {
        return this.redstoneMode;
    }

    @Override
    public IItemHandlerModifiable getPrimaryInventory() {
        return this.inventory;
    }

    public IItemHandler getFilterHandler() {
        return this.filterHandler;
    }

    private class PressureChamberFilterHandler extends ItemStackHandler {
        PressureChamberFilterHandler() {
            super(FILTER_SIZE);
        }

        boolean doesItemMatchFilter(ItemStack itemStack) {
            if (itemStack.isEmpty()) return true;

            switch (TileEntityPressureChamberInterface.this.filterMode) {
                case ITEM:
                    boolean filterEmpty = true;
                    for (int i = 0; i < 9; i++) {
                        ItemStack filterStack = this.getStackInSlot(i);
                        if (!filterStack.isEmpty()) {
                            filterEmpty = false;
                            if (itemStack.isItemEqual(filterStack)) {
                                return true;
                            }
                        }
                    }
                    return filterEmpty;
                case CREATIVE_TAB:
                    try {
                        int itemCreativeTabIndex = itemStack.getItem().getCreativeTab() != null ? itemStack.getItem().getCreativeTab().getIndex() : -1;
                        if (itemCreativeTabIndex == TileEntityPressureChamberInterface.this.creativeTabID) {
                            return true;
                        }
                    } catch (Throwable e) {
                        //when we are SMP getCreativeTab() is client only.
                        TileEntityPressureChamberInterface.this.filterMode = EnumFilterMode.NAME_BEGINS;
                    }
                    return false;
                case NAME_BEGINS:
                    return itemStack.getDisplayName().toLowerCase().startsWith(TileEntityPressureChamberInterface.this.itemNameFilter.toLowerCase());
                case NAME_CONTAINS:
                    return itemStack.getDisplayName().toLowerCase().contains(TileEntityPressureChamberInterface.this.itemNameFilter.toLowerCase());
            }
            return false;
        }
    }

    private class PressureChamberInterfaceHandler extends BaseItemStackHandler {
        PressureChamberInterfaceHandler() {
            super(TileEntityPressureChamberInterface.this, INVENTORY_SIZE);
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            return TileEntityPressureChamberInterface.this.inputProgress == MAX_PROGRESS ? super.insertItem(slot, stack, simulate) : stack;
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return TileEntityPressureChamberInterface.this.outputProgress == MAX_PROGRESS ? super.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (!TileEntityPressureChamberInterface.this.getWorld().isRemote && slot == 0) {
                TileEntityPressureChamberInterface.this.sendDescriptionPacket();
            }
        }
    }
}
