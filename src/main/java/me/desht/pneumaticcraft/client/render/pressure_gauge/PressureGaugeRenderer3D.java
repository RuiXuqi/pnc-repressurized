/*
 * This file is part of pnc-repressurized.
 *
 *     pnc-repressurized is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     pnc-repressurized is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with pnc-repressurized.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.desht.pneumaticcraft.client.render.pressure_gauge;

import me.desht.pneumaticcraft.client.util.RenderUtils;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

import static me.desht.pneumaticcraft.client.render.pressure_gauge.PressureGaugeRenderer2D.*;

public class PressureGaugeRenderer3D {
    private static final float RADIUS = 19F;
    private static final float PI_F = (float) Math.PI;
    private static final float START_ANGLE = 240F / 180F * PI_F;
    private static final float STOP_ANGLE = -60F / 180F * PI_F;
    private static final int CIRCLE_POINTS = 180;
    private static final int GAUGE_POINTS = (int) ((START_ANGLE - STOP_ANGLE) / (2F * PI_F) * CIRCLE_POINTS);

    /**
     * Render a pressure gauge into the world.
     *
     * @param minPressure        minimum pressure
     * @param maxPressure        maximum pressure
     * @param dangerPressure     danger pressure (red area)
     * @param minWorkingPressure min. working pressure
     * @param currentPressure    current pressure (where needle points)
     * @param xPos               x position
     * @param yPos               y position
     * @param zLevel             z level, always 0
     */
    public static void drawPressureGauge(
            float minPressure, float maxPressure, float dangerPressure,
            float minWorkingPressure, float currentPressure,
            int xPos, int yPos, float zLevel
    ) {
        drawPressureGauge(
                minPressure, maxPressure, dangerPressure,
                minWorkingPressure, currentPressure,
                xPos, yPos, zLevel, 0xFF000000
        );
    }

    /**
     * Render a pressure gauge into the world.
     *
     * @param minPressure        minimum pressure
     * @param maxPressure        maximum pressure
     * @param dangerPressure     danger pressure (red area)
     * @param minWorkingPressure min. working pressure
     * @param currentPressure    current pressure (where needle points)
     * @param xPos               x position
     * @param yPos               y position
     * @param zLevel             z level, always 0
     * @param fgColor            color to draw the surround, needle and text
     */
    public static void drawPressureGauge(
            float minPressure, float maxPressure, float dangerPressure,
            float minWorkingPressure, float currentPressure,
            int xPos, int yPos, float zLevel, int fgColor
    ) {
        // Blend in case fgColor is alpha
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        float lastX = OpenGlHelper.lastBrightnessX;
        float lastY = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
        GlStateManager.disableLighting();
        GlStateManager.disableTexture2D();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.glLineWidth(2.0F);
        Tessellator tess = Tessellator.getInstance();

        // Draw the green and red surface in the gauge.
        drawGaugeBackground(tess, minPressure, maxPressure, dangerPressure, minWorkingPressure, xPos, yPos, zLevel);

        // Draw the surrounding circle in the foreground colour
        drawGaugeSurround(tess, xPos, yPos, zLevel, fgColor);

        // Draw the scale
        int currentScale = (int) maxPressure;
        List<TextScaler> textScalers = new ArrayList<>();
        drawScale(tess, minPressure, maxPressure, xPos, yPos, zLevel, currentScale, textScalers);

        // Draw the needle.
        float angleIndicator = GAUGE_POINTS - (int) ((currentPressure - minPressure) / (maxPressure - minPressure) * GAUGE_POINTS);
        angleIndicator = -angleIndicator / CIRCLE_POINTS * 2F * PI_F - STOP_ANGLE;
        drawNeedle(tess, xPos, yPos, zLevel, angleIndicator, fgColor);

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastX, lastY);

        // draw the numbers next to the scaler.
        drawText(xPos, yPos, zLevel, fgColor, textScalers);

        GlStateManager.disableBlend();
    }

    private static void drawGaugeBackground(
            Tessellator tess, float minPressure, float maxPressure,
            float dangerPressure, float minWorkingPressure,
            int xPos, int yPos, float zLevel
    ) {
        BufferBuilder builder = tess.getBuffer();
        builder.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        float[] color = RED;

        builder.pos(xPos, yPos, zLevel).color(color[0], color[1], color[2], color[3]).endVertex();

        int explodeBoundary = GAUGE_POINTS - (int) ((dangerPressure - minPressure) / (maxPressure - minPressure) * GAUGE_POINTS);
        int workingBoundary = GAUGE_POINTS - (int) ((minWorkingPressure - minPressure) / (maxPressure - minPressure) * GAUGE_POINTS);

        boolean changedColorGreen = false;
        boolean changedColorYellow = false;

        for (int i = 0; i < GAUGE_POINTS; i++) {
            if (i == explodeBoundary && !changedColorGreen) {
                color = minWorkingPressure < 0 && minWorkingPressure >= -1 ? YELLOW : GREEN;
                builder.pos(xPos, yPos, zLevel).color(color[0], color[1], color[2], color[3]).endVertex();
                i--;
                changedColorGreen = true;
            }
            if (i == workingBoundary && !changedColorYellow) {
                color = minWorkingPressure < 0 && minWorkingPressure >= -1 ? GREEN : YELLOW;
                builder.pos(xPos, yPos, zLevel).color(color[0], color[1], color[2], color[3]).endVertex();
                i--;
                changedColorYellow = true;
            }
            float angle = -i / (float) CIRCLE_POINTS * 2F * PI_F - STOP_ANGLE;
            builder.pos(MathHelper.cos(angle) * RADIUS + xPos, MathHelper.sin(angle) * RADIUS + yPos, zLevel)
                    .color(color[0], color[1], color[2], color[3]).endVertex();
        }
        tess.draw();
    }

    private static void drawGaugeSurround(
            Tessellator tess,
            int xPos, int yPos, float zLevel,
            int fgColor
    ) {
        BufferBuilder builder = tess.getBuffer();
        builder.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        int[] cols = RenderUtils.decomposeColor(fgColor);
        for (int i = 0; i < CIRCLE_POINTS; i++) {
            float angle = (float) i / (float) CIRCLE_POINTS * 2F * PI_F;
            builder.pos(MathHelper.cos(angle) * RADIUS + xPos, MathHelper.sin(angle) * RADIUS + yPos, zLevel)
                    .color(cols[1], cols[2], cols[3], cols[0]).endVertex();
        }
        tess.draw();
    }

    private static void drawScale(
            Tessellator tess,
            float minPressure, float maxPressure,
            int xPos, int yPos, float zLevel,
            int currentScale, List<TextScaler> textScalers
    ) {
        BufferBuilder builder = tess.getBuffer();
        builder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= GAUGE_POINTS; i++) {
            float angle = -i / (float) CIRCLE_POINTS * 2F * PI_F - STOP_ANGLE;
            if (i == GAUGE_POINTS - (int) ((currentScale - minPressure) / (maxPressure - minPressure) * GAUGE_POINTS)) {
                float x = MathHelper.cos(angle);
                float y = MathHelper.sin(angle);
                textScalers.add(new TextScaler(currentScale, (int) (x * RADIUS * 1.3F), (int) (y * RADIUS * 1.3F)));
                currentScale--;
                float r1 = maxPressure > 10 && textScalers.size() % 5 == 1 ? 0.8F : 0.92F;
                float r2 = maxPressure > 10 && textScalers.size() % 5 == 1 ? 1.15F : 1.08F;
                builder.pos(x * RADIUS * r1 + xPos, y * RADIUS * r1 + yPos, zLevel)
                        .color(0, 0, 0, 255).endVertex();
                builder.pos(x * RADIUS * r2 + xPos, y * RADIUS * r2 + yPos, zLevel)
                        .color(0, 0, 0, 255).endVertex();
            }
        }
        tess.draw();
    }

    private static void drawNeedle(
            Tessellator tess,
            int xPos, int yPos, float zLevel,
            float angle, int fgColor
    ) {
        BufferBuilder builder = tess.getBuffer();
        builder.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        int[] cols = RenderUtils.decomposeColor(fgColor);
        builder.pos(MathHelper.cos(angle + 0.89F * PI_F) * RADIUS * 0.3F + xPos, MathHelper.sin(angle + 0.89F * PI_F) * RADIUS * 0.3F + yPos, zLevel)
                .color(cols[1], cols[2], cols[3], cols[0]).endVertex();
        builder.pos(MathHelper.cos(angle + 1.11F * PI_F) * RADIUS * 0.3F + xPos, MathHelper.sin(angle + 1.11F * PI_F) * RADIUS * 0.3F + yPos, zLevel)
                .color(cols[1], cols[2], cols[3], cols[0]).endVertex();
        builder.pos(MathHelper.cos(angle) * RADIUS * 0.8F + xPos, MathHelper.sin(angle) * RADIUS * 0.8F + yPos, zLevel)
                .color(cols[1], cols[2], cols[3], cols[0]).endVertex();
        tess.draw();
    }

    private static void drawText(
            int xPos, int yPos, float zLevel,
            int fgColor, List<TextScaler> textScalers
    ) {
        for (int i = 0; i < textScalers.size(); i++) {
            if (textScalers.size() <= 14 || i % 5 == 0) {
                TextScaler scaler = textScalers.get(i);
                GlStateManager.pushMatrix();
                GlStateManager.translate(xPos + scaler.x() - 1.5, yPos + scaler.y() - 1.5, zLevel);
                GlStateManager.scale(0.5f, 0.5f, 1f);
                RenderUtils.renderString3d(Integer.toString(scaler.pressure()), 0, 0, fgColor, false);
                GlStateManager.popMatrix();
            }
        }
    }
}
