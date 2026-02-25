package me.desht.pneumaticcraft.common.network;

import io.netty.buffer.ByteBuf;
import me.desht.pneumaticcraft.common.block.tubes.ModuleRedstone;
import me.desht.pneumaticcraft.common.block.tubes.TubeModule;
import me.desht.pneumaticcraft.common.tileentity.TileEntityPressureTube;
import net.minecraft.entity.player.EntityPlayer;

public class PacketSyncRedstoneModuleToClient extends LocationIntPacket<PacketSyncRedstoneModuleToClient> {
    private ModuleRedstone.EnumRedstoneDirection dir;
    private int outputLevel;
    private int inputLevel;
    private int channel;
    private byte side;

    @SuppressWarnings("unused")
    public PacketSyncRedstoneModuleToClient() {
    }

    public PacketSyncRedstoneModuleToClient(ModuleRedstone module) {
        super(module.getTube().pos());

        this.dir = module.getRedstoneDirection();
        this.outputLevel = module.getRedstoneLevel();
        this.inputLevel = module.getInputLevel();
        this.channel = module.getColorChannel();
        this.side = (byte) module.getDirection().getIndex();
    }

    @Override
    public void handleClientSide(PacketSyncRedstoneModuleToClient message, EntityPlayer player) {
        TileEntityPressureTube te = TileEntityPressureTube.getTube(message.getTileEntity(player.world));
        if (te != null) {
            TubeModule module = te.modules[message.side];
            if (module instanceof ModuleRedstone) {
                ModuleRedstone mr = (ModuleRedstone) module;
                mr.setColorChannel(message.channel);
                mr.setRedstoneDirection(message.dir);
                mr.setOutputLevel(message.outputLevel);
                mr.setInputLevel(message.inputLevel);
            }
        }
    }

    @Override
    public void handleServerSide(PacketSyncRedstoneModuleToClient message, EntityPlayer player) {
        // empty
    }

    @Override
    public void toBytes(ByteBuf buf) {
        super.toBytes(buf);

        buf.writeByte(this.dir.ordinal());
        buf.writeByte(this.side);
        buf.writeByte(this.outputLevel);
        buf.writeByte(this.inputLevel);
        buf.writeByte(this.channel);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        super.fromBytes(buf);

        this.dir = ModuleRedstone.EnumRedstoneDirection.values()[buf.readByte()];
        this.side = buf.readByte();
        this.outputLevel = buf.readByte();
        this.inputLevel = buf.readByte();
        this.channel = buf.readByte();
    }
}
