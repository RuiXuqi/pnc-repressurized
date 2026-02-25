package me.desht.pneumaticcraft.client.gui.pneumatic_armor;

import me.desht.pneumaticcraft.client.gui.GuiButtonSpecial;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.client.FMLClientHandler;

class KeybindingButton extends GuiButtonSpecial {
    private final KeyBinding keyBinding;
    private final String origButtonText;
    private boolean bindingMode = false;

    KeybindingButton(int buttonID, int startX, int startY, int xSize, int ySize, String buttonText, KeyBinding keyBinding) {
        super(buttonID, startX, startY, xSize, ySize, buttonText);
        this.keyBinding = keyBinding;
        this.origButtonText = buttonText;
        this.addTooltip();
    }

    private void addTooltip() {
        this.setTooltipText("Bound to: " + TextFormatting.GREEN + this.keyBinding.getDisplayName());
    }

    void toggleKeybindMode() {
        this.bindingMode = !this.bindingMode;

        if (this.bindingMode) {
            this.displayString = TextFormatting.YELLOW + "Press a key to set keybind";
            this.setTooltipText("");
        } else {
            this.displayString = this.origButtonText;
            this.addTooltip();
        }
    }

    boolean receiveKey(int key) {
        if (this.bindingMode && !KeyModifier.isKeyCodeModifier(key)) {
            this.keyBinding.setKeyModifierAndCode(KeyModifier.getActiveModifier(), key);
            KeyBinding.resetKeyBindingArrayAndHash();
            FMLClientHandler.instance().getClient().gameSettings.saveOptions();
            FMLClientHandler.instance().getClient().player.playSound(SoundEvents.BLOCK_NOTE_CHIME, 1.0f, 1.0f);
            this.toggleKeybindMode();
            return true;
        } else {
            return false;
        }
    }

}
