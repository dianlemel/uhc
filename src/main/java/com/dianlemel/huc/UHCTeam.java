package com.dianlemel.huc;

import com.dianlemel.huc.util.MessageUtil;
import com.google.common.collect.Lists;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.stream.Collectors;

public class UHCTeam {

    private static final ChatColor[] colors = new ChatColor[]{
            ChatColor.GREEN,
            ChatColor.RED,
            ChatColor.BLUE,
            ChatColor.GOLD,
            ChatColor.AQUA,
            ChatColor.GRAY,
            ChatColor.YELLOW,
            ChatColor.BLACK
    };

    private static final List<UHCTeam> teams = Lists.newArrayList();

    public static List<UHCTeam> getTeams() {
        return teams;
    }

    public static void startAllTeam() {
        teams.forEach(UHCTeam::start);
    }

    public static void stopAllTeam() {
        teams.forEach(UHCTeam::stop);
    }

    //建立隊伍
    public static int createTeam(int size) throws UHCException {
        if (colors.length < size) {
            throw new UHCException("達到隊伍建立數量上限");
        }
        if (!teams.isEmpty()) {
            clearTeam();
        }
        for (var i = 0; i < size; i++) {
            teams.add(new UHCTeam(colors[i]));
        }
        return teams.size();
    }

    //隨機分隊
    public static void random(boolean kickAll) throws UHCException {
        //當尚未建立任何隊伍
        if (teams.isEmpty()) {
            throw new UHCException("請先建立隊伍數量");
        }
        //獲取已經再線上的所有玩家，並過濾必須模式為冒險者模式
        var players = Bukkit.getServer().getOnlinePlayers().stream().filter(player -> player.getGameMode().equals(GameMode.ADVENTURE)).collect(Collectors.toList());
        //打亂
        Collections.shuffle(players);
        if (kickAll) {
            //踢出已經再隊伍裡的所有玩家
            teams.forEach(UHCTeam::kickAllPlayer);
        } else {
            //過濾已經有隊伍的玩家
            players = players.stream().filter(player -> !UHCTeam.hasTeam(player.getUniqueId())).collect(Collectors.toList());
        }
        var i = 0;
        for (Player value : players) {
            var player = UHCPlayer.getUHCPlayer(value.getUniqueId());
            var team = teams.get(i++ % teams.size());
            team.joinPlayer(player);
        }
        String msg = teams.stream().map(t -> String.format("%s%s : %d", t.getColor().toString(), t.getColor().name(), t.size())).collect(Collectors.joining("§f , "));
        MessageUtil.broadcastInfo(msg);
    }

    //判斷該玩家是否有隊伍
    public static boolean hasTeam(UUID uuid) {
        return teams.stream().anyMatch(team -> team.inTeam(uuid));
    }

    //根據玩家的UUID去取得玩家所再的隊伍
    public static UHCTeam getTeam(UUID uuid) {
        return teams.stream().filter(team -> team.inTeam(uuid)).findAny().orElseGet(() -> null);
    }

    //取得隊伍
    public static UHCTeam getTeam(String name) {
        return teams.stream().filter(t -> t.getColor().name().equals(name)).findAny().orElse(null);
    }

    //踢出所有玩家，並清空所有隊伍
    public static void clearTeam() throws UHCException {
        if (UHCController.getInstance().isRunning()) {
            throw new UHCException("遊戲正在進行中");
        }
        teams.forEach(team -> {
            team.kickAllPlayer();
            team.unregisterTeamScoreboard();
        });
        teams.clear();
    }

    //是否所有隊伍都沒有玩家
    public static boolean isAllEmpty() {
        return teams.stream().allMatch(UHCTeam::isEmpty);
    }

    private final ChatColor color;
    private final List<UHCPlayer> players = Lists.newArrayList();
    private Team team;

    private Location startSpawn;

    public UHCTeam(ChatColor color) {
        this.color = color;
        initTeamScoreboard();
    }

    public ChatColor getColor() {
        return color;
    }

    //初始化隊伍記分板
    private void initTeamScoreboard() {
        //獲取主記分板
        var scoreboard = Bukkit.getServer().getScoreboardManager().getMainScoreboard();
        try {
            scoreboard.registerNewObjective(color.name(), Criteria.HEALTH, color.name(), RenderType.HEARTS);
        } catch (IllegalArgumentException e) {
            scoreboard.getObjective(color.name()).unregister();
            scoreboard.registerNewObjective(color.name(), Criteria.HEALTH, color.name(), RenderType.HEARTS);
        }
        try {
            //註冊新的隊伍
            team = scoreboard.registerNewTeam(color.toString());
        } catch (IllegalArgumentException e) {
            //當已經有註冊新的隊伍，會接收到例外，並取消註冊
            Objects.requireNonNull(scoreboard.getTeam(color.toString())).unregister();
            //再註冊一次新的隊伍
            team = scoreboard.registerNewTeam(color.toString());
        }
        //設置隊伍顏色
        team.setColor(color);
        //隊伍傷害關閉
        team.setAllowFriendlyFire(false);
        //隊友隱形還可以看得到
        team.setCanSeeFriendlyInvisibles(true);
    }

    public void unregisterTeamScoreboard() {
        var scoreboard = Bukkit.getServer().getScoreboardManager().getMainScoreboard();
        Optional.ofNullable(scoreboard.getObjective(color.name())).ifPresent(Objective::unregister);
        Optional.ofNullable(scoreboard.getTeam(color.toString())).ifPresent(Team::unregister);
    }

    //隊伍成員數量
    public int size() {
        return players.size();
    }

    //判斷該隊伍是否沒有任何玩家
    public boolean isEmpty() {
        return players.isEmpty();
    }

    //加入某位玩家
    public void joinPlayer(UHCPlayer player) {
        players.add(player);
        team.addEntry(player.getName());
    }

    //踢出某位玩家
    public void kickPlayer(UHCPlayer player) {
        players.remove(player);
        team.removeEntry(player.getName());
    }

    //獲取隊伍記分板
    public Team getScoreboardTeam() {
        return team;
    }

    //踢出所有玩家
    public void kickAllPlayer() {
        Lists.newArrayList(players).forEach(this::kickPlayer);
    }

    //判斷該玩家是否在這隊伍
    public boolean inTeam(UUID uuid) {
        return players.stream().map(UHCPlayer::getUuid).anyMatch(uuid::equals);
    }

    //判斷該玩家是否在這隊伍
    public boolean inTeam(String name) {
        var player = Bukkit.getPlayer(name);
        if (player == null) {
            return false;
        }
        var uuid = player.getUniqueId();
        return players.stream().map(UHCPlayer::getUuid).anyMatch(uuid::equals);
    }

    //取得開始遊戲重生點，當取得不到將會嘗試取得其他隊友的座標
    public Location getStartSpawn() {
        return Optional.ofNullable(startSpawn).orElseGet(() -> getPlayers().stream().filter(p -> p.isOnline() && !p.isDead()).findAny().map(p -> p.getPlayer().getLocation()).orElseGet(() -> null));
    }

    public void setStartSpawn(Location startSpawn) {
        this.startSpawn = startSpawn;
    }

    //遊戲開始
    public void start() {
        players.stream().filter(UHCPlayer::isOnline).forEach(players -> {
            players.init();
            players.teleport(getStartSpawn());
        });
    }

    //遊戲結束
    public void stop() {
        players.stream().filter(UHCPlayer::isOnline).forEach(players -> {
            players.stop();
            players.teleport(UHCConfig.getInstance().getSpawn());
        });
    }

    public boolean isAllDead() {
        return players.stream().filter(UHCPlayer::isOnline).filter(UHCPlayer::isInit).allMatch(UHCPlayer::isDead);
    }

    public List<UHCPlayer> getPlayers() {
        return players;
    }
}
