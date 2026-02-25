package me.desht.pneumaticcraft.client.gui.programmer;

import me.desht.pneumaticcraft.client.gui.GuiPneumaticScreenBase;
import me.desht.pneumaticcraft.client.gui.GuiProgrammer;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketProgrammerUpdate;
import me.desht.pneumaticcraft.common.progwidgets.IProgWidget;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class GuiProgWidgetOptionBase<Widget extends IProgWidget> extends GuiPneumaticScreenBase {
    protected final Widget widget;
    protected final GuiProgrammer guiProgrammer;

    public GuiProgWidgetOptionBase(Widget widget, GuiProgrammer guiProgrammer) {
        this.widget = widget;
        this.guiProgrammer = guiProgrammer;
        this.xSize = 183;
        this.ySize = 202;
    }

    @Override
    public void keyTyped(char key, int keyCode) throws IOException {
        super.keyTyped(key, keyCode);
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.onGuiClosed();
            if (this.guiProgrammer != null) {
                NetworkHandler.sendToServer(new PacketProgrammerUpdate(this.guiProgrammer.te));
                this.mc.displayGuiScreen(this.guiProgrammer);
            }
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        String title = TextFormatting.UNDERLINE + I18n.format("programmingPuzzle." + this.widget.getWidgetString() + ".name");
        this.addLabel(title, this.width / 2 - this.fontRenderer.getStringWidth(title) / 2, this.guiTop + 5);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected ResourceLocation getTexture() {
        return Textures.GUI_WIDGET_OPTIONS;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
