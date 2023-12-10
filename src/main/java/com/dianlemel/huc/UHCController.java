package com.dianlemel.huc;

import com.dianlemel.huc.item.AbstractItem;
import com.dianlemel.huc.util.LocationUtil;
import com.dianlemel.huc.util.MessageUtil;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.RenderType;

import java.util.Objects;
import java.util.Optional;

public class UHCController implements Listener {

    private static final String HEALTH_PLAYER_LIST = "HEALTH_PLAYER_LIST";
    private static final String HEALTH_BELOW_NAME = "BELOW_NAME";

    private static UHCController controller;

    public static UHCController getInstance() {
        return Optional.ofNullable(controller).orElseGet(() -> {
            controller = new UHCController();
            return controller;
        });
    }

    private boolean isRunning = false;
    private final World world;

    public UHCController() {
        //確保只有一個世界
        world = UHCCore.getPlugin().getServer().getWorlds().get(0);
        init();
    }

    public void init() {
        initScoreBoard();
        initWorld();
    }

    //初始化玩家清單血量顯示、玩家名稱上方顯示
    private void initScoreBoard() {
        var scoreBoard = Objects.requireNonNull(UHCCore.getPlugin().getServer().getScoreboardManager()).getMainScoreboard();
        var objectiveHPL = Optional.ofNullable(scoreBoard.getObjective(HEALTH_PLAYER_LIST)).orElseGet(() -> scoreBoard.registerNewObjective(HEALTH_PLAYER_LIST, Criteria.HEALTH, HEALTH_PLAYER_LIST, RenderType.HEARTS));
        objectiveHPL.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        var objectiveHBN = Optional.ofNullable(scoreBoard.getObjective(HEALTH_BELOW_NAME)).orElseGet(() -> scoreBoard.registerNewObjective(HEALTH_BELOW_NAME, Criteria.HEALTH, HEALTH_BELOW_NAME, RenderType.HEARTS));
        objectiveHBN.setDisplaySlot(DisplaySlot.BELOW_NAME);
    }

    //設定世界基礎資訊
    private void initWorld() {
        world.setAutoSave(false);//關閉自動儲存
        world.setPVP(true);//可以PVP
        world.setDifficulty(Difficulty.HARD);//難度最難
        world.setClearWeatherDuration(9999);//設定晴朗天氣
        world.setTime(12000);//設置時間為中午12點
        world.setDifficulty(Difficulty.PEACEFUL);//設置和平模式

        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);//死亡自動重生
        world.setGameRule(GameRule.KEEP_INVENTORY, false);//死亡不保留背包
        world.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);//觀察者模式不生成地圖
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);//時間不變化
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);//天氣不變化
        world.setGameRule(GameRule.DISABLE_RAIDS, true);//關閉襲擊
        world.setGameRule(GameRule.DO_PATROL_SPAWNING, false);//不生成災厄巡邏隊
        world.setGameRule(GameRule.NATURAL_REGENERATION, false);//不自然恢復血量
        world.setGameRule(GameRule.DO_FIRE_TICK, false);//火不要延燒
    }

    //遊戲開始
    public void start() throws UHCException {
        if (UHCTeam.getTeams().isEmpty()) {
            throw new UHCException("尚未建立隊伍");
        }
        if (UHCTeam.isAllEmpty()) {
            throw new UHCException("任何隊伍尚未有任何玩家");
        }
        if (isRunning) {
            throw new UHCException("遊戲正在進行中");
        }
        var config = UHCConfig.getInstance();

        //各隊伍重生點計算
        var center = config.getCenter();
        var range = LocationUtil.calculate2DRange(center, UHCConfig.getInstance().getBorderMaxRadius());
        var minDistance = config.getMinDistance();
        var spawnY = config.getSpawnY();
        var teams = UHCTeam.getTeams();
        var spawnPoints = range.generateRandomLocation(teams.size(), spawnY, minDistance);
        if (spawnPoints.size() != teams.size()) {
            throw new UHCException("隊伍數量與重生點數量不一致!");
        }
        for (var i = 0; i < teams.size(); i++) {
            var team = teams.get(i);
            var spawn = spawnPoints.get(i);
            team.setStartSpawn(spawn);
        }

        //邊界設定
        var worldBorder = world.getWorldBorder();
        worldBorder.setCenter(config.getCenter());
        worldBorder.setSize(config.getBorderMaxRadius());

        //世界難度
        world.setDifficulty(Difficulty.HARD);

        //物品初始化
        AbstractItem.initItems();

        //隊伍開始
        UHCTeam.startAllTeam();

        MessageUtil.broadcastTitle("", "§6遊戲開始", 40, 40, 100);
    }

    //遊戲結束
    public void stop(ChatColor win) throws UHCException {
        if (!isRunning) {
            throw new UHCException("遊戲尚未進行中");
        }
        world.getWorldBorder().reset();
        world.setDifficulty(Difficulty.PEACEFUL);
        UHCTeam.stopAllTeam();
        var subtitle = "";
        if (win != null) {
            subtitle = String.format("§6 %s 獲勝", win.name());
        }
        MessageUtil.broadcastTitle("§6遊戲結束", subtitle, 40, 40, 100);
    }

    //檢查戰場狀態，誰輸誰贏
    private void check() {

    }

    // 天氣改變
    @EventHandler
    public void onWeatherChangeEvent(WeatherChangeEvent event) {

    }

    // 玩家製作
    @EventHandler
    public void onCraftItemEvent(CraftItemEvent event) {

    }

    // 玩家破壞方塊
    @EventHandler
    public void onBlockBreakEvent(BlockBreakEvent event) {
        //遊戲尚未開始，不進行處理
        if (!isRunning) {
            return;
        }
        var world = event.getBlock().getWorld();
        var location = event.getBlock().getLocation();
        switch (event.getBlock().getType()) {
            case COPPER_ORE:
                event.setDropItems(true);
                world.dropItemNaturally(location, new ItemStack(Material.COPPER_INGOT));
                break;
            case IRON_ORE:
                event.setDropItems(true);
                world.dropItemNaturally(location, new ItemStack(Material.IRON_INGOT));
                break;
            case GOLD_ORE:
                event.setDropItems(true);
                world.dropItemNaturally(location, new ItemStack(Material.GOLD_INGOT));
                break;
        }
    }

    // 生物受到傷害
    @EventHandler
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        //如果不是玩家受到傷害
        if (!event.getEntity().getType().equals(EntityType.PLAYER)) {
            return;
        }
        //遊戲尚未開始，阻止受到傷害
        event.setCancelled(!isRunning);
    }

    // 玩家重生
    @EventHandler
    public void onPlayerRespawnEvent(PlayerRespawnEvent event) {
        if (isRunning) {

        } else {

        }
    }

    // 生物回血
    @EventHandler
    public void onEntityRegainHealthEvent(EntityRegainHealthEvent event) {
        //遊戲開始，阻止自然回血
        event.setCancelled(isRunning);
    }

    // 玩家互動
    @EventHandler
    public void onPlayerInteractEvent(PlayerInteractEvent event) {

    }

    // 玩家死亡
    @EventHandler
    public void onPlayerDeathEvent(PlayerDeathEvent event) {
        var player = UHCPlayer.getUHCPlayer(event.getEntity().getUniqueId());
        if (isRunning) {
            check();
        } else {

        }
    }

    //飽食度變更
    @EventHandler
    public void onFoodLevelChangeEvent(FoodLevelChangeEvent event) {
        //遊戲尚未開始，阻止飽食度變更
        event.setCancelled(!isRunning);
    }

    //玩家加入伺服器(呼叫優先級最高)
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        var player = UHCPlayer.getUHCPlayer(event.getPlayer().getUniqueId());
        var team = player.getTeam();
        player.online();
        if (isRunning) {
            if (team == null) {
                player.getPlayer().kickPlayer(ChatColor.RED + "遊戲已經開始了!");
                return;
            }
            if (player.isDead()) {
                player.setGameMode(GameMode.SPECTATOR);
                return;
            }
            if (player.isStart()) {
                return;
            }
            player.setGameMode(GameMode.SURVIVAL);
            var spawn = Optional.ofNullable(team.getStartSpawn()).orElseGet(() -> team.getPlayers().stream().filter(p -> p.isOnline() && !p.isDead()).findAny().map(p -> p.getPlayer().getLocation()).orElseGet(() -> null));
            if (spawn == null) {
                spawn = UHCConfig.getInstance().getSpawn();
                MessageUtil.broadcastError(String.format("隊伍 %s 找不到重生點", team.getColor().name()));
            }
            player.teleport(spawn);
        } else {
            player.setGameMode(GameMode.ADVENTURE);
            player.teleport(UHCConfig.getInstance().getSpawn());
        }
    }

    //玩家離開伺服器(呼叫優先級最高)
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        var player = UHCPlayer.getUHCPlayer(event.getPlayer().getUniqueId());
        if (isRunning) {
            check();
        } else {
            var team = player.getTeam();
            if (team != null) {
                team.kickPlayer(player);
            }
        }
        player.offline();
    }

    public boolean isRunning() {
        return isRunning;
    }

}
