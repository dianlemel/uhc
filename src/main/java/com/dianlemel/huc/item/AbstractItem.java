package com.dianlemel.huc.item;

import com.dianlemel.huc.UHCConfig;
import com.dianlemel.huc.UHCCore;
import com.dianlemel.huc.util.ItemUtil;
import com.dianlemel.huc.util.MapData;
import com.dianlemel.huc.util.MessageUtil;
import com.google.common.collect.Maps;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class AbstractItem implements Listener {

    public enum ItemType {
        INSTANT_HEALTH_BOOK, LUCKY_BLOCK
    }

    private static final Map<ItemType, Class<? extends AbstractItem>> register = Maps.newConcurrentMap();
    private static final Map<String, AbstractItem> items = Maps.newConcurrentMap();
    private static final UUID uuid = UUID.randomUUID();

    public static AbstractItem getItem(String key) {
        return items.get(key);
    }

    public static Collection<AbstractItem> getItems(){
        return items.values();
    }

    public static void load() {
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
                    item.register();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
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
        items.values().forEach(AbstractItem::unregister);
        items.clear();
    }

    public static void initItems() {
        items.values().forEach(AbstractItem::init);
    }

    static {
        register.put(ItemType.INSTANT_HEALTH_BOOK, InstantHealthBook.class);
        register.put(ItemType.LUCKY_BLOCK, LuckyBlock.class);
    }

    protected final NamespacedKey recipeNamespacedKey;
    private final String key;
    private final String name;
    private final Material material;
    private final List<String> lore;
    private final List<String> recipe;
    private final Map<String, Object> ingredient;

    public AbstractItem(MapData data) {
        key = data.getString("key");
        name = data.getString("name");
        material = data.getMaterial("material");
        lore = data.getStringList("lore");
        recipe = data.getStringList("recipe");
        ingredient = data.getMap("ingredient");
        recipeNamespacedKey = new NamespacedKey(UHCCore.getPlugin(), uuid + "_" + key);
    }

    //註冊合成表
    protected void register() {
        var item = createItem();
        var shapedRecipe = new ShapedRecipe(recipeNamespacedKey, item);
        shapedRecipe.shape(this.recipe.toArray(new String[0]));
        ingredient.forEach((k, m) -> {
            shapedRecipe.setIngredient(k.charAt(0), Material.valueOf((String) m));
        });
        UHCCore.getPlugin().getServer().addRecipe(shapedRecipe);
        UHCCore.getPlugin().getServer().getPluginManager().registerEvents(this, UHCCore.getPlugin());
    }

    //移除合成表
    protected void unregister() {
        UHCCore.getPlugin().getServer().removeRecipe(recipeNamespacedKey);
    }

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
