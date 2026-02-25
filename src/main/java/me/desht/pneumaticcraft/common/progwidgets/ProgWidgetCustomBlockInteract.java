package me.desht.pneumaticcraft.common.progwidgets;

import me.desht.pneumaticcraft.api.drone.ICustomBlockInteract;
import me.desht.pneumaticcraft.common.ai.DroneAICustomBlockInteract;
import me.desht.pneumaticcraft.common.ai.IDroneBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.ResourceLocation;

public class ProgWidgetCustomBlockInteract extends ProgWidgetInventoryBase {

    private ICustomBlockInteract interactor;

    public ProgWidgetCustomBlockInteract setInteractor(ICustomBlockInteract interactor) {
        this.interactor = interactor;
        return this;
    }

    @Override
    public IProgWidget copy() {
        ProgWidgetCustomBlockInteract widget = (ProgWidgetCustomBlockInteract) super.copy();
        widget.setInteractor(this.interactor);
        return widget;
    }

    @Override
    public String getWidgetString() {
        return this.interactor.getName();
    }

    @Override
    public ResourceLocation getTexture() {
        return this.interactor.getTexture();
    }

    @Override
    public EntityAIBase getWidgetAI(IDroneBase drone, IProgWidget widget) {
        return new DroneAICustomBlockInteract(drone, (ProgWidgetAreaItemBase) widget, this.interactor);
    }

    @Override
    public Class<? extends IProgWidget>[] getParameters() {
        return new Class[]{ProgWidgetArea.class};
    }

    @Override
    public int getCraftingColorIndex() {
        return this.interactor.getCraftingColorIndex();
    }

}
