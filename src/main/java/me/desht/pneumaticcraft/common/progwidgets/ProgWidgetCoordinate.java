package me.desht.pneumaticcraft.common.progwidgets;

import me.desht.pneumaticcraft.client.gui.GuiProgrammer;
import me.desht.pneumaticcraft.client.gui.programmer.GuiProgWidgetCoordinate;
import me.desht.pneumaticcraft.common.ai.DroneAIManager;
import me.desht.pneumaticcraft.common.item.ItemGPSTool;
import me.desht.pneumaticcraft.common.item.ItemPlastic;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.Set;

public class ProgWidgetCoordinate extends ProgWidget implements IVariableWidget {

    private int x, y, z;
    private String variable = "";
    private boolean useVariable;
    private DroneAIManager aiManager;

    @Override
    public boolean hasStepInput() {
        return false;
    }

    @Override
    public Class<? extends IProgWidget> returnType() {
        return ProgWidgetCoordinate.class;
    }

    @Override
    public Class<? extends IProgWidget>[] getParameters() {
        return new Class[]{ProgWidgetCoordinate.class};
    }

    @Override
    public void addWarnings(List<String> curInfo, List<IProgWidget> widgets) {
        super.addWarnings(curInfo, widgets);
        if (!this.useVariable && this.x == 0 && this.y == 0 && this.z == 0) {
            curInfo.add("gui.progWidget.coordinate.warning.noCoordinate");
        }
    }

    @Override
    public void addErrors(List<String> curInfo, List<IProgWidget> widgets) {
        super.addErrors(curInfo, widgets);
        if (this.useVariable && this.variable.equals("")) {
            curInfo.add("gui.progWidget.general.error.emptyVariable");
        }
    }

    @Override
    public String getWidgetString() {
        return "coordinate";
    }

    @Override
    public int getCraftingColorIndex() {
        return ItemPlastic.GREEN;
    }

    @Override
    public WidgetDifficulty getDifficulty() {
        return WidgetDifficulty.ADVANCED;
    }

    @Override
    public ResourceLocation getTexture() {
        return Textures.PROG_WIDGET_COORDINATE;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("posX", this.x);
        tag.setInteger("posY", this.y);
        tag.setInteger("posZ", this.z);
        tag.setString("variable", this.variable);
        tag.setBoolean("useVariable", this.useVariable);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.x = tag.getInteger("posX");
        this.y = tag.getInteger("posY");
        this.z = tag.getInteger("posZ");
        this.variable = tag.getString("variable");
        this.useVariable = tag.getBoolean("useVariable");
    }

    @Override
    public void setAIManager(DroneAIManager aiManager) {
        this.aiManager = aiManager;
    }

    public BlockPos getCoordinate() {
        if (this.useVariable && this.aiManager != null) {
            return this.aiManager.getCoordinate(this.variable);
        } else {
            return this.getRawCoordinate();
        }
    }

    public BlockPos getRawCoordinate() {
        return new BlockPos(this.x, this.y, this.z);
    }

    public void setCoordinate(BlockPos pos) {
        if (pos != null) {
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
        } else {
            this.x = this.y = this.z = 0;
        }
    }

    public void setVariable(String varName) {
        this.variable = varName;
    }

    public String getVariable() {
        return this.variable;
    }

    public boolean isUsingVariable() {
        return this.useVariable;
    }

    public void setUsingVariable(boolean useVariable) {
        this.useVariable = useVariable;
    }

    public void loadFromGPSTool(ItemStack gpsTool) {
        String variable = ItemGPSTool.getVariable(gpsTool);
        if ("".equals(variable)) {
            this.setCoordinate(ItemGPSTool.getGPSLocation(gpsTool));
            this.setUsingVariable(false);
        } else {
            this.setVariable("#" + variable);
            this.setUsingVariable(true);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiScreen getOptionWindow(GuiProgrammer guiProgrammer) {
        return new GuiProgWidgetCoordinate(this, guiProgrammer);
    }

    @Override
    public void getTooltip(List<String> curTooltip) {
        super.getTooltip(curTooltip);

        if (this.useVariable) curTooltip.add("XYZ: \"" + this.variable + "\"");
        else if (this.x != 0 || this.y != 0 || this.z != 0) curTooltip.add("X: " + this.x + ", Y: " + this.y + ", Z: " + this.z);
    }

    @Override
    public String getExtraStringInfo() {
        if (this.useVariable) return "\"" + this.variable + "\"";
        else return this.x != 0 || this.y != 0 || this.z != 0 ? this.x + ", " + this.y + ", " + this.z : null;
    }

    @Override
    public void addVariables(Set<String> variables) {
        variables.add(this.variable);
    }
}
