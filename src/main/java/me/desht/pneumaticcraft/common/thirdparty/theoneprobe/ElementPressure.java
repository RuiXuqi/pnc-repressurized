package me.desht.pneumaticcraft.common.thirdparty.theoneprobe;

import io.netty.buffer.ByteBuf;
import mcjty.theoneprobe.api.IElement;
import me.desht.pneumaticcraft.api.tileentity.IAirHandler;
import me.desht.pneumaticcraft.api.tileentity.IPneumaticMachine;
import me.desht.pneumaticcraft.client.render.pressure_gauge.PressureGaugeRenderer2D;
import me.desht.pneumaticcraft.common.tileentity.IMinWorkingPressure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

public class ElementPressure implements IElement {
    private final float min;
    private final float pressure;
    private final float danger;
    private final float crit;

    private static final float SCALE = 0.7f;

    ElementPressure(IPneumaticMachine te) {
        this.min = te instanceof IMinWorkingPressure ? ((IMinWorkingPressure) te).getMinWorkingPressure() : 0;
        IAirHandler airHandler = te.getAirHandler(null);
        this.pressure = airHandler.getPressure();
        this.danger = airHandler.getDangerPressure();
        this.crit = airHandler.getCriticalPressure();
    }

    ElementPressure(ByteBuf byteBuf) {
        this.min = byteBuf.readFloat();
        this.pressure = byteBuf.readFloat();
        this.danger = byteBuf.readFloat();
        this.crit = byteBuf.readFloat();
    }

    @Override
    public void render(int x, int y) {
        GlStateManager.pushMatrix();
//        double scale = getWidth() / (GuiUtils.PRESSURE_GAUGE_RADIUS * 2.0);
        GlStateManager.scale(SCALE, SCALE, SCALE);
        int x1 = (int) ((x + (float) this.getWidth() / 2) / SCALE);
        int y1 = (int) ((y + (float) this.getHeight() / 2) / SCALE);
        PressureGaugeRenderer2D.drawPressureGauge(Minecraft.getMinecraft().fontRenderer, -1, this.crit, this.danger, this.min, this.pressure, x1, y1, 0, 0xFFC0C0C0);
        GlStateManager.popMatrix();
    }

    @Override
    public int getWidth() {
        return 40;
    }

    @Override
    public int getHeight() {
        return 40;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeFloat(this.min);
        buf.writeFloat(this.pressure);
        buf.writeFloat(this.danger);
        buf.writeFloat(this.crit);
    }

    @Override
    public int getID() {
        return TOPCallback.elementPressure;
    }
}
