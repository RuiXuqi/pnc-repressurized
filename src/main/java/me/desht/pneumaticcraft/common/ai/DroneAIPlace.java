package me.desht.pneumaticcraft.common.ai;

import me.desht.pneumaticcraft.common.entity.living.EntityDrone;
import me.desht.pneumaticcraft.common.progwidgets.ISidedWidget;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetAreaItemBase;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetPlace;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class DroneAIPlace extends DroneAIBlockInteraction {

    /**
     * @param drone
     * @param widget needs to implement IBlockOrdered and IDirectionalWidget.
     */
    public DroneAIPlace(IDroneBase drone, ProgWidgetAreaItemBase widget) {
        super(drone, widget);
    }

    @Override
    protected boolean respectClaims() {
        return true;
    }

    @Override
    protected boolean isValidPosition(BlockPos pos) {
        if (this.drone.world().getBlockState(pos).getMaterial().isReplaceable()) {
            boolean failedOnPlacement = false;
            for (int i = 0; i < this.drone.getInv().getSlots(); i++) {
                ItemStack droneStack = this.drone.getInv().getStackInSlot(i);
                if (droneStack.getItem() instanceof ItemBlock) {
                    if (this.widget.isItemValidForFilters(droneStack)) {
                        Block placingBlock = ((ItemBlock) droneStack.getItem()).getBlock();
                        EnumFacing side = ProgWidgetPlace.getDirForSides(((ISidedWidget) this.widget).getSides());
                        if (this.drone.world().mayPlace(placingBlock, pos, false, side, this.drone instanceof EntityDrone ? (EntityDrone) this.drone : null)) {
                            return true;
                        } else {
                            if (this.drone.world().mayPlace(placingBlock, pos, true, side, this.drone instanceof EntityDrone ? (EntityDrone) this.drone : null)) {
                                this.drone.addDebugEntry("gui.progWidget.place.debug.cantPlaceBlock", pos);
                            } else {
                                this.drone.addDebugEntry("gui.progWidget.place.debug.entityInWay", pos);
                            }
                            failedOnPlacement = true;
                        }
                    }
                }
            }
            if (!failedOnPlacement) this.abort();
        }
        return false;
    }

    //TODO 1.8 test
    @Override
    protected boolean doBlockInteraction(BlockPos pos, double distToBlock) {
        if (this.drone.getPathNavigator().hasNoPath()) {
            EnumFacing side = ProgWidgetPlace.getDirForSides(((ISidedWidget) this.widget).getSides());
            for (int i = 0; i < this.drone.getInv().getSlots(); i++) {
                ItemStack droneStack = this.drone.getInv().getStackInSlot(i);
                if (droneStack.getItem() instanceof ItemBlock && ((ItemBlock) droneStack.getItem()).getBlock().canPlaceBlockOnSide(this.drone.world(), pos, ProgWidgetPlace.getDirForSides(((ISidedWidget) this.widget).getSides()))) {
                    if (this.widget.isItemValidForFilters(droneStack)) {
                        ItemBlock itemBlock = (ItemBlock) droneStack.getItem();
                        Block block = itemBlock.getBlock();
                        if (this.drone.world().mayPlace(block, pos, false, side, this.drone instanceof EntityDrone ? (EntityDrone) this.drone : null)) {
                            int newMeta = itemBlock.getMetadata(droneStack.getMetadata());
                            this.setFakePlayerAccordingToDir();
                            IBlockState iblockstate1 = block.getStateForPlacement(this.drone.world(), pos, side, side.getXOffset(), side.getYOffset(), side.getZOffset(), newMeta, this.drone.getFakePlayer(), EnumHand.MAIN_HAND);
                            if (itemBlock.placeBlockAt(droneStack, this.drone.getFakePlayer(), this.drone.world(), pos, side, side.getXOffset(), side.getYOffset(), side.getZOffset(), iblockstate1)) {
                                this.drone.addAir(null, -PneumaticValues.DRONE_USAGE_PLACE);
                                SoundType soundType = block.getSoundType(iblockstate1, this.drone.world(), pos, this.drone.getFakePlayer());
                                this.drone.world().playSound(pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F, soundType.getPlaceSound(), SoundCategory.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F, false);
                                droneStack.shrink(1);
                                if (droneStack.getCount() <= 0) {
                                    this.drone.getInv().setStackInSlot(i, ItemStack.EMPTY);
                                }
                            }
                            return false;
                        }
                    }
                }
            }
            return false;
        } else {
            return true;
        }
    }

    private void setFakePlayerAccordingToDir() {
        EntityPlayer fakePlayer = this.drone.getFakePlayer();
        Vec3d pos = this.drone.getDronePos();
        fakePlayer.posX = pos.x;
        fakePlayer.posZ = pos.z;
        switch (ProgWidgetPlace.getDirForSides(((ISidedWidget) this.widget).getSides())) {
            case UP:
                fakePlayer.rotationPitch = -90;
                fakePlayer.posY = pos.y - 10;//do this for PistonBase.determineDirection()
                return;
            case DOWN:
                fakePlayer.rotationPitch = 90;
                fakePlayer.posY = pos.y + 10;//do this for PistonBase.determineDirection()
                return;
            case NORTH:
                fakePlayer.rotationYaw = 180;
                fakePlayer.posY = pos.y;//do this for PistonBase.determineDirection()
                break;
            case EAST:
                fakePlayer.rotationYaw = 270;
                fakePlayer.posY = pos.y;//do this for PistonBase.determineDirection()
                break;
            case SOUTH:
                fakePlayer.rotationYaw = 0;
                fakePlayer.posY = pos.y;//do this for PistonBase.determineDirection()
                break;
            case WEST:
                fakePlayer.rotationYaw = 90;
                fakePlayer.posY = pos.y;//do this for PistonBase.determineDirection()
                break;
        }
    }

}
