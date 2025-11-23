package com.bmfalkye.game;

import com.bmfalkye.BMFalkye;
import com.bmfalkye.cards.CardDeck;
import com.bmfalkye.cards.LeaderCard;
import com.bmfalkye.cards.LeaderRegistry;
import com.bmfalkye.network.NetworkHandler;
import com.bmfalkye.util.ModLogger;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager {
    // Используем ConcurrentHashMap для потокобезопасности
    private static final Map<UUID, PendingChallenge> pendingChallenges = new ConcurrentHashMap<>();
    private static final Map<UUID, FalkyeGameSession> activeGames = new ConcurrentHashMap<>();
    private static final Map<UUID, FalkyeGameSession> activeVillagerGames = new ConcurrentHashMap<>(); // Key: Player UUID
    
    public static Map<UUID, FalkyeGameSession> getActiveGames() {
        return activeGames;
    }
    
    public static Map<UUID, FalkyeGameSession> getActiveVillagerGames() {
        return activeVillagerGames;
    }
    
    /**
     * Проверяет, есть ли у игрока активная игра
     */
    public static boolean hasActiveGame(ServerPlayer player) {
        return activeGames.containsKey(player.getUUID()) || activeVillagerGames.containsKey(player.getUUID());
    }

    /**
     * Отправляет вызов на дуэль от одного игрока другому
     * Теперь открывает предматчевое меню для обоих игроков
     */
    public static void sendDuelChallenge(ServerPlayer challenger, ServerPlayer target) {
        // Валидация входных данных
        if (!com.bmfalkye.util.InputValidator.isPlayerValid(challenger)) {
            ModLogger.warn("Invalid challenger in sendDuelChallenge");
            return;
        }
        
        if (!com.bmfalkye.util.InputValidator.isPlayerValid(target)) {
            challenger.sendSystemMessage(Component.literal("§cНеверный противник"));
            return;
        }
        
        ModLogger.logGameEvent("Duel challenge sent", "challenger", challenger.getName().getString(), "target", target.getName().getString());
        
        // Проверяем, не занят ли целевой игрок
        if (activeGames.containsKey(target.getUUID())) {
            ModLogger.logGameEvent("Challenge failed: target busy", "challenger", challenger.getName().getString(), "target", target.getName().getString());
            challenger.sendSystemMessage(Component.translatable("message.bm_falkye.target_busy", target.getName()));
            return;
        }

        // Проверяем, не отправил ли уже вызов
        if (pendingChallenges.containsKey(challenger.getUUID())) {
            ModLogger.logGameEvent("Challenge failed: challenger already has pending challenge", "challenger", challenger.getName().getString());
            challenger.sendSystemMessage(Component.translatable("message.bm_falkye.already_challenged"));
            return;
        }

        // Создаём вызов
        PendingChallenge challenge = new PendingChallenge(challenger.getUUID(), target.getUUID());
        pendingChallenges.put(target.getUUID(), challenge);
        ModLogger.logGameEvent("Challenge created", "challenger", challenger.getName().getString(), "target", target.getName().getString(), "challengeId", challenge.getTargetUUID().toString());

        // Открываем предматчевое меню для обоих игроков
        // Challenger - не тот, на кого вызвали (isChallenged = false)
        NetworkHandler.openPreMatchScreen(challenger, target.getUUID(), target.getName().getString(), false, -1, false);
        // Target - тот, на кого вызвали (isChallenged = true)
        NetworkHandler.openPreMatchScreen(target, challenger.getUUID(), challenger.getName().getString(), false, -1, true);

        ModLogger.logMinecraftInteraction("Pre-match screen opened", "challenger", challenger.getName().getString(), "target", target.getName().getString());
        BMFalkye.LOGGER.info("Pre-match screen opened for {} and {}", challenger.getName().getString(), target.getName().getString());
    }

    /**
     * Принимает вызов на дуэль (устаревший метод, теперь используется предматчевое меню)
     */
    public static void acceptChallenge(ServerPlayer player) {
        PendingChallenge challenge = pendingChallenges.get(player.getUUID());
        if (challenge == null) {
            player.sendSystemMessage(Component.translatable("message.bm_falkye.no_pending_challenge"));
            return;
        }

        ServerPlayer challenger = player.server.getPlayerList().getPlayer(challenge.getChallengerUUID());
        if (challenger == null || !challenger.isAlive()) {
            player.sendSystemMessage(Component.translatable("message.bm_falkye.challenger_offline"));
            pendingChallenges.remove(player.getUUID());
            return;
        }

        // Открываем предматчевое меню (используем дефолтную конфигурацию)
        MatchConfig defaultConfig = new MatchConfig();
        startPlayerMatch(player, challenger, defaultConfig);
        
        // Удаляем вызов
        pendingChallenges.remove(player.getUUID());
    }

    /**
     * Отклоняет вызов на дуэль
     */
    public static void denyChallenge(ServerPlayer player) {
        PendingChallenge challenge = pendingChallenges.remove(player.getUUID());
        if (challenge == null) {
            ModLogger.logGameEvent("Challenge deny failed: no pending challenge", "player", player.getName().getString());
            player.sendSystemMessage(Component.translatable("message.bm_falkye.no_pending_challenge"));
            return;
        }

        ServerPlayer challenger = player.server.getPlayerList().getPlayer(challenge.getChallengerUUID());
        ModLogger.logGameEvent("Challenge denied", "player", player.getName().getString(), "challenger", challenger != null ? challenger.getName().getString() : "unknown");
        
        if (challenger != null && challenger.isAlive()) {
            challenger.sendSystemMessage(Component.translatable("message.bm_falkye.challenge_denied", player.getName()));
        }

        player.sendSystemMessage(Component.translatable("message.bm_falkye.challenge_denied_self"));
        BMFalkye.LOGGER.info("Player {} denied challenge from {}", player.getName().getString(), 
            challenger != null ? challenger.getName().getString() : "unknown");
    }

    /**
     * Завершает игровую сессию
     */
    public static void endGame(FalkyeGameSession session) {
        // Удаляем данные восстановления для обоих игроков
        if (session.getPlayer1() != null) {
            com.bmfalkye.game.ReconnectManager.removeReconnectData(session.getPlayer1().getUUID());
        }
        if (session.getPlayer2() != null) {
            com.bmfalkye.game.ReconnectManager.removeReconnectData(session.getPlayer2().getUUID());
        }
        String player1Name = session.getPlayer1() != null ? session.getPlayer1().getName().getString() : "null";
        String player2Name = session.getPlayer2() != null ? session.getPlayer2().getName().getString() : (session.isPlayingWithVillager() ? "Villager" : "null");
        int roundsWon1 = session.getRoundsWon(session.getPlayer1());
        int roundsWon2 = session.isPlayingWithVillager() ? session.getRoundsWon(null) : (session.getPlayer2() != null ? session.getRoundsWon(session.getPlayer2()) : 0);
        
        ModLogger.logGameEvent("Game ending", 
            "player1", player1Name, 
            "player2", player2Name,
            "roundsWon1", roundsWon1,
            "roundsWon2", roundsWon2,
            "currentRound", session.getCurrentRound(),
            "isVillagerGame", session.isPlayingWithVillager());
        
        activeGames.remove(session.getPlayer1().getUUID());
        if (session.getPlayer2() != null) {
            activeGames.remove(session.getPlayer2().getUUID());
        }
        activeVillagerGames.remove(session.getPlayer1().getUUID());
        
        // Разблокируем ставки (на случай, если они не были разблокированы в GameEndHandler)
        // Это защита от эксплойтов - гарантируем, что ставки всегда разблокируются
        if (session.getPlayer1() != null) {
            com.bmfalkye.game.BetProtectionSystem.unlockBet(session.getPlayer1());
            // Сбрасываем статистику античита при окончании игры
            com.bmfalkye.anticheat.AntiCheatSystem.resetPlayerStats(session.getPlayer1().getUUID());
        }
        if (session.getPlayer2() != null) {
            com.bmfalkye.game.BetProtectionSystem.unlockBet(session.getPlayer2());
            // Сбрасываем статистику античита при окончании игры
            com.bmfalkye.anticheat.AntiCheatSystem.resetPlayerStats(session.getPlayer2().getUUID());
        }
        
        ModLogger.logGameEvent("Game session removed from active games", 
            "player1", player1Name,
            "player2", player2Name);
        
        // Очищаем память завершённой сессии
        com.bmfalkye.memory.MemoryOptimizer.cleanupFinishedSession(session);
        
        BMFalkye.LOGGER.info("Falkye game ended");
    }

    /**
     * Получает активную игровую сессию игрока
     */
    public static FalkyeGameSession getActiveGame(ServerPlayer player) {
        FalkyeGameSession game = activeGames.get(player.getUUID());
        if (game == null) {
            game = activeVillagerGames.get(player.getUUID());
        }
        return game;
    }
    
    /**
     * Начинает игру с деревенским жителем (автоматически принимает вызов)
     */
    public static void startGameWithVillager(ServerPlayer player, net.minecraft.world.entity.npc.Villager villager) {
        // Проверяем, не занят ли игрок
        if (activeGames.containsKey(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("message.bm_falkye.target_busy", Component.literal("Вы")));
            return;
        }
        
        // Создаём игровую сессию с villager (используется только для обратной совместимости)
        com.bmfalkye.game.MatchConfig defaultConfig = new com.bmfalkye.game.MatchConfig();
        FalkyeGameSession session = com.bmfalkye.game.VillagerAIPlayer.createGameWithVillager(player, (net.minecraft.world.entity.LivingEntity) villager, defaultConfig);
        if (session == null) {
            player.sendSystemMessage(Component.translatable("message.bm_falkye.villager_game_failed"));
            return;
        }
        
        activeGames.put(player.getUUID(), session);
        
        // Открываем игровое поле для игрока
        NetworkHandler.openGameScreen(player, session);
        
        // Показываем подсказку о первой игре (для новых игроков)
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            com.bmfalkye.tutorial.TutorialSystem.showFirstGameHint(player, serverLevel);
        }
        
        // Отправляем сообщение
        player.sendSystemMessage(Component.translatable("message.bm_falkye.villager_game_started", villager.getName()));
        
        BMFalkye.LOGGER.info("Falkye game started between {} and villager {}", player.getName().getString(), villager.getName().getString());
        
        // Если ход villager, делаем его ход в следующем тике сервера
        if (session.isVillagerTurn() && player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.getServer().execute(() -> {
                com.bmfalkye.game.VillagerAIPlayer.makeAITurn(session);
                com.bmfalkye.network.NetworkHandler.updateGameState(player, session);
            });
        }
    }

    /**
     * Начинает матч с конфигурацией (вызывается после предматчевого меню)
     */
    public static void startMatchWithConfig(ServerPlayer player, UUID opponentUUID, MatchConfig config) {
        // Валидация входных данных
        if (!com.bmfalkye.util.InputValidator.isPlayerValid(player)) {
            ModLogger.warn("Invalid player in startMatchWithConfig", "player", player != null ? player.getName().getString() : "null");
            return;
        }
        
        if (!com.bmfalkye.util.InputValidator.isValidPlayerUUID(opponentUUID)) {
            player.sendSystemMessage(Component.literal("§cНеверный UUID противника"));
            return;
        }
        
        if (config == null) {
            ModLogger.warn("Config is null in startMatchWithConfig");
            return;
        }
        
        // Проверяем и блокируем ставку (если есть) с защитой от эксплойтов
        if (config.getBetAmount() > 0) {
            if (!com.bmfalkye.util.InputValidator.isValidBet(config.getBetAmount())) {
                player.sendSystemMessage(Component.literal("§cНеверная сумма ставки! Минимум: " + 
                    com.bmfalkye.game.BetProtectionSystem.getMinBetAmount() + 
                    ", Максимум: " + com.bmfalkye.game.BetProtectionSystem.getMaxBetAmount()));
                return;
            }
            
            // Проверяем, может ли игрок сделать ставку
            if (!com.bmfalkye.game.BetProtectionSystem.canPlayerBet(player)) {
                player.sendSystemMessage(Component.literal("§cВы не можете сделать ставку сейчас!"));
                return;
            }
            
            // Блокируем ставку перед началом матча
            if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
                if (!com.bmfalkye.game.BettingSystem.lockBet(player, config.getBetAmount(), level)) {
                    return; // Недостаточно монет или другая ошибка
                }
            }
        }
        
        // Проверяем, не занят ли игрок
        if (activeGames.containsKey(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("message.bm_falkye.already_in_game"));
            return;
        }
        
        // Ищем противника
        ServerPlayer opponentPlayer = player.server.getPlayerList().getPlayer(opponentUUID);
        net.minecraft.world.entity.LivingEntity npcEntity = null;
        
        if (opponentPlayer != null) {
            // Игра с другим игроком
            startPlayerMatch(player, opponentPlayer, config);
        } else {
            // Ищем NPC по UUID
            npcEntity = findNPCByUUID(player, opponentUUID);
            if (npcEntity != null) {
                if (npcEntity instanceof net.minecraft.world.entity.npc.Villager villager) {
                    // Проверяем, что это не детский житель
                    if (villager.isBaby()) {
                        player.sendSystemMessage(Component.translatable("message.bm_falkye.villager_is_baby"));
                        return;
                    }
                    
                    // Проверяем, является ли это сюжетным NPC
                    com.bmfalkye.npc.StoryNPC storyNPC = com.bmfalkye.npc.StoryNPCRegistry.findNPCByVillager(villager);
                    if (storyNPC != null) {
                        com.bmfalkye.game.StoryDuelManager.startStoryDuelWithConfig(player, villager, config);
                    } else {
                        startVillagerMatch(player, villager, config);
                    }
                }
            } else {
                player.sendSystemMessage(Component.translatable("message.bm_falkye.opponent_not_found"));
            }
        }
    }
    
    /**
     * Начинает матч с другим игроком (публичный метод для использования из NetworkHandler)
     */
    public static void startPlayerMatch(ServerPlayer player, ServerPlayer opponent, MatchConfig config) {
        ModLogger.logGameLogic("Starting player match", "player1", player.getName().getString(), "player2", opponent.getName().getString(), "bet", config.getBetAmount(), "difficulty", config.getDifficulty());
        
        // Блокируем ставку для обоих игроков перед началом матча (с защитой от эксплойтов)
        if (config.getBetAmount() > 0 && player.level() instanceof net.minecraft.server.level.ServerLevel level) {
            // Проверяем, может ли игрок сделать ставку
            if (!com.bmfalkye.game.BetProtectionSystem.canPlayerBet(player)) {
                player.sendSystemMessage(Component.literal("§cВы не можете сделать ставку сейчас!"));
                return;
            }
            
            if (!com.bmfalkye.game.BettingSystem.lockBet(player, config.getBetAmount(), level)) {
                player.sendSystemMessage(Component.literal("§cНедостаточно монет для ставки!"));
                return;
            }
            
            // Проверяем, может ли противник сделать ставку
            if (!com.bmfalkye.game.BetProtectionSystem.canPlayerBet(opponent)) {
                // Возвращаем ставку первому игроку
                com.bmfalkye.storage.PlayerCurrency currency = com.bmfalkye.storage.PlayerCurrency.get(level);
                currency.addCoins(player, config.getBetAmount());
                com.bmfalkye.game.BetProtectionSystem.unlockBet(player);
                player.sendSystemMessage(Component.literal("§cПротивник не может сделать ставку сейчас!"));
                return;
            }
            
            if (!com.bmfalkye.game.BettingSystem.lockBet(opponent, config.getBetAmount(), level)) {
                // Возвращаем ставку первому игроку
                com.bmfalkye.storage.PlayerCurrency currency = com.bmfalkye.storage.PlayerCurrency.get(level);
                currency.addCoins(player, config.getBetAmount());
                com.bmfalkye.game.BetProtectionSystem.unlockBet(player);
                player.sendSystemMessage(Component.literal("§cУ противника недостаточно монет для ставки!"));
                return;
            }
        }
        
        // Загружаем колоды игроков
        com.bmfalkye.storage.PlayerDeckStorage storage = 
            com.bmfalkye.storage.PlayerDeckStorage.get((net.minecraft.server.level.ServerLevel) player.level());
        com.bmfalkye.storage.PlayerDeckManager deckManager = 
            com.bmfalkye.storage.PlayerDeckManager.get((net.minecraft.server.level.ServerLevel) player.level());
        
        com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData deckData1 = null;
        com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData deckData2 = null;
        
        // Если выбрана колода для игрока 1, загружаем её из менеджера колод
        if (config.getSelectedDeckName() != null && !config.getSelectedDeckName().isEmpty()) {
            java.util.List<com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData> playerDecks = deckManager.getPlayerDecks(player);
            for (com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData deck : playerDecks) {
                if (deck.getDeckName().equals(config.getSelectedDeckName())) {
                    deckData1 = deck;
                    break;
                }
            }
        }
        
        // Если колода не найдена или не выбрана, используем сохранённую колоду
        if (deckData1 == null) {
            deckData1 = storage.getPlayerDeck(player);
        }
        
        // Для игрока 2 пока используем сохранённую колоду (можно добавить выбор позже)
        deckData2 = storage.getPlayerDeck(opponent);
        
        // Проверяем наличие колод
        if (deckData1 == null) {
            java.util.Map<String, Object> context = com.bmfalkye.util.ErrorContextCollector.collectPlayerContext(player);
            context.put("operation", "startPlayerMatch");
            context.put("issue", "player1DeckNull");
            ModLogger.error("Player1 deck data is null", null, context);
            player.sendSystemMessage(Component.translatable("message.bm_falkye.deck_not_found"));
            return;
        }
        if (deckData2 == null) {
            java.util.Map<String, Object> context = com.bmfalkye.util.ErrorContextCollector.collectPlayerContext(opponent);
            context.put("operation", "startPlayerMatch");
            context.put("issue", "player2DeckNull");
            ModLogger.error("Player2 deck data is null", null, context);
            opponent.sendSystemMessage(Component.translatable("message.bm_falkye.deck_not_found"));
            return;
        }
        
        ModLogger.logGameLogic("Player decks loaded", "player1DeckSize", deckData1.getCardIds().size(), "player2DeckSize", deckData2.getCardIds().size());
        
        com.bmfalkye.cards.CardDeck deck1 = deckData1.createDeck(player, (net.minecraft.server.level.ServerLevel) player.level());
        com.bmfalkye.cards.CardDeck deck2 = deckData2.createDeck(opponent, (net.minecraft.server.level.ServerLevel) opponent.level());
        
        // Проверяем, что колоды созданы успешно
        if (deck1 == null || deck2 == null) {
            java.util.Map<String, Object> context1 = com.bmfalkye.util.ErrorContextCollector.collectPlayerContext(player);
            java.util.Map<String, Object> context2 = com.bmfalkye.util.ErrorContextCollector.collectPlayerContext(opponent);
            java.util.Map<String, Object> context = com.bmfalkye.util.ErrorContextCollector.mergeContexts(context1, context2);
            context.put("operation", "startPlayerMatch");
            context.put("issue", "failedToCreateDecks");
            context.put("deck1Exists", deck1 != null);
            context.put("deck2Exists", deck2 != null);
            ModLogger.error("Failed to create decks", null, context);
            player.sendSystemMessage(Component.translatable("message.bm_falkye.deck_creation_failed"));
            return;
        }
        
        com.bmfalkye.cards.LeaderCard leader1 = config.getSelectedLeader() != null ? 
            config.getSelectedLeader() : deckData1.getLeader();
        com.bmfalkye.cards.LeaderCard leader2 = deckData2.getLeader();
        
        if (leader1 == null) {
            leader1 = com.bmfalkye.cards.LeaderRegistry.getLeader("leader_fire_architect");
        }
        if (leader2 == null) {
            leader2 = com.bmfalkye.cards.LeaderRegistry.getLeader("leader_fire_architect");
        }
        
        ModLogger.logGameLogic("Leaders assigned", "leader1", leader1 != null ? leader1.getName() : "null", "leader2", leader2 != null ? leader2.getName() : "null");
        
        // Создаём игровую сессию через фабрику
        // player = target (тот, кому бросили вызов) -> player1 в сессии
        // opponent = challenger (тот, кто бросил вызов) -> player2 в сессии
        // Первый ход будет у player2 (того, кому бросили вызов) согласно логике в конструкторе
        FalkyeGameSession session = com.bmfalkye.game.GameSessionFactory.createPlayerMatch(
            player, opponent, deck1, deck2, leader1, leader2);
        session.setMatchConfig(config); // Сохраняем конфигурацию
        
        activeGames.put(player.getUUID(), session);
        activeGames.put(opponent.getUUID(), session);
        
        ModLogger.logGameLogic("Game session created and registered", "sessionId", player.getUUID());
        
        // Открываем игровое поле для обоих игроков
        NetworkHandler.openGameScreen(player, session);
        NetworkHandler.openGameScreen(opponent, session);
        
        // Показываем подсказку о первой игре (для новых игроков)
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            com.bmfalkye.tutorial.TutorialSystem.showFirstGameHint(player, serverLevel);
        }
        if (opponent.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            com.bmfalkye.tutorial.TutorialSystem.showFirstGameHint(opponent, serverLevel);
        }
        
        ModLogger.logMinecraftInteraction("Game screens opened", "player1", player.getName().getString(), "player2", opponent.getName().getString());
        
        BMFalkye.LOGGER.info("Falkye match started between {} and {} with bet: {}", 
            player.getName().getString(), opponent.getName().getString(), config.getBetAmount());
        
        // Проверяем и запускаем случайные игровые события при начале игры
        // Проверяем события для первого игрока
        com.bmfalkye.game.GameEventSystem.checkAndTriggerEvents(session, player);
        // Проверяем события для второго игрока
        com.bmfalkye.game.GameEventSystem.checkAndTriggerEvents(session, opponent);
    }
    
    /**
     * Начинает матч с деревенским жителем
     */
    static void startVillagerMatch(ServerPlayer player, net.minecraft.world.entity.npc.Villager villager, MatchConfig config) {
        ModLogger.logGameLogic("Starting villager match", "player", player.getName().getString(), "villager", villager.getName().getString(), "bet", config.getBetAmount(), "difficulty", config.getDifficulty());
        
        // Проверяем, что это не детский житель
        if (villager.isBaby()) {
            ModLogger.warn("Cannot start match: villager is baby");
            player.sendSystemMessage(Component.translatable("message.bm_falkye.villager_is_baby"));
            return;
        }
        
        // Проверяем баланс игрока
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            com.bmfalkye.storage.PlayerCurrency playerCurrency = com.bmfalkye.storage.PlayerCurrency.get(serverLevel);
            int betAmount = config.getBetAmount();
            
            if (!playerCurrency.hasEnoughCoins(player, betAmount)) {
                ModLogger.warn("Insufficient player coins for match", "player", player.getName().getString(), "required", betAmount);
                player.sendSystemMessage(Component.literal("§cУ вас недостаточно монет для ставки! Нужно: " + betAmount));
                return;
            }
            
            // Проверяем баланс жителя
            com.bmfalkye.storage.VillagerCurrency villagerCurrency = com.bmfalkye.storage.VillagerCurrency.get(serverLevel);
            int villagerCoins = villagerCurrency.getCoins((net.minecraft.world.entity.LivingEntity) villager);
            if (!villagerCurrency.hasEnoughCoins((net.minecraft.world.entity.LivingEntity) villager, betAmount)) {
                ModLogger.warn("Insufficient villager coins for match", "villager", villager.getName().getString(), "required", betAmount, "has", villagerCoins);
                player.sendSystemMessage(Component.literal("§cУ жителя недостаточно монет для ставки! У него: " + villagerCoins));
                return;
            }
        }
        
        // Проверяем, является ли это сюжетным NPC
        com.bmfalkye.npc.StoryNPC storyNPC = com.bmfalkye.npc.StoryNPCRegistry.findNPCByVillager(villager);
        if (storyNPC != null) {
            ModLogger.logGameLogic("Starting story duel", "player", player.getName().getString(), "storyNPC", storyNPC.getName());
            com.bmfalkye.game.StoryDuelManager.startStoryDuelWithConfig(player, villager, config);
            return;
        }
        
        // Обычный villager - создаём игровую сессию
        ModLogger.logGameLogic("Creating villager game session");
        FalkyeGameSession session = com.bmfalkye.game.VillagerAIPlayer.createGameWithVillager(
            player, (net.minecraft.world.entity.LivingEntity) villager, config);
        if (session == null) {
            ModLogger.error("Failed to create villager game session");
            player.sendSystemMessage(Component.translatable("message.bm_falkye.villager_game_failed"));
            return;
        }
        
        activeGames.put(player.getUUID(), session);
        activeVillagerGames.put(player.getUUID(), session);
        ModLogger.logGameLogic("Villager game session created and registered", "sessionId", player.getUUID());
        
        // Открываем игровое поле
        NetworkHandler.openGameScreen(player, session);
        ModLogger.logMinecraftInteraction("Game screen opened for villager match", "player", player.getName().getString());
        
        player.sendSystemMessage(Component.translatable("message.bm_falkye.villager_game_started", villager.getName()));
        
        // Если ход villager, делаем его ход
        if (session.isVillagerTurn() && player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            ModLogger.logGameLogic("Villager starts first, scheduling AI turn");
            serverLevel.getServer().execute(() -> {
                com.bmfalkye.game.VillagerAIPlayer.makeAITurn(session);
                NetworkHandler.updateGameState(player, session);
            });
        }
    }
    
    /**
     * Начинает матч с боссом
     */
    public static void startBossMatch(ServerPlayer player, net.minecraft.world.entity.npc.Villager bossNPC, 
                                     com.bmfalkye.npc.BossSystem.Boss boss, MatchConfig config) {
        // Проверяем, не занят ли игрок
        if (activeGames.containsKey(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("message.bm_falkye.target_busy", Component.literal("Вы")));
            return;
        }
        
        if (config == null) {
            config = new MatchConfig();
        }
        
        // Загружаем колоду игрока
        com.bmfalkye.storage.PlayerDeckStorage storage = 
            com.bmfalkye.storage.PlayerDeckStorage.get((net.minecraft.server.level.ServerLevel) player.level());
        com.bmfalkye.storage.PlayerDeckManager deckManager = 
            com.bmfalkye.storage.PlayerDeckManager.get((net.minecraft.server.level.ServerLevel) player.level());
        
        com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData playerDeckData = null;
        
        // Если выбрана колода, загружаем её
        if (config.getSelectedDeckName() != null && !config.getSelectedDeckName().isEmpty()) {
            java.util.List<com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData> playerDecks = deckManager.getPlayerDecks(player);
            for (com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData deck : playerDecks) {
                if (deck.getDeckName().equals(config.getSelectedDeckName())) {
                    playerDeckData = deck;
                    break;
                }
            }
        }
        
        if (playerDeckData == null) {
            playerDeckData = storage.getPlayerDeck(player);
        }
        
        CardDeck playerDeck = playerDeckData.createDeck(player, (net.minecraft.server.level.ServerLevel) player.level());
        LeaderCard playerLeader = config.getSelectedLeader() != null ? 
            config.getSelectedLeader() : playerDeckData.getLeader();
        
        if (playerLeader == null) {
            playerLeader = LeaderRegistry.getLeader("leader_fire_architect");
        }
        
        // Используем колоду и лидера босса
        CardDeck bossDeck = boss.getDeck();
        LeaderCard bossLeader = boss.getLeader();
        
        // Создаём игровую сессию
        FalkyeGameSession session = new FalkyeGameSession(
            player, null, playerDeck, bossDeck, playerLeader, bossLeader);
        session.setVillagerOpponent((net.minecraft.world.entity.LivingEntity) bossNPC);
        session.setMatchConfig(config);
        
        activeGames.put(player.getUUID(), session);
        activeVillagerGames.put(player.getUUID(), session);
        
        // Открываем игровое поле
        NetworkHandler.openGameScreen(player, session);
        
        player.sendSystemMessage(Component.literal("§aБой с боссом " + boss.getName() + " начался!"));
        
        // Если ход босса, делаем его ход
        if (session.isVillagerTurn() && player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.getServer().execute(() -> {
                com.bmfalkye.game.VillagerAIPlayer.makeAITurn(session);
                NetworkHandler.updateGameState(player, session);
            });
        }
    }
    
    /**
     * Находит NPC по UUID в мире игрока
     */
    private static net.minecraft.world.entity.LivingEntity findNPCByUUID(ServerPlayer player, UUID npcUUID) {
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            net.minecraft.world.entity.Entity entity = serverLevel.getEntity(npcUUID);
            if (entity instanceof net.minecraft.world.entity.LivingEntity livingEntity) {
                return livingEntity;
            }
        }
        return null;
    }
    
    /**
     * Получает карту ожидающих вызовов (для использования в NetworkHandler)
     */
    public static Map<UUID, PendingChallenge> getPendingChallenges() {
        return pendingChallenges;
    }
    
    /**
     * Класс для хранения информации о вызове
     */
    public static class PendingChallenge {
        private final UUID challengerUUID;
        private final UUID targetUUID;
        
        public PendingChallenge(UUID challengerUUID, UUID targetUUID) {
            this.challengerUUID = challengerUUID;
            this.targetUUID = targetUUID;
        }
        
        public UUID getChallengerUUID() {
            return challengerUUID;
        }
        
        public UUID getTargetUUID() {
            return targetUUID;
        }
    }
}

