package me.desht.pneumaticcraft.client.model.module;

import me.desht.pneumaticcraft.client.render.pressure_gauge.PressureGaugeRenderer3D;
import me.desht.pneumaticcraft.client.util.ClientUtils;
import me.desht.pneumaticcraft.client.util.RenderUtils;
import me.desht.pneumaticcraft.common.block.tubes.ModulePressureGauge;
import me.desht.pneumaticcraft.common.tileentity.TileEntityPneumaticBase;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

public class ModelGauge extends ModelModuleBase {
    private static final float GAUGE_SCALE = 0.007f;

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
    }

    @Override
    protected void renderExtras(float scale, float partialTicks) {
        if (this.gaugeModule == null || this.gaugeModule.isFake()) return;

        BlockPos pos = this.gaugeModule.getTube().pos();
        if (ClientUtils.getClientPlayer().getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 256)
            return;

        GlStateManager.pushMatrix();

        float pressure = 0f;
        float critPressure = 7f;
        float dangerPressure = 5f;
        if (this.gaugeModule.getTube() instanceof TileEntityPneumaticBase base) {
            pressure = base.getPressure();
            critPressure = base.criticalPressure;
            dangerPressure = base.dangerPressure;
        }
        RenderUtils.rotateMatrixForDirection(this.gaugeModule.getDirection());
        GlStateManager.translate(0, 1.01, 0.378);
        GlStateManager.scale(GAUGE_SCALE, GAUGE_SCALE, GAUGE_SCALE);
        GlStateManager.rotate(180, 0, 1, 0);
        PressureGaugeRenderer3D.drawPressureGauge(-1, critPressure, dangerPressure, 0, pressure, 0, 0, 0, 0xFF000000);

        GlStateManager.popMatrix();
    }

    @Override
    protected ResourceLocation getTexture() {
        return Textures.MODEL_GAUGE;
    }
}
