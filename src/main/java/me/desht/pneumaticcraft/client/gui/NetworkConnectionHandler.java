package me.desht.pneumaticcraft.client.gui;

import me.desht.pneumaticcraft.client.ClientTickHandler;
import me.desht.pneumaticcraft.client.render.RenderProgressingLine;
import me.desht.pneumaticcraft.common.tileentity.TileEntitySecurityStation;
import me.desht.pneumaticcraft.lib.TileEntityConstants;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class NetworkConnectionHandler implements INeedTickUpdate {
    protected final GuiSecurityStationBase gui;
    protected final TileEntitySecurityStation station;
    protected int baseX;
    protected int baseY;
    protected final int nodeSpacing;
    protected final int color;
    protected final List<RenderProgressingLine> lineList = new ArrayList<>();
    protected final boolean[] slotHacked = new boolean[35];
    public final boolean[] slotFortified = new boolean[35];
    private final float baseBridgeSpeed;

    /**
     * Normal constructor
     *
     * @param gui
     * @param station
     * @param baseX
     * @param baseY
     * @param nodeSpacing
     * @param color
     */
    public NetworkConnectionHandler(GuiSecurityStationBase gui, TileEntitySecurityStation station, int baseX,
                                    int baseY, int nodeSpacing, int color, float baseBridgeSpeed) {
        this.gui = gui;
        this.station = station;
        this.baseX = baseX;
        this.baseY = baseY;
        this.nodeSpacing = nodeSpacing;
        this.color = color;
        this.baseBridgeSpeed = baseBridgeSpeed;
        ClientTickHandler.instance().registerUpdatedObject(this);
    }

    /**
     * Copy-constructor
     *
     * @param copy
     */
    public NetworkConnectionHandler(NetworkConnectionHandler copy) {
        this(copy.gui, copy.station, copy.baseX, copy.baseY, copy.nodeSpacing, copy.color, copy.baseBridgeSpeed);
        for (int i = 0; i < this.slotHacked.length; i++) {
            this.slotHacked[i] = copy.slotHacked[i];
            this.slotFortified[i] = copy.slotFortified[i];
        }
        for (RenderProgressingLine line : copy.lineList) {
            this.lineList.add(new RenderProgressingLine(line));
        }
    }

    /**
     * Constructor used when resolution gets updated.
     *
     * @param copy
     * @param baseX
     * @param baseY
     */
    public NetworkConnectionHandler(NetworkConnectionHandler copy, int baseX, int baseY) {
        this(copy);
        this.baseX = baseX;
        this.baseY = baseY;
        for (RenderProgressingLine line : this.lineList) { //adjust the copied lines for the new baseX and baseY
            line.startX = line.startX - copy.baseX + baseX;
            line.startY = line.startY - copy.baseY + baseY;
            line.endX = line.endX - copy.baseX + baseX;
            line.endY = line.endY - copy.baseY + baseY;
        }
    }

    public void render() {
        float f = (this.color >> 24 & 255) / 255.0F;
        float f1 = (this.color >> 16 & 255) / 255.0F;
        float f2 = (this.color >> 8 & 255) / 255.0F;
        float f3 = (this.color & 255) / 255.0F;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(f1, f2, f3, f);
        for (RenderProgressingLine line : this.lineList) {
            line.render();
        }
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    @Override
    public void update() {
        for (RenderProgressingLine line : this.lineList) {
            int slot = line.getPointedSlotNumber(this.gui);
            ItemStack stack = this.station.getPrimaryInventory().getStackInSlot(slot);
            boolean done = line.incProgress(this.baseBridgeSpeed * (1 / (TileEntityConstants.NETWORK_NOTE_RATING_MULTIPLIER * (stack.isEmpty() ? 1 : stack.getCount() + (this.slotFortified[slot] ? 1 : 0)))));
            if (done) {
                if (slot < this.slotHacked.length) {
                    if (!this.slotHacked[slot]) this.onSlotHack(slot, false);
                    this.slotHacked[slot] = true;
                }
            }
        }
    }

    protected void onSlotHack(int slot, boolean nuked) {
    }

    protected void addConnection(int firstSlot, int secondSlot) {
        double startX = this.baseX + firstSlot % 5 * this.nodeSpacing;
        double startY = this.baseY + firstSlot / 5 * this.nodeSpacing;
        double endX = this.baseX + secondSlot % 5 * this.nodeSpacing;
        double endY = this.baseY + secondSlot / 5 * this.nodeSpacing;
        for (RenderProgressingLine line : this.lineList) {
            if (line.hasLineSameProperties(startX, startY, 0, endX, endY, 0)) return;
        }
        this.lineList.add(new RenderProgressingLine(startX, startY, endX, endY));
    }

    protected void removeConnection(int firstSlot, int secondSlot) {
        double startX = this.baseX + firstSlot % 5 * this.nodeSpacing;
        double startY = this.baseY + firstSlot / 5 * this.nodeSpacing;
        double endX = this.baseX + secondSlot % 5 * this.nodeSpacing;
        double endY = this.baseY + secondSlot / 5 * this.nodeSpacing;
        for (RenderProgressingLine line : this.lineList) {
            if (line.hasLineSameProperties(startX, startY, 0, endX, endY, 0)) {
                this.lineList.remove(line);
                return;
            }
        }
    }

    protected boolean tryToHackSlot(int slotNumber) {
        boolean successfullyHacked = false;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (this.station.connects(slotNumber, slotNumber + i + j * 5) && this.slotHacked[slotNumber + i + j * 5]) {
                    this.addConnection(slotNumber + i + j * 5, slotNumber);
                    successfullyHacked = true;
                }
            }
        }
        return successfullyHacked;
    }

    public boolean canHackSlot(int slotNumber) {
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (this.station.connects(slotNumber, slotNumber + i + j * 5) && this.slotHacked[slotNumber + i + j * 5]) {
                    return true;
                }
            }
        }
        return false;
    }
}
