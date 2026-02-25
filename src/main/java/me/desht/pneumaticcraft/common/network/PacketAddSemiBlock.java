package me.desht.pneumaticcraft.common.network;

import io.netty.buffer.ByteBuf;
import me.desht.pneumaticcraft.common.semiblock.ISemiBlock;
import me.desht.pneumaticcraft.common.semiblock.SemiBlockManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import org.apache.commons.lang3.Validate;

public class PacketAddSemiBlock extends LocationIntPacket<PacketAddSemiBlock> {

    private String id;

    public PacketAddSemiBlock() {
    }

    public PacketAddSemiBlock(ISemiBlock semiBlock) {
        this(semiBlock.getPos(), semiBlock);
    }

    public PacketAddSemiBlock(BlockPos pos, ISemiBlock semiBlock) {
        super(pos);
        Validate.notNull(semiBlock);
        this.id = SemiBlockManager.getKeyForSemiBlock(semiBlock);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        super.toBytes(buf);
        ByteBufUtils.writeUTF8String(buf, this.id);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        super.fromBytes(buf);
        this.id = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void handleClientSide(PacketAddSemiBlock message, EntityPlayer player) {
        SemiBlockManager.getInstance(player.world).addSemiBlock(player.world, message.pos, SemiBlockManager.getSemiBlockForKey(message.id));
    }

    @Override
    public void handleServerSide(PacketAddSemiBlock message, EntityPlayer player) {

    }

}
