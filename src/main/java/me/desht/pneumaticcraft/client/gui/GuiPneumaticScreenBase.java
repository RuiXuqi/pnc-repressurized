package me.desht.pneumaticcraft.client.gui;

import me.desht.pneumaticcraft.PneumaticCraftRepressurized;
import me.desht.pneumaticcraft.api.client.IGuiAnimatedStat;
import me.desht.pneumaticcraft.client.gui.widget.IGuiWidget;
import me.desht.pneumaticcraft.client.gui.widget.IWidgetListener;
import me.desht.pneumaticcraft.client.gui.widget.WidgetComboBox;
import me.desht.pneumaticcraft.client.gui.widget.WidgetLabel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.FMLClientHandler;
import org.apache.commons.lang3.text.WordUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public abstract class GuiPneumaticScreenBase extends GuiScreen implements IWidgetListener {

    protected final List<IGuiWidget> widgets = new ArrayList<>();
    public int guiLeft, guiTop, xSize, ySize;

    @Override
    public void initGui() {
        super.initGui();
        this.widgets.clear();
        this.guiLeft = this.width / 2 - this.xSize / 2;
        this.guiTop = this.height / 2 - this.ySize / 2;
    }

    public void addWidget(IGuiWidget widget) {
        this.widgets.add(widget);
        widget.setListener(this);
    }

    protected void addLabel(String text, int x, int y) {
        this.addWidget(new WidgetLabel(x, y, text));
    }

    protected void removeWidget(IGuiWidget widget) {
        this.widgets.remove(widget);
    }

    protected abstract ResourceLocation getTexture();

    @Override
    public void drawScreen(int x, int y, float partialTicks) {
        GlStateManager.color(1f, 1f, 1f, 1.0f);
        if (this.getTexture() != null) {
            FMLClientHandler.instance().getClient().getTextureManager().bindTexture(this.getTexture());
            this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
        }
        super.drawScreen(x, y, partialTicks);

        for (IGuiWidget widget : this.widgets) {
            widget.render(x, y, partialTicks);
        }
        for (IGuiWidget widget : this.widgets) {
            widget.postRender(x, y, partialTicks);
        }
        GlStateManager.enableTexture2D();
        GlStateManager.color(0.25f, 0.25f, 0.25f, 1.0f);

        List<String> tooltip = new ArrayList<>();
        for (Object obj : this.buttonList) {
            if (obj instanceof GuiButtonSpecial) {
                GuiButtonSpecial button = (GuiButtonSpecial) obj;
                if (button.x < x && button.x + button.getWidth() > x && button.y < y && button.y + button.getHeight() > y) {
                    button.getTooltip(tooltip);
                }
            }
        }
        boolean shift = PneumaticCraftRepressurized.proxy.isSneakingInGui();
        for (IGuiWidget widget : this.widgets) {
            if (widget.getBounds().contains(x, y)) widget.addTooltip(x, y, tooltip, shift);
        }
        if (!tooltip.isEmpty()) {
            List<String> localizedTooltip = new ArrayList<>();
            for (String line : tooltip) {
                String localizedLine = I18n.format(line);
                for (String wrappedLine : localizedLine.split("\\\\n")) {
                    String[] lines = WordUtils.wrap(wrappedLine, 50).split(System.getProperty("line.separator"));
                    localizedTooltip.addAll(Arrays.asList(lines));
                }
            }
            this.drawHoveringText(localizedTooltip, x, y, this.fontRenderer);
        }
        GlStateManager.color(0.25f, 0.25f, 0.25f, 1.0f);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        // new list creation necessary to avoid a comod exception
        LinkedList<IGuiWidget> l = new LinkedList<>();
        this.widgets.forEach(w -> {
            if (!(w instanceof WidgetComboBox && ((WidgetComboBox) w).isFocused())) {
                // ensure any focused combobox is added last
                l.addFirst(w);
            } else {
                l.add(w);
            }
        });

        l.forEach(widget -> {
            if (widget.getBounds().contains(mouseX, mouseY)) {
                widget.onMouseClicked(mouseX, mouseY, mouseButton);
            } else {
                widget.onMouseClickedOutsideBounds(mouseX, mouseY, mouseButton);
            }
        });
    }

    @Override
    protected void keyTyped(char key, int keyCode) throws IOException {
        if (keyCode == 1) {
            super.keyTyped(key, keyCode);
        } else {
            for (IGuiWidget widget : this.widgets) {
                widget.onKey(key, keyCode);
            }
        }
    }

    @Override
    public void actionPerformed(IGuiWidget widget) {
        if (widget instanceof IGuiAnimatedStat) {
            boolean leftSided = ((IGuiAnimatedStat) widget).isLeftSided();
            for (IGuiWidget w : this.widgets) {
                if (w instanceof IGuiAnimatedStat) {
                    IGuiAnimatedStat stat = (IGuiAnimatedStat) w;
                    if (widget != stat && stat.isLeftSided() == leftSided) {//when the stat is on the same side, close it.
                        stat.closeWindow();
                    }
                }
            }
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        for (IGuiWidget widget : this.widgets) {
            widget.update();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        for (IGuiWidget widget : this.widgets) {
            widget.handleMouseInput();
        }
    }

    @Override
    public void onKeyTyped(IGuiWidget widget) {
    }

    @Override
    public void setWorldAndResolution(Minecraft par1Minecraft, int par2, int par3) {
        this.widgets.clear();
        super.setWorldAndResolution(par1Minecraft, par2, par3);
    }
}
