package org.dynmap.fabric_1_14_1;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.dynmap.DynmapChunk;
import org.dynmap.DynmapCore;
import org.dynmap.DynmapWorld;
import org.dynmap.common.DynmapListenerManager;
import org.dynmap.common.DynmapPlayer;
import org.dynmap.common.DynmapServerInterface;
import org.dynmap.utils.MapChunkCache;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FabricServerInterface extends DynmapServerInterface {
    private static final Pattern patternControlCode = Pattern.compile("(?i)\\u00A7[0-9A-FK-OR]");
    private final MinecraftServer server;
    private final Long2ObjectMap<List<Runnable>> tickTaskMap = new Long2ObjectOpenHashMap<>();
    private final WeakHashMap<World, FabricWorld> fabricWorldMap = new WeakHashMap<>();

    public FabricServerInterface(MinecraftServer server) {
        this.server = server;
    }

    private FabricWorld getWorld(World world) {
        return fabricWorldMap.computeIfAbsent(world, FabricWorld::new);
    }

    void init(DynmapCore core) {
        for (ServerWorld world : server.getWorlds()) {
            core.processWorldLoad(getWorld(world));
        }
    }

    private GameProfile getProfileByName(String player) {
        return server.getUserCache().findByName(player);
    }

    @Override
    public void scheduleServerTask(Runnable run, long delay) {
        if (delay < 0) delay = 0;

        long tick = server.getTicks() + delay;
        tickTaskMap.computeIfAbsent(tick, (a) -> new ArrayList<>()).add(run);
    }

    void tick() {
        List<Runnable> list = tickTaskMap.remove(server.getTicks());
        if (list != null) {
            for (Runnable r : list) {
                r.run();
            }
        }
    }

    @Override
    public <T> Future<T> callSyncMethod(Callable<T> task) {
        return server.executeFuture(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    @Override
    public DynmapPlayer[] getOnlinePlayers() {
        return new DynmapPlayer[0]; // TODO
    }

    @Override
    public void reload() {
        server.reload();
    }

    @Override
    public DynmapPlayer getPlayer(String name) {
        return null; // TODO
    }

    @Override
    public DynmapPlayer getOfflinePlayer(String name) {
        return null; //TODO
    }

    @Override
    public Set<String> getIPBans() {
        return new HashSet<>(Arrays.asList(server.getPlayerManager().getIpBanList().getNames()));
    }

    @Override
    public String getServerName() {
        return server.getLevelName();
    }

    @Override
    public boolean isPlayerBanned(String pid) {
        return server.getPlayerManager().getUserBanList().contains(getProfileByName(pid));
    }

    @Override
    public String stripChatColor(String s) {
        return patternControlCode.matcher(s).replaceAll("");
    }

    @Override
    public boolean requestEventNotification(DynmapListenerManager.EventType type) {
        // TODO
        return true;
    }

    @Override
    public boolean sendWebChatEvent(String source, String name, String msg) {
        // TODO
        return false;
    }

    @Override
    public void broadcastMessage(String msg) {
        server.sendMessage(new TextComponent("[Dynmap] " + msg));
    }

    @Override
    public String[] getBiomeIDs() {
        return Registry.BIOME.getIds().stream()
                .map(Identifier::toString)
                .toArray(String[]::new);
    }

    @Override
    public double getCacheHitRate() {
        return 0; // TODO
    }

    @Override
    public void resetCacheStats() {
        // TODO
    }

    @Override
    public DynmapWorld getWorldByName(String wname) {
        return getWorld(server.getWorld(Registry.DIMENSION.get(new Identifier(wname))));
    }

    @Override
    public Set<String> checkPlayerPermissions(String player, Set<String> perms) {
        return Collections.emptySet(); // TODO
    }

    @Override
    public boolean checkPlayerPermission(String player, String perm) {
        return true; // TODO
    }

    @Override
    public MapChunkCache createMapChunkCache(DynmapWorld w, List<DynmapChunk> chunks, boolean blockdata, boolean highesty, boolean biome, boolean rawbiome) {
        FabricMapChunkCache cache = new FabricMapChunkCache((FabricWorld) w, ((FabricWorld) w).world, chunks);
        cache.setChunkDataTypes(blockdata, biome, highesty, rawbiome);
        return cache;
    }

    @Override
    public int getMaxPlayers() {
        return server.getMaxPlayerCount();
    }

    @Override
    public int getCurrentPlayers() {
        return server.getCurrentPlayerCount();
    }

    @Override
    public int getBlockIDAt(String wname, int x, int y, int z) {
        return -1;
    }

    @Override
    public double getServerTPS() {
        return 20.0; // TODO
    }

    @Override
    public String getServerIP() {
        return server.getServerIp();
    }
}
