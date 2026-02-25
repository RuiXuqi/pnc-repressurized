package me.desht.pneumaticcraft.common.util;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TileEntityCache {
    private TileEntity te;
    private final World world;
    private final BlockPos pos;

    public TileEntityCache(World world, BlockPos pos) {
        this.world = world;
        this.pos = pos;
        this.update();
    }

    public void update() {
        this.te = this.world.isBlockLoaded(this.pos) ? this.world.getTileEntity(this.pos) : null;
    }

    public TileEntity getTileEntity() {
        if (this.te != null && this.te.isInvalid()) this.te = null;
        return this.te;
    }

    public static TileEntityCache[] getDefaultCache(World world, BlockPos pos) {
        TileEntityCache[] cache = new TileEntityCache[6];
        for (int i = 0; i < 6; i++) {
            EnumFacing d = EnumFacing.byIndex(i);
            cache[i] = new TileEntityCache(world, pos.offset(d));
        }
        return cache;
    }

}
