package me.desht.pneumaticcraft.client.gui.widget;

import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.Validate;
import org.lwjgl.input.Mouse;

public class WidgetVerticalScrollbar extends WidgetBase {
    private static final ResourceLocation SCROLL_TEXTURE = new ResourceLocation(Textures.GUI_LOCATION + "widget/vertical_scrollbar.png");

    public float currentScroll;
    private int states;
    private boolean listening;
    private boolean dragging;
    private boolean wasClicking;
    private boolean enabled = true;

    public WidgetVerticalScrollbar(int x, int y, int height) {
        this(-1, x, y, height);
    }

    public WidgetVerticalScrollbar(int id, int x, int y, int height) {
        super(id, x, y, 14, height);
    }

    public WidgetVerticalScrollbar setStates(int states) {
        this.states = states;
        return this;
    }

    public WidgetVerticalScrollbar setCurrentState(int state) {
        Validate.isTrue(state >= 0 && state <= this.states, "State " + state + " out of range! Valid range [1 - " + this.states + "] inclusive");
        this.currentScroll = (float) state / this.states;
        return this;
    }

    @Override
    public void handleMouseInput() {
        if (this.listening) {
            int wheel = -Mouse.getDWheel();
            wheel = MathHelper.clamp(wheel, -1, 1);
            this.currentScroll += (float) wheel / this.states;
        }
    }

    public WidgetVerticalScrollbar setListening(boolean listening) {
        this.listening = listening;
        return this;
    }

    public int getState() {
        float scroll = this.currentScroll;
        scroll += 0.5F / this.states;
        return MathHelper.clamp((int) (scroll * this.states), 0, this.states);
    }

    public WidgetVerticalScrollbar setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) this.wasClicking = false;
        return this;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTick) {
        GlStateManager.color(1, 1, 1, 1);
        if (!Mouse.isButtonDown(0)) this.dragging = false;
        if (!this.wasClicking && Mouse.isButtonDown(0) && this.getBounds().contains(mouseX, mouseY)) {
            this.dragging = true;
        }
        if (!this.enabled) this.dragging = false;
        this.wasClicking = Mouse.isButtonDown(0);
        if (this.dragging)
            this.currentScroll = (float) (mouseY - 7 - this.getBounds().y) / (this.getBounds().height - 17);
        this.currentScroll = MathHelper.clamp(this.currentScroll, 0, 1);
        Minecraft.getMinecraft().getTextureManager().bindTexture(SCROLL_TEXTURE);
        Gui.drawModalRectWithCustomSizedTexture(this.x, this.y, 12, 0, this.getBounds().width, 1, 26, 15);
        for (int i = 0; i < this.getBounds().height - 2; i++)
            Gui.drawModalRectWithCustomSizedTexture(this.x, this.y + 1 + i, 12, 1, this.getBounds().width, 1, 26, 15);
        Gui.drawModalRectWithCustomSizedTexture(this.x, this.y + this.getBounds().height - 1, 12, 14, this.getBounds().width, 1, 26, 15);

        if (!this.enabled) GlStateManager.color(0.6F, 0.6F, 0.6F, 1);
        Gui.drawModalRectWithCustomSizedTexture(this.x + 1, this.y + 1 + (int) ((this.getBounds().height - 17) * this.currentScroll), 0, 0, 12, 15, 26, 15);
        GlStateManager.color(1, 1, 1, 1);
    }

    public boolean isDragging() {
        return this.dragging;
    }
}
