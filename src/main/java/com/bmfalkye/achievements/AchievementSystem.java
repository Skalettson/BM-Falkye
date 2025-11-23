package com.bmfalkye.achievements;

import com.bmfalkye.BMFalkye;
import com.bmfalkye.player.PlayerProgress;
import com.bmfalkye.storage.PlayerProgressStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Система достижений (50+ достижений)
 */
public class AchievementSystem {
    private static final Map<String, Achievement> ACHIEVEMENTS = new HashMap<>();
    
    public static void initializeAchievements() {
        // Достижения за победы
        registerAchievement(new Achievement("first_win", "Первая победа", 
            "Выиграйте свою первую игру", Achievement.Rarity.COMMON));
        registerAchievement(new Achievement("win_streak_5", "Серия побед", 
            "Выиграйте 5 игр подряд", Achievement.Rarity.RARE));
        registerAchievement(new Achievement("win_streak_10", "Неуязвимый", 
            "Выиграйте 10 игр подряд", Achievement.Rarity.EPIC));
        registerAchievement(new Achievement("win_100", "Ветеран", 
            "Выиграйте 100 игр", Achievement.Rarity.LEGENDARY));
        
        // Достижения за карты
        registerAchievement(new Achievement("collect_50", "Коллекционер", 
            "Соберите 50 различных карт", Achievement.Rarity.COMMON));
        registerAchievement(new Achievement("collect_100", "Мастер коллекций", 
            "Соберите 100 различных карт", Achievement.Rarity.RARE));
        registerAchievement(new Achievement("collect_all", "Полная коллекция", 
            "Соберите все карты", Achievement.Rarity.LEGENDARY));
        
        // Достижения за уровни
        registerAchievement(new Achievement("level_10", "Новичок", 
            "Достигните 10 уровня", Achievement.Rarity.COMMON));
        registerAchievement(new Achievement("level_25", "Опытный", 
            "Достигните 25 уровня", Achievement.Rarity.RARE));
        registerAchievement(new Achievement("level_50", "Мастер", 
            "Достигните 50 уровня", Achievement.Rarity.LEGENDARY));
        
        // Достижения за комбинации
        registerAchievement(new Achievement("perfect_round", "Идеальный раунд", 
            "Выиграйте раунд с разницей в 20+ очков", Achievement.Rarity.RARE));
        registerAchievement(new Achievement("comeback", "Возвращение", 
            "Выиграйте игру, проигрывая 0-2", Achievement.Rarity.EPIC));
        registerAchievement(new Achievement("flawless_victory", "Безупречная победа", 
            "Выиграйте игру, не проиграв ни одного раунда", Achievement.Rarity.EPIC));
        
        // Достижения за фракции
        registerAchievement(new Achievement("fire_master", "Мастер Пламени", 
            "Выиграйте 50 игр колодой Дома Пламени", Achievement.Rarity.RARE));
        registerAchievement(new Achievement("watcher_master", "Мастер Дозора", 
            "Выиграйте 50 игр колодой Дозорных Руин", Achievement.Rarity.RARE));
        registerAchievement(new Achievement("nature_master", "Мастер Природы", 
            "Выиграйте 50 игр колодой Детей Рощения", Achievement.Rarity.RARE));
        
        // Достижения за специальные условия
        registerAchievement(new Achievement("dragon_slayer", "Убийца драконов", 
            "Разыграйте всех трёх легендарных драконов в одной игре", Achievement.Rarity.LEGENDARY));
        registerAchievement(new Achievement("weather_master", "Повелитель погоды", 
            "Используйте все типы погоды в одной игре", Achievement.Rarity.EPIC));
        registerAchievement(new Achievement("leader_combo", "Комбо лидеров", 
            "Используйте способности всех трёх лидеров в одной игре", Achievement.Rarity.EPIC));
        
        // Новые достижения за комбо
        registerAchievement(new Achievement("faction_combo", "Синергия фракции", 
            "Активируйте комбо фракции 10 раз", Achievement.Rarity.RARE));
        registerAchievement(new Achievement("legendary_combo", "Легендарное комбо", 
            "Активируйте легендарное комбо 5 раз", Achievement.Rarity.EPIC));
        registerAchievement(new Achievement("power_combo", "Мощное комбо", 
            "Активируйте мощное комбо 3 раза", Achievement.Rarity.LEGENDARY));
        
        // Достижения за рейтинг
        registerAchievement(new Achievement("bronze_rank", "Бронзовый воин", 
            "Достигните ранга Бронза", Achievement.Rarity.COMMON));
        registerAchievement(new Achievement("silver_rank", "Серебряный мастер", 
            "Достигните ранга Серебро", Achievement.Rarity.COMMON));
        registerAchievement(new Achievement("gold_rank", "Золотой чемпион", 
            "Достигните ранга Золото", Achievement.Rarity.RARE));
        registerAchievement(new Achievement("platinum_rank", "Платиновый элита", 
            "Достигните ранга Платина", Achievement.Rarity.RARE));
        registerAchievement(new Achievement("diamond_rank", "Алмазный легенда", 
            "Достигните ранга Алмаз", Achievement.Rarity.EPIC));
        registerAchievement(new Achievement("master_rank", "Мастер игры", 
            "Достигните ранга Мастер", Achievement.Rarity.EPIC));
        registerAchievement(new Achievement("grandmaster_rank", "Грандмастер", 
            "Достигните ранга Грандмастер", Achievement.Rarity.LEGENDARY));
        registerAchievement(new Achievement("legend_rank", "Легенда", 
            "Достигните ранга Легенда", Achievement.Rarity.LEGENDARY));
        
        // Достижения за турниры
        registerAchievement(new Achievement("tournament_winner", "Победитель турнира", 
            "Выиграйте турнир", Achievement.Rarity.EPIC));
        registerAchievement(new Achievement("tournament_master", "Мастер турниров", 
            "Выиграйте 5 турниров", Achievement.Rarity.LEGENDARY));
        
        // Достижения за ежедневные задания
        registerAchievement(new Achievement("daily_streak_7", "Неделя заданий", 
            "Выполните ежедневные задания 7 дней подряд", Achievement.Rarity.RARE));
        registerAchievement(new Achievement("daily_streak_30", "Месяц заданий", 
            "Выполните ежедневные задания 30 дней подряд", Achievement.Rarity.EPIC));
        registerAchievement(new Achievement("quest_master", "Мастер заданий", 
            "Выполните 100 ежедневных заданий", Achievement.Rarity.LEGENDARY));
        
        // Достижения за коллекцию
        registerAchievement(new Achievement("collect_75", "Страстный коллекционер", 
            "Соберите 75 различных карт", Achievement.Rarity.RARE));
        registerAchievement(new Achievement("collect_90", "Эксперт коллекций", 
            "Соберите 90% всех карт", Achievement.Rarity.EPIC));
        
        // Достижения за статистику
        registerAchievement(new Achievement("win_500", "Великий воин", 
            "Выиграйте 500 игр", Achievement.Rarity.LEGENDARY));
        registerAchievement(new Achievement("win_1000", "Легендарный боец", 
            "Выиграйте 1000 игр", Achievement.Rarity.LEGENDARY));
        registerAchievement(new Achievement("perfect_season", "Идеальный сезон", 
            "Выиграйте все игры в сезоне", Achievement.Rarity.LEGENDARY));
        
        BMFalkye.LOGGER.info("Initialized {} achievements", ACHIEVEMENTS.size());
    }
    
    public static void registerAchievement(Achievement achievement) {
        ACHIEVEMENTS.put(achievement.getId(), achievement);
    }
    
    public static Achievement getAchievement(String id) {
        return ACHIEVEMENTS.get(id);
    }
    
    public static Collection<Achievement> getAllAchievements() {
        return ACHIEVEMENTS.values();
    }
    
    /**
     * Проверяет и разблокирует достижения для игрока
     */
    public static void checkAchievements(ServerPlayer player, PlayerProgress progress) {
        // Проверяем достижения за победы
        if (progress.getTotalGamesWon() >= 1 && !progress.hasAchievement("first_win")) {
            unlockAchievement(player, progress, "first_win");
        }
        if (progress.getTotalGamesWon() >= 100 && !progress.hasAchievement("win_100")) {
            unlockAchievement(player, progress, "win_100");
        }
        
        // Проверяем достижения за коллекцию
        int cardCount = progress.getUnlockedCards().size();
        if (cardCount >= 50 && !progress.hasAchievement("collect_50")) {
            unlockAchievement(player, progress, "collect_50");
        }
        if (cardCount >= 100 && !progress.hasAchievement("collect_100")) {
            unlockAchievement(player, progress, "collect_100");
        }
        
        // Проверяем достижения за уровни
        if (progress.getLevel() >= 10 && !progress.hasAchievement("level_10")) {
            unlockAchievement(player, progress, "level_10");
        }
        if (progress.getLevel() >= 25 && !progress.hasAchievement("level_25")) {
            unlockAchievement(player, progress, "level_25");
        }
        if (progress.getLevel() >= 50 && !progress.hasAchievement("level_50")) {
            unlockAchievement(player, progress, "level_50");
        }
    }
    
    private static void unlockAchievement(ServerPlayer player, PlayerProgress progress, String achievementId) {
        progress.unlockAchievement(achievementId);
        Achievement achievement = getAchievement(achievementId);
        if (achievement != null) {
            player.sendSystemMessage(Component.translatable("achievement.bm_falkye.unlocked", 
                achievement.getDisplayName()));
            
            // Выдаём награды за достижение
            giveAchievementRewards(player, achievement);
        }
    }
    
    /**
     * Выдаёт награды за достижение
     */
    private static void giveAchievementRewards(ServerPlayer player, Achievement achievement) {
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            // Награды в зависимости от редкости
            int coins = 0;
            int xp = 0;
            List<String> cardRewards = new ArrayList<>();
            
            switch (achievement.getRarity()) {
                case COMMON:
                    coins = 50;
                    xp = 100;
                    break;
                case RARE:
                    coins = 200;
                    xp = 500;
                    // Случайная редкая карта
                    cardRewards.add(getRandomCardByRarity(com.bmfalkye.cards.CardRarity.RARE));
                    break;
                case EPIC:
                    coins = 500;
                    xp = 1000;
                    // Случайная эпическая карта
                    cardRewards.add(getRandomCardByRarity(com.bmfalkye.cards.CardRarity.EPIC));
                    break;
                case LEGENDARY:
                    coins = 1000;
                    xp = 2500;
                    // Случайная легендарная карта
                    cardRewards.add(getRandomCardByRarity(com.bmfalkye.cards.CardRarity.LEGENDARY));
                    break;
            }
            
            // Выдаём монеты
            if (coins > 0) {
                com.bmfalkye.storage.PlayerCurrency currency = com.bmfalkye.storage.PlayerCurrency.get(serverLevel);
                currency.addCoins(player, coins);
                player.sendSystemMessage(Component.literal("§e+ " + coins + " монет"));
            }
            
            // Выдаём опыт
            if (xp > 0) {
                PlayerProgressStorage storage = PlayerProgressStorage.get(serverLevel);
                PlayerProgress progress = storage.getPlayerProgress(player);
                progress.addExperience(xp);
                storage.setPlayerProgress(player, progress);
                player.sendSystemMessage(Component.literal("§b+ " + xp + " опыта"));
            }
            
            // Выдаём карты
            for (String cardId : cardRewards) {
                if (cardId != null) {
                    com.bmfalkye.storage.PlayerCardCollection collection = 
                        com.bmfalkye.storage.PlayerCardCollection.get(serverLevel);
                    collection.addCard(player, cardId);
                    com.bmfalkye.cards.Card card = com.bmfalkye.cards.CardRegistry.getCard(cardId);
                    if (card != null) {
                        player.sendSystemMessage(Component.literal("§6+ Карта: " + card.getName()));
                    }
                }
            }
        }
    }
    
    /**
     * Получает случайную карту определённой редкости
     */
    private static String getRandomCardByRarity(com.bmfalkye.cards.CardRarity rarity) {
        List<com.bmfalkye.cards.Card> cards = com.bmfalkye.cards.CardRegistry.getCardsByRarity(rarity);
        if (cards.isEmpty()) {
            return null;
        }
        return cards.get(new java.util.Random().nextInt(cards.size())).getId();
    }
    
    public static class Achievement {
        private final String id;
        private final String name;
        private final String description;
        private final Rarity rarity;
        
        public Achievement(String id, String name, String description, Rarity rarity) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.rarity = rarity;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public Rarity getRarity() { return rarity; }
        
        public Component getDisplayName() {
            return Component.translatable("achievement.bm_falkye." + id + ".name");
        }
        
        public enum Rarity {
            COMMON, RARE, EPIC, LEGENDARY
        }
    }
}

