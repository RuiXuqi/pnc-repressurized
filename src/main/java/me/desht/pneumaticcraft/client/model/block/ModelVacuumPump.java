package me.desht.pneumaticcraft.client.model.block;

import me.desht.pneumaticcraft.client.render.tileentity.AbstractModelRenderer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

public class ModelVacuumPump extends AbstractModelRenderer.BaseModel {
    private static final int BLADE_COUNT = 6;
    private final ModelRenderer blade;

    public ModelVacuumPump() {
        textureWidth = 32;
        textureHeight = 32;

        blade = new ModelRenderer(this, 24, 25);
        blade.addBox(0.0F, 0.0F, -1.0F, 1, 4, 3);
        blade.setRotationPoint(-0.5F, 14.0F, -3.0F);
        blade.mirror = true;
    }

    public void renderModel(float size, float rotation) {
        rotation++;
        GlStateManager.pushMatrix();
        GlStateManager.translate(0, -0.68f, 1f);
        GlStateManager.scale(0.8f, 0.8f, 0.8f);
        for (int i = 0; i < BLADE_COUNT; i++) {
            GlStateManager.pushMatrix();
            GlStateManager.rotate(rotation * 2 + (i + 0.5F) / BLADE_COUNT * 360, 0, 1, 0);
            GlStateManager.translate(0, 0, 1D / 16D);
            blade.render(size);
            GlStateManager.popMatrix();
        }
        GlStateManager.popMatrix();

        GlStateManager.rotate(180, 0, 1, 0);
    }
}
