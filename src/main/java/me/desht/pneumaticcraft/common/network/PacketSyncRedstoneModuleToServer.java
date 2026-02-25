package me.desht.pneumaticcraft.common.network;

import io.netty.buffer.ByteBuf;
import me.desht.pneumaticcraft.common.block.tubes.ModuleRedstone;
import me.desht.pneumaticcraft.common.block.tubes.TubeModule;
import me.desht.pneumaticcraft.common.tileentity.TileEntityPressureTube;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;

public class PacketSyncRedstoneModuleToServer extends LocationIntPacket<PacketSyncRedstoneModuleToServer> {
    private byte side;
    private byte op;
    private byte ourColor;
    private byte otherColor;
    private byte constantVal;
    private boolean invert;

    @SuppressWarnings("unused")
    public PacketSyncRedstoneModuleToServer() {
    }

    public PacketSyncRedstoneModuleToServer(ModuleRedstone module) {
        super(module.getTube().pos());

        this.side = (byte) module.getDirection().ordinal();
        this.op = (byte) module.getOperation().ordinal();
        this.ourColor = (byte) module.getColorChannel();
        this.otherColor = (byte) module.getOtherColor();
        this.constantVal = (byte) module.getConstantVal();
        this.invert = module.isInvert();
    }

    @Override
    public void handleClientSide(PacketSyncRedstoneModuleToServer message, EntityPlayer player) {
        // empty
    }

    @Override
    public void handleServerSide(PacketSyncRedstoneModuleToServer message, EntityPlayer player) {
        TileEntity te = player.world.getTileEntity(this.pos);
        if (te instanceof TileEntityPressureTube) {
            TubeModule m = ((TileEntityPressureTube) te).modules[message.side];
            if (m instanceof ModuleRedstone) {
                ModuleRedstone mr = (ModuleRedstone) m;
                mr.setColorChannel(message.ourColor);
                mr.setInvert(this.invert);
                mr.setOperation(ModuleRedstone.Operation.values()[message.op], message.otherColor, message.constantVal);
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        super.toBytes(buf);

        buf.writeByte(this.side);
        buf.writeByte(this.op);
        buf.writeByte(this.ourColor);
        buf.writeByte(this.otherColor);
        buf.writeByte(this.constantVal);
        buf.writeBoolean(this.invert);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        super.fromBytes(buf);

        this.side = buf.readByte();
        this.op = buf.readByte();
        this.ourColor = buf.readByte();
        this.otherColor = buf.readByte();
        this.constantVal = buf.readByte();
        this.invert = buf.readBoolean();
    }
}
