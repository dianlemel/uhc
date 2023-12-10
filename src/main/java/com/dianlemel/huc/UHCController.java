package com.dianlemel.huc;

import com.dianlemel.huc.item.AbstractItem;
import com.dianlemel.huc.util.LocationUtil;
import com.dianlemel.huc.util.MessageUtil;
import com.dianlemel.huc.util.TaskUtil;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.RenderType;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

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
        if (isRunning()) {
            throw new UHCException("遊戲正在進行中");
        }
        var config = UHCConfig.getInstance();

        //各隊伍重生點計算
        var center = config.getCenter();
        var range = LocationUtil.calculate2DRange(center, UHCConfig.getInstance().getBorderMaxRadius());
        var minDistance = config.getMinDistance();
        var spawnY = config.getSpawnY();
        var teams = UHCTeam.getTeams();
        //產生各隊伍的重生點
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
        if (!isRunning()) {
            throw new UHCException("遊戲尚未進行中");
        }
        isRunning = false;
        //重置世界邊界
        world.getWorldBorder().reset();
        //設置世界難度
        world.setDifficulty(Difficulty.PEACEFUL);
        //遊戲結束
        UHCTeam.stopAllTeam();
        var subtitle = "";
        if (win != null) {
            subtitle = String.format("§6 %s 獲勝", win.name());
        }
        MessageUtil.broadcastTitle("§6遊戲結束", subtitle, 40, 40, 100);
    }

    //檢查戰場狀態，誰輸誰贏
    private void check() {
        TaskUtil.syncTask(() -> {
            var teams = UHCTeam.getTeams().stream().filter(not(UHCTeam::isAllDead)).collect(Collectors.toList());
            //當隊伍存活還有一組以上，就不繼續處理
            if (teams.size() > 1) {
                return;
            }
            try {
                stop(teams.stream().map(UHCTeam::getColor).findFirst().orElse(null));
            } catch (UHCException e) {
                MessageUtil.sendError(e);
            }
        }, 20);
    }

    // 天氣改變
    @EventHandler
    public void onWeatherChangeEvent(WeatherChangeEvent event) {
        //阻止天氣改變
        event.setCancelled(true);
    }

    // 玩家破壞方塊
    @EventHandler
    public void onBlockBreakEvent(BlockBreakEvent event) {
        //遊戲尚未開始，不進行處理
        if (!isRunning()) {
            return;
        }
        var world = event.getBlock().getWorld();
        var location = event.getBlock().getLocation();
        //如果是鐵礦、銅礦、金礦，需要阻止他噴出來，並噴出對應的錠
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

    //生物死亡
    @EventHandler
    public void onEntityDeathEvent(EntityDeathEvent event) {
        //如果是玩家不處理
        if (event.getEntityType().equals(EntityType.PLAYER)) {
            return;
        }
        //遊戲尚未開始，不處理
        if (!isRunning()) {
            return;
        }
        //準備噴出的物品進行替換，馬鈴薯、生豬肉、生牛肉、生羊肉、生雞肉、生兔肉、生鱈魚、生鮭魚進行替換
        event.getDrops().forEach(item -> {
            switch (item.getType()) {
                case POTATO:
                    item.setType(Material.BAKED_POTATO);
                    break;
                case PORKCHOP:
                    item.setType(Material.COOKED_PORKCHOP);
                    break;
                case BEEF:
                    item.setType(Material.COOKED_BEEF);
                    break;
                case MUTTON:
                    item.setType(Material.COOKED_MUTTON);
                    break;
                case CHICKEN:
                    item.setType(Material.COOKED_CHICKEN);
                    break;
                case RABBIT:
                    item.setType(Material.COOKED_RABBIT);
                    break;
                case COD:
                    item.setType(Material.COOKED_COD);
                    break;
                case SALMON:
                    item.setType(Material.COOKED_SALMON);
                    break;
            }
        });
    }

    // 生物受到傷害
    @EventHandler
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        //如果不是玩家受到傷害
        if (!event.getEntity().getType().equals(EntityType.PLAYER)) {
            return;
        }
        //遊戲尚未開始，阻止受到傷害
        event.setCancelled(!isRunning());
    }

    // 玩家重生
    @EventHandler
    public void onPlayerRespawnEvent(PlayerRespawnEvent event) {
        var player = UHCPlayer.getUHCPlayer(event.getPlayer().getUniqueId());
        //取得擊殺者
        var killer = event.getPlayer().getKiller();
        //如果有擊殺者、並且該擊殺者為玩家
        if (killer != null && EntityType.PLAYER.equals(killer.getType())) {
            //延遲5秒後執行
            TaskUtil.syncTask(() -> {
                //判斷遊戲是否正在進行
                if (isRunning()) {
                    //判斷玩家是否再線上
                    player.ifOnline(p -> {
                        //將玩家進入觀察者模式
                        p.setGameMode(GameMode.SPECTATOR);
                        //附身再擊殺者身上
                        p.setSpectatorTarget(killer);
                    });
                }
            }, 100);
        }
        //取得死亡位置
        var spawn = player.getDeadLocation();
        if (spawn == null) {
            spawn = UHCConfig.getInstance().getSpawn();
        }
        //將玩家進入觀察者模式
        player.setGameMode(GameMode.SPECTATOR);
        //設置重生點
        event.setRespawnLocation(spawn);
    }

    // 生物回血
    @EventHandler
    public void onEntityRegainHealthEvent(EntityRegainHealthEvent event) {
        //遊戲開始，阻止自然回血
        event.setCancelled(isRunning());
    }

    // 玩家死亡
    @EventHandler
    public void onPlayerDeathEvent(PlayerDeathEvent event) {
        //如果遊戲尚未進行
        if (!isRunning()) {
            return;
        }
        var player = UHCPlayer.getUHCPlayer(event.getEntity().getUniqueId());
        var deadLocation = event.getEntity().getLocation();
        var team = player.getTeam();
        //當該玩家沒有隊伍，不進行處理
        if (team == null) {
            return;
        }
        //標記該玩家死亡
        player.setDead(true);
        //儲存死亡點
        player.setDeadLocation(deadLocation);

        //生成頭顱
        var item = new ItemStack(Material.PLAYER_HEAD);
        var meta = item.getItemMeta();
        if (meta instanceof SkullMeta skullMeta) {
            //設定該頭顱的資訊，讓他有SKIN
            skullMeta.setOwnerProfile(player.getPlayer().getPlayerProfile());
        }
        item.setItemMeta(meta);
        deadLocation.getWorld().dropItemNaturally(deadLocation, item);

        //檢查
        check();
    }

    //傳送
    @EventHandler
    public void onEntityPortalEvent(EntityPortalEvent event) {
        //如果不是玩家就不處理
        if (!EntityType.PLAYER.equals(event.getEntityType())) {
            return;
        }
        var player = (Player) event.getEntity();
        //如果是創造者模式，就不處理
        if (GameMode.CREATIVE.equals(player.getGameMode())) {
            return;
        }
        //如果世界不一樣，阻止傳送
        event.setCancelled(event.getFrom().getWorld().equals(event.getTo().getWorld()));
    }

    //飽食度變更
    @EventHandler
    public void onFoodLevelChangeEvent(FoodLevelChangeEvent event) {
        //遊戲尚未開始，阻止飽食度變更
        event.setCancelled(!isRunning());
    }

    //玩家加入伺服器(呼叫優先級最高)
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        var player = UHCPlayer.getUHCPlayer(event.getPlayer().getUniqueId());
        var team = player.getTeam();
        player.online();
        //如果遊戲正在進行
        if (isRunning()) {
            //如果沒有隊伍
            if (team == null) {
                var p = player.getPlayer();
                if (p.isOp()) {
                    p.setGameMode(GameMode.SPECTATOR);
                    p.teleport(UHCConfig.getInstance().getSpawn());
                } else {
                    p.kickPlayer(ChatColor.RED + "遊戲已經開始了!");
                }
                return;
            }
            //如果死亡
            if (player.isDead()) {
                player.setGameMode(GameMode.SPECTATOR);
                return;
            }
            //如果已經開始
            if (player.isStart()) {
                return;
            }
            //設置該玩家為生存模式
            player.setGameMode(GameMode.SURVIVAL);
            //取得遊戲開始重生點
            var spawn = team.getStartSpawn();
            //當取得不到重生點，就異常了
            if (spawn == null) {
                //至少給一開始進入伺服器的眾生點
                spawn = UHCConfig.getInstance().getSpawn();
                MessageUtil.broadcastError(String.format("隊伍 %s 找不到重生點", team.getColor().name()));
            }
            player.teleport(spawn);
        } else {
            //設置該玩家為冒險者模式
            player.setGameMode(GameMode.ADVENTURE);
            //傳送到遊戲尚未開始的重生點
            player.teleport(UHCConfig.getInstance().getSpawn());
            //清除所有效果
            player.clearAllEffects();
        }
    }

    //玩家離開伺服器(呼叫優先級最高)
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        var player = UHCPlayer.getUHCPlayer(event.getPlayer().getUniqueId());
        //如果遊戲正在進行
        if (isRunning()) {
            //標記該玩家為離線
            player.offline();
            check();
        } else {
            var team = player.getTeam();
            //如果該玩家有隊伍
            if (team != null) {
                //剔除
                team.kickPlayer(player);
            }
            //標記該玩家為離線
            player.offline();
        }
    }

    //遊戲是否正在進行
    public boolean isRunning() {
        return isRunning;
    }

}
