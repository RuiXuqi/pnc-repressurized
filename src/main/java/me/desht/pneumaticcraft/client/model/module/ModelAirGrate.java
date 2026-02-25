package me.desht.pneumaticcraft.client.model.module;

import me.desht.pneumaticcraft.client.util.RenderUtils;
import me.desht.pneumaticcraft.common.block.tubes.ModuleAirGrate;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.util.ResourceLocation;

public class ModelAirGrate extends ModelModuleBase {
    private final ModelRenderer top;
    private final ModelRenderer side1;
    private final ModelRenderer side2;
    private final ModelRenderer side3;
    private final ModelRenderer side4;
    private final ModelRenderer base1;
    private final ModelRenderer base2;
    private final ModelRenderer base3;
    private final ModuleAirGrate grateModule;

    public ModelAirGrate(ModuleAirGrate grate) {
        this.grateModule = grate;
        this.textureWidth = 128;
        this.textureHeight = 128;

        this.top = new ModelRenderer(this, 42, 19);
        this.top.addBox(0F, 0F, 0F, 14, 0, 14);
        this.top.setRotationPoint(-7F, 9F, 8F);
        this.top.setTextureSize(128, 128);
        this.top.mirror = true;
        this.setRotation(this.top, -1.570796F, 0F, 0F);
        this.side1 = new ModelRenderer(this, 0, 18);
        this.side1.addBox(0F, 0F, 0F, 16, 1, 1);
        this.side1.setRotationPoint(-8F, 23F, 7F);
        this.side1.setTextureSize(128, 128);
        this.side1.mirror = true;
        this.setRotation(this.side1, 0F, 0F, 0F);
        this.side2 = new ModelRenderer(this, 0, 21);
        this.side2.addBox(0F, 0F, 0F, 16, 1, 1);
        this.side2.setRotationPoint(-8F, 8F, 7F);
        this.side2.setTextureSize(128, 128);
        this.side2.mirror = true;
        this.setRotation(this.side2, 0F, 0F, 0F);
        this.side3 = new ModelRenderer(this, 50, 0);
        this.side3.addBox(0F, 0F, 0F, 1, 1, 14);
        this.side3.setRotationPoint(-8F, 23F, 7F);
        this.side3.setTextureSize(128, 128);
        this.side3.mirror = true;
        this.setRotation(this.side3, 1.570796F, 0F, 0F);
        this.side4 = new ModelRenderer(this, 82, 0);
        this.side4.addBox(0F, 0F, 0F, 1, 1, 14);
        this.side4.setRotationPoint(7F, 23F, 7F);
        this.side4.setTextureSize(128, 128);
        this.side4.mirror = true;
        this.setRotation(this.side4, 1.570796F, 0F, 0F);
        this.base1 = new ModelRenderer(this, 69, 0);
        this.base1.addBox(0F, 0F, 0F, 6, 2, 6);
        this.base1.setRotationPoint(-3F, 13F, 4F);
        this.base1.setTextureSize(128, 128);
        this.base1.mirror = true;
        this.setRotation(this.base1, -1.570796F, 0F, 0F);
        this.base2 = new ModelRenderer(this, 0, 25);
        this.base2.addBox(0F, 0F, 0F, 12, 2, 12);
        this.base2.setRotationPoint(-6F, 10F, 6F);
        this.base2.setTextureSize(128, 128);
        this.base2.mirror = true;
        this.setRotation(this.base2, -1.570796F, 0F, 0F);
        this.base3 = new ModelRenderer(this, 0, 0);
        this.base3.addBox(2F, 0F, 0F, 16, 1, 16);
        this.base3.setRotationPoint(-10F, 8F, 7F);
        this.base3.setTextureSize(128, 128);
        this.base3.mirror = true;
        this.setRotation(this.base3, -1.570796F, 0F, 0F);
    }

    @Override
    protected void renderDynamic(float scale, float partialTicks) {
        if (this.grateModule != null && this.grateModule.isUpgraded()) RenderUtils.glColorHex(0xFFC0FF70);
        this.top.render(scale);
        this.side1.render(scale);
        this.side2.render(scale);
        this.side3.render(scale);
        this.side4.render(scale);
        this.base1.render(scale);
        this.base2.render(scale);
        this.base3.render(scale);
    }

    @Override
    protected ResourceLocation getTexture() {
        return Textures.MODEL_AIR_GRATE;
    }

}
