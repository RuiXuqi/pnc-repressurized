package me.desht.pneumaticcraft.client.model.block;

import me.desht.pneumaticcraft.client.render.tileentity.AbstractModelRenderer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;

public class ModelElevatorBase extends AbstractModelRenderer.BaseModel {
    private final ModelRenderer pole1;
    private final ModelRenderer pole2;
    private final ModelRenderer pole3;
    private final ModelRenderer pole4;
    private final ModelRenderer floor;

    public ModelElevatorBase() {
        this.textureWidth = 64;
        this.textureHeight = 64;

        this.pole1 = new ModelRenderer(this, 0, 17);
        this.pole1.addBox(0F, 0F, 0F, 2, 14, 2);
        this.pole1.setRotationPoint(-1F, 9F, -1F);
        this.pole1.setTextureSize(64, 64);
        this.pole1.mirror = true;
        this.setRotation(this.pole1, 0F, 0F, 0F);
        this.pole2 = new ModelRenderer(this, 0, 17);
        this.pole2.addBox(0F, 0F, 0F, 4, 14, 4);
        this.pole2.setRotationPoint(-2F, 9F, -2F);
        this.pole2.setTextureSize(64, 64);
        this.pole2.mirror = true;
        this.setRotation(this.pole2, 0F, 0F, 0F);
        this.pole3 = new ModelRenderer(this, 0, 17);
        this.pole3.addBox(0F, 0F, 0F, 6, 14, 6);
        this.pole3.setRotationPoint(-3F, 9F, -3F);
        this.pole3.setTextureSize(64, 64);
        this.pole3.mirror = true;
        this.setRotation(this.pole3, 0F, 0F, 0F);
        this.pole4 = new ModelRenderer(this, 0, 17);
        this.pole4.addBox(0F, 0F, 0F, 8, 14, 8);
        this.pole4.setRotationPoint(-4F, 9F, -4F);
        this.pole4.setTextureSize(64, 64);
        this.pole4.mirror = true;
        this.setRotation(this.pole4, 0F, 0F, 0F);
        this.floor = new ModelRenderer(this, 0, 0);
        this.floor.addBox(0F, 0F, 0F, 16, 1, 16);
        this.floor.setRotationPoint(-8F, 8F, -8F);
        this.floor.setTextureSize(64, 64);
        this.floor.mirror = true;
        this.setRotation(this.floor, 0F, 0F, 0F);
    }

    private static final float FACTOR = 9F / 16;

    public void renderModel(float scale, float extension) {
        this.renderPole(this.pole4, 0, scale, extension);
        this.renderPole(this.pole3, 1, scale, extension);
        this.renderPole(this.pole2, 2, scale, extension);
        this.renderPole(this.pole1, 3, scale, extension);
        GlStateManager.color(1, 1, 1, 1);
        this.floor.render(scale);
    }

    private void renderPole(ModelRenderer pole, int idx, float scale, float extension) {
        GlStateManager.translate(0, -extension / 4, 0);
        GlStateManager.pushMatrix();
        GlStateManager.translate(0, FACTOR, 0);
        GlStateManager.scale(1, extension * 16 / 14 / 4, 1);
        GlStateManager.translate(0, -FACTOR, 0);
        GlStateManager.color(1 - idx * 0.15f, 1 - idx * 0.15f, 1 - idx * 0.15f, 1);
        pole.render(scale);
        GlStateManager.popMatrix();
    }
}
