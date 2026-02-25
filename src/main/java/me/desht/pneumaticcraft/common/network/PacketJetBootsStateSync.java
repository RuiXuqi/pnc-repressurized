package me.desht.pneumaticcraft.common.network;

import io.netty.buffer.ByteBuf;
import me.desht.pneumaticcraft.common.pneumatic_armor.JetBootsStateTracker;
import net.minecraft.entity.player.EntityPlayer;

import java.util.UUID;

/**
 * Sent from server to clients so that all clients know the jet boots state (enabled/active) of all other clients.
 * Allows us to play particles, do rotations, etc. with minimum traffic.
 */
public class PacketJetBootsStateSync extends AbstractPacket<PacketJetBootsStateSync> {
    private UUID playerId;
    private JetBootsStateTracker.JetBootsState state;

    public PacketJetBootsStateSync() {
    }

    public PacketJetBootsStateSync(EntityPlayer player, JetBootsStateTracker.JetBootsState state) {
        this.playerId = player.getUniqueID();
        this.state = state;
    }

    @Override
    public void handleClientSide(PacketJetBootsStateSync message, EntityPlayer player) {
        JetBootsStateTracker.getTracker(player).setJetBootsState(message.playerId, message.state);
    }

    @Override
    public void handleServerSide(PacketJetBootsStateSync message, EntityPlayer player) {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.playerId = new UUID(buf.readLong(), buf.readLong());
        this.state = new JetBootsStateTracker.JetBootsState(buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(this.playerId.getMostSignificantBits());
        buf.writeLong(this.playerId.getLeastSignificantBits());
        buf.writeBoolean(this.state.isEnabled());
        buf.writeBoolean(this.state.isActive());
        buf.writeBoolean(this.state.isBuilderMode());
    }
}
