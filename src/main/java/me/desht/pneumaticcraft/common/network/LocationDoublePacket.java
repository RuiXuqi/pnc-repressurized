package me.desht.pneumaticcraft.common.network;

import io.netty.buffer.ByteBuf;
import me.desht.pneumaticcraft.lib.TileEntityConstants;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

/**
 * MineChess
 *
 * @author MineMaarten
 * www.minemaarten.com
 * @license Lesser GNU Public License v3 (http://www.gnu.org/licenses/lgpl.html)
 */

public abstract class LocationDoublePacket<REQ extends AbstractPacket<REQ>> extends AbstractPacket<REQ> {

    protected double x, y, z;

    LocationDoublePacket() {
    }

    LocationDoublePacket(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
    }

    NetworkRegistry.TargetPoint getTargetPoint(World world) {
        return new NetworkRegistry.TargetPoint(world.provider.getDimension(), this.x, this.y, this.z, TileEntityConstants.PACKET_UPDATE_DISTANCE);
    }
}
