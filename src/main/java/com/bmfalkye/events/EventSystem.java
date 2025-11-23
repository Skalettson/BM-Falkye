package com.bmfalkye.events;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRegistry;
import com.bmfalkye.cards.CardRarity;
import com.bmfalkye.player.PlayerProgress;
import com.bmfalkye.storage.PlayerProgressStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Система событий и ограниченных по времени карт
 * Улучшенная версия с автоматическим созданием, прогрессом событий и квастами
 */
public class EventSystem {
    private static final Map<String, GameEvent> activeEvents = new HashMap<>();
    private static final Map<UUID, Map<String, EventProgress>> playerEventProgress = new HashMap<>();
    
    // Автоматическое создание событий
    private static final long AUTO_EVENT_INTERVAL = 172800000L; // 48 часов в миллисекундах
    private static long lastAutoEventTime = System.currentTimeMillis();
    private static final int MAX_ACTIVE_EVENTS = 3; // Максимум 3 активных события одновременно
    
    // Счётчик для коротких ID
    private static int eventIdCounter = 1;
    
    /**
     * Генерирует короткий ID для события
     */
    private static String generateShortId() {
        return "E" + eventIdCounter++;
    }
    
    /**
     * Создаёт новое событие
     */
    public static GameEvent createEvent(String name, EventType type, long duration, 
                                       List<Card> eventCards, Map<String, Object> rewards) {
        GameEvent event = new GameEvent(name, type, duration, eventCards, rewards);
        activeEvents.put(event.getId(), event);
        return event;
    }
    
    /**
     * Проверяет активные события и выдаёт награды
     */
    public static void checkActiveEvents(ServerPlayer player) {
        for (GameEvent event : activeEvents.values()) {
            if (event.isActive() && !event.hasPlayerParticipated(player)) {
                // Игрок может участвовать в событии
                notifyPlayerAboutEvent(player, event);
            }
        }
    }
    
    /**
     * Уведомляет игрока о событии
     */
    private static void notifyPlayerAboutEvent(ServerPlayer player, GameEvent event) {
        player.sendSystemMessage(Component.literal("§6§l[СОБЫТИЕ] §r§e" + event.getName()));
        player.sendSystemMessage(Component.literal("§7" + event.getDescription()));
        player.sendSystemMessage(Component.literal("§7Осталось времени: §f" + 
            formatTimeRemaining(event.getTimeRemaining())));
    }
    
    /**
     * Форматирует оставшееся время
     */
    private static String formatTimeRemaining(long milliseconds) {
        if (milliseconds <= 0) {
            return "0м";
        }
        
        long totalSeconds = milliseconds / 1000;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        
        if (days > 0) {
            return days + "д " + hours + "ч " + minutes + "м";
        } else if (hours > 0) {
            return hours + "ч " + minutes + "м";
        } else if (minutes > 0) {
            return minutes + "м " + seconds + "с";
        } else {
            return seconds + "с";
        }
    }
    
    /**
     * Публичный метод для форматирования времени (для использования в других классах)
     */
    public static String formatTime(long milliseconds) {
        return formatTimeRemaining(milliseconds);
    }
    
    /**
     * Выдаёт награды за участие в событии
     */
    public static void giveEventRewards(ServerPlayer player, String eventId) {
        GameEvent event = activeEvents.get(eventId);
        if (event == null || !event.isActive()) {
            return;
        }
        
        if (event.hasPlayerParticipated(player)) {
            player.sendSystemMessage(Component.literal("§cВы уже получили награды за это событие!"));
            return;
        }
        
        // Выдаём награды
        for (Card card : event.getEventCards()) {
            PlayerProgressStorage storage = PlayerProgressStorage.get(
                (net.minecraft.server.level.ServerLevel) player.level());
            PlayerProgress progress = storage.getPlayerProgress(player);
            progress.unlockCard(card.getId());
            storage.setPlayerProgress(player, progress);
            
            player.sendSystemMessage(Component.literal("§aПолучена карта события: §f" + card.getName()));
        }
        
        // Дополнительные награды
        Map<String, Object> rewards = event.getRewards();
        if (rewards.containsKey("xp")) {
            int xp = (Integer)rewards.get("xp");
            PlayerProgressStorage storage = PlayerProgressStorage.get(
                (net.minecraft.server.level.ServerLevel) player.level());
            PlayerProgress progress = storage.getPlayerProgress(player);
            progress.addExperience(xp);
            storage.setPlayerProgress(player, progress);
            
            player.sendSystemMessage(Component.literal("§aПолучено опыта: " + xp));
        }
        
        event.markPlayerAsParticipated(player);
    }
    
    /**
     * Получает активные события
     */
    public static List<GameEvent> getActiveEvents() {
        List<GameEvent> active = new ArrayList<>();
        for (GameEvent event : activeEvents.values()) {
            if (event.isActive()) {
                active.add(event);
            }
        }
        return active;
    }
    
    /**
     * Проверяет и автоматически создаёт новые события
     */
    public static void checkAndCreateAutoEvents(ServerLevel level) {
        // Удаляем завершённые события
        removeFinishedEvents();
        
        // Проверяем, нужно ли создавать новое событие
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAutoEventTime < AUTO_EVENT_INTERVAL) {
            return;
        }
        
        // Проверяем, не превышен ли лимит активных событий
        int activeCount = getActiveEvents().size();
        if (activeCount >= MAX_ACTIVE_EVENTS) {
            return;
        }
        
        // Создаём случайное событие
        createRandomEvent(level);
        lastAutoEventTime = currentTime;
    }
    
    /**
     * Удаляет завершённые события
     */
    private static void removeFinishedEvents() {
        activeEvents.entrySet().removeIf(entry -> !entry.getValue().isActive());
    }
    
    /**
     * Создаёт случайное событие
     */
    private static void createRandomEvent(ServerLevel level) {
        Random random = new Random();
        EventType[] types = EventType.values();
        EventType type = types[random.nextInt(types.length)];
        
        String[] eventNames = {
            "Великая битва",
            "Сокровища дракона",
            "Праздник карт",
            "Время легенд",
            "Вызов мастеров",
            "Двойной удар",
            "Эксклюзивные карты"
        };
        
        String name = eventNames[random.nextInt(eventNames.length)];
        long duration = 86400000L + random.nextInt(172800000); // 1-3 дня
        
        // Генерируем награды в зависимости от типа
        List<Card> eventCards = new ArrayList<>();
        Map<String, Object> rewards = new HashMap<>();
        
        switch (type) {
            case LIMITED_TIME_CARDS -> {
                // 1-2 эксклюзивные карты
                eventCards.add(getRandomCardByRarity(CardRarity.EPIC));
                if (random.nextBoolean()) {
                    eventCards.add(getRandomCardByRarity(CardRarity.RARE));
                }
                rewards.put("xp", 500);
                rewards.put("coins", 200);
            }
            case DOUBLE_XP -> {
                rewards.put("xp_multiplier", 2.0);
                rewards.put("duration", duration);
            }
            case TOURNAMENT -> {
                rewards.put("tournament_boost", true);
                rewards.put("coins", 500);
            }
            case COLLECTION_CHALLENGE -> {
                // Кваст на сбор карт
                eventCards.add(getRandomCardByRarity(CardRarity.LEGENDARY));
                rewards.put("xp", 1000);
                rewards.put("coins", 300);
            }
        }
        
        GameEvent event = createEvent(name, type, duration, eventCards, rewards);
        
        // Уведомляем всех игроков
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            player.sendSystemMessage(Component.literal(
                "§6§l══════ НОВОЕ СОБЫТИЕ! ══════"));
            player.sendSystemMessage(Component.literal(
                "§6§l" + event.getName()));
            player.sendSystemMessage(Component.literal(
                "§7" + event.getDescription()));
            player.sendSystemMessage(Component.literal(
                "§7Длительность: §f" + formatTimeRemaining(event.getTimeRemaining())));
        }
    }
    
    /**
     * Получает случайную карту по редкости
     */
    private static Card getRandomCardByRarity(CardRarity rarity) {
        List<Card> cards = CardRegistry.getCardsByRarity(rarity);
        if (cards.isEmpty()) {
            return null;
        }
        return cards.get(new Random().nextInt(cards.size()));
    }
    
    /**
     * Увеличивает прогресс события для игрока
     */
    public static void incrementEventProgress(ServerPlayer player, String eventId, EventQuestType questType, int amount) {
        GameEvent event = activeEvents.get(eventId);
        if (event == null || !event.isActive()) {
            return;
        }
        
        Map<String, EventProgress> playerProgress = playerEventProgress.computeIfAbsent(
            player.getUUID(), k -> new HashMap<>());
        EventProgress progress = playerProgress.computeIfAbsent(
            eventId, k -> new EventProgress(eventId));
        
        progress.incrementProgress(questType, amount);
        
        // Проверяем выполнение квастов
        checkEventQuests(player, event, progress);
    }
    
    /**
     * Проверяет выполнение квастов события
     */
    private static void checkEventQuests(ServerPlayer player, GameEvent event, EventProgress progress) {
        for (EventQuest quest : event.getQuests()) {
            if (progress.isQuestCompleted(quest.getType())) {
                continue; // Кваст уже выполнен
            }
            
            int currentProgress = progress.getProgress(quest.getType());
            if (currentProgress >= quest.getTarget()) {
                // Кваст выполнен
                progress.completeQuest(quest.getType());
                giveEventQuestReward(player, event, quest);
            }
        }
    }
    
    /**
     * Выдаёт награду за выполнение кваста события
     */
    private static void giveEventQuestReward(ServerPlayer player, GameEvent event, EventQuest quest) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        
        PlayerProgressStorage storage = PlayerProgressStorage.get(serverLevel);
        PlayerProgress progress = storage.getPlayerProgress(player);
        com.bmfalkye.storage.PlayerCurrency currency = com.bmfalkye.storage.PlayerCurrency.get(serverLevel);
        
        player.sendSystemMessage(Component.literal(
            "§a§lКВАСТ ВЫПОЛНЕН: §f" + quest.getDescription()));
        
        // Выдаём награды кваста
        if (quest.getRewardXP() > 0) {
            progress.addExperience(quest.getRewardXP());
            player.sendSystemMessage(Component.literal(
                "§bПолучено опыта: " + quest.getRewardXP()));
        }
        
        if (quest.getRewardCoins() > 0) {
            currency.addCoins(player, quest.getRewardCoins());
            player.sendSystemMessage(Component.literal(
                "§eПолучено монет: " + quest.getRewardCoins()));
        }
        
        if (quest.getRewardCard() != null) {
            progress.unlockCard(quest.getRewardCard().getId());
            player.sendSystemMessage(Component.literal(
                "§a§lПОЛУЧЕНА КАРТА: §f" + quest.getRewardCard().getName()));
        }
        
        storage.setPlayerProgress(player, progress);
    }
    
    /**
     * Получает прогресс игрока в событии
     */
    public static EventProgress getPlayerEventProgress(ServerPlayer player, String eventId) {
        Map<String, EventProgress> playerProgress = playerEventProgress.get(player.getUUID());
        if (playerProgress == null) {
            return new EventProgress(eventId);
        }
        return playerProgress.getOrDefault(eventId, new EventProgress(eventId));
    }
    
    /**
     * Тип события
     */
    public enum EventType {
        LIMITED_TIME_CARDS("Ограниченные карты"),
        DOUBLE_XP("Двойной опыт"),
        TOURNAMENT("Турнир"),
        COLLECTION_CHALLENGE("Вызов коллекции");
        
        private final String displayName;
        
        EventType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Класс события
     */
    public static class GameEvent {
        private final String id;
        private final String name;
        private final EventType type;
        private final long startTime;
        private final long endTime;
        private final List<Card> eventCards;
        private final Map<String, Object> rewards;
        private final Set<UUID> participants = new HashSet<>();
        private final List<EventQuest> quests = new ArrayList<>();
        private String description;
        
        public GameEvent(String name, EventType type, long duration, 
                        List<Card> eventCards, Map<String, Object> rewards) {
            this.id = generateShortId();
            this.name = name;
            this.type = type;
            this.startTime = System.currentTimeMillis();
            this.endTime = startTime + duration;
            this.eventCards = eventCards;
            this.rewards = rewards;
            this.description = generateDescription(type);
            
            // Генерируем квасты для события
            generateEventQuests();
        }
        
        /**
         * Генерирует квасты для события
         */
        private void generateEventQuests() {
            Random random = new Random();
            
            // Всегда добавляем базовые квасты
            switch (type) {
                case LIMITED_TIME_CARDS, COLLECTION_CHALLENGE -> {
                    quests.add(new EventQuest(EventQuestType.WIN_GAMES, 5, 200, 50, null));
                    quests.add(new EventQuest(EventQuestType.PLAY_CARDS, 20, 150, 30, null));
                    if (random.nextBoolean()) {
                        quests.add(new EventQuest(EventQuestType.COLLECT_CARDS, 3, 300, 100, 
                            getRandomCardByRarity(CardRarity.RARE)));
                    }
                }
                case DOUBLE_XP -> {
                    quests.add(new EventQuest(EventQuestType.WIN_GAMES, 10, 500, 100, null));
                    quests.add(new EventQuest(EventQuestType.WIN_ROUNDS, 20, 300, 50, null));
                }
                case TOURNAMENT -> {
                    quests.add(new EventQuest(EventQuestType.WIN_TOURNAMENT, 1, 1000, 500, 
                        getRandomCardByRarity(CardRarity.EPIC)));
                }
            }
        }
        
        private Card getRandomCardByRarity(CardRarity rarity) {
            List<Card> cards = CardRegistry.getCardsByRarity(rarity);
            if (cards.isEmpty()) {
                return null;
            }
            return cards.get(new Random().nextInt(cards.size()));
        }
        
        private String generateDescription(EventType type) {
            return switch (type) {
                case LIMITED_TIME_CARDS -> "Получите эксклюзивные карты, доступные только во время события!";
                case DOUBLE_XP -> "Получайте двойной опыт за все игры!";
                case TOURNAMENT -> "Участвуйте в специальном турнире с уникальными призами!";
                case COLLECTION_CHALLENGE -> "Соберите все карты события для получения особой награды!";
            };
        }
        
        public boolean isActive() {
            return System.currentTimeMillis() < endTime;
        }
        
        public long getTimeRemaining() {
            return Math.max(0, endTime - System.currentTimeMillis());
        }
        
        public boolean hasPlayerParticipated(ServerPlayer player) {
            return participants.contains(player.getUUID());
        }
        
        public void markPlayerAsParticipated(ServerPlayer player) {
            participants.add(player.getUUID());
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public EventType getType() { return type; }
        public String getDescription() { return description; }
        public List<Card> getEventCards() { return eventCards; }
        public Map<String, Object> getRewards() { return rewards; }
        public List<EventQuest> getQuests() { return quests; }
    }
    
    /**
     * Тип кваста события
     */
    public enum EventQuestType {
        WIN_GAMES("Выиграть игры"),
        PLAY_CARDS("Разыграть карты"),
        WIN_ROUNDS("Выиграть раунды"),
        COLLECT_CARDS("Собрать карты"),
        WIN_TOURNAMENT("Выиграть турнир"),
        USE_LEADER("Использовать лидера"),
        PLAY_SPELLS("Использовать заклинания");
        
        private final String displayName;
        
        EventQuestType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Кваст события
     */
    public static class EventQuest {
        private final EventQuestType type;
        private final int target;
        private final int rewardXP;
        private final int rewardCoins;
        private final Card rewardCard;
        
        public EventQuest(EventQuestType type, int target, int rewardXP, int rewardCoins, Card rewardCard) {
            this.type = type;
            this.target = target;
            this.rewardXP = rewardXP;
            this.rewardCoins = rewardCoins;
            this.rewardCard = rewardCard;
        }
        
        public EventQuestType getType() { return type; }
        public int getTarget() { return target; }
        public int getRewardXP() { return rewardXP; }
        public int getRewardCoins() { return rewardCoins; }
        public Card getRewardCard() { return rewardCard; }
        
        public String getDescription() {
            return type.getDisplayName() + " (" + target + ")";
        }
    }
    
    /**
     * Прогресс игрока в событии
     */
    public static class EventProgress {
        private final String eventId;
        private final Map<EventQuestType, Integer> progress = new HashMap<>();
        private final Set<EventQuestType> completedQuests = new HashSet<>();
        
        public EventProgress(String eventId) {
            this.eventId = eventId;
        }
        
        public void incrementProgress(EventQuestType type, int amount) {
            progress.put(type, progress.getOrDefault(type, 0) + amount);
        }
        
        public int getProgress(EventQuestType type) {
            return progress.getOrDefault(type, 0);
        }
        
        public boolean isQuestCompleted(EventQuestType type) {
            return completedQuests.contains(type);
        }
        
        public void completeQuest(EventQuestType type) {
            completedQuests.add(type);
        }
        
        public String getEventId() { return eventId; }
        public Map<EventQuestType, Integer> getProgress() { return progress; }
        public Set<EventQuestType> getCompletedQuests() { return completedQuests; }
    }
}

