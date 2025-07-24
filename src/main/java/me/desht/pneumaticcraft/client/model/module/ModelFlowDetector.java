package me.desht.pneumaticcraft.client.model.module;

import me.desht.pneumaticcraft.common.block.tubes.ModuleFlowDetector;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.util.ResourceLocation;

public class ModelFlowDetector extends ModelModuleBase {
    private final ModuleFlowDetector flowDetector;
    private static final int TUBE_PARTS = 4;
    private final ModelRenderer face;

    public ModelFlowDetector(ModuleFlowDetector flowDetector){
        this.flowDetector = flowDetector;
        textureWidth = 32;
        textureHeight = 32;

        face = new ModelRenderer(this, 0, 0);
        face.addBox(-2.0F, -3.0F, -2.0F, 4, 1, 5);
        face.setRotationPoint(0.0F, 16.0F, 4.5F);
        face.mirror = true;
    }

    @Override
    public void renderDynamic(float scale, float partialTicks) {
        float rot = flowDetector != null ? flowDetector.oldRotation + (flowDetector.rotation - flowDetector.oldRotation) * partialTicks : 0f;
        for (int i = 0; i < TUBE_PARTS; i++) {
            face.rotateAngleZ = (float)i / TUBE_PARTS * 2 * (float)Math.PI + rot;
            face.render(scale);
        }
    }

    @Override
    protected ResourceLocation getTexture() {
        return Textures.MODEL_FLOW_DETECTOR;
    }
}
