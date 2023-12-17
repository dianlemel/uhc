package com.dianlemel.huc.item;

import com.dianlemel.huc.UHCCore;
import com.dianlemel.huc.util.MapData;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;

import java.util.UUID;

abstract class RecipeItem extends BaseItem {
    private static final UUID uuid = UUID.randomUUID();

    protected final NamespacedKey recipeNamespacedKey;

    public RecipeItem(MapData data) {
        super(data);
        recipeNamespacedKey = new NamespacedKey(UHCCore.getPlugin(), uuid + "_" + getKey());
    }

    @Override
    protected void register(MapData data) {
        var recipe = data.getStringList("recipe");
        var ingredient = data.getMap("ingredient");
        var item = createItem();
        var shapedRecipe = new ShapedRecipe(recipeNamespacedKey, item);
        shapedRecipe.shape(recipe.toArray(new String[0]));
        ingredient.forEach((k, m) -> {
            shapedRecipe.setIngredient(k.charAt(0), Material.valueOf((String) m));
        });
        UHCCore.getPlugin().getServer().addRecipe(shapedRecipe);
    }

    @Override
    protected void unregister() {
        UHCCore.getPlugin().getServer().removeRecipe(recipeNamespacedKey);
    }
}
