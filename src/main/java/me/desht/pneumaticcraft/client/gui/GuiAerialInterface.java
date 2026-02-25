package me.desht.pneumaticcraft.client.gui;

import com.google.common.collect.ImmutableList;
import me.desht.pneumaticcraft.client.gui.widget.GuiAnimatedStat;
import me.desht.pneumaticcraft.client.gui.widget.IGuiWidget;
import me.desht.pneumaticcraft.client.gui.widget.WidgetEnergy;
import me.desht.pneumaticcraft.common.PneumaticCraftAPIHandler;
import me.desht.pneumaticcraft.common.inventory.ContainerEnergy;
import me.desht.pneumaticcraft.common.thirdparty.ModNameCache;
import me.desht.pneumaticcraft.common.tileentity.TileEntityAerialInterface;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.text.WordUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SideOnly(Side.CLIENT)
public class GuiAerialInterface extends GuiPneumaticContainerBase<TileEntityAerialInterface> {
    private final GuiButtonSpecial[] modeButtons = new GuiButtonSpecial[3];
    private GuiButtonSpecial xpButton;

    public GuiAerialInterface(InventoryPlayer player, TileEntityAerialInterface te) {
        super(new ContainerEnergy(player, te), te, Textures.GUI_4UPGRADE_SLOTS);
    }

    @Override
    public void initGui() {
        super.initGui();

        this.addAnimatedStat("gui.tab.info.aerialInterface.interfacingRF.info.title",
                Textures.GUI_BUILDCRAFT_ENERGY, 0xFFa02222, false).setText("gui.tab.info.aerialInterface.interfacingRF.info");

        if (this.te.hasCapability(CapabilityEnergy.ENERGY, null)) {
            IEnergyStorage storage = this.te.getCapability(CapabilityEnergy.ENERGY, null);
            this.addWidget(new WidgetEnergy(this.guiLeft + 20, this.guiTop + 20, storage));
        }

        if (this.te.dispenserUpgradeInserted) {
            // Experience Tab
            if (PneumaticCraftAPIHandler.getInstance().liquidXPs.size() > 0) {
                GuiAnimatedStat xpStat = this.addAnimatedStat("gui.tab.info.aerialInterface.liquidXp.info.title",
                        new ItemStack(Items.EXPERIENCE_BOTTLE), 0xFF55FF55, false);
                xpStat.setText(this.getLiquidXPText());
                this.xpButton = new GuiButtonSpecial(4, 20, 15, 20, 20, "");
                this.xpButton.setListener(this);
                this.setupXPButton();
                xpStat.addWidget(this.xpButton);
            }

            // Feeding Tab
            GuiAnimatedStat optionStat = this.addAnimatedStat("gui.tab.aerialInterface.feedMode",
                    new ItemStack(Items.BEEF), 0xFFFFCC00, false);
            optionStat.addPadding(4, 16);

            GuiButtonSpecial button = new GuiButtonSpecial(1, 5, 20, 20, 20, "");
            button.setRenderStacks(new ItemStack(Items.BEEF));
            button.setTooltipText(I18n.format("gui.tab.aerialInterface.feedMode.feedFullyUtilize"));
            optionStat.addWidget(button);
            this.modeButtons[0] = button;

            button = new GuiButtonSpecial(2, 30, 20, 20, 20, "");
            button.setRenderStacks(new ItemStack(Items.APPLE));
            button.setTooltipText(I18n.format("gui.tab.aerialInterface.feedMode.feedWhenPossible"));
            optionStat.addWidget(button);
            this.modeButtons[1] = button;

            button = new GuiButtonSpecial(3, 55, 20, 20, 20, "");
            button.setRenderStacks(new ItemStack(Items.GOLDEN_APPLE));
            button.setTooltipText(Arrays.asList(WordUtils.wrap(I18n.format("gui.tab.aerialInterface.feedMode.utilizeFullHealthElsePossible"), 40).split(System.getProperty("line.separator"))));
            optionStat.addWidget(button);
            this.modeButtons[2] = button;

            this.addAnimatedStat("gui.tab.info.aerialInterface.interfacingFood", new ItemStack(Items.BREAD), 0xFFA0A0A0, false)
                    .setText("gui.tab.info.aerialInterface.removeDispenser");

        } else {
            this.addAnimatedStat("gui.tab.info.aerialInterface.interfacingItems", new ItemStack(Blocks.CHEST), 0xFFA0A0A0, false)
                    .setText("gui.tab.info.aerialInterface.insertDispenser");
            for (int i = 0; i < this.modeButtons.length; i++)
                this.modeButtons[i] = null;
        }
    }

    @Override
    protected boolean shouldAddSideConfigTabs() {
        return !this.te.dispenserUpgradeInserted;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (this.te.dispenserUpgradeInserted) {
            if (this.modeButtons[0] != null) {
                for (int i = 0; i < this.modeButtons.length; i++) {
                    this.modeButtons[i].enabled = this.te.feedMode != i;
                }
            } else {
                this.refreshScreen();
            }
        } else if (this.modeButtons[0] != null) {
            this.refreshScreen();
        }
    }

    @Override
    public void actionPerformed(IGuiWidget widget) {
        if (widget.getID() == 4) {
            this.te.curXPFluidIndex++;
            if (this.te.curXPFluidIndex >= PneumaticCraftAPIHandler.getInstance().availableLiquidXPs.size()) {
                this.te.curXPFluidIndex = -1;
            }
            this.setupXPButton();
        }
        super.actionPerformed(widget);
    }

    private void setupXPButton() {
        Fluid fluid = this.te.curXPFluidIndex >= 0 && this.te.curXPFluidIndex < PneumaticCraftAPIHandler.getInstance().availableLiquidXPs.size() ?
                PneumaticCraftAPIHandler.getInstance().availableLiquidXPs.get(this.te.curXPFluidIndex) : null;
        if (fluid != null) {
            FluidStack fluidStack = new FluidStack(fluid, 1000);
            this.xpButton.setRenderStacks(FluidUtil.getFilledBucket(fluidStack));
            String modname = ModNameCache.getModName(FluidRegistry.getModId(fluidStack));
            this.xpButton.setTooltipText(ImmutableList.of(fluid.getLocalizedName(fluidStack), TextFormatting.BLUE.toString() + TextFormatting.ITALIC + modname));
        } else {
            this.xpButton.setRenderStacks(new ItemStack(Items.BUCKET));
            this.xpButton.setTooltipText(I18n.format("gui.tooltip.aerial_interface.xpDisabled"));
        }
    }

    private List<String> getLiquidXPText() {
        List<String> liquidXpText = new ArrayList<>();
        liquidXpText.add("");
        liquidXpText.add("");
        liquidXpText.add("");
        liquidXpText.add("gui.tab.info.aerialInterface.liquidXp.info");
        liquidXpText.add("");
        if (PneumaticCraftAPIHandler.getInstance().availableLiquidXPs.isEmpty()) {
            liquidXpText.add(TextFormatting.ITALIC + "None");
        } else {
            for (Fluid f : PneumaticCraftAPIHandler.getInstance().availableLiquidXPs) {
                FluidStack stack = new FluidStack(f, 1000);
                String modId = FluidRegistry.getModId(stack);
                liquidXpText.add(TextFormatting.BLACK + "\u2022  " + f.getLocalizedName(stack) + " (" + ModNameCache.getModName(modId) + ")");
            }
        }
        return liquidXpText;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int x, int y) {
        super.drawGuiContainerForegroundLayer(x, y);
        this.fontRenderer.drawString("Upgr.", 53, 19, 4210752);

    }

    @Override
    protected void addPressureStatInfo(List<String> pressureStatText) {
        super.addPressureStatInfo(pressureStatText);
        if (this.te.getPressure() > PneumaticValues.MIN_PRESSURE_AERIAL_INTERFACE && this.te.isConnectedToPlayer) {
            pressureStatText.add(TextFormatting.GRAY + "Usage:");
            pressureStatText.add(TextFormatting.BLACK + PneumaticCraftUtils.roundNumberTo(PneumaticValues.USAGE_AERIAL_INTERFACE, 1) + " mL/tick.");
        }
    }

    @Override
    protected void addProblems(List<String> textList) {
        super.addProblems(textList);
        if (this.te.playerName.equals("")) {
            textList.add("\u00a7No player set!");
            textList.add(TextFormatting.BLACK + "Break and replace the machine.");
        } else if (!this.te.isConnectedToPlayer) {
            textList.add(TextFormatting.GRAY + this.te.playerName + " is not online!");
            textList.add(TextFormatting.BLACK + "The Aerial Interface is non-functional");
            textList.add(TextFormatting.BLACK + "until they return.");
        }
    }

    @Override
    protected void addInformation(List<String> curInfo) {
        if (this.te.playerName != null && !this.te.playerName.isEmpty()) {
            curInfo.add(I18n.format("gui.tab.problems.aerialInterface.linked", this.te.playerName));
        }
    }
}
