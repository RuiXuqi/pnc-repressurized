package me.desht.pneumaticcraft.client.model.block;

import me.desht.pneumaticcraft.client.render.tileentity.AbstractModelRenderer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;

public class ModelAirCannon extends AbstractModelRenderer.BaseModel {
    private final ModelRenderer baseTurn;
    private final ModelRenderer baseFrame1;
    private final ModelRenderer baseFrame2;
    private final ModelRenderer baseFrame3;
    private final ModelRenderer baseFrame4;
    private final ModelRenderer baseFrame5;
    private final ModelRenderer baseFrame6;
    private final ModelRenderer cannon1;
    private final ModelRenderer cannon2;
    private final ModelRenderer cannon3;
    private final ModelRenderer cannon4;
    private final ModelRenderer cannon5;

    public ModelAirCannon() {
        this.baseTurn = new ModelRenderer(this, 36, 7);
        this.baseTurn.addBox(0F, 0F, 0F, 7, 1, 7);
        this.baseTurn.setRotationPoint(-3.5F, 20F, -5F);
        this.baseTurn.setTextureSize(64, 32);
        this.baseTurn.mirror = true;
        this.setRotation(this.baseTurn, 0F, 0F, 0F);

        this.baseFrame1 = new ModelRenderer(this, 10, 7);
        this.baseFrame1.addBox(0F, 0F, 0F, 1, 5, 3);
        this.baseFrame1.setRotationPoint(-3.5F, 15F, -3F);
        this.baseFrame1.setTextureSize(64, 32);
        this.baseFrame1.mirror = true;
        this.setRotation(this.baseFrame1, 0F, 0F, 0F);
        this.baseFrame2 = new ModelRenderer(this, 10, 7);
        this.baseFrame2.addBox(0F, 0F, 0F, 1, 5, 3);
        this.baseFrame2.setRotationPoint(2.5F, 15F, -3F);
        this.baseFrame2.setTextureSize(64, 32);
        this.baseFrame2.mirror = true;
        this.setRotation(this.baseFrame2, 0F, 0F, 0F);
        this.baseFrame3 = new ModelRenderer(this, 18, 13);
        this.baseFrame3.addBox(0F, 0F, 0F, 1, 1, 1);
        this.baseFrame3.setRotationPoint(-3.5F, 14F, -2F);
        this.baseFrame3.setTextureSize(64, 32);
        this.baseFrame3.mirror = true;
        this.setRotation(this.baseFrame3, 0F, 0F, 0F);
        this.baseFrame4 = new ModelRenderer(this, 18, 13);
        this.baseFrame4.addBox(0F, 0F, 0F, 1, 1, 1);
        this.baseFrame4.setRotationPoint(2.5F, 14F, -2F);
        this.baseFrame4.setTextureSize(64, 32);
        this.baseFrame4.mirror = true;
        this.setRotation(this.baseFrame4, 0F, 0F, 0F);
        this.baseFrame5 = new ModelRenderer(this, 19, 10);
        this.baseFrame5.addBox(0F, 0F, 0F, 1, 1, 1);
        this.baseFrame5.setRotationPoint(2F, 15.5F, -2F);
        this.baseFrame5.setTextureSize(64, 32);
        this.baseFrame5.mirror = true;
        this.setRotation(this.baseFrame5, 0F, 0F, 0F);
        this.baseFrame6 = new ModelRenderer(this, 19, 10);
        this.baseFrame6.addBox(0F, 0F, 0F, 1, 1, 1);
        this.baseFrame6.setRotationPoint(-3F, 15.5F, -2F);
        this.baseFrame6.setTextureSize(64, 32);
        this.baseFrame6.mirror = true;
        this.setRotation(this.baseFrame6, 0F, 0F, 0F);

        this.cannon1 = new ModelRenderer(this, 24, 0);
        this.cannon1.addBox(0F, 3F, 0F, 2, 1, 2);
        this.cannon1.setRotationPoint(-1F, 15F, -2.5F);
        this.cannon1.setTextureSize(64, 32);
        this.cannon1.mirror = true;
        this.setRotation(this.cannon1, 0F, 0F, 0F);
        this.cannon2 = new ModelRenderer(this, 27, 3);
        this.cannon2.addBox(0F, 0F, 0F, 2, 8, 1);
        this.cannon2.setRotationPoint(-1F, 10F, -0.5F);
        this.cannon2.setTextureSize(64, 32);
        this.cannon2.mirror = true;
        this.setRotation(this.cannon2, 0F, 0F, 0F);
        this.cannon3 = new ModelRenderer(this, 27, 3);
        this.cannon3.addBox(0F, 0F, 0F, 2, 8, 1);
        this.cannon3.setRotationPoint(-1F, 10F, -3.5F);
        this.cannon3.setTextureSize(64, 32);
        this.cannon3.mirror = true;
        this.setRotation(this.cannon3, 0F, 0F, 0F);
        this.cannon4 = new ModelRenderer(this, 18, 0);
        this.cannon4.addBox(0F, 0F, 0F, 1, 8, 2);
        this.cannon4.setRotationPoint(-2F, 10F, -2.5F);
        this.cannon4.setTextureSize(64, 32);
        this.cannon4.mirror = true;
        this.setRotation(this.cannon4, 0F, 0F, 0F);
        this.cannon5 = new ModelRenderer(this, 18, 0);
        this.cannon5.addBox(0F, 0F, 0F, 1, 8, 2);
        this.cannon5.setRotationPoint(1F, 10F, -2.5F);
        this.cannon5.setTextureSize(64, 32);
        this.cannon5.mirror = true;
        this.setRotation(this.cannon5, 0F, 0F, 0F);
    }

//    @Override
//    public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
//        super.render(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
//
//        setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entityIn);
//        baseTurn.render(scale);
//        baseFrame1.render(scale);
//        baseFrame2.render(scale);
//        baseFrame3.render(scale);
//        baseFrame4.render(scale);
//        baseFrame5.render(scale);
//        baseFrame6.render(scale);
//        cannon1.render(scale);
//        cannon2.render(scale);
//        cannon3.render(scale);
//        cannon4.render(scale);
//        cannon5.render(scale);
//    }

    public void renderModel(float size, float rotationAngle, float heightAngle) {
        GlStateManager.pushMatrix();

        GlStateManager.translate(0.0, 0.0, -0.09375D);
        GlStateManager.rotate(rotationAngle, 0.0F, 1.0F, 0.0F);
        GlStateManager.translate(0.0, 0.0, 0.09375D);
        this.baseTurn.render(size);
        this.baseFrame1.render(size);
        this.baseFrame2.render(size);
        this.baseFrame3.render(size);
        this.baseFrame4.render(size);
        this.baseFrame5.render(size);
        this.baseFrame6.render(size);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0D, 1.0D, -0.09375D);
        GlStateManager.rotate(heightAngle, 1.0F, 0.0F, 0.0F);
        GlStateManager.translate(0.0D, -1.0D, 0.09375D);
        this.cannon1.render(size);
        this.cannon2.render(size);
        this.cannon3.render(size);
        this.cannon4.render(size);
        this.cannon5.render(size);
        GlStateManager.popMatrix();

        GlStateManager.popMatrix();
    }
}
