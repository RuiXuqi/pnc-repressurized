package me.desht.pneumaticcraft.common.config;

import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Set;

public class HelmetWidgetDefaults extends JsonConfig {
    private final Set<String> keyBinds = Sets.newHashSet();

    public static final HelmetWidgetDefaults INSTANCE = new HelmetWidgetDefaults();

    private HelmetWidgetDefaults() {
        super(true);
    }

    @Override
    public String getConfigFilename() {
        return "HelmetWidgetDefaults";
    }

    @Override
    protected void writeToJson(JsonObject json) {
        json.addProperty("description", "Tracks the active upgrades for the Pneumatic Armor");
        JsonArray array = new JsonArray();
        if (this.keyBinds.isEmpty()) {
            // armor is on by default
            this.keyBinds.add("pneumaticHelmet.upgrade.coreComponents");
        }
        for (String s : this.keyBinds) {
            array.add(s);
        }
        json.add("active", array);
    }

    @Override
    protected void readFromJson(JsonObject json) {
        JsonArray array = json.get("active").getAsJsonArray();
        this.keyBinds.clear();
        for (JsonElement element : array) {
            this.keyBinds.add(element.getAsString());
        }
    }

    public void setKey(String keyBindName, boolean checked) {
        if (checked) {
            this.keyBinds.add(keyBindName);
        } else {
            this.keyBinds.remove(keyBindName);
        }
    }

    public boolean getKey(String keyBindingName) {
        return this.keyBinds.contains(keyBindingName);
    }
}
