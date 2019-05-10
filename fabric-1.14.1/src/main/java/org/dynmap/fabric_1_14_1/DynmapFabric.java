package org.dynmap.fabric_1_14_1;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.ModContainer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.util.UrlConversionException;
import net.fabricmc.loader.util.UrlUtil;
import net.minecraft.server.MinecraftServer;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;
import org.dynmap.DynmapCore;
import org.dynmap.common.BiomeMap;
import org.dynmap.markers.MarkerAPI;

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.Path;

public class DynmapFabric implements DedicatedServerModInitializer {
    private static DynmapCore core;
    private static String mcVer;
    private static FabricServerInterface serverInterface;
    private static DynmapFabric instance;

    @Override
    public void onInitializeServer() {
        instance = this;

        mcVer = "1.14";
        File jarfile = null;

        // TODO: de-non-API-ize
        ModContainer modContainer = ((ModContainer) FabricLoader.getInstance().getModContainer("dynmap").get());
        try {
            jarfile = UrlUtil.asFile(modContainer.getOriginUrl());
        } catch (UrlConversionException e) {
            e.printStackTrace();
        }

        File dataDirectory = new File("dynmap");
        if (!dataDirectory.exists()) {
            dataDirectory.mkdirs();
        }

        core = new DynmapCore();
        core.setPluginJarFile(jarfile);
        core.setPluginVersion(modContainer.getMetadata().getVersion().getFriendlyString(), "Fabric");
        core.setMinecraftVersion(mcVer); // TODO
        core.setDataFolder(dataDirectory);
        core.setServer(serverInterface = new FabricServerInterface((MinecraftServer) FabricLoader.getInstance().getGameInstance()));

        if (!core.initConfiguration(null)) {
            throw new RuntimeException("DynmapCore.initConfiguration failed!");
        }

        DynmapCommonAPIListener.apiInitialized(core);
    }

    public static void stop() {
        DynmapCommonAPIListener.apiTerminated();

        core.disableCore();
    }

    public static void tick() {
        serverInterface.tick();
    }

    public static void start() {
        FabricBlockStateMapper.INSTANCE.init();
        BiomeMap.loadWellKnownByVersion(mcVer);
        // TODO: custom biome support

        if (!core.enableCore(null)) {
            throw new RuntimeException("DynmapCore.enableCore failed!");
        }

        serverInterface.init(core);

        DynmapCommonAPIListener.apiInitialized(core);
    }
}
