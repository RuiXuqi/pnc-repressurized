package me.desht.pneumaticcraft.common.tileentity;

import me.desht.pneumaticcraft.api.tileentity.IAirHandler;
import me.desht.pneumaticcraft.api.tileentity.IAirListener;
import me.desht.pneumaticcraft.api.tileentity.IManoMeasurable;
import me.desht.pneumaticcraft.common.block.tubes.IInfluenceDispersing;
import me.desht.pneumaticcraft.common.block.tubes.ModuleRegistrator;
import me.desht.pneumaticcraft.common.block.tubes.TubeModule;
import me.desht.pneumaticcraft.common.network.DescSynced;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;

public class TileEntityPressureTube extends TileEntityPneumaticBase implements IAirListener, IManoMeasurable, ICamouflageableTE {
    @DescSynced
    public final boolean[] sidesConnected = new boolean[6];
    @DescSynced
    public final boolean[] sidesClosed = new boolean[6];
    public TubeModule[] modules = new TubeModule[6];
    @DescSynced
    private ItemStack camoStack = ItemStack.EMPTY;
    private IBlockState camoState;
    private AxisAlignedBB renderBoundingBox = null;

    public TileEntityPressureTube() {
        super(PneumaticValues.DANGER_PRESSURE_PRESSURE_TUBE, PneumaticValues.MAX_PRESSURE_PRESSURE_TUBE, PneumaticValues.VOLUME_PRESSURE_TUBE, 0);
    }

    public TileEntityPressureTube(float dangerPressurePressureTube, float maxPressurePressureTube, int volumePressureTube) {
        super(dangerPressurePressureTube, maxPressurePressureTube, volumePressureTube, 0);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        if (nbt.hasKey("sidesConnected")) {
            // new-style: far more compact storage
            byte connected = nbt.getByte("sidesConnected");
            byte closed = nbt.getByte("sidesClosed");
            for (int i = 0; i < 6; i++) {
                this.sidesConnected[i] = ((connected & 1 << i) != 0);
                this.sidesClosed[i] = ((closed & 1 << i) != 0);
            }
        } else {
            // old-style
            for (int i = 0; i < 6; i++) {
                this.sidesConnected[i] = nbt.getBoolean("sideConnected" + i);
                this.sidesClosed[i] = nbt.getBoolean("sideClosed" + i);
            }
        }
        this.camoStack = ICamouflageableTE.readCamoStackFromNBT(nbt);
        this.camoState = ICamouflageableTE.getStateForStack(this.camoStack);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        byte connected = 0, closed = 0;
        for (int i = 0; i < 6; i++) {
            if (this.sidesConnected[i]) connected |= 1 << i;
            if (this.sidesClosed[i]) closed |= 1 << i;
        }
        nbt.setByte("sidesConnected", connected);
        nbt.setByte("sidesClosed", closed);
        ICamouflageableTE.writeCamoStackToNBT(this.camoStack, nbt);
        return nbt;
    }

    @Override
    public void writeToPacket(NBTTagCompound tag) {
        super.writeToPacket(tag);
        this.writeModulesToNBT(tag);
    }

    public void writeModulesToNBT(NBTTagCompound tag) {
        NBTTagList moduleList = new NBTTagList();
        for (int i = 0; i < this.modules.length; i++) {
            if (this.modules[i] != null) {
                NBTTagCompound moduleTag = new NBTTagCompound();
                moduleTag.setString("type", this.modules[i].getType());
                this.modules[i].writeToNBT(moduleTag);
                moduleTag.setInteger("side", i);
                moduleList.appendTag(moduleTag);
            }
        }
        tag.setTag("modules", moduleList);
    }

    @Override
    public void readFromPacket(NBTTagCompound tag) {
        super.readFromPacket(tag);
        this.modules = new TubeModule[6];
        NBTTagList moduleList = tag.getTagList("modules", 10);
        for (int i = 0; i < moduleList.tagCount(); i++) {
            NBTTagCompound moduleTag = moduleList.getCompoundTagAt(i);
            TubeModule module = ModuleRegistrator.getModule(moduleTag.getString("type"));
            if (module != null) {
                module.readFromNBT(moduleTag);
                this.setModule(module, EnumFacing.byIndex(moduleTag.getInteger("side")));
            }
        }
        this.updateRenderBoundingBox();
        if (this.hasWorld() && this.getWorld().isRemote) {
            this.rerenderTileEntity();
        }
    }

    private void updateRenderBoundingBox() {
        this.renderBoundingBox = new AxisAlignedBB(this.getPos().getX(), this.getPos().getY(), this.getPos().getZ(), this.getPos().getX() + 1, this.getPos().getY() + 1, this.getPos().getZ() + 1);

        for (int i = 0; i < 6; i++) {
            if (this.modules[i] != null && this.modules[i].getRenderBoundingBox() != null) {
                this.renderBoundingBox = this.renderBoundingBox.union(this.modules[i].getRenderBoundingBox());
            }
        }
    }

    @Override
    public void update() {
        super.update();

        boolean hasModules = false;
        for (TubeModule module : this.modules) {
            if (module != null) {
                hasModules = true;
                module.shouldDrop = true;
                module.update();
            }
        }

        List<Pair<EnumFacing, IAirHandler>> teList = this.getAirHandler(null).getConnectedPneumatics();

        if (!hasModules && teList.size() == 1 && !this.getWorld().isRemote) {
            for (Pair<EnumFacing, IAirHandler> entry : teList) {
                if (entry.getKey() != null && this.modules[entry.getKey().getOpposite().ordinal()] == null && this.isConnectedTo(entry.getKey().getOpposite()))
                    this.getAirHandler(null).airLeak(entry.getKey().getOpposite());
            }
        }
    }

    @Override
    public void onAirDispersion(IAirHandler handler, EnumFacing side, int amount) {
        if (side != null) {
            int intSide = side.ordinal();
            if (this.modules[intSide] instanceof IInfluenceDispersing) {
                ((IInfluenceDispersing) this.modules[intSide]).onAirDispersion(amount);
            }
        }
    }

    @Override
    public int getMaxDispersion(IAirHandler handler, EnumFacing side) {
        if (side != null) {
            int intSide = side.ordinal();
            if (this.modules[intSide] instanceof IInfluenceDispersing) {
                return ((IInfluenceDispersing) this.modules[intSide]).getMaxDispersion();
            }
        }
        return Integer.MAX_VALUE;
    }

    @Override
    public void addConnectedPneumatics(List<Pair<EnumFacing, IAirHandler>> pneumatics) {
    }

    public void setModule(TubeModule module, EnumFacing side) {
        if (module != null) {
            module.setDirection(side);
            module.setTube(this);
        }
        this.modules[side.ordinal()] = module;
        if (this.getWorld() != null && !this.getWorld().isRemote) {
            this.sendDescriptionPacket();
        }
        this.markDirty();
    }

    @Override
    public boolean isConnectedTo(EnumFacing side) {
        return !this.sidesClosed[side.ordinal()]
                && (this.modules[side.ordinal()] == null || this.modules[side.ordinal()].isInline());
    }

    @Override
    public void onNeighborTileUpdate() {
        super.onNeighborTileUpdate();
        this.updateConnections();
        for (TubeModule module : this.modules) {
            if (module != null) module.onNeighborTileUpdate();
        }
    }

    @Override
    public void onNeighborBlockUpdate() {
        super.onNeighborBlockUpdate();
        this.updateConnections();
        for (TubeModule module : this.modules) {
            if (module != null) module.onNeighborBlockUpdate();
        }
    }

    private void updateConnections() {
        List<Pair<EnumFacing, IAirHandler>> connections = this.getAirHandler(null).getConnectedPneumatics();
        Arrays.fill(this.sidesConnected, false);
        for (Pair<EnumFacing, IAirHandler> entry : connections) {
            this.sidesConnected[entry.getKey().ordinal()] = true;
        }

        boolean hasModule = false;
        for (int i = 0; i < 6; i++) {
            if (this.modules[i] != null) {
                hasModule = true;
                break;
            }
        }

        int sidesCount = 0;
        for (boolean bool : this.sidesConnected) {
            if (bool) sidesCount++;
        }
        if (sidesCount == 1 && !hasModule) {
            for (int i = 0; i < 6; i++) {
                if (this.sidesConnected[i]) {
                    EnumFacing opposite = EnumFacing.byIndex(i).getOpposite();
                    if (this.isConnectedTo(opposite)) this.sidesConnected[opposite.ordinal()] = true;
                    break;
                }
            }
        }
        for (int i = 0; i < 6; i++) {
            if (this.modules[i] != null && this.modules[i].isInline()) this.sidesConnected[i] = false;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return this.renderBoundingBox != null ? this.renderBoundingBox : new AxisAlignedBB(this.getPos());
    }

    @Override
    public void printManometerMessage(EntityPlayer player, List<String> text) {
        RayTraceResult mop = PneumaticCraftUtils.getEntityLookedObject(player);
        if (mop != null && mop.hitInfo instanceof EnumFacing) {
            EnumFacing dir = (EnumFacing) mop.hitInfo;
            if (this.modules[dir.ordinal()] != null) {
                this.modules[dir.ordinal()].addInfo(text);
            }
        }
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

    @Override
    public void onDescUpdate() {
        this.camoState = ICamouflageableTE.getStateForStack(this.camoStack);

        super.onDescUpdate();
    }

    public static TileEntityPressureTube getTube(TileEntity te) {
        return te instanceof TileEntityPressureTube ? (TileEntityPressureTube) te : null;
    }
}
