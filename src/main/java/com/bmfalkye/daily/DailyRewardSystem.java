package com.bmfalkye.daily;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRarity;
import com.bmfalkye.cards.CardRegistry;
import com.bmfalkye.player.PlayerProgress;
import com.bmfalkye.storage.PlayerProgressStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Система ежедневных наград и заданий
 */
public class DailyRewardSystem {
    private static final Map<UUID, DailyRewardData> playerRewards = new HashMap<>();
    
    public static void checkAndGiveDailyReward(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        DailyRewardData data = playerRewards.computeIfAbsent(playerUUID, k -> new DailyRewardData());
        
        long currentDay = player.level().getGameTime() / 24000; // Дни в Minecraft
        
        if (data.lastRewardDay < currentDay) {
            // Новый день - выдаём награду
            giveDailyReward(player, data);
            data.lastRewardDay = currentDay;
            data.streakDays++;
        }
    }
    
    /**
     * Получает ежедневную награду (вызывается игроком)
     */
    public static boolean claimDailyReward(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        DailyRewardData data = playerRewards.computeIfAbsent(playerUUID, k -> new DailyRewardData());
        
        long currentDay = player.level().getGameTime() / 24000;
        
        if (data.lastRewardDay == currentDay) {
            // Уже получено сегодня
            return false;
        }
        
        // Выдаём награду
        giveDailyReward(player, data);
        data.lastRewardDay = currentDay;
        
        // Обновляем серию
        if (data.lastRewardDay == currentDay - 1) {
            data.streakDays++;
        } else {
            data.streakDays = 1; // Сброс серии
        }
        
        return true;
    }
    
    private static void giveDailyReward(ServerPlayer player, DailyRewardData data) {
        // Базовые награды
        int baseXP = 50 + (data.streakDays * 10); // Больше опыта за серию дней
        int baseCards = 1;
        
        // Бонус за серию дней
        if (data.streakDays >= 7) {
            baseCards = 2; // Две карты за недельную серию
        }
        if (data.streakDays >= 30) {
            baseCards = 3; // Три карты за месячную серию
        }
        
        // Выдаём опыт
        PlayerProgressStorage storage = PlayerProgressStorage.get((net.minecraft.server.level.ServerLevel) player.level());
        PlayerProgress progress = storage.getPlayerProgress(player);
        progress.addExperience(baseXP);
        storage.setPlayerProgress(player, progress);
        
        // Выдаём случайные карты
        for (int i = 0; i < baseCards; i++) {
            Card randomCard = getRandomCardByRarity();
            if (randomCard != null) {
                progress.unlockCard(randomCard.getId());
                player.sendSystemMessage(Component.translatable("message.bm_falkye.daily_card_reward", 
                    randomCard.getName()));
            }
        }
        
        storage.setPlayerProgress(player, progress);
        
        player.sendSystemMessage(Component.translatable("message.bm_falkye.daily_reward", 
            baseXP, data.streakDays));
    }
    
    private static Card getRandomCardByRarity() {
        Random random = new Random();
        float roll = random.nextFloat();
        
        CardRarity rarity;
        if (roll < 0.70f) {
            rarity = CardRarity.COMMON;
        } else if (roll < 0.90f) {
            rarity = CardRarity.RARE;
        } else if (roll < 0.98f) {
            rarity = CardRarity.EPIC;
        } else {
            rarity = CardRarity.LEGENDARY;
        }
        
        List<Card> cards = CardRegistry.getCardsByRarity(rarity);
        if (cards.isEmpty()) {
            return null;
        }
        
        return cards.get(random.nextInt(cards.size()));
    }
    
    /**
     * Система ежедневных заданий
     */
    private static final Map<UUID, DailyQuestData> playerQuests = new HashMap<>();
    
    public static void checkDailyQuests(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        DailyQuestData questData = playerQuests.computeIfAbsent(playerUUID, k -> new DailyQuestData());
        
        long currentDay = player.level().getGameTime() / 24000;
        
        // Если новый день, генерируем новые задания
        if (questData.lastQuestDay < currentDay) {
            generateDailyQuests(player, questData);
            questData.lastQuestDay = currentDay;
        }
        
        // Проверяем прогресс заданий
        checkQuestProgress(player, questData);
    }
    
    /**
     * Генерирует ежедневные задания для игрока
     */
    private static void generateDailyQuests(ServerPlayer player, DailyQuestData questData) {
        Random random = new Random();
        questData.quests.clear();
        questData.progress.clear();
        
        // Генерируем 3 случайных задания из пула
        List<QuestType> availableQuests = new ArrayList<>(List.of(
            QuestType.WIN_GAMES, QuestType.PLAY_CARDS, QuestType.USE_LEADER,
            QuestType.PLAY_SPELLS, QuestType.WIN_ROUNDS, QuestType.COLLECT_CARDS,
            QuestType.DEAL_DAMAGE, QuestType.HEAL_CARDS
        ));
        
        Collections.shuffle(availableQuests);
        for (int i = 0; i < Math.min(3, availableQuests.size()); i++) {
            QuestType type = availableQuests.get(i);
            int target = getQuestTarget(type);
            questData.quests.add(new DailyQuest(type, target));
            questData.progress.put(type, 0);
        }
        
        // Уведомляем игрока о новых заданиях
        player.sendSystemMessage(Component.literal("§a§lНовые ежедневные задания!"));
        for (DailyQuest quest : questData.quests) {
            player.sendSystemMessage(Component.literal("§7- " + quest.getDescription()));
        }
    }
    
    /**
     * Получает цель для задания в зависимости от типа
     */
    private static int getQuestTarget(QuestType type) {
        return switch (type) {
            case WIN_GAMES -> 3;
            case PLAY_CARDS -> 15;
            case USE_LEADER -> 5;
            case PLAY_SPELLS -> 8;
            case WIN_ROUNDS -> 5;
            case COLLECT_CARDS -> 2;
            case DEAL_DAMAGE -> 50;
            case HEAL_CARDS -> 20;
        };
    }
    
    /**
     * Проверяет прогресс заданий и выдаёт награды
     */
    private static void checkQuestProgress(ServerPlayer player, DailyQuestData questData) {
        PlayerProgressStorage storage = PlayerProgressStorage.get((net.minecraft.server.level.ServerLevel) player.level());
        PlayerProgress progress = storage.getPlayerProgress(player);
        
        List<DailyQuest> completedQuests = new ArrayList<>();
        
        for (DailyQuest quest : questData.quests) {
            int currentProgress = questData.progress.getOrDefault(quest.getType(), 0);
            if (currentProgress >= quest.getTarget()) {
                // Задание выполнено
                completedQuests.add(quest);
                giveQuestReward(player, progress, quest);
            }
        }
        
        // Удаляем выполненные задания
        for (DailyQuest quest : completedQuests) {
            questData.quests.remove(quest);
            questData.progress.remove(quest.getType());
        }
        
        storage.setPlayerProgress(player, progress);
    }
    
    /**
     * Выдаёт награду за выполнение задания
     */
    private static void giveQuestReward(ServerPlayer player, PlayerProgress progress, DailyQuest quest) {
        int xpReward = quest.getTarget() * 10; // 10 XP за единицу прогресса
        int cardReward = 1;
        
        // Бонусные награды за сложные задания
        if (quest.getType() == QuestType.WIN_GAMES || quest.getType() == QuestType.WIN_ROUNDS) {
            xpReward *= 2;
            cardReward = 2;
        }
        
        progress.addExperience(xpReward);
        
        // Выдаём карты
        for (int i = 0; i < cardReward; i++) {
            Card randomCard = getRandomCardByRarity();
            if (randomCard != null) {
                progress.unlockCard(randomCard.getId());
            }
        }
        
        player.sendSystemMessage(Component.literal("§a§lЗадание выполнено: " + quest.getDescription()));
        player.sendSystemMessage(Component.literal("§7Награда: §e" + xpReward + " опыта, §b" + cardReward + " карт"));
    }
    
    /**
     * Увеличивает прогресс задания
     */
    public static void incrementQuestProgress(ServerPlayer player, QuestType type, int amount) {
        UUID playerUUID = player.getUUID();
        DailyQuestData questData = playerQuests.get(playerUUID);
        if (questData == null) return;
        
        int currentProgress = questData.progress.getOrDefault(type, 0);
        questData.progress.put(type, currentProgress + amount);
        
        // Проверяем прогресс
        checkQuestProgress(player, questData);
    }
    
    /**
     * Получает текущие задания игрока
     */
    public static List<DailyQuest> getPlayerQuests(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        DailyQuestData questData = playerQuests.get(playerUUID);
        if (questData == null) return new ArrayList<>();
        return new ArrayList<>(questData.quests);
    }
    
    /**
     * Получает прогресс задания
     */
    public static int getQuestProgress(ServerPlayer player, QuestType type) {
        UUID playerUUID = player.getUUID();
        DailyQuestData questData = playerQuests.get(playerUUID);
        if (questData == null) return 0;
        return questData.progress.getOrDefault(type, 0);
    }
    
    /**
     * Получает информацию о ежедневных наградах игрока
     */
    public static DailyRewardInfo getDailyRewardInfo(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        DailyRewardData data = playerRewards.computeIfAbsent(playerUUID, k -> new DailyRewardData());
        long currentDay = player.level().getGameTime() / 24000;
        int day = (int)((currentDay - data.lastRewardDay) % 7) + 1;
        if (day > 7) day = 1;
        boolean claimed = data.lastRewardDay == currentDay;
        return new DailyRewardInfo(day, claimed, data.streakDays);
    }
    
    /**
     * Типы заданий
     */
    public enum QuestType {
        WIN_GAMES("Выиграть игры"),
        PLAY_CARDS("Разыграть карты"),
        USE_LEADER("Использовать лидера"),
        PLAY_SPELLS("Использовать заклинания"),
        WIN_ROUNDS("Выиграть раунды"),
        COLLECT_CARDS("Получить карты"),
        DEAL_DAMAGE("Нанести урон"),
        HEAL_CARDS("Исцелить карты");
        
        private final String displayName;
        
        QuestType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Класс задания
     */
    public static class DailyQuest {
        private final QuestType type;
        private final int target;
        
        public DailyQuest(QuestType type, int target) {
            this.type = type;
            this.target = target;
        }
        
        public QuestType getType() {
            return type;
        }
        
        public int getTarget() {
            return target;
        }
        
        public String getDescription() {
            return type.getDisplayName() + " (" + target + ")";
        }
        
        public int getProgress(ServerPlayer player) {
            return getQuestProgress(player, type);
        }
        
        public boolean isCompleted(ServerPlayer player) {
            return getProgress(player) >= target;
        }
    }
    
    /**
     * Информация о ежедневных наградах
     */
    public static class DailyRewardInfo {
        private final int day;
        private final boolean claimed;
        private final int streakDays;
        
        public DailyRewardInfo(int day, boolean claimed, int streakDays) {
            this.day = day;
            this.claimed = claimed;
            this.streakDays = streakDays;
        }
        
        public int getDay() { return day; }
        public boolean isClaimed() { return claimed; }
        public int getStreakDays() { return streakDays; }
    }
    
    /**
     * Данные заданий игрока
     */
    private static class DailyQuestData {
        List<DailyQuest> quests = new ArrayList<>();
        Map<QuestType, Integer> progress = new HashMap<>();
        long lastQuestDay = -1;
    }
    
    private static class DailyRewardData {
        long lastRewardDay = -1;
        int streakDays = 0;
    }
}

