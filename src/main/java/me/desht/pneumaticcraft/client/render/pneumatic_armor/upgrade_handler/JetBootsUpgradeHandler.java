package me.desht.pneumaticcraft.client.render.pneumatic_armor.upgrade_handler;

import me.desht.pneumaticcraft.api.client.IGuiAnimatedStat;
import me.desht.pneumaticcraft.api.client.pneumaticHelmet.IOptionPage;
import me.desht.pneumaticcraft.api.client.pneumaticHelmet.IUpgradeRenderHandler;
import me.desht.pneumaticcraft.api.item.IItemRegistry;
import me.desht.pneumaticcraft.client.gui.pneumatic_armor.GuiJetBootsOptions;
import me.desht.pneumaticcraft.client.gui.widget.GuiAnimatedStat;
import me.desht.pneumaticcraft.common.config.ArmorHUDLayout;
import me.desht.pneumaticcraft.common.item.Itemss;
import me.desht.pneumaticcraft.common.recipes.CraftingRegistrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class JetBootsUpgradeHandler extends IUpgradeRenderHandler.SimpleToggleableRenderHandler {

    private static final String[] HEADINGS = new String[]{"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

    private String l1, l2, l3, r1, r2, r3;
    private int widestR;

    @SideOnly(Side.CLIENT)
    private IGuiAnimatedStat jbStat;

    @Override
    public String getUpgradeName() {
        return "jetBoots";
    }

    @Override
    public Item[] getRequiredUpgrades() {
        return new Item[]{Itemss.upgrades.get(IItemRegistry.EnumUpgrade.JET_BOOTS)};
    }

    @Override
    public IOptionPage getGuiOptionsPage() {
        return new GuiJetBootsOptions(this);
    }

    @Override
    public EntityEquipmentSlot getEquipmentSlot() {
        return EntityEquipmentSlot.FEET;
    }

    @Override
    public void update(EntityPlayer player, int rangeUpgrades) {
        super.update(player, rangeUpgrades);

        String g1 = TextFormatting.WHITE.toString();
        String g2 = TextFormatting.GREEN.toString();

        if (this.jbStat.isClicked()) {
            double mx = player.posX - player.lastTickPosX;
            double my = player.posY - player.lastTickPosY;
            double mz = player.posZ - player.lastTickPosZ;
            double v = Math.sqrt(mx * mx + my * my + mz * mz);
            double vg = Math.sqrt(mx * mx + mz * mz);
            int heading = MathHelper.floor((double) (player.rotationYaw * 8.0F / 360.0F) + 0.5D) & 0x7;
            int yaw = ((int) player.rotationYaw + 180) % 360;
            if (yaw < 0) yaw += 360;
            BlockPos pos = player.getPosition();

            this.l1 = String.format(" %sSpd: %s%05.2fm/s", g1, g2, v * 20);
            this.l2 = String.format("  %sAlt: %s%03dm", g1, g2, pos.getY());
            this.l3 = String.format("%sHead: %s%d° (%s)", g1, g2, yaw, HEADINGS[heading]);
            this.r1 = String.format("%sGnd: %s%05.2f", g1, g2, vg * 20);
            this.r2 = String.format("%sGnd: %s%dm", g1, g2, pos.getY() - player.world.getHeight(pos.getX(), pos.getZ()));
            this.r3 = String.format("%sPch: %s%d°", g1, g2, (int) -player.rotationPitch);
            FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
            int wl = Math.max(fr.getStringWidth(this.l1), Math.max(fr.getStringWidth(this.l2), fr.getStringWidth(this.l3)));
            int wr = Math.max(fr.getStringWidth(this.r1), Math.max(fr.getStringWidth(this.r2), fr.getStringWidth(this.r3)));
            if (wl + wr + 16 > this.jbStat.getWidth()) {
                this.jbStat.setForcedDimensions(wl + wr + 16, 5 * fr.FONT_HEIGHT);
            }
            this.widestR = Math.max(wr, this.widestR);
        }
    }

    @Override
    public void render2D(float partialTicks, boolean helmetEnabled) {
        super.render2D(partialTicks, helmetEnabled);

        if (helmetEnabled && this.jbStat.isClicked()) {
            FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
            int xl = this.jbStat.getBaseX() + 5;
            int y = this.jbStat.getBaseY() + fr.FONT_HEIGHT + 8;
            int xr = this.jbStat.getBaseX() + this.jbStat.getWidth() - 5;
            if (this.jbStat.isLeftSided()) {
                xl -= this.jbStat.getWidth();
                xr -= this.jbStat.getWidth();
            }
            fr.drawStringWithShadow(this.l1, xl, y, 0x404040);
            fr.drawStringWithShadow(this.l2, xl, y + fr.FONT_HEIGHT, 0x404040);
            fr.drawStringWithShadow(this.l3, xl, y + fr.FONT_HEIGHT * 2, 0x404040);
            fr.drawStringWithShadow(this.r1, xr - this.widestR, y, 0x404040);
            fr.drawStringWithShadow(this.r2, xr - this.widestR, y + fr.FONT_HEIGHT, 0x404040);
            fr.drawStringWithShadow(this.r3, xr - this.widestR, y + fr.FONT_HEIGHT * 2, 0x404040);
        }
    }

    @Override
    public IGuiAnimatedStat getAnimatedStat() {
        if (this.jbStat == null) {
            this.jbStat = new GuiAnimatedStat(null, "Jet Boots",
                    GuiAnimatedStat.StatIcon.of(CraftingRegistrator.getUpgrade(IItemRegistry.EnumUpgrade.JET_BOOTS)),
                    0x3000AA00, null, ArmorHUDLayout.INSTANCE.jetBootsStat);
            this.jbStat.setMinDimensionsAndReset(0, 0);
            FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
            int n = fr.getStringWidth("  Spd: 00.00m/s   Gnd: 00.00");
            this.jbStat.addPadding(3, n / fr.getStringWidth(" "));
        }
        return this.jbStat;
    }

    @Override
    public void onResolutionChanged() {
        this.jbStat = null;
    }
}
