package me.desht.pneumaticcraft.client.gui.tubemodule;

import com.google.common.collect.ImmutableList;
import me.desht.pneumaticcraft.client.gui.GuiButtonSpecial;
import me.desht.pneumaticcraft.client.gui.widget.GuiCheckBox;
import me.desht.pneumaticcraft.client.gui.widget.WidgetComboBox;
import me.desht.pneumaticcraft.client.gui.widget.WidgetLabel;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTextFieldNumber;
import me.desht.pneumaticcraft.common.block.tubes.ModuleRedstone;
import me.desht.pneumaticcraft.common.block.tubes.ModuleRedstone.Operation;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketSyncRedstoneModuleToServer;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.dyeColorDesc;

public class GuiRedstoneModule extends GuiTubeModule {
    private WidgetComboBox comboBox;
    private WidgetLabel constLabel;
    private WidgetTextFieldNumber textField;
    private WidgetLabel otherColorLabel;
    private GuiButtonSpecial ourColorButton;
    private GuiButtonSpecial otherColorButton;
    private int ourColor;
    private int otherColor;
    private GuiCheckBox invertCheckBox;

    public GuiRedstoneModule(EntityPlayer player, int x, int y, int z) {
        super(player, x, y, z);
        this.ySize = 202;
    }

    @Override
    protected ResourceLocation getTexture() {
        return Textures.GUI_WIDGET_OPTIONS;
    }

    @Override
    public void initGui() {
        super.initGui();

        ModuleRedstone mr = (ModuleRedstone) this.module;
        this.ourColor = mr.getColorChannel();
        this.otherColor = mr.getOtherColor();

        this.addWidget(new WidgetLabel(this.guiLeft + this.xSize / 2, this.guiTop + 5, "Redstone Module").setAlignment(WidgetLabel.Alignment.CENTRE));

        WidgetLabel ourColorLabel;
        this.addWidget(ourColorLabel = new WidgetLabel(this.guiLeft + 10, this.guiTop + 20, "Our Color"));

        WidgetLabel opLabel;
        this.addWidget(opLabel = new WidgetLabel(this.guiLeft + 10, this.guiTop + 40, "Operation"));

        this.otherColorLabel = new WidgetLabel(this.guiLeft + 10, this.guiTop + 60, "Other Color");
        this.addWidget(this.otherColorLabel);

        this.constLabel = new WidgetLabel(this.guiLeft + 15, this.guiTop + 60, "Constant");
        this.addWidget(this.constLabel);

        int w = 0;
        for (WidgetLabel label : ImmutableList.of(ourColorLabel, this.otherColorLabel, opLabel, this.constLabel)) {
            w = Math.max(label.getBounds().width, w);
        }
        int xBase = this.guiLeft + w + 15;

        this.ourColorButton = new GuiButtonSpecial(0, xBase, this.guiTop + 15, 20, 20, "") {
            @Override
            public void onMouseClicked(int mouseX, int mouseY, int button) {
                if (button == 0) {
                    if (--GuiRedstoneModule.this.ourColor < 0) GuiRedstoneModule.this.ourColor = 15;
                } else if (button == 1) {
                    if (++GuiRedstoneModule.this.ourColor > 15) GuiRedstoneModule.this.ourColor = 0;
                }
            }
        };
        this.addWidget(this.ourColorButton);

        List<String> ops = new ArrayList<>();
        for (Operation op : Operation.values()) {
            ops.add(I18n.format(op.getTranslationKey()));
        }
        this.comboBox = new WidgetComboBox(this.fontRenderer, xBase, this.guiTop + 39, this.xSize - xBase + this.guiLeft - 10, 12)
                .setFixedOptions().setShouldSort(false).setElements(ops);
        this.comboBox.selectElement(mr.getOperation().ordinal());
        this.addWidget(this.comboBox);

        this.otherColorButton = new GuiButtonSpecial(0, xBase, this.guiTop + 55, 20, 20, "") {
            @Override
            public void onMouseClicked(int mouseX, int mouseY, int button) {
                if (GuiRedstoneModule.this.comboBox.isFocused()) return;  // it hangs over the button
                if (button == 0) {
                    if (--GuiRedstoneModule.this.otherColor < 0) GuiRedstoneModule.this.otherColor = 15;
                } else if (button == 1) {
                    if (++GuiRedstoneModule.this.otherColor > 15) GuiRedstoneModule.this.otherColor = 0;
                }
            }
        };
        this.addWidget(this.otherColorButton);

        this.textField = new WidgetTextFieldNumber(this.fontRenderer, xBase, this.guiTop + 58, 30, 12);
        this.textField.minValue = 0;
        this.textField.setDecimals(0);
        this.textField.setValue(mr.getConstantVal());
        this.addWidget(this.textField);

        this.invertCheckBox = new GuiCheckBox(1, this.guiLeft + 10, this.guiTop + 80, 0xFF404040, "Invert Output?") {
            @Override
            public void onMouseClicked(int mouseX, int mouseY, int button) {
                if (GuiRedstoneModule.this.comboBox.isFocused()) return;  // it hangs over the button
                super.onMouseClicked(mouseX, mouseY, button);
            }
        };
        this.invertCheckBox.checked = mr.isInvert();
        this.invertCheckBox.setTooltip(I18n.format("gui.redstoneModule.invert.tooltip"));
        this.addWidget(this.invertCheckBox);

        this.updateWidgetVisibility();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        this.updateWidgetVisibility();
    }

    private void updateWidgetVisibility() {
        Operation op = this.getSelectedOp();
        this.constLabel.visible = op.useConst();
        this.textField.setVisible(op.useConst());
        this.otherColorLabel.visible = op.useOtherColor();
        this.otherColorButton.visible = op.useOtherColor();
        this.otherColorButton.setVisible(op.useOtherColor());
        this.ourColorButton.setRenderStacks(new ItemStack(Blocks.CONCRETE, 1, EnumDyeColor.byDyeDamage(this.ourColor).getMetadata()));
        this.otherColorButton.setRenderStacks(new ItemStack(Blocks.CONCRETE, 1, EnumDyeColor.byDyeDamage(this.otherColor).getMetadata()));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        Operation op = this.getSelectedOp();
        String key = op.getTranslationKey() + ".tooltip";
        String s;
        if (op.useConst()) {
            s = I18n.format(key, dyeColorDesc(this.ourColor), this.textField.getValue());
        } else if (op.useOtherColor()) {
            s = I18n.format(key, dyeColorDesc(this.ourColor), dyeColorDesc(this.otherColor));
        } else {
            s = I18n.format(key, dyeColorDesc(this.ourColor));
        }
        List<String> l = PneumaticCraftUtils.convertStringIntoList(s, 30);
        int yBase = this.guiTop + this.ySize - l.size() * this.fontRenderer.FONT_HEIGHT - 10;
        for (int i = 0; i < l.size(); i++) {
            this.fontRenderer.drawString(l.get(i), this.guiLeft + 10, yBase + i * this.fontRenderer.FONT_HEIGHT, 0xFF404040);
        }
    }

    private Operation getSelectedOp() {
        return Operation.values()[this.comboBox.getSelectedElementIndex()];
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();

        ((ModuleRedstone) this.module).setColorChannel(this.ourColor);
        ((ModuleRedstone) this.module).setInvert(this.invertCheckBox.checked);
        ((ModuleRedstone) this.module).setOperation(this.getSelectedOp(), this.otherColor, this.textField.getValue());
        NetworkHandler.sendToServer(new PacketSyncRedstoneModuleToServer((ModuleRedstone) this.module));
    }
}
