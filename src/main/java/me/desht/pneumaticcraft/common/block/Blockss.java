package me.desht.pneumaticcraft.common.block;

import me.desht.pneumaticcraft.common.config.ConfigHandler;
import me.desht.pneumaticcraft.common.heat.HeatUtil;
import me.desht.pneumaticcraft.common.thirdparty.ThirdPartyManager;
import me.desht.pneumaticcraft.common.tileentity.*;
import me.desht.pneumaticcraft.lib.Names;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = Names.MOD_ID)
@GameRegistry.ObjectHolder(Names.MOD_ID)
public class Blockss {
    @GameRegistry.ObjectHolder("pressure_tube")
    public static final Block PRESSURE_TUBE = Blocks.AIR;
    @GameRegistry.ObjectHolder("air_compressor")
    public static final Block AIR_COMPRESSOR = Blocks.AIR;
    @GameRegistry.ObjectHolder("air_cannon")
    public static final Block AIR_CANNON = Blocks.AIR;
    @GameRegistry.ObjectHolder("pressure_chamber_wall")
    public static final Block PRESSURE_CHAMBER_WALL = Blocks.AIR;
    @GameRegistry.ObjectHolder("pressure_chamber_glass")
    public static final Block PRESSURE_CHAMBER_GLASS = Blocks.AIR;
    @GameRegistry.ObjectHolder("pressure_chamber_valve")
    public static final Block PRESSURE_CHAMBER_VALVE = Blocks.AIR;
    @GameRegistry.ObjectHolder("pressure_chamber_interface")
    public static final Block PRESSURE_CHAMBER_INTERFACE = Blocks.AIR;
    @GameRegistry.ObjectHolder("charging_station")
    public static final Block CHARGING_STATION = Blocks.AIR;
    @GameRegistry.ObjectHolder("elevator_base")
    public static final Block ELEVATOR_BASE = Blocks.AIR;
    @GameRegistry.ObjectHolder("elevator_frame")
    public static final Block ELEVATOR_FRAME = Blocks.AIR;
    @GameRegistry.ObjectHolder("vacuum_pump")
    public static final Block VACUUM_PUMP = Blocks.AIR;
    @GameRegistry.ObjectHolder("pneumatic_door_base")
    public static final Block PNEUMATIC_DOOR_BASE = Blocks.AIR;
    @GameRegistry.ObjectHolder("pneumatic_door")
    public static final Block PNEUMATIC_DOOR = Blocks.AIR;
    @GameRegistry.ObjectHolder("assembly_platform")
    public static final Block ASSEMBLY_PLATFORM = Blocks.AIR;
    @GameRegistry.ObjectHolder("assembly_io_unit")
    public static final Block ASSEMBLY_IO_UNIT = Blocks.AIR;
    @GameRegistry.ObjectHolder("assembly_drill")
    public static final Block ASSEMBLY_DRILL = Blocks.AIR;
    @GameRegistry.ObjectHolder("assembly_laser")
    public static final Block ASSEMBLY_LASER = Blocks.AIR;
    @GameRegistry.ObjectHolder("assembly_controller")
    public static final Block ASSEMBLY_CONTROLLER = Blocks.AIR;
    @GameRegistry.ObjectHolder("advanced_pressure_tube")
    public static final Block ADVANCED_PRESSURE_TUBE = Blocks.AIR;
    @GameRegistry.ObjectHolder("compressed_iron_block")
    public static final Block COMPRESSED_IRON = Blocks.AIR;
    @GameRegistry.ObjectHolder("uv_light_box")
    public static final Block UV_LIGHT_BOX = Blocks.AIR;
    @GameRegistry.ObjectHolder("security_station")
    public static final Block SECURITY_STATION = Blocks.AIR;
    @GameRegistry.ObjectHolder("universal_sensor")
    public static final Block UNIVERSAL_SENSOR = Blocks.AIR;
    @GameRegistry.ObjectHolder("universal_actuator")
    public static final Block UNIVERSAL_ACTUATOR = Blocks.AIR;
    @GameRegistry.ObjectHolder("aerial_interface")
    public static final Block AERIAL_INTERFACE = Blocks.AIR;
    @GameRegistry.ObjectHolder("electrostatic_compressor")
    public static final Block ELECTROSTATIC_COMPRESSOR = Blocks.AIR;
    @GameRegistry.ObjectHolder("aphorism_tile")
    public static final Block APHORISM_TILE = Blocks.AIR;
    @GameRegistry.ObjectHolder("omnidirectional_hopper")
    public static final Block OMNIDIRECTIONAL_HOPPER = Blocks.AIR;
    @GameRegistry.ObjectHolder("elevator_caller")
    public static final Block ELEVATOR_CALLER = Blocks.AIR;
    @GameRegistry.ObjectHolder("programmer")
    public static final Block PROGRAMMER = Blocks.AIR;
    @GameRegistry.ObjectHolder("creative_compressor")
    public static final Block CREATIVE_COMPRESSOR = Blocks.AIR;
    @GameRegistry.ObjectHolder("plastic_mixer")
    public static final Block PLASTIC_MIXER = Blocks.AIR;
    @GameRegistry.ObjectHolder("liquid_compressor")
    public static final Block LIQUID_COMPRESSOR = Blocks.AIR;
    @GameRegistry.ObjectHolder("advanced_liquid_compressor")
    public static final Block ADVANCED_LIQUID_COMPRESSOR = Blocks.AIR;
    @GameRegistry.ObjectHolder("advanced_air_compressor")
    public static final Block ADVANCED_AIR_COMPRESSOR = Blocks.AIR;
    @GameRegistry.ObjectHolder("liquid_hopper")
    public static final Block LIQUID_HOPPER = Blocks.AIR;
    @GameRegistry.ObjectHolder("drone_redstone_emitter")
    public static final Block DRONE_REDSTONE_EMITTER = Blocks.AIR;
    @GameRegistry.ObjectHolder("heat_sink")
    public static final Block HEAT_SINK = Blocks.AIR;
    @GameRegistry.ObjectHolder("vortex_tube")
    public static final Block VORTEX_TUBE = Blocks.AIR;
    @GameRegistry.ObjectHolder("programmable_controller")
    public static final Block PROGRAMMABLE_CONTROLLER = Blocks.AIR;
    @GameRegistry.ObjectHolder("gas_lift")
    public static final Block GAS_LIFT = Blocks.AIR;
    @GameRegistry.ObjectHolder("refinery")
    public static final Block REFINERY = Blocks.AIR;
    @GameRegistry.ObjectHolder("thermopneumatic_processing_plant")
    public static final Block THERMOPNEUMATIC_PROCESSING_PLANT = Blocks.AIR;
    @GameRegistry.ObjectHolder("kerosene_lamp")
    public static final Block KEROSENE_LAMP = Blocks.AIR;
    @GameRegistry.ObjectHolder("kerosene_lamp_light")
    public static final Block KEROSENE_LAMP_LIGHT = Blocks.AIR;
    @GameRegistry.ObjectHolder("sentry_turret")
    public static final Block SENTRY_TURRET = Blocks.AIR;
    @GameRegistry.ObjectHolder("flux_compressor")
    public static final Block FLUX_COMPRESSOR = Blocks.AIR;
    @GameRegistry.ObjectHolder("pneumatic_dynamo")
    public static final Block PNEUMATIC_DYNAMO = Blocks.AIR;
    @GameRegistry.ObjectHolder("fake_ice")
    public static final Block FAKE_ICE = Blocks.AIR;
    @GameRegistry.ObjectHolder("thermal_compressor")
    public static final Block THERMAL_COMPRESSOR = Blocks.AIR;

    public static List<Block> blocks = new ArrayList<>();

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        IForgeRegistry<Block> registry = event.getRegistry();

        registerBlock(registry, new BlockPressureTube("pressure_tube", BlockPressureTube.Tier.ONE));
        registerBlock(registry, new BlockPressureTube("advanced_pressure_tube", BlockPressureTube.Tier.TWO));
        registerBlock(registry, new BlockAirCompressor());
        registerBlock(registry, new BlockAdvancedAirCompressor());
        registerBlock(registry, new BlockAirCannon());
        registerBlock(registry, new BlockPressureChamberWall());
        registerBlock(registry, new BlockPressureChamberGlass());
        registerBlock(registry, new BlockPressureChamberValve());
        registerBlock(registry, new BlockChargingStation());
        registerBlock(registry, new BlockElevatorBase());
        registerBlock(registry, new BlockElevatorFrame());
        registerBlock(registry, new BlockPressureChamberInterface());
        registerBlock(registry, new BlockVacuumPump());
        registerBlock(registry, new BlockPneumaticDoorBase());
        registerBlock(registry, new BlockPneumaticDoor());
        registerBlock(registry, new BlockAssemblyIOUnit());
        registerBlock(registry, new BlockAssemblyPlatform());
        registerBlock(registry, new BlockAssemblyDrill());
        registerBlock(registry, new BlockAssemblyLaser());
        registerBlock(registry, new BlockAssemblyController());
        registerBlock(registry, new BlockCompressedIron());
        registerBlock(registry, new BlockUVLightBox());
        registerBlock(registry, new BlockSecurityStation());
        registerBlock(registry, new BlockUniversalSensor());
//        registerBlock(registry, new BlockUniversalActuator());
        registerBlock(registry, new BlockAerialInterface());
        registerBlock(registry, new BlockElectrostaticCompressor());
        registerBlock(registry, new BlockAphorismTile());
        registerBlock(registry, new BlockOmnidirectionalHopper());
        registerBlock(registry, new BlockLiquidHopper());
        registerBlock(registry, new BlockElevatorCaller());
        registerBlock(registry, new BlockProgrammer());
        registerBlock(registry, new BlockCreativeCompressor());
        registerBlock(registry, new BlockPlasticMixer());
        registerBlock(registry, new BlockLiquidCompressor());
        registerBlock(registry, new BlockAdvancedLiquidCompressor());
        registerBlock(registry, new BlockDroneRedstoneEmitter());
        registerBlock(registry, new BlockHeatSink());
        registerBlock(registry, new BlockVortexTube());
        registerBlock(registry, new BlockProgrammableController());
        registerBlock(registry, new BlockGasLift());
        registerBlock(registry, new BlockRefinery());
        registerBlock(registry, new BlockThermopneumaticProcessingPlant());
        registerBlock(registry, new BlockKeroseneLamp());
        if (!ConfigHandler.advanced.disableKeroseneLampFakeAirBlock)
            registerBlock(registry, new BlockKeroseneLampLight());
        registerBlock(registry, new BlockSentryTurret());
        registerBlock(registry, new BlockFluxCompressor());
        registerBlock(registry, new BlockPneumaticDynamo());
        registerBlock(registry, new BlockFakeIce());
        registerBlock(registry, new BlockThermalCompressor());
    }

    public static void registerBlock(IForgeRegistry<Block> registry, Block block) {
        registry.register(block);
        ThirdPartyManager.instance().onBlockRegistry(block);
        blocks.add(block);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void registerBlockColorHandlers(ColorHandlerEvent.Block event) {
        event.getBlockColors().registerBlockColorHandler((state, blockAccess, pos, tintIndex) -> {
            if (blockAccess != null && pos != null) {
                TileEntity te = blockAccess.getTileEntity(pos);
                int heatLevel = te instanceof IHeatTinted iHeatTinted ? iHeatTinted.getHeatLevelForTintIndex(tintIndex) : 10;
                float[] color = HeatUtil.getColorForHeatLevel(heatLevel);
                return 0xFF000000 + ((int) (color[0] * 255) << 16) + ((int) (color[1] * 255) << 8) + (int) (color[2] * 255);
            }
            return 0xFFFFFFFF;
        }, Blockss.COMPRESSED_IRON, Blockss.HEAT_SINK, Blockss.VORTEX_TUBE, Blockss.THERMAL_COMPRESSOR);

        event.getBlockColors().registerBlockColorHandler((state, blockAccess, pos, tintIndex) -> {
            if (blockAccess != null && pos != null) {
                TileEntity te = blockAccess.getTileEntity(pos);
                if (te instanceof TileEntityUVLightBox uvLightBox) {
                    return uvLightBox.areLightsOn ? 0xFF4000FF : 0xFFAFAFE4;
                }
            }
            return 0xFFAFAFE4;
        }, Blockss.UV_LIGHT_BOX);

        event.getBlockColors().registerBlockColorHandler((state, blockAccess, pos, tintIndex) -> {
            if (blockAccess != null && pos != null) {
                TileEntity te = blockAccess.getTileEntity(pos);
                if (te instanceof TileEntityOmnidirectionalHopper omnidirectionalHopper) {
                    return omnidirectionalHopper.isCreative ? 0xFFFF60FF : 0xFFFFFFFF;
                }
            }
            return 0xFFFFFFFF;
        }, Blockss.OMNIDIRECTIONAL_HOPPER, Blockss.LIQUID_HOPPER);

        for (Block b : Blockss.blocks) {
            if (b instanceof BlockPneumaticCraftCamo) {
                event.getBlockColors().registerBlockColorHandler((state, worldIn, pos, tintIndex) -> {
                    if (pos == null || worldIn == null) return 0xffffff;
                    TileEntity te = worldIn.getTileEntity(pos);
                    if (te instanceof ICamouflageableTE iCamouflageable && iCamouflageable.getCamouflage() != null) {
                        return Minecraft.getMinecraft().getBlockColors().colorMultiplier(iCamouflageable.getCamouflage(), te.getWorld(), pos, tintIndex);
                    } else {
                        return 0xffffff;
                    }
                }, b);
            }
        }

        event.getBlockColors().registerBlockColorHandler((state, worldIn, pos, tintIndex) -> {
            if (worldIn != null && pos != null) {
                TileEntity te = worldIn.getTileEntity(pos);
                if (te instanceof TileEntityAphorismTile aphorismTile) {
                    int dmg;
                    return switch (tintIndex) {
                        case 0 -> {
                            dmg = aphorismTile.getBorderColor();
                            yield EnumDyeColor.byDyeDamage(dmg).getColorValue();
                        }
                        case 1 -> {
                            dmg = aphorismTile.getBackgroundColor();
                            yield desaturate(EnumDyeColor.byDyeDamage(dmg).getColorValue());
                        }
                        default -> 0xFFFFFF;
                    };
                }
            }
            return 0xFFFFFF;
        }, Blockss.APHORISM_TILE);
    }

    public static int desaturate(int c) {
        float[] hsb = Color.RGBtoHSB((c & 0xFF0000) >> 16, (c & 0xFF00) >> 8, c & 0xFF, null);
        Color color = Color.getHSBColor(hsb[0], hsb[1] * 0.4f, hsb[2]);
        if (hsb[2] < 0.7) color = color.brighter();
        return color.getRGB();
    }
}
