package me.desht.pneumaticcraft.client.gui;

import me.desht.pneumaticcraft.client.gui.widget.IGuiWidget;
import me.desht.pneumaticcraft.client.gui.widget.IWidgetListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiButtonExt;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Extension of GuiButton that allows a invisible clickable field. It can be added in Gui's like buttons (with the buttonList).
 */

public class GuiButtonSpecial extends GuiButtonExt implements IGuiWidget {

    public enum IconPosition {MIDDLE, LEFT, RIGHT}

    private ItemStack[] renderedStacks;
    private ResourceLocation resLoc;
    private final List<String> tooltipText = new ArrayList<>();
    private final RenderItem itemRenderer = Minecraft.getMinecraft().getRenderItem();
    private int invisibleHoverColor;
    private boolean thisVisible = true;
    private IWidgetListener listener;

    private IconPosition iconPosition = IconPosition.MIDDLE;

    public GuiButtonSpecial(int buttonID, int startX, int startY, int xSize, int ySize, String buttonText) {
        super(buttonID, startX, startY, xSize, ySize, buttonText);
    }

    public void setVisible(boolean visible) {
        this.thisVisible = visible;
    }

    public void setInvisibleHoverColor(int color) {
        this.invisibleHoverColor = color;
    }

    public void setIconPosition(IconPosition iconPosition) {
        this.iconPosition = iconPosition;
    }

    public GuiButtonSpecial setRenderStacks(ItemStack... renderedStacks) {
        this.renderedStacks = renderedStacks;
        return this;
    }

    public void setRenderedIcon(ResourceLocation resLoc) {
        this.resLoc = resLoc;
    }

    public GuiButtonSpecial setTooltipText(List<String> tooltip) {
        this.tooltipText.clear();
        this.tooltipText.addAll(tooltip);
        return this;
    }

    public GuiButtonSpecial setTooltipText(String tooltip) {
        this.tooltipText.clear();
        if (tooltip != null && !tooltip.equals("")) {
            this.tooltipText.add(tooltip);
        }
        return this;
    }

    public void getTooltip(List<String> curTooltip) {
        if (this.tooltipText != null) {
            curTooltip.addAll(this.tooltipText);
        }
    }

    public String getTooltip() {
        return this.tooltipText.size() > 0 ? this.tooltipText.get(0) : "";
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    @Override
    public void drawButton(Minecraft mc, int x, int y, float partialTicks) {
        if (this.thisVisible) super.drawButton(mc, x, y, partialTicks);

        if (this.visible) {
            if (this.renderedStacks != null) {
                int startX = this.getIconX();
                GlStateManager.enableRescaleNormal();
                RenderHelper.enableGUIStandardItemLighting();
                for (int i = 0; i < this.renderedStacks.length; i++) {
                    this.itemRenderer.renderItemAndEffectIntoGUI(this.renderedStacks[i], startX + i * 18, this.y + 2);
                }
                RenderHelper.disableStandardItemLighting();
                GlStateManager.disableRescaleNormal();
            }
            if (this.resLoc != null) {
                mc.getTextureManager().bindTexture(this.resLoc);
                drawModalRectWithCustomSizedTexture(this.x + this.width / 2 - 8, this.y + 2, 0, 0, 16, 16, 16, 16);
            }
            if (this.enabled && !this.thisVisible && x >= this.x && y >= this.y && x < this.x + this.width && y < this.y + this.height) {
                Gui.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, this.invisibleHoverColor);
            }
        }
    }

    private int getIconX() {
        switch (this.iconPosition) {
            case LEFT:
                return this.x - 1 - 18 * this.renderedStacks.length;
            case RIGHT:
                return this.x + this.width + 1;
            case MIDDLE:
            default:
                return this.x + this.width / 2 - this.renderedStacks.length * 9 + 1;
        }
    }

    @Override
    public void setListener(IWidgetListener gui) {
        this.listener = gui;
    }

    @Override
    public int getID() {
        return this.id;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTick) {
        this.drawButton(Minecraft.getMinecraft(), mouseX, mouseY, partialTick);
    }

    @Override
    public void onMouseClicked(int mouseX, int mouseY, int button) {
        if (this.mousePressed(Minecraft.getMinecraft(), mouseX, mouseY)) {
            this.playPressSound(Minecraft.getMinecraft().getSoundHandler());
            this.listener.actionPerformed(this);
        }
    }

    @Override
    public void onMouseClickedOutsideBounds(int mouseX, int mouseY, int button) {

    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(this.x, this.y, this.width, this.height);
    }

    @Override
    public void addTooltip(int mouseX, int mouseY, List<String> curTooltip, boolean shiftPressed) {
        if (this.visible) curTooltip.addAll(this.tooltipText);
    }

    @Override
    public boolean onKey(char key, int keyCode) {
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
