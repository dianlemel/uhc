package com.dianlemel.huc.item;

import com.dianlemel.huc.UHCController;
import com.dianlemel.huc.util.BukkitUtil;
import com.dianlemel.huc.util.ItemUtil;
import com.dianlemel.huc.util.MapData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;

import java.util.List;
import java.util.stream.Collectors;

public class InstantHealthBook extends AbstractItem {

    private final int health;
    private final List<PotionEffect> effects;

    public InstantHealthBook(MapData data) {
        super(data);
        health = data.getInteger("health");
        effects = data.getMapList("effects").stream().map(AbstractItem::toEffect).collect(Collectors.toList());
    }

    @Override
    public ItemType getType() {
        return ItemType.INSTANT_HEALTH_BOOK;
    }

    @Override
    public void init() {

    }

    //玩家互動
    @EventHandler
    public void onPlayerInteractEvent(PlayerInteractEvent event) {
        //如果遊戲尚未開始
        if (!UHCController.getInstance().isRunning()) {
            return;
        }
        //如果不是主手
        if (!EquipmentSlot.HAND.equals(event.getHand())) {
            return;
        }
        //取得主手上的物品
        var item = event.getItem();
        //如果主手沒拿東西
        if (item == null) {
            return;
        }
        //取得該物品的KEY
        var key = ItemUtil.getKey(item);
        //如果不是自定義物品
        if (getKey().equals(key)) {
            return;
        }
        //減少一個或移除
        item.setAmount(item.getAmount() - 1);
        //取得互動玩家
        var player = event.getPlayer();
        //給予該玩家所有效果
        effects.forEach(player::addPotionEffect);
        //恢復血量
        BukkitUtil.setHealth(player, health);
    }
}
