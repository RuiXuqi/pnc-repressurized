package me.desht.pneumaticcraft.common.network;

import io.netty.buffer.ByteBuf;
import me.desht.pneumaticcraft.PneumaticCraftRepressurized;
import me.desht.pneumaticcraft.lib.EnumCustomParticleType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;

/**
 * MineChess
 *
 * @author MineMaarten
 * www.minemaarten.com
 * @license Lesser GNU Public License v3 (http://www.gnu.org/licenses/lgpl.html)
 */

public class PacketSpawnParticle extends LocationDoublePacket<PacketSpawnParticle> {

    private double dx, dy, dz;
    private int particleId;
    private int numParticles;
    private double rx, ry, rz;

    public PacketSpawnParticle() {
    }

    public PacketSpawnParticle(EnumParticleTypes particle, double x, double y, double z, double dx, double dy, double dz) {
        super(x, y, z);
        this.particleId = particle.ordinal();
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.numParticles = 1;
        this.rx = this.ry = this.rz = 0d;
    }

    public PacketSpawnParticle(EnumParticleTypes particle, double x, double y, double z, double dx, double dy, double dz, int numParticles, double rx, double ry, double rz) {
        this(particle, x, y, z, dx, dy, dz);
        this.numParticles = numParticles;
        this.rx = rx;
        this.ry = ry;
        this.rz = rz;
    }

    public PacketSpawnParticle(EnumCustomParticleType particle, double x, double y, double z, double dx, double dy, double dz) {
        super(x, y, z);
        this.particleId = EnumParticleTypes.values().length + particle.ordinal();
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.numParticles = 1;
        this.rx = this.ry = this.rz = 0d;
    }

    public PacketSpawnParticle(EnumCustomParticleType particle, double x, double y, double z, double dx, double dy, double dz, int numParticles, double rx, double ry, double rz) {
        this(particle, x, y, z, dx, dy, dz);
        this.numParticles = numParticles;
        this.rx = rx;
        this.ry = ry;
        this.rz = rz;
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        super.toBytes(buffer);
        buffer.writeInt(this.particleId);
        buffer.writeDouble(this.dx);
        buffer.writeDouble(this.dy);
        buffer.writeDouble(this.dz);
        buffer.writeInt(this.numParticles);
        if (this.numParticles > 1) {
            buffer.writeDouble(this.rx);
            buffer.writeDouble(this.ry);
            buffer.writeDouble(this.rz);
        }
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        super.fromBytes(buffer);
        this.particleId = buffer.readInt();
        this.dx = buffer.readDouble();
        this.dy = buffer.readDouble();
        this.dz = buffer.readDouble();
        this.numParticles = buffer.readInt();
        if (this.numParticles > 1) {
            this.rx = buffer.readDouble();
            this.ry = buffer.readDouble();
            this.rz = buffer.readDouble();
        }
    }

    @Override
    public void handleClientSide(PacketSpawnParticle message, EntityPlayer player) {
        for (int i = 0; i < this.numParticles; i++) {
            double x = message.x + (this.numParticles == 1 ? 0 : player.world.rand.nextDouble() * this.rx);
            double y = message.y + (this.numParticles == 1 ? 0 : player.world.rand.nextDouble() * this.ry);
            double z = message.z + (this.numParticles == 1 ? 0 : player.world.rand.nextDouble() * this.rz);
            if (this.particleId >= EnumParticleTypes.values().length) {
                EnumCustomParticleType particle = EnumCustomParticleType.values()[message.particleId - EnumParticleTypes.values().length];
                PneumaticCraftRepressurized.proxy.playCustomParticle(particle, player.world, x, y, z, message.dx, message.dy, message.dz);
            } else {
                player.world.spawnParticle(EnumParticleTypes.values()[message.particleId], x, y, z, message.dx, message.dy, message.dz);
            }
        }
    }

    @Override
    public void handleServerSide(PacketSpawnParticle message, EntityPlayer player) {
    }

}
