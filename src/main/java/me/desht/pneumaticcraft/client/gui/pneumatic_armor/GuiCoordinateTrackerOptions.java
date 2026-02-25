package me.desht.pneumaticcraft.client.gui.pneumatic_armor;

import me.desht.pneumaticcraft.api.client.pneumaticHelmet.IGuiScreen;
import me.desht.pneumaticcraft.api.client.pneumaticHelmet.IOptionPage;
import me.desht.pneumaticcraft.client.render.pneumatic_armor.ArmorMessage;
import me.desht.pneumaticcraft.client.render.pneumatic_armor.HUDHandler;
import me.desht.pneumaticcraft.client.render.pneumatic_armor.upgrade_handler.CoordTrackUpgradeHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Collections;

@SideOnly(Side.CLIENT)
public class GuiCoordinateTrackerOptions implements IOptionPage {
    private GuiButton wirePath;
    private GuiButton pathEnabled;
    private GuiButton xRayEnabled;
    private GuiButton pathUpdateRate;

    @Override
    public String getPageName() {
        return "Coordinate Tracker";
    }

    @Override
    public void initGui(IGuiScreen gui) {
        gui.getButtonList().add(new GuiButton(10, 30, 40, 150, 20, "Select Target..."));
        gui.getButtonList().add(new GuiButton(11, 30, 62, 150, 20, "Navigate to Surface..."));
        this.pathEnabled = new GuiButton(12, 30, 128, 150, 20, "");
        this.wirePath = new GuiButton(13, 30, 150, 150, 20, "");
        this.xRayEnabled = new GuiButton(14, 30, 172, 150, 20, "");
        this.pathUpdateRate = new GuiButton(15, 30, 194, 150, 20, "");
        gui.getButtonList().add(this.pathEnabled);
        gui.getButtonList().add(this.wirePath);
        gui.getButtonList().add(this.xRayEnabled);
        gui.getButtonList().add(this.pathUpdateRate);
        this.updateButtonTexts();
    }

    @Override
    public void actionPerformed(GuiButton button) {
        Minecraft mc = FMLClientHandler.instance().getClient();
        CoordTrackUpgradeHandler coordHandler = HUDHandler.instance().getSpecificRenderer(CoordTrackUpgradeHandler.class);
        switch (button.id) {
            case 10:
                mc.displayGuiScreen(null);
                mc.setIngameFocus();
                coordHandler.isListeningToCoordTrackerSetting = true;
                HUDHandler.instance().addMessage(new ArmorMessage("Changing Coordinate Tracker coordinate...", Collections.singletonList("Right-click the desired coordinate"), 90, 0x7000AA00));
                break;
            case 11:
                mc.displayGuiScreen(null);
                mc.setIngameFocus();
                switch (coordHandler.navigateToSurface(mc.player)) {
                    case EASY_PATH:
                        HUDHandler.instance().addMessage(new ArmorMessage(I18n.format("pneumaticHelmet.message.coordinateTracker.routeFound"), new ArrayList<>(), 90, 0x7000AA00));
                        break;
                    case DRONE_PATH:
                        HUDHandler.instance().addMessage(new ArmorMessage(I18n.format("pneumaticHelmet.message.coordinateTracker.harderRouteFound"), new ArrayList<>(), 90, 0x7044AA00));
                        break;
                    case NO_PATH:
                        HUDHandler.instance().addMessage(new ArmorMessage(I18n.format("pneumaticHelmet.message.coordinateTracker.noRouteFound"), new ArrayList<>(), 90, 0x70FF0000));
                        break;
                }

                break;
            case 12:
                coordHandler.pathEnabled = !coordHandler.pathEnabled;
                break;
            case 13:
                coordHandler.wirePath = !coordHandler.wirePath;
                break;
            case 14:
                coordHandler.xRayEnabled = !coordHandler.xRayEnabled;
                break;
            case 15:
                coordHandler.pathUpdateSetting++;
                if (coordHandler.pathUpdateSetting > 2) {
                    coordHandler.pathUpdateSetting = 0;
                }
                break;
        }
        this.updateButtonTexts();
        coordHandler.saveToConfig();
    }

    @Override
    public void drawPreButtons(int x, int y, float partialTicks) {
    }

    @Override
    public void drawScreen(int x, int y, float partialTicks) {
    }

    private void updateButtonTexts() {
        CoordTrackUpgradeHandler coordHandler = HUDHandler.instance().getSpecificRenderer(CoordTrackUpgradeHandler.class);
        this.pathEnabled.displayString = coordHandler.pathEnabled ? "Navigation Enabled" : "Navigation Disabled";
        this.wirePath.displayString = coordHandler.wirePath ? "Wire Navigation" : "Tile Navigation";
        this.xRayEnabled.displayString = coordHandler.xRayEnabled ? "X-Ray Enabled" : "X-Ray Disabled";
        switch (coordHandler.pathUpdateSetting) {
            case 0:
                this.pathUpdateRate.displayString = "Path update rate: Low";
                break;
            case 1:
                this.pathUpdateRate.displayString = "Path update rate: Normal";
                break;
            case 2:
                this.pathUpdateRate.displayString = "Path update rate: Fast";
                break;
        }
        this.wirePath.enabled = coordHandler.pathEnabled;
        this.xRayEnabled.enabled = coordHandler.pathEnabled;
        this.pathUpdateRate.enabled = coordHandler.pathEnabled;
    }

    @Override
    public void keyTyped(char ch, int key) {
    }

    @Override
    public void mouseClicked(int x, int y, int button) {
    }

    @Override
    public void handleMouseInput() {
    }

    @Override
    public boolean canBeTurnedOff() {
        return true;
    }

    @Override
    public boolean displaySettingsText() {
        return true;
    }
}
