package me.desht.pneumaticcraft.common.progwidgets;

import me.desht.pneumaticcraft.client.gui.GuiProgrammer;
import me.desht.pneumaticcraft.client.gui.programmer.GuiProgWidgetCondition;
import me.desht.pneumaticcraft.client.gui.widget.GuiCheckBox;
import me.desht.pneumaticcraft.client.gui.widget.IGuiWidget;
import me.desht.pneumaticcraft.common.ai.DroneAIBlockCondition;
import me.desht.pneumaticcraft.common.ai.DroneAIDig;
import me.desht.pneumaticcraft.common.ai.IDroneBase;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ProgWidgetBlockCondition extends ProgWidgetCondition {
    private boolean checkingForAir;
    private boolean checkingForLiquids;

    @Override
    public String getWidgetString() {
        return "conditionBlock";
    }

    @Override
    public Class<? extends IProgWidget>[] getParameters() {
        return new Class[]{ProgWidgetArea.class, ProgWidgetItemFilter.class, ProgWidgetString.class};
    }

    @Override
    protected DroneAIBlockCondition getEvaluator(IDroneBase drone, IProgWidget widget) {
        return new DroneAIBlockCondition(drone, (ProgWidgetAreaItemBase) widget) {

            @Override
            protected boolean evaluate(BlockPos pos) {
                if (ProgWidgetBlockCondition.this.checkingForAir && this.drone.world().isAirBlock(pos)) return true;
                if (ProgWidgetBlockCondition.this.checkingForLiquids && PneumaticCraftUtils.isBlockLiquid(this.drone.world().getBlockState(pos).getBlock()))
                    return true;
                if (!ProgWidgetBlockCondition.this.checkingForAir && !ProgWidgetBlockCondition.this.checkingForLiquids || ProgWidgetBlockCondition.this.getConnectedParameters()[1] != null) {
                    return DroneAIDig.isBlockValidForFilter(this.drone.world(), this.drone, pos, this.widget);
                } else {
                    return false;
                }
            }
        };
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiScreen getOptionWindow(GuiProgrammer guiProgrammer) {
        return new GuiProgWidgetCondition(this, guiProgrammer) {
            @Override
            public void initGui() {
                super.initGui();
                this.addWidget(new GuiCheckBox(500, this.guiLeft + 5, this.guiTop + 60, 0xFF404040, I18n.format("gui.progWidget.conditionBlock.checkForAir")).setChecked(ProgWidgetBlockCondition.this.checkingForAir).setTooltip(I18n.format("gui.progWidget.conditionBlock.checkForAir.tooltip")));
                this.addWidget(new GuiCheckBox(501, this.guiLeft + 5, this.guiTop + 72, 0xFF404040, I18n.format("gui.progWidget.conditionBlock.checkForLiquids")).setChecked(ProgWidgetBlockCondition.this.checkingForLiquids).setTooltip(I18n.format("gui.progWidget.conditionBlock.checkForLiquids.tooltip")));
            }

            @Override
            protected boolean requiresNumber() {
                return false;
            }

            @Override
            protected boolean isSidedWidget() {
                return false;
            }

            @Override
            public void actionPerformed(IGuiWidget widget) {
                switch (widget.getID()) {
                    case 500:
                        ProgWidgetBlockCondition.this.checkingForAir = !ProgWidgetBlockCondition.this.checkingForAir;
                        break;
                    case 501:
                        ProgWidgetBlockCondition.this.checkingForLiquids = !ProgWidgetBlockCondition.this.checkingForLiquids;
                        break;
                    default:
                        super.actionPerformed(widget);
                        break;
                }
            }

        };
    }

    @Override
    public ResourceLocation getTexture() {
        return Textures.PROG_WIDGET_CONDITION_BLOCK;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setBoolean("checkingForAir", this.checkingForAir);
        tag.setBoolean("checkingForLiquids", this.checkingForLiquids);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.checkingForAir = tag.getBoolean("checkingForAir");
        this.checkingForLiquids = tag.getBoolean("checkingForLiquids");
    }

}
