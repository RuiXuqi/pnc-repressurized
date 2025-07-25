package me.desht.pneumaticcraft.client.gui;

import me.desht.pneumaticcraft.api.recipe.TemperatureRange;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTemperature;
import me.desht.pneumaticcraft.common.heat.HeatUtil;
import me.desht.pneumaticcraft.common.inventory.ContainerThermalCompressor;
import me.desht.pneumaticcraft.common.tileentity.TileEntityThermalCompressor;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.EnumFacing;

import java.awt.*;
import java.util.List;

public class GuiThermalCompressor extends GuiPneumaticContainerBase<TileEntityThermalCompressor> {
    private final WidgetTemperatureSided[] tempWidgets = new WidgetTemperatureSided[4];

    public GuiThermalCompressor(InventoryPlayer inv, TileEntityThermalCompressor te) {
        super(new ContainerThermalCompressor(inv, te), te, Textures.GUI_THERMAL_COMPRESSOR_LOCATION);
    }

    @Override
    public void initGui() {
        super.initGui();

        addWidget(tempWidgets[0] = new WidgetTemperatureSided(EnumFacing.NORTH, 55));
        addWidget(tempWidgets[1] = new WidgetTemperatureSided(EnumFacing.SOUTH, 65));

        addWidget(tempWidgets[2] = new WidgetTemperatureSided(EnumFacing.WEST, 88));
        addWidget(tempWidgets[3] = new WidgetTemperatureSided(EnumFacing.EAST, 98));
    }

    @Override
    protected Point getGaugeLocation() {
        int xStart = (width - xSize) / 2;
        int yStart = (height - ySize) / 2;
        return new Point(xStart + (int)(xSize * 0.82), yStart + ySize / 4 + 4);
    }

    private int getTemperatureDifferential(EnumFacing side) {
        return Math.abs(te.getHeatExchangerLogic(side).getTemperatureAsInt()
                - te.getHeatExchangerLogic(side.getOpposite()).getTemperatureAsInt());
    }

    @Override
    protected void addProblems(List<String> curInfo) {
        super.addProblems(curInfo);

        int d = getTemperatureDifferential(EnumFacing.NORTH) + getTemperatureDifferential(EnumFacing.EAST);
        if (d == 0) {
            curInfo.add("\u00a7fNo temperature differential");
            curInfo.addAll(PneumaticCraftUtils.convertStringIntoList("\u00a70Place a hot block on any side of the compressor, and a cold block on the opposite side."));
        }
    }

    @Override
    protected void addWarnings(List<String> curInfo) {
        super.addWarnings(curInfo);

        int d = getTemperatureDifferential(EnumFacing.NORTH) + getTemperatureDifferential(EnumFacing.EAST);
        if (d > 0 && d < 20) {
            curInfo.add("\u00a7fPoor temperature differential");
            curInfo.addAll(PneumaticCraftUtils.convertStringIntoList("\u00a70Place a hot block on any side of the compressor, and a cold block on the opposite side."));
        }
    }


    @Override
    public void updateScreen() {
        super.updateScreen();

        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (EnumFacing d : EnumFacing.HORIZONTALS) {
            int t = te.getHeatExchangerLogic(d).getTemperatureAsInt();
            tempWidgets[d.getHorizontalIndex()].setTemperature(t);
            min = Math.min(min, t);
            max = Math.max(max, t);
        }
        for (WidgetTemperatureSided temp : tempWidgets) {
            temp.setTotalRange(TemperatureRange.of(Math.max(0, min - 50), Math.min(2273, max + 50)));
            temp.autoScaleForTemperature();
        }
    }

    private class WidgetTemperatureSided extends WidgetTemperature {
        private final EnumFacing side;

        WidgetTemperatureSided(EnumFacing side, int x) {
            super(side.getHorizontalIndex(), guiLeft + x, guiTop + 20, TemperatureRange.of(0, 2000), 273, 200);
            this.side = side;
        }

        @Override
        public void addTooltip(int mouseX, int mouseY, List<String> curTip, boolean shift) {
            curTip.add(HeatUtil.formatHeatString(side, getTemperature()));
        }
    }
}
