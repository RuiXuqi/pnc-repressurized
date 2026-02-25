package me.desht.pneumaticcraft.client.render.pneumatic_armor;

import me.desht.pneumaticcraft.client.render.pneumatic_armor.upgrade_handler.CoordTrackUpgradeHandler;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.FMLClientHandler;
import org.lwjgl.opengl.GL11;

public class RenderNavigator {
    private final BlockPos targetPos;
    private Path path;
    private boolean increaseAlpha;
    private float alphaValue = 0.2F;

    public RenderNavigator(World world, BlockPos targetPos) {
        this.targetPos = targetPos;
        this.updatePath();
    }

    public void updatePath() {
        EntityPlayer player = FMLClientHandler.instance().getClient().player;
        this.path = PneumaticCraftUtils.getPathFinder().findPath(player.world, PneumaticCraftUtils.createDummyEntity(player), this.targetPos, CoordTrackUpgradeHandler.SEARCH_RANGE);
        // TODO: this just doesn't work anymore
        if (!this.tracedToDestination()) {
            this.path = CoordTrackUpgradeHandler.getDronePath(player, this.targetPos);
        }
    }

    public void render(boolean wirePath, boolean xRayEnabled, float partialTicks) {
        if (this.path == null) return;

        GlStateManager.depthMask(false);
        if (xRayEnabled) GlStateManager.disableDepth();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableTexture2D();
        GlStateManager.glLineWidth(5.0F);

        boolean hasDestinationPath = this.tracedToDestination();

        BufferBuilder wr = Tessellator.getInstance().getBuffer();

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, 0.01D, 0);

        // Draws just wires
        if (wirePath) {
            if (!hasDestinationPath) {
                GL11.glEnable(GL11.GL_LINE_STIPPLE);
                GL11.glLineStipple(2, (short) 0x00FF);
            }
            for (int i = 1; i < this.path.getCurrentPathLength(); i++) {
                float red = 1;
                if (this.path.getCurrentPathLength() - i < 200) {
                    red = (this.path.getCurrentPathLength() - i) * 0.005F;
                }
                GlStateManager.color(red, 1 - red, 0, 0.5F);
                PathPoint lastPoint = this.path.getPathPointFromIndex(i - 1);
                PathPoint pathPoint = this.path.getPathPointFromIndex(i);
                wr.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION);
                wr.pos(lastPoint.x + 0.5D, lastPoint.y, lastPoint.z + 0.5D).endVertex();
                wr.pos((lastPoint.x + pathPoint.x) / 2D + 0.5D, Math.max(lastPoint.y, pathPoint.y), (lastPoint.z + pathPoint.z) / 2D + 0.5D).endVertex();
                wr.pos(pathPoint.x + 0.5D, pathPoint.y, pathPoint.z + 0.5D).endVertex();
                Tessellator.getInstance().draw();
            }
        } else {
            if (hasDestinationPath) {
                if (this.alphaValue > 0.2F) this.alphaValue -= 0.005F;
            } else {
                if (this.increaseAlpha) {
                    this.alphaValue += 0.005F;
                    if (this.alphaValue > 0.3F) this.increaseAlpha = false;
                } else {
                    this.alphaValue -= 0.005F;
                    if (this.alphaValue < 0.2F) this.increaseAlpha = true;
                }
            }
            for (int i = 0; i < this.path.getCurrentPathLength(); i++) {
                float red = 1;
                if (this.path.getCurrentPathLength() - i < 200) {
                    red = (this.path.getCurrentPathLength() - i) * 0.005F;
                }
                GlStateManager.color(red, 1 - red, 0, this.alphaValue);
                PathPoint pathPoint = this.path.getPathPointFromIndex(i);
                wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
                wr.pos(pathPoint.x, pathPoint.y, pathPoint.z).endVertex();
                wr.pos(pathPoint.x, pathPoint.y, pathPoint.z + 1).endVertex();
                wr.pos(pathPoint.x + 1, pathPoint.y, pathPoint.z + 1).endVertex();
                wr.pos(pathPoint.x + 1, pathPoint.y, pathPoint.z).endVertex();
                Tessellator.getInstance().draw();
            }
        }

        GlStateManager.popMatrix();
        GlStateManager.enableCull();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GL11.glDisable(GL11.GL_LINE_STIPPLE);
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
    }

    public boolean tracedToDestination() {
        if (this.path == null) return false;
        PathPoint finalPoint = this.path.getFinalPathPoint();
        return finalPoint != null && this.targetPos.equals(new BlockPos(finalPoint.x, finalPoint.y, finalPoint.z));
    }
}
