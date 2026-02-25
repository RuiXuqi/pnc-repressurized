package me.desht.pneumaticcraft.client.model;

import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.common.FMLLog;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public enum TintedOBJLoader implements ICustomModelLoader {
    INSTANCE;

    private final Set<String> enabledDomains = new HashSet<>();
    private IResourceManager manager;
    private final Map<ResourceLocation, TintedOBJModel> cache = new HashMap<>();
    private final Map<ResourceLocation, Exception> errors = new HashMap<>();

    public void addDomain(String domain) {
        this.enabledDomains.add(domain.toLowerCase());
        FMLLog.log.info("OBJLoader: Domain {} has been added.", domain.toLowerCase());
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        this.manager = resourceManager;
        this.cache.clear();
        this.errors.clear();
    }

    @Override
    public boolean accepts(ResourceLocation modelLocation) {
        return this.enabledDomains.contains(modelLocation.getNamespace()) && modelLocation.getPath().endsWith(".obj");
    }

    @Override
    public IModel loadModel(ResourceLocation modelLocation) throws Exception {
        ResourceLocation file = new ResourceLocation(modelLocation.getNamespace(), modelLocation.getPath());
        if (!this.cache.containsKey(file)) {
            IResource resource;
            try {
                resource = this.manager.getResource(file);
            } catch (FileNotFoundException e) {
                if (modelLocation.getPath().startsWith("models/block/"))
                    resource = this.manager.getResource(new ResourceLocation(file.getNamespace(), "models/item/" + file.getPath().substring("models/block/".length())));
                else if (modelLocation.getPath().startsWith("models/item/"))
                    resource = this.manager.getResource(new ResourceLocation(file.getNamespace(), "models/block/" + file.getPath().substring("models/item/".length())));
                else throw e;
            }
            TintedOBJModel.Parser parser = new TintedOBJModel.Parser(resource, this.manager);
            TintedOBJModel model = null;
            try {
                model = parser.parse();
            } catch (Exception e) {
                this.errors.put(modelLocation, e);
            } finally {
                this.cache.put(modelLocation, model);
            }
        }
        TintedOBJModel model = this.cache.get(file);
        if (model == null)
            throw new ModelLoaderRegistry.LoaderException("Error loading model previously: " + file, this.errors.get(modelLocation));
        return model;
    }
}
