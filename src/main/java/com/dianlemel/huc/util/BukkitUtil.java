package com.dianlemel.huc.util;

import com.dianlemel.huc.UHCException;
import com.google.common.collect.Lists;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

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

    public static void playSoundToAll(Sound arg1, SoundCategory arg2, float arg3, float arg4) {
        Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), arg1, arg2, arg3, arg4));
    }

}
