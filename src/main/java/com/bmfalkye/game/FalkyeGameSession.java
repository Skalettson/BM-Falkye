package com.bmfalkye.game;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardDeck;
import com.bmfalkye.cards.LeaderCard;
import com.bmfalkye.util.ModLogger;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Полноценная игровая сессия в стиле Falkye.
 * 
 * <p>Управляет состоянием игры между двумя игроками или игроком и NPC.
 * Игра состоит из 3 раундов, для победы необходимо выиграть 2 раунда.
 * 
 * <p>Механика игры:
 * <ul>
 *   <li>Каждый игрок начинает с 10 картами на руке</li>
 *   <li>Карты размещаются в трёх рядах: ближний бой (меле), дальний бой (рангед), осада (сидж)</li>
 *   <li>Победа в раунде определяется по сумме силы всех карт на поле</li>
 *   <li>Раунд заканчивается, когда оба игрока спасовали или один из них сдался</li>
 * </ul>
 * 
 * <p>Особенности:
 * <ul>
 *   <li>Поддержка игры с NPC/villager через {@link #villagerOpponent}</li>
 *   <li>Система погодных эффектов, влияющих на силу карт</li>
 *   <li>Модификаторы силы карт для эффектов усиления/ослабления</li>
 *   <li>Таймер хода для ограничения времени на принятие решения</li>
 * </ul>
 * 
 * @author BeforeMine Team
 * @since 1.0
 */
public class FalkyeGameSession {
    private final ServerPlayer player1;
    private final ServerPlayer player2; // Может быть null, если играем с villager
    private final CardDeck deck1;
    private final CardDeck deck2;
    
    // Для игры с villager или NPC из BM Characters
    private net.minecraft.world.entity.LivingEntity villagerOpponent;
    
    // Для сюжетных дуэлей
    private com.bmfalkye.npc.StoryNPC storyNPC;
    
    // Для арены драфта
    private boolean draftArena = false;
    private com.bmfalkye.draft.DraftSession draftSession;
    private com.bmfalkye.draft.DraftSession draftSession2; // Для второго игрока в PvP
    
    // Конфигурация матча
    private com.bmfalkye.game.MatchConfig matchConfig;
    
    // Руки игроков
    private final List<Card> hand1;
    private final List<Card> hand2;
    
    // Поля игроков (3 ряда: ближний, дальний, осада)
    private final List<Card> meleeRow1;
    private final List<Card> rangedRow1;
    private final List<Card> siegeRow1;
    private final List<Card> meleeRow2;
    private final List<Card> rangedRow2;
    private final List<Card> siegeRow2;
    
    // Лидеры
    private LeaderCard leader1;
    private LeaderCard leader2;
    
    // Погода на поле
    protected WeatherType weather;
    
    // Раунды
    protected int currentRound = 1;
    protected int roundsWon1 = 0;
    protected int roundsWon2 = 0;
    
    // Текущий раунд
    protected int roundScore1 = 0;
    protected int roundScore2 = 0;
    
    // Состояние игры
    protected UUID currentPlayer; // Чей ход
    protected boolean player1Passed = false;
    protected boolean player2Passed = false;
    protected boolean roundEnded = false;
    protected boolean gameEnded = false;
    
    // Флаги для отслеживания хода (сыграл ли обычную карту и карту способности)
    protected boolean player1PlayedNormalCard = false;
    protected boolean player1PlayedAbilityCard = false;
    protected boolean player2PlayedNormalCard = false;
    protected boolean player2PlayedAbilityCard = false;
    
    // Сбросы
    private final List<Card> graveyard1;
    private final List<Card> graveyard2;
    
    /**
     * Модификаторы силы карт (для эффектов усиления/ослабления)
     * 
     * Структура: Map<UUID игрока, Map<ID карты, модификатор>>
     * 
     * ВАЖНО: Храним модификаторы по комбинации (UUID игрока, ID карты), чтобы модификаторы
     * применялись только к картам конкретного игрока, даже если у обоих игроков одинаковые карты.
     * 
     * Пример: Если у обоих игроков есть карта "fire_dragon" и мы усиливаем карты игрока 1,
     * то модификатор будет применён только к карте игрока 1, а не к карте игрока 2.
     * 
     * Модификаторы могут быть:
     * - Положительными (усиление): +1, +2, +5 и т.д.
     * - Отрицательными (ослабление): -1, -2, -5 и т.д.
     * 
     * Модификаторы применяются при расчёте силы карты в методе getCardPower().
     */
    private final Map<UUID, Map<String, Integer>> powerModifiers = new HashMap<>();
    
    // Показанные карты оппонента (UUID игрока, который видит карты -> список ID показанных карт)
    private final Map<UUID, List<String>> revealedCards = new HashMap<>();
    
    // Отслеживание собранных комбо за раунд (UUID игрока -> Set идентификаторов комбо)
    private final Map<UUID, Set<String>> collectedCombos = new HashMap<>();
    
    // Баффы/дебаффы карт (UUID игрока -> Map<ID карты, List<CardBuff>>)
    private final Map<UUID, Map<String, List<com.bmfalkye.cards.CardBuff>>> cardBuffs = new HashMap<>();
    
    // Система записи реплеев
    private final long gameStartTime = System.currentTimeMillis();
    private final List<com.bmfalkye.replay.ReplaySystem.ReplayMove> recordedMoves = new ArrayList<>();
    
    // Отслеживание использованных карт для системы балансировки
    // UUID игрока -> Set ID использованных карт
    private final Map<UUID, Set<String>> usedCards = new HashMap<>();
    
    /**
     * Получает карту баффов для всех игроков
     */
    public Map<UUID, Map<String, List<com.bmfalkye.cards.CardBuff>>> getCardBuffs() {
        return cardBuffs;
    }
    
    /**
     * Получает время начала игры
     */
    public long getGameStartTime() {
        return gameStartTime;
    }
    
    /**
     * Получает записанные ходы
     */
    public List<com.bmfalkye.replay.ReplaySystem.ReplayMove> getRecordedMoves() {
        return new ArrayList<>(recordedMoves);
    }
    
    /**
     * Записывает ход в реплей
     */
    public void recordMove(ServerPlayer player, String action, String cardId) {
        if (player == null) {
            // Ход villager
            recordedMoves.add(new com.bmfalkye.replay.ReplaySystem.ReplayMove(
                "Villager", action, cardId, currentRound));
        } else {
            recordedMoves.add(new com.bmfalkye.replay.ReplaySystem.ReplayMove(
                player.getName().getString(), action, cardId, currentRound));
        }
    }

    public FalkyeGameSession(ServerPlayer player1, ServerPlayer player2, CardDeck deck1, CardDeck deck2, LeaderCard leader1, LeaderCard leader2) {
        // Оптимизация: убрано избыточное логирование при создании сессии
        
        this.player1 = player1;
        this.player2 = player2;
        this.deck1 = deck1;
        this.deck2 = deck2;
        this.leader1 = leader1;
        this.leader2 = leader2;
        this.villagerOpponent = null;
        
        this.hand1 = new ArrayList<>();
        this.hand2 = new ArrayList<>();
        this.meleeRow1 = new ArrayList<>();
        this.rangedRow1 = new ArrayList<>();
        this.siegeRow1 = new ArrayList<>();
        this.meleeRow2 = new ArrayList<>();
        this.rangedRow2 = new ArrayList<>();
        this.siegeRow2 = new ArrayList<>();
        this.graveyard1 = new ArrayList<>();
        this.graveyard2 = new ArrayList<>();
        
        this.weather = WeatherType.NONE;
        
        // Определяем, у кого первый ход:
        // - При игре между игроками: первый ход у того, кому бросили вызов (player1)
        //   В startPlayerMatch: player = target (тот, кому бросили вызов) -> player1 в сессии
        //   opponent = challenger (тот, кто бросил вызов) -> player2 в сессии
        // - При игре с жителем: первый ход у игрока (player1)
        if (isPlayingWithVillager()) {
            // Игра с жителем - первый ход у игрока (player1)
            this.currentPlayer = player1 != null ? player1.getUUID() : null;
        } else {
            // Игра между игроками - первый ход у того, кому бросили вызов (player1)
            // player1 = target (тот, кому бросили вызов)
            this.currentPlayer = player1 != null ? player1.getUUID() : null;
        }
        
        // Сбрасываем счётчики таймаутов для обоих игроков при начале новой игры
        if (player1 != null) {
            com.bmfalkye.game.TurnTimer.resetTimeOutCount(player1.getUUID());
        }
        if (player2 != null) {
            com.bmfalkye.game.TurnTimer.resetTimeOutCount(player2.getUUID());
        }
        
        // Раздаём начальные карты (10 карт каждому)
        dealInitialCards();
        
        // Запускаем таймер для первого хода
        com.bmfalkye.game.TurnTimer.startTurn(this);
        
        // Логируем, чей первый ход
        if (isPlayingWithVillager()) {
            // Игра с жителем
            if (player1 != null) {
                if (isVillagerTurn()) {
                    com.bmfalkye.network.NetworkHandler.addActionLog(player1, 
                        net.minecraft.network.chat.Component.translatable("message.bm_falkye.first_turn_villager").getString());
                } else {
                    com.bmfalkye.network.NetworkHandler.addActionLog(player1, "§aПервый ход: вы");
                }
            }
        } else {
            // Игра между игроками
            if (player1 != null) {
                if (isPlayerTurn(player1)) {
                    com.bmfalkye.network.NetworkHandler.addActionLog(player1, "§aПервый ход: вы");
                } else {
                    com.bmfalkye.network.NetworkHandler.addActionLog(player1, 
                        net.minecraft.network.chat.Component.translatable("message.bm_falkye.first_turn_opponent", 
                            player2 != null ? player2.getName() : net.minecraft.network.chat.Component.literal("противник")).getString());
                }
            }
            if (player2 != null) {
                if (isPlayerTurn(player2)) {
                    com.bmfalkye.network.NetworkHandler.addActionLog(player2, 
                        net.minecraft.network.chat.Component.translatable("message.bm_falkye.first_turn_you").getString());
                } else {
                    com.bmfalkye.network.NetworkHandler.addActionLog(player2, 
                        net.minecraft.network.chat.Component.translatable("message.bm_falkye.first_turn_opponent", 
                            player1 != null ? player1.getName() : net.minecraft.network.chat.Component.literal("противник")).getString());
                }
            }
        }
        
        // Если играем с villager и первый ход у villager, делаем ход AI
        if (isPlayingWithVillager() && isVillagerTurn()) {
            if (player1 != null && player1.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                // Используем асинхронное выполнение без блокирующей задержки
                // Выполняем ход AI сразу, без задержки (задержка была причиной зависания)
                serverLevel.getServer().execute(() -> {
                    com.bmfalkye.game.VillagerAIPlayer.makeAITurn(this);
                    com.bmfalkye.network.NetworkHandler.updateGameState(player1, this);
                });
            }
        }
    }
    
    public void setVillagerOpponent(net.minecraft.world.entity.LivingEntity entity) {
        this.villagerOpponent = entity;
        // Фиксируем жителя (отключаем AI, чтобы он не двигался)
        if (entity != null && entity instanceof net.minecraft.world.entity.Mob mob) {
            mob.setNoAi(true);
        }
    }
    
    public void setStoryNPC(com.bmfalkye.npc.StoryNPC npc) {
        this.storyNPC = npc;
    }
    
    public com.bmfalkye.npc.StoryNPC getStoryNPC() {
        return storyNPC;
    }
    
    public void setDraftArena(boolean draftArena) {
        this.draftArena = draftArena;
    }
    
    public boolean isDraftArena() {
        return draftArena;
    }
    
    public void setDraftSession(com.bmfalkye.draft.DraftSession session) {
        this.draftSession = session;
    }
    
    public com.bmfalkye.draft.DraftSession getDraftSession() {
        return draftSession;
    }
    
    public void setDraftSession2(com.bmfalkye.draft.DraftSession session) {
        this.draftSession2 = session;
    }
    
    public com.bmfalkye.draft.DraftSession getDraftSession2() {
        return draftSession2;
    }
    
    public net.minecraft.world.entity.LivingEntity getVillagerOpponent() {
        return villagerOpponent;
    }
    
    public boolean isPlayingWithVillager() {
        return villagerOpponent != null;
    }
    
    public void setMatchConfig(com.bmfalkye.game.MatchConfig config) {
        this.matchConfig = config;
    }
    
    public com.bmfalkye.game.MatchConfig getMatchConfig() {
        return matchConfig;
    }
    
    /**
     * Получает тип локации на основе позиции игрока
     */
    public com.bmfalkye.game.LocationEffect.LocationType getLocationType() {
        if (player1 == null || !(player1.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return com.bmfalkye.game.LocationEffect.LocationType.NONE;
        }
        
        // Используем позицию первого игрока для определения локации
        return com.bmfalkye.game.LocationEffect.getLocationType(level, player1.blockPosition());
    }

    /**
     * Раздаёт начальные карты игрокам.
     * 
     * <p>Каждый игрок получает 10 карт из своей колоды в начале игры.
     * Метод вызывается при создании игровой сессии.
     * 
     * <p>Логирует количество разданных карт для отладки.
     */
    private void dealInitialCards() {
        ModLogger.logGameEvent("Dealing initial cards", 
            "player1", player1 != null ? player1.getName().getString() : "null",
            "player2", player2 != null ? player2.getName().getString() : (isPlayingWithVillager() ? "Villager" : "null"));
        
        int cardsDealt1 = 0;
        int cardsDealt2 = 0;
        
        for (int i = 0; i < 10; i++) {
            Card card1 = deck1.drawCard();
            if (card1 != null) {
                hand1.add(card1);
                cardsDealt1++;
            }
            
            Card card2 = deck2.drawCard();
            if (card2 != null) {
                hand2.add(card2);
                cardsDealt2++;
            }
        }
        
        ModLogger.logGameEvent("Initial cards dealt", 
            "player1Cards", cardsDealt1,
            "player2Cards", cardsDealt2,
            "player1HandSize", hand1.size(),
            "player2HandSize", hand2.size());
    }

    /**
     * Играет карту на поле от имени указанного игрока.
     * 
     * <p>Проверяет все условия для разыгрывания карты:
     * <ul>
     *   <li>Игра не должна быть завершена</li>
     *   <li>Игрок не должен быть в пасе в текущем раунде</li>
     *   <li>Должен быть ход указанного игрока</li>
     *   <li>Карта должна находиться в руке игрока</li>
     * </ul>
     * 
     * <p>После успешного размещения карты на поле:
     * <ul>
     *   <li>Применяются эффекты карты (если применимо)</li>
     *   <li>Пересчитывается счёт раунда</li>
     *   <li>Ход переходит к оппоненту (если не была сыграна способность)</li>
     * </ul>
     * 
     * @param player игрок, который разыгрывает карту
     * @param card карта для разыгрывания
     * @param row ряд, в который помещается карта (для существ) или на который применяется эффект
     * @return {@code true} если карта была успешно разыграна, {@code false} в противном случае
     */
    public boolean playCard(ServerPlayer player, Card card, CardRow row) {
        if (player == null || roundEnded || gameEnded) {
            ModLogger.logGameLogic("playCard failed: invalid state", "player", player != null ? player.getName().getString() : "null", "roundEnded", roundEnded, "gameEnded", gameEnded);
            com.bmfalkye.BMFalkye.LOGGER.debug("playCard: player is null or game ended. player={}, roundEnded={}, gameEnded={}", 
                player, roundEnded, gameEnded);
            return false;
        }
        
        // Проверяем, не пасовал ли игрок в этом раунде (пас действует на весь раунд)
        if (player.equals(player1) && player1Passed) {
            if (player != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "message.bm_falkye.already_passed_round"));
            }
            return false;
        }
        if (player2 != null && player.equals(player2) && player2Passed) {
            if (player != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "message.bm_falkye.already_passed_round"));
            }
            return false;
        }
        
        // Проверяем, чей ход (для villager игнорируем проверку, так как villager играет через playCardForVillager)
        if (!isPlayerTurn(player)) {
            ModLogger.logGameLogic("playCard failed: not player's turn", "player", player.getName().getString(), "currentPlayer", currentPlayer != null ? currentPlayer.toString() : "null");
            com.bmfalkye.BMFalkye.LOGGER.debug("playCard: not player's turn. player={}, currentPlayer={}, isPlayerTurn={}", 
                player != null ? player.getUUID() : null, currentPlayer, isPlayerTurn(player));
            return false;
        }
        
        List<Card> hand = getHand(player);
        // Оптимизация: убрано избыточное логирование
        com.bmfalkye.BMFalkye.LOGGER.debug("playCard: player={}, hand size={}, cardId={}", 
            player.getUUID(), hand.size(), card.getId());
        
        // Ищем карту в руке по ID (так как Card не переопределяет equals)
        Card cardInHand = null;
        for (Card c : hand) {
            if (c.getId().equals(card.getId())) {
                cardInHand = c;
                break;
            }
        }
        if (cardInHand == null) {
            ModLogger.logGameLogic("playCard failed: card not in hand", "player", player.getName().getString(), "cardId", card.getId());
            com.bmfalkye.BMFalkye.LOGGER.debug("playCard: card not found in hand. cardId={}, hand cards={}", 
                card.getId(), hand.stream().map(Card::getId).collect(java.util.stream.Collectors.toList()));
            return false;
        }
        
        ModLogger.logCardAction("Card played", 
            "player", player.getName().getString(), 
            "card", card.getName(),
            "cardId", card.getId(),
            "cardType", card.getType().toString(),
            "cardPower", card.getPower(),
            "row", row.toString(),
            "round", currentRound);
        
        // Определяем тип карты (обычная или способность)
        boolean isAbilityCard = cardInHand.getType() == Card.CardType.SPELL || cardInHand.getType() == Card.CardType.SPECIAL;
        boolean isNormalCard = cardInHand.getType() == Card.CardType.CREATURE;
        
        // Проверяем логику хода
        if (player.equals(player1)) {
            if (isNormalCard && player1PlayedNormalCard) {
                // Уже сыграл обычную карту в этом ходу
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        "message.bm_falkye.already_played_normal_card"));
                }
                return false;
            }
            if (isAbilityCard && player1PlayedAbilityCard) {
                // Уже сыграл картой способности в этом ходу
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§cВы уже сыграли картой способности в этом ходу!"));
                }
                return false;
            }
            if (isNormalCard && player1PlayedAbilityCard) {
                // Нельзя играть обычной картой после карты способности
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        "message.bm_falkye.cannot_play_normal_after_ability"));
                }
                return false;
            }
        } else if (player2 != null && player.equals(player2)) {
            if (isNormalCard && player2PlayedNormalCard) {
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        "message.bm_falkye.already_played_normal_card"));
                }
                return false;
            }
            if (isAbilityCard && player2PlayedAbilityCard) {
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§cВы уже сыграли картой способности в этом ходу!"));
                }
                return false;
            }
            if (isNormalCard && player2PlayedAbilityCard) {
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        "message.bm_falkye.cannot_play_normal_after_ability"));
                }
                return false;
            }
        }
        
        // Удаляем карту из руки (используем найденный объект)
        hand.remove(cardInHand);
        
        // Отслеживаем использование карты для системы балансировки
        UUID playerUUID = player.getUUID();
        usedCards.computeIfAbsent(playerUUID, k -> new HashSet<>()).add(cardInHand.getId());
        
        // Если оппонент видел эту карту (через Прозрение Дозора), удаляем её из показанных
        ServerPlayer opponent = getOpponent(player);
        if (opponent != null) {
            removeRevealedCard(opponent, cardInHand.getId());
        }
        
        // Если это карта способности, она не идёт на поле, а применяет эффект и уходит в сброс
        if (isAbilityCard) {
            // Применяем эффект карты способности
            applyCardEffect(player, cardInHand, row);
            // Отправляем в сброс
            getGraveyard(player).add(cardInHand);
            
            // Проверяем, является ли это картой "Странник Снов" - обрабатываем её эффект при сбросе
            handleDreamWalkerOnDiscard(player, cardInHand);
            
            // Отмечаем, что сыграли картой способности
            if (player.equals(player1)) {
                player1PlayedAbilityCard = true;
            } else if (player2 != null && player.equals(player2)) {
                player2PlayedAbilityCard = true;
            }
            
            com.bmfalkye.network.NetworkHandler.addActionLog(player,
                net.minecraft.network.chat.Component.translatable("message.bm_falkye.ability_card_used", cardInHand.getName()).getString()
            );
            
            // Записываем ход в реплей
            recordMove(player, "use_ability", cardInHand.getId());
            
            // Обновляем ежедневные задания
            if (player != null) {
                com.bmfalkye.daily.DailyRewardSystem.incrementQuestProgress(player, 
                    com.bmfalkye.daily.DailyRewardSystem.QuestType.PLAY_CARDS, 1);
                if (cardInHand.getType() == Card.CardType.SPELL) {
                    com.bmfalkye.daily.DailyRewardSystem.incrementQuestProgress(player, 
                        com.bmfalkye.daily.DailyRewardSystem.QuestType.PLAY_SPELLS, 1);
                }
            }
        } else {
            // Обычная карта идёт на поле
            List<Card> targetRow = getRow(player, row);
            targetRow.add(cardInHand);
            ModLogger.logCardAction("Card added to field", 
                "player", player.getName().getString(), 
                "card", cardInHand.getName(),
                "row", row.toString(), 
                "rowSize", targetRow.size(),
                "round", currentRound);
            
            // Применяем эффект карты (используем карту из руки)
            applyCardEffect(player, cardInHand, row);
            
            // Проверяем комбо-эффекты ПОСЛЕ применения эффекта карты
            // Это важно, так как эффект карты может изменить состояние поля
            com.bmfalkye.combo.ComboSystem.checkAndApplyCombos(this, player, cardInHand);
            
            ModLogger.logCardEffect("Card effect applied after playing", 
                "player", player.getName().getString(), 
                "card", card.getName(),
                "cardId", card.getId(),
                "round", currentRound);
            
            // Отмечаем, что сыграли обычной картой
            if (player.equals(player1)) {
                player1PlayedNormalCard = true;
            } else if (player2 != null && player.equals(player2)) {
                player2PlayedNormalCard = true;
            }
            
            // Логируем действие через лог действий
            String rowName = switch (row) {
                case MELEE -> net.minecraft.network.chat.Component.translatable("row.bm_falkye.melee").getString();
                case RANGED -> net.minecraft.network.chat.Component.translatable("row.bm_falkye.ranged").getString();
                case SIEGE -> net.minecraft.network.chat.Component.translatable("row.bm_falkye.siege").getString();
            };
            com.bmfalkye.network.NetworkHandler.addActionLog(player,
                net.minecraft.network.chat.Component.translatable("message.bm_falkye.card_played", cardInHand.getName(), rowName).getString()
            );
            
            // Записываем ход в реплей
            recordMove(player, "play_card", cardInHand.getId());
            
            // Обновляем ежедневные задания
            if (player != null) {
                com.bmfalkye.daily.DailyRewardSystem.incrementQuestProgress(player, 
                    com.bmfalkye.daily.DailyRewardSystem.QuestType.PLAY_CARDS, 1);
            }
        }
        
        // ВАЖНО: Пересчитываем очки раунда после применения всех эффектов
        // Это гарантирует актуальные очки перед обновлением состояния игры
        recalculateRoundScore();
        ModLogger.logGameLogic("Round score recalculated after card play", 
            "player1Score", roundScore1, 
            "player2Score", roundScore2,
            "round", currentRound);
        
        // Визуальные эффекты будут проиграны на клиенте через GuiEffectManager
        // Не создаем эффекты в мире, чтобы они были в GUI
        
        // Проигрываем звук реакции жителя на ход игрока (если играем с villager)
        if (isPlayingWithVillager() && villagerOpponent != null) {
            int aiScore = getRoundScore(null);
            int humanScore = getRoundScore(player);
            int scoreDifference = aiScore - humanScore;
            com.bmfalkye.game.VillagerAIPlayer.playVillagerReactionOnPlayerMove(villagerOpponent, scoreDifference);
        }
        
        /**
         * ЛОГИКА ПЕРЕКЛЮЧЕНИЯ ХОДА:
         * 
         * Правила переключения хода после разыгрывания карты:
         * 
         * 1. Если игрок сыграл ОБЫЧНОЙ картой (CREATURE):
         *    - Если в руке есть карта способности (SPELL/SPECIAL) → НЕ переключаем ход
         *      Игрок может сыграть картой способности или спасовать
         *    - Если в руке НЕТ карты способности → переключаем ход
         * 
         * 2. Если игрок сыграл КАРТОЙ СПОСОБНОСТИ (SPELL/SPECIAL):
         *    - ВСЕГДА переключаем ход, даже если есть другие карты способности
         *    Это предотвращает бесконечные цепочки способностей
         * 
         * 3. Если игрок спасовал:
         *    - ВСЕГДА переключаем ход
         * 
         * Эта логика обеспечивает баланс между стратегией и темпом игры.
         */
        boolean shouldSwitchTurn = true;
        if (player.equals(player1)) {
            if (isNormalCard && hasAbilityCardInHand(player)) {
                // Сыграли обычной картой и есть карта способности - не переключаем ход
                // Игрок может сыграть картой способности или спасовать (отказаться)
                shouldSwitchTurn = false;
                if (player != null) {
                    com.bmfalkye.network.NetworkHandler.addActionLog(player,
                        net.minecraft.network.chat.Component.translatable("message.bm_falkye.can_play_ability_or_pass").getString());
                }
            } else if (isAbilityCard) {
                // Сыграли картой способности - ВСЕГДА переключаем ход
                shouldSwitchTurn = true;
            }
        } else if (player2 != null && player.equals(player2)) {
            if (isNormalCard && hasAbilityCardInHand(player)) {
                // Сыграли обычной картой и есть карта способности - не переключаем ход
                shouldSwitchTurn = false;
                if (player != null) {
                    com.bmfalkye.network.NetworkHandler.addActionLog(player,
                        net.minecraft.network.chat.Component.translatable("message.bm_falkye.can_play_ability_or_pass").getString());
                }
            } else if (isAbilityCard) {
                // Сыграли картой способности - ВСЕГДА переключаем ход
                shouldSwitchTurn = true;
            }
        }
        
        if (shouldSwitchTurn) {
            // Переключаем ход
            switchTurn();
        }
        
        // Проверяем условия автоматического окончания раунда/игры после хода
        checkAutoEndConditions();
        
        // Обновляем состояние игры после хода (критичное обновление)
        com.bmfalkye.network.NetworkHandler.updateGameStateImmediate(player1, this);
        if (player2 != null) {
            com.bmfalkye.network.NetworkHandler.updateGameStateImmediate(player2, this);
        }
        
        return true;
    }
    
    /**
     * Играет карту для villager (AI)
     */
    public boolean playCardForVillager(Card card, CardRow row) {
        com.bmfalkye.util.ModLogger.logAIAction("Villager playing card", 
            "card", card.getName(),
            "cardId", card.getId(),
            "row", row.toString(),
            "round", currentRound);
        
        if (!isPlayingWithVillager() || roundEnded || gameEnded) {
            return false;
        }
        
        List<Card> hand = hand2; // Рука villager
        // Ищем карту в руке по ID (так как Card не переопределяет equals)
        Card cardInHand = null;
        for (Card c : hand) {
            if (c.getId().equals(card.getId())) {
                cardInHand = c;
                break;
            }
        }
        if (cardInHand == null) {
            return false;
        }
        
        hand.remove(cardInHand);
        
        // Отслеживаем использование карты для системы балансировки (для villager)
        if (villagerOpponent != null) {
            UUID villagerUUID = villagerOpponent.getUUID();
            usedCards.computeIfAbsent(villagerUUID, k -> new HashSet<>()).add(cardInHand.getId());
        }
        
        // Определяем тип карты (обычная или способность)
        boolean isAbilityCard = cardInHand.getType() == Card.CardType.SPELL || cardInHand.getType() == Card.CardType.SPECIAL;
        boolean isNormalCard = cardInHand.getType() == Card.CardType.CREATURE;
        
        // Проверяем логику хода для villager
        if (isNormalCard && player2PlayedNormalCard) {
            // Уже сыграл обычную карту в этом ходу
            return false;
        }
        if (isAbilityCard && player2PlayedAbilityCard) {
            // Уже сыграл картой способности в этом ходу
            return false;
        }
        if (isNormalCard && player2PlayedAbilityCard) {
            // Нельзя играть обычной картой после карты способности
            return false;
        }
        
        // Если это карта способности, она не идёт на поле, а применяет эффект и уходит в сброс
        if (isAbilityCard) {
            // Применяем эффект карты способности
            applyCardEffectForVillager(cardInHand, row);
            // Отправляем в сброс
            getGraveyard(null).add(cardInHand);
            
            // Отмечаем, что сыграли картой способности
            player2PlayedAbilityCard = true;
            
            // Уведомляем игрока о ходе AI через лог действий
            if (player1 != null) {
                com.bmfalkye.network.NetworkHandler.addActionLog(player1,
                    net.minecraft.network.chat.Component.translatable("message.bm_falkye.opponent_used_ability", cardInHand.getName()).getString()
                );
            }
        } else {
            // Обычная карта идёт на поле
            List<Card> targetRow = getRowForVillager(row);
            targetRow.add(cardInHand);
            
            // Применяем эффект карты (если есть)
            applyCardEffectForVillager(cardInHand, row);
            
            // Отмечаем, что сыграли обычной картой
            player2PlayedNormalCard = true;
            
            // Уведомляем игрока о ходе AI через лог действий
            if (player1 != null) {
                String rowName = switch (row) {
                    case MELEE -> "ближний бой";
                    case RANGED -> "дальний бой";
                    case SIEGE -> "осаду";
                };
                com.bmfalkye.network.NetworkHandler.addActionLog(player1,
                    "§cПротивник сыграл: §f" + cardInHand.getName() + " §cв §f" + rowName
                );
            }
            
            // Записываем ход villager в реплей
            recordMove(null, "play_card", cardInHand.getId());
        }
        
        recalculateRoundScore();
        
        // Переключаем ход
        switchTurn();
        
        // Проверяем условия автоматического окончания раунда/игры после хода villager
        checkAutoEndConditions();
        
        // Обновляем состояние игры после хода жителя
        if (player1 != null) {
            com.bmfalkye.network.NetworkHandler.updateGameState(player1, this);
        }
        
        return true;
    }
    
    private List<Card> getRowForVillager(CardRow row) {
        switch (row) {
            case MELEE: return meleeRow2;
            case RANGED: return rangedRow2;
            case SIEGE: return siegeRow2;
            default: return new ArrayList<>();
        }
    }

    /**
     * Пасование (пропуск хода)
     */
    public void pass(ServerPlayer player) {
        if (player == null || roundEnded || gameEnded) {
            ModLogger.logGameLogic("pass failed: invalid state", "player", player != null ? player.getName().getString() : "null", "roundEnded", roundEnded, "gameEnded", gameEnded);
            com.bmfalkye.BMFalkye.LOGGER.debug("pass: player is null or game ended. player={}, roundEnded={}, gameEnded={}", 
                player, roundEnded, gameEnded);
            return;
        }
        
        // Проверяем, чей ход
        if (!isPlayerTurn(player)) {
            if (player != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cНе ваш ход!"));
            }
            return;
        }
        
        // Если игрок сыграл обычной картой и может сыграть картой способности, пас означает отказ от карты способности
        if (player.equals(player1)) {
            if (player1PlayedNormalCard && hasAbilityCardInHand(player) && !player1PlayedAbilityCard) {
                // Игрок отказывается играть картой способности - переключаем ход
                player1PlayedAbilityCard = false; // Отмечаем, что отказался
                com.bmfalkye.network.NetworkHandler.addActionLog(player, "§eВы отказались играть картой способности");
                switchTurn();
                
                // Обновляем состояние игры
                com.bmfalkye.network.NetworkHandler.updateGameStateImmediate(player1, this);
                if (player2 != null) {
                    com.bmfalkye.network.NetworkHandler.updateGameStateImmediate(player2, this);
                }
                return;
            }
        } else if (player2 != null && player.equals(player2)) {
            if (player2PlayedNormalCard && hasAbilityCardInHand(player) && !player2PlayedAbilityCard) {
                player2PlayedAbilityCard = false;
                com.bmfalkye.network.NetworkHandler.addActionLog(player, "§eВы отказались играть картой способности");
                switchTurn();
                
                com.bmfalkye.network.NetworkHandler.updateGameStateImmediate(player1, this);
                if (player2 != null) {
                    com.bmfalkye.network.NetworkHandler.updateGameStateImmediate(player2, this);
                }
                return;
            }
        }
        
        // Обычный пас - пропуск хода
        if (player.equals(player1)) {
            if (player1Passed) {
                ModLogger.logPlayerAction("Pass failed: already passed", "player", player.getName().getString(), "round", currentRound);
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§cВы уже спасовали в этом раунде!"));
                }
                return;
            }
            player1Passed = true;
            
            // Записываем ход в реплей
            recordMove(player, "pass", null);
            
            ModLogger.logPlayerAction("Player passed", 
                "player", player.getName().getString(),
                "round", currentRound,
                "player1Score", roundScore1,
                "player2Score", roundScore2,
                "player1Passed", player1Passed,
                "player2Passed", player2Passed);
            com.bmfalkye.network.NetworkHandler.addActionLog(player, "§eВы спасовали");
            // Уведомляем оппонента о пасе
            if (player2 != null) {
                com.bmfalkye.network.NetworkHandler.addActionLog(player2, "§7Противник спасовал");
            }
        } else if (player2 != null && player.equals(player2)) {
            if (player2Passed) {
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§cВы уже спасовали в этом раунде!"));
                }
                return;
            }
            player2Passed = true;
            
            // Записываем ход в реплей
            recordMove(player, "pass", null);
            
            ModLogger.logPlayerAction("Player2 passed", 
                "player", player.getName().getString(),
                "round", currentRound,
                "player1Score", roundScore1,
                "player2Score", roundScore2,
                "player1Passed", player1Passed,
                "player2Passed", player2Passed);
            com.bmfalkye.network.NetworkHandler.addActionLog(player, "§eВы спасовали");
            // Уведомляем оппонента о пасе
            com.bmfalkye.network.NetworkHandler.addActionLog(player1, "§7Противник спасовал");
        }
        
        // Оптимизация: убрано избыточное логирование
        com.bmfalkye.BMFalkye.LOGGER.debug("pass: player passed. player={}, player1Passed={}, player2Passed={}", 
            player.getUUID(), player1Passed, player2Passed);
        
        // Проверяем условия для битвы карт
        checkForCardBattle();
        
        // Переключаем ход (противник может продолжать играть)
        switchTurn();
        
        // Обновляем состояние игры после паса
        com.bmfalkye.network.NetworkHandler.updateGameStateImmediate(player1, this);
        if (player2 != null) {
            com.bmfalkye.network.NetworkHandler.updateGameStateImmediate(player2, this);
        }
    }
    
    /**
     * Проверяет наличие карты способности в руке игрока
     */
    private boolean hasAbilityCardInHand(ServerPlayer player) {
        List<Card> hand = getHand(player);
        for (Card card : hand) {
            if (card.getType() == Card.CardType.SPELL || card.getType() == Card.CardType.SPECIAL) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Обрабатывает эффект карты "Странник Снов" при сбросе
     * Возвращает 1 случайную карту из сброшенных кроме самой себя
     * Если сброшенных карт нет, то не возвращает ничего
     */
    private void handleDreamWalkerOnDiscard(ServerPlayer player, Card discardedCard) {
        if (discardedCard == null || !discardedCard.getId().equals("dream_walker")) {
            return;
        }
        
        // Получаем сброс игрока
        List<Card> graveyard = getGraveyard(player);
        
        // Фильтруем карты, исключая саму карту "Странник Снов"
        List<Card> availableCards = new ArrayList<>();
        for (Card graveyardCard : graveyard) {
            if (!graveyardCard.getId().equals("dream_walker")) {
                availableCards.add(graveyardCard);
            }
        }
        
        if (!availableCards.isEmpty()) {
            // Возвращаем случайную карту из сброшенных (кроме самой себя)
            Card randomCard = availableCards.get(new java.util.Random().nextInt(availableCards.size()));
            List<Card> hand = getHand(player);
            hand.add(randomCard);
            graveyard.remove(randomCard);
            
            if (player != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§dСтранник Снов возвращает карту из сброса: §f" + randomCard.getName()));
                com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                    "§dСтранник Снов возвращает карту из сброса: §f" + randomCard.getName());
            }
        } else {
            // Нет карт в сбросе (кроме самой себя) - не возвращаем ничего
            if (player != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§7Странник Снов: в сбросе нет других карт"));
                com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                    "§7Странник Снов: в сбросе нет других карт");
            }
        }
    }
    
    /**
     * Проверяет, сыграл ли villager обычную карту в этом ходу
     */
    public boolean hasPlayedNormalCard(ServerPlayer player) {
        if (player == null && isPlayingWithVillager()) {
            return player2PlayedNormalCard;
        }
        if (player != null && player.equals(player1)) {
            return player1PlayedNormalCard;
        }
        if (player != null && player2 != null && player.equals(player2)) {
            return player2PlayedNormalCard;
        }
        return false;
    }
    
    /**
     * Проверяет, сыграл ли villager карту способности в этом ходу
     */
    public boolean hasPlayedAbilityCard(ServerPlayer player) {
        if (player == null && isPlayingWithVillager()) {
            return player2PlayedAbilityCard;
        }
        if (player != null && player.equals(player1)) {
            return player1PlayedAbilityCard;
        }
        if (player != null && player2 != null && player.equals(player2)) {
            return player2PlayedAbilityCard;
        }
        return false;
    }
    
    /**
     * Проверяет условия для битвы карт
     * Битва происходит только если оба игрока пасовали и не выкинули ни одной карты за свои ходы
     */
    private void checkForCardBattle() {
        // Проверяем, пасовали ли оба игрока
        if (!player1Passed || !player2Passed) {
            return;
        }
        
        // Проверяем, выкинули ли игроки карты в этом раунде
        boolean player1PlayedCards = player1PlayedNormalCard || player1PlayedAbilityCard;
        boolean player2PlayedCards = player2PlayedNormalCard || player2PlayedAbilityCard;
        
        // Битва происходит только если оба пасовали и НЕ выкинули ни одной карты
        if (!player1PlayedCards && !player2PlayedCards) {
            ModLogger.logCardAction("Card battle triggered: both players passed without playing cards", 
                "round", currentRound,
                "player1Score", roundScore1,
                "player2Score", roundScore2);
            
            // Проводим битву между картами одного типа боя
            resolveCardBattles();
            // После битвы определяем победителя по очкам и заканчиваем раунд
            endRound();
        }
        // Если хотя бы один игрок выкинул карты, битва не происходит, раунд продолжается
    }
    
    /**
     * Пас для villager (AI)
     */
    public void passVillager() {
        if (!isPlayingWithVillager() || roundEnded || gameEnded) {
            return;
        }
        
        // Если villager сыграл обычной картой и может сыграть картой способности, пас означает отказ от карты способности
        if (player2PlayedNormalCard && hasAbilityCardInHand(null) && !player2PlayedAbilityCard) {
            // Villager отказывается играть картой способности - переключаем ход
            player2PlayedAbilityCard = false; // Отмечаем, что отказался
            com.bmfalkye.util.ModLogger.logAIAction("Villager passing (declining ability card)", 
                "round", currentRound,
                "player1Score", roundScore1,
                "player2Score", roundScore2);
            
            switchTurn();
            
            // Обновляем состояние игры
            if (player1 != null) {
                com.bmfalkye.network.NetworkHandler.updateGameState(player1, this);
            }
            return;
        }
        
        // Проверяем, не пасовал ли уже villager
        if (player2Passed) {
            return;
        }
        
        com.bmfalkye.util.ModLogger.logAIAction("Villager passing", 
            "round", currentRound,
            "player1Score", roundScore1,
            "player2Score", roundScore2);
        
        player2Passed = true;
        
        // Уведомляем игрока о пасе жителя через лог действий
        if (player1 != null) {
            com.bmfalkye.network.NetworkHandler.addActionLog(player1, "§7Противник спасовал");
        }
        
        // Проверяем условия для битвы карт
        checkForCardBattle();
    }

    /**
     * Использование лидера
     */
    public boolean useLeader(ServerPlayer player) {
        if (roundEnded || gameEnded) {
            return false;
        }
        
        LeaderCard leader;
        boolean isVillagerTurn = false;
        
        // Определяем лидера и проверяем ход
        if (player == null && isPlayingWithVillager()) {
            // Использование лидера для villager
            isVillagerTurn = isVillagerTurn();
            if (!isVillagerTurn) {
                return false;
            }
            leader = leader2;
        } else if (player != null) {
            // Использование лидера для игрока
            if (!isPlayerTurn(player)) {
                return false;
            }
            leader = player.equals(player1) ? leader1 : leader2;
        } else {
            return false;
        }
        
        if (leader == null || leader.isUsed()) {
            return false;
        }
        
        // Применяем способность лидера
        leader.use(this, player);
        
        // Записываем ход в реплей
        recordMove(player, "use_leader", leader.getId());
        
        // Обновляем ежедневные задания
        if (player != null) {
            com.bmfalkye.daily.DailyRewardSystem.incrementQuestProgress(player, 
                com.bmfalkye.daily.DailyRewardSystem.QuestType.USE_LEADER, 1);
        }
        
        // Пересчитываем очки после использования лидера
        recalculateRoundScore();
        
        // Переключаем ход после использования лидера
        switchTurn();
        
        // Обновляем состояние игры после использования лидера
        if (player1 != null) {
            com.bmfalkye.network.NetworkHandler.updateGameState(player1, this);
        }
        if (player2 != null) {
            com.bmfalkye.network.NetworkHandler.updateGameState(player2, this);
        }
        
        return true;
    }

    /**
     * Играет карту погоды
     */
    public void playWeatherCard(WeatherType weatherType) {
        this.weather = weatherType;
        recalculateRoundScore();
        
        // Обновляем состояние игры после изменения погоды
        if (player1 != null) {
            com.bmfalkye.network.NetworkHandler.updateGameState(player1, this);
        }
        if (player2 != null) {
            com.bmfalkye.network.NetworkHandler.updateGameState(player2, this);
        }
    }

    private void applyCardEffect(ServerPlayer player, Card card, CardRow row) {
        // Применяем эффект карты, передавая выбранный ряд
        com.bmfalkye.cards.CardEffects.applyCardEffect(this, player, card, row);
    }
    
    private void applyCardEffectForVillager(Card card, CardRow row) {
        // Применяем эффект карты для villager, передавая выбранный ряд
        com.bmfalkye.cards.CardEffects.applyCardEffect(this, null, card, row);
    }
    
    public ServerPlayer getOpponent(ServerPlayer player) {
        if (player == null) {
            return player1;
        }
        if (isPlayingWithVillager()) {
            // Если играем с villager, противник - это player1
            return player.equals(player1) ? null : player1;
        }
        if (player2 == null) {
            return null;
        }
        return player.equals(player1) ? player2 : player1;
    }

    /**
     * Пересчитывает очки раунда (публичный метод для использования в CardEffects)
     */
    public void recalculateRoundScore() {
        roundScore1 = calculateRowScore(player1);
        // Для villager используем null, для обычного игрока - player2
        if (isPlayingWithVillager()) {
            roundScore2 = calculateRowScore(null); // null означает villager
        } else {
            roundScore2 = calculateRowScore(player2);
        }
    }

    private int calculateRowScore(ServerPlayer player) {
        int score = 0;
        
        // Считаем очки с учётом погоды
        List<Card> melee = getMeleeRow(player);
        List<Card> ranged = getRangedRow(player);
        List<Card> siege = getSiegeRow(player);
        
        // Если player null и играем с villager, используем ряды villager
        if (player == null && isPlayingWithVillager()) {
            melee = meleeRow2;
            ranged = rangedRow2;
            siege = siegeRow2;
        }
        
        // Применяем эффекты погоды
        if (weather == WeatherType.FROST) {
            // Мороз снижает силу всех ближних карт до 1
            for (Card card : melee) {
                score += 1;
            }
            score += calculateRowPower(ranged, player);
            score += calculateRowPower(siege, player);
        } else if (weather == WeatherType.FOG) {
            // Туман снижает силу всех дальних карт до 1
            score += calculateRowPower(melee, player);
            for (Card card : ranged) {
                score += 1;
            }
            score += calculateRowPower(siege, player);
        } else if (weather == WeatherType.RAIN) {
            // Дождь снижает силу всех осадных карт до 1
            score += calculateRowPower(melee, player);
            score += calculateRowPower(ranged, player);
            for (Card card : siege) {
                score += 1;
            }
        } else {
            // Нет погоды - считаем нормально
            score += calculateRowPower(melee, player);
            score += calculateRowPower(ranged, player);
            score += calculateRowPower(siege, player);
        }
        
        return score;
    }

    private int calculateRowPower(List<Card> row, ServerPlayer player) {
        int power = 0;
        for (Card card : row) {
            power += getEffectivePower(card, player);
        }
        return power;
    }
    
    private int calculateRowPower(List<Card> row) {
        // Для обратной совместимости - пытаемся определить игрока по картам
        int power = 0;
        for (Card card : row) {
            power += getEffectivePower(card);
        }
        return power;
    }
    
    /**
     * Получает эффективную силу карты с учётом модификаторов
     * ВАЖНО: Модификаторы применяются только к картам конкретного игрока
     */
    public int getEffectivePower(Card card) {
        return getEffectivePower(card, null);
    }
    
    /**
     * Получает эффективную силу карты с учётом модификаторов для конкретного игрока
     * ВАЖНО: Модификаторы применяются только к картам конкретного игрока
     * Также учитывает баффы/дебаффы и эффекты окружения
     */
    public int getEffectivePower(Card card, ServerPlayer player) {
        if (card == null) return 0;
        
        // Начинаем с базовой силы карты
        int basePower = card.getPower();
        
        // Применяем баффы/дебаффы через BuffSystem
        basePower = com.bmfalkye.cards.BuffSystem.calculateEffectivePower(this, player, card);
        
        // Определяем, какому игроку принадлежит карта
        UUID playerUUID = null;
        if (player != null) {
            playerUUID = player.getUUID();
        } else {
            // Если player не указан, пытаемся определить по карте на поле
            if (getMeleeRow(player1).contains(card) || getRangedRow(player1).contains(card) || getSiegeRow(player1).contains(card)) {
                playerUUID = player1 != null ? player1.getUUID() : null;
            } else if (player2 != null && (getMeleeRow(player2).contains(card) || getRangedRow(player2).contains(card) || getSiegeRow(player2).contains(card))) {
                playerUUID = player2.getUUID();
            } else if (isPlayingWithVillager() && (meleeRow2.contains(card) || rangedRow2.contains(card) || siegeRow2.contains(card))) {
                playerUUID = villagerOpponent != null ? villagerOpponent.getUUID() : null;
            }
        }
        
        // Получаем модификатор для этой карты у этого игрока (старая система модификаторов)
        int modifier = 0;
        if (playerUUID != null) {
            Map<String, Integer> playerModifiers = powerModifiers.get(playerUUID);
            if (playerModifiers != null) {
                modifier = playerModifiers.getOrDefault(card.getId(), 0);
            }
        }
        
        basePower += modifier;
        
        // Применяем эффекты окружения (если карта на поле)
        if (player != null && player.level() instanceof net.minecraft.server.level.ServerLevel level) {
            // Проверяем, находится ли карта на поле
            boolean cardOnField = getMeleeRow(player).contains(card) || 
                                getRangedRow(player).contains(card) || 
                                getSiegeRow(player).contains(card);
            
            if (cardOnField) {
                // Получаем тип локации по позиции игрока
                com.bmfalkye.game.LocationEffect.LocationType location = 
                    com.bmfalkye.game.LocationEffect.getLocationType(level, player.blockPosition());
                
                // Применяем модификатор локации
                int locationModifier = com.bmfalkye.game.LocationEffect.getLocationPowerModifier(
                    location, card.getFaction());
                basePower += locationModifier;
            }
        }
        
        return Math.max(0, basePower); // Сила не может быть отрицательной
    }
    
    /**
     * Добавляет модификатор силы для карты конкретного игрока
     * ВАЖНО: Модификатор применяется только к карте этого игрока
     */
    public void addPowerModifier(Card card, int modifier) {
        addPowerModifier(card, modifier, null);
    }
    
    /**
     * Получает текущий модификатор силы для карты конкретного игрока
     * ВАЖНО: Возвращает модификатор только для карты этого игрока
     */
    public int getPowerModifier(Card card, ServerPlayer player) {
        if (card == null) return 0;
        
        UUID playerUUID = player != null ? player.getUUID() : null;
        if (playerUUID == null) {
            // Пытаемся определить игрока по карте на поле
            if (getMeleeRow(player1).contains(card) || getRangedRow(player1).contains(card) || getSiegeRow(player1).contains(card)) {
                playerUUID = player1 != null ? player1.getUUID() : null;
            } else if (player2 != null && (getMeleeRow(player2).contains(card) || getRangedRow(player2).contains(card) || getSiegeRow(player2).contains(card))) {
                playerUUID = player2.getUUID();
            } else if (isPlayingWithVillager() && (meleeRow2.contains(card) || rangedRow2.contains(card) || siegeRow2.contains(card))) {
                playerUUID = villagerOpponent != null ? villagerOpponent.getUUID() : null;
            }
        }
        
        if (playerUUID == null) return 0;
        
        Map<String, Integer> playerModifiers = powerModifiers.get(playerUUID);
        if (playerModifiers == null) return 0;
        
        return playerModifiers.getOrDefault(card.getId(), 0);
    }
    
    /**
     * Добавляет модификатор силы для карты конкретного игрока
     * ВАЖНО: Модификатор применяется только к карте этого игрока
     */
    public void addPowerModifier(Card card, int modifier, ServerPlayer player) {
        if (card == null) return;
        
        // Определяем, какому игроку принадлежит карта
        UUID playerUUID = null;
        if (player != null) {
            playerUUID = player.getUUID();
        } else {
            // Если player не указан, пытаемся определить по карте на поле
            if (getMeleeRow(player1).contains(card) || getRangedRow(player1).contains(card) || getSiegeRow(player1).contains(card)) {
                playerUUID = player1 != null ? player1.getUUID() : null;
            } else if (player2 != null && (getMeleeRow(player2).contains(card) || getRangedRow(player2).contains(card) || getSiegeRow(player2).contains(card))) {
                playerUUID = player2.getUUID();
            } else if (isPlayingWithVillager() && (meleeRow2.contains(card) || rangedRow2.contains(card) || siegeRow2.contains(card))) {
                playerUUID = villagerOpponent != null ? villagerOpponent.getUUID() : null;
            }
        }
        
        if (playerUUID == null) {
            // Не удалось определить игрока - не применяем модификатор
            return;
        }
        
        // Получаем или создаем Map модификаторов для этого игрока
        Map<String, Integer> playerModifiers = powerModifiers.computeIfAbsent(playerUUID, k -> new HashMap<>());
        
        // Добавляем модификатор для этой карты
        String cardId = card.getId();
        playerModifiers.put(cardId, playerModifiers.getOrDefault(cardId, 0) + modifier);
    }
    
    /**
     * Сбрасывает все модификаторы силы (при начале нового раунда)
     */
    public void clearPowerModifiers() {
        powerModifiers.clear();
    }
    
    /**
     * Получает модификаторы силы для всех карт (для передачи на клиент)
     * ВАЖНО: Возвращает модификаторы для ВСЕХ игроков, чтобы оба игрока видели изменения
     * @return Map с ключом - ID карты, значением - модификатор силы
     */
    public java.util.Map<String, Integer> getPowerModifiers() {
        java.util.Map<String, Integer> result = new java.util.HashMap<>();
        // Объединяем модификаторы всех игроков
        // ВАЖНО: Если у обоих игроков есть карты с одинаковым ID, модификаторы объединяются
        // Но это нормально, так как на клиенте модификаторы применяются только к картам на поле
        for (Map<String, Integer> playerModifiers : powerModifiers.values()) {
            for (java.util.Map.Entry<String, Integer> entry : playerModifiers.entrySet()) {
                // Если у карты уже есть модификатор, берем максимальный (на случай конфликтов)
                result.put(entry.getKey(), Math.max(result.getOrDefault(entry.getKey(), 0), entry.getValue()));
            }
        }
        return result;
    }
    
    /**
     * Получает модификаторы силы только для карт указанного игрока (для передачи на клиент)
     * Важно: возвращает модификаторы только для карт, которые принадлежат указанному игроку
     * @param player Игрок, для которого нужно получить модификаторы
     * @return Map с ключом - ID карты, значением - модификатор силы
     */
    public java.util.Map<String, Integer> getPowerModifiersForPlayer(ServerPlayer player) {
        java.util.Map<String, Integer> result = new java.util.HashMap<>();
        
        // Получаем все карты указанного игрока на поле
        List<Card> playerCards = new ArrayList<>();
        playerCards.addAll(getMeleeRow(player));
        playerCards.addAll(getRangedRow(player));
        playerCards.addAll(getSiegeRow(player));
        
        // Добавляем модификаторы только для карт этого игрока
        UUID playerUUID = player != null ? player.getUUID() : null;
        if (playerUUID != null) {
            Map<String, Integer> playerModifiers = powerModifiers.get(playerUUID);
            if (playerModifiers != null) {
                // Добавляем модификаторы только для карт, которые есть на поле у этого игрока
                for (java.util.Map.Entry<String, Integer> entry : playerModifiers.entrySet()) {
                    String cardId = entry.getKey();
                    // Проверяем, что карта с этим ID есть на поле у этого игрока
                    boolean cardOnField = playerCards.stream().anyMatch(c -> c.getId().equals(cardId));
                    if (cardOnField) {
                        result.put(cardId, entry.getValue());
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * Получает показанные карты оппонента для игрока
     * @param player игрок, который видит карты
     * @return список ID показанных карт
     */
    public List<String> getRevealedCards(ServerPlayer player) {
        if (player == null) return new ArrayList<>();
        return revealedCards.getOrDefault(player.getUUID(), new ArrayList<>());
    }
    
    /**
     * Устанавливает показанные карты оппонента для игрока
     * @param player игрок, который видит карты
     * @param cardIds список ID показанных карт
     */
    public void setRevealedCards(ServerPlayer player, List<String> cardIds) {
        if (player == null) return;
        revealedCards.put(player.getUUID(), new ArrayList<>(cardIds));
    }
    
    /**
     * Удаляет показанную карту (когда оппонент использует её)
     * @param player игрок, который видел карту
     * @param cardId ID использованной карты
     */
    public void removeRevealedCard(ServerPlayer player, String cardId) {
        if (player == null) return;
        List<String> revealed = revealedCards.get(player.getUUID());
        if (revealed != null) {
            revealed.remove(cardId);
            if (revealed.isEmpty()) {
                revealedCards.remove(player.getUUID());
            }
        }
    }
    
    /**
     * Очищает все показанные карты (при начале нового раунда)
     */
    public void clearRevealedCards() {
        revealedCards.clear();
    }
    
    /**
     * Проверяет, было ли комбо уже собрано игроком в этом раунде
     * @param player игрок
     * @param comboId идентификатор комбо (например, "faction_Дом Пламени", "legendary", "epic", "spell", "creature", "power")
     * @return true если комбо уже было собрано
     */
    public boolean isComboCollected(ServerPlayer player, String comboId) {
        if (player == null) {
            // Для villager используем UUID villager
            if (isPlayingWithVillager() && villagerOpponent != null) {
                Set<String> combos = collectedCombos.get(villagerOpponent.getUUID());
                return combos != null && combos.contains(comboId);
            }
            return false;
        }
        Set<String> combos = collectedCombos.get(player.getUUID());
        return combos != null && combos.contains(comboId);
    }
    
    /**
     * Отмечает комбо как собранное для игрока в этом раунде
     * @param player игрок
     * @param comboId идентификатор комбо
     */
    public void markComboAsCollected(ServerPlayer player, String comboId) {
        UUID playerUUID;
        if (player == null) {
            // Для villager используем UUID villager
            if (isPlayingWithVillager() && villagerOpponent != null) {
                playerUUID = villagerOpponent.getUUID();
            } else {
                return;
            }
        } else {
            playerUUID = player.getUUID();
        }
        collectedCombos.computeIfAbsent(playerUUID, k -> new HashSet<>()).add(comboId);
    }
    
    /**
     * Очищает все собранные комбо (при начале нового раунда)
     */
    public void clearCollectedCombos() {
        collectedCombos.clear();
    }
    
    /**
     * Получает сброс игрока
     */
    public List<Card> getGraveyard(ServerPlayer player) {
        if (player == null && isPlayingWithVillager()) {
            return graveyard2;
        }
        if (player == null) {
            return new ArrayList<>();
        }
        return player.equals(player1) ? graveyard1 : graveyard2;
    }
    
    /**
     * Находит самую сильную карту на поле игрока
     */
    public Card findStrongestCard(ServerPlayer player) {
        Card strongest = null;
        int maxPower = -1;
        
        List<Card> melee = getMeleeRow(player);
        List<Card> ranged = getRangedRow(player);
        List<Card> siege = getSiegeRow(player);
        
        for (Card card : melee) {
            int power = getEffectivePower(card, player);
            if (power > maxPower) {
                maxPower = power;
                strongest = card;
            }
        }
        for (Card card : ranged) {
            int power = getEffectivePower(card, player);
            if (power > maxPower) {
                maxPower = power;
                strongest = card;
            }
        }
        for (Card card : siege) {
            int power = getEffectivePower(card, player);
            if (power > maxPower) {
                maxPower = power;
                strongest = card;
            }
        }
        
        return strongest;
    }
    
    /**
     * Удаляет карту с поля игрока
     */
    public boolean removeCardFromField(ServerPlayer player, Card card) {
        List<Card> melee = getMeleeRow(player);
        List<Card> ranged = getRangedRow(player);
        List<Card> siege = getSiegeRow(player);
        
        // Ищем карту по ID (так как Card не переопределяет equals)
        Card cardToRemove = null;
        for (Card c : melee) {
            if (c.getId().equals(card.getId())) {
                cardToRemove = c;
                break;
            }
        }
        if (cardToRemove == null) {
            for (Card c : ranged) {
                if (c.getId().equals(card.getId())) {
                    cardToRemove = c;
                    break;
                }
            }
        }
        if (cardToRemove == null) {
            for (Card c : siege) {
                if (c.getId().equals(card.getId())) {
                    cardToRemove = c;
                    break;
                }
            }
        }
        
        if (cardToRemove != null) {
            melee.remove(cardToRemove);
            ranged.remove(cardToRemove);
            siege.remove(cardToRemove);
            // Добавляем в сброс
            getGraveyard(player).add(cardToRemove);
            // Проверяем, является ли это картой "Странник Снов" - обрабатываем её эффект при сбросе
            handleDreamWalkerOnDiscard(player, cardToRemove);
            // Удаляем модификаторы для этой карты у этого игрока
            UUID playerUUID = player != null ? player.getUUID() : null;
            if (playerUUID != null) {
                Map<String, Integer> playerModifiers = powerModifiers.get(playerUUID);
                if (playerModifiers != null) {
                    playerModifiers.remove(cardToRemove.getId());
                }
            }
            // Пересчитываем очки
            recalculateRoundScore();
            return true;
        }
        return false;
    }
    
    /**
     * Проводит битву между картами одного типа боя
     * Когда оба игрока пасуют, карты одного типа боя сражаются друг с другом
     * Слабая карта проигрывает и исчезает, у победившей карты вычитается сила проигравшей
     */
    private void resolveCardBattles() {
        ModLogger.logCardAction("Resolving card battles", 
            "round", currentRound,
            "melee1", meleeRow1.size(), 
            "melee2", meleeRow2.size(),
            "ranged1", rangedRow1.size(),
            "ranged2", rangedRow2.size(),
            "siege1", siegeRow1.size(),
            "siege2", siegeRow2.size());
        
        // Битва в ближнем бою
        resolveRowBattle(meleeRow1, meleeRow2, "ближний бой", player1);
        
        // Битва в дальнем бою
        resolveRowBattle(rangedRow1, rangedRow2, "дальний бой", player1);
        
        // Битва в осаде
        resolveRowBattle(siegeRow1, siegeRow2, "осада", player1);
        
        // Пересчитываем очки после битв
        recalculateRoundScore();
        
        // Обновляем состояние игры после битв
        if (player1 != null) {
            com.bmfalkye.network.NetworkHandler.updateGameStateImmediate(player1, this);
        }
        if (player2 != null) {
            com.bmfalkye.network.NetworkHandler.updateGameStateImmediate(player2, this);
        }
    }
    
    /**
     * Проводит битву между картами одного ряда
     * Сравнивает общую силу рядов и уничтожает слабейшие карты противника
     */
    private void resolveRowBattle(List<Card> row1, List<Card> row2, String rowName, ServerPlayer player1) {
        if (row1.isEmpty() && row2.isEmpty()) {
            // Нет карт для битвы
            return;
        }
        
        // Вычисляем общую силу каждого ряда
        // ВАЖНО: Передаем игроков для правильного расчета эффективной силы
        int totalPower1 = calculateRowPower(row1, player1);
        int totalPower2 = calculateRowPower(row2, player2);
        
        ModLogger.logCardAction("Row battle resolved", 
            "row", rowName, 
            "power1", totalPower1, 
            "power2", totalPower2,
            "round", currentRound,
            "cards1", row1.size(), "cards2", row2.size());
        
        if (totalPower1 == 0 && totalPower2 == 0) {
            return; // Нет силы для битвы
        }
        
        // Определяем победителя
        if (totalPower1 > totalPower2) {
            // Игрок 1 выигрывает - наносим урон ряду игрока 2
            int damage = totalPower2;
            applyDamageToRow(row2, damage, player2, rowName, false);
            if (player1 != null) {
                com.bmfalkye.network.NetworkHandler.addActionLog(player1,
                    "§aВы выиграли ряд " + rowName + "! §7(Ваша сила: §f" + totalPower1 + "§7, сила противника: §f" + totalPower2 + "§7)");
            }
            if (player2 != null) {
                com.bmfalkye.network.NetworkHandler.addActionLog(player2,
                    "§cВы проиграли ряд " + rowName + ". §7(Ваша сила: §f" + totalPower2 + "§7, сила противника: §f" + totalPower1 + "§7)");
            }
        } else if (totalPower2 > totalPower1) {
            // Игрок 2 выигрывает - наносим урон ряду игрока 1
            int damage = totalPower1;
            applyDamageToRow(row1, damage, player1, rowName, true);
            if (player1 != null) {
                com.bmfalkye.network.NetworkHandler.addActionLog(player1,
                    "§cВы проиграли ряд " + rowName + ". §7(Ваша сила: §f" + totalPower1 + "§7, сила противника: §f" + totalPower2 + "§7)");
            }
            if (player2 != null) {
                com.bmfalkye.network.NetworkHandler.addActionLog(player2,
                    "§aВы выиграли ряд " + rowName + "! §7(Ваша сила: §f" + totalPower2 + "§7, сила противника: §f" + totalPower1 + "§7)");
            }
        } else {
            // Ничья - обе стороны получают урон равный их силе
            applyDamageToRow(row1, totalPower1, player1, rowName, true);
            applyDamageToRow(row2, totalPower2, player2, rowName, false);
            if (player1 != null) {
                com.bmfalkye.network.NetworkHandler.addActionLog(player1,
                    "§7Ничья в ряду " + rowName + ". Обе стороны получили урон.");
            }
            if (player2 != null) {
                com.bmfalkye.network.NetworkHandler.addActionLog(player2,
                    "§7Ничья в ряду " + rowName + ". Обе стороны получили урон.");
            }
        }
    }
    
    /**
     * Применяет урон к ряду карт, уничтожая слабейшие карты первыми
     * @param row ряд карт для получения урона
     * @param damage количество урона
     * @param player игрок, чей ряд получает урон (null для villager)
     * @param rowName название ряда (для логов)
     * @param isPlayer1 true если это ряд игрока 1, false если игрока 2
     */
    private void applyDamageToRow(List<Card> row, int damage, ServerPlayer player, String rowName, boolean isPlayer1) {
        if (row.isEmpty() || damage <= 0) {
            return;
        }
        
        // Создаём копию списка для безопасной итерации
        List<Card> cardsToProcess = new ArrayList<>(row);
        
        // Сортируем карты по силе (от слабой к сильной)
        // ВАЖНО: Используем игрока для правильного расчета эффективной силы
        final ServerPlayer finalPlayer = player;
        cardsToProcess.sort(Comparator.comparingInt(card -> getEffectivePower(card, finalPlayer)));
        
        int remainingDamage = damage;
        List<Card> cardsToRemove = new ArrayList<>();
        
        for (Card card : cardsToProcess) {
            if (remainingDamage <= 0) {
                break;
            }
            
            int cardPower = getEffectivePower(card, player);
            
            if (remainingDamage >= cardPower) {
                // Карта уничтожена
                cardsToRemove.add(card);
                remainingDamage -= cardPower;
                
                // Отправляем в сброс
                ServerPlayer cardOwner = isPlayer1 ? player1 : (isPlayingWithVillager() ? null : player2);
                if (cardOwner != null || isPlayingWithVillager()) {
                    getGraveyard(cardOwner).add(card);
                    // Проверяем, является ли это картой "Странник Снов" - обрабатываем её эффект при сбросе
                    if (cardOwner != null) {
                        handleDreamWalkerOnDiscard(cardOwner, card);
                    }
                }
                
                // Удаляем модификаторы для этой карты у этого игрока
                UUID playerUUID = player != null ? player.getUUID() : null;
                if (playerUUID == null && isPlayingWithVillager()) {
                    playerUUID = villagerOpponent != null ? villagerOpponent.getUUID() : null;
                }
                if (playerUUID != null) {
                    Map<String, Integer> playerModifiers = powerModifiers.get(playerUUID);
                    if (playerModifiers != null) {
                        playerModifiers.remove(card.getId());
                    }
                }
                
                // Логируем уничтожение
                if (player != null) {
                    com.bmfalkye.network.NetworkHandler.addActionLog(player,
                        "§cКарта §f" + card.getName() + " §cуничтожена в ряду " + rowName + "!");
                }
            } else {
                // Карта получает урон, но не уничтожается
                int newPower = cardPower - remainingDamage;
                int basePower = card.getPower();
                int currentModifier = getPowerModifier(card, player);
                int newModifier = newPower - basePower;
                // ВАЖНО: Применяем модификатор только к карте этого игрока
                addPowerModifier(card, newModifier - currentModifier, player);
                
                // Логируем урон
                if (player != null) {
                    com.bmfalkye.network.NetworkHandler.addActionLog(player,
                        "§cКарта §f" + card.getName() + " §cполучила урон: §f" + remainingDamage + 
                        " §7(Осталось силы: §f" + newPower + "§7)");
                }
                
                remainingDamage = 0;
            }
        }
        
        // Удаляем уничтоженные карты из ряда
        for (Card card : cardsToRemove) {
            row.remove(card);
        }
        
        ModLogger.logCardAction("Damage applied to row", 
            "row", rowName, 
            "damage", damage,
            "cardsDestroyed", cardsToRemove.size(), 
            "remainingDamage", remainingDamage,
            "round", currentRound);
    }
    

    private void endRound() {
        // Обновляем баффы в конце раунда
        com.bmfalkye.cards.BuffSystem.updateBuffsEndOfRound(this);
        roundEnded = true;
        
        ModLogger.logRoundEvent("Round ending", 
            "round", currentRound,
            "player1Score", roundScore1,
            "player2Score", roundScore2,
            "roundsWon1", roundsWon1,
            "roundsWon2", roundsWon2);
        
        // Определяем победителя раунда по количеству выигранных сторон (рядов)
        // Сравниваем очки по каждому ряду отдельно: ближний бой, дальний бой, осада
        int meleeScore1 = calculateRowPower(getMeleeRow(player1), player1);
        int rangedScore1 = calculateRowPower(getRangedRow(player1), player1);
        int siegeScore1 = calculateRowPower(getSiegeRow(player1), player1);
        
        int meleeScore2, rangedScore2, siegeScore2;
        if (isPlayingWithVillager()) {
            // Для villager используем null, так как у villager нет ServerPlayer
            meleeScore2 = calculateRowPower(meleeRow2, null);
            rangedScore2 = calculateRowPower(rangedRow2, null);
            siegeScore2 = calculateRowPower(siegeRow2, null);
        } else {
            meleeScore2 = calculateRowPower(getMeleeRow(player2), player2);
            rangedScore2 = calculateRowPower(getRangedRow(player2), player2);
            siegeScore2 = calculateRowPower(getSiegeRow(player2), player2);
        }
        
        // Применяем эффекты погоды к очкам рядов
        if (weather == WeatherType.FROST) {
            meleeScore1 = getMeleeRow(player1).size(); // Все ближние карты считаются как 1
            if (isPlayingWithVillager()) {
                meleeScore2 = meleeRow2.size();
            } else {
                meleeScore2 = getMeleeRow(player2).size();
            }
        } else if (weather == WeatherType.FOG) {
            rangedScore1 = getRangedRow(player1).size(); // Все дальние карты считаются как 1
            if (isPlayingWithVillager()) {
                rangedScore2 = rangedRow2.size();
            } else {
                rangedScore2 = getRangedRow(player2).size();
            }
        } else if (weather == WeatherType.RAIN) {
            siegeScore1 = getSiegeRow(player1).size(); // Все осадные карты считаются как 1
            if (isPlayingWithVillager()) {
                siegeScore2 = siegeRow2.size();
            } else {
                siegeScore2 = getSiegeRow(player2).size();
            }
        }
        
        // Определяем победителя каждого ряда
        int player1Wins = 0; // Количество выигранных рядов player1
        int player2Wins = 0; // Количество выигранных рядов player2
        
        if (meleeScore1 > meleeScore2) {
            player1Wins++;
        } else if (meleeScore2 > meleeScore1) {
            player2Wins++;
        }
        
        if (rangedScore1 > rangedScore2) {
            player1Wins++;
        } else if (rangedScore2 > rangedScore1) {
            player2Wins++;
        }
        
        if (siegeScore1 > siegeScore2) {
            player1Wins++;
        } else if (siegeScore2 > siegeScore1) {
            player2Wins++;
        }
        
        // Определяем победителя раунда по количеству выигранных рядов
        String roundResult = "";
        String winner = "";
        
        if (player1Wins > player2Wins) {
            // Player1 выиграл больше рядов
            roundsWon1++;
            winner = player1 != null ? player1.getName().getString() : "Player1";
            if (player1 != null) {
                roundResult = "§aВы выиграли раунд! (§6" + player1Wins + "§a из 3 сторон)";
                com.bmfalkye.network.NetworkHandler.addActionLog(player1, roundResult);
                // Детали по рядам
                com.bmfalkye.network.NetworkHandler.addActionLog(player1, 
                    "§7Ближний бой: §a" + meleeScore1 + " §7- §c" + meleeScore2 + 
                    " | Дальний бой: §a" + rangedScore1 + " §7- §c" + rangedScore2 + 
                    " | Осада: §a" + siegeScore1 + " §7- §c" + siegeScore2);
            }
            if (player2 != null) {
                com.bmfalkye.network.NetworkHandler.addActionLog(player2, "§cВы проиграли раунд. (§6" + player2Wins + "§c из 3 сторон)");
                com.bmfalkye.network.NetworkHandler.addActionLog(player2, 
                    "§7Ближний бой: §c" + meleeScore1 + " §7- §a" + meleeScore2 + 
                    " | Дальний бой: §c" + rangedScore1 + " §7- §a" + rangedScore2 + 
                    " | Осада: §c" + siegeScore1 + " §7- §a" + siegeScore2);
            } else if (isPlayingWithVillager() && player1 != null) {
                com.bmfalkye.network.NetworkHandler.addActionLog(player1, 
                    "§7Противник выиграл §c" + player2Wins + " §7из 3 сторон");
            }
        } else if (player2Wins > player1Wins) {
            // Player2 выиграл больше рядов
            roundsWon2++;
            winner = player2 != null ? player2.getName().getString() : (isPlayingWithVillager() ? "Villager" : "Player2");
            if (player1 != null) {
                roundResult = "§cПротивник выиграл раунд. (§6" + player2Wins + "§c из 3 сторон)";
                com.bmfalkye.network.NetworkHandler.addActionLog(player1, roundResult);
                com.bmfalkye.network.NetworkHandler.addActionLog(player1, 
                    "§7Ближний бой: §c" + meleeScore1 + " §7- §a" + meleeScore2 + 
                    " | Дальний бой: §c" + rangedScore1 + " §7- §a" + rangedScore2 + 
                    " | Осада: §c" + siegeScore1 + " §7- §a" + siegeScore2);
            }
            if (player2 != null) {
                com.bmfalkye.network.NetworkHandler.addActionLog(player2, "§aВы выиграли раунд! (§6" + player2Wins + "§a из 3 сторон)");
                com.bmfalkye.network.NetworkHandler.addActionLog(player2, 
                    "§7Ближний бой: §a" + meleeScore1 + " §7- §c" + meleeScore2 + 
                    " | Дальний бой: §a" + rangedScore1 + " §7- §c" + rangedScore2 + 
                    " | Осада: §a" + siegeScore1 + " §7- §c" + siegeScore2);
            }
        } else {
            // Ничья по количеству выигранных рядов - используем общие очки как тай-брейк
            if (roundScore1 > roundScore2) {
                roundsWon1++;
                winner = player1 != null ? player1.getName().getString() : "Player1";
                if (player1 != null) {
                    roundResult = "§aВы выиграли раунд! (ничья по рядам, но больше общих очков)";
                    com.bmfalkye.network.NetworkHandler.addActionLog(player1, roundResult);
                }
                if (player2 != null) {
                    com.bmfalkye.network.NetworkHandler.addActionLog(player2, "§cВы проиграли раунд. (ничья по рядам, но меньше общих очков)");
                }
            } else if (roundScore2 > roundScore1) {
                roundsWon2++;
                winner = player2 != null ? player2.getName().getString() : (isPlayingWithVillager() ? "Villager" : "Player2");
                if (player1 != null) {
                    roundResult = "§cПротивник выиграл раунд. (ничья по рядам, но больше общих очков)";
                    com.bmfalkye.network.NetworkHandler.addActionLog(player1, roundResult);
                }
                if (player2 != null) {
                    com.bmfalkye.network.NetworkHandler.addActionLog(player2, "§aВы выиграли раунд! (ничья по рядам, но больше общих очков)");
                }
            } else {
                // Полная ничья
                winner = "Draw";
                ModLogger.logRoundEvent("Round ended in draw", 
                    "round", currentRound,
                    "player1Score", roundScore1,
                    "player2Score", roundScore2,
                    "player1Wins", player1Wins,
                    "player2Wins", player2Wins);
                if (player1 != null) {
                    roundResult = "§7Раунд закончился ничьей. (равное количество выигранных рядов и общих очков)";
                    com.bmfalkye.network.NetworkHandler.addActionLog(player1, roundResult);
                }
                if (player2 != null) {
                    com.bmfalkye.network.NetworkHandler.addActionLog(player2, roundResult);
                }
            }
            
            // Показываем детали по рядам даже при ничьей
            if (player1 != null) {
                com.bmfalkye.network.NetworkHandler.addActionLog(player1, 
                    "§7Ближний бой: §e" + meleeScore1 + " §7- §e" + meleeScore2 + 
                    " | Дальний бой: §e" + rangedScore1 + " §7- §e" + rangedScore2 + 
                    " | Осада: §e" + siegeScore1 + " §7- §e" + siegeScore2);
            }
            if (player2 != null) {
                com.bmfalkye.network.NetworkHandler.addActionLog(player2, 
                    "§7Ближний бой: §e" + meleeScore1 + " §7- §e" + meleeScore2 + 
                    " | Дальний бой: §e" + rangedScore1 + " §7- §e" + rangedScore2 + 
                    " | Осада: §e" + siegeScore1 + " §7- §e" + siegeScore2);
            }
        }
        
        ModLogger.logRoundEvent("Round ended", 
            "round", currentRound,
            "winner", winner,
            "roundsWon1", roundsWon1,
            "roundsWon2", roundsWon2);
        
        // Уведомляем о счёте раунда
        if (player1 != null) {
            com.bmfalkye.network.NetworkHandler.addActionLog(player1, 
                "§7Счёт раунда: §a" + roundScore1 + " §7- §c" + roundScore2);
        }
        
        // Визуальные эффекты окончания раунда будут проиграны на клиенте через GuiEffectManager
        // Не создаем эффекты в мире, чтобы они были в GUI
        
        // Обновляем состояние игры после окончания раунда (критичное обновление)
        if (player1 != null) {
            com.bmfalkye.network.NetworkHandler.updateGameStateImmediate(player1, this);
        }
        if (player2 != null) {
            com.bmfalkye.network.NetworkHandler.updateGameStateImmediate(player2, this);
        }
        
        // Проверяем, закончена ли игра
        if (roundsWon1 >= 2 || roundsWon2 >= 2) {
            endGame();
        } else {
            // Начинаем следующий раунд
            startNextRound();
            
            // Если играем с villager и теперь его ход, делаем ход AI
            if (isPlayingWithVillager() && isVillagerTurn()) {
                // Выполняем ход AI в следующем тике сервера
                if (player1 != null && player1.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    serverLevel.getServer().execute(() -> {
                        if (!isGameEnded() && !isRoundEnded() && isVillagerTurn()) {
                            com.bmfalkye.game.VillagerAIPlayer.makeAITurn(this);
                            com.bmfalkye.network.NetworkHandler.updateGameStateImmediate(player1, this);
                        }
                    });
                }
            }
        }
    }

    private void startNextRound() {
        // Проверяем, что не превышен лимит раундов (максимум 3 раунда)
        if (currentRound >= 3) {
            ModLogger.logGameLogic("Cannot start next round: maximum rounds reached", "currentRound", currentRound);
            // Если уже 3 раунда, завершаем игру
            endGame();
            return;
        }
        
        int newRound = currentRound + 1;
        ModLogger.logRoundEvent("Starting next round", 
            "newRound", newRound, 
            "roundsWon1", roundsWon1, 
            "roundsWon2", roundsWon2,
            "player1", player1 != null ? player1.getName().getString() : "null",
            "player2", player2 != null ? player2.getName().getString() : (isPlayingWithVillager() ? "Villager" : "null"));
        
        currentRound++;
        roundEnded = false;
        player1Passed = false;
        
        // Очищаем баффы в начале нового раунда (те, что должны истечь)
        // Баффы с duration > 0 будут обновлены в endRound через updateBuffsEndOfRound
        player2Passed = false;
        player1PlayedNormalCard = false;
        player1PlayedAbilityCard = false;
        player2PlayedNormalCard = false;
        player2PlayedAbilityCard = false;
        roundScore1 = 0;
        roundScore2 = 0;
        weather = WeatherType.NONE;
        
        // Очищаем модификаторы силы (эффекты карт действуют только в рамках раунда)
        clearPowerModifiers();
        
        // Очищаем показанные карты (эффект действует только в рамках раунда)
        clearRevealedCards();
        
        // Очищаем собранные комбо (каждое комбо можно собрать только один раз за раунд)
        clearCollectedCombos();
        
        // Возвращаем карты с поля в колоду (через сброс, затем в колоду)
        // Сначала отправляем карты с поля в сброс
        graveyard1.addAll(meleeRow1);
        graveyard1.addAll(rangedRow1);
        graveyard1.addAll(siegeRow1);
        graveyard2.addAll(meleeRow2);
        graveyard2.addAll(rangedRow2);
        graveyard2.addAll(siegeRow2);
        
        // Возвращаем карты из сброса в колоду (создаём копию списка, чтобы избежать ConcurrentModificationException)
        List<Card> graveyard1Copy = new ArrayList<>(graveyard1);
        List<Card> graveyard2Copy = new ArrayList<>(graveyard2);
        
        for (Card card : graveyard1Copy) {
            if (deck1 != null && card != null) {
                deck1.returnCardToDeck(card);
            }
        }
        for (Card card : graveyard2Copy) {
            if (deck2 != null && card != null) {
                deck2.returnCardToDeck(card);
            }
        }
        
        // Очищаем сбросы
        graveyard1.clear();
        graveyard2.clear();
        
        // Очищаем поля
        meleeRow1.clear();
        rangedRow1.clear();
        siegeRow1.clear();
        meleeRow2.clear();
        rangedRow2.clear();
        siegeRow2.clear();
        
        // Перемешиваем колоды
        if (deck1 != null) {
            deck1.shuffle();
        }
        if (deck2 != null) {
            deck2.shuffle();
        }
        
        // Раздаём новые карты игрокам (10 карт каждому)
        for (int i = 0; i < 10; i++) {
            if (deck1 != null) {
                Card card1 = deck1.drawCard();
                if (card1 != null) {
                    hand1.add(card1);
                }
            }
            if (deck2 != null) {
                Card card2 = deck2.drawCard();
                if (card2 != null) {
                    hand2.add(card2);
                }
            }
        }
        
        // Переключаем первого ходящего (тот, кто выиграл предыдущий раунд)
        if (isPlayingWithVillager() && villagerOpponent != null) {
            if (roundsWon1 > roundsWon2) {
                currentPlayer = player1 != null ? player1.getUUID() : null;
            } else if (roundsWon2 > roundsWon1) {
                currentPlayer = villagerOpponent.getUUID();
            } else {
                // Если была ничья, переключаем
                if (player1 != null && currentPlayer != null && currentPlayer.equals(player1.getUUID())) {
                    currentPlayer = villagerOpponent.getUUID();
                } else {
                    if (player1 != null) {
                        currentPlayer = player1.getUUID();
                    }
                }
            }
        } else if (player2 != null) {
            if (roundsWon1 > roundsWon2) {
                currentPlayer = player1 != null ? player1.getUUID() : null;
            } else if (roundsWon2 > roundsWon1) {
                currentPlayer = player2.getUUID();
            } else {
                // Если была ничья, первый ход делает тот, кто ходил вторым в прошлом раунде
                if (player1 != null && currentPlayer != null) {
                    currentPlayer = currentPlayer.equals(player1.getUUID()) ? player2.getUUID() : player1.getUUID();
                } else if (player1 != null) {
                    currentPlayer = player1.getUUID();
                }
            }
        } else if (player1 != null) {
            // Если нет второго игрока и не villager, всегда ход первого игрока
            currentPlayer = player1.getUUID();
        }
        
        // Запускаем таймер для нового хода
        if (currentPlayer != null) {
            com.bmfalkye.game.TurnTimer.startTurn(this);
        }
        
        // Логируем, чей первый ход в новом раунде
        if (player1 != null) {
            if (isPlayingWithVillager() && isVillagerTurn()) {
                com.bmfalkye.network.NetworkHandler.addActionLog(player1, "§7Раунд " + currentRound + " - Ход жителя");
            } else if (isPlayerTurn(player1)) {
                com.bmfalkye.network.NetworkHandler.addActionLog(player1, "§7Раунд " + currentRound + " - §aВаш ход!");
            } else {
                com.bmfalkye.network.NetworkHandler.addActionLog(player1, "§7Раунд " + currentRound + " - §cХод противника");
            }
        }
        if (player2 != null) {
            if (isPlayerTurn(player2)) {
                com.bmfalkye.network.NetworkHandler.addActionLog(player2, "§7Раунд " + currentRound + " - §aВаш ход!");
            } else {
                com.bmfalkye.network.NetworkHandler.addActionLog(player2, "§7Раунд " + currentRound + " - §cХод противника");
            }
        }
        
        // Обновляем состояние игры после начала нового раунда
        if (player1 != null) {
            com.bmfalkye.network.NetworkHandler.updateGameState(player1, this);
        }
        if (player2 != null) {
            com.bmfalkye.network.NetworkHandler.updateGameState(player2, this);
        }
        
        // Если играем с villager и теперь его ход, делаем ход AI (без блокирующей задержки)
        if (isPlayingWithVillager() && isVillagerTurn()) {
            if (player1 != null && player1.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                // Выполняем ход AI асинхронно без блокирующей задержки
                serverLevel.getServer().execute(() -> {
                    if (!isGameEnded() && !isRoundEnded() && isVillagerTurn()) {
                        com.bmfalkye.game.VillagerAIPlayer.makeAITurn(this);
                        com.bmfalkye.network.NetworkHandler.updateGameState(player1, this);
                    }
                });
            }
        }
    }

    private void endGame() {
        // Очищаем баффы при завершении игры
        if (player1 != null) {
            com.bmfalkye.cards.BuffSystem.clearPlayerBuffs(this, player1);
        }
        if (player2 != null) {
            com.bmfalkye.cards.BuffSystem.clearPlayerBuffs(this, player2);
        }
        String winner = "";
        if (roundsWon1 >= 2) {
            winner = player1 != null ? player1.getName().getString() : "Player1";
        } else if (roundsWon2 >= 2) {
            winner = player2 != null ? player2.getName().getString() : (isPlayingWithVillager() ? "Villager" : "Player2");
        }
        
        ModLogger.logGameEvent("Game ended", 
            "winner", winner,
            "roundsWon1", roundsWon1,
            "roundsWon2", roundsWon2,
            "finalRound", currentRound,
            "player1", player1 != null ? player1.getName().getString() : "null",
            "player2", player2 != null ? player2.getName().getString() : (isPlayingWithVillager() ? "Villager" : "null"));
        
        gameEnded = true;
        roundEnded = true;
        
        // Визуальные эффекты окончания игры будут проиграны на клиенте через GuiEffectManager
        // Не создаем эффекты в мире, чтобы они были в GUI
        
        // Освобождаем жителя
        if (villagerOpponent != null && villagerOpponent instanceof net.minecraft.world.entity.Mob mob) {
            mob.setNoAi(false);
        }
        
        // Обновляем состояние игры после окончания игры
        if (player1 != null) {
            com.bmfalkye.network.NetworkHandler.updateGameState(player1, this);
        }
        if (player2 != null) {
            com.bmfalkye.network.NetworkHandler.updateGameState(player2, this);
        }
        
        // Обрабатываем окончание игры
        com.bmfalkye.game.GameEndHandler.handleGameEnd(this);
    }
    
    /**
     * Принудительно завершить игру (для сдачи или проигрыша по таймауту)
     */
    public void forceGameEnd(ServerPlayer player, boolean playerWon) {
        if (gameEnded) {
            ModLogger.logGameEvent("forceGameEnd failed: game already ended", "player", player != null ? player.getName().getString() : "null");
            return;
        }
        
        ModLogger.logGameEvent("Force game end", 
            "player", player != null ? player.getName().getString() : "null",
            "playerWon", playerWon,
            "round", currentRound,
            "roundsWon1", roundsWon1,
            "roundsWon2", roundsWon2);
        
        gameEnded = true;
        roundEnded = true;
        
        // Определяем, кто сдался: player1 или player2
        boolean isPlayer1 = player1 != null && player.equals(player1);
        boolean isPlayer2 = player2 != null && player.equals(player2);
        
        // Если сдался player1, побеждает player2 (или villager)
        // Если сдался player2, побеждает player1
        if (isPlayer1) {
            // player1 сдался - побеждает player2 (или villager)
            roundsWon2 = 2;
            roundsWon1 = 0;
            ModLogger.logGameEvent("Player1 surrendered, Player2 wins", 
                "player1", player1.getName().getString(),
                "player2", player2 != null ? player2.getName().getString() : (isPlayingWithVillager() ? "Villager" : "null"));
        } else if (isPlayer2) {
            // player2 сдался - побеждает player1
            roundsWon1 = 2;
            roundsWon2 = 0;
            ModLogger.logGameEvent("Player2 surrendered, Player1 wins", 
                "player1", player1 != null ? player1.getName().getString() : "null",
                "player2", player2.getName().getString());
        } else {
            // Если игрок не найден, используем старую логику
            if (playerWon) {
                roundsWon1 = 2;
            } else {
                roundsWon2 = 2;
            }
        }
        
        // Освобождаем жителя
        if (villagerOpponent != null && villagerOpponent instanceof net.minecraft.world.entity.Mob mob) {
            mob.setNoAi(false);
        }
        
        // Обрабатываем окончание игры
        com.bmfalkye.game.GameEndHandler.handleGameEnd(this);
    }

    private void switchTurn() {
        String previousPlayer = currentPlayer != null ? 
            (player1 != null && currentPlayer.equals(player1.getUUID()) ? player1.getName().getString() : 
             (player2 != null && currentPlayer.equals(player2.getUUID()) ? player2.getName().getString() : 
              (isPlayingWithVillager() && villagerOpponent != null && currentPlayer.equals(villagerOpponent.getUUID()) ? "Villager" : "Unknown"))) : "null";
        
        // Сбрасываем флаги сыгранных карт для текущего игрока (который только что закончил свой ход)
        // Это позволяет игроку снова играть картами, когда ход вернется к нему
        if (currentPlayer != null) {
            if (player1 != null && currentPlayer.equals(player1.getUUID())) {
                player1PlayedNormalCard = false;
                player1PlayedAbilityCard = false;
            } else if (player2 != null && currentPlayer.equals(player2.getUUID())) {
                player2PlayedNormalCard = false;
                player2PlayedAbilityCard = false;
            }
        }
        
        // Завершаем таймер предыдущего хода
        com.bmfalkye.game.TurnTimer.endTurn(this);
        
        // Если currentPlayer null, устанавливаем его на player1
        if (currentPlayer == null && player1 != null) {
            currentPlayer = player1.getUUID();
        }
        
        if (isPlayingWithVillager()) {
            // Для villager переключаем между игроком и villager
            if (player1Passed && !player2Passed) {
                // Ход villager
                if (villagerOpponent != null) {
                    currentPlayer = villagerOpponent.getUUID();
                }
            } else if (player2Passed && !player1Passed) {
                if (player1 != null) {
                    currentPlayer = player1.getUUID();
                }
            } else {
                // Переключаем между игроком и villager
                if (player1 != null && currentPlayer != null && currentPlayer.equals(player1.getUUID())) {
                    if (villagerOpponent != null) {
                        currentPlayer = villagerOpponent.getUUID();
                    }
                } else {
                    if (player1 != null) {
                        currentPlayer = player1.getUUID();
                    }
                }
            }
        } else {
            if (player1Passed && !player2Passed && player2 != null) {
                currentPlayer = player2.getUUID();
            } else if (player2Passed && !player1Passed) {
                if (player1 != null) {
                    currentPlayer = player1.getUUID();
                }
            } else if (player2 != null && currentPlayer != null) {
                currentPlayer = currentPlayer.equals(player1.getUUID()) ? player2.getUUID() : player1.getUUID();
            } else if (player1 != null && currentPlayer == null) {
                currentPlayer = player1.getUUID();
            }
        }
        
        // Запускаем таймер для нового хода
        if (currentPlayer != null) {
            com.bmfalkye.game.TurnTimer.startTurn(this);
            // Оптимизация: убрано избыточное логирование
        }
        
        // Уведомляем игроков о смене хода через лог действий
        if (player1 != null) {
            if (isPlayingWithVillager() && isVillagerTurn()) {
                com.bmfalkye.network.NetworkHandler.addActionLog(player1, "§7Ход жителя");
            } else if (isPlayerTurn(player1)) {
                com.bmfalkye.network.NetworkHandler.addActionLog(player1, "§aВаш ход!");
            } else {
                com.bmfalkye.network.NetworkHandler.addActionLog(player1, "§cХод противника");
            }
        }
        if (player2 != null) {
            if (isPlayerTurn(player2)) {
                com.bmfalkye.network.NetworkHandler.addActionLog(player2, "§aВаш ход!");
            } else {
                com.bmfalkye.network.NetworkHandler.addActionLog(player2, "§cХод противника");
            }
        }
        
        // Проверяем условия автоматического окончания раунда/игры
        checkAutoEndConditions();
        
        // Обновляем состояние игры после переключения хода (критичное обновление)
        if (player1 != null) {
            com.bmfalkye.network.NetworkHandler.updateGameStateImmediate(player1, this);
        }
        if (player2 != null) {
            com.bmfalkye.network.NetworkHandler.updateGameStateImmediate(player2, this);
        }
        
        // Если играем с villager и теперь его ход, делаем ход AI
        if (isPlayingWithVillager() && isVillagerTurn()) {
            // Выполняем ход AI асинхронно без блокирующей задержки
            if (player1 != null && player1.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.getServer().execute(() -> {
                    if (!isGameEnded() && !isRoundEnded() && isVillagerTurn()) {
                        com.bmfalkye.game.VillagerAIPlayer.makeAITurn(this);
                        com.bmfalkye.network.NetworkHandler.updateGameState(player1, this);
                    }
                });
            }
        }
    }
    
    /**
     * Проверяет условия автоматического окончания раунда/игры
     * Раунд заканчивается только после битвы карт и проверки наличия карт в руках
     */
    private void checkAutoEndConditions() {
        if (roundEnded || gameEnded) {
            return;
        }
        
        // Проверяем, что это 3 раунд
        if (currentRound != 3) {
            return;
        }
        
        // Проверяем, что у обоих игроков закончились карты в колоде
        boolean deck1Empty = deck1 == null || deck1.isEmpty();
        boolean deck2Empty = deck2 == null || deck2.isEmpty();
        
        // Проверяем, что у обоих игроков закончились карты в руке
        boolean hand1Empty = hand1 == null || hand1.isEmpty();
        boolean hand2Empty = hand2 == null || hand2.isEmpty();
        
        // Если у обоих игроков закончились карты в колоде и руке
        if (deck1Empty && deck2Empty && hand1Empty && hand2Empty) {
            // Проверяем, пасовали ли оба игрока
            if (player1Passed && player2Passed) {
                // Проверяем, выкинули ли игроки карты в этом раунде
                boolean player1PlayedCards = player1PlayedNormalCard || player1PlayedAbilityCard;
                boolean player2PlayedCards = player2PlayedNormalCard || player2PlayedAbilityCard;
                
                // Если оба пасовали и не выкинули ни одной карты - ничья
                if (!player1PlayedCards && !player2PlayedCards) {
                    if (player1 != null) {
                        com.bmfalkye.network.NetworkHandler.addActionLog(player1, 
                            "§7У обоих игроков закончились карты. Раунд завершён ничьей.");
                    }
                    if (player2 != null) {
                        com.bmfalkye.network.NetworkHandler.addActionLog(player2, 
                            "§7У обоих игроков закончились карты. Раунд завершён ничьей.");
                    }
                    endRound();
                } else {
                    // Проводим битву карт и затем заканчиваем раунд
                    resolveCardBattles();
                    endRound();
                }
            }
        }
    }

    public boolean isPlayerTurn(ServerPlayer player) {
        if (player == null || currentPlayer == null) return false;
        return player.getUUID().equals(currentPlayer);
    }
    
    public boolean isVillagerTurn() {
        return isPlayingWithVillager() && villagerOpponent != null && 
               currentPlayer != null && villagerOpponent.getUUID().equals(currentPlayer);
    }

    // Геттеры
    public ServerPlayer getPlayer1() { return player1; }
    public ServerPlayer getPlayer2() { return player2; }
    public List<Card> getHand(ServerPlayer player) {
        if (player == null) {
            // Для villager возвращаем hand2
            if (isPlayingWithVillager()) {
                return hand2;
            }
            return new ArrayList<>();
        }
        return player.equals(player1) ? hand1 : hand2;
    }
    public List<Card> getMeleeRow(ServerPlayer player) {
        if (player == null && isPlayingWithVillager()) {
            return meleeRow2;
        }
        if (player == null) return new ArrayList<>();
        return player.equals(player1) ? meleeRow1 : meleeRow2;
    }
    
    public List<Card> getRangedRow(ServerPlayer player) {
        if (player == null && isPlayingWithVillager()) {
            return rangedRow2;
        }
        if (player == null) return new ArrayList<>();
        return player.equals(player1) ? rangedRow1 : rangedRow2;
    }
    
    public List<Card> getSiegeRow(ServerPlayer player) {
        if (player == null && isPlayingWithVillager()) {
            return siegeRow2;
        }
        if (player == null) return new ArrayList<>();
        return player.equals(player1) ? siegeRow1 : siegeRow2;
    }
    private List<Card> getRow(ServerPlayer player, CardRow row) {
        switch (row) {
            case MELEE: return getMeleeRow(player);
            case RANGED: return getRangedRow(player);
            case SIEGE: return getSiegeRow(player);
            default: return new ArrayList<>();
        }
    }
    public int getRoundScore(ServerPlayer player) {
        if (player == null && isPlayingWithVillager()) {
            return roundScore2;
        }
        if (player == null) return 0;
        return player.equals(player1) ? roundScore1 : roundScore2;
    }
    
    public int getRoundsWon(ServerPlayer player) {
        if (player == null && isPlayingWithVillager()) {
            return roundsWon2;
        }
        if (player == null) return 0;
        return player.equals(player1) ? roundsWon1 : roundsWon2;
    }
    
    /**
     * Получает список использованных карт для игрока
     */
    public List<String> getUsedCards(ServerPlayer player) {
        if (player == null) {
            return new ArrayList<>();
        }
        Set<String> cards = usedCards.get(player.getUUID());
        return cards != null ? new ArrayList<>(cards) : new ArrayList<>();
    }
    
    /**
     * Получает все использованные карты в игре (для обоих игроков)
     */
    public List<String> getAllUsedCards() {
        Set<String> allCards = new HashSet<>();
        for (Set<String> cards : usedCards.values()) {
            allCards.addAll(cards);
        }
        return new ArrayList<>(allCards);
    }
    
    public int getCurrentRound() { return currentRound; }
    public boolean isRoundEnded() { return roundEnded; }
    public boolean isGameEnded() { return gameEnded; }
    public ServerPlayer getWinner() {
        if (roundsWon1 >= 2) return player1;
        if (roundsWon2 >= 2) return player2;
        return null;
    }
    public UUID getCurrentPlayerUUID() { return currentPlayer; }
    public ServerPlayer getCurrentPlayer() {
        if (currentPlayer == null) return null;
        if (isPlayingWithVillager() && villagerOpponent != null && 
            villagerOpponent.getUUID().equals(currentPlayer)) {
            return null; // Ход villager
        }
        if (player1 != null && currentPlayer.equals(player1.getUUID())) {
            return player1;
        }
        return player2;
    }
    public boolean hasPassed(ServerPlayer player) {
        if (player == null && isPlayingWithVillager()) {
            return player2Passed;
        }
        if (player == null) return false;
        return player.equals(player1) ? player1Passed : player2Passed;
    }
    
    public WeatherType getWeather() { return weather; }
    
    public void setWeather(WeatherType weatherType) {
        this.weather = weatherType;
    }
    
    public CardDeck getDeck1() { return deck1; }
    
    public CardDeck getDeck2() { return deck2; }
    
    public LeaderCard getLeader(ServerPlayer player) {
        if (player == null && isPlayingWithVillager()) {
            return leader2;
        }
        if (player == null) return null;
        return player.equals(player1) ? leader1 : leader2;
    }

    public enum CardRow {
        MELEE,    // Ближний бой
        RANGED,   // Дальний бой
        SIEGE     // Осада
    }

    public enum WeatherType {
        NONE,     // Нет погоды
        FROST,    // Мороз (ближний бой)
        FOG,      // Туман (дальний бой)
        RAIN;     // Дождь (осада)
        
        public String getDisplayName() {
            return switch (this) {
                case NONE -> "Нет";
                case FROST -> "Мороз";
                case FOG -> "Туман";
                case RAIN -> "Дождь";
            };
        }
    }
}

