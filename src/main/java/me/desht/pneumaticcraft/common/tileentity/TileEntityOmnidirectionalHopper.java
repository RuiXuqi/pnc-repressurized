package me.desht.pneumaticcraft.common.tileentity;

import me.desht.pneumaticcraft.api.item.IItemRegistry.EnumUpgrade;
import me.desht.pneumaticcraft.common.block.Blockss;
import me.desht.pneumaticcraft.common.config.ConfigHandler;
import me.desht.pneumaticcraft.common.inventory.handler.ComparatorItemStackHandler;
import me.desht.pneumaticcraft.common.network.DescSynced;
import me.desht.pneumaticcraft.common.network.GuiSynced;
import me.desht.pneumaticcraft.common.util.IOHelper;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.List;

public class TileEntityOmnidirectionalHopper extends TileEntityTickableBase implements IRedstoneControlled, IComparatorSupport {
    public static final int INVENTORY_SIZE = 5;
    @DescSynced
    EnumFacing inputDir = EnumFacing.UP;
    @DescSynced
    private EnumFacing outputDir = EnumFacing.UP;
    private final ComparatorItemStackHandler inventory = new ComparatorItemStackHandler(this, this.getInvSize());
    private int lastComparatorValue = -1;
    @GuiSynced
    public int redstoneMode;
    private int cooldown;
    @GuiSynced
    int leaveMaterialCount; // leave items/liquids (used as filter)
    private final int importSlot = 0;
    @DescSynced
    public boolean isCreative; // has a creative upgrade installed

    public TileEntityOmnidirectionalHopper() {
        super(4);
        this.addApplicableUpgrade(EnumUpgrade.SPEED);
        this.addApplicableUpgrade(EnumUpgrade.CREATIVE);
        if (ConfigHandler.machineProperties.omniHopperDispenser) this.addApplicableUpgrade(EnumUpgrade.DISPENSER);
    }

    protected int getInvSize() {
        return INVENTORY_SIZE;
    }

    @Override
    protected boolean shouldRerenderChunkOnDescUpdate() {
        return true;
    }

    @Override
    protected void onFirstServerUpdate() {
        super.onFirstServerUpdate();

        this.isCreative = this.getUpgrades(EnumUpgrade.CREATIVE) > 0;
    }

    @Override
    public IItemHandlerModifiable getPrimaryInventory() {
        return this.inventory;
    }

    @Override
    public void update() {
        super.update();

        if (!this.getWorld().isRemote && --this.cooldown <= 0 && this.redstoneAllows()) {
            int maxItems = this.getMaxItems();
            boolean success = this.doImport(maxItems);
            success |= this.doExport(maxItems);

            // If we couldn't pull or push, slow down a bit for performance reasons
            this.cooldown = success ? this.getItemTransferInterval() : 8;

            if (this.lastComparatorValue != this.getComparatorValueInternal()) {
                this.lastComparatorValue = this.getComparatorValueInternal();
                this.updateNeighbours();
            }
        }
    }

    protected int getComparatorValueInternal() {
        return this.inventory.getComparatorValue();
    }

    protected boolean doExport(int maxItems) {
        IItemHandler handler = IOHelper.getInventoryForTE(this.getCachedNeighbor(this.outputDir), this.outputDir.getOpposite());
        if (handler != null) {
            for (int i = 0; i < this.inventory.getSlots(); i++) {
                ItemStack stack = this.inventory.getStackInSlot(i);
                if (stack.getCount() > this.leaveMaterialCount) {
                    ItemStack exportedStack = ItemHandlerHelper.copyStackWithSize(stack, Math.min(maxItems, stack.getCount() - this.leaveMaterialCount));
                    int toExport = exportedStack.getCount();
                    ItemStack excess = ItemHandlerHelper.insertItem(handler, exportedStack, false);
                    int exportedCount = toExport - excess.getCount();
                    if (!this.isCreative) {
                        stack.shrink(exportedCount);
                        if (exportedCount > 0) this.inventory.invalidateComparatorValue();
                    }
                    maxItems -= exportedCount;
                    if (maxItems <= 0) return true;
                }
            }
        } else if (ConfigHandler.machineProperties.omniHopperDispenser && this.getUpgrades(EnumUpgrade.DISPENSER) > 0) {
            BlockPos pos = this.getPos().offset(this.outputDir);
            int remaining = maxItems;
            if (!this.world.isBlockFullCube(pos)) {
                for (int i = 0; i < this.inventory.getSlots(); i++) {
                    ItemStack inSlot = this.inventory.getStackInSlot(i);
                    ItemStack stack = this.inventory.extractItem(i, Math.min(inSlot.getCount() - this.leaveMaterialCount, remaining), this.isCreative);
                    if (!stack.isEmpty()) {
                        remaining -= stack.getCount();
                        PneumaticCraftUtils.dropItemOnGroundPrecisely(stack, this.getWorld(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                        if (remaining <= 0) return true;
                    }
                }
            }
            return remaining < maxItems;
        }

        return false;
    }

    protected boolean doImport(int maxItems) {
        boolean success = false;

        if (this.isInventoryFull()) {
            return false;
        }

        // Suck from input inventory
        IItemHandler handler = IOHelper.getInventoryForTE(this.getCachedNeighbor(this.inputDir), this.inputDir.getOpposite());
        if (handler != null) {
            int remaining = maxItems;
            for (int i = 0; i < handler.getSlots(); i++) {
                if (handler.getStackInSlot(i).isEmpty()) continue;
                ItemStack toExtract = handler.extractItem(i, remaining, true);
                ItemStack excess = ItemHandlerHelper.insertItemStacked(this.inventory, toExtract, false);
                int transferred = toExtract.getCount() - excess.getCount();
                if (transferred > 0) {
                    handler.extractItem(i, transferred, false);
                    remaining -= transferred;
                    if (remaining <= 0) {
                        return true;
                    }
                }
            }
            return remaining < maxItems;
        }

        // Suck in item entities
        for (EntityItem entity : getNeighborItems(this, this.inputDir)) {
            ItemStack remainder = IOHelper.insert(this, entity.getItem(), null, false);
            if (remainder.isEmpty()) {
                entity.setDead();
                success = true;
            } else if (remainder.getCount() < entity.getItem().getCount()) {
                // some but not all were inserted
                entity.setItem(remainder);
                success = true;
            }
        }

        return success;
    }

    private boolean isInventoryFull() {
        for (int i = 0; i < this.inventory.getSlots(); i++) {
            ItemStack stack = this.inventory.getStackInSlot(i);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }

    static List<EntityItem> getNeighborItems(TileEntity te, EnumFacing dir) {
        AxisAlignedBB box = new AxisAlignedBB(te.getPos().offset(dir));
        return te.getWorld().getEntitiesWithinAABB(EntityItem.class, box, EntitySelectors.IS_ALIVE);
    }

    public int getMaxItems() {
        int upgrades = this.getUpgrades(EnumUpgrade.SPEED);
        if (upgrades > 3) {
            return Math.min(1 << (upgrades - 3), 256);
        } else {
            return 1;
        }
    }

    public int getItemTransferInterval() {
        return 8 / (1 << this.getUpgrades(EnumUpgrade.SPEED));
    }

    public void setInputDirection(EnumFacing dir) {
        this.inputDir = dir;
    }

    public EnumFacing getInputDirection() {
        return this.inputDir;
    }

    @Override
    public EnumFacing getRotation() {
        return this.outputDir;
    }

    public void setRotation(EnumFacing rotation) {
        this.outputDir = rotation;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("inputDir", this.inputDir.ordinal());
        tag.setInteger("outputDir", this.outputDir.ordinal());
        tag.setInteger("redstoneMode", this.redstoneMode);
        tag.setInteger("leaveMaterialCount", this.leaveMaterialCount);
        tag.setTag("Items", this.inventory.serializeNBT());
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.inputDir = EnumFacing.byIndex(tag.getInteger("inputDir"));
        this.outputDir = EnumFacing.byIndex(tag.getInteger("outputDir"));
        this.redstoneMode = tag.getInteger("redstoneMode");
        if (tag.hasKey("leaveMaterial")) {
            this.leaveMaterialCount = (byte) (tag.getBoolean("leaveMaterial") ? 1 : 0);
        } else {
            this.leaveMaterialCount = tag.getInteger("leaveMaterialCount");
        }
        this.inventory.deserializeNBT(tag.getCompoundTag("Items"));
    }

    /**
     * Returns the name of the inventory.
     */
    @Override
    public String getName() {
        return Blockss.OMNIDIRECTIONAL_HOPPER.getTranslationKey();
    }

    @Override
    public void handleGUIButtonPress(int buttonID, EntityPlayer player) {
        if (buttonID == 0) {
            this.redstoneMode++;
            if (this.redstoneMode > 2) this.redstoneMode = 0;
        } else if (buttonID == 1) {
            this.leaveMaterialCount = 0;
        } else if (buttonID == 2) {
            this.leaveMaterialCount = 1;
        }
    }

    @Override
    public int getRedstoneMode() {
        return this.redstoneMode;
    }

    public boolean doesLeaveMaterial() {
        return this.leaveMaterialCount > 0;
    }

    @Override
    public int getComparatorValue() {
        return this.getComparatorValueInternal();
    }

    @Override
    protected void onUpgradesChanged() {
        super.onUpgradesChanged();

        if (this.world != null && !this.world.isRemote) {
            this.isCreative = this.getUpgrades(EnumUpgrade.CREATIVE) > 0;
        }
    }

    @Override
    public boolean shouldPreserveStateOnBreak() {
        // always preserve state, since we can't sneak-wrench this machine (sneak-wrench rotates output)
        return true;
    }
}
