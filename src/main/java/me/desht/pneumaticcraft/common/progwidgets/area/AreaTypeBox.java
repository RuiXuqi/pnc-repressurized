package me.desht.pneumaticcraft.common.progwidgets.area;

import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetArea.EnumAreaType;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.function.Consumer;

public class AreaTypeBox extends AreaType {

    public static final String ID = "box";

    private EnumBoxType boxType = EnumBoxType.FILLED;

    private enum EnumBoxType {
        FILLED("filled"), HOLLOW("hollow"), FRAME("frame");

        private final String name;

        EnumBoxType(String name) {
            this.name = "gui.progWidget.area.type.box.boxType." + name;
        }

        @Override
        public String toString() {
            return I18n.format(this.name);
        }
    }

    public AreaTypeBox() {
        super(ID);
    }

    @Override
    public void addArea(Consumer<BlockPos> areaAdder, BlockPos p1, BlockPos p2, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        switch (this.boxType) {
            case FILLED:
                for (int x = minX; x <= maxX; x++) {
                    for (int y = Math.min(255, maxY); y >= minY && y >= 0; y--) {
                        for (int z = minZ; z <= maxZ; z++) {
                            areaAdder.accept(new BlockPos(x, y, z));
                        }
                    }
                }
                break;
            case FRAME:
                for (int x = minX; x <= maxX; x++) {
                    for (int y = Math.max(0, minY); y <= maxY && y < 256; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            int axisRight = 0;
                            if (x == minX || x == maxX) axisRight++;
                            if (y == minY || y == maxY) axisRight++;
                            if (z == minZ || z == maxZ) axisRight++;
                            if (axisRight > 1) {
                                areaAdder.accept(new BlockPos(x, y, z));
                            }
                        }
                    }
                }
                break;
            case HOLLOW:
                for (int x = minX; x <= maxX; x++) {
                    for (int y = Math.max(0, minY); y <= maxY && y < 256; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            if (x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ) {
                                areaAdder.accept(new BlockPos(x, y, z));
                            }
                        }
                    }
                }
                break;
            default:
                throw new IllegalArgumentException(this.boxType.toString());
        }
    }

    @Override
    public void addUIWidgets(List<AreaTypeWidget> widgets) {
        super.addUIWidgets(widgets);
        widgets.add(new AreaTypeWidgetEnum<>("gui.progWidget.area.type.box.boxType", EnumBoxType.class, () -> this.boxType, boxType -> this.boxType = boxType));
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setByte("boxType", (byte) this.boxType.ordinal());
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.boxType = EnumBoxType.values()[tag.getByte("boxType")];
    }

    @Override
    public void convertFromLegacy(EnumAreaType oldAreaType, int typeInfo) {
        switch (oldAreaType) {
            case FILL:
                this.boxType = EnumBoxType.FILLED;
                break;
            case WALL:
                this.boxType = EnumBoxType.HOLLOW;
                break;
            case FRAME:
                this.boxType = EnumBoxType.FRAME;
                break;
            default:
                throw new IllegalArgumentException();
        }
    }
}
