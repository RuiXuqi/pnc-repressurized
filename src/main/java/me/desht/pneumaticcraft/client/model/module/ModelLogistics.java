package me.desht.pneumaticcraft.client.model.module;

import me.desht.pneumaticcraft.client.util.RenderUtils;
import me.desht.pneumaticcraft.common.block.tubes.ModuleLogistics;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemDye;
import net.minecraft.util.ResourceLocation;

public class ModelLogistics extends ModelModuleBase {
    private final ModelRenderer base2;
    private final ModelRenderer shape1;
    private final ModelRenderer shape2;
    private final ModelRenderer shape3;
    private final ModelRenderer shape4;
    private final ModelRenderer notPowered, powered, action, notEnoughAir;
    private final ModuleLogistics module;

    public ModelLogistics(ModuleLogistics module) {
        this.module = module;
        this.textureWidth = 128;
        this.textureHeight = 128;

        this.notPowered = new ModelRenderer(this, 72, 0);
        this.notPowered.addBox(0F, 0F, 0F, 6, 2, 6);
        this.notPowered.setRotationPoint(-3F, 13F, 4F);
        this.notPowered.setTextureSize(128, 128);
        this.notPowered.mirror = true;
        this.setRotation(this.notPowered, -1.570796F, 0F, 0F);
        this.powered = new ModelRenderer(this, 48, 0);
        this.powered.addBox(0F, 0F, 0F, 6, 2, 6);
        this.powered.setRotationPoint(-3F, 13F, 4F);
        this.powered.setTextureSize(128, 128);
        this.powered.mirror = true;
        this.setRotation(this.powered, -1.570796F, 0F, 0F);
        this.action = new ModelRenderer(this, 24, 0);
        this.action.addBox(0F, 0F, 0F, 6, 2, 6);
        this.action.setRotationPoint(-3F, 13F, 4F);
        this.action.setTextureSize(128, 128);
        this.action.mirror = true;
        this.setRotation(this.action, -1.570796F, 0F, 0F);
        this.notEnoughAir = new ModelRenderer(this, 0, 0);
        this.notEnoughAir.addBox(0F, 0F, 0F, 6, 2, 6);
        this.notEnoughAir.setRotationPoint(-3F, 13F, 4F);
        this.notEnoughAir.setTextureSize(128, 128);
        this.notEnoughAir.mirror = true;
        this.setRotation(this.notEnoughAir, -1.570796F, 0F, 0F);

        this.base2 = new ModelRenderer(this, 0, 25);
        this.base2.addBox(0F, 0F, 0F, 12, 2, 12);
        this.base2.setRotationPoint(-6F, 10F, 6F);
        this.base2.setTextureSize(128, 128);
        this.base2.mirror = true;
        this.setRotation(this.base2, -1.570796F, 0F, 0F);
        this.shape1 = new ModelRenderer(this, 0, 39);
        this.shape1.addBox(0F, 0F, 0F, 1, 13, 1);
        this.shape1.setRotationPoint(5.5F, 9.5F, 5.5F);
        this.shape1.setTextureSize(128, 128);
        this.shape1.mirror = true;
        this.setRotation(this.shape1, 0F, 0F, 0F);
        this.shape2 = new ModelRenderer(this, 4, 39);
        this.shape2.addBox(0F, 0F, 0F, 1, 13, 1);
        this.shape2.setRotationPoint(-6.5F, 9.5F, 5.5F);
        this.shape2.setTextureSize(128, 128);
        this.shape2.mirror = true;
        this.setRotation(this.shape2, 0F, 0F, 0F);
        this.shape3 = new ModelRenderer(this, 8, 39);
        this.shape3.addBox(0F, 0F, 0F, 11, 1, 1);
        this.shape3.setRotationPoint(-5.5F, 9.5F, 5.5F);
        this.shape3.setTextureSize(128, 128);
        this.shape3.mirror = true;
        this.setRotation(this.shape3, 0F, 0F, 0F);
        this.shape4 = new ModelRenderer(this, 8, 41);
        this.shape4.addBox(0F, 0F, 0F, 11, 1, 1);
        this.shape4.setRotationPoint(-5.5F, 21.5F, 5.5F);
        this.shape4.setTextureSize(128, 128);
        this.shape4.mirror = true;
        this.setRotation(this.shape4, 0F, 0F, 0F);
    }

    private void renderChannelColorFrame(float size) {
        RenderUtils.glColorHex(0xFF000000 | ItemDye.DYE_COLORS[this.module.getColorChannel()]);
        this.shape1.render(size);
        this.shape2.render(size);
        this.shape3.render(size);
        this.shape4.render(size);
        GlStateManager.color(1, 1, 1, 1);
    }

    @Override
    protected void renderDynamic(float scale, float partialTicks) {
        ModelRenderer base;
        if (this.module.getTicksSinceAction() >= 0) {
            base = this.action;
        } else if (this.module.getTicksSinceNotEnoughAir() >= 0) {
            base = this.notEnoughAir;
        } else {
            base = this.module.hasPower() ? this.powered : this.notPowered;
        }
        base.render(scale);
        this.base2.render(scale);
        this.renderChannelColorFrame(scale);
    }

    @Override
    protected ResourceLocation getTexture() {
        return Textures.MODEL_LOGISTICS_MODULE;
    }
}
