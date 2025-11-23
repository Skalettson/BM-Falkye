package com.bmfalkye.season;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRegistry;
import com.bmfalkye.cards.CardRarity;
import com.bmfalkye.player.PlayerProgress;
import com.bmfalkye.rating.RatingSystem;
import com.bmfalkye.storage.PlayerProgressStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Система сезонов и сезонных наград
 * Улучшенная версия с рейтинговой таблицей и наградами за окончание сезона
 */
public class SeasonSystem {
    private static Season currentSeason = null;
    private static final Map<UUID, SeasonProgress> playerSeasonProgress = new HashMap<>();
    private static final Map<Integer, List<SeasonRankingEntry>> seasonRankings = new HashMap<>(); // Сезон -> Рейтинг
    private static final int SEASON_DURATION_DAYS = 30; // 30 дней в Minecraft (в игровом времени)
    
    /**
     * Проверяет окончание сезона и выдаёт финальные награды
     */
    public static void checkSeasonEnd(net.minecraft.server.level.ServerLevel level) {
        if (currentSeason == null) {
            initializeSeason();
            return;
        }
        
        // Используем игровое время вместо реального
        long currentGameTime = level.getGameTime();
        long seasonStartTime = currentSeason.getStartGameTime();
        long seasonDuration = SEASON_DURATION_DAYS * 24000L; // 30 дней в игровом времени
        
        if (currentGameTime - seasonStartTime >= seasonDuration) {
            endSeason(level);
            startNewSeason(level);
        }
    }
    
    /**
     * Инициализирует текущий сезон
     */
    public static void initializeSeason() {
        if (currentSeason == null || currentSeason.isEnded()) {
            // Нужен ServerLevel для получения игрового времени
            // Это будет вызвано из обработчика тиков
            return;
        }
    }
    
    /**
     * Инициализирует сезон с сервером
     */
    public static void initializeSeason(net.minecraft.server.level.ServerLevel level) {
        if (currentSeason == null) {
            long currentGameTime = level.getGameTime();
            currentSeason = new Season(1, currentGameTime);
            generateSeasonRewards(currentSeason);
        } else {
            checkSeasonEnd(level);
        }
    }
    
    /**
     * Начинает новый сезон
     */
    private static void startNewSeason(net.minecraft.server.level.ServerLevel level) {
        int seasonNumber = currentSeason != null ? currentSeason.getNumber() + 1 : 1;
        long currentGameTime = level.getGameTime();
        currentSeason = new Season(seasonNumber, currentGameTime);
        
        // Генерируем сезонные награды
        generateSeasonRewards(currentSeason);
        
        // Очищаем прогресс игроков для нового сезона
        playerSeasonProgress.clear();
        
        // Уведомляем всех онлайн игроков
        for (net.minecraft.server.level.ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§6§l══════ НОВЫЙ СЕЗОН " + seasonNumber + "! ══════"));
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§7Продолжайте играть, чтобы получать награды за уровни сезона!"));
        }
    }
    
    /**
     * Завершает сезон и выдаёт финальные награды
     */
    private static void endSeason(net.minecraft.server.level.ServerLevel level) {
        if (currentSeason == null) {
            return;
        }
        
        int seasonNumber = currentSeason.getNumber();
        
        // Формируем рейтинговую таблицу
        List<SeasonRankingEntry> ranking = buildSeasonRanking();
        seasonRankings.put(seasonNumber, ranking);
        
        // Выдаём награды топ-игрокам
        giveSeasonEndRewards(ranking, level);
        
        // Уведомляем всех игроков
        for (net.minecraft.server.level.ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§6§l══════ СЕЗОН " + seasonNumber + " ЗАВЕРШЁН! ══════"));
            
            // Находим позицию игрока в рейтинге
            int playerRank = findPlayerRank(player.getUUID(), ranking);
            if (playerRank > 0 && playerRank <= 10) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§aВы заняли §e" + playerRank + " §aместо в рейтинге!"));
            }
        }
    }
    
    /**
     * Формирует рейтинговую таблицу сезона
     */
    private static List<SeasonRankingEntry> buildSeasonRanking() {
        List<SeasonRankingEntry> ranking = new ArrayList<>();
        
        for (Map.Entry<UUID, SeasonProgress> entry : playerSeasonProgress.entrySet()) {
            UUID playerUUID = entry.getKey();
            SeasonProgress progress = entry.getValue();
            
            // Получаем дополнительную статистику игрока
            // TODO: Можно добавить больше критериев для рейтинга
            
            ranking.add(new SeasonRankingEntry(playerUUID, progress.getLevel(), progress.getTotalXP()));
        }
        
        // Сортируем по уровню, затем по опыту
        ranking.sort((a, b) -> {
            if (a.getLevel() != b.getLevel()) {
                return Integer.compare(b.getLevel(), a.getLevel()); // Больше уровень = выше
            }
            return Integer.compare(b.getTotalXP(), a.getTotalXP()); // Больше опыт = выше
        });
        
        return ranking;
    }
    
    /**
     * Выдаёт награды топ-игрокам по окончании сезона
     */
    private static void giveSeasonEndRewards(List<SeasonRankingEntry> ranking, net.minecraft.server.level.ServerLevel level) {
        int topCount = Math.min(10, ranking.size()); // Топ-10
        
        for (int i = 0; i < topCount; i++) {
            SeasonRankingEntry entry = ranking.get(i);
            net.minecraft.server.level.ServerPlayer player = 
                level.getServer().getPlayerList().getPlayer(entry.getPlayerUUID());
            
            if (player == null) continue;
            
            PlayerProgressStorage storage = PlayerProgressStorage.get(level);
            PlayerProgress progress = storage.getPlayerProgress(player);
            
            // Награды в зависимости от места
            int rank = i + 1;
            int coinReward = 0;
            int xpReward = 0;
            Card cardReward = null;
            
            switch (rank) {
                case 1 -> {
                    coinReward = 5000;
                    xpReward = 5000;
                    cardReward = getRandomCard(CardRarity.LEGENDARY);
                    progress.getStatistics().put("season_top1", 
                        progress.getStatistics().getOrDefault("season_top1", 0) + 1);
                }
                case 2 -> {
                    coinReward = 3000;
                    xpReward = 3000;
                    cardReward = getRandomCard(CardRarity.EPIC);
                    progress.getStatistics().put("season_top2", 
                        progress.getStatistics().getOrDefault("season_top2", 0) + 1);
                }
                case 3 -> {
                    coinReward = 2000;
                    xpReward = 2000;
                    cardReward = getRandomCard(CardRarity.EPIC);
                    progress.getStatistics().put("season_top3", 
                        progress.getStatistics().getOrDefault("season_top3", 0) + 1);
                }
                case 4, 5 -> {
                    coinReward = 1000;
                    xpReward = 1000;
                    cardReward = getRandomCard(CardRarity.RARE);
                }
                default -> {
                    coinReward = 500;
                    xpReward = 500;
                }
            }
            
            // Выдаём награды
            if (coinReward > 0) {
                com.bmfalkye.storage.PlayerCurrency currency = com.bmfalkye.storage.PlayerCurrency.get(level);
                currency.addCoins(player, coinReward);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§6§lНАГРАДА ЗА СЕЗОН: §e" + coinReward + " монет"));
            }
            
            if (xpReward > 0) {
                progress.addExperience(xpReward);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§bПолучено опыта: " + xpReward));
            }
            
            if (cardReward != null) {
                progress.unlockCard(cardReward.getId());
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§a§lПОЛУЧЕНА КАРТА: §f" + cardReward.getName()));
            }
            
            storage.setPlayerProgress(player, progress);
            
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§6§lВы заняли §e" + rank + " §6§lместо в сезоне " + currentSeason.getNumber() + "!"));
        }
    }
    
    /**
     * Находит позицию игрока в рейтинге
     */
    private static int findPlayerRank(UUID playerUUID, List<SeasonRankingEntry> ranking) {
        for (int i = 0; i < ranking.size(); i++) {
            if (ranking.get(i).getPlayerUUID().equals(playerUUID)) {
                return i + 1;
            }
        }
        return -1;
    }
    
    /**
     * Получает рейтинговую таблицу текущего сезона
     */
    public static List<SeasonRankingEntry> getCurrentSeasonRanking() {
        if (currentSeason == null) {
            return new ArrayList<>();
        }
        return buildSeasonRanking();
    }
    
    /**
     * Получает рейтинговую таблицу конкретного сезона
     */
    public static List<SeasonRankingEntry> getSeasonRanking(int seasonNumber) {
        return seasonRankings.getOrDefault(seasonNumber, new ArrayList<>());
    }
    
    /**
     * Запись в рейтинге сезона
     */
    public static class SeasonRankingEntry {
        private final UUID playerUUID;
        private final int level;
        private final int totalXP;
        
        public SeasonRankingEntry(UUID playerUUID, int level, int totalXP) {
            this.playerUUID = playerUUID;
            this.level = level;
            this.totalXP = totalXP;
        }
        
        public UUID getPlayerUUID() { return playerUUID; }
        public int getLevel() { return level; }
        public int getTotalXP() { return totalXP; }
    }
    
    /**
     * Генерирует награды для сезона
     */
    private static void generateSeasonRewards(Season season) {
        List<SeasonReward> rewards = new ArrayList<>();
        
        // Награды за уровни сезона
        rewards.add(new SeasonReward(1, 50, "Опыт", null));
        rewards.add(new SeasonReward(2, 100, "Опыт", null));
        rewards.add(new SeasonReward(3, getRandomCard(CardRarity.COMMON), "Карта", null));
        rewards.add(new SeasonReward(5, 200, "Опыт", null));
        rewards.add(new SeasonReward(7, getRandomCard(CardRarity.RARE), "Карта", null));
        rewards.add(new SeasonReward(10, 500, "Опыт", null));
        rewards.add(new SeasonReward(12, getRandomCard(CardRarity.RARE), "Карта", null));
        rewards.add(new SeasonReward(15, 1000, "Опыт", null));
        rewards.add(new SeasonReward(17, getRandomCard(CardRarity.EPIC), "Карта", null));
        rewards.add(new SeasonReward(20, 2000, "Опыт", null));
        rewards.add(new SeasonReward(22, getRandomCard(CardRarity.EPIC), "Карта", null));
        rewards.add(new SeasonReward(25, 5000, "Опыт", null));
        rewards.add(new SeasonReward(27, getRandomCard(CardRarity.LEGENDARY), "Карта", null));
        rewards.add(new SeasonReward(30, getRandomCard(CardRarity.LEGENDARY), "Легендарная карта", null));
        
        season.setRewards(rewards);
    }
    
    /**
     * Получает случайную карту по редкости
     */
    private static Card getRandomCard(CardRarity rarity) {
        List<Card> cards = CardRegistry.getCardsByRarity(rarity);
        if (cards.isEmpty()) {
            return null;
        }
        return cards.get(new Random().nextInt(cards.size()));
    }
    
    /**
     * Обновляет прогресс сезона для игрока
     */
    public static void updateSeasonProgress(ServerPlayer player, int xpGained) {
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            if (currentSeason == null) {
                initializeSeason(serverLevel);
            } else if (currentSeason.isEnded(serverLevel)) {
                endSeason(serverLevel);
                startNewSeason(serverLevel);
            }
        }
        
        if (currentSeason == null) {
            return;
        }
        
        SeasonProgress progress = playerSeasonProgress.computeIfAbsent(
            player.getUUID(), k -> new SeasonProgress(currentSeason.getNumber()));
        
        progress.addXP(xpGained);
        
        // Проверяем, достиг ли игрок нового уровня сезона
        int newLevel = calculateSeasonLevel(progress.getTotalXP());
        if (newLevel > progress.getLevel()) {
            progress.setLevel(newLevel);
            giveSeasonReward(player, newLevel);
        }
        
        // Сохраняем прогресс
        saveSeasonProgress(player, progress);
    }
    
    /**
     * Вычисляет уровень сезона на основе опыта
     */
    private static int calculateSeasonLevel(int totalXP) {
        // Формула: уровень = sqrt(опыт / 100)
        return Math.min(30, (int)Math.sqrt(totalXP / 100.0) + 1);
    }
    
    /**
     * Выдаёт награду за уровень сезона
     */
    private static void giveSeasonReward(ServerPlayer player, int level) {
        if (currentSeason == null) {
            return;
        }
        
        SeasonReward reward = currentSeason.getRewardForLevel(level);
        if (reward == null) {
            return;
        }
        
        player.sendSystemMessage(Component.literal(
            "§6§lСЕЗОННЫЙ УРОВЕНЬ " + level + "!"));
        
        if (reward.getType().equals("Опыт")) {
            int xp = reward.getXpAmount();
            PlayerProgressStorage storage = PlayerProgressStorage.get(
                (net.minecraft.server.level.ServerLevel) player.level());
            PlayerProgress progress = storage.getPlayerProgress(player);
            progress.addExperience(xp);
            storage.setPlayerProgress(player, progress);
            
            player.sendSystemMessage(Component.literal("§aПолучено опыта: " + xp));
        } else if (reward.getType().equals("Карта") || reward.getType().equals("Легендарная карта")) {
            Card card = reward.getCard();
            if (card != null) {
                PlayerProgressStorage storage = PlayerProgressStorage.get(
                    (net.minecraft.server.level.ServerLevel) player.level());
                PlayerProgress progress = storage.getPlayerProgress(player);
                progress.unlockCard(card.getId());
                storage.setPlayerProgress(player, progress);
                
                player.sendSystemMessage(Component.literal(
                    "§a§lПОЛУЧЕНА КАРТА: §f" + card.getName()));
            }
        }
    }
    
    /**
     * Сохраняет прогресс сезона
     */
    private static void saveSeasonProgress(ServerPlayer player, SeasonProgress progress) {
        PlayerProgressStorage storage = PlayerProgressStorage.get(
            (net.minecraft.server.level.ServerLevel) player.level());
        PlayerProgress playerProgress = storage.getPlayerProgress(player);
        playerProgress.getStatistics().put("season_" + progress.getSeasonNumber() + "_xp", progress.getTotalXP());
        playerProgress.getStatistics().put("season_" + progress.getSeasonNumber() + "_level", progress.getLevel());
        storage.setPlayerProgress(player, playerProgress);
    }
    
    /**
     * Получает прогресс сезона игрока
     */
    public static SeasonProgress getPlayerSeasonProgress(ServerPlayer player) {
        if (currentSeason == null) {
            if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                initializeSeason(serverLevel);
            }
        }
        if (currentSeason == null) {
            return new SeasonProgress(1);
        }
        return playerSeasonProgress.computeIfAbsent(
            player.getUUID(), k -> new SeasonProgress(currentSeason.getNumber()));
    }
    
    /**
     * Получает информацию о сезоне для игрока
     */
    public static SeasonInfo getPlayerSeasonInfo(ServerPlayer player) {
        if (currentSeason == null) {
            if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                initializeSeason(serverLevel);
            }
        }
        SeasonProgress progress = getPlayerSeasonProgress(player);
        int xpForNext = calculateXPForLevel(progress.getLevel() + 1) - progress.getTotalXP();
        long daysRemaining = 0;
        if (currentSeason != null && player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            daysRemaining = currentSeason.getDaysRemaining(serverLevel);
        } else if (currentSeason != null) {
            daysRemaining = currentSeason.getDaysRemaining();
        }
        return new SeasonInfo(currentSeason != null ? currentSeason.getNumber() : 1, progress.getLevel(), 
            progress.getTotalXP(), xpForNext, daysRemaining);
    }
    
    /**
     * Вычисляет необходимый опыт для уровня
     */
    private static int calculateXPForLevel(int level) {
        return (level - 1) * (level - 1) * 100;
    }
    
    /**
     * Получает текущий сезон
     */
    public static Season getCurrentSeason() {
        return currentSeason;
    }
    
    /**
     * Получает текущий сезон с инициализацией
     */
    public static Season getCurrentSeason(net.minecraft.server.level.ServerLevel level) {
        if (currentSeason == null) {
            initializeSeason(level);
        }
        return currentSeason;
    }
    
    /**
     * Класс сезона
     */
    public static class Season {
        private final int number;
        private final long startTime; // Реальное время для совместимости
        private final long startGameTime; // Игровое время
        private final long endTime;
        private List<SeasonReward> rewards = new ArrayList<>();
        
        public Season(int number, long startGameTime) {
            this.number = number;
            this.startTime = System.currentTimeMillis();
            this.startGameTime = startGameTime;
            // endTime теперь рассчитывается на основе игрового времени
            this.endTime = startTime + (SEASON_DURATION_DAYS * 24 * 60 * 60 * 1000L);
        }
        
        public long getStartGameTime() {
            return startGameTime;
        }
        
        public SeasonReward getRewardForLevel(int level) {
            for (SeasonReward reward : rewards) {
                if (reward.getLevel() == level) {
                    return reward;
                }
            }
            return null;
        }
        
        public boolean isEnded() {
            // Проверяется через checkSeasonEnd с использованием игрового времени
            return System.currentTimeMillis() >= endTime;
        }
        
        public boolean isEnded(net.minecraft.server.level.ServerLevel level) {
            long currentGameTime = level.getGameTime();
            long seasonDuration = SEASON_DURATION_DAYS * 24000L;
            return currentGameTime - startGameTime >= seasonDuration;
        }
        
        public int getNumber() { return number; }
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
        public List<SeasonReward> getRewards() { return rewards; }
        public void setRewards(List<SeasonReward> rewards) { this.rewards = rewards; }
        
        public long getDaysRemaining(net.minecraft.server.level.ServerLevel level) {
            long currentGameTime = level.getGameTime();
            long seasonDuration = SEASON_DURATION_DAYS * 24000L;
            long remaining = seasonDuration - (currentGameTime - startGameTime);
            return Math.max(0, remaining / 24000L); // Дни в игровом времени
        }
        
        public long getDaysRemaining() {
            long remaining = endTime - System.currentTimeMillis();
            return Math.max(0, remaining / (24 * 60 * 60 * 1000L));
        }
    }
    
    /**
     * Награда сезона
     */
    public static class SeasonReward {
        private final int level;
        private final int xpAmount;
        private final Card card;
        private final String type;
        
        public SeasonReward(int level, int xpAmount, String type, Card card) {
            this.level = level;
            this.xpAmount = xpAmount;
            this.type = type;
            this.card = card;
        }
        
        public SeasonReward(int level, Card card, String type, Void unused) {
            this.level = level;
            this.xpAmount = 0;
            this.type = type;
            this.card = card;
        }
        
        public int getLevel() { return level; }
        public int getXpAmount() { return xpAmount; }
        public Card getCard() { return card; }
        public String getType() { return type; }
    }
    
    /**
     * Прогресс игрока в сезоне
     */
    /**
     * Информация о сезоне
     */
    public static class SeasonInfo {
        private final int seasonNumber;
        private final int seasonLevel;
        private final int seasonXP;
        private final int xpForNextLevel;
        private final long daysRemaining;
        
        public SeasonInfo(int seasonNumber, int seasonLevel, int seasonXP, 
                         int xpForNextLevel, long daysRemaining) {
            this.seasonNumber = seasonNumber;
            this.seasonLevel = seasonLevel;
            this.seasonXP = seasonXP;
            this.xpForNextLevel = xpForNextLevel;
            this.daysRemaining = daysRemaining;
        }
        
        public int getSeasonNumber() { return seasonNumber; }
        public int getSeasonLevel() { return seasonLevel; }
        public int getSeasonXP() { return seasonXP; }
        public int getXPForNextLevel() { return xpForNextLevel; }
        public long getDaysRemaining() { return daysRemaining; }
    }
    
    public static class SeasonProgress {
        private final int seasonNumber;
        private int level = 1;
        private int totalXP = 0;
        
        public SeasonProgress(int seasonNumber) {
            this.seasonNumber = seasonNumber;
        }
        
        public void addXP(int amount) {
            this.totalXP += amount;
        }
        
        public int getSeasonNumber() { return seasonNumber; }
        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }
        public int getTotalXP() { return totalXP; }
    }
}

