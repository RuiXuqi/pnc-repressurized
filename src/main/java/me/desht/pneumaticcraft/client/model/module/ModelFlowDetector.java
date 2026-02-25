package me.desht.pneumaticcraft.client.model.module;

import me.desht.pneumaticcraft.common.block.tubes.ModuleFlowDetector;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.util.ResourceLocation;

public class ModelFlowDetector extends ModelModuleBase {
    private final ModuleFlowDetector flowDetector;
    private final ModelRenderer shape1;

    public ModelFlowDetector(ModuleFlowDetector flowDetector) {
        this.textureWidth = 64;
        this.textureHeight = 32;

        this.shape1 = new ModelRenderer(this, 0, 8);
        this.shape1.addBox(-1F, -3F, -2F, 2, 1, 5);
        this.shape1.setRotationPoint(0F, 16F, 4.5F);
        this.shape1.setTextureSize(64, 32);
        this.shape1.mirror = true;
        this.setRotation(this.shape1, 0F, 0F, 0F);
        this.flowDetector = flowDetector;
    }

    @Override
    public void renderDynamic(float scale, float partialTicks) {
        int parts = 9;
        for (int i = 0; i < parts; i++) {
            this.shape1.rotateAngleZ = (float) i / parts * 2 * (float) Math.PI + (this.flowDetector != null ? this.flowDetector.oldRotation + (this.flowDetector.rotation - this.flowDetector.oldRotation) * partialTicks : 0);
            this.shape1.render(scale);
        }
    }

    @Override
    protected ResourceLocation getTexture() {
        return Textures.MODEL_FLOW_DETECTOR;
    }
}
