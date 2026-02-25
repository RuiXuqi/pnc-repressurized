package me.desht.pneumaticcraft.client.gui;

import me.desht.pneumaticcraft.client.gui.widget.IGuiWidget;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTextField;
import me.desht.pneumaticcraft.common.util.JsonToNBTConverter;
import me.desht.pneumaticcraft.common.util.NBTToJsonConverter;
import me.desht.pneumaticcraft.common.util.PastebinHandler;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class GuiPastebin extends GuiPneumaticScreenBase {

    private WidgetTextField usernameBox, passwordBox;
    private WidgetTextField pastebinBox;
    private final String pastingString;
    public NBTTagCompound outputTag;
    private final GuiScreen parentScreen;
    private String errorMessage;
    private EnumState state = EnumState.NONE;

    private enum EnumState {
        NONE, GETTING, PUTTING, LOGIN, LOGOUT
    }

    public GuiPastebin(GuiScreen parentScreen, String pastingString) {
        this.xSize = 183;
        this.ySize = 202;
        this.pastingString = pastingString;
        this.parentScreen = parentScreen;
        Keyboard.enableRepeatEvents(true);
    }

    public GuiPastebin(GuiScreen parentScreen, NBTTagCompound tag) {
        this(parentScreen, new NBTToJsonConverter(tag).convert(true));
    }

    @Override
    public void initGui() {
        super.initGui();
        if (!PastebinHandler.isLoggedIn()) {
            this.usernameBox = new WidgetTextField(this.fontRenderer, this.guiLeft + 10, this.guiTop + 30, 80, 10);
            this.addWidget(this.usernameBox);

            this.passwordBox = new WidgetTextField(this.fontRenderer, this.guiLeft + 10, this.guiTop + 56, 80, 10).setAsPasswordBox();
            this.addWidget(this.passwordBox);

            GuiButtonSpecial loginButton = new GuiButtonSpecial(0, this.guiLeft + 100, this.guiTop + 30, 60, 20, I18n.format("gui.pastebin.button.login"));
            loginButton.setTooltipText("Pastebin login is optional");
            this.addWidget(loginButton);

            this.addLabel(I18n.format("gui.pastebin.username"), this.guiLeft + 10, this.guiTop + 20);
            this.addLabel(I18n.format("gui.pastebin.password"), this.guiLeft + 10, this.guiTop + 46);

        } else {
            GuiButtonSpecial logoutButton = new GuiButtonSpecial(3, this.guiLeft + 60, this.guiTop + 30, 60, 20, I18n.format("gui.pastebin.button.logout"));
            this.addWidget(logoutButton);
        }

        this.pastebinBox = new WidgetTextField(this.fontRenderer, this.guiLeft + 10, this.guiTop + 130, 160, 10) {
            @Override
            public void onMouseClicked(int mouseX, int mouseY, int button) {
                boolean wasFocused = this.isFocused();
                super.onMouseClicked(mouseX, mouseY, button);
                if (this.isFocused()) {
                    if (!wasFocused) { //setText("");
                        this.setCursorPositionEnd();
                        this.setSelectionPos(0);
                    }
                }
            }

        };
        this.addWidget(this.pastebinBox);

        GuiButtonSpecial pasteButton = new GuiButtonSpecial(1, this.guiLeft + 31, this.guiTop + 78, 120, 20, I18n.format("gui.pastebin.button.upload"));
        this.addWidget(pasteButton);
        GuiButtonSpecial getButton = new GuiButtonSpecial(2, this.guiLeft + 31, this.guiTop + 167, 120, 20, I18n.format("gui.pastebin.button.get"));
        this.addWidget(getButton);

        GuiButtonSpecial putInClipBoard = new GuiButtonSpecial(4, this.guiLeft + 8, this.guiTop + 78, 20, 20, "");
        putInClipBoard.setRenderedIcon(Textures.GUI_COPY_ICON_LOCATION);
        putInClipBoard.setTooltipText(I18n.format("gui.pastebin.button.copyToClipboard"));
        this.addWidget(putInClipBoard);
        GuiButtonSpecial retrieveFromClipboard = new GuiButtonSpecial(5, this.guiLeft + 8, this.guiTop + 167, 20, 20, "");
        retrieveFromClipboard.setRenderedIcon(Textures.GUI_PASTE_ICON_LOCATION);
        retrieveFromClipboard.setTooltipText(I18n.format("gui.pastebin.button.loadFromClipboard"));
        this.addWidget(retrieveFromClipboard);

        this.addLabel(I18n.format("gui.pastebin.pastebinLink"), this.guiLeft + 10, this.guiTop + 120);

    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (this.state == EnumState.LOGOUT) {
            this.state = EnumState.NONE;
            this.initGui();
        }
        if (this.state != EnumState.NONE && PastebinHandler.isDone()) {
            this.errorMessage = "";
            String pastebinText;
            switch (this.state) {
                case GETTING:
                    pastebinText = PastebinHandler.getHandler().contents;
                    if (pastebinText != null) {
                        this.readFromString(pastebinText);
                    } else {
                        this.errorMessage = I18n.format("gui.pastebin.invalidPastebin");
                    }
                    break;
                case PUTTING:
                    if (PastebinHandler.getException() != null) {
                        this.errorMessage = PastebinHandler.getException().getMessage();
                    } else {
                        pastebinText = PastebinHandler.getHandler().getLink;
                        if (pastebinText == null) pastebinText = "<ERROR>";
                        if (pastebinText.contains("pastebin.com")) {
                            this.pastebinBox.setText(pastebinText);
                        } else {
                            this.errorMessage = pastebinText;
                        }
                    }
                    break;
                case LOGIN:
                    if (!PastebinHandler.isLoggedIn()) {
                        this.errorMessage = I18n.format("gui.pastebin.invalidLogin");
                    }
                    this.initGui();
            }
            this.state = EnumState.NONE;
        }
    }

    private void readFromString(String string) {
        try {
            this.outputTag = new JsonToNBTConverter(string).convert();
        } catch (Exception e) {
            e.printStackTrace();
            this.errorMessage = I18n.format("gui.pastebin.invalidFormattedPastebin");
        }
    }

    @Override
    public void drawScreen(int x, int y, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(x, y, partialTicks);
        if (this.errorMessage != null) this.fontRenderer.drawString(this.errorMessage, this.guiLeft + 5, this.guiTop + 5, 0xFFFF0000);
    }

    /**
     * Fired when a key is typed. This is the equivalent of KeyListener.keyTyped(KeyEvent e).
     */
    @Override
    protected void keyTyped(char par1, int par2) throws IOException {
        if (par2 == 1) {
            Keyboard.enableRepeatEvents(false);
            this.mc.displayGuiScreen(this.parentScreen);
            this.onGuiClosed();
        } else {
            super.keyTyped(par1, par2);
        }
    }

    @Override
    public void actionPerformed(IGuiWidget widget) {
        super.actionPerformed(widget);
        this.errorMessage = "";
        if (widget.getID() == 0) {
            PastebinHandler.login(this.usernameBox.getText(), this.passwordBox.getText());
            this.state = EnumState.LOGIN;
            this.errorMessage = I18n.format("gui.pastebin.loggingIn");
        } else if (widget.getID() == 1) {
            PastebinHandler.put(this.pastingString);
            this.state = EnumState.PUTTING;
            this.errorMessage = I18n.format("gui.pastebin.uploadingToPastebin");
        } else if (widget.getID() == 2) {
            PastebinHandler.get(this.pastebinBox.getText());
            this.state = EnumState.GETTING;
            this.errorMessage = I18n.format("gui.pastebin.retrievingFromPastebin");
        } else if (widget.getID() == 3) {
            PastebinHandler.logout();
            this.state = EnumState.LOGOUT;
        } else if (widget.getID() == 4) {
            GuiScreen.setClipboardString(this.pastingString);
            this.errorMessage = I18n.format("gui.pastebin.clipboardSetToContents");
        } else if (widget.getID() == 5) {
            this.errorMessage = I18n.format("gui.pastebin.retrievedFromClipboard");
            this.readFromString(GuiScreen.getClipboardString());
        }
    }

    @Override
    protected ResourceLocation getTexture() {
        return Textures.GUI_PASTEBIN;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
