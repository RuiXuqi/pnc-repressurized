package me.desht.pneumaticcraft.client.gui.tubemodule;

import me.desht.pneumaticcraft.client.gui.GuiButtonSpecial;
import me.desht.pneumaticcraft.client.util.GuiUtils;
import me.desht.pneumaticcraft.common.block.tubes.ModuleAirGrate;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketUpdateAirGrateModule;
import me.desht.pneumaticcraft.common.util.EntityFilter;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class GuiAirGrateModule extends GuiTubeModule {
    private int sendTimer = 0;
    private GuiButtonSpecial warningButton;

    public GuiAirGrateModule(EntityPlayer player, int x, int y, int z) {
        super(player, x, y, z);
        this.ySize = 61;
    }

    private GuiTextField textfield;

    @Override
    public void initGui() {
        super.initGui();
        this.addLabel(I18n.format("gui.entityFilter"), this.guiLeft + 10, this.guiTop + 14);

        this.textfield = new GuiTextField(-1, this.fontRenderer, this.guiLeft + 10, this.guiTop + 25, 140, 10);
        this.textfield.setText(((ModuleAirGrate) this.module).getEntityFilterString());

        this.warningButton = new GuiButtonSpecial(3, this.guiLeft + 152, this.guiTop + 20, 20, 20, "");
        this.warningButton.setVisible(false);
        this.warningButton.setRenderedIcon(Textures.GUI_PROBLEMS_TEXTURE);
        this.buttonList.add(this.warningButton);

        this.textfield.setFocused(true);
        this.validateEntityFilter(this.textfield.getText());
    }

    private boolean validateEntityFilter(String filter) {
        try {
            this.warningButton.visible = false;
            this.warningButton.setTooltipText("");
            EntityFilter f = new EntityFilter(filter);  // syntax check
            return true;
        } catch (Exception e) {
            this.warningButton.visible = true;
            this.warningButton.setTooltipText(TextFormatting.GOLD + e.getMessage());
            return false;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (!this.textfield.isFocused()) this.textfield.setText(((ModuleAirGrate) this.module).getEntityFilterString());
        this.textfield.drawTextBox();
        if (Keyboard.isKeyDown(Keyboard.KEY_F1)) {
            GuiUtils.showPopupHelpScreen(this, this.fontRenderer,
                    PneumaticCraftUtils.convertStringIntoList(I18n.format("gui.entityFilter.helpText"), 60));
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int par3) throws IOException {
        super.mouseClicked(mouseX, mouseY, par3);
        this.textfield.mouseClicked(mouseX, mouseY, par3);
    }

    @Override
    public void keyTyped(char par1, int par2) throws IOException {
        if (this.textfield.isFocused() && par2 != Keyboard.KEY_ESCAPE) {
            this.textfield.textboxKeyTyped(par1, par2);
            String filterStr = this.textfield.getText();
            if (this.validateEntityFilter(filterStr)) {
                this.sendTimer = 5;  // delayed send to reduce packet spam while typing
            }
        } else {
            super.keyTyped(par1, par2);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (this.sendTimer > 0 && --this.sendTimer == 0) {
            NetworkHandler.sendToServer(new PacketUpdateAirGrateModule(this.module, this.textfield.getText()));
        }
    }

    @Override
    protected ResourceLocation getTexture() {
        return Textures.GUI_TEXT_WIDGET;
    }

}
