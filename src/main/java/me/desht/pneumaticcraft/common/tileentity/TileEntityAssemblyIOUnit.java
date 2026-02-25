package me.desht.pneumaticcraft.common.tileentity;

import me.desht.pneumaticcraft.common.inventory.handler.BaseItemStackHandler;
import me.desht.pneumaticcraft.common.network.DescSynced;
import me.desht.pneumaticcraft.common.network.LazySynced;
import me.desht.pneumaticcraft.common.recipes.AssemblyRecipe;
import me.desht.pneumaticcraft.common.recipes.programs.AssemblyProgram;
import me.desht.pneumaticcraft.common.util.IOHelper;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.TileEntityConstants;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import java.util.List;

public class TileEntityAssemblyIOUnit extends TileEntityAssemblyRobot {
    private static final int INVENTORY_SIZE = 1;

    @DescSynced
    private boolean shouldClawClose;
    @DescSynced
    @LazySynced
    public float clawProgress;
    public float oldClawProgress;
    @DescSynced
    private ItemStackHandler inventory = new BaseItemStackHandler(this, INVENTORY_SIZE);
    private List<AssemblyRecipe> recipeList;
    private ItemStack searchedItemStack = ItemStack.EMPTY;
    private byte state = 0;
    private byte tickCounter = 0;
    private boolean hasSwitchedThisTick;
    @DescSynced
    private boolean exporting;

    private final static byte SLEEP_TICKS = 50;

    private final static byte STATE_IDLE = 0;
    private final static byte STATE_SEARCH_SRC = 1;
    private final static byte STATE_CLOSECLAW_AFTER_PICKUP = 5;
    private final static byte STATE_RESET_CLOSECLAW_AFTER_PICKUP = 20;
    private final static byte STATE_RESET_GOTO_IDLE = 26;
    private final static byte STATE_MAX = 127;

    @Override
    public void update() {
        super.update();
        this.hasSwitchedThisTick = false;
        if (this.getWorld().isRemote) {
            if (!this.isClawDone()) this.moveClaw();
        } else {
            this.slowMode = false;
            switch (this.state) {
                case STATE_IDLE:
                    break;
                case STATE_SEARCH_SRC:
                    if (this.findPickupLocation()) this.state++;
                    break;
                // rise to the right height for target location
                case 2: // for pickup
                case 7: // for drop-off
                case 22: // for reset
                    if (this.hoverOverTarget()) this.state++;
                    break;
                // turn and move to target
                case 3: // for pickup
                case 8: // for drop-off
                case 23: // for reset
                    this.slowMode = true;
                    if (this.gotoTarget()) this.state++;
                    break;
                case 4: // pickup item - need to pick up before closeClaw; claw needs to know item size to 'grab' it!
                    if (this.getItemFromCurrentDirection()) this.state++;
                    break;
                case STATE_CLOSECLAW_AFTER_PICKUP:
                case STATE_RESET_CLOSECLAW_AFTER_PICKUP:
                    if (this.closeClaw()) this.state++;
                    break;
                case 6:
                case 21:
                    if (this.findDropOffLocation()) this.state++;
                    break;
                case 9:
                case 24:
                    if (this.openClaw()) this.state++;
                    break;
                case 10: // drop off item
                case 25:
                    if (this.putItemToCurrentDirection()) this.state++;
                    break;
                case 11:
                case STATE_RESET_GOTO_IDLE:
                    if (this.gotoIdlePos()) this.state = 0;
                case STATE_MAX: // this will be set if we encounter an unknown state; prevents log-spam that would result from default-case
                    break;
                default:
                    System.out.printf("unexpected state: %d%n", this.state);
                    this.state = STATE_MAX;
                    break;
            }
        }
    }

    public ItemStack getHeldStack() {
        return this.inventory.getStackInSlot(0);
    }

    @Override
    public boolean reset() {
        if (this.state >= STATE_RESET_CLOSECLAW_AFTER_PICKUP) {
            return false;
        } else if (!this.inventory.getStackInSlot(0).isEmpty()) {
            this.state = STATE_RESET_CLOSECLAW_AFTER_PICKUP;
            return false;
        } else if (this.state == STATE_IDLE) {
            return true;
        } else {
            this.state = STATE_RESET_GOTO_IDLE;
            return this.isIdle();
        }
    }

    /**
     * @return true if the controller should use air and display 'running'
     */
    public boolean pickupItem(List<AssemblyRecipe> list) {
        this.recipeList = list;

        if (this.state == STATE_IDLE) this.state++;

        // don't use air while waiting for item/inventory to be available
        return this.state > STATE_IDLE && !this.isSleeping() && this.state < STATE_MAX;
    }

    private boolean gotoIdlePos() {
        this.gotoHomePosition();
        return this.isDoneInternal();
    }

    private boolean findPickupLocation() {
        if (this.shouldSleep()) return false;

        EnumFacing[] inventoryDir = null;

        if (this.isImportUnit()) {
            this.searchedItemStack = ItemStack.EMPTY;
            if (this.recipeList != null) {
                for (AssemblyRecipe recipe : this.recipeList) {
                    inventoryDir = this.getInventoryDirectionForItem(recipe.getInput());
                    if (inventoryDir != null) {
                        this.searchedItemStack = recipe.getInput();
                        break;
                    }
                }
            }
        } else {
            inventoryDir = this.getPlatformDirection();
        }

        this.targetDirection = inventoryDir;

        if (this.targetDirection == null) {
            this.sleepBeforeNextSearch();

            return false;
        } else return true;
    }

    private boolean isSleeping() {
        return this.tickCounter > 0;
    }

    private boolean shouldSleep() {
        if (this.tickCounter > 0 && this.tickCounter++ < SLEEP_TICKS) {
            return true;
        } else {
            this.tickCounter = 0;
            return false;
        }
    }

    private void sleepBeforeNextSearch() {
        this.tickCounter = 1;
    }

    private boolean findDropOffLocation() {
        if (this.shouldSleep()) {
            return false;
        }
        this.targetDirection = this.isImportUnit() ? this.getPlatformDirection() : this.getExportLocationForItem(this.inventory.getStackInSlot(0));
        if (this.targetDirection == null) {
            this.sleepBeforeNextSearch();
            return false;
        } else {
            return true;
        }
    }

    private boolean getItemFromCurrentDirection() {
        TileEntity tile = this.getTileEntityForCurrentDirection();

        boolean extracted = false;

        /*
         * we must not .reset here because we might inadvertently change this.state right before this.state++
         *
        if((tile == null) || !(tile instanceof IInventory)) // TE / inventory is gone
            reset();
        */

        if (this.isImportUnit()) {
            if (this.searchedItemStack.isEmpty()) { // we don't know what we're supposed to pick up
                this.reset();
            } else if (tile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
                IItemHandler otherInv = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
                ItemStack currentStack = this.inventory.getStackInSlot(0);
                int oldStackSize = currentStack.getCount();

                for (int i = 0; i < otherInv.getSlots(); i++) {
                    if (!otherInv.getStackInSlot(i).isEmpty()) {
                        if (currentStack.isEmpty()) {
                            if (otherInv.getStackInSlot(i).isItemEqual(this.searchedItemStack)) {
                                ItemStack exStack = otherInv.extractItem(i, 1, false);
                                this.inventory.insertItem(0, exStack, false);
                            }
                        } else if (ItemHandlerHelper.canItemStacksStack(currentStack, otherInv.getStackInSlot(i))) {
                            ItemStack exStack = otherInv.extractItem(i, 1, false);
                            this.inventory.insertItem(0, exStack, false);
                        }
                        extracted = this.inventory.getStackInSlot(0).getCount() >= this.searchedItemStack.getCount();
                        if (extracted) {
                            break;
                        }
                    }
                }

                if (oldStackSize == (this.inventory.getStackInSlot(0).getCount())) { // nothing picked up, search for different inventory
                    this.state = STATE_SEARCH_SRC;
                }
            } else {
                this.state = STATE_SEARCH_SRC; // inventory gone
            }
        } else {
            if (tile instanceof TileEntityAssemblyPlatform) {
                TileEntityAssemblyPlatform plat = (TileEntityAssemblyPlatform) tile;

                if (plat.openClaw()) {
                    this.inventory.setStackInSlot(0, plat.getHeldStack());
                    plat.setHeldStack(ItemStack.EMPTY);
                    extracted = !this.inventory.getStackInSlot(0).isEmpty();
                    if (!extracted) { // something went wrong - either the platform is gone altogether, or the item is not there anymore
                        this.state = STATE_SEARCH_SRC;
                    }
                }
            }
        }

        return extracted;
    }

    private boolean putItemToCurrentDirection() {
        if (this.isImportUnit()) {
            TileEntity tile = this.getTileEntityForCurrentDirection();
            if (tile instanceof TileEntityAssemblyPlatform) {
                TileEntityAssemblyPlatform plat = (TileEntityAssemblyPlatform) tile;

                if (this.inventory.getStackInSlot(0).isEmpty()) {
                    return plat.closeClaw();
                }

                if (plat.isIdle()) {
                    plat.setHeldStack(this.inventory.getStackInSlot(0));
                    this.inventory.setStackInSlot(0, ItemStack.EMPTY);
                    return plat.closeClaw();
                }
            } else {
                this.repeatDropOffSearch(); // platform gone; close claw and search new drop-off-location
            }
        } else {
            TileEntity te = this.getTileEntityForCurrentDirection();
            if (te == null) this.repeatDropOffSearch(); // inventory gone; close claw and search new drop-off-location
            else {
                ItemStack currentStack = this.inventory.getStackInSlot(0);
                int startSize = currentStack.getCount();
                for (int i = 0; i < 6; i++) {
                    ItemStack excess = PneumaticCraftUtils.exportStackToInventory(te, currentStack, EnumFacing.byIndex(i));
                    this.inventory.setStackInSlot(0, excess);
                    if (excess.isEmpty()) break;
                }
                currentStack = this.inventory.getStackInSlot(0);
                if (currentStack.isEmpty() || startSize != currentStack.getCount())
                    this.sendDescriptionPacket(); // TODO - is this still needed? Shouldn't @DescSynced on inventory take care of this?

                if (!currentStack.isEmpty() && startSize == currentStack.getCount())
                    this.repeatDropOffSearch(); // target-inventory full or unavailable
            }

            return this.inventory.getStackInSlot(0).isEmpty();
        }

        return false;
    }

    private void repeatDropOffSearch() {
        this.state = this.state >= STATE_RESET_CLOSECLAW_AFTER_PICKUP ? STATE_RESET_CLOSECLAW_AFTER_PICKUP : STATE_CLOSECLAW_AFTER_PICKUP;
    }

    private boolean closeClaw() {
        this.shouldClawClose = true;
        return this.moveClaw();
    }

    private boolean openClaw() {
        this.shouldClawClose = false;
        return this.moveClaw();
    }

    private boolean moveClaw() {
        this.oldClawProgress = this.clawProgress;

        if (!this.shouldClawClose && this.clawProgress > 0F) {
            this.clawProgress = Math.max(this.clawProgress - TileEntityConstants.ASSEMBLY_IO_UNIT_CLAW_SPEED * this.speed, 0);
        } else if (this.shouldClawClose && this.clawProgress < 1F) {
            this.clawProgress = Math.min(this.clawProgress + TileEntityConstants.ASSEMBLY_IO_UNIT_CLAW_SPEED * this.speed, 1);
        }
        this.markDirty();
        return this.isClawDone();
    }

    private boolean isClawDone() {
        // need to make sure that clawProgress and oldClawProgress are the same, or we will get rendering artifacts
        return this.clawProgress == this.oldClawProgress && this.clawProgress == (this.shouldClawClose ? 1F : 0F);
    }

    public boolean isImportUnit() {
        return !this.exporting;
    }

    public boolean switchMode() {
        if (this.state <= STATE_SEARCH_SRC) {
            if (!this.hasSwitchedThisTick) {
                this.exporting = !this.exporting;
                this.hasSwitchedThisTick = true;
                this.markDirty();
                this.invalidateSystem();
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void gotoHomePosition() {
        super.gotoHomePosition();

        if (this.isClawDone()) this.openClaw();
    }

    @Override
    public boolean isIdle() {
        return this.state == STATE_IDLE;
    }

    @Override
    public AssemblyProgram.EnumMachine getAssemblyType() {
        return this.isImportUnit() ? AssemblyProgram.EnumMachine.IO_UNIT_IMPORT : AssemblyProgram.EnumMachine.IO_UNIT_EXPORT;
    }

    private boolean isDoneInternal() {
        return super.isDoneMoving();
        /*
        if(super.isDone()) {
            boolean searchDone = feedPlatformStep != 4 || searchedItemStack != null && inventory[0] != null && searchedItemStack.isItemEqual(inventory[0]) && inventory[0].stackSize == searchedItemStack.stackSize;
            return clawProgress == (shouldClawClose ? 1F : 0F) && searchDone;
        } else {
            return false;
        }
        */
    }

    private EnumFacing[] getInventoryDirectionForItem(ItemStack searchedItem) {
        ItemStack stack = this.inventory.getStackInSlot(0);
        if (!searchedItem.isEmpty() && (stack.isEmpty() || stack.isItemEqual(searchedItem))) {
            for (EnumFacing dir : EnumFacing.HORIZONTALS) {
                IItemHandler handler = IOHelper.getInventoryForTE(this.getCachedNeighbor(dir), EnumFacing.UP);
                if (handler != null && !IOHelper.extract(handler, searchedItem, IOHelper.ExtractCount.EXACT, true, false).isEmpty()) {
                    return new EnumFacing[]{dir, null};
                }
            }
            if (this.canMoveToDiagonalNeighbours()) {
                for (EnumFacing secDir : new EnumFacing[]{EnumFacing.WEST, EnumFacing.EAST}) {
                    for (EnumFacing primDir : new EnumFacing[]{EnumFacing.NORTH, EnumFacing.SOUTH}) {
                        TileEntity te = this.getWorld().getTileEntity(this.getPos().offset(primDir).offset(secDir));
                        IItemHandler handler = IOHelper.getInventoryForTE(te, EnumFacing.UP);
                        if (!IOHelper.extract(handler, searchedItem, IOHelper.ExtractCount.EXACT, true, false).isEmpty()) {
                            return new EnumFacing[]{primDir, secDir};
                        }
                    }
                }
            }
        }
        return null;
    }

    private EnumFacing[] getExportLocationForItem(ItemStack exportedItem) {
        if (!exportedItem.isEmpty()) {
            for (EnumFacing dir : EnumFacing.HORIZONTALS) {
                TileEntity te = this.getWorld().getTileEntity(this.getPos().offset(dir));
                int slot = getPlacementSlot(exportedItem, te);
                if (slot >= 0) return new EnumFacing[]{dir, null};
            }
            if (this.canMoveToDiagonalNeighbours()) {
                for (EnumFacing secDir : new EnumFacing[]{EnumFacing.WEST, EnumFacing.EAST}) {
                    for (EnumFacing primDir : new EnumFacing[]{EnumFacing.NORTH, EnumFacing.SOUTH}) {
                        TileEntity te = this.getWorld().getTileEntity(this.getPos().offset(primDir).offset(secDir));
                        int slot = getPlacementSlot(exportedItem, te);
                        if (slot >= 0) return new EnumFacing[]{primDir, secDir};
                    }
                }
            }
        }
        return null;
    }

    /**
     * Find a slot into which to place an exported item.  Note that other assembly robots are not valid export
     * locations, but any other TE which provides CAPABILITY_ITEM_HANDLER on the top face is a valid candidate.
     *
     * @param exportedItem item to export
     * @param te           where the item is being attempted to insert to (will use the top face for IItemHandler cap.)
     * @return the placement slot, or -1 when the item can't be placed / accessed
     */
    private static int getPlacementSlot(ItemStack exportedItem, TileEntity te) {
        if (te != null && !(te instanceof TileEntityAssemblyRobot) && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP)) {
            IItemHandler handler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP);
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack excess = handler.insertItem(slot, exportedItem, true);
                if (excess.getCount() < exportedItem.getCount()) {
                    return slot;
                }
            }
        }
        return -1;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.clawProgress = tag.getFloat("clawProgress");
        this.shouldClawClose = tag.getBoolean("clawClosing");
        this.state = tag.getByte("state");
        this.exporting = tag.getBoolean("exporting");
        this.inventory = new ItemStackHandler(INVENTORY_SIZE);
        this.inventory.deserializeNBT(tag.getCompoundTag("Items"));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setFloat("clawProgress", this.clawProgress);
        tag.setBoolean("clawClosing", this.shouldClawClose);
        tag.setByte("state", this.state);
        tag.setBoolean("exporting", this.exporting);
        tag.setTag("Items", this.inventory.serializeNBT());
        return tag;
    }

    @Override
    public boolean canMoveToDiagonalNeighbours() {
        return true;
    }

}
