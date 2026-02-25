package me.desht.pneumaticcraft.api.heat;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Extend this class, and register it via
 * {@link me.desht.pneumaticcraft.api.tileentity.IHeatRegistry#registerHeatBehaviour(Class heatBehaviour)}
 * <p>
 * This can be used to add heat dependent logic to non-TE's or blocks you don't have access to. For example,
 * PneumaticCraft uses this to power Furnaces with heat, and to turn Lava into Obsidian when heat is drained.
 * This only works for ticking heat logic, not for static heat sources like lava blocks.
 */
public abstract class HeatBehaviour<Tile extends TileEntity> {

    private String id;
    private IHeatExchangerLogic connectedHeatLogic;
    private World world;
    private BlockPos pos;
    private Tile cachedTE;
    private IBlockState blockState;
    private EnumFacing direction;  // direction of this behaviour from the tile entity's PoV

    /**
     * Called by the connected IHeatExchangerLogic.
     *
     * @param id                 ID of this behaviour; can be used to
     * @param connectedHeatLogic
     * @param world
     * @param pos
     * @param direction          direction of this behaviour from the tile entity's PoV
     */
    public void initialize(String id, IHeatExchangerLogic connectedHeatLogic, World world, BlockPos pos, EnumFacing direction) {
        this.connectedHeatLogic = connectedHeatLogic;
        this.world = world;
        this.pos = pos;
        this.direction = direction;
        this.cachedTE = null;
        this.blockState = null;
    }

    public IHeatExchangerLogic getHeatExchanger() {
        return this.connectedHeatLogic;
    }

    public World getWorld() {
        return this.world;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public EnumFacing getDirection() {
        return this.direction;
    }

    public Tile getTileEntity() {
        if (this.cachedTE == null || this.cachedTE.isInvalid()) this.cachedTE = (Tile) this.world.getTileEntity(this.pos);
        return this.cachedTE;
    }

    public IBlockState getBlockState() {
        if (this.blockState == null) this.blockState = this.world.getBlockState(this.pos);
        return this.blockState;
    }

    /**
     * Unique id for this behaviour. Used in NBT saving. I recommend prefixing it with your modid.
     *
     * @return a unique ID
     */
    public abstract String getId();

    /**
     * Return true when this heat behaviour is applicable for this coordinate. World access methods can be used here
     * (getWorld(), getPos(), getBlock(), getTileEntity()).
     *
     * @return true if this behaviour is applicable here
     */
    public abstract boolean isApplicable();

    /**
     * Called every tick to update this behaviour.
     */
    public abstract void update();

    public void writeToNBT(NBTTagCompound tag) {
        tag.setInteger("x", this.pos.getX());
        tag.setInteger("y", this.pos.getY());
        tag.setInteger("z", this.pos.getZ());
    }

    public void readFromNBT(NBTTagCompound tag) {
        this.pos = new BlockPos(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z"));
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof HeatBehaviour) {
            HeatBehaviour behaviour = (HeatBehaviour) o;
            return behaviour.getId().equals(this.getId()) && behaviour.getPos().equals(this.getPos());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int i = this.getId().hashCode();
        i = i * 31 + this.getPos().hashCode();
        return i;
    }
}
