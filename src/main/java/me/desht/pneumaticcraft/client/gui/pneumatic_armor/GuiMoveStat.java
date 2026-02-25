package me.desht.pneumaticcraft.client.gui.pneumatic_armor;

import me.desht.pneumaticcraft.api.client.IGuiAnimatedStat;
import me.desht.pneumaticcraft.api.client.pneumaticHelmet.IUpgradeRenderHandler;
import me.desht.pneumaticcraft.client.gui.GuiPneumaticScreenBase;
import me.desht.pneumaticcraft.client.gui.widget.GuiAnimatedStat;
import me.desht.pneumaticcraft.client.gui.widget.GuiCheckBox;
import me.desht.pneumaticcraft.client.gui.widget.GuiKeybindCheckBox;
import me.desht.pneumaticcraft.client.render.pneumatic_armor.HUDHandler;
import me.desht.pneumaticcraft.client.render.pneumatic_armor.UpgradeRenderHandlerList;
import me.desht.pneumaticcraft.client.render.pneumatic_armor.upgrade_handler.MainHelmetHandler;
import me.desht.pneumaticcraft.client.util.GuiUtils;
import me.desht.pneumaticcraft.common.config.ArmorHUDLayout;
import me.desht.pneumaticcraft.common.pneumatic_armor.CommonArmorHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.config.GuiSlider;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiMoveStat extends GuiPneumaticScreenBase {
    private final IGuiAnimatedStat movedStat;
    private final IUpgradeRenderHandler renderHandler;
    private boolean clicked = false;
    private final List<IGuiAnimatedStat> otherStats = new ArrayList<>();
    private final List<String> helpText = new ArrayList<>();
    private final ArmorHUDLayout.LayoutTypes layoutItem;

    private GuiCheckBox snapToGrid;
    private GuiSlider gridSlider;

    private static boolean snap = false;
    private static int gridSize = 4;

    GuiMoveStat(IUpgradeRenderHandler renderHandler, ArmorHUDLayout.LayoutTypes layoutItem) {
        this(renderHandler, layoutItem, renderHandler.getAnimatedStat());
    }

    GuiMoveStat(IUpgradeRenderHandler renderHandler, ArmorHUDLayout.LayoutTypes layoutItem, @Nonnull IGuiAnimatedStat movedStat) {
        this.movedStat = movedStat;
        this.renderHandler = renderHandler;
        this.layoutItem = layoutItem;

        movedStat.openWindow();

        CommonArmorHandler hudHandler = CommonArmorHandler.getHandlerForPlayer();
        for (EntityEquipmentSlot slot : UpgradeRenderHandlerList.ARMOR_SLOTS) {
            List<IUpgradeRenderHandler> renderHandlers = UpgradeRenderHandlerList.instance().getHandlersForSlot(slot);
            for (int i = 0; i < renderHandlers.size(); i++) {
                IUpgradeRenderHandler upgradeRenderHandler = renderHandlers.get(i);
                if (hudHandler.isUpgradeRendererInserted(slot, i) && hudHandler.isUpgradeRendererEnabled(slot, i)) {
                    IGuiAnimatedStat stat = upgradeRenderHandler.getAnimatedStat();
                    if (stat != null && stat != movedStat) {
                        this.otherStats.add(stat);
                    }
                }
            }
        }

        MainHelmetHandler mainOptions = HUDHandler.instance().getSpecificRenderer(MainHelmetHandler.class);
        if (movedStat != mainOptions.testMessageStat) {
            mainOptions.testMessageStat = new GuiAnimatedStat(null, "Test Message, keep in mind messages can be long!",
                    GuiAnimatedStat.StatIcon.NONE, 0x7000AA00, null, ArmorHUDLayout.INSTANCE.messageStat);
            mainOptions.testMessageStat.openWindow();
            this.otherStats.add(mainOptions.testMessageStat);
        }
    }

    @Override
    public void initGui() {
        super.initGui();

        this.snapToGrid = new GuiCheckBox(1, 10, (this.height * 3) / 5, 0xC0C0C0, "Snap To Grid");
        this.snapToGrid.x = (this.width - this.snapToGrid.getBounds().width) / 2;
        this.snapToGrid.checked = snap;
        this.addWidget(this.snapToGrid);

        this.gridSlider = new GuiSlider(2, this.snapToGrid.x, this.snapToGrid.y + 12, this.snapToGrid.getBounds().width, 10, "", "", 1, 12, gridSize, false, true);
        this.addButton(this.gridSlider);
    }

    @Override
    protected ResourceLocation getTexture() {
        return null;
    }

    @Override
    protected void mouseClickMove(int x, int y, int lastButtonClicked, long timeSinceMouseClick) {
        if (this.clicked) {
            this.reposition(this.movedStat, x, y);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (this.movedStat.getBounds().contains(mouseX, mouseY)) {
            if (mouseButton == 2) {
                this.movedStat.setLeftSided(!this.movedStat.isLeftSided());
                this.save();
            } else if (mouseButton < 2) {
                this.clicked = true;
                this.reposition(this.movedStat, mouseX, mouseY);
            }
        } else {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        if (this.clicked) {
            if (mouseButton == 0 || mouseButton == 1) {
                this.reposition(this.movedStat, mouseX, mouseY);
            }
            this.save();
            this.clicked = false;
        }
        super.mouseReleased(mouseX, mouseY, mouseButton);
    }

    private void reposition(IGuiAnimatedStat stat, int x, int y) {
        if (snap) {
            x = x - (x % gridSize);
            y = y - (y % gridSize);
        }
        stat.setBaseX(x);
        stat.setBaseY(y);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            Minecraft.getMinecraft().displayGuiScreen(GuiHelmetMainScreen.getInstance());
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public void drawScreen(int x, int y, float partialTicks) {
        this.drawDefaultBackground();

        GuiUtils.showPopupHelpScreen(this, this.fontRenderer, this.helpText);

        super.drawScreen(x, y, partialTicks);

        this.movedStat.render(-1, -1, partialTicks);

        this.otherStats.forEach(stat -> {
            int c = stat.getBackgroundColor();
            stat.setBackGroundColor(0x30606060);
            stat.render(-1, -1, partialTicks);
            stat.setBackGroundColor(c);
        });
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        snap = this.snapToGrid.checked;
        gridSize = this.gridSlider.getValueInt();
        this.gridSlider.visible = snap;

        this.movedStat.update();
        this.otherStats.forEach(IGuiAnimatedStat::update);

        if (this.helpText.isEmpty()) {
            this.helpText.add(TextFormatting.GREEN + "" + TextFormatting.UNDERLINE + "Moving: "
                    + I18n.format(GuiKeybindCheckBox.UPGRADE_PREFIX + this.renderHandler.getUpgradeName()));
            this.helpText.add("");
            this.helpText.add("Left- or Right-Click: move the highlighted stat");
            this.helpText.add("...");
        }
        this.helpText.set(3, "Stat expands " + this.getDir(this.movedStat.isLeftSided()) + ". Middle-click: expand " + this.getDir(!this.movedStat.isLeftSided()));
    }

    private String getDir(boolean left) {
        return TextFormatting.YELLOW + (left ? "Left" : "Right") + TextFormatting.RESET;
    }

    private void save() {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        ArmorHUDLayout.INSTANCE.updateLayout(this.layoutItem,
                (float) (this.movedStat.getBaseX() / sr.getScaledWidth_double()),
                (float) (this.movedStat.getBaseY() / sr.getScaledHeight_double()),
                this.movedStat.isLeftSided());
    }
}
