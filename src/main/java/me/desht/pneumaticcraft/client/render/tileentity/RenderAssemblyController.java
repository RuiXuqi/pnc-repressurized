package me.desht.pneumaticcraft.client.render.tileentity;

import me.desht.pneumaticcraft.client.gui.GuiPneumaticContainerBase;
import me.desht.pneumaticcraft.client.model.block.ModelAssemblyControllerScreen;
import me.desht.pneumaticcraft.client.util.RenderUtils;
import me.desht.pneumaticcraft.common.tileentity.TileEntityAssemblyController;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class RenderAssemblyController extends AbstractModelRenderer<TileEntityAssemblyController> {
    private static final float TEXT_SIZE = 0.007F;
    private final ModelAssemblyControllerScreen model;

    public RenderAssemblyController() {
        model = new ModelAssemblyControllerScreen();
    }

    @Override
    ResourceLocation getTexture(TileEntityAssemblyController te) {
        return Textures.MODEL_ASSEMBLY_CONTROLLER;
    }

    @Override
    void renderModel(TileEntityAssemblyController te, float partialTicks) {
        RenderUtils.rotateMatrixByMetadata(2);

        // have the screen face the player
        GlStateManager.rotate(180 + Minecraft.getMinecraft().getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);

        model.renderModel(0.0625f);

        // status text & possible problem icon
        GlStateManager.translate(-0.23D, 0.50D, -0.04D);
        GlStateManager.rotate(-34, 1, 0, 0);
        GlStateManager.scale(TEXT_SIZE, TEXT_SIZE, TEXT_SIZE);
        GlStateManager.disableLighting();
        Minecraft.getMinecraft().fontRenderer.drawString("> " + te.displayedText, 1, 4, 0xFF4ce568);
        if(te.hasProblem) {
            GuiPneumaticContainerBase.drawTexture(Textures.GUI_GREEN_PROBLEMS_TEXTURE, 0, 18);
        }
        GlStateManager.enableLighting();
    }
}
