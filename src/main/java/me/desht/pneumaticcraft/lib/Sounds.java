package me.desht.pneumaticcraft.lib;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.RL;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = Names.MOD_ID)
@GameRegistry.ObjectHolder(Names.MOD_ID)
public class Sounds {
    // To make IDEA happy
    private static final SoundEvent EMPTY = new SoundEvent(new ResourceLocation(""));
    @GameRegistry.ObjectHolder("air_cannon")
    public static final SoundEvent CANNON_SOUND = EMPTY;
    @GameRegistry.ObjectHolder("leaking_gas")
    public static final SoundEvent LEAKING_GAS_SOUND = EMPTY;
    @GameRegistry.ObjectHolder("pneumatic_crusher")
    public static final SoundEvent PNEUMATIC_CRUSHER_SOUND = EMPTY;
    @GameRegistry.ObjectHolder("interface_door")
    public static final SoundEvent INTERFACE_DOOR = EMPTY;
    @GameRegistry.ObjectHolder("elevator_rising_start")
    public static final SoundEvent ELEVATOR_START = EMPTY;
    @GameRegistry.ObjectHolder("elevator_rising_stop")
    public static final SoundEvent ELEVATOR_STOP = EMPTY;
    @GameRegistry.ObjectHolder("elevator_rising")
    public static final SoundEvent ELEVATOR_MOVING = EMPTY;
    @GameRegistry.ObjectHolder("helmet_hack_finish")
    public static final SoundEvent HELMET_HACK_FINISH = EMPTY;
    @GameRegistry.ObjectHolder("hud_init")
    public static final SoundEvent HUD_INIT = EMPTY;
    @GameRegistry.ObjectHolder("hud_init_complete")
    public static final SoundEvent HUD_INIT_COMPLETE = EMPTY;
    @GameRegistry.ObjectHolder("hud_entity_lock")
    public static final SoundEvent HUD_ENTITY_LOCK = EMPTY;
    @GameRegistry.ObjectHolder("sci_fi")
    public static final SoundEvent SCIFI = EMPTY;
    @GameRegistry.ObjectHolder("pneumatic_wrench")
    public static final SoundEvent PNEUMATIC_WRENCH = EMPTY;
    @GameRegistry.ObjectHolder("minigun")
    public static final SoundEvent MINIGUN = EMPTY;
    @GameRegistry.ObjectHolder("minigun_stop")
    public static final SoundEvent MINIGUN_STOP = EMPTY;
    @GameRegistry.ObjectHolder("drone_hurt")
    public static final SoundEvent DRONE_HURT = EMPTY;
    @GameRegistry.ObjectHolder("drone_death")
    public static final SoundEvent DRONE_DEATH = EMPTY;
    @GameRegistry.ObjectHolder("short_hiss")
    public static final SoundEvent SHORT_HISS = EMPTY;
    @GameRegistry.ObjectHolder("punch")
    public static final SoundEvent PUNCH = EMPTY;
    @GameRegistry.ObjectHolder("pneumatic_door")
    public static final SoundEvent PNEUMATIC_DOOR = EMPTY;
    @GameRegistry.ObjectHolder("chirp")
    public static final SoundEvent CHIRP = EMPTY;
    @GameRegistry.ObjectHolder("scuba")
    public static final SoundEvent SCUBA = EMPTY;
    @GameRegistry.ObjectHolder("leaking_gas_low")
    public static final SoundEvent LEAKING_GAS_LOW = EMPTY;

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().registerAll(
                buildSound("air_cannon"),
                buildSound("leaking_gas"),
                buildSound("pneumatic_crusher"),
                buildSound("interface_door"),
                buildSound("elevator_rising_start"),
                buildSound("elevator_rising_stop"),
                buildSound("elevator_rising"),
                buildSound("helmet_hack_finish"),
                buildSound("hud_init"),
                buildSound("hud_init_complete"),
                buildSound("hud_entity_lock"),
                buildSound("sci_fi"),
                buildSound("pneumatic_wrench"),
                buildSound("minigun"),
                buildSound("minigun_stop"),
                buildSound("drone_hurt"),
                buildSound("drone_death"),
                buildSound("short_hiss"),
                buildSound("punch"),
                buildSound("pneumatic_door"),
                buildSound("chirp"),
                buildSound("scuba"),
                buildSound("leaking_gas_low")
        );
    }

    private static SoundEvent buildSound(String key) {
        return new SoundEvent(RL(key)).setRegistryName(key);
    }
}
