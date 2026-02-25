package me.desht.pneumaticcraft.client.model.block;

import me.desht.pneumaticcraft.client.gui.GuiPneumaticContainerBase;
import me.desht.pneumaticcraft.client.render.tileentity.AbstractModelRenderer;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;

public class ModelAssemblyLaser extends AbstractModelRenderer.BaseModel {
    private final ModelRenderer baseTurn;
    private final ModelRenderer baseTurn2;
    private final ModelRenderer armBase1;
    private final ModelRenderer armBase2;
    private final ModelRenderer supportMiddle;
    private final ModelRenderer armMiddle1;
    private final ModelRenderer armMiddle2;
    private final ModelRenderer laserBase;
    private final ModelRenderer laser;

    public ModelAssemblyLaser() {
        this.textureWidth = 64;
        this.textureHeight = 64;

        this.baseTurn = new ModelRenderer(this, 0, 17);
        this.baseTurn.addBox(0F, 0F, 0F, 7, 1, 7);
        this.baseTurn.setRotationPoint(-3.5F, 22F, -3.5F);
        this.baseTurn.setTextureSize(64, 32);
        this.baseTurn.mirror = true;
        this.setRotation(this.baseTurn, 0F, 0F, 0F);
        this.baseTurn2 = new ModelRenderer(this, 28, 17);
        this.baseTurn2.addBox(0F, 0F, 0F, 4, 5, 4);
        this.baseTurn2.setRotationPoint(-2F, 17F, -2F);
        this.baseTurn2.setTextureSize(64, 32);
        this.baseTurn2.mirror = true;
        this.setRotation(this.baseTurn2, 0F, 0F, 0F);
        this.armBase1 = new ModelRenderer(this, 0, 25);
        this.armBase1.addBox(0F, 0F, 0F, 1, 2, 8);
        this.armBase1.setRotationPoint(2F, 17F, -1F);
        this.armBase1.setTextureSize(64, 32);
        this.armBase1.mirror = true;
        this.setRotation(this.armBase1, 0F, 0F, 0F);
        this.armBase2 = new ModelRenderer(this, 0, 25);
        this.armBase2.addBox(0F, 0F, 0F, 1, 2, 8);
        this.armBase2.setRotationPoint(-3F, 17F, -1F);
        this.armBase2.setTextureSize(64, 32);
        this.armBase2.mirror = true;
        this.setRotation(this.armBase2, 0F, 0F, 0F);
        this.supportMiddle = new ModelRenderer(this, 0, 57);
        this.supportMiddle.addBox(0F, 0F, 0F, 2, 1, 1);
        this.supportMiddle.setRotationPoint(-1F, 17.5F, 5.5F);
        this.supportMiddle.setTextureSize(64, 32);
        this.supportMiddle.mirror = true;
        this.setRotation(this.supportMiddle, 0F, 0F, 0F);
        this.armMiddle1 = new ModelRenderer(this, 0, 35);
        this.armMiddle1.addBox(0F, 0F, 0F, 1, 17, 2);
        this.armMiddle1.setRotationPoint(-2F, 2F, 5F);
        this.armMiddle1.setTextureSize(64, 32);
        this.armMiddle1.mirror = true;
        this.setRotation(this.armMiddle1, 0F, 0F, 0F);
        this.armMiddle2 = new ModelRenderer(this, 0, 35);
        this.armMiddle2.addBox(0F, 0F, 0F, 1, 17, 2);
        this.armMiddle2.setRotationPoint(1F, 2F, 5F);
        this.armMiddle2.setTextureSize(64, 32);
        this.armMiddle2.mirror = true;
        this.setRotation(this.armMiddle2, 0F, 0F, 0F);
        this.laserBase = new ModelRenderer(this, 8, 38);
        this.laserBase.addBox(0F, 0F, 0F, 2, 2, 3);
        this.laserBase.setRotationPoint(-1F, 2F, 4.5F);
        this.laserBase.setTextureSize(64, 32);
        this.laserBase.mirror = true;
        this.setRotation(this.laserBase, 0F, 0F, 0F);
        this.laser = new ModelRenderer(this, 54, 59);
        this.laser.addBox(0F, 0F, 0F, 1, 1, 32);
        this.laser.setRotationPoint(-0.5F, 2.5F, 1F);
        this.laser.setTextureSize(64, 32);
        this.laser.mirror = true;
        this.setRotation(this.laser, 0F, 0F, 0F);
    }

    public void renderModel(float size, float[] angles, boolean laserOn) {
        GlStateManager.pushMatrix();

        GlStateManager.rotate(angles[0], 0, 1, 0);
        this.baseTurn.render(size);
        this.baseTurn2.render(size);
        GlStateManager.translate(0, 18 / 16F, 0);
        GlStateManager.rotate(angles[1], 1, 0, 0);
        GlStateManager.translate(0, -18 / 16F, 0);
        this.armBase1.render(size);
        this.armBase2.render(size);
        this.supportMiddle.render(size);
        GlStateManager.translate(0, 18 / 16F, 6 / 16F);
        GlStateManager.rotate(angles[2], 1, 0, 0);
        GlStateManager.translate(0, -18 / 16F, -6 / 16F);
        this.armMiddle1.render(size);
        this.armMiddle2.render(size);
        GlStateManager.translate(0, 3 / 16F, 6 / 16F);
        GlStateManager.rotate(angles[3], 1, 0, 0);
        GlStateManager.translate(0, -3 / 16F, -6 / 16F);
        this.laserBase.render(size);
        if (laserOn) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0, 2.75 / 16D, 1 / 16D);
            GlStateManager.disableTexture2D();
            GlStateManager.color(1.0F, 0.1F, 0, 1);
            this.laser.render(size / 8);
            GlStateManager.popMatrix();
            GlStateManager.color(1, 1, 1, 1);
            GlStateManager.enableTexture2D();
        }
        double textSize = 1 / 150D;
        GlStateManager.scale(textSize, textSize, textSize);
        GlStateManager.rotate(-90, 1, 0, 0);
        GlStateManager.translate(0, 0, 18);
        GlStateManager.disableLighting();
        GuiPneumaticContainerBase.drawTexture(Textures.GUI_LASER_DANGER, -8, -65);
        GlStateManager.enableLighting();

        GlStateManager.popMatrix();
    }
}
