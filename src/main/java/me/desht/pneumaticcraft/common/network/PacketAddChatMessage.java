package me.desht.pneumaticcraft.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.ByteBufUtils;

/**
 * MineChess
 *
 * @author MineMaarten
 * www.minemaarten.com
 * @license Lesser GNU Public License v3 (http://www.gnu.org/licenses/lgpl.html)
 */

public class PacketAddChatMessage extends AbstractPacket<PacketAddChatMessage> {

    private String message;
    private String[] replacements;

    public PacketAddChatMessage() {
    }

    public PacketAddChatMessage(String message, String... replacements) {
        this.message = message;
        this.replacements = replacements;
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        ByteBufUtils.writeUTF8String(buffer, this.message);
        if (this.replacements != null) {
            buffer.writeInt(this.replacements.length);
            for (String replacement : this.replacements) {
                ByteBufUtils.writeUTF8String(buffer, replacement);
            }
        } else {
            buffer.writeInt(0);
        }
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        this.message = ByteBufUtils.readUTF8String(buffer);
        this.replacements = new String[buffer.readInt()];
        for (int i = 0; i < this.replacements.length; i++) {
            this.replacements[i] = ByteBufUtils.readUTF8String(buffer);
        }
    }

    @Override
    public void handleClientSide(PacketAddChatMessage message, EntityPlayer player) {
        //      player.addChatComponentMessage(new ChatComponentTranslation(LocalizationHandler.getStringFromUnlocalizedParts(message, replacements), new Object[0]));
    }

    @Override
    public void handleServerSide(PacketAddChatMessage message, EntityPlayer player) {
    }

}
