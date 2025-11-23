package com.bmfalkye.integration;

import com.bmfalkye.BMFalkye;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Интеграция с Curios API для ношения карт как аксессуаров
 * Использует рефлексию для безопасного доступа без обязательной зависимости
 */
public class CuriosIntegration {
    
    /**
     * Проверяет, установлен ли мод Curios
     */
    public static boolean isCuriosLoaded() {
        return net.minecraftforge.fml.ModList.get().isLoaded("curios");
    }
    
    /**
     * Проверяет, может ли игрок носить карту в слоте Curios
     */
    public static boolean canWearCard(LivingEntity entity, ItemStack cardStack) {
        if (!isCuriosLoaded() || entity == null || cardStack.isEmpty()) {
            return false;
        }
        
        try {
            // Используем рефлексию для безопасного доступа к Curios API
            Class<?> curiosApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Object curiosHelper = curiosApiClass.getMethod("getCuriosHelper").invoke(null);
            java.util.Optional<?> result = (java.util.Optional<?>) curiosHelper.getClass()
                .getMethod("findEquippedCurio", java.util.function.Predicate.class, LivingEntity.class)
                .invoke(curiosHelper, (java.util.function.Predicate<ItemStack>) stack -> 
                    stack.getItem() == cardStack.getItem(), entity);
            return result.isPresent();
        } catch (Exception e) {
            BMFalkye.LOGGER.debug("Curios API not available or card not in Curios slot", e);
            return false;
        }
    }
    
    /**
     * Получает карту из слота Curios игрока
     */
    public static ItemStack getCardFromCuriosSlot(LivingEntity entity) {
        if (!isCuriosLoaded() || entity == null) {
            return ItemStack.EMPTY;
        }
        
        try {
            // Используем рефлексию для безопасного доступа к Curios API
            Class<?> curiosApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Object curiosHelper = curiosApiClass.getMethod("getCuriosHelper").invoke(null);
            java.util.Optional<?> result = (java.util.Optional<?>) curiosHelper.getClass()
                .getMethod("findEquippedCurio", java.util.function.Predicate.class, LivingEntity.class)
                .invoke(curiosHelper, (java.util.function.Predicate<ItemStack>) stack -> 
                    stack.getItem() instanceof com.bmfalkye.items.CardItem, entity);
            
            if (result.isPresent()) {
                // Получаем ItemStack из пары
                Object pair = result.get();
                return (ItemStack) pair.getClass().getMethod("getSecond").invoke(pair);
            }
        } catch (Exception e) {
            BMFalkye.LOGGER.debug("Curios API not available", e);
        }
        
        return ItemStack.EMPTY;
    }
    
    /**
     * Регистрирует слот для карт в Curios (вызывается при инициализации)
     */
    public static void registerCuriosSlot() {
        if (!isCuriosLoaded()) {
            return;
        }
        
        try {
            // Curios автоматически регистрирует слоты через события
            BMFalkye.LOGGER.info("Curios integration: Card slot will be available if Curios is installed");
        } catch (Exception e) {
            BMFalkye.LOGGER.debug("Curios integration not available", e);
        }
    }
}
