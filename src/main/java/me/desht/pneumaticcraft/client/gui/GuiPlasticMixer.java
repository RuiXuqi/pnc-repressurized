package me.desht.pneumaticcraft.client.gui;

import me.desht.pneumaticcraft.client.gui.widget.*;
import me.desht.pneumaticcraft.common.heat.HeatUtil;
import me.desht.pneumaticcraft.common.inventory.ContainerPlasticMixer;
import me.desht.pneumaticcraft.common.item.Itemss;
import me.desht.pneumaticcraft.common.recipes.PlasticMixerRegistry;
import me.desht.pneumaticcraft.common.recipes.PlasticMixerRegistry.PlasticMixerRecipe;
import me.desht.pneumaticcraft.common.tileentity.TileEntityPlasticMixer;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.util.Collections;
import java.util.List;

@SideOnly(Side.CLIENT)
public class GuiPlasticMixer extends GuiPneumaticContainerBase<TileEntityPlasticMixer> {
    private GuiButtonSpecial[] buttons;
    private GuiCheckBox lockSelection;
    private WidgetLabel noItemsLabel;
    private WidgetLabel amountLabel;
    private WidgetTemperature tempWidget;
    private int nExposedFaces;
    private GuiAnimatedStat selectionTab;
    private Fluid lastFluid;
    private ItemStack lastItemStack = ItemStack.EMPTY;

    public GuiPlasticMixer(InventoryPlayer player, TileEntityPlasticMixer te) {
        super(new ContainerPlasticMixer(player, te), te, Textures.GUI_PLASTIC_MIXER);
    }

    @Override
    public void initGui() {
        super.initGui();

        this.addWidget(new WidgetTemperature(0, this.guiLeft + 55, this.guiTop + 25, 273, 773, this.te.getLogic(0)));
        this.addWidget(this.tempWidget = new WidgetTemperature(1, this.guiLeft + 82, this.guiTop + 25, 273, 773, this.te.getLogic(1), 273) {
            @Override
            public void addTooltip(int mouseX, int mouseY, List<String> curTip, boolean shift) {
                super.addTooltip(mouseX, mouseY, curTip, shift);
                if (this.getScales().length >= 2) {
                    TextFormatting tf = this.getScales()[1] <= GuiPlasticMixer.this.te.getLogic(1).getTemperatureAsInt() ? TextFormatting.GREEN : TextFormatting.GOLD;
                    curTip.add(tf + "Required Temperature: " + (this.getScales()[1] - 273) + "\u00b0C");
                }
            }
        });
        this.addWidget(new WidgetTank(3, this.guiLeft + 152, this.guiTop + 14, this.te.getTank()));

        PlasticMixerRecipe recipe = PlasticMixerRegistry.INSTANCE.getRecipe(this.te.getTank().getFluid());
        Item targetItem = recipe == null ? Itemss.PLASTIC : recipe.getItemStack().getItem();
        this.selectionTab = this.addAnimatedStat("gui.tab.plasticMixer.plasticSelection", new ItemStack(targetItem, 1, 1), 0xFF005500, false);
        this.selectionTab.addPadding(12, 88 / this.fontRenderer.getStringWidth(" "));

        this.buttons = new GuiButtonSpecial[16];
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                int index = y * 4 + x;
                ItemStack stack = new ItemStack(targetItem, 1, index);
                this.buttons[index] = new GuiButtonSpecial(index + 1, x * 21 + 15, y * 21 + 30, 20, 20, "")
                        .setRenderStacks(stack)
                        .setTooltipText(stack.getDisplayName());
                this.selectionTab.addWidget(this.buttons[index]);
            }
        }
        this.noItemsLabel = new WidgetLabel(15, 34, TextFormatting.GOLD.toString() + TextFormatting.ITALIC + I18n.format("gui.tab.plasticMixer.tankEmpty"));
        this.selectionTab.addWidget(this.noItemsLabel);
        this.amountLabel = new WidgetLabel(15, 118, "");
        this.selectionTab.addWidget(this.amountLabel);

        this.selectionTab.addWidget(this.lockSelection = new GuiCheckBox(17, 15, 18, 0xFF000000, "gui.plasticMixer.lockSelection")
                .setChecked(this.te.lockSelection)
                .setTooltip(PneumaticCraftUtils.convertStringIntoList(I18n.format("gui.plasticMixer.lockSelection.tooltip"))));

        if (this.te.getTank().getFluid() != null && this.te.getTank().getFluid().amount > 0) {
            this.selectionTab.openWindow();
        }
        this.updateSelectionTab();

        this.nExposedFaces = HeatUtil.countExposedFaces(Collections.singletonList(this.te));
    }

    private void updateSelectionTab() {
        PlasticMixerRecipe recipe = PlasticMixerRegistry.INSTANCE.getRecipe(this.te.getTank().getFluid());

        if (recipe == null || !recipe.allowSolidifying()) {
            for (int index = 0; index < 16; index++) {
                this.buttons[index].setVisible(false);
                this.buttons[index].visible = false;
            }
            this.amountLabel.visible = false;
            this.noItemsLabel.visible = true;
            this.selectionTab.setTexture(new ItemStack(Blocks.STRUCTURE_VOID));
        } else {
            for (int index = 0; index < 16; index++) {
                boolean showButton = recipe.getMeta() < 0 ? index < recipe.getNumSubTypes() : index == recipe.getMeta();
                if (showButton) {
                    ItemStack stack = new ItemStack(recipe.getItemStack().getItem(), 1, index);
                    this.buttons[index].setRenderStacks(stack).setTooltipText(stack.getDisplayName());
                }
                this.buttons[index].setVisible(showButton);
                this.buttons[index].visible = showButton;
            }
            FluidStack f = recipe.getFluidStack();
            this.amountLabel.text = StringUtils.abbreviate(TextFormatting.GRAY + "" + f.amount + "mB " + f.getFluid().getLocalizedName(f), 20);
            this.amountLabel.visible = true;
            this.noItemsLabel.visible = false;
            this.selectionTab.setTexture(recipe.getItemStack());
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        if (this.selectionTab != null) {
            for (int i = 0; i < this.buttons.length; i++) {
                this.buttons[i].enabled = this.te.selectedPlastic != i;
            }
            this.lockSelection.checked = this.te.lockSelection;
        }

        Fluid f = this.getFluid();
        if (f != this.lastFluid) {
            this.updateSelectionTab();
        }
        this.lastFluid = this.getFluid();

        ItemStack input = this.te.getPrimaryInventory().getStackInSlot(TileEntityPlasticMixer.INV_INPUT);
        if (!ItemStack.areItemsEqual(input, this.lastItemStack)) {
            PlasticMixerRecipe recipe = PlasticMixerRegistry.INSTANCE.getRecipe(input);
            if (recipe != null && recipe.allowMelting()) {
                this.tempWidget.setScales(273, recipe.getTemperature());
            } else {
                this.tempWidget.setScales(273);
            }
        }
        this.lastItemStack = this.te.getPrimaryInventory().getStackInSlot(TileEntityPlasticMixer.INV_OUTPUT).copy();
    }

    private Fluid getFluid() {
        return this.te.getTank().getFluid() == null ? null : this.te.getTank().getFluid().getFluid();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int x, int y) {
        super.drawGuiContainerForegroundLayer(x, y);

        this.fontRenderer.drawString("Upgr.", 15, 19, 4210752);
        this.fontRenderer.drawString("Hull", 56, 16, 4210752);
        this.fontRenderer.drawString("Item", 88, 16, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int x, int y) {
        super.drawGuiContainerBackgroundLayer(partialTicks, x, y);
        for (int i = 0; i < 3; i++) {
            double percentage = (double) this.te.dyeBuffers[i] / TileEntityPlasticMixer.DYE_BUFFER_MAX;
            this.drawVerticalLine(this.guiLeft + 124, this.guiTop + 35 + i * 18, this.guiTop + 37 - MathHelper.clamp((int) (percentage * 16), 1, 15) + i * 18, 0xFF000000 | 0xFF0000 >> 8 * i);
        }
    }

    @Override
    protected Point getInvNameOffset() {
        return new Point(0, -1);
    }

    @Override
    protected Point getInvTextOffset() {
        return null;
    }

    @Override
    protected void addProblems(List<String> curInfo) {
        super.addProblems(curInfo);
        ItemStack stack = this.te.getPrimaryInventory().getStackInSlot(0);
        if (this.te.getTank().getFluidAmount() == 0) {
            if (stack.isEmpty()) {
                curInfo.add("gui.tab.problems.plasticMixer.noPlastic");
            } else {
                curInfo.add("gui.tab.problems.notEnoughHeat");
            }
        } else if (!stack.isEmpty()) {
            PlasticMixerRecipe recipe = PlasticMixerRegistry.INSTANCE.getRecipe(stack);
            int temp = recipe == null ? PneumaticValues.PLASTIC_MIXER_MELTING_TEMP : recipe.getTemperature();
            int amount = recipe == null ? 1000 : recipe.getFluidStack().amount;
            if (this.te.getLogic(1).getTemperatureAsInt() >= temp && this.te.getTank().getCapacity() - this.te.getTank().getFluidAmount() < amount) {
                curInfo.add("gui.tab.problems.plasticMixer.plasticOverflow");
            }
        }
        if (this.te.getPrimaryInventory().getStackInSlot(TileEntityPlasticMixer.INV_DYE_RED).isEmpty()) {
            curInfo.add(I18n.format("gui.tab.problems.plasticMixer.noDye", new ItemStack(Items.DYE, 1, 1).getDisplayName()));
        }
        if (this.te.getPrimaryInventory().getStackInSlot(TileEntityPlasticMixer.INV_DYE_GREEN).isEmpty()) {
            curInfo.add(I18n.format("gui.tab.problems.plasticMixer.noDye", new ItemStack(Items.DYE, 1, 2).getDisplayName()));
        }
        if (this.te.getPrimaryInventory().getStackInSlot(TileEntityPlasticMixer.INV_DYE_BLUE).isEmpty()) {
            curInfo.add(I18n.format("gui.tab.problems.plasticMixer.noDye", new ItemStack(Items.DYE, 1, 4).getDisplayName()));
        }
    }

    @Override
    protected void addInformation(List<String> curInfo) {
        if (curInfo.size() == 0) {
            curInfo.add(I18n.format("gui.tab.problems.plasticMixer.noProblems"));
        }
    }


    @Override
    protected void addWarnings(List<String> curInfo) {
        super.addWarnings(curInfo);

        if (this.nExposedFaces > 0 && !this.te.getPrimaryInventory().getStackInSlot(0).isEmpty()) {
            curInfo.add(I18n.format("gui.tab.problems.exposedFaces", this.nExposedFaces, 6));
        }
    }
}
