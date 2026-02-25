package me.desht.pneumaticcraft.client.model.block;

import me.desht.pneumaticcraft.client.render.tileentity.AbstractModelRenderer;
import net.minecraft.client.model.ModelRenderer;

public class ModelDoor extends AbstractModelRenderer.BaseModel {
    private final ModelRenderer Shape1;
    private final ModelRenderer Shape2;
    private final ModelRenderer Shape3;
    private final ModelRenderer Shape4;
    private final ModelRenderer Shape5;
    private final ModelRenderer Shape6;
    private final ModelRenderer Shape7;
    private final ModelRenderer Shape8;
    private final ModelRenderer Shape9;

    public ModelDoor() {
        this.Shape1 = new ModelRenderer(this, 0, 24);
        this.Shape1.addBox(0F, 0F, 0F, 16, 3, 3);
        this.Shape1.setRotationPoint(-8F, -8F, -8F);
        this.Shape1.setTextureSize(64, 32);
        this.Shape1.mirror = true;
        this.setRotation(this.Shape1, 0F, 0F, 0F);
        this.Shape2 = new ModelRenderer(this, 38, 0);
        this.Shape2.addBox(0F, 0F, 0F, 3, 3, 3);
        this.Shape2.setRotationPoint(-8F, -5F, -8F);
        this.Shape2.setTextureSize(64, 32);
        this.Shape2.mirror = true;
        this.setRotation(this.Shape2, 0F, 0F, 0F);
        this.Shape3 = new ModelRenderer(this, 50, 0);
        this.Shape3.addBox(0F, 0F, 0F, 2, 3, 3);
        this.Shape3.setRotationPoint(-1F, -5F, -8F);
        this.Shape3.setTextureSize(64, 32);
        this.Shape3.mirror = true;
        this.setRotation(this.Shape3, 0F, 0F, 0F);
        this.Shape4 = new ModelRenderer(this, 38, 6);
        this.Shape4.addBox(0F, 0F, 0F, 3, 3, 3);
        this.Shape4.setRotationPoint(5F, -5F, -8F);
        this.Shape4.setTextureSize(64, 32);
        this.Shape4.mirror = true;
        this.setRotation(this.Shape4, 0F, 0F, 0F);
        this.Shape5 = new ModelRenderer(this, 0, 24);
        this.Shape5.addBox(0F, 0F, 0F, 16, 2, 3);
        this.Shape5.setRotationPoint(-8F, -2F, -8F);
        this.Shape5.setTextureSize(64, 32);
        this.Shape5.mirror = true;
        this.setRotation(this.Shape5, 0F, 0F, 0F);
        this.Shape6 = new ModelRenderer(this, 38, 12);
        this.Shape6.addBox(0F, 0F, 0F, 3, 3, 3);
        this.Shape6.setRotationPoint(-8F, 0F, -8F);
        this.Shape6.setTextureSize(64, 32);
        this.Shape6.mirror = true;
        this.setRotation(this.Shape6, 0F, 0F, 0F);
        this.Shape7 = new ModelRenderer(this, 50, 12);
        this.Shape7.addBox(0F, 0F, 0F, 2, 3, 3);
        this.Shape7.setRotationPoint(-1F, 0F, -8F);
        this.Shape7.setTextureSize(64, 32);
        this.Shape7.mirror = true;
        this.setRotation(this.Shape7, 0F, 0F, 0F);
        this.Shape8 = new ModelRenderer(this, 38, 18);
        this.Shape8.addBox(0F, 0F, 0F, 3, 3, 3);
        this.Shape8.setRotationPoint(5F, 0F, -8F);
        this.Shape8.setTextureSize(64, 32);
        this.Shape8.mirror = true;
        this.setRotation(this.Shape8, 0F, 0F, 0F);
        this.Shape9 = new ModelRenderer(this, 0, 0);
        this.Shape9.addBox(0F, 0F, 0F, 16, 21, 3);
        this.Shape9.setRotationPoint(-8F, 3F, -8F);
        this.Shape9.setTextureSize(64, 32);
        this.Shape9.mirror = true;
        this.setRotation(this.Shape9, 0F, 0F, 0F);
    }

    public void renderModel(float size) {
        this.Shape1.render(size);
        this.Shape2.render(size);
        this.Shape3.render(size);
        this.Shape4.render(size);
        this.Shape5.render(size);
        this.Shape6.render(size);
        this.Shape7.render(size);
        this.Shape8.render(size);
        this.Shape9.render(size);
    }
}
