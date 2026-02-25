package me.desht.pneumaticcraft.client.gui.semiblock;

import me.desht.pneumaticcraft.PneumaticCraftRepressurized;
import me.desht.pneumaticcraft.client.gui.GuiButtonSpecial;
import me.desht.pneumaticcraft.client.gui.GuiPneumaticContainerBase;
import me.desht.pneumaticcraft.client.gui.GuiSearcher;
import me.desht.pneumaticcraft.client.gui.widget.*;
import me.desht.pneumaticcraft.common.config.ConfigHandler;
import me.desht.pneumaticcraft.common.inventory.ContainerLogistics;
import me.desht.pneumaticcraft.common.inventory.SlotPhantom;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketSetLogisticsFilterStack;
import me.desht.pneumaticcraft.common.network.PacketSetLogisticsFluidFilterStack;
import me.desht.pneumaticcraft.common.semiblock.SemiBlockLogistics;
import me.desht.pneumaticcraft.common.semiblock.SemiBlockManager;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import org.apache.commons.lang3.text.WordUtils;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.util.Arrays;

public class GuiLogisticsBase<Logistics extends SemiBlockLogistics> extends GuiPneumaticContainerBase {
    protected final Logistics logistics;
    private GuiSearcher searchGui;
    private GuiLogisticsLiquidFilter fluidSearchGui;
    private int editingSlot; //either fluid or item search.
    private GuiCheckBox invisible;
    private GuiCheckBox fuzzyMeta;
    private GuiCheckBox fuzzyNBT;
    private GuiCheckBox whitelist;
    private final GuiButtonSpecial[] facingButtons = new GuiButtonSpecial[6];
    private GuiAnimatedStat facingTab;

    public GuiLogisticsBase(InventoryPlayer invPlayer, Logistics logistics) {
        super(new ContainerLogistics(invPlayer, logistics), null, Textures.GUI_LOGISTICS_REQUESTER);
        this.logistics = (Logistics) ((ContainerLogistics) this.inventorySlots).logistics;
        this.ySize = 216;
    }

    @Override
    public void initGui() {
        super.initGui();

        if (this.searchGui != null) {
            this.inventorySlots.getSlot(this.editingSlot).putStack(this.searchGui.getSearchStack());
            NetworkHandler.sendToServer(new PacketSetLogisticsFilterStack(this.logistics, this.searchGui.getSearchStack(), this.editingSlot));
            this.searchGui = null;
        }
        if (this.fluidSearchGui != null && this.fluidSearchGui.getFilter() != null) {
            FluidStack filter = new FluidStack(this.fluidSearchGui.getFilter(), 1000);
            this.logistics.setFilter(this.editingSlot, filter);
            NetworkHandler.sendToServer(new PacketSetLogisticsFluidFilterStack(this.logistics, filter, this.editingSlot));
            this.fluidSearchGui = null;
        }
        String invisibleText = I18n.format("gui.logistic_frame.invisible");
        this.addWidget(this.invisible = new GuiCheckBox(9, this.guiLeft + this.xSize - 15 - this.fontRenderer.getStringWidth(invisibleText), this.guiTop + 6, 0xFF404040, invisibleText));
        this.invisible.setTooltip(Arrays.asList(WordUtils.wrap(I18n.format("gui.logistic_frame.invisible.tooltip"), 40).split(System.getProperty("line.separator"))));

        this.addWidget(new WidgetLabel(this.guiLeft + 8, this.guiTop + 18, I18n.format(String.format("gui.%s.filters", SemiBlockManager.getKeyForSemiBlock(this.logistics)))));
        this.addWidget(new WidgetLabel(this.guiLeft + 8, this.guiTop + 90, I18n.format("gui.logistic_frame.liquid")));
        for (int i = 0; i < 9; i++) {
            this.addWidget(new WidgetFluidStack(i, this.guiLeft + i * 18 + 8, this.guiTop + 101, this.logistics.getTankFilter(i)));
        }

        this.addInfoTab(I18n.format("gui.tab.info." + SemiBlockManager.getKeyForSemiBlock(this.logistics)));
        this.addFilterTab();
        if (!((ContainerLogistics) this.inventorySlots).isItemContainer()) {
            this.addFacingTab();
        }
    }

    private void addFilterTab() {
        GuiAnimatedStat filterTab = this.addAnimatedStat("gui.logistic_frame.filter_settings", new ItemStack(Blocks.WEB), 0xFF106010, false);
        filterTab.addPadding(this.logistics.supportsBlacklisting() ? 6 : 4, 26);
        this.fuzzyMeta = new GuiCheckBox(10, 5, 20, 0xFFFFFFFF, I18n.format("gui.logistic_frame.fuzzyMeta"));
        filterTab.addWidget(this.fuzzyMeta);
        this.fuzzyNBT = new GuiCheckBox(11, 5, 36, 0xFFFFFFFF, I18n.format("gui.logistic_frame.fuzzyNBT"));
        filterTab.addWidget(this.fuzzyNBT);
        if (this.logistics.supportsBlacklisting()) {
            this.whitelist = new GuiCheckBox(12, 5, 52, 0xFFFFFFFF, I18n.format("gui.logistic_frame.whitelist"));
            filterTab.addWidget(this.whitelist);
        }
    }

    private void addFacingTab() {
        this.facingTab = this.addAnimatedStat("", new ItemStack(Items.MAP), 0xFFC0C0C0, false);
        this.facingTab.addPadding(8, 18);
        this.facingTab.addWidget(this.facingButtons[0] = new GuiButtonSpecial(13, 15, 62, 20, 20, "D"));
        this.facingTab.addWidget(this.facingButtons[1] = new GuiButtonSpecial(14, 15, 20, 20, 20, "U"));
        this.facingTab.addWidget(this.facingButtons[2] = new GuiButtonSpecial(15, 36, 20, 20, 20, "N"));
        this.facingTab.addWidget(this.facingButtons[3] = new GuiButtonSpecial(16, 36, 62, 20, 20, "S"));
        this.facingTab.addWidget(this.facingButtons[4] = new GuiButtonSpecial(17, 15, 41, 20, 20, "W"));
        this.facingTab.addWidget(this.facingButtons[5] = new GuiButtonSpecial(18, 57, 41, 20, 20, "E"));
        GuiButtonSpecial info = new GuiButtonSpecial(19, 36, 41, 20, 20, "");
        info.setVisible(false);
        info.setTooltipText(PneumaticCraftUtils.convertStringIntoList(I18n.format("gui.logistic_frame.facing.tooltip")));
        info.setRenderedIcon(Textures.GUI_INFO_LOCATION);
        this.facingTab.addWidget(info);
    }

    @Override
    protected int getBackgroundTint() {
        if (!ConfigHandler.client.logisticsGUITint) return super.getBackgroundTint();

        int c = this.logistics.getColor();
        // desaturate; this is a background colour...
        float[] hsb = Color.RGBtoHSB((c & 0xFF0000) >> 16, (c & 0xFF00) >> 8, c & 0xFF, null);
        Color color = Color.getHSBColor(hsb[0], hsb[1] * 0.2f, hsb[2]);
        if (hsb[2] < 0.7) color = color.brighter();
        return color.getRGB();
    }

    @Override
    protected boolean shouldAddProblemTab() {
        return false;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        this.invisible.checked = this.logistics.isInvisible();
        this.fuzzyMeta.checked = this.logistics.isFuzzyMeta();
        this.fuzzyNBT.checked = this.logistics.isFuzzyNBT();
        if (this.logistics.supportsBlacklisting())
            this.whitelist.checked = this.logistics.isWhitelist();
        String s = this.logistics.getSide() == null ? "-" : this.logistics.getSide().getName();
        if (this.facingTab != null) {
            this.facingTab.setTitle(I18n.format("gui.logistic_frame.facing") + ": " + s);
            for (EnumFacing face : EnumFacing.values()) {
                this.facingButtons[face.getIndex()].enabled = face != this.logistics.getSide();
            }
        }
    }

    @Override
    public void actionPerformed(IGuiWidget widget) {
        super.actionPerformed(widget);
        if (widget instanceof WidgetFluidStack) {
            boolean leftClick = Mouse.isButtonDown(0);
            boolean middleClick = Mouse.isButtonDown(2);
            boolean shift = PneumaticCraftRepressurized.proxy.isSneakingInGui();
            IFluidTank tank = this.logistics.getTankFilter(widget.getID());
            if (tank.getFluidAmount() > 0) {
                if (middleClick) {
                    this.logistics.setFilter(widget.getID(), null);
                } else if (leftClick) {
                    tank.drain(shift ? tank.getFluidAmount() / 2 : 1000, true);
                    if (tank.getFluidAmount() < 1000) {
                        tank.drain(1000, true);
                    }
                } else {
                    tank.fill(new FluidStack(tank.getFluid().getFluid(), shift ? tank.getFluidAmount() : 1000), true);
                }
                NetworkHandler.sendToServer(new PacketSetLogisticsFluidFilterStack(this.logistics, tank.getFluid(), widget.getID()));
            } else {
                this.fluidSearchGui = new GuiLogisticsLiquidFilter(this);
                this.editingSlot = widget.getID();
                this.mc.displayGuiScreen(this.fluidSearchGui);
            }
        }
    }

    @Override
    protected void handleMouseClick(Slot slot, int slotId, int clickedButton, ClickType clickType) {
        if (slot instanceof SlotPhantom && Minecraft.getMinecraft().player.inventory.getItemStack().isEmpty() && !slot.getHasStack() && clickedButton == 1) {
            this.editingSlot = slot.getSlotIndex();
            Minecraft.getMinecraft().displayGuiScreen(this.searchGui = new GuiSearcher(Minecraft.getMinecraft().player));
        } else {
            super.handleMouseClick(slot, slotId, clickedButton, clickType);
        }
    }
}
