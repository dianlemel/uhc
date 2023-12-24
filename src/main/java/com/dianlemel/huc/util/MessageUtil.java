package com.dianlemel.huc.util;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

public class MessageUtil {

    private static final String ERROR_PLAYER = "§0§l[§c錯誤§0] §r§f";
    private static final String INFO_PLAYER = "§0§l[§b資訊§0] §r§f";
    private static final String SYSTEM_PLAYER = "§0§l[§e系統§0] §r§f";

    private static final String ERROR_CONSOLE = "§f§l[§c錯誤§f] §r§7";
    private static final String INFO_CONSOLE = "§f§l[§b資訊§f] §r§7";
    private static final String SYSTEM_CONSOLE = "§f§l[§e系統§f] §r§7";

    public static void sendError(Object sender, String message) {
        send(sender, (sender instanceof ConsoleCommandSender ? ERROR_CONSOLE : ERROR_PLAYER) + message);
    }

    public static void sendInfo(World world, String message) {
        BukkitUtil.getPlayers(world).forEach(player -> {
            sendInfo(player, message);
        });
    }

    public static void sendInfo(Object sender, String message) {
        send(sender, (sender instanceof ConsoleCommandSender ? INFO_CONSOLE : INFO_PLAYER) + message);
    }

    public static void sendSystem(World world, String message) {
        BukkitUtil.getPlayers(world).forEach(player -> {
            sendSystem(player, message);
        });
    }

    public static void sendSystem(Object sender, String message) {
        send(sender, (sender instanceof ConsoleCommandSender ? SYSTEM_CONSOLE : SYSTEM_PLAYER) + message);
    }

    public static void sendError(World world, String message) {
        BukkitUtil.getPlayers(world).forEach(player -> {
            sendError(player, message);
        });
    }

    public static void sendError(Object sender, Throwable e) {
        while (e.getCause() != null) {
            e = e.getCause();
        }
        send(sender, e.getMessage());
    }

    public static void sendError(String message) {
        sendError(Bukkit.getConsoleSender(), message);
    }

    public static void sendError(Throwable e) {
        sendError(Bukkit.getConsoleSender(), e);
    }

    public static void sendInfo(String message) {
        sendInfo(Bukkit.getConsoleSender(), message);
    }

    public static void sendSystem(String message) {
        sendSystem(Bukkit.getConsoleSender(), message);
    }

    public static void sendHeading(CommandSender sender, String title) {
        send(sender, buildTitle(title));
    }

    public static void send(Object sender, String line) {
        if (!BukkitUtil.isMainThread()) {
            TaskUtil.syncTask(() -> send(sender, line));
            return;
        }
        if (sender instanceof CommandSender commandSender) {
            commandSender.sendMessage(line);
        }
    }

    public static void broadcastTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (!BukkitUtil.isMainThread()) {
            TaskUtil.syncTask(() -> broadcastTitle(title, subtitle, fadeIn, stay, fadeOut));
            return;
        }
        Bukkit.getServer().getOnlinePlayers().forEach(player -> {
            player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
        });
    }

    public static void sendActionBarToAll(BaseComponent... components) {
        for (var player : Bukkit.getOnlinePlayers()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, components);
        }
    }

    public static void broadcastInfo(String message) {
        Bukkit.broadcastMessage("§0§l[§b資訊§0] §r§f" + message);
    }

    public static void broadcastError(String message) {
        Bukkit.broadcastMessage("§0§l[§b錯誤§0] §r§f" + message);
    }

    public static String buildTitle(String title) {
        String line = "-------------------------------------------------";
        String titleBracket = "[ " + ChatColor.YELLOW + title + ChatColor.BLUE + " ]";

        if (titleBracket.length() > line.length()) {
            return ChatColor.BLUE + "-" + titleBracket + "-";
        }

        int min = (line.length() / 2) - titleBracket.length() / 2;
        int max = (line.length() / 2) + titleBracket.length() / 2;

        String out = ChatColor.BLUE + line.substring(0, Math.max(0, min));
        out += titleBracket + line.substring(max);

        return out;
    }
}
