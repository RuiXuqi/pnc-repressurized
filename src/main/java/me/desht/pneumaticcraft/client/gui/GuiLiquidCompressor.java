package me.desht.pneumaticcraft.client.gui;

import me.desht.pneumaticcraft.client.gui.widget.WidgetTank;
import me.desht.pneumaticcraft.common.PneumaticCraftAPIHandler;
import me.desht.pneumaticcraft.common.inventory.ContainerLiquidCompressor;
import me.desht.pneumaticcraft.common.tileentity.TileEntityLiquidCompressor;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.util.List;
import java.util.*;

import static me.desht.pneumaticcraft.common.fluid.Fluids.LPG;
import static me.desht.pneumaticcraft.common.fluid.Fluids.getBucketStack;

public class GuiLiquidCompressor extends GuiPneumaticContainerBase<TileEntityLiquidCompressor> {

    public GuiLiquidCompressor(InventoryPlayer player, TileEntityLiquidCompressor te) {
        super(new ContainerLiquidCompressor(player, te), te, Textures.GUI_LIQUID_COMPRESSOR);
    }

    public GuiLiquidCompressor(Container container, TileEntityLiquidCompressor te, String texture) {
        super(container, te, texture);
    }

    @Override
    public void initGui() {
        super.initGui();
        addWidget(new WidgetTank(-1, guiLeft + getFluidOffset(), guiTop + 15, te.getTank()));
        addAnimatedStat("gui.tab.liquidCompressor.fuel", getBucketStack(LPG), 0xFFB04000, true).setTextWithoutCuttingString(getAllFuels());
    }

    @Override
    protected void addPressureStatInfo(List<String> pressureStatText) {
        super.addPressureStatInfo(pressureStatText);
        pressureStatText.add(TextFormatting.BLACK + I18n.format("gui.tooltip.maxProduction",
                PneumaticCraftUtils.roundNumberTo(Math.round(te.getBaseProduction() * te.getEfficiency() * te.getSpeedMultiplierFromUpgrades() / 100),2)));
    }

    protected int getFluidOffset() {
        return 86;
    }

    @Override
    protected Point getInvNameOffset() {
        return new Point(0, -2);
    }

    @Override
    protected Point getGaugeLocation() {
        return getGaugeLocation(5, 0);
    }

    private List<String> getAllFuels() {
        List<String> fuels = new ArrayList<>();
        fuels.add(TextFormatting.AQUA + PneumaticCraftUtils.xlate("gui.liquidCompressor.fuelsHeader"));
        for (Map.Entry<String, Integer> map : sortByValue(PneumaticCraftAPIHandler.getInstance().liquidFuels).entrySet()) {
            String value = map.getValue() / 1000 + "";
            while (fontRenderer.getStringWidth(value) < 25) {
                value = value + " ";
            }
            Fluid fluid = FluidRegistry.getFluid(map.getKey());
            fuels.add(value + "| " + StringUtils.abbreviate(fluid.getLocalizedName(new FluidStack(fluid, 1)), 25));
        }
        return fuels;
    }

    private static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
        list.sort((o1, o2) -> -o1.getValue().compareTo(o2.getValue()));

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    @Override
    public void addProblems(List<String> curInfo) {
        super.addProblems(curInfo);
        IFluidHandler fluidHandler = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
        if (!te.isProducing && (fluidHandler == null || fluidHandler.getTankProperties()[0].getContents() == null)) {
            curInfo.add("gui.tab.problems.liquidCompressor.noFuel");
        }
    }
}
