package com.bmfalkye.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/**
 * Хранилище данных об эволюции карт игроков.
 * 
 * <p>Хранит для каждой карты игрока:
 * <ul>
 *   <li>Уровень карты</li>
 *   <li>Опыт карты</li>
 *   <li>Открытые ветки улучшений</li>
 * </ul>
 */
public class CardEvolutionStorage extends SavedData {
    // UUID игрока -> Map<ID карты, данные эволюции>
    private final Map<UUID, Map<String, CardEvolutionData>> playerCardEvolutions = new HashMap<>();
    
    public static CardEvolutionStorage get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            CardEvolutionStorage::load,
            CardEvolutionStorage::new,
            "bm_falkye_card_evolutions"
        );
    }
    
    public static CardEvolutionStorage load(CompoundTag tag) {
        CardEvolutionStorage storage = new CardEvolutionStorage();
        
        try {
            CompoundTag playersTag = tag.getCompound("players");
            
            for (String key : playersTag.getAllKeys()) {
                UUID playerUUID = com.bmfalkye.util.DataLoadValidator.safeParseUUID(key);
                if (playerUUID == null) {
                    com.bmfalkye.util.ModLogger.warn("Skipping invalid UUID during card evolution load", "key", key);
                    continue;
                }
                
                try {
                    CompoundTag playerCardsTag = playersTag.getCompound(key);
                    Map<String, CardEvolutionData> cardEvolutions = new HashMap<>();
                    
                    for (String cardId : playerCardsTag.getAllKeys()) {
                        if (!com.bmfalkye.util.DataLoadValidator.isValidCardId(cardId)) {
                            com.bmfalkye.util.ModLogger.warn("Skipping invalid card ID during evolution load", 
                                "cardId", cardId, "playerUUID", playerUUID.toString());
                            continue;
                        }
                        
                        CompoundTag cardDataTag = playerCardsTag.getCompound(cardId);
                        CardEvolutionData data = CardEvolutionData.load(cardDataTag);
                        if (data != null) {
                            cardEvolutions.put(cardId, data);
                        }
                    }
                    
                    if (!cardEvolutions.isEmpty()) {
                        storage.playerCardEvolutions.put(playerUUID, cardEvolutions);
                    }
                } catch (Exception e) {
                    com.bmfalkye.util.ModLogger.error("Error loading card evolution for player", 
                        "uuid", playerUUID.toString(), "error", e.getMessage());
                }
            }
        } catch (Exception e) {
            com.bmfalkye.BMFalkye.LOGGER.error("Error loading CardEvolutionStorage, using empty storage", e);
            com.bmfalkye.util.ModLogger.error("Error loading CardEvolutionStorage", "error", e.getMessage());
            return new CardEvolutionStorage();
        }
        
        return storage;
    }
    
    @Override
    public CompoundTag save(@org.jetbrains.annotations.NotNull CompoundTag tag) {
        CompoundTag playersTag = new CompoundTag();
        
        for (Map.Entry<UUID, Map<String, CardEvolutionData>> playerEntry : playerCardEvolutions.entrySet()) {
            CompoundTag playerCardsTag = new CompoundTag();
            
            for (Map.Entry<String, CardEvolutionData> cardEntry : playerEntry.getValue().entrySet()) {
                CompoundTag cardDataTag = cardEntry.getValue().save(new CompoundTag());
                playerCardsTag.put(cardEntry.getKey(), cardDataTag);
            }
            
            if (!playerCardsTag.isEmpty()) {
                playersTag.put(playerEntry.getKey().toString(), playerCardsTag);
            }
        }
        
        tag.put("players", playersTag);
        return tag;
    }
    
    /**
     * Получить данные эволюции карты игрока
     */
    public CardEvolutionData getCardEvolution(ServerPlayer player, String cardId) {
        Map<String, CardEvolutionData> playerEvolutions = playerCardEvolutions.computeIfAbsent(
            player.getUUID(), k -> new HashMap<>());
        return playerEvolutions.computeIfAbsent(cardId, k -> new CardEvolutionData());
    }
    
    /**
     * Установить данные эволюции карты игрока
     */
    public void setCardEvolution(ServerPlayer player, String cardId, CardEvolutionData data) {
        Map<String, CardEvolutionData> playerEvolutions = playerCardEvolutions.computeIfAbsent(
            player.getUUID(), k -> new HashMap<>());
        playerEvolutions.put(cardId, data);
        setDirty();
    }
    
    /**
     * Добавить опыт карте игрока
     * @return true если карта получила новый уровень
     */
    public boolean addExperience(ServerPlayer player, String cardId, int experience) {
        CardEvolutionData data = getCardEvolution(player, cardId);
        int oldLevel = data.getLevel();
        data.addExperience(experience);
        setCardEvolution(player, cardId, data);
        
        // Проверяем, получила ли карта новый уровень
        return data.getLevel() > oldLevel;
    }
    
    /**
     * Получить уровень карты игрока
     */
    public int getCardLevel(ServerPlayer player, String cardId) {
        return getCardEvolution(player, cardId).getLevel();
    }
    
    /**
     * Получить опыт карты игрока
     */
    public int getCardExperience(ServerPlayer player, String cardId) {
        return getCardEvolution(player, cardId).getExperience();
    }
    
    /**
     * Получить все открытые ветки улучшений для карты
     */
    public Set<String> getUnlockedBranches(ServerPlayer player, String cardId) {
        return getCardEvolution(player, cardId).getUnlockedBranches();
    }
    
    /**
     * Открыть ветку улучшения для карты
     */
    public boolean unlockBranch(ServerPlayer player, String cardId, String branchId) {
        CardEvolutionData data = getCardEvolution(player, cardId);
        boolean unlocked = data.unlockBranch(branchId);
        if (unlocked) {
            setCardEvolution(player, cardId, data);
        }
        return unlocked;
    }
    
    /**
     * Данные об эволюции конкретной карты
     */
    public static class CardEvolutionData {
        private int level = 1;
        private int experience = 0;
        private final Set<String> unlockedBranches = new HashSet<>();
        
        public CardEvolutionData() {
        }
        
        public CardEvolutionData(int level, int experience, Set<String> unlockedBranches) {
            this.level = level;
            this.experience = experience;
            this.unlockedBranches.addAll(unlockedBranches);
        }
        
        public static CardEvolutionData load(CompoundTag tag) {
            try {
                int level = tag.getInt("level");
                int experience = tag.getInt("experience");
                ListTag branchesTag = tag.getList("unlockedBranches", 8); // 8 = String tag type
                
                Set<String> unlockedBranches = new HashSet<>();
                for (int i = 0; i < branchesTag.size(); i++) {
                    unlockedBranches.add(branchesTag.getString(i));
                }
                
                return new CardEvolutionData(level, experience, unlockedBranches);
            } catch (Exception e) {
                com.bmfalkye.util.ModLogger.error("Error loading CardEvolutionData", "error", e.getMessage());
                return new CardEvolutionData();
            }
        }
        
        public CompoundTag save(CompoundTag tag) {
            tag.putInt("level", level);
            tag.putInt("experience", experience);
            
            ListTag branchesTag = new ListTag();
            for (String branchId : unlockedBranches) {
                branchesTag.add(net.minecraft.nbt.StringTag.valueOf(branchId));
            }
            tag.put("unlockedBranches", branchesTag);
            
            return tag;
        }
        
        /**
         * Добавить опыт карте
         * Автоматически повышает уровень при достижении необходимого опыта
         */
        public void addExperience(int amount) {
            experience += amount;
            
            // Проверяем повышение уровня
            // Формула: для уровня N нужно N * 100 опыта
            while (experience >= getExperienceForNextLevel()) {
                experience -= getExperienceForNextLevel();
                level++;
            }
        }
        
        /**
         * Получить необходимое количество опыта для следующего уровня
         */
        public int getExperienceForNextLevel() {
            return level * 100;
        }
        
        /**
         * Получить текущий уровень
         */
        public int getLevel() {
            return level;
        }
        
        /**
         * Установить уровень (для тестирования)
         */
        public void setLevel(int level) {
            this.level = Math.max(1, level);
        }
        
        /**
         * Получить текущий опыт
         */
        public int getExperience() {
            return experience;
        }
        
        /**
         * Установить опыт (для тестирования)
         */
        public void setExperience(int experience) {
            this.experience = Math.max(0, experience);
        }
        
        /**
         * Получить все открытые ветки улучшений
         */
        public Set<String> getUnlockedBranches() {
            return new HashSet<>(unlockedBranches);
        }
        
        /**
         * Открыть ветку улучшения
         */
        public boolean unlockBranch(String branchId) {
            return unlockedBranches.add(branchId);
        }
        
        /**
         * Проверить, открыта ли ветка улучшения
         */
        public boolean isBranchUnlocked(String branchId) {
            return unlockedBranches.contains(branchId);
        }
    }
}

