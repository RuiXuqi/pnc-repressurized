package me.desht.pneumaticcraft.client.model.module;

import me.desht.pneumaticcraft.client.util.RenderUtils;
import me.desht.pneumaticcraft.common.block.tubes.ModuleRegulatorTube;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.util.ResourceLocation;

public class ModelPressureRegulator extends ModelModuleBase {
    private final ModelRenderer shape1;
    private final ModelRenderer valve;
    private final ModuleRegulatorTube module;

    public ModelPressureRegulator(ModuleRegulatorTube module) {
        this.module = module;
        this.textureWidth = 64;
        this.textureHeight = 32;

        this.shape1 = new ModelRenderer(this, 0, 0);
        this.shape1.addBox(0F, 0F, 0F, 7, 7, 7);
        this.shape1.setRotationPoint(-3.5F, 12.5F, -3F);
        this.shape1.setTextureSize(64, 32);
        this.shape1.mirror = true;
        this.setRotation(this.shape1, 0F, 0F, 0F);
        this.valve = new ModelRenderer(this, 0, 16);
        this.valve.addBox(0F, 0F, 0F, 4, 4, 4);
        this.valve.setRotationPoint(-2F, 14F, 4F);
        this.valve.setTextureSize(64, 32);
        this.valve.mirror = true;
        this.setRotation(this.valve, 0F, 0F, 0F);
    }

    @Override
    protected void renderDynamic(float scale, float partialTicks) {
        if (this.module.isUpgraded()) RenderUtils.glColorHex(0xFFC0FF70);
        this.shape1.render(scale);
        this.valve.render(scale);
    }

    @Override
    protected ResourceLocation getTexture() {
        return Textures.MODEL_REGULATOR_MODULE;
    }
}
