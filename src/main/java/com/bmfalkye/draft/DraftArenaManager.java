package com.bmfalkye.draft;

import com.bmfalkye.cards.CardDeck;
import com.bmfalkye.cards.LeaderCard;
import com.bmfalkye.game.FalkyeGameSession;
import com.bmfalkye.game.GameManager;
import com.bmfalkye.game.MatchConfig;
import com.bmfalkye.network.NetworkHandler;
import com.bmfalkye.util.ModLogger;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Менеджер арены драфта - управляет поиском оппонентов и запуском матчей
 */
public class DraftArenaManager {
    // Игроки, ожидающие матч в арене
    private static final Map<UUID, DraftArenaQueueEntry> arenaQueue = new ConcurrentHashMap<>();
    
    /**
     * Добавляет игрока в очередь арены
     */
    public static void queueForArena(ServerPlayer player) {
        if (player.level() instanceof ServerLevel serverLevel) {
            DraftStorage storage = DraftStorage.get(serverLevel);
            DraftSession session = storage.getSession(player);
            
            if (session == null || !session.isCompleted()) {
                player.sendSystemMessage(Component.literal("§cСначала завершите драфт!"));
                return;
            }
            
            if (session.isArenaCompleted()) {
                player.sendSystemMessage(Component.literal("§cАрена уже завершена!"));
                return;
            }
            
            // Проверяем, не в очереди ли уже
            if (arenaQueue.containsKey(player.getUUID())) {
                player.sendSystemMessage(Component.literal("§cВы уже в очереди!"));
                return;
            }
            
            // Добавляем в очередь
            DraftArenaQueueEntry entry = new DraftArenaQueueEntry(player, session);
            arenaQueue.put(player.getUUID(), entry);
            
            player.sendSystemMessage(Component.literal("§6Поиск оппонента для арены..."));
            
            // Пытаемся найти оппонента
            findMatch(player, serverLevel);
        }
    }
    
    /**
     * Ищет оппонента для игрока
     */
    private static void findMatch(ServerPlayer player, ServerLevel level) {
        DraftArenaQueueEntry playerEntry = arenaQueue.get(player.getUUID());
        if (playerEntry == null) {
            return;
        }
        
        // Ищем другого игрока в очереди
        for (Map.Entry<UUID, DraftArenaQueueEntry> entry : arenaQueue.entrySet()) {
            if (entry.getKey().equals(player.getUUID())) {
                continue; // Пропускаем самого игрока
            }
            
            ServerPlayer opponent = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (opponent != null && opponent.isAlive()) {
                // Нашли оппонента - запускаем матч
                startArenaMatch(player, opponent, level);
                return;
            }
        }
        
        // Если не нашли оппонента, создаём AI оппонента
        createAIMatch(player, level);
    }
    
    /**
     * Создаёт матч с AI оппонентом
     */
    private static void createAIMatch(ServerPlayer player, ServerLevel level) {
        DraftStorage storage = DraftStorage.get(level);
        DraftSession session = storage.getSession(player);
        
        if (session == null) {
            return;
        }
        
        // Создаём колоду из драфта
        CardDeck playerDeck = session.createDeck();
        if (playerDeck == null) {
            player.sendSystemMessage(Component.literal("§cОшибка: не удалось создать колоду из драфта!"));
            return;
        }
        
        // Создаём AI колоду (случайная колода из всех карт)
        CardDeck aiDeck = createAIDeck();
        
        // Создаём лидеров
        LeaderCard playerLeader = session.getSelectedLeader();
        if (playerLeader == null) {
            playerLeader = com.bmfalkye.cards.LeaderRegistry.getLeader("leader_fire_architect");
        }
        // Выбираем случайного лидера для AI
        java.util.List<LeaderCard> allLeaders = com.bmfalkye.cards.LeaderRegistry.getAllLeaders();
        LeaderCard aiLeader = allLeaders.isEmpty() ? 
            com.bmfalkye.cards.LeaderRegistry.getLeader("leader_fire_architect") :
            allLeaders.get(new Random().nextInt(allLeaders.size()));
        
        // Создаём конфигурацию матча
        MatchConfig config = new MatchConfig();
        config.setDraftArena(true); // Помечаем как матч арены
        
        // Создаём игровую сессию
        FalkyeGameSession gameSession = new FalkyeGameSession(
            player, null, // player2 = null для AI
            playerDeck, aiDeck,
            playerLeader, aiLeader
        );
        
        // Устанавливаем флаг арены
        gameSession.setDraftArena(true);
        gameSession.setDraftSession(session);
        
        // Сохраняем в активные игры
        GameManager.getActiveGames().put(player.getUUID(), gameSession);
        GameManager.getActiveVillagerGames().put(player.getUUID(), gameSession);
        
        // Открываем игровой экран
        NetworkHandler.openGameScreen(player, gameSession);
        
        // Удаляем из очереди
        arenaQueue.remove(player.getUUID());
        
        player.sendSystemMessage(Component.literal("§6Матч арены начат!"));
        
        ModLogger.logGameEvent("Draft arena match started", 
            "player", player.getName().getString(),
            "wins", session.getWins(),
            "losses", session.getLosses());
    }
    
    /**
     * Запускает матч между двумя игроками в арене
     */
    private static void startArenaMatch(ServerPlayer player1, ServerPlayer player2, ServerLevel level) {
        DraftStorage storage = DraftStorage.get(level);
        DraftSession session1 = storage.getSession(player1);
        DraftSession session2 = storage.getSession(player2);
        
        if (session1 == null || session2 == null) {
            return;
        }
        
        // Создаём колоды из драфта
        CardDeck deck1 = session1.createDeck();
        CardDeck deck2 = session2.createDeck();
        
        if (deck1 == null || deck2 == null) {
            player1.sendSystemMessage(Component.literal("§cОшибка: не удалось создать колоду!"));
            player2.sendSystemMessage(Component.literal("§cОшибка: не удалось создать колоду!"));
            return;
        }
        
        // Создаём лидеров
        LeaderCard leader1 = session1.getSelectedLeader();
        LeaderCard leader2 = session2.getSelectedLeader();
        
        if (leader1 == null) {
            leader1 = com.bmfalkye.cards.LeaderRegistry.getLeader("leader_fire_architect");
        }
        if (leader2 == null) {
            leader2 = com.bmfalkye.cards.LeaderRegistry.getLeader("leader_fire_architect");
        }
        
        // Создаём конфигурацию матча
        MatchConfig config = new MatchConfig();
        config.setDraftArena(true);
        
        // Создаём игровую сессию
        FalkyeGameSession gameSession = new FalkyeGameSession(
            player1, player2,
            deck1, deck2,
            leader1, leader2
        );
        
        // Устанавливаем флаги арены
        gameSession.setDraftArena(true);
        gameSession.setDraftSession(session1);
        gameSession.setDraftSession2(session2);
        
        // Сохраняем в активные игры
        GameManager.getActiveGames().put(player1.getUUID(), gameSession);
        GameManager.getActiveGames().put(player2.getUUID(), gameSession);
        
        // Открываем игровые экраны
        NetworkHandler.openGameScreen(player1, gameSession);
        NetworkHandler.openGameScreen(player2, gameSession);
        
        // Удаляем из очереди
        arenaQueue.remove(player1.getUUID());
        arenaQueue.remove(player2.getUUID());
        
        player1.sendSystemMessage(Component.literal("§6Матч арены начат!"));
        player2.sendSystemMessage(Component.literal("§6Матч арены начат!"));
        
        ModLogger.logGameEvent("Draft arena match started (PvP)", 
            "player1", player1.getName().getString(),
            "player2", player2.getName().getString());
    }
    
    /**
     * Создаёт случайную колоду для AI
     */
    private static CardDeck createAIDeck() {
        CardDeck deck = new CardDeck();
        List<com.bmfalkye.cards.Card> allCards = com.bmfalkye.cards.CardRegistry.getAllCards();
        Random random = new Random();
        
        // Добавляем 30 случайных карт
        Set<String> usedCardIds = new HashSet<>();
        while (deck.getCards().size() < 30 && usedCardIds.size() < allCards.size()) {
            com.bmfalkye.cards.Card randomCard = allCards.get(random.nextInt(allCards.size()));
            if (!usedCardIds.contains(randomCard.getId())) {
                deck.addCard(randomCard);
                usedCardIds.add(randomCard.getId());
            }
        }
        
        return deck;
    }
    
    /**
     * Обрабатывает результат матча арены
     */
    public static void handleArenaMatchResult(FalkyeGameSession session, ServerPlayer winner, ServerPlayer loser) {
        if (winner == null || !session.isDraftArena()) {
            return;
        }
        
        if (winner.level() instanceof ServerLevel level) {
            DraftStorage storage = DraftStorage.get(level);
            
            // Обновляем статистику победителя
            DraftSession winnerSession = session.getDraftSession();
            if (winnerSession == null && winner.equals(session.getPlayer1())) {
                winnerSession = session.getDraftSession();
            } else if (winnerSession == null && winner.equals(session.getPlayer2()) && session.getDraftSession2() != null) {
                winnerSession = session.getDraftSession2();
            }
            
            if (winnerSession != null) {
                winnerSession.recordWin();
                
                if (winnerSession.isArenaCompleted()) {
                    // Арена завершена - выдаём награды
                    DraftSystem.giveArenaRewards(winner, winnerSession.getWins());
                    storage.removeSession(winner);
                    winner.sendSystemMessage(Component.literal("§6§lАрена завершена! Награды получены."));
                } else {
                    // Продолжаем арену
                    winner.sendSystemMessage(Component.literal("§aПобеда! Побед: §e" + winnerSession.getWins() + 
                        "§7/§e7 §7| Поражений: §c" + winnerSession.getLosses() + "§7/§c3"));
                    winner.sendSystemMessage(Component.literal("§7Используйте команду для продолжения арены"));
                }
            }
            
            // Обновляем статистику проигравшего (если это PvP)
            if (loser != null) {
                DraftSession loserSession = session.getDraftSession2();
                if (loserSession == null && loser.equals(session.getPlayer1())) {
                    loserSession = session.getDraftSession();
                } else if (loserSession == null && loser.equals(session.getPlayer2())) {
                    loserSession = session.getDraftSession2();
                }
                
                if (loserSession != null) {
                    loserSession.recordLoss();
                    
                    if (loserSession.isArenaCompleted()) {
                        // Арена завершена - выдаём награды
                        DraftSystem.giveArenaRewards(loser, loserSession.getWins());
                        storage.removeSession(loser);
                        loser.sendSystemMessage(Component.literal("§6§lАрена завершена! Награды получены."));
                    } else {
                        // Продолжаем арену
                        loser.sendSystemMessage(Component.literal("§cПоражение. Побед: §e" + loserSession.getWins() + 
                            "§7/§e7 §7| Поражений: §c" + loserSession.getLosses() + "§7/§c3"));
                        loser.sendSystemMessage(Component.literal("§7Используйте команду для продолжения арены"));
                    }
                }
            }
        }
    }
    
    /**
     * Удаляет игрока из очереди
     */
    public static void removeFromQueue(ServerPlayer player) {
        arenaQueue.remove(player.getUUID());
    }
    
    /**
     * Запись в очереди арены
     */
    private static class DraftArenaQueueEntry {
        private final ServerPlayer player;
        private final DraftSession session;
        private final long queueTime;
        
        public DraftArenaQueueEntry(ServerPlayer player, DraftSession session) {
            this.player = player;
            this.session = session;
            this.queueTime = System.currentTimeMillis();
        }
        
        public ServerPlayer getPlayer() { return player; }
        public DraftSession getSession() { return session; }
        public long getQueueTime() { return queueTime; }
    }
}

