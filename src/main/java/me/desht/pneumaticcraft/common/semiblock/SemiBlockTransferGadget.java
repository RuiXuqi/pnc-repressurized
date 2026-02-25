package me.desht.pneumaticcraft.common.semiblock;

import me.desht.pneumaticcraft.common.network.DescSynced;
import me.desht.pneumaticcraft.common.util.IOHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public class SemiBlockTransferGadget extends SemiBlockBasic<TileEntity> implements IDirectionalSemiblock {
    private static final int TRANSFER_INTERVAL = 40;
    public static final String ID = "transfer_gadget";

    public enum EnumInputOutput {
        INPUT,
        OUTPUT
    }

    @DescSynced
    private EnumInputOutput io;
    @DescSynced
    private EnumFacing facing;
    private SemiBlockTransferGadget connectedGadget;

    private int counter;

    public SemiBlockTransferGadget() {
        super(TileEntity.class);
    }

    @Override
    public boolean canStay() {
        return super.canStay() && this.getConnectedGadget() != null && (!this.isAir() || !this.getConnectedGadget().isAir());
    }

    @Override
    public void addDrops(NonNullList<ItemStack> drops) {
        // Only drop one of the two halves.
        if (this.io == EnumInputOutput.INPUT) super.addDrops(drops);
    }

    @Override
    public void prePlacement(EntityPlayer player, ItemStack stack, EnumFacing facing) {
        super.prePlacement(player, stack, facing);
        this.io = EnumInputOutput.INPUT;
        this.facing = facing;
    }

    @Override
    public void onPlaced(EntityPlayer player, ItemStack stack, EnumFacing facing) {
        super.onPlaced(player, stack, facing);

        this.connectedGadget = new SemiBlockTransferGadget();
        this.connectedGadget.facing = this.facing.getOpposite();
        this.connectedGadget.io = EnumInputOutput.OUTPUT;
        SemiBlockManager.getInstance(this.world).addSemiBlock(this.world, this.pos.offset(this.facing), this.connectedGadget);
    }

    @Override
    public boolean onRightClickWithConfigurator(EntityPlayer player, EnumFacing side) {
        if (this.getFacing() == side) {
            this.toggleIO();
            this.getConnectedGadget().toggleIO();
            return true;
        } else {
            return super.onRightClickWithConfigurator(player, side);
        }
    }

    private void toggleIO() {
        this.io = (this.io == EnumInputOutput.INPUT ? EnumInputOutput.OUTPUT : EnumInputOutput.INPUT);
    }

    @Override
    public boolean canCoexistInSameBlock(ISemiBlock semiBlock) {
        return semiBlock instanceof SemiBlockTransferGadget && ((SemiBlockTransferGadget) semiBlock).facing != this.facing;
    }

    private SemiBlockTransferGadget getConnectedGadget() {
        if (this.connectedGadget == null || this.connectedGadget.isInvalid()) {
            this.connectedGadget = SemiBlockManager.getInstance(this.world).getSemiBlocks(SemiBlockTransferGadget.class, this.world, this.pos.offset(this.facing))
                    .filter(gadget -> gadget.facing == this.facing.getOpposite())
                    .findFirst().orElse(null);
        }
        return this.connectedGadget;
    }

    @Override
    public void update() {
        super.update();

        if (!this.world.isRemote && !this.isInvalid() && this.io == EnumInputOutput.INPUT && ++this.counter >= TRANSFER_INTERVAL) {
            this.counter = 0;
            this.transfer();
        }
    }

    private void transfer() {
        TileEntity inputTE = this.getTileEntity();
        TileEntity outputTE = this.getConnectedGadget().getTileEntity();
        if (inputTE == null || outputTE == null) return;
        this.tryTransferItem(inputTE, outputTE);
        this.tryTransferFluid(inputTE, outputTE);
    }

    private void tryTransferItem(TileEntity inputTE, TileEntity outputTE) {
        IItemHandler input = inputTE.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, this.facing);
        IItemHandler output = outputTE.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, this.facing.getOpposite());
        IOHelper.transferOneItem(input, output);
    }

    private void tryTransferFluid(TileEntity inputTE, TileEntity outputTE) {
        IFluidHandler input = inputTE.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, this.facing);
        IFluidHandler output = outputTE.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, this.facing.getOpposite());
        if (input != null && output != null) {
            FluidUtil.tryFluidTransfer(output, input, 100, true);
        }
    }

    @Override
    public EnumFacing getFacing() {
        return this.facing;
    }

    public EnumInputOutput getInputOutput() {
        return this.io;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("counter", this.counter);
        tag.setByte("facing", (byte) this.facing.ordinal());
        tag.setBoolean("input", this.io == EnumInputOutput.INPUT);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.counter = tag.getInteger("counter");
        this.facing = EnumFacing.VALUES[tag.getByte("facing")];
        this.io = tag.getBoolean("input") ? EnumInputOutput.INPUT : EnumInputOutput.OUTPUT;
    }
}
