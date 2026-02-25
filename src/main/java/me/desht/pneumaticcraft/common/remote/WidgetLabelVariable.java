package me.desht.pneumaticcraft.common.remote;

import me.desht.pneumaticcraft.client.gui.widget.WidgetLabel;
import net.minecraft.client.Minecraft;

import java.awt.*;

public class WidgetLabelVariable extends WidgetLabel {
    private final TextVariableParser parser;

    public WidgetLabelVariable(int x, int y, String text) {
        super(x, y, text);
        this.parser = new TextVariableParser(text);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTick) {
        String oldText = this.text;
        this.text = this.parser.parse();
        super.render(mouseX, mouseY, partialTick);
        this.text = oldText;
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(this.x, this.y, Minecraft.getMinecraft().fontRenderer.getStringWidth(this.parser.parse()), Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT);
    }
}
