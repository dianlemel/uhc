package com.dianlemel.huc;

import com.dianlemel.huc.item.BaseItem;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class UHCCommand implements CommandExecutor, TabCompleter {

    private final Map<String, String> COMMANDS = Maps.newConcurrentMap();
    private final List<String> ON_OFF = Lists.newArrayList("true", "false");

    {
        COMMANDS.put("start", "<遊戲進行> <開始遊戲幾秒後顯示名稱> <剩餘幾秒後清除怪物> <剩餘幾秒後全體玩家發光> <剩餘幾秒開始縮圈> 開始遊戲");
        COMMANDS.put("stop", "<獲勝隊伍(可不填)> 結束遊戲");
        COMMANDS.put("createTeam", "<數量> 建立隊伍數量");
        COMMANDS.put("clearTeam", "解散、清除隊伍");
        COMMANDS.put("random", "<是否包含已經有隊伍> 隨機分隊");
        COMMANDS.put("join", "<隊伍> <玩家...> 玩家加入該隊伍");
        COMMANDS.put("leave", "<隊伍> <玩家...> 該隊伍踢除玩家");
        COMMANDS.put("give", "<物品KEY> 獲取特殊物品");
        COMMANDS.put("reload", "重新讀取設定");
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
                    case "reload":
                        UHCConfig.getInstance().loadConfig();
                        BaseItem.loadData();
                        MessageUtil.sendInfo(sender, "重新讀取完成");
                        return true;
                    case "start":
                        if (strings.length == 6) {
                            try {
                                var seconds = Integer.parseInt(strings[1]);
                                var showNameTimer = Integer.parseInt(strings[2]);
                                var clearMonsterTimer = Integer.parseInt(strings[3]);
                                var glowingTimer = Integer.parseInt(strings[4]);
                                var borderTimer = Integer.parseInt(strings[5]);
                                UHCController.getInstance().start(seconds, showNameTimer, clearMonsterTimer, glowingTimer, borderTimer);
                                return true;
                            } catch (NumberFormatException e) {
                                MessageUtil.sendError(sender, e.getMessage());
                            }
                        }
                        break;
                    case "stop":
                        if (strings.length == 1) {
                            UHCController.getInstance().stop(null);
                            return true;
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
                                size = UHCTeam.createTeam(size);
                                MessageUtil.sendInfo(sender, String.format("已成功建立 %d 組隊伍", size));
                                return true;
                            } catch (NumberFormatException e) {
                                MessageUtil.sendError(sender, e.getMessage());
                            }
                        }
                        break;
                    case "clearTeam":
                        UHCTeam.clearTeam();
                        MessageUtil.sendInfo(sender, "隊伍清除完成");
                        return true;
                    case "random":
                        if (strings.length == 2) {
                            try {
                                var kickAll = Boolean.parseBoolean(strings[1]);
                                UHCTeam.random(kickAll);
                                MessageUtil.sendInfo(sender, "已成功隨機分隊");
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
                                        if (!team.inTeam(player.getUuid())) {
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
                                var abstractItem = BaseItem.getItem(strings[1]);
                                if (abstractItem != null) {
                                    var item = abstractItem.createItem();
                                    player.getInventory().addItem(item);
                                    MessageUtil.sendInfo(sender, "已成功創建物品");
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
            return COMMANDS.keySet().stream().toList();
        }
        if (strings.length == 1) {
            return COMMANDS.keySet().stream().filter(c -> c.startsWith(strings[0])).collect(Collectors.toList());
        }
        if (strings.length == 2) {
            switch (strings[0]) {
                case "random":
                    return ON_OFF.stream().filter(c -> c.startsWith(strings[1])).collect(Collectors.toList());
                case "give":
                    return BaseItem.getItems().stream()
                            .map(BaseItem::getKey)
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

        switch (strings[0]) {
            case "join":
                var joinTeam = UHCTeam.getTeam(strings[1]);
                if (joinTeam != null) {
                    var players = Bukkit.getOnlinePlayers();
                    var joinList = Arrays.stream(strings).skip(2).toList();
                    var lastInput = strings[strings.length - 1];
                    return players.stream()
                            .map(Player::getName)
                            .filter(Predicate.not(joinTeam::inTeam))
                            .filter(Predicate.not(joinList::contains))
                            .filter(name -> name.startsWith(lastInput))
                            .collect(Collectors.toList());
                }
                break;
            case "leave":
                var leaveTeam = UHCTeam.getTeam(strings[1]);
                if (leaveTeam != null) {
                    var players = leaveTeam.getPlayers();
                    var leaveList = Arrays.stream(strings).skip(2).toList();
                    var lastInput = strings[strings.length - 1];
                    return players.stream()
                            .map(UHCPlayer::getName)
                            .filter(name -> name.startsWith(lastInput))
                            .filter(Predicate.not(leaveList::contains))
                            .collect(Collectors.toList());
                }
                break;
        }
        return Lists.newArrayList();
    }
}
