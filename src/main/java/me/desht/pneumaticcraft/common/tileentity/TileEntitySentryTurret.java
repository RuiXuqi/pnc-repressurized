package me.desht.pneumaticcraft.common.tileentity;

import com.mojang.authlib.GameProfile;
import me.desht.pneumaticcraft.api.item.IItemRegistry.EnumUpgrade;
import me.desht.pneumaticcraft.common.ai.StringFilterEntitySelector;
import me.desht.pneumaticcraft.common.block.Blockss;
import me.desht.pneumaticcraft.common.inventory.handler.BaseItemStackHandler;
import me.desht.pneumaticcraft.common.item.ItemGunAmmo;
import me.desht.pneumaticcraft.common.minigun.Minigun;
import me.desht.pneumaticcraft.common.network.DescSynced;
import me.desht.pneumaticcraft.common.network.GuiSynced;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketPlaySound;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.common.util.fakeplayer.FakeNetHandlerPlayerServer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class TileEntitySentryTurret extends TileEntityTickableBase implements IRedstoneControlled, IGUITextFieldSensitive {
    private static final int INVENTORY_SIZE = 4;

    private final ItemStackHandler inventory = new TurretItemStackHandler(this);
    @GuiSynced
    private String entityFilter = "";
    @GuiSynced
    private int redstoneMode;
    @DescSynced
    private double range;
    @DescSynced
    private boolean activated;
    @DescSynced
    private ItemStack minigunColorStack = ItemStack.EMPTY;
    private Minigun minigun;
    @DescSynced
    private int targetEntityId = -1;
    @DescSynced
    private boolean sweeping;
    private final SentryTurretEntitySelector entitySelector = new SentryTurretEntitySelector();
    private double rangeSq;

    public TileEntitySentryTurret() {
        super(4);
        this.addApplicableUpgrade(EnumUpgrade.RANGE);
    }

    @Override
    public void update() {
        super.update();
        if (!this.getWorld().isRemote) {
            if (this.getMinigun().getAttackTarget() == null && this.redstoneAllows()) {
                this.getMinigun().setSweeping(true);
                if ((this.getWorld().getTotalWorldTime() & 0xF) == 0) {
                    List<EntityLivingBase> entities = this.getWorld().getEntitiesWithinAABB(EntityLivingBase.class, this.getTargetingBoundingBox(), this.entitySelector);
                    if (entities.size() > 0) {
                        entities.sort(new TargetSorter());
                        this.getMinigun().setAttackTarget(entities.get(0));
                        this.targetEntityId = entities.get(0).getEntityId();
                    }
                }
            } else {
                this.getMinigun().setSweeping(false);
            }
            EntityLivingBase target = this.getMinigun().getAttackTarget();
            if (target != null) {
                if (!this.redstoneAllows() || !this.entitySelector.apply(target)) {
                    this.getMinigun().setAttackTarget(null);
                    this.targetEntityId = -1;
                } else {
                    if ((this.getWorld().getTotalWorldTime() & 0x7) == 0) {
                        this.getFakePlayer().setPosition(this.getPos().getX() + 0.5, this.getPos().getY() + 0.5, this.getPos().getZ() + 0.5); //Make sure the knockback has the right direction.
                        boolean usedAmmo = this.getMinigun().tryFireMinigun(target);
                        if (usedAmmo) {
                            for (int i = 0; i < this.inventory.getSlots(); i++) {
                                if (!this.inventory.getStackInSlot(i).isEmpty()) {
                                    this.inventory.setStackInSlot(i, ItemStack.EMPTY);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        this.getMinigun().update(this.getPos().getX() + 0.5, this.getPos().getY() + 0.5, this.getPos().getZ() + 0.5);
    }

    private boolean canSeeEntity(Entity entity) {
        Vec3d entityVec = new Vec3d(entity.posX + entity.width / 2, entity.posY + entity.height / 2, entity.posZ + entity.width / 2);
        Vec3d tileVec = new Vec3d(this.getPos().getX() + 0.5, this.getPos().getY() + 0.5, this.getPos().getZ() + 0.5);
        RayTraceResult trace = this.getWorld().rayTraceBlocks(entityVec, tileVec);
        return trace != null && trace.getBlockPos().equals(this.getPos());
    }

    private AxisAlignedBB getTargetingBoundingBox() {
        return new AxisAlignedBB(this.getPos().getX() - this.range, this.getPos().getY() - this.range, this.getPos().getZ() - this.range, this.getPos().getX() + this.range + 1, this.getPos().getY() + this.range + 1, this.getPos().getZ() + this.range + 1);
    }

    @Override
    protected void onFirstServerUpdate() {
        super.onFirstServerUpdate();
        this.updateAmmo();
        this.setText(0, this.entityFilter);
    }

    @Override
    public void onDescUpdate() {
        super.onDescUpdate();
        Entity entity = this.getWorld().getEntityByID(this.targetEntityId);
        if (entity instanceof EntityLivingBase) {
            this.getMinigun().setAttackTarget((EntityLivingBase) entity);
        } else {
            this.getMinigun().setAttackTarget(null);
        }
    }

    public Minigun getMinigun() {
        if (this.minigun == null) {
            this.minigun = new MinigunSentryTurret();
            this.minigun.setWorld(this.getWorld());
            if (!this.getWorld().isRemote) {
                this.minigun.setPlayer(this.getFakePlayer());
            }
        }
        return this.minigun;
    }

    private EntityPlayer getFakePlayer() {
        FakePlayer fakePlayer = FakePlayerFactory.get((WorldServer) this.getWorld(), new GameProfile(null, "Sentry Turret"));
        if (fakePlayer.connection == null) {
            fakePlayer.connection = new FakeNetHandlerPlayerServer(FMLCommonHandler.instance().getMinecraftServerInstance(), fakePlayer);
        }
        return fakePlayer;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setTag("Items", this.inventory.serializeNBT());
        tag.setByte("redstoneMode", (byte) this.redstoneMode);
        tag.setString("entityFilter", this.entityFilter);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.inventory.deserializeNBT(tag.getCompoundTag("Items"));
        this.redstoneMode = tag.getByte("redstoneMode");
        this.entityFilter = tag.getString("entityFilter");
    }

    @Override
    public boolean redstoneAllows() {
        return this.redstoneMode == 3 || super.redstoneAllows();
    }

    @Override
    public int getRedstoneMode() {
        return this.redstoneMode;
    }

    @Override
    public void handleGUIButtonPress(int buttonID, EntityPlayer player) {
        if (buttonID == 0) {
            this.redstoneMode++;
            if (this.redstoneMode > 2) this.redstoneMode = 0;
        }
    }

    /**
     * Returns the name of the inventory.
     */
    @Override
    public String getName() {
        return Blockss.SENTRY_TURRET.getTranslationKey();
    }

    private class TurretItemStackHandler extends BaseItemStackHandler {
        TurretItemStackHandler(TileEntity te) {
            super(te, INVENTORY_SIZE);
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            TileEntitySentryTurret.this.updateAmmo();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack itemStack) {
            return itemStack.isEmpty() || itemStack.getItem() instanceof ItemGunAmmo;
        }
    }

    private void updateAmmo() {
        ItemStack ammo = ItemStack.EMPTY;
        for (int i = 0; i < this.inventory.getSlots(); i++) {
            ammo = this.inventory.getStackInSlot(i);
            if (!ammo.isEmpty()) {
                break;
            }
        }
        this.getMinigun().setAmmoStack(ammo);
        this.recalculateRange();
    }

    @Override
    public IItemHandlerModifiable getPrimaryInventory() {
        return this.inventory;
    }

    @Override
    protected void onUpgradesChanged() {
        super.onUpgradesChanged();
        if (this.getWorld() != null) {
            // this can get called when reading nbt on load when world = null
            // in that case, range is recalculated in onFirstServerUpdate()
            this.recalculateRange();
        }
    }

    private void recalculateRange() {
        this.range = 16.0 + Math.min(16, this.getUpgrades(EnumUpgrade.RANGE));
        ItemStack ammoStack = this.getMinigun().getAmmoStack();
        if (ammoStack.getItem() instanceof ItemGunAmmo) {
            this.range *= ((ItemGunAmmo) ammoStack.getItem()).getRangeMultiplier(ammoStack);
        }
        this.rangeSq = this.range * this.range;
    }

    private class MinigunSentryTurret extends Minigun {

        MinigunSentryTurret() {
            super(true);
        }

        @Override
        public boolean isMinigunActivated() {
            return TileEntitySentryTurret.this.activated;
        }

        @Override
        public void setMinigunActivated(boolean activated) {
            TileEntitySentryTurret.this.activated = activated;
        }

        @Override
        public void setAmmoColorStack(@Nonnull ItemStack ammo) {
            TileEntitySentryTurret.this.minigunColorStack = ammo;
        }

        @Override
        public int getAmmoColor() {
            return this.getAmmoColor(TileEntitySentryTurret.this.minigunColorStack);
        }

        @Override
        public void playSound(SoundEvent soundName, float volume, float pitch) {
            NetworkHandler.sendToAllAround(new PacketPlaySound(soundName, SoundCategory.BLOCKS,
                    TileEntitySentryTurret.this.getPos().getX() + 0.5, TileEntitySentryTurret.this.getPos().getY() + 0.5, TileEntitySentryTurret.this.getPos().getZ() + 0.5,
                    volume, pitch, false), this.world);
        }

        @Override
        public void setSweeping(boolean sweeping) {
            TileEntitySentryTurret.this.sweeping = sweeping;
        }

        @Override
        public boolean isSweeping() {
            return TileEntitySentryTurret.this.sweeping;
        }

        @Override
        public Object getSoundSource() {
            return TileEntitySentryTurret.this.getPos();
        }
    }

    private class TargetSorter implements Comparator<Entity> {
        private final BlockPos pos;

        TargetSorter() {
            this.pos = new BlockPos(TileEntitySentryTurret.this.getPos().getX(), TileEntitySentryTurret.this.getPos().getY(), TileEntitySentryTurret.this.getPos().getZ());
        }

        @Override
        public int compare(Entity arg0, Entity arg1) {
            double dist1 = PneumaticCraftUtils.distBetweenSq(this.pos, arg0.getPosition());
            double dist2 = PneumaticCraftUtils.distBetweenSq(this.pos, arg1.getPosition());
            return Double.compare(dist1, dist2);
        }
    }

    private class SentryTurretEntitySelector extends StringFilterEntitySelector {
        @Override
        public boolean apply(Entity entity) {
            if (entity instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) entity;
                if (player.capabilities.isCreativeMode || this.isExcludedBySecurityStations(player)) return false;
            }
            return super.apply(entity) && this.inRange(entity) && TileEntitySentryTurret.this.canSeeEntity(entity);
        }

        private boolean inRange(Entity entity) {
            return PneumaticCraftUtils.distBetweenSq(new BlockPos(TileEntitySentryTurret.this.getPos().getX(), TileEntitySentryTurret.this.getPos().getY(), TileEntitySentryTurret.this.getPos().getZ()), entity.posX, entity.posY, entity.posZ) <= TileEntitySentryTurret.this.rangeSq;
        }

        private boolean isExcludedBySecurityStations(EntityPlayer player) {
            Iterator<TileEntitySecurityStation> iterator = TileEntitySecurityStation.getSecurityStations(TileEntitySentryTurret.this.getWorld(), TileEntitySentryTurret.this.getPos(), false).iterator();
            if (iterator.hasNext()) { //When there are Security Stations, all stations need to be allowing the player.
                while (iterator.hasNext()) {
                    if (!iterator.next().doesAllowPlayer(player)) return false;
                }
                return true;
            } else {
                return false; //When there are no Security Stations at all, the player isn't automatically 'allowed to live'.
            }
        }
    }

    @Override
    public void setText(int textFieldID, String text) {
        this.entityFilter = text;
        if (this.world != null && !this.world.isRemote) {
            this.entitySelector.setFilter(text);
            if (this.minigun != null) this.minigun.setAttackTarget(null);
            this.markDirty();
        }
    }

    @Override
    public String getText(int textFieldID) {
        return this.entityFilter;
    }
}
