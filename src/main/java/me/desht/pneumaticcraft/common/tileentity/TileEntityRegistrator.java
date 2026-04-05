package me.desht.pneumaticcraft.common.tileentity;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.RL;

public class TileEntityRegistrator {
    public static void init() {
        register(TileEntityPressureTube.class, "PressureTube");
        register(TileEntityAdvancedPressureTube.class, "advancedPressureTube");
        register(TileEntityAirCompressor.class, "AirCompressor");
        register(TileEntityAdvancedAirCompressor.class, "advancedAirCompressor");
        register(TileEntityAirCannon.class, "AirCannon");
        register(TileEntityPressureChamberWall.class, "PressureChamberWall");
        register(TileEntityPressureChamberValve.class, "PressureChamberValve");
        register(TileEntityChargingStation.class, "ChargingStation");
        register(TileEntityElevatorBase.class, "ElevatorBase");
        register(TileEntityElevatorFrame.class, "ElevatorFrame");
        register(TileEntityPressureChamberInterface.class, "PressureChamberInterface");
        register(TileEntityVacuumPump.class, "VacuumPump");
        register(TileEntityPneumaticDoorBase.class, "PneumaticDoorBase");
        register(TileEntityPneumaticDoor.class, "PneumaticDoor");
        register(TileEntityAssemblyIOUnit.class, "AssemblyIOUnit");
        register(TileEntityAssemblyPlatform.class, "AssemblyPlatform");
        register(TileEntityAssemblyDrill.class, "AssemblyDrill");
        register(TileEntityAssemblyLaser.class, "AssemblyLaser");
        register(TileEntityAssemblyController.class, "AssemblyController");
        register(TileEntityUVLightBox.class, "UVLightBox");
        register(TileEntitySecurityStation.class, "SecurityStation");
        register(TileEntityUniversalSensor.class, "UniversalSensor");
        register(TileEntityUniversalActuator.class, "universalActuator");
        register(TileEntityAerialInterface.class, "AerialInterface");
        register(TileEntityElectrostaticCompressor.class, "ElectrostaticCompressor");
        register(TileEntityAphorismTile.class, "AphorismTile");
        register(TileEntityOmnidirectionalHopper.class, "OmnidirectionalHopper");
        register(TileEntityLiquidHopper.class, "liquidHopper");
        register(TileEntityElevatorCaller.class, "ElevatorCaller");
        register(TileEntityProgrammer.class, "Programmer");
        register(TileEntityCreativeCompressor.class, "CreativeCompressor");
        register(TileEntityPlasticMixer.class, "plasticMixer");
        register(TileEntityLiquidCompressor.class, "liquidCompressor");
        register(TileEntityAdvancedLiquidCompressor.class, "advancedLiquidCompressor");
        register(TileEntityDroneRedstoneEmitter.class, "droneRedstoneEmitter");
        register(TileEntityCompressedIronBlock.class, "compressedIronBlock");
        register(TileEntityHeatSink.class, "heatSink");
        register(TileEntityVortexTube.class, "vortexTube");
        register(TileEntityProgrammableController.class, "programmableController");
        register(TileEntityGasLift.class, "gasLift");
        register(TileEntityRefinery.class, "refinery");
        register(TileEntityThermopneumaticProcessingPlant.class, "thermopneumaticProcessingPlant");
        register(TileEntityKeroseneLamp.class, "keroseneLamp");
        register(TileEntitySentryTurret.class, "sentryTurret");
        register(TileEntityFluxCompressor.class, "fluxCompressor");
        register(TileEntityPneumaticDynamo.class, "pneumaticDynamo");
        register(TileEntityThermalCompressor.class, "thermalCompressor");
    }

    public static void register(Class<? extends TileEntity> tileEntityClass, String id) {
        GameRegistry.registerTileEntity(tileEntityClass, RL(id));
    }
}
