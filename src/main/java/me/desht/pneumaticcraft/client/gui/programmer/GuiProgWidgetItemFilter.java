package me.desht.pneumaticcraft.client.gui.programmer;

import me.desht.pneumaticcraft.client.gui.GuiInventorySearcher;
import me.desht.pneumaticcraft.client.gui.GuiProgrammer;
import me.desht.pneumaticcraft.client.gui.GuiSearcher;
import me.desht.pneumaticcraft.client.gui.widget.GuiCheckBox;
import me.desht.pneumaticcraft.client.gui.widget.IGuiWidget;
import me.desht.pneumaticcraft.client.gui.widget.WidgetComboBox;
import me.desht.pneumaticcraft.common.config.ConfigHandler;
import me.desht.pneumaticcraft.common.progwidgets.IProgWidget;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetItemFilter;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.FMLClientHandler;

import java.io.IOException;
import java.util.Arrays;

public class GuiProgWidgetItemFilter extends GuiProgWidgetOptionBase {
    private GuiSearcher searchGui;
    private GuiInventorySearcher invSearchGui;
    private GuiCheckBox checkBoxUseDamage;
    private GuiCheckBox checkBoxUseNBT;
    private GuiCheckBox checkBoxUseOreDict;
    private GuiCheckBox checkBoxUseModSimilarity;
    private GuiCheckBox checkBoxMatchBlock;
    private final ProgWidgetItemFilter filterWidget;
    private GuiButton incButton, decButton;
    private WidgetComboBox variableField;

    public GuiProgWidgetItemFilter(IProgWidget widget, GuiProgrammer guiProgrammer) {
        super(widget, guiProgrammer);
        this.filterWidget = (ProgWidgetItemFilter) widget;
    }

    @Override
    public void initGui() {
        super.initGui();

        this.buttonList.add(new GuiButton(0, this.guiLeft + 4, this.guiTop + 20, 70, 20, "Search item..."));
        this.buttonList.add(new GuiButton(1, this.guiLeft + 78, this.guiTop + 20, 100, 20, "Search inventory..."));
        this.decButton = new GuiButton(2, this.guiLeft + 140, this.guiTop + 87, 10, 12, "-");
        this.incButton = new GuiButton(3, this.guiLeft + 167, this.guiTop + 87, 10, 12, "+");
        this.buttonList.add(this.decButton);
        this.buttonList.add(this.incButton);
        this.checkBoxUseDamage = new GuiCheckBox(0, this.guiLeft + 4, this.guiTop + 72, 0xFF404040, "Use metadata / damage values");
        this.checkBoxUseDamage.setTooltip(Arrays.asList("Check to handle differently damaged tools", "or different colors of Wool as different."));
        this.checkBoxUseDamage.checked = this.filterWidget.useMetadata;
        this.addWidget(this.checkBoxUseDamage);
        this.checkBoxUseNBT = new GuiCheckBox(2, this.guiLeft + 4, this.guiTop + 108, 0xFF404040, "Use NBT");
        this.checkBoxUseNBT.setTooltip(Arrays.asList("Check to handle items like Enchanted Books", "or Firework as different."));
        this.checkBoxUseNBT.checked = this.filterWidget.useNBT;
        this.addWidget(this.checkBoxUseNBT);
        this.checkBoxUseOreDict = new GuiCheckBox(3, this.guiLeft + 4, this.guiTop + 120, 0xFF404040, "Use Ore Dictionary");
        this.checkBoxUseOreDict.setTooltip(Arrays.asList("Check to handle items registered in the", "Ore Dictionary as the same."));
        this.checkBoxUseOreDict.checked = this.filterWidget.useOreDict;
        this.addWidget(this.checkBoxUseOreDict);
        this.checkBoxUseModSimilarity = new GuiCheckBox(4, this.guiLeft + 4, this.guiTop + 132, 0xFF404040, "Use Mod similarity");
        this.checkBoxUseModSimilarity.setTooltip(Arrays.asList("Check to handle items from the", "same mod as the same."));
        this.checkBoxUseModSimilarity.checked = this.filterWidget.useModSimilarity;
        this.addWidget(this.checkBoxUseModSimilarity);
        this.checkBoxMatchBlock = new GuiCheckBox(5, this.guiLeft + 4, this.guiTop + 144, 0xFF404040, "Match by Block");
        this.checkBoxMatchBlock.setTooltip(Arrays.asList("Check to match by block instead of", "dropped item. Useful for blocks", "which don't drop an item.", TextFormatting.GRAY.toString() + TextFormatting.ITALIC + "Only used by the 'Dig' programming piece."));
        this.checkBoxMatchBlock.checked = this.filterWidget.matchBlock;
        this.addWidget(this.checkBoxMatchBlock);

        this.variableField = new WidgetComboBox(this.fontRenderer, this.guiLeft + 90, this.guiTop + 56, 80, this.fontRenderer.FONT_HEIGHT + 1);
        this.variableField.setElements(this.guiProgrammer.te.getAllVariables());
        this.variableField.setText(this.filterWidget.getVariable());

        if (ConfigHandler.getProgrammerDifficulty() == 2) {
            this.addWidget(this.variableField);
        }

        this.checkBoxUseDamage.enabled = !this.checkBoxUseOreDict.checked && !this.checkBoxUseModSimilarity.checked;
        this.incButton.enabled = this.checkBoxUseDamage.enabled && this.checkBoxUseDamage.checked;
        this.decButton.enabled = this.checkBoxUseDamage.enabled && this.checkBoxUseDamage.checked;
        this.checkBoxUseNBT.enabled = !this.checkBoxUseOreDict.checked && !this.checkBoxUseModSimilarity.checked && !this.checkBoxMatchBlock.checked;
        this.checkBoxUseOreDict.enabled = !this.checkBoxUseModSimilarity.checked && !this.checkBoxMatchBlock.checked;
        this.checkBoxUseModSimilarity.enabled = !this.checkBoxUseOreDict.checked && !this.checkBoxMatchBlock.checked;
        this.checkBoxMatchBlock.enabled = !this.checkBoxUseNBT.checked && !this.checkBoxUseModSimilarity.checked && !this.checkBoxUseOreDict.checked;

        if (this.searchGui != null) this.filterWidget.setFilter(this.searchGui.getSearchStack());
        if (this.invSearchGui != null) this.filterWidget.setFilter(this.invSearchGui.getSearchStack());
    }

    @Override
    public void keyTyped(char key, int keyCode) throws IOException {
        if (keyCode == 1) {
            this.filterWidget.setVariable(this.variableField.getText());
        }
        super.keyTyped(key, keyCode);
    }

    @Override
    public void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            this.searchGui = new GuiSearcher(FMLClientHandler.instance().getClient().player);
            this.searchGui.setSearchStack(this.filterWidget.getFilter());
            FMLClientHandler.instance().showGuiScreen(this.searchGui);
        } else if (button.id == 1) {
            this.invSearchGui = new GuiInventorySearcher(FMLClientHandler.instance().getClient().player);
            this.invSearchGui.setSearchStack(this.filterWidget.getFilter());
            FMLClientHandler.instance().showGuiScreen(this.invSearchGui);
        } else if (button.id == 2) {
            if (--this.filterWidget.specificMeta < 0) this.filterWidget.specificMeta = 15;
        } else if (button.id == 3) {
            if (++this.filterWidget.specificMeta > 15) this.filterWidget.specificMeta = 0;
        }
        super.actionPerformed(button);
    }

    @Override
    public void actionPerformed(IGuiWidget guiWidget) {
        if (guiWidget instanceof GuiCheckBox) {
            GuiCheckBox checkBox = (GuiCheckBox) guiWidget;
            switch (checkBox.getID()) {
                case 0:
                    this.filterWidget.useMetadata = checkBox.checked;
                    this.incButton.enabled = this.checkBoxUseDamage.enabled && this.checkBoxUseDamage.checked;
                    this.decButton.enabled = this.checkBoxUseDamage.enabled && this.checkBoxUseDamage.checked;
                    break;
                case 2:
                    this.filterWidget.useNBT = checkBox.checked;
                    this.checkBoxMatchBlock.enabled = !checkBox.checked;
                    break;
                case 3:
                    this.filterWidget.useOreDict = checkBox.checked;
                    this.checkBoxUseDamage.enabled = !checkBox.checked;
                    this.checkBoxUseNBT.enabled = !checkBox.checked;
                    this.checkBoxUseModSimilarity.enabled = !checkBox.checked;
                    this.checkBoxMatchBlock.enabled = !checkBox.checked;
                    this.incButton.enabled = this.checkBoxUseDamage.enabled && this.checkBoxUseDamage.checked;
                    this.decButton.enabled = this.checkBoxUseDamage.enabled && this.checkBoxUseDamage.checked;
                    break;
                case 4:
                    this.filterWidget.useModSimilarity = checkBox.checked;
                    this.checkBoxUseDamage.enabled = !checkBox.checked;
                    this.checkBoxUseNBT.enabled = !checkBox.checked;
                    this.checkBoxUseOreDict.enabled = !checkBox.checked;
                    this.checkBoxMatchBlock.enabled = !checkBox.checked;
                    this.incButton.enabled = this.checkBoxUseDamage.enabled && this.checkBoxUseDamage.checked;
                    this.decButton.enabled = this.checkBoxUseDamage.enabled && this.checkBoxUseDamage.checked;
                    break;
                case 5:
                    this.filterWidget.matchBlock = checkBox.checked;
                    this.checkBoxUseModSimilarity.enabled = !checkBox.checked;
                    this.checkBoxUseNBT.enabled = !checkBox.checked;
                    this.checkBoxUseOreDict.enabled = !checkBox.checked;
                    break;
            }
        }
        super.actionPerformed(guiWidget);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        String value = String.valueOf(this.filterWidget.specificMeta);
        this.fontRenderer.drawString(value, this.guiLeft + 158 - this.fontRenderer.getStringWidth(value) / 2, this.guiTop + 90, this.checkBoxUseDamage.enabled && this.checkBoxUseDamage.checked ? 0xFF404040 : 0xFF888888);
        this.fontRenderer.drawString("Specific block metadata:", this.guiLeft + 14, this.guiTop + 90, this.checkBoxUseDamage.enabled && this.checkBoxUseDamage.checked ? 0xFF404040 : 0xFF888888);
        if (ConfigHandler.getProgrammerDifficulty() == 2)
            this.fontRenderer.drawString("Variable:", this.guiLeft + 90, this.guiTop + 45, 0xFF404040);
        this.fontRenderer.drawString("Filter:", this.guiLeft + 10, this.guiTop + 53, 0xFF404040);

        String oldVarName = this.filterWidget.getVariable();
        this.filterWidget.setVariable("");
        if (!this.filterWidget.getFilter().isEmpty())
            ProgWidgetItemFilter.drawItemStack(this.filterWidget.getFilter(), this.guiLeft + 50, this.guiTop + 48, "");
        this.filterWidget.setVariable(oldVarName);
    }
}
