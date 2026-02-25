package me.desht.pneumaticcraft.client.gui.programmer;

import com.google.common.collect.Lists;
import me.desht.pneumaticcraft.api.item.IPositionProvider;
import me.desht.pneumaticcraft.client.gui.GuiButtonSpecial;
import me.desht.pneumaticcraft.client.gui.GuiInventorySearcher;
import me.desht.pneumaticcraft.client.gui.GuiProgrammer;
import me.desht.pneumaticcraft.client.gui.widget.*;
import me.desht.pneumaticcraft.common.config.ConfigHandler;
import me.desht.pneumaticcraft.common.item.ItemGPSTool;
import me.desht.pneumaticcraft.common.item.Itemss;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetArea;
import me.desht.pneumaticcraft.common.progwidgets.area.AreaType;
import me.desht.pneumaticcraft.common.progwidgets.area.AreaType.AreaTypeWidget;
import me.desht.pneumaticcraft.common.progwidgets.area.AreaType.AreaTypeWidgetEnum;
import me.desht.pneumaticcraft.common.progwidgets.area.AreaType.AreaTypeWidgetInteger;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.FMLClientHandler;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class GuiProgWidgetArea extends GuiProgWidgetAreaShow<ProgWidgetArea> {
    private GuiInventorySearcher invSearchGui;
    private int pointSearched;
    private WidgetComboBox variableField1;
    private WidgetComboBox variableField2;

    private final List<AreaType> allAreaTypes = ProgWidgetArea.getAllAreaTypes();
    private final List<Pair<AreaTypeWidget, IGuiWidget>> areaTypeValueWidgets = new ArrayList<>();
    private final List<IGuiWidget> areaTypeStaticWidgets = new ArrayList<>();

    public GuiProgWidgetArea(ProgWidgetArea widget, GuiProgrammer guiProgrammer) {
        super(widget, guiProgrammer);
        this.xSize = 256;
    }

    @Override
    public void initGui() {
        super.initGui();

        this.addLabel(I18n.format("gui.progWidget.area.point1"), this.guiLeft + 50, this.guiTop + 10);
        this.addLabel(I18n.format("gui.progWidget.area.point2"), this.guiLeft + 177, this.guiTop + 10);
        this.addLabel(I18n.format("gui.progWidget.area.type"), this.guiLeft + 4, this.guiTop + 50);

        boolean advancedMode = ConfigHandler.getProgrammerDifficulty() == 2;
        GuiButtonSpecial gpsButton1 = new GuiButtonSpecial(0, this.guiLeft + (advancedMode ? 6 : 55), this.guiTop + 20, 20, 20, "");
        GuiButtonSpecial gpsButton2 = new GuiButtonSpecial(1, this.guiLeft + (advancedMode ? 133 : 182), this.guiTop + 20, 20, 20, "");
        gpsButton1.setRenderStacks(new ItemStack(Itemss.GPS_TOOL));
        gpsButton2.setRenderStacks(new ItemStack(Itemss.GPS_TOOL));
        this.buttonList.add(gpsButton1);
        this.buttonList.add(gpsButton2);

        this.variableField1 = new WidgetComboBox(this.fontRenderer, this.guiLeft + 28, this.guiTop + 25, 88, this.fontRenderer.FONT_HEIGHT + 1);
        this.variableField2 = new WidgetComboBox(this.fontRenderer, this.guiLeft + 155, this.guiTop + 25, 88, this.fontRenderer.FONT_HEIGHT + 1);
        Set<String> variables = this.guiProgrammer == null ? Collections.emptySet() : this.guiProgrammer.te.getAllVariables();
        this.variableField1.setElements(variables);
        this.variableField2.setElements(variables);
        this.variableField1.setText(this.widget.getCoord1Variable());
        this.variableField2.setText(this.widget.getCoord2Variable());

        if (advancedMode) {
            this.addWidget(this.variableField1);
            this.addWidget(this.variableField2);
        }

        final int widgetsPerColumn = 5;
        List<GuiRadioButton> radioButtons = new ArrayList<>();
        for (int i = 0; i < this.allAreaTypes.size(); i++) {
            AreaType areaType = this.allAreaTypes.get(i);
            GuiRadioButton radioButton = new GuiRadioButton(i, this.guiLeft + widgetsPerColumn + i / widgetsPerColumn * 80, this.guiTop + 60 + i % widgetsPerColumn * 12, 0xFF404040, areaType.getName());
            if (this.widget.type.getClass() == areaType.getClass()) {
                this.allAreaTypes.set(i, this.widget.type);
                radioButton.checked = true;
            }

            this.addWidget(radioButton);
            radioButtons.add(radioButton);
            radioButton.otherChoices = radioButtons;
        }

        //typeInfoField.setTooltip(I18n.format("gui.progWidget.area.extraInfo.tooltip"));
        //addWidget(new WidgetLabel(guiLeft + 160, guiTop + 100, I18n.format("gui.progWidget.area.extraInfo")));
        this.switchToWidgets(this.widget.type);

        if (this.invSearchGui != null) {
            ItemStack stack = this.invSearchGui.getSearchStack();
            if (stack.getItem() instanceof IPositionProvider) {
                List<BlockPos> posList = ((IPositionProvider) stack.getItem()).getStoredPositions(stack);
                if (!posList.isEmpty()) {
                    BlockPos pos = posList.get(0);
                    if (pos != null) {
                        if (this.pointSearched == 0) {
                            this.widget.x1 = pos.getX();
                            this.widget.y1 = pos.getY();
                            this.widget.z1 = pos.getZ();
                        } else {
                            this.widget.x2 = pos.getX();
                            this.widget.y2 = pos.getY();
                            this.widget.z2 = pos.getZ();
                        }
                    } else {
                        if (this.pointSearched == 0) {
                            this.widget.x1 = this.widget.y1 = this.widget.z1 = 0;
                        } else {
                            this.widget.x2 = this.widget.y2 = this.widget.z2 = 0;
                        }
                    }
                }
            }
        }

        List<String> b1List = Lists.newArrayList(I18n.format("gui.progWidget.area.selectGPS1"));
        if (this.widget.x1 != 0 || this.widget.y1 != 0 || this.widget.z1 != 0) {
            b1List.add(String.format(TextFormatting.GRAY + "[Current] %d, %d, %d", this.widget.x1, this.widget.y1, this.widget.z1));
        }
        gpsButton1.setTooltipText(b1List);

        List<String> b2List = Lists.newArrayList(I18n.format("gui.progWidget.area.selectGPS2"));
        if (this.widget.x2 != 0 || this.widget.y2 != 0 || this.widget.z2 != 0) {
            b2List.add(String.format(TextFormatting.GRAY + "[Current] %d, %d, %d", this.widget.x2, this.widget.y2, this.widget.z2));
        }
        gpsButton2.setTooltipText(b2List);
    }

    private void switchToWidgets(AreaType type) {
        this.saveWidgets();

        this.areaTypeValueWidgets.forEach(p -> this.removeWidget(p.getRight()));
        this.areaTypeStaticWidgets.forEach(this::removeWidget);

        this.areaTypeValueWidgets.clear();
        this.areaTypeStaticWidgets.clear();

        int curY = this.guiTop + 60;
        int x = this.guiLeft + 150;
        List<AreaTypeWidget> widgets = new ArrayList<>();
        type.addUIWidgets(widgets);
        for (AreaTypeWidget widget : widgets) {
            WidgetLabel titleWidget = new WidgetLabel(x, curY, I18n.format(widget.title));
            this.addWidget(titleWidget);
            this.areaTypeStaticWidgets.add(titleWidget);
            curY += this.fontRenderer.FONT_HEIGHT + 1;

            if (widget instanceof AreaTypeWidgetInteger) {
                AreaTypeWidgetInteger intWidget = (AreaTypeWidgetInteger) widget;
                WidgetTextFieldNumber intField = new WidgetTextFieldNumber(this.fontRenderer, x, curY, 40, this.fontRenderer.FONT_HEIGHT + 1);
                intField.setValue(intWidget.readAction.get());
                this.addWidget(intField);
                this.areaTypeValueWidgets.add(new ImmutablePair<>(widget, intField));

                curY += this.fontRenderer.FONT_HEIGHT + 20;
            } else if (widget instanceof AreaTypeWidgetEnum<?>) {
                AreaTypeWidgetEnum<?> enumWidget = (AreaTypeWidgetEnum<?>) widget;
                WidgetComboBox enumCbb = new WidgetComboBox(this.fontRenderer, x, curY, 80, this.fontRenderer.FONT_HEIGHT + 1).setFixedOptions();
                enumCbb.setElements(this.getEnumNames(enumWidget.enumClass));
                enumCbb.setText(enumWidget.readAction.get().toString());
                this.addWidget(enumCbb);
                this.areaTypeValueWidgets.add(new ImmutablePair<>(widget, enumCbb));

                curY += this.fontRenderer.FONT_HEIGHT + 20;
            } else {
                throw new IllegalStateException("Invalid widget type: " + widget.getClass());
            }
        }
    }

    private void saveWidgets() {
        for (Pair<AreaTypeWidget, IGuiWidget> entry : this.areaTypeValueWidgets) {
            AreaTypeWidget widget = entry.getLeft();
            IGuiWidget guiWidget = entry.getRight();
            if (widget instanceof AreaTypeWidgetInteger) {
                AreaTypeWidgetInteger intWidget = (AreaTypeWidgetInteger) widget;
                intWidget.writeAction.accept(((WidgetTextFieldNumber) guiWidget).getValue());
            } else if (widget instanceof AreaTypeWidgetEnum<?>) {
                @SuppressWarnings("unchecked")
                AreaTypeWidgetEnum<Enum<?>> enumWidget = (AreaTypeWidgetEnum<Enum<?>>) widget;
                WidgetComboBox cbb = (WidgetComboBox) guiWidget;
                List<String> enumNames = this.getEnumNames(enumWidget.enumClass);
                Object[] enumValues = enumWidget.enumClass.getEnumConstants();
                Object selectedValue = enumValues[enumNames.indexOf(cbb.getText())];
                enumWidget.writeAction.accept((Enum<?>) selectedValue);
            }
        }
    }

    private List<String> getEnumNames(Class<?> enumClass) {
        Object[] enumValues = enumClass.getEnumConstants();
        List<String> enumNames = new ArrayList<>();
        for (Object enumValue : enumValues) {
            enumNames.add(enumValue.toString());
        }
        return enumNames;
    }

    @Override
    public void actionPerformed(IGuiWidget guiWidget) {
        if (guiWidget instanceof GuiRadioButton) {
            AreaType areaType = this.allAreaTypes.get(guiWidget.getID());
            this.widget.type = areaType;
            this.switchToWidgets(areaType);
        }
        super.actionPerformed(guiWidget);
    }

    @Override
    public void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0 || button.id == 1) {
            this.invSearchGui = new GuiInventorySearcher(FMLClientHandler.instance().getClient().player);
            this.invSearchGui.setStackPredicate(itemStack -> itemStack.getItem() instanceof IPositionProvider);
            ItemStack gps = new ItemStack(Itemss.GPS_TOOL);
            if (button.id == 0) {
                ItemGPSTool.setGPSLocation(gps, new BlockPos(this.widget.x1, this.widget.y1, this.widget.z1));
            } else {
                ItemGPSTool.setGPSLocation(gps, new BlockPos(this.widget.x2, this.widget.y2, this.widget.z2));
            }
            this.invSearchGui.setSearchStack(ItemGPSTool.getGPSLocation(gps) != null ? gps : ItemStack.EMPTY);
            FMLClientHandler.instance().showGuiScreen(this.invSearchGui);
            this.pointSearched = button.id;
        }
        if (button.id == 1000) { //When the area is going to be displayed.
            this.saveWidgets();
        }
        super.actionPerformed(button);
    }

    @Override
    protected ResourceLocation getTexture() {
        return Textures.GUI_WIDGET_AREA;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();

        this.widget.setCoord1Variable(this.variableField1.getText());
        this.widget.setCoord2Variable(this.variableField2.getText());
        this.saveWidgets();
    }

}
