package me.desht.pneumaticcraft.client.gui;

import me.desht.pneumaticcraft.client.gui.widget.WidgetTextField;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTextFieldNumber;
import me.desht.pneumaticcraft.common.item.Itemss;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketChangeGPSToolCoordinate;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

public class GuiGPSTool extends GuiPneumaticScreenBase {

    private final WidgetTextFieldNumber[] textFields = new WidgetTextFieldNumber[3];
    private WidgetTextField variableField;
    private static final int TEXTFIELD_WIDTH = 40;
    private final BlockPos oldGPSLoc;
    private String oldVarName;
    private static final int[] BUTTON_ACTIONS = {-10, -1, 1, 10};
    private final int metadata;

    public GuiGPSTool(BlockPos gpsLoc, String oldVarName, int metadata) {
        this.oldGPSLoc = gpsLoc;
        this.oldVarName = oldVarName;
        this.metadata = metadata;
    }

    public GuiGPSTool(BlockPos gpsLoc, String oldVarName) {
        this(gpsLoc, oldVarName, -1);
    }

    @Override
    public void initGui() {
        super.initGui();
        int[] oldText = new int[3];
        if (this.textFields[0] == null) {
            oldText[0] = this.oldGPSLoc.getX();
            oldText[1] = this.oldGPSLoc.getY();
            oldText[2] = this.oldGPSLoc.getZ();
        } else {
            for (int i = 0; i < 3; i++)
                oldText[i] = this.textFields[i].getValue();
        }
        int xMiddle = this.width / 2;
        int yMiddle = this.height / 2;
        for (int i = 0; i < 3; i++) {
            this.textFields[i] = new WidgetTextFieldNumber(this.fontRenderer, xMiddle - TEXTFIELD_WIDTH / 2, yMiddle - 27 + i * 22, TEXTFIELD_WIDTH, this.fontRenderer.FONT_HEIGHT);
            this.textFields[i].setValue(oldText[i]);
            if (i == 1) {
                this.textFields[i].minValue = 0;
                this.textFields[i].maxValue = 255;
            }
            this.addWidget(this.textFields[i]);
        }

        for (int i = 0; i < 3; i++) {
            this.buttonList.add(new GuiButton(i * 4, xMiddle - 49 - TEXTFIELD_WIDTH / 2, yMiddle - 32 + i * 22, 22, 20, "-10"));
            this.buttonList.add(new GuiButton(1 + i * 4, xMiddle - 25 - TEXTFIELD_WIDTH / 2, yMiddle - 32 + i * 22, 22, 20, "-1"));
            this.buttonList.add(new GuiButton(2 + i * 4, xMiddle + 3 + TEXTFIELD_WIDTH / 2, yMiddle - 32 + i * 22, 22, 20, "+1"));
            this.buttonList.add(new GuiButton(3 + i * 4, xMiddle + 27 + TEXTFIELD_WIDTH / 2, yMiddle - 32 + i * 22, 22, 20, "+10"));
        }

        if (this.variableField != null) this.oldVarName = this.variableField.getText();
        this.variableField = new WidgetTextField(this.fontRenderer, xMiddle - 50, yMiddle + 60, 100, this.fontRenderer.FONT_HEIGHT);
        this.variableField.setText(this.oldVarName);
        this.addWidget(this.variableField);

        String var = I18n.format("gui.progWidget.coordinate.variable");
        this.addLabel(var, xMiddle - 62 - this.fontRenderer.getStringWidth(var), yMiddle + 61);
        this.addLabel("#", xMiddle - 60, yMiddle + 61);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        this.textFields[button.id / 4].setValue(this.textFields[button.id / 4].getValue() + BUTTON_ACTIONS[button.id % 4]);
    }

    @Override
    public void drawScreen(int par1, int par2, float par3) {
        this.drawDefaultBackground();
        super.drawScreen(par1, par2, par3);

        int xMiddle = this.width / 2;
        int yMiddle = this.height / 2;
        int stringX = xMiddle - 60 - TEXTFIELD_WIDTH / 2;
        this.drawCenteredString(this.fontRenderer, new ItemStack(Itemss.GPS_TOOL).getDisplayName(), xMiddle, yMiddle - 44, 0xFFFFFFFF);
        this.drawString(this.fontRenderer, "X:", stringX, yMiddle - 22 - this.fontRenderer.FONT_HEIGHT / 2, 0xFFFFFFFF);
        this.drawString(this.fontRenderer, "Y:", stringX, yMiddle - this.fontRenderer.FONT_HEIGHT / 2, 0xFFFFFFFF);
        this.drawString(this.fontRenderer, "Z:", stringX, yMiddle + 22 - this.fontRenderer.FONT_HEIGHT / 2, 0xFFFFFFFF);
    }

    @Override
    public void onGuiClosed() {
        BlockPos newPos = new BlockPos(this.textFields[0].getValue(), this.textFields[1].getValue(), this.textFields[2].getValue());
        NetworkHandler.sendToServer(new PacketChangeGPSToolCoordinate(newPos.equals(this.oldGPSLoc) ? new BlockPos(-1, -1, -1) : newPos, this.variableField.getText(), this.metadata));
    }

    @Override
    protected ResourceLocation getTexture() {
        return null;
    }
}
