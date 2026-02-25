package me.desht.pneumaticcraft.common.progwidgets;

import me.desht.pneumaticcraft.client.gui.GuiProgrammer;
import me.desht.pneumaticcraft.client.gui.programmer.GuiProgWidgetCondition;
import me.desht.pneumaticcraft.common.ai.IDroneBase;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class ProgWidgetDroneEvaluation extends ProgWidgetConditionBase implements ICondition {

    private boolean isAndFunction;
    private ICondition.Operator operator = ICondition.Operator.HIGHER_THAN_EQUALS;
    private int requiredCount = 1;

    @Override
    public boolean isAndFunction() {
        return this.isAndFunction;
    }

    @Override
    public void setAndFunction(boolean isAndFunction) {
        this.isAndFunction = isAndFunction;
    }

    @Override
    public boolean evaluate(IDroneBase drone, IProgWidget widget) {
//        int count = getCount(drone, widget);
        return this.getOperator().evaluate(this.getCount(drone, widget), this.getRequiredCount());
//        return getOperator() == Operator.EQUALS ? count == getRequiredCount() : count >= getRequiredCount();
    }

    protected abstract int getCount(IDroneBase drone, IProgWidget widget);

    @Override
    public EntityAIBase getWidgetAI(IDroneBase drone, IProgWidget widget) {
        if (widget instanceof ProgWidgetDroneEvaluation) {
            return null;
        } else {
            return new EntityAIBase() {//Trick the CC program into thinking this is an executable piece.
                @Override
                public boolean shouldExecute() {
                    return false;
                }
            };
        }
    }

    @Override
    public int getRequiredCount() {
        return this.requiredCount;
    }

    @Override
    public void setRequiredCount(int count) {
        this.requiredCount = count;
    }

    @Override
    public Operator getOperator() {
        return this.operator;
    }

    @Override
    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setBoolean("isAndFunction", this.isAndFunction);
        tag.setByte("operator", (byte) this.operator.ordinal());
        tag.setInteger("requiredCount", this.requiredCount);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.isAndFunction = tag.getBoolean("isAndFunction");
        this.operator = ICondition.Operator.values()[tag.getByte("operator")];
        this.requiredCount = tag.getInteger("requiredCount");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiScreen getOptionWindow(GuiProgrammer guiProgrammer) {
        return new GuiProgWidgetCondition(this, guiProgrammer) {
            @Override
            protected boolean isUsingAndOr() {
                return false;
            }
        };
    }

    @Override
    public String getExtraStringInfo() {
        String anyAll = I18n.format(this.isAndFunction() ? "gui.progWidget.condition.all" : "gui.progWidget.condition.any");
        return anyAll + " " + this.getOperator().toString() + " " + this.getRequiredCount();
    }

}
