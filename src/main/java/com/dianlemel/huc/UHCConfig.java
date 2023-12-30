package com.dianlemel.huc;

import com.dianlemel.huc.util.MapData;
import com.dianlemel.huc.util.Range;
import org.bukkit.Location;

import java.util.List;
import java.util.Optional;

public class UHCConfig {

    private static UHCConfig config;

    public static UHCConfig getInstance() {
        return Optional.ofNullable(config).orElseGet(() -> {
            UHCCore.getPlugin().saveDefaultConfig();
            config = new UHCConfig();
            return config;
        });
    }

    private Location center;
    private String deadMusic;
    private int deadMusicCoolDown;
    private Range spawn;
    private int borderMinRadius;
    private int borderMaxRadius;
    private String borderMusic;
    private int progressiveTriggerRule;
    private int baselineThreshold;
    private List<MapData> punishedEffects;
    private List<MapData> specialItems;
    private int minDistance;
    private int spawnY;
    private int invincible;

    private UHCConfig() {
        loadConfig();
    }

    public void loadConfig() {
        UHCCore.getPlugin().reloadConfig();
        var plugin = UHCCore.getPlugin();
        var config = new MapData(plugin.getConfig().getValues(false));
        center = config.getLocation("center");
        deadMusic = config.getString("deadMusic");
        deadMusicCoolDown = config.getInteger("deadMusicCoolDown");
        spawn = config.getRange("spawn");

        var border = config.getMapData("border");
        borderMinRadius = border.getInteger("minRadius");
        borderMaxRadius = border.getInteger("maxRadius");
        borderMusic = border.getString("music");

        var start = config.getMapData("start");
        invincible = start.getInteger("invincible");
        spawnY = start.getInteger("spawnY");
        minDistance = start.getInteger("minDistance");
        var punished = start.getMapData("punished");
        progressiveTriggerRule = punished.getInteger("progressiveTriggerRule");
        baselineThreshold = punished.getInteger("baselineThreshold");
        punishedEffects = punished.getMapList("effects");

        specialItems = config.getMapList("specialItem");
    }

    public int getInvincible() {
        return invincible;
    }

    public String getBorderMusic() {
        return borderMusic;
    }

    public int getDeadMusicCoolDown() {
        return deadMusicCoolDown;
    }

    public Location getCenter() {
        return center.clone();
    }

    public Location getSpawn() {
        return spawn.getLocationForRandom();
    }

    public int getBorderMinRadius() {
        return borderMinRadius;
    }

    public int getBorderMaxRadius() {
        return borderMaxRadius;
    }

    public String getDeadMusic() {
        return deadMusic;
    }

    public List<MapData> getSpecialItems() {
        return specialItems;
    }

    public int getMinDistance() {
        return minDistance;
    }

    public int getSpawnY() {
        return spawnY;
    }

    public int getProgressiveTriggerRule() {
        return progressiveTriggerRule;
    }

    public int getBaselineThreshold() {
        return baselineThreshold;
    }

    public List<MapData> getPunishedEffects() {
        return punishedEffects;
    }
}
