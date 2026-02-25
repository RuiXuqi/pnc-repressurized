package me.desht.pneumaticcraft.common.tileentity;

import com.google.common.collect.ImmutableList;
import me.desht.pneumaticcraft.common.block.BlockElectrostaticCompressor;
import me.desht.pneumaticcraft.common.block.Blockss;
import me.desht.pneumaticcraft.common.config.ConfigHandler;
import me.desht.pneumaticcraft.common.network.GuiSynced;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class TileEntityElectrostaticCompressor extends TileEntityPneumaticBase implements IRedstoneControl {

    @GameRegistry.ObjectHolder("chisel:ironpane")
    private static final Block CHISELED_BARS = null;

    private static final List<String> REDSTONE_LABELS = ImmutableList.of(
            "gui.tab.redstoneBehaviour.button.never",
            "gui.tab.redstoneBehaviour.electrostaticCompressor.button.struckByLightning"
    );
    private static final int MAX_ELECTROSTATIC_GRID_SIZE = 250;
    private static final int MAX_BARS_ABOVE = 10;

    private boolean lastRedstoneState;
    @GuiSynced
    public int redstoneMode = 0;
    public int ironBarsBeneath = 0;
    public int ironBarsAbove = 0;
    private int struckByLightningCooldown; //used by the redstone.

    public TileEntityElectrostaticCompressor() {
        super(PneumaticValues.DANGER_PRESSURE_ELECTROSTATIC_COMPRESSOR, PneumaticValues.MAX_PRESSURE_ELECTROSTATIC_COMPRESSOR, PneumaticValues.VOLUME_ELECTROSTATIC_COMPRESSOR, 4);
    }

    @Override
    public void update() {
        if ((this.getWorld().getTotalWorldTime() & 0x1f) == 0) {  // every 32 ticks
            int max = PneumaticValues.PRODUCTION_ELECTROSTATIC_COMPRESSOR / PneumaticValues.MAX_REDIRECTION_PER_IRON_BAR;
            for (this.ironBarsBeneath = 0; this.ironBarsBeneath < max; this.ironBarsBeneath++) {
                if (!isValidGridBlock(this.getWorld().getBlockState(this.getPos().down(this.ironBarsBeneath + 1)).getBlock())) {
                    break;
                }
            }
            for (this.ironBarsAbove = 0; this.ironBarsAbove < MAX_BARS_ABOVE; this.ironBarsAbove++) {
                if (!isValidGridBlock(this.getWorld().getBlockState(this.getPos().up(this.ironBarsAbove + 1)).getBlock())) {
                    break;
                }
            }
        }

        super.update();

        this.maybeLightningStrike();

        if (!this.getWorld().isRemote) {
            if (this.lastRedstoneState != this.shouldEmitRedstone()) {
                this.lastRedstoneState = !this.lastRedstoneState;
                this.updateNeighbours();
            }
            this.struckByLightningCooldown--;
        }
    }

    public int getStrikeChance() {
        int strikeChance = ConfigHandler.machineProperties.electrostaticLightningChance;
        if (this.getWorld().isRaining()) strikeChance *= 0.5;  // slightly more likely if raining
        if (this.getWorld().isThundering()) strikeChance *= 0.2; // much more likely if thundering
        strikeChance *= (1f - (0.02f * this.ironBarsAbove));
        return strikeChance;
    }

    private void maybeLightningStrike() {
        Random rnd = this.getWorld().rand;
        if (rnd.nextInt(this.getStrikeChance()) == 0) {
            int dist = rnd.nextInt(6);
            float angle = rnd.nextFloat() * (float) Math.PI;
            int x = (int) (this.getPos().getX() + dist * MathHelper.sin(angle));
            int z = (int) (this.getPos().getZ() + dist * MathHelper.cos(angle));
            for (int y = this.getPos().getY() + 5; y > this.getPos().getY() - 5; y--) {
                BlockPos hitPos = new BlockPos(x, y, z);
                IBlockState state = this.getWorld().getBlockState(hitPos);
                if (state.getBlock() instanceof BlockElectrostaticCompressor || state.getBlock() == Blocks.IRON_BARS) {
                    Set<BlockPos> posSet = new HashSet<>();
                    this.getElectrostaticGrid(posSet, this.getWorld(), hitPos, null);
                    List<TileEntityElectrostaticCompressor> compressors = posSet.stream()
                            .filter(pos -> this.world.getBlockState(pos).getBlock() == Blockss.ELECTROSTATIC_COMPRESSOR)
                            .map(pos -> this.world.getTileEntity(pos))
                            .filter(te -> te instanceof TileEntityElectrostaticCompressor)
                            .map(te -> (TileEntityElectrostaticCompressor) te)
                            .collect(Collectors.toList());
                    EntityLightningBolt bolt = new EntityLightningBolt(this.getWorld(), x, y, z, true);
                    this.getWorld().spawnEntity(bolt);
                    for (TileEntityElectrostaticCompressor compressor : compressors) {
                        compressor.addAir(PneumaticValues.PRODUCTION_ELECTROSTATIC_COMPRESSOR / compressors.size());
                        compressor.onStruckByLightning();
                    }
                    AxisAlignedBB box = new AxisAlignedBB(this.getPos()).grow(16, 16, 16);
                    for (EntityLivingBase entity : this.getWorld().getEntitiesWithinAABB(EntityLivingBase.class, box, EntitySelectors.IS_ALIVE)) {
                        if (posSet.contains(entity.getPosition()) || posSet.contains(entity.getPosition().down())) {
                            if (!net.minecraftforge.event.ForgeEventFactory.onEntityStruckByLightning(entity, bolt)) {
                                entity.onStruckByLightning(bolt);
                            }
                        }
                    }

                    break;
                }
            }
        }
    }

    @Override
    public boolean isConnectedTo(EnumFacing dir) {
        return dir != EnumFacing.UP;
    }

    private boolean shouldEmitRedstone() {
        switch (this.redstoneMode) {
            case 0:
                return false;
            case 1:
                return this.struckByLightningCooldown > 0;
        }
        return false;
    }

    public void onStruckByLightning() {
        this.struckByLightningCooldown = 10;
        if (this.getPressure() > PneumaticValues.DANGER_PRESSURE_ELECTROSTATIC_COMPRESSOR) {
            int maxRedirection = PneumaticValues.MAX_REDIRECTION_PER_IRON_BAR * this.ironBarsBeneath;
            int tooMuchAir = (int) ((this.getPressure() - PneumaticValues.DANGER_PRESSURE_ELECTROSTATIC_COMPRESSOR) * this.getAirHandler(null).getVolume());
            this.addAir(-Math.min(maxRedirection, tooMuchAir));
        }
    }

    @Override
    public void handleGUIButtonPress(int buttonID, EntityPlayer player) {
        if (buttonID == 0) {
            this.redstoneMode++;
            if (this.redstoneMode > 1) this.redstoneMode = 0;
        }
    }

    @Override
    public String getName() {
        return Blockss.ELECTROSTATIC_COMPRESSOR.getTranslationKey();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbtTagCompound) {
        super.readFromNBT(nbtTagCompound);
        this.redstoneMode = nbtTagCompound.getInteger("redstoneMode");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbtTagCompound) {
        super.writeToNBT(nbtTagCompound);
        nbtTagCompound.setInteger("redstoneMode", this.redstoneMode);
        return nbtTagCompound;
    }

    @Override
    public int getRedstoneMode() {
        return this.redstoneMode;
    }

    @Override
    protected List<String> getRedstoneButtonLabels() {
        return REDSTONE_LABELS;
    }

    /**
     * Scan recursively, adding all connected iron bars and electrostatic compressors to the grid
     *
     * @param set
     * @param world
     * @param pos
     */
    public void getElectrostaticGrid(Set<BlockPos> set, World world, BlockPos pos, EnumFacing dir) {
        for (EnumFacing d : EnumFacing.VALUES) {
            if (d == dir) continue;
            BlockPos newPos = pos.offset(d);
            Block block = world.getBlockState(newPos).getBlock();
            if ((isValidGridBlock(block) || block == Blockss.ELECTROSTATIC_COMPRESSOR)
                    && set.size() < MAX_ELECTROSTATIC_GRID_SIZE && set.add(newPos)) {
                this.getElectrostaticGrid(set, world, newPos, d.getOpposite());
            }
        }
    }

    private static boolean isValidGridBlock(Block block) {
        return block == Blocks.IRON_BARS || block == CHISELED_BARS;
    }
}
