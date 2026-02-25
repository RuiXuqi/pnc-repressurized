package me.desht.pneumaticcraft.client.gui;

import me.desht.pneumaticcraft.client.gui.widget.GuiCheckBox;
import me.desht.pneumaticcraft.client.gui.widget.IGuiWidget;
import me.desht.pneumaticcraft.client.gui.widget.WidgetComboBox;
import me.desht.pneumaticcraft.common.config.ConfigHandler;
import me.desht.pneumaticcraft.common.item.ItemRemote;
import me.desht.pneumaticcraft.common.item.Itemss;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketUpdateRemoteLayout;
import me.desht.pneumaticcraft.common.remote.*;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.client.FMLClientHandler;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiRemoteEditor extends GuiRemote {
    private GuiInventorySearcher invSearchGui;
    private GuiPastebin pastebinGui;
    private final List<ActionWidget> visibleSpawnWidgets = new ArrayList<>();
    private boolean wasClicking;
    private ActionWidget draggingWidget;
    private int dragMouseStartX, dragMouseStartY;
    private int dragWidgetStartX, dragWidgetStartY;
    private int oldGuiLeft, oldGuiTop;

    public GuiRemoteEditor(ItemStack remote) {
        super(remote, Textures.GUI_REMOTE_EDITOR);
        this.xSize = 283;
    }

    @Override
    public void initGui() {
        if (this.pastebinGui != null && this.pastebinGui.outputTag != null) {
            NBTTagCompound tag = this.remote.getTagCompound();
            if (tag == null) {
                tag = new NBTTagCompound();
                this.remote.setTagCompound(tag);
            }
            tag.setTag("actionWidgets", this.pastebinGui.outputTag.getTagList("main", 10));
        } else if (this.remoteLayout != null) {
            NBTTagCompound tag = this.remote.getTagCompound();
            if (tag == null) {
                tag = new NBTTagCompound();
                this.remote.setTagCompound(tag);
            }
            tag.setTag("actionWidgets", this.remoteLayout.toNBT(this.oldGuiLeft, this.oldGuiTop).getTagList("actionWidgets", 10));
        }

        if (this.invSearchGui != null && this.invSearchGui.getSearchStack() != null && this.invSearchGui.getSearchStack().getItem() == Itemss.REMOTE) {
            if (ItemRemote.hasSameSecuritySettings(this.remote, this.invSearchGui.getSearchStack())) {
                this.remoteLayout = new RemoteLayout(this.invSearchGui.getSearchStack(), this.guiLeft, this.guiTop);
            } else {
                this.mc.player.sendStatusMessage(new TextComponentString("gui.remote.differentSecuritySettings"), false);
            }
        }
        super.initGui();

        this.oldGuiLeft = this.guiLeft;
        this.oldGuiTop = this.guiTop;
        this.visibleSpawnWidgets.clear();
        this.visibleSpawnWidgets.add(new ActionWidgetCheckBox(new GuiCheckBox(-1, this.guiLeft + 200, this.guiTop + 20, 0xFF404040, I18n.format("remote.checkbox.name"))));
        this.visibleSpawnWidgets.add(new ActionWidgetLabel(new WidgetLabelVariable(this.guiLeft + 200, this.guiTop + 35, I18n.format("remote.label.name"))));
        this.visibleSpawnWidgets.add(new ActionWidgetButton(new GuiButtonSpecial(-1, this.guiLeft + 200, this.guiTop + 50, 50, 20, I18n.format("remote.button.name"))));
        this.visibleSpawnWidgets.add(new ActionWidgetDropdown(new WidgetComboBox(this.fontRenderer, this.guiLeft + 200, this.guiTop + 80, 70, this.fontRenderer.FONT_HEIGHT + 1).setFixedOptions()));

        for (ActionWidget actionWidget : this.visibleSpawnWidgets) {
            this.addWidget(actionWidget.getWidget());
        }

        GuiButtonSpecial importRemoteButton = new GuiButtonSpecial(0, this.guiLeft - 24, this.guiTop, 20, 20, "");
        importRemoteButton.setTooltipText(I18n.format("gui.remote.button.importRemoteButton"));
        importRemoteButton.setRenderStacks(new ItemStack(Itemss.REMOTE));
        this.buttonList.add(importRemoteButton);

        GuiButtonSpecial pastebinButton = new GuiButtonSpecial(1, this.guiLeft - 24, this.guiTop + 22, 20, 20, "");
        pastebinButton.setTooltipText(I18n.format("gui.remote.button.pastebinButton"));
        pastebinButton.setRenderedIcon(Textures.GUI_PASTEBIN_ICON_LOCATION);
        this.buttonList.add(pastebinButton);

        GuiCheckBox snapCheck = new GuiCheckBox(1000, this.guiLeft + 200, this.guiTop + 100, 0xFF404040, "Snap to Grid");
        snapCheck.checked = ConfigHandler.client.guiRemoteGridSnap;
        this.addWidget(snapCheck);
    }

    @Override
    public void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            this.invSearchGui = new GuiInventorySearcher(FMLClientHandler.instance().getClient().player);
            FMLClientHandler.instance().showGuiScreen(this.invSearchGui);
        } else if (button.id == 1) {
            NBTTagCompound mainTag = new NBTTagCompound();
            mainTag.setTag("main", this.remote.getTagCompound() != null ? this.remote.getTagCompound().getTagList("actionWidgets", 10) : new NBTTagCompound());
            FMLClientHandler.instance().showGuiScreen(this.pastebinGui = new GuiPastebin(this, mainTag));
        }
    }

    @Override
    protected boolean shouldDrawBackground() {
        return false;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int x, int y) {
        this.drawDefaultBackground();
        this.bindGuiTexture();
        Gui.drawModalRectWithCustomSizedTexture(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize, 320, 256);
        super.drawGuiContainerBackgroundLayer(partialTicks, x, y);

        x += this.guiLeft;
        y += this.guiTop;

        boolean isLeftClicking = Mouse.isButtonDown(0);
        boolean isMiddleClicking = Mouse.isButtonDown(2);

        if (this.draggingWidget != null) {
            int x1 = x - this.dragMouseStartX + this.dragWidgetStartX - this.guiLeft;
            int y1 = y - this.dragMouseStartY + this.dragWidgetStartY - this.guiTop;
            if (ConfigHandler.client.guiRemoteGridSnap) {
                x1 = (x1 / 4) * 4;
                y1 = (y1 / 4) * 4;
            }
            this.draggingWidget.setWidgetPos(x1, y1);
        }

        if (isLeftClicking && !this.wasClicking) {
            for (ActionWidget widget : this.visibleSpawnWidgets) {
                Rectangle bounds = widget.getWidget().getBounds();
                if (x >= bounds.x + this.guiLeft && y >= bounds.y + this.guiTop && x <= bounds.x + this.guiLeft + bounds.width && y <= bounds.y + this.guiTop + bounds.height) {
                    this.draggingWidget = widget.copy();
                    this.remoteLayout.addWidget(this.draggingWidget);
                    this.addWidget(this.draggingWidget.getWidget());
                    this.dragMouseStartX = x - this.guiLeft;
                    this.dragMouseStartY = y - this.guiTop;
                    this.dragWidgetStartX = bounds.x;
                    this.dragWidgetStartY = bounds.y;
                    break;
                }
            }
            if (this.draggingWidget == null) {
                for (ActionWidget widget : this.remoteLayout.getActionWidgets()) {
                    Rectangle bounds = widget.getWidget().getBounds();
                    if (x >= bounds.x + this.guiLeft && y >= bounds.y + this.guiTop && x <= bounds.x + this.guiLeft + bounds.width && y <= bounds.y + this.guiTop + bounds.height) {
                        this.draggingWidget = widget;
                        this.dragMouseStartX = x - this.guiLeft;
                        this.dragMouseStartY = y - this.guiTop;
                        this.dragWidgetStartX = bounds.x;
                        this.dragWidgetStartY = bounds.y;
                        break;
                    }
                }
            }
        } else if (isMiddleClicking && !this.wasClicking) {
            for (ActionWidget widget : this.remoteLayout.getActionWidgets()) {
                Rectangle bounds = widget.getWidget().getBounds();
                if (x >= bounds.x + this.guiLeft && y >= bounds.y + this.guiTop && x <= bounds.x + this.guiLeft + bounds.width && y <= bounds.y + this.guiTop + bounds.height) {
                    this.draggingWidget = widget.copy();
                    this.remoteLayout.addWidget(this.draggingWidget);
                    this.addWidget(this.draggingWidget.getWidget());
                    this.dragMouseStartX = 0;
                    this.dragMouseStartY = 0;
                    this.dragWidgetStartX = bounds.x - (x - this.guiLeft);
                    this.dragWidgetStartY = bounds.y - (y - this.guiTop);
                    break;
                }
            }
        }

        if (!isLeftClicking && !isMiddleClicking && this.draggingWidget != null) {
            if (this.isOutsideProgrammingArea(this.draggingWidget)) {
                this.remoteLayout.getActionWidgets().remove(this.draggingWidget);
                this.removeWidget(this.draggingWidget.getWidget());
            }
            this.draggingWidget = null;
        }
        this.wasClicking = isLeftClicking || isMiddleClicking;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int x, int y) {
        super.drawGuiContainerForegroundLayer(x, y);
        this.fontRenderer.drawString("Widget Tray", 194, 8, 0x404040);
    }

    private boolean isOutsideProgrammingArea(ActionWidget widget) {
        Rectangle bounds = widget.getWidget().getBounds();
        return !new Rectangle(this.guiLeft, this.guiTop, 183, this.ySize).contains(bounds);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton == 1) {
            for (ActionWidget widget : this.remoteLayout.getActionWidgets()) {
                if (!this.isOutsideProgrammingArea(widget)) {
                    Rectangle bounds = widget.getWidget().getBounds();
                    if (mouseX >= bounds.x && mouseY >= bounds.y && mouseX <= bounds.x + bounds.width && mouseY <= bounds.y + bounds.height) {
                        GuiScreen screen = widget.getGui(this);
                        if (screen != null) this.mc.displayGuiScreen(screen);
                    }
                }
            }
        }
    }

    @Override
    public void actionPerformed(IGuiWidget widget) {
        if (widget.getID() == 1000) {
            ConfigHandler.client.guiRemoteGridSnap = ((GuiCheckBox) widget).checked;
            ConfigHandler.sync();
        } else {
            super.actionPerformed(widget);
        }
    }

    @Override
    protected void onActionPerformed(ActionWidgetVariable actionWidget) {
        actionWidget.onVariableChange();
    }

    @Override
    public void onGlobalVariableChange(String variable) {
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        NetworkHandler.sendToServer(new PacketUpdateRemoteLayout(this.remoteLayout.toNBT(this.guiLeft, this.guiTop)));
    }
}
