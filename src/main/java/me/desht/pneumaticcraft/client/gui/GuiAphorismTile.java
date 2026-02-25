package me.desht.pneumaticcraft.client.gui;

import me.desht.pneumaticcraft.client.util.GuiUtils;
import me.desht.pneumaticcraft.common.config.ConfigHandler;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketAphorismTileUpdate;
import me.desht.pneumaticcraft.common.tileentity.TileEntityAphorismTile;
import me.desht.pneumaticcraft.common.util.DramaSplash;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ChatAllowedCharacters;
import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.List;

public class GuiAphorismTile extends GuiScreen {
    public final TileEntityAphorismTile tile;
    private String[] textLines;
    public int cursorY;
    public int cursorX;
    public int updateCounter;

    public GuiAphorismTile(TileEntityAphorismTile tile) {
        this.tile = tile;
        this.textLines = tile.getTextLines();
        if (ConfigHandler.client.aphorismDrama && this.textLines.length == 1 && this.textLines[0].equals("")) {
            List<String> l = PneumaticCraftUtils.convertStringIntoList(DramaSplash.getInstance().getSplash(), 20);
            tile.setTextLines(l.toArray(new String[0]));
        }
        NetworkHandler.sendToServer(new PacketAphorismTileUpdate(tile));
    }

    @Override
    public void updateScreen() {
        this.updateCounter++;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (Keyboard.isKeyDown(Keyboard.KEY_F1)) {
            GuiUtils.showPopupHelpScreen(this, this.fontRenderer,
                    PneumaticCraftUtils.convertStringIntoList(I18n.format("gui.aphorismTile.helpText"), 40));
        }
    }

    @Override
    protected void keyTyped(char par1, int par2) throws IOException {
        if (par2 == Keyboard.KEY_ESCAPE) {
            NetworkHandler.sendToServer(new PacketAphorismTileUpdate(this.tile));
        } else if (par2 == Keyboard.KEY_LEFT || par2 == Keyboard.KEY_UP) {
            this.cursorY--;
            if (this.cursorY < 0) this.cursorY = this.textLines.length - 1;
        } else if (par2 == Keyboard.KEY_DOWN || par2 == Keyboard.KEY_NUMPADENTER) {
            this.cursorY++;
            if (this.cursorY >= this.textLines.length) this.cursorY = 0;
        } else if (par2 == Keyboard.KEY_RETURN) {
            this.cursorY++;
            this.textLines = ArrayUtils.add(this.textLines, this.cursorY, "");
        } else if (par2 == Keyboard.KEY_BACK) {
            if (this.textLines[this.cursorY].length() > 0) {
                this.textLines[this.cursorY] = this.textLines[this.cursorY].substring(0, this.textLines[this.cursorY].length() - 1);
                if (this.textLines[this.cursorY].endsWith("\u00a7")) {
                    this.textLines[this.cursorY] = this.textLines[this.cursorY].substring(0, this.textLines[this.cursorY].length() - 1);
                }
            } else if (this.textLines.length > 1) {
                this.textLines = ArrayUtils.remove(this.textLines, this.cursorY);
                this.cursorY--;
                if (this.cursorY < 0) this.cursorY = 0;
            }
        } else if (par2 == Keyboard.KEY_DELETE) {
            if (GuiScreen.isShiftKeyDown()) {
                this.textLines = new String[1];
                this.textLines[0] = "";
                this.cursorY = 0;
            } else {
                if (this.textLines.length > 1) {
                    this.textLines = ArrayUtils.remove(this.textLines, this.cursorY);
                    if (this.cursorY > this.textLines.length - 1)
                        this.cursorY = this.textLines.length - 1;
                }
            }
        } else if (ChatAllowedCharacters.isAllowedCharacter(par1)) {
            if (GuiScreen.isAltKeyDown()) {
                if (par1 >= 'a' && par1 <= 'f' || par1 >= 'l' && par1 <= 'o' || par1 == 'r' || par1 >= '0' && par1 <= '9') {
                    this.textLines[this.cursorY] = this.textLines[this.cursorY] + "\u00a7" + par1;
                }
            } else {
                this.textLines[this.cursorY] = this.textLines[this.cursorY] + par1;
            }
        }
        this.tile.setTextLines(this.textLines);
        super.keyTyped(par1, par2);
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }
}
