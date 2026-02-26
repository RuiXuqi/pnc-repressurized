package me.desht.pneumaticcraft.client.gui;

import me.desht.pneumaticcraft.client.gui.widget.WidgetVerticalScrollbar;
import me.desht.pneumaticcraft.common.progwidgets.IJump;
import me.desht.pneumaticcraft.common.progwidgets.ILabel;
import me.desht.pneumaticcraft.common.progwidgets.IProgWidget;
import me.desht.pneumaticcraft.common.thirdparty.ThirdPartyManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.lang3.text.WordUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GuiUnitProgrammer extends GuiScreen {
    private final FontRenderer fontRenderer;
    private final List<IProgWidget> progWidgets;
    private final int guiLeft, guiTop;
    private final int startX, startY, areaWidth, areaHeight;
    private int translatedX, translatedY;
    private int lastMouseX, lastMouseY;
    private int lastZoom;
    private boolean wasClicking;

    private final WidgetVerticalScrollbar scaleScroll;
    private static final float SCALE_PER_STEP = 0.2F;

    public GuiUnitProgrammer(List<IProgWidget> progWidgets, FontRenderer fontRenderer, int guiLeft, int guiTop,
                             int width, int height, int xSize, int startX, int startY, int areaWidth, int areaHeight, int translatedX,
                             int translatedY, int lastZoom) {
        this.fontRenderer = fontRenderer;
        this.progWidgets = progWidgets;
        this.guiLeft = guiLeft;
        this.guiTop = guiTop;
        this.setWorldAndResolution(Minecraft.getMinecraft(), width, height);
        this.startX = startX;
        this.startY = startY;
        this.areaWidth = areaWidth;
        this.areaHeight = areaHeight;
        this.translatedX = translatedX;
        this.translatedY = translatedY;
        this.lastZoom = lastZoom;

        this.scaleScroll = new WidgetVerticalScrollbar(guiLeft + areaWidth + 8, guiTop + 40, areaHeight - 25).setStates(9).setCurrentState(lastZoom).setListening(true);
    }

    public WidgetVerticalScrollbar getScrollBar() {
        return this.scaleScroll;
    }

    public int getLastZoom() {
        return this.lastZoom;
    }

    public int getTranslatedX() {
        return this.translatedX;
    }

    public int getTranslatedY() {
        return this.translatedY;
    }

    public void renderForeground(int x, int y, IProgWidget tooltipExcludingWidget) {
        IProgWidget widget = this.getHoveredWidget(x, y);
        if (widget != null && widget != tooltipExcludingWidget) {
            List<String> tooltip = new ArrayList<>();
            widget.getTooltip(tooltip);

            List<String> errors = new ArrayList<>();
            widget.addErrors(errors, this.progWidgets);
            if (errors.size() > 0) {
                tooltip.add(TextFormatting.RED + I18n.format("gui.programmer.errors"));
                for (String s : errors) {
                    String msg = I18n.hasKey(s) ? I18n.format(s) : s;
                    String[] lines = WordUtils.wrap("- " + msg, 35).split(System.getProperty("line.separator"));
                    for (String line : lines) {
                        tooltip.add(TextFormatting.RED + /*"   " +*/ line);
                    }
                }
            }

            List<String> warnings = new ArrayList<>();
            widget.addWarnings(warnings, this.progWidgets);
            if (warnings.size() > 0) {
                tooltip.add(TextFormatting.YELLOW + I18n.format("gui.programmer.warnings"));
                for (String s : warnings) {
                    String msg = I18n.hasKey(s) ? I18n.format(s) : s;
                    String[] lines = WordUtils.wrap("- " + msg, 35).split(System.getProperty("line.separator"));
                    for (String line : lines) {
                        tooltip.add(TextFormatting.YELLOW + "   " + line);
                    }
                }
            }
            this.addAdditionalInfoToTooltip(widget, tooltip);

            if (tooltip.size() > 0)
                this.drawHoveringText(tooltip, x - this.guiLeft, y - this.guiTop, this.fontRenderer);
        }

    }

    public IProgWidget getHoveredWidget(int x, int y) {
        float scale = this.getScale();
        for (IProgWidget widget : this.progWidgets) {
            if (!this.isOutsideProgrammingArea(widget)) {
                if ((x - this.translatedX) / scale - this.guiLeft >= widget.getX() && (y - this.translatedY) / scale - this.guiTop >= widget.getY() && (x - this.translatedX) / scale - this.guiLeft <= widget.getX() + widget.getWidth() / 2 && (y - this.translatedY) / scale - this.guiTop <= widget.getY() + widget.getHeight() / 2) {
                    return widget;
                }
            }
        }
        return null;
    }

    protected void addAdditionalInfoToTooltip(IProgWidget widget, List<String> tooltip) {
        if (widget.getOptionWindow(null) != null) {
            tooltip.add(TextFormatting.GOLD + "Right click for options");
        }
        ThirdPartyManager.instance().docsProvider.addTooltip(tooltip, false);
    }

    public void render(int x, int y, boolean showFlow, boolean showInfo, boolean translate) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        if (this.scaleScroll.getState() != this.lastZoom) {
            float shift = SCALE_PER_STEP * (this.scaleScroll.getState() - this.lastZoom);
            float prevScale = 2.0F - this.lastZoom * SCALE_PER_STEP;
            this.translatedX += shift * (x - this.translatedX) / prevScale;
            this.translatedY += shift * (y - this.translatedY) / prevScale;
        }
        this.lastZoom = this.scaleScroll.getState();

        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        GL11.glScissor((this.guiLeft + this.startX) * sr.getScaleFactor(), (sr.getScaledHeight() - this.areaHeight - (this.guiTop + this.startY)) * sr.getScaleFactor(), this.areaWidth * sr.getScaleFactor(), this.areaHeight * sr.getScaleFactor());
        GL11.glEnable(GL11.GL_SCISSOR_TEST);

        GlStateManager.pushMatrix();
        GlStateManager.translate(this.translatedX, this.translatedY, 0);
        float scale = this.getScale();
        GlStateManager.scale(scale, scale, 1);

        if (showFlow) this.showFlow();

        GlStateManager.enableTexture2D();
        for (IProgWidget widget : this.progWidgets) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(widget.getX() + this.guiLeft, widget.getY() + this.guiTop, 0);
            GlStateManager.scale(0.5, 0.5, 1);
            widget.render();
            GlStateManager.popMatrix();
        }

        for (IProgWidget widget : this.progWidgets) {
            List<String> errors = new ArrayList<>();
            widget.addErrors(errors, this.progWidgets);
            if (errors.size() > 0) {
                this.drawBorder(widget, 0xFFFF0000);
            } else {
                List<String> warnings = new ArrayList<>();
                widget.addWarnings(warnings, this.progWidgets);
                if (warnings.size() > 0) {
                    this.drawBorder(widget, 0xFFFFFF00);
                }
            }
        }

        this.renderAdditionally();

        GlStateManager.color(1, 1, 1, 1);

        if (showInfo) {
            for (IProgWidget widget : this.progWidgets) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(widget.getX() + this.guiLeft, widget.getY() + this.guiTop, 0);
                GlStateManager.scale(0.5, 0.5, 1);
                widget.renderExtraInfo();
                GlStateManager.popMatrix();
            }
        }

        GlStateManager.popMatrix();

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        boolean isLeftClicking = Mouse.isButtonDown(0);
        if (translate && isLeftClicking && this.wasClicking && !this.scaleScroll.isDragging() && new Rectangle(this.guiLeft + this.startX, this.guiTop + this.startY, this.areaWidth, this.areaHeight).contains(x, y)) {
            this.translatedX += x - this.lastMouseX;
            this.translatedY += y - this.lastMouseY;
        }

        this.wasClicking = isLeftClicking;
        this.lastMouseX = x;
        this.lastMouseY = y;
    }

    protected void renderAdditionally() {

    }

    protected void drawBorder(IProgWidget widget, int color) {
        this.drawBorder(widget, color, 0);
    }

    protected void drawBorder(IProgWidget widget, int color, int inset) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(widget.getX() + this.guiLeft, widget.getY() + this.guiTop, 0);
        GlStateManager.scale(0.5, 0.5, 1);
        this.drawVerticalLine(inset, inset, widget.getHeight() - inset, color);
        this.drawVerticalLine(widget.getWidth() - inset, inset, widget.getHeight() - inset, color);
        this.drawHorizontalLine(widget.getWidth() - inset, inset, inset, color);
        this.drawHorizontalLine(widget.getWidth() - inset, inset, widget.getHeight() - inset, color);
        GlStateManager.popMatrix();
    }

    private void showFlow() {
        GlStateManager.glLineWidth(1);
        GlStateManager.disableTexture2D();
        GlStateManager.glBegin(GL11.GL_LINES);

        for (IProgWidget widget : this.progWidgets) {
            if (widget instanceof IJump) {
                List<String> jumpLocations = ((IJump) widget).getPossibleJumpLocations();
                if (jumpLocations != null) {
                    for (String jumpLocation : jumpLocations) {
                        if (jumpLocation != null) {
                            for (IProgWidget w : this.progWidgets) {
                                if (w instanceof ILabel) {
                                    String label = ((ILabel) w).getLabel();
                                    if (jumpLocation.equals(label)) {
                                        int x1 = widget.getX() + widget.getWidth() / 4;
                                        int y1 = widget.getY() + widget.getHeight() / 4;
                                        int x2 = w.getX() + w.getWidth() / 4;
                                        int y2 = w.getY() + w.getHeight() / 4;
                                        float midX = (x2 + x1) / 2F;
                                        float midY = (y2 + y1) / 2F;
                                        GlStateManager.glVertex3f(this.guiLeft + x1, this.guiTop + y1, this.zLevel);
                                        GlStateManager.glVertex3f(this.guiLeft + x2, this.guiTop + y2, this.zLevel);
                                        Vec3d arrowVec = new Vec3d(x1 - x2, y1 - y2, 0).normalize();
                                        float arrowAngle = (float) Math.toRadians(30);
                                        float arrowSize = 5;
                                        arrowVec = new Vec3d(arrowVec.x * arrowSize, 0, arrowVec.y * arrowSize);
                                        arrowVec = arrowVec.rotateYaw(arrowAngle);
                                        GlStateManager.glVertex3f(this.guiLeft + midX, this.guiTop + midY, this.zLevel);
                                        GlStateManager.glVertex3f(this.guiLeft + midX + (float) arrowVec.x, this.guiTop + midY + (float) arrowVec.z, this.zLevel);
                                        arrowVec = arrowVec.rotateYaw(-2 * arrowAngle);
                                        GlStateManager.glVertex3f(this.guiLeft + midX, this.guiTop + midY, this.zLevel);
                                        GlStateManager.glVertex3f(this.guiLeft + midX + (float) arrowVec.x, this.guiTop + midY + (float) arrowVec.z, this.zLevel);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        GlStateManager.glEnd();

        GlStateManager.enableTexture2D();
    }

    public float getScale() {
        return 2.0F - this.scaleScroll.getState() * SCALE_PER_STEP;
    }

    public boolean isOutsideProgrammingArea(IProgWidget widget) {
        float scale = this.getScale();
        int x = (int) ((widget.getX() + this.guiLeft) * scale);
        int y = (int) ((widget.getY() + this.guiTop) * scale);
        x += this.translatedX - this.guiLeft;
        y += this.translatedY - this.guiTop;

        return x < this.startX || x + widget.getWidth() * scale / 2 > this.startX + this.areaWidth || y < this.startY || y + widget.getHeight() * scale / 2 > this.startY + this.areaHeight;
    }

    public void gotoPiece(IProgWidget widget) {
        if (widget != null) {
            this.scaleScroll.currentScroll = 0;
            this.lastZoom = 0;
            this.translatedX = -widget.getX() * 2 + this.areaWidth / 2 - this.guiLeft;
            this.translatedY = -widget.getY() * 2 + this.areaHeight / 2 - this.guiTop;
        }
    }
}
