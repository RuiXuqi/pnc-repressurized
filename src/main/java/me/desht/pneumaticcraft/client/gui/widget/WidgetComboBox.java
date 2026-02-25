package me.desht.pneumaticcraft.client.gui.widget;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class WidgetComboBox extends WidgetTextField {

    private final ArrayList<String> elements = new ArrayList<>();
    private final FontRenderer fontRenderer;
    private boolean enabled = true;
    private boolean fixedOptions;
    private boolean shouldSort = true;
    private int selectedIndex = -1;

    public WidgetComboBox(FontRenderer fontRenderer, int x, int y, int width, int height) {
        super(fontRenderer, x, y, width, height);
        this.fontRenderer = fontRenderer;
    }

    public WidgetComboBox setElements(Collection<String> elements) {
        this.elements.clear();
        this.elements.addAll(elements);
        if (this.shouldSort) Collections.sort(this.elements);
        return this;
    }

    public WidgetComboBox setElements(String[] elements) {
        this.elements.clear();
        this.elements.ensureCapacity(elements.length);
        this.elements.addAll(Arrays.asList(elements));
        if (this.shouldSort) Collections.sort(this.elements);
        return this;
    }

    public WidgetComboBox setShouldSort(boolean shouldSort) {
        this.shouldSort = shouldSort;
        return this;
    }

    private List<String> getApplicableElements() {
        return this.elements.stream()
                .filter(element -> this.fixedOptions || element.toLowerCase().contains(this.getText().toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public void postRender(int mouseX, int mouseY, float partialTick) {
        super.postRender(mouseX, mouseY, partialTick);

        if (this.enabled && this.isFocused()) {
            List<String> applicableElements = this.getApplicableElements();
            GlStateManager.translate(0, 0, 300);
            drawRect(this.x - 1, this.y + this.height + 1, this.x + this.width + 1, this.y + this.height + 3 + applicableElements.size() * this.fontRenderer.FONT_HEIGHT, 0xFFA0A0A0);
            drawRect(this.x, this.y + this.height + 1, this.x + this.width, this.y + this.height + 2 + applicableElements.size() * this.fontRenderer.FONT_HEIGHT, 0xFF000000);
            for (int i = 0; i < applicableElements.size(); i++) {
                String element = applicableElements.get(i);
                this.fontRenderer.drawStringWithShadow(this.fontRenderer.trimStringToWidth(element, this.getWidth()), this.x + 4, this.y + this.height + 2 + i * this.fontRenderer.FONT_HEIGHT, 0xE0E0E0);
                this.fontRenderer.drawString("\u25b2", this.x + this.width - 6, this.y + 1, 0xc0c0c0);
            }
            GlStateManager.translate(0, 0, -300);
        } else {
            this.fontRenderer.drawString("\u25bc", this.x + this.width - 6, this.y + 1, 0xc0c0c0);
        }
    }

    @Override
    public void onMouseClicked(int mouseX, int mouseY, int button) {
        if (!this.fixedOptions || button != 1) super.onMouseClicked(mouseX, mouseY, button);

        if (this.enabled) {
            if (mouseY < this.y + this.height && mouseX > this.x + this.width - 8 && this.isFocused()) {
                this.setFocused(false);
            } else {
                this.setFocused(true);
                List<String> applicableElements = this.getApplicableElements();
                for (int i = 0; i < applicableElements.size(); i++) {
                    if (new Rectangle(this.x - 1, this.y + this.height + 2 + i * this.fontRenderer.FONT_HEIGHT, this.width, this.fontRenderer.FONT_HEIGHT).contains(mouseX, mouseY)) {
                        this.setText(applicableElements.get(i));
                        this.selectedIndex = i;
                        this.listener.onKeyTyped(this);
                        this.setFocused(false);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void onMouseClickedOutsideBounds(int mouseX, int mouseY, int button) {
        this.setFocused(false);
    }

    @Override
    public Rectangle getBounds() {
        return this.enabled && this.isFocused() ? new Rectangle(this.x, this.y, this.width, this.height + 2 + this.getApplicableElements().size() * this.fontRenderer.FONT_HEIGHT) : super.getBounds();
    }

    @Override
    public boolean onKey(char key, int keyCode) {
        if (this.fixedOptions) return false;
        if (this.enabled && this.isFocused() && keyCode == Keyboard.KEY_TAB) {//Auto-complete
            List<String> applicableElements = this.getApplicableElements();
            if (applicableElements.size() > 0) {
                this.setText(applicableElements.get(0));
                this.listener.onKeyTyped(this);
                return true;
            } else {
                return super.onKey(key, keyCode);
            }
        } else {
            return super.onKey(key, keyCode);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.enabled = enabled;
    }

    public WidgetComboBox setFixedOptions() {
        this.fixedOptions = true;
        return this;
    }

    public int getSelectedElementIndex() {
        return this.selectedIndex;
    }

    public void selectElement(int index) {
        if (index >= 0 && index < this.elements.size()) {
            this.selectedIndex = index;
            this.setText(this.elements.get(index));
        }
    }
}
