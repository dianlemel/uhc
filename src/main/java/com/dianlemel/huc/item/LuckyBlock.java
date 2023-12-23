package com.dianlemel.huc.item;

import com.dianlemel.huc.UHCController;
import com.dianlemel.huc.util.ItemUtil;
import com.dianlemel.huc.util.MapData;
import com.google.common.collect.Lists;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class LuckyBlock extends RecipeItem {


    private static final Random random = new Random();
    private final List<Location> locationList = Lists.newArrayList();
    private List<RandomItem> items = Lists.newArrayList();

    public LuckyBlock(MapData data) {
        super(data);
    }

    @Override
    public ItemType getType() {
        return ItemType.LUCKY_BLOCK;
    }

    @Override
    protected void register(MapData data) {
        super.register(data);
        items = data.getMapList("randomItem").stream().map(RandomItem::new).collect(Collectors.toList());
    }

    @Override
    public void init() {
        //清除所有位置
        locationList.clear();
    }

    //活塞伸出去
    @EventHandler
    public void onBlockPistonExtendEvent(BlockPistonExtendEvent event) {
        //如果遊戲尚未開始
        if (!UHCController.getInstance().isRunning()) {
            return;
        }
        //取得偏移量
        var vector = event.getDirection().getDirection();
        onBlockPistonEvent(vector, event.getBlocks());
    }

    //活塞縮回來
    @EventHandler
    public void onBlockPistonEvent(BlockPistonRetractEvent event) {
        //如果遊戲尚未開始
        if (!UHCController.getInstance().isRunning()) {
            return;
        }
        //取得座標變更
        var vector = event.getDirection().getDirection();
        onBlockPistonEvent(vector, event.getBlocks());
    }

    //計算方塊新的位置
    private void onBlockPistonEvent(Vector vector, List<Block> blocks) {
        //取得所有被更動的方塊
        blocks.forEach(block -> {
            var location = block.getLocation();
            //判斷該方塊位置是特殊方塊，如果是的話將會移除
            if (locationList.remove(location)) {
                //該座標加上偏移量，得到該方塊被黏回來/推出去後的新位置
                location.add(vector);
                //儲存起來
                locationList.add(location);
            }
        });
    }

    //玩家放置方塊
    @EventHandler
    public void onBlockPlaceEvent(BlockPlaceEvent event) {
        //如果遊戲尚未開始
        if (!UHCController.getInstance().isRunning()) {
            var player = event.getPlayer();
            if (!player.isOp()) {
                return;
            }
        }
        //如果不是主手
        if (!EquipmentSlot.HAND.equals(event.getHand())) {
            return;
        }
        //取得主手上的物品
        var item = event.getItemInHand();
        //分析該物品的KEY值
        var key = ItemUtil.getKey(item);
        //如果不是LuckyBlock
        if (!getKey().equals(key)) {
            return;
        }
        //取得放置的位置
        var location = event.getBlockPlaced().getLocation();
        //紀錄該位置為特殊方塊
        locationList.add(location);
    }

    //玩家破壞方塊
    @EventHandler
    public void onBlockBreakEvent(BlockBreakEvent event) {
        //如果遊戲尚未開始
        if (!UHCController.getInstance().isRunning()) {
            var player = event.getPlayer();
            if (!player.isOp()) {
                return;
            }
        }
        //獲得該方塊位置
        var location = event.getBlock().getLocation();
        //如果該方塊位置不是特殊方塊
        if (!locationList.remove(event.getBlock().getLocation())) {
            return;
        }
        //阻止該方塊掉落任何物品
        event.setDropItems(false);
        //獲得該位置的世界
        var world = location.getWorld();
        //過濾所有中獎的RandomItem，並且生成物品
        items.stream().filter(RandomItem::inChance).map(RandomItem::create).forEach(item -> {
            //生成出的物品，丟到該世界的指定位置上附近
            world.dropItemNaturally(location, item);
        });
    }

    private static class RandomItem {

        //機率(1~100)
        private final int chance;
        //材質
        private final Material material;
        //數量
        private final int amount;
        //原始資料
        private final MapData mapData;

        RandomItem(MapData mapData) {
            chance = mapData.getInteger("chance");
            material = mapData.getMaterial("material");
            amount = mapData.getInteger("amount");
            this.mapData = mapData;
        }

        //是否中獎
        boolean inChance() {
            return chance >= random.nextInt(100) + 1;
        }

        //新的物品
        ItemStack create() {
            var item = new ItemStack(material);
            var itemMeta = item.getItemMeta();
            if (itemMeta instanceof PotionMeta potionMeta && mapData.containsKey("effects")) {
                mapData.getMapList("effects").stream().map(BaseItem::toEffect).forEach(effect -> {
                    potionMeta.addCustomEffect(effect, true);
                });
            }
            item.setItemMeta(itemMeta);
            item.setAmount(amount);
            return item;
        }

    }
}
