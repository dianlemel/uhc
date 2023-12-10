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
            config = new UHCConfig();
            return config;
        });
    }

    private Location center;
    private String deadMusic;
    private Range spawn;
    private int borderTimer;
    private int borderMinRadius;
    private int borderMaxRadius;
    private int mineralPunish;
    private int clearMonsterTimer;
    private List<MapData> specialItems;
    private int minDistance;
    private int spawnY;

    private UHCConfig() {
        loadConfig();
    }

    public void loadConfig() {
        var plugin = UHCCore.getPlugin();
        var config = new MapData(plugin.getConfig().getValues(false));
        center = config.getLocation("center");
        deadMusic = config.getString("deadMusic");
        spawn = config.getRange("spawn");

        var board = config.getMapData("board");
        borderTimer = board.getInteger("time");
        borderMinRadius = board.getInteger("minRadius");
        borderMaxRadius = board.getInteger("maxRadius");

        var start = config.getMapData("start");
        mineralPunish = start.getInteger("mineralPunish");
        clearMonsterTimer = start.getInteger("clearMonsterTimer");
        minDistance = start.getInteger("minDistance");
        spawnY = start.getInteger("spawnY");

        specialItems = config.getMapList("specialItem");
    }

    public Location getCenter() {
        return center.clone();
    }

    public Location getSpawn() {
        return spawn.getLocationForRandom();
    }

    public int getBorderTimer() {
        return borderTimer;
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

    public int getMineralPunish() {
        return mineralPunish;
    }

    public int getClearMonsterTimer() {
        return clearMonsterTimer;
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
}
