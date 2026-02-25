package me.desht.pneumaticcraft.common.entity.living;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public class DebugEntry /*implements Comparable<DebugEntry>*/ {
    private final int progWidgetId;
    private final String message;
    private final BlockPos pos;
    private long receivedTime; // timestamp for when packet was received on client

    /**
     * Called server side when a debug message is added to a drone.
     *
     * @param message      the message text
     * @param progWidgetId a programming widget ID
     * @param pos          block position
     */
    DebugEntry(String message, int progWidgetId, BlockPos pos) {
        this.message = message;
        this.pos = pos != null ? pos : BlockPos.ORIGIN;
        this.progWidgetId = progWidgetId;
    }

    /**
     * Called client-side when a message is synced.
     *
     * @param buf message buffer
     */
    public DebugEntry(ByteBuf buf) {
        this.message = ByteBufUtils.readUTF8String(buf);
        this.pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        this.progWidgetId = buf.readInt();
        this.receivedTime = System.currentTimeMillis();
    }

    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.message);
        buf.writeInt(this.pos.getX());
        buf.writeInt(this.pos.getY());
        buf.writeInt(this.pos.getZ());
        buf.writeInt(this.progWidgetId);
    }

    public String getMessage() {
        return this.message;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public int getProgWidgetId() {
        return this.progWidgetId;
    }

    public long getReceivedTime() {
        return this.receivedTime;
    }

    public boolean hasCoords() {
        return this.pos.getX() != 0 || this.pos.getY() != 0 || this.pos.getZ() != 0;
    }

}
