package com.bmfalkye.integration;

import com.bmfalkye.BMFalkye;
import com.bmfalkye.cards.Card;

/**
 * Интеграция с JEI (Just Enough Items) для просмотра карт
 */
public class JEIIntegration {
    
    /**
     * Проверяет, установлен ли мод JEI
     */
    public static boolean isJEILoaded() {
        return net.minecraftforge.fml.ModList.get().isLoaded("jei");
    }
    
    /**
     * Регистрирует карты в JEI (вызывается на клиенте)
     */
    public static void registerJEI() {
        if (!isJEILoaded()) {
            return;
        }
        
        try {
            // JEI автоматически подхватывает предметы, но мы можем добавить кастомную информацию
            BMFalkye.LOGGER.info("JEI integration: Cards will be visible in JEI");
        } catch (Exception e) {
            BMFalkye.LOGGER.warn("Failed to register JEI integration", e);
        }
    }
    
    /**
     * Получает информацию о карте для JEI
     */
    public static String getCardJEIInfo(Card card) {
        if (card == null) return "";
        
        StringBuilder info = new StringBuilder();
        info.append("§6").append(card.getName()).append("\n");
        info.append("§7Сила: §f").append(card.getPower()).append("\n");
        info.append("§7Редкость: §f").append(card.getRarity().getDisplayName()).append("\n");
        info.append("§7Тип: §f").append(card.getType().name()).append("\n");
        info.append("§7Фракция: §f").append(card.getFaction()).append("\n");
        info.append("§7").append(card.getDescription());
        
        return info.toString();
    }
}

