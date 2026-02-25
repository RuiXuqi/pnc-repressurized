package me.desht.pneumaticcraft.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.IFluidTank;

public class WidgetFluidStack extends WidgetFluidFilter {
    private final IFluidTank tank;

    public WidgetFluidStack(int id, int x, int y, IFluidTank tank) {
        super(id, x, y);
        this.tank = tank;
    }

    public WidgetFluidStack(int id, int x, int y, FluidStack stack) {
        super(id, x, y);
        this.tank = new FluidTank(stack.amount);
        this.tank.fill(stack, true);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTick) {
        this.fluid = this.tank.getFluid() != null ? this.tank.getFluid().getFluid() : null;
        super.render(mouseX, mouseY, partialTick);
        if (this.fluid != null) {
            int fluidAmount = this.tank.getFluidAmount() / 1000;
            String s = fluidAmount + "B";
            if (fluidAmount > 1) {
                FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
                GlStateManager.translate(0, 0, 400);  // ensure amount is drawn in front of the fluid texture
                fr.drawString(s, this.x - fr.getStringWidth(s) + 17, this.y + 9, 0xFFFFFFFF, true);
                GlStateManager.translate(0, 0, -400);
            }
        }
    }
}
