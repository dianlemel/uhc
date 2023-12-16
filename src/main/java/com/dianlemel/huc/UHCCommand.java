package com.dianlemel.huc;

import com.dianlemel.huc.item.AbstractItem;
import com.dianlemel.huc.util.MessageUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UHCCommand implements CommandExecutor, TabCompleter {

    private final Map<String, String> COMMANDS = Maps.newConcurrentMap();
    private final List<String> ON_OFF = Lists.newArrayList("true", "false");

    {
        COMMANDS.put("start", "<秒> 開始遊戲");
        COMMANDS.put("stop", "<獲勝隊伍(可不填)> 結束遊戲");
        COMMANDS.put("createTeam", "<數量> 建立隊伍數量");
        COMMANDS.put("clearTeam", "解散、清除隊伍");
        COMMANDS.put("random", "<是否包含已經有隊伍> 隨機分隊");
        COMMANDS.put("join", "<隊伍> <玩家...> 玩家加入該隊伍");
        COMMANDS.put("leave", "<隊伍> <玩家...> 該隊伍踢除玩家");
        COMMANDS.put("give", "<物品KEY> 獲取特殊物品");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!sender.isOp()) {
            MessageUtil.sendError(sender, "你沒有使用該指令的權限");
            return true;
        }
        if (strings.length > 0) {
            try {
                switch (strings[0]) {
                    case "start":
                        if (strings.length == 2) {
                            try {
                                var seconds = Integer.parseInt(strings[1]);
                                UHCController.getInstance().start(seconds);
                                return true;
                            } catch (NumberFormatException e) {
                                MessageUtil.sendError(sender, e.getMessage());
                            }
                        }
                        break;
                    case "stop":
                        if (strings.length == 1) {
                            UHCController.getInstance().stop(null);
                        }
                        if (strings.length == 2) {
                            try {
                                var color = ChatColor.valueOf(strings[1]);
                                UHCController.getInstance().stop(color);
                                return true;
                            } catch (IllegalArgumentException e) {
                                MessageUtil.sendError(sender, e.getMessage());
                            }
                        }
                        break;
                    case "createTeam":
                        if (strings.length == 2) {
                            try {
                                var size = Integer.parseInt(strings[1]);
                                UHCTeam.createTeam(size);
                                return true;
                            } catch (NumberFormatException e) {
                                MessageUtil.sendError(sender, e.getMessage());
                            }
                        }
                        break;
                    case "clearTeam":
                        UHCTeam.clearTeam();
                        return true;
                    case "random":
                        if (strings.length == 2) {
                            try {
                                var kickAll = Boolean.parseBoolean(strings[1]);
                                UHCTeam.random(kickAll);
                                return true;
                            } catch (NumberFormatException e) {
                                MessageUtil.sendError(sender, e.getMessage());
                            }
                        }
                        break;
                    case "join":
                        if (strings.length >= 3) {
                            var team = UHCTeam.getTeam(strings[1]);
                            Arrays.stream(strings)
                                    .skip(2)
                                    .map(UHCPlayer::getUHCPlayer)
                                    .filter(Objects::nonNull)
                                    .forEach(player -> {
                                        if (team.inTeam(player.getUuid())) {
                                            MessageUtil.sendError(sender, String.format("%s 已經再該隊伍裡", player.getName()));
                                        } else {
                                            team.joinPlayer(player);
                                            MessageUtil.sendInfo(sender, String.format("%s 加入成功", player.getName()));
                                        }
                                    });
                            return true;
                        }
                        break;
                    case "leave":
                        if (strings.length >= 3) {
                            var team = UHCTeam.getTeam(strings[1]);
                            Arrays.stream(strings)
                                    .skip(2)
                                    .map(UHCPlayer::getUHCPlayer)
                                    .filter(Objects::nonNull)
                                    .forEach(player -> {
                                        if (team.inTeam(player.getUuid())) {
                                            MessageUtil.sendError(sender, String.format("%s 不再隊伍裡", player.getName()));
                                        } else {
                                            team.kickPlayer(player);
                                            MessageUtil.sendInfo(sender, String.format("%s 離開該隊伍", player.getName()));
                                        }
                                    });
                            return true;
                        }
                        break;
                    case "give":
                        if (strings.length == 2) {
                            if (sender instanceof Player player) {
                                var abstractItem = AbstractItem.getItem(strings[1]);
                                if (abstractItem != null) {
                                    var item = abstractItem.createItem();
                                    player.getInventory().addItem(item);
                                } else {
                                    MessageUtil.sendError(sender, String.format("無此 %s 編號", strings[1]));
                                }
                            } else {
                                MessageUtil.sendError(sender, "不支援後端輸入");
                            }
                            return true;
                        }
                        break;
                }
                Optional.ofNullable(COMMANDS.get(strings[0])).ifPresent(cmd -> {
                    MessageUtil.sendInfo(sender, String.format("/uhc %s %s", strings[0], cmd));
                });
                return true;
            } catch (UHCException e) {
                MessageUtil.sendError(sender, e.getMessage());
            }
            return true;
        }
        COMMANDS.forEach((key, value) -> MessageUtil.sendInfo(sender, String.format("/uhc %s %s", key, value)));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!commandSender.isOp()) {
            return Lists.newArrayList();
        }
        if (strings.length == 0) {
            return COMMANDS.values().stream().toList();
        }
        if (strings.length == 1) {
            return COMMANDS.values().stream().filter(c -> c.startsWith(strings[0])).collect(Collectors.toList());
        }
        if (strings.length == 2) {
            switch (strings[0]) {
                case "random":
                    return ON_OFF.stream().filter(c -> c.startsWith(strings[1])).collect(Collectors.toList());
                case "give":
                    return AbstractItem.getItems().stream()
                            .map(AbstractItem::getKey)
                            .filter(c -> c.startsWith(strings[1]))
                            .collect(Collectors.toList());
                case "join":
                case "leave":
                case "stop":
                    return UHCTeam.getTeams().stream()
                            .map(t -> t.getColor().name())
                            .filter(n -> n.toLowerCase().startsWith(strings[1].toLowerCase()))
                            .collect(Collectors.toList());
            }
        }
        if (strings[0].equals("join")) {
            var team = UHCTeam.getTeam(strings[1]);
            if (team != null) {
                var players = Bukkit.getOnlinePlayers();
                var lastInput = strings[strings.length - 1];
                return players.stream()
                        .map(Player::getName)
                        .filter(team::inTeam)
                        .filter(name -> name.startsWith(lastInput))
                        .collect(Collectors.toList());
            }
        }
        if (strings[0].equals("leave")) {
            var team = UHCTeam.getTeam(strings[1]);
            if (team != null) {
                var players = team.getPlayers();
                var lastInput = strings[strings.length - 1];
                return players.stream()
                        .map(UHCPlayer::getName)
                        .filter(name -> name.startsWith(lastInput))
                        .collect(Collectors.toList());
            }
        }
        return Lists.newArrayList();
    }
}
