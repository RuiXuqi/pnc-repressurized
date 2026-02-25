package me.desht.pneumaticcraft.common.ai;

import me.desht.pneumaticcraft.api.item.IItemRegistry.EnumUpgrade;
import me.desht.pneumaticcraft.common.item.Itemss;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketSpawnParticle;
import me.desht.pneumaticcraft.common.pneumatic_armor.CommonArmorHandler;
import me.desht.pneumaticcraft.common.progwidgets.IBlockOrdered;
import me.desht.pneumaticcraft.common.progwidgets.IBlockOrdered.EnumOrder;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetAreaItemBase;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetDigAndPlace;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetPlace;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.common.util.ThreadedSorter;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class DroneAIBlockInteraction<Widget extends ProgWidgetAreaItemBase> extends EntityAIBase {
    protected final IDroneBase drone;
    protected final Widget widget;
    private final EnumOrder order;
    private BlockPos curPos;
    private final List<BlockPos> area;
    final IBlockAccess worldCache;
    private final List<BlockPos> blacklist = new ArrayList<>();//a list of position which weren't allowed to be digged in the past.
    private int curY;
    private int lastSuccessfulY;
    private int minY, maxY;
    private ThreadedSorter<BlockPos> sorter;
    private boolean aborted;

    private boolean searching; //true while the drone is searching for a coordinate, false if traveling/processing a coordinate.
    private int searchIndex;//The current index in the area list the drone is searching at.
    private static final int LOOKUPS_PER_SEARCH_TICK = 30; //How many blocks does the drone access per AI update.
    private int totalActions;
    private int maxActions = -1;

    /**
     * @param drone  the drone
     * @param widget needs to implement IBlockOrdered
     */
    public DroneAIBlockInteraction(IDroneBase drone, Widget widget) {
        this.drone = drone;
        this.setMutexBits(63);//binary 111111, so it won't run along with other AI tasks.
        this.widget = widget;
        this.order = widget instanceof IBlockOrdered ? ((IBlockOrdered) widget).getOrder() : EnumOrder.CLOSEST;
        this.area = widget.getCachedAreaList();
        this.worldCache = ProgWidgetAreaItemBase.getCache(this.area, drone.world());
        if (this.area.size() > 0) {
            Iterator<BlockPos> iterator = this.area.iterator();
            BlockPos pos = iterator.next();
            this.minY = this.maxY = pos.getY();
            while (iterator.hasNext()) {
                pos = iterator.next();
                this.minY = Math.min(this.minY, pos.getY());
                this.maxY = Math.max(this.maxY, pos.getY());
            }
            if (this.order == EnumOrder.HIGH_TO_LOW) {
                this.curY = this.maxY;
            } else if (this.order == EnumOrder.LOW_TO_HIGH) {
                this.curY = this.minY;
            }
        }
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    @Override
    public boolean shouldExecute() {
        if (this.aborted || this.maxActions >= 0 && this.totalActions >= this.maxActions) {
            return false;
        } else {
            if (!this.searching) {
                this.searching = true;
                this.searchIndex = 0;
                this.curPos = null;
                this.lastSuccessfulY = this.curY;
                if (this.sorter == null || this.sorter.isDone())
                    this.sorter = new ThreadedSorter<>(this.area, new ChunkPositionSorter(this.drone));
                return true;
            } else {
                return false;
            }
        }
    }

    private void updateY() {
        this.searchIndex = 0;
        if (this.order == ProgWidgetPlace.EnumOrder.LOW_TO_HIGH) {
            if (++this.curY > this.maxY) this.curY = this.minY;
        } else if (this.order == ProgWidgetPlace.EnumOrder.HIGH_TO_LOW) {
            if (--this.curY < this.minY) this.curY = this.maxY;
        }
    }

    private boolean isYValid(int y) {
        return this.order == ProgWidgetPlace.EnumOrder.CLOSEST || y == this.curY;
    }

    public DroneAIBlockInteraction<Widget> setMaxActions(int maxActions) {
        this.maxActions = maxActions;
        return this;
    }

    protected abstract boolean isValidPosition(BlockPos pos);

    protected abstract boolean doBlockInteraction(BlockPos pos, double distToBlock);

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    @Override
    public boolean shouldContinueExecuting() {
        if (this.aborted) return false;
        if (this.searching) {
            if (!this.sorter.isDone()) return true;//Wait until the area is sorted from closest to furtherest.
            boolean firstRun = true;
            int searchedBlocks = 0; //keeps track of the looked up blocks, and stops searching when we reach our quota.
            while (this.curPos == null && this.curY != this.lastSuccessfulY && this.order != ProgWidgetDigAndPlace.EnumOrder.CLOSEST || firstRun) {
                firstRun = false;
                while (!this.shouldAbort() && this.searchIndex < this.area.size()) {
                    BlockPos pos = this.area.get(this.searchIndex);
                    if (this.isYValid(pos.getY()) && !this.blacklist.contains(pos) && (!this.respectClaims() || !DroneClaimManager.getInstance(this.drone.world()).isClaimed(pos))) {
                        this.indicateToListeningPlayers(pos);
                        if (this.isValidPosition(pos)) {
                            this.curPos = pos;
                            if (this.moveToPositions()) {
                                if (this.tryMoveToBlock(pos)) {
                                    return true;
                                }
                                if (this.drone.getPathNavigator().isGoingToTeleport()) {
                                    return this.movedToBlockOK(pos);
                                } else {
                                    this.drone.addDebugEntry("gui.progWidget.general.debug.cantNavigate", pos);
                                }
                            } else {
                                this.searching = false;
                                this.totalActions++;
                                return true;
                            }
                        }
                        searchedBlocks++;
                    }
                    this.searchIndex++;
                    if (searchedBlocks >= this.lookupsPerSearch()) return true;
                }
                if (this.curPos == null) this.updateY();
            }
            if (!this.shouldAbort()) this.addEndingDebugEntry();
            return false;
        } else {
            Vec3d dronePos = this.drone.getDronePos();
            double dist = this.curPos != null ? PneumaticCraftUtils.distBetween(this.curPos.getX() + 0.5, this.curPos.getY() + 0.5, this.curPos.getZ() + 0.5, dronePos.x, dronePos.y, dronePos.z) : 0;
            if (this.curPos != null) {
                if (!this.moveToPositions()) return this.doBlockInteraction(this.curPos, dist);
                if (this.respectClaims()) DroneClaimManager.getInstance(this.drone.world()).claim(this.curPos);
                if (dist < (this.moveIntoBlock() ? 1 : 2)) {
                    return this.doBlockInteraction(this.curPos, dist);
                }
            }
            return !this.drone.getPathNavigator().hasNoPath();
        }
    }

    private boolean tryMoveToBlock(BlockPos pos) {
        if (this.moveIntoBlock()) {
            if (this.drone.getPathNavigator().moveToXYZ(this.curPos.getX(), this.curPos.getY() + 0.5, this.curPos.getZ())) {
                return this.movedToBlockOK(pos);
            }
        } else {
            for (EnumFacing dir : EnumFacing.VALUES) {
                if (this.drone.getPathNavigator().moveToXYZ(this.curPos.getX() + dir.getXOffset(), this.curPos.getY() + dir.getYOffset() + 0.5, this.curPos.getZ() + dir.getZOffset())) {
                    return this.movedToBlockOK(pos);
                }
            }
        }
        return false;
    }

    private boolean movedToBlockOK(BlockPos pos) {
        this.searching = false;
        this.totalActions++;
        if (this.respectClaims()) DroneClaimManager.getInstance(this.drone.world()).claim(pos);
        this.blacklist.clear(); //clear the list for next time (maybe the blocks/rights have changed by the time there will be dug again).
        return true;
    }

    protected void addEndingDebugEntry() {
        this.drone.addDebugEntry("gui.progWidget.blockInteraction.debug.noBlocksValid");
    }

    protected int lookupsPerSearch() {
        return LOOKUPS_PER_SEARCH_TICK;
    }

    protected boolean respectClaims() {
        return false;
    }

    protected boolean moveIntoBlock() {
        return false;
    }

    protected boolean shouldAbort() {
        return this.aborted;
    }

    public void abort() {
        this.aborted = true;
    }

    protected boolean moveToPositions() {
        return true;
    }

    /**
     * Sends particle spawn packets to any close player that has a charged pneumatic helmet with entity tracker.
     *
     * @param pos
     */
    private void indicateToListeningPlayers(BlockPos pos) {
        for (EntityPlayer player : this.drone.world().playerEntities) {
            if (player.getDistanceSq(pos) < 1024) {
                ItemStack helmet = player.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
                if (helmet.getItem() == Itemss.PNEUMATIC_HELMET) {
                    CommonArmorHandler handler = CommonArmorHandler.getHandlerForPlayer(player);
                    if (handler.isArmorReady(EntityEquipmentSlot.HEAD) && handler.isEntityTrackerEnabled()
                            && handler.getUpgradeCount(EntityEquipmentSlot.HEAD, EnumUpgrade.ENTITY_TRACKER) > 0
                            && handler.getUpgradeCount(EntityEquipmentSlot.HEAD, EnumUpgrade.DISPENSER) > 0) {
                        NetworkHandler.sendTo(new PacketSpawnParticle(EnumParticleTypes.REDSTONE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 0, 0), (EntityPlayerMP) player);
                    }
                }
            }
        }
    }

    protected void addToBlacklist(BlockPos coord) {
        this.blacklist.add(coord);
        this.drone.sendWireframeToClient(coord);
    }

}
