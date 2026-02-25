package me.desht.pneumaticcraft.client.model.block;

import me.desht.pneumaticcraft.api.client.assemblymachine.IAssemblyRenderOverriding;
import me.desht.pneumaticcraft.client.GuiRegistry;
import me.desht.pneumaticcraft.client.render.tileentity.AbstractModelRenderer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemBlock;

public class ModelAssemblyIOUnit extends AbstractModelRenderer.BaseModel {
    private final ModelRenderer baseTurn;
    private final ModelRenderer baseTurn2;
    private final ModelRenderer armBase1;
    private final ModelRenderer armBase2;
    private final ModelRenderer supportMiddle;
    private final ModelRenderer armMiddle1;
    private final ModelRenderer armMiddle2;
    private final ModelRenderer clawBase;
    private final ModelRenderer clawAxil;
    private final ModelRenderer clawTurn;
    private final ModelRenderer claw1;
    private final ModelRenderer claw2;
    private RenderEntityItem customRenderer = null;

    public ModelAssemblyIOUnit() {
        this.textureWidth = 64;
        this.textureHeight = 64;

        this.baseTurn = new ModelRenderer(this, 0, 17);
        this.baseTurn.addBox(0F, 0F, 0F, 7, 1, 7);
        this.baseTurn.setRotationPoint(-3.5F, 22F, -3.5F);
        this.baseTurn.setTextureSize(64, 32);
        this.baseTurn.mirror = true;
        this.setRotation(this.baseTurn, 0F, 0F, 0F);
        this.baseTurn2 = new ModelRenderer(this, 28, 17);
        this.baseTurn2.addBox(0F, 0F, 0F, 4, 5, 4);
        this.baseTurn2.setRotationPoint(-2F, 17F, -2F);
        this.baseTurn2.setTextureSize(64, 32);
        this.baseTurn2.mirror = true;
        this.setRotation(this.baseTurn2, 0F, 0F, 0F);
        this.armBase1 = new ModelRenderer(this, 0, 25);
        this.armBase1.addBox(0F, 0F, 0F, 1, 2, 8);
        this.armBase1.setRotationPoint(2F, 17F, -1F);
        this.armBase1.setTextureSize(64, 32);
        this.armBase1.mirror = true;
        this.setRotation(this.armBase1, 0F, 0F, 0F);
        this.armBase2 = new ModelRenderer(this, 0, 25);
        this.armBase2.addBox(0F, 0F, 0F, 1, 2, 8);
        this.armBase2.setRotationPoint(-3F, 17F, -1F);
        this.armBase2.setTextureSize(64, 32);
        this.armBase2.mirror = true;
        this.setRotation(this.armBase2, 0F, 0F, 0F);
        this.supportMiddle = new ModelRenderer(this, 0, 57);
        this.supportMiddle.addBox(0F, 0F, 0F, 2, 1, 1);
        this.supportMiddle.setRotationPoint(-1F, 17.5F, 5.5F);
        this.supportMiddle.setTextureSize(64, 32);
        this.supportMiddle.mirror = true;
        this.setRotation(this.supportMiddle, 0F, 0F, 0F);
        this.armMiddle1 = new ModelRenderer(this, 0, 35);
        this.armMiddle1.addBox(0F, 0F, 0F, 1, 17, 2);
        this.armMiddle1.setRotationPoint(-2F, 2F, 5F);
        this.armMiddle1.setTextureSize(64, 32);
        this.armMiddle1.mirror = true;
        this.setRotation(this.armMiddle1, 0F, 0F, 0F);
        this.armMiddle2 = new ModelRenderer(this, 0, 35);
        this.armMiddle2.addBox(0F, 0F, 0F, 1, 17, 2);
        this.armMiddle2.setRotationPoint(1F, 2F, 5F);
        this.armMiddle2.setTextureSize(64, 32);
        this.armMiddle2.mirror = true;
        this.setRotation(this.armMiddle2, 0F, 0F, 0F);
        this.clawBase = new ModelRenderer(this, 8, 38);
        this.clawBase.addBox(0F, 0F, 0F, 2, 2, 3);
        this.clawBase.setRotationPoint(-1F, 2F, 4.5F);
        this.clawBase.setTextureSize(64, 32);
        this.clawBase.mirror = true;
        this.setRotation(this.clawBase, 0F, 0F, 0F);
        this.clawAxil = new ModelRenderer(this, 8, 45);
        this.clawAxil.addBox(0F, 0F, 0F, 1, 1, 1);
        this.clawAxil.setRotationPoint(-0.5F, 2.5F, 4F);
        this.clawAxil.setTextureSize(64, 32);
        this.clawAxil.mirror = true;
        this.setRotation(this.clawAxil, 0F, 0F, 0F);
        this.clawTurn = new ModelRenderer(this, 8, 49);
        this.clawTurn.addBox(0F, 0F, 0F, 4, 2, 1);
        this.clawTurn.setRotationPoint(-2F, 2F, 3F);
        this.clawTurn.setTextureSize(64, 32);
        this.clawTurn.mirror = true;
        this.setRotation(this.clawTurn, 0F, 0F, 0F);
        this.claw1 = new ModelRenderer(this, 8, 54);
        this.claw1.addBox(0F, 0F, 0F, 1, 2, 1);
        this.claw1.setRotationPoint(0F, 2F, 2F);
        this.claw1.setTextureSize(64, 32);
        this.claw1.mirror = true;
        this.setRotation(this.claw1, 0F, 0F, 0F);
        this.claw2 = new ModelRenderer(this, 8, 59);
        this.claw2.addBox(0F, 0F, 0F, 1, 2, 1);
        this.claw2.setRotationPoint(-1F, 2F, 2F);
        this.claw2.setTextureSize(64, 32);
        this.claw2.mirror = true;
        this.setRotation(this.claw2, 0F, 0F, 0F);
    }

    public void renderModel(float size, float[] angles, float clawProgress, EntityItem carriedItem) {
        float clawTrans;
        float scaleFactor = 0.7F;

        if (this.customRenderer == null) {
            this.customRenderer = new AbstractModelRenderer.NoBobItemRenderer();
        }

        IAssemblyRenderOverriding renderOverride = null;
        if (carriedItem != null) {
            renderOverride = GuiRegistry.renderOverrides.get(carriedItem.getItem().getItem().getRegistryName());
            if (renderOverride != null) {
                clawTrans = renderOverride.getIOUnitClawShift(carriedItem.getItem());
            } else {
                if (carriedItem.getItem().getItem() instanceof ItemBlock) {
                    clawTrans = 1.5F / 16F - clawProgress * 0.1F / 16F;
                } else {
                    clawTrans = 1.5F / 16F - clawProgress * 1.4F / 16F;
                }
            }
        } else {
            clawTrans = 1.5F / 16F - clawProgress * 1.5F / 16F;
        }

        GlStateManager.pushMatrix();

        GlStateManager.rotate(angles[0], 0, 1, 0);
        this.baseTurn.render(size);
        this.baseTurn2.render(size);
        GlStateManager.translate(0, 18 / 16F, 0);
        GlStateManager.rotate(angles[1], 1, 0, 0);
        GlStateManager.translate(0, -18 / 16F, 0);
        this.armBase1.render(size);
        this.armBase2.render(size);
        this.supportMiddle.render(size);
        GlStateManager.translate(0, 18 / 16F, 6 / 16F);
        GlStateManager.rotate(angles[2], 1, 0, 0);
        GlStateManager.translate(0, -18 / 16F, -6 / 16F);
        this.armMiddle1.render(size);
        this.armMiddle2.render(size);
        GlStateManager.translate(0, 3 / 16F, 6 / 16F);
        GlStateManager.rotate(angles[3], 1, 0, 0);
        GlStateManager.translate(0, -3 / 16F, -6 / 16F);
        this.clawBase.render(size);
        GlStateManager.translate(0, 3 / 16F, 0);
        GlStateManager.rotate(angles[4], 0, 0, 1);
        GlStateManager.translate(0, -3 / 16F, 0);
        this.clawAxil.render(size);
        this.clawTurn.render(size);

        GlStateManager.pushMatrix();
        GlStateManager.translate(clawTrans, 0, 0);
        this.claw1.render(size);
        GlStateManager.translate(-2 * clawTrans, 0, 0);
        this.claw2.render(size);
        GlStateManager.popMatrix();

        if (carriedItem != null) {
            if (renderOverride == null || renderOverride.applyRenderChangeIOUnit(carriedItem.getItem())) {
                GlStateManager.rotate(90, 1, 0, 0);
                double yOffset = carriedItem.getItem().getItem() instanceof ItemBlock ? 1.5 / 16D : 0.5 / 16D;
                GlStateManager.translate(0, yOffset - 0.2, -3 / 16D);
                GlStateManager.rotate(-90, 0, 1, 0);
                GlStateManager.scale(scaleFactor, scaleFactor, scaleFactor);
                this.customRenderer.doRender(carriedItem, 0, 0, 0, 0, 0);
            }
        }

        GlStateManager.popMatrix();
    }
}
