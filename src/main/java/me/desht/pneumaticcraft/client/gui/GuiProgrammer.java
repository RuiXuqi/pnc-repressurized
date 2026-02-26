package me.desht.pneumaticcraft.client.gui;

import com.google.common.base.CaseFormat;
import me.desht.pneumaticcraft.PneumaticCraftRepressurized;
import me.desht.pneumaticcraft.client.gui.widget.GuiCheckBox;
import me.desht.pneumaticcraft.client.gui.widget.GuiRadioButton;
import me.desht.pneumaticcraft.client.gui.widget.IGuiWidget;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTextField;
import me.desht.pneumaticcraft.common.config.ConfigHandler;
import me.desht.pneumaticcraft.common.inventory.ContainerProgrammer;
import me.desht.pneumaticcraft.common.item.ItemGPSAreaTool;
import me.desht.pneumaticcraft.common.item.ItemGPSTool;
import me.desht.pneumaticcraft.common.item.Itemss;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketGuiButton;
import me.desht.pneumaticcraft.common.network.PacketProgrammerUpdate;
import me.desht.pneumaticcraft.common.network.PacketUpdateTextfield;
import me.desht.pneumaticcraft.common.progwidgets.*;
import me.desht.pneumaticcraft.common.thirdparty.ThirdPartyManager;
import me.desht.pneumaticcraft.common.tileentity.TileEntityProgrammer;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

@SideOnly(Side.CLIENT)
public class GuiProgrammer extends GuiPneumaticContainerBase<TileEntityProgrammer> {
    private final EntityPlayer player;
    private GuiPastebin pastebinGui;

    private GuiButtonSpecial importButton;
    private GuiButtonSpecial exportButton;
    private GuiButtonSpecial allWidgetsButton;
    private List<GuiRadioButton> difficultyButtons;
    private GuiCheckBox showInfo, showFlow;
    private WidgetTextField nameField;
    private WidgetTextField filterField;
    private GuiButtonSpecial undoButton, redoButton;
    private GuiButtonSpecial convertToRelativeButton;

    private final List<IProgWidget> visibleSpawnWidgets = new ArrayList<>();
    private BitSet filteredSpawnWidgets;

    private GuiUnitProgrammer programmerUnit;
    private boolean wasClicking;
    private boolean wasFocused;
    private IProgWidget draggingWidget;
    private int lastMouseX, lastMouseY;
    private int dragMouseStartX, dragMouseStartY;
    private int dragWidgetStartX, dragWidgetStartY;
    private static final int FAULT_MARGIN = 4;
    private int widgetPage;
    private int maxPage;

    private boolean showingAllWidgets;
    private int showingWidgetProgress;
    private int oldShowingWidgetProgress;

    private static final Rectangle PROGRAMMER_STD_RES = new Rectangle(5, 17, 294, 154);
    private static final Rectangle PROGRAMMER_HI_RES = new Rectangle(5, 17, 644, 410);

    private static final int WIDGET_X_SPACING = 22; // x size of widgets in the widget tray

    private final boolean hiRes;

    public GuiProgrammer(InventoryPlayer player, TileEntityProgrammer te) {
        super(new ContainerProgrammer(player, te, isScreenHiRes()), te, null);

        this.hiRes = ((ContainerProgrammer) this.inventorySlots).isHiRes();
        this.xSize = this.hiRes ? 700 : 350;
        this.ySize = this.hiRes ? 512 : 256;

        this.player = FMLClientHandler.instance().getClient().player;
    }

    private static boolean isScreenHiRes() {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        return sr.getScaledWidth() > 700 && sr.getScaledHeight() > 512;
    }

    private Rectangle getProgrammerBounds() {
        return this.hiRes ? PROGRAMMER_HI_RES : PROGRAMMER_STD_RES;
    }

    private int getWidgetTrayRight() {
        return this.hiRes ? 672 : 322;
    }

    @Override
    protected void bindGuiTexture() {
        ResourceLocation res = new ResourceLocation(this.hiRes ? Textures.GUI_PROGRAMMER_LARGE : Textures.GUI_PROGRAMMER_STD);
        this.mc.getTextureManager().bindTexture(res);
        GlStateManager.enableTexture2D();
    }

    private void updateVisibleProgWidgets() {
        int y = 0, page = 0;
        int x = this.getWidgetTrayRight() - this.maxPage * WIDGET_X_SPACING;
        boolean showAllWidgets = this.showingWidgetProgress == WIDGET_X_SPACING * this.maxPage && this.showingAllWidgets;
        this.filterField.setVisible(showAllWidgets);

        this.maxPage = 0;
        this.visibleSpawnWidgets.clear();
        int difficulty = 0;
        for (int i = 0; i < this.difficultyButtons.size(); i++) {
            if (this.difficultyButtons.get(i).checked) {
                difficulty = i;
                break;
            }
        }
        List<IProgWidget> registeredWidgets = WidgetRegistrator.registeredWidgets;
        for (int i = 0; i < registeredWidgets.size(); i++) {
            IProgWidget widget = registeredWidgets.get(i);
            if (difficulty >= widget.getDifficulty().ordinal()) {
                widget.setY(y + 40);
                widget.setX(showAllWidgets ? x : this.getWidgetTrayRight());
                int widgetHeight = widget.getHeight() / 2 + (widget.hasStepOutput() ? 5 : 0) + 1;
                y += widgetHeight;

                if (showAllWidgets || page == this.widgetPage) {
                    this.visibleSpawnWidgets.add(widget);
                }
                if (y > this.ySize - (this.hiRes ? 260 : 160)) {
                    y = 0;
                    x += WIDGET_X_SPACING;
                    page++;
                    if (i < registeredWidgets.size() - 1) this.maxPage++;
                }
            }
        }
        this.maxPage++;

        this.filterField.x = Math.min(this.guiLeft + this.getWidgetTrayRight() - 25 - this.filterField.width, this.guiLeft + this.getWidgetTrayRight() - (this.maxPage * WIDGET_X_SPACING) - 2);
        this.filterSpawnWidgets();

        if (this.widgetPage >= this.maxPage) {
            this.widgetPage = this.maxPage - 1;
            this.updateVisibleProgWidgets();
        }
    }

    private void filterSpawnWidgets() {
        String filterText = this.filterField.getText().trim();
        if (!this.visibleSpawnWidgets.isEmpty() && !filterText.isEmpty()) {
            this.filteredSpawnWidgets = new BitSet(this.visibleSpawnWidgets.size());
            for (int i = 0; i < this.visibleSpawnWidgets.size(); i++) {
                IProgWidget widget = this.visibleSpawnWidgets.get(i);
                String widgetName = I18n.format("programmingPuzzle." + widget.getWidgetString() + ".name");
                this.filteredSpawnWidgets.set(i, widgetName.toLowerCase().contains(filterText.toLowerCase()));
            }
        } else {
            this.filteredSpawnWidgets = null;
        }
    }

    @Override
    protected boolean shouldAddInfoTab() {
        return false;
    }

    @Override
    public void initGui() {
        boolean pastebinLoaded = false;

        if (this.pastebinGui != null && this.pastebinGui.outputTag != null) {
            this.te.readProgWidgetsFromNBT(this.pastebinGui.outputTag);
            this.pastebinGui = null;
            NetworkHandler.sendToServer(new PacketProgrammerUpdate(this.te));
            pastebinLoaded = true;
        }

        super.initGui();

        if (this.programmerUnit != null) {
            this.te.translatedX = this.programmerUnit.getTranslatedX();
            this.te.translatedY = this.programmerUnit.getTranslatedY();
            this.te.zoomState = this.programmerUnit.getLastZoom();
            if (pastebinLoaded) {
                this.programmerUnit.gotoPiece(findWidget(this.te.progWidgets, ProgWidgetStart.class));
            }
        }

        Rectangle bounds = this.getProgrammerBounds();
        this.programmerUnit = new GuiUnitProgrammer(this.te.progWidgets, this.fontRenderer, this.guiLeft, this.guiTop, this.xSize, this.width, this.height, bounds.x, bounds.y, bounds.width, bounds.height, this.te.translatedX, this.te.translatedY, this.te.zoomState);
        this.addWidget(this.programmerUnit.getScrollBar());

        int xStart = (this.width - this.xSize) / 2;
        int yStart = (this.height - this.ySize) / 2;

        // right and bottom edges of the programming area
        int xRight = this.getProgrammerBounds().x + this.getProgrammerBounds().width; // 299 or 649
        int yBottom = this.getProgrammerBounds().y + this.getProgrammerBounds().height + 3; // 171 or 427

        this.importButton = new GuiButtonSpecial(1, xStart + xRight + 2, yStart + 3, 20, 15, "\u27f5");
        this.importButton.setTooltipText("Import program");
        this.buttonList.add(this.importButton);

        this.exportButton = new GuiButtonSpecial(2, xStart + xRight + 2, yStart + 20, 20, 15, "\u27f6");
        this.buttonList.add(this.exportButton);

        this.buttonList.add(new GuiButtonExt(3, xStart + xRight - 3, yStart + yBottom, 10, 10, "\u25c0"));
        this.buttonList.add(new GuiButtonExt(4, xStart + xRight + 38, yStart + yBottom, 10, 10, "\u25b6"));

        this.allWidgetsButton = new GuiButtonSpecial(8, xStart + xRight + 22, yStart + yBottom - 16, 10, 10, "\u25e4");
        this.allWidgetsButton.setTooltipText(I18n.format("gui.programmer.button.openPanel.tooltip"));
        this.addWidget(this.allWidgetsButton);

        this.difficultyButtons = new ArrayList<>();
        for (int i = 0; i < IProgWidget.WidgetDifficulty.values().length; i++) {
            GuiRadioButton radioButton = new GuiRadioButton(i, xStart + xRight - 36, yStart + yBottom + 29 + i * 12, 0xFF404040, IProgWidget.WidgetDifficulty.values()[i].getLocalizedName());
            radioButton.checked = ConfigHandler.getProgrammerDifficulty() == i;
            this.addWidget(radioButton);
            this.difficultyButtons.add(radioButton);
            radioButton.otherChoices = this.difficultyButtons;
            if (i == 1) radioButton.setTooltip(I18n.format("gui.programmer.difficulty.medium.tooltip"));
            if (i == 2) radioButton.setTooltip(I18n.format("gui.programmer.difficulty.advanced.tooltip"));
        }

        this.buttonList.add(new GuiButtonExt(5, xStart + 5, yStart + yBottom + 4, 87, 20, I18n.format("gui.programmer.button.showStart")));
        this.buttonList.add(new GuiButtonExt(6, xStart + 5, yStart + yBottom + 26, 87, 20, I18n.format("gui.programmer.button.showLatest")));
        this.addWidget(this.showInfo = new GuiCheckBox(-1, xStart + 5, yStart + yBottom + 49, 0xFF404040, "gui.programmer.checkbox.showInfo").setChecked(this.te.showInfo));
        this.addWidget(this.showFlow = new GuiCheckBox(-1, xStart + 5, yStart + yBottom + 61, 0xFF404040, "gui.programmer.checkbox.showFlow").setChecked(this.te.showFlow));

        GuiButtonSpecial pastebinButton = new GuiButtonSpecial(7, this.guiLeft - 24, this.guiTop + 44, 20, 20, "");
        pastebinButton.setTooltipText(I18n.format("gui.remote.button.pastebinButton"));
        pastebinButton.setRenderedIcon(Textures.GUI_PASTEBIN_ICON_LOCATION);
        this.buttonList.add(pastebinButton);

        this.undoButton = new GuiButtonSpecial(9, this.guiLeft - 24, this.guiTop + 2, 20, 20, "");
        this.redoButton = new GuiButtonSpecial(10, this.guiLeft - 24, this.guiTop + 23, 20, 20, "");
        GuiButtonSpecial clearAllButton = new GuiButtonSpecial(11, this.guiLeft - 24, this.guiTop + 65, 20, 20, "");
        this.convertToRelativeButton = new GuiButtonSpecial(12, this.guiLeft - 24, this.guiTop + 86, 20, 20, "Rel");

        this.undoButton.setRenderedIcon(Textures.GUI_UNDO_ICON_LOCATION);
        this.redoButton.setRenderedIcon(Textures.GUI_REDO_ICON_LOCATION);
        clearAllButton.setRenderedIcon(Textures.GUI_DELETE_ICON_LOCATION);

        this.undoButton.setTooltipText(I18n.format("gui.programmer.button.undoButton.tooltip"));
        this.redoButton.setTooltipText(I18n.format("gui.programmer.button.redoButton.tooltip"));
        clearAllButton.setTooltipText(I18n.format("gui.programmer.button.clearAllButton.tooltip"));

        this.buttonList.add(this.undoButton);
        this.buttonList.add(this.redoButton);
        this.buttonList.add(clearAllButton);
        this.buttonList.add(this.convertToRelativeButton);

        String containerName = I18n.format(this.te.getName() + ".name");
        this.addLabel(containerName, this.guiLeft + 7, this.guiTop + 5, 0xFF404040);

        this.nameField = new WidgetTextField(this.fontRenderer, this.guiLeft + xRight - 99, this.guiTop + 5, 98, this.fontRenderer.FONT_HEIGHT);
        this.addWidget(this.nameField);

        this.filterField = new FilterTextField(this.fontRenderer, this.guiLeft + 78, this.guiTop + 26, 100, this.fontRenderer.FONT_HEIGHT);
        this.filterField.setListener(this);

        this.addWidget(this.filterField);

        String name = I18n.format("gui.programmer.name");
        this.addLabel(name, this.guiLeft + xRight - 102 - this.fontRenderer.getStringWidth(name), this.guiTop + 5, 0xFF404040);

        this.updateVisibleProgWidgets();

        for (IProgWidget widget : this.te.progWidgets) {
            if (!this.programmerUnit.isOutsideProgrammingArea(widget)) {
                return;
            }
        }
        this.programmerUnit.gotoPiece(findWidget(this.te.progWidgets, ProgWidgetStart.class));
    }

    @Override
    protected Point getInvNameOffset() {
        return null;
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
    protected void drawGuiContainerForegroundLayer(int x, int y) {
        super.drawGuiContainerForegroundLayer(x, y);

        int xRight = this.getProgrammerBounds().x + this.getProgrammerBounds().width; // 299 or 649
        int yBottom = this.getProgrammerBounds().y + this.getProgrammerBounds().height; // 171 or 427

        String str = this.widgetPage + 1 + "/" + this.maxPage;
        this.fontRenderer.drawString(str, xRight + (22 - this.fontRenderer.getStringWidth(str) / 2), yBottom + 4, 0xFF404040);
        this.fontRenderer.drawString(I18n.format("gui.programmer.difficulty"), xRight - 36, yBottom + 20, 0xFF404040);

        if (this.showingWidgetProgress == 0) {
            this.programmerUnit.renderForeground(x, y, this.draggingWidget);
        }

        for (int i = 0; i < this.visibleSpawnWidgets.size(); i++) {
            IProgWidget widget = this.visibleSpawnWidgets.get(i);
            if (widget != this.draggingWidget && x - this.guiLeft >= widget.getX()
                    && y - this.guiTop >= widget.getY() && x - this.guiLeft <= widget.getX() + widget.getWidth() / 2
                    && y - this.guiTop <= widget.getY() + widget.getHeight() / 2
                    && (!this.showingAllWidgets || this.filteredSpawnWidgets == null || this.filteredSpawnWidgets.get(i))) {
                List<String> tooltip = new ArrayList<>();
                widget.getTooltip(tooltip);
                ThirdPartyManager.instance().docsProvider.addTooltip(tooltip, this.showingAllWidgets);
                if (!tooltip.isEmpty()) {
                    this.drawHoveringString(tooltip, x - this.guiLeft, y - this.guiTop, this.fontRenderer);
                }
            }
        }

    }

    @Override
    protected void keyTyped(char key, int keyCode) throws IOException {
        super.keyTyped(key, keyCode);

        if (this.nameField.isFocused() || this.filterField.isFocused() && keyCode != Keyboard.KEY_TAB) {
            return;
        }

        if (Keyboard.KEY_I == keyCode) {
            this.showWidgetDocs();
        }
        if (Keyboard.KEY_R == keyCode) {
            if (this.exportButton.getBounds().contains(this.lastMouseX, this.lastMouseY)) {
                NetworkHandler.sendToServer(new PacketGuiButton(0));
            }
        }
        if (Keyboard.KEY_SPACE == keyCode || Keyboard.KEY_TAB == keyCode) {
            this.toggleShowWidgets();
        }
        if (Keyboard.KEY_DELETE == keyCode) {
            IProgWidget widget = this.programmerUnit.getHoveredWidget(this.lastMouseX, this.lastMouseY);
            if (widget != null) {
                this.te.progWidgets.remove(widget);
                NetworkHandler.sendToServer(new PacketProgrammerUpdate(this.te));
            }
        }
        if (Keyboard.KEY_Z == keyCode) {
            NetworkHandler.sendToServer(new PacketGuiButton(this.undoButton.id));
        }
        if (Keyboard.KEY_Y == keyCode) {
            NetworkHandler.sendToServer(new PacketGuiButton(this.redoButton.id));
        }
    }

    private void showWidgetDocs() {
        int x = this.lastMouseX;
        int y = this.lastMouseY;

        IProgWidget hoveredWidget = this.programmerUnit.getHoveredWidget(x, y);
        ThirdPartyManager.instance().docsProvider.showWidgetDocs(this.getWidgetId(hoveredWidget));
        for (IProgWidget widget : this.visibleSpawnWidgets) {
            if (widget != this.draggingWidget && x - this.guiLeft >= widget.getX() && y - this.guiTop >= widget.getY() && x - this.guiLeft <= widget.getX() + widget.getWidth() / 2 && y - this.guiTop <= widget.getY() + widget.getHeight() / 2) {
                ThirdPartyManager.instance().docsProvider.showWidgetDocs(this.getWidgetId(widget));
                break;
            }
        }
    }

    private String getWidgetId(IProgWidget w) {
        if (w == null) return null;
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, w.getWidgetString());
    }

    @Override
    protected boolean shouldDrawBackground() {
        return false;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int x, int y) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.drawDefaultBackground();
        this.bindGuiTexture();
        int xStart = (this.width - this.xSize) / 2;
        int yStart = (this.height - this.ySize) / 2;
        drawModalRectWithCustomSizedTexture(xStart, yStart, 0, 0, this.xSize, this.ySize, this.xSize, this.ySize);

        this.programmerUnit.getScrollBar().setEnabled(this.showingWidgetProgress == 0);
        super.drawGuiContainerBackgroundLayer(partialTicks, x, y);
        if (this.showingWidgetProgress > 0)
            this.programmerUnit.getScrollBar().setCurrentState(this.programmerUnit.getLastZoom());

        this.programmerUnit.render(x, y, this.showFlow.checked, this.showInfo.checked && this.showingWidgetProgress == 0, this.draggingWidget == null);

        int origX = x;
        int origY = y;
        x -= this.programmerUnit.getTranslatedX();
        y -= this.programmerUnit.getTranslatedY();
        float scale = this.programmerUnit.getScale();
        x = (int) (x / scale);
        y = (int) (y / scale);

        if (this.showingWidgetProgress > 0) {
            int xRight = this.getProgrammerBounds().x + this.getProgrammerBounds().width; // 299 or 649
            int yBottom = this.getProgrammerBounds().y + this.getProgrammerBounds().height; // 171 or 427

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.bindGuiTexture();
            int width = this.oldShowingWidgetProgress + (int) ((this.showingWidgetProgress - this.oldShowingWidgetProgress) * partialTicks);
            for (int i = 0; i < width; i++) {
                drawModalRectWithCustomSizedTexture(xStart + xRight + 21 - i, yStart + 36, xRight + 24, 36, 1, yBottom - 35, this.xSize, this.ySize);
            }
            drawModalRectWithCustomSizedTexture(xStart + xRight + 20 - width, yStart + 36, xRight + 20, 36, 2, yBottom - 35, this.xSize, this.ySize);

            if (this.showingAllWidgets && this.draggingWidget != null) this.toggleShowWidgets();
        }
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        for (int i = 0; i < this.visibleSpawnWidgets.size(); i++) {
            IProgWidget widget = this.visibleSpawnWidgets.get(i);
            GlStateManager.pushMatrix();
            GlStateManager.translate(widget.getX() + this.guiLeft, widget.getY() + this.guiTop, 0);
            GlStateManager.scale(0.5, 0.5, 1);
            if (this.showingAllWidgets && this.filteredSpawnWidgets != null && !this.filteredSpawnWidgets.get(i)) {
                GlStateManager.color(1, 1, 1, 0.2f);
            } else {
                GlStateManager.color(1, 1, 1, 1);
            }
            widget.render();
            GlStateManager.popMatrix();
        }
        GlStateManager.disableBlend();

        GlStateManager.pushMatrix();
        GlStateManager.translate(this.programmerUnit.getTranslatedX(), this.programmerUnit.getTranslatedY(), 0);
        GlStateManager.scale(scale, scale, 1);
        if (this.draggingWidget != null) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(this.draggingWidget.getX() + this.guiLeft, this.draggingWidget.getY() + this.guiTop, 0);
            GlStateManager.scale(0.5, 0.5, 1);
            this.draggingWidget.render();
            GlStateManager.popMatrix();
        }
        GlStateManager.popMatrix();

        boolean isLeftClicking = Mouse.isButtonDown(0);
        boolean isMiddleClicking = GameSettings.isKeyDown(Minecraft.getMinecraft().gameSettings.keyBindPickBlock);

        if (this.draggingWidget != null) {
            this.setConnectingWidgetsToXY(this.draggingWidget, x - this.dragMouseStartX + this.dragWidgetStartX - this.guiLeft, y - this.dragMouseStartY + this.dragWidgetStartY - this.guiTop);
        }

        if (isLeftClicking && !this.wasClicking) {
            for (IProgWidget widget : this.visibleSpawnWidgets) {
                if (origX >= widget.getX() + this.guiLeft && origY >= widget.getY() + this.guiTop && origX <= widget.getX() + this.guiLeft + widget.getWidth() / 2 && origY <= widget.getY() + this.guiTop + widget.getHeight() / 2) {
                    this.draggingWidget = widget.copy();
                    this.te.progWidgets.add(this.draggingWidget);
                    this.dragMouseStartX = x - (int) (this.guiLeft / scale);
                    this.dragMouseStartY = y - (int) (this.guiTop / scale);
                    this.dragWidgetStartX = (int) ((widget.getX() - this.programmerUnit.getTranslatedX()) / scale);
                    this.dragWidgetStartY = (int) ((widget.getY() - this.programmerUnit.getTranslatedY()) / scale);
                    break;
                }
            }

            // create area widgets straight from GPS Area Tools
            ItemStack heldItem = this.mc.player.inventory.getItemStack();
            ProgWidgetArea areaToolWidget = heldItem.getItem() instanceof ItemGPSAreaTool ? ItemGPSAreaTool.getArea(heldItem) : null;

            if (this.draggingWidget == null && this.showingWidgetProgress == 0) {
                IProgWidget widget = this.programmerUnit.getHoveredWidget(origX, origY);
                if (widget != null) {
                    this.draggingWidget = widget;
                    this.dragMouseStartX = x - this.guiLeft;
                    this.dragMouseStartY = y - this.guiTop;
                    this.dragWidgetStartX = widget.getX();
                    this.dragWidgetStartY = widget.getY();

                    if (areaToolWidget != null && widget instanceof ProgWidgetArea) {
                        NBTTagCompound tag = new NBTTagCompound();
                        areaToolWidget.writeToNBT(tag);
                        widget.readFromNBT(tag);
                    } else if (heldItem.getItem() == Itemss.GPS_TOOL) {
                        if (widget instanceof ProgWidgetCoordinate) {
                            ((ProgWidgetCoordinate) widget).loadFromGPSTool(heldItem);
                        } else if (widget instanceof ProgWidgetArea) {
                            BlockPos pos = ItemGPSTool.getGPSLocation(heldItem);
                            String var = ItemGPSTool.getVariable(heldItem);
                            if (pos != null) ((ProgWidgetArea) widget).setP1(pos);
                            ((ProgWidgetArea) widget).setP2(BlockPos.ORIGIN);
                            ((ProgWidgetArea) widget).setCoord1Variable(var);
                            ((ProgWidgetArea) widget).setCoord2Variable("");
                        }
                    }
                }
            }

            // Create a new widget from a GPS Area tool when nothing was selected
            if (this.draggingWidget == null) {
                if (areaToolWidget != null) {
                    this.draggingWidget = areaToolWidget;
                } else if (heldItem.getItem() == Itemss.GPS_TOOL) {
                    if (PneumaticCraftRepressurized.proxy.isSneakingInGui()) {
                        BlockPos pos = ItemGPSTool.getGPSLocation(heldItem);
                        ProgWidgetArea areaWidget = ProgWidgetArea.fromPositions(pos, BlockPos.ORIGIN);
                        String var = ItemGPSTool.getVariable(heldItem);
                        if (!var.isEmpty()) areaWidget.setCoord1Variable(var);
                        this.draggingWidget = areaWidget;
                    } else {
                        ProgWidgetCoordinate coordWidget = new ProgWidgetCoordinate();
                        this.draggingWidget = coordWidget;
                        coordWidget.loadFromGPSTool(heldItem);
                    }
                }

                if (this.draggingWidget != null) {
                    this.draggingWidget.setX(Integer.MAX_VALUE);
                    this.draggingWidget.setY(Integer.MAX_VALUE);
                    this.te.progWidgets.add(this.draggingWidget);
                    this.dragMouseStartX = this.draggingWidget.getWidth() / 3;
                    this.dragMouseStartY = this.draggingWidget.getHeight() / 4;
                    this.dragWidgetStartX = 0;
                    this.dragWidgetStartY = 0;
                }
            }
        } else if (isMiddleClicking && !this.wasClicking && this.showingWidgetProgress == 0) {
            IProgWidget widget = this.programmerUnit.getHoveredWidget(origX, origY);
            if (widget != null) {
                this.draggingWidget = widget.copy();
                this.te.progWidgets.add(this.draggingWidget);
                this.dragMouseStartX = 0;
                this.dragMouseStartY = 0;
                this.dragWidgetStartX = widget.getX() - (x - this.guiLeft);
                this.dragWidgetStartY = widget.getY() - (y - this.guiTop);
                if (PneumaticCraftRepressurized.proxy.isSneakingInGui())
                    this.copyAndConnectConnectingWidgets(widget, this.draggingWidget);
            }
        } else if (isMiddleClicking && this.showingAllWidgets) {
            this.showWidgetDocs();
        }

        if (!isLeftClicking && !isMiddleClicking && this.draggingWidget != null) {
            if (this.programmerUnit.isOutsideProgrammingArea(this.draggingWidget)) {
                this.deleteConnectingWidgets(this.draggingWidget);
            } else {
                this.handlePuzzleMargins();
                if (!this.isValidPlaced(this.draggingWidget)) {
                    this.setConnectingWidgetsToXY(this.draggingWidget, this.dragWidgetStartX, this.dragWidgetStartY);
                    if (this.programmerUnit.isOutsideProgrammingArea(this.draggingWidget))
                        this.deleteConnectingWidgets(this.draggingWidget);
                }
            }
            NetworkHandler.sendToServer(new PacketProgrammerUpdate(this.te));
            TileEntityProgrammer.updatePuzzleConnections(this.te.progWidgets);

            this.draggingWidget = null;
        }
        this.wasClicking = isLeftClicking || isMiddleClicking;
        this.lastMouseX = origX;
        this.lastMouseY = origY;
    }

    private boolean isValidPlaced(IProgWidget widget1) {
        Rectangle draggingRect = new Rectangle(widget1.getX(), widget1.getY(), widget1.getWidth() / 2, widget1.getHeight() / 2);
        for (IProgWidget widget : this.te.progWidgets) {
            if (widget != widget1) {
                if (draggingRect.intersects(widget.getX(), widget.getY(), widget.getWidth() / 2.0, widget.getHeight() / 2.0)) {
                    return false;
                }
            }
        }
        IProgWidget[] parameters = widget1.getConnectedParameters();
        if (parameters != null) {
            for (IProgWidget widget : parameters) {
                if (widget != null && !this.isValidPlaced(widget)) return false;
            }
        }
        IProgWidget outputWidget = widget1.getOutputWidget();
        return !(outputWidget != null && !this.isValidPlaced(outputWidget));
    }

    private void handlePuzzleMargins() {
        //Check for connection to the left of the dragged widget.
        Class<? extends IProgWidget> returnValue = this.draggingWidget.returnType();
        if (returnValue != null) {
            for (IProgWidget widget : this.te.progWidgets) {
                if (widget != this.draggingWidget && Math.abs(widget.getX() + widget.getWidth() / 2 - this.draggingWidget.getX()) <= FAULT_MARGIN) {
                    Class<? extends IProgWidget>[] parameters = widget.getParameters();
                    if (parameters != null) {
                        for (int i = 0; i < parameters.length; i++) {
                            if (widget.canSetParameter(i) && parameters[i] == returnValue && Math.abs(widget.getY() + i * 11 - this.draggingWidget.getY()) <= FAULT_MARGIN) {
                                this.setConnectingWidgetsToXY(this.draggingWidget, widget.getX() + widget.getWidth() / 2, widget.getY() + i * 11);
                                return;
                            }
                        }
                    }
                }
            }
        }

        //check for connection to the right of the dragged widget.
        Class<? extends IProgWidget>[] parameters = this.draggingWidget.getParameters();
        if (parameters != null) {
            for (IProgWidget widget : this.te.progWidgets) {
                IProgWidget outerPiece = this.draggingWidget;
                if (outerPiece.returnType() != null) {//When the piece is a parameter pice (area, item filter, text).
                    while (outerPiece.getConnectedParameters()[0] != null) {
                        outerPiece = outerPiece.getConnectedParameters()[0];
                    }
                }
                if (widget != this.draggingWidget && Math.abs(outerPiece.getX() + outerPiece.getWidth() / 2 - widget.getX()) <= FAULT_MARGIN) {
                    if (widget.returnType() != null) {
                        for (int i = 0; i < parameters.length; i++) {
                            if (this.draggingWidget.canSetParameter(i) && parameters[i] == widget.returnType() && Math.abs(this.draggingWidget.getY() + i * 11 - widget.getY()) <= FAULT_MARGIN) {
                                this.setConnectingWidgetsToXY(this.draggingWidget, widget.getX() - this.draggingWidget.getWidth() / 2 - (outerPiece.getX() - this.draggingWidget.getX()), widget.getY() - i * 11);
                            }
                        }
                    } else {
                        Class<? extends IProgWidget>[] checkingPieceParms = widget.getParameters();
                        if (checkingPieceParms != null) {
                            for (int i = 0; i < checkingPieceParms.length; i++) {
                                if (widget.canSetParameter(i + parameters.length) && checkingPieceParms[i] == parameters[0] && Math.abs(widget.getY() + i * 11 - this.draggingWidget.getY()) <= FAULT_MARGIN) {
                                    this.setConnectingWidgetsToXY(this.draggingWidget, widget.getX() - this.draggingWidget.getWidth() / 2 - (outerPiece.getX() - this.draggingWidget.getX()), widget.getY() + i * 11);
                                }
                            }
                        }
                    }
                }
            }
        }

        //check for connection to the top of the dragged widget.
        if (this.draggingWidget.hasStepInput()) {
            for (IProgWidget widget : this.te.progWidgets) {
                if (widget.hasStepOutput() && Math.abs(widget.getX() - this.draggingWidget.getX()) <= FAULT_MARGIN && Math.abs(widget.getY() + widget.getHeight() / 2 - this.draggingWidget.getY()) <= FAULT_MARGIN) {
                    this.setConnectingWidgetsToXY(this.draggingWidget, widget.getX(), widget.getY() + widget.getHeight() / 2);
                }
            }
        }

        //check for connection to the bottom of the dragged widget.
        if (this.draggingWidget.hasStepOutput()) {
            for (IProgWidget widget : this.te.progWidgets) {
                if (widget.hasStepInput() && Math.abs(widget.getX() - this.draggingWidget.getX()) <= FAULT_MARGIN && Math.abs(widget.getY() - this.draggingWidget.getY() - this.draggingWidget.getHeight() / 2) <= FAULT_MARGIN) {
                    this.setConnectingWidgetsToXY(this.draggingWidget, widget.getX(), widget.getY() - this.draggingWidget.getHeight() / 2);
                }
            }
        }
    }

    private void setConnectingWidgetsToXY(IProgWidget widget, int x, int y) {
        widget.setX(x);
        widget.setY(y);
        IProgWidget[] connectingWidgets = widget.getConnectedParameters();
        if (connectingWidgets != null) {
            for (int i = 0; i < connectingWidgets.length; i++) {
                if (connectingWidgets[i] != null) {
                    if (i < connectingWidgets.length / 2) {
                        this.setConnectingWidgetsToXY(connectingWidgets[i], x + widget.getWidth() / 2, y + i * 11);
                    } else {
                        int totalWidth = 0;
                        IProgWidget branch = connectingWidgets[i];
                        while (branch != null) {
                            totalWidth += branch.getWidth() / 2;
                            branch = branch.getConnectedParameters()[0];
                        }
                        this.setConnectingWidgetsToXY(connectingWidgets[i], x - totalWidth, y + (i - connectingWidgets.length / 2) * 11);
                    }
                }
            }
        }
        IProgWidget outputWidget = widget.getOutputWidget();
        if (outputWidget != null) this.setConnectingWidgetsToXY(outputWidget, x, y + widget.getHeight() / 2);
    }

    private void copyAndConnectConnectingWidgets(IProgWidget original, IProgWidget copy) {
        IProgWidget[] connectingWidgets = original.getConnectedParameters();
        if (connectingWidgets != null) {
            for (int i = 0; i < connectingWidgets.length; i++) {
                if (connectingWidgets[i] != null) {
                    IProgWidget c = connectingWidgets[i].copy();
                    this.te.progWidgets.add(c);
                    copy.setParameter(i, c);
                    this.copyAndConnectConnectingWidgets(connectingWidgets[i], c);
                }
            }
        }
        IProgWidget outputWidget = original.getOutputWidget();
        if (outputWidget != null) {
            IProgWidget c = outputWidget.copy();
            this.te.progWidgets.add(c);
            copy.setOutputWidget(c);
            this.copyAndConnectConnectingWidgets(outputWidget, c);
        }
    }

    private void deleteConnectingWidgets(IProgWidget widget) {
        this.te.progWidgets.remove(widget);
        IProgWidget[] connectingWidgets = widget.getConnectedParameters();
        if (connectingWidgets != null) {
            for (IProgWidget widg : connectingWidgets) {
                if (widg != null) this.deleteConnectingWidgets(widg);
            }
        }
        IProgWidget outputWidget = widget.getOutputWidget();
        if (outputWidget != null) this.deleteConnectingWidgets(outputWidget);
    }

    /**
     * Fired when a control is clicked. This is the equivalent of
     * ActionListener.actionPerformed(ActionEvent e).
     */
    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0:// redstone button
                //          redstoneBehaviourStat.closeWindow();
                break;
            case 3:
                if (--this.widgetPage < 0) this.widgetPage = this.maxPage - 1;
                this.updateVisibleProgWidgets();
                return;
            case 4:
                if (++this.widgetPage >= this.maxPage) this.widgetPage = 0;
                this.updateVisibleProgWidgets();
                return;
            case 5:
                this.programmerUnit.gotoPiece(findWidget(this.te.progWidgets, ProgWidgetStart.class));
                return;
            case 6:
                if (this.te.progWidgets.size() > 0) {
                    this.programmerUnit.gotoPiece(this.te.progWidgets.get(this.te.progWidgets.size() - 1));
                }
                return;
            case 7:
                NBTTagCompound mainTag = new NBTTagCompound();
                this.te.writeProgWidgetsToNBT(mainTag);
                FMLClientHandler.instance().showGuiScreen(this.pastebinGui = new GuiPastebin(this, mainTag));
                break;
            case 11:
                this.te.progWidgets.clear();
                NetworkHandler.sendToServer(new PacketProgrammerUpdate(this.te));
                break;
            case 12:
                for (IProgWidget widget : this.te.progWidgets) {
                    if (widget instanceof ProgWidgetStart) {
                        this.generateRelativeOperators((ProgWidgetCoordinateOperator) widget.getOutputWidget(), null, false);
                        break;
                    }
                }
                break;
        }

        NetworkHandler.sendToServer(new PacketGuiButton(button.id));
    }

    private void toggleShowWidgets() {
        this.showingAllWidgets = !this.showingAllWidgets;
        this.allWidgetsButton.displayString = this.showingAllWidgets ? "\u25e2" : "\u25e4";
        this.updateVisibleProgWidgets();
        this.filterField.setFocused(this.showingAllWidgets);
    }

    @Override
    public void actionPerformed(IGuiWidget button) {
        if (button == this.allWidgetsButton) {
            this.toggleShowWidgets();
        } else {
            for (int i = 0; i < this.difficultyButtons.size(); i++) {
                if (this.difficultyButtons.get(i).checked) {
                    ConfigHandler.setProgrammerDifficulty(i);
                    break;
                }
            }
            if (this.showingAllWidgets && button != this.programmerUnit.getScrollBar()) {
                // scrollbar is under the widget area when showingAllWidgets is true...
                this.toggleShowWidgets();
            }
            this.updateVisibleProgWidgets();
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        if (this.te.recentreStartPiece) {
            this.programmerUnit.gotoPiece(findWidget(this.te.progWidgets, ProgWidgetStart.class));
            this.te.recentreStartPiece = false;
        }

        this.undoButton.enabled = this.te.canUndo;
        this.redoButton.enabled = this.te.canRedo;

        this.updateConvertRelativeState();

        ItemStack programmedItem = this.te.getIteminProgrammingSlot();
        this.oldShowingWidgetProgress = this.showingWidgetProgress;
        if (this.showingAllWidgets) {
            int maxProgress = this.maxPage * WIDGET_X_SPACING;
            if (this.showingWidgetProgress < maxProgress) {
                this.showingWidgetProgress += 60;
                if (this.showingWidgetProgress >= maxProgress) {
                    this.showingWidgetProgress = maxProgress;
                    this.updateVisibleProgWidgets();
                }
            }
        } else {
            this.showingWidgetProgress -= 60;
            if (this.showingWidgetProgress < 0) this.showingWidgetProgress = 0;
        }

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        for (IProgWidget w : this.te.progWidgets) {
            w.addErrors(errors, this.te.progWidgets);
            w.addWarnings(warnings, this.te.progWidgets);
        }

        boolean isDeviceInserted = !programmedItem.isEmpty();
        this.importButton.enabled = isDeviceInserted;
        this.exportButton.enabled = isDeviceInserted && errors.size() == 0;

        List<String> exportButtonTooltip = new ArrayList<>();
        exportButtonTooltip.add("Export program");
        exportButtonTooltip.add(I18n.format("gui.programmer.button.export.programmingWhen", I18n.format("gui.programmer.button.export." + (this.te.redstoneMode == 0 ? "pressingButton" : "onItemInsert"))));
        exportButtonTooltip.add(I18n.format("gui.programmer.button.export.pressRToChange"));
        if (!programmedItem.isEmpty()) {
            List<ItemStack> requiredPieces = this.te.getRequiredPuzzleStacks();
            List<ItemStack> returnedPieces = this.te.getReturnedPuzzleStacks();
            if (!requiredPieces.isEmpty() || !returnedPieces.isEmpty()) exportButtonTooltip.add("");
            if (!requiredPieces.isEmpty()) {
                exportButtonTooltip.add(I18n.format("gui.tooltip.programmable.requiredPieces"));
                if (this.player.capabilities.isCreativeMode)
                    exportButtonTooltip.add("(Creative mode, so the following is free)");
                for (ItemStack stack : requiredPieces) {
                    String prefix;
                    if (this.te.hasEnoughPuzzleStacks(this.player, stack)) {
                        prefix = TextFormatting.GREEN.toString();
                    } else {
                        prefix = TextFormatting.RED.toString();
                        this.exportButton.enabled = this.player.capabilities.isCreativeMode && errors.size() == 0;
                    }
                    exportButtonTooltip.add(prefix + "-" + stack.getCount() + "x " + stack.getDisplayName());
                }
            }
            if (!returnedPieces.isEmpty()) {
                exportButtonTooltip.add("Returned Programming Puzzles:");
                if (this.player.capabilities.isCreativeMode)
                    exportButtonTooltip.add("(Creative mode, nothing's given)");
                for (ItemStack stack : returnedPieces) {
                    exportButtonTooltip.add("-" + stack.getCount() + "x " + stack.getDisplayName());
                }
            }
        } else {
            exportButtonTooltip.add(TextFormatting.GOLD + "No programmable item inserted.");
        }

        if (errors.size() > 0)
            exportButtonTooltip.add(TextFormatting.RED + I18n.format("gui.programmer.errorCount", errors.size()));
        if (warnings.size() > 0)
            exportButtonTooltip.add(TextFormatting.YELLOW + I18n.format("gui.programmer.warningCount", warnings.size()));

        this.exportButton.setTooltipText(exportButtonTooltip);
        if (!programmedItem.isEmpty()) {
            this.nameField.setEnabled(true);
            if (!this.nameField.isFocused()) {
                if (this.wasFocused) {
                    programmedItem.setStackDisplayName(this.nameField.getText());
                    NetworkHandler.sendToServer(new PacketUpdateTextfield(this.te, 0));
                }
                this.nameField.setText(programmedItem.getDisplayName());
                this.wasFocused = false;
            } else {
                this.wasFocused = true;
            }
        } else {
            this.nameField.setEnabled(false);
            this.nameField.setText("");
            this.wasFocused = false;
        }
    }

    private void updateConvertRelativeState() {
        this.convertToRelativeButton.enabled = false;
        List<String> tooltip = new ArrayList<>();
        tooltip.add("gui.programmer.button.convertToRelative.desc");

        boolean startFound = false;
        for (IProgWidget startWidget : this.te.progWidgets) {
            if (startWidget instanceof ProgWidgetStart) {
                startFound = true;
                IProgWidget widget = startWidget.getOutputWidget();
                if (widget instanceof ProgWidgetCoordinateOperator) {
                    ProgWidgetCoordinateOperator operatorWidget = (ProgWidgetCoordinateOperator) widget;
                    if (!operatorWidget.getVariable().equals("")) {
                        try {
                            if (this.generateRelativeOperators(operatorWidget, tooltip, true)) {
                                this.convertToRelativeButton.enabled = true;
                            } else {
                                tooltip.add("gui.programmer.button.convertToRelative.notEnoughRoom");
                            }
                        } catch (NullPointerException e) {
                            tooltip.add("gui.programmer.button.convertToRelative.cantHaveVariables");
                        }
                    } else {
                        tooltip.add("gui.programmer.button.convertToRelative.noVariableName");
                    }
                } else {
                    tooltip.add("gui.programmer.button.convertToRelative.noBaseCoordinate");
                }
            }
        }
        if (!startFound) tooltip.add("gui.programmer.button.convertToRelative.noStartPiece");

        List<String> localizedTooltip = new ArrayList<>();
        for (String s : tooltip) {
            localizedTooltip.addAll(PneumaticCraftUtils.convertStringIntoList(I18n.format(s), 40));
        }
        this.convertToRelativeButton.setTooltipText(localizedTooltip);
    }

    /**
     * @param baseWidget
     * @param simulate
     * @return true if successful
     */
    private boolean generateRelativeOperators(ProgWidgetCoordinateOperator baseWidget, List<String> tooltip, boolean simulate) {
        BlockPos baseCoord = ProgWidgetCoordinateOperator.calculateCoordinate(baseWidget, 0, baseWidget.getOperator());
        Map<BlockPos, String> offsetToVariableNames = new HashMap<>();
        for (IProgWidget widget : this.te.progWidgets) {
            if (widget instanceof ProgWidgetArea) {
                ProgWidgetArea area = (ProgWidgetArea) widget;
                if (area.getCoord1Variable().equals("") && (area.x1 != 0 || area.y1 != 0 || area.z1 != 0)) {
                    BlockPos offset = new BlockPos(area.x1 - baseCoord.getX(), area.y1 - baseCoord.getY(), area.z1 - baseCoord.getZ());
                    String var = this.getOffsetVariable(offsetToVariableNames, baseWidget.getVariable(), offset);
                    if (!simulate) area.setCoord1Variable(var);
                }
                if (area.getCoord2Variable().equals("") && (area.x2 != 0 || area.y2 != 0 || area.z2 != 0)) {
                    BlockPos offset = new BlockPos(area.x2 - baseCoord.getX(), area.y2 - baseCoord.getY(), area.z2 - baseCoord.getZ());
                    String var = this.getOffsetVariable(offsetToVariableNames, baseWidget.getVariable(), offset);
                    if (!simulate) area.setCoord2Variable(var);
                }
            } else if (widget instanceof ProgWidgetCoordinate && baseWidget.getConnectedParameters()[0] != widget) {
                ProgWidgetCoordinate coordinate = (ProgWidgetCoordinate) widget;
                if (!coordinate.isUsingVariable()) {
                    BlockPos c = coordinate.getCoordinate();
                    String chunkString = "(" + c.getX() + ", " + c.getY() + ", " + c.getZ() + ")";
                    if (PneumaticCraftUtils.distBetween(c, 0, 0, 0) < 64) {
                        // When the coordinate value is close to 0, there's a low chance it means a position, and rather an offset.
                        if (tooltip != null)
                            tooltip.add(I18n.format("gui.programmer.button.convertToRelative.coordIsNotChangedWarning", chunkString));
                    } else {
                        if (tooltip != null)
                            tooltip.add(I18n.format("gui.programmer.button.convertToRelative.coordIsChangedWarning", chunkString));
                        if (!simulate) {
                            BlockPos offset = new BlockPos(c.getX() - baseCoord.getX(), c.getY() - baseCoord.getY(), c.getZ() - baseCoord.getZ());
                            String var = this.getOffsetVariable(offsetToVariableNames, baseWidget.getVariable(), offset);
                            coordinate.setVariable(var);
                            coordinate.setUsingVariable(true);
                        }
                    }
                }
            }
        }
        if (offsetToVariableNames.size() > 0) {
            ProgWidgetCoordinateOperator firstOperator = null;
            ProgWidgetCoordinateOperator prevOperator = baseWidget;
            int x = baseWidget.getX();
            for (Map.Entry<BlockPos, String> entry : offsetToVariableNames.entrySet()) {
                ProgWidgetCoordinateOperator operator = new ProgWidgetCoordinateOperator();
                operator.setVariable(entry.getValue());

                int y = prevOperator.getY() + prevOperator.getHeight() / 2;
                operator.setX(x);
                operator.setY(y);
                if (!this.isValidPlaced(operator)) return false;

                ProgWidgetCoordinate coordinatePiece1 = new ProgWidgetCoordinate();
                coordinatePiece1.setX(x + prevOperator.getWidth() / 2);
                coordinatePiece1.setY(y);
                coordinatePiece1.setVariable(baseWidget.getVariable());
                coordinatePiece1.setUsingVariable(true);
                if (!this.isValidPlaced(coordinatePiece1)) return false;

                ProgWidgetCoordinate coordinatePiece2 = new ProgWidgetCoordinate();
                coordinatePiece2.setX(x + prevOperator.getWidth() / 2 + coordinatePiece1.getWidth() / 2);
                coordinatePiece2.setY(y);
                coordinatePiece2.setCoordinate(entry.getKey());
                if (!this.isValidPlaced(coordinatePiece2)) return false;

                if (!simulate) {
                    this.te.progWidgets.add(operator);
                    this.te.progWidgets.add(coordinatePiece1);
                    this.te.progWidgets.add(coordinatePiece2);
                }
                if (firstOperator == null) firstOperator = operator;
                prevOperator = operator;
            }
            if (!simulate) {
                NetworkHandler.sendToServer(new PacketProgrammerUpdate(this.te));
                TileEntityProgrammer.updatePuzzleConnections(this.te.progWidgets);
            }
            return true;
        } else {
            return true; //When there's nothing to place there's always room.
        }
    }

    private String getOffsetVariable(Map<BlockPos, String> offsetToVariableNames, String baseVariable, BlockPos offset) {
        if (offset.equals(BlockPos.ORIGIN))
            return baseVariable;
        return offsetToVariableNames.computeIfAbsent(offset, k -> "var" + (offsetToVariableNames.size() + 1));
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        ItemStack programmedItem = this.te.getIteminProgrammingSlot();
        if (this.nameField.isFocused() && !programmedItem.isEmpty()) {
            programmedItem.setStackDisplayName(this.nameField.getText());
            NetworkHandler.sendToServer(new PacketUpdateTextfield(this.te, 0));
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton == 1 && this.showingWidgetProgress == 0) {
            IProgWidget widget = this.programmerUnit.getHoveredWidget(mouseX, mouseY);
            if (widget != null) {
                GuiScreen screen = widget.getOptionWindow(this);
                if (screen != null) this.mc.displayGuiScreen(screen);
            }
        }
    }

    @Override
    public void onGuiClosed() {
        this.te.translatedX = this.programmerUnit.getTranslatedX();
        this.te.translatedY = this.programmerUnit.getTranslatedY();
        this.te.zoomState = this.programmerUnit.getLastZoom();
        this.te.showFlow = this.showFlow.checked;
        this.te.showInfo = this.showInfo.checked;
        super.onGuiClosed();
    }

    @Override
    public void onKeyTyped(IGuiWidget widget) {
        if (widget.getID() == this.filterField.getID()) {
            this.filterSpawnWidgets();
        }
    }

    public static IProgWidget findWidget(List<IProgWidget> widgets, Class<? extends IProgWidget> cls) {
        for (IProgWidget w : widgets) {
            if (cls.isAssignableFrom(w.getClass())) return w;
        }
        return null;
    }

    private class FilterTextField extends WidgetTextField {
        FilterTextField(FontRenderer fontRenderer, int x, int y, int width, int height) {
            super(fontRenderer, x, y, width, height);
        }

        @Override
        public void drawTextBox() {
            // this is needed to force the textfield to draw on top of any
            // widgets in the programming area
            GlStateManager.translate(0, 0, 300);
            super.drawTextBox();
            GlStateManager.translate(0, 0, -300);
        }
    }
}
