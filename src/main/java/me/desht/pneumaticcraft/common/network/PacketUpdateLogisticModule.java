package me.desht.pneumaticcraft.common.network;

import io.netty.buffer.ByteBuf;
import me.desht.pneumaticcraft.common.block.tubes.ModuleLogistics;
import me.desht.pneumaticcraft.common.block.tubes.TubeModule;
import me.desht.pneumaticcraft.common.tileentity.TileEntityPressureTube;
import net.minecraft.entity.player.EntityPlayer;

public class PacketUpdateLogisticModule extends LocationIntPacket<PacketUpdateLogisticModule> {

    private int side;
    private int colorIndex;
    private int status;

    public PacketUpdateLogisticModule() {
    }

    public PacketUpdateLogisticModule(ModuleLogistics logisticsModule, int action) {
        super(logisticsModule.getTube().pos());
        this.side = logisticsModule.getDirection().ordinal();
        this.colorIndex = logisticsModule.getColorChannel();
        if (action > 0) {
            this.status = 1 + action;
        } else {
            this.status = logisticsModule.hasPower() ? 1 : 0;
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        super.toBytes(buf);
        buf.writeByte(this.side);
        buf.writeByte(this.colorIndex);
        buf.writeByte(this.status);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        super.fromBytes(buf);
        this.side = buf.readByte();
        this.colorIndex = buf.readByte();
        this.status = buf.readByte();
    }

    @Override
    public void handleClientSide(PacketUpdateLogisticModule message, EntityPlayer player) {
        TileEntityPressureTube te = TileEntityPressureTube.getTube(message.getTileEntity(player.world));
        if (te != null) {
            TubeModule module = te.modules[message.side];
            if (module instanceof ModuleLogistics) {
                ((ModuleLogistics) module).onUpdatePacket(message.status, message.colorIndex);
            }
        }
    }

    @Override
    public void handleServerSide(PacketUpdateLogisticModule message, EntityPlayer player) {

    }

}
