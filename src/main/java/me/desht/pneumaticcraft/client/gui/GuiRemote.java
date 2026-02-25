package me.desht.pneumaticcraft.client.gui;

import me.desht.pneumaticcraft.client.gui.widget.IGuiWidget;
import me.desht.pneumaticcraft.common.inventory.ContainerRemote;
import me.desht.pneumaticcraft.common.remote.ActionWidget;
import me.desht.pneumaticcraft.common.remote.ActionWidgetVariable;
import me.desht.pneumaticcraft.common.remote.RemoteLayout;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.item.ItemStack;

import java.awt.*;

public class GuiRemote extends GuiPneumaticContainerBase {

    protected RemoteLayout remoteLayout;
    protected final ItemStack remote;

    public GuiRemote(ItemStack remote, String texture) {
        super(new ContainerRemote(remote), null, texture);
        this.xSize = 183;
        this.ySize = 202;
        this.remote = remote;
    }

    public GuiRemote(ItemStack remote) {
        this(remote, Textures.GUI_WIDGET_OPTIONS_STRING);
    }

    @Override
    public void initGui() {
        this.remoteLayout = null;
        super.initGui();
        if (this.remoteLayout == null) this.remoteLayout = new RemoteLayout(this.remote, this.guiLeft, this.guiTop);
        this.addWidgets(this.remoteLayout.getWidgets(!(this instanceof GuiRemoteEditor)));
    }

    @Override
    protected Point getInvTextOffset() {
        return null;
    }

    @Override
    protected boolean shouldAddProblemTab() {
        return false;
    }

    @Override
    public void actionPerformed(IGuiWidget widget) {
        for (ActionWidget actionWidget : this.remoteLayout.getActionWidgets()) {
            if (actionWidget.getWidget() == widget && actionWidget instanceof ActionWidgetVariable) {
                this.onActionPerformed((ActionWidgetVariable) actionWidget);
            }
        }
    }

    protected void onActionPerformed(ActionWidgetVariable actionWidget) {
        actionWidget.onActionPerformed();
    }

    @Override
    public void onKeyTyped(IGuiWidget widget) {
        super.onKeyTyped(widget);
        for (ActionWidget actionWidget : this.remoteLayout.getActionWidgets()) {
            if (actionWidget.getWidget() == widget && actionWidget instanceof ActionWidgetVariable) {
                this.onKeyTyped((ActionWidgetVariable) actionWidget);
            }
        }
    }

    protected void onKeyTyped(ActionWidgetVariable actionWidget) {
        actionWidget.onKeyTyped();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    public void onGlobalVariableChange(String variable) {
        this.widgets.clear();
        this.initGui();
        for (ActionWidget actionWidget : this.remoteLayout.getActionWidgets()) {
            if (actionWidget instanceof ActionWidgetVariable) {
                ((ActionWidgetVariable) actionWidget).onVariableChange();
            }
        }
    }

    @Override
    protected boolean shouldParseVariablesInTooltips() {
        return true;
    }
}
