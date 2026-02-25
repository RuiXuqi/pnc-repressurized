package me.desht.pneumaticcraft.common.worldgen;

import me.desht.pneumaticcraft.common.config.ConfigHandler;
import me.desht.pneumaticcraft.common.fluid.Fluids;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkGeneratorFlat;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.feature.WorldGenLakes;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.Random;

public class WorldGeneratorPneumaticCraft implements IWorldGenerator {

    private Block oilBlock = null;
    private boolean worldGenDisabled = false;

    public WorldGeneratorPneumaticCraft() {
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        if (this.worldGenDisabled) {
            return;
        }

        if (this.oilBlock == null) {
            this.oilBlock = FluidRegistry.getFluid(Fluids.OIL.getName()).getBlock();
            if (this.oilBlock == null) {
                this.worldGenDisabled = true;
                return;
            }
        }

        if (!(chunkGenerator instanceof ChunkGeneratorFlat)) { //don't generate on flatworlds
            switch (world.provider.getDimension()) {
                case 0:
                    this.generateSurface(world, random, chunkX * 16, chunkZ * 16);
                    break;
                case -1:
                    this.generateNether(world, random, chunkX * 16, chunkZ * 16);
                    break;
                case 1:
                    this.generateEnd(world, random, chunkX * 16, chunkZ * 16);
                    break;
                default:
                    this.generateSurface(world, random, chunkX * 16, chunkZ * 16);
            }
        }
    }

    private boolean isBlacklisted(int dimension) {
        for (int d : ConfigHandler.general.oilWorldGenBlacklist) {
            if (d == dimension) return true;
        }
        return false;
    }

    private void generateSurface(World world, Random rand, int chunkX, int chunkZ) {
        if (!this.isBlacklisted(world.provider.getDimension()) && rand.nextDouble() < ConfigHandler.general.oilGenerationChance / 100D) {
            int y = rand.nextInt(rand.nextInt(128) + 8);
            new WorldGenLakes(this.oilBlock).generate(world, rand, new BlockPos(chunkX + 8, y, chunkZ + 8));
        }
    }

    private void generateNether(World world, Random rand, int chunkX, int chunkZ) {

    }

    private void generateEnd(World world, Random rand, int chunkX, int chunkZ) {

    }

}
