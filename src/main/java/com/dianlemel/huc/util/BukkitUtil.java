package com.dianlemel.huc.util;

import com.dianlemel.huc.UHCException;
import com.google.common.collect.Lists;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.projectiles.ProjectileSource;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BukkitUtil {

    private static final Random random = new Random();

    public static void test(int limit) {
        StackTraceElement[] st = Thread.getAllStackTraces().get(Thread.currentThread());
        List<String> s = Stream.of(st).skip(3).limit(limit).map(StackTraceElement::toString).collect(Collectors.toList());
        int cnt = 0;
        for (String ss : s) {
            System.out.println(cnt++ + " " + ss);
        }
    }

    public static void test(String msg) {
        try {
            throw new Exception(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showEvents(HandlerList h) {
        Lists.newArrayList(h.getRegisteredListeners()).stream().map(RegisteredListener::getListener)
                .forEach(System.out::println);
    }

    public static void setHealth(Player player, double h) {
        Optional.ofNullable(player).ifPresent(p -> {
            var health = p.getHealth();
            health += h;
            if (health > 20) {
                health = 20;
            }
            if (health < 0) {
                health = 0;
            }
            p.setHealth(health);
        });
    }

    public static <T> T random(List<T> list) {
        if (list.size() == 1) {
            return list.get(0);
        }
        return list.get(random.nextInt(list.size()));
    }

    public static List<Player> getPlayers(World world, GameMode... mode) {
        List<GameMode> modes = Lists.newArrayList(mode);
        return getPlayers(world).stream().filter(p -> modes.contains(p.getGameMode())).collect(Collectors.toList());
    }

    public static List<Player> getPlayers(World world) {
        return Lists.newArrayList(Bukkit.getOnlinePlayers()).stream().filter(p -> p.getWorld().equals(world))
                .collect(Collectors.toList());
    }

    public static boolean isMainThread() {
        return Bukkit.isPrimaryThread();
    }

    public static void runMainThread(Runnable runnable) {
        if (!isMainThread()) {
            TaskUtil.syncTask(runnable);
            return;
        }
        runnable.run();
    }

    public static Plugin getPlugin(String name) throws Exception {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
        if (plugin == null) {
            throw new UHCException("尚未安裝 " + name);
        }
        return plugin;
    }

    public static Entity getDamager(Entity entity) {
        if (entity instanceof Projectile projectile) {
            ProjectileSource ps = projectile.getShooter();
            if (ps instanceof LivingEntity livingEntity) {
                return livingEntity;
            }
            return null;
        }
        return entity;
    }

//    public static void stopSound(Location loc, String sound, SoundCategory category) {
//        PacketPlayOutStopSound packet = new PacketPlayOutStopSound(new MinecraftKey(sound),
//                category == null ? BSoundCategory.MASTER
//                        : net.minecraft.sounds.SoundCategory.valueOf(category.name()));
//        sendPacketNearby(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), (float) 1.0, packet);
//    }
//
//    public static void playSound(Location loc, String sound, SoundCategory category) {
//        PacketPlayOutCustomSoundEffect packet = new PacketPlayOutCustomSoundEffect(new MinecraftKey(sound),
//                net.minecraft.sounds.SoundCategory.valueOf(category.name()),
//                new Vec3D(loc.getX(), loc.getY(), loc.getZ()), 1.0f, 1.0f / (random.nextFloat() * 0.4f + 0.8f));
//        sendPacketNearby(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), (float) 1.0, packet);
//    }
//
//    public static void sendPacketNearby(World world, double x, double y, double z, float range, Packet<?> packet) {
//        var minecraftServer = getMinecraftServer();
//        var playerList = minecraftServer.getPlayerList();
//        var worldServer = new BWorld(getWorldServer(world));
//        playerList.broadcast(
//                null, x, y, z,
//                range > 1.0f ? (double) (16.0f * range) : 16.0,
//                worldServer.dimension(), packet);
//    }

    public static void playSoundToAll(Sound arg1, SoundCategory arg2, float arg3, float arg4) {
        Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), arg1, arg2, arg3, arg4));
    }

//    public static BMinecraftServer getMinecraftServer() {
//        var server = Bukkit.getServer();
//        var craftServer = (CraftServer) server;
//        return new BMinecraftServer(craftServer.getServer());
//    }
//
//    public static UUID getPlayer(String name) {
//        var minecraft = getMinecraftServer();
//        var userCache = minecraft.getUserCache();
//        return userCache.getProfile(name)
//                .map(GameProfile::getId).orElse(null);
//    }

//    public static boolean isOp(UUID uuid) {
//        var minecraft = getMinecraftServer();
//        var playerList = minecraft.getPlayerList();
//        return getGameProfile(uuid)
//                .map(gameProfile -> playerList.isOp(gameProfile))
//                .orElse(false);
//    }

//    public static Optional<GameProfile> getGameProfile(UUID uuid) {
//        var minecraft = getMinecraftServer();
//        var userCache = minecraft.getUserCache();
//        return userCache.getProfile(uuid);
//    }
//
//    public static Optional<GameProfile> getGameProfile(String name) {
//        var minecraft = getMinecraftServer();
//        var userCache = minecraft.getUserCache();
//        return userCache.getProfile(name);
//    }
//
//    public static String getPlayerName(UUID uuid) {
//        return getGameProfile(uuid).map(GameProfile::getName).orElse("Unknown");
//    }
//
//    public static EntityPlayer getNMSPlayer(Entity entity) {
//        if (!entity.getType().equals(EntityType.PLAYER)) {
//            return null;
//        }
//        var craftPlayer = (CraftPlayer) entity;
//        return craftPlayer.getHandle();
//    }
//
//    public static EntityPlayer getNMSPlayer(Player player) {
//        if (player == null) {
//            return null;
//        }
//        var craftPlayer = (CraftPlayer) player;
//        return craftPlayer.getHandle();
//    }
//
//    public static net.minecraft.world.entity.Entity getNMSEntity(Entity entity) {
//        if (entity == null) {
//            return null;
//        }
//        var craftEntity = (CraftEntity) entity;
//        return craftEntity.getHandle();
//    }
//
//    public static void sendPacket(Player player, Packet<?> packet) {
//        if (player == null) {
//            return;
//        }
//        new BEntityPlayer(getNMSPlayer(player)).sendPacket(packet);
//    }
//
//    public static BWorldServer getWorldServer(World world) {
//        return new BWorldServer(((CraftWorld) world).getHandle());
//    }
//
//    public static void sendAllMessage(IChatBaseComponent[] ichatbasecomponent) {
//        var minecraft = getMinecraftServer();
//        var playerList = minecraft.getPlayerList();
//        playerList.getRoot().broadcastMessage(ichatbasecomponent);
//    }
//
//    public static void sendAllPacket(Packet<?> packet) {
//        Bukkit.getOnlinePlayers().forEach(player -> sendPacket(player, packet));
//    }

}
