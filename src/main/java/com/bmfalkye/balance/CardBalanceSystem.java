package com.bmfalkye.balance;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRegistry;
import com.bmfalkye.util.ModLogger;
import net.minecraft.server.level.ServerLevel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Система балансировки карт
 * Отслеживает статистику использования карт и их эффективность для обеспечения справедливого игрового процесса
 */
public class CardBalanceSystem {
    // Статистика использования карт: cardId -> количество использований
    private static final Map<String, Integer> cardUsageCount = new ConcurrentHashMap<>();
    
    // Статистика побед с картами: cardId -> количество побед
    private static final Map<String, Integer> cardWinCount = new ConcurrentHashMap<>();
    
    // Статистика поражений с картами: cardId -> количество поражений
    private static final Map<String, Integer> cardLossCount = new ConcurrentHashMap<>();
    
    // Статистика использования карт в победных играх: cardId -> количество использований в победных играх
    private static final Map<String, Integer> cardWinningGameUsage = new ConcurrentHashMap<>();
    
    // Статистика использования карт в проигрышных играх: cardId -> количество использований в проигрышных играх
    private static final Map<String, Integer> cardLosingGameUsage = new ConcurrentHashMap<>();
    
    // Минимальное количество использований для анализа (чтобы избежать статистических ошибок)
    private static final int MIN_USAGE_FOR_ANALYSIS = 10;
    
    // Пороговые значения для определения дисбаланса
    private static final double HIGH_WIN_RATE_THRESHOLD = 0.65; // 65%+ win rate считается высоким
    private static final double LOW_WIN_RATE_THRESHOLD = 0.35;  // 35%- win rate считается низким
    
    /**
     * Регистрирует использование карты в игре
     * @param cardId ID карты
     * @param playerWon true если игрок, использовавший карту, выиграл игру
     */
    public static void recordCardUsage(String cardId, boolean playerWon) {
        if (cardId == null || cardId.isEmpty()) {
            return;
        }
        
        // Увеличиваем счётчик использования
        cardUsageCount.merge(cardId, 1, Integer::sum);
        
        // Обновляем статистику побед/поражений
        if (playerWon) {
            cardWinCount.merge(cardId, 1, Integer::sum);
            cardWinningGameUsage.merge(cardId, 1, Integer::sum);
        } else {
            cardLossCount.merge(cardId, 1, Integer::sum);
            cardLosingGameUsage.merge(cardId, 1, Integer::sum);
        }
    }
    
    /**
     * Регистрирует использование карты в игре (без информации о результате)
     */
    public static void recordCardUsage(String cardId) {
        if (cardId == null || cardId.isEmpty()) {
            return;
        }
        cardUsageCount.merge(cardId, 1, Integer::sum);
    }
    
    /**
     * Регистрирует результат игры для всех использованных карт
     * @param usedCardIds список ID карт, использованных в игре
     * @param playerWon true если игрок выиграл
     */
    public static void recordGameResult(List<String> usedCardIds, boolean playerWon) {
        if (usedCardIds == null || usedCardIds.isEmpty()) {
            return;
        }
        
        for (String cardId : usedCardIds) {
            recordCardUsage(cardId, playerWon);
        }
    }
    
    /**
     * Вычисляет win rate для карты
     * @param cardId ID карты
     * @return win rate от 0.0 до 1.0, или -1.0 если недостаточно данных
     */
    public static double getCardWinRate(String cardId) {
        if (cardId == null || cardId.isEmpty()) {
            return -1.0;
        }
        
        int wins = cardWinCount.getOrDefault(cardId, 0);
        int losses = cardLossCount.getOrDefault(cardId, 0);
        int total = wins + losses;
        
        if (total < MIN_USAGE_FOR_ANALYSIS) {
            return -1.0; // Недостаточно данных
        }
        
        return (double) wins / total;
    }
    
    /**
     * Вычисляет общий win rate карты (с учётом всех использований)
     */
    public static double getCardOverallWinRate(String cardId) {
        if (cardId == null || cardId.isEmpty()) {
            return -1.0;
        }
        
        int winningUsage = cardWinningGameUsage.getOrDefault(cardId, 0);
        int losingUsage = cardLosingGameUsage.getOrDefault(cardId, 0);
        int total = winningUsage + losingUsage;
        
        if (total < MIN_USAGE_FOR_ANALYSIS) {
            return -1.0;
        }
        
        return (double) winningUsage / total;
    }
    
    /**
     * Получает статистику карты
     */
    public static CardStatistics getCardStatistics(String cardId) {
        if (cardId == null || cardId.isEmpty()) {
            return null;
        }
        
        int usage = cardUsageCount.getOrDefault(cardId, 0);
        int wins = cardWinCount.getOrDefault(cardId, 0);
        int losses = cardLossCount.getOrDefault(cardId, 0);
        double winRate = getCardWinRate(cardId);
        double overallWinRate = getCardOverallWinRate(cardId);
        
        return new CardStatistics(cardId, usage, wins, losses, winRate, overallWinRate);
    }
    
    /**
     * Анализирует баланс всех карт и возвращает список проблемных карт
     * @return список карт, требующих балансировки
     */
    public static List<BalanceIssue> analyzeBalance() {
        List<BalanceIssue> issues = new ArrayList<>();
        
        for (Card card : CardRegistry.getAllCards()) {
            String cardId = card.getId();
            int usage = cardUsageCount.getOrDefault(cardId, 0);
            
            // Пропускаем карты с недостаточной статистикой
            if (usage < MIN_USAGE_FOR_ANALYSIS) {
                continue;
            }
            
            double winRate = getCardWinRate(cardId);
            if (winRate < 0) {
                continue; // Недостаточно данных
            }
            
            // Проверяем на дисбаланс
            if (winRate >= HIGH_WIN_RATE_THRESHOLD) {
                issues.add(new BalanceIssue(
                    cardId,
                    card.getName(),
                    BalanceIssueType.OVERPOWERED,
                    winRate,
                    usage,
                    "Карта имеет слишком высокий win rate: " + String.format("%.1f%%", winRate * 100)
                ));
            } else if (winRate <= LOW_WIN_RATE_THRESHOLD) {
                issues.add(new BalanceIssue(
                    cardId,
                    card.getName(),
                    BalanceIssueType.UNDERPOWERED,
                    winRate,
                    usage,
                    "Карта имеет слишком низкий win rate: " + String.format("%.1f%%", winRate * 100)
                ));
            }
        }
        
        // Сортируем по серьёзности проблемы (сначала пересильные, потом слабые)
        issues.sort((a, b) -> {
            if (a.type != b.type) {
                return a.type == BalanceIssueType.OVERPOWERED ? -1 : 1;
            }
            return Double.compare(Math.abs(b.winRate - 0.5), Math.abs(a.winRate - 0.5));
        });
        
        return issues;
    }
    
    /**
     * Получает рекомендацию по балансировке для карты
     */
    public static BalanceRecommendation getBalanceRecommendation(String cardId) {
        CardStatistics stats = getCardStatistics(cardId);
        if (stats == null || stats.usage < MIN_USAGE_FOR_ANALYSIS) {
            return null;
        }
        
        Card card = CardRegistry.getCard(cardId);
        if (card == null) {
            return null;
        }
        
        int currentPower = card.getPower();
        int recommendedPower = currentPower;
        String reason = "";
        
        if (stats.winRate >= HIGH_WIN_RATE_THRESHOLD) {
            // Карта слишком сильная - уменьшаем силу
            int powerReduction = (int) Math.ceil((stats.winRate - 0.5) * 2);
            recommendedPower = Math.max(1, currentPower - powerReduction);
            reason = String.format("Win rate %.1f%% слишком высокий, рекомендуется уменьшить силу на %d", 
                stats.winRate * 100, currentPower - recommendedPower);
        } else if (stats.winRate <= LOW_WIN_RATE_THRESHOLD && stats.winRate >= 0) {
            // Карта слишком слабая - увеличиваем силу
            int powerIncrease = (int) Math.ceil((0.5 - stats.winRate) * 2);
            recommendedPower = currentPower + powerIncrease;
            reason = String.format("Win rate %.1f%% слишком низкий, рекомендуется увеличить силу на %d", 
                stats.winRate * 100, recommendedPower - currentPower);
        } else {
            // Карта сбалансирована
            return new BalanceRecommendation(cardId, currentPower, currentPower, 
                "Карта сбалансирована (win rate: " + String.format("%.1f%%", stats.winRate * 100) + ")");
        }
        
        return new BalanceRecommendation(cardId, currentPower, recommendedPower, reason);
    }
    
    /**
     * Получает полный отчёт о балансе всех карт
     */
    public static BalanceReport generateBalanceReport() {
        List<BalanceIssue> issues = analyzeBalance();
        List<BalanceRecommendation> recommendations = new ArrayList<>();
        
        // Генерируем рекомендации для проблемных карт
        for (BalanceIssue issue : issues) {
            BalanceRecommendation rec = getBalanceRecommendation(issue.cardId);
            if (rec != null) {
                recommendations.add(rec);
            }
        }
        
        // Статистика по всем картам
        int totalCards = CardRegistry.getTotalCardCount();
        int analyzedCards = 0;
        int balancedCards = 0;
        int overpoweredCards = 0;
        int underpoweredCards = 0;
        
        for (Card card : CardRegistry.getAllCards()) {
            String cardId = card.getId();
            int usage = cardUsageCount.getOrDefault(cardId, 0);
            
            if (usage >= MIN_USAGE_FOR_ANALYSIS) {
                analyzedCards++;
                double winRate = getCardWinRate(cardId);
                if (winRate >= 0) {
                    if (winRate >= HIGH_WIN_RATE_THRESHOLD) {
                        overpoweredCards++;
                    } else if (winRate <= LOW_WIN_RATE_THRESHOLD) {
                        underpoweredCards++;
                    } else {
                        balancedCards++;
                    }
                }
            }
        }
        
        return new BalanceReport(issues, recommendations, totalCards, analyzedCards, 
            balancedCards, overpoweredCards, underpoweredCards);
    }
    
    /**
     * Очищает статистику (используется для сброса данных)
     */
    public static void clearStatistics() {
        cardUsageCount.clear();
        cardWinCount.clear();
        cardLossCount.clear();
        cardWinningGameUsage.clear();
        cardLosingGameUsage.clear();
        ModLogger.logGameEvent("Card balance statistics cleared");
    }
    
    /**
     * Сохраняет статистику в NBT
     */
    public static net.minecraft.nbt.CompoundTag saveStatistics() {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        
        // Сохраняем статистику использования
        net.minecraft.nbt.CompoundTag usageTag = new net.minecraft.nbt.CompoundTag();
        for (Map.Entry<String, Integer> entry : cardUsageCount.entrySet()) {
            usageTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("usage", usageTag);
        
        // Сохраняем статистику побед
        net.minecraft.nbt.CompoundTag winsTag = new net.minecraft.nbt.CompoundTag();
        for (Map.Entry<String, Integer> entry : cardWinCount.entrySet()) {
            winsTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("wins", winsTag);
        
        // Сохраняем статистику поражений
        net.minecraft.nbt.CompoundTag lossesTag = new net.minecraft.nbt.CompoundTag();
        for (Map.Entry<String, Integer> entry : cardLossCount.entrySet()) {
            lossesTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("losses", lossesTag);
        
        // Сохраняем статистику использования в победных играх
        net.minecraft.nbt.CompoundTag winningUsageTag = new net.minecraft.nbt.CompoundTag();
        for (Map.Entry<String, Integer> entry : cardWinningGameUsage.entrySet()) {
            winningUsageTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("winningUsage", winningUsageTag);
        
        // Сохраняем статистику использования в проигрышных играх
        net.minecraft.nbt.CompoundTag losingUsageTag = new net.minecraft.nbt.CompoundTag();
        for (Map.Entry<String, Integer> entry : cardLosingGameUsage.entrySet()) {
            losingUsageTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("losingUsage", losingUsageTag);
        
        return tag;
    }
    
    /**
     * Загружает статистику из NBT
     */
    public static void loadStatistics(net.minecraft.nbt.CompoundTag tag) {
        if (tag == null) {
            return;
        }
        
        // Загружаем статистику использования
        if (tag.contains("usage", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            net.minecraft.nbt.CompoundTag usageTag = tag.getCompound("usage");
            for (String key : usageTag.getAllKeys()) {
                cardUsageCount.put(key, usageTag.getInt(key));
            }
        }
        
        // Загружаем статистику побед
        if (tag.contains("wins", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            net.minecraft.nbt.CompoundTag winsTag = tag.getCompound("wins");
            for (String key : winsTag.getAllKeys()) {
                cardWinCount.put(key, winsTag.getInt(key));
            }
        }
        
        // Загружаем статистику поражений
        if (tag.contains("losses", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            net.minecraft.nbt.CompoundTag lossesTag = tag.getCompound("losses");
            for (String key : lossesTag.getAllKeys()) {
                cardLossCount.put(key, lossesTag.getInt(key));
            }
        }
        
        // Загружаем статистику использования в победных играх
        if (tag.contains("winningUsage", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            net.minecraft.nbt.CompoundTag winningUsageTag = tag.getCompound("winningUsage");
            for (String key : winningUsageTag.getAllKeys()) {
                cardWinningGameUsage.put(key, winningUsageTag.getInt(key));
            }
        }
        
        // Загружаем статистику использования в проигрышных играх
        if (tag.contains("losingUsage", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            net.minecraft.nbt.CompoundTag losingUsageTag = tag.getCompound("losingUsage");
            for (String key : losingUsageTag.getAllKeys()) {
                cardLosingGameUsage.put(key, losingUsageTag.getInt(key));
            }
        }
        
        ModLogger.logGameEvent("Card balance statistics loaded", 
            "totalCards", String.valueOf(cardUsageCount.size()));
    }
    
    /**
     * Статистика карты
     */
    public static class CardStatistics {
        public final String cardId;
        public final int usage;
        public final int wins;
        public final int losses;
        public final double winRate;
        public final double overallWinRate;
        
        public CardStatistics(String cardId, int usage, int wins, int losses, 
                            double winRate, double overallWinRate) {
            this.cardId = cardId;
            this.usage = usage;
            this.wins = wins;
            this.losses = losses;
            this.winRate = winRate;
            this.overallWinRate = overallWinRate;
        }
    }
    
    /**
     * Проблема баланса
     */
    public static class BalanceIssue {
        public final String cardId;
        public final String cardName;
        public final BalanceIssueType type;
        public final double winRate;
        public final int usage;
        public final String description;
        
        public BalanceIssue(String cardId, String cardName, BalanceIssueType type, 
                           double winRate, int usage, String description) {
            this.cardId = cardId;
            this.cardName = cardName;
            this.type = type;
            this.winRate = winRate;
            this.usage = usage;
            this.description = description;
        }
    }
    
    /**
     * Тип проблемы баланса
     */
    public enum BalanceIssueType {
        OVERPOWERED,  // Карта слишком сильная
        UNDERPOWERED  // Карта слишком слабая
    }
    
    /**
     * Рекомендация по балансировке
     */
    public static class BalanceRecommendation {
        public final String cardId;
        public final int currentPower;
        public final int recommendedPower;
        public final String reason;
        
        public BalanceRecommendation(String cardId, int currentPower, int recommendedPower, String reason) {
            this.cardId = cardId;
            this.currentPower = currentPower;
            this.recommendedPower = recommendedPower;
            this.reason = reason;
        }
    }
    
    /**
     * Отчёт о балансе
     */
    public static class BalanceReport {
        public final List<BalanceIssue> issues;
        public final List<BalanceRecommendation> recommendations;
        public final int totalCards;
        public final int analyzedCards;
        public final int balancedCards;
        public final int overpoweredCards;
        public final int underpoweredCards;
        
        public BalanceReport(List<BalanceIssue> issues, List<BalanceRecommendation> recommendations,
                           int totalCards, int analyzedCards, int balancedCards, 
                           int overpoweredCards, int underpoweredCards) {
            this.issues = issues;
            this.recommendations = recommendations;
            this.totalCards = totalCards;
            this.analyzedCards = analyzedCards;
            this.balancedCards = balancedCards;
            this.overpoweredCards = overpoweredCards;
            this.underpoweredCards = underpoweredCards;
        }
    }
}

