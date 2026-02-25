package me.desht.pneumaticcraft.client.gui.widget;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WidgetTextField extends GuiTextField implements IGuiWidget {

    protected IWidgetListener listener;
    private final List<String> tooltip = new ArrayList<>();
    private boolean passwordBox;

    public WidgetTextField(FontRenderer fontRenderer, int x, int y, int width, int height) {
        super(-1, fontRenderer, x, y, width, height);
    }

    @Override
    public void setListener(IWidgetListener gui) {
        this.listener = gui;
    }

    public WidgetTextField setAsPasswordBox() {
        this.passwordBox = true;
        return this;
    }

    @Override
    public int getID() {
        return -1;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTick) {
        String oldText = this.getText();
        int oldCursorPos = this.getCursorPosition();
        if (this.passwordBox) {
            this.setText(StringUtils.repeat('*', oldText.length()));
            this.setCursorPosition(oldCursorPos);
        }
        this.drawTextBox();
        if (this.passwordBox) {
            this.setText(oldText);
            this.setCursorPosition(oldCursorPos);
        }
    }

    @Override
    public void onMouseClicked(int mouseX, int mouseY, int button) {
        this.mouseClicked(mouseX, mouseY, button);
        if (this.isFocused() && button == 1) {
            this.setText("");
            this.listener.onKeyTyped(this);
        }
    }

    @Override
    public void onMouseClickedOutsideBounds(int mouseX, int mouseY, int button) {
        this.onMouseClicked(mouseX, mouseY, button);
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(this.x, this.y, this.width, this.height);
    }

    @Override
    public void addTooltip(int mouseX, int mouseY, List<String> curTooltip, boolean shiftPressed) {
        curTooltip.addAll(this.tooltip);
    }

    public void setTooltip(String... tooltip) {
        this.tooltip.clear();
        Collections.addAll(this.tooltip, tooltip);
    }

    @Override
    public boolean onKey(char key, int keyCode) {
        if (this.textboxKeyTyped(key, keyCode)) {
            this.listener.onKeyTyped(this);
            return true;
        }
        return false;
    }

    @Override
    public void update() {

    }

    @Override
    public void handleMouseInput() {

    }

    @Override
    public void postRender(int mouseX, int mouseY, float partialTick) {
    }

}
