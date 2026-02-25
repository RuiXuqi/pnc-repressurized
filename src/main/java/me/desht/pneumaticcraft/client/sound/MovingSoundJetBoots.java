package me.desht.pneumaticcraft.client.sound;

import me.desht.pneumaticcraft.common.pneumatic_armor.CommonArmorHandler;
import me.desht.pneumaticcraft.lib.Sounds;
import net.minecraft.block.material.Material;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;

public class MovingSoundJetBoots extends MovingSound {
    private final EntityPlayer player;
    private final CommonArmorHandler handler;
    private float targetPitch;
    private int endTimer = -1;

    public MovingSoundJetBoots(EntityPlayer player) {
        super(Sounds.LEAKING_GAS_LOW, SoundCategory.NEUTRAL);

        this.player = player;
        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = 0.5F;
        this.targetPitch = 0.7F;
        this.pitch = 0.4F;

        this.handler = CommonArmorHandler.getHandlerForPlayer(player);
    }

    @Override
    public void update() {
        if (!this.handler.isValid() || !this.handler.isArmorEnabled()) {
            // handler gets invalidated if the tracked player disconnects
            this.donePlaying = true;
            return;
        }

        if (!this.handler.isJetBootsEnabled() && this.endTimer == -1 || !this.handler.isJetBootsActive() && this.player.onGround && this.endTimer == -1) {
            this.endTimer = 20;
        }
        if (this.endTimer > 0 && --this.endTimer <= 0) {
            this.donePlaying = true;
        }

        this.xPosF = (float) this.player.posX;
        this.yPosF = (float) this.player.posY;
        this.zPosF = (float) this.player.posZ;

        if (this.endTimer > 0) {
            this.targetPitch = 0.5F;
            this.volume = 0.5F - ((20 - this.endTimer) / 50F);
        } else {
            if (this.handler.isJetBootsActive()) {
                double vel = Math.sqrt(this.player.motionX * this.player.motionX + this.player.motionY * this.player.motionY + this.player.motionZ * this.player.motionZ);
                this.targetPitch = 0.7F + (float) vel / 15;
                this.volume = 0.5F + (float) vel / 15;
            } else {
                this.targetPitch = 0.5F;
                this.volume = 0.4F;
            }
        }
        this.pitch += (this.targetPitch - this.pitch) / 10F;
        if (this.player.isInsideOfMaterial(Material.WATER)) {
            this.pitch *= 0.75f;
            this.volume *= 0.5f;
        }
    }
}
