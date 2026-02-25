package me.desht.pneumaticcraft.client.gui.widget;

import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.math.NumberUtils;

public class WidgetTextFieldNumber extends WidgetTextField {

    public int minValue = Integer.MIN_VALUE;
    public int maxValue = Integer.MAX_VALUE;
    private int decimals;

    public WidgetTextFieldNumber(FontRenderer fontRenderer, int x, int y, int width, int height) {
        super(fontRenderer, x, y, width, height);
        this.setValue(0);

        this.setValidator(input -> {
            if (input == null || input.isEmpty() || input.equals("-")) {
                return true;  // treat as numeric zero
            }
            return NumberUtils.isCreatable(input);
        });
    }

    public WidgetTextFieldNumber setDecimals(int decimals) {
        this.decimals = decimals;
        return this;
    }

    @Override
    public void onMouseClicked(int mouseX, int mouseY, int button) {
        boolean wasFocused = this.isFocused();
        super.onMouseClicked(mouseX, mouseY, button);
        if (this.isFocused()) {
            if (!wasFocused) { //setText("");
                this.setCursorPositionEnd();
                this.setSelectionPos(0);
            }
        } else {
            this.setValue(this.getDoubleValue());
        }
    }

    public WidgetTextFieldNumber setValue(double value) {
        this.setText(PneumaticCraftUtils.roundNumberTo(value, this.decimals));
        return this;
    }

    public int getValue() {
        return MathHelper.clamp(NumberUtils.toInt(this.getText()), this.minValue, this.maxValue);
    }

    public double getDoubleValue() {
        return PneumaticCraftUtils.roundNumberToDouble(MathHelper.clamp(NumberUtils.toDouble(this.getText()), this.minValue, this.maxValue), this.decimals);
    }
}
