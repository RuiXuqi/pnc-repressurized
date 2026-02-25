package me.desht.pneumaticcraft.client.gui;

import me.desht.pneumaticcraft.api.item.IPositionProvider;
import me.desht.pneumaticcraft.client.gui.semiblock.GuiLogisticsLiquidFilter;
import me.desht.pneumaticcraft.client.gui.widget.IGuiWidget;
import me.desht.pneumaticcraft.client.gui.widget.WidgetFluidFilter;
import me.desht.pneumaticcraft.client.gui.widget.WidgetLabel;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTextFieldNumber;
import me.desht.pneumaticcraft.common.inventory.ContainerAmadronAddTrade;
import me.desht.pneumaticcraft.common.item.ItemAmadronTablet;
import me.desht.pneumaticcraft.common.item.ItemGPSTool;
import me.desht.pneumaticcraft.common.item.Itemss;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketAmadronTradeAddCustom;
import me.desht.pneumaticcraft.common.network.PacketAmadronTradeAddPeriodic;
import me.desht.pneumaticcraft.common.network.PacketAmadronTradeAddStatic;
import me.desht.pneumaticcraft.common.recipes.AmadronOffer;
import me.desht.pneumaticcraft.common.recipes.AmadronOffer.TradeType;
import me.desht.pneumaticcraft.common.recipes.AmadronOfferCustom;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.client.FMLClientHandler;
import org.apache.commons.lang3.text.WordUtils;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class GuiAmadronAddTrade extends GuiPneumaticContainerBase {
    private GuiSearcher searchGui;
    private GuiInventorySearcher invSearchGui;
    private GuiInventorySearcher gpsSearchGui;
    private GuiLogisticsLiquidFilter fluidGui;
    private boolean isSettingInput;

    private WidgetFluidFilter inputFluid;
    private WidgetFluidFilter outputFluid;

    private WidgetTextFieldNumber inputNumber, outputNumber;
    private GuiButton addButton;

    private BlockPos inputPosition, outputPosition;
    private final TradeType tradeType;

    public GuiAmadronAddTrade(TradeType tradeType) {
        super(new ContainerAmadronAddTrade(), null, Textures.GUI_WIDGET_OPTIONS_STRING);
        this.tradeType = tradeType;
        this.xSize = 183;
        this.ySize = 202;
    }

    @Override
    public void initGui() {
        super.initGui();

        ContainerAmadronAddTrade container = (ContainerAmadronAddTrade) this.inventorySlots;

        this.addLabel(I18n.format("gui.amadron.addTrade.selling"), this.guiLeft + 4, this.guiTop + 5, 0xFFFFFFFF);
        this.addLabel(I18n.format("gui.amadron.addTrade.buying"), this.guiLeft + 93, this.guiTop + 5, 0xFFFFFFFF);

        this.buttonList.add(new GuiButton(0, this.guiLeft + 4, this.guiTop + 20, 85, 20, "Search item..."));
        this.buttonList.add(new GuiButton(1, this.guiLeft + 4, this.guiTop + 42, 85, 20, "Search inv..."));
        this.buttonList.add(new GuiButton(2, this.guiLeft + 4, this.guiTop + 64, 85, 20, "Search fluid..."));
        this.buttonList.add(new GuiButton(3, this.guiLeft + 93, this.guiTop + 20, 85, 20, "Search item..."));
        this.buttonList.add(new GuiButton(4, this.guiLeft + 93, this.guiTop + 42, 85, 20, "Search inv..."));
        this.buttonList.add(new GuiButton(5, this.guiLeft + 93, this.guiTop + 64, 85, 20, "Search fluid..."));
        this.buttonList.add(this.addButton = new GuiButton(8, this.guiLeft + 50, this.guiTop + 164, 85, 20, "Add Trade"));

        Fluid oldInputFluid = this.inputFluid != null ? this.inputFluid.getFluid() : null;
        Fluid oldOutputFluid = this.outputFluid != null ? this.outputFluid.getFluid() : null;
        this.inputFluid = new WidgetFluidFilter(-1, this.guiLeft + 10, this.guiTop + 90);
        this.outputFluid = new WidgetFluidFilter(-1, this.guiLeft + 99, this.guiTop + 90);
        this.inputFluid.setFluid(oldInputFluid);
        this.outputFluid.setFluid(oldOutputFluid);
        this.addWidget(this.inputFluid);
        this.addWidget(this.outputFluid);

        if (this.tradeType == TradeType.PLAYER) {
            GuiButtonSpecial gpsButton1 = new GuiButtonSpecial(6, this.guiLeft + 10, this.guiTop + 115, 20, 20, "");
            GuiButtonSpecial gpsButton2 = new GuiButtonSpecial(7, this.guiLeft + 99, this.guiTop + 115, 20, 20, "");
            gpsButton1.setTooltipText(Arrays.asList(WordUtils.wrap(I18n.format("gui.amadron.button.selectSellingBlock.tooltip"), 40).split(System.getProperty("line.separator"))));
            gpsButton2.setTooltipText(Arrays.asList(WordUtils.wrap(I18n.format("gui.amadron.button.selectPaymentBlock.tooltip"), 40).split(System.getProperty("line.separator"))));
            gpsButton1.setRenderStacks(new ItemStack(Itemss.GPS_TOOL));
            gpsButton2.setRenderStacks(new ItemStack(Itemss.GPS_TOOL));
            this.addWidget(gpsButton1);
            this.addWidget(gpsButton2);
        }

        this.inputNumber = new WidgetTextFieldNumber(this.fontRenderer, this.guiLeft + 6, this.guiTop + 145, 40, this.fontRenderer.FONT_HEIGHT).setValue(this.inputNumber != null ? this.inputNumber.getValue() : 0);
        this.outputNumber = new WidgetTextFieldNumber(this.fontRenderer, this.guiLeft + 95, this.guiTop + 145, 40, this.fontRenderer.FONT_HEIGHT).setValue(this.outputNumber != null ? this.outputNumber.getValue() : 0);
        this.inputNumber.setTooltip(I18n.format("gui.amadron.addTrade.itemFluidAmount"));
        this.outputNumber.setTooltip(I18n.format("gui.amadron.addTrade.itemFluidAmount"));
        this.addWidget(this.inputNumber);
        this.addWidget(this.outputNumber);

        if (this.searchGui != null) {
            if (this.isSettingInput) {
                this.inputFluid.setFluid(null);
                container.setStack(0, this.searchGui.getSearchStack());
            } else {
                this.outputFluid.setFluid(null);
                container.setStack(1, this.searchGui.getSearchStack());
            }
        }
        if (this.invSearchGui != null) {
            if (this.isSettingInput) {
                this.inputFluid.setFluid(null);
                container.setStack(0, this.invSearchGui.getSearchStack());
            } else {
                this.outputFluid.setFluid(null);
                container.setStack(1, this.invSearchGui.getSearchStack());
            }
        }
        if (this.fluidGui != null) {
            if (this.isSettingInput) {
                container.setStack(0, ItemStack.EMPTY);
                this.inputFluid.setFluid(this.fluidGui.getFilter());
            } else {
                container.setStack(1, ItemStack.EMPTY);
                this.outputFluid.setFluid(this.fluidGui.getFilter());
            }
        }
        if (this.gpsSearchGui != null) {
            if (this.isSettingInput) {
                this.inputPosition = this.gpsSearchGui.getSearchStack().isEmpty() ? null : ItemGPSTool.getGPSLocation(this.gpsSearchGui.getSearchStack());
            } else {
                this.outputPosition = this.gpsSearchGui.getSearchStack().isEmpty() ? null : ItemGPSTool.getGPSLocation(this.gpsSearchGui.getSearchStack());
            }
        }
        this.searchGui = null;
        this.fluidGui = null;
        this.invSearchGui = null;
        this.gpsSearchGui = null;

        WidgetLabel inputNumberLabel = new WidgetLabel(this.guiLeft + 52, this.guiTop + 145, container.getInputStack().isEmpty() ? this.inputFluid.getFluid() != null ? "mB" : "" : "x", 0xFFFFFFFF);
        WidgetLabel outputNumberLabel = new WidgetLabel(this.guiLeft + 149, this.guiTop + 145, container.getOutputStack().isEmpty() ? this.outputFluid.getFluid() != null ? "mB" : "" : "x", 0xFFFFFFFF);
        this.addWidget(inputNumberLabel);
        this.addWidget(outputNumberLabel);
    }

    @Override
    protected int getBackgroundTint() {
        return 0x068e2c;
    }

    @Override
    public void actionPerformed(GuiButton button) {
        EntityPlayer player = FMLClientHandler.instance().getClient().player;
        ContainerAmadronAddTrade container = (ContainerAmadronAddTrade) this.inventorySlots;
        if (button.id < 6 && button.id >= 0) {
            this.isSettingInput = button.id < 3;
            if (button.id % 3 == 0) {
                this.searchGui = new GuiSearcher(player);
                this.searchGui.setSearchStack(this.isSettingInput ? container.getInputStack() : container.getOutputStack());
                FMLClientHandler.instance().showGuiScreen(this.searchGui);
            } else if (button.id % 3 == 1) {
                this.invSearchGui = new GuiInventorySearcher(player);
                this.invSearchGui.setSearchStack(this.isSettingInput ? container.getInputStack() : container.getOutputStack());
                FMLClientHandler.instance().showGuiScreen(this.invSearchGui);
            } else if (button.id % 3 == 2) {
                this.fluidGui = new GuiLogisticsLiquidFilter(this);
                this.fluidGui.setFilter(this.isSettingInput ? this.inputFluid.getFluid() : this.outputFluid.getFluid());
                FMLClientHandler.instance().showGuiScreen(this.fluidGui);
            }

        } else if (button.id == 8) {
            Object input;
            if (!container.getInputStack().isEmpty()) {
                input = container.getInputStack().copy();
                ((ItemStack) input).setCount(this.inputNumber.getValue());
            } else {
                input = new FluidStack(this.inputFluid.getFluid(), this.inputNumber.getValue());
            }
            Object output;
            if (!container.getOutputStack().isEmpty()) {
                output = container.getOutputStack().copy();
                ((ItemStack) output).setCount(this.outputNumber.getValue());
            } else {
                output = new FluidStack(this.outputFluid.getFluid(), this.outputNumber.getValue());
            }
            if (this.tradeType == TradeType.PLAYER) {
                AmadronOfferCustom trade = new AmadronOfferCustom(input, output, player);
                BlockPos pos = this.getPosition(ContainerAmadronAddTrade.INPUT_SLOT);
                int dimensionId = this.getDimension(ContainerAmadronAddTrade.INPUT_SLOT);
                trade.setProvidingPosition(pos, dimensionId);
                pos = this.getPosition(ContainerAmadronAddTrade.OUTPUT_SLOT);
                dimensionId = this.getDimension(ContainerAmadronAddTrade.OUTPUT_SLOT);
                trade.setReturningPosition(pos, dimensionId);
                NetworkHandler.sendToServer(new PacketAmadronTradeAddCustom(trade.invert()));
            } else if (this.tradeType == TradeType.PERIODIC) {
                AmadronOffer trade = new AmadronOffer(input, output);
                NetworkHandler.sendToServer(new PacketAmadronTradeAddPeriodic(trade));
            } else if (this.tradeType == TradeType.STATIC) {
                AmadronOffer trade = new AmadronOffer(input, output);
                NetworkHandler.sendToServer(new PacketAmadronTradeAddStatic(trade));
            }
            player.closeScreen();
        }
        super.actionPerformed(button);
    }

    @Override
    public void actionPerformed(IGuiWidget widget) {
        if (widget.getID() == 6 || widget.getID() == 7) {
            this.gpsSearchGui = new GuiInventorySearcher(FMLClientHandler.instance().getClientPlayerEntity());
            this.gpsSearchGui.setStackPredicate(itemStack -> itemStack.getItem() instanceof IPositionProvider);
            this.isSettingInput = widget.getID() == 6;
            ItemStack gps = new ItemStack(Itemss.GPS_TOOL);
            BlockPos pos;
            if (widget.getID() == 6) {
                pos = this.getPosition(ContainerAmadronAddTrade.INPUT_SLOT);
            } else {
                pos = this.getPosition(ContainerAmadronAddTrade.OUTPUT_SLOT);
            }
            if (pos != null) ItemGPSTool.setGPSLocation(gps, pos);
            this.gpsSearchGui.setSearchStack(ItemGPSTool.getGPSLocation(gps) != null ? gps : ItemStack.EMPTY);
            FMLClientHandler.instance().showGuiScreen(this.gpsSearchGui);
        }
        super.actionPerformed(widget);
    }

    private BlockPos getPosition(int slot) {
        if (slot == ContainerAmadronAddTrade.INPUT_SLOT) {
            if (this.inputPosition != null) return this.inputPosition;
        } else if (slot == ContainerAmadronAddTrade.OUTPUT_SLOT) {
            if (this.outputPosition != null) return this.outputPosition;
        }
        EntityPlayer player = FMLClientHandler.instance().getClient().player;
        if (((ContainerAmadronAddTrade) this.inventorySlots).getStack(slot).isEmpty()) {
            return ItemAmadronTablet.getLiquidProvidingLocation(player.getHeldItemMainhand());
        } else {
            return ItemAmadronTablet.getItemProvidingLocation(player.getHeldItemMainhand());
        }
    }

    private int getDimension(int slot) {
        EntityPlayer player = FMLClientHandler.instance().getClient().player;
        if (slot == ContainerAmadronAddTrade.INPUT_SLOT) {
            if (this.inputPosition != null) return player.world.provider.getDimension();
        } else if (slot == ContainerAmadronAddTrade.OUTPUT_SLOT) {
            if (this.outputPosition != null) return player.world.provider.getDimension();
        }
        if (((ContainerAmadronAddTrade) this.inventorySlots).getStack(slot).isEmpty()) {
            return ItemAmadronTablet.getLiquidProvidingDimension(player.getHeldItemMainhand());
        } else {
            return ItemAmadronTablet.getItemProvidingDimension(player.getHeldItemMainhand());
        }
    }

    @Override
    protected Point getInvTextOffset() {
        return null;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        ContainerAmadronAddTrade container = (ContainerAmadronAddTrade) this.inventorySlots;
        boolean posOK = this.tradeType != TradeType.PLAYER
                || (this.getPosition(ContainerAmadronAddTrade.INPUT_SLOT) != null && this.getPosition(ContainerAmadronAddTrade.OUTPUT_SLOT) != null);
        this.addButton.enabled = this.inputNumber.getValue() > 0
                && this.outputNumber.getValue() > 0
                && (this.inputFluid.getFluid() != null || !container.getInputStack().isEmpty())
                && (this.outputFluid.getFluid() != null || !container.getOutputStack().isEmpty())
                && posOK;
    }

    @Override
    protected void addProblems(List curInfo) {
        if (this.tradeType == TradeType.PLAYER && (this.getPosition(ContainerAmadronAddTrade.INPUT_SLOT) == null || this.getPosition(ContainerAmadronAddTrade.OUTPUT_SLOT) == null)) {
            curInfo.add("gui.amadron.addTrade.problems.noSellingOrPayingBlock");
        }
        super.addProblems(curInfo);
    }
}
