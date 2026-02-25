package me.desht.pneumaticcraft.common.tileentity;

import me.desht.pneumaticcraft.api.item.IItemRegistry.EnumUpgrade;
import me.desht.pneumaticcraft.api.tileentity.IAirHandler;
import me.desht.pneumaticcraft.common.block.Blockss;
import me.desht.pneumaticcraft.common.inventory.handler.BaseItemStackHandler;
import me.desht.pneumaticcraft.common.item.ItemAssemblyProgram;
import me.desht.pneumaticcraft.common.item.Itemss;
import me.desht.pneumaticcraft.common.network.DescSynced;
import me.desht.pneumaticcraft.common.network.GuiSynced;
import me.desht.pneumaticcraft.common.recipes.programs.AssemblyProgram;
import me.desht.pneumaticcraft.common.recipes.programs.AssemblyProgram.EnumMachine;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TileEntityAssemblyController extends TileEntityPneumaticBase implements IAssemblyMachine, IMinWorkingPressure {
    private static final int PROGRAM_INVENTORY_INDEX = 0;
    private static final int INVENTORY_SIZE = 1;

    private final ItemStackHandler inventory = new BaseItemStackHandler(this, INVENTORY_SIZE) {
        @Override
        public boolean isItemValid(int slot, ItemStack itemStack) {
            return itemStack.isEmpty() || itemStack.getItem() == Itemss.ASSEMBLY_PROGRAM;
        }
    };
    @DescSynced
    public final boolean[] sidesConnected = new boolean[6];
    private AssemblyProgram curProgram;
    @GuiSynced
    private boolean isMachineMissing;
    @GuiSynced
    private boolean isMachineDuplicate;
    @GuiSynced
    private EnumMachine missingMachine;
    @GuiSynced
    private EnumMachine duplicateMachine;
    private boolean goingToHomePosition;
    @DescSynced
    public String displayedText = "";
    @DescSynced
    public boolean hasProblem;
    private AssemblySystem assemblySystem = null;

    @Override
    public IItemHandlerModifiable getPrimaryInventory() {
        return this.inventory;
    }

    public TileEntityAssemblyController() {
        super(PneumaticValues.DANGER_PRESSURE_ASSEMBLY_CONTROLLER, PneumaticValues.MAX_PRESSURE_ASSEMBLY_CONTROLLER, PneumaticValues.VOLUME_ASSEMBLY_CONTROLLER, 4);
        this.addApplicableUpgrade(EnumUpgrade.SPEED);
    }

    @Override
    public void update() {
        ItemStack programStack = this.inventory.getStackInSlot(PROGRAM_INVENTORY_INDEX);

        // curProgram must be available on the client, or we can't show program-problems in the GUI
        if (this.curProgram == null && !this.goingToHomePosition && programStack.getItem() == Itemss.ASSEMBLY_PROGRAM) {
            this.curProgram = ItemAssemblyProgram.getProgramFromItem(programStack.getMetadata());
        } else if (this.curProgram != null && (programStack.isEmpty() || this.curProgram.getClass() != ItemAssemblyProgram.getProgramFromItem(programStack.getMetadata()).getClass())) {
            this.curProgram = null;
            if (!this.getWorld().isRemote) this.goingToHomePosition = true;
        }

        if (!this.getWorld().isRemote) {
            this.setStatus("Standby");
            if (this.getPressure() >= PneumaticValues.MIN_PRESSURE_ASSEMBLY_CONTROLLER) {
                if (this.curProgram != null || this.goingToHomePosition) {
                    if (this.assemblySystem == null) {
                        this.assemblySystem = this.findAssemblySystem();
                    }
                    if (this.assemblySystem != null && (!this.isMachineMissing || this.curProgram == null) && !this.isMachineDuplicate) {
                        boolean useAir;
                        if (this.curProgram != null) {
                            useAir = this.curProgram.executeStep(this.assemblySystem);
                            if (useAir) {
                                this.setStatus("Running...");
                            }
                        } else {
                            useAir = true;
                            boolean resetDone = this.assemblySystem.reset();
                            this.goingToHomePosition = this.isMachineMissing || !resetDone;
                            this.setStatus("Resetting...");
                        }
                        if (useAir) {
                            this.addAir(-(int) (PneumaticValues.USAGE_ASSEMBLING * this.getSpeedUsageMultiplierFromUpgrades()));
                        }
                        this.assemblySystem.setSpeed(this.getSpeedMultiplierFromUpgrades());
                    }
                }
            }
            this.hasProblem = this.isMachineMissing
                    || this.isMachineDuplicate
                    || this.getPressure() < PneumaticValues.MIN_PRESSURE_ASSEMBLY_CONTROLLER
                    || this.curProgram == null
                    || this.curProgram.curProblem != AssemblyProgram.EnumTubeProblem.NO_PROBLEM;
        }
        super.update();
    }

    /**
     * Force the controller to rediscover its machines on the next tick.
     */
    void invalidateAssemblySystem() {
        this.assemblySystem = null;
    }

    private AssemblySystem findAssemblySystem() {
        EnumMachine[] requiredMachines = this.curProgram != null ? this.curProgram.getRequiredMachines() : EnumMachine.values();

        this.duplicateMachine = null;
        AssemblySystem assemblySystem = new AssemblySystem(this);
        for (IAssemblyMachine machine : this.findMachines(requiredMachines.length * 2)) {  // *2 ensures duplicates are noticed
            if (!assemblySystem.addMachine(machine)) {
                this.duplicateMachine = machine.getAssemblyType();
            }
        }
        this.missingMachine = assemblySystem.checkForMissingMachine(requiredMachines);

        this.isMachineDuplicate = this.duplicateMachine != null;
        this.isMachineMissing = this.missingMachine != null;

        return assemblySystem;
    }

    @Override
    protected boolean shouldRerenderChunkOnDescUpdate() {
        return true;
    }

    private void setStatus(String text) {
        this.displayedText = text;
    }

    @SideOnly(Side.CLIENT)
    public void addProblems(List<String> problemList) {
        if (this.curProgram == null) {
            problemList.addAll(PneumaticCraftUtils.convertStringIntoList(I18n.format("gui.tab.problems.assembly_controller.no_program")));
        } else {
            if (this.isMachineDuplicate) {
                String key = I18n.format(this.duplicateMachine.getTranslationKey());
                problemList.addAll(PneumaticCraftUtils.convertStringIntoList(I18n.format("gui.tab.problems.assembly_controller.duplicateMachine", key)));
            } else if (!this.isMachineMissing) {
                this.curProgram.addProgramProblem(problemList);
            } else {
                String key = I18n.format(this.missingMachine.getTranslationKey());
                problemList.addAll(PneumaticCraftUtils.convertStringIntoList(I18n.format("gui.tab.problems.assembly_controller.missingMachine", key)));
            }
        }
    }

    public List<IAssemblyMachine> findMachines(int max) {
        List<IAssemblyMachine> machineList = new ArrayList<>();
        this.findMachines(machineList, this.getPos(), max);
        return machineList;
    }

    private void findMachines(List<IAssemblyMachine> machineList, BlockPos pos, int max) {
        for (EnumFacing dir : EnumFacing.HORIZONTALS) {
            TileEntity te = this.getWorld().getTileEntity(pos.offset(dir));
            if (te instanceof IAssemblyMachine && !machineList.contains(te) && machineList.size() < max) {
                machineList.add((IAssemblyMachine) te);
                this.findMachines(machineList, te.getPos(), max);
            }
        }
    }

    @Override
    public void onNeighborBlockUpdate() {
        super.onNeighborBlockUpdate();
        this.updateConnections();
        this.invalidateAssemblySystem();
    }

    private void updateConnections() {
        List<Pair<EnumFacing, IAirHandler>> connections = this.getAirHandler(null).getConnectedPneumatics();
        Arrays.fill(this.sidesConnected, false);
        for (Pair<EnumFacing, IAirHandler> entry : connections) {
            this.sidesConnected[entry.getKey().ordinal()] = true;
        }
    }

    @Override
    public boolean isConnectedTo(EnumFacing side) {
        return side != EnumFacing.UP;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(this.getPos().getX(), this.getPos().getY(), this.getPos().getZ(), this.getPos().getX() + 1, this.getPos().getY() + 1, this.getPos().getZ() + 1);
    }

    @Override
    public String getName() {
        return Blockss.ASSEMBLY_CONTROLLER.getTranslationKey();
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.goingToHomePosition = tag.getBoolean("goingToHomePosition");
        this.displayedText = tag.getString("displayedText");
        for (int i = 0; i < 6; i++) {
            this.sidesConnected[i] = tag.getBoolean("sideConnected" + i);
        }
        this.inventory.deserializeNBT(tag.getCompoundTag("Items"));
        if (!this.inventory.getStackInSlot(PROGRAM_INVENTORY_INDEX).isEmpty()) {
            this.curProgram = ItemAssemblyProgram.getProgramFromItem(this.inventory.getStackInSlot(PROGRAM_INVENTORY_INDEX).getMetadata());
            if (this.curProgram != null) this.curProgram.readFromNBT(tag);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setBoolean("goingToHomePosition", this.goingToHomePosition);
        tag.setString("displayedText", this.displayedText);
        if (this.curProgram != null) this.curProgram.writeToNBT(tag);
        for (int i = 0; i < 6; i++) {
            tag.setBoolean("sideConnected" + i, this.sidesConnected[i]);
        }
        tag.setTag("Items", this.inventory.serializeNBT());
        return tag;
    }

    @Override
    public boolean isIdle() {
        return true;
    }

    @Override
    public void setSpeed(float speed) {
    }

    @Override
    public float getMinWorkingPressure() {
        return PneumaticValues.MIN_PRESSURE_ASSEMBLY_CONTROLLER;
    }

    @Override
    public EnumMachine getAssemblyType() {
        return EnumMachine.CONTROLLER;
    }

    @Override
    public void setControllerPos(BlockPos controllerPos) {
        // nop - we *are the controller!
    }

    public class AssemblySystem {
        final IAssemblyMachine[] machines = new IAssemblyMachine[EnumMachine.values().length];

        AssemblySystem(TileEntityAssemblyController controller) {
        }

        private IAssemblyMachine get(EnumMachine machine) {
            return this.machines[machine.ordinal()];
        }

        boolean addMachine(IAssemblyMachine machine) {
            if (this.machines[machine.getAssemblyType().ordinal()] != null) {
                return false;  // already present
            }
            this.machines[machine.getAssemblyType().ordinal()] = machine;
            machine.setControllerPos(TileEntityAssemblyController.this.getPos());
            return true;
        }

        boolean reset() {
            boolean resetDone = true;
            for (IAssemblyMachine machine : this.machines) {
                if (machine instanceof IResettable) {
                    if (!((IResettable) machine).reset()) {
                        resetDone = false;
                        if (machine instanceof TileEntityAssemblyPlatform) {
                            this.getExportUnit().pickupItem(null);
                        }
                        break;
                    }
                }
            }
            return resetDone;
        }

        void setSpeed(float speedMult) {
            for (IAssemblyMachine te : this.machines) {
                if (te != null) te.setSpeed(speedMult);
            }
        }

        TileEntityAssemblyController getController() {
            return (TileEntityAssemblyController) this.get(EnumMachine.CONTROLLER);
        }

        public TileEntityAssemblyIOUnit getImportUnit() {
            return (TileEntityAssemblyIOUnit) this.get(EnumMachine.IO_UNIT_IMPORT);
        }

        public TileEntityAssemblyIOUnit getExportUnit() {
            return (TileEntityAssemblyIOUnit) this.get(EnumMachine.IO_UNIT_EXPORT);
        }

        public TileEntityAssemblyPlatform getPlatform() {
            return (TileEntityAssemblyPlatform) this.get(EnumMachine.PLATFORM);
        }

        public TileEntityAssemblyLaser getLaser() {
            return (TileEntityAssemblyLaser) this.get(EnumMachine.LASER);
        }

        public TileEntityAssemblyDrill getDrill() {
            return (TileEntityAssemblyDrill) this.get(EnumMachine.DRILL);
        }

        EnumMachine checkForMissingMachine(EnumMachine[] requiredMachines) {
            for (EnumMachine e : requiredMachines) {
                if (this.get(e) == null) {
                    return e;
                }
            }
            return null;
        }
    }

}
