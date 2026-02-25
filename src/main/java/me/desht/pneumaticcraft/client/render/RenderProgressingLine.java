package me.desht.pneumaticcraft.client.render;

import me.desht.pneumaticcraft.client.gui.GuiSecurityStationBase;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.inventory.Slot;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

public class RenderProgressingLine {
    public double startX;
    public double startY;
    public double startZ;
    public double endX;
    public double endY;
    public double endZ;
    protected float progress = 0;

    public RenderProgressingLine() {
    }

    public RenderProgressingLine(double startX, double startY, double startZ, double endX, double endY, double endZ) {
        this.startX = startX;
        this.startY = startY;
        this.startZ = startZ;
        this.endX = endX;
        this.endY = endY;
        this.endZ = endZ;
    }

    public RenderProgressingLine(double startX, double startY, double endX, double endY) {
        this(startX, startY, 0, endX, endY, 0);
    }

    public RenderProgressingLine(RenderProgressingLine copy) {
        this(copy.startX, copy.startY, copy.startZ, copy.endX, copy.endY, copy.endZ);
        this.progress = copy.progress;
    }

    public boolean hasLineSameProperties(double startX, double startY, double startZ, double endX, double endY, double endZ) {
        return Math.abs(startX - this.startX) < 0.01D && Math.abs(startY - this.startY) < 0.01D && Math.abs(startZ - this.startZ) < 0.01D && Math.abs(endX - this.endX) < 0.01D && Math.abs(endY - this.endY) < 0.01D && Math.abs(endZ - this.endZ) < 0.01D;
    }

    public float getProgress() {
        return this.progress;
    }

    public RenderProgressingLine setProgress(float progress) {
        this.progress = progress;
        return this;
    }

    /**
     * Increases the progress by the given amount.
     *
     * @param increment
     * @return Returns true when the maximum progress has been reached.
     */
    public boolean incProgress(float increment) {
        this.progress += increment;
        if (this.progress > 1F) {
            this.progress = 1F;
            return true;
        } else if (this.progress < 0F) {
            this.progress = 0F;
            return true;
        }
        return false;
    }

    public boolean incProgressByDistance(double distance) {
        double totalDistance = Math.sqrt(Math.pow(this.endX - this.startX, 2) + Math.pow(this.endY - this.startY, 2) + Math.pow(this.endZ - this.startZ, 2));
        this.progress += distance / totalDistance;
        if (this.progress > 1F) {
            this.progress = 1F;
            return true;
        } else if (this.progress < 0F) {
            this.progress = 0F;
            return true;
        }
        return false;
    }

    @SideOnly(Side.CLIENT)
    public void render() {
        BufferBuilder wr = Tessellator.getInstance().getBuffer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        wr.pos(this.startX, this.startY, this.startZ).endVertex();
        wr.pos(this.startX + (this.endX - this.startX) * this.progress, this.startY + (this.endY - this.startY) * this.progress, this.startZ + (this.endZ - this.startZ) * this.progress).endVertex();
        Tessellator.getInstance().draw();
    }

    @SideOnly(Side.CLIENT)
    public void renderInterpolated(RenderProgressingLine lastTickLine, float partialTick) {
        BufferBuilder wr = Tessellator.getInstance().getBuffer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        wr.pos(this.getInter(this.startX, lastTickLine.startX, partialTick), this.getInter(this.startY, lastTickLine.startY, partialTick), this.getInter(this.startZ, lastTickLine.startZ, partialTick)).endVertex();
        wr.pos(this.getInter(this.startX, lastTickLine.startX, partialTick) + (this.getInter(this.endX, lastTickLine.endX, partialTick) - this.getInter(this.startX, lastTickLine.startX, partialTick)) * this.progress, this.getInter(this.startY, lastTickLine.startY, partialTick) + (this.getInter(this.startY, lastTickLine.startY, partialTick) - this.getInter(this.endY, lastTickLine.endY, partialTick)) * this.progress, this.getInter(this.startZ, lastTickLine.startZ, partialTick) + (this.getInter(this.endZ, lastTickLine.endZ, partialTick) - this.getInter(this.startZ, lastTickLine.startZ, partialTick)) * this.progress).endVertex();
        Tessellator.getInstance().draw();
    }

    protected double getInter(double cur, double old, float partialTick) {
        return old + (cur - old) * partialTick;
    }

    public int getPointedSlotNumber(GuiSecurityStationBase gui) {
        Slot slot = gui.getSlotAtPosition((int) this.endX, (int) this.endY);
        return slot != null ? slot.slotNumber : 0;
    }
}
