package me.desht.pneumaticcraft.client.render.entity;

import me.desht.pneumaticcraft.client.model.entity.ModelDrone;
import me.desht.pneumaticcraft.common.entity.living.EntityDroneBase;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;

public class RenderDrone extends RenderLiving<EntityDroneBase> {
    public static final IRenderFactory<EntityDroneBase> REGULAR_FACTORY = manager -> new RenderDrone(manager, Textures.DRONE_ENTITY);
    public static final IRenderFactory<EntityDroneBase> PROGRAMMABLE_CONTROLLER_FACTORY = manager -> new RenderDrone(manager, Textures.DRONE_ENTITY, 0.25f);
    public static final IRenderFactory<EntityDroneBase> LOGISTICS_FACTORY = manager -> new RenderDrone(manager, Textures.LOGISTICS_DRONE_ENTITY);
    public static final IRenderFactory<EntityDroneBase> HARVESTING_FACTORY = manager -> new RenderDrone(manager, Textures.HARVESTING_DRONE_ENTITY);

    private final ResourceLocation texture;

    private final float scale;

    private RenderDrone(RenderManager manager, ResourceLocation texture, float scale) {
        super(manager, new ModelDrone(), 0f);

        this.scale = scale;
        this.texture = texture;
    }

    private RenderDrone(RenderManager manager, ResourceLocation texture) {
        this(manager,  texture, 0.35f);
    }

    private void renderDrone(EntityDroneBase drone, double x, double y, double z, float yaw, float partialTicks) {
        if (drone.getHealth() <= 0) return;

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, (float) z);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, 0.76F, 0);
        GlStateManager.scale(scale, -scale, -scale);
        bindEntityTexture(drone);
        mainModel.setLivingAnimations(drone, 0, 0, partialTicks);
        mainModel.render(drone, 0, 0, 0, 0, partialTicks, 1 / 16F);
        GlStateManager.popMatrix();

        drone.renderExtras(x, y, z, partialTicks);
        GlStateManager.popMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityDroneBase par1Entity) {
        return texture;
    }

    @Override
    public void doRender(EntityDroneBase drone, double par2, double par4, double par6, float par8, float par9) {
        renderDrone(drone, par2, par4, par6, par8, par9);
        renderName(drone, par2, par4, par6); //TODO 1.8 test (renaming)
    }

    @Override
    protected boolean canRenderName(EntityDroneBase drone) {
        return super.canRenderName(drone) && (drone.getAlwaysRenderNameTagForRender() || drone.hasCustomName() && drone == renderManager.pointedEntity);
    }
}
