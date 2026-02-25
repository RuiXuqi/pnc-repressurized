package me.desht.pneumaticcraft.client.gui;

import me.desht.pneumaticcraft.client.gui.widget.WidgetTextField;
import me.desht.pneumaticcraft.client.util.GuiUtils;
import me.desht.pneumaticcraft.common.inventory.ContainerSentryTurret;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketUpdateTextfield;
import me.desht.pneumaticcraft.common.tileentity.TileEntitySentryTurret;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.List;

@SideOnly(Side.CLIENT)
public class GuiSentryTurret extends GuiPneumaticContainerBase<TileEntitySentryTurret> {

    private WidgetTextField entityFilter;

    public GuiSentryTurret(InventoryPlayer player, TileEntitySentryTurret te) {

        super(new ContainerSentryTurret(player, te), te, Textures.GUI_SENTRY_TURRET);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.addWidget(this.entityFilter = new WidgetTextField(this.fontRenderer, this.guiLeft + 80, this.guiTop + 63, 70, this.fontRenderer.FONT_HEIGHT));

    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (!this.entityFilter.isFocused()) this.entityFilter.setText(this.te.getText(0));
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (!this.entityFilter.isFocused()) {
            this.te.setText(0, this.entityFilter.getText());
            NetworkHandler.sendToServer(new PacketUpdateTextfield(this.te, 0));
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();

        this.te.setText(0, this.entityFilter.getText());
        NetworkHandler.sendToServer(new PacketUpdateTextfield(this.te, 0));
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int x, int y) {
        super.drawGuiContainerForegroundLayer(x, y);
        this.fontRenderer.drawString("Upgr.", 28, 19, 4210752);
        this.fontRenderer.drawString(I18n.format("gui.sentryTurret.ammo"), 80, 19, 4210752);
        this.fontRenderer.drawString(I18n.format("gui.sentryTurret.targetFilter"), 80, 53, 4210752);
        if (Keyboard.isKeyDown(Keyboard.KEY_F1)) {
            GuiUtils.showPopupHelpScreen(this, this.fontRenderer,
                    PneumaticCraftUtils.convertStringIntoList(I18n.format("gui.entityFilter.helpText"), 60));
        } else if (x >= this.guiLeft + 76 && y >= this.guiTop + 51 && x <= this.guiLeft + 153 && y <= this.guiTop + 74) {
            // cursor inside the entity filter area
            String str = I18n.format("gui.entityFilter");
            this.fontRenderer.drawString(str, (this.xSize - this.fontRenderer.getStringWidth(str)) / 2, this.ySize + 5, 0x808080);
        }
    }

    @Override
    protected void addProblems(List<String> curInfo) {
        super.addProblems(curInfo);

        boolean hasAmmo = false;
        for (int i = 0; i < this.te.getPrimaryInventory().getSlots(); i++) {
            if (!this.te.getPrimaryInventory().getStackInSlot(i).isEmpty()) {
                hasAmmo = true;
                break;
            }
        }
        if (!hasAmmo) curInfo.add("gui.tab.problems.sentryTurret.noAmmo");
    }

}
