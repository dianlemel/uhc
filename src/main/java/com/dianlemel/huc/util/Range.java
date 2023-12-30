package com.dianlemel.huc.util;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class Range {

    private static final String LOCATION_R1 = "R1";
    private static final String LOCATION_R2 = "R2";
    private static final Random random = new Random();

    private final Location r1;
    private final Location r2;

    public Range(MapData data) {
        r1 = data.getLocation(LOCATION_R1);
        r2 = data.getLocation(LOCATION_R2);
    }

    public Range(Location r1, Location r2) {
        this.r1 = r1;
        this.r2 = r2;
    }

    public World getWorld() {
        return r1.getWorld();
    }

    public boolean inRange(Location target, boolean ignoreHeight) {
        return LocationUtil.inRange(target, this, ignoreHeight);
    }

    public Location getLocationForRandom() {
        return BukkitUtil.random(toList());
    }

    public boolean inChunk(Chunk chunk) {
        var maxX = Math.max(r1.getChunk().getX(), r2.getChunk().getX());
        var maxZ = Math.max(r1.getChunk().getZ(), r2.getChunk().getZ());
        var minX = Math.min(r1.getChunk().getX(), r2.getChunk().getX());
        var minZ = Math.min(r1.getChunk().getZ(), r2.getChunk().getZ());
        var x = chunk.getX();
        var z = chunk.getZ();
        return minX <= x && x <= maxX && minZ <= z && z <= maxZ;
    }

    public boolean inRange(Location target) {
        return inRange(target, false);
    }

    public boolean inRange(double x, double y, double z) {
        return LocationUtil.inRange(x, y, z, this);
    }

    public boolean inRange(double x, double y, double z, boolean ignoreHeight) {
        return LocationUtil.inRange(x, y, z, this, ignoreHeight);
    }

    public Location getR1() {
        return r1;
    }

    public Location getR2() {
        return r2;
    }

    public int getMaxX() {
        return Math.max(r1.getBlockX(), r2.getBlockX());
    }

    public int getMaxZ() {
        return Math.max(r1.getBlockZ(), r2.getBlockZ());
    }

    public int getMaxY() {
        return Math.max(r1.getBlockY(), r2.getBlockY());
    }

    public int getMinX() {
        return Math.min(r1.getBlockX(), r2.getBlockX());
    }

    public int getMinZ() {
        return Math.min(r1.getBlockZ(), r2.getBlockZ());
    }

    public int getMinY() {
        return Math.min(r1.getBlockY(), r2.getBlockY());
    }

    public List<Location> toList() {
        return LocationUtil.rangeToList(r2, r1);
    }

    public List<Location> generateRandomLocation(int size, double defaultY, int minDistance) {
        var points = new ArrayList<Vector>();
        var lowerLeft = new Vector(Math.min(r1.getX(), r2.getX()), 0, Math.min(r1.getZ(), r2.getZ()));
        var upperRight = new Vector(Math.max(r1.getX(), r2.getX()), 0, Math.max(r1.getZ(), r2.getZ()));
        for (int i = 0; i < size; i++) {
            for (int j = 1; j <= 100; j++) {
                var x = lowerLeft.getX() + random.nextDouble() * (upperRight.getX() - lowerLeft.getX());
                var z = lowerLeft.getZ() + random.nextDouble() * (upperRight.getZ() - lowerLeft.getZ());
                var vector = new Vector(x, defaultY, z);
                if (points.stream().noneMatch(point -> point.distance(vector) < minDistance)) {
                    points.add(vector);
                    break;
                }
                if (j == 100) {
                    return null;
                }
            }
        }
        return points.stream().map(point -> point.toLocation(getWorld())).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return String.format("r1: %s ,r2: %s", LocationUtil.locToStringXYZ(r1), LocationUtil.locToStringXYZ(r2));
    }

    @Override
    protected Range clone() {
        return new Range(r1.clone(), r2.clone());
    }

}
