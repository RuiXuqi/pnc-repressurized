package me.desht.pneumaticcraft.client.gui;

import me.desht.pneumaticcraft.client.ClientTickHandler;
import me.desht.pneumaticcraft.client.gui.widget.GuiAnimatedStat;
import me.desht.pneumaticcraft.client.util.GuiUtils;
import me.desht.pneumaticcraft.common.block.Blockss;
import me.desht.pneumaticcraft.common.inventory.ContainerSecurityStationHacking;
import me.desht.pneumaticcraft.common.item.Itemss;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketSecurityStationFailedHack;
import me.desht.pneumaticcraft.common.network.PacketUseItem;
import me.desht.pneumaticcraft.common.tileentity.TileEntitySecurityStation;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class GuiSecurityStationHacking extends GuiSecurityStationBase {
    private GuiAnimatedStat statusStat;

    private NetworkConnectionBackground playerBackgroundBridges;
    private NetworkConnectionBackground aiBackgroundBridges;
    private NetworkConnectionPlayerHandler hackerBridges;
    private NetworkConnectionAIHandler aiBridges;

    private int stopWorms = 0;
    private int nukeViruses = 0;

    private final ItemStack stopWorm = new ItemStack(Itemss.STOP_WORM);
    private final ItemStack nukeVirus = new ItemStack(Itemss.NUKE_VIRUS);

    public GuiSecurityStationHacking(InventoryPlayer player, TileEntitySecurityStation te) {

        super(new ContainerSecurityStationHacking(player, te), te, Textures.GUI_HACKING);
        this.ySize = 238;
    }

    @Override
    public void initGui() {
        super.initGui();

        int xStart = (this.width - this.xSize) / 2;
        int yStart = (this.height - this.ySize) / 2;

        this.statusStat = this.addAnimatedStat("Security Status", new ItemStack(Blockss.SECURITY_STATION), 0xFFFFAA00, false);
        this.addAnimatedStat("gui.tab.info", Textures.GUI_INFO_LOCATION, 0xFF8888FF, true).setText("gui.tab.info.tile.security_station.hacking");
        this.addAnimatedStat("gui.tab.upgrades", Textures.GUI_UPGRADES_LOCATION, 0xFF0000FF, true).setText("gui.tab.upgrades.tile.security_station.hacking");
        this.addAnimatedStat(Itemss.NUKE_VIRUS.getTranslationKey() + ".name", new ItemStack(Itemss.NUKE_VIRUS), 0xFF18c9e8, false).setText("gui.tab.info.tile.security_station.nukeVirus");
        this.addAnimatedStat(Itemss.STOP_WORM.getTranslationKey() + ".name", new ItemStack(Itemss.STOP_WORM), 0xFFc13232, false).setText("gui.tab.info.tile.security_station.stopWorm");

        if (this.playerBackgroundBridges == null) {
            this.playerBackgroundBridges = new NetworkConnectionBackground(this, this.te, xStart + 21, yStart + 26, 31, 0xAA4444FF);
            this.aiBackgroundBridges = new NetworkConnectionBackground(this, this.te, xStart + 23, yStart + 27, 31, 0xAA4444FF);
            this.hackerBridges = new NetworkConnectionPlayerHandler(this, this.te, xStart + 21, yStart + 26, 31, 0xFF00FF00);
            this.aiBridges = new NetworkConnectionAIHandler(this, this.te, xStart + 23, yStart + 27, 31, 0xFFFF0000);
        } else {
            this.playerBackgroundBridges = new NetworkConnectionBackground(this.playerBackgroundBridges, xStart + 21, yStart + 26);
            this.aiBackgroundBridges = new NetworkConnectionBackground(this.aiBackgroundBridges, xStart + 23, yStart + 27);
            this.hackerBridges = new NetworkConnectionPlayerHandler(this.hackerBridges, xStart + 21, yStart + 26);
            this.aiBridges = new NetworkConnectionAIHandler(this.aiBridges, xStart + 23, yStart + 27);
        }
    }

    @Override
    protected boolean shouldAddInfoTab() {
        return false;
    }

    @Override
    protected boolean shouldAddUpgradeTab() {
        return false;
    }

    @Override
    protected boolean shouldAddRedstoneTab() {
        return false;
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
    protected void drawGuiContainerForegroundLayer(int x, int y) {
        super.drawGuiContainerForegroundLayer(x, y);
        this.fontRenderer.drawString((this.aiBridges.isTracing() ? TextFormatting.RED : TextFormatting.GREEN) + "Tracing: " + PneumaticCraftUtils.convertTicksToMinutesAndSeconds(this.aiBridges.getRemainingTraceTime(), true), 15, 7, 4210752);
        this.renderConsumables(x, y);
    }

    private void renderConsumables(int x, int y) {
        this.stopWorms = 0;
        this.nukeViruses = 0;
        EntityPlayer player = FMLClientHandler.instance().getClient().player;
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack.getItem() == Itemss.STOP_WORM) this.stopWorms += stack.getCount();
            if (stack.getItem() == Itemss.NUKE_VIRUS) this.nukeViruses += stack.getCount();
        }
        GuiUtils.drawItemStack(this.nukeVirus, 155, 30);
        GuiUtils.drawItemStack(this.stopWorm, 155, 55);
        this.fontRenderer.drawString(PneumaticCraftUtils.convertAmountToString(this.nukeViruses), 155, 45, 0xFFFFFFFF);
        this.fontRenderer.drawString(PneumaticCraftUtils.convertAmountToString(this.stopWorms), 155, 70, 0xFFFFFFFF);

    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        this.statusStat.setText(this.getStatusText());
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float opacity, int x, int y) {
        super.drawGuiContainerBackgroundLayer(opacity, x, y);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        this.playerBackgroundBridges.render();
        this.aiBackgroundBridges.render();
        this.hackerBridges.render();
        this.aiBridges.render();

        if (x >= this.guiLeft + 155 && x <= this.guiLeft + 171 && y >= this.guiTop + 30 && y <= this.guiTop + 50) {
            List<String> text = new ArrayList<>();
            text.add("Nuke Virus");
            if (this.hasNukeViruses()) {
                text.add(TextFormatting.GRAY + "Middle-click a hackable node to use.");
            } else {
                text.add(TextFormatting.RED + "You don't have any Nuke Viruses.");
            }
            this.drawHoveringString(text, x, y, this.fontRenderer);
        }
        if (x >= this.guiLeft + 155 && x <= this.guiLeft + 171 && y >= this.guiTop + 55 && y <= this.guiTop + 75) {
            List<String> text = new ArrayList<>();
            text.add("STOP! Worm");
            if (this.stopWorms > 0) {
                if (this.aiBridges.isTracing()) {
                    text.add(TextFormatting.GRAY + "Left-click to use.");
                } else {
                    text.add(TextFormatting.GRAY + "STOP! Worms can only be used when being traced.");
                }
            } else {
                text.add(TextFormatting.RED + "You don't have any STOP! Worms.");
            }
            this.drawHoveringString(text, x, y, this.fontRenderer);
        }
    }

    @Override
    protected void addProblems(List<String> text) {
        super.addProblems(text);
        if (this.aiBridges.isTracing()) {
            text.add(TextFormatting.GRAY + "Intrusion detected!");
            text.add(TextFormatting.BLACK + "Time till trace: " + PneumaticCraftUtils.convertTicksToMinutesAndSeconds(this.aiBridges.getRemainingTraceTime(), false));
        }
    }

    private List<String> getStatusText() {
        List<String> text = new ArrayList<>();
        text.add(TextFormatting.GRAY + "Security Level");
        text.add(TextFormatting.BLACK + "Level " + this.te.getSecurityLevel());
        text.add(TextFormatting.GRAY + "Security Range");
        text.add(TextFormatting.BLACK.toString() + this.te.getSecurityRange() + "m (square)");
        return text;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton != 2) super.mouseClicked(mouseX, mouseY, mouseButton);
        this.hackerBridges.mouseClicked(mouseX, mouseY, mouseButton, this.getSlotAtPosition(mouseX, mouseY));
        if (this.aiBridges.isTracing() && mouseX >= this.guiLeft + 155 && mouseX <= this.guiLeft + 171 && mouseY >= this.guiTop + 55 && mouseY <= this.guiTop + 75) {
            EntityPlayer player = FMLClientHandler.instance().getClient().player;
            NetworkHandler.sendToServer(new PacketUseItem(Itemss.STOP_WORM, 1));
            PneumaticCraftUtils.consumeInventoryItem(player.inventory, Itemss.STOP_WORM);
            this.aiBridges.applyStopWorm();
        }
    }

    public void addExtraHackInfo(List<String> currenttip) {
        int mouseX = Mouse.getX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getY() * this.height / this.mc.displayHeight - 1;
        Slot slot = this.getSlotAtPosition(mouseX, mouseY);
        if (slot != null) {
            if (this.hackerBridges.slotHacked[slot.slotNumber]) {
                if (!this.hackerBridges.slotFortified[slot.slotNumber]) {
                    currenttip.add(TextFormatting.RED + "DETECTION: " + this.te.getDetectionChance() + "%");
                    currenttip.add(TextFormatting.YELLOW + "Right-click to fortify");
                }
            } else if (this.hackerBridges.canHackSlot(slot.slotNumber)) {
                currenttip.add(TextFormatting.RED + "DETECTION: " + this.te.getDetectionChance() + "%");
                currenttip.add(TextFormatting.GREEN + "Left-click to hack");

            }
        }
    }

    public boolean hasNukeViruses() {
        return this.nukeViruses > 0;
    }

    public void onSlotHack(int slot) {
        if (Math.random() < this.te.getDetectionChance() / 100D) {
            this.aiBridges.setTracing(true);
        }
    }

    public void onSlotFortification(int slot) {
        this.aiBridges.slotFortified[slot] = true;
        if (Math.random() < this.te.getDetectionChance() / 100D) {
            this.aiBridges.setTracing(true);
        }
    }

    @Override
    public void onGuiClosed() {
        if (this.aiBridges.isTracing() && !this.hackerBridges.hackedSuccessfully)
            NetworkHandler.sendToServer(new PacketSecurityStationFailedHack(this.te.getPos()));
        this.removeUpdatesOnConnectionHandlers();
        super.onGuiClosed();
    }

    public void removeUpdatesOnConnectionHandlers() {
        ClientTickHandler.instance().removeUpdatedObject(this.hackerBridges);
        ClientTickHandler.instance().removeUpdatedObject(this.aiBridges);
    }

}
