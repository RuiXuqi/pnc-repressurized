package me.desht.pneumaticcraft.lib;

import net.minecraft.util.ResourceLocation;

public class Textures {

    public static final String ICON_LOCATION = Names.MOD_ID + ":";

    public static final String MODEL_LOCATION = ICON_LOCATION + "textures/model/";
    public static final String PNCMODEL_LOCATION = ICON_LOCATION + "textures/pnc_model/";
    public static final String TUBE_MODULE_MODEL_LOCATION = PNCMODEL_LOCATION + "modules/";
    public static final String GUI_LOCATION = ICON_LOCATION + "textures/gui/";
    public static final String ENTITY_LOCATION = ICON_LOCATION + "textures/entities/";
    public static final String DRONE_LOCATION = ICON_LOCATION + "textures/entities/drone/";
    public static final String ARMOR_LOCATION = ICON_LOCATION + "textures/armor/";
    public static final String RENDER_LOCATION = ICON_LOCATION + "textures/render/";
    public static final String PROG_WIDGET_LOCATION = ICON_LOCATION + "textures/items/progwidgets/";
    public static final String PRESSURE_GLASS_LOCATION = ICON_LOCATION + "blocks/pressure_chamber/windows/";

    public static final String ARMOR_PNEUMATIC = ARMOR_LOCATION + "pneumatic";
//    public static final ResourceLocation MODEL_DRONE = new ResourceLocation(ENTITY_LOCATION + "drone.png");
    public static final String ITEM_VORTEX = "vortex";

    // Entity & TESR model textures
    public static final ResourceLocation MODEL_AIR_CANNON = modelTextureNew("air_cannon.png");
    public static final ResourceLocation MODEL_CHARGING_STATION_PAD = modelTexture("charging_station_pad.png");
    public static final ResourceLocation MODEL_ELEVATOR = modelTextureNew("elevator.png");
    public static final ResourceLocation MODEL_PRESSURE_CHAMBER_INTERFACE = modelTextureNew("pressure_chamber_interface.png");
    public static final ResourceLocation MODEL_VACUUM_PUMP = modelTextureNew("vacuum_pump.png");
    public static final ResourceLocation MODEL_PNEUMATIC_DOOR_DYNAMIC = modelTextureNew("pneumatic_door_dynamic.png");
    public static final ResourceLocation MODEL_PNEUMATIC_DOOR_BASE = modelTextureNew("pneumatic_door_base.png");
    public static final ResourceLocation MODEL_ASSEMBLY_IO_EXPORT = modelTextureNew("assembly_io_export.png");
    public static final ResourceLocation MODEL_ASSEMBLY_IO_IMPORT = modelTextureNew("assembly_io_import.png");
    public static final ResourceLocation MODEL_ASSEMBLY_LASER_AND_DRILL = modelTextureNew("assembly_laser_and_drill.png");
    public static final ResourceLocation MODEL_ASSEMBLY_PLATFORM = modelTexture("assembly_platform.png");
    public static final ResourceLocation MODEL_ASSEMBLY_CONTROLLER = modelTextureNew("assembly_controller.png");
    public static final ResourceLocation MODEL_UNIVERSAL_SENSOR = modelTextureNew("universal_sensor.png");
    public static final ResourceLocation MODEL_DRONE_MINIGUN = modelTextureNew("drone_minigun.png");
    public static final ResourceLocation MODEL_HEAT_FRAME = modelTextureNew("heat_frame.png");
    public static final ResourceLocation MODEL_CROP_SUPPORT = modelTextureNew("crop_support.png");
    public static final ResourceLocation MODEL_SENTRY_TURRET = modelTexture("sentry_turret.png");

    // Tube Module textures
    public static final ResourceLocation MODEL_FLOW_DETECTOR = tubeModuleTexture("flow_detector.png");
    public static final ResourceLocation MODEL_LOGISTICS_MODULE = tubeModuleTexture("logistics_module.png");
    public static final ResourceLocation MODEL_GAUGE = tubeModuleTexture("gauge_module.png");
    public static final ResourceLocation MODEL_GAUGE_UPGRADED = tubeModuleTexture("gauge_module_upgraded.png");
    public static final ResourceLocation MODEL_AIR_GRATE = tubeModuleTexture("air_grate.png");
    public static final ResourceLocation MODEL_AIR_GRATE_UPGRADED = tubeModuleTexture("air_grate_upgraded.png");
    public static final ResourceLocation MODEL_CHARGING_MODULE = tubeModuleTexture("charging_module.png");
    public static final ResourceLocation MODEL_CHARGING_MODULE_UPGRADED = tubeModuleTexture("charging_module_upgraded.png");
    public static final ResourceLocation MODEL_SAFETY_VALVE = tubeModuleTexture("safety_valve.png");
    public static final ResourceLocation MODEL_SAFETY_VALVE_UPGRADED = tubeModuleTexture("safety_valve_upgraded.png");
    public static final ResourceLocation MODEL_REGULATOR_MODULE = tubeModuleTexture("regulator.png");
    public static final ResourceLocation MODEL_REGULATOR_MODULE_UPGRADED = tubeModuleTexture("regulator_upgraded.png");
    public static final ResourceLocation MODEL_REDSTONE_MODULE = tubeModuleTexture("redstone.png");
    public static final ResourceLocation MODEL_REDSTONE_MODULE_UPGRADED = tubeModuleTexture("redstone_upgraded.png");
//    public static final ResourceLocation MODEL_VACUUM_MODULE = tubeModuleTexture("vacuum.png");
//    public static final ResourceLocation MODEL_VACUUM_MODULE_UPGRADED = tubeModuleTexture("vacuum_upgraded.png");
//    public static final ResourceLocation MODEL_THERMOSTAT_MODULE = tubeModuleTexture("thermostat_module.png");
//    public static final ResourceLocation MODEL_THERMOSTAT_MODULE_UPGRADED = tubeModuleTexture("thermostat_module_upgraded.png");

    // GUI background textures
    public static final String GUI_AIR_COMPRESSOR_LOCATION = GUI_LOCATION + "gui_air_compressor.png";
    public static final String GUI_ADVANCED_AIR_COMPRESSOR_LOCATION = GUI_LOCATION + "gui_advanced_air_compressor.png";
    public static final String GUI_AIR_CANNON_LOCATION = GUI_LOCATION + "gui_air_cannon.png";
    public static final String GUI_4UPGRADE_SLOTS = GUI_LOCATION + "gui_pressure_chamber.png";
    public static final String GUI_NEI_PRESSURE_CHAMBER_LOCATION = GUI_LOCATION + "gui_nei_pressure_chamber.png";
    public static final String GUI_CHARGING_STATION_LOCATION = GUI_LOCATION + "gui_charging_station.png";
    public static final String GUI_PNEUMATIC_ARMOR_LOCATION = GUI_LOCATION + "gui_pneumatic_armor.png";
    public static final String GUI_PRESSURE_CHAMBER_INTERFACE_LOCATION = GUI_LOCATION + "gui_pressure_chamber_interface.png";
    public static final String GUI_PRESSURE_CHAMBER_INTERFACE_CREATIVE_FILTER_LOCATION = GUI_LOCATION + "gui_pressure_chamber_interface_creative_filter.png";
    public static final String GUI_VACUUM_PUMP_LOCATION = GUI_LOCATION + "gui_vacuum_pump.png";
    public static final String GUI_ITEM_SEARCHER_LOCATION = GUI_LOCATION + "gui_item_searcher.png";
    public static final String GUI_ASSEMBLY_CONTROLLER = GUI_LOCATION + "gui_assembly_controller.png";
    public static final String GUI_NEI_ASSEMBLY_CONTROLLER = GUI_LOCATION + "gui_nei_assembly_controller.png";
    public static final String GUI_UV_LIGHT_BOX = GUI_LOCATION + "gui_uv_light_box.png";
    public static final String GUI_SECURITY_STATION = GUI_LOCATION + "gui_security_station.png";
    public static final String GUI_HACKING = GUI_LOCATION + "gui_hacking.png";
    public static final String GUI_UNIVERSAL_SENSOR = GUI_LOCATION + "gui_universal_sensor.png";
//    public static final String GUI_UNIVERSAL_SENSOR_SLOT = GUI_LOCATION + "gui_universal_sensor_slot.png";
    public static final String GUI_PNEUMATIC_DOOR = GUI_LOCATION + "gui_pneumatic_door_base.png";
    public static final String GUI_OMNIDIRECTIONAL_HOPPER = GUI_LOCATION + "gui_omnidirectional_hopper.png";
    public static final String GUI_PROGRAMMER_STD = GUI_LOCATION + "gui_programmer.png";
    public static final String GUI_PROGRAMMER_LARGE = GUI_LOCATION + "gui_programmer_large.png";
    public static final String GUI_PLASTIC_MIXER = GUI_LOCATION + "gui_plastic_mixer.png";
    public static final String GUI_LIQUID_COMPRESSOR = GUI_LOCATION + "gui_liquid_compressor.png";
    public static final String GUI_ADVANCED_LIQUID_COMPRESSOR = GUI_LOCATION + "gui_advanced_liquid_compressor.png";
    public static final String GUI_LIQUID_HOPPER = GUI_LOCATION + "gui_liquid_hopper.png";
    public static final String GUI_ELEVATOR = GUI_LOCATION + "gui_elevator.png";
    public static final String GUI_REMOTE_EDITOR = GUI_LOCATION + "gui_remote_editor.png";
    public static final String GUI_WIDGET_OPTIONS_STRING = GUI_LOCATION + "gui_widget_options.png";
    public static final String GUI_PROGRAMMABLE_CONTROLLER = GUI_LOCATION + "gui_programmable_controller.png";
    public static final String GUI_GAS_LIFT = GUI_LOCATION + "gui_gas_lift.png";
    public static final String GUI_REFINERY = GUI_LOCATION + "gui_refinery.png";
    public static final String GUI_THERMOPNEUMATIC_PROCESSING_PLANT = GUI_LOCATION + "gui_thermopneumatic_processing_plant.png";
    public static final String GUI_LOGISTICS_REQUESTER = GUI_LOCATION + "gui_logistics_requester.png";
    public static final String GUI_AMADRON = GUI_LOCATION + "gui_amadron.png";
    public static final String GUI_NEI_MISC_RECIPES = GUI_LOCATION + "gui_nei_misc_recipes.png";
    public static final String GUI_KEROSENE_LAMP = GUI_LOCATION + "gui_kerosene_lamp.png";
    public static final String GUI_SENTRY_TURRET = GUI_LOCATION + "gui_sentry_turret.png";
    public static final String GUI_MINIGUN_MAGAZINE = GUI_LOCATION + "gui_minigun_magazine.png";
    public static final String GUI_THERMAL_COMPRESSOR_LOCATION = GUI_LOCATION + "gui_thermal_compressor.png";
    public static final String WIDGET_AMADRON_OFFER_STRING = GUI_LOCATION + "widget/widget_amadron_offer.png";

    public static final ResourceLocation GUI_WIDGET_OPTIONS = new ResourceLocation(GUI_WIDGET_OPTIONS_STRING);
    public static final ResourceLocation GUI_WIDGET_AREA = guiTexture("gui_widget_area.png");
    public static final ResourceLocation GUI_PASTEBIN = guiTexture("gui_pastebin.png");
    public static final ResourceLocation GUI_INVENTORY_SEARCHER = guiTexture("gui_inventory_searcher.png");
    public static final ResourceLocation GUI_TUBE_MODULE = guiTexture("gui_tube_module.png");
//    public static final ResourceLocation GUI_REDSTONE_MODULE = guiTexture("gui_redstone_module.png");
    public static final ResourceLocation GUI_TEXT_WIDGET = guiTexture("gui_text_widget.png");
    public static final ResourceLocation GUI_MODULE_SIMPLE = guiTexture("gui_tube_module_simple.png");
    public static final ResourceLocation GUI_MICROMISSILE = guiTexture("gui_micromissile.png");

    public static final String RENDER_BLUR = RENDER_LOCATION + "blur.png";

    // Progwidget textures
    public static final ResourceLocation PROG_WIDGET_COMMENT = progWidgetTexture("comment_piece.png");
    public static final ResourceLocation PROG_WIDGET_AREA = progWidgetTexture("area_piece.png");
    public static final ResourceLocation PROG_WIDGET_ATTACK = progWidgetTexture("attack_piece.png");
    public static final ResourceLocation PROG_WIDGET_CC = progWidgetTexture("computer_control_piece.png");
    public static final ResourceLocation PROG_WIDGET_DIG = progWidgetTexture("dig_piece.png");
    public static final ResourceLocation PROG_WIDGET_HARVEST = progWidgetTexture("harvest_piece.png");
    public static final ResourceLocation PROG_WIDGET_GOTO = progWidgetTexture("goto_piece.png");
    public static final ResourceLocation PROG_WIDGET_TELEPORT = progWidgetTexture("teleport_piece.png");
    public static final ResourceLocation PROG_WIDGET_INV_EX = progWidgetTexture("inventory_export_piece.png");
    public static final ResourceLocation PROG_WIDGET_INV_IM = progWidgetTexture("inventory_import_piece.png");
    public static final ResourceLocation PROG_WIDGET_LIQUID_EX = progWidgetTexture("liquid_export_piece.png");
    public static final ResourceLocation PROG_WIDGET_LIQUID_IM = progWidgetTexture("liquid_import_piece.png");
    public static final ResourceLocation PROG_WIDGET_ENTITY_EX = progWidgetTexture("entity_export_piece.png");
    public static final ResourceLocation PROG_WIDGET_ENTITY_IM = progWidgetTexture("entity_import_piece.png");
    public static final ResourceLocation PROG_WIDGET_RF_EXPORT = progWidgetTexture("rf_export_piece.png");
    public static final ResourceLocation PROG_WIDGET_RF_IMPORT = progWidgetTexture("rf_import_piece.png");
//    public static final ResourceLocation PROG_WIDGET_ESSENTIA_EX = progWidgetTexture("essentia_export_piece.png");
//    public static final ResourceLocation PROG_WIDGET_ESSENTIA_IM = progWidgetTexture("essentia_import_piece.png");
//    public static final ResourceLocation PROG_WIDGET_ESSENTIA_FILTER = progWidgetTexture("essentia_filter_piece.png");
    public static final ResourceLocation PROG_WIDGET_PICK_ITEM = progWidgetTexture("item_pick_piece.png");
    public static final ResourceLocation PROG_WIDGET_ENTITY_RIGHT_CLICK = progWidgetTexture("entity_right_click_piece.png");
    public static final ResourceLocation PROG_WIDGET_BLOCK_RIGHT_CLICK = progWidgetTexture("block_right_click_piece.png");
    public static final ResourceLocation PROG_WIDGET_ITEM_FILTER = progWidgetTexture("item_filter_piece.png");
    public static final ResourceLocation PROG_WIDGET_LIQUID_FILTER = progWidgetTexture("liquid_filter_piece.png");
    public static final ResourceLocation PROG_WIDGET_PLACE = progWidgetTexture("place_piece.png");
    public static final ResourceLocation PROG_WIDGET_START = progWidgetTexture("start_piece.png");
    public static final ResourceLocation PROG_WIDGET_TEXT = progWidgetTexture("text_piece.png");
    public static final ResourceLocation PROG_WIDGET_LABEL = progWidgetTexture("label_piece.png");
    public static final ResourceLocation PROG_WIDGET_JUMP = progWidgetTexture("jump_piece.png");
//    public static final ResourceLocation PROG_WIDGET_JUMP_SUB = progWidgetTexture("jump_sub_piece.png");
    public static final ResourceLocation PROG_WIDGET_WAIT = progWidgetTexture("wait_piece.png");
    public static final ResourceLocation PROG_WIDGET_DROP_ITEM = progWidgetTexture("item_drop_piece.png");
    public static final ResourceLocation PROG_WIDGET_EMIT_REDSTONE = progWidgetTexture("emit_redstone_piece.png");
    public static final ResourceLocation PROG_WIDGET_RENAME = progWidgetTexture("rename_piece.png");
    public static final ResourceLocation PROG_WIDGET_SUICIDE = progWidgetTexture("suicide_piece.png");
    public static final ResourceLocation PROG_WIDGET_EXTERNAL_PROGRAM = progWidgetTexture("external_program_piece.png");
    public static final ResourceLocation PROG_WIDGET_CRAFTING = progWidgetTexture("craft_piece.png");
    public static final ResourceLocation PROG_WIDGET_STANDBY = progWidgetTexture("standby_piece.png");
    public static final ResourceLocation PROG_WIDGET_COORDINATE = progWidgetTexture("coordinate_piece.png");
    public static final ResourceLocation PROG_WIDGET_ITEM_ASSIGN = progWidgetTexture("item_assign_piece.png");
    public static final ResourceLocation PROG_WIDGET_LOGISTICS = progWidgetTexture("logistics_piece.png");
    public static final ResourceLocation PROG_WIDGET_FOR_EACH_COORDINATE = progWidgetTexture("for_each_coordinate.png");
    public static final ResourceLocation PROG_WIDGET_FOR_EACH_ITEM = progWidgetTexture("for_each_item.png");
    public static final ResourceLocation PROG_WIDGET_EDIT_SIGN = progWidgetTexture("edit_sign_piece.png");
    public static final ResourceLocation PROG_WIDGET_CONDITION_REDSTONE = progWidgetTexture("condition_redstone_piece.png");
    public static final ResourceLocation PROG_WIDGET_CONDITION_ENTITY = progWidgetTexture("condition_entity_piece.png");
    public static final ResourceLocation PROG_WIDGET_CONDITION_LIQUID_INVENTORY = progWidgetTexture("condition_liquid_inventory_piece.png");
    public static final ResourceLocation PROG_WIDGET_CONDITION_ITEM_INVENTORY = progWidgetTexture("condition_item_inventory_piece.png");
    public static final ResourceLocation PROG_WIDGET_CONDITION_BLOCK = progWidgetTexture("condition_block_piece.png");
    public static final ResourceLocation PROG_WIDGET_CONDITION_ENERGY = progWidgetTexture("condition_rf_piece.png");
    public static final ResourceLocation PROG_WIDGET_CONDITION_PRESSURE = progWidgetTexture("condition_pressure_piece.png");
    public static final ResourceLocation PROG_WIDGET_CONDITION_COORDINATE = progWidgetTexture("condition_coordinate_piece.png");
    public static final ResourceLocation PROG_WIDGET_CONDITION_ITEM = progWidgetTexture("condition_item.png");
    public static final ResourceLocation PROG_WIDGET_CONDITION_LIGHT = progWidgetTexture("condition_light_piece.png");
    public static final ResourceLocation PROG_WIDGET_CONDITION_DRONE_ENTITY = progWidgetTexture("condition_drone_entity_piece.png");
    public static final ResourceLocation PROG_WIDGET_CONDITION_DRONE_LIQUID_INVENTORY = progWidgetTexture("condition_drone_liquid_piece.png");
    public static final ResourceLocation PROG_WIDGET_CONDITION_DRONE_ITEM_INVENTORY = progWidgetTexture("condition_drone_inventory_piece.png");
    public static final ResourceLocation PROG_WIDGET_CONDITION_DRONE_RF = progWidgetTexture("condition_drone_rf_piece.png");
    public static final ResourceLocation PROG_WIDGET_CONDITION_DRONE_PRESSURE = progWidgetTexture("condition_drone_pressure_piece.png");
//    public static final ResourceLocation PROG_WIDGET_CONDITION_DRONE_UPGRADES = progWidgetTexture("condition_drone_upgrades_piece.png");
//    public static final ResourceLocation PROG_WIDGET_VOID_ITEM = progWidgetTexture("void_item_piece.png");
//    public static final ResourceLocation PROG_WIDGET_VOID_LIQUID = progWidgetTexture("void_fluid_piece.png");

    // misc GUI Icons
    public static final ResourceLocation GUI_BUILDCRAFT_ENERGY = guiIconTexture("gui_buildcraft_energy.png");
    public static final ResourceLocation GUI_COPY_ICON_LOCATION = guiIconTexture("gui_copy.png");
    public static final ResourceLocation GUI_DELETE_ICON_LOCATION = guiIconTexture("gui_delete.png");
    public static final ResourceLocation GUI_INFO_LOCATION = guiIconTexture("gui_info.png");
    public static final ResourceLocation GUI_NO_PROBLEMS_TEXTURE = guiIconTexture("gui_no_problem.png");
    public static final ResourceLocation GUI_PASTE_ICON_LOCATION = guiIconTexture("gui_paste.png");
    public static final ResourceLocation GUI_PASTEBIN_ICON_LOCATION = guiIconTexture("gui_pastebin_icon.png");
    public static final ResourceLocation GUI_PROBLEMS_TEXTURE = guiIconTexture("gui_problem.png");
    public static final ResourceLocation GUI_REDO_ICON_LOCATION = guiIconTexture("gui_redo.png");
    public static final ResourceLocation GUI_RIGHT_ARROW = guiIconTexture("gui_right_arrow.png");
    public static final ResourceLocation GUI_UNDO_ICON_LOCATION = guiIconTexture("gui_undo.png");
    public static final ResourceLocation GUI_UPGRADES_LOCATION = guiIconTexture("gui_upgrade.png");
    public static final ResourceLocation GUI_WARNING_TEXTURE = guiIconTexture("gui_warning.png");
    public static final ResourceLocation GUI_X_BUTTON = guiIconTexture("gui_x_button.png");
    public static final ResourceLocation GUI_GREEN_PROBLEMS_TEXTURE = guiIconTexture("gui_green_problem.png");

    // widget textures
    public static final ResourceLocation WIDGET_ENERGY = guiTexture("widget/widget_energy.png");
    public static final ResourceLocation WIDGET_TEMPERATURE = guiTexture("widget/widget_temperature.png");
    public static final ResourceLocation WIDGET_TANK = guiTexture("widget/widget_tank.png");
    public static final ResourceLocation WIDGET_AMADRON_OFFER = guiTexture("widget/widget_amadron_offer.png");
    public static final ResourceLocation WIDGET_VERTICAL_SCROLLBAR = guiTexture("widget/widget_vertical_scrollbar.png");
    public static final ResourceLocation JEI_EXPLOSION = guiTexture("jei/explosion.png");
//    public static final ResourceLocation JEI_THERMOMETER = guiTexture("jei/thermometer.png");

    // misc rendering textures
    public static final ResourceLocation GUI_MINIGUN_CROSSHAIR = guiTexture("minigun_crosshair.png");
    public static final ResourceLocation RENDER_LASER = new ResourceLocation(RENDER_LOCATION + "laser/laser.png");
    public static final ResourceLocation RENDER_LASER_OVERLAY = new ResourceLocation(RENDER_LOCATION + "laser/laser_overlay.png");
    public static final ResourceLocation RENDER_LASER_START = new ResourceLocation(RENDER_LOCATION + "laser/laser_start.png");
    public static final ResourceLocation RENDER_LASER_START_OVERLAY = new ResourceLocation(RENDER_LOCATION + "laser/laser_start_overlay.png");
    public static final ResourceLocation GLOW_RESOURCE = new ResourceLocation(Textures.RENDER_BLUR);

    // entities
    public static final ResourceLocation DRONE_ENTITY = droneTexture("default_drone.png");
//    public static final ResourceLocation GUARD_DRONE_ENTITY = droneTexture("guard_drone.png");
    public static final ResourceLocation HARVESTING_DRONE_ENTITY = droneTexture("harvesting_drone.png");
    public static final ResourceLocation LOGISTICS_DRONE_ENTITY = droneTexture("logistics_drone.png");
//    public static final ResourceLocation COLLECTOR_DRONE_ENTITY = droneTexture("collector_drone.png");
//    public static final ResourceLocation AMADRONE_ENTITY = droneTexture("amadrone.png");
//    public static final ResourceLocation VORTEX_ENTITY = entityTexture("vortex.png");
    public static final ResourceLocation MICROMISSILE_ENTITY = entityTexture("micromissile.png");

    public static final String VILLAGER_MECHANIC = ENTITY_LOCATION + "villager_mechanic.png";
    public static final String VILLAGER_MECHANIC_ZOMBIE = ENTITY_LOCATION + "villager_mechanic_zombie.png";

    private static ResourceLocation guiTexture(String img) {
        return new ResourceLocation(GUI_LOCATION + img);
    }

    public static ResourceLocation guiIconTexture(String img) {
        return new ResourceLocation(GUI_LOCATION + "icon/" + img);
    }

    public static ResourceLocation modelTexture(String img) {
        return new ResourceLocation(MODEL_LOCATION + img);
    }

    public static ResourceLocation modelTextureNew(String img) {
        return new ResourceLocation(PNCMODEL_LOCATION + img);
    }

    private static ResourceLocation tubeModuleTexture(String img) {
        return new ResourceLocation(TUBE_MODULE_MODEL_LOCATION + img);
    }

    public static ResourceLocation progWidgetTexture(String img) {
        return new ResourceLocation(PROG_WIDGET_LOCATION + img);
    }

    public static ResourceLocation entityTexture(String img) {
        return new ResourceLocation(ENTITY_LOCATION + img);
    }

    public static ResourceLocation droneTexture(String img) {
        return new ResourceLocation(DRONE_LOCATION + img);
    }
}
