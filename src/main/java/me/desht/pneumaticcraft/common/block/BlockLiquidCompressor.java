package me.desht.pneumaticcraft.common.block;

import me.desht.pneumaticcraft.common.GuiHandler.EnumGuiId;
import me.desht.pneumaticcraft.common.tileentity.TileEntityLiquidCompressor;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;

public class BlockLiquidCompressor extends BlockPneumaticCraftModeled {

    BlockLiquidCompressor() {
        super(Material.IRON, "liquid_compressor");
    }

    @Override
    protected Class<? extends TileEntity> getTileEntityClass() {
        return TileEntityLiquidCompressor.class;
    }

    @Override
    public boolean isRotatable() {
        return true;
    }

    @Override
    public EnumGuiId getGuiID() {
        return EnumGuiId.LIQUID_COMPRESSOR;
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }
}
