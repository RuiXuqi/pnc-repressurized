package me.desht.pneumaticcraft.client.model.block;

import me.desht.pneumaticcraft.client.render.tileentity.AbstractModelRenderer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;

public class ModelUniversalSensor extends AbstractModelRenderer.BaseModel {
    private final ModelRenderer dish1;
    private final ModelRenderer dish2;
    private final ModelRenderer dish3;
    private final ModelRenderer dish4;
    private final ModelRenderer dish5;
    private final ModelRenderer dish6;

    public ModelUniversalSensor() {
        this.textureWidth = 64;
        this.textureHeight = 64;

        this.dish1 = new ModelRenderer(this, 0, 33);
        this.dish1.addBox(-2F, 0F, -2F, 4, 1, 4);
        this.dish1.setRotationPoint(0F, 16F, 0F);
        this.dish1.setTextureSize(64, 64);
        this.dish1.mirror = true;
        this.setRotation(this.dish1, 0F, 0F, 0F);
        this.dish2 = new ModelRenderer(this, 0, 38);
        this.dish2.addBox(-3F, -1F, 0F, 1, 8, 4);
        this.dish2.setRotationPoint(0F, 9F, -2F);
        this.dish2.setTextureSize(64, 64);
        this.dish2.mirror = true;
        this.setRotation(this.dish2, 0F, 0F, -0.2268928F);
        this.dish3 = new ModelRenderer(this, 0, 50);
        this.dish3.addBox(-3.8F, 0F, 0.8F, 1, 4, 4);
        this.dish3.setRotationPoint(0F, 8F, 0F);
        this.dish3.setTextureSize(64, 64);
        this.dish3.mirror = true;
        this.setRotation(this.dish3, 0.0698132F, 0.3839724F, -0.2268928F);
        this.dish4 = new ModelRenderer(this, 10, 50);
        this.dish4.addBox(-3.8F, 0F, -4.7F, 1, 4, 4);
        this.dish4.setRotationPoint(0F, 8F, 0F);
        this.dish4.setTextureSize(64, 64);
        this.dish4.mirror = true;
        this.setRotation(this.dish4, -0.0698132F, -0.3839724F, -0.2268928F);
        this.dish5 = new ModelRenderer(this, 0, 58);
        this.dish5.addBox(-2F, 0F, -0.5F, 6, 1, 1);
        this.dish5.setRotationPoint(0F, 12F, 0F);
        this.dish5.setTextureSize(64, 64);
        this.dish5.mirror = true;
        this.setRotation(this.dish5, 0F, 0F, -0.2268928F);
        this.dish6 = new ModelRenderer(this, 0, 60);
        this.dish6.addBox(3F, 0F, -1F, 1, 1, 2);
        this.dish6.setRotationPoint(0F, 10.2F, 0F);
        this.dish6.setTextureSize(64, 64);
        this.dish6.mirror = true;
        this.setRotation(this.dish6, 0F, 0F, 0F);
    }

    public void renderModel(float scale, float dishRotation) {
        GlStateManager.pushMatrix();
        GlStateManager.rotate(dishRotation, 0, 1, 0);
        this.dish1.render(scale);
        this.dish2.render(scale);
        this.dish3.render(scale);
        this.dish4.render(scale);
        this.dish5.render(scale);
        this.dish6.render(scale);
        GlStateManager.popMatrix();
    }
}
