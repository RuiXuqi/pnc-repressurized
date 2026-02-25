package me.desht.pneumaticcraft.common.tileentity;

import me.desht.pneumaticcraft.api.PneumaticRegistry;
import me.desht.pneumaticcraft.api.tileentity.IAirHandler;
import me.desht.pneumaticcraft.common.block.tubes.IPneumaticPosProvider;
import me.desht.pneumaticcraft.common.network.GuiSynced;
import me.desht.pneumaticcraft.common.thirdparty.computercraft.LuaConstant;
import me.desht.pneumaticcraft.common.thirdparty.computercraft.LuaMethod;
import me.desht.pneumaticcraft.common.thirdparty.computercraft.LuaMethodRegistry;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TileEntityPneumaticBase extends TileEntityTickableBase implements IPneumaticPosProvider {
    @GuiSynced
    final IAirHandler airHandler;
    public final float dangerPressure, criticalPressure;
    private final int defaultVolume;

    public TileEntityPneumaticBase(float dangerPressure, float criticalPressure, int volume, int upgradeSlots) {
        super(upgradeSlots);
        this.airHandler = PneumaticRegistry.getInstance().getAirHandlerSupplier().createAirHandler(dangerPressure, criticalPressure, volume);
        for (Item upgrade : this.airHandler.getApplicableUpgrades()) {
            this.addApplicableUpgrade(upgrade);
        }

        this.dangerPressure = dangerPressure;
        this.criticalPressure = criticalPressure;
        this.defaultVolume = volume;
    }

    @Override
    public void update() {
        super.update();
        this.airHandler.update();
    }

    @Override
    public void validate() {
        super.validate();
        this.airHandler.validate(this);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        this.airHandler.writeToNBT(tag);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.airHandler.readFromNBT(tag);
    }

    @Override
    public void onNeighborTileUpdate() {
        super.onNeighborTileUpdate();
        this.airHandler.onNeighborChange();
    }

    @Override
    public void onNeighborBlockUpdate() {
        super.onNeighborBlockUpdate();
        this.airHandler.onNeighborChange();
    }

    @Override
    public void writeToPacket(NBTTagCompound tag) {
        super.writeToPacket(tag);
        this.airHandler.writeToNBT(tag);
    }

    @Override
    public void readFromPacket(NBTTagCompound tag) {
        super.readFromPacket(tag);
        this.airHandler.readFromNBT(tag);
    }

    @Override
    protected void addLuaMethods(LuaMethodRegistry registry) {
        super.addLuaMethods(registry);
        registry.registerLuaMethod(new LuaMethod("getPressure") {
            @Override
            public Object[] call(Object[] args) {
                this.requireArgs(args, 0, 1, "face (down/up/north/south/west/east)");
                if (args.length == 0) {
                    return new Object[]{TileEntityPneumaticBase.this.airHandler.getPressure()};
                } else {
                    IAirHandler handler = TileEntityPneumaticBase.this.getAirHandler(this.getDirForString((String) args[0]));
                    return new Object[]{handler != null ? handler.getPressure() : 0};
                }
            }
        });

        if (this instanceof IMinWorkingPressure) {
            final IMinWorkingPressure mwp = (IMinWorkingPressure) this;
            registry.registerLuaMethod(new LuaMethod("getMinWorkingPressure") {
                @Override
                public Object[] call(Object[] args) {
                    this.requireNoArgs(args);
                    return new Object[]{mwp.getMinWorkingPressure()};
                }
            });
        }

        registry.registerLuaMethod(new LuaConstant("getDangerPressure", this.dangerPressure));
        registry.registerLuaMethod(new LuaConstant("getCriticalPressure", this.criticalPressure));
        registry.registerLuaMethod(new LuaConstant("getDefaultVolume", this.defaultVolume));
    }

    /*
     * End ComputerCraft API
     */

    @Override
    public World world() {
        return this.getWorld();
    }

    @Override
    public BlockPos pos() {
        return this.getPos();
    }

    @Override
    public IAirHandler getAirHandler(EnumFacing side) {
        return side == null || this.isConnectedTo(side) ? this.airHandler : null;
    }

    public float getPressure() {
        return this.getAirHandler(null).getPressure();
    }

    public void addAir(int air) {
        this.getAirHandler(null).addAir(air);
    }

    /**
     * Checks if the given side of this TE can be pneumatically connected to.
     *
     * @param side the side to check
     * @return true if connected, false otherwise
     */
    public boolean isConnectedTo(EnumFacing side) {
        return true;
    }

    public int getDefaultVolume() {
        return this.defaultVolume;
    }
}
