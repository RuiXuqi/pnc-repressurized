package me.desht.pneumaticcraft.client.model.entity;

import me.desht.pneumaticcraft.client.util.RenderUtils;
import me.desht.pneumaticcraft.common.entity.EntityProgrammableController;
import me.desht.pneumaticcraft.common.entity.living.EntityDrone;
import me.desht.pneumaticcraft.common.entity.living.EntityDroneBase;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.AxisAlignedBB;

public class ModelDrone extends ModelBase {
    //fields
    private final ModelRenderer Base;
    private final ModelRenderer Base2;
    private final ModelRenderer Base3;
    private final ModelRenderer Base4;
    private final ModelRenderer Base5;
    private final ModelRenderer Prop1Part1;
    private final ModelRenderer Prop1Part2;
    private final ModelRenderer Prop1Part3;
    private final ModelRenderer Prop2Part1;
    private final ModelRenderer Prop2Part2;
    private final ModelRenderer Prop2Part3;
    private final ModelRenderer Prop3Part1;
    private final ModelRenderer Prop3Part2;
    private final ModelRenderer Prop3Part3;
    private final ModelRenderer Prop4Part1;
    private final ModelRenderer Prop4Part2;
    private final ModelRenderer Prop4Part3;
    private final ModelRenderer Frame1;
    private final ModelRenderer Frame2;
    private final ModelRenderer LandingStand1;
    private final ModelRenderer LandingStand2;
    private final ModelRenderer LandingStand3;
    private final ModelRenderer LandingStand4;
    private final ModelRenderer LaserArm;
    private final ModelRenderer LaserSource;
    private final ModelDroneMinigun minigun = new ModelDroneMinigun();
    private boolean renderFrame = false;
    private int frameColor = 0;

    public ModelDrone(int frameColor) {
        this();
        this.renderFrame = true;
        this.frameColor = frameColor;
    }

    public ModelDrone() {
        this.textureWidth = 64;
        this.textureHeight = 32;

        this.Base = new ModelRenderer(this, 0, 0);
        this.Base.addBox(0F, 0F, 0F, 6, 6, 6);
        this.Base.setRotationPoint(-3F, 14F, -3F);
        this.Base.setTextureSize(64, 32);
        this.Base.mirror = true;
        this.setRotation(this.Base, 0F, 0F, 0F);
        this.Base2 = new ModelRenderer(this, 0, 12);
        this.Base2.addBox(0F, 0F, 0F, 4, 4, 1);
        this.Base2.setRotationPoint(-2F, 15F, -4F);
        this.Base2.setTextureSize(64, 32);
        this.Base2.mirror = true;
        this.setRotation(this.Base2, 0F, 0F, 0F);
        this.Base3 = new ModelRenderer(this, 0, 12);
        this.Base3.addBox(0F, 0F, 0F, 4, 4, 1);
        this.Base3.setRotationPoint(-2F, 15F, 3F);
        this.Base3.setTextureSize(64, 32);
        this.Base3.mirror = true;
        this.setRotation(this.Base3, 0F, 0F, 0F);
        this.Base4 = new ModelRenderer(this, 10, 12);
        this.Base4.addBox(0F, 0F, 0F, 1, 4, 4);
        this.Base4.setRotationPoint(3F, 15F, -2F);
        this.Base4.setTextureSize(64, 32);
        this.Base4.mirror = true;
        this.setRotation(this.Base4, 0F, 0F, 0F);
        this.Base5 = new ModelRenderer(this, 10, 12);
        this.Base5.addBox(0F, 0F, 0F, 1, 4, 4);
        this.Base5.setRotationPoint(-4F, 15F, -2F);
        this.Base5.setTextureSize(64, 32);
        this.Base5.mirror = true;
        this.setRotation(this.Base5, 0F, 0F, 0F);
        this.Prop1Part1 = new ModelRenderer(this, 0, 17);
        this.Prop1Part1.addBox(0.5F, 0F, -0.8F, 3, 1, 2);
        this.Prop1Part1.setRotationPoint(11.5F, 14F, 0F);
        this.Prop1Part1.setTextureSize(64, 32);
        this.Prop1Part1.mirror = true;
        this.setRotation(this.Prop1Part1, -0.3490659F, 0F, 0F);
        this.Prop1Part2 = new ModelRenderer(this, 0, 17);
        this.Prop1Part2.addBox(-3.5F, 0F, -1.2F, 3, 1, 2);
        this.Prop1Part2.setRotationPoint(11.5F, 14F, 0F);
        this.Prop1Part2.setTextureSize(64, 32);
        this.Prop1Part2.mirror = true;
        this.setRotation(this.Prop1Part2, 0.3490659F, 0F, 0F);
        this.Prop1Part3 = new ModelRenderer(this, 0, 20);
        this.Prop1Part3.addBox(-0.5F, 0F, -0.5F, 1, 2, 1);
        this.Prop1Part3.setRotationPoint(11.5F, 14F, 0F);
        this.Prop1Part3.setTextureSize(64, 32);
        this.Prop1Part3.mirror = true;
        this.setRotation(this.Prop1Part3, 0F, 0F, 0F);
        this.Prop2Part1 = new ModelRenderer(this, 0, 17);
        this.Prop2Part1.addBox(0.5F, 0F, -0.8F, 3, 1, 2);
        this.Prop2Part1.setRotationPoint(-11.5F, 14F, 0F);
        this.Prop2Part1.setTextureSize(64, 32);
        this.Prop2Part1.mirror = true;
        this.setRotation(this.Prop2Part1, -0.3490659F, 0F, 0F);
        this.Prop2Part2 = new ModelRenderer(this, 0, 17);
        this.Prop2Part2.addBox(-3.5F, 0F, -1.2F, 3, 1, 2);
        this.Prop2Part2.setRotationPoint(-11.5F, 14F, 0F);
        this.Prop2Part2.setTextureSize(64, 32);
        this.Prop2Part2.mirror = true;
        this.setRotation(this.Prop2Part2, 0.3490659F, 0F, 0F);
        this.Prop2Part3 = new ModelRenderer(this, 0, 20);
        this.Prop2Part3.addBox(-0.5F, 0F, -0.5F, 1, 2, 1);
        this.Prop2Part3.setRotationPoint(-11.5F, 14F, 0F);
        this.Prop2Part3.setTextureSize(64, 32);
        this.Prop2Part3.mirror = true;
        this.setRotation(this.Prop2Part3, 0F, 0F, 0F);
        this.Prop3Part1 = new ModelRenderer(this, 0, 17);
        this.Prop3Part1.addBox(-3.5F, 0F, -0.8F, 3, 1, 2);
        this.Prop3Part1.setRotationPoint(0F, 13.7F, -11.5F);
        this.Prop3Part1.setTextureSize(64, 32);
        this.Prop3Part1.mirror = true;
        this.setRotation(this.Prop3Part1, -0.3490659F, 0F, 0F);
        this.Prop3Part2 = new ModelRenderer(this, 0, 17);
        this.Prop3Part2.addBox(0.5F, 0F, -1.1F, 3, 1, 2);
        this.Prop3Part2.setRotationPoint(0F, 14F, -11.5F);
        this.Prop3Part2.setTextureSize(64, 32);
        this.Prop3Part2.mirror = true;
        this.setRotation(this.Prop3Part2, 0.3490659F, 0F, 0F);
        this.Prop3Part3 = new ModelRenderer(this, 0, 20);
        this.Prop3Part3.addBox(-0.5F, 0F, -0.5F, 1, 2, 1);
        this.Prop3Part3.setRotationPoint(0F, 14F, -11.5F);
        this.Prop3Part3.setTextureSize(64, 32);
        this.Prop3Part3.mirror = true;
        this.setRotation(this.Prop3Part3, 0F, 0F, 0F);
        this.Prop4Part1 = new ModelRenderer(this, 0, 17);
        this.Prop4Part1.addBox(-3.5F, 0F, -0.8F, 3, 1, 2);
        this.Prop4Part1.setRotationPoint(0F, 14F, 11.5F);
        this.Prop4Part1.setTextureSize(64, 32);
        this.Prop4Part1.mirror = true;
        this.setRotation(this.Prop4Part1, -0.3490659F, 0F, 0F);
        this.Prop4Part2 = new ModelRenderer(this, 0, 17);
        this.Prop4Part2.addBox(0.5F, 0F, -1.1F, 3, 1, 2);
        this.Prop4Part2.setRotationPoint(0F, 14F, 11.5F);
        this.Prop4Part2.setTextureSize(64, 32);
        this.Prop4Part2.mirror = true;
        this.setRotation(this.Prop4Part2, 0.3490659F, 0F, 0F);
        this.Prop4Part3 = new ModelRenderer(this, 0, 20);
        this.Prop4Part3.addBox(-0.5F, 0F, -0.5F, 1, 2, 1);
        this.Prop4Part3.setRotationPoint(0F, 14F, 11.5F);
        this.Prop4Part3.setTextureSize(64, 32);
        this.Prop4Part3.mirror = true;
        this.setRotation(this.Prop4Part3, 0F, 0F, 0F);
        this.Frame1 = new ModelRenderer(this, 0, 26);
        this.Frame1.addBox(0F, 0F, 0F, 26, 2, 2);
        this.Frame1.setRotationPoint(-13F, 16F, -1F);
        this.Frame1.setTextureSize(64, 32);
        this.Frame1.mirror = true;
        this.setRotation(this.Frame1, 0F, 0F, 0F);
        this.Frame2 = new ModelRenderer(this, 0, 0);
        this.Frame2.addBox(0F, 0F, 0F, 2, 2, 26);
        this.Frame2.setRotationPoint(-1F, 16F, -13F);
        this.Frame2.setTextureSize(64, 32);
        this.Frame2.mirror = true;
        this.setRotation(this.Frame2, 0F, 0F, 0F);
        this.LandingStand1 = new ModelRenderer(this, 30, 0);
        this.LandingStand1.addBox(-1F, 0F, -0.5F, 1, 6, 1);
        this.LandingStand1.setRotationPoint(-8F, 18F, 0F);
        this.LandingStand1.setTextureSize(64, 32);
        this.LandingStand1.mirror = true;
        this.setRotation(this.LandingStand1, 0F, 0F, 0F);
        this.LandingStand2 = new ModelRenderer(this, 30, 0);
        this.LandingStand2.addBox(0F, 0F, -0.5F, 1, 6, 1);
        this.LandingStand2.setRotationPoint(8F, 18F, 0F);
        this.LandingStand2.setTextureSize(64, 32);
        this.LandingStand2.mirror = true;
        this.setRotation(this.LandingStand2, 0F, 0F, 0F);
        this.LandingStand3 = new ModelRenderer(this, 30, 0);
        this.LandingStand3.addBox(-0.5F, 0F, -1F, 1, 6, 1);
        this.LandingStand3.setRotationPoint(0F, 18F, -8F);
        this.LandingStand3.setTextureSize(64, 32);
        this.LandingStand3.mirror = true;
        this.setRotation(this.LandingStand3, 0F, 0F, 0F);
        this.LandingStand4 = new ModelRenderer(this, 30, 0);
        this.LandingStand4.addBox(-0.5F, 0F, 0F, 1, 6, 1);
        this.LandingStand4.setRotationPoint(0F, 18F, 8F);
        this.LandingStand4.setTextureSize(64, 32);
        this.LandingStand4.mirror = true;
        this.setRotation(this.LandingStand4, 0F, 0F, 0F);
        this.LaserArm = new ModelRenderer(this, 56, 0);
        this.LaserArm.addBox(0F, 0F, 0F, 1, 2, 1);
        this.LaserArm.setRotationPoint(-0.5F, 20F, -0.5F);
        this.LaserArm.setTextureSize(64, 32);
        this.LaserArm.mirror = true;
        this.setRotation(this.LaserArm, 0F, 0F, 0F);
        this.LaserSource = new ModelRenderer(this, 56, 3);
        this.LaserSource.addBox(0F, 0F, 0F, 2, 2, 2);
        this.LaserSource.setRotationPoint(-1F, 22F, -1F);
        this.LaserSource.setTextureSize(64, 32);
        this.LaserSource.mirror = true;
        this.setRotation(this.LaserSource, 0F, 0F, 0F);

        this.LaserArm.offsetY = this.LaserSource.offsetY = -4.5F / 16;
    }

    @Override
    public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
        if (entity instanceof EntityProgrammableController) f5 /= 2F;
        super.render(entity, f, f1, f2, f3, f4, f5);
        this.setRotationAngles(f, f1, f2, f3, f4, f5, entity);
        if (entity != null) RenderUtils.glColorHex(0xFF000000 + ((EntityDroneBase) entity).getDroneColor());
        this.Base2.render(f5);
        this.Base3.render(f5);
        this.Base4.render(f5);
        this.Base5.render(f5);
        GlStateManager.color(1, 1, 1, 1);
        this.Base.render(f5);
        this.Prop1Part1.render(f5);
        this.Prop1Part2.render(f5);
        this.Prop1Part3.render(f5);
        this.Prop2Part1.render(f5);
        this.Prop2Part2.render(f5);
        this.Prop2Part3.render(f5);
        this.Prop3Part1.render(f5);
        this.Prop3Part2.render(f5);
        this.Prop3Part3.render(f5);
        this.Prop4Part1.render(f5);
        this.Prop4Part2.render(f5);
        this.Prop4Part3.render(f5);
        this.Frame1.render(f5);
        this.Frame2.render(f5);
        this.LandingStand1.render(f5);
        this.LandingStand2.render(f5);
        this.LandingStand3.render(f5);
        this.LandingStand4.render(f5);
        this.LaserArm.render(f5);
        this.LaserSource.render(f5);
        if (entity instanceof EntityDrone && ((EntityDrone) entity).hasMinigun())
            this.minigun.render(entity, f, f1, f2, f3, f4, f5);
        if (this.renderFrame) {
            GlStateManager.disableTexture2D();
            RenderUtils.glColorHex(this.frameColor);
            double s = 3 / 16D;
            double y = 17 / 16D;
            RenderUtils.renderFrame(new AxisAlignedBB(-s, y - s, -s, s, y + s, s), 1 / 32D);
            GlStateManager.enableTexture2D();
        }
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entity, float par2, float par3, float partialTicks) {
        EntityDroneBase drone = (EntityDroneBase) entity;
        float propRotation = drone.oldPropRotation + (drone.propRotation - drone.oldPropRotation) * partialTicks;
        this.Prop1Part1.rotateAngleY = propRotation;
        this.Prop1Part2.rotateAngleY = propRotation;
        this.Prop1Part3.rotateAngleY = propRotation;
        this.Prop2Part1.rotateAngleY = propRotation;
        this.Prop2Part2.rotateAngleY = propRotation;
        this.Prop2Part3.rotateAngleY = propRotation;
        this.Prop3Part1.rotateAngleY = -propRotation;
        this.Prop3Part2.rotateAngleY = -propRotation;
        this.Prop3Part3.rotateAngleY = -propRotation;
        this.Prop4Part1.rotateAngleY = -propRotation;
        this.Prop4Part2.rotateAngleY = -propRotation;
        this.Prop4Part3.rotateAngleY = -propRotation;

        float laserExtension = drone.oldLaserExtension + (drone.laserExtension - drone.oldLaserExtension) * partialTicks;
        laserExtension = (1F - laserExtension) * -4.5F / 16F;
        this.LaserArm.offsetY = this.LaserSource.offsetY = laserExtension;
    }

    private void setRotation(ModelRenderer model, float x, float y, float z) {
        model.rotateAngleX = x;
        model.rotateAngleY = y;
        model.rotateAngleZ = z;
    }

}
