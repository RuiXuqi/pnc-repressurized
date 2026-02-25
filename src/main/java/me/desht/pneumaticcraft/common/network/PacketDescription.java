package me.desht.pneumaticcraft.common.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.desht.pneumaticcraft.common.inventory.ContainerLogistics;
import me.desht.pneumaticcraft.common.semiblock.ISemiBlock;
import me.desht.pneumaticcraft.common.semiblock.SemiBlockManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.util.Arrays;
import java.util.List;

public class PacketDescription extends LocationIntPacket<PacketDescription> {
    private byte[] types;
    private Object[] values;
    private NBTTagCompound extraData;
    private IDescSynced.Type type;

    public PacketDescription() {
    }

    public PacketDescription(IDescSynced te) {
        super(te.getPosition());
        this.type = te.getSyncType();
        this.values = new Object[te.getDescriptionFields().size()];
        this.types = new byte[this.values.length];
        for (int i = 0; i < this.values.length; i++) {
            this.values[i] = te.getDescriptionFields().get(i).getValue();
            this.types[i] = PacketUpdateGui.getType(te.getDescriptionFields().get(i));
        }
        this.extraData = new NBTTagCompound();
        te.writeToPacket(this.extraData);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        super.toBytes(buf);
        buf.writeByte(this.type.ordinal());
        buf.writeInt(this.values.length);
        for (int i = 0; i < this.types.length; i++) {
            buf.writeByte(this.types[i]);
            PacketUpdateGui.writeField(buf, this.values[i], this.types[i]);
        }
        ByteBufUtils.writeTag(buf, this.extraData);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        super.fromBytes(buf);
        this.type = IDescSynced.Type.values()[buf.readByte()];
        int dataAmount = buf.readInt();
        this.types = new byte[dataAmount];
        this.values = new Object[dataAmount];
        for (int i = 0; i < dataAmount; i++) {
            this.types[i] = buf.readByte();
            this.values[i] = PacketUpdateGui.readField(buf, this.types[i]);
        }
        this.extraData = ByteBufUtils.readTag(buf);
    }

    private static Object getSyncableForType(PacketDescription message, EntityPlayer player, IDescSynced.Type type) {
        switch (type) {
            case TILE_ENTITY:
                return message.getTileEntity(player.world);
            case SEMI_BLOCK:
                if (message.pos.equals(BlockPos.ORIGIN)) {
                    Container container = player.openContainer;
                    if (container instanceof ContainerLogistics) {
                        return ((ContainerLogistics) container).logistics;
                    }
                } else {
                    List<ISemiBlock> semiBlocks = SemiBlockManager.getInstance(player.world).getSemiBlocksAsList(player.world, message.pos);
                    int index = message.extraData.getByte("index");
                    return index < semiBlocks.size() ? semiBlocks.get(index) : null;
                }
        }
        return null;
    }

    @Override
    public void handleClientSide(PacketDescription message, EntityPlayer player) {
        if (player.world.isBlockLoaded(message.pos)) {
            Object syncable = getSyncableForType(message, player, message.type);
            if (syncable instanceof IDescSynced) {
                IDescSynced descSynced = (IDescSynced) syncable;
                List<SyncedField> descFields = descSynced.getDescriptionFields();
                if (descFields != null && descFields.size() == message.types.length) {
                    for (int i = 0; i < descFields.size(); i++) {
                        descFields.get(i).setValue(message.values[i]);
                    }
                }
                descSynced.readFromPacket(message.extraData);
                descSynced.onDescUpdate();
            }
        }
    }

    @Override
    public void handleServerSide(PacketDescription message, EntityPlayer player) {
    }

    /********************
     * These two methods are only used for initial chunk sending (getUpdateTag() and handleUpdateTag())
     */

    public NBTTagCompound writeNBT(NBTTagCompound compound) {
        compound.setTag("Pos", NBTUtil.createPosTag(this.pos));
        compound.setInteger("SyncType", this.type.ordinal());
        compound.setInteger("Length", this.values.length);
        ByteBuf buf = Unpooled.buffer();
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < this.types.length; i++) {
            NBTTagCompound element = new NBTTagCompound();
            element.setByte("Type", this.types[i]);
            buf.clear();
            PacketUpdateGui.writeField(buf, this.values[i], this.types[i]);
            element.setByteArray("Value", Arrays.copyOf(buf.array(), buf.writerIndex()));
            list.appendTag(element);
        }
        buf.release();
        compound.setTag("Data", list);
        compound.setTag("Extra", this.extraData);

        return compound;
    }

    public PacketDescription(NBTTagCompound compound) {
        super(NBTUtil.getPosFromTag(compound.getCompoundTag("Pos")));
        this.type = IDescSynced.Type.values()[compound.getInteger("SyncType")];
        this.values = new Object[compound.getInteger("Length")];
        this.types = new byte[this.values.length];
        NBTTagList list = compound.getTagList("Data", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < this.values.length; i++) {
            NBTTagCompound element = list.getCompoundTagAt(i);
            this.types[i] = element.getByte("Type");
            byte[] b = element.getByteArray("Value");
            this.values[i] = PacketUpdateGui.readField(Unpooled.wrappedBuffer(b), this.types[i]);
        }
        this.extraData = compound.getCompoundTag("Extra");
    }
}
