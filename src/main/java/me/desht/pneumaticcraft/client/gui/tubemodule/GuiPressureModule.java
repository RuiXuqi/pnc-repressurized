package me.desht.pneumaticcraft.client.gui.tubemodule;

import me.desht.pneumaticcraft.api.client.IGuiAnimatedStat;
import me.desht.pneumaticcraft.client.gui.widget.GuiAnimatedStat;
import me.desht.pneumaticcraft.client.gui.widget.GuiCheckBox;
import me.desht.pneumaticcraft.client.gui.widget.IGuiWidget;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTooltipArea;
import me.desht.pneumaticcraft.common.block.tubes.TubeModule;
import me.desht.pneumaticcraft.common.block.tubes.TubeModuleRedstoneReceiving;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketUpdatePressureModule;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.FMLClientHandler;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.IOException;

public class GuiPressureModule extends GuiTubeModule {

    private GuiTextField lowerBoundField;
    private GuiTextField higherBoundField;
    private int graphLowY;
    private int graphHighY;
    private int graphLeft;
    private int graphRight;
    private Rectangle lowerBoundArea, higherBoundArea;
    private boolean grabLower, grabHigher;

    public GuiPressureModule(EntityPlayer player, int x, int y, int z) {
        super(player, x, y, z);
        this.ySize = 191;
    }

    public GuiPressureModule(TubeModule module) {
        super(module);
        this.ySize = 191;
    }

    @Override
    public void initGui() {
        super.initGui();
        int xStart = (this.width - this.xSize) / 2;
        int yStart = (this.height - this.ySize) / 2;

        this.addLabel("lower", this.guiLeft + 10, this.guiTop + 30);
        this.addLabel("bar", this.guiLeft + 45, this.guiTop + 42);
        this.addLabel("higher", this.guiLeft + 140, this.guiTop + 30);

        String title = I18n.format("item." + this.module.getType() + ".name");
        this.addLabel(title, this.width / 2 - this.fontRenderer.getStringWidth(title) / 2, this.guiTop + 5);

        this.lowerBoundField = new GuiTextField(-1, this.fontRenderer, xStart + 10, yStart + 41, 30, 10);
        this.lowerBoundField.setText(PneumaticCraftUtils.roundNumberTo(this.module.lowerBound, 1));
        this.higherBoundField = new GuiTextField(-1, this.fontRenderer, xStart + 140, yStart + 41, 30, 10);
        this.higherBoundField.setText(PneumaticCraftUtils.roundNumberTo(this.module.higherBound, 1));

        this.graphLowY = this.guiTop + 153;
        this.graphHighY = this.guiTop + 93;
        this.graphLeft = this.guiLeft + 22;
        this.graphRight = this.guiLeft + 172;

        this.addWidget(new WidgetTooltipArea(this.graphLeft - 20, this.graphHighY, 25, this.graphLowY - this.graphHighY, "gui.redstone"));
        this.addWidget(new WidgetTooltipArea(this.graphLeft, this.graphLowY - 5, this.graphRight - this.graphLeft, 25, "gui.threshold"));

        IGuiAnimatedStat stat = new GuiAnimatedStat(this, "gui.tab.info", GuiAnimatedStat.StatIcon.of(Textures.GUI_INFO_LOCATION), xStart, yStart + 5, 0xFF8888FF, null, true).setText("gui.tab.info.tubeModule");
        stat.setBeveled(true);
        this.addWidget((IGuiWidget) stat);

        GuiCheckBox advancedMode = new GuiCheckBox(0, this.guiLeft + 6, this.guiTop + 15, 0xFF404040, "gui.tubeModule.advancedConfig").setTooltip(I18n.format("gui.tubeModule.advancedConfig.tooltip"));
        advancedMode.checked = true;
        this.addWidget(advancedMode);

        this.higherBoundArea = new Rectangle(this.guiLeft + 11, this.guiTop + 59, 158, 15);
        this.lowerBoundArea = new Rectangle(this.guiLeft + 11, this.guiTop + 73, 158, 15);
    }

    @Override
    protected ResourceLocation getTexture() {
        return Textures.GUI_TUBE_MODULE;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        GlStateManager.disableLighting();

        FMLClientHandler.instance().getClient().getTextureManager().bindTexture(this.getTexture());
        int scrollbarLowerBoundX = (int) (this.guiLeft + 16 + (158 - 11) * (this.module.lowerBound / (TubeModule.MAX_VALUE + 1)));
        int scrollbarHigherBoundX = (int) (this.guiLeft + 16 + (158 - 11) * (this.module.higherBound / (TubeModule.MAX_VALUE + 1)));

        this.drawTexturedModalRect(scrollbarLowerBoundX, this.guiTop + 73, 183, 0, 15, 12);
        this.drawTexturedModalRect(scrollbarHigherBoundX, this.guiTop + 59, 183, 0, 15, 12);

        this.lowerBoundField.drawTextBox();
        this.higherBoundField.drawTextBox();

        /*
         * Draw graph
         */
        this.drawVerticalLine(this.graphLeft, this.graphHighY, this.graphLowY, 0xFF000000);
        for (int i = 0; i < 16; i++) {
            boolean longer = i % 5 == 0;
            if (longer) {
                this.fontRenderer.drawString(i + "", this.graphLeft - 5 - this.fontRenderer.getStringWidth(i + ""), this.graphHighY + (this.graphLowY - this.graphHighY) * (15 - i) / 15 - 3, 0xFF000000);
                this.drawHorizontalLine(this.graphLeft + 4, this.graphRight, this.graphHighY + (this.graphLowY - this.graphHighY) * (15 - i) / 15, i == 0 ? 0xFF000000 : 0x33000000);

            }
            this.drawHorizontalLine(this.graphLeft - (longer ? 5 : 3), this.graphLeft + 3, this.graphHighY + (this.graphLowY - this.graphHighY) * (15 - i) / 15, 0xFF000000);
        }
        for (int i = 0; i < 31; i++) {
            boolean longer = i % 5 == 0;
            if (longer) {
                this.fontRenderer.drawString(i + "", this.graphLeft + (this.graphRight - this.graphLeft) * i / 30 - this.fontRenderer.getStringWidth(i + "") / 2 + 1, this.graphLowY + 6, 0xFF000000);
                this.drawVerticalLine(this.graphLeft + (this.graphRight - this.graphLeft) * i / 30, this.graphHighY, this.graphLowY - 2, 0x33000000);
            }
            this.drawVerticalLine(this.graphLeft + (this.graphRight - this.graphLeft) * i / 30, this.graphLowY - 3, this.graphLowY + (longer ? 5 : 3), 0xFF000000);
        }

        /*
         * Draw the current redstone strength
         */
        if (this.module instanceof TubeModuleRedstoneReceiving) {
            this.module.onNeighborBlockUpdate();
            this.drawHorizontalLine(this.graphLeft + 4, this.graphRight, this.graphHighY + (this.graphLowY - this.graphHighY) * (15 - ((TubeModuleRedstoneReceiving) this.module).getReceivingRedstoneLevel()) / 15, 0xFFFF0000);
            String status = "Current threshold: " + PneumaticCraftUtils.roundNumberTo(((TubeModuleRedstoneReceiving) this.module).getThreshold(), 1) + " bar";
            this.fontRenderer.drawString(status, this.guiLeft + this.xSize / 2 - this.fontRenderer.getStringWidth(status) / 2, this.guiTop + 173, 0xFF000000);
        }

        /*
         * Draw the data in the graph
         */
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        bufferBuilder.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.disableTexture2D();
        GlStateManager.color(0, 0, 0, 1.0f);
        for (int i = 0; i < 16; i++) {
            double y = this.graphHighY + (this.graphLowY - this.graphHighY) * (15 - i) / 15.0;
            double x = this.graphLeft + (this.graphRight - this.graphLeft) * this.module.getThreshold(i) / 30;
            bufferBuilder.pos(x, y, 90.0d).color(0.25f + i * 0.05f, 0f, 0f, 1.0f).endVertex();
        }
        Tessellator.getInstance().draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();

    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        boolean wasFocused = this.lowerBoundField.isFocused();
        this.lowerBoundField.mouseClicked(mouseX, mouseY, mouseButton);
        if (wasFocused && !this.lowerBoundField.isFocused()) {
            this.updateBoundFromTextfield(0);
        }

        wasFocused = this.higherBoundField.isFocused();
        this.higherBoundField.mouseClicked(mouseX, mouseY, mouseButton);
        if (wasFocused && !this.higherBoundField.isFocused()) {
            this.updateBoundFromTextfield(1);
        }

        if (this.lowerBoundArea.contains(mouseX, mouseY)) {
            this.module.lowerBound = (float) (mouseX - 6 - (this.guiLeft + 11)) / (158 - 11) * TubeModule.MAX_VALUE;
            this.module.lowerBound = Math.min(Math.max(-1, this.module.lowerBound), TubeModule.MAX_VALUE);
            this.grabLower = true;
        } else if (this.higherBoundArea.contains(mouseX, mouseY)) {
            this.module.higherBound = (float) (mouseX - 6 - (this.guiLeft + 11)) / (158 - 11) * TubeModule.MAX_VALUE;
            this.module.higherBound = Math.min(Math.max(-1, this.module.higherBound), TubeModule.MAX_VALUE);
            this.grabHigher = true;
        }
    }

    private void updateBoundFromTextfield(int fieldId) {
        try {
            switch (fieldId) {
                case 0:
                    this.module.lowerBound = Float.parseFloat(this.lowerBoundField.getText());
                    this.module.lowerBound = Math.max(-1, Math.min(this.module.lowerBound, TubeModule.MAX_VALUE));
                    NetworkHandler.sendToServer(new PacketUpdatePressureModule(this.module, 0, this.module.lowerBound));
                    break;
                case 1:
                    this.module.higherBound = Float.parseFloat(this.higherBoundField.getText());
                    this.module.higherBound = Math.max(-1, Math.min(this.module.higherBound, TubeModule.MAX_VALUE));
                    NetworkHandler.sendToServer(new PacketUpdatePressureModule(this.module, 1, this.module.higherBound));
                    break;
                default:
                    throw new IllegalArgumentException("unknown field id " + fieldId);
            }
        } catch (NumberFormatException e) {
            // ignore
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (this.grabLower) {
            this.module.lowerBound = (float) (mouseX - 6 - (this.guiLeft + 11)) / (158 - 11) * TubeModule.MAX_VALUE;
            this.module.lowerBound = Math.min(Math.max(-1, this.module.lowerBound), TubeModule.MAX_VALUE);
        } else if (this.grabHigher) {
            this.module.higherBound = (float) (mouseX - 6 - (this.guiLeft + 11)) / (158 - 11) * TubeModule.MAX_VALUE;
            this.module.higherBound = Math.min(Math.max(-1, this.module.higherBound), TubeModule.MAX_VALUE);
        } else {
            super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (this.grabLower) {
            NetworkHandler.sendToServer(new PacketUpdatePressureModule(this.module, 0, this.module.lowerBound));
            this.grabLower = false;
        } else if (this.grabHigher) {
            NetworkHandler.sendToServer(new PacketUpdatePressureModule(this.module, 1, this.module.higherBound));
            this.grabHigher = false;
        } else {
            super.mouseReleased(mouseX, mouseY, state);
        }
    }

    @Override
    protected void keyTyped(char key, int keyCode) throws IOException {
        if (this.lowerBoundField.isFocused() && keyCode != Keyboard.KEY_ESCAPE) {
            this.lowerBoundField.textboxKeyTyped(key, keyCode);
            if (keyCode == Keyboard.KEY_RETURN) this.updateBoundFromTextfield(0);
        } else if (this.higherBoundField.isFocused() && keyCode != Keyboard.KEY_ESCAPE) {
            this.higherBoundField.textboxKeyTyped(key, keyCode);
            if (keyCode == Keyboard.KEY_RETURN) this.updateBoundFromTextfield(1);
        } else {
            super.keyTyped(key, keyCode);
        }
    }

    @Override
    public void actionPerformed(IGuiWidget widget) {
        super.actionPerformed(widget);
        if (widget.getID() == 0) {
            this.module.advancedConfig = ((GuiCheckBox) widget).checked;
            NetworkHandler.sendToServer(new PacketUpdatePressureModule(this.module, 2, this.module.advancedConfig ? 1 : 0));
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (!this.module.advancedConfig) this.mc.displayGuiScreen(new GuiPressureModuleSimple(this.module));

        if (!this.lowerBoundField.isFocused())
            this.lowerBoundField.setText(PneumaticCraftUtils.roundNumberTo(this.module.lowerBound, 1));
        if (!this.higherBoundField.isFocused())
            this.higherBoundField.setText(PneumaticCraftUtils.roundNumberTo(this.module.higherBound, 1));
    }
}
