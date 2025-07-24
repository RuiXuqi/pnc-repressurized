package me.desht.pneumaticcraft.common.block;

import me.desht.pneumaticcraft.common.GuiHandler.EnumGuiId;
import me.desht.pneumaticcraft.common.tileentity.TileEntityFluxCompressor;
import me.desht.pneumaticcraft.common.tileentity.TileEntityPneumaticDynamo;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class BlockFluxCompressor extends BlockPneumaticCraftModeled {
    private static final PropertyBool ACTIVE = PropertyBool.create("active");

    public BlockFluxCompressor() {
        super(Material.IRON, "flux_compressor");
    }

    @Override
    protected Class<? extends TileEntity> getTileEntityClass() {
        return TileEntityFluxCompressor.class;
    }

    @Override
    public EnumGuiId getGuiID(){
        return EnumGuiId.FLUX_COMPRESSOR;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, ROTATION, ACTIVE);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        TileEntity te = PneumaticCraftUtils.getTileEntitySafely(worldIn, pos);
        if (te instanceof TileEntityFluxCompressor) {
            return state.withProperty(ACTIVE, ((TileEntityFluxCompressor) te).isEnabled);
        }
        return state;
    }

    @Override
    public boolean isRotatable() {
        return true;
    }
}
