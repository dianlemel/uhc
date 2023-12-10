package com.dianlemel.huc.util;

import com.dianlemel.huc.UHCCore;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.nio.ByteBuffer;

public class ItemUtil {
    private static final NamespacedKey namespacedKey = new NamespacedKey(UHCCore.getPlugin(), "UHC");

    public static String getKey(ItemStack item) {
        if (item != null && item.hasItemMeta()) {
            var meta = item.getItemMeta();
            if (meta.getPersistentDataContainer().has(namespacedKey, PersistentDataType.BYTE_ARRAY)) {
                var data = meta.getPersistentDataContainer().get(namespacedKey,
                        PersistentDataType.BYTE_ARRAY);
                ByteBuffer buffer = ByteBuffer.wrap(data);
                byte[] bytes = new byte[buffer.get()];
                buffer.get(bytes);
                return new String(bytes);
            }
        }
        return null;
    }

    public static void setItemKey(ItemMeta meta, String key) {
        byte[] keyByte = key.getBytes();
        ByteBuffer buffer = ByteBuffer.allocate(16 + keyByte.length + 1);
        buffer.put((byte) keyByte.length);
        buffer.put(keyByte);
        meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.BYTE_ARRAY, buffer.array());
    }

}
