package me.desht.pneumaticcraft.client.model.block;

import me.desht.pneumaticcraft.client.render.tileentity.AbstractModelRenderer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;

public class ModelDoorBase extends AbstractModelRenderer.BaseModel {
    private final ModelRenderer cylinder1;
    private final ModelRenderer cylinder2;
    private final ModelRenderer cylinder3;

    public ModelDoorBase() {
        this.textureWidth = 64;
        this.textureHeight = 64;

        this.cylinder1 = new ModelRenderer(this, 0, 28);
        this.cylinder1.addBox(0F, 0F, 0F, 3, 3, 10);
        this.cylinder1.setRotationPoint(2.5F, 8.5F, -6F);
        this.cylinder1.setTextureSize(64, 32);
        this.cylinder1.mirror = true;
        this.setRotation(this.cylinder1, 0F, 0F, 0F);
        this.cylinder2 = new ModelRenderer(this, 0, 28);
        this.cylinder2.addBox(0F, 0F, 0F, 2, 2, 10);
        this.cylinder2.setRotationPoint(3F, 9F, -6F);
        this.cylinder2.setTextureSize(64, 32);
        this.cylinder2.mirror = true;
        this.setRotation(this.cylinder2, 0F, 0F, 0F);
        this.cylinder3 = new ModelRenderer(this, 0, 28);
        this.cylinder3.addBox(0F, 0F, 0F, 1, 1, 10);
        this.cylinder3.setRotationPoint(3.5F, 9.5F, -6F);
        this.cylinder3.setTextureSize(64, 32);
        this.cylinder3.mirror = true;
        this.setRotation(this.cylinder3, 0F, 0F, 0F);
    }

    public void renderModel(float size, float progress, boolean rightGoing) {
        float cosinus = /*12 / 16F -*/(float) Math.sin(Math.toRadians((1 - progress) * 90)) * 12 / 16F;
        float sinus = 9 / 16F - (float) Math.cos(Math.toRadians((1 - progress) * 90)) * 9 / 16F;
        double extension = Math.sqrt(Math.pow(sinus, 2) + Math.pow(cosinus + 4 / 16F, 2));
        GlStateManager.translate(((rightGoing ? -4 : 0) + 2.5) / 16F, 0, -6 / 16F);
        double cylinderAngle = Math.toDegrees(Math.atan(sinus / (cosinus + 14 / 16F)));
        GlStateManager.rotate((float) cylinderAngle, 0f, rightGoing ? 1f : -1f, 0f);
        GlStateManager.translate(((rightGoing ? -3 : 0) - 2.5) / 16F, 0, 6 / 16F);
        double extensionPart = extension * 0.5D;
        this.cylinder1.render(size);
        GlStateManager.translate(0, 0, extensionPart);
        this.cylinder2.render(size);
        GlStateManager.translate(0, 0, extensionPart);
        this.cylinder3.render(size);
    }
}
