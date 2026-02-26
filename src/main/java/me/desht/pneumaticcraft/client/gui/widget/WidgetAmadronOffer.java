package me.desht.pneumaticcraft.client.gui.widget;

import me.desht.pneumaticcraft.PneumaticCraftRepressurized;
import me.desht.pneumaticcraft.common.recipes.AmadronOffer;
import me.desht.pneumaticcraft.common.recipes.AmadronOfferCustom;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.FluidStack;
import org.apache.commons.lang3.text.WordUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WidgetAmadronOffer extends WidgetBase {
    private final AmadronOffer offer;
    private final List<IGuiWidget> widgets = new ArrayList<>();
    private int shoppingAmount;
    private boolean canBuy;
    private final Rectangle[] tooltipRectangles = new Rectangle[2];
    private boolean renderBackground = true;

    public WidgetAmadronOffer(int id, int x, int y, AmadronOffer offer) {
        super(id, x, y, 73, 35);
        this.offer = offer;
        if (offer.getInput() instanceof FluidStack) {
            this.widgets.add(new WidgetFluidStack(0, x + 6, y + 15, (FluidStack) offer.getInput()));
        }
        if (offer.getOutput() instanceof FluidStack) {
            this.widgets.add(new WidgetFluidStack(0, x + 51, y + 15, (FluidStack) offer.getOutput()));
        }
        this.tooltipRectangles[0] = new Rectangle(x + 6, y + 15, 16, 16);
        this.tooltipRectangles[1] = new Rectangle(x + 51, y + 15, 16, 16);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTick) {
        if (this.renderBackground) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(Textures.WIDGET_AMADRON_OFFER);
            GlStateManager.color(1f, this.canBuy ? 1f : 0.4f, this.canBuy ? 1f : 0.4f, this.canBuy ? 0.75f : 1f);
            Gui.drawModalRectWithCustomSizedTexture(this.x, this.y, 0, 0, this.getBounds().width, this.getBounds().height, 256, 256);
        }
        for (IGuiWidget widget : this.widgets) {
            widget.render(mouseX, mouseY, partialTick);
        }
        Minecraft.getMinecraft().fontRenderer.drawString(this.offer.getVendor(), this.x + 2, this.y + 2, 0xFF000000);
        boolean customOffer = this.offer instanceof AmadronOfferCustom;
        if (this.shoppingAmount > 0) {
            Minecraft.getMinecraft().fontRenderer.drawString(TextFormatting.BLACK.toString() + this.shoppingAmount, this.x + 36 - Minecraft.getMinecraft().fontRenderer.getStringWidth("" + this.shoppingAmount) / 2, this.y + (customOffer ? 15 : 20), 0xFF000000);
        }
        if (customOffer) {
            AmadronOfferCustom custom = (AmadronOfferCustom) this.offer;
            Minecraft.getMinecraft().fontRenderer.drawString(TextFormatting.DARK_BLUE.toString() + custom.getStock(), this.x + 36 - Minecraft.getMinecraft().fontRenderer.getStringWidth("" + custom.getStock()) / 2, this.y + 25, 0xFF000000);
        }
    }

    public WidgetAmadronOffer setDrawBackground(boolean drawBackground) {
        this.renderBackground = drawBackground;
        return this;
    }

    public void setCanBuy(boolean canBuy) {
        this.canBuy = canBuy;
    }

    @Override
    public void addTooltip(int mouseX, int mouseY, List<String> curTip, boolean shiftPressed) {
        super.addTooltip(mouseX, mouseY, curTip, shiftPressed);
        for (IGuiWidget widget : this.widgets) {
            if (widget.getBounds().contains(mouseX, mouseY)) {
                widget.addTooltip(mouseX, mouseY, curTip, shiftPressed);
            }
        }
        boolean isInBounds = false;
        for (Rectangle rect : this.tooltipRectangles) {
            if (rect.contains(mouseX, mouseY)) {
                isInBounds = true;
            }
        }
        if (!isInBounds) {
            curTip.add(I18n.format("gui.amadron.amadronWidget.vendor", this.offer.getVendor()));
            curTip.add(I18n.format("gui.amadron.amadronWidget.selling", getStringForObject(this.offer.getOutput())));
            curTip.add(I18n.format("gui.amadron.amadronWidget.buying", getStringForObject(this.offer.getInput())));
            curTip.add(I18n.format("gui.amadron.amadronWidget.inBasket", getStringForObject(this.offer.getOutput(), this.shoppingAmount)));
            if (this.offer.getStock() >= 0)
                curTip.add(I18n.format("gui.amadron.amadronWidget.stock", this.offer.getStock()));
            if (this.offer.getVendor().equals(PneumaticCraftRepressurized.proxy.getClientPlayer().getName())) {
                curTip.addAll(Arrays.asList(WordUtils.wrap(I18n.format("gui.amadron.amadronWidget.sneakRightClickToRemove"), 40).split(System.getProperty("line.separator"))));
            }
        }
    }

    public static String getStringForObject(Object o) {
        return getStringForObject(o, 1);
    }

    public static String getStringForObject(Object o, int times) {
        if (o instanceof ItemStack) {
            ItemStack stack = (ItemStack) o;
            return times * stack.getCount() + "x " + stack.getDisplayName();
        } else {
            FluidStack stack = (FluidStack) o;
            return times * stack.amount + "mB " + stack.getLocalizedName();
        }
    }

    public AmadronOffer getOffer() {
        return this.offer;
    }

    public void setShoppingAmount(int amount) {
        this.shoppingAmount = amount;
    }
}
