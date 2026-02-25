package me.desht.pneumaticcraft.client.gui;

import me.desht.pneumaticcraft.client.gui.widget.GuiAnimatedStat;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTank;
import me.desht.pneumaticcraft.common.block.Blockss;
import me.desht.pneumaticcraft.common.config.ConfigHandler;
import me.desht.pneumaticcraft.common.inventory.ContainerLiquidHopper;
import me.desht.pneumaticcraft.common.tileentity.TileEntityLiquidHopper;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GuiLiquidHopper extends GuiPneumaticContainerBase<TileEntityLiquidHopper> {
    private GuiAnimatedStat statusStat;
    private final GuiButtonSpecial[] modeButtons = new GuiButtonSpecial[2];

    public GuiLiquidHopper(InventoryPlayer player, TileEntityLiquidHopper te) {
        super(new ContainerLiquidHopper(player, te), te, Textures.GUI_LIQUID_HOPPER);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.addWidget(new WidgetTank(0, this.guiLeft + 116, this.guiTop + 15, this.te.getTank()));
        this.statusStat = this.addAnimatedStat("gui.tab.hopperStatus", new ItemStack(Blockss.LIQUID_HOPPER), 0xFFFFAA00, false);

        GuiAnimatedStat optionStat = this.addAnimatedStat("gui.tab.gasLift.mode", new ItemStack(Blocks.LEVER), 0xFFFFCC00, false);
        optionStat.addPadding(4, 14);

        GuiButtonSpecial button = new GuiButtonSpecial(1, 5, 20, 20, 20, "");
        button.setRenderStacks(new ItemStack(Items.BUCKET));
        button.setTooltipText(I18n.format("gui.tab.liquidHopper.mode.empty"));
        optionStat.addWidget(button);
        this.modeButtons[0] = button;

        button = new GuiButtonSpecial(2, 30, 20, 20, 20, "");
        button.setRenderStacks(new ItemStack(Items.WATER_BUCKET));
        button.setTooltipText(I18n.format("gui.tab.liquidHopper.mode.leaveLiquid"));
        optionStat.addWidget(button);
        this.modeButtons[1] = button;
    }

    @Override
    protected Point getInvNameOffset() {
        return new Point(0, -1);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        this.statusStat.setText(this.getStatus());
        this.modeButtons[0].enabled = this.te.doesLeaveMaterial();
        this.modeButtons[1].enabled = !this.te.doesLeaveMaterial();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int x, int y) {
        super.drawGuiContainerForegroundLayer(x, y);
        this.fontRenderer.drawString("Upgr.", 53, 19, 4210752);
    }

    private List<String> getStatus() {
        List<String> textList = new ArrayList<>();
        int itemsPer = this.te.getMaxItems();
        if (itemsPer > 1) {
            textList.add(I18n.format("gui.tab.hopperStatus.liquidTransferPerTick", itemsPer * 100));
        } else {
            int transferInterval = this.te.getItemTransferInterval();
            textList.add(I18n.format("gui.tab.hopperStatus.liquidTransferPerSecond", transferInterval == 0 ? "2000" : PneumaticCraftUtils.roundNumberTo(2000F / transferInterval, 1)));
        }
        return textList;
    }

    @Override
    protected void addExtraUpgradeText(List<String> text) {
        if (ConfigHandler.machineProperties.liquidHopperDispenser) {
            text.add("gui.tab.upgrades.tile.liquid_hopper.dispenser");
        }
    }
}
