package com.dianlemel.huc;

import com.dianlemel.huc.item.BaseItem;
import com.dianlemel.huc.util.BukkitUtil;
import com.dianlemel.huc.util.LocationUtil;
import com.dianlemel.huc.util.MessageUtil;
import com.dianlemel.huc.util.TaskUtil;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Team;

import java.util.Calendar;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

import static java.util.function.Predicate.not;

public class UHCController implements Listener, Runnable {

    private static final String HEALTH_PLAYER_LIST = "HEALTH_PLAYER_LIST";
    private static final String HEALTH_BELOW_NAME = ChatColor.RED + "❤";
    private static final int[] PITCH = new int[]{1, 2};

    private static UHCController controller;

    public static UHCController getInstance() {
        return Optional.ofNullable(controller).orElseGet(() -> {
            controller = new UHCController();
            return controller;
        });
    }

    private boolean isRunning = false;
    private boolean spawnMonster = true;
    private boolean isInvincible = true;
    private int clearMonsterTimer = 0;
    private int glowingTimer = 0;
    private int borderTimer = 0;
    private int time;
    private long deadMusicCoolDown = 0;
    private final World world;
    private final Random random = new Random();

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
        world.setTime(6000);//設置時間為中午12點
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
    public void start(int time, int showNameTimer, int clearMonsterTimer, int glowingTimer, int borderTimer) throws UHCException {
        if (UHCTeam.getTeams().isEmpty()) {
            throw new UHCException("尚未建立隊伍");
        }
        if (UHCTeam.isAllEmpty()) {
            throw new UHCException("任何隊伍尚未有任何玩家");
        }
        if (isRunning()) {
            throw new UHCException("遊戲正在進行中");
        }
        isRunning = true;
        isInvincible = true;
        this.clearMonsterTimer = clearMonsterTimer;
        this.glowingTimer = glowingTimer;
        this.borderTimer = borderTimer;
        this.time = time;
        var config = UHCConfig.getInstance();

        //各隊伍重生點計算
        var center = config.getCenter();
        var range = LocationUtil.calculate2DRange(center, UHCConfig.getInstance().getBorderMaxRadius() / 2);
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
        //中心點
        worldBorder.setCenter(config.getCenter());
        //初始範圍
        worldBorder.setSize(config.getBorderMaxRadius());
        //距離幾格顯示警告
        worldBorder.setWarningDistance(16);
        //邊界外持續受到傷害
        worldBorder.setDamageAmount(0.1);
        //緩衝距離
        worldBorder.setDamageBuffer(0);

        //清除怪物
        world.getLivingEntities().stream().filter(Enemy.class::isInstance).forEach(Entity::remove);
        //世界難度
        world.setDifficulty(Difficulty.HARD);

        //物品初始化
        BaseItem.initItems();

        //隊伍開始
        UHCTeam.startAllTeam();

        MessageUtil.broadcastTitle("", "§6遊戲開始", 40, 40, 100);

        spawnMonster = true;
        deadMusicCoolDown = Calendar.getInstance().getTimeInMillis();
        TaskUtil.syncTimer(this, 20, 20);
        TaskUtil.syncTaskLater(this::showName, showNameTimer * 20L);
        //無敵倒數
        TaskUtil.syncTaskLater(() -> {
            isInvincible = false;
        }, config.getInvincible() * 20);

        MessageUtil.broadcastInfo(String.format("遊戲開始 %d 秒後，開始顯示玩家ID", showNameTimer));
        MessageUtil.broadcastInfo(String.format("遊戲開始 %d 秒內，無敵", config.getInvincible()));
        MessageUtil.broadcastInfo(String.format("剩餘 %d 秒後，清除怪物", this.clearMonsterTimer));
        MessageUtil.broadcastInfo(String.format("剩餘 %d 秒後，全體發光", this.glowingTimer));
        MessageUtil.broadcastInfo(String.format("剩餘 %d 秒後，開始縮圈", this.borderTimer));

        //隱藏玩家名稱，只有隊伍自己人看得到
        UHCTeam.getTeams().forEach(team -> team.getScoreboardTeam().setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OTHER_TEAMS));
    }

    //每一秒執行一次
    @Override
    public void run() {
        time--;
        MessageUtil.sendActionBarToAll(new ComponentBuilder("剩餘時間: " + time).color(net.md_5.bungee.api.ChatColor.BOLD).create());
        if (time == clearMonsterTimer) {
            clearMonster();
        }
        if (time == glowingTimer) {
            glowingAllPlayer();
        }
        if (time == borderTimer) {
            startBoard();
        }
    }

    //開始縮圈
    private void startBoard() {
        BukkitUtil.playSoundToAll(Sound.BLOCK_ANVIL_PLACE, 1f, 0);
        MessageUtil.broadcastInfo("開始縮圈");
        var worldBorder = world.getWorldBorder();
        var config = UHCConfig.getInstance();
        //設定需要花費幾秒時間，將邊界縮至指定的範圍
        worldBorder.setSize(config.getBorderMinRadius(), borderTimer);
        BukkitUtil.playSoundToAll(config.getBorderMusic(), 10f, 1);
    }

    //顯示玩家名稱，可以看得到敵方
    private void showName() {
        BukkitUtil.playSoundToAll(Sound.BLOCK_ANVIL_PLACE, 1f, 0);
        MessageUtil.broadcastInfo("現在可以看得到敵方名稱");
        UHCTeam.getTeams().forEach(team -> team.getScoreboardTeam().setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS));
    }

    //清除所有怪物
    private void clearMonster() {
        BukkitUtil.playSoundToAll(Sound.BLOCK_ANVIL_PLACE, 1f, 0);
        MessageUtil.broadcastInfo("清除怪物");
        spawnMonster = false;
        var config = UHCConfig.getInstance();
        var range = LocationUtil.calculate2DRange(config.getCenter(), UHCConfig.getInstance().getBorderMaxRadius() / 2);
        world.getLivingEntities().stream().filter(Enemy.class::isInstance).filter(e -> range.inRange(e.getLocation(), true)).forEach(Entity::remove);
    }

    //所有生存模式下的玩家都發光
    private void glowingAllPlayer() {
        BukkitUtil.playSoundToAll(Sound.BLOCK_ANVIL_PLACE, 1f, 0);
        MessageUtil.broadcastInfo("所有玩家發光");
        BukkitUtil.getPlayers(world, GameMode.SURVIVAL).forEach(player -> player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 999999, 0)));
    }

    //遊戲結束
    public void stop(ChatColor win) throws UHCException {
        if (!isRunning()) {
            throw new UHCException("遊戲尚未進行中");
        }
        isRunning = false;
        //停止計時器
        TaskUtil.cancelAllTask();
        //重置世界邊界
        world.getWorldBorder().reset();
        //設置世界難度
        world.setDifficulty(Difficulty.PEACEFUL);
        //遊戲結束
        UHCTeam.stopAllTeam();
        var subtitle = Optional.ofNullable(win).map(v -> String.format("§6 %s 獲勝", v.name())).orElse("");
        MessageUtil.broadcastTitle("§6遊戲結束", subtitle, 40, 40, 100);
    }

    //檢查戰場狀態，誰輸誰贏
    private void check() {
        TaskUtil.syncTask(() -> {
            var teams = UHCTeam.getTeams().stream().filter(not(UHCTeam::isAllDead)).toList();
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

    //實體生成
    @EventHandler
    public void onEntitySpawnEvent(EntitySpawnEvent event) {
        //遊戲尚未開始，不進行處理
        if (!isRunning()) {
            return;
        }
        var entity = event.getEntity();
        //當實體為掉落物
        if (entity instanceof Item item) {
            //取得該掉落物的種類
            var itemStack = item.getItemStack();
            //替換物品
            switch (itemStack.getType()) {
                case POTATO:
                    itemStack.setType(Material.BAKED_POTATO);
                    break;
                case RAW_COPPER:
                    itemStack.setType(Material.COPPER_INGOT);
                    break;
                case RAW_IRON:
                    itemStack.setType(Material.IRON_INGOT);
                    break;
                case RAW_GOLD:
                    itemStack.setType(Material.GOLD_INGOT);
                    break;
            }
        }
    }

    //生物生成
    @EventHandler
    public void onCreatureSpawnEvent(CreatureSpawnEvent event) {
        //遊戲尚未開始，不進行處理
        if (!isRunning()) {
            return;
        }
        //當還沒開始阻止生成怪物
        if (spawnMonster) {
            return;
        }
        var entity = event.getEntity();
        //如果是敵方生物
        if (entity instanceof Enemy) {
            //如果是怪物蛋生成
            if (CreatureSpawnEvent.SpawnReason.SPAWNER_EGG.equals(event.getSpawnReason())) {
                return;
            }
            entity.remove();
        }
    }

    //當chunk被載入的時候，再該chunk的生物也會被載入
    @EventHandler
    public void onEntitiesLoadEvent(EntitiesLoadEvent event) {
        //遊戲尚未開始，不進行處理
        if (!isRunning()) {
            return;
        }
        if (!spawnMonster) {
            var config = UHCConfig.getInstance();
            var range = LocationUtil.calculate2DRange(config.getCenter(), UHCConfig.getInstance().getBorderMaxRadius() / 2);
            if (range.inChunk(event.getChunk())) {
                event.getEntities().stream().filter(Enemy.class::isInstance).forEach(Entity::remove);
            }
        }
    }

    // 玩家破壞方塊
    @EventHandler
    public void onBlockBreakEvent(BlockBreakEvent event) {
        //遊戲尚未開始，不進行處理
        if (!isRunning()) {
            return;
        }
        var type = event.getBlock().getType();
        if (Material.STONE.equals(type) || Material.DEEPSLATE.equals(type)) {
            //如果是鵝卵石、深板岩，增加挖礦次數
            onBlockBreakCount(event.getPlayer());
        }
    }

    //魚骨判斷
    private void onBlockBreakCount(Player p) {
        var player = UHCPlayer.getUHCPlayer(p);
        var config = UHCConfig.getInstance();
        var count = player.getBlockBreakCount() + 1;
        player.setBlockBreakCount(count);
        if (count > config.getBaselineThreshold()) {
            count = -config.getBaselineThreshold();
            if (count % config.getProgressiveTriggerRule() == 0) {
                var effect = BaseItem.toEffect(BukkitUtil.random(config.getPunishedEffects()));
                //給予效果
                p.addPotionEffect(effect);
                //清除所有鎬類
                p.getInventory().remove(Material.WOODEN_PICKAXE);
                p.getInventory().remove(Material.STONE_PICKAXE);
                p.getInventory().remove(Material.DIAMOND_PICKAXE);
                p.getInventory().remove(Material.IRON_PICKAXE);
                p.getInventory().remove(Material.NETHERITE_PICKAXE);
                MessageUtil.broadcastInfo(p.getName() + " 受到魚骨懲罰");
                BukkitUtil.playSoundToAll(Sound.BLOCK_ANVIL_PLACE, 1f, 0);
            }
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

    //生物受到傷害
    @EventHandler
    public void onEntityDamageEvent(EntityDamageEvent event) {
        //如果不是玩家受到傷害
        if (!event.getEntity().getType().equals(EntityType.PLAYER)) {
            return;
        }
        //如果遊戲尚未進行
        if (!isRunning()) {
            event.setCancelled(true);
            return;
        }
        //無敵時間
        if (isInvincible) {
            event.setCancelled(true);
        }
    }

    // 玩家重生
    @EventHandler
    public void onPlayerRespawnEvent(PlayerRespawnEvent event) {
        var player = UHCPlayer.getUHCPlayer(event.getPlayer().getUniqueId());
        //取得擊殺者
        var killer = event.getPlayer().getKiller();
        //設置玩家為冒險者模式
        player.setGameMode(GameMode.ADVENTURE);
        //傳送至重生點
        event.setRespawnLocation(UHCConfig.getInstance().getSpawn());
        //延遲3秒後執行
        TaskUtil.syncTask(() -> {
            //判斷遊戲是否正在進行
            if (isRunning()) {
                //判斷玩家是否再線上
                player.ifOnline(p -> {
                    //將玩家進入觀察者模式
                    p.setGameMode(GameMode.SPECTATOR);
                    //如果獲取死亡位置失敗，將會給重生點
                    var spawn = Optional.ofNullable(player.getDeadLocation()).orElse(UHCConfig.getInstance().getSpawn());
                    if (killer != null) {
                        Player target = killer;
                        while (target.getKiller() != null && target.getKiller().isOnline()) {
                            target = target.getKiller();
                        }
                        //取得擊殺者位置
                        spawn = target.getLocation();
                    }
                    p.teleport(spawn);
                    Optional.ofNullable(killer).ifPresent(p::setSpectatorTarget);
                });
            }
        }, 40);
    }

    // 生物回血
    @EventHandler
    public void onEntityRegainHealthEvent(EntityRegainHealthEvent event) {
        //允許藥水回血
        if (EntityRegainHealthEvent.RegainReason.MAGIC.equals(event.getRegainReason()) || EntityRegainHealthEvent.RegainReason.MAGIC_REGEN.equals(event.getRegainReason())) {
            return;
        }
        //遊戲開始，阻止所有回血
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

        var now = Calendar.getInstance().getTimeInMillis();
        var config = UHCConfig.getInstance();
        //確認冷卻時間
        if (now - deadMusicCoolDown > config.getDeadMusicCoolDown() * 1000) {
            deadMusicCoolDown = now;
            var music = config.getDeadMusic();
            //播放死亡音效
            BukkitUtil.playSoundToAll(music, 1f, PITCH[random.nextInt(2)]);
        }

        //檢查
        check();
    }

    //傳送事件
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
            if (player.isInit()) {
                return;
            }
            //初始化玩家
            player.init();
            //取得遊戲開始重生點
            var spawn = Optional.ofNullable(team.getStartSpawn()).orElseGet(() -> {
                //當取得不到重生點，至少給一開始進入伺服器的眾生點
                MessageUtil.broadcastError(String.format("隊伍 %s 找不到重生點", team.getColor().name()));
                return UHCConfig.getInstance().getSpawn();
            });
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
            Optional.ofNullable(team).ifPresent(t -> {
                t.kickPlayer(player);
            });
            //標記該玩家為離線
            player.offline();
        }
    }

    //遊戲是否正在進行
    public boolean isRunning() {
        return isRunning;
    }
}
