package me.desht.pneumaticcraft.client.sound;

import me.desht.pneumaticcraft.common.entity.living.EntityDrone;
import me.desht.pneumaticcraft.common.item.ItemMinigun;
import me.desht.pneumaticcraft.common.item.Itemss;
import me.desht.pneumaticcraft.common.minigun.Minigun;
import me.desht.pneumaticcraft.common.tileentity.TileEntitySentryTurret;
import me.desht.pneumaticcraft.lib.Sounds;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;

public class MovingSoundMinigun extends MovingSound {
    private final Entity entity;
    private final TileEntity tileEntity;

    protected MovingSoundMinigun(Entity entity) {
        super(Sounds.MINIGUN, SoundCategory.NEUTRAL);
        this.entity = entity;
        this.tileEntity = null;
        this.init();
    }

    public MovingSoundMinigun(TileEntity tileEntity) {
        super(Sounds.MINIGUN, SoundCategory.NEUTRAL);
        this.entity = null;
        this.tileEntity = tileEntity;
        this.xPosF = tileEntity.getPos().getX();
        this.yPosF = tileEntity.getPos().getY();
        this.zPosF = tileEntity.getPos().getZ();
        this.init();
    }

    private void init() {
        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = 0.3F;
    }

    @Override
    public void update() {
        Minigun minigun = null;
        if (this.entity != null) {
            if (this.entity.isDead) {
                this.donePlaying = true;
                return;
            }
            this.xPosF = (float) this.entity.posX;
            this.yPosF = (float) this.entity.posY;
            this.zPosF = (float) this.entity.posZ;
            if (this.entity instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) this.entity;
                ItemStack curItem = player.getHeldItemMainhand();
                if (curItem.getItem() == Itemss.MINIGUN) {
                    minigun = ((ItemMinigun) Itemss.MINIGUN).getMinigun(curItem, player);
                }
            } else if (this.entity instanceof EntityDrone) {
                minigun = ((EntityDrone) this.entity).getMinigun();
            }
        } else if (this.tileEntity != null) {
            if (this.tileEntity.isInvalid()) {
                this.donePlaying = true;
                return;
            }
            if (this.tileEntity instanceof TileEntitySentryTurret) {
                minigun = ((TileEntitySentryTurret) this.tileEntity).getMinigun();
            }
        }
        if (minigun != null) {
            this.donePlaying = !minigun.isMinigunActivated() || minigun.getMinigunSpeed() < Minigun.MAX_GUN_SPEED * 0.9;
        } else {
            this.donePlaying = true;
        }
    }
}
