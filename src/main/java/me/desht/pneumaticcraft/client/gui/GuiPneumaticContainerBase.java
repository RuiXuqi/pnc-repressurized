package me.desht.pneumaticcraft.client.gui;

import me.desht.pneumaticcraft.PneumaticCraftRepressurized;
import me.desht.pneumaticcraft.api.client.IGuiAnimatedStat;
import me.desht.pneumaticcraft.api.tileentity.IAirHandler;
import me.desht.pneumaticcraft.api.tileentity.IHeatExchanger;
import me.desht.pneumaticcraft.client.gui.widget.*;
import me.desht.pneumaticcraft.client.gui.widget.GuiAnimatedStat.StatIcon;
import me.desht.pneumaticcraft.client.render.pressure_gauge.PressureGaugeRenderer2D;
import me.desht.pneumaticcraft.client.util.RenderUtils;
import me.desht.pneumaticcraft.common.block.Blockss;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketGuiButton;
import me.desht.pneumaticcraft.common.remote.TextVariableParser;
import me.desht.pneumaticcraft.common.thirdparty.ThirdPartyManager;
import me.desht.pneumaticcraft.common.tileentity.*;
import me.desht.pneumaticcraft.common.tileentity.SideConfigurator.RelativeFace;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SideOnly(Side.CLIENT)
public class GuiPneumaticContainerBase<Tile extends TileEntityBase> extends GuiContainer implements IWidgetListener {

    public final Tile te;
    private final ResourceLocation guiTexture;
    /**
     * Any GuiAnimatedStat added to this list will be tracked for mouseclicks, tooltip renders, rendering,updating (resolution and expansion).
     */
    protected final List<IGuiWidget> widgets = new ArrayList<>();
    private IGuiAnimatedStat lastLeftStat, lastRightStat;

    private GuiAnimatedStat pressureStat;
    private GuiAnimatedStat redstoneTab;
    GuiAnimatedStat problemTab;
    GuiButtonSpecial redstoneButton;
    private boolean hasInit; //Fix for some weird race condition occuring in 1.8 where drawing is called before initGui().
    protected boolean firstUpdate = true;

    public GuiPneumaticContainerBase(Container par1Container, Tile te, String guiTexture) {
        super(par1Container);
        this.te = te;
        this.guiTexture = guiTexture != null ? new ResourceLocation(guiTexture) : null;
    }

    private GuiAnimatedStat addAnimatedStat(String title, StatIcon icon, int color, boolean leftSided) {
        int xStart = (this.width - this.xSize) / 2;
        int yStart = (this.height - this.ySize) / 2;

        GuiAnimatedStat stat = new GuiAnimatedStat(this, title, icon, xStart + (leftSided ? 0 : this.xSize + 1), leftSided && this.lastLeftStat != null || !leftSided && this.lastRightStat != null ? 3 : yStart + 5, color, leftSided ? this.lastLeftStat : this.lastRightStat, leftSided);
        stat.setBeveled(true);
        this.addWidget(stat);
        if (leftSided) {
            this.lastLeftStat = stat;
        } else {
            this.lastRightStat = stat;
        }
        return stat;
    }

    protected GuiAnimatedStat addAnimatedStat(String title, @Nonnull ItemStack icon, int color, boolean leftSided) {
        return this.addAnimatedStat(title, StatIcon.of(icon), color, leftSided);
    }

    protected GuiAnimatedStat addAnimatedStat(String title, ResourceLocation icon, int color, boolean leftSided) {
        return this.addAnimatedStat(title, StatIcon.of(icon), color, leftSided);
    }

    protected GuiAnimatedStat addAnimatedStat(String title, int color, boolean leftSided) {
        return this.addAnimatedStat(title, StatIcon.NONE, color, leftSided);
    }

    protected void addWidget(IGuiWidget widget) {
        this.widgets.add(widget);
        widget.setListener(this);
    }

    protected void addWidgets(Iterable<IGuiWidget> widgets) {
        for (IGuiWidget widget : widgets) {
            this.addWidget(widget);
        }
    }

    protected void addLabel(String text, int x, int y) {
        this.addWidget(new WidgetLabel(x, y, text));
    }

    protected void addLabel(String text, int x, int y, int color) {
        this.addWidget(new WidgetLabel(x, y, text, color));
    }

    protected void removeWidget(IGuiWidget widget) {
        this.widgets.remove(widget);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.lastLeftStat = this.lastRightStat = null;
        if (this.shouldAddPressureTab() && this.te instanceof TileEntityPneumaticBase) {
            this.pressureStat = this.addAnimatedStat("gui.tab.pressure", new ItemStack(Blockss.PRESSURE_TUBE), 0xFF00AA00, false);
        }
        if (this.shouldAddProblemTab()) {
            this.problemTab = this.addAnimatedStat("gui.tab.problems", 0xFFA0A0A0, false);
        }
        if (this.te != null) {
            if (this.shouldAddInfoTab()) {
                this.addInfoTab("gui.tab.info." + this.te.getName());
            }
            if (this.shouldAddRedstoneTab() && this.te instanceof IRedstoneControl) {
                this.addRedstoneTab();
            }
            if (this.te instanceof IHeatExchanger) {
                this.addAnimatedStat("gui.tab.info.heat.title", new ItemStack(Items.BLAZE_POWDER), 0xFFFF5500, false).setText("gui.tab.info.heat");
            }
            if (this.shouldAddUpgradeTab()) {
                this.addUpgradeTab();
            }
            if (this.shouldAddSideConfigTabs()) {
                this.addSideConfiguratorTabs();
            }
        }
        this.hasInit = true;
    }

    private void addRedstoneTab() {
        this.redstoneTab = this.addAnimatedStat("gui.tab.redstoneBehaviour", new ItemStack(Items.REDSTONE), 0xFFCC0000, true);
        List<String> curInfo = new ArrayList<>();
        curInfo.add(I18n.format(this.te.getRedstoneTabTitle()));
        int width = this.getWidestRedstoneLabel();
        this.redstoneTab.addPadding(curInfo, 4, width / this.fontRenderer.getStringWidth(" "));
        Rectangle buttonRect = this.redstoneTab.getButtonScaledRectangle(-width - 12, 24, width + 10, 20);
        this.redstoneButton = new GuiButtonSpecial(0, buttonRect.x, buttonRect.y, buttonRect.width, buttonRect.height, "-");
        this.redstoneTab.addWidget(this.redstoneButton);
    }

    private void addUpgradeTab() {
        String upgrades = "gui.tab.upgrades." + this.te.getName();
        String translatedUpgrades = I18n.format(upgrades);
        List<String> upgradeText = new ArrayList<>();
        if (this.te instanceof TileEntityPneumaticBase) {
            upgradeText.add("gui.tab.upgrades.volume");
            upgradeText.add("gui.tab.upgrades.security");
        }
        if (this.te instanceof IHeatExchanger) {
            upgradeText.add("gui.tab.upgrades.volumeCapacity");
        }
        if (!translatedUpgrades.equals(upgrades)) upgradeText.add(upgrades);

        this.addExtraUpgradeText(upgradeText);

        if (upgradeText.size() > 0)
            this.addAnimatedStat("gui.tab.upgrades", Textures.GUI_UPGRADES_LOCATION, 0xFF6060FF, true).setText(upgradeText);
    }

    protected void addExtraUpgradeText(List<String> upgradeText) {
    }

    private int getWidestRedstoneLabel() {
        int max = 0;
        for (int i = 0; i < this.te.getRedstoneModeCount(); i++) {
            max = Math.max(max, this.fontRenderer.getStringWidth(I18n.format(this.te.getRedstoneButtonText(i))));
        }
        return max;
    }

    private void addSideConfiguratorTabs() {
        for (SideConfigurator sc : ((ISideConfigurable) this.te).getSideConfigurators()) {
            GuiAnimatedStat stat = this.addAnimatedStat(sc.getTranslationKey(), new ItemStack(Blockss.OMNIDIRECTIONAL_HOPPER), 0xFF90C0E0, false);
            stat.addPadding(7, 16);

            int yTop = 15, xLeft = 25;
            stat.addWidget(this.makeSideConfButton(sc, RelativeFace.TOP, xLeft + 22, yTop));
            stat.addWidget(this.makeSideConfButton(sc, RelativeFace.LEFT, xLeft, yTop + 22));
            stat.addWidget(this.makeSideConfButton(sc, RelativeFace.FRONT, xLeft + 22, yTop + 22));
            stat.addWidget(this.makeSideConfButton(sc, RelativeFace.RIGHT, xLeft + 44, yTop + 22));
            stat.addWidget(this.makeSideConfButton(sc, RelativeFace.BOTTOM, xLeft + 22, yTop + 44));
            stat.addWidget(this.makeSideConfButton(sc, RelativeFace.BACK, xLeft + 44, yTop + 44));
        }
    }

    private GuiButtonSpecial makeSideConfButton(SideConfigurator sc, RelativeFace relativeFace, int x, int y) {
        GuiButtonSpecial button = new GuiButtonSpecial(sc.getButtonId(relativeFace), x, y, 20, 20, "");
        sc.setupButton(button);
        return button;
    }

    protected void addInfoTab(String info) {
        IGuiAnimatedStat stat = this.addAnimatedStat("gui.tab.info", Textures.GUI_INFO_LOCATION, 0xFF8888FF, true);
        stat.setText(info);
        if (!ThirdPartyManager.instance().docsProvider.docsProviderInstalled()) {
            stat.appendText(Arrays.asList("", "gui.tab.info.assistIGW"));
        }
    }

    protected boolean shouldAddRedstoneTab() {
        return true;
    }

    protected boolean shouldAddPressureTab() {
        return true;
    }

    protected boolean shouldAddUpgradeTab() {
        return true;
    }

    protected boolean shouldAddInfoTab() {
        return true;
    }

    protected boolean shouldAddProblemTab() {
        return true;
    }

    protected boolean shouldAddSideConfigTabs() {
        return this.te instanceof ISideConfigurable;
    }

    protected int getBackgroundTint() {
        return 0xFFFFFF;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int i, int j) {
        if (this.shouldDrawBackground()) {
            this.drawDefaultBackground();
            RenderUtils.glColorHex(0xFF000000 | this.getBackgroundTint());
            this.bindGuiTexture();
            int xStart = (this.width - this.xSize) / 2;
            int yStart = (this.height - this.ySize) / 2;
            this.drawTexturedModalRect(xStart, yStart, 0, 0, this.xSize, this.ySize);
        }

        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.disableLighting();
        this.widgets.forEach(widget -> widget.render(i, j, partialTicks));
        this.widgets.forEach(widget -> widget.postRender(i, j, partialTicks));

        if (this.pressureStat != null) {
            Point gaugeLocation = this.getGaugeLocation();
            if (gaugeLocation != null) {
                TileEntityPneumaticBase pneu = (TileEntityPneumaticBase) this.te;
                PressureGaugeRenderer2D.drawPressureGauge(this.fontRenderer, -1, pneu.criticalPressure, pneu.dangerPressure, this.te instanceof IMinWorkingPressure ? ((IMinWorkingPressure) this.te).getMinWorkingPressure() : -Float.MAX_VALUE, pneu.getPressure(), gaugeLocation.x, gaugeLocation.y, this.zLevel);
            }
        }
    }

    protected boolean shouldDrawBackground() {
        return true;
    }

    protected void bindGuiTexture() {
        if (this.guiTexture != null) {
            this.mc.getTextureManager().bindTexture(this.guiTexture);
            GlStateManager.enableTexture2D();
        }
    }

    protected Point getGaugeLocation() {
        int xStart = (this.width - this.xSize) / 2;
        int yStart = (this.height - this.ySize) / 2;
        return new Point(xStart + this.xSize * 3 / 4, yStart + this.ySize / 4 + 4);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int x, int y) {
        if (this.te != null && this.getInvNameOffset() != null) {
            String containerName = I18n.format(this.te.getName() + ".name");
            this.fontRenderer.drawString(containerName, this.xSize / 2 - this.fontRenderer.getStringWidth(containerName) / 2 + this.getInvNameOffset().x, 6 + this.getInvNameOffset().y, this.getTitleColor());
        }
        if (this.getInvTextOffset() != null)
            this.fontRenderer.drawString(I18n.format("container.inventory"), 8 + this.getInvTextOffset().x, this.ySize - 94 + this.getInvTextOffset().y, 0x404040);
    }

    protected int getTitleColor() {
        return 0x404040;
    }

    protected Point getInvNameOffset() {
        return new Point(0, 0);
    }

    protected Point getInvTextOffset() {
        return new Point(0, 0);
    }

    @Override
    public void drawScreen(int x, int y, float partialTick) {
        if (!this.hasInit) return;
        super.drawScreen(x, y, partialTick);

        List<String> tooltip = new ArrayList<>();
        for (Object obj : this.buttonList) {
            if (obj instanceof GuiButtonSpecial) {
                GuiButtonSpecial button = (GuiButtonSpecial) obj;
                if (button.visible && button.x < x && button.x + button.getWidth() > x && button.y < y && button.y + button.getHeight() > y) {
                    button.getTooltip(tooltip);
                }
            }
        }

        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.disableLighting();
        for (IGuiWidget widget : this.widgets) {
            if (widget.getBounds().contains(x, y))
                widget.addTooltip(x, y, tooltip, PneumaticCraftRepressurized.proxy.isSneakingInGui());
        }
        if (this.shouldParseVariablesInTooltips()) {
            for (int i = 0; i < tooltip.size(); i++) {
                tooltip.set(i, new TextVariableParser(tooltip.get(i)).parse());
            }
        }

        if (tooltip.size() > 0) {
            this.drawHoveringString(tooltip, x, y, this.fontRenderer);
            tooltip.clear();
        }

        this.renderHoveredToolTip(x, y);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        for (IGuiWidget widget : this.widgets)
            widget.update();

        if (this.pressureStat != null) {
            List<String> pressureText = new ArrayList<>();
            this.addPressureStatInfo(pressureText);
            this.pressureStat.setText(pressureText);
        }
        if (this.problemTab != null && ((Minecraft.getMinecraft().world.getTotalWorldTime() & 0x7) == 0 || this.firstUpdate)) {
            this.handleProblemsTab();
        }
        if (this.redstoneTab != null) {
            this.redstoneButton.displayString = I18n.format(this.te.getRedstoneButtonText(((IRedstoneControl) this.te).getRedstoneMode()));
        }
        this.firstUpdate = false;
    }

    private void handleProblemsTab() {
        List<String> problemText = new ArrayList<>();
        this.addProblems(problemText);
        int nProbs = problemText.size();
        this.addWarnings(problemText);
        int nWarnings = problemText.size() - nProbs;
        this.addInformation(problemText);
        int nInfo = problemText.size() - nWarnings;

        if (nProbs > 0) {
            this.problemTab.setTexture(Textures.GUI_PROBLEMS_TEXTURE);
            this.problemTab.setTitle("gui.tab.problems");
            this.problemTab.setBackGroundColor(0xFFFF0000);
        } else if (nWarnings > 0) {
            this.problemTab.setTexture(Textures.GUI_WARNING_TEXTURE);
            this.problemTab.setTitle("gui.tab.problems.warning");
            this.problemTab.setBackGroundColor(0xFFC0C000);
        } else {
            this.problemTab.setTexture(Textures.GUI_NO_PROBLEMS_TEXTURE);
            this.problemTab.setTitle("gui.tab.problems.noProblems");
            this.problemTab.setBackGroundColor(0xFFA0FFA0);
        }
        if (problemText.isEmpty()) problemText.add("");
        this.problemTab.setText(problemText);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        this.sendPacketToServer(button.id);
    }

    protected void addPressureStatInfo(List<String> pressureStatText) {
        TileEntityPneumaticBase pneumaticTile = (TileEntityPneumaticBase) this.te;
        IAirHandler airHandler = pneumaticTile.getAirHandler(null);
        pressureStatText.add("\u00a77Current Pressure:");
        pressureStatText.add("\u00a70" + PneumaticCraftUtils.roundNumberTo(pneumaticTile.getPressure(), 1) + " bar.");
        pressureStatText.add("\u00a77Current Air:");
        pressureStatText.add("\u00a70" + (airHandler.getAir() + airHandler.getVolume()) + " mL.");
        pressureStatText.add("\u00a77Volume:");
        pressureStatText.add("\u00a70" + pneumaticTile.getDefaultVolume() + " mL.");
        int volumeLeft = airHandler.getVolume() - pneumaticTile.getDefaultVolume();
        if (volumeLeft > 0) {
            pressureStatText.add("\u00a70" + volumeLeft + " mL. (Volume Upgrades)");
            pressureStatText.add("\u00a70--------+");
            pressureStatText.add("\u00a70" + airHandler.getVolume() + " mL.");
        }
    }

    /**
     * Use this to add problem information; situations that prevent the machine from operating.
     *
     * @param curInfo string list to append to
     */
    protected void addProblems(List<String> curInfo) {
        if (this.te instanceof IMinWorkingPressure) {
            IMinWorkingPressure minWork = (IMinWorkingPressure) this.te;
            if (((TileEntityPneumaticBase) this.te).getPressure() < minWork.getMinWorkingPressure()) {
                curInfo.add("gui.tab.problems.notEnoughPressure");
                curInfo.add(I18n.format("gui.tab.problems.applyPressure", minWork.getMinWorkingPressure()));
            }
        }
    }

    /**
     * Use this to add informational messages to the problems tab, which don't actually count as problems.
     *
     * @param curInfo string list to append to, which may already contain some problem text
     */
    protected void addInformation(List<String> curInfo) {
    }

    /**
     * Use this to add warning messages; the machine will run but with potential problems.
     *
     * @param curInfo string list to append to, which may already contain some problem text
     */
    protected void addWarnings(List<String> curInfo) {
        if (this.te instanceof IRedstoneControlled && !this.te.redstoneAllows()) {
            IRedstoneControlled redstoneControlled = (IRedstoneControlled) this.te;
            curInfo.add("gui.tab.problems.redstoneDisallows");
            if (redstoneControlled.getRedstoneMode() == 1) {
                curInfo.add("gui.tab.problems.provideRedstone");
            } else {
                curInfo.add("gui.tab.problems.removeRedstone");
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        for (IGuiWidget widget : this.widgets) {
            if (widget.getBounds().contains(mouseX, mouseY)) widget.onMouseClicked(mouseX, mouseY, mouseButton);
            else widget.onMouseClickedOutsideBounds(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void actionPerformed(IGuiWidget widget) {
        if (widget instanceof IGuiAnimatedStat) {
            boolean leftSided = ((IGuiAnimatedStat) widget).isLeftSided();
            this.widgets.stream()
                    .filter(w -> w instanceof IGuiAnimatedStat)
                    .map(w -> (IGuiAnimatedStat) w)
                    .filter(stat -> widget != stat && stat.isLeftSided() == leftSided) // when the stat is on the same side, close it.
                    .forEach(IGuiAnimatedStat::closeWindow);
        } else if (this.te instanceof ISideConfigurable && widget instanceof GuiButtonSpecial) {
            ((ISideConfigurable) this.te).getSideConfigurators().stream()
                    .filter(sc -> sc.handleButtonPress(widget.getID()))
                    .findFirst()
                    .ifPresent(sc -> sc.setupButton((GuiButtonSpecial) widget));
        }
        this.sendPacketToServer(widget.getID());
    }

    protected void sendPacketToServer(int id) {
        NetworkHandler.sendToServer(new PacketGuiButton(id));
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        this.widgets.forEach(IGuiWidget::handleMouseInput);
    }

    @Override
    protected void keyTyped(char key, int keyCode) throws IOException {
        for (IGuiWidget widget : this.widgets) {
            if (widget.onKey(key, keyCode)) return;
        }
        super.keyTyped(key, keyCode);
    }

    @Override
    public void setWorldAndResolution(Minecraft par1Minecraft, int par2, int par3) {
        this.widgets.clear();
        super.setWorldAndResolution(par1Minecraft, par2, par3);
    }

    public void drawHoveringString(List<String> text, int x, int y, FontRenderer fontRenderer) {
        this.drawHoveringText(text, x, y, fontRenderer);
    }

    public static void drawTexture(ResourceLocation texture, int x, int y) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        BufferBuilder wr = Tessellator.getInstance().getBuffer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        wr.pos(x, y + 16, 0).tex(0.0, 1.0).endVertex();
        wr.pos(x + 16, y + 16, 0).tex(1.0, 1.0).endVertex();
        wr.pos(x + 16, y, 0).tex(1.0, 0.0).endVertex();
        wr.pos(x, y, 0).tex(0.0, 0.0).endVertex();
        Tessellator.getInstance().draw();
        // this.drawTexturedModalRect(x, y, 0, 0, 16, 16);
    }

    public GuiButtonSpecial getButtonFromRectangle(int buttonID, Rectangle buttonSize, String buttonText) {
        return new GuiButtonSpecial(buttonID, buttonSize.x, buttonSize.y, buttonSize.width, buttonSize.height, buttonText);
    }

    public GuiButtonSpecial getInvisibleButtonFromRectangle(int buttonID, Rectangle buttonSize) {
        return new GuiButtonSpecial(buttonID, buttonSize.x, buttonSize.y, buttonSize.width, buttonSize.height, "");
    }

    public WidgetTextField getTextFieldFromRectangle(Rectangle textFieldSize) {
        return new WidgetTextField(this.fontRenderer, textFieldSize.x, textFieldSize.y, textFieldSize.width, textFieldSize.height);
    }

    @Override
    public int getGuiLeft() {
        return this.guiLeft;
    }

    @Override
    public int getGuiTop() {
        return this.guiTop;
    }

    public List<Rectangle> getTabRectangles() {
        return this.widgets.stream()
                .filter(w -> w instanceof IGuiAnimatedStat)
                .map(IGuiWidget::getBounds)
                .collect(Collectors.toList());
    }

    @Override
    public void onKeyTyped(IGuiWidget widget) {
    }

    protected void refreshScreen() {
        ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
        int i = scaledresolution.getScaledWidth();
        int j = scaledresolution.getScaledHeight();
        this.setWorldAndResolution(Minecraft.getMinecraft(), i, j);
        this.widgets.stream().filter(widget -> widget instanceof GuiAnimatedStat).forEach(IGuiWidget::update);
    }

    protected boolean shouldParseVariablesInTooltips() {
        return false;
    }
}
