package me.desht.pneumaticcraft.client.util;

import me.desht.pneumaticcraft.client.render.pressure_gauge.PressureGaugeRenderer2D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.HashMap;
import java.util.List;

public class GuiUtils extends Gui {
    private static final HashMap<String, ResourceLocation> resourceMap = new HashMap<>();
    public static final double PRESSURE_GAUGE_RADIUS = 20D;
    private static final RenderItem itemRenderer = Minecraft.getMinecraft().getRenderItem();

    public static void drawItemStack(ItemStack stack, int x, int y) {
        GlStateManager.enableRescaleNormal();
        RenderHelper.enableGUIStandardItemLighting();
        itemRenderer.renderItemAndEffectIntoGUI(stack, x, y);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
    }

    private static final int TEX_WIDTH = 16;
    private static final int TEX_HEIGHT = 16;

    public static void drawFluid(final Rectangle bounds, @Nullable FluidStack fluidStack, @Nullable IFluidTank tank) {
        if (fluidStack == null || fluidStack.getFluid() == null) {
            return;
        }

        Fluid fluid = fluidStack.getFluid();
        TextureMap textureMapBlocks = Minecraft.getMinecraft().getTextureMapBlocks();
        ResourceLocation fluidStill = fluid.getStill();
        TextureAtlasSprite fluidStillSprite = null;
        if (fluidStill != null) {
            fluidStillSprite = textureMapBlocks.getTextureExtry(fluidStill.toString());
        }
        if (fluidStillSprite == null) {
            fluidStillSprite = textureMapBlocks.getMissingSprite();
        }

        int fluidColor = fluid.getColor(fluidStack);

        int scaledAmount = tank == null ? bounds.height : fluidStack.amount * bounds.height / tank.getCapacity();
        if (fluidStack.amount > 0 && scaledAmount < 1) {
            scaledAmount = 1;
        }
        scaledAmount = Math.min(scaledAmount, bounds.height);

        Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        RenderUtils.glColorHex(fluidColor, 255);

        final int xTileCount = bounds.width / TEX_WIDTH;
        final int xRemainder = bounds.width - xTileCount * TEX_WIDTH;
        final int yTileCount = scaledAmount / TEX_HEIGHT;
        final int yRemainder = scaledAmount - yTileCount * TEX_HEIGHT;

        int yStart = bounds.y + bounds.height;
        if (fluid.getDensity() < 0) yStart -= (bounds.height - scaledAmount);

        for (int xTile = 0; xTile <= xTileCount; xTile++) {
            for (int yTile = 0; yTile <= yTileCount; yTile++) {
                int w = xTile == xTileCount ? xRemainder : TEX_WIDTH;
                int h = yTile == yTileCount ? yRemainder : TEX_HEIGHT;
                int x = bounds.x + xTile * TEX_WIDTH;
                int y = yStart - (yTile + 1) * TEX_HEIGHT;
                if (bounds.width > 0 && h > 0) {
                    int maskTop = TEX_HEIGHT - h;
                    int maskRight = TEX_WIDTH - w;

                    drawFluidTexture(x, y, fluidStillSprite, maskTop, maskRight, 100);
                }
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static void drawFluidTexture(double xCoord, double yCoord, TextureAtlasSprite textureSprite, int maskTop, int maskRight, double zLevel) {
        double uMin = textureSprite.getMinU();
        double uMax = textureSprite.getMaxU();
        double vMin = textureSprite.getMinV();
        double vMax = textureSprite.getMaxV();
        uMax = uMax - maskRight / 16.0 * (uMax - uMin);
        vMax = vMax - maskTop / 16.0 * (vMax - vMin);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder worldrenderer = tessellator.getBuffer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(xCoord, yCoord + 16, zLevel).tex(uMin, vMax).endVertex();
        worldrenderer.pos(xCoord + 16 - maskRight, yCoord + 16, zLevel).tex(uMax, vMax).endVertex();
        worldrenderer.pos(xCoord + 16 - maskRight, yCoord + maskTop, zLevel).tex(uMax, vMin).endVertex();
        worldrenderer.pos(xCoord, yCoord + maskTop, zLevel).tex(uMin, vMin).endVertex();
        tessellator.draw();
    }

    public static void showPopupHelpScreen(GuiScreen screen, FontRenderer fontRenderer, List<String> helpText) {
        int boxWidth = 0;
        int boxHeight = helpText.size() * fontRenderer.FONT_HEIGHT;
        for (String s : helpText) {
            boxWidth = Math.max(boxWidth, fontRenderer.getStringWidth(s));
        }

        int x, y;
        if (screen instanceof GuiContainer container) {
            x = (container.getXSize() - boxWidth) / 2;
            y = (container.getYSize() - boxHeight) / 2;
        } else {
            x = (screen.width - boxWidth) / 2;
            y = (screen.height - boxHeight) / 2;
        }
        GlStateManager.translate(0, 0, 400);
        drawRect(x - 4, y - 4, x + boxWidth + 8, y + boxHeight + 8, 0xC0000000);
        drawRect(x - 4, y - 4, x + boxWidth + 8, y - 3, 0xFF808080);
        drawRect(x - 4, y + boxHeight + 8, x + boxWidth + 8, y + boxHeight + 9, 0xFF808080);
        drawRect(x - 4, y - 4, x - 3, y + boxHeight + 8, 0xFF808080);
        drawRect(x + boxWidth + 8, y - 4, x + boxWidth + 9, y + boxHeight + 8, 0xFF808080);

        for (String s : helpText) {
            fontRenderer.drawString(s, x, y, 0xFFE0E0E0);
            y += fontRenderer.FONT_HEIGHT;
        }
        GlStateManager.translate(0, 0, -300);
    }

    /**
     * @deprecated Call {@link PressureGaugeRenderer2D#drawPressureGauge(FontRenderer, float, float, float, float, float, int, int, float) PressureGaugeRenderer2D} for 2D
     * or {@link me.desht.pneumaticcraft.client.render.pressure_gauge.PressureGaugeRenderer3D#drawPressureGauge(float, float, float, float, float, int, int, float) PressureGaugeRenderer3D} for 3D.
     */
    @Deprecated
    public static void drawPressureGauge(
            FontRenderer fontRenderer,
            float minPressure, float maxPressure, float dangerPressure,
            float minWorkingPressure, float currentPressure,
            int xPos, int yPos, float zLevel
    ) {
        PressureGaugeRenderer2D.drawPressureGauge(
                fontRenderer,
                minPressure, maxPressure, dangerPressure,
                minWorkingPressure, currentPressure,
                xPos, yPos, zLevel
        );
    }

    /**
     * @deprecated Call {@link PressureGaugeRenderer2D#drawPressureGauge(FontRenderer, float, float, float, float, float, int, int, float, int) PressureGaugeRenderer2D} for 2D
     * or {@link me.desht.pneumaticcraft.client.render.pressure_gauge.PressureGaugeRenderer3D#drawPressureGauge(float, float, float, float, float, int, int, float, int) PressureGaugeRenderer3D} for 3D.
     */
    @Deprecated
    public static void drawPressureGauge(
            FontRenderer fontRenderer,
            float minPressure, float maxPressure, float dangerPressure,
            float minWorkingPressure, float currentPressure,
            int xPos, int yPos, float zLevel, int fgColor
    ) {
        PressureGaugeRenderer2D.drawPressureGauge(
                fontRenderer,
                minPressure, maxPressure, dangerPressure,
                minWorkingPressure, currentPressure,
                xPos, yPos, zLevel, fgColor
        );
    }
}
