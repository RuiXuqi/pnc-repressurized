package me.desht.pneumaticcraft.client.gui.widget;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SideOnly(Side.CLIENT)
public class GuiRadioButton extends Gui implements IGuiWidget {
    public boolean checked, enabled = true;
    public final int x, y, color;
    private final int id;
    public final String text;
    private final FontRenderer fontRenderer = FMLClientHandler.instance().getClient().fontRenderer;
    private List<String> tooltip = new ArrayList<>();
    public List<GuiRadioButton> otherChoices;
    private IWidgetListener listener;

    private static final int BUTTON_WIDTH = 10;
    private static final int BUTTON_HEIGHT = 10;

    public GuiRadioButton(int id, int x, int y, int color, String text) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.color = color;
        this.text = text;
    }

    @Override
    public int getID() {
        return this.id;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTick) {
        this.drawCircle(this.x + BUTTON_WIDTH / 2, this.y + BUTTON_HEIGHT / 2, BUTTON_WIDTH / 2, this.enabled ? 0xFFA0A0A0 : 0xFF999999);
        this.drawCircle(this.x + BUTTON_WIDTH / 2, this.y + BUTTON_HEIGHT / 2, BUTTON_WIDTH / 2 - 1, this.enabled ? 0XFF202020 : 0xFFAAAAAA);
        if (this.checked) {
            this.drawCircle(this.x + BUTTON_WIDTH / 2, this.y + BUTTON_HEIGHT / 2, 1, this.enabled ? 0xFFFFFFFF : 0xFFAAAAAA);
        }
        this.fontRenderer.drawString(I18n.format(this.text), this.x + 1 + BUTTON_WIDTH, this.y + BUTTON_HEIGHT / 2 - this.fontRenderer.FONT_HEIGHT / 2, this.enabled ? this.color : 0xFF888888);
    }

    private void drawCircle(int x, int y, int radius, int color) {
        BufferBuilder wr = Tessellator.getInstance().getBuffer();
        float f = (color >> 24 & 255) / 255.0F;
        float f1 = (color >> 16 & 255) / 255.0F;
        float f2 = (color >> 8 & 255) / 255.0F;
        float f3 = (color & 255) / 255.0F;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(f1, f2, f3, f);
        wr.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
        int points = 20;
        for (int i = 0; i < points; i++) {
            double sin = Math.sin((double) i / points * Math.PI * 2);
            double cos = Math.cos((double) i / points * Math.PI * 2);
            wr.pos(x + sin * radius, y + cos * radius, this.zLevel).endVertex();
        }
        Tessellator.getInstance().draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(this.x, this.y, BUTTON_WIDTH + this.fontRenderer.getStringWidth(this.text), BUTTON_HEIGHT);
    }

    @Override
    public void onMouseClicked(int mouseX, int mouseY, int button) {
        if (this.enabled) {
            if (this.otherChoices != null) {
                for (GuiRadioButton radioButton : this.otherChoices) {
                    radioButton.checked = false;
                }
            } else {
                throw new IllegalArgumentException("A radio button needs more than one choice! You need to set the GuiRadioButton#otherChoices field!");
            }
            this.checked = true;
            this.listener.actionPerformed(this);
        }
    }

    @Override
    public void onMouseClickedOutsideBounds(int mouseX, int mouseY, int button) {

    }

    public void setTooltip(String tooltip) {
        this.setTooltip(Collections.singletonList(tooltip));
    }

    public void setTooltip(List<String> tooltip) {
        this.tooltip = tooltip;
    }

    @Override
    public void addTooltip(int mouseX, int mouseY, List<String> curTooltip, boolean shiftPressed) {
        curTooltip.addAll(this.tooltip);
    }

    @Override
    public boolean onKey(char key, int keyCode) {
        return false;
    }

    @Override
    public void setListener(IWidgetListener gui) {
        this.listener = gui;
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
