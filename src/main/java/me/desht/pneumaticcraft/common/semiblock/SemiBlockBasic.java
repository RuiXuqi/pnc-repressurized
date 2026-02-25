package me.desht.pneumaticcraft.common.semiblock;

import me.desht.pneumaticcraft.common.network.*;
import me.desht.pneumaticcraft.common.tileentity.IGUIButtonSensitive;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

import static me.desht.pneumaticcraft.common.GuiHandler.EnumGuiId;

public abstract class SemiBlockBasic<TTileEntity extends TileEntity> implements ISemiBlock, IDescSynced, IGUIButtonSensitive {
    private final Class<TTileEntity> tileClass;
    protected World world;
    protected BlockPos pos;
    private int index = -1; //There can be multiple semi blocks in one block.
    private boolean isInvalid;
    private TTileEntity cachedTE;
    private List<SyncedField> descriptionFields;
    private boolean descriptionPacketScheduled;

    public SemiBlockBasic(Class<TTileEntity> tileClass) {
        this.tileClass = tileClass;
    }

    @Override
    public void initialize(World world, BlockPos pos) {
        this.world = world;
        this.pos = pos;
    }

    @Override
    public int getIndex() {
        if (this.index == -1) {
            this.index = SemiBlockManager.getInstance(this.world).getSemiBlocksAsList(this.world, this.getPos()).indexOf(this);
            if (this.index == -1) throw new IllegalStateException("Semi block is not part of the world! " + this);
        }
        return this.index;
    }

    @Override
    public void onSemiBlockRemovedFromThisPos(ISemiBlock semiBlock) {
        this.index = -1; //Invalidate cache, only update on removing, because added semiblocks are appended to the back, not influencing the index.
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {

    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {

    }

    @Override
    public void update() {
        if (!this.world.isRemote && !this.canStay()) this.drop();
        if (!this.world.isRemote && !this.isInvalid()) {
            if (this.descriptionFields == null) this.descriptionPacketScheduled = true;
            for (SyncedField field : this.getDescriptionFields()) {
                if (field.update()) {
                    this.descriptionPacketScheduled = true;
                }
            }

            if (this.descriptionPacketScheduled) {
                this.descriptionPacketScheduled = false;
                this.sendDescriptionPacket();
            }
        }
    }

    private void sendDescriptionPacket() {
        NetworkHandler.sendToAllAround(this.getDescriptionPacket(), this.world);
    }

    @Override
    public PacketDescription getDescriptionPacket() {
        return new PacketDescription(this);
    }

    @Override
    public BlockPos getPosition() {
        return this.getPos();
    }

    protected void drop() {
        SemiBlockManager.getInstance(this.world).breakSemiBlock(this);
    }

    protected boolean isAirBlock() {
        return this.world.isAirBlock(this.pos);
    }

    public IBlockState getBlockState() {
        return this.world.getBlockState(this.pos);
    }

    public boolean isAir() {
        IBlockState state = this.getBlockState();
        return state.getBlock().isAir(state, this.world, this.pos);
    }

    @SuppressWarnings("unchecked")
    public TTileEntity getTileEntity() {
        if (this.cachedTE == null || this.cachedTE.isInvalid()) {
            TileEntity te = this.world.getTileEntity(this.pos);
            if (te != null && this.tileClass.isAssignableFrom(te.getClass())) {
                this.cachedTE = (TTileEntity) te;
            } else {
                this.cachedTE = null;
            }
        }
        return this.cachedTE;
    }

    @Override
    public void invalidate() {
        this.isInvalid = true;
    }

    @Override
    public boolean isInvalid() {
        return this.isInvalid;
    }

    @Override
    public World getWorld() {
        return this.world;
    }

    @Override
    public BlockPos getPos() {
        return this.pos;
    }

    @Override
    public void addDrops(NonNullList<ItemStack> drops) {
        Item item = SemiBlockManager.getItemForSemiBlock(this);
        if (item != null) drops.add(new ItemStack(item));
    }

    @Override
    public boolean canPlace(EnumFacing facing) {
        return true;
    }

    @Override
    public void prePlacement(EntityPlayer player, ItemStack stack, EnumFacing facing) {

    }

    @Override
    public void onPlaced(EntityPlayer player, ItemStack stack, EnumFacing facing) {

    }

    public boolean canStay() {
        return this.canPlace(null);
    }

    @Override
    public boolean onRightClickWithConfigurator(EntityPlayer player, EnumFacing side) {
        return false;
    }

    public EnumGuiId getGuiID() {
        return null;
    }

    /**
     * Add information for the benefit of info mods such as TOP or WAILA/HWYLA.
     * Adds nothing by default; subclasses will override this. Note: the semiblock name is expected to be added by
     * the caller. NOTE: this can be called on the server too (TOP) so don't use any client-only methods (I18n.format)
     *
     * @param curInfo  list to add info to
     * @param tag      NBT data from the semiblock in question containing extra info
     * @param extended show extended data?
     */
    public void addTooltip(List<String> curInfo, NBTTagCompound tag, boolean extended) {
    }

    public void addWailaInfoToTag(NBTTagCompound tag) {

    }

    @Override
    public Type getSyncType() {
        return Type.SEMI_BLOCK;
    }

    @Override
    public List<SyncedField> getDescriptionFields() {
        if (this.descriptionFields == null) {
            this.descriptionFields = NetworkUtils.getSyncedFields(this, DescSynced.class);
            for (SyncedField field : this.descriptionFields) {
                field.update();
            }
        }
        return this.descriptionFields;
    }


    @Override
    public void writeToPacket(NBTTagCompound tag) {
        tag.setByte("index", (byte) this.getIndex()); //Used in packet decoding to figure out which semiblock updated.
    }

    @Override
    public void readFromPacket(NBTTagCompound tag) {

    }

    @Override
    public void onDescUpdate() {
    }

    @Override
    public void handleGUIButtonPress(int guiID, EntityPlayer player) {

    }

    @Override
    public String toString() {
        return String.format("Pos: %s, %s", this.getPos(), this.getClass());
    }
}
