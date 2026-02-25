package me.desht.pneumaticcraft.common.tileentity;

import me.desht.pneumaticcraft.common.block.BlockElevatorCaller;
import me.desht.pneumaticcraft.common.network.DescSynced;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;

public class TileEntityElevatorCaller extends TileEntityTickableBase implements ICamouflageableTE {
    private ElevatorButton[] floors = new ElevatorButton[0];
    private int thisFloor;
    private boolean emittingRedstone;
    private boolean shouldUpdateNeighbors;
    @DescSynced
    @Nonnull
    private ItemStack camoStack = ItemStack.EMPTY;
    private IBlockState camoState;

    public void setEmittingRedstone(boolean emittingRedstone) {
        if (emittingRedstone != this.emittingRedstone) {
            this.emittingRedstone = emittingRedstone;
            this.shouldUpdateNeighbors = true;
        }
    }

    @Override
    public void update() {
        super.update();
        if (this.shouldUpdateNeighbors) {
            this.updateNeighbours();
            this.shouldUpdateNeighbors = false;
        }
    }

    @Override
    public void onDescUpdate() {
        this.camoState = ICamouflageableTE.getStateForStack(this.camoStack);

        super.onDescUpdate();
    }

    public boolean getEmittingRedstone() {
        return this.emittingRedstone;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.emittingRedstone = tag.getBoolean("emittingRedstone");
        this.thisFloor = tag.getInteger("thisFloor");
        this.camoStack = ICamouflageableTE.readCamoStackFromNBT(tag);
        this.camoState = ICamouflageableTE.getStateForStack(this.camoStack);
        this.shouldUpdateNeighbors = tag.getBoolean("shouldUpdateNeighbors");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setBoolean("emittingRedstone", this.emittingRedstone);
        tag.setInteger("thisFloor", this.thisFloor);
        ICamouflageableTE.writeCamoStackToNBT(this.camoStack, tag);
        tag.setBoolean("shouldUpdateNeighbors", this.shouldUpdateNeighbors);
        return tag;
    }

    @Override
    public void readFromPacket(NBTTagCompound tag) {
        super.readFromPacket(tag);
        int floorAmount = tag.getInteger("floors");
        this.floors = new ElevatorButton[floorAmount];
        for (int i = 0; i < floorAmount; i++) {
            NBTTagCompound buttonTag = tag.getCompoundTag("floor" + i);
            this.floors[i] = new ElevatorButton();
            this.floors[i].readFromNBT(buttonTag);
        }
    }

    @Override
    public void writeToPacket(NBTTagCompound tag) {
        super.writeToPacket(tag);
        tag.setInteger("floors", this.floors.length);
        for (ElevatorButton floor : this.floors) {
            NBTTagCompound buttonTag = new NBTTagCompound();
            floor.writeToNBT(buttonTag);
            tag.setTag("floor" + floor.floorNumber, buttonTag);
        }
    }

    @Override
    public void onNeighborBlockUpdate() {
        boolean wasPowered = this.poweredRedstone > 0;
        super.onNeighborBlockUpdate();
        if (this.poweredRedstone > 0 && !wasPowered) {
            BlockElevatorCaller.setSurroundingElevators(this.getWorld(), this.getPos(), this.thisFloor);
        }
    }

    void setFloors(ElevatorButton[] floors, int thisFloorLevel) {
        this.floors = floors;
        this.thisFloor = thisFloorLevel;
        this.sendDescriptionPacket();
    }

    public ElevatorButton[] getFloors() {
        return this.floors;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(this.getPos().getX(), this.getPos().getY(), this.getPos().getZ(), this.getPos().getX() + 1, this.getPos().getY() + 1, this.getPos().getZ() + 1);
    }

    @Override
    public IBlockState getCamouflage() {
        return this.camoState;
    }

    @Override
    public void setCamouflage(IBlockState state) {
        this.camoState = state;
        this.camoStack = ICamouflageableTE.getStackForState(state);
        this.sendDescriptionPacket();
        this.markDirty();
    }

    public static class ElevatorButton {
        public double posX, posY, width, height;
        public float red, green, blue;
        public String buttonText = "";
        public int floorNumber;
        public int floorHeight;

        public ElevatorButton(double posX, double posY, double width, double height, int floorNumber, int floorHeight) {
            this.posX = posX;
            this.posY = posY;
            this.width = width;
            this.height = height;
            this.floorNumber = floorNumber;
            this.floorHeight = floorHeight;
            this.buttonText = floorNumber + 1 + "";
        }

        public ElevatorButton() {
        }

        public void setColor(float red, float green, float blue) {
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        public void writeToNBT(NBTTagCompound tag) {
            tag.setDouble("posX", this.posX);
            tag.setDouble("posY", this.posY);
            tag.setDouble("width", this.width);
            tag.setDouble("height", this.height);
            tag.setString("buttonText", this.buttonText);
            tag.setInteger("floorNumber", this.floorNumber);
            tag.setInteger("floorHeight", this.floorHeight);
            tag.setFloat("red", this.red);
            tag.setFloat("green", this.green);
            tag.setFloat("blue", this.blue);
        }

        public void readFromNBT(NBTTagCompound tag) {
            this.posX = tag.getDouble("posX");
            this.posY = tag.getDouble("posY");
            this.width = tag.getDouble("width");
            this.height = tag.getDouble("height");
            this.buttonText = tag.getString("buttonText");
            this.floorNumber = tag.getInteger("floorNumber");
            this.floorHeight = tag.getInteger("floorHeight");
            this.red = tag.getFloat("red");
            this.green = tag.getFloat("green");
            this.blue = tag.getFloat("blue");
        }
    }
}
