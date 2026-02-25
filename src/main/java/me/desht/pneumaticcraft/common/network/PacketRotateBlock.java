package me.desht.pneumaticcraft.common.network;

import io.netty.buffer.ByteBuf;
import me.desht.pneumaticcraft.api.block.IPneumaticWrenchable;
import me.desht.pneumaticcraft.common.thirdparty.ModdedWrenchUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;

public class PacketRotateBlock extends LocationIntPacket<PacketRotateBlock> {
    private EnumFacing side;
    private EnumHand hand;
    private int entityID;

    @SuppressWarnings("unused")
    public PacketRotateBlock() {
    }

    public PacketRotateBlock(BlockPos pos, EnumFacing side, EnumHand hand) {
        super(pos);
        this.side = side;
        this.hand = hand;
        this.entityID = -1;
    }

    public PacketRotateBlock(BlockPos pos, EnumHand hand, int entityID) {
        super(pos);
        this.side = null;
        this.hand = hand;
        this.entityID = entityID;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        super.toBytes(buf);
        buf.writeByte(this.hand.ordinal());
        if (this.entityID >= 0) {
            buf.writeBoolean(true);
            buf.writeInt(this.entityID);
        } else {
            buf.writeBoolean(false);
            buf.writeByte(this.side.ordinal());
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        super.fromBytes(buf);
        this.hand = EnumHand.values()[buf.readByte()];
        if (buf.readBoolean()) {
            this.entityID = buf.readInt();
            this.side = null;
        } else {
            this.side = EnumFacing.values()[buf.readByte()];
            this.entityID = -1;
        }

    }

    @Override
    public void handleClientSide(PacketRotateBlock message, EntityPlayer player) {
        // empty
    }

    @Override
    public void handleServerSide(PacketRotateBlock message, EntityPlayer player) {
        if (player.world.isBlockLoaded(message.pos) && player.getDistanceSq(message.pos) < 64) {
            if (ModdedWrenchUtils.getInstance().isModdedWrench(player.getHeldItem(message.hand))) {
                if (message.entityID >= 0) {
                    Entity e = player.world.getEntityByID(message.entityID);
                    if (e instanceof IPneumaticWrenchable && e.isEntityAlive()) {
                        ((IPneumaticWrenchable) e).rotateBlock(player.world, player, message.pos, message.side, message.hand);
                    }
                } else if (message.side != null) {
                    IBlockState state = player.world.getBlockState(message.pos);
                    if (state.getBlock() instanceof IPneumaticWrenchable) {
                        ((IPneumaticWrenchable) state.getBlock()).rotateBlock(player.world, player, message.pos, message.side, message.hand);
                    }
                }
            }
        }
    }
}
