package me.desht.pneumaticcraft.client.gui.programmer;

import me.desht.pneumaticcraft.client.gui.GuiProgrammer;
import me.desht.pneumaticcraft.client.gui.widget.IGuiWidget;
import me.desht.pneumaticcraft.client.gui.widget.WidgetFluidFilter;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTextField;
import me.desht.pneumaticcraft.client.gui.widget.WidgetVerticalScrollbar;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetLiquidFilter;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GuiProgWidgetLiquidFilter extends GuiProgWidgetOptionBase<ProgWidgetLiquidFilter> {

    private static final int GRID_WIDTH = 8;
    private static final int GRID_HEIGHT = 6;
    private WidgetFluidFilter mainFilter;
    private WidgetTextField searchField;
    private WidgetVerticalScrollbar scrollbar;
    private int lastScroll;

    public GuiProgWidgetLiquidFilter(ProgWidgetLiquidFilter widget, GuiProgrammer guiProgrammer) {
        super(widget, guiProgrammer);
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    protected ResourceLocation getTexture() {
        return new ResourceLocation(Textures.GUI_ITEM_SEARCHER_LOCATION);
    }

    @Override
    public void initGui() {
        super.initGui();

        this.mainFilter = new WidgetFluidFilter(-1, this.guiLeft + 124, this.guiTop + 25).setFluid(this.widget.getFluid());
        this.addWidget(this.mainFilter);

        for (int x = 0; x < GRID_WIDTH; x++) {
            for (int y = 0; y < GRID_HEIGHT; y++) {
                this.addWidget(new WidgetFluidFilter(x + y * GRID_WIDTH, this.guiLeft + 8 + x * 18, this.guiTop + 52 + y * 18));
            }
        }

        this.searchField = new WidgetTextField(Minecraft.getMinecraft().fontRenderer, this.guiLeft + 10, this.guiTop + 30, 90, 10);
        this.addWidget(this.searchField);
        this.searchField.setFocused(true);

        this.scrollbar = new WidgetVerticalScrollbar(this.guiLeft + 155, this.guiTop + 47, 112);
        this.scrollbar.setListening(true);
        this.addWidget(this.scrollbar);

        this.addValidFluids();
    }

    private void addValidFluids() {

        List<Fluid> fluids = new ArrayList<>();

        for (Fluid fluid : FluidRegistry.getRegisteredFluids().values()) {
            if (fluid.getLocalizedName(new FluidStack(fluid, 1)).toLowerCase().contains(this.searchField.getText())) {
                fluids.add(fluid);
            }
        }
        fluids.sort(Comparator.comparing(Fluid::getName));

        this.scrollbar.setStates(Math.max(0, (fluids.size() - GRID_WIDTH * GRID_HEIGHT + GRID_WIDTH - 1) / GRID_WIDTH));

        int offset = this.scrollbar.getState() * GRID_WIDTH;
        for (IGuiWidget widget : this.widgets) {
            if (widget.getID() >= 0 && widget instanceof WidgetFluidFilter) {
                int idWithOffset = widget.getID() + offset;
                ((WidgetFluidFilter) widget).setFluid(idWithOffset >= 0 && idWithOffset < fluids.size() ? fluids.get(idWithOffset) : null);
            }
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (this.lastScroll != this.scrollbar.getState()) {
            this.lastScroll = this.scrollbar.getState();
            this.addValidFluids();
        }
    }

    @Override
    public void keyTyped(char key, int keyCode) throws IOException {
        super.keyTyped(key, keyCode);
        this.addValidFluids();
    }

    @Override
    public void actionPerformed(IGuiWidget widget) {
        if (widget == this.mainFilter) {
            ((WidgetFluidFilter) widget).setFluid(null);
        } else if (widget instanceof WidgetFluidFilter) {
            this.mainFilter.setFluid(((WidgetFluidFilter) widget).getFluid());
        }
        this.widget.setFluid(this.mainFilter.getFluid());
        super.actionPerformed(widget);
    }
}
