package com.dianlemel.huc.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.bukkit.*;
import org.bukkit.configuration.MemorySection;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public class MapData {

    private final Map<String, Object> data;

    public MapData() {
        data = new HashMap<>();
    }

    public MapData(MemorySection data) {
        this.data = data.getValues(true);
    }

    @SuppressWarnings("unchecked")
    public MapData(Map<?, ?> data) {
        this.data = (Map<String, Object>) data;
    }

    public boolean getBoolean(String key) {
        return (boolean) data.get(key);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        Object obj = data.get(key);
        return (boolean) (obj == null ? defaultValue : obj);
    }

    public boolean containsKey(String key) {
        return data.containsKey(key);
    }

    public Set<String> keySet() {
        return data.keySet();
    }

    public Object getObject(String key) {
        return data.get(key);
    }

    public String getString(String key) {
        return (String) data.get(key);
    }

    public ChatColor getColor(String key) {
        String c = getString(key);
        if (c != null) {
            return ChatColor.getByChar(c);
        }
        return null;
    }

    public <T extends Enum<T>> T getType(String key, Class<T> enumClass) {
        return getType(key, enumClass, null);
    }

    public <T extends Enum<T>> T getType(String key, Class<T> enumClass, T defaultEnum) {
        var value = getString(key);
        if (value == null) {
            return defaultEnum;
        }
        try {
            var method = enumClass.getDeclaredMethod("valueOf", String.class);
            return (T) method.invoke(method, value);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultEnum;
        }
    }

    public List<Material> getMaterialList(String key) {
        return getStringList(key).stream().map(Material::valueOf).collect(Collectors.toList());
    }

    public Material getMaterial(String key) {
        return Material.valueOf(Optional.ofNullable(getString(key)).orElseGet(() -> "NULL"));
    }

    @SuppressWarnings("unchecked")
    public List<Integer> getIntegerList(String key) {
        Object obj = data.get(key);
        return (List<Integer>) (obj == null ? Lists.newArrayList() : obj);
    }

    @SuppressWarnings("unchecked")
    public List<String> getStringList(String key) {
        Object obj = data.get(key);
        return (List<String>) (obj == null ? Lists.newArrayList() : obj);
    }

    @SuppressWarnings("unchecked")
    public List<MapData> getMapList(String key) {
        Object obj = data.get(key);
        if (obj == null) {
            return Lists.newArrayList();
        }
        return ((List<Map<?, ?>>) obj).stream().map(MapData::new).collect(Collectors.toList());
    }

    public int getInteger(String key) {
        return (int) data.get(key);
    }

    public GameMode getGameMode(String key) {
        return GameMode.valueOf(getString(key));
    }

    public int getInteger(String key, int defaultValue) {
        Object obj = data.get(key);
        return (int) (obj == null ? defaultValue : obj);
    }

    public List<Location> getLocations(String key) {
        List<String> list = (List<String>) getList(key);
        return list.stream().map(LocationUtil::stringToLoc).collect(Collectors.toList());
    }

    public List<?> getList(String key) {
        Object obj = data.get(key);
        return (List<?>) (obj == null ? Lists.newArrayList() : obj);
    }

    public double getDouble(String key) {
        return (double) data.get(key);
    }

    public double getDouble(String key, double defaultValue) {
        Object obj = data.get(key);
        return (double) (obj == null ? defaultValue : obj);
    }

    public float getFloat(String key) {
        return (float) getDouble(key);
    }

    public World getWorld(String key) {
        return Bukkit.getWorld(getString(key));
    }

    public List<World> getWorlds(String key) {
        return getStringList(key).stream().map(Bukkit::getWorld).collect(Collectors.toList());
    }

    public Location getLocation(String key) {
        String v = getString(key);
        if (v == null) {
            return null;
        }
        return LocationUtil.stringToLoc(v);
    }

    public Range getRange(String key) {
        return new Range(getMapData(key));
    }

    public Range getRange() {
        return new Range(new MapData(data));
    }

    public MapData getMapData(String key) {
        Object object = data.get(key);
        if (object instanceof Map map) {
            return new MapData(map);
        } else if (object instanceof MemorySection memorySection) {
            return new MapData(memorySection.getValues(false));
        }
        return null;
    }

    public Map<String, Object> getMap(String key) {
        Map<String, Object> map = Maps.newConcurrentMap();
        Object object = data.get(key);
        if (object instanceof MemorySection ms) {
            ms.getValues(false).forEach((key1, value) -> {
                if (value instanceof MemorySection memorySection) {
                    map.put(key1, new MapData(memorySection));
                } else {
                    map.put(key1, value);
                }
            });
        }
        return map;
    }

    public void set(String key, Object value) {
        data.put(key, value);
    }

    @Override
    public String toString() {
        return "MapData [data=" + data + "]";
    }

}
