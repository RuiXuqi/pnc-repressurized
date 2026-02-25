package me.desht.pneumaticcraft.common.ai;

import me.desht.pneumaticcraft.common.progwidgets.IToolUser;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetAreaItemBase;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nonnull;

public class DroneAIDig extends DroneAIBlockInteraction<ProgWidgetAreaItemBase> {

    /**
     * @param drone  the drone
     * @param widget needs to implement IBlockOrdered, IToolUser
     */
    public DroneAIDig(IDroneBase drone, ProgWidgetAreaItemBase widget) {
        super(drone, widget);
    }

    @Override
    protected boolean isValidPosition(BlockPos pos) {
        IBlockState blockState = this.worldCache.getBlockState(pos);
        Block block = blockState.getBlock();
        if (!this.worldCache.isAirBlock(pos) && !ignoreBlock(block)) {
            NonNullList<ItemStack> droppedStacks = NonNullList.create();
            if (block.canSilkHarvest(this.drone.world(), pos, blockState, this.drone.getFakePlayer())) {
                droppedStacks.add(getSilkTouchBlock(block, blockState));
            } else {
                block.getDrops(droppedStacks, this.drone.world(), pos, blockState, 0);
            }
            for (ItemStack droppedStack : droppedStacks) {
                if (this.widget.isItemValidForFilters(droppedStack, blockState)) {
                    return this.swapBestItemToFirstSlot(pos) || !((IToolUser) this.widget).requiresTool();
                }
            }
            if (this.widget.isItemValidForFilters(ItemStack.EMPTY, blockState)) {
                // try a by-block check
                return this.swapBestItemToFirstSlot(pos) || !((IToolUser) this.widget).requiresTool();
            }
        }
        return false;
    }

    @Override
    protected boolean respectClaims() {
        return true;
    }

    //gui.progWidget.dig.debug.missingDiggingTool
    private boolean swapBestItemToFirstSlot(BlockPos pos) {

        ItemStack oldCurrentStack = this.drone.getInv().getStackInSlot(0).copy();
        this.drone.getInv().setStackInSlot(0, ItemStack.EMPTY);
        float baseSoftness = this.worldCache.getBlockState(pos).getPlayerRelativeBlockHardness(this.drone.getFakePlayer(), this.drone.world(), pos);
        this.drone.getInv().setStackInSlot(0, oldCurrentStack);
        boolean hasDiggingTool = false;

        int bestSlot = 0;
        float bestSoftness = Float.MIN_VALUE;
        for (int i = 0; i < this.drone.getInv().getSlots(); i++) {
            this.drone.getInv().setStackInSlot(0, this.drone.getInv().getStackInSlot(i));
            float softness = this.worldCache.getBlockState(pos).getPlayerRelativeBlockHardness(this.drone.getFakePlayer(), this.drone.world(), pos);
            if (softness > bestSoftness) {
                bestSlot = i;
                bestSoftness = softness;

                if (softness > baseSoftness) {
                    hasDiggingTool = true;
                }
            }
        }
        this.drone.getInv().setStackInSlot(0, oldCurrentStack);
        if (bestSlot != 0) {
            ItemStack bestItem = this.drone.getInv().getStackInSlot(bestSlot).copy();
            this.drone.getInv().setStackInSlot(bestSlot, this.drone.getInv().getStackInSlot(0));
            this.drone.getInv().setStackInSlot(0, bestItem);
        }
        return hasDiggingTool;
    }

    @Override
    protected boolean doBlockInteraction(BlockPos pos, double distToBlock) {
        PlayerInteractionManager manager = this.drone.getFakePlayer().interactionManager;
        if (!manager.isDestroyingBlock || !manager.receivedFinishDiggingPacket) { //is not destroying and is not acknowledged.
            IBlockState blockState = this.worldCache.getBlockState(pos);
            Block block = blockState.getBlock();
            if (!ignoreBlock(block) && isBlockValidForFilter(this.worldCache, this.drone, pos, this.widget)) {
                if (blockState.getBlockHardness(this.drone.world(), pos) < 0) {
                    this.addToBlacklist(pos);
                    this.drone.addDebugEntry("gui.progWidget.dig.debug.cantDigBlock", pos);
                    this.drone.setDugBlock(null);
                    return false;
                }
                manager.onBlockClicked(pos, EnumFacing.DOWN);
                manager.blockRemoving(pos);
                /*if (!manager.isDestroyingBlock) { Commenting this a fix for #142? This statement always holds when blockRemoving(pos) is called.
                    addToBlacklist(pos);
                    drone.addDebugEntry("gui.progWidget.dig.debug.cantDigBlock", pos);
                    drone.setDugBlock(null);
                    return false;
                }*/
                this.drone.setDugBlock(pos);
                return true;
            }
            this.drone.setDugBlock(null);
            return false;
        } else {
            return true;
        }
    }

    public static boolean isBlockValidForFilter(IBlockAccess worldCache, IDroneBase drone, BlockPos pos, ProgWidgetAreaItemBase widget) {
        IBlockState blockState = worldCache.getBlockState(pos);
        Block block = blockState.getBlock();

        if (!block.isAir(blockState, worldCache, pos)) {
            NonNullList<ItemStack> droppedStacks = NonNullList.create();
            if (block.canSilkHarvest(drone.world(), pos, blockState, drone.getFakePlayer())) {
                droppedStacks.add(getSilkTouchBlock(block, blockState));
            } else {
                block.getDrops(droppedStacks, drone.world(), pos, blockState, 0);
            }
            for (ItemStack droppedStack : droppedStacks) {
                if (widget.isItemValidForFilters(droppedStack, blockState)) {
                    return true;
                }
            }
            return widget.isItemValidForFilters(ItemStack.EMPTY, blockState);  // try a by-block check
        }
        return false;
    }

    @Nonnull
    private static ItemStack getSilkTouchBlock(Block block, IBlockState state) {
        Item item = Item.getItemFromBlock(block);
        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        } else {
            return new ItemStack(item, 1, block.getMetaFromState(state));
        }
    }

    private static boolean ignoreBlock(Block block) {
        return PneumaticCraftUtils.isBlockLiquid(block);
    }

}
