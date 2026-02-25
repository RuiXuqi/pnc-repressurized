package me.desht.pneumaticcraft.common.pressure;

import me.desht.pneumaticcraft.PneumaticCraftRepressurized;
import me.desht.pneumaticcraft.api.item.IItemRegistry.EnumUpgrade;
import me.desht.pneumaticcraft.api.tileentity.IAirHandler;
import me.desht.pneumaticcraft.api.tileentity.IAirListener;
import me.desht.pneumaticcraft.api.tileentity.IPneumaticMachine;
import me.desht.pneumaticcraft.common.item.Itemss;
import me.desht.pneumaticcraft.common.network.GuiSynced;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketPlaySound;
import me.desht.pneumaticcraft.common.network.PacketSpawnParticle;
import me.desht.pneumaticcraft.common.tileentity.TileEntityBase;
import me.desht.pneumaticcraft.common.tileentity.TileEntityTickableBase;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.common.util.TileEntityCache;
import me.desht.pneumaticcraft.lib.EnumCustomParticleType;
import me.desht.pneumaticcraft.lib.Log;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import me.desht.pneumaticcraft.lib.Sounds;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.*;

public class AirHandler implements IAirHandler {
    private float maxPressure;
    @GuiSynced
    private int volume;
    private int defaultVolume;
    private final float dangerPressure;
    private final float criticalPressure;
    @GuiSynced
    private int air; //Pressure = air / volume
    private int soundCounter;
    private final Set<IAirHandler> specialConnectedHandlers = new HashSet<>();
    private TileEntityCache[] tileCache;

    private TileEntityTickableBase.UpgradeCache upgradeCache;
    private IAirListener airListener;
    private IPneumaticMachine parentPneumatic;
    private World world;
    private BlockPos pos;

    AirHandler(float dangerPressure, float criticalPressure, int volume) {
        Validate.isTrue(volume > 0, "Volume can't be lower than or equal to 0!");
        this.dangerPressure = dangerPressure;
        this.criticalPressure = criticalPressure;
        this.maxPressure = dangerPressure + (criticalPressure - dangerPressure) * (float) Math.random();
        this.volume = volume;
        this.defaultVolume = volume;
    }

    public TileEntityCache[] getTileCache() {
        if (this.tileCache == null) this.tileCache = TileEntityCache.getDefaultCache(this.getWorld(), this.getPos());
        return this.tileCache;
    }

    @Override
    public void createConnection(@Nonnull IAirHandler otherHandler) {
        if (this.specialConnectedHandlers.add(otherHandler)) {
            otherHandler.createConnection(this);
        }
    }

    @Override
    public void removeConnection(@Nonnull IAirHandler otherHandler) {
        if (this.specialConnectedHandlers.remove(otherHandler)) {
            otherHandler.removeConnection(this);
        }
    }

    @Override
    public void printManometerMessage(EntityPlayer player, List<String> curInfo) {
        curInfo.add(TextFormatting.GREEN + "Current pressure: " + PneumaticCraftUtils.roundNumberTo(this.getPressure(), 1) + " bar.");
    }

    @Override
    public void update() {
        if (!this.getWorld().isRemote) {
            this.updateVolume();

            if (this.getUpgrades(EnumUpgrade.SECURITY) > 0) {
                this.doSecurityAirChecks();
            }

            if (this.getPressure() > this.maxPressure) {
                this.getWorld().createExplosion(null, this.getPos().getX() + 0.5D, this.getPos().getY() + 0.5D, this.getPos().getZ() + 0.5D, 1.0F, true);
                this.getWorld().setBlockToAir(this.getPos());
            } else {
                this.disperseAir();
            }
        }

        if (this.soundCounter > 0) this.soundCounter--;
    }

    private void updateVolume() {
        this.setVolume(this.defaultVolume + this.getVolumeFromUpgrades());
    }

    private void doSecurityAirChecks() {
        if (this.getPressure() >= this.dangerPressure - 0.1) {
            this.airLeak(EnumFacing.UP);
        }

        // Remove any remaining air
        int excessAir = this.getAir() - (int) (this.getVolume() * (this.dangerPressure - 0.1));
        if (excessAir > 0) {
            this.addAir(-excessAir);
            this.onAirDispersion(null, -excessAir);
        }
    }

    /**
     * Sets the volume of this TE's air tank. When the volume decreases the pressure will remain the same, meaning air will
     * be lost. When the volume increases, the air remains the same meaning the pressure will drop.
     * Used in the Volume Upgrade calculations.
     *
     * @param newVolume the new volume
     */
    public void setVolume(int newVolume) {
        Validate.isTrue(newVolume > 0, "Volume can't be lower or equal than 0!");

        if (newVolume < this.volume) {
            this.air = (int) (this.air * (float) newVolume / this.volume); // lose air when we decrease in volume.
        }
        this.volume = newVolume;
    }

    private void onAirDispersion(EnumFacing dir, int airAdded) {
        if (this.airListener != null) this.airListener.onAirDispersion(this, dir, airAdded);
    }

    private int getMaxDispersion(EnumFacing dir) {
        return this.airListener != null ? this.airListener.getMaxDispersion(this, dir) : Integer.MAX_VALUE;
    }

    private int getUpgrades(EnumUpgrade upgrade) {
        return this.upgradeCache == null ? 0 : this.upgradeCache.getUpgrades(upgrade);
    }

    private int getVolumeFromUpgrades() {
        return this.getUpgrades(EnumUpgrade.VOLUME) * PneumaticValues.VOLUME_VOLUME_UPGRADE;
    }

    /**
     * Method invoked every update tick which is used to handle air dispersion. It retrieves the pneumatics connecting
     * with this TE, and pushes air to those with a lower pressure than this one.
     */
    private void disperseAir() {
        if (this.getWorld().isRemote) return;
        this.disperseAir(this.getConnectedPneumatics());
    }

    private void disperseAir(List<Pair<EnumFacing, IAirHandler>> teList) {

        boolean shouldRepeat;
        List<Pair<Integer, Integer>> dispersion = new ArrayList<>();
        do {
            shouldRepeat = false;
            //Add up every volume and air.
            int totalVolume = this.getVolume();
            int totalAir = this.air;
            for (Pair<EnumFacing, IAirHandler> entry : teList) {
                IAirHandler airHandler = entry.getValue();
                totalVolume += airHandler.getVolume();
                totalAir += airHandler.getAir();
            }
            //Only go push based, ignore any machines that have a higher pressure than this block.
            Iterator<Pair<EnumFacing, IAirHandler>> iterator = teList.iterator();
            while (iterator.hasNext()) {
                Pair<EnumFacing, IAirHandler> entry = iterator.next();
                IAirHandler airHandler = entry.getValue();
                int totalMachineAir = (int) ((long) totalAir * airHandler.getVolume() / totalVolume);//Calculate the total air the machine is going to get.
                int airDispersed = totalMachineAir - airHandler.getAir();
                if (airDispersed < 0) {
                    iterator.remove();
                    shouldRepeat = true;
                    dispersion.clear();
                    break;
                } else {
                    dispersion.add(new MutablePair<>(this.getMaxDispersion(entry.getKey()), airDispersed));
                }
            }
        } while (shouldRepeat);

        int toBeDivided = 0;
        int receivers = dispersion.size();
        for (Pair<Integer, Integer> disp : dispersion) {
            if (disp.getValue() > disp.getKey()) {
                toBeDivided += disp.getValue() - disp.getKey();//Any air that wants to go to a neighbor, but can't (because of regulator module) gives back its air.
                disp.setValue(disp.getKey());
                receivers--;
            }
        }

        while (toBeDivided >= receivers && receivers > 0) {
            int dividedValue = toBeDivided / receivers; //try to give every receiver an equal part of the to be divided air.
            for (Pair<Integer, Integer> disp : dispersion) {
                int maxTransfer = disp.getKey() - disp.getValue();
                if (maxTransfer > 0) {
                    if (maxTransfer <= dividedValue) {
                        receivers--;//next step this receiver won't be able to receive any air.
                    }
                    int transfered = Math.min(dividedValue, maxTransfer);//cap it at the max it can have.
                    disp.setValue(disp.getValue() + transfered);
                    toBeDivided -= transfered;
                } else {
                    receivers--;
                }
            }
        }

        for (int i = 0; i < teList.size(); i++) {
            IAirHandler neighbor = teList.get(i).getValue();
            int transferedAir = dispersion.get(i).getValue();

            this.onAirDispersion(teList.get(i).getKey(), transferedAir);
            neighbor.addAir(transferedAir);
            this.addAir(-transferedAir);
        }
    }

    /**
     * Adds air to the tank of the given side of this TE.
     *
     * @param amount amount of air (in mL) to add
     */
    @Override
    public void addAir(int amount) {
        this.air = Math.max(this.air + amount, -this.volume);  // floor at -1 bar otherwise negative air is reported
    }

    @Override
    public void setDefaultVolume(int defaultVolume) {
        this.defaultVolume = defaultVolume;
    }

    @Override
    public float getPressure() {
        return (float) this.air / this.volume;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        NBTTagCompound pneumaticTag = tag.getCompoundTag("pneumatic");
        this.air = pneumaticTag.getInteger("air");
        this.maxPressure = pneumaticTag.getFloat("maxPressure");
        this.volume = pneumaticTag.getInteger("volume");
        if (this.volume == 0 && PneumaticCraftRepressurized.proxy.getClientWorld() == null) {
            // only warn about a zero volume on the server side
            Log.error("Volume was 0! Assigning default");
            this.volume = this.defaultVolume;
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        NBTTagCompound pneumaticTag = new NBTTagCompound();
        pneumaticTag.setInteger("air", this.air);
        pneumaticTag.setInteger("volume", this.volume);
        pneumaticTag.setFloat("maxPressure", this.maxPressure);
        tag.setTag("pneumatic", pneumaticTag);
    }

    @Override
    public void validate(TileEntity parent) {
        this.upgradeCache = parent instanceof TileEntityBase ? ((TileEntityBase) parent).getUpgradeCache() : null;
        this.airListener = parent instanceof IAirListener ? (IAirListener) parent : null;
        this.parentPneumatic = (IPneumaticMachine) parent;
        this.setWorld(parent.getWorld());
        this.setPos(parent.getPos());
    }

    @Override
    public void setPneumaticMachine(IPneumaticMachine machine) {
        this.parentPneumatic = machine;
    }

    @Override
    public void setAirListener(IAirListener airListener) {
        this.airListener = airListener;
    }

    @Override
    public void airLeak(EnumFacing side) {
        if (this.getWorld().isRemote || Math.abs(this.getPressure()) < 0.01F) return;
        double motionX = side.getXOffset();
        double motionY = side.getYOffset();
        double motionZ = side.getZOffset();
        if (this.soundCounter <= 0) {
            float pitch = MathHelper.clamp(1.0f + ((this.getPressure() - 3) / 10), 0.8f, 1.2f);
            this.soundCounter = (int) (20 / pitch);
            NetworkHandler.sendToAllAround(new PacketPlaySound(Sounds.LEAKING_GAS_SOUND, SoundCategory.BLOCKS, this.getPos().getX(), this.getPos().getY(), this.getPos().getZ(), 0.1F, pitch, true), this.getWorld());
        }

        if (this.getPressure() < 0) {
            double speed = this.getPressure() * 0.1F - 0.1F;
            NetworkHandler.sendToAllAround(new PacketSpawnParticle(EnumCustomParticleType.AIR_PARTICLE_DENSE, this.getPos().getX() + 0.5D + motionX / 2D, this.getPos().getY() + 0.5D + motionY / 2D, this.getPos().getZ() + 0.5D + motionZ / 2D, motionX * speed, motionY * speed, motionZ * speed), this.getWorld());
            int dispersedAmount = -(int) (this.getPressure() * PneumaticValues.AIR_LEAK_FACTOR) + 20;
            if (this.getAir() > dispersedAmount) dispersedAmount = -this.getAir();
            this.onAirDispersion(side, dispersedAmount);
            this.addAir(dispersedAmount);
        } else {
            double speed = this.getPressure() * 0.1F + 0.1F;
            NetworkHandler.sendToAllAround(new PacketSpawnParticle(EnumCustomParticleType.AIR_PARTICLE_DENSE, this.getPos().getX() + 0.5D + motionX / 2D, this.getPos().getY() + 0.5D + motionY / 2D, this.getPos().getZ() + 0.5D + motionZ / 2D, motionX * speed, motionY * speed, motionZ * speed), this.getWorld());
            int dispersedAmount = (int) (this.getPressure() * PneumaticValues.AIR_LEAK_FACTOR) + 20;
            if (dispersedAmount > this.getAir()) dispersedAmount = this.getAir();
            this.onAirDispersion(side, -dispersedAmount);
            this.addAir(-dispersedAmount);
        }
    }

    /**
     * Retrieves a list of all the connecting pneumatics. It takes sides in account.
     *
     * @return a list of face->air-handler pairs
     */
    @Override
    public List<Pair<EnumFacing, IAirHandler>> getConnectedPneumatics() {
        List<Pair<EnumFacing, IAirHandler>> teList = new ArrayList<>();
        for (IAirHandler specialConnection : this.specialConnectedHandlers) {
            teList.add(new ImmutablePair<>(null, specialConnection));
        }
        for (EnumFacing direction : EnumFacing.VALUES) {
            TileEntity te = this.getTileCache()[direction.ordinal()].getTileEntity();
            IPneumaticMachine machine = IPneumaticMachine.getMachine(te);
            if (machine != null && this.parentPneumatic.getAirHandler(direction) == this && machine.getAirHandler(direction.getOpposite()) != null) {
                teList.add(new ImmutablePair<>(direction, machine.getAirHandler(direction.getOpposite())));
            }
        }
        if (this.airListener != null) this.airListener.addConnectedPneumatics(teList);
        return teList;
    }

    @Override
    public void onNeighborChange() {
        for (TileEntityCache cache : this.getTileCache()) {
            cache.update();
        }
    }

    @Override
    public int getVolume() {
        return this.volume;
    }

    @Override
    public float getMaxPressure() {
        return this.maxPressure;
    }

    @Override
    public float getDangerPressure() {
        return this.dangerPressure;
    }

    @Override
    public float getCriticalPressure() {
        return this.criticalPressure;
    }

    @Override
    public int getAir() {
        return this.air;
    }

    /**
     * Sets the amount of air in this handler.  Currently only used for server->client sync'ing.
     *
     * @param air air amount in mL
     */
    public void setAir(int air) {
        this.air = air;
    }

    /**
     * Set the air pressure directly.  Currently only used by the Creative Compressor.
     *
     * @param pressure the pressure, in bar
     */
    public void setPressure(float pressure) {
        this.air = (int) (pressure * this.volume);
    }

    @Override
    public void setUpgradeSlots(int... upgradeSlots) {
        // does nothing - deprecated method
    }

    @Override
    public int[] getUpgradeSlots() {
        return new int[0];
    }

    @Override
    public World getWorld() {
        return this.world;
    }

    @Override
    public void setWorld(World world) {
        this.world = world;
    }

    @Override
    public BlockPos getPos() {
        return this.pos;
    }

    @Override
    public void setPos(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public Set<Item> getApplicableUpgrades() {
        Set<Item> upgrades = new HashSet<>(2);
        upgrades.add(Itemss.upgrades.get(EnumUpgrade.VOLUME));
        upgrades.add(Itemss.upgrades.get(EnumUpgrade.SECURITY));
        return upgrades;
    }

    @Override
    public String getName() {
        throw new UnsupportedOperationException();
    }
}
