package me.desht.pneumaticcraft.client.sound;

import me.desht.pneumaticcraft.common.tileentity.TileEntityElevatorBase;
import me.desht.pneumaticcraft.lib.Sounds;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.SoundCategory;

public class MovingSoundElevator extends MovingSound {
    private final TileEntityElevatorBase te;

    protected MovingSoundElevator(TileEntityElevatorBase te) {
        super(Sounds.ELEVATOR_MOVING, SoundCategory.BLOCKS);
        this.te = te;
        this.xPosF = te.getPos().getX();
        this.yPosF = te.getPos().getY() + te.extension / 2;
        this.zPosF = te.getPos().getZ();
        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = 0.9F - (0.05F * Math.min(8, te.multiElevatorCount));
    }

    @Override
    public void update() {
        if (this.te.isInvalid() || this.te.extension == this.te.getTargetExtension()) {
            this.donePlaying = true;
        }
        this.yPosF = this.te.getPos().getY() + this.te.extension / 2;
    }
}
