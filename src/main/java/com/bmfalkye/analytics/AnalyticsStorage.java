package com.bmfalkye.analytics;

import com.bmfalkye.util.ModLogger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Хранилище аналитики игры
 */
public class AnalyticsStorage extends SavedData {
    // Win Rate карт: cardId -> (wins, losses)
    private final Map<String, CardStats> cardStats = new HashMap<>();
    
    // Комбинации карт: "cardId1,cardId2" -> (wins, losses)
    private final Map<String, CombinationStats> combinationStats = new HashMap<>();
    
    // Соотношение побед/поражений между фракциями: "faction1 vs faction2" -> (wins, losses)
    private final Map<String, FactionMatchupStats> factionMatchupStats = new HashMap<>();
    
    // Среднее время хода: playerUUID -> среднее время в секундах
    private final Map<String, Double> averageTurnTime = new HashMap<>();
    
    public static AnalyticsStorage get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            AnalyticsStorage::load,
            AnalyticsStorage::new,
            "bm_falkye_analytics"
        );
    }
    
    @NotNull
    public static AnalyticsStorage load(@NotNull CompoundTag tag) {
        AnalyticsStorage storage = new AnalyticsStorage();
        
        try {
            // Загружаем статистику карт
            CompoundTag cardStatsTag = tag.getCompound("cardStats");
            for (String cardId : cardStatsTag.getAllKeys()) {
                CompoundTag statsTag = cardStatsTag.getCompound(cardId);
                storage.cardStats.put(cardId, CardStats.load(statsTag));
            }
            
            // Загружаем статистику комбинаций
            CompoundTag combinationStatsTag = tag.getCompound("combinationStats");
            for (String combination : combinationStatsTag.getAllKeys()) {
                CompoundTag statsTag = combinationStatsTag.getCompound(combination);
                storage.combinationStats.put(combination, CombinationStats.load(statsTag));
            }
            
            // Загружаем статистику фракций
            CompoundTag factionStatsTag = tag.getCompound("factionMatchupStats");
            for (String matchup : factionStatsTag.getAllKeys()) {
                CompoundTag statsTag = factionStatsTag.getCompound(matchup);
                storage.factionMatchupStats.put(matchup, FactionMatchupStats.load(statsTag));
            }
            
            // Загружаем среднее время хода
            CompoundTag turnTimeTag = tag.getCompound("averageTurnTime");
            for (String playerId : turnTimeTag.getAllKeys()) {
                storage.averageTurnTime.put(playerId, turnTimeTag.getDouble(playerId));
            }
        } catch (Exception e) {
            com.bmfalkye.BMFalkye.LOGGER.error("Error loading AnalyticsStorage, using empty storage", e);
            ModLogger.error("Error loading AnalyticsStorage", "error", e.getMessage());
            return new AnalyticsStorage();
        }
        
        return storage;
    }
    
    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag) {
        // Сохраняем статистику карт
        CompoundTag cardStatsTag = new CompoundTag();
        for (Map.Entry<String, CardStats> entry : cardStats.entrySet()) {
            cardStatsTag.put(entry.getKey(), entry.getValue().save(new CompoundTag()));
        }
        tag.put("cardStats", cardStatsTag);
        
        // Сохраняем статистику комбинаций
        CompoundTag combinationStatsTag = new CompoundTag();
        for (Map.Entry<String, CombinationStats> entry : combinationStats.entrySet()) {
            combinationStatsTag.put(entry.getKey(), entry.getValue().save(new CompoundTag()));
        }
        tag.put("combinationStats", combinationStatsTag);
        
        // Сохраняем статистику фракций
        CompoundTag factionStatsTag = new CompoundTag();
        for (Map.Entry<String, FactionMatchupStats> entry : factionMatchupStats.entrySet()) {
            factionStatsTag.put(entry.getKey(), entry.getValue().save(new CompoundTag()));
        }
        tag.put("factionMatchupStats", factionStatsTag);
        
        // Сохраняем среднее время хода
        CompoundTag turnTimeTag = new CompoundTag();
        for (Map.Entry<String, Double> entry : averageTurnTime.entrySet()) {
            turnTimeTag.putDouble(entry.getKey(), entry.getValue());
        }
        tag.put("averageTurnTime", turnTimeTag);
        
        return tag;
    }
    
    /**
     * Записывает результат использования карты
     */
    public void recordCardResult(String cardId, boolean won) {
        CardStats stats = cardStats.computeIfAbsent(cardId, k -> new CardStats());
        if (won) {
            stats.wins++;
        } else {
            stats.losses++;
        }
        setDirty();
    }
    
    /**
     * Записывает результат комбинации карт
     */
    public void recordCombinationResult(String cardId1, String cardId2, boolean won) {
        // Сортируем ID для консистентности
        String combination = cardId1.compareTo(cardId2) < 0 ? 
            cardId1 + "," + cardId2 : cardId2 + "," + cardId1;
        
        CombinationStats stats = combinationStats.computeIfAbsent(combination, k -> new CombinationStats());
        if (won) {
            stats.wins++;
        } else {
            stats.losses++;
        }
        setDirty();
    }
    
    /**
     * Записывает результат матча между фракциями
     */
    public void recordFactionMatchup(String faction1, String faction2, boolean faction1Won) {
        String matchup = faction1 + " vs " + faction2;
        
        FactionMatchupStats stats = factionMatchupStats.computeIfAbsent(matchup, k -> new FactionMatchupStats());
        if (faction1Won) {
            stats.faction1Wins++;
        } else {
            stats.faction2Wins++;
        }
        setDirty();
    }
    
    /**
     * Обновляет среднее время хода игрока
     */
    public void updateAverageTurnTime(String playerId, double turnTimeSeconds) {
        Double currentAverage = averageTurnTime.get(playerId);
        if (currentAverage == null) {
            averageTurnTime.put(playerId, turnTimeSeconds);
        } else {
            // Простое скользящее среднее
            averageTurnTime.put(playerId, (currentAverage + turnTimeSeconds) / 2.0);
        }
        setDirty();
    }
    
    /**
     * Получает среднее время хода
     */
    public java.util.Map<String, Double> getAverageTurnTime() {
        return new HashMap<>(averageTurnTime);
    }
    
    /**
     * Получает Win Rate карты
     */
    public double getCardWinRate(String cardId) {
        CardStats stats = cardStats.get(cardId);
        if (stats == null || stats.getTotalGames() == 0) {
            return 0.0;
        }
        return (double)stats.wins / stats.getTotalGames() * 100.0;
    }
    
    /**
     * Получает статистику карты
     */
    public CardStats getCardStats(String cardId) {
        return cardStats.getOrDefault(cardId, new CardStats());
    }
    
    /**
     * Получает топ карт по Win Rate
     */
    public List<CardWinRateEntry> getTopCardsByWinRate(int count) {
        List<CardWinRateEntry> entries = new ArrayList<>();
        for (Map.Entry<String, CardStats> entry : cardStats.entrySet()) {
            if (entry.getValue().getTotalGames() >= 10) { // Минимум 10 игр
                entries.add(new CardWinRateEntry(
                    entry.getKey(),
                    entry.getValue().getWinRate()
                ));
            }
        }
        entries.sort((a, b) -> Double.compare(b.winRate, a.winRate));
        return entries.subList(0, Math.min(count, entries.size()));
    }
    
    /**
     * Статистика карты
     */
    public static class CardStats {
        public int wins = 0;
        public int losses = 0;
        
        public int getTotalGames() {
            return wins + losses;
        }
        
        public double getWinRate() {
            if (getTotalGames() == 0) return 0.0;
            return (double)wins / getTotalGames() * 100.0;
        }
        
        public CompoundTag save(CompoundTag tag) {
            tag.putInt("wins", wins);
            tag.putInt("losses", losses);
            return tag;
        }
        
        public static CardStats load(CompoundTag tag) {
            CardStats stats = new CardStats();
            stats.wins = tag.getInt("wins");
            stats.losses = tag.getInt("losses");
            return stats;
        }
    }
    
    /**
     * Статистика комбинации
     */
    public static class CombinationStats {
        public int wins = 0;
        public int losses = 0;
        
        public int getTotalGames() {
            return wins + losses;
        }
        
        public double getWinRate() {
            if (getTotalGames() == 0) return 0.0;
            return (double)wins / getTotalGames() * 100.0;
        }
        
        public CompoundTag save(CompoundTag tag) {
            tag.putInt("wins", wins);
            tag.putInt("losses", losses);
            return tag;
        }
        
        public static CombinationStats load(CompoundTag tag) {
            CombinationStats stats = new CombinationStats();
            stats.wins = tag.getInt("wins");
            stats.losses = tag.getInt("losses");
            return stats;
        }
    }
    
    /**
     * Статистика матча между фракциями
     */
    public static class FactionMatchupStats {
        public int faction1Wins = 0;
        public int faction2Wins = 0;
        
        public int getTotalGames() {
            return faction1Wins + faction2Wins;
        }
        
        public CompoundTag save(CompoundTag tag) {
            tag.putInt("faction1Wins", faction1Wins);
            tag.putInt("faction2Wins", faction2Wins);
            return tag;
        }
        
        public static FactionMatchupStats load(CompoundTag tag) {
            FactionMatchupStats stats = new FactionMatchupStats();
            stats.faction1Wins = tag.getInt("faction1Wins");
            stats.faction2Wins = tag.getInt("faction2Wins");
            return stats;
        }
    }
    
    /**
     * Запись Win Rate карты
     */
    public static class CardWinRateEntry {
        public final String cardId;
        public final double winRate;
        
        public CardWinRateEntry(String cardId, double winRate) {
            this.cardId = cardId;
            this.winRate = winRate;
        }
    }
}

