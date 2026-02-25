package me.desht.pneumaticcraft.client.gui;

import com.mojang.authlib.GameProfile;
import me.desht.pneumaticcraft.client.gui.widget.GuiAnimatedStat;
import me.desht.pneumaticcraft.client.gui.widget.IGuiWidget;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTextField;
import me.desht.pneumaticcraft.common.block.Blockss;
import me.desht.pneumaticcraft.common.inventory.ContainerSecurityStationInventory;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketSecurityStationAddUser;
import me.desht.pneumaticcraft.common.network.PacketUpdateTextfield;
import me.desht.pneumaticcraft.common.tileentity.TileEntitySecurityStation;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class GuiSecurityStationInventory extends GuiSecurityStationBase {
    private GuiAnimatedStat statusStat;
    private GuiAnimatedStat accessStat;

    private GuiButtonSpecial addButton;
    private GuiButton rebootButton;
    private WidgetTextField sharedUserTextField;
    private List<GuiButtonSpecial> removeUserButtons;
    private NetworkConnectionHandler nodeHandler;

    public GuiSecurityStationInventory(InventoryPlayer player, TileEntitySecurityStation te) {

        super(new ContainerSecurityStationInventory(player, te), te, Textures.GUI_SECURITY_STATION);
        this.ySize = 239;
    }

    @Override
    public void initGui() {
        super.initGui();

        int xStart = (this.width - this.xSize) / 2;
        int yStart = (this.height - this.ySize) / 2;

        this.statusStat = this.addAnimatedStat("Security Status", new ItemStack(Blockss.SECURITY_STATION), 0xFFFFAA00, false);
        this.accessStat = this.addAnimatedStat("Shared Users", new ItemStack(Items.SKULL, 1, 3), 0xFF005500, false);

        Rectangle accessButtonRectangle = this.accessStat.getButtonScaledRectangle(145, 10, 20, 20);
        this.addButton = this.getButtonFromRectangle(1, accessButtonRectangle, "+");
        this.rebootButton = new GuiButton(2, xStart + 110, yStart + 20, 60, 20, "Reboot");
        this.sharedUserTextField = this.getTextFieldFromRectangle(this.accessStat.getButtonScaledRectangle(20, 15, 120, 10));
        this.accessStat.addWidget(this.sharedUserTextField);
        this.accessStat.addWidget(this.addButton);

        GuiButtonSpecial testButton = new GuiButtonSpecial(3, this.guiLeft + 108, this.guiTop + 103, 64, 20, I18n.format("gui.securityStation.test"));
        testButton.setTooltipText(I18n.format("gui.securityStation.test.tooltip"));
        this.buttonList.add(testButton);
        this.buttonList.add(this.rebootButton);
        this.buttonList.add(new GuiButton(-1, this.guiLeft + 108, this.guiTop + 125, 64, 20, I18n.format("gui.universalSensor.button.showRange")));

        this.updateUserRemoveButtons();

        this.nodeHandler = new NetworkConnectionBackground(this, this.te, xStart + 25, yStart + 30, 18, 0xFF2222FF);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int x, int y) {
        super.drawGuiContainerForegroundLayer(x, y);
        this.fontRenderer.drawString("Network Layout", 15, 12, 4210752);
        this.fontRenderer.drawString("Upgr.", 133, 52, 4210752);
    }

    @Override
    protected Point getInvTextOffset() {
        return new Point(0, 2);
    }

    @Override
    protected Point getInvNameOffset() {
        return new Point(0, -2);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float opacity, int x, int y) {
        super.drawGuiContainerBackgroundLayer(opacity, x, y);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.nodeHandler.render();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        this.statusStat.setText(this.getStatusText());
        this.accessStat.setTextWithoutCuttingString(this.getAccessText());
        String rebootButtonString;
        if (this.te.getRebootTime() > 0) {
            rebootButtonString = this.te.getRebootTime() % 100 < 50 ? TextFormatting.YELLOW + "Rebooting.." : PneumaticCraftUtils.convertTicksToMinutesAndSeconds(this.te.getRebootTime(), false);
        } else {
            rebootButtonString = "Reboot";
        }

        this.rebootButton.displayString = rebootButtonString;

        this.addButton.visible = this.accessStat.isDoneExpanding();
        for (GuiButton button : this.removeUserButtons) {
            button.enabled = this.accessStat.isDoneExpanding();
        }
        if (this.removeUserButtons.size() != this.te.sharedUsers.size()) {
            this.updateUserRemoveButtons();
        }
    }

    @Override
    protected void addProblems(List<String> text) {
        super.addProblems(text);
        if (this.te.getRebootTime() > 0) {
            text.add(TextFormatting.WHITE + "The Security Station doesn't provide security!");
            text.add(TextFormatting.BLACK + "The station is rebooting (" + PneumaticCraftUtils.convertTicksToMinutesAndSeconds(this.te.getRebootTime(), false) + ").");
        } else if (this.te.isHacked()) {
            text.add(TextFormatting.WHITE + "This Station has been hacked!");
            text.add(TextFormatting.BLACK + "Reboot the station.");
        }
        if (!this.te.hasValidNetwork()) {
            text.add(TextFormatting.GRAY + "Invalid network configuration!");
            switch (this.te.checkForNetworkValidity()) {
                case NO_SUBROUTINE:
                    text.add(TextFormatting.BLACK + "Add a Diagnostic Subroutine.");
                    break;
                case NO_IO_PORT:
                    text.add(TextFormatting.BLACK + "Add a Network IO Port.");
                    break;
                case NO_REGISTRY:
                    text.add(TextFormatting.BLACK + "Add a Network Registry.");
                    break;
                case TOO_MANY_SUBROUTINES:
                    text.add(TextFormatting.BLACK + "There can only be one Diagnostic Subroutine.");
                    break;
                case TOO_MANY_IO_PORTS:
                    text.add(TextFormatting.BLACK + "There can only be one Network IO Port.");
                    break;
                case TOO_MANY_REGISTRIES:
                    text.add(TextFormatting.BLACK + "There can only be one Network Registry.");
                    break;
                case NO_CONNECTION_SUB_AND_IO_PORT:
                    text.add(TextFormatting.BLACK + "The Diagnostic Subroutine and the Network IO Port need to be connected in the network.");
                    break;
                case NO_CONNECTION_IO_PORT_AND_REGISTRY:
                    text.add(TextFormatting.BLACK + "The Network Registry and the Network IO Port need to be connected in the network.");
                    break;
            }
        }
    }

    private List<String> getStatusText() {
        List<String> text = new ArrayList<>();
        text.add(TextFormatting.WHITE + "Protection");
        if (this.te.getRebootTime() > 0) {
            text.add(TextFormatting.DARK_RED + "No protection because of rebooting!");
        } else if (this.te.isHacked()) {
            text.add(TextFormatting.DARK_RED + "Hacked by:");
            for (GameProfile hacker : this.te.hackedUsers) {
                text.add(TextFormatting.DARK_RED + "\u2022 " + hacker.getName());
            }
        } else {
            text.add(TextFormatting.BLACK + "System secure");
        }
        text.add(TextFormatting.WHITE + "Security Level");
        text.add(TextFormatting.BLACK + "Level " + this.te.getSecurityLevel());
        text.add(TextFormatting.WHITE + "Intruder Detection Chance");
        text.add(TextFormatting.BLACK.toString() + this.te.getDetectionChance() + "%%");
        text.add(TextFormatting.WHITE + "Security Range");
        text.add(TextFormatting.BLACK.toString() + this.te.getSecurityRange() + "m (square)");
        return text;
    }

    private List<String> getAccessText() {
        List<String> textList = new ArrayList<>();
        textList.add("                                      ");
        textList.add("");
        for (GameProfile user : this.te.sharedUsers) {
            textList.add(TextFormatting.WHITE + "\u2022 " + user.getName());
        }
        return textList;
    }

    @Override
    public void actionPerformed(IGuiWidget widget) {
        if (widget.getID() == 1 && !this.sharedUserTextField.getText().equals(""))
            NetworkHandler.sendToServer(new PacketSecurityStationAddUser(this.te, this.sharedUserTextField.getText()));
        super.actionPerformed(widget);
    }

    /**
     * Fired when a control is clicked. This is the equivalent of
     * ActionListener.actionPerformed(ActionEvent e).
     */
    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 2) {
            this.te.rebootStation();
        } else if (button.id == -1) {
            this.te.showRangeLines();
        }

        super.actionPerformed(button);
    }

    @Override
    public void onKeyTyped(IGuiWidget widget) {
        this.te.setText(0, this.sharedUserTextField.getText());
        NetworkHandler.sendToServer(new PacketUpdateTextfield(this.te, 0));
    }

    private void updateUserRemoveButtons() {
        if (this.removeUserButtons != null) {
            for (GuiButtonSpecial button : this.removeUserButtons) {
                this.accessStat.removeWidget(button);
            }
        }
        this.removeUserButtons = new ArrayList<>();
        for (int i = 0; i < this.te.sharedUsers.size(); i++) {
            Rectangle rect = this.accessStat.getButtonScaledRectangle(24, 30 + i * 10, this.fontRenderer.getStringWidth(this.te.sharedUsers.get(i).getName()), 8);
            GuiButtonSpecial button = this.getInvisibleButtonFromRectangle(4 + i, rect);
            button.setInvisibleHoverColor(0x44FF0000);
            button.setVisible(false);
            this.accessStat.addWidget(button);
            this.removeUserButtons.add(button);
            if (this.te.sharedUsers.get(i).getName().equals(FMLClientHandler.instance().getClient().player.getGameProfile().getName())) {
                button.visible = false;
            }
        }
    }
}
