package me.desht.pneumaticcraft.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class GuiCheckBox extends Gui implements IGuiWidget {
    public boolean checked, enabled = true, visible = true;
    public int x, y, color;
    private final int id;
    public String text;
    private List<String> tooltip = new ArrayList<>();
    private IWidgetListener listener;

    private static final int CHECKBOX_WIDTH = 10;
    private static final int CHECKBOX_HEIGHT = 10;

    public GuiCheckBox(int id, int x, int y, int color, String text) {
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
        if (this.visible) {
            drawRect(this.x, this.y, this.x + CHECKBOX_WIDTH, this.y + CHECKBOX_HEIGHT, this.enabled ? 0xFFA0A0A0 : 0xFF999999);
            drawRect(this.x + 1, this.y + 1, this.x + CHECKBOX_WIDTH - 1, this.y + CHECKBOX_HEIGHT - 1, this.enabled ? 0xFF202020 : 0xFFAAAAAA);
            if (this.checked) {
                GlStateManager.disableTexture2D();
                if (this.enabled) {
                    GlStateManager.color(0.5f, 1, 0.5f, 1);
                } else {
                    GlStateManager.color(0.8f, 0.8f, 0.8f, 1);
                }
                BufferBuilder wr = Tessellator.getInstance().getBuffer();
                GlStateManager.glLineWidth(2);
                wr.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION);
                wr.pos(this.x + 2, this.y + 5, this.zLevel).endVertex();
                wr.pos(this.x + 5, this.y + 7, this.zLevel).endVertex();
                wr.pos(this.x + 8, this.y + 3, this.zLevel).endVertex();
                Tessellator.getInstance().draw();
                GlStateManager.enableTexture2D();
                GlStateManager.color(0.25f, 0.25f, 0.25f, 1);
            }
            Minecraft.getMinecraft().fontRenderer.drawString(I18n.format(this.text), this.x + 3 + CHECKBOX_WIDTH, this.y + CHECKBOX_HEIGHT / 2 - Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT / 2, this.enabled ? this.color : 0xFF888888);
        }
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(this.x, this.y, CHECKBOX_WIDTH + Minecraft.getMinecraft().fontRenderer.getStringWidth(I18n.format(this.text)), CHECKBOX_HEIGHT);
    }

    @Override
    public void onMouseClicked(int mouseX, int mouseY, int button) {
        if (this.enabled) {
            this.checked = !this.checked;
            if (this.listener != null) this.listener.actionPerformed(this);
        }
    }

    @Override
    public void onMouseClickedOutsideBounds(int mouseX, int mouseY, int button) {

    }

    public GuiCheckBox setTooltip(String tooltip) {
        this.tooltip.clear();
        if (tooltip != null && !tooltip.equals("")) {
            this.tooltip.add(tooltip);
        }
        return this;
    }

    public GuiCheckBox setTooltip(List<String> tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    @Override
    public void addTooltip(int mouseX, int mouseY, List<String> curTooltip, boolean shiftPressed) {
        if (this.visible) curTooltip.addAll(this.tooltip);
    }

    public String getTooltip() {
        return this.tooltip.size() > 0 ? this.tooltip.get(0) : "";
    }

    @Override
    public boolean onKey(char key, int keyCode) {
        return false;
    }

    @Override
    public void setListener(IWidgetListener gui) {
        this.listener = gui;
    }

    public GuiCheckBox setChecked(boolean checked) {
        this.checked = checked;
        return this;
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
