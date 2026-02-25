package me.desht.pneumaticcraft.client.render;

import me.desht.pneumaticcraft.PneumaticCraftRepressurized;
import me.desht.pneumaticcraft.client.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RenderRangeLines {
    private final List<RenderProgressingLine> rangeLines = new ArrayList<>();
    private int rangeLinesTimer = 0;
    private final int color;
    private final BlockPos pos;

    public RenderRangeLines(int color) {
        this(color, null);
    }

    public RenderRangeLines(int color, BlockPos pos) {
        this.color = color;
        this.pos = pos;
    }

    public void resetRendering(double range) {
        this.rangeLinesTimer = 120;

        this.rangeLines.clear();
        double renderRange = range + 0.5D;
        for (int i = 0; i < range * 16 + 8; i++) {
            //Add the vertical lines of the walls
            this.rangeLines.add(new RenderProgressingLine(-renderRange + i / 8D, -renderRange + 1, -renderRange, -renderRange + i / 8D, renderRange + 1, -renderRange));
            this.rangeLines.add(new RenderProgressingLine(renderRange - i / 8D, -renderRange + 1, renderRange, renderRange - i / 8D, renderRange + 1, renderRange));
            this.rangeLines.add(new RenderProgressingLine(-renderRange, -renderRange + 1, renderRange - i / 8D, -renderRange, renderRange + 1, renderRange - i / 8D));
            this.rangeLines.add(new RenderProgressingLine(renderRange, -renderRange + 1, -renderRange + i / 8D, renderRange, renderRange + 1, -renderRange + i / 8D));

            //Add the horizontal lines of the walls
            this.rangeLines.add(new RenderProgressingLine(-renderRange, -renderRange + i / 8D + 1, -renderRange, -renderRange, -renderRange + i / 8D + 1, renderRange));
            this.rangeLines.add(new RenderProgressingLine(renderRange, -renderRange + i / 8D + 1, -renderRange, renderRange, -renderRange + i / 8D + 1, renderRange));
            this.rangeLines.add(new RenderProgressingLine(-renderRange, renderRange - i / 8D + 1, -renderRange, renderRange, renderRange - i / 8D + 1, -renderRange));
            this.rangeLines.add(new RenderProgressingLine(-renderRange, -renderRange + i / 8D + 1, renderRange, renderRange, -renderRange + i / 8D + 1, renderRange));

            //Add the roof and floor
            this.rangeLines.add(new RenderProgressingLine(renderRange - i / 8D, -renderRange + 1, -renderRange, renderRange - i / 8D, -renderRange + 1, renderRange));
            this.rangeLines.add(new RenderProgressingLine(renderRange - i / 8D, renderRange + 1, -renderRange, renderRange - i / 8D, renderRange + 1, renderRange));
            this.rangeLines.add(new RenderProgressingLine(-renderRange, -renderRange + 1, -renderRange + i / 8D, renderRange, -renderRange + 1, -renderRange + i / 8D));
            this.rangeLines.add(new RenderProgressingLine(-renderRange, renderRange + 1, -renderRange + i / 8D, renderRange, renderRange + 1, -renderRange + i / 8D));

        }
    }

    public boolean isIdle() {
        return this.rangeLines.size() <= 0;
    }

    public void update() {
        if (this.rangeLinesTimer > 0) {
            this.rangeLinesTimer--;
            for (RenderProgressingLine line : this.rangeLines) {
                if (line.getProgress() > 0.005F || Minecraft.getMinecraft().world.rand.nextInt(15) == 0) {
                    line.incProgress(0.025F);
                }
            }
        } else {
            Iterator<RenderProgressingLine> iterator = this.rangeLines.iterator();
            while (iterator.hasNext()) {
                RenderProgressingLine line = iterator.next();
                if (line.getProgress() > 0.005F) {
                    line.incProgress(0.025F);
                }
                if (Minecraft.getMinecraft().world.rand.nextInt(8) == 0) {
                    iterator.remove();
                }
            }
        }
    }

    public void render() {
        if (this.rangeLines.isEmpty()) return;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderUtils.glColorHex(this.color);
        EntityPlayer player = PneumaticCraftRepressurized.proxy.getClientPlayer();
        if (this.pos != null) {
            GlStateManager.translate(this.pos.getX() - player.posX + 0.5, this.pos.getY() - player.posY + 0.5, this.pos.getZ() - player.posZ + 0.5);
        }
        GlStateManager.glLineWidth(2.0F);
        for (RenderProgressingLine line : this.rangeLines) {
            line.render();
        }
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }
}
