package me.desht.pneumaticcraft.client.gui;

import me.desht.pneumaticcraft.api.item.IPositionProvider;
import me.desht.pneumaticcraft.api.universalSensor.ISensorSetting;
import me.desht.pneumaticcraft.client.gui.widget.GuiAnimatedStat;
import me.desht.pneumaticcraft.client.util.GuiUtils;
import me.desht.pneumaticcraft.common.block.Blockss;
import me.desht.pneumaticcraft.common.inventory.ContainerUniversalSensor;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketUpdateTextfield;
import me.desht.pneumaticcraft.common.sensor.SensorHandler;
import me.desht.pneumaticcraft.common.tileentity.TileEntityUniversalSensor;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@SideOnly(Side.CLIENT)
public class GuiUniversalSensor extends GuiPneumaticContainerBase<TileEntityUniversalSensor> {
    private GuiAnimatedStat sensorInfoStat;
    private GuiTextField nameFilterField;
    private int page;
    private int maxPage;
    private static final int MAX_SENSORS_PER_PAGE = 4;
    private int ticksExisted;

    public GuiUniversalSensor(InventoryPlayer player, TileEntityUniversalSensor te) {
        super(new ContainerUniversalSensor(player, te), te, Textures.GUI_UNIVERSAL_SENSOR);
        this.ySize = 239;
    }

    @Override
    public void initGui() {
        super.initGui();

        int xStart = (this.width - this.xSize) / 2;
        int yStart = (this.height - this.ySize) / 2;

        this.sensorInfoStat = this.addAnimatedStat("Sensor Info", new ItemStack(Blockss.UNIVERSAL_SENSOR), 0xFFFFAA00, false);
        this.addAnimatedStat("gui.tab.upgrades", Textures.GUI_UPGRADES_LOCATION, 0xFF0000FF, true).setText(this.getUpgradeText());

        this.nameFilterField = new GuiTextField(-1, this.fontRenderer, xStart + 70, yStart + 58, 100, 10);
        this.nameFilterField.setText(this.te.getText(0));

        this.updateButtons();//also adds the redstoneButton.
    }

    @Override
    protected boolean shouldAddUpgradeTab() {
        return false;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int x, int y) {
        super.drawGuiContainerForegroundLayer(x, y);

        if (this.maxPage > 1) {
            this.fontRenderer.drawString(this.page + "/" + this.maxPage, 110, 46 + 22 * MAX_SENSORS_PER_PAGE, 0x404040);
        }
        this.fontRenderer.drawString("Upgr.", 23, 98, 0x404040);

        String[] folders = this.te.getSensorSetting().split("/");
        if (folders.length == 1 && !folders[0].isEmpty()) {
            Set<Item> requiredItems = SensorHandler.getInstance().getRequiredStacksFromText(folders[0]);
            int curX = 92;
            for (Item requiredItem : requiredItems) {
                GuiUtils.drawItemStack(new ItemStack(requiredItem), curX, 20);
                curX += 18;
            }
        } else {
            int xSpace = this.xSize - 96;
            int size = this.fontRenderer.getStringWidth(folders[folders.length - 1]);
            GlStateManager.pushMatrix();
            GlStateManager.translate(92, 24, 0);
            if (size > xSpace) {
                GlStateManager.scale((float) xSpace / (float) size, 1, 1);
            }
            this.fontRenderer.drawString(folders[folders.length - 1], 0, 0, 0x4040A0);
            GlStateManager.popMatrix();
        }

        if (Keyboard.isKeyDown(Keyboard.KEY_F1)) {
            GuiUtils.showPopupHelpScreen(this, this.fontRenderer,
                    PneumaticCraftUtils.convertStringIntoList(I18n.format("gui.entityFilter.helpText"), 60));
        }
    }

    @Override
    protected Point getInvTextOffset() {
        return new Point(0, 2);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float opacity, int x, int y) {
        super.drawGuiContainerBackgroundLayer(opacity, x, y);

        if (this.nameFilterField != null) this.nameFilterField.drawTextBox();

        ISensorSetting sensor = SensorHandler.getInstance().getSensorFromPath(this.te.getSensorSetting());
        if (sensor != null) {
            GlStateManager.translate(this.guiLeft, this.guiTop, 0);
            sensor.drawAdditionalInfo(this.fontRenderer);
            GlStateManager.translate(-this.guiLeft, -this.guiTop, 0);
        }
    }

    @Override
    protected Point getGaugeLocation() {
        int xStart = (this.width - this.xSize) / 2;
        int yStart = (this.height - this.ySize) / 2;
        return new Point(xStart + 34, yStart + this.ySize / 4);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        this.nameFilterField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char par1, int par2) throws IOException {
        if (this.nameFilterField.isFocused() && par2 != 1) {
            this.nameFilterField.textboxKeyTyped(par1, par2);
            this.te.setText(0, this.nameFilterField.getText());
            NetworkHandler.sendToServer(new PacketUpdateTextfield(this.te, 0));
        } else {
            super.keyTyped(par1, par2);
        }
    }

    public void updateButtons() {
        this.buttonList.clear();
        this.buttonList.add(this.redstoneButton);
        if (!this.te.getSensorSetting().equals("")) {
            this.buttonList.add(new GuiButton(1, this.guiLeft + 70, this.guiTop + 18, 20, 20, "\u2b05"));
        } else {
            this.buttonList.add(new GuiButton(-1, this.guiLeft + 70, this.guiTop + 125, 98, 20, I18n.format("gui.universalSensor.button.showRange")));
        }
        String[] directories = SensorHandler.getInstance().getDirectoriesAtLocation(this.te.getSensorSetting());
        this.maxPage = (directories.length - 1) / MAX_SENSORS_PER_PAGE + 1;
        if (this.page > this.maxPage) this.page = this.maxPage;
        if (this.page < 1) this.page = 1;
        if (this.maxPage > 1) {
            this.buttonList.add(new GuiButton(2, this.guiLeft + 70, this.guiTop + 40 + 22 * MAX_SENSORS_PER_PAGE, 30, 20, "\u27f5"));
            this.buttonList.add(new GuiButton(3, this.guiLeft + 138, this.guiTop + 40 + 22 * MAX_SENSORS_PER_PAGE, 30, 20, "\u27f6"));
        }

        int buttonsOnPage = MAX_SENSORS_PER_PAGE;
        if (this.page == this.maxPage && (directories.length % MAX_SENSORS_PER_PAGE > 0 || directories.length == 0)) {
            buttonsOnPage = directories.length % MAX_SENSORS_PER_PAGE;
        }
        for (int i = 0; i < buttonsOnPage; i++) {
            String buttonText = directories[i + (this.page - 1) * MAX_SENSORS_PER_PAGE];
            if (SensorHandler.getInstance().getSensorFromPath(this.te.getSensorSetting() + "/" + buttonText) != null) {
                buttonText = TextFormatting.YELLOW + buttonText;
            }
            int buttonID = i * 10 + 10 + (this.page - 1) * MAX_SENSORS_PER_PAGE * 10;
            int buttonX = this.guiLeft + 70;
            int buttonY = this.guiTop + 40 + i * 22;
            int buttonWidth = 98;
            int buttonHeight = 20;
            if (this.te.getSensorSetting().equals("")) {
                Set<Item> requiredItems = SensorHandler.getInstance().getRequiredStacksFromText(buttonText);
                GuiButtonSpecial button = new GuiButtonSpecial(buttonID, buttonX, buttonY, buttonWidth, buttonHeight, "");
                ItemStack[] requiredStacks = new ItemStack[requiredItems.size()];
                Iterator<Item> iterator = requiredItems.iterator();
                for (int j = 0; j < requiredStacks.length; j++) {
                    requiredStacks[j] = new ItemStack(iterator.next());
                }
                button.setRenderStacks(requiredStacks);
                button.enabled = this.te.areGivenUpgradesInserted(requiredItems);
                this.buttonList.add(button);
            } else {
                this.buttonList.add(new GuiButton(buttonID, buttonX, buttonY, buttonWidth, buttonHeight, buttonText));
            }
        }
        this.sensorInfoStat.setText(this.getSensorInfo());
        ISensorSetting sensor = SensorHandler.getInstance().getSensorFromPath(this.te.getSensorSetting());
        boolean textboxEnabled = sensor != null && sensor.needsTextBox();
        this.nameFilterField.setVisible(textboxEnabled);
        if (!textboxEnabled) this.nameFilterField.setFocused(false);

    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (this.te.getSensorSetting().equals("") && this.ticksExisted++ > 5) {
            this.ticksExisted = 0;
            this.updateButtons();
        }
        if (!this.nameFilterField.isFocused()) {
            this.nameFilterField.setText(this.te.getText(0));
        }
    }

    private List<String> getUpgradeText() {
        List<String> upgradeInfo = new ArrayList<>();
        upgradeInfo.add("gui.tab.upgrades.volume");
        upgradeInfo.add("gui.tab.upgrades.security");
        upgradeInfo.addAll(SensorHandler.getInstance().getUpgradeInfo());
        return upgradeInfo;
    }

    private List<String> getSensorInfo() {
        List<String> text = new ArrayList<>();
        ISensorSetting sensor = SensorHandler.getInstance().getSensorFromPath(this.te.getSensorSetting());
        if (sensor != null) {
            String[] folders = this.te.getSensorSetting().split("/");
            text.add(TextFormatting.WHITE + folders[folders.length - 1]);
            text.addAll(sensor.getDescription());
        } else {
            text.add(TextFormatting.BLACK + "No sensor selected.");
        }
        return text;
    }

    @Override
    protected void addPressureStatInfo(List<String> pressureStatText) {
        super.addPressureStatInfo(pressureStatText);
        if (this.te.isSensorActive) {
            pressureStatText.add(TextFormatting.GRAY + "Usage:");
            pressureStatText.add(TextFormatting.BLACK.toString() + PneumaticValues.USAGE_UNIVERSAL_SENSOR + "mL/tick");
        }
    }

    @Override
    protected void addProblems(List<String> textList) {
        super.addProblems(textList);
        if (SensorHandler.getInstance().getSensorFromPath(this.te.getSensorSetting()) == null) {
            textList.add(TextFormatting.GRAY + "No sensor selected!");
            textList.add(TextFormatting.BLACK + "Insert upgrades and select the desired sensor.");
        }
        if (!this.te.lastSensorError.isEmpty()) {
            textList.add(TextFormatting.GRAY + "Sensor error reported!");
            textList.add(TextFormatting.BLACK + this.te.lastSensorError);
        }

        // upgrades are in slots 0..3
        // get upgrades from the container, not the tile entity (te upgrade handler isn't sync'd)
        for (int i = 0; i < this.te.getUpgradesInventory().getSlots(); i++) {
            ItemStack stack = this.inventorySlots.inventorySlots.get(i).getStack();
            if (stack.getItem() instanceof IPositionProvider) {
                BlockPos pos = ((IPositionProvider) stack.getItem()).getStoredPositions(stack).get(0);
                if (pos == null) {
                    textList.add(TextFormatting.GRAY + "The inserted GPS Tool doesn't have a coordinate selected!");
                    textList.add(TextFormatting.BLACK + "Insert a GPS Tool with a stored coordinate.");
                    break;
                }
                int sensorRange = this.te.getRange();
                if (Math.abs(pos.getX() - this.te.getPos().getX()) > sensorRange || Math.abs(pos.getY() - this.te.getPos().getY()) > sensorRange || Math.abs(pos.getZ() - this.te.getPos().getZ()) > sensorRange) {
                    textList.add(TextFormatting.GRAY + "The stored coordinate in the GPS Tool is out of the Sensor's range!");
                    textList.add(TextFormatting.BLACK + "Move the sensor closer, select a closer coordinate or insert Range Upgrades.");
                }
            }
        }
    }

    /**
     * Fired when a control is clicked. This is the equivalent of
     * ActionListener.actionPerformed(ActionEvent e).
     */
    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 2) {
            this.page--;
            if (this.page <= 0) this.page = this.maxPage;
            this.updateButtons();
        } else if (button.id == 3) {
            this.page++;
            if (this.page > this.maxPage) this.page = 1;
            this.updateButtons();
        } else if (button.id == -1) {
            this.te.showRangeLines();
        } else {
            super.actionPerformed(button);
        }
    }
}
