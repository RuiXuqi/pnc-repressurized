package me.desht.pneumaticcraft.client.gui;

import me.desht.pneumaticcraft.api.tileentity.IAirHandler;
import me.desht.pneumaticcraft.common.inventory.ContainerAirCompressor;
import me.desht.pneumaticcraft.common.tileentity.TileEntityAirCompressor;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

@SideOnly(Side.CLIENT)
public class GuiAirCompressor extends GuiPneumaticContainerBase<TileEntityAirCompressor> {

    public GuiAirCompressor(Container container, TileEntityAirCompressor te, String texture) {
        super(container, te, texture);
    }

    public GuiAirCompressor(InventoryPlayer player, TileEntityAirCompressor te) {
        super(new ContainerAirCompressor(player, te), te, Textures.GUI_AIR_COMPRESSOR_LOCATION);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int x, int y) {
        super.drawGuiContainerForegroundLayer(x, y);

        this.fontRenderer.drawString("Upgr.", 28, 19, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float opacity, int x, int y) {
        super.drawGuiContainerBackgroundLayer(opacity, x, y);

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.bindGuiTexture();

        int i1 = this.te.getBurnTimeRemainingScaled(12);
        int xStart = (this.width - this.xSize) / 2;
        int yStart = (this.height - this.ySize) / 2;

        if (this.te.burnTime >= this.te.curFuelUsage)
            this.drawTexturedModalRect(xStart + this.getFuelSlotXOffset(), yStart + 38 + 12 - i1, 176, 12 - i1, 14, i1 + 2);
    }

    protected int getFuelSlotXOffset() {
        return 80;
    }

    @Override
    protected void addPressureStatInfo(List<String> pressureStatText) {
        super.addPressureStatInfo(pressureStatText);
        if (this.te.getBurnTimeRemainingScaled(12) > 0 || TileEntityFurnace.isItemFuel(this.te.getPrimaryInventory().getStackInSlot(0)) && this.te.redstoneAllows()) {
            pressureStatText.add("\u00a77Currently producing:");
            pressureStatText.add("\u00a70" + (double) Math.round(this.te.getBaseProduction() * this.te.getEfficiency() * this.te.getSpeedMultiplierFromUpgrades() / 100) + " mL/tick.");
        }
    }

    @Override
    protected void addProblems(List<String> textList) {
        super.addProblems(textList);
        if (this.te.burnTime <= 0 && !TileEntityFurnace.isItemFuel(this.te.getPrimaryInventory().getStackInSlot(0))) {
            textList.add("\u00a77No fuel!");
            textList.add("\u00a70Insert any burnable item.");
        }
        List<Pair<EnumFacing, IAirHandler>> teSurrounding = this.te.getAirHandler(null).getConnectedPneumatics();
        if (teSurrounding.isEmpty()) {
            textList.add("\u00a77Air leaking!");
            textList.add("\u00a70Add pipes / machines");
            textList.add("\u00a70to the output.");
        }
    }
}
