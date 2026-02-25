package me.desht.pneumaticcraft.client.render.pneumatic_armor;

import me.desht.pneumaticcraft.client.gui.widget.GuiAnimatedStat;
import me.desht.pneumaticcraft.common.config.ArmorHUDLayout;
import me.desht.pneumaticcraft.lib.Sounds;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

@SideOnly(Side.CLIENT)
public class ArmorMessage {
    private final GuiAnimatedStat stat;
    int lifeSpan;

    public ArmorMessage(String title, List<String> message, int duration, int backColor) {
        this.lifeSpan = duration;
        this.stat = new GuiAnimatedStat(null, title, GuiAnimatedStat.StatIcon.NONE, backColor, null, ArmorHUDLayout.INSTANCE.messageStat);
        this.stat.setMinDimensionsAndReset(0, 0);
        this.stat.setText(message);
        EntityPlayer player = FMLClientHandler.instance().getClient().player;
        player.world.playSound(player.posX, player.posY, player.posZ, Sounds.SCIFI, SoundCategory.PLAYERS, 0.1F, 1.0F, true);
    }

    void setDependingMessage(GuiAnimatedStat dependingStat) {
        this.stat.setParentStat(dependingStat);
        this.stat.setBaseY(2);
    }

    public GuiAnimatedStat getStat() {
        return this.stat;
    }

    void renderMessage(FontRenderer fontRenderer, float partialTicks) {
        if (this.lifeSpan > 10) {
            this.stat.openWindow();
        } else {
            this.stat.closeWindow();
        }
        this.stat.render(-1, -1, partialTicks);
    }
}
