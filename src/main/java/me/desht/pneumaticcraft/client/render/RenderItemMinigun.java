package me.desht.pneumaticcraft.client.render;

import me.desht.pneumaticcraft.client.model.entity.ModelMinigun;
import me.desht.pneumaticcraft.common.item.ItemMinigun;
import me.desht.pneumaticcraft.common.item.Itemss;
import me.desht.pneumaticcraft.common.minigun.Minigun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;

public class RenderItemMinigun extends TileEntityItemStackRenderer {
    private final ModelMinigun model = new ModelMinigun();

    @Override
    public void renderByItem(ItemStack stack, float partialTicks) {
        if (stack.getItem() == Itemss.MINIGUN && stack.hasTagCompound()) {
            EntityPlayer player = Minecraft.getMinecraft().player;
            int id = stack.getTagCompound().getInteger("owningPlayerId");
            Entity owningPlayer = Minecraft.getMinecraft().world.getEntityByID(id);
            if (owningPlayer instanceof EntityPlayer) {
                Minigun minigun = ((ItemMinigun) stack.getItem()).getMinigun(stack, (EntityPlayer) owningPlayer);
                GlStateManager.pushMatrix();
                if (Minecraft.getMinecraft().gameSettings.thirdPersonView != 0 || player.getEntityId() != owningPlayer.getEntityId()) {
                    // rendering our own gun in 3rd person, or rendering someone else's gun
                    GlStateManager.scale(1, -1, -1);
                    GlStateManager.rotate(75, 1, 0, 0);
                    GlStateManager.rotate(180, 0, 1, 0);
                    GlStateManager.translate(-0.5, -2, -0.3);
                } else if (Minecraft.getMinecraft().currentScreen instanceof GuiInventory) {
                    // our own gun in the rendered player model in inventory screen
                    GlStateManager.rotate(-180, 1, 0, 0);
                    GlStateManager.translate(0.5, -1, -0.5);
                } else {
                    // Hides minigun in first person if in offhand because it's not usable in offhand anyway
                    if(player.getHeldItemOffhand() == stack) {
                        GlStateManager.scale(0f, 0f, 0f);
                    }

                    // Shows minigun in main hand appropriate to which side is set as the main hand
                    else {
                        // our own gun in 1st person
                        GlStateManager.scale(1.5, 1.5, 1.5);
                        GlStateManager.rotate(180, 0, 0, 1);
                        if (Minecraft.getMinecraft().gameSettings.mainHand == EnumHandSide.RIGHT) {
                            GlStateManager.translate(-1, -1.7, 0.1);
                        } else {
                            GlStateManager.translate(0.4, -1.7, 0.1);
                        }
                    }
                }
                model.renderMinigun(minigun, 1 / 16F, partialTicks, false);
                GlStateManager.popMatrix();
            }
        }
    }
}
