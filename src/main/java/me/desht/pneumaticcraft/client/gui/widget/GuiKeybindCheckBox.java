package me.desht.pneumaticcraft.client.gui.widget;

import me.desht.pneumaticcraft.PneumaticCraftRepressurized;
import me.desht.pneumaticcraft.api.client.pneumaticHelmet.IUpgradeRenderHandler;
import me.desht.pneumaticcraft.client.render.pneumatic_armor.HUDHandler;
import me.desht.pneumaticcraft.client.render.pneumatic_armor.UpgradeRenderHandlerList;
import me.desht.pneumaticcraft.common.config.HelmetWidgetDefaults;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketToggleArmorFeature;
import me.desht.pneumaticcraft.common.pneumatic_armor.CommonArmorHandler;
import me.desht.pneumaticcraft.lib.Names;
import me.desht.pneumaticcraft.proxy.ClientProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.*;

public class GuiKeybindCheckBox extends GuiCheckBox {
    public static final String UPGRADE_PREFIX = "pneumaticHelmet.upgrade.";

    private boolean isAwaitingKey;
    private String oldCheckboxText;
    private KeyBinding keyBinding;
    private final String keyBindingName;
    private static final Map<String, GuiKeybindCheckBox> trackedCheckboxes = new HashMap<>();
    private static GuiKeybindCheckBox coreComponents;

    public GuiKeybindCheckBox(int id, int x, int y, int color, String text) {
        this(id, x, y, color, text, text);
    }

    public GuiKeybindCheckBox(int id, int x, int y, int color, String text, String keyBindingName) {
        super(id, x, y, color, text);
        this.keyBindingName = keyBindingName;
        this.keyBinding = setOrAddKeybind(keyBindingName, -1, KeyModifier.NONE); //get the saved value.
        if (!trackedCheckboxes.containsKey(keyBindingName)) {
            this.checked = HelmetWidgetDefaults.INSTANCE.getKey(keyBindingName);
            trackedCheckboxes.put(keyBindingName, this);
            MinecraftForge.EVENT_BUS.register(this);
            if (keyBindingName.equals(UPGRADE_PREFIX + "coreComponents")) {
                // stash this one since it's referenced a lot
                coreComponents = this;
            }
        } else {
            this.checked = trackedCheckboxes.get(keyBindingName).checked;
        }
    }

    public static GuiKeybindCheckBox getCoreComponents() {
        return coreComponents;
    }

    public static GuiKeybindCheckBox fromKeyBindingName(String name) {
        return trackedCheckboxes.get(name);
    }

    public static boolean isHandlerEnabled(IUpgradeRenderHandler handler) {
        return fromKeyBindingName(GuiKeybindCheckBox.UPGRADE_PREFIX + handler.getUpgradeName()).checked;
    }

    @Override
    public void onMouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0) {
            super.onMouseClicked(mouseX, mouseY, button);
            GuiKeybindCheckBox trackedBox = trackedCheckboxes.get(this.keyBindingName);
            if (trackedBox != this) {
                trackedBox.onMouseClicked(mouseX, mouseY, button);
            } else {
                HelmetWidgetDefaults.INSTANCE.setKey(this.keyBindingName, this.checked);
                try {
                    HelmetWidgetDefaults.INSTANCE.writeToFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                CommonArmorHandler hudHandler = CommonArmorHandler.getHandlerForPlayer();
                for (EntityEquipmentSlot slot : UpgradeRenderHandlerList.ARMOR_SLOTS) {
                    List<IUpgradeRenderHandler> renderHandlers = UpgradeRenderHandlerList.instance().getHandlersForSlot(slot);
                    for (int i = 0; i < renderHandlers.size(); i++) {
                        IUpgradeRenderHandler upgradeRenderHandler = renderHandlers.get(i);
                        if ((UPGRADE_PREFIX + upgradeRenderHandler.getUpgradeName()).equals(this.keyBindingName) && hudHandler.isUpgradeRendererInserted(slot, i)) {
                            NetworkHandler.sendToServer(new PacketToggleArmorFeature((byte) i, coreComponents.checked && this.checked, slot));
                            hudHandler.setUpgradeRenderEnabled(slot, (byte) i, coreComponents.checked && this.checked);
                            HUDHandler.instance().addFeatureToggleMessage(this.keyBindingName, this.checked);
                            break;
                        }
                    }
                    if (this.keyBindingName.equals(UPGRADE_PREFIX + "coreComponents")) {
                        for (int i = 0; i < renderHandlers.size(); i++) {
                            boolean state = GuiKeybindCheckBox.fromKeyBindingName(GuiKeybindCheckBox.UPGRADE_PREFIX + renderHandlers.get(i).getUpgradeName()).checked;
                            NetworkHandler.sendToServer(new PacketToggleArmorFeature((byte) i, coreComponents.checked && state, slot));
                            hudHandler.setUpgradeRenderEnabled(slot, (byte) i, coreComponents.checked && state);
                        }
                    }
                }
            }
        } else {
            if (PneumaticCraftRepressurized.proxy.isSneakingInGui()) {
                this.clearKeybinding();
            } else {
                this.isAwaitingKey = !this.isAwaitingKey;
                if (this.isAwaitingKey) {
                    this.oldCheckboxText = this.text;
                    this.text = "gui.setKeybind";
                } else {
                    this.text = this.oldCheckboxText;
                }
            }
        }
    }

    private void clearKeybinding() {
        KeyBinding[] keyBindings = Minecraft.getMinecraft().gameSettings.keyBindings;
        Set<Integer> idx = new HashSet<>();
        for (int i = 0; i < keyBindings.length; i++) {
            if (keyBindings[i].getKeyDescription().equals(this.keyBinding.getKeyDescription())) {
                idx.add(i);
                break;
            }
        }
        if (!idx.isEmpty()) {
            List<KeyBinding> l = new ArrayList<>(keyBindings.length);
            for (int i = 0; i < keyBindings.length; i++) {
                if (!idx.contains(i)) l.add(keyBindings[i]);
            }
            Minecraft.getMinecraft().gameSettings.keyBindings = l.toArray(new KeyBinding[0]);
            this.keyBinding = new KeyBinding(this.keyBindingName, KeyConflictContext.IN_GAME, KeyModifier.NONE, Keyboard.KEY_NONE, Names.PNEUMATIC_KEYBINDING_CATEGORY);
            ClientRegistry.registerKeyBinding(this.keyBinding);
            KeyBinding.resetKeyBindingArrayAndHash();
            ((ClientProxy) PneumaticCraftRepressurized.proxy).keybindToKeyCodes.put(this.keyBindingName, Pair.of(Keyboard.KEY_NONE, KeyModifier.NONE));
            Minecraft.getMinecraft().gameSettings.saveOptions();
        }
    }

    @Override
    public boolean onKey(char key, int keyCode) {
        if (this.isAwaitingKey) {
            if (KeyModifier.isKeyCodeModifier(keyCode)) {
                return true;
            } else {
                this.isAwaitingKey = false;
                this.keyBinding = setOrAddKeybind(this.keyBindingName, keyCode, KeyModifier.getActiveModifier());
                this.text = this.oldCheckboxText;
                if (trackedCheckboxes.containsKey(this.keyBindingName)) {
                    MinecraftForge.EVENT_BUS.unregister(trackedCheckboxes.get(this.keyBindingName));
                }
                MinecraftForge.EVENT_BUS.register(this);
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = FMLClientHandler.instance().getClient();
        if (mc.inGameHasFocus && this.keyBinding != null && this.keyBinding.isPressed()) {
            this.onMouseClicked(0, 0, 0);
        }
    }

    /**
     * @param keybindName name of the key binding
     * @param keyCode     when < 0, this will function as a getter & may return null
     * @param modifier    key modifier (may be NONE)
     * @return the key binding
     */
    private static KeyBinding setOrAddKeybind(String keybindName, int keyCode, KeyModifier modifier) {
        GameSettings gameSettings = FMLClientHandler.instance().getClient().gameSettings;
        for (KeyBinding keyBinding : gameSettings.keyBindings) {
            if (keyBinding != null && keyBinding.getKeyDescription().equals(keybindName)) {
                if (keyCode >= 0) {
                    keyBinding.setKeyModifierAndCode(modifier, keyCode);
                    KeyBinding.resetKeyBindingArrayAndHash();
                    gameSettings.saveOptions();
                }
                return keyBinding;
            }
        }
        // If the keybind wasn't registered yet, look for it in the Minecraft options.txt file (which
        // we scanned in ClientProxy#getAllKeybindsFromOptionsFile() during pre-init)
        if (keyCode < 0) {
            if (((ClientProxy) PneumaticCraftRepressurized.proxy).keybindToKeyCodes.containsKey(keybindName)) {
                Pair<Integer, KeyModifier> binding = ((ClientProxy) PneumaticCraftRepressurized.proxy).keybindToKeyCodes.get(keybindName);
                keyCode = binding.getLeft();
                modifier = binding.getRight();
            } else {
                return null;
            }
        }
        KeyBinding keyBinding = new KeyBinding(keybindName, KeyConflictContext.IN_GAME, modifier, keyCode, Names.PNEUMATIC_KEYBINDING_CATEGORY);
        ClientRegistry.registerKeyBinding(keyBinding);
        KeyBinding.resetKeyBindingArrayAndHash();
        gameSettings.saveOptions();
        return keyBinding;
    }

    @Override
    public void addTooltip(int mouseX, int mouseY, List<String> curTooltip, boolean shiftPressed) {
        if (this.keyBinding != null) {
            String s = this.keyBinding.getKeyModifier() != KeyModifier.NONE ? this.keyBinding.getKeyModifier() + " + " : "";
            curTooltip.add(I18n.format("gui.keybindBoundKey", s + Keyboard.getKeyName(this.keyBinding.getKeyCode())));
        }
        if (!this.isAwaitingKey) {
            curTooltip.add("gui.keybindRightClickToSet");
            if (this.keyBinding != null && this.keyBinding.getKeyCode() != Keyboard.KEY_NONE) {
                curTooltip.add("gui.keybindShiftRightClickToClear");
            }
        }
    }
}
