package me.desht.pneumaticcraft.client.model.module;

import me.desht.pneumaticcraft.client.util.RenderUtils;
import me.desht.pneumaticcraft.common.block.tubes.ModuleCharging;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.util.ResourceLocation;

public class ModelCharging extends ModelModuleBase {
    private final ModelRenderer shape1;
    private final ModelRenderer shape2;
    private final ModelRenderer shape3;
    private final ModuleCharging chargingModule;

    public ModelCharging(ModuleCharging charging) {
        this.chargingModule = charging;
        this.textureWidth = 64;
        this.textureHeight = 32;

        this.shape1 = new ModelRenderer(this, 22, 0);
        this.shape1.addBox(0F, 0F, 0F, 2, 2, 2);
        this.shape1.setRotationPoint(1F, 15F, 8F);
        this.shape1.setTextureSize(64, 32);
        this.shape1.mirror = true;
        this.setRotation(this.shape1, 0F, 3.141593F, 0F);
        this.shape2 = new ModelRenderer(this, 12, 0);
        this.shape2.addBox(0F, 0F, 0F, 3, 3, 2);
        this.shape2.setRotationPoint(1.5F, 14.5F, 6F);
        this.shape2.setTextureSize(64, 32);
        this.shape2.mirror = true;
        this.setRotation(this.shape2, 0F, 3.141593F, 0F);
        this.shape3 = new ModelRenderer(this, 0, 0);
        this.shape3.addBox(0F, 0F, 0F, 4, 4, 2);
        this.shape3.setRotationPoint(2F, 14F, 4F);
        this.shape3.setTextureSize(64, 32);
        this.shape3.mirror = true;
        this.setRotation(this.shape3, 0F, 3.141593F, 0F);
    }

    @Override
    protected void renderDynamic(float scale, float partialTicks) {
        if (this.chargingModule.isUpgraded()) RenderUtils.glColorHex(0xFFC0FF70);
        this.shape1.render(scale);
        this.shape2.render(scale);
        this.shape3.render(scale);
    }

    @Override
    protected ResourceLocation getTexture() {
        return Textures.MODEL_CHARGING_MODULE;
    }
}
