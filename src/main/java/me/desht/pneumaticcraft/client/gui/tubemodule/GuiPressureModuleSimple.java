package me.desht.pneumaticcraft.client.gui.tubemodule;

import me.desht.pneumaticcraft.client.gui.GuiButtonSpecial;
import me.desht.pneumaticcraft.client.gui.widget.GuiCheckBox;
import me.desht.pneumaticcraft.client.gui.widget.IGuiWidget;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTextFieldNumber;
import me.desht.pneumaticcraft.common.block.tubes.TubeModule;
import me.desht.pneumaticcraft.common.block.tubes.TubeModuleRedstoneReceiving;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketUpdatePressureModule;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

public class GuiPressureModuleSimple extends GuiTubeModule {
    private WidgetTextFieldNumber thresholdField;
    private GuiButtonSpecial moreOrLessButton;

    public GuiPressureModuleSimple(EntityPlayer player, int x, int y, int z) {
        super(player, x, y, z);
        this.ySize = 57;
    }

    public GuiPressureModuleSimple(TubeModule module) {
        super(module);
        this.ySize = 57;
    }

    @Override
    public void initGui() {
        super.initGui();

        String title = I18n.format("item." + this.module.getType() + ".name");
        this.addLabel(title, this.width / 2 - this.fontRenderer.getStringWidth(title) / 2, this.guiTop + 5);

        GuiCheckBox advancedMode = new GuiCheckBox(0, this.guiLeft + 6, this.guiTop + 15, 0xFF404040, "gui.tubeModule.advancedConfig").setTooltip(I18n.format("gui.tubeModule.advancedConfig.tooltip"));
        advancedMode.checked = false;
        this.addWidget(advancedMode);

        this.thresholdField = new WidgetTextFieldNumber(this.fontRenderer, this.guiLeft + 110, this.guiTop + 33, 30, this.fontRenderer.FONT_HEIGHT).setDecimals(1);
        this.addWidget(this.thresholdField);

        if (this.module instanceof TubeModuleRedstoneReceiving) {
            this.thresholdField.setValue(((TubeModuleRedstoneReceiving) this.module).getThreshold());
            this.addLabel(I18n.format("gui.tubeModule.simpleConfig.threshold"), this.guiLeft + 6, this.guiTop + 33);
        } else {
            this.thresholdField.setValue(this.module.lowerBound);
            this.addLabel(I18n.format("gui.tubeModule.simpleConfig.turn"), this.guiLeft + 6, this.guiTop + 33);
            this.moreOrLessButton = new GuiButtonSpecial(1, this.guiLeft + 85, this.guiTop + 28, 20, 20, this.module.lowerBound < this.module.higherBound ? ">" : "<");
            this.moreOrLessButton.setTooltipText(I18n.format(this.module.lowerBound < this.module.higherBound ? "gui.tubeModule.simpleConfig.higherThan" : "gui.tubeModule.simpleConfig.lowerThan"));
            this.addWidget(this.moreOrLessButton);
        }
        this.addLabel(I18n.format("gui.general.bar"), this.guiLeft + 145, this.guiTop + 34);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (this.module.advancedConfig) {
            this.module.lowerBound = (float) this.thresholdField.getDoubleValue();
            this.mc.displayGuiScreen(new GuiPressureModule(this.module));
        }
    }

    @Override
    protected ResourceLocation getTexture() {
        return Textures.GUI_MODULE_SIMPLE;
    }

    @Override
    public void actionPerformed(IGuiWidget widget) {
        super.actionPerformed(widget);
        switch (widget.getID()) {
            case 0:
                this.module.advancedConfig = true;
                NetworkHandler.sendToServer(new PacketUpdatePressureModule(this.module, 2, 1));
                break;
            case 1:
                //Toggle
                float temp = this.module.higherBound;
                this.module.higherBound = this.module.lowerBound;
                this.module.lowerBound = temp;

                this.updateThreshold();
                this.moreOrLessButton.displayString = this.module.lowerBound < this.module.higherBound ? ">" : "<";
                this.moreOrLessButton.setTooltipText(I18n.format(this.module.lowerBound < this.module.higherBound ? "gui.tubeModule.simpleConfig.higherThan" : "gui.tubeModule.simpleConfig.lowerThan"));
                NetworkHandler.sendToServer(new PacketUpdatePressureModule(this.module, 1, this.module.higherBound));
                break;
        }
    }

    private void updateThreshold() {
        boolean moreThanMode = this.module.lowerBound > this.module.higherBound;
        this.module.lowerBound = (float) this.thresholdField.getDoubleValue();
        if (moreThanMode) {
            this.module.higherBound = this.module.lowerBound - 0.1F;
        } else {
            this.module.higherBound = this.module.lowerBound + 0.1F;
        }
    }

    @Override
    public void onGuiClosed() {
        this.updateThreshold();
        NetworkHandler.sendToServer(new PacketUpdatePressureModule(this.module, 0, this.module.lowerBound));
        NetworkHandler.sendToServer(new PacketUpdatePressureModule(this.module, 1, this.module.higherBound));
        super.onGuiClosed();
    }
}
