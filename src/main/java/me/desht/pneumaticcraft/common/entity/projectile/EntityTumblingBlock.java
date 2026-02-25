package me.desht.pneumaticcraft.common.entity.projectile;

import com.mojang.authlib.GameProfile;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.common.util.fakeplayer.FakeNetHandlerPlayerServer;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;

/**
 * A bit like an EntityFallingBlock but tumbles as it flies, and tries to form a block on impact with any other
 * block, not just when it lands on top of another block.
 */
public class EntityTumblingBlock extends EntityThrowable {
    private static final DataParameter<BlockPos> ORIGIN = EntityDataManager.createKey(EntityTumblingBlock.class, DataSerializers.BLOCK_POS);
    private static final DataParameter<ItemStack> STATE_STACK = EntityDataManager.createKey(EntityTumblingBlock.class, DataSerializers.ITEM_STACK);
    private static FakePlayer fakePlayer;

    public EntityTumblingBlock(World worldIn) {
        super(worldIn);
    }

    public EntityTumblingBlock(World worldIn, double x, double y, double z, @Nonnull ItemStack stack) {
        super(worldIn);
        Validate.isTrue(!stack.isEmpty() && stack.getItem() instanceof ItemBlock);

        this.preventEntitySpawning = true;
        this.setSize(0.98F, 0.98F);
        this.setPosition(x, y + (double) ((1.0F - this.height) / 2.0F), z);
        this.motionX = 0.0D;
        this.motionY = 0.0D;
        this.motionZ = 0.0D;
        this.prevPosX = x;
        this.prevPosY = y;
        this.prevPosZ = z;
        this.setOrigin(new BlockPos(this));
        this.dataManager.set(STATE_STACK, stack);
    }

    @Override
    protected void entityInit() {
        this.dataManager.register(ORIGIN, BlockPos.ORIGIN);
        this.dataManager.register(STATE_STACK, ItemStack.EMPTY);
    }

    @Override
    public void shoot(Entity entityThrower, float rotationPitchIn, float rotationYawIn, float pitchOffset, float velocity, float inaccuracy) {
        // velocities etc. get set up in TileEntityAirCannon#launchEntity()
    }

    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
    }

    public ItemStack getStack() {
        return this.dataManager.get(STATE_STACK);
    }

    @SideOnly(Side.CLIENT)
    public BlockPos getOrigin() {
        return this.dataManager.get(ORIGIN);
    }

    private void setOrigin(BlockPos pos) {
        this.dataManager.set(ORIGIN, pos);
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        super.onUpdate();  // handles nearly all of the in-flight logic

        if (!this.world.isRemote) {
            BlockPos blockpos1 = new BlockPos(this);
            if (!this.onGround && (this.ticksExisted > 100 && (blockpos1.getY() < 1 || blockpos1.getY() > 256) || this.ticksExisted > 600)) {
                this.dropAsItem();
                this.setDead();
            }
        }
    }

    @Override
    protected void onImpact(RayTraceResult result) {
        if (!this.world.isRemote) {
            if (result.typeOfHit == RayTraceResult.Type.BLOCK) {
                this.setDead();
                if (!this.tryPlaceAsBlock(result.getBlockPos(), result.sideHit)) {
                    this.dropAsItem();
                }
            }
        }
    }

    private boolean tryPlaceAsBlock(BlockPos pos0, EnumFacing side) {
        Block b = this.world.getBlockState(pos0).getBlock();
        BlockPos pos = b.isReplaceable(this.world, pos0) ? pos0 : pos0.offset(side);
        if (this.world.getBlockState(pos).getBlock().isReplaceable(this.world, pos)) {
            ItemStack stack = this.dataManager.get(STATE_STACK);
            Block block = ((ItemBlock) stack.getItem()).getBlock();
            EntityPlayer placer = this.thrower instanceof EntityPlayer ? (EntityPlayer) this.thrower : this.getFakePlayer();

            IBlockState newState = block.getStateForPlacement(this.world, pos, side, 0f, 0f, 0f, stack.getMetadata(), placer, EnumHand.MAIN_HAND);
            return PneumaticCraftUtils.tryPlaceBlock(this.world, pos, placer, side, newState);
        }
        return false;
    }

    private void dropAsItem() {
        if (this.world.getGameRules().getBoolean("doEntityDrops")) {
            this.entityDropItem(this.dataManager.get(STATE_STACK).copy(), 0.0F);
        }
    }

    private EntityPlayer getFakePlayer() {
        if (fakePlayer == null) {
            fakePlayer = FakePlayerFactory.get((WorldServer) this.world, new GameProfile(null, "[Tumbling Block]"));
            fakePlayer.connection = new FakeNetHandlerPlayerServer(FMLCommonHandler.instance().getMinecraftServerInstance(), fakePlayer);
        }
        fakePlayer.posX = this.posX;
        fakePlayer.posY = this.posY;
        fakePlayer.posZ = this.posZ;
        return fakePlayer;
    }
}
