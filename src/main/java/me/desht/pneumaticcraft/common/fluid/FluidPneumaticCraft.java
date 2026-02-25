package me.desht.pneumaticcraft.common.fluid;

import net.minecraftforge.fluids.Fluid;

import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.RL;

public class FluidPneumaticCraft extends Fluid {
    private Integer customColor = null;

    public FluidPneumaticCraft(String fluidName) {
        super(fluidName, RL("blocks/" + fluidName + "_still"), RL("blocks/" + fluidName + "_flow"));
    }

    @Override
    public int getColor() {
        return this.customColor == null ? super.getColor() : this.customColor;
    }

    public void setCustomColor(int color) {
        this.customColor = color;
    }
}
