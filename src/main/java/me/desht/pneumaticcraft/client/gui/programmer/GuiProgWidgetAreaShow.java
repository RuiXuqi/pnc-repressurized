package me.desht.pneumaticcraft.client.gui.programmer;

import me.desht.pneumaticcraft.client.AreaShowManager;
import me.desht.pneumaticcraft.client.gui.GuiProgrammer;
import me.desht.pneumaticcraft.common.progwidgets.IAreaProvider;
import me.desht.pneumaticcraft.common.progwidgets.IProgWidget;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;

import java.io.IOException;

public class GuiProgWidgetAreaShow<Widget extends IProgWidget> extends GuiProgWidgetOptionBase<Widget> {

    public GuiProgWidgetAreaShow(Widget widget, GuiProgrammer guiProgrammer) {
        super(widget, guiProgrammer);
    }

    @Override
    public void initGui() {
        super.initGui();

        if (this.showShowAreaButtons() && this.widget instanceof IAreaProvider) {
            this.buttonList.add(new GuiButton(1000, this.guiLeft + this.xSize / 2 - 50, this.guiTop + 150, 100, 20, I18n.format("gui.programmer.button.showArea")));
            if (AreaShowManager.getInstance().isShowing(this.guiProgrammer.te))
                this.buttonList.add(new GuiButton(1001, this.guiLeft + this.xSize / 2 - 50, this.guiTop + 175, 100, 20, I18n.format("gui.programmer.button.stopShowingArea")));
        }
    }

    @Override
    public void actionPerformed(GuiButton button) throws IOException {
        if (this.showShowAreaButtons() && this.widget instanceof IAreaProvider) {
            if (button.id == 1000) {
                if (!AreaShowManager.getInstance().isShowing(this.guiProgrammer.te))
                    this.buttonList.add(new GuiButton(1001, this.guiLeft + this.xSize / 2 - 50, this.guiTop + 175, 100, 20, I18n.format("gui.programmer.button.stopShowingArea")));
                this.guiProgrammer.te.previewArea(this.widget.getX(), this.widget.getY());
                return;
            } else if (button.id == 1001) {
                AreaShowManager.getInstance().removeHandlers(this.guiProgrammer.te);
                this.buttonList.remove(button);
                return;
            }
        }
        // PacketDispatcher.sendPacketToServer(PacketHandlerPneumaticCraft.showDroneArea(guiProgrammer.te.getPos().getX(), guiProgrammer.te.getPos().getY(), guiProgrammer.te.getPos().getZ(), widget.getX(), widget.getY()));
        super.actionPerformed(button);
    }

    public boolean showShowAreaButtons() {
        return true;
    }
}
