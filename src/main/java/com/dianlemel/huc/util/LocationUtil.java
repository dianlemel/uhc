package com.dianlemel.huc.util;

import com.google.common.collect.Lists;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LocationUtil {

    private static final String SPLIT = ",";

    public static boolean equals(Location l1, Location l2) {
        return l1.getBlockX() == l2.getBlockX() && l1.getBlockY() == l2.getBlockY() && l1.getBlockZ() == l2.getBlockZ();
    }

    public static Vector toVector(String s) {
        String[] split = s.split(SPLIT);
        return new Vector(Double.parseDouble(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]));
    }

    public static Vector getVectorBetween(Location to, Location from) {
        Vector dir = new Vector();
        dir.setX(to.getX() - from.getX());
        dir.setY(to.getY() - from.getY());
        dir.setZ(to.getZ() - from.getZ());
        return dir;
    }

    public static Location min(Location L1, Location L2) {
        Location loc = L1.clone();
        loc.setX(Math.min(L1.getX(), L2.getX()));
        loc.setY(Math.min(L1.getY(), L2.getY()));
        loc.setZ(Math.min(L1.getZ(), L2.getZ()));
        return loc;
    }

    public static Location max(Location L1, Location L2) {
        Location loc = L1.clone();
        loc.setX(Math.max(L1.getX(), L2.getX()));
        loc.setY(Math.max(L1.getY(), L2.getY()));
        loc.setZ(Math.max(L1.getZ(), L2.getZ()));
        return loc;
    }

    public static boolean inRange(Location R1, Location R2, double v) {
        if (!R1.getWorld().equals(R2.getWorld())) {
            return false;
        }
        return R1.distance(R2) <= v;
    }

    public static boolean inRange(Location target, Range range, boolean ignoreHeight) {
        return inRange(target, range.getR1(), range.getR2(), ignoreHeight);
    }

    public static boolean inRange(Location target, Range range) {
        return inRange(target, range.getR1(), range.getR2());
    }

    public static boolean inRange(double x, double y, double z, Range range, boolean ignoreHeight) {
        return inRange(x, y, z, range.getR1(), range.getR2(), ignoreHeight);
    }

    public static boolean inRange(double x, double y, double z, Range range) {
        return inRange(x, y, z, range.getR1(), range.getR2());
    }

    public static boolean inRange(Location target, Location r1, Location r2, boolean ignoreHeight) {
        if (!target.getWorld().equals(r1.getWorld())) {
            return false;
        }
        return inRange(target.getX(), target.getY(), target.getZ(), r1, r2, ignoreHeight);
    }

    public static boolean inRange(Location target, Location r1, Location r2) {
        if (!target.getWorld().equals(r1.getWorld())) {
            return false;
        }
        return inRange(target.getX(), target.getY(), target.getZ(), r1, r2);
    }

    public static boolean inRange(double x, double y, double z, Location r1, Location r2) {
        return inRange(x, y, z, r1, r2, false);
    }

    public static boolean inRange(double x, double y, double z, Location r1, Location r2, boolean ignoreHeight) {
        double maxX = Math.max(r1.getX(), r2.getX()) + 0.5;
        double maxY = Math.max(r1.getY(), r2.getY()) + 0.5;
        double maxZ = Math.max(r1.getZ(), r2.getZ()) + 0.5;
        double minX = Math.min(r1.getX(), r2.getX()) - 0.5;
        double minY = Math.min(r1.getY(), r2.getY()) - 0.5;
        double minZ = Math.min(r1.getZ(), r2.getZ()) - 0.5;
        if (minX <= x && x <= maxX) {
            if (ignoreHeight || (minY <= y && y <= maxY)) {
                return minZ <= z && z <= maxZ;
            }
        }
        return false;
    }

    public static List<Location> rangeToList(Location r1, Location r2) {
        if (!Objects.equals(r1.getWorld(), r2.getWorld())) {
            return Lists.newArrayList();
        }
        double minX = Math.min(r1.getX(), r2.getX());
        double minY = Math.min(r1.getY(), r2.getY());
        double minZ = Math.min(r1.getZ(), r2.getZ());
        double maxX = Math.max(r1.getX(), r2.getX());
        double maxY = Math.max(r1.getY(), r2.getY());
        double maxZ = Math.max(r1.getZ(), r2.getZ());
        List<Location> list = Lists.newArrayList();
        for (double x = minX; x <= maxX; x++) {
            for (double y = minY; y <= maxY; y++) {
                for (double z = minZ; z <= maxZ; z++) {
                    Vector v = new Vector(x, y, z);
                    list.add(v.toLocation(r1.getWorld()));
                }
            }
        }
        return list;
    }

    public static Range calculate2DRange(Location center, double radius) {
        Vector r1 = new Vector(center.getX() - radius, 0, center.getZ() + radius);
        Vector r2 = new Vector(center.getX() + radius, 0, center.getZ() - radius);
        return new Range(r1.toLocation(center.getWorld()), r2.toLocation(center.getWorld()));
    }

    public static String locToString(Location loc) {
        return String.format("%s,%f,%f,%f,%f,%f", loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(),
                loc.getPitch(), loc.getYaw());
    }

    public static String locToStringXYZPY(Location loc) {
        return String.format("%f,%f,%f,%f,%f", loc.getX(), loc.getY(), loc.getZ(), loc.getPitch(), loc.getYaw());
    }

    public static String locToStringXYZ(Location loc) {
        return String.format("%f,%f,%f", loc.getX(), loc.getY(), loc.getZ());
    }

    public static Location stringToLoc(String text) {
        String[] split = text.split(SPLIT);
        World world = Bukkit.getWorld(split[0]);
        if (world == null) {
            throw new NullPointerException("找不到世界 " + split[0]);
        }
        if (split.length == 1) {
            return stringToLoc(world);
        } else {
            String[] newArr = new String[split.length - 1];
            System.arraycopy(split, 1, newArr, 0, split.length - 1);
            return stringToLoc(world, newArr);
        }
    }

    public static Location stringToLoc(World world, String text) {
        if (text == null || text.isEmpty()) {
            throw new NullPointerException("text 是空值");
        }
        if (world == null) {
            throw new NullPointerException("world 是空值");
        }
        String[] split = text.split(SPLIT);
        return stringToLoc(world, split);
    }

    public static Location stringToLoc(World world) {
        if (world == null) {
            throw new NullPointerException();
        }
        return stringToLoc(world, "0,0,0");
    }

    public static Location stringToLoc(World world, String[] split) {
        if (world == null) {
            throw new NullPointerException();
        }
        if (split.length == 3) {
            return new Location(world, Double.parseDouble(split[0]), Double.parseDouble(split[1]),
                    Double.parseDouble(split[2]));
        } else if (split.length == 5) {
            return new Location(world, Double.parseDouble(split[0]), Double.parseDouble(split[1]),
                    Double.parseDouble(split[2]), Float.parseFloat(split[3]), Float.parseFloat(split[4]));
        } else {
            throw new RuntimeException("LocationManager 轉換格式錯誤 " + Stream.of(split).collect(Collectors.joining(",")));
        }
    }

    /*
    yaw = rotX
    pitch = rotY
     */
    public static Vector getDirection(float rotX, float rotY) {
        var vector = new Vector();
        vector.setY(-Math.sin(Math.toRadians(rotY)));
        var xz = Math.cos(Math.toRadians(rotY));
        vector.setX(-xz * Math.sin(Math.toRadians(rotX)));
        vector.setZ(xz * Math.cos(Math.toRadians(rotX)));
        return vector;
    }

}
