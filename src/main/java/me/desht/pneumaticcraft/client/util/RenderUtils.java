package me.desht.pneumaticcraft.client.util;

import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

public class RenderUtils {
    /**
     * Decompose a 32-bit color into ARGB 8-bit int values
     *
     * @param color color to decompose
     * @return 4-element array of ints
     */
    public static int[] decomposeColor(int color) {
        int[] res = new int[4];
        res[0] = color >> 24 & 0xff;
        res[1] = color >> 16 & 0xff;
        res[2] = color >> 8 & 0xff;
        res[3] = color & 0xff;
        return res;
    }

    public static float[] decomposeColorF(int color) {
        float[] res = new float[4];
        res[0] = (color >> 24 & 0xff) / 255f;
        res[1] = (color >> 16 & 0xff) / 255f;
        res[2] = (color >> 8 & 0xff) / 255f;
        res[3] = (color & 0xff) / 255f;
        return res;
    }

    public static void glColorHex(int color, float brightness) {
        float alpha = (color >> 24 & 255) / 255F;
        float div = 255F / brightness;
        float red = (color >> 16 & 255) / div;
        float green = (color >> 8 & 255) / div;
        float blue = (color & 255) / div;
        GlStateManager.color(red, green, blue, alpha);
    }

    public static void glColorHex(int color) {
        float alpha = (color >> 24 & 255) / 255F;
        float red = (color >> 16 & 255) / 255F;
        float green = (color >> 8 & 255) / 255F;
        float blue = (color & 255) / 255F;
        GlStateManager.color(red, green, blue, alpha);
    }

    public static void glColorHex(int color, int alpha) {
        glColorHex(color | alpha << 24);
    }

    public static void render3DArrow() {
        GlStateManager.disableTexture2D();
        double arrowTipLength = 0.2;
        double arrowTipRadius = 0.25;
        double baseLength = 0.3;
        double baseRadius = 0.15;

        BufferBuilder wr = Tessellator.getInstance().getBuffer();
        wr.begin(GL11.GL_POLYGON, DefaultVertexFormats.POSITION);
        for (int i = PneumaticCraftUtils.sin.length - 1; i >= 0; i--) {
            double sin = PneumaticCraftUtils.sin[i] * baseRadius;
            double cos = PneumaticCraftUtils.cos[i] * baseRadius;
            wr.pos(sin, 0, cos).endVertex();
        }
        Tessellator.getInstance().draw();
        wr.begin(GL11.GL_POLYGON, DefaultVertexFormats.POSITION);
        for (int i = PneumaticCraftUtils.sin.length - 1; i >= 0; i--) {
            double sin = PneumaticCraftUtils.sin[i] * arrowTipRadius;
            double cos = PneumaticCraftUtils.cos[i] * arrowTipRadius;
            wr.pos(sin, baseLength, cos).endVertex();
        }
        Tessellator.getInstance().draw();
        wr.begin(GL11.GL_QUAD_STRIP, DefaultVertexFormats.POSITION);
        for (int i = PneumaticCraftUtils.sin.length - 1; i >= 0; i--) {
            double sin = PneumaticCraftUtils.sin[i] * baseRadius;
            double cos = PneumaticCraftUtils.cos[i] * baseRadius;
            wr.pos(sin, 0, cos).endVertex();
            wr.pos(sin, baseLength, cos).endVertex();
        }
        Tessellator.getInstance().draw();

        wr.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
        wr.pos(0, baseLength + arrowTipLength, 0).endVertex();
        for (int i = 0; i < PneumaticCraftUtils.sin.length; i++) {
            double sin = PneumaticCraftUtils.sin[i] * arrowTipRadius;
            double cos = PneumaticCraftUtils.cos[i] * arrowTipRadius;
            wr.pos(sin, baseLength, cos).endVertex();
        }
        wr.pos(0, baseLength, arrowTipRadius).endVertex();
        Tessellator.getInstance().draw();
        GlStateManager.enableTexture2D();
    }

    public static void renderFrame(AxisAlignedBB aabb, double fw) {
        renderOffsetAABB(new AxisAlignedBB(aabb.minX + fw, aabb.minY - fw, aabb.minZ - fw, aabb.maxX - fw, aabb.minY + fw, aabb.minZ + fw));
        renderOffsetAABB(new AxisAlignedBB(aabb.minX + fw, aabb.maxY - fw, aabb.minZ - fw, aabb.maxX - fw, aabb.maxY + fw, aabb.minZ + fw));
        renderOffsetAABB(new AxisAlignedBB(aabb.minX + fw, aabb.minY - fw, aabb.maxZ - fw, aabb.maxX - fw, aabb.minY + fw, aabb.maxZ + fw));
        renderOffsetAABB(new AxisAlignedBB(aabb.minX + fw, aabb.maxY - fw, aabb.maxZ - fw, aabb.maxX - fw, aabb.maxY + fw, aabb.maxZ + fw));

        renderOffsetAABB(new AxisAlignedBB(aabb.minX - fw, aabb.minY - fw, aabb.minZ + fw, aabb.minX + fw, aabb.minY + fw, aabb.maxZ - fw));
        renderOffsetAABB(new AxisAlignedBB(aabb.minX - fw, aabb.maxY - fw, aabb.minZ + fw, aabb.minX + fw, aabb.maxY + fw, aabb.maxZ - fw));
        renderOffsetAABB(new AxisAlignedBB(aabb.maxX - fw, aabb.minY - fw, aabb.minZ + fw, aabb.maxX + fw, aabb.minY + fw, aabb.maxZ - fw));
        renderOffsetAABB(new AxisAlignedBB(aabb.maxX - fw, aabb.maxY - fw, aabb.minZ + fw, aabb.maxX + fw, aabb.maxY + fw, aabb.maxZ - fw));

        renderOffsetAABB(new AxisAlignedBB(aabb.minX - fw, aabb.minY - fw, aabb.minZ - fw, aabb.minX + fw, aabb.maxY + fw, aabb.minZ + fw));
        renderOffsetAABB(new AxisAlignedBB(aabb.maxX - fw, aabb.minY - fw, aabb.minZ - fw, aabb.maxX + fw, aabb.maxY + fw, aabb.minZ + fw));
        renderOffsetAABB(new AxisAlignedBB(aabb.minX - fw, aabb.minY - fw, aabb.maxZ - fw, aabb.minX + fw, aabb.maxY + fw, aabb.maxZ + fw));
        renderOffsetAABB(new AxisAlignedBB(aabb.maxX - fw, aabb.minY - fw, aabb.maxZ - fw, aabb.maxX + fw, aabb.maxY + fw, aabb.maxZ + fw));
    }

    private static void renderOffsetAABB(AxisAlignedBB aabb) {
        BufferBuilder wr = Tessellator.getInstance().getBuffer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_NORMAL);

        wr.pos(aabb.minX, aabb.maxY, aabb.minZ).normal(0, 0, -1).endVertex();
        wr.pos(aabb.maxX, aabb.maxY, aabb.minZ).normal(0, 0, -1).endVertex();
        wr.pos(aabb.maxX, aabb.minY, aabb.minZ).normal(0, 0, -1).endVertex();
        wr.pos(aabb.minX, aabb.minY, aabb.minZ).normal(0, 0, -1).endVertex();

        wr.pos(aabb.minX, aabb.minY, aabb.maxZ).normal(0, 0, 1).endVertex();
        wr.pos(aabb.maxX, aabb.minY, aabb.maxZ).normal(0, 0, 1).endVertex();
        wr.pos(aabb.maxX, aabb.maxY, aabb.maxZ).normal(0, 0, 1).endVertex();
        wr.pos(aabb.minX, aabb.maxY, aabb.maxZ).normal(0, 0, 1).endVertex();

        wr.pos(aabb.minX, aabb.minY, aabb.minZ).normal(0, -1, 0).endVertex();
        wr.pos(aabb.maxX, aabb.minY, aabb.minZ).normal(0, -1, 0).endVertex();
        wr.pos(aabb.maxX, aabb.minY, aabb.maxZ).normal(0, -1, 0).endVertex();
        wr.pos(aabb.minX, aabb.minY, aabb.maxZ).normal(0, -1, 0).endVertex();

        wr.pos(aabb.minX, aabb.maxY, aabb.maxZ).normal(0, 1, 0).endVertex();
        wr.pos(aabb.maxX, aabb.maxY, aabb.maxZ).normal(0, 1, 0).endVertex();
        wr.pos(aabb.maxX, aabb.maxY, aabb.minZ).normal(0, 1, 0).endVertex();
        wr.pos(aabb.minX, aabb.maxY, aabb.minZ).normal(0, 1, 0).endVertex();

        wr.pos(aabb.minX, aabb.minY, aabb.maxZ).normal(-1, 0, 0).endVertex();
        wr.pos(aabb.minX, aabb.maxY, aabb.maxZ).normal(-1, 0, 0).endVertex();
        wr.pos(aabb.minX, aabb.maxY, aabb.minZ).normal(-1, 0, 0).endVertex();
        wr.pos(aabb.minX, aabb.minY, aabb.minZ).normal(-1, 0, 0).endVertex();

        wr.pos(aabb.maxX, aabb.minY, aabb.minZ).normal(1, 0, 0).endVertex();
        wr.pos(aabb.maxX, aabb.maxY, aabb.minZ).normal(1, 0, 0).endVertex();
        wr.pos(aabb.maxX, aabb.maxY, aabb.maxZ).normal(1, 0, 0).endVertex();
        wr.pos(aabb.maxX, aabb.minY, aabb.maxZ).normal(1, 0, 0).endVertex();
        Tessellator.getInstance().draw();
    }

    /**
     * Rotates the render matrix dependant on the given metadata of a block. Used in many render methods.
     *
     * @param metadata block metadata
     * @return the angle (in degrees) of resulting rotation around the Y axis
     */
    public static float rotateMatrixByMetadata(int metadata) {
        EnumFacing facing = EnumFacing.byIndex(metadata & 7);
        return rotateMatrixForDirection(facing);
    }

    /**
     * Rotates the render matrix dependent on the rotation of a block. Used by many block entity render methods.
     *
     * @param facing block facing direction
     * @return the angle (in degrees) of resulting rotation around the Y axis
     */
    public static float rotateMatrixForDirection(EnumFacing facing) {
        float metaRotation = switch (facing) {
            case UP -> {
                GlStateManager.rotate(90, 1, 0, 0);
                GlStateManager.translate(0, -1, -1);
                yield 0;
            }
            case DOWN -> {
                GlStateManager.rotate(-90, 1, 0, 0);
                GlStateManager.translate(0, -1, 1);
                yield 0;
            }
            case NORTH -> 0;
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
        };
        GlStateManager.rotate(metaRotation, 0, 1, 0);
        return metaRotation;
    }

    public static void renderString3d(String str, float x, float y, int color, boolean dropShadow) {
        GlStateManager.disableLighting();
        float lastX = OpenGlHelper.lastBrightnessX;
        float lastY = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);

        FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        fr.drawString(str, x, y, color, dropShadow);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastX, lastY);
        GlStateManager.enableLighting();
    }
}
