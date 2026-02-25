package me.desht.pneumaticcraft.common.thirdparty.computercraft;

import net.minecraft.util.EnumFacing;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.util.LinkedHashMap;
import java.util.List;

public abstract class LuaMethod implements ILuaMethod {
    private final String methodName;

    protected LuaMethod(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public String getMethodName() {
        return this.methodName;
    }

    protected EnumFacing getDirForString(String luaParm) {
        for (EnumFacing dir : EnumFacing.VALUES) {
            if (dir.toString().equalsIgnoreCase(luaParm)) {
                return dir;
            }
        }
        throw new IllegalArgumentException("Side must be one of: up, down, north, east, south or west!");
    }

    LinkedHashMap<Integer, String> getStringTable(List<String> list) {
        LinkedHashMap<Integer, String> table = new LinkedHashMap<>();
        for (int i = 0; i < list.size(); i++) {
            table.put(i + 1, list.get(i));
        }
        return table;
    }

    protected void requireArgs(Object[] args, int min, int max, String desc) {
        Validate.isTrue(args.length >= min && args.length <= max,
                String.format("Method '%s' takes between %d and %d arguments! (%s)", this.getMethodName(), min, max, desc));
    }

    protected void requireArgs(Object[] args, int len, String desc) {
        Validate.isTrue(args.length == len,
                String.format("Method '%s' takes exactly %d arguments! (%s)", this.getMethodName(), len, desc));
    }

    protected void requireArgs(Object[] args, int[] argcount, String desc) {
        for (int a : argcount) {
            if (args.length == a) return;
        }

        throw new IllegalArgumentException(String.format("Method '%s' takes either %s arguments! (%s)",
                this.getMethodName(), StringUtils.join(ArrayUtils.toObject(argcount), " or "), desc));
    }

    protected void requireNoArgs(Object[] args) {
        Validate.isTrue(args.length == 0, String.format("Method '%s' takes no arguments!", this.getMethodName()));
    }
}
