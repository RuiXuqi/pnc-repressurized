package me.desht.pneumaticcraft.common.semiblock;

import me.desht.pneumaticcraft.common.config.ConfigHandler;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.IPlantable;

public class SemiBlockCropSupport extends SemiBlockBasic<TileEntity> {
    public static final String ID = "crop_support";

    public SemiBlockCropSupport() {
        super(TileEntity.class);
    }

    /**
     * Use this custom addition to prevent placement derpyness, where the crop gets placed on the tilled earth.
     */
    @Override
    public boolean canPlace(EnumFacing facing) {
        IBlockState state = this.getBlockState();
        if (!state.getBlock().isAir(state, this.world, this.getPos()) &&
                !(state.getBlock() instanceof IPlantable)) return false;

        return this.canStay();
    }

    /**
     * When this block is not air, or the block below the crop is not air, it's OK.
     *
     * @return
     */
    @Override
    public boolean canStay() {
        IBlockState state = this.getBlockState();
        if (!state.getBlock().isAir(state, this.world, this.getPos())) return true;

        BlockPos posBelow = this.getPos().offset(EnumFacing.DOWN);
        IBlockState stateBelow = this.world.getBlockState(posBelow);
        return !stateBelow.getBlock().isAir(stateBelow, this.world, posBelow);
    }

    @Override
    public void update() {
        super.update();

        if (this.world.rand.nextDouble() < ConfigHandler.machineProperties.cropSticksGrowthBoostChance) {
            if (!this.world.isRemote) {
                IBlockState state = this.getBlockState();
                state.getBlock().updateTick(this.world, this.getPos(), state, this.world.rand);
            } else {
                this.world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, this.getPos().getX() + 0.5, this.getPos().getY() + 0.5, this.getPos().getZ() + 0.5, 0, 0, 0);
            }
        }
    }
}
