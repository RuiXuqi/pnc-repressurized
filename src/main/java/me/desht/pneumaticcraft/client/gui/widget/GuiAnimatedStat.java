package me.desht.pneumaticcraft.client.gui.widget;

import com.google.common.base.Strings;
import me.desht.pneumaticcraft.PneumaticCraftRepressurized;
import me.desht.pneumaticcraft.api.client.IGuiAnimatedStat;
import me.desht.pneumaticcraft.client.gui.GuiPneumaticContainerBase;
import me.desht.pneumaticcraft.common.config.ArmorHUDLayout;
import me.desht.pneumaticcraft.common.config.ConfigHandler;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.GuiConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.FMLClientHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.RL;

public class GuiAnimatedStat implements IGuiAnimatedStat, IGuiWidget, IWidgetListener {
    private static final int ANIMATED_STAT_SPEED = 30;
    private static final int WIDGET_SCROLLBAR_ID = -1000;

    private IGuiAnimatedStat affectingStat;

    private StatIcon statIcon;

    private final GuiScreen gui;
    private final List<String> textList = new ArrayList<>();
    private final List<IGuiWidget> widgets = new ArrayList<>();
    private int baseX;
    private int baseY;
    private int affectedY;
    private int width;
    private int height;

    private int oldBaseX;
    private int oldAffectedY;
    private int oldWidth;
    private int oldHeight;
    private boolean isClicked = false;
    private int minWidth = 17;
    private int minHeight = 17;
    private int backGroundColor;
    private Color bgColorHi, bgColorLo;
    private String title;
    private boolean leftSided; // determines if the stat is going to expand to the left or right.
    private boolean doneExpanding;
    private float textSize;
    private float textScale = 1F;
    private IWidgetListener listener;
    private int curScroll;
    private static final int MAX_LINES = 12;
    private int lastMouseX, lastMouseY;
    private int lineSpacing = 10;
    private int widgetOffsetLeft = 0;
    private int widgetOffsetRight = 0;
    private boolean bevel = false;
    private Pair<Integer, Integer> forcedDimensions = null;

    public GuiAnimatedStat(GuiScreen gui, String title, int xPos, int yPos, int backGroundColor,
                           IGuiAnimatedStat affectingStat, boolean leftSided) {
        this.gui = gui;
        this.baseX = xPos;
        this.baseY = yPos;
        this.affectingStat = affectingStat;
        this.width = this.minWidth;
        this.height = this.minHeight;
        this.backGroundColor = backGroundColor;
        this.calculateColorHighlights(this.backGroundColor);
        this.setTitle(title);
        this.statIcon = StatIcon.NONE;
        this.leftSided = leftSided;
        this.textSize = 1;

        this.affectedY = this.baseY;
        if (affectingStat != null) {
            this.affectedY += affectingStat.getAffectedY() + affectingStat.getHeight();
        }
    }

    public GuiAnimatedStat(GuiScreen gui, int backgroundColor) {
        this(gui, "", 0, 0, backgroundColor, null, false);
    }

    public GuiAnimatedStat(GuiScreen gui, int backgroundColor, ItemStack icon) {
        this(gui, backgroundColor);
        this.statIcon = StatIcon.of(icon);
    }

    public GuiAnimatedStat(GuiScreen gui, int backgroundColor, String texture) {
        this(gui, backgroundColor);
        this.statIcon = StatIcon.of(RL(texture));
    }

    public GuiAnimatedStat(GuiScreen gui, String title, StatIcon icon, int xPos, int yPos, int backGroundColor,
                           IGuiAnimatedStat affectingStat, boolean leftSided) {
        this(gui, title, xPos, yPos, backGroundColor, affectingStat, leftSided);
        this.statIcon = icon;
    }

    public GuiAnimatedStat(GuiScreen gui, String title, StatIcon icon, int backGroundColor,
                           IGuiAnimatedStat affectingStat, ArmorHUDLayout.LayoutItem layout) {
        this(gui, title, 0, 0, backGroundColor, affectingStat, layout.isLeftSided());
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        int x = layout.getX() == -1 ? sr.getScaledWidth() - 2 : (int) (sr.getScaledWidth() * layout.getX());
        this.setBaseX(x);
        this.setBaseY((int) (sr.getScaledHeight() * layout.getY()));
        this.statIcon = icon;
    }

    @Override
    public void setParentStat(IGuiAnimatedStat stat) {
        this.affectingStat = stat;
    }

    public void addWidget(IGuiWidget widget) {
        this.widgets.add(widget);
        widget.setListener(this);
    }

    public void removeWidget(IGuiWidget widget) {
        this.widgets.remove(widget);
    }

    public void setWidgetOffsets(int left, int right) {
        this.widgetOffsetLeft = left;
        this.widgetOffsetRight = right;
    }

    @Override
    public Rectangle getButtonScaledRectangle(int origX, int origY, int width, int height) {
        int scaledX = (int) (origX * this.textSize);
        int scaledY = (int) (origY * this.textSize);
        return new Rectangle(scaledX, scaledY, (int) (width * this.textSize), (int) (height * this.textSize));
    }

    @Override
    public void scaleTextSize(float scale) {
        this.textSize *= scale;
        this.textScale = scale;

        for (IGuiWidget widget : this.widgets) {
            if (widget.getID() == WIDGET_SCROLLBAR_ID) {
                this.widgets.remove(widget);
                break;
            }
        }
        this.onTextChange();
    }

    @Override
    public boolean isLeftSided() {
        return this.leftSided;
    }

    @Override
    public void setLeftSided(boolean leftSided) {
        this.leftSided = leftSided;
    }

    @Override
    public IGuiAnimatedStat setText(List<String> text) {
        this.textList.clear();
        for (String line : text) {
            this.textList.addAll(PneumaticCraftUtils.convertStringIntoList(I18n.format(line), (int) (GuiConstants.MAX_CHAR_PER_LINE_LEFT / this.textScale)));
        }
        this.onTextChange();
        return this;
    }

    @Override
    public IGuiAnimatedStat setText(String text) {
        this.textList.clear();
        this.textList.addAll(PneumaticCraftUtils.convertStringIntoList(I18n.format(text), (int) (GuiConstants.MAX_CHAR_PER_LINE_LEFT / this.textScale)));
        this.onTextChange();
        return this;
    }

    @Override
    public void setTextWithoutCuttingString(List<String> text) {
        this.textList.clear();
        this.textList.addAll(text);
        this.onTextChange();
    }

    @Override
    public void appendText(List<String> text) {
        for (String line : text) {
            this.textList.addAll(PneumaticCraftUtils.convertStringIntoList(I18n.format(line), (int) (GuiConstants.MAX_CHAR_PER_LINE_LEFT / this.textScale)));
        }
        this.onTextChange();
    }

    @Override
    public void addPadding(int nRows, int nCols) {
        String s = Strings.repeat(" ", nCols);
        this.setTextWithoutCuttingString(IntStream.range(0, nRows).mapToObj(i -> s).collect(Collectors.toList()));
    }

    @Override
    public void addPadding(List<String> text, int nRows, int nCols) {
        String s = Strings.repeat(" ", nCols);
        List<String> l = IntStream.range(0, nRows).mapToObj(i -> s).collect(Collectors.toList());
        for (int i = 0; i < text.size() && i < nRows; i++) {
            l.set(i, text.get(i));
        }
        this.setTextWithoutCuttingString(l);
    }

    @Override
    public void setBackGroundColor(int backGroundColor) {
        if (backGroundColor != this.backGroundColor) {
            this.backGroundColor = backGroundColor;
            this.calculateColorHighlights(backGroundColor);
        }
    }

    @Override
    public int getBackgroundColor() {
        return this.backGroundColor;
    }

    private void calculateColorHighlights(int color) {
        if (ConfigHandler.client.guiBevel) {
            float fgR = (float) (color >> 16 & 255) / 255.0F;
            float fgG = (float) (color >> 8 & 255) / 255.0F;
            float fgB = (float) (color & 255) / 255.0F;
            float fgA = (float) (color >> 24 & 255) / 255.0F;
            Color c = new Color(fgR, fgG, fgB, fgA);
            if (this.bevel) {
                this.bgColorHi = c.brighter();
                this.bgColorLo = c.darker();
            } else {
                this.bgColorHi = c.darker().darker();
                this.bgColorLo = this.bgColorHi;
            }
        } else {
            this.bgColorLo = this.bgColorHi = Color.BLACK;
        }
    }

    @Override
    public void setBeveled(boolean bevel) {
        this.bevel = bevel;
        this.calculateColorHighlights(this.backGroundColor);
    }

    private void onTextChange() {
        // add/remove a scrollbar, as necessary
        if (this.textList.size() > MAX_LINES) {
            for (IGuiWidget widget : this.widgets) {
                if (widget.getID() == WIDGET_SCROLLBAR_ID) return;
            }
            this.curScroll = 0;
            this.addWidget(new WidgetVerticalScrollbar(WIDGET_SCROLLBAR_ID, this.leftSided ? -16 : 2, 20, (int) ((MAX_LINES * this.lineSpacing - 20) * this.textSize)).setStates(this.textList.size() - MAX_LINES));
        } else {
            Iterator<IGuiWidget> iterator = this.widgets.iterator();
            while (iterator.hasNext()) {
                IGuiWidget widget = iterator.next();
                if (widget.getID() == WIDGET_SCROLLBAR_ID) {
                    iterator.remove();
                    this.curScroll = 0;
                }
            }
        }
    }

    @Override
    public void setMinDimensionsAndReset(int minWidth, int minHeight) {
        this.minWidth = minWidth;
        this.minHeight = minHeight;
        this.width = minWidth;
        this.height = minHeight;
    }

    @Override
    public void setForcedDimensions(int width, int height) {
        //noinspection SuspiciousNameCombination
        this.forcedDimensions = width > 0 && height > 0 ? Pair.of(width, height) : null;
    }

    @Override
    public void update() {
        this.oldBaseX = this.baseX;
        this.oldAffectedY = this.affectedY;
        this.oldWidth = this.width;
        this.oldHeight = this.height;

        this.doneExpanding = true;
        if (this.isClicked) {
            Pair<Integer, Integer> maxSize = this.calculateMaxSize();
            int maxWidth = maxSize.getLeft(), maxHeight = maxSize.getRight();

            // expand the box
            this.width = Math.min(maxWidth, this.width + ANIMATED_STAT_SPEED);
            this.height = Math.min(maxHeight, this.height + ANIMATED_STAT_SPEED);
            this.doneExpanding = this.width == maxWidth && this.height == maxHeight;

            Pair<Integer, Integer> size = PneumaticCraftRepressurized.proxy.getScaledScreenSize();
            if (this.isLeftSided()) {
                if (this.baseX >= size.getLeft()) this.baseX = size.getLeft();
            } else {
                if (this.baseX < 0) this.baseX = 1;
            }
            if (this.baseY + this.height >= size.getRight()) {
                this.baseY = size.getRight() - this.height - 1;
            }

            if (this.doneExpanding) {
                for (IGuiWidget widget : this.widgets) {
                    if (widget.getID() == WIDGET_SCROLLBAR_ID) {
                        this.curScroll = ((WidgetVerticalScrollbar) widget).getState();
                        break;
                    }
                }
            }
        } else {
            // contract the box
            this.width = Math.max(this.minWidth, this.width - ANIMATED_STAT_SPEED);
            this.height = Math.max(this.minHeight, this.height - ANIMATED_STAT_SPEED);
            this.doneExpanding = false;
        }

        this.affectedY = this.baseY;
        if (this.affectingStat != null) {
            this.affectedY += this.affectingStat.getAffectedY() + this.affectingStat.getHeight();
        }
    }

    private Pair<Integer, Integer> calculateMaxSize() {
        if (this.forcedDimensions != null) return this.forcedDimensions;

        FontRenderer fontRenderer = FMLClientHandler.instance().getClient().fontRenderer;

        // scale the box down if necessary to avoid extending beyond screen edge
        // (should only be an issue for very low scaled X resolution)
        int availableWidth;
        if (this.gui instanceof GuiContainer) {
            GuiContainer gc = (GuiContainer) this.gui;
            availableWidth = this.leftSided ? gc.getGuiLeft() : gc.width - (gc.getGuiLeft() + gc.getXSize());
        } else {
            availableWidth = new ScaledResolution(Minecraft.getMinecraft()).getScaledWidth();
        }

        // calculate the width and height needed for the box to fit the strings.
        int maxWidth = fontRenderer.getStringWidth(this.title);
        for (String line : this.textList) {
            maxWidth = Math.max(maxWidth, fontRenderer.getStringWidth(line));
        }
        maxWidth += 20;  // to allow space for the scrollbar, where necessary

        int maxHeight = this.title.isEmpty() ? 6 : 16;
        if (!this.textList.isEmpty()) {
            maxHeight += Math.min(MAX_LINES, this.textList.size()) * this.lineSpacing;
        }
        maxHeight -= (this.lineSpacing - fontRenderer.FONT_HEIGHT);

        float lastTextSize = this.textSize;
        if (maxWidth > availableWidth - 3) {
            this.textSize = (availableWidth - 3f) / maxWidth;
            maxWidth = (int) (maxWidth * this.textSize);
            maxHeight = (int) (maxHeight * this.textSize);
        } else {
            this.textSize = 1.0f;
        }
        if (lastTextSize != this.textSize) {
            float newTextSize = this.textSize;
            this.textSize = 1.0f;
            this.scaleTextSize(newTextSize);
        }

        //noinspection SuspiciousNameCombination
        return Pair.of(maxWidth, maxHeight);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        float zLevel = 0;
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        int renderBaseX = (int) (this.oldBaseX + (this.baseX - this.oldBaseX) * partialTicks);
        int renderAffectedY = (int) (this.oldAffectedY + (this.affectedY - this.oldAffectedY) * partialTicks);
        int renderWidth = (int) (this.oldWidth + (this.width - this.oldWidth) * partialTicks);
        int renderHeight = (int) (this.oldHeight + (this.height - this.oldHeight) * partialTicks);

        if (this.leftSided) renderWidth *= -1;
        Gui.drawRect(renderBaseX, renderAffectedY, renderBaseX + renderWidth, renderAffectedY + renderHeight, this.backGroundColor);
        GlStateManager.disableTexture2D();
        GlStateManager.glLineWidth(3.0F);
        GlStateManager.color(0, 0, 0, 1);
        BufferBuilder wr = Tessellator.getInstance().getBuffer();
        wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        float[] c1 = this.leftSided ? this.bgColorLo.getComponents(null) : this.bgColorHi.getComponents(null);
        float[] c2 = this.bgColorHi.getComponents(null);
        float[] c3 = this.leftSided ? this.bgColorHi.getComponents(null) : this.bgColorLo.getComponents(null);
        float[] c4 = this.bgColorLo.getComponents(null);
        wr.pos(renderBaseX, renderAffectedY, zLevel).color(c1[0], c1[1], c1[2], c1[3]).endVertex();
        wr.pos(renderBaseX + renderWidth, renderAffectedY, zLevel).color(c2[0], c2[1], c2[2], c2[3]).endVertex();
        wr.pos(renderBaseX + renderWidth, renderAffectedY + renderHeight, zLevel).color(c3[0], c3[1], c3[2], c3[3]).endVertex();
        wr.pos(renderBaseX, renderAffectedY + renderHeight, zLevel).color(c4[0], c4[1], c4[2], c4[3]).endVertex();
        Tessellator.getInstance().draw();
        GlStateManager.enableTexture2D();
        if (this.leftSided) renderWidth *= -1;

        // if done expanding, draw the information
        int titleYoffset = this.title.isEmpty() ? 3 : 12;
        if (this.doneExpanding) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(renderBaseX + (this.leftSided ? -renderWidth : 16), renderAffectedY, 0);
            GlStateManager.scale(this.textSize, this.textSize, this.textSize);
            GlStateManager.translate(-renderBaseX - (this.leftSided ? -renderWidth : 16), -renderAffectedY, 0);
            if (!this.title.isEmpty()) {
                fontRenderer.drawStringWithShadow(this.title, renderBaseX + (this.leftSided ? -renderWidth + 2 : 18), renderAffectedY + 2, 0xFFFF00);
            }
            for (int i = this.curScroll; i < this.textList.size() && i < this.curScroll + MAX_LINES; i++) {
                if (this.textList.get(i).contains("\u00a70") || this.textList.get(i).contains(TextFormatting.DARK_RED.toString())) {
                    fontRenderer.drawString(this.textList.get(i), renderBaseX + (this.leftSided ? -renderWidth + 2 : 18), renderAffectedY + (i - this.curScroll) * this.lineSpacing + titleYoffset, 0xFFFFFF);
                } else {
                    fontRenderer.drawStringWithShadow(this.textList.get(i), renderBaseX + (this.leftSided ? -renderWidth + 2 : 18), renderAffectedY + (i - this.curScroll) * this.lineSpacing + titleYoffset, 0xFFFFFF);
                }
            }
            GlStateManager.popMatrix();

            GlStateManager.pushMatrix();
            GlStateManager.translate(renderBaseX + (this.leftSided ? this.widgetOffsetLeft : this.widgetOffsetRight), renderAffectedY + (titleYoffset - 10), 0);
            GlStateManager.enableTexture2D();
            for (IGuiWidget widget : this.widgets)
                widget.render(mouseX - renderBaseX, mouseY - renderAffectedY, partialTicks);
            GlStateManager.popMatrix();
        }
        if (renderHeight > 16 && renderWidth > 16 && this.statIcon != null) {
            this.statIcon.render(this.gui, renderBaseX, renderAffectedY, this.leftSided);
        }
    }

    /*
     * button: 0 = left 1 = right 2 = middle
     */
    @Override
    public void onMouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0) {
            this.isClicked = !this.isClicked;
            this.listener.actionPerformed(this);
        }
        mouseX -= this.baseX;
        mouseY -= this.affectedY;
        for (IGuiWidget widget : this.widgets) {
            if (widget.getBounds().contains(mouseX, mouseY)) {
                widget.onMouseClicked(mouseX, mouseY, button);
                this.isClicked = true;
            } else {
                widget.onMouseClickedOutsideBounds(mouseX, mouseY, button);
            }
        }
    }

    @Override
    public void onMouseClickedOutsideBounds(int mouseX, int mouseY, int button) {

    }

    @Override
    public void closeWindow() {
        this.isClicked = false;
    }

    @Override
    public void openWindow() {
        this.isClicked = true;
    }

    @Override
    public boolean isClicked() {
        return this.isClicked;
    }

    @Override
    public int getAffectedY() {
        return this.affectedY;
    }

    @Override
    public int getBaseX() {
        return this.baseX;
    }

    @Override
    public int getBaseY() {
        return this.baseY;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public void setBaseY(int y) {
        this.baseY = y;
    }

    @Override
    public void setTitle(String title) {
        this.title = I18n.format(title);
    }

    @Override
    public boolean isDoneExpanding() {
        return this.doneExpanding;
    }

    @Override
    public void setBaseX(int x) {
        this.baseX = x;
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(this.baseX - (this.leftSided ? this.width : 0), this.affectedY, this.width, this.height);
    }

    @Override
    public void setListener(IWidgetListener gui) {
        this.listener = gui;
    }

    @Override
    public int getID() {
        return -1;
    }

    @Override
    public void actionPerformed(IGuiWidget widget) {
        this.isClicked = !this.isClicked;
        this.listener.actionPerformed(widget);
    }

    @Override
    public void onKeyTyped(IGuiWidget widget) {
        this.listener.onKeyTyped(widget);
    }

    @Override
    public void addTooltip(int mouseX, int mouseY, List<String> curTooltip, boolean shiftPressed) {

        if (this.mouseIsHoveringOverIcon(mouseX, mouseY)) {
            curTooltip.add(this.title);
        }

        for (IGuiWidget widget : this.widgets)
            if (this.isMouseOverWidget(widget, mouseX, mouseY))
                widget.addTooltip(mouseX, mouseY, curTooltip, shiftPressed);
    }

    private boolean mouseIsHoveringOverIcon(int x, int y) {
        if (this.leftSided) {
            return x <= this.baseX && x >= this.baseX - 16 && y >= this.affectedY && y <= this.affectedY + 16;
        } else {
            return x >= this.baseX && x <= this.baseX + 16 && y >= this.affectedY && y <= this.affectedY + 16;
        }
    }

    @Override
    public boolean onKey(char key, int keyCode) {
        for (IGuiWidget widget : this.widgets)
            if (widget.onKey(key, keyCode)) return true;
        return false;
    }

    private boolean isMouseOverWidget(IGuiWidget widget, int mouseX, int mouseY) {
        Rectangle rect = this.getBounds();
        mouseX -= rect.x;
        mouseY -= rect.y;
        return widget.getBounds().contains(mouseX, mouseY);
    }

    @Override
    public void handleMouseInput() {
        if (this.getBounds().contains(this.lastMouseX, this.lastMouseY)) {
            this.handleMouseWheel(Mouse.getDWheel());
        }
    }

    public boolean handleMouseWheel(int mouseWheel) {
        for (IGuiWidget widget : this.widgets) {
            widget.handleMouseInput();
            if (widget.getID() == WIDGET_SCROLLBAR_ID) {
                int wheel = -mouseWheel;
                wheel = MathHelper.clamp(wheel, -1, 1);
                ((WidgetVerticalScrollbar) widget).currentScroll += (float) wheel / (this.textList.size() - MAX_LINES);
                return true;
            }
        }
        return false;
    }

    @Override
    public void postRender(int mouseX, int mouseY, float partialTick) {

    }

    public void setLineSpacing(int lineSpacing) {
        this.lineSpacing = lineSpacing;
    }

    public void setTexture(ResourceLocation texture) {
        this.statIcon = StatIcon.of(texture);
    }

    public void setTexture(ItemStack itemStack) {
        this.statIcon = StatIcon.of(itemStack);
    }

    public static class StatIcon {
        public static final StatIcon NONE = new StatIcon(ItemStack.EMPTY, null);

        private final ItemStack stack;
        private final ResourceLocation texture;

        private StatIcon(ItemStack stack, ResourceLocation texture) {
            this.stack = stack;
            this.texture = texture;
        }

        public static StatIcon of(ItemStack stack) {
            return new StatIcon(stack, null);
        }

        public static StatIcon of(Item item) {
            return new StatIcon(new ItemStack(item, 1, 0), null);
        }

        public static StatIcon of(ResourceLocation texture) {
            return new StatIcon(ItemStack.EMPTY, texture);
        }

        void render(Gui gui, int x, int y, boolean leftSided) {
            GlStateManager.color(1, 1, 1, 1);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            if (this.texture != null) {
                GuiPneumaticContainerBase.drawTexture(this.texture, x - (leftSided ? 16 : 0), y);
            } else if (!this.stack.isEmpty() && gui != null || !(this.stack.getItem() instanceof ItemBlock)) {
                RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();
                renderItem.zLevel = 1;
                GlStateManager.pushMatrix();
                GlStateManager.translate(0, 0, -50);
                GlStateManager.enableRescaleNormal();
                RenderHelper.enableGUIStandardItemLighting();
                renderItem.renderItemAndEffectIntoGUI(this.stack, x - (leftSided ? 16 : 0), y);
                RenderHelper.disableStandardItemLighting();
                GlStateManager.disableRescaleNormal();
                GlStateManager.popMatrix();
                GlStateManager.enableAlpha();
            }
            GlStateManager.disableBlend();
        }
    }
}
