package me.desht.pneumaticcraft.common.entity.projectile;

import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.IShearable;

import java.util.List;

public class EntityVortex extends EntityThrowable {

    private int hitCounter = 0;

    // clientside: rendering X offset of vortex, depends on which hand the vortex was fired from
    private float renderOffsetX = -Float.MAX_VALUE;

    public EntityVortex(World world) {
        super(world);
    }

    public EntityVortex(World world, EntityLivingBase thrower) {
        super(world, thrower);
    }

    public EntityVortex(World world, double par2, double par4, double par6) {
        super(world, par2, par4, par6);
    }

    @Override
    protected void entityInit() {
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        this.motionX *= 0.95D; // equal to the potion effect friction. 0.95F
        this.motionY *= 0.95D;
        this.motionZ *= 0.95D;
        if (this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ < 0.1D) {
            this.setDead();
        }
    }

    public boolean hasRenderOffsetX() {
        return this.renderOffsetX > -Float.MAX_VALUE;
    }

    public float getRenderOffsetX() {
        return this.renderOffsetX;
    }

    public void setRenderOffsetX(float renderOffsetX) {
        this.renderOffsetX = renderOffsetX;
    }

    private boolean tryCutPlants(BlockPos pos) {
        Block block = this.world.getBlockState(pos).getBlock();
        if (block instanceof IPlantable || block instanceof BlockLeaves) {
            this.world.destroyBlock(pos, true);
            return true;
        }
        return false;
    }

    @Override
    public float getGravityVelocity() {
        return 0;
    }

    @Override
    protected void onImpact(RayTraceResult objectPosition) {
        if (objectPosition.entityHit != null) {
            Entity entity = objectPosition.entityHit;
            entity.motionX += this.motionX;
            entity.motionY += this.motionY;
            entity.motionZ += this.motionZ;
            if (!entity.world.isRemote && entity instanceof IShearable) {
                IShearable shearable = (IShearable) entity;
                BlockPos pos = new BlockPos(this.posX, this.posY, this.posZ);
                if (shearable.isShearable(ItemStack.EMPTY, this.world, pos)) {
                    List<ItemStack> drops = shearable.onSheared(ItemStack.EMPTY, this.world, pos, 0);
                    for (ItemStack stack : drops) {
                        PneumaticCraftUtils.dropItemOnGround(stack, this.world, entity.posX, entity.posY, entity.posZ);
                    }
                }
            }
        } else {
            Block block = this.world.getBlockState(objectPosition.getBlockPos()).getBlock();
            if (block instanceof IPlantable || block instanceof BlockLeaves) {
                if (!this.world.isRemote) {
                    BlockPos pos = objectPosition.getBlockPos();
                    BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos(pos);
                    if (this.tryCutPlants(pos)) {
                        int plantsCut = 1;
                        for (int x = -1; x <= 1; x++) {
                            for (int y = -1; y <= 1; y++) {
                                for (int z = -1; z <= 1; z++) {
                                    if (x == 0 && y == 0 && z == 0) continue;
                                    mPos.setPos(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                                    if (this.tryCutPlants(mPos)) plantsCut++;
                                }
                            }
                        }
                        // slow the vortex down for each plant it broke
                        double mult = Math.pow(0.8D, plantsCut);
                        this.motionX *= mult;
                        this.motionY *= mult;
                        this.motionZ *= mult;
                    }
                }
            } else {
                this.setDead();
            }
        }
        this.hitCounter++;
        if (this.hitCounter > 20) this.setDead();
    }
}
