package me.desht.pneumaticcraft.common.thirdparty.computercraft;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedPeripheral;
import li.cil.oc.api.network.SimpleComponent;
import me.desht.pneumaticcraft.api.item.IItemRegistry.EnumUpgrade;
import me.desht.pneumaticcraft.common.ai.DroneAIManager.EntityAITaskEntry;
import me.desht.pneumaticcraft.common.entity.living.EntityDrone;
import me.desht.pneumaticcraft.common.item.ItemProgrammingPuzzle;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketShowArea;
import me.desht.pneumaticcraft.common.network.PacketSpawnRing;
import me.desht.pneumaticcraft.common.progwidgets.IBlockOrdered.EnumOrder;
import me.desht.pneumaticcraft.common.progwidgets.*;
import me.desht.pneumaticcraft.lib.ModIds;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.item.ItemDye;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

@Optional.InterfaceList({
        @Optional.Interface(iface = "dan200.computercraft.api.peripheral.IPeripheral", modid = ModIds.COMPUTERCRAFT),
        @Optional.Interface(iface = "li.cil.oc.api.network.ManagedPeripheral", modid = ModIds.OPEN_COMPUTERS),
        @Optional.Interface(iface = "li.cil.oc.api.network.SimpleComponent", modid = ModIds.OPEN_COMPUTERS)
})
public class TileEntityDroneInterface extends TileEntity
        implements ITickable, IPeripheral, ManagedPeripheral, SimpleComponent {

    private LuaMethodRegistry luaMethodRegistry;

    private final CopyOnWriteArrayList<IComputerAccess> attachedComputers = new CopyOnWriteArrayList<>();
    private EntityDrone drone;
    public float rotationYaw;
    public float rotationPitch = (float) Math.toRadians(-42);
    private final ConcurrentLinkedQueue<Integer> ringSendQueue = new ConcurrentLinkedQueue<>();
    private int ringSendCooldown;
    private IProgWidget curAction;
    private int droneId; // track drone ID client-side

    public TileEntityDroneInterface() {
        this.setupLuaMethods();
    }

    @Override
    public void update() {
        if (this.drone != null && this.drone.isDead) {
            this.setDrone(null);
        }
        if (this.drone != null) {
            if (this.getWorld().isRemote) {
                double dx = this.drone.posX - (this.getPos().getX() + 0.5);
                double dy = this.drone.posY - (this.getPos().getY() + 0.5);
                double dz = this.drone.posZ - (this.getPos().getZ() + 0.5);
                float f3 = MathHelper.sqrt(dx * dx + dz * dz);
                this.rotationYaw = (float) -Math.atan2(dx, dz);
                this.rotationPitch = (float) -Math.atan2(dy, f3);
            } else {
                if (this.ringSendCooldown > 0) this.ringSendCooldown--;
                if (!this.ringSendQueue.isEmpty() && this.ringSendCooldown <= 0) {
                    this.ringSendCooldown = this.ringSendQueue.size() > 10 ? 1 : 5;
                    NetworkHandler.sendToDimension(new PacketSpawnRing(this.getPos().getX() + 0.5, this.getPos().getY() + 0.8, this.getPos().getZ() + 0.5, this.drone, this.ringSendQueue.poll()), this.getWorld().provider.getDimension());
                }
            }
        }
        if (this.getWorld().isRemote) {
            EntityDrone prevDrone = this.drone;
            Entity e = this.getWorld().getEntityByID(this.droneId);
            if (e instanceof EntityDrone) {
                this.drone = (EntityDrone) e;
            } else {
                this.drone = null;
            }
            if (prevDrone != this.drone) {
                this.world.markBlockRangeForRenderUpdate(this.pos, this.pos);
            }
        }
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.getPos(), this.getBlockMetadata(), this.getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.getUpdateTag();
        tag.setInteger("drone", this.drone != null ? this.drone.getEntityId() : -1);
        return tag;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        super.handleUpdateTag(tag);
        this.droneId = tag.getInteger("drone");
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        this.handleUpdateTag(pkt.getNbtCompound());
    }

    boolean isDroneConnected() {
        return this.drone != null;
    }

    private EntityDrone validateAndGetDrone() {
        if (this.drone == null) throw new IllegalStateException("There's no connected Drone!");
        return this.drone;
    }

    private void setupLuaMethods() {
        this.luaMethodRegistry = new LuaMethodRegistry();

        this.registerLuaMethod(new LuaMethod("isConnectedToDrone") {
            @Override
            public Object[] call(Object[] args) {
                this.requireNoArgs(args);
                return new Object[]{TileEntityDroneInterface.this.drone != null};
            }
        });

        this.registerLuaMethod(new LuaMethod("getDronePressure") {
            @Override
            public Object[] call(Object[] args) {
                this.requireNoArgs(args);
                return new Object[]{(double) TileEntityDroneInterface.this.validateAndGetDrone().getPressure(null)};
            }
        });

        this.registerLuaMethod(new LuaMethod("exitPiece") {
            @Override
            public Object[] call(Object[] args) {
                this.requireNoArgs(args);
                //noinspection ResultOfMethodCallIgnored
                TileEntityDroneInterface.this.validateAndGetDrone();
                TileEntityDroneInterface.this.setDrone(null); // disconnect
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("getAllActions") {
            @Override
            public Object[] call(Object[] args) {
                this.requireNoArgs(args);
                List<String> actions = new ArrayList<>();
                for (IProgWidget widget : WidgetRegistrator.registeredWidgets) {
                    if (widget.canBeRunByComputers(new EntityDrone(TileEntityDroneInterface.this.getWorld()), TileEntityDroneInterface.this.getWidget())) {
                        actions.add(widget.getWidgetString());
                    }
                }
                return new Object[]{this.getStringTable(actions)};
            }
        });

        this.registerLuaMethod(new LuaMethod("getDronePosition") {
            @Override
            public Object[] call(Object[] args) {
                this.requireNoArgs(args);
                EntityDrone d = TileEntityDroneInterface.this.validateAndGetDrone();
                return new Double[]{d.posX, d.posY, d.posZ};
            }
        });

        this.registerLuaMethod(new LuaMethod("setBlockOrder") {
            @Override
            public Object[] call(Object[] args) {
                this.requireArgs(args, 1, "'closest'/'highToLow'/'lowToHigh'");
                String arg = (String) args[0];
                for (EnumOrder order : EnumOrder.values()) {
                    if (order.toString().equalsIgnoreCase(arg)) {
                        TileEntityDroneInterface.this.getWidget().setOrder(order);
                        return null;
                    }
                }
                throw new IllegalArgumentException("No valid order. Valid arguments:  'closest', 'highToLow' or 'lowToHigh'!");
            }
        });

        this.registerLuaMethod(new LuaMethod("getAreaTypes") {
            @Override
            public Object[] call(Object[] args) {
                this.requireNoArgs(args);
                return TileEntityDroneInterface.this.getWidget().getAreaTypes();
            }
        });

        this.registerLuaMethod(new LuaMethod("addArea") {
            @Override
            public Object[] call(Object[] args) {
                this.requireArgs(args, new int[]{3, 7}, "(x,y,z) or (x1,y1,z1,x2,y2,z2,areaType)");
                if (args.length == 3) {
                    TileEntityDroneInterface.this.getWidget().addArea(((Double) args[0]).intValue(), ((Double) args[1]).intValue(), ((Double) args[2]).intValue());
                } else {  // 7
                    TileEntityDroneInterface.this.getWidget().addArea(((Double) args[0]).intValue(), ((Double) args[1]).intValue(), ((Double) args[2]).intValue(),
                            ((Double) args[3]).intValue(), ((Double) args[4]).intValue(), ((Double) args[5]).intValue(),
                            (String) args[6]);
                }
                TileEntityDroneInterface.this.messageToDrone(ProgWidgetArea.class);
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("removeArea") {
            @Override
            public Object[] call(Object[] args) {
                this.requireArgs(args, new int[]{3, 7}, "(x,y,z) or (x1,y1,z1,x2,y2,z2,areaType)");
                if (args.length == 3) {
                    TileEntityDroneInterface.this.getWidget().removeArea(((Double) args[0]).intValue(), ((Double) args[1]).intValue(), ((Double) args[2]).intValue());
                } else if (args.length == 7) {
                    TileEntityDroneInterface.this.getWidget().removeArea(((Double) args[0]).intValue(), ((Double) args[1]).intValue(), ((Double) args[2]).intValue(),
                            ((Double) args[3]).intValue(), ((Double) args[4]).intValue(), ((Double) args[5]).intValue(),
                            (String) args[6]);

                }
                TileEntityDroneInterface.this.messageToDrone(ProgWidgetArea.class);
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("clearArea") {
            @Override
            public Object[] call(Object[] args) {
                this.requireNoArgs(args);
                TileEntityDroneInterface.this.getWidget().clearArea();
                TileEntityDroneInterface.this.messageToDrone(ProgWidgetArea.class);
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("showArea") {
            @Override
            public Object[] call(Object[] args) {
                this.requireNoArgs(args);
                Set<BlockPos> area = new HashSet<>();
                TileEntityDroneInterface.this.getWidget().getArea(area);
                NetworkHandler.sendToDimension(new PacketShowArea(TileEntityDroneInterface.this.getPos(), area), TileEntityDroneInterface.this.getWorld().provider.getDimension());
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("hideArea") {
            @Override
            public Object[] call(Object[] args) {
                this.requireNoArgs(args);
                NetworkHandler.sendToDimension(new PacketShowArea(TileEntityDroneInterface.this.getPos()), TileEntityDroneInterface.this.getWorld().provider.getDimension());
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("addWhitelistItemFilter") {
            @Override
            public Object[] call(Object[] args) {
                this.requireArgs(args, 6, "<string> item/block name, <int> item/block metadata, <bool> Use Metadata, <bool> Use NBT, <bool> Use Ore Dictionary, <bool> Use Mod Similarity");
                TileEntityDroneInterface.this.getWidget().addWhitelistItemFilter((String) args[0], ((Double) args[1]).intValue(), (Boolean) args[2], (Boolean) args[3], (Boolean) args[4], (Boolean) args[5]);
                TileEntityDroneInterface.this.messageToDrone(ProgWidgetItemFilter.class);
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("addBlacklistItemFilter") {
            @Override
            public Object[] call(Object[] args) {
                this.requireArgs(args, 6, "<string> item/block name, <int> item/block metadata, <bool> Use Metadata, <bool> Use NBT, <bool> Use Ore Dictionary, <bool> Use Mod Similarity");
                TileEntityDroneInterface.this.getWidget().addBlacklistItemFilter((String) args[0], ((Double) args[1]).intValue(), (Boolean) args[2], (Boolean) args[3], (Boolean) args[4], (Boolean) args[5]);
                TileEntityDroneInterface.this.messageToDrone(ProgWidgetItemFilter.class);
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("clearWhitelistItemFilter") {
            @Override
            public Object[] call(Object[] args) {
                this.requireNoArgs(args);
                TileEntityDroneInterface.this.getWidget().clearItemWhitelist();
                TileEntityDroneInterface.this.messageToDrone(ProgWidgetItemFilter.class);
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("clearBlacklistItemFilter") {
            @Override
            public Object[] call(Object[] args) {
                this.requireNoArgs(args);
                TileEntityDroneInterface.this.getWidget().clearItemBlacklist();
                TileEntityDroneInterface.this.messageToDrone(ProgWidgetItemFilter.class);
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("addWhitelistText") {
            @Override
            public Object[] call(Object[] args) {
                this.requireArgs(args, 1, "<string> text");
                TileEntityDroneInterface.this.getWidget().addWhitelistText((String) args[0]);
                TileEntityDroneInterface.this.messageToDrone(ProgWidgetString.class);
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("addBlacklistText") {
            @Override
            public Object[] call(Object[] args) {
                this.requireArgs(args, 1, "<string> text");
                TileEntityDroneInterface.this.getWidget().addBlacklistText((String) args[0]);
                TileEntityDroneInterface.this.messageToDrone(ProgWidgetString.class);
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("clearWhitelistText") {
            @Override
            public Object[] call(Object[] args) {
                this.requireNoArgs(args);
                TileEntityDroneInterface.this.getWidget().clearWhitelistText();
                TileEntityDroneInterface.this.messageToDrone(ProgWidgetString.class);
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("clearBlacklistText") {
            @Override
            public Object[] call(Object[] args) {
                this.requireNoArgs(args);
                TileEntityDroneInterface.this.getWidget().clearBlacklistText();
                TileEntityDroneInterface.this.messageToDrone(ProgWidgetString.class);
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("setSide") {
            @Override
            public Object[] call(Object[] args) {
                this.requireArgs(args, 2, "down/up/north/south/west/east, <boolean> valid");
                EnumFacing dir = this.getDirForString((String) args[0]);
                boolean[] sides = TileEntityDroneInterface.this.getWidget().getSides();
                sides[dir.ordinal()] = (Boolean) args[1]; // We don't need to set them afterwards, got a reference.
                TileEntityDroneInterface.this.messageToDrone(0xFFFFFFFF);
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("setSides") {
            @Override
            public Object[] call(Object[] args) {
                this.requireArgs(args, 6, "6 x boolean (order: DUNSWE)");
                boolean[] sides = new boolean[6];
                for (int i = 0; i < 6; i++) {
                    sides[i] = (Boolean) args[i];
                }
                TileEntityDroneInterface.this.getWidget().setSides(sides);
                TileEntityDroneInterface.this.messageToDrone(0xFFFFFFFF);
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("setEmittingRedstone") {
            @Override
            public Object[] call(Object[] args) {
                this.requireArgs(args, 1, "<int> signal_strength");
                TileEntityDroneInterface.this.getWidget().setEmittingRedstone(((Double) args[0]).intValue());
                TileEntityDroneInterface.this.messageToDrone(0xFFFFFFFF);
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("setRenameString") {
            @Override
            public Object[] call(Object[] args) {
                this.requireArgs(args, 1, "<string> new_name");
                TileEntityDroneInterface.this.getWidget().setNewName((String) args[0]);
                TileEntityDroneInterface.this.messageToDrone(0xFFFFFFFF);
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("addWhitelistLiquidFilter") {
            @Override
            public Object[] call(Object[] args) {
                this.requireArgs(args, 1, "<string> fluid_name");
                TileEntityDroneInterface.this.getWidget().addWhitelistLiquidFilter((String) args[0]);
                TileEntityDroneInterface.this.messageToDrone(0xFFFFFFFF);
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("addBlacklistLiquidFilter") {
            @Override
            public Object[] call(Object[] args) {
                this.requireArgs(args, 1, "<string> fluid_name");
                TileEntityDroneInterface.this.getWidget().addBlacklistLiquidFilter((String) args[0]);
                TileEntityDroneInterface.this.messageToDrone(0xFFFFFFFF);
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("clearWhitelistLiquidFilter") {
            @Override
            public Object[] call(Object[] args) {
                this.requireNoArgs(args);
                TileEntityDroneInterface.this.getWidget().clearLiquidWhitelist();
                TileEntityDroneInterface.this.messageToDrone(0xFFFFFFFF);
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("clearBlacklistLiquidFilter") {
            @Override
            public Object[] call(Object[] args) {
                this.requireNoArgs(args);
                TileEntityDroneInterface.this.getWidget().clearLiquidBlacklist();
                TileEntityDroneInterface.this.messageToDrone(0xFFFFFFFF);
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("setDropStraight") {
            @Override
            public Object[] call(Object[] args) {
                this.requireArgs(args, 1, "<boolean> drop_straight");
                TileEntityDroneInterface.this.getWidget().setDropStraight((Boolean) args[0]);
                TileEntityDroneInterface.this.messageToDrone(0xFFFFFFFF);
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("setUseCount") {
            @Override
            public Object[] call(Object[] args) {
                this.requireArgs(args, 1, "<boolean> should_use_count");
                TileEntityDroneInterface.this.getWidget().setUseCount((Boolean) args[0]);
                TileEntityDroneInterface.this.messageToDrone(0xFFFFFFFF);
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("setCount") {
            @Override
            public Object[] call(Object[] args) {
                this.requireArgs(args, 1, "<int> use_count");
                TileEntityDroneInterface.this.getWidget().setCount(((Double) args[0]).intValue());
                TileEntityDroneInterface.this.messageToDrone(0xFFFFFFFF);
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("setIsAndFunction") {
            @Override
            public Object[] call(Object[] args) {
                this.requireArgs(args, 1, "<boolean> is_and_function");
                TileEntityDroneInterface.this.getWidget().setAndFunction((Boolean) args[0]);
                TileEntityDroneInterface.this.messageToDrone(0xFFFFFFFF);
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("setOperator") {
            @Override
            public Object[] call(Object[] args) {
                this.requireArgs(args, 1, "<string> '>=', '=' or = '>='");
                TileEntityDroneInterface.this.getWidget().setOperator((String) args[0]);
                TileEntityDroneInterface.this.messageToDrone(0xFFFFFFFF);
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("evaluateCondition") {
            @Override
            public Object[] call(Object[] args) {
                this.requireNoArgs(args);
                if (TileEntityDroneInterface.this.curAction instanceof ICondition) {
                    boolean bool = ((ICondition) TileEntityDroneInterface.this.curAction).evaluate(TileEntityDroneInterface.this.drone, TileEntityDroneInterface.this.getWidget());
                    return new Object[]{bool};
                } else {
                    throw new IllegalArgumentException("Current action is not a condition! Action: " + (TileEntityDroneInterface.this.curAction != null ? TileEntityDroneInterface.this.curAction.getWidgetString() : "*none*"));
                }
            }
        });

        this.registerLuaMethod(new LuaMethod("setUseMaxActions") {
            @Override
            public Object[] call(Object[] args) {
                this.requireArgs(args, 1, "<boolean> should_use_max_actions");
                TileEntityDroneInterface.this.getWidget().setUseMaxActions((Boolean) args[0]);
                TileEntityDroneInterface.this.messageToDrone(0xFFFFFFFF);
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("setMaxActions") {
            @Override
            public Object[] call(Object[] args) {
                this.requireArgs(args, 1, "<int> max_actions");
                TileEntityDroneInterface.this.getWidget().setMaxActions(((Double) args[0]).intValue());
                TileEntityDroneInterface.this.messageToDrone(0xFFFFFFFF);
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("setSneaking") {
            @Override
            public Object[] call(Object[] args) {
                this.requireArgs(args, 1, "<boolean> is_sneaking");
                TileEntityDroneInterface.this.getWidget().setSneaking((Boolean) args[0]);
                TileEntityDroneInterface.this.messageToDrone(0xFFFFFFFF);
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("setPlaceFluidBlocks") {
            @Override
            public Object[] call(Object[] args) {
                this.requireArgs(args, 1, "<boolean> should_place_fluid_blocks");
                TileEntityDroneInterface.this.getWidget().setPlaceFluidBlocks((Boolean) args[0]);
                TileEntityDroneInterface.this.messageToDrone(0xFFFFFFFF);
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("setAction") {
            @Override
            public Object[] call(Object[] args) {
                this.requireArgs(args, 1, "<string> action_name");
                String widgetName = (String) args[0];
                for (IProgWidget widget : WidgetRegistrator.registeredWidgets) {
                    if (widget.getWidgetString().equalsIgnoreCase(widgetName)) {
                        EntityAIBase ai = widget.getWidgetAI(TileEntityDroneInterface.this.drone, TileEntityDroneInterface.this.getWidget());
                        if (ai == null || !widget.canBeRunByComputers(TileEntityDroneInterface.this.drone, TileEntityDroneInterface.this.getWidget())) {
                            throw new IllegalArgumentException("Parsed action '" + widgetName + "' is not a runnable action!");
                        }
                        TileEntityDroneInterface.this.getAI().setAction(widget, ai);
                        TileEntityDroneInterface.this.getTargetAI().setAction(widget, widget.getWidgetTargetAI(TileEntityDroneInterface.this.drone, TileEntityDroneInterface.this.getWidget()));
                        TileEntityDroneInterface.this.messageToDrone(ItemDye.DYE_COLORS[widget.getCraftingColorIndex()]);
                        TileEntityDroneInterface.this.curAction = widget;
                        return null;
                    }
                }
                throw new IllegalArgumentException("No action with the name '" + widgetName + "'!");
            }
        });

        this.registerLuaMethod(new LuaMethod("getAction") {
            @Override
            public Object[] call(Object[] args) {
                this.requireNoArgs(args);
                return TileEntityDroneInterface.this.curAction != null ? new Object[]{TileEntityDroneInterface.this.curAction.getWidgetString()} : null;
            }
        });

        this.registerLuaMethod(new LuaMethod("abortAction") {
            @Override
            public Object[] call(Object[] args) {
                this.requireNoArgs(args);
                TileEntityDroneInterface.this.getAI().abortAction();
                TileEntityDroneInterface.this.getTargetAI().abortAction();
                TileEntityDroneInterface.this.messageToDrone(0xFFFFFFFF);
                TileEntityDroneInterface.this.curAction = null;
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("isActionDone") {
            @Override
            public Object[] call(Object[] args) {
                this.requireNoArgs(args);
                return new Object[]{TileEntityDroneInterface.this.getAI().isActionDone()};
            }
        });

        this.registerLuaMethod(new LuaMethod("forgetTarget") {
            @Override
            public Object[] call(Object[] args) {
                this.requireNoArgs(args);
                TileEntityDroneInterface.this.validateAndGetDrone().setAttackTarget(null);
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("getUpgrades") {
            @Override
            public Object[] call(Object[] args) {
                this.requireArgs(args, 1, "<int> upgrade_index");
                return new Object[]{(double) TileEntityDroneInterface.this.validateAndGetDrone().getUpgrades(EnumUpgrade.values()[((Double) args[0]).intValue()])};
            }
        });

        this.registerLuaMethod(new LuaMethod("setCraftingGrid") {
            @Override
            public Object[] call(Object[] args) {
                this.requireArgs(args, 9, "9 x item_name");
                String[] grid = new String[9];
                for (int i = 0; i < 9; i++) {
                    grid[i] = (String) args[i];
                }
                TileEntityDroneInterface.this.getWidget().setCraftingGrid(grid);
                TileEntityDroneInterface.this.messageToDrone(0xFFFFFFFF);
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("setVariable") {
            @Override
            public Object[] call(Object[] args) {
                this.requireArgs(args, new int[]{2, 4}, "<string> var_name, true/false OR <string> var_name, <int> x, <int> y, <int> z");
                EntityDrone d = TileEntityDroneInterface.this.validateAndGetDrone();
                String varName = (String) args[0];
                int x = args[1] instanceof Double ? ((Double) args[1]).intValue() : (Boolean) args[1] ? 1 : 0;
                int y = 0;
                int z = 0;
                if (args.length == 4) {
                    y = ((Double) args[2]).intValue();
                    z = ((Double) args[3]).intValue();
                }
                d.setVariable(varName, new BlockPos(x, y, z));
                TileEntityDroneInterface.this.messageToDrone(0xFFFFFFFF);
                return null;
            }

        });

        this.registerLuaMethod(new LuaMethod("getVariable") {
            @Override
            public Object[] call(Object[] args) {
                this.requireArgs(args, 1, "<string> var_name");
                BlockPos var = TileEntityDroneInterface.this.validateAndGetDrone().getVariable((String) args[0]);
                TileEntityDroneInterface.this.messageToDrone(0xFFFFFFFF);
                return new Object[]{var.getX(), var.getY(), var.getZ()};
            }
        });

        this.registerLuaMethod(new LuaMethod("setSignText") {
            @Override
            public Object[] call(Object[] args) {
                TileEntityDroneInterface.this.getWidget().signText = new String[args.length];
                for (int i = 0; i < args.length; i++) {
                    TileEntityDroneInterface.this.getWidget().signText[i] = (String) args[i];
                }
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("setRequiresTool") {
            @Override
            public Object[] call(Object[] args) {
                this.requireArgs(args, 1, "<boolean> require_tool");
                TileEntityDroneInterface.this.getWidget().setRequiresTool((Boolean) args[0]);
                TileEntityDroneInterface.this.messageToDrone(0xFFFFFFFF);
                return null;
            }
        });

        this.registerLuaMethod(new LuaMethod("getDroneName") {
            @Override
            public Object[] call(Object[] args) {
                this.requireNoArgs(args);
                return new Object[]{TileEntityDroneInterface.this.validateAndGetDrone().getName()};
            }
        });

        this.registerLuaMethod(new LuaMethod("getOwnerName") {
            @Override
            public Object[] call(Object[] args) {
                this.requireNoArgs(args);
                return new Object[]{TileEntityDroneInterface.this.validateAndGetDrone().getPlayerName()};
            }
        });

        this.registerLuaMethod(new LuaMethod("getOwnerID") {
            @Override
            public Object[] call(Object[] args) {
                this.requireNoArgs(args);
                return new Object[]{TileEntityDroneInterface.this.validateAndGetDrone().getOwnerUUID()};
            }
        });
    }

    private void registerLuaMethod(ILuaMethod method) {
        this.luaMethodRegistry.registerLuaMethod(method);
    }

    @Override
    public String getType() {
        return "droneInterface";
    }

    @Override
    public String getComponentName() {
        return this.getType();
    }

    @Override
    public String[] getMethodNames() {
        return this.luaMethodRegistry.getMethodNames();
    }

    @Override
    public String[] methods() {
        return this.getMethodNames();
    }

    @Override
    @Optional.Method(modid = ModIds.OPEN_COMPUTERS)
    public Object[] invoke(String method, Context context, Arguments args) throws Exception {
        if ("greet".equals(method)) {
            return new Object[]{String.format("Hello, %s!", args.checkString(0))};
        }
        return this.luaMethodRegistry.getMethod(method).call(args.toArray());
    }

    @Override
    @Optional.Method(modid = ModIds.COMPUTERCRAFT)
    public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) throws LuaException {
        try {
            return this.luaMethodRegistry.getMethod(method).call(arguments);
        } catch (Exception e) {
            throw new LuaException(e.getMessage());
        }
    }

    @Override
    @Optional.Method(modid = ModIds.COMPUTERCRAFT)
    public void attach(IComputerAccess computer) {
        this.attachedComputers.add(computer);
    }

    @Override
    @Optional.Method(modid = ModIds.COMPUTERCRAFT)
    public void detach(IComputerAccess computer) {
        this.attachedComputers.remove(computer);
    }

    @Override
    @Optional.Method(modid = ModIds.COMPUTERCRAFT)
    public boolean equals(IPeripheral other) {
        if (other == null) {
            return false;
        }
        if (this == other) {
            return true;
        }
        if (other instanceof TileEntity) {
            TileEntity tother = (TileEntity) other;
            return tother.getWorld().equals(this.getWorld()) && tother.getPos().equals(this.getPos());
        }

        return false;
    }

    private void sendEvent(String name, Object... parms) {
        if (Loader.isModLoaded(ModIds.COMPUTERCRAFT)) {
            for (IComputerAccess computer : this.attachedComputers) {
                computer.queueEvent(name, parms);
            }
        }
    }

    public void setDrone(EntityDrone drone) {
        this.drone = drone;
        this.sendEvent(drone != null ? "droneConnected" : "droneDisconnected");
        IBlockState state = this.getWorld().getBlockState(this.getPos());
        this.getWorld().notifyBlockUpdate(this.getPos(), state, state, 3);
    }

    public EntityDrone getDrone() {
        return this.drone;
    }

    private ProgWidgetCC getWidget() {
        return this.getAI().getWidget();
    }

    private DroneAICC getAI() {
        if (this.drone != null) {
            for (EntityAITaskEntry task : this.drone.getRunningTasks()) {
                if (task.action instanceof DroneAICC) {
                    return (DroneAICC) task.action;
                }
            }
        }
        // shouldn't get here under normal circumstances : drone is connected but somehow isn't running the CC piece
        this.setDrone(null);
        throw new IllegalStateException("There's no connected Drone!");
    }

    private DroneAICC getTargetAI() {
        if (this.drone != null && this.drone.getRunningTargetAI() instanceof DroneAICC) {
            return (DroneAICC) this.drone.getRunningTargetAI();
        } else {
            // shouldn't get here under normal circumstances : drone is connected but somehow isn't running the CC piece
            this.setDrone(null);
            throw new IllegalStateException("There's no connected Drone!");
        }
    }

    private void messageToDrone(Class<? extends IProgWidget> widget) {
        this.messageToDrone(ItemDye.DYE_COLORS[ItemProgrammingPuzzle.getWidgetForClass(widget).getCraftingColorIndex()]);
    }

    private void messageToDrone(int color) {
        this.ringSendQueue.offer(color);
    }

}
