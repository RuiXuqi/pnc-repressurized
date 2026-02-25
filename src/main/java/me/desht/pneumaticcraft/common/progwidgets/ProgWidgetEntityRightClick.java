package me.desht.pneumaticcraft.common.progwidgets;

import me.desht.pneumaticcraft.client.gui.GuiProgrammer;
import me.desht.pneumaticcraft.client.gui.programmer.GuiProgWidgetAreaShow;
import me.desht.pneumaticcraft.common.ai.DroneEntityBase;
import me.desht.pneumaticcraft.common.ai.IDroneBase;
import me.desht.pneumaticcraft.common.item.ItemPlastic;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ProgWidgetEntityRightClick extends ProgWidget implements IAreaProvider, IEntityProvider {

    private EntityFilterPair entityFilters;

    @Override
    public void addErrors(List<String> curInfo, List<IProgWidget> widgets) {
        super.addErrors(curInfo, widgets);
        if (this.getConnectedParameters()[0] == null) {
            curInfo.add("gui.progWidget.area.error.noArea");
        }
        EntityFilterPair.addErrors(this, curInfo);
    }

    @Override
    public boolean hasStepInput() {
        return true;
    }

    @Override
    public Class<? extends IProgWidget> returnType() {
        return null;
    }

    @Override
    public Class<? extends IProgWidget>[] getParameters() {
        return new Class[]{ProgWidgetArea.class, ProgWidgetString.class};
    }

    @Override
    public String getWidgetString() {
        return "entityRightClick";
    }

    @Override
    public int getCraftingColorIndex() {
        return ItemPlastic.YELLOW;
    }

    @Override
    public WidgetDifficulty getDifficulty() {
        return WidgetDifficulty.EASY;
    }

    @Override
    public ResourceLocation getTexture() {
        return Textures.PROG_WIDGET_ENTITY_RIGHT_CLICK;
    }

    @Override
    public EntityAIBase getWidgetAI(IDroneBase drone, IProgWidget widget) {
        return new DroneEntityBase<IProgWidget, EntityLivingBase>(drone, widget) {
            private final List<Entity> visitedEntities = new ArrayList<>();

            @Override
            protected boolean isEntityValid(Entity entity) {
                return entity instanceof EntityLivingBase && !this.visitedEntities.contains(entity);
            }

            @Override
            protected boolean doAction() {
                this.visitedEntities.add(this.targetedEntity);
                boolean activated = false;
                ItemStack stack = this.drone.getInv().getStackInSlot(0);
                if (stack.getItem().itemInteractionForEntity(stack, this.drone.getFakePlayer(), this.targetedEntity, EnumHand.MAIN_HAND)) {
                    activated = true;
                }
                if (!activated && this.targetedEntity instanceof EntityAgeable && ((EntityAgeable) this.targetedEntity).processInteract(this.drone.getFakePlayer(), EnumHand.MAIN_HAND)) {
                    activated = true;
                }
                return false;//return activated; <-- will right click as long as it's sucessfully activated.
            }

        };
    }

    @Override
    public List<Entity> getValidEntities(World world) {
        if (this.entityFilters == null) {
            this.entityFilters = new EntityFilterPair(this);
        }
        return this.entityFilters.getValidEntities(world);
    }

    @Override
    public boolean isEntityValid(Entity entity) {
        if (this.entityFilters == null) {
            this.entityFilters = new EntityFilterPair(this);
        }
        return this.entityFilters.isEntityValid(entity);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiScreen getOptionWindow(GuiProgrammer guiProgrammer) {
        return new GuiProgWidgetAreaShow(this, guiProgrammer);
    }

    @Override
    public void getArea(Set<BlockPos> area) {
        ProgWidgetEntityAttack.getArea(area, (ProgWidgetArea) this.getConnectedParameters()[0], (ProgWidgetArea) this.getConnectedParameters()[2]);
    }
}
