package me.desht.pneumaticcraft.client.gui.pneumatic_armor;

import me.desht.pneumaticcraft.api.client.pneumaticHelmet.IGuiScreen;
import me.desht.pneumaticcraft.api.client.pneumaticHelmet.IOptionPage;
import me.desht.pneumaticcraft.api.client.pneumaticHelmet.IUpgradeRenderHandler;
import me.desht.pneumaticcraft.client.gui.GuiButtonSpecial;
import me.desht.pneumaticcraft.client.gui.GuiPneumaticScreenBase;
import me.desht.pneumaticcraft.client.gui.widget.GuiKeybindCheckBox;
import me.desht.pneumaticcraft.client.render.pneumatic_armor.UpgradeRenderHandlerList;
import me.desht.pneumaticcraft.client.render.pneumatic_armor.upgrade_handler.MainHelmetHandler;
import me.desht.pneumaticcraft.common.item.ItemPneumaticArmor;
import me.desht.pneumaticcraft.common.item.Itemss;
import me.desht.pneumaticcraft.common.pneumatic_armor.CommonArmorHandler;
import me.desht.pneumaticcraft.lib.Log;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.FMLClientHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GuiHelmetMainScreen extends GuiPneumaticScreenBase implements IGuiScreen {
    private static final String TITLE_PREFIX = TextFormatting.AQUA + "" + TextFormatting.UNDERLINE;

    public static final ItemStack[] ARMOR_STACKS = new ItemStack[]{
            new ItemStack(Itemss.PNEUMATIC_BOOTS),
            new ItemStack(Itemss.PNEUMATIC_LEGGINGS),
            new ItemStack(Itemss.PNEUMATIC_CHESTPLATE),
            new ItemStack(Itemss.PNEUMATIC_HELMET)
    };
    private final List<UpgradeOption> upgradeOptions = new ArrayList<>();
    private static int pageNumber;
    private boolean inInitPhase = true;

    private static GuiHelmetMainScreen instance;//Creating a static instance, as we can use it to handle keybinds when the GUI is closed.

    public static GuiHelmetMainScreen getInstance() {
        return instance;
    }

    public static void init() {
        if (instance == null) {
            instance = new GuiHelmetMainScreen();
            Minecraft minecraft = FMLClientHandler.instance().getClient();
            ScaledResolution scaledresolution = new ScaledResolution(minecraft);
            int width = scaledresolution.getScaledWidth();
            int height = scaledresolution.getScaledHeight();
            instance.setWorldAndResolution(minecraft, width, height);  // causes initGui() to be called
            if (instance.upgradeOptions.isEmpty()) {
                Log.warning("Empty armor upgradeOptions list - did some other mod cancel GuiScreenEvent.InitGuiEvent.Pre? Forcing manual init.");
                instance.initGui();
            }

            for (int i = 1; i < instance.upgradeOptions.size(); i++) {
                pageNumber = i;
                instance.initGui();
            }
            pageNumber = 0;
            instance.inInitPhase = false;
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.clear();
        this.upgradeOptions.clear();
        this.addPages();
        for (int i = 0; i < this.upgradeOptions.size(); i++) {
            GuiButtonSpecial button = new GuiButtonSpecial(100 + i, 210, 20 + i * 22, 120, 20, this.upgradeOptions.get(i).page.getPageName());
            button.setRenderStacks(this.upgradeOptions.get(i).icons);
            button.setIconPosition(GuiButtonSpecial.IconPosition.RIGHT);
            if (pageNumber == i) button.enabled = false;
            this.buttonList.add(button);
        }
        if (pageNumber > this.upgradeOptions.size() - 1) {
            pageNumber = this.upgradeOptions.size() - 1;
        }
        GuiKeybindCheckBox checkBox = new GuiKeybindCheckBox(100, 40, 25, 0xFFFFFFFF,
                I18n.format("gui.enableModule", I18n.format(GuiKeybindCheckBox.UPGRADE_PREFIX + this.upgradeOptions.get(pageNumber).text)),
                GuiKeybindCheckBox.UPGRADE_PREFIX + this.upgradeOptions.get(pageNumber).text);
        if (this.upgradeOptions.get(pageNumber).page.canBeTurnedOff()) {
            this.addWidget(checkBox);
        }
        this.upgradeOptions.get(pageNumber).page.initGui(this);
    }

    @Override
    protected ResourceLocation getTexture() {
        return null;
    }

    private void addPages() {
        for (EntityEquipmentSlot slot : UpgradeRenderHandlerList.ARMOR_SLOTS) {
            List<IUpgradeRenderHandler> renderHandlers = UpgradeRenderHandlerList.instance().getHandlersForSlot(slot);
            for (int i = 0; i < renderHandlers.size(); i++) {
                if (this.inInitPhase || CommonArmorHandler.getHandlerForPlayer().isUpgradeRendererInserted(slot, i)) {
                    IUpgradeRenderHandler upgradeRenderHandler = renderHandlers.get(i);
                    if (this.inInitPhase
                            || ItemPneumaticArmor.isPneumaticArmorPiece(Minecraft.getMinecraft().player, slot)
                            || upgradeRenderHandler instanceof MainHelmetHandler) {
                        IOptionPage optionPage = upgradeRenderHandler.getGuiOptionsPage();
                        if (optionPage != null) {
                            List<ItemStack> stacks = new ArrayList<>();
                            stacks.add(ARMOR_STACKS[upgradeRenderHandler.getEquipmentSlot().getIndex()]);
                            Arrays.stream(upgradeRenderHandler.getRequiredUpgrades()).map(ItemStack::new).forEach(stacks::add);
                            this.upgradeOptions.add(new UpgradeOption(optionPage, upgradeRenderHandler.getUpgradeName(), stacks.toArray(new ItemStack[0])));
                        }
                    }
                }
            }
        }
    }

    @Override
    public void drawScreen(int x, int y, float partialTicks) {
        this.drawDefaultBackground();
        IOptionPage optionPage = this.upgradeOptions.get(pageNumber).page;
        optionPage.drawPreButtons(x, y, partialTicks);
        this.drawCenteredString(this.fontRenderer, TITLE_PREFIX + this.upgradeOptions.get(pageNumber).page.getPageName(), 100, 12, 0xFFFFFFFF);
        if (optionPage.displaySettingsText())
            this.drawCenteredString(this.fontRenderer, "Settings", 100, optionPage.settingsYposition(), 0xFFFFFFFF);
        super.drawScreen(x, y, partialTicks);
        optionPage.drawScreen(x, y, partialTicks);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        IOptionPage optionPage = this.upgradeOptions.get(pageNumber).page;
        optionPage.updateScreen();
    }

    @Override
    public void keyTyped(char par1, int par2) throws IOException {
        super.keyTyped(par1, par2);
        this.upgradeOptions.get(pageNumber).page.keyTyped(par1, par2);
    }

    @Override
    public void actionPerformed(GuiButton button) {
        if (button.id >= 100 && button.id < 100 + this.upgradeOptions.size()) {
            pageNumber = button.id - 100;
            this.initGui();
        } else {
            this.upgradeOptions.get(pageNumber).page.actionPerformed(button);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        this.upgradeOptions.get(pageNumber).page.handleMouseInput();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int par3) throws IOException {
        super.mouseClicked(mouseX, mouseY, par3);
        this.upgradeOptions.get(pageNumber).page.mouseClicked(mouseX, mouseY, par3);
    }

    @Override
    public List getButtonList() {
        return this.buttonList;
    }

    @Override
    public FontRenderer getFontRenderer() {
        return this.fontRenderer;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private static class UpgradeOption {
        private final IOptionPage page;
        private final String text;
        private final ItemStack[] icons;

        UpgradeOption(IOptionPage page, String text, ItemStack... icons) {
            this.page = page;
            this.text = text;
            this.icons = icons;
        }
    }
}
