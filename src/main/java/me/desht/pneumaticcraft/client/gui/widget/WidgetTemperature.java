package me.desht.pneumaticcraft.client.gui.widget;

import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.MathHelper;

import java.util.List;

public class WidgetTemperature extends WidgetBase {

    private int[] scales;
    protected final IHeatExchangerLogic logic;
    private final int minTemp, maxTemp;

    public WidgetTemperature(int id, int x, int y, int minTemp, int maxTemp, IHeatExchangerLogic logic, int... scales) {
        super(id, x, y, 13, 50);
        this.scales = scales;
        this.logic = logic;
        this.minTemp = minTemp;
        this.maxTemp = maxTemp - 273;
    }

    public void setScales(int... scales) {
        this.scales = scales;
    }

    public int[] getScales() {
        return this.scales;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTick) {
        GlStateManager.disableLighting();
        Minecraft.getMinecraft().getTextureManager().bindTexture(Textures.WIDGET_TEMPERATURE);
        GlStateManager.color(1, 1, 1, 1);
        Gui.drawModalRectWithCustomSizedTexture(this.x + 6, this.y, 6, 0, 7, 50, 18, 50);

        int barLength = (this.logic.getTemperatureAsInt() - this.minTemp) * 48 / this.maxTemp;
        barLength = MathHelper.clamp(barLength, 0, 48);
        Gui.drawModalRectWithCustomSizedTexture(this.x + 7, this.y + 1 + 48 - barLength, 13, 48 - barLength, 5, barLength, 18, 50);

        for (int scale : this.scales) {
            int scaleY = 48 - (scale - this.minTemp) * 48 / this.maxTemp;
            int v = scaleY < 0 ? 6 : (scaleY > 48 ? 12 : 0);
            Gui.drawModalRectWithCustomSizedTexture(this.x, this.y - 1 + MathHelper.clamp(scaleY, 0, 48), 0, v, 6, 6, 18, 50);
        }
    }

    @Override
    public void addTooltip(int mouseX, int mouseY, List<String> curTip, boolean shift) {
        curTip.add("Temperature: " + (this.logic.getTemperatureAsInt() - 273) + "\u00b0C");
    }
}
