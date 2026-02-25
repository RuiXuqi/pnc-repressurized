package me.desht.pneumaticcraft.common.ai;

import me.desht.pneumaticcraft.api.item.IItemRegistry.EnumUpgrade;
import me.desht.pneumaticcraft.common.entity.living.EntityDrone;
import me.desht.pneumaticcraft.common.tileentity.TileEntityChargingStation;
import me.desht.pneumaticcraft.common.tileentity.TileEntitySecurityStation;
import me.desht.pneumaticcraft.common.util.GlobalTileEntityCacheManager;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DroneGoToChargingStation extends EntityAIBase {
    private final EntityDrone drone;
    private TileEntityChargingStation curCharger;
    private int chargingTime;
    public boolean isExecuting;

    public DroneGoToChargingStation(EntityDrone drone) {
        this.drone = drone;
        this.setMutexBits(63);//binary 111111, so it won't run along with other AI tasks.
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    @Override
    public boolean shouldExecute() {
        List<TileEntityChargingStation> validChargingStations = new ArrayList<>();
        if (this.drone.getPressure(null) < PneumaticValues.DRONE_LOW_PRESSURE) {
            for (TileEntityChargingStation station : GlobalTileEntityCacheManager.getInstance().chargingStations) {
                if (station.getWorld() == this.drone.world) {
                    BlockPos pos = new BlockPos(station.getPos().getX(), station.getPos().getY(), station.getPos().getZ());
                    if (DroneClaimManager.getInstance(this.drone.world).isClaimed(pos)) {
                        this.drone.addDebugEntry("gui.progWidget.chargingStation.debug.claimed", pos);
                    } else if (station.getPressure() <= PneumaticValues.DRONE_LOW_PRESSURE) {
                        this.drone.addDebugEntry("gui.progWidget.chargingStation.debug.notEnoughPressure", pos);
                    } else if (station.getUpgrades(EnumUpgrade.DISPENSER) == 0) {
                        this.drone.addDebugEntry("gui.progWidget.chargingStation.debug.noDispenserUpgrades", pos);
                    } else {
                        validChargingStations.add(station);
                    }
                }
            }
        }

        validChargingStations.sort(Comparator.comparingDouble(arg -> PneumaticCraftUtils.distBetweenSq(arg.getPos().getX(), arg.getPos().getY(), arg.getPos().getZ(), this.drone.posX, this.drone.posY, this.drone.posZ)));

        for (TileEntityChargingStation station : validChargingStations) {
            boolean protect = TileEntitySecurityStation.getProtectingSecurityStations(this.drone.world, station.getPos(), this.drone.getFakePlayer(), false, false) > 0;
            BlockPos pos = new BlockPos(station.getPos());
            if (protect) {
                this.drone.addDebugEntry("gui.progWidget.chargingStation.debug.protected", pos);
            } else if (this.drone.getPathNavigator().moveToXYZ(station.getPos().getX(), station.getPos().getY() + 1, station.getPos().getZ()) || this.drone.getPathNavigator().isGoingToTeleport()) {
                this.isExecuting = true;
                this.curCharger = station;
                DroneClaimManager.getInstance(this.drone.world).claim(pos);
                return true;
            } else {
                this.drone.addDebugEntry("gui.progWidget.chargingStation.debug.cantNavigate", pos);
            }
        }
        this.isExecuting = false;
        return false;
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    @Override
    public boolean shouldContinueExecuting() {
        if (this.curCharger.getUpgrades(EnumUpgrade.DISPENSER) == 0 || this.curCharger.isInvalid()) {
            // Our path was blocked.
            this.isExecuting = false;
            return false;
        } else if (!this.drone.getPathNavigator().isGoingToTeleport() && (this.drone.getNavigator().getPath() == null || this.drone.getNavigator().getPath().isFinished())) {
            this.isExecuting = this.drone.getPressure(null) < 9.9F && this.curCharger.getPressure() > this.drone.getPressure(null) + 0.1F;
            if (this.isExecuting) {
                this.chargingTime++;
                if (this.chargingTime > 20) {
                    this.drone.getPathNavigator().moveToXYZ(this.curCharger.getPos().getX(), this.curCharger.getPos().getY() + 1.5, this.curCharger.getPos().getZ());
                    if (this.drone.getNavigator().getPath() == null || this.drone.getNavigator().getPath().isFinished()) {
                        this.drone.setStandby(true);
                    } else {
                        this.chargingTime = 0;
                    }
                }
                DroneClaimManager.getInstance(this.drone.world).claim(new BlockPos(this.curCharger.getPos().getX(), this.curCharger.getPos().getY(), this.curCharger.getPos().getZ()));
            }
            return this.isExecuting;
        } else {
            this.chargingTime = 0;
            DroneClaimManager.getInstance(this.drone.world).claim(new BlockPos(this.curCharger.getPos().getX(), this.curCharger.getPos().getY(), this.curCharger.getPos().getZ()));
            return this.drone.isAccelerating();
        }
    }
}
