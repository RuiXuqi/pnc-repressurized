package me.desht.pneumaticcraft.client.model.module;

import me.desht.pneumaticcraft.client.util.GuiUtils;
import me.desht.pneumaticcraft.client.util.RenderUtils;
import me.desht.pneumaticcraft.common.block.tubes.ModulePressureGauge;
import me.desht.pneumaticcraft.common.tileentity.TileEntityPneumaticBase;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.FMLClientHandler;

public class ModelGauge extends ModelModuleBase {
    private final ModelRenderer shape1;
    private final ModelRenderer shape2;
    private final ModulePressureGauge gaugeModule;

    public ModelGauge(ModulePressureGauge gaugeModule) {
        this.textureWidth = 64;
        this.textureHeight = 32;

        this.shape1 = new ModelRenderer(this, 0, 0);
        this.shape1.addBox(0F, 0F, 0F, 3, 3, 3);
        this.shape1.setRotationPoint(-1.5F, 14.5F, 2F);
        this.shape1.setTextureSize(64, 32);
        this.shape1.mirror = true;
        this.setRotation(this.shape1, 0F, 0F, 0F);
        this.shape2 = new ModelRenderer(this, 0, 6);
        this.shape2.addBox(0F, 0F, 0F, 8, 8, 1);
        this.shape2.setRotationPoint(-4F, 12F, 5F);
        this.shape2.setTextureSize(64, 32);
        this.shape2.mirror = true;
        this.setRotation(this.shape2, 0F, 0F, 0F);
        this.gaugeModule = gaugeModule;
    }

    @Override
    protected void renderDynamic(float scale, float partialTicks) {
        if (this.gaugeModule != null && this.gaugeModule.isUpgraded()) RenderUtils.glColorHex(0xFFC0FF70);
        this.shape1.render(scale);
        this.shape2.render(scale);

        float pressure = 0f;
        float dangerPressure = 5f;
        float critPressure = 7f;
        if (this.gaugeModule != null && this.gaugeModule.getTube() instanceof TileEntityPneumaticBase) {
            TileEntityPneumaticBase base = (TileEntityPneumaticBase) this.gaugeModule.getTube();
            pressure = base.getPressure();
            critPressure = base.criticalPressure;
            dangerPressure = base.dangerPressure;
        }
        GlStateManager.translate(0, 1, 0.378);
        double widgetScale = 0.007D;
        GlStateManager.scale(widgetScale, widgetScale, widgetScale);
        GlStateManager.rotate(180, 0, 1, 0);
        GlStateManager.disableLighting();
        GuiUtils.drawPressureGauge(FMLClientHandler.instance().getClient().fontRenderer, -1, critPressure, dangerPressure, -1.001F, pressure, 0, 0, 0);
        GlStateManager.enableLighting();

    }

    @Override
    protected ResourceLocation getTexture() {
        return Textures.MODEL_GAUGE;
    }
}
