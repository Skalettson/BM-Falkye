package com.bmfalkye.items;

import com.bmfalkye.blocks.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Предмет Картографического Стола для крафта
 */
public class CartographyTableItem extends BlockItem {
    public CartographyTableItem(Properties properties) {
        super(ModBlocks.CARTOGRAPHY_TABLE.get(), properties);
    }
    
    @Override
    public int getBurnTime(ItemStack itemStack, RecipeType<?> recipeType) {
        return 0; // Не горит
    }
}

