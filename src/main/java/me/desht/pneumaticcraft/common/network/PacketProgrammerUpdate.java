package me.desht.pneumaticcraft.common.network;

import io.netty.buffer.ByteBuf;
import me.desht.pneumaticcraft.common.tileentity.TileEntityProgrammer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.List;

public class PacketProgrammerUpdate extends LocationIntPacket<PacketProgrammerUpdate> implements ILargePayload {
    private NBTTagCompound progWidgets;

    public PacketProgrammerUpdate() {
    }

    public PacketProgrammerUpdate(TileEntityProgrammer te) {
        super(te.getPos());
        this.progWidgets = new NBTTagCompound();
        te.writeProgWidgetsToNBT(this.progWidgets);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        super.toBytes(buffer);
        try {
            new PacketBuffer(buffer).writeCompoundTag(this.progWidgets);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        super.fromBytes(buffer);
        try {
            this.progWidgets = new PacketBuffer(buffer).readCompoundTag();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleClientSide(PacketProgrammerUpdate message, EntityPlayer player) {
        this.handleServerSide(null, player);
    }

    @Override
    public void handleServerSide(PacketProgrammerUpdate message, EntityPlayer player) {
        TileEntity te = message.getTileEntity(player.getEntityWorld());
        if (te instanceof TileEntityProgrammer) {
            ((TileEntityProgrammer) te).readProgWidgetsFromNBT(message.progWidgets);
            ((TileEntityProgrammer) te).saveToHistory();
            if (!player.world.isRemote) {
                message.updateOtherWatchingPlayers((TileEntityProgrammer) te, player);
            }
        }
    }

    private void updateOtherWatchingPlayers(TileEntityProgrammer te, EntityPlayer changingPlayer) {
        List<EntityPlayerMP> players = changingPlayer.world.getEntitiesWithinAABB(EntityPlayerMP.class, new AxisAlignedBB(this.pos.add(-5, -5, -5), this.pos.add(6, 6, 6)));
        for (EntityPlayerMP player : players) {
            if (player != changingPlayer) {
                NetworkHandler.sendTo(new PacketProgrammerUpdate(te), player);
            }
        }
    }

}
