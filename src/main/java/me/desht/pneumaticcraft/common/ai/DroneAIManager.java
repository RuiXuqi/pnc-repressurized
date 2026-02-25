package me.desht.pneumaticcraft.common.ai;

import me.desht.pneumaticcraft.api.drone.SpecialVariableRetrievalEvent;
import me.desht.pneumaticcraft.api.item.IItemRegistry.EnumUpgrade;
import me.desht.pneumaticcraft.common.config.ConfigHandler;
import me.desht.pneumaticcraft.common.item.ItemRegistry;
import me.desht.pneumaticcraft.common.progwidgets.*;
import me.desht.pneumaticcraft.common.remote.GlobalVariableManager;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * This class is derived from Minecraft's {link EntityAITasks} class. As the original class would need quite a few
 * accesstransformers or reflection calls to do what I want, I've copied most of that class in here.
 */

public class DroneAIManager implements IVariableProvider {
    /**
     * A list of EntityAITaskEntrys in EntityAITasks.
     */
    private final List<EntityAITaskEntry> taskEntries = new ArrayList<>();

    /**
     * A list of EntityAITaskEntrys that are currently being executed.
     */
    private final List<EntityAITaskEntry> executingTaskEntries = new ArrayList<>();

    /**
     * Instance of Profiler.
     */
    private final Profiler theProfiler;
    private int tickCount;
    static final int TICK_RATE = 3;

    private final IDroneBase drone;
    private List<IProgWidget> progWidgets;
    private IProgWidget curActiveWidget;
    private EntityAIBase curWidgetAI;
    private EntityAIBase curWidgetTargetAI;
    private boolean stopWhenEndReached;
    private boolean wasAIOveridden;
    private String currentLabel = "Main";//Holds the name of the last label that was jumped to.

    private Map<String, BlockPos> coordinateVariables = new HashMap<>();
    private Map<String, ItemStack> itemVariables = new HashMap<>();
    private final Stack<IProgWidget> jumpBackWidgets = new Stack<>();//Used to jump back to a for each widget.

    private static final int MAX_JUMP_STACK_SIZE = 100;

    public DroneAIManager(IDroneBase drone) {
        this.theProfiler = drone.world().profiler;
        this.drone = drone;
        if (!drone.world().isRemote) {
            // we normally don't called clientside, but The One Probe can do it
            // don't set the widgets clientside because there aren't any and that messes up any entity tracker info
            this.setWidgets(drone.getProgWidgets());
        }
    }

    public DroneAIManager(IDroneBase drone, List<IProgWidget> progWidgets) {
        this.theProfiler = drone.world().profiler;
        this.drone = drone;
        this.stopWhenEndReached = true;
        this.setWidgets(progWidgets);
    }

    public void dontStopWhenEndReached() {
        this.stopWhenEndReached = false;
    }

    public void setWidgets(List<IProgWidget> progWidgets) {
        this.progWidgets = progWidgets;
        if (progWidgets.isEmpty()) {
            this.setActiveWidget(null);
        } else {
            for (IProgWidget widget : progWidgets) {
                if (widget instanceof IVariableWidget) {
                    ((IVariableWidget) widget).setAIManager(this);
                }
            }
            this.gotoFirstWidget();
        }
    }

    public void connectVariables(DroneAIManager subAI) {
        subAI.coordinateVariables = this.coordinateVariables;
        subAI.itemVariables = this.itemVariables;
    }

    public boolean isIdling() {
        return this.curWidgetAI == null;
    }

    public EntityAIBase getCurrentAI() {
        return this.curWidgetAI;
    }

    public IDroneBase getDrone() {
        return this.drone;
    }

    public void writeToNBT(NBTTagCompound tag) {
        NBTTagList tagList = new NBTTagList();
        for (Map.Entry<String, BlockPos> entry : this.coordinateVariables.entrySet()) {
            NBTTagCompound t = new NBTTagCompound();
            t.setString("key", entry.getKey());
            t.setInteger("x", entry.getValue().getX());
            t.setInteger("y", entry.getValue().getY());
            t.setInteger("z", entry.getValue().getZ());
            tagList.appendTag(t);
        }
        tag.setTag("coords", tagList);

        GlobalVariableManager.getInstance().writeItemVars(tag, this.itemVariables);
    }

    public void readFromNBT(NBTTagCompound tag) {
        this.coordinateVariables.clear();
        NBTTagList tagList = tag.getTagList("coords", 10);
        for (int i = 0; i < tagList.tagCount(); i++) {
            NBTTagCompound t = tagList.getCompoundTagAt(i);
            this.coordinateVariables.put(t.getString("key"), new BlockPos(t.getInteger("x"), t.getInteger("y"), t.getInteger("z")));
        }

        GlobalVariableManager.readItemVars(tag, this.itemVariables);
    }

    @Override
    public BlockPos getCoordinate(String varName) {
        BlockPos pos;
        if (varName.startsWith("$")) {
            SpecialVariableRetrievalEvent.CoordinateVariable.Drone event = new SpecialVariableRetrievalEvent.CoordinateVariable.Drone(this.drone, varName.substring(1));
            MinecraftForge.EVENT_BUS.post(event);
            pos = event.coordinate;
        } else if (varName.startsWith("#")) {
            pos = GlobalVariableManager.getInstance().getPos(varName.substring(1));
        } else {
            pos = this.coordinateVariables.get(varName);
        }
        return pos != null ? pos : BlockPos.ORIGIN;
    }

    public void setCoordinate(String varName, BlockPos coord) {
        if (varName.startsWith("#")) {
            GlobalVariableManager.getInstance().set(varName.substring(1), coord);
        } else if (!varName.startsWith("$")) this.coordinateVariables.put(varName, coord);
    }

    @Nonnull
    public ItemStack getStack(String varName) {
        ItemStack item;
        if (varName.startsWith("$")) {
            SpecialVariableRetrievalEvent.ItemVariable.Drone event = new SpecialVariableRetrievalEvent.ItemVariable.Drone(this.drone, varName.substring(1));
            MinecraftForge.EVENT_BUS.post(event);
            item = event.item;
        } else if (varName.startsWith("#")) {
            item = GlobalVariableManager.getInstance().getItem(varName.substring(1));
        } else {
            item = this.itemVariables.getOrDefault(varName, ItemStack.EMPTY);
        }
        return item;
    }

    public void setItem(String varName, @Nonnull ItemStack item) {
        if (varName.startsWith("#")) {
            GlobalVariableManager.getInstance().set(varName.substring(1), item);
        } else if (!varName.startsWith("$")) this.itemVariables.put(varName, item);
    }

    private void updateWidgetFlow() {
        boolean isExecuting = false;
        for (EntityAITaskEntry entry : this.executingTaskEntries) {
            if (this.curWidgetAI == entry.action) {
                isExecuting = true;
                break;
            }
        }
        if (!isExecuting && this.curActiveWidget != null && (this.curWidgetTargetAI == null || !this.curWidgetTargetAI.shouldExecute())) {
            IProgWidget widget = this.curActiveWidget.getOutputWidget(this.drone, this.progWidgets);
            if (widget != null) {
                if (this.curActiveWidget.getOutputWidget() != widget) {
                    if (this.addJumpBackWidget(this.curActiveWidget)) return;
                }
                this.setActiveWidget(widget);
            } else {
                if (this.stopWhenEndReached) {
                    this.setActiveWidget(null);
                } else {
                    this.gotoFirstWidget();
                }
            }
        }
        if (this.curActiveWidget == null && !this.stopWhenEndReached) {
            this.gotoFirstWidget();
        }
    }

    private void gotoFirstWidget() {
        this.setLabel("Main");
        if (!this.jumpBackWidgets.isEmpty()) {
            this.setActiveWidget(this.jumpBackWidgets.pop());
        } else {
            for (IProgWidget widget : this.progWidgets) {
                if (widget instanceof ProgWidgetStart) {
                    this.setActiveWidget(widget);
                    return;
                }
            }
        }
    }

    private void setActiveWidget(IProgWidget widget) {
        EntityAIBase targetAI = null;
        EntityAIBase ai = null;
        if (widget != null) {
            boolean first = widget instanceof ProgWidgetStart;
            targetAI = widget.getWidgetTargetAI(this.drone, widget);
            ai = widget.getWidgetAI(this.drone, widget);
            Set<IProgWidget> visitedWidgets = new HashSet<>();//Prevent endless loops
            while (!visitedWidgets.contains(widget) && targetAI == null && ai == null) {
                visitedWidgets.add(widget);
                IProgWidget oldWidget = widget;
                widget = widget.getOutputWidget(this.drone, this.progWidgets);
                if (widget == null) {
                    if (first) {
                        return;
                    } else {
                        if (this.stopWhenEndReached) {
                            this.setActiveWidget(null);
                        } else {
                            this.gotoFirstWidget();
                        }
                        return;
                    }
                } else if (oldWidget.getOutputWidget() != widget) {
                    if (this.addJumpBackWidget(oldWidget)) return;
                }
                targetAI = widget.getWidgetTargetAI(this.drone, widget);
                ai = widget.getWidgetAI(this.drone, widget);
            }
            this.drone.setActiveProgram(widget);
        } else {
            this.setLabel("Stopped");
        }

        this.curActiveWidget = widget;
        if (this.curWidgetAI != null) this.removeTask(this.curWidgetAI);
        if (this.curWidgetTargetAI != null) this.drone.getTargetAI().removeTask(this.curWidgetTargetAI);
        if (ai != null) this.addTask(2, ai);
        if (targetAI != null) this.drone.getTargetAI().addTask(2, targetAI);
        this.curWidgetAI = ai;
        this.curWidgetTargetAI = targetAI;
    }

    private boolean addJumpBackWidget(IProgWidget widget) {
        if (widget instanceof IJumpBackWidget) {
            if (this.jumpBackWidgets.size() >= MAX_JUMP_STACK_SIZE) {
                this.drone.overload("jumpStackTooLarge", MAX_JUMP_STACK_SIZE);
                this.jumpBackWidgets.clear();
                this.setActiveWidget(null);
                return true;
            } else {
                this.jumpBackWidgets.push(widget);
            }
        }
        return false;
    }

    public List<EntityAITaskEntry> getRunningTasks() {
        return this.taskEntries;
    }

    public EntityAIBase getTargetAI() {
        return this.curWidgetTargetAI;
    }

    /**
     * START EntityAITasks code
     */

    public void addTask(int par1, EntityAIBase par2EntityAIBase) {
        this.taskEntries.add(new EntityAITaskEntry(par1, par2EntityAIBase));
    }

    /**
     * removes the indicated task from the entity's AI tasks.
     */
    public void removeTask(EntityAIBase par1EntityAIBase) {
        Iterator iterator = this.taskEntries.iterator();

        while (iterator.hasNext()) {
            EntityAITaskEntry entityaitaskentry = (EntityAITaskEntry) iterator.next();
            EntityAIBase entityaibase1 = entityaitaskentry.action;

            if (entityaibase1 == par1EntityAIBase) {
                if (this.executingTaskEntries.contains(entityaitaskentry)) {
                    entityaibase1.resetTask();
                    this.executingTaskEntries.remove(entityaitaskentry);
                }

                iterator.remove();
            }
        }
    }

    private void pickupItemsIfMagnet() {
        int magnetUpgrades = this.drone.getUpgrades(ItemRegistry.getInstance().getUpgrade(EnumUpgrade.MAGNET));
        if (magnetUpgrades > 0) {
            int range = Math.min(6, 1 + magnetUpgrades);
            Vec3d v = this.drone.getDronePos();
            AxisAlignedBB aabb = new AxisAlignedBB(v.x, v.y, v.z, v.x, v.y, v.z).grow(range);
            List<EntityItem> items = this.drone.world().getEntitiesWithinAABB(EntityItem.class, aabb,
                    item -> item != null
                            && item.isEntityAlive()
                            && !item.cannotPickup()
                            && this.drone.getDronePos().squareDistanceTo(item.getPositionVector()) <= range * range);

            for (EntityItem item : items) {
                DroneEntityAIPickupItems.tryPickupItem(this.drone, item);
            }
        }
    }

    public void onUpdateTasks() {
        this.pickupItemsIfMagnet();

        if (ConfigHandler.advanced.stopDroneAI) return;
        if (!this.drone.isAIOverriden()) {
            if (this.wasAIOveridden && this.curWidgetTargetAI != null) this.drone.getTargetAI().addTask(2, this.curWidgetTargetAI);
            this.wasAIOveridden = false;
            ArrayList<EntityAITaskEntry> arraylist = new ArrayList<>();
            Iterator<EntityAITaskEntry> iterator;
            EntityAITaskEntry entityaitaskentry;

            if (this.tickCount++ % TICK_RATE == 0) {
                iterator = this.taskEntries.iterator();

                while (iterator.hasNext()) {
                    entityaitaskentry = iterator.next();
                    boolean flag = this.executingTaskEntries.contains(entityaitaskentry);

                    if (flag) {
                        if (this.canUse(entityaitaskentry) && this.canContinue(entityaitaskentry)) {
                            continue;
                        }

                        entityaitaskentry.action.resetTask();
                        this.executingTaskEntries.remove(entityaitaskentry);
                    }

                    if (this.canUse(entityaitaskentry) && entityaitaskentry.action.shouldExecute()) {
                        arraylist.add(entityaitaskentry);
                        this.executingTaskEntries.add(entityaitaskentry);
                    }
                }
                this.updateWidgetFlow();
            } else {
                iterator = this.executingTaskEntries.iterator();

                while (iterator.hasNext()) {
                    entityaitaskentry = iterator.next();

                    if (!entityaitaskentry.action.shouldContinueExecuting()) {
                        entityaitaskentry.action.resetTask();
                        iterator.remove();
                    }
                }
            }

            this.theProfiler.startSection("goalStart");
            iterator = arraylist.iterator();

            while (iterator.hasNext()) {
                entityaitaskentry = iterator.next();
                this.theProfiler.startSection(entityaitaskentry.action.getClass().getSimpleName());
                entityaitaskentry.action.startExecuting();
                this.theProfiler.endSection();
            }

            this.theProfiler.endSection();
            this.theProfiler.startSection("goalTick");
            iterator = this.executingTaskEntries.iterator();

            while (iterator.hasNext()) {
                entityaitaskentry = iterator.next();
                entityaitaskentry.action.updateTask();
            }

            this.theProfiler.endSection();
        } else {//drone charging ai is running
            if (!this.wasAIOveridden && this.curWidgetTargetAI != null) {
                this.drone.getTargetAI().removeTask(this.curWidgetTargetAI);
            }
            this.wasAIOveridden = true;
            for (EntityAITaskEntry ai : this.executingTaskEntries) {
                ai.action.resetTask();
            }
            this.executingTaskEntries.clear();
            this.drone.setDugBlock(null);
        }
    }

    /**
     * Determine if a specific AI Task should continue being executed.
     */
    private boolean canContinue(EntityAITaskEntry par1EntityAITaskEntry) {
        this.theProfiler.startSection("canContinue");
        boolean flag = par1EntityAITaskEntry.action.shouldContinueExecuting();
        this.theProfiler.endSection();
        return flag;
    }

    /**
     * Determine if a specific AI Task can be executed, which means that all running higher (= lower int value) priority
     * tasks are compatible with it or all lower priority tasks can be interrupted.
     */
    private boolean canUse(EntityAITaskEntry par1EntityAITaskEntry) {
        this.theProfiler.startSection("canUse");

        for (EntityAITaskEntry entry : this.taskEntries) {
            if (entry != par1EntityAITaskEntry) {
                if (par1EntityAITaskEntry.priority >= entry.priority) {
                    if (this.executingTaskEntries.contains(entry) && !this.areTasksCompatible(par1EntityAITaskEntry, entry)) {
                        this.theProfiler.endSection();
                        return false;
                    }
                } else if (this.executingTaskEntries.contains(entry) && !entry.action.isInterruptible()) {
                    this.theProfiler.endSection();
                    return false;
                }
            }
        }

        this.theProfiler.endSection();
        return true;
    }

    /**
     * Returns whether two EntityAITaskEntries can be executed concurrently
     */
    private boolean areTasksCompatible(EntityAITaskEntry par1EntityAITaskEntry, EntityAITaskEntry par2EntityAITaskEntry) {
        return (par1EntityAITaskEntry.action.getMutexBits() & par2EntityAITaskEntry.action.getMutexBits()) == 0;
    }

    public void setLabel(String label) {
        this.currentLabel = label;
        this.drone.updateLabel();
    }

    public String getLabel() {
        if (this.curWidgetAI instanceof DroneAIExternalProgram) {
            return ((DroneAIExternalProgram) this.curWidgetAI).getRunningAI().getLabel() + " --> " + this.currentLabel;
        } else {
            return this.currentLabel;
        }
    }

    public class EntityAITaskEntry {
        /**
         * The EntityAIBase object.
         */
        public final EntityAIBase action;
        /**
         * Priority of the EntityAIBase
         */
        public final int priority;

        public EntityAITaskEntry(int par2, EntityAIBase par3EntityAIBase) {
            this.priority = par2;
            this.action = par3EntityAIBase;
        }
    }

}
