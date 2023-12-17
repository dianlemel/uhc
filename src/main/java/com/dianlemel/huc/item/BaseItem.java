package com.dianlemel.huc.item;

import com.dianlemel.huc.UHCConfig;
import com.dianlemel.huc.UHCCore;
import com.dianlemel.huc.util.ItemUtil;
import com.dianlemel.huc.util.MapData;
import com.dianlemel.huc.util.MessageUtil;
import com.google.common.collect.Maps;
import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class BaseItem implements Listener {

    public enum ItemType {
        INSTANT_HEALTH_BOOK, LUCKY_BLOCK
    }

    private static final Map<ItemType, Class<? extends BaseItem>> register = Maps.newConcurrentMap();
    private static final Map<String, BaseItem> items = Maps.newConcurrentMap();

    public static BaseItem getItem(String key) {
        return items.get(key);
    }

    public static Collection<BaseItem> getItems() {
        return items.values();
    }

    public static void loadItem() {
        clear();

        UHCConfig.getInstance().getSpecialItems().forEach(data -> {
            var type = data.getType("type", ItemType.class);
            if (type == null) {
                MessageUtil.sendError("未知的物品種類: " + data.getString("type"));
            } else {
                var classes = register.get(type);
                try {
                    var item = classes.getDeclaredConstructor(MapData.class).newInstance(data);
                    items.put(item.getKey(), item);
                    UHCCore.getPlugin().getServer().getPluginManager().registerEvents(item, UHCCore.getPlugin());
                    item.register(data);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void loadData() {
        UHCConfig.getInstance().getSpecialItems().forEach(data -> {
            var key = data.getString("key");
            var item = items.get(key);
            item.unregister();
            item.register(data);
        });
    }

    public static PotionEffect toEffect(MapData mapData) {
        var type = PotionEffectType.getByName(mapData.getString("name"));
        var time = mapData.getInteger("time", 100);
        var level = mapData.getInteger("level", 0);
        var ambient = mapData.getBoolean("ambient", true);
        var particles = mapData.getBoolean("particles", true);
        return new PotionEffect(type, time * 20, level, ambient, particles);
    }

    public static void clear() {
        items.values().forEach(BaseItem::unregister);
        items.clear();
    }

    public static void initItems() {
        items.values().forEach(BaseItem::init);
    }

    static {
        register.put(ItemType.INSTANT_HEALTH_BOOK, InstantHealthBook.class);
        register.put(ItemType.LUCKY_BLOCK, LuckyBlock.class);
    }

    private final String key;
    private final String name;
    private final Material material;
    private final List<String> lore;
    private final List<String> recipe;
    private final Map<String, Object> ingredient;

    public BaseItem(MapData data) {
        key = data.getString("key");
        name = data.getString("name");
        material = data.getMaterial("material");
        lore = data.getStringList("lore");
        recipe = data.getStringList("recipe");
        ingredient = data.getMap("ingredient");
    }

    //註冊
    abstract void register(MapData data);

    //撤銷
    abstract void unregister();

    //建立物品
    public ItemStack createItem() {
        var item = new ItemStack(material);
        var itemMeta = item.getItemMeta();
        modifyItemMeta(itemMeta);
        item.setItemMeta(itemMeta);
        return item;
    }

    //複寫請一定要呼叫super
    public void modifyItemMeta(ItemMeta itemMeta) {
        itemMeta.setLore(lore);
        itemMeta.setDisplayName(name);
        ItemUtil.setItemKey(itemMeta, key);
    }

    public String getKey() {
        return key;
    }

    public abstract void init();

    public abstract ItemType getType();

}
