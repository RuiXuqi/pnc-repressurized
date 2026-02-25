package me.desht.pneumaticcraft.client.model.block;

import me.desht.pneumaticcraft.client.render.tileentity.AbstractModelRenderer;
import net.minecraft.client.model.ModelRenderer;

public class ModelAssemblyControllerScreen extends AbstractModelRenderer.BaseModel {
    private final ModelRenderer screen;

    public ModelAssemblyControllerScreen() {
        this.textureWidth = 64;
        this.textureHeight = 64;

        this.screen = new ModelRenderer(this, 33, 32);
        this.screen.addBox(0F, 0F, 0F, 10, 6, 1);
        this.screen.setRotationPoint(-5F, 8F, 1F);
        this.screen.setTextureSize(64, 32);
        this.screen.mirror = true;
        this.setRotation(this.screen, -0.5934119F, 0F, 0F);
    }

    public void renderModel(float scale) {
        this.screen.render(scale);
    }
}
