package me.desht.pneumaticcraft.client;

import me.desht.pneumaticcraft.client.render.pneumatic_armor.HUDHandler;
import me.desht.pneumaticcraft.lib.Names;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

public class KeyHandler {
    private static final String DESCRIPTION_ARMOR_OPTIONS = "pneumaticcraft.armor.options";
    private static final String DESCRIPTION_HELMET_HACK = "pneumaticcraft.helmet.hack";
    private static final String DESCRIPTION_HELMET_DEBUGGING_DRONE = "pneumaticcraft.helmet.debugging.drone";
    private static final String DESCRIPTION_BOOTS_KICK = "pneumaticcraft.boots.kick";
    private static final String DESCRIPTION_LAUNCHER = "pneumaticcraft.chestplate.launcher";

    private static final KeyHandler INSTANCE = new KeyHandler();

    public final KeyBinding keybindOpenOptions;
    public final KeyBinding keybindHack;
    public final KeyBinding keybindDebuggingDrone;
    public final KeyBinding keybindKick;
    public final KeyBinding keybindLauncher;
    private final List<IKeyListener> keyListeners = new ArrayList<>();
    private final List<KeyBinding> keys = new ArrayList<>();

    public static KeyHandler getInstance() {
        return INSTANCE;
    }

    private KeyHandler() {
        this.registerKeyListener(HUDHandler.instance());

        this.keybindOpenOptions = this.registerKeyBinding(new KeyBinding(KeyHandler.DESCRIPTION_ARMOR_OPTIONS, KeyConflictContext.IN_GAME, KeyModifier.NONE, Keyboard.KEY_U, Names.PNEUMATIC_KEYBINDING_CATEGORY));
        this.keybindHack = this.registerKeyBinding(new KeyBinding(KeyHandler.DESCRIPTION_HELMET_HACK, KeyConflictContext.IN_GAME, KeyModifier.NONE, Keyboard.KEY_H, Names.PNEUMATIC_KEYBINDING_CATEGORY));
        this.keybindDebuggingDrone = this.registerKeyBinding(new KeyBinding(KeyHandler.DESCRIPTION_HELMET_DEBUGGING_DRONE, KeyConflictContext.IN_GAME, KeyModifier.NONE, Keyboard.KEY_Y, Names.PNEUMATIC_KEYBINDING_CATEGORY));
        this.keybindKick = this.registerKeyBinding(new KeyBinding(KeyHandler.DESCRIPTION_BOOTS_KICK, KeyConflictContext.IN_GAME, KeyModifier.CONTROL, Keyboard.KEY_X, Names.PNEUMATIC_KEYBINDING_CATEGORY));
        this.keybindLauncher = this.registerKeyBinding(new KeyBinding(KeyHandler.DESCRIPTION_LAUNCHER, KeyConflictContext.IN_GAME, KeyModifier.CONTROL, Keyboard.KEY_C, Names.PNEUMATIC_KEYBINDING_CATEGORY));
    }

    private KeyBinding registerKeyBinding(KeyBinding keyBinding) {
        ClientRegistry.registerKeyBinding(keyBinding);
        this.keys.add(keyBinding);
        return keyBinding;
    }

    private void registerKeyListener(IKeyListener listener) {
        this.keyListeners.add(listener);
    }

    /**
     * This will only subscribe when NotEnoughKeys is not installed.
     *
     * @param event
     */
    @SubscribeEvent
    public void onKey(KeyInputEvent event) {
        for (KeyBinding key : this.keys) {
            if (key.isPressed()) {
                this.onKey(key);
            }
        }
    }

    public void onKey(KeyBinding keybinding) {
        for (IKeyListener listener : this.keyListeners) {
            listener.onKeyPress(keybinding);
        }
    }

}
