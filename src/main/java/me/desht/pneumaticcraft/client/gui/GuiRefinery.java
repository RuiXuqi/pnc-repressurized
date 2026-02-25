package me.desht.pneumaticcraft.client.gui;

import me.desht.pneumaticcraft.client.gui.widget.WidgetTank;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTemperature;
import me.desht.pneumaticcraft.common.heat.HeatUtil;
import me.desht.pneumaticcraft.common.inventory.ContainerRefinery;
import me.desht.pneumaticcraft.common.tileentity.TileEntityRefinery;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GuiRefinery extends GuiPneumaticContainerBase<TileEntityRefinery> {
    private List<TileEntityRefinery> refineries;
    private WidgetTemperature widgetTemperature;
    private int nExposedFaces;

    public GuiRefinery(InventoryPlayer player, TileEntityRefinery te) {
        super(new ContainerRefinery(player, te), te, Textures.GUI_REFINERY);
    }

    @Override
    public void initGui() {
        super.initGui();

        this.widgetTemperature = new WidgetTemperature(-1, this.guiLeft + 32, this.guiTop + 20, 273, 673, this.te.getHeatExchangerLogic(null)) {
            @Override
            public void addTooltip(int mouseX, int mouseY, List<String> curTip, boolean shift) {
                super.addTooltip(mouseX, mouseY, curTip, shift);
                if (GuiRefinery.this.te.minTemp > 0) {
                    TextFormatting tf = GuiRefinery.this.te.minTemp < GuiRefinery.this.te.getHeatExchangerLogic(null).getTemperatureAsInt() ? TextFormatting.GREEN : TextFormatting.GOLD;
                    curTip.add(tf + "Required Temperature: " + (GuiRefinery.this.te.minTemp - 273) + "\u00b0C");
                }
            }
        };
        this.addWidget(this.widgetTemperature);

        this.addWidget(new WidgetTank(-1, this.guiLeft + 8, this.guiTop + 13, this.te.getInputTank()));

        int x = this.guiLeft + 95;
        int y = this.guiTop + 17;
        this.addWidget(new WidgetTank(-1, x, y, this.te.getOutputTank()));

        // "te" always refers to the master refinery; the bottom block of the stack
        this.refineries = new ArrayList<>();
        this.refineries.add(this.te);
        TileEntityRefinery refinery = this.te;
        while (refinery.getTileCache()[EnumFacing.UP.ordinal()].getTileEntity() instanceof TileEntityRefinery) {
            refinery = (TileEntityRefinery) refinery.getTileCache()[EnumFacing.UP.ordinal()].getTileEntity();
            x += 20;
            y -= 4;
            if (this.refineries.size() < 4) this.addWidget(new WidgetTank(-1, x, y, refinery.getOutputTank()));
            this.refineries.add(refinery);
        }

        if (this.refineries.size() < 2 || this.refineries.size() > 4) {
            this.problemTab.openWindow();
        }

        this.nExposedFaces = HeatUtil.countExposedFaces(this.refineries);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        if (this.te.minTemp > 0) {
            this.widgetTemperature.setScales(this.te.minTemp);
        } else {
            this.widgetTemperature.setScales();
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float f, int x, int y) {
        super.drawGuiContainerBackgroundLayer(f, x, y);
        if (this.refineries.size() < 4) {
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            drawRect(this.guiLeft + 155, this.guiTop + 5, this.guiLeft + 171, this.guiTop + 69, 0x40FF0000);
            if (this.refineries.size() < 3) {
                drawRect(this.guiLeft + 135, this.guiTop + 9, this.guiLeft + 151, this.guiTop + 73, 0x40FF0000);
            }
            if (this.refineries.size() < 2) {
                drawRect(this.guiLeft + 115, this.guiTop + 13, this.guiLeft + 131, this.guiTop + 77, 0x40FF0000);
            }
            GlStateManager.disableBlend();
        }
    }

    @Override
    protected Point getInvNameOffset() {
        return new Point(-36, 0);
    }

    @Override
    protected Point getInvTextOffset() {
        return new Point(20, -1);
    }

    @Override
    public void addProblems(List<String> curInfo) {
        super.addProblems(curInfo);

        if (this.te.getHeatExchangerLogic(null).getTemperatureAsInt() < this.te.minTemp) {
            curInfo.add("gui.tab.problems.notEnoughHeat");
        }
        if (this.te.getInputTank().getFluidAmount() < 10) {
            curInfo.add("gui.tab.problems.refinery.noOil");
        }
        if (this.refineries.size() < 2) {
            curInfo.add("gui.tab.problems.refinery.notEnoughRefineries");
        } else if (this.refineries.size() > 4) {
            curInfo.add("gui.tab.problems.refinery.tooManyRefineries");
        }
    }

    @Override
    protected void addWarnings(List<String> curInfo) {
        super.addWarnings(curInfo);

        if (this.te.isBlocked()) {
            curInfo.add("gui.tab.problems.refinery.outputBlocked");
        }
        if (this.nExposedFaces > 0) {
            curInfo.add(I18n.format("gui.tab.problems.exposedFaces", this.nExposedFaces, this.refineries.size() * 6));
        }
    }

    @Override
    protected boolean shouldAddUpgradeTab() {
        return false;
    }
}
