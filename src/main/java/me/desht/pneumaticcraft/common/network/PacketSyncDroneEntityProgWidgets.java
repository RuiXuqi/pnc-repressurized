package me.desht.pneumaticcraft.common.network;

import io.netty.buffer.ByteBuf;
import me.desht.pneumaticcraft.common.entity.living.EntityDrone;
import me.desht.pneumaticcraft.common.progwidgets.IProgWidget;
import me.desht.pneumaticcraft.common.tileentity.TileEntityProgrammer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.util.List;

public class PacketSyncDroneEntityProgWidgets extends AbstractPacket<PacketSyncDroneEntityProgWidgets> {

    private List<IProgWidget> progWidgets;
    private int entityId;

    public PacketSyncDroneEntityProgWidgets() {

    }

    public PacketSyncDroneEntityProgWidgets(EntityDrone drone) {
        this.progWidgets = drone.getProgWidgets();
        this.entityId = drone.getEntityId();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.progWidgets = TileEntityProgrammer.getWidgetsFromNBT(ByteBufUtils.readTag(buf));
        this.entityId = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        NBTTagCompound tag = new NBTTagCompound();
        TileEntityProgrammer.setWidgetsToNBT(this.progWidgets, tag);
        ByteBufUtils.writeTag(buf, tag);

        buf.writeInt(this.entityId);
    }

    @Override
    public void handleClientSide(PacketSyncDroneEntityProgWidgets message, EntityPlayer player) {
        Entity entity = player.world.getEntityByID(message.entityId);
        if (entity instanceof EntityDrone) {
            EntityDrone drone = (EntityDrone) entity;
            List<IProgWidget> widgets = drone.getProgWidgets();
            widgets.clear();
            widgets.addAll(message.progWidgets);
        }
    }

    @Override
    public void handleServerSide(PacketSyncDroneEntityProgWidgets message, EntityPlayer player) {
    }

}
