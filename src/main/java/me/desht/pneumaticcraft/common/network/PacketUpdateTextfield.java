package me.desht.pneumaticcraft.common.network;

import io.netty.buffer.ByteBuf;
import me.desht.pneumaticcraft.common.tileentity.IGUITextFieldSensitive;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public class PacketUpdateTextfield extends LocationIntPacket<PacketUpdateTextfield> {

    private int textFieldID;
    private String text;

    public PacketUpdateTextfield() {
    }

    public PacketUpdateTextfield(TileEntity te, int textfieldID) {
        super(te.getPos());
        this.textFieldID = textfieldID;
        this.text = ((IGUITextFieldSensitive) te).getText(textfieldID);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        super.toBytes(buffer);
        buffer.writeInt(this.textFieldID);
        ByteBufUtils.writeUTF8String(buffer, this.text);
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        super.fromBytes(buffer);
        this.textFieldID = buffer.readInt();
        this.text = ByteBufUtils.readUTF8String(buffer);
    }

    @Override
    public void handleClientSide(PacketUpdateTextfield message, EntityPlayer player) {
    }

    @Override
    public void handleServerSide(PacketUpdateTextfield message, EntityPlayer player) {
        TileEntity te = message.getTileEntity(player.getEntityWorld());
        if (te instanceof IGUITextFieldSensitive) {
            ((IGUITextFieldSensitive) te).setText(message.textFieldID, message.text);
        }
    }

}
