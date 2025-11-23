package com.bmfalkye.leaderboard;

import com.bmfalkye.util.ModLogger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Хранилище лидербордов (еженедельных и общих)
 */
public class LeaderboardStorage extends SavedData {
    // Еженедельный рейтинг: неделя -> список игроков
    private final Map<String, List<LeaderboardEntry>> weeklyLeaderboards = new HashMap<>();
    
    // Зал Славы: список игроков, достигших ранга Легенда
    private final List<HallOfFameEntry> hallOfFame = new ArrayList<>();
    
    // Текущая неделя
    private String currentWeek = getCurrentWeekKey();
    
    // Последнее время сброса недели
    private long lastWeekReset = System.currentTimeMillis();
    
    private static final long WEEK_DURATION_MS = 7 * 24 * 60 * 60 * 1000L; // 7 дней
    
    public static LeaderboardStorage get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            LeaderboardStorage::load,
            LeaderboardStorage::new,
            "bm_falkye_leaderboards"
        );
    }
    
    @NotNull
    public static LeaderboardStorage load(@NotNull CompoundTag tag) {
        LeaderboardStorage storage = new LeaderboardStorage();
        
        try {
            storage.currentWeek = tag.getString("currentWeek");
            storage.lastWeekReset = tag.getLong("lastWeekReset");
            
            // Загружаем еженедельные рейтинги
            CompoundTag weeklyTag = tag.getCompound("weeklyLeaderboards");
            for (String weekKey : weeklyTag.getAllKeys()) {
                ListTag entriesTag = weeklyTag.getList(weekKey, 10); // TAG_Compound
                List<LeaderboardEntry> entries = new ArrayList<>();
                
                for (int i = 0; i < entriesTag.size(); i++) {
                    CompoundTag entryTag = entriesTag.getCompound(i);
                    LeaderboardEntry entry = LeaderboardEntry.load(entryTag);
                    if (entry != null) {
                        entries.add(entry);
                    }
                }
                
                storage.weeklyLeaderboards.put(weekKey, entries);
            }
            
            // Загружаем Зал Славы
            ListTag hallOfFameTag = tag.getList("hallOfFame", 10);
            for (int i = 0; i < hallOfFameTag.size(); i++) {
                CompoundTag entryTag = hallOfFameTag.getCompound(i);
                HallOfFameEntry entry = HallOfFameEntry.load(entryTag);
                if (entry != null) {
                    storage.hallOfFame.add(entry);
                }
            }
        } catch (Exception e) {
            com.bmfalkye.BMFalkye.LOGGER.error("Error loading LeaderboardStorage, using empty storage", e);
            ModLogger.error("Error loading LeaderboardStorage", "error", e.getMessage());
            return new LeaderboardStorage();
        }
        
        return storage;
    }
    
    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag) {
        tag.putString("currentWeek", currentWeek);
        tag.putLong("lastWeekReset", lastWeekReset);
        
        // Сохраняем еженедельные рейтинги
        CompoundTag weeklyTag = new CompoundTag();
        for (Map.Entry<String, List<LeaderboardEntry>> entry : weeklyLeaderboards.entrySet()) {
            ListTag entriesTag = new ListTag();
            for (LeaderboardEntry leaderboardEntry : entry.getValue()) {
                entriesTag.add(leaderboardEntry.save(new CompoundTag()));
            }
            weeklyTag.put(entry.getKey(), entriesTag);
        }
        tag.put("weeklyLeaderboards", weeklyTag);
        
        // Сохраняем Зал Славы
        ListTag hallOfFameTag = new ListTag();
        for (HallOfFameEntry entry : hallOfFame) {
            hallOfFameTag.add(entry.save(new CompoundTag()));
        }
        tag.put("hallOfFame", hallOfFameTag);
        
        return tag;
    }
    
    /**
     * Проверяет и сбрасывает неделю, если нужно
     */
    public void checkWeekReset() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastWeekReset >= WEEK_DURATION_MS) {
            // Сохраняем текущую неделю перед сбросом
            String oldWeek = currentWeek;
            currentWeek = getCurrentWeekKey();
            lastWeekReset = currentTime;
            
            // Очищаем текущую неделю
            weeklyLeaderboards.put(currentWeek, new ArrayList<>());
            
            setDirty();
        }
    }
    
    /**
     * Получает текущую неделю
     */
    public String getCurrentWeek() {
        checkWeekReset();
        return currentWeek;
    }
    
    /**
     * Добавляет или обновляет запись в еженедельном рейтинге
     */
    public void updateWeeklyEntry(UUID playerUUID, String playerName, int rating) {
        checkWeekReset();
        
        List<LeaderboardEntry> entries = weeklyLeaderboards.computeIfAbsent(currentWeek, k -> new ArrayList<>());
        
        // Ищем существующую запись
        LeaderboardEntry existing = null;
        for (LeaderboardEntry entry : entries) {
            if (entry.playerUUID.equals(playerUUID)) {
                existing = entry;
                break;
            }
        }
        
        if (existing != null) {
            // Обновляем существующую
            existing.rating = rating;
            existing.playerName = playerName;
        } else {
            // Добавляем новую
            entries.add(new LeaderboardEntry(playerUUID, playerName, rating));
        }
        
        // Сортируем по рейтингу
        entries.sort((a, b) -> Integer.compare(b.rating, a.rating));
        
        // Ограничиваем топ-50
        if (entries.size() > 50) {
            entries.subList(50, entries.size()).clear();
        }
        
        setDirty();
    }
    
    /**
     * Получает еженедельный рейтинг (топ-50)
     */
    public List<LeaderboardEntry> getWeeklyLeaderboard() {
        checkWeekReset();
        return new ArrayList<>(weeklyLeaderboards.getOrDefault(currentWeek, new ArrayList<>()));
    }
    
    /**
     * Получает позицию игрока в еженедельном рейтинге
     */
    public int getWeeklyPosition(UUID playerUUID) {
        checkWeekReset();
        List<LeaderboardEntry> entries = weeklyLeaderboards.getOrDefault(currentWeek, new ArrayList<>());
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).playerUUID.equals(playerUUID)) {
                return i + 1;
            }
        }
        return -1;
    }
    
    /**
     * Добавляет игрока в Зал Славы (ранг Легенда)
     */
    public void addToHallOfFame(UUID playerUUID, String playerName, int season) {
        // Проверяем, не добавлен ли уже
        for (HallOfFameEntry entry : hallOfFame) {
            if (entry.playerUUID.equals(playerUUID) && entry.season == season) {
                return; // Уже добавлен
            }
        }
        
        hallOfFame.add(new HallOfFameEntry(playerUUID, playerName, season));
        setDirty();
    }
    
    /**
     * Получает Зал Славы
     */
    public List<HallOfFameEntry> getHallOfFame() {
        return new ArrayList<>(hallOfFame);
    }
    
    /**
     * Генерирует ключ для текущей недели
     */
    private static String getCurrentWeekKey() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int week = cal.get(Calendar.WEEK_OF_YEAR);
        return year + "-W" + week;
    }
    
    /**
     * Запись в лидерборде
     */
    public static class LeaderboardEntry {
        public UUID playerUUID;
        public String playerName;
        public int rating;
        
        public LeaderboardEntry(UUID playerUUID, String playerName, int rating) {
            this.playerUUID = playerUUID;
            this.playerName = playerName;
            this.rating = rating;
        }
        
        public CompoundTag save(CompoundTag tag) {
            tag.putUUID("playerUUID", playerUUID);
            tag.putString("playerName", playerName);
            tag.putInt("rating", rating);
            return tag;
        }
        
        public static LeaderboardEntry load(CompoundTag tag) {
            try {
                return new LeaderboardEntry(
                    tag.getUUID("playerUUID"),
                    tag.getString("playerName"),
                    tag.getInt("rating")
                );
            } catch (Exception e) {
                ModLogger.error("Error loading LeaderboardEntry", "error", e.getMessage());
                return null;
            }
        }
    }
    
    /**
     * Запись в Зале Славы
     */
    public static class HallOfFameEntry {
        public UUID playerUUID;
        public String playerName;
        public int season;
        public long timestamp;
        
        public HallOfFameEntry(UUID playerUUID, String playerName, int season) {
            this.playerUUID = playerUUID;
            this.playerName = playerName;
            this.season = season;
            this.timestamp = System.currentTimeMillis();
        }
        
        public CompoundTag save(CompoundTag tag) {
            tag.putUUID("playerUUID", playerUUID);
            tag.putString("playerName", playerName);
            tag.putInt("season", season);
            tag.putLong("timestamp", timestamp);
            return tag;
        }
        
        public static HallOfFameEntry load(CompoundTag tag) {
            try {
                HallOfFameEntry entry = new HallOfFameEntry(
                    tag.getUUID("playerUUID"),
                    tag.getString("playerName"),
                    tag.getInt("season")
                );
                entry.timestamp = tag.getLong("timestamp");
                return entry;
            } catch (Exception e) {
                ModLogger.error("Error loading HallOfFameEntry", "error", e.getMessage());
                return null;
            }
        }
    }
}

