package com.bmfalkye.game;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardDeck;
import com.bmfalkye.cards.LeaderCard;
import com.bmfalkye.cards.LeaderRegistry;
import com.bmfalkye.util.ModLogger;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Random;

/**
 * AI игрок для деревенских жителей
 */
public class VillagerAIPlayer {
    private static final Random RANDOM = new Random();
    
    /**
     * Создаёт игровую сессию с villager или NPC как вторым игроком
     */
    public static FalkyeGameSession createGameWithVillager(ServerPlayer player, net.minecraft.world.entity.LivingEntity entity, com.bmfalkye.game.MatchConfig config) {
        if (player == null || entity == null) {
            return null;
        }
        
        if (config == null) {
            config = new com.bmfalkye.game.MatchConfig(); // Дефолтная конфигурация
        }
        // Загружаем колоду игрока
        com.bmfalkye.storage.PlayerDeckStorage storage = 
            com.bmfalkye.storage.PlayerDeckStorage.get((net.minecraft.server.level.ServerLevel) player.level());
        com.bmfalkye.storage.PlayerDeckManager deckManager = 
            com.bmfalkye.storage.PlayerDeckManager.get((net.minecraft.server.level.ServerLevel) player.level());
        
        com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData playerDeckData = null;
        
        // Если выбрана колода, загружаем её из менеджера колод
        if (config.getSelectedDeckName() != null && !config.getSelectedDeckName().isEmpty()) {
            java.util.List<com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData> playerDecks = deckManager.getPlayerDecks(player);
            for (com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData deck : playerDecks) {
                if (deck.getDeckName().equals(config.getSelectedDeckName())) {
                    playerDeckData = deck;
                    break;
                }
            }
        }
        
        // Если колода не найдена или не выбрана, используем сохранённую колоду
        if (playerDeckData == null) {
            playerDeckData = storage.getPlayerDeck(player);
        }
        
        CardDeck playerDeck = playerDeckData.createDeck(player, (net.minecraft.server.level.ServerLevel) player.level());
        LeaderCard playerLeader = config.getSelectedLeader() != null ? 
            config.getSelectedLeader() : playerDeckData.getLeader();
        
        if (playerLeader == null) {
            playerLeader = LeaderRegistry.getLeader("leader_fire_architect");
        }
        
        // Создаём колоду для villager (базовая колода)
        CardDeck villagerDeck = createVillagerDeck();
        LeaderCard villagerLeader = LeaderRegistry.getLeader("leader_fire_architect");
        
        // Создаём сессию
        FalkyeGameSession session = com.bmfalkye.game.GameSessionFactory.createVillagerMatch(
            player, entity, playerDeck, villagerDeck, playerLeader, villagerLeader);
        
        // Сохраняем entity в сессии для AI
        session.setVillagerOpponent(entity);
        session.setMatchConfig(config); // Сохраняем конфигурацию
        
        return session;
    }
    
    /**
     * Создаёт базовую колоду для villager
     */
    private static CardDeck createVillagerDeck() {
        CardDeck deck = new CardDeck();
        
        // Добавляем разнообразные карты
        String[] cardIds = {
            "fire_mage", "pyro_phoenix", "watcher_scholar", "void_walker",
            "nature_guardian", "ancient_tree", "ruin_echo", "corrupted_mage",
            "dwarf_engineer", "academy_scholar", "fire_drake", "frost_drake",
            "storm_drake", "light_elf", "grove_keeper"
        };
        
        // Добавляем по 1 копии каждой карты (правило: 1 карта одного вида на игрока)
        for (String cardId : cardIds) {
            Card card = com.bmfalkye.cards.CardRegistry.getCard(cardId);
            if (card != null) {
                deck.addCard(card);
            }
        }
        
        // Добавляем несколько заклинаний
        String[] spellIds = {"flame_storm", "nature_heal", "time_freeze"};
        for (String spellId : spellIds) {
            Card spell = com.bmfalkye.cards.CardRegistry.getCard(spellId);
            if (spell != null) {
                deck.addCard(spell);
            }
        }
        
        deck.shuffle();
        return deck;
    }
    
    /**
     * AI делает ход - значительно улучшенная версия с продвинутой стратегией для каждого уровня сложности
     */
    public static void makeAITurn(FalkyeGameSession session) {
        com.bmfalkye.util.ModLogger.logAIAction("AI turn started", 
            "player", session.getPlayer1() != null ? session.getPlayer1().getName().getString() : "null",
            "round", session.getCurrentRound(),
            "player1Score", session.getRoundScore(session.getPlayer1()),
            "player2Score", session.getRoundScore(null));
        
        if (session.isGameEnded() || session.isRoundEnded()) {
            com.bmfalkye.util.ModLogger.logAIAction("AI turn cancelled: game/round ended", 
                "gameEnded", session.isGameEnded(),
                "roundEnded", session.isRoundEnded());
            return;
        }
        
        ServerPlayer humanPlayer = session.getPlayer1();
        net.minecraft.world.entity.LivingEntity opponent = session.getVillagerOpponent();
        
        if (opponent == null || humanPlayer == null) {
            return;
        }
        
        // Проверяем, чей сейчас ход
        if (!session.isVillagerTurn()) {
            return;
        }
        
        com.bmfalkye.game.MatchConfig config = session.getMatchConfig();
        MatchConfig.Difficulty difficulty = config != null ? config.getDifficulty() : MatchConfig.Difficulty.NORMAL;
        
        // Анализ текущей ситуации
        int aiScore = session.getRoundScore(null);
        int humanScore = session.getRoundScore(humanPlayer);
        int scoreDifference = aiScore - humanScore;
        int currentRound = session.getCurrentRound();
        int roundsWonAI = session.getRoundsWon(null);
        int roundsWonHuman = session.getRoundsWon(humanPlayer);
        
        List<Card> hand = session.getHand(null);
        
        // Анализ поля
        List<Card> aiMelee = session.getMeleeRow(null);
        List<Card> aiRanged = session.getRangedRow(null);
        List<Card> aiSiege = session.getSiegeRow(null);
        List<Card> humanMelee = session.getMeleeRow(humanPlayer);
        List<Card> humanRanged = session.getRangedRow(humanPlayer);
        List<Card> humanSiege = session.getSiegeRow(humanPlayer);
        
        int aiMeleeCount = aiMelee.size();
        int aiRangedCount = aiRanged.size();
        int aiSiegeCount = aiSiege.size();
        int humanMeleeCount = humanMelee.size();
        int humanRangedCount = humanRanged.size();
        int humanSiegeCount = humanSiege.size();
        
        // Анализ силы рядов
        int aiMeleePower = calculateRowPower(session, aiMelee);
        int aiRangedPower = calculateRowPower(session, aiRanged);
        int aiSiegePower = calculateRowPower(session, aiSiege);
        int humanMeleePower = calculateRowPower(session, humanMelee);
        int humanRangedPower = calculateRowPower(session, humanRanged);
        int humanSiegePower = calculateRowPower(session, humanSiege);
        
        // Анализ погоды
        FalkyeGameSession.WeatherType weather = session.getWeather();
        boolean weatherAffectsMelee = weather == FalkyeGameSession.WeatherType.FROST;
        boolean weatherAffectsRanged = weather == FalkyeGameSession.WeatherType.FOG;
        boolean weatherAffectsSiege = weather == FalkyeGameSession.WeatherType.RAIN;
        
        // Определяем стратегию для этого хода (добавляет непредсказуемость)
        com.bmfalkye.ai.AIStrategy.StrategyType currentStrategy = 
            com.bmfalkye.ai.AIStrategy.determineStrategy(difficulty, session, scoreDifference, 
                currentRound, roundsWonAI, roundsWonHuman);
        
        com.bmfalkye.util.ModLogger.logAIAction("AI strategy determined", 
            "strategy", currentStrategy.toString(),
            "difficulty", difficulty.toString(),
            "scoreDifference", scoreDifference);
        
        // Проигрываем звук реакции
        playVillagerReactionSound(opponent, scoreDifference, hand.isEmpty());
        
        // ========== ЛОГИКА ИСПОЛЬЗОВАНИЯ ЛИДЕРА ==========
        LeaderCard leader = session.getLeader(null);
        if (leader != null && !leader.isUsed()) {
            boolean shouldUseLeader = shouldUseLeader(difficulty, scoreDifference, 
                roundsWonAI, roundsWonHuman, currentRound, hand.size());
            
            if (shouldUseLeader) {
                session.useLeader(null);
                com.bmfalkye.network.NetworkHandler.updateGameState(humanPlayer, session);
                return;
            }
        }
        
        /**
         * ========== ЛОГИКА ИГРЫ КАРТЫ С УЧЁТОМ НОВОЙ МЕХАНИКИ ==========
         * 
         * Новая механика хода:
         * 1. Игрок может сыграть ОДНУ обычную карту (CREATURE) за ход
         * 2. После обычной карты, если в руке есть карта способности (SPELL/SPECIAL),
         *    игрок может сыграть её ИЛИ спасовать
         * 3. Если игрок сыграл картой способности, ход переключается
         * 4. Если игрок спасовал, ход переключается
         * 
         * Для AI это означает:
         * - Сначала играем обычную карту (если есть)
         * - Затем решаем, играть ли карту способности или пасовать
         * - Решение зависит от стратегии, сложности и текущей ситуации
         */
        boolean playedNormalCard = session.hasPlayedNormalCard(null);
        boolean playedAbilityCard = session.hasPlayedAbilityCard(null);
        boolean hasAbilityCard = hasAbilityCardInHand(session, null);
        
        // Если уже сыграл обычную карту и есть карта способности, решаем - играть её или пасовать
        if (playedNormalCard && hasAbilityCard && !playedAbilityCard) {
            boolean shouldPlayAbilityCard = shouldPlayAbilityCard(difficulty, scoreDifference, 
                roundsWonAI, roundsWonHuman, currentRound, hand);
            
            /**
             * Учитываем стратегию для карт способностей:
             * - Агрессивные стратегии (AGGRESSIVE, RUSH) играют карты способностей чаще
             *   (добавляем 30% шанс даже если базовая логика не рекомендует)
             * - Защитные стратегии (DEFENSIVE) играют карты способностей реже
             *   (только 70% шанс даже если базовая логика рекомендует)
             * - Это добавляет разнообразие и делает AI менее предсказуемым
             */
            if (currentStrategy == com.bmfalkye.ai.AIStrategy.StrategyType.AGGRESSIVE || 
                currentStrategy == com.bmfalkye.ai.AIStrategy.StrategyType.RUSH) {
                shouldPlayAbilityCard = shouldPlayAbilityCard || RANDOM.nextDouble() < 0.3;
            } else if (currentStrategy == com.bmfalkye.ai.AIStrategy.StrategyType.DEFENSIVE) {
                shouldPlayAbilityCard = shouldPlayAbilityCard && RANDOM.nextDouble() < 0.7;
            }
            
            if (shouldPlayAbilityCard) {
                // Выбираем карту способности
                Card abilityCard = selectAbilityCard(difficulty, hand, scoreDifference,
                    aiMeleePower, aiRangedPower, aiSiegePower,
                    humanMeleePower, humanRangedPower, humanSiegePower,
                    weatherAffectsMelee, weatherAffectsRanged, weatherAffectsSiege);
                
                if (abilityCard != null) {
                    // Выбираем ряд для карты способности с учётом стратегии
                    FalkyeGameSession.CardRow row = com.bmfalkye.ai.AIStrategy.applyStrategyForRow(
                        currentStrategy, abilityCard,
                        aiMeleeCount, aiRangedCount, aiSiegeCount,
                        humanMeleeCount, humanRangedCount, humanSiegeCount,
                        aiMeleePower, aiRangedPower, aiSiegePower,
                        humanMeleePower, humanRangedPower, humanSiegePower,
                        weatherAffectsMelee, weatherAffectsRanged, weatherAffectsSiege);
                    
                    // Если стратегия не вернула ряд, используем старый метод
                    if (row == null) {
                        row = selectRow(difficulty, abilityCard,
                            aiMeleeCount, aiRangedCount, aiSiegeCount,
                            humanMeleeCount, humanRangedCount, humanSiegeCount,
                            aiMeleePower, aiRangedPower, aiSiegePower,
                            humanMeleePower, humanRangedPower, humanSiegePower,
                            weatherAffectsMelee, weatherAffectsRanged, weatherAffectsSiege);
                    }
                    
                    // Играем карту способности
                    session.playCardForVillager(abilityCard, row);
                    playVillagerReactionSound(opponent, scoreDifference, false);
                    com.bmfalkye.network.NetworkHandler.updateGameState(humanPlayer, session);
                    return;
                }
            }
            
            // Решили не играть картой способности - пасуем (отказываемся от карты способности)
            com.bmfalkye.util.ModLogger.logAIAction("AI passing (declining ability card)", 
                "round", session.getCurrentRound(),
                "player1Score", session.getRoundScore(humanPlayer),
                "player2Score", session.getRoundScore(null));
            
            session.passVillager();
            com.bmfalkye.network.NetworkHandler.updateGameState(humanPlayer, session);
            return;
        }
        
        // Если уже сыграл обе карты (обычную и способности), пасуем
        if (playedNormalCard && (playedAbilityCard || !hasAbilityCard)) {
            com.bmfalkye.util.ModLogger.logAIAction("AI passing (already played cards)", 
                "round", session.getCurrentRound(),
                "player1Score", session.getRoundScore(humanPlayer),
                "player2Score", session.getRoundScore(null));
            
            session.passVillager();
            com.bmfalkye.network.NetworkHandler.updateGameState(humanPlayer, session);
            return;
        }
        
        // Играем обычную карту (если ещё не сыграли)
        // Используем стратегию для определения, нужно ли играть
        boolean shouldPlayCard = com.bmfalkye.ai.AIStrategy.shouldPlayCardWithStrategy(
            currentStrategy, scoreDifference, currentRound, roundsWonAI, roundsWonHuman, hand.size());
        
        // Также учитываем базовую логику сложности
        boolean baseShouldPlay = shouldPlayCard(difficulty, scoreDifference, 
            roundsWonAI, roundsWonHuman, currentRound, hand.size());
        
        // Объединяем решения (стратегия имеет приоритет на HARD/EXPERT)
        if (difficulty == MatchConfig.Difficulty.HARD || difficulty == MatchConfig.Difficulty.EXPERT) {
            shouldPlayCard = shouldPlayCard || (baseShouldPlay && RANDOM.nextDouble() < 0.3);
        } else {
            shouldPlayCard = shouldPlayCard && baseShouldPlay;
        }
        
        if (shouldPlayCard && !hand.isEmpty()) {
            // Выбираем обычную карту с учётом стратегии
            List<Card> normalCards = new java.util.ArrayList<>();
            for (Card card : hand) {
                if (card.getType() == Card.CardType.CREATURE) {
                    normalCards.add(card);
                }
            }
            
            Card cardToPlay = null;
            if (!normalCards.isEmpty()) {
                // Используем стратегию для выбора карты
                cardToPlay = com.bmfalkye.ai.AIStrategy.applyStrategyForCard(currentStrategy, normalCards,
                    scoreDifference, currentRound,
                    aiMeleePower, aiRangedPower, aiSiegePower,
                    humanMeleePower, humanRangedPower, humanSiegePower);
                
                // Если стратегия не вернула карту, используем старый метод
                if (cardToPlay == null) {
                    cardToPlay = selectNormalCard(difficulty, normalCards, scoreDifference, 
                        aiMeleePower, aiRangedPower, aiSiegePower,
                        humanMeleePower, humanRangedPower, humanSiegePower,
                        weatherAffectsMelee, weatherAffectsRanged, weatherAffectsSiege);
                }
            }
            
            if (cardToPlay != null) {
                // Выбираем ряд для карты с учётом стратегии
                FalkyeGameSession.CardRow row = com.bmfalkye.ai.AIStrategy.applyStrategyForRow(
                    currentStrategy, cardToPlay,
                    aiMeleeCount, aiRangedCount, aiSiegeCount,
                    humanMeleeCount, humanRangedCount, humanSiegeCount,
                    aiMeleePower, aiRangedPower, aiSiegePower,
                    humanMeleePower, humanRangedPower, humanSiegePower,
                    weatherAffectsMelee, weatherAffectsRanged, weatherAffectsSiege);
                
                // Если стратегия не вернула ряд, используем старый метод
                if (row == null) {
                    row = selectRow(difficulty, cardToPlay,
                        aiMeleeCount, aiRangedCount, aiSiegeCount,
                        humanMeleeCount, humanRangedCount, humanSiegeCount,
                        aiMeleePower, aiRangedPower, aiSiegePower,
                        humanMeleePower, humanRangedPower, humanSiegePower,
                        weatherAffectsMelee, weatherAffectsRanged, weatherAffectsSiege);
                }
                
                // Играем карту
                session.playCardForVillager(cardToPlay, row);
                playVillagerReactionSound(opponent, scoreDifference, false);
                com.bmfalkye.network.NetworkHandler.updateGameState(humanPlayer, session);
                
                // Если после хода villager снова его ход, делаем еще один ход
                if (session.isVillagerTurn() && !session.isRoundEnded() && !session.isGameEnded()) {
                    if (humanPlayer.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        serverLevel.getServer().execute(() -> {
                            makeAITurn(session);
                            com.bmfalkye.network.NetworkHandler.updateGameState(humanPlayer, session);
                        });
                    }
                }
                return;
            }
        }
        
        // Пасуем (не играем карту)
        com.bmfalkye.util.ModLogger.logAIAction("AI passing", 
            "round", session.getCurrentRound(),
            "player1Score", session.getRoundScore(humanPlayer),
            "player2Score", session.getRoundScore(null));
        
        session.passVillager();
        com.bmfalkye.network.NetworkHandler.updateGameState(humanPlayer, session);
    }
    
    /**
     * Определяет, нужно ли использовать лидера
     */
    private static boolean shouldUseLeader(MatchConfig.Difficulty difficulty, int scoreDifference,
                                          int roundsWonAI, int roundsWonHuman, int currentRound, int handSize) {
        switch (difficulty) {
            case EASY:
                // EASY: Использует лидера только в критических ситуациях (5% шанс)
                return RANDOM.nextDouble() < 0.05 && scoreDifference < -10;
                
            case NORMAL:
                // NORMAL: Использует лидера когда проигрывает или в конце игры (10% шанс)
                double chance = 0.1;
                if (scoreDifference < -5) chance = 0.3;
                if (currentRound >= 2 && roundsWonAI < roundsWonHuman) chance = 0.4;
                return RANDOM.nextDouble() < chance;
                
            case HARD:
                // HARD: Стратегическое использование лидера (15% базовая, до 50% в критических ситуациях)
                double hardChance = 0.15;
                if (scoreDifference < -8) hardChance = 0.4;
                if (currentRound >= 2 && roundsWonAI < roundsWonHuman && scoreDifference < -5) hardChance = 0.5;
                if (currentRound == 3 && roundsWonAI == 1 && roundsWonHuman == 1) hardChance = 0.6;
                return RANDOM.nextDouble() < hardChance;
                
            case EXPERT:
                // EXPERT: Оптимальное использование лидера (20% базовая, до 80% в критических ситуациях)
                double expertChance = 0.2;
                // Критическая ситуация - проигрываем раунд и игру
                if (currentRound >= 2 && roundsWonAI < roundsWonHuman && scoreDifference < -5) {
                    expertChance = 0.7;
                }
                // Последний раунд, равный счёт
                if (currentRound == 3 && roundsWonAI == 1 && roundsWonHuman == 1 && Math.abs(scoreDifference) <= 3) {
                    expertChance = 0.8;
                }
                // Сильно проигрываем текущий раунд
                if (scoreDifference < -12) {
                    expertChance = 0.6;
                }
                // Можем выиграть раунд с помощью лидера
                if (scoreDifference > -3 && scoreDifference < 3 && currentRound >= 2) {
                    expertChance = 0.5;
                }
                return RANDOM.nextDouble() < expertChance;
                
            default:
                return false;
        }
    }
    
    /**
     * Определяет, нужно ли играть карту
     */
    private static boolean shouldPlayCard(MatchConfig.Difficulty difficulty, int scoreDifference,
                                         int roundsWonAI, int roundsWonHuman, int currentRound, int handSize) {
        if (handSize == 0) return false;
        
        switch (difficulty) {
            case EASY:
                // EASY: Играет если проигрывает, пасует если впереди на 5+ очков
                if (scoreDifference < 0) return true;
                if (scoreDifference >= 5) return RANDOM.nextDouble() < 0.2; // 20% шанс продолжить
                return RANDOM.nextDouble() < 0.5; // 50% шанс если впереди на 1-4 очка
                
            case NORMAL:
                // NORMAL: Играет если проигрывает или равный счёт, пасует если впереди на 8+ очков
                if (scoreDifference <= 0) return true;
                if (scoreDifference >= 8) return RANDOM.nextDouble() < 0.25; // 25% шанс
                return RANDOM.nextDouble() < 0.65; // 65% шанс если впереди на 1-7 очков
                
            case HARD:
                // HARD: Почти всегда играет, пасует только если впереди на 15+ очков
                if (scoreDifference < 0) return true;
                if (scoreDifference >= 15) return RANDOM.nextDouble() < 0.3; // 30% шанс
                // В последнем раунде играет агрессивнее
                if (currentRound == 3 && roundsWonAI == roundsWonHuman) return true;
                return RANDOM.nextDouble() < 0.75; // 75% шанс
                
            case EXPERT:
                // EXPERT: Почти никогда не пасует, всегда ищет преимущество
                if (scoreDifference < 0) return true;
                if (scoreDifference >= 20) return RANDOM.nextDouble() < 0.4; // 40% шанс даже при большом отрыве
                // В критических раундах играет всегда
                if (currentRound >= 2 && roundsWonAI <= roundsWonHuman) return true;
                return RANDOM.nextDouble() < 0.85; // 85% шанс
                
            default:
                return true;
        }
    }
    
    /**
     * Проверяет, есть ли у villager карта способности в руке
     */
    private static boolean hasAbilityCardInHand(FalkyeGameSession session, ServerPlayer player) {
        List<Card> hand = session.getHand(player);
        for (Card card : hand) {
            if (card.getType() == Card.CardType.SPELL || card.getType() == Card.CardType.SPECIAL) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Определяет, нужно ли играть картой способности (после того как сыграли обычной картой)
     */
    private static boolean shouldPlayAbilityCard(MatchConfig.Difficulty difficulty, int scoreDifference,
                                                int roundsWonAI, int roundsWonHuman, int currentRound, List<Card> hand) {
        if (hand.isEmpty()) return false;
        
        // Проверяем, есть ли вообще карты способностей
        boolean hasAbility = false;
        for (Card card : hand) {
            if (card.getType() == Card.CardType.SPELL || card.getType() == Card.CardType.SPECIAL) {
                hasAbility = true;
                break;
            }
        }
        if (!hasAbility) return false;
        
        switch (difficulty) {
            case EASY:
                // EASY: Играет картой способности только если сильно проигрывает (30% шанс)
                return scoreDifference < -5 && RANDOM.nextDouble() < 0.3;
                
            case NORMAL:
                // NORMAL: Играет если проигрывает или равный счёт (50% шанс)
                if (scoreDifference <= 0) return RANDOM.nextDouble() < 0.5;
                return RANDOM.nextDouble() < 0.3; // 30% шанс если впереди
                
            case HARD:
                // HARD: Играет чаще, особенно если проигрывает (70% если проигрывает, 50% если впереди)
                if (scoreDifference < 0) return RANDOM.nextDouble() < 0.7;
                if (currentRound >= 2 && roundsWonAI <= roundsWonHuman) return RANDOM.nextDouble() < 0.8;
                return RANDOM.nextDouble() < 0.5;
                
            case EXPERT:
                // EXPERT: Почти всегда играет картой способности если она есть (85% если проигрывает, 70% если впереди)
                if (scoreDifference < 0) return RANDOM.nextDouble() < 0.85;
                if (currentRound >= 2 && roundsWonAI <= roundsWonHuman) return RANDOM.nextDouble() < 0.9;
                return RANDOM.nextDouble() < 0.7;
                
            default:
                return false;
        }
    }
    
    /**
     * Выбирает карту способности для игры
     */
    private static Card selectAbilityCard(MatchConfig.Difficulty difficulty, List<Card> hand, int scoreDifference,
                                         int aiMeleePower, int aiRangedPower, int aiSiegePower,
                                         int humanMeleePower, int humanRangedPower, int humanSiegePower,
                                         boolean weatherAffectsMelee, boolean weatherAffectsRanged, boolean weatherAffectsSiege) {
        // Фильтруем только карты способностей
        List<Card> abilityCards = new java.util.ArrayList<>();
        for (Card card : hand) {
            if (card.getType() == Card.CardType.SPELL || card.getType() == Card.CardType.SPECIAL) {
                abilityCards.add(card);
            }
        }
        
        if (abilityCards.isEmpty()) return null;
        
        switch (difficulty) {
            case EASY:
            case NORMAL:
                // EASY/NORMAL: Случайный выбор из карт способностей
                return abilityCards.get(RANDOM.nextInt(abilityCards.size()));
                
            case HARD:
            case EXPERT:
                // HARD/EXPERT: Стратегический выбор
                return selectStrategicCard(abilityCards, scoreDifference,
                    aiMeleePower, aiRangedPower, aiSiegePower,
                    humanMeleePower, humanRangedPower, humanSiegePower,
                    weatherAffectsMelee, weatherAffectsRanged, weatherAffectsSiege,
                    difficulty == MatchConfig.Difficulty.EXPERT);
                
            default:
                return abilityCards.get(RANDOM.nextInt(abilityCards.size()));
        }
    }
    
    /**
     * Выбирает обычную карту (не способность) для игры
     */
    private static Card selectNormalCard(MatchConfig.Difficulty difficulty, List<Card> hand, int scoreDifference,
                                        int aiMeleePower, int aiRangedPower, int aiSiegePower,
                                        int humanMeleePower, int humanRangedPower, int humanSiegePower,
                                        boolean weatherAffectsMelee, boolean weatherAffectsRanged, boolean weatherAffectsSiege) {
        // Фильтруем только обычные карты (существа)
        List<Card> normalCards = new java.util.ArrayList<>();
        for (Card card : hand) {
            if (card.getType() == Card.CardType.CREATURE) {
                normalCards.add(card);
            }
        }
        
        if (normalCards.isEmpty()) return null;
        
        switch (difficulty) {
            case EASY:
                // EASY: Предпочитает более сильные карты, но с элементом случайности
                normalCards.sort((c1, c2) -> Integer.compare(c2.getPower(), c1.getPower()));
                int topCards = Math.min(3, normalCards.size());
                return normalCards.get(RANDOM.nextInt(topCards));
                
            case NORMAL:
                // NORMAL: Выбирает из топ-50% карт по силе
                normalCards.sort((c1, c2) -> Integer.compare(c2.getPower(), c1.getPower()));
                int topHalf = Math.max(1, normalCards.size() / 2);
                return normalCards.get(RANDOM.nextInt(topHalf));
                
            case HARD:
                // HARD: Стратегический выбор с учётом эффектов и ситуации
                return selectStrategicCard(normalCards, scoreDifference, 
                    aiMeleePower, aiRangedPower, aiSiegePower,
                    humanMeleePower, humanRangedPower, humanSiegePower,
                    weatherAffectsMelee, weatherAffectsRanged, weatherAffectsSiege, false);
                
            case EXPERT:
                // EXPERT: Продвинутый выбор с полным анализом
                return selectStrategicCard(normalCards, scoreDifference,
                    aiMeleePower, aiRangedPower, aiSiegePower,
                    humanMeleePower, humanRangedPower, humanSiegePower,
                    weatherAffectsMelee, weatherAffectsRanged, weatherAffectsSiege, true);
                
            default:
                return normalCards.get(RANDOM.nextInt(normalCards.size()));
        }
    }
    
    /**
     * Выбирает карту для игры в зависимости от сложности (устаревший метод, используется для обратной совместимости)
     */
    private static Card selectCard(MatchConfig.Difficulty difficulty, List<Card> hand, int scoreDifference,
                                   int aiMeleePower, int aiRangedPower, int aiSiegePower,
                                   int humanMeleePower, int humanRangedPower, int humanSiegePower,
                                   boolean weatherAffectsMelee, boolean weatherAffectsRanged, boolean weatherAffectsSiege) {
        // Используем selectNormalCard для обратной совместимости
        return selectNormalCard(difficulty, hand, scoreDifference,
            aiMeleePower, aiRangedPower, aiSiegePower,
            humanMeleePower, humanRangedPower, humanSiegePower,
            weatherAffectsMelee, weatherAffectsRanged, weatherAffectsSiege);
    }
    
    /**
     * Стратегический выбор карты (для HARD и EXPERT)
     */
    private static Card selectStrategicCard(List<Card> hand, int scoreDifference,
                                           int aiMeleePower, int aiRangedPower, int aiSiegePower,
                                           int humanMeleePower, int humanRangedPower, int humanSiegePower,
                                           boolean weatherAffectsMelee, boolean weatherAffectsRanged, boolean weatherAffectsSiege,
                                           boolean expertMode) {
        // Оцениваем каждую карту
        Card bestCard = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        
        for (Card card : hand) {
            double score = evaluateCard(card, scoreDifference,
                aiMeleePower, aiRangedPower, aiSiegePower,
                humanMeleePower, humanRangedPower, humanSiegePower,
                weatherAffectsMelee, weatherAffectsRanged, weatherAffectsSiege, expertMode);
            
            if (score > bestScore) {
                bestScore = score;
                bestCard = card;
            }
        }
        
        // В EXPERT режиме всегда выбираем лучшую, в HARD - иногда добавляем случайность
        if (expertMode || RANDOM.nextDouble() < 0.8) {
            return bestCard != null ? bestCard : hand.get(0);
        } else {
            // 20% шанс выбрать случайную из топ-3
            hand.sort((c1, c2) -> {
                double s1 = evaluateCard(c1, scoreDifference, aiMeleePower, aiRangedPower, aiSiegePower,
                    humanMeleePower, humanRangedPower, humanSiegePower,
                    weatherAffectsMelee, weatherAffectsRanged, weatherAffectsSiege, false);
                double s2 = evaluateCard(c2, scoreDifference, aiMeleePower, aiRangedPower, aiSiegePower,
                    humanMeleePower, humanRangedPower, humanSiegePower,
                    weatherAffectsMelee, weatherAffectsRanged, weatherAffectsSiege, false);
                return Double.compare(s2, s1);
            });
            int topCards = Math.min(3, hand.size());
            return hand.get(RANDOM.nextInt(topCards));
        }
    }
    
    /**
     * Оценивает карту для стратегического выбора
     */
    private static double evaluateCard(Card card, int scoreDifference,
                                      int aiMeleePower, int aiRangedPower, int aiSiegePower,
                                      int humanMeleePower, int humanRangedPower, int humanSiegePower,
                                      boolean weatherAffectsMelee, boolean weatherAffectsRanged, boolean weatherAffectsSiege,
                                      boolean expertMode) {
        double score = card.getPower();
        
        // Бонус за заклинания в зависимости от ситуации
        if (card.getType() == Card.CardType.SPELL) {
            String cardId = card.getId();
            if (cardId.equals("flame_storm")) {
                // Огненная буря хороша когда у противника много карт
                int opponentCards = humanMeleePower + humanRangedPower + humanSiegePower;
                score += opponentCards * 0.5;
            } else if (cardId.equals("nature_heal")) {
                // Исцеление природы хорошо когда у нас много карт
                int myCards = aiMeleePower + aiRangedPower + aiSiegePower;
                score += myCards * 0.3;
            } else if (cardId.equals("time_freeze")) {
                // Заморозка времени хороша когда проигрываем
                if (scoreDifference < 0) score += 5;
            }
        }
        
        // В EXPERT режиме учитываем больше факторов
        if (expertMode) {
            // Бонус за редкие карты
            switch (card.getRarity()) {
                case LEGENDARY: score += 3; break;
                case EPIC: score += 2; break;
                case RARE: score += 1; break;
                default: break;
            }
            
            // Штраф если карта будет затронута погодой (для существ)
            if (card.getType() == Card.CardType.CREATURE) {
                // Это упрощённая оценка - в реальности нужно знать в какой ряд будет играться
                // Но для оценки карты в руке это достаточно
            }
        }
        
        return score;
    }
    
    /**
     * Выбирает ряд для карты
     */
    private static FalkyeGameSession.CardRow selectRow(MatchConfig.Difficulty difficulty, Card card,
                                                       int aiMeleeCount, int aiRangedCount, int aiSiegeCount,
                                                       int humanMeleeCount, int humanRangedCount, int humanSiegeCount,
                                                       int aiMeleePower, int aiRangedPower, int aiSiegePower,
                                                       int humanMeleePower, int humanRangedPower, int humanSiegePower,
                                                       boolean weatherAffectsMelee, boolean weatherAffectsRanged, boolean weatherAffectsSiege) {
        switch (difficulty) {
            case EASY:
                // EASY: Случайный выбор с небольшим предпочтением менее заполненных рядов
                double rand = RANDOM.nextDouble();
                if (aiMeleeCount <= aiRangedCount && aiMeleeCount <= aiSiegeCount && rand < 0.5) {
                    return FalkyeGameSession.CardRow.MELEE;
                } else if (aiRangedCount <= aiSiegeCount && rand < 0.7) {
                    return FalkyeGameSession.CardRow.RANGED;
                } else {
                    return FalkyeGameSession.CardRow.SIEGE;
                }
                
            case NORMAL:
                // NORMAL: Выбирает ряд с наименьшим количеством карт
                if (aiMeleeCount <= aiRangedCount && aiMeleeCount <= aiSiegeCount) {
                    return FalkyeGameSession.CardRow.MELEE;
                } else if (aiRangedCount <= aiSiegeCount) {
                    return FalkyeGameSession.CardRow.RANGED;
                } else {
                    return FalkyeGameSession.CardRow.SIEGE;
                }
                
            case HARD:
            case EXPERT:
                // HARD/EXPERT: Стратегический выбор с учётом всех факторов
                return selectStrategicRow(card, aiMeleeCount, aiRangedCount, aiSiegeCount,
                    humanMeleeCount, humanRangedCount, humanSiegeCount,
                    aiMeleePower, aiRangedPower, aiSiegePower,
                    humanMeleePower, humanRangedPower, humanSiegePower,
                    weatherAffectsMelee, weatherAffectsRanged, weatherAffectsSiege,
                    difficulty == MatchConfig.Difficulty.EXPERT);
                
            default:
                return FalkyeGameSession.CardRow.MELEE;
        }
    }
    
    /**
     * Стратегический выбор ряда (для HARD и EXPERT)
     */
    private static FalkyeGameSession.CardRow selectStrategicRow(Card card,
                                                                 int aiMeleeCount, int aiRangedCount, int aiSiegeCount,
                                                                 int humanMeleeCount, int humanRangedCount, int humanSiegeCount,
                                                                 int aiMeleePower, int aiRangedPower, int aiSiegePower,
                                                                 int humanMeleePower, int humanRangedPower, int humanSiegePower,
                                                                 boolean weatherAffectsMelee, boolean weatherAffectsRanged, boolean weatherAffectsSiege,
                                                                 boolean expertMode) {
        // Оцениваем каждый ряд
        double meleeScore = evaluateRow(aiMeleeCount, humanMeleeCount, aiMeleePower, humanMeleePower, 
            weatherAffectsMelee, card.getPower(), expertMode);
        double rangedScore = evaluateRow(aiRangedCount, humanRangedCount, aiRangedPower, humanRangedPower,
            weatherAffectsRanged, card.getPower(), expertMode);
        double siegeScore = evaluateRow(aiSiegeCount, humanSiegeCount, aiSiegePower, humanSiegePower,
            weatherAffectsSiege, card.getPower(), expertMode);
        
        // Выбираем лучший ряд
        if (meleeScore >= rangedScore && meleeScore >= siegeScore) {
            return FalkyeGameSession.CardRow.MELEE;
        } else if (rangedScore >= siegeScore) {
            return FalkyeGameSession.CardRow.RANGED;
        } else {
            return FalkyeGameSession.CardRow.SIEGE;
        }
    }
    
    /**
     * Оценивает ряд для размещения карты
     */
    private static double evaluateRow(int myCount, int opponentCount, int myPower, int opponentPower,
                                      boolean weatherAffected, int cardPower, boolean expertMode) {
        double score = 0;
        
        // Бонус за меньшее количество карт (избегаем переполнения)
        score += (10 - myCount) * 0.5;
        
        // Бонус если у противника меньше карт в этом ряду (меньше риск погодных эффектов)
        score += (10 - opponentCount) * 0.3;
        
        // Бонус за силу карты (если ряд слабый, сильная карта поможет больше)
        if (myPower < opponentPower) {
            score += cardPower * 0.2;
        }
        
        // Штраф если ряд затронут погодой
        if (weatherAffected) {
            score -= 3;
        }
        
        // В EXPERT режиме учитываем баланс силы
        if (expertMode) {
            int powerDiff = myPower - opponentPower;
            if (powerDiff < 0) {
                // Проигрываем в этом ряду - бонус за размещение
                score += Math.abs(powerDiff) * 0.1;
            } else if (powerDiff > 5) {
                // Уже выигрываем - небольшой штраф (но не критичный)
                score -= 1;
            }
        }
        
        return score;
    }
    
    /**
     * Вычисляет силу ряда с учётом модификаторов
     */
    private static int calculateRowPower(FalkyeGameSession session, List<Card> row) {
        int power = 0;
        for (Card card : row) {
            power += session.getEffectivePower(card);
        }
        return power;
    }
    
    /**
     * Проигрывает звук реакции жителя на ход игрока
     */
    public static void playVillagerReactionOnPlayerMove(net.minecraft.world.entity.LivingEntity villager, int scoreDifference) {
        if (!(villager instanceof net.minecraft.world.entity.npc.Villager)) {
            return;
        }
        
        net.minecraft.world.level.Level level = villager.level();
        if (level.isClientSide) {
            return;
        }
        
        // Выбираем звук в зависимости от ситуации после хода игрока
        net.minecraft.sounds.SoundEvent sound;
        float pitch = 1.0f;
        
        if (scoreDifference < -15) {
            // Сильно проигрывает после хода игрока - обеспокоенный звук
            sound = net.minecraft.sounds.SoundEvents.VILLAGER_AMBIENT;
            pitch = 0.85f;
        } else if (scoreDifference < -5) {
            // Проигрывает - нейтральный звук
            sound = net.minecraft.sounds.SoundEvents.VILLAGER_TRADE;
            pitch = 0.95f;
        } else if (scoreDifference > 15) {
            // Сильно впереди - довольный звук
            sound = net.minecraft.sounds.SoundEvents.VILLAGER_YES;
            pitch = 1.15f;
        } else {
            // Нейтральная ситуация - обычный звук
            sound = net.minecraft.sounds.SoundEvents.VILLAGER_AMBIENT;
            pitch = 1.0f;
        }
        
        level.playSound(null, villager.getX(), villager.getY(), villager.getZ(), 
            sound, villager.getSoundSource(), 0.4f, pitch);
    }
    
    /**
     * Проигрывает звук реакции жителя на события игры
     */
    private static void playVillagerReactionSound(net.minecraft.world.entity.LivingEntity villager, int scoreDifference, boolean noCards) {
        if (!(villager instanceof net.minecraft.world.entity.npc.Villager)) {
            return;
        }
        
        net.minecraft.world.level.Level level = villager.level();
        if (level.isClientSide) {
            return;
        }
        
        // Выбираем звук в зависимости от ситуации
        net.minecraft.sounds.SoundEvent sound;
        float pitch = 1.0f;
        
        if (noCards) {
            // Нет карт - грустный звук
            sound = net.minecraft.sounds.SoundEvents.VILLAGER_NO;
            pitch = 0.8f;
        } else if (scoreDifference > 10) {
            // Впереди - довольный звук
            sound = net.minecraft.sounds.SoundEvents.VILLAGER_YES;
            pitch = 1.2f;
        } else if (scoreDifference < -10) {
            // Проигрывает - обеспокоенный звук
            sound = net.minecraft.sounds.SoundEvents.VILLAGER_AMBIENT;
            pitch = 0.9f;
        } else {
            // Нейтральная ситуация - обычный звук
            sound = net.minecraft.sounds.SoundEvents.VILLAGER_TRADE;
            pitch = 1.0f;
        }
        
        level.playSound(null, villager.getX(), villager.getY(), villager.getZ(), 
            sound, villager.getSoundSource(), 0.5f, pitch);
    }
    
    /**
     * Проигрывает звук реакции жителя на окончание игры (победа/поражение)
     */
    public static void playVillagerReactionOnGameEnd(net.minecraft.world.entity.LivingEntity villager, boolean villagerWon) {
        if (!(villager instanceof net.minecraft.world.entity.npc.Villager)) {
            return;
        }
        
        net.minecraft.world.level.Level level = villager.level();
        if (level.isClientSide) {
            return;
        }
        
        net.minecraft.sounds.SoundEvent sound;
        float pitch = 1.0f;
        float volume = 0.6f;
        
        if (villagerWon) {
            // Villager победил - радостный звук
            sound = net.minecraft.sounds.SoundEvents.VILLAGER_YES;
            pitch = 1.3f;
            volume = 0.7f;
        } else {
            // Villager проиграл - грустный звук
            sound = net.minecraft.sounds.SoundEvents.VILLAGER_NO;
            pitch = 0.7f;
            volume = 0.5f;
        }
        
        level.playSound(null, villager.getX(), villager.getY(), villager.getZ(), 
            sound, villager.getSoundSource(), volume, pitch);
    }
}

