package me.desht.pneumaticcraft.common.network;

import io.netty.buffer.ByteBuf;
import me.desht.pneumaticcraft.common.block.tubes.TubeModule;
import me.desht.pneumaticcraft.common.tileentity.TileEntityPressureTube;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;

public abstract class PacketUpdateTubeModule<REQ extends PacketUpdateTubeModule<REQ>> extends LocationIntPacket<REQ> {

    protected EnumFacing moduleSide;

    public PacketUpdateTubeModule() {
    }

    public PacketUpdateTubeModule(TubeModule module) {
        super(module.getTube().pos());
        this.moduleSide = module.getDirection();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        super.toBytes(buffer);
        buffer.writeByte((byte) this.moduleSide.ordinal());
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        super.fromBytes(buffer);
        this.moduleSide = EnumFacing.byIndex(buffer.readByte());
    }

    @Override
    public void handleClientSide(REQ message, EntityPlayer player) {
        this.handleServerSide(message, player);
    }

    @Override
    public void handleServerSide(REQ message, EntityPlayer player) {
        TileEntityPressureTube te = TileEntityPressureTube.getTube(message.getTileEntity(player.getEntityWorld()));
        if (te != null) {
            TubeModule module = te.modules[message.moduleSide.ordinal()];
            if (module != null) {
                this.onModuleUpdate(module, message, player);
                if (!player.world.isRemote) NetworkHandler.sendToAllAround(message, player.world);
            }
        }
    }

    protected abstract void onModuleUpdate(TubeModule module, REQ message, EntityPlayer player);

}
