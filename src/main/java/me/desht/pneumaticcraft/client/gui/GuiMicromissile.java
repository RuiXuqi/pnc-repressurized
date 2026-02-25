package me.desht.pneumaticcraft.client.gui;

import me.desht.pneumaticcraft.client.gui.widget.IGuiWidget;
import me.desht.pneumaticcraft.client.gui.widget.WidgetLabel;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTextField;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTooltipArea;
import me.desht.pneumaticcraft.client.util.GuiUtils;
import me.desht.pneumaticcraft.client.util.RenderUtils;
import me.desht.pneumaticcraft.common.item.ItemMicromissiles;
import me.desht.pneumaticcraft.common.item.ItemMicromissiles.FireMode;
import me.desht.pneumaticcraft.common.item.Itemss;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketUpdateMicromissileSettings;
import me.desht.pneumaticcraft.common.util.EntityFilter;
import me.desht.pneumaticcraft.common.util.NBTUtil;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.IOException;

public class GuiMicromissile extends GuiPneumaticScreenBase {
    private static final Rectangle SELECTOR_BOUNDS = new Rectangle(12, 21, 92, 81);
    private static final int MAX_DIST = SELECTOR_BOUNDS.width;

    // these points are relative to the SELECTOR_BOUNDS box defined above (note positive Y is downwards)
    private static final Point TOP_SPEED_PT = new Point(46, 1);
    private static final Point TURN_SPEED_PT = new Point(1, 80);
    private static final Point DMG_PT = new Point(92, 80);

    private float turnSpeed;
    private float topSpeed;
    private float damage;
    private Point point;
    private FireMode fireMode;
    private boolean dragging = false;
    private String entityFilter;
    private int sendTimer = 0;

    private WidgetTextField textField;
    private WidgetLabel filterLabel;
    private GuiButtonSpecial modeButton;
    private GuiButtonSpecial warningButton;

    public GuiMicromissile() {
        this.xSize = 183;
        this.ySize = 191;

        ItemStack stack = ItemMicromissiles.getHeldMicroMissile(Minecraft.getMinecraft().player);
        if (stack.getItem() == Itemss.MICROMISSILES) {
            if (stack.hasTagCompound()) {
                this.topSpeed = NBTUtil.getFloat(stack, ItemMicromissiles.NBT_TOP_SPEED);
                this.turnSpeed = NBTUtil.getFloat(stack, ItemMicromissiles.NBT_TURN_SPEED);
                this.damage = NBTUtil.getFloat(stack, ItemMicromissiles.NBT_DAMAGE);
                this.entityFilter = NBTUtil.getString(stack, ItemMicromissiles.NBT_FILTER);
                this.point = new Point(NBTUtil.getInteger(stack, ItemMicromissiles.NBT_PX), NBTUtil.getInteger(stack, ItemMicromissiles.NBT_PY));
                this.fireMode = FireMode.fromString(NBTUtil.getString(stack, ItemMicromissiles.NBT_FIRE_MODE));
            } else {
                this.topSpeed = this.turnSpeed = this.damage = 1 / 3f;
                this.point = new Point(MAX_DIST / 2, MAX_DIST / 4);
                this.entityFilter = "";
                this.fireMode = FireMode.SMART;
            }
        }
    }

    @Override
    public void initGui() {
        super.initGui();

        FontRenderer fr = Minecraft.getMinecraft().fontRenderer;

        String labelStr = I18n.format("gui.sentryTurret.targetFilter");
        this.filterLabel = new WidgetLabel(this.guiLeft + 12, this.guiTop + 130, labelStr);
        this.addWidget(this.filterLabel);
        int textBoxX = this.guiLeft + 12 + fr.getStringWidth(labelStr) + 5;
        int textBoxWidth = this.xSize - (textBoxX - this.guiLeft) - 20;
        this.textField = new WidgetTextField(Minecraft.getMinecraft().fontRenderer, textBoxX, this.guiTop + 128, textBoxWidth, 10);
        this.textField.setText(this.entityFilter);
        this.addWidget(this.textField);
        this.textField.setFocused(true);

        this.addWidget(new WidgetTooltipArea(this.guiLeft + 42, this.guiTop + 9, 35, 9, "gui.micromissile.topSpeed"));
        this.addWidget(new WidgetTooltipArea(this.guiLeft + 6, this.guiTop + 103, 25, 12, "gui.micromissile.turnSpeed"));
        this.addWidget(new WidgetTooltipArea(this.guiLeft + 96, this.guiTop + 103, 15, 15, "gui.micromissile.damage"));

        String saveLabel = I18n.format("gui.micromissile.saveDefault");
        int buttonWidth = fr.getStringWidth(saveLabel) + 10;
        int buttonX = this.guiLeft + (this.xSize - buttonWidth) / 2;
        this.buttonList.add(new GuiButtonSpecial(1, buttonX, this.guiTop + 160, buttonWidth, 20, saveLabel));

        this.modeButton = new GuiButtonSpecial(2, this.guiLeft + 123, this.guiTop + 20, 52, 20, "");
        this.modeButton.setTooltipText("gui.micromissile.modeTooltip");
        this.buttonList.add(this.modeButton);

        this.warningButton = new GuiButtonSpecial(3, this.guiLeft + 162, this.guiTop + 123, 20, 20, "");
        this.warningButton.setVisible(false);
        this.warningButton.setRenderedIcon(Textures.GUI_PROBLEMS_TEXTURE);
        this.buttonList.add(this.warningButton);

        this.validateEntityFilter(this.entityFilter);

        this.setupWidgets();
    }

    private void setupWidgets() {
        this.textField.setEnabled(this.fireMode == FireMode.SMART);
        this.filterLabel.setColor(this.fireMode == FireMode.SMART ? 0xFF404040 : 0xFFAAAAAA);
        this.modeButton.displayString = I18n.format("gui.micromissile.mode." + this.fireMode.toString());
    }

    @Override
    public void drawScreen(int x, int y, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(x, y, partialTicks);

        if (Keyboard.isKeyDown(Keyboard.KEY_F1)) {
            GuiUtils.showPopupHelpScreen(this, this.fontRenderer,
                    PneumaticCraftUtils.convertStringIntoList(I18n.format("gui.entityFilter.helpText"), 60));
        } else if (this.textField.getBounds().contains(x, y)) {
            String str = I18n.format("gui.entityFilter");
            this.fontRenderer.drawString(str, this.guiLeft + (this.xSize - this.fontRenderer.getStringWidth(str)) / 2, this.guiTop + this.ySize + 5, 0x808080);
        }

        if (this.fireMode == FireMode.DUMB) {
            return;
        }

        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        if (this.point != null) {
            double px = this.point.getX();
            double py = this.point.getY();
            RenderUtils.glColorHex(0x2020A0, 255);
            GlStateManager.pushMatrix();
            GlStateManager.translate(this.guiLeft + SELECTOR_BOUNDS.x, this.guiTop + SELECTOR_BOUNDS.y, 0);
            BufferBuilder wr = Tessellator.getInstance().getBuffer();

            // crosshairs
            int size = this.dragging ? 5 : 3;
            GlStateManager.glLineWidth(2);
            GlStateManager.glBegin(GL11.GL_LINES);
            GL11.glVertex2d(px - size, py);
            GL11.glVertex2d(px + size, py);
            GlStateManager.glEnd();
            GlStateManager.glBegin(GL11.GL_LINES);
            GL11.glVertex2d(px, py - size);
            GL11.glVertex2d(px, py + size);
            GlStateManager.glEnd();

            GL11.glEnable(GL11.GL_LINE_STIPPLE);
            GL11.glLineStipple(1, (short) 0xAAAA);
            // speed line
            GlStateManager.glBegin(GL11.GL_LINES);
            GL11.glVertex2d(px, py);
            GL11.glVertex2d(SELECTOR_BOUNDS.width / 2.0, 0);
            GlStateManager.glEnd();
            // turn speed line
            GlStateManager.glBegin(GL11.GL_LINES);
            GL11.glVertex2d(px, py);
            GL11.glVertex2d(0, SELECTOR_BOUNDS.height);
            GlStateManager.glEnd();
            // damage line
            GlStateManager.glBegin(GL11.GL_LINES);
            GL11.glVertex2d(px, py);
            GL11.glVertex2d(SELECTOR_BOUNDS.width, SELECTOR_BOUNDS.height);
            GlStateManager.glEnd();

            GL11.glDisable(GL11.GL_LINE_STIPPLE);
            GlStateManager.popMatrix();
            RenderUtils.glColorHex(0xffffff, 255);
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(this.guiLeft, this.guiTop, 0);
        GlStateManager.glLineWidth(10);
        GL11.glEnable(GL11.GL_LINE_STIPPLE);
        GL11.glLineStipple(1, (short) 0xFEFE);
        RenderUtils.glColorHex(0x00C000, 255);
        GlStateManager.glBegin(GL11.GL_LINES);
        GL11.glVertex2i(125, 51);
        GL11.glVertex2i(125 + (int) (49 * this.topSpeed), 51);
        GlStateManager.glEnd();
        GlStateManager.glBegin(GL11.GL_LINES);
        GL11.glVertex2i(125, 71);
        GL11.glVertex2i(125 + (int) (49 * this.turnSpeed), 71);
        GlStateManager.glEnd();
        GlStateManager.glBegin(GL11.GL_LINES);
        GL11.glVertex2i(125, 91);
        GL11.glVertex2i(125 + (int) (49 * this.damage), 91);
        GlStateManager.glEnd();
        GlStateManager.popMatrix();

        GL11.glDisable(GL11.GL_LINE_STIPPLE);
        GlStateManager.glLineWidth(1);
        GlStateManager.enableLighting();

        GlStateManager.enableTexture2D();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        if (this.sendTimer > 0 && --this.sendTimer == 0) {
            this.sendSettingsToServer(false);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    protected ResourceLocation getTexture() {
        return Textures.GUI_MICROMISSILE;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (this.trySetPoint(mouseX, mouseY)) {
            this.dragging = true;
        } else {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (this.dragging) {
            this.trySetPoint(mouseX, mouseY);
        } else {
            super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (this.dragging) {
            // send updated values to server
            this.sendSettingsToServer(false);
            this.dragging = false;
        } else {
            super.mouseReleased(mouseX, mouseY, state);
        }
    }

    @Override
    public void onKeyTyped(IGuiWidget widget) {
        if (widget instanceof WidgetTextField) {
            // entity filter updated
            this.entityFilter = ((WidgetTextField) widget).getText();
            if (this.validateEntityFilter(this.entityFilter)) {
                this.sendTimer = 5;  // delayed send to reduce packet spam while typing
            }
        }
    }

    private boolean validateEntityFilter(String filter) {
        try {
            this.warningButton.visible = false;
            this.warningButton.setTooltipText("");
            EntityFilter f = new EntityFilter(filter);  // syntax check
            return true;
        } catch (Exception e) {
            this.warningButton.visible = true;
            this.warningButton.setTooltipText(TextFormatting.GOLD + e.getMessage());
            return false;
        }
    }

    private boolean trySetPoint(int mouseX, int mouseY) {
        Point p = this.getPoint(mouseX, mouseY);
        if (p != null) {
            double dSpeed = MAX_DIST - p.distance(TOP_SPEED_PT);
            double dTurnSpd = MAX_DIST - p.distance(TURN_SPEED_PT);
            double dDamage = MAX_DIST - p.distance(DMG_PT);
            double total = dSpeed + dTurnSpd + dDamage;
            this.topSpeed = (float) (dSpeed / total);
            this.turnSpeed = (float) (dTurnSpd / total);
            this.damage = (float) (dDamage / total);
            this.point = p;
            return true;
        }
        return false;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 1) {
            this.sendSettingsToServer(true);
        } else if (button.id == 2) {
            int n = this.fireMode.ordinal() + 1;
            if (n >= FireMode.values().length) n = 0;
            this.fireMode = FireMode.values()[n];
            this.setupWidgets();
            this.sendSettingsToServer(false);
        } else {
            super.actionPerformed(button);
        }
    }

    private void sendSettingsToServer(boolean saveDefault) {
        NetworkHandler.sendToServer(new PacketUpdateMicromissileSettings(this.topSpeed, this.turnSpeed, this.damage, this.point, this.entityFilter, this.fireMode, saveDefault));
    }

    private Point getPoint(int mouseX, int mouseY) {
        Rectangle r = new Rectangle(SELECTOR_BOUNDS.x + this.guiLeft, SELECTOR_BOUNDS.y + this.guiTop, SELECTOR_BOUNDS.width, SELECTOR_BOUNDS.height);

        if (!r.contains(mouseX, mouseY)) {
            return null;
        }

        Point p = new Point(mouseX - r.x, mouseY - r.y);
        return this.isPointInTriangle(p, TOP_SPEED_PT, TURN_SPEED_PT, DMG_PT) ? p : null;
    }

    private boolean isPointInTriangle(Point s, Point a, Point b, Point c) {
        int as_x = s.x - a.x;
        int as_y = s.y - a.y;

        boolean s_ab = (b.x - a.x) * as_y - (b.y - a.y) * as_x > 0;

        if ((c.x - a.x) * as_y - (c.y - a.y) * as_x > 0 == s_ab) return false;

        return (c.x - b.x) * (s.y - b.y) - (c.y - b.y) * (s.x - b.x) > 0 == s_ab;
    }
}
