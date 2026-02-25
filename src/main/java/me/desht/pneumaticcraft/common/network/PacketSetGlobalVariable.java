package me.desht.pneumaticcraft.common.network;

import io.netty.buffer.ByteBuf;
import me.desht.pneumaticcraft.client.gui.GuiRemote;
import me.desht.pneumaticcraft.common.remote.GlobalVariableManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PacketSetGlobalVariable extends AbstractPacket<PacketSetGlobalVariable> {
    private String varName;
    private BlockPos value;

    public PacketSetGlobalVariable() {
    }

    public PacketSetGlobalVariable(String varName, BlockPos value) {
        this.varName = varName;
        this.value = value;
    }

    public PacketSetGlobalVariable(String varName, int value) {
        this(varName, new BlockPos(value, 0, 0));
    }

    public PacketSetGlobalVariable(String varName, boolean value) {
        this(varName, value ? 1 : 0);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.varName = ByteBufUtils.readUTF8String(buf);
        this.value = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.varName);
        buf.writeInt(this.value.getX());
        buf.writeInt(this.value.getY());
        buf.writeInt(this.value.getZ());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleClientSide(PacketSetGlobalVariable message, EntityPlayer player) {
        this.handleServerSide(message, player);
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (screen instanceof GuiRemote) {
            ((GuiRemote) screen).onGlobalVariableChange(message.varName);
        }
    }

    @Override
    public void handleServerSide(PacketSetGlobalVariable message, EntityPlayer player) {
        GlobalVariableManager.getInstance().set(message.varName, message.value);
    }

}
