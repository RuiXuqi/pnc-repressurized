package me.desht.pneumaticcraft.common.config;

import com.google.common.base.Charsets;
import com.google.gson.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public abstract class JsonConfig implements ISubConfig {
    protected File file;
    private final boolean inPreInit;

    public JsonConfig(boolean inPreInit) {
        this.inPreInit = inPreInit;
    }

    @Override
    public void preInit(File file) throws IOException {
        this.file = file;
        if (this.inPreInit) {
            this.processFile();
        }
    }

    @Override
    public void postInit() throws IOException {
        if (!this.inPreInit) {
            this.processFile();
        }
    }

    private void processFile() throws IOException {
        if (this.file.exists()) {
            this.readFromFile();
            this.writeToFile();
        } else {
            if (this.file.createNewFile()) {
                this.writeToFile();
            }
        }
    }

    public void writeToFile() throws IOException {
        JsonObject root = new JsonObject();
        this.writeToJson(root);
        String jsonString = root.toString();

        JsonParser parser = new JsonParser();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        JsonElement el = parser.parse(jsonString);
        FileUtils.write(this.file, gson.toJson(el), Charsets.UTF_8);
    }

    protected void readFromFile() throws IOException {
        JsonParser parser = new JsonParser();
        JsonObject root = (JsonObject) parser.parse(FileUtils.readFileToString(this.file, Charsets.UTF_8));
        this.readFromJson(root);
    }

    protected abstract void writeToJson(JsonObject json);

    protected abstract void readFromJson(JsonObject json);
}
