package me.desht.pneumaticcraft.common.entity.living;

import me.desht.pneumaticcraft.common.item.Itemss;
import me.desht.pneumaticcraft.common.progwidgets.IBlockOrdered.EnumOrder;
import me.desht.pneumaticcraft.common.progwidgets.*;
import me.desht.pneumaticcraft.common.util.DroneProgramBuilder;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;

import java.util.List;

public class EntityHarvestingDrone extends EntityBasicDrone {

    public EntityHarvestingDrone(World world) {
        super(world);
    }

    public EntityHarvestingDrone(World world, EntityPlayer player) {
        super(world, player);
    }

    @Override
    protected Item getDroneItem() {
        return Itemss.HARVESTING_DRONE;
    }

    @Override
    public void addProgram(BlockPos clickPos, EnumFacing facing, BlockPos pos, List<IProgWidget> widgets) {
        TileEntity te = this.world.getTileEntity(clickPos);
        ProgWidgetHarvest harvestPiece = new ProgWidgetHarvest();
        harvestPiece.setRequiresTool(te != null && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null));
        harvestPiece.setOrder(EnumOrder.HIGH_TO_LOW);

        DroneProgramBuilder builder = new DroneProgramBuilder();
        builder.add(new ProgWidgetStart());
        builder.add(new ProgWidgetInventoryImport(), ProgWidgetArea.fromPosition(clickPos)); //No filter, because we cannot guarantee we won't filter away modded hoes...
        builder.add(harvestPiece, ProgWidgetArea.fromPosAndExpansions(clickPos, 16, 16, 16));
        builder.add(new ProgWidgetWait(), ProgWidgetString.withText("10s")); //Wait 10 seconds for performance reasons.
        widgets.addAll(builder.build());
    }

}
