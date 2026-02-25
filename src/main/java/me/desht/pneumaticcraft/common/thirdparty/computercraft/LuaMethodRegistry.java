package me.desht.pneumaticcraft.common.thirdparty.computercraft;

import org.apache.commons.lang3.Validate;

import java.util.*;

public class LuaMethodRegistry {
    private final List<ILuaMethod> luaMethods = new ArrayList<>();
    private final Map<String, Integer> luaMethodMap = new HashMap<>();  // index into luaMethods list
    private String[] luaMethodNames = null;

    public void registerLuaMethod(ILuaMethod method) {
        Integer idx = this.luaMethodMap.get(method.getMethodName());

        if (idx == null) {
            // add new
            this.luaMethods.add(method);
            this.luaMethodMap.put(method.getMethodName(), this.luaMethods.size() - 1);
        } else {
            // override previous
            this.luaMethods.set(idx, method);
        }
    }

    public String[] getMethodNames() {
        if (this.luaMethodNames == null) {
            this.luaMethodNames = new String[this.luaMethods.size()];
            Arrays.setAll(this.luaMethodNames, i -> this.luaMethods.get(i).getMethodName());
        }
        return this.luaMethodNames;
    }

    public ILuaMethod getMethod(String methodName) {
        Validate.isTrue(this.luaMethodMap.containsKey(methodName), "Attempt to get unregistered method '" + methodName + "'.");
        return this.luaMethods.get(this.luaMethodMap.get(methodName));
    }

    public ILuaMethod getMethod(int methodIndex) {
        return this.luaMethods.get(methodIndex);
    }

    public boolean isInited() {
        return !this.luaMethods.isEmpty();
    }
}
