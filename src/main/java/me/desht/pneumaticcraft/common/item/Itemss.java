package me.desht.pneumaticcraft.common.item;

import me.desht.pneumaticcraft.api.item.IItemRegistry.EnumUpgrade;
import me.desht.pneumaticcraft.common.block.BlockAphorismTile;
import me.desht.pneumaticcraft.common.block.Blockss;
import me.desht.pneumaticcraft.common.block.ICustomItemBlock;
import me.desht.pneumaticcraft.common.entity.living.EntityHarvestingDrone;
import me.desht.pneumaticcraft.common.entity.living.EntityLogisticsDrone;
import me.desht.pneumaticcraft.common.semiblock.*;
import me.desht.pneumaticcraft.common.thirdparty.ThirdPartyManager;
import me.desht.pneumaticcraft.common.util.NBTUtil;
import me.desht.pneumaticcraft.common.util.UpgradableItemUtils;
import me.desht.pneumaticcraft.lib.Names;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = Names.MOD_ID)
@ObjectHolder(Names.MOD_ID)
public class Itemss {
    @ObjectHolder("gps_tool")
    public static final Item GPS_TOOL = Items.AIR;
    @ObjectHolder("gps_area_tool")
    public static final Item GPS_AREA_TOOL = Items.AIR;
    @ObjectHolder("ingot_iron_compressed")
    public static final Item INGOT_IRON_COMPRESSED = Items.AIR;
    @ObjectHolder("pressure_gauge")
    public static final Item PRESSURE_GAUGE = Items.AIR;
    @ObjectHolder("stone_base")
    public static final Item STONE_BASE = Items.AIR;
    @ObjectHolder("cannon_barrel")
    public static final Item CANNON_BARREL = Items.AIR;
    @ObjectHolder("turbine_blade")
    public static final Item TURBINE_BLADE = Items.AIR;
    @ObjectHolder("plastic")
    public static final Item PLASTIC = Items.AIR;
    @ObjectHolder("air_canister")
    public static final Item AIR_CANISTER = Items.AIR;
    @ObjectHolder("reinforced_air_canister")
    public static final Item REINFORCED_AIR_CANISTER = Items.AIR;
    @ObjectHolder("vortex_cannon")
    public static final Item VORTEX_CANNON = Items.AIR;
    @ObjectHolder("pneumatic_cylinder")
    public static final Item PNEUMATIC_CYLINDER = Items.AIR;
    @ObjectHolder("pneumatic_helmet")
    public static final Item PNEUMATIC_HELMET = Items.AIR;
    @ObjectHolder("pneumatic_chestplate")
    public static final Item PNEUMATIC_CHESTPLATE = Items.AIR;
    @ObjectHolder("pneumatic_leggings")
    public static final Item PNEUMATIC_LEGGINGS = Items.AIR;
    @ObjectHolder("pneumatic_boots")
    public static final Item PNEUMATIC_BOOTS = Items.AIR;
    @ObjectHolder("manometer")
    public static final Item MANOMETER = Items.AIR;
    @ObjectHolder("turbine_rotor")
    public static final Item TURBINE_ROTOR = Items.AIR;
    @ObjectHolder("assembly_program")
    public static final Item ASSEMBLY_PROGRAM = Items.AIR;
    @ObjectHolder("empty_pcb")
    public static final Item EMPTY_PCB = Items.AIR;
    @ObjectHolder("unassembled_pcb")
    public static final Item UNASSEMBLED_PCB = Items.AIR;
    @ObjectHolder("pcb_blueprint")
    public static final Item PCB_BLUEPRINT = Items.AIR;
    @ObjectHolder("transistor")
    public static final Item TRANSISTOR = Items.AIR;
    @ObjectHolder("capacitor")
    public static final Item CAPACITOR = Items.AIR;
    @ObjectHolder("printed_circuit_board")
    public static final Item PRINTED_CIRCUIT_BOARD = Items.AIR;
    @ObjectHolder("failed_pcb")
    public static final Item FAILED_PCB = Items.AIR;
    @ObjectHolder("network_component")
    public static final Item NETWORK_COMPONENT = Items.AIR;
    @ObjectHolder("stop_worm")
    public static final Item STOP_WORM = Items.AIR;
    @ObjectHolder("nuke_virus")
    public static final Item NUKE_VIRUS = Items.AIR;
    @ObjectHolder("compressed_iron_gear")
    public static final Item COMPRESSED_IRON_GEAR = Items.AIR;
    @ObjectHolder("pneumatic_wrench")
    public static final Item PNEUMATIC_WRENCH = Items.AIR;
    @ObjectHolder("drone")
    public static final Item DRONE = Items.AIR;
    @ObjectHolder("programming_puzzle")
    public static final Item PROGRAMMING_PUZZLE = Items.AIR;
    @ObjectHolder("advanced_pcb")
    public static final Item ADVANCED_PCB = Items.AIR;
    @ObjectHolder("remote")
    public static final Item REMOTE = Items.AIR;
    @ObjectHolder("seismic_sensor")
    public static final Item SEISMIC_SENSOR = Items.AIR;
    @ObjectHolder("logistics_configurator")
    public static final Item LOGISTICS_CONFIGURATOR = Items.AIR;
    @ObjectHolder(SemiBlockRequester.ID)
    public static final Item LOGISTICS_FRAME_REQUESTER = Items.AIR;
    @ObjectHolder(SemiBlockStorage.ID)
    public static final Item LOGISTICS_FRAME_STORAGE = Items.AIR;
    @ObjectHolder(SemiBlockDefaultStorage.ID)
    public static final Item LOGISTICS_FRAME_DEFAULT_STORAGE = Items.AIR;
    @ObjectHolder(SemiBlockPassiveProvider.ID)
    public static final Item LOGISTICS_FRAME_PASSIVE_PROVIDER = Items.AIR;
    @ObjectHolder(SemiBlockActiveProvider.ID)
    public static final Item LOGISTICS_FRAME_ACTIVE_PROVIDER = Items.AIR;
    @ObjectHolder(SemiBlockHeatFrame.ID)
    public static final Item HEAT_FRAME = Items.AIR;
    @ObjectHolder(SemiBlockSpawnerAgitator.ID)
    public static final Item SPAWNER_AGITATOR = Items.AIR;
    @ObjectHolder(SemiBlockCropSupport.ID)
    public static final Item CROP_SUPPORT = Items.AIR;
    @ObjectHolder(SemiBlockTransferGadget.ID)
    public static final Item TRANSFER_GADGET = Items.AIR;
    @ObjectHolder("logistic_drone")
    public static final Item LOGISTICS_DRONE = Items.AIR;
    @ObjectHolder("harvesting_drone")
    public static final Item HARVESTING_DRONE = Items.AIR;
    @ObjectHolder("gun_ammo")
    public static final Item GUN_AMMO = Items.AIR;
    @ObjectHolder("gun_ammo_incendiary")
    public static final Item GUN_AMMO_INCENDIARY = Items.AIR;
    @ObjectHolder("gun_ammo_weighted")
    public static final Item GUN_AMMO_WEIGHTED = Items.AIR;
    @ObjectHolder("gun_ammo_ap")
    public static final Item GUN_AMMO_ARMOR_PIERCING = Items.AIR;
    @ObjectHolder("gun_ammo_explosive")
    public static final Item GUN_AMMO_EXPLOSIVE = Items.AIR;
    @ObjectHolder("gun_ammo_freezing")
    public static final Item GUN_AMMO_FREEZING = Items.AIR;
    @ObjectHolder("amadron_tablet")
    public static final Item AMADRON_TABLET = Items.AIR;
    @ObjectHolder("minigun")
    public static final Item MINIGUN = Items.AIR;
    @ObjectHolder("camo_applicator")
    public static final Item CAMO_APPLICATOR = Items.AIR;
    @ObjectHolder("micromissiles")
    public static final Item MICROMISSILES = Items.AIR;

    public static List<Item> items = new ArrayList<>();
    private static final List<ItemBlock> all_itemblocks = new ArrayList<>();
    public static UpgradeList upgrades = new UpgradeList();

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        IForgeRegistry<Item> registry = event.getRegistry();

        registerItem(registry, new ItemGPSTool());
        registerItem(registry, new ItemGPSAreaTool());
        registerItem(registry, new ItemPneumatic("ingot_iron_compressed"));
        registerItem(registry, new ItemPneumatic("pressure_gauge"));
        registerItem(registry, new ItemPneumatic("stone_base"));
        registerItem(registry, new ItemPneumatic("cannon_barrel"));
        registerItem(registry, new ItemPneumatic("turbine_blade"));
        registerItem(registry, new ItemPlastic());
        registerItem(registry, new ItemPressurizable("air_canister", PneumaticValues.AIR_CANISTER_MAX_AIR, PneumaticValues.AIR_CANISTER_VOLUME));
        registerItem(registry, new ItemReinforcedAirCanister());
        registerItem(registry, new ItemVortexCannon());
        registerItem(registry, new ItemPneumatic("pneumatic_cylinder"));
        registerItem(registry, new ItemPneumaticArmor("pneumatic_helmet", EntityEquipmentSlot.HEAD));
        registerItem(registry, new ItemPneumaticArmor("pneumatic_chestplate", EntityEquipmentSlot.CHEST));
        registerItem(registry, new ItemPneumaticArmor("pneumatic_leggings", EntityEquipmentSlot.LEGS));
        registerItem(registry, new ItemPneumaticArmor("pneumatic_boots", EntityEquipmentSlot.FEET));
        registerItem(registry, new ItemManometer());
        registerItem(registry, new ItemPneumatic("turbine_rotor"));
        registerItem(registry, new ItemAssemblyProgram());
        registerItem(registry, new ItemEmptyPCB());
        registerItem(registry, new ItemNonDespawning("unassembled_pcb"));
        registerItem(registry, new ItemPneumatic("pcb_blueprint"));
        registerItem(registry, new ItemPneumatic("transistor"));
        registerItem(registry, new ItemPneumatic("capacitor"));
        registerItem(registry, new ItemPneumatic("printed_circuit_board"));
        registerItem(registry, new ItemNonDespawning("failed_pcb"));
        registerItem(registry, new ItemNetworkComponents());
        registerItem(registry, new ItemPneumatic("stop_worm"));
        registerItem(registry, new ItemPneumatic("nuke_virus"));
        registerItem(registry, new ItemPneumatic("compressed_iron_gear"));
        registerItem(registry, new ItemPneumaticWrench());
        registerItem(registry, new ItemDrone());
        registerItem(registry, new ItemProgrammingPuzzle());
        registerItem(registry, new ItemPneumatic("advanced_pcb"));
        registerItem(registry, new ItemRemote());
        registerItem(registry, new ItemSeismicSensor());
        registerItem(registry, new ItemLogisticsConfigurator());
        registerItem(registry, new ItemBasicDrone("logistic_drone", EntityLogisticsDrone::new));
        registerItem(registry, new ItemBasicDrone("harvesting_drone", EntityHarvestingDrone::new));
        registerItem(registry, new ItemGunAmmoStandard());
        registerItem(registry, new ItemGunAmmoIncendiary());
        registerItem(registry, new ItemGunAmmoWeighted());
        registerItem(registry, new ItemGunAmmoArmorPiercing());
        registerItem(registry, new ItemGunAmmoExplosive());
        registerItem(registry, new ItemGunAmmoFreezing());
        registerItem(registry, new ItemAmadronTablet());
        registerItem(registry, new ItemMinigun());
        registerItem(registry, new ItemCamoApplicator());
        registerItem(registry, new ItemMicromissiles());

        registerUpgrades(registry);

        ItemPneumaticArmor.initApplicableUpgrades();

        for (Block b : Blockss.blocks) {
            if (!(b instanceof BlockAir)) {
                ItemBlock itemBlock = b instanceof ICustomItemBlock customItemBlock ? customItemBlock.getCustomItemBlock() : new ItemBlock(b);
                //noinspection DataFlowIssue
                registerItem(registry, itemBlock.setRegistryName(b.getRegistryName()));
            }
        }
    }

    private static void registerUpgrades(IForgeRegistry<Item> registry) {
        for (EnumUpgrade upgrade : EnumUpgrade.values()) {
            if (upgrade.isDepLoaded()) {
                String upgradeName = upgrade.toString().toLowerCase() + "_upgrade";
                Item upgradeItem = new ItemMachineUpgrade(upgradeName, upgrade.ordinal());
                registerItem(registry, upgradeItem);
                upgrades.add(upgradeItem);
            } else {
                upgrades.add(null);
            }
        }
    }

    public static void registerItem(IForgeRegistry<Item> registry, Item item) {
        registry.register(item);
        ThirdPartyManager.instance().onItemRegistry(item);
        if (item instanceof ItemBlock itemBlock) {
            all_itemblocks.add(itemBlock);
        } else {
            items.add(item);
        }
    }

    public static class UpgradeList extends ArrayList<Item> {
        public Item get(EnumUpgrade upgrade) {
            return this.get(upgrade.ordinal());
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void registerItemColorHandlers(ColorHandlerEvent.Item event) {
        event.getItemColors().registerItemColorHandler((stack, tintIndex) -> {
            if (tintIndex == 1) {
                return getAmmoColor(stack);
            }
            return Color.WHITE.getRGB();
        }, Itemss.GUN_AMMO, Itemss.GUN_AMMO_INCENDIARY, Itemss.GUN_AMMO_ARMOR_PIERCING, Itemss.GUN_AMMO_EXPLOSIVE, Itemss.GUN_AMMO_WEIGHTED, Itemss.GUN_AMMO_FREEZING);

        event.getItemColors().registerItemColorHandler((stack, tintIndex) -> {
            int plasticColour = ItemPlastic.getColour(stack);
            return plasticColour >= 0 ? plasticColour : 0xffffff;
        }, Itemss.PLASTIC);

        event.getItemColors().registerItemColorHandler((stack, tintIndex) ->
                        NBTUtil.hasTag(stack, UpgradableItemUtils.NBT_CREATIVE) ? 0xFFFF60FF : 0xFFFFFFFF,
                Item.getItemFromBlock(Blockss.OMNIDIRECTIONAL_HOPPER), Item.getItemFromBlock(Blockss.LIQUID_HOPPER));

        event.getItemColors().registerItemColorHandler((stack, tintIndex) -> switch (tintIndex) {
            case 0 -> // border
                    EnumDyeColor.byDyeDamage(BlockAphorismTile.getBorderColor(stack)).getColorValue();
            case 1 -> // background
                    Blockss.desaturate(EnumDyeColor.byDyeDamage(BlockAphorismTile.getBackgroundColor(stack)).getColorValue());
            default -> 0xFFFFFF;
        }, Item.getItemFromBlock(Blockss.APHORISM_TILE));
    }

    public static int getAmmoColor(@Nonnull ItemStack stack) {
        if (stack.getItem() instanceof ItemGunAmmo gunAmmo) {
            return gunAmmo.getAmmoColor(stack);
        } else {
            return 0x00FFFF00;
        }
    }
}
