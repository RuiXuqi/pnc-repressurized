package me.desht.pneumaticcraft.common.block.tubes;

import me.desht.pneumaticcraft.client.model.module.ModelModuleBase;
import me.desht.pneumaticcraft.client.model.module.ModelRedstone;
import me.desht.pneumaticcraft.common.GuiHandler;
import me.desht.pneumaticcraft.common.config.ConfigHandler;
import me.desht.pneumaticcraft.common.item.ItemPneumaticWrench;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketOpenTubeModuleGui;
import me.desht.pneumaticcraft.common.network.PacketSyncRedstoneModuleToClient;
import me.desht.pneumaticcraft.common.thirdparty.ModdedWrenchUtils;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.Names;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.oredict.DyeUtils;

import java.util.List;
import java.util.OptionalInt;
import java.util.stream.IntStream;

// since this is both a receiver and an emitter, we won't use either redstone superclass here
public class ModuleRedstone extends TubeModule implements INetworkedModule {
    private EnumRedstoneDirection redstoneDirection = EnumRedstoneDirection.OUTPUT;
    private int inputLevel = -1;
    private int outputLevel;
    private int colorChannel;
    private Operation operation = Operation.PASSTHROUGH;
    private boolean invert = false;
    private int otherColor = 0;   // for advanced modules
    private int constantVal = 0;  // for advanced modules
    private final byte[] prevLevels = new byte[16];

    // for client-side rendering the redstone connector
    public float extension = 1.0f;
    public float lastExtension;

    @Override
    public String getType() {
        return Names.MODULE_REDSTONE;
    }

    @Override
    protected GuiHandler.EnumGuiId getGuiId() {
        return GuiHandler.EnumGuiId.REDSTONE_MODULE;
    }

    @Override
    public Class<? extends ModelModuleBase> getModelClass() {
        return ModelRedstone.class;
    }

    @Override
    public void onNeighborBlockUpdate() {
        this.updateInputLevel();
    }

    @Override
    public double getWidth() {
        return 0.5;
    }

    @Override
    protected double getHeight() {
        return 0.25;
    }

    @Override
    public void update() {
        super.update();

        if (!this.pressureTube.world().isRemote) {
            byte[] levels = new byte[16];

            if (this.redstoneDirection == EnumRedstoneDirection.OUTPUT) {
                for (TubeModule module : ModuleNetworkManager.getInstance(this.getTube().world()).getConnectedModules(this)) {
                    if (module instanceof ModuleRedstone) {
                        ModuleRedstone mr = (ModuleRedstone) module;
                        if (mr.getRedstoneDirection() == EnumRedstoneDirection.INPUT && mr.getInputLevel() > levels[mr.getColorChannel()])
                            levels[mr.getColorChannel()] = (byte) mr.inputLevel;
                    }
                }

                int out = this.computeOutputSignal(this.outputLevel, levels);
                if (this.invert) out = out > 0 ? 0 : 15;
                if (this.setOutputLevel(out)) {
                    NetworkHandler.sendToAllAround(new PacketSyncRedstoneModuleToClient(this), this.getTube().world());
                }
            } else {
                if (this.inputLevel < 0) this.updateInputLevel();  // first update
            }
            System.arraycopy(levels, 0, this.prevLevels, 0, 16);
        } else {
            this.lastExtension = this.extension;
            if (this.redstoneDirection == EnumRedstoneDirection.OUTPUT) {
                this.extension = Math.min(1.0f, this.extension + 0.125f);
            } else {
                this.extension = Math.max(0.0f, this.extension - 0.125f);
            }
        }
    }

    private int computeOutputSignal(int lastOutput, byte[] levels) {
        byte s1 = levels[this.getColorChannel()];
        byte s2 = levels[this.otherColor];

        switch (this.operation) {
            case PASSTHROUGH:
                return s1;
            case AND:
                return s1 > 0 && s2 > 0 ? 15 : 0;
            case OR:
                return s1 > 0 || s2 > 0 ? 15 : 0;
            case XOR:
                return s1 == 0 && s2 == 0 || s1 > 0 && s2 > 0 ? 0 : 15;
            case COMPARATOR:
                return s1 > s2 ? 15 : 0;
            case SUBTRACT:
                return MathHelper.clamp(s1 - s2, 0, 15);
            case COMPARE:
                return s1 > this.constantVal ? 15 : 0;
            case CLOCK:
                return s1 == 0 && this.getTube().world().getTotalWorldTime() % this.constantVal < 2 ? 15 : 0;
            case TOGGLE:
                if (s1 > this.prevLevels[this.getColorChannel()]) {
                    return lastOutput > 0 ? 0 : 15;
                } else {
                    return lastOutput;
                }
            case CONSTANT:
                return MathHelper.clamp(this.constantVal, 0, 15);
            case COUNTER:
                if (s1 > this.prevLevels[this.getColorChannel()]) {
                    lastOutput++;
                    return lastOutput > Math.min(15, this.constantVal) ? 0 : lastOutput;
                } else {
                    return lastOutput;
                }
            default:
                return 0;
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);

        tag.setBoolean("input", this.redstoneDirection == EnumRedstoneDirection.INPUT);
        tag.setByte("channel", (byte) this.colorChannel);
        tag.setByte("outputLevel", (byte) this.outputLevel);
        tag.setString("op", this.operation.toString());
        tag.setByte("color2", (byte) this.otherColor);
        tag.setByte("const", (byte) this.constantVal);
        tag.setBoolean("invert", this.invert);
        tag.setLong("prevLevels", this.encodeLevels(this.prevLevels));
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);

        this.redstoneDirection = tag.getBoolean("input") ? EnumRedstoneDirection.INPUT : EnumRedstoneDirection.OUTPUT;
        this.colorChannel = tag.getByte("channel");
        this.outputLevel = tag.getByte("outputLevel"); // for sync'ing to clients on login
        try {
            this.operation = tag.hasKey("op") ? Operation.valueOf(tag.getString("op")) : Operation.PASSTHROUGH;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            this.operation = Operation.PASSTHROUGH;
        }
        this.otherColor = tag.getByte("color2");
        this.constantVal = tag.getByte("const");
        this.invert = tag.getBoolean("invert");
        this.decodeLevels(tag.getLong("prevLevels"), this.prevLevels);
    }

    private long encodeLevels(byte[] l) {
        return IntStream.range(0, l.length)
                .mapToLong(i -> (long) l[i] << 4 * i)
                .reduce(0, (a, b) -> a | b);
    }

    private void decodeLevels(long l, byte[] res) {
        for (int i = 0; i < res.length; i++) {
            res[i] = (byte) (l >> (4 * i) | 0xf);
        }
    }

    public EnumRedstoneDirection getRedstoneDirection() {
        return this.redstoneDirection;
    }

    public void setRedstoneDirection(EnumRedstoneDirection redstoneDirection) {
        this.redstoneDirection = redstoneDirection;
    }

    @Override
    public int getRedstoneLevel() {
        return this.redstoneDirection == EnumRedstoneDirection.OUTPUT ? this.outputLevel : 0;
    }

    public boolean setOutputLevel(int level) {
        level = MathHelper.clamp(level, 0, 15);
        if (level != this.outputLevel) {
            this.outputLevel = level;
            this.updateNeighbors();
            return true;
        } else {
            return false;
        }
    }

    public int getInputLevel() {
        return this.inputLevel;
    }

    // for clientside use
    public void setInputLevel(int level) {
        this.inputLevel = level;
    }

    @Override
    public int getColorChannel() {
        return this.colorChannel;
    }

    @Override
    public void setColorChannel(int channel) {
        this.colorChannel = channel;
    }

    public boolean isInvert() {
        return this.invert;
    }

    public void setInvert(boolean invert) {
        this.invert = invert;
    }

    @Override
    public void addInfo(List<String> curInfo) {
        super.addInfo(curInfo);
        if (this.getRedstoneDirection() == EnumRedstoneDirection.INPUT) {
            curInfo.add("Receiving Redstone: " + TextFormatting.YELLOW + this.inputLevel);
        } else {
            curInfo.add("Emitting Redstone: " + TextFormatting.YELLOW + this.outputLevel);
            if (this.upgraded) this.addAdvancedInfo(curInfo);
        }
    }

    private void addAdvancedInfo(List<String> curInfo) {
        String s = "Operation: " + TextFormatting.YELLOW + PneumaticCraftUtils.xlate(this.operation.getTranslationKey()) + " ";
        if (this.operation.useOtherColor) {
            s += "(" + PneumaticCraftUtils.dyeColorDesc(this.otherColor) + ")";
        }
        if (this.operation.useConst) {
            s += "(" + this.constantVal + ")";
        }
        curInfo.add(s);
        curInfo.add("Output inverted: " + TextFormatting.YELLOW + (this.invert ? "Yes" : "No"));
    }

    @Override
    public boolean onActivated(EntityPlayer player, EnumHand hand) {
        ItemStack heldStack = player.getHeldItem(hand);
        OptionalInt colorIndex = DyeUtils.dyeDamageFromStack(heldStack);
        if (colorIndex.isPresent()) {
            this.setColorChannel(colorIndex.getAsInt());
            if (ConfigHandler.general.useUpDyesWhenColoring && !player.capabilities.isCreativeMode) {
                heldStack.shrink(1);
            }
            return true;
        } else if (heldStack.getItem() instanceof ItemPneumaticWrench || ModdedWrenchUtils.getInstance().isModdedWrench(heldStack)) {
            this.redstoneDirection = this.redstoneDirection == EnumRedstoneDirection.INPUT ? EnumRedstoneDirection.OUTPUT : EnumRedstoneDirection.INPUT;
            this.updateNeighbors();
            if (!this.updateInputLevel()) {
                NetworkHandler.sendToAllAround(new PacketSyncRedstoneModuleToClient(this), this.getTube().world());
            }
            return true;
        } else if (!this.getTube().world().isRemote && this.upgraded && this.getRedstoneDirection() == EnumRedstoneDirection.OUTPUT) {
            NetworkHandler.sendTo(new PacketOpenTubeModuleGui(this.getGuiId().ordinal(), this.pressureTube.pos()), (EntityPlayerMP) player);
            return true;
        }
        return false;
    }

    private boolean updateInputLevel() {
        int newInputLevel = this.redstoneDirection == EnumRedstoneDirection.INPUT ?
                this.pressureTube.world().getRedstonePower(this.pressureTube.pos().offset(this.getDirection()), this.getDirection()) : 0;
        if (newInputLevel != this.inputLevel) {
            this.inputLevel = newInputLevel;
            NetworkHandler.sendToAllAround(new PacketSyncRedstoneModuleToClient(this), this.getTube().world());
            return true;
        }
        return false;
    }

    public Operation getOperation() {
        return this.operation;
    }

    public int getOtherColor() {
        return this.otherColor;
    }

    public int getConstantVal() {
        return this.constantVal;
    }

    public void setOperation(Operation operation, int otherColor, int constantVal) {
        // 4 tick interval is smallest sensible value
        if (operation == Operation.CLOCK) constantVal = Math.max(4, constantVal);

        this.operation = operation;
        this.otherColor = otherColor;
        this.constantVal = constantVal;
    }

    public enum EnumRedstoneDirection {
        INPUT, OUTPUT
    }

    public enum Operation {
        PASSTHROUGH(false, false),
        AND(true, false),
        OR(true, false),
        XOR(true, false),
        CLOCK(false, true),
        COMPARATOR(true, false),
        SUBTRACT(true, false),
        COMPARE(false, true),
        TOGGLE(false, false),
        CONSTANT(false, true),
        COUNTER(false, true);

        private final boolean useOtherColor;
        private final boolean useConst;

        Operation(boolean useOtherColor, boolean useConst) {
            this.useOtherColor = useOtherColor;
            this.useConst = useConst;
        }

        public String getTranslationKey() {
            return "gui.redstoneModule.operation_" + this.toString().toLowerCase();
        }

        public boolean useOtherColor() {
            return this.useOtherColor;
        }

        public boolean useConst() {
            return this.useConst;
        }
    }
}
