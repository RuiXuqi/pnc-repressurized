package me.desht.pneumaticcraft.common.network;

import io.netty.buffer.ByteBuf;
import me.desht.pneumaticcraft.common.item.ItemPneumaticArmor;
import me.desht.pneumaticcraft.common.util.NBTUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class PacketUpdateSearchStack extends AbstractPacket<PacketUpdateSearchStack> {

    private ItemStack stack;
    private int itemId, itemDamage;

    public PacketUpdateSearchStack() {
    }

    public PacketUpdateSearchStack(ItemStack stack) {
        if (!stack.isEmpty()) {
            this.itemId = Item.getIdFromItem(stack.getItem());
            this.itemDamage = stack.getItemDamage();
        } else {
            this.itemId = this.itemDamage = -1;
        }
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(this.itemId);
        buffer.writeInt(this.itemDamage);
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        this.itemId = buffer.readInt();
        this.itemDamage = buffer.readInt();
    }

    @Override
    public void handleClientSide(PacketUpdateSearchStack message, EntityPlayer player) {
    }

    @Override
    public void handleServerSide(PacketUpdateSearchStack message, EntityPlayer player) {
        ItemStack helmetStack = player.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
        if (!helmetStack.isEmpty()) {
            NBTTagCompound tag = NBTUtil.getCompoundTag(helmetStack, ItemPneumaticArmor.NBT_SEARCH_STACK);
            tag.setInteger("itemID", message.itemId);
            tag.setInteger("itemDamage", message.itemDamage);
        }
    }
}
