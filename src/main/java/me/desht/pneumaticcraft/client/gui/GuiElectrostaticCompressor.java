package me.desht.pneumaticcraft.client.gui;

import me.desht.pneumaticcraft.client.gui.widget.GuiAnimatedStat;
import me.desht.pneumaticcraft.common.block.Blockss;
import me.desht.pneumaticcraft.common.inventory.Container4UpgradeSlots;
import me.desht.pneumaticcraft.common.tileentity.TileEntityElectrostaticCompressor;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SideOnly(Side.CLIENT)
public class GuiElectrostaticCompressor extends GuiPneumaticContainerBase<TileEntityElectrostaticCompressor> {
    private int connectedCompressors = 1;
    private int ticksExisted;
    private GuiAnimatedStat electrostaticStat;

    public GuiElectrostaticCompressor(InventoryPlayer player, TileEntityElectrostaticCompressor te) {
        super(new Container4UpgradeSlots(player, te), te, Textures.GUI_4UPGRADE_SLOTS);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.electrostaticStat = this.addAnimatedStat("gui.tab.electrostaticCompressor.info.title", new ItemStack(Blockss.ELECTROSTATIC_COMPRESSOR), 0xFF20A0FF, false);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int x, int y) {
        super.drawGuiContainerForegroundLayer(x, y);
        this.fontRenderer.drawString("Upgr.", 53, 19, 4210752);
    }

    @Override
    protected void addWarnings(List<String> textList) {
        super.addWarnings(textList);
        if (PneumaticValues.MAX_REDIRECTION_PER_IRON_BAR * this.te.ironBarsBeneath < PneumaticValues.PRODUCTION_ELECTROSTATIC_COMPRESSOR / this.connectedCompressors) {
            textList.add("gui.tab.problems.electrostatic.notEnoughGrounding");
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (this.ticksExisted % 20 == 0) {
            Set<BlockPos> positions = new HashSet<>();
            positions.add(this.te.getPos());
            this.te.getElectrostaticGrid(positions, this.te.getWorld(), this.te.getPos(), null);
            this.connectedCompressors = 0;
            for (BlockPos coord : positions) {
                if (this.te.getWorld().getBlockState(coord).getBlock() == Blockss.ELECTROSTATIC_COMPRESSOR) {
                    this.connectedCompressors++;
                }
            }
        }

        this.ticksExisted++;

        List<String> info = new ArrayList<>();
        info.add(TextFormatting.WHITE + "Energy production:");
        info.add(TextFormatting.BLACK + PneumaticCraftUtils.roundNumberTo(PneumaticValues.PRODUCTION_ELECTROSTATIC_COMPRESSOR / (float) this.connectedCompressors, 1) + " mL/lightning strike");
        info.add(TextFormatting.BLACK + "(" + this.connectedCompressors + " connected compressors)");
        info.add(TextFormatting.WHITE + "Maximum air redirection:");
        info.add(TextFormatting.BLACK + PneumaticCraftUtils.roundNumberTo(PneumaticValues.MAX_REDIRECTION_PER_IRON_BAR * this.te.ironBarsBeneath, 1) + " mL/lightning strike");
        info.add(TextFormatting.WHITE + "Lightning rod length (iron bars above):");
        info.add(TextFormatting.BLACK + "" + this.te.ironBarsAbove);
        String t = PneumaticCraftUtils.convertTicksToMinutesAndSeconds(this.te.getStrikeChance(), false);
        info.add(TextFormatting.WHITE + "Average strike time: ");
        info.add(TextFormatting.BLACK + t + " (with optimal-sized grid)");

        this.electrostaticStat.setText(info);
    }
}
