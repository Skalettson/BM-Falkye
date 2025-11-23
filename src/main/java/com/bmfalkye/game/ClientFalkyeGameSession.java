package com.bmfalkye.game;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Клиентская версия FalkyeGameSession для отображения в GUI
 * ВАЖНО: Этот класс использует клиентские классы и должен использоваться только на клиенте
 */
@OnlyIn(Dist.CLIENT)
public class ClientFalkyeGameSession {
    private final UUID player1UUID;
    private final UUID player2UUID;
    private final List<String> hand1Ids;
    private final List<String> hand2Ids;
    private final List<String> melee1Ids, ranged1Ids, siege1Ids;
    private final List<String> melee2Ids, ranged2Ids, siege2Ids;
    private final List<String> graveyard1Ids, graveyard2Ids;
    private final int roundScore1, roundScore2;
    private final int currentRound;
    private final UUID currentPlayerUUID;
    private final FalkyeGameSession.WeatherType weather;
    private final String leader1Id, leader2Id;
    private final int roundsWon1, roundsWon2;
    private final boolean player1Passed, player2Passed;
    private final boolean isPlayer2AI;
    private final int remainingTime; // Оставшееся время хода в секундах
    private final int timeoutCount; // Счётчик пропусков по таймауту
    private final java.util.Map<String, Integer> powerModifiers; // Модификаторы силы карт (cardId -> modifier)
    private final List<String> revealedCards; // Показанные карты оппонента (ID карт)
    private final com.bmfalkye.game.LocationEffect.LocationType locationType; // Тип локации
    private final com.bmfalkye.settings.GameModeSettings.GameMode gameMode; // Режим отображения игры

    public ClientFalkyeGameSession(UUID player1UUID, UUID player2UUID,
                                  int score1, int score2, int currentRound, UUID currentPlayerUUID,
                                  List<String> hand1Ids, List<String> hand2Ids,
                                  List<String> meleeRow1Ids, List<String> rangedRow1Ids, List<String> siegeRow1Ids,
                                  List<String> meleeRow2Ids, List<String> rangedRow2Ids, List<String> siegeRow2Ids,
                                  List<String> graveyard1Ids, List<String> graveyard2Ids,
                                  FalkyeGameSession.WeatherType weather, String leader1Id, String leader2Id,
                                  int roundsWon1, int roundsWon2,
                                  boolean player1Passed, boolean player2Passed,
                                  boolean isPlayer2AI, int remainingTime, int timeoutCount,
                                  java.util.Map<String, Integer> powerModifiers,
                                  List<String> revealedCards,
                                  com.bmfalkye.game.LocationEffect.LocationType locationType,
                                  com.bmfalkye.settings.GameModeSettings.GameMode gameMode) {
        this.player1UUID = player1UUID;
        this.player2UUID = player2UUID;
        this.hand1Ids = hand1Ids;
        this.hand2Ids = hand2Ids;
        this.melee1Ids = meleeRow1Ids;
        this.ranged1Ids = rangedRow1Ids;
        this.siege1Ids = siegeRow1Ids;
        this.melee2Ids = meleeRow2Ids;
        this.ranged2Ids = rangedRow2Ids;
        this.siege2Ids = siegeRow2Ids;
        this.graveyard1Ids = graveyard1Ids;
        this.graveyard2Ids = graveyard2Ids;
        this.roundScore1 = score1;
        this.roundScore2 = score2;
        this.currentRound = currentRound;
        this.currentPlayerUUID = currentPlayerUUID;
        this.weather = weather;
        this.leader1Id = leader1Id;
        this.leader2Id = leader2Id;
        this.roundsWon1 = roundsWon1;
        this.roundsWon2 = roundsWon2;
        this.player1Passed = player1Passed;
        this.player2Passed = player2Passed;
        this.isPlayer2AI = isPlayer2AI;
        this.remainingTime = remainingTime;
        this.timeoutCount = timeoutCount;
        this.powerModifiers = powerModifiers != null ? new java.util.HashMap<>(powerModifiers) : new java.util.HashMap<>();
        this.revealedCards = revealedCards != null ? new ArrayList<>(revealedCards) : new ArrayList<>();
        this.locationType = locationType != null ? locationType : com.bmfalkye.game.LocationEffect.LocationType.NONE;
        this.gameMode = gameMode != null ? gameMode : com.bmfalkye.settings.GameModeSettings.GameMode.MODE_2D;
    }
    
    /**
     * Получает эффективную силу карты с учётом модификаторов
     * ВАЖНО: Модификаторы применяются ко ВСЕМ картам на поле, чтобы оба игрока видели изменения
     */
    public int getEffectivePower(Card card) {
        if (card == null) return 0;
        int basePower = card.getPower();
        
        // Проверяем, находится ли карта на поле (у любого игрока)
        boolean isOnField = melee1Ids.contains(card.getId()) || 
                           ranged1Ids.contains(card.getId()) || 
                           siege1Ids.contains(card.getId()) ||
                           melee2Ids.contains(card.getId()) || 
                           ranged2Ids.contains(card.getId()) || 
                           siege2Ids.contains(card.getId());
        
        // Применяем модификатор ко всем картам на поле
        // Это позволяет обоим игрокам видеть изменения силы карт друг друга
        int modifier = 0;
        if (isOnField) {
            modifier = powerModifiers.getOrDefault(card.getId(), 0);
        }
        
        return Math.max(0, basePower + modifier); // Сила не может быть отрицательной
    }
    
    /**
     * Получает модификатор силы для карты
     */
    public int getPowerModifier(String cardId) {
        return powerModifiers.getOrDefault(cardId, 0);
    }
    
    /**
     * Получает все модификаторы силы (для сериализации)
     */
    public java.util.Map<String, Integer> getPowerModifiers() {
        return new java.util.HashMap<>(powerModifiers);
    }
    
    public int getRemainingTime() {
        return remainingTime;
    }
    
    public int getTimeoutCount() {
        return timeoutCount;
    }

    public ServerPlayer getPlayer1() { 
        return null; // На клиенте нет ServerPlayer
    }
    
    public ServerPlayer getPlayer2() { 
        return null; // На клиенте нет ServerPlayer
    }

    public List<Card> getHand(ServerPlayer player) {
        UUID currentUUID = getLocalPlayerUUID();
        // Если player == null, используем UUID текущего игрока
        if (player == null) {
            if (currentUUID != null && currentUUID.equals(player1UUID)) {
                // Текущий игрок - player1, возвращаем его руку
                return getCardsByIds(hand1Ids);
            } else if (currentUUID != null && currentUUID.equals(player2UUID)) {
                // Текущий игрок - player2, возвращаем его руку
                return getCardsByIds(hand2Ids);
            }
            // Если UUID не найден, возвращаем руку первого игрока по умолчанию
            // (это может быть villager, если играем с villager и player2UUID - это UUID villager)
            return getCardsByIds(hand1Ids);
        }
        // Если player указан, используем его UUID (для совместимости)
        UUID playerUUID = player.getUUID();
        if (playerUUID.equals(player1UUID)) {
            return getCardsByIds(hand1Ids);
        } else if (playerUUID.equals(player2UUID)) {
            return getCardsByIds(hand2Ids);
        }
        return new ArrayList<>();
    }
    
    /**
     * Получает руку противника (для отображения рубашками)
     */
    public List<String> getOpponentHandIds() {
        UUID currentUUID = getLocalPlayerUUID();
        if (currentUUID != null && currentUUID.equals(player1UUID)) {
            return hand2Ids;
        } else if (currentUUID != null && currentUUID.equals(player2UUID)) {
            return hand1Ids;
        }
        return new ArrayList<>();
    }
    
    /**
     * Получает количество карт в руке противника
     */
    public int getOpponentHandSize() {
        return getOpponentHandIds().size();
    }
    
    @OnlyIn(Dist.CLIENT)
    private UUID getLocalPlayerUUID() {
        try {
            return net.minecraft.client.Minecraft.getInstance().player != null ?
                net.minecraft.client.Minecraft.getInstance().player.getUUID() : null;
        } catch (Exception e) {
            return null; // На сервере вернет null
        }
    }

    public List<Card> getMeleeRow(ServerPlayer player) {
        UUID currentUUID = getLocalPlayerUUID();
        if (currentUUID != null && currentUUID.equals(player1UUID)) {
            return getCardsByIds(melee1Ids);
        } else if (currentUUID != null && currentUUID.equals(player2UUID)) {
            return getCardsByIds(melee2Ids);
        }
        return new ArrayList<>();
    }

    public List<Card> getRangedRow(ServerPlayer player) {
        UUID currentUUID = getLocalPlayerUUID();
        if (currentUUID != null && currentUUID.equals(player1UUID)) {
            return getCardsByIds(ranged1Ids);
        } else if (currentUUID != null && currentUUID.equals(player2UUID)) {
            return getCardsByIds(ranged2Ids);
        }
        return new ArrayList<>();
    }

    public List<Card> getSiegeRow(ServerPlayer player) {
        UUID currentUUID = getLocalPlayerUUID();
        if (currentUUID != null && currentUUID.equals(player1UUID)) {
            return getCardsByIds(siege1Ids);
        } else if (currentUUID != null && currentUUID.equals(player2UUID)) {
            return getCardsByIds(siege2Ids);
        }
        return new ArrayList<>();
    }

    private List<Card> getCardsByIds(List<String> ids) {
        List<Card> cards = new ArrayList<>();
        for (String id : ids) {
            Card card = CardRegistry.getCard(id);
            if (card != null) {
                cards.add(card);
            }
        }
        return cards;
    }

    public UUID getPlayer1UUID() { return player1UUID; }
    public UUID getPlayer2UUID() { return player2UUID; }
    
    // Геттеры для внутренних полей (для сериализации)
    public List<String> getHand1Ids() { return hand1Ids; }
    public List<String> getHand2Ids() { return hand2Ids; }
    public List<String> getMelee1Ids() { return melee1Ids; }
    public List<String> getRanged1Ids() { return ranged1Ids; }
    public List<String> getSiege1Ids() { return siege1Ids; }
    public List<String> getMelee2Ids() { return melee2Ids; }
    public List<String> getRanged2Ids() { return ranged2Ids; }
    public List<String> getSiege2Ids() { return siege2Ids; }
    public int getRoundScore1() { return roundScore1; }
    public int getRoundScore2() { return roundScore2; }
    public int getRoundsWon1() { return roundsWon1; }
    public int getRoundsWon2() { return roundsWon2; }
    public boolean getPlayer1Passed() { return player1Passed; }
    public boolean getPlayer2Passed() { return player2Passed; }
    public boolean getIsPlayer2AI() { return isPlayer2AI; }
    
    public int getRoundScore(ServerPlayer player) {
        UUID currentUUID = getLocalPlayerUUID();
        if (currentUUID != null && currentUUID.equals(player1UUID)) {
            return roundScore1;
        } else if (currentUUID != null && currentUUID.equals(player2UUID)) {
            return roundScore2;
        }
        return 0;
    }
    
    public int getRoundsWon(ServerPlayer player) {
        UUID currentUUID = getLocalPlayerUUID();
        if (currentUUID != null && currentUUID.equals(player1UUID)) {
            return roundsWon1;
        } else if (currentUUID != null && currentUUID.equals(player2UUID)) {
            return roundsWon2;
        }
        return 0;
    }
    
    /**
     * Получает очки противника в текущем раунде
     */
    public int getOpponentRoundScore() {
        UUID currentUUID = getLocalPlayerUUID();
        if (currentUUID != null && currentUUID.equals(player1UUID)) {
            return roundScore2;
        } else if (currentUUID != null && currentUUID.equals(player2UUID)) {
            return roundScore1;
        }
        return 0;
    }
    
    /**
     * Получает количество выигранных раундов противника
     */
    public int getOpponentRoundsWon() {
        UUID currentUUID = getLocalPlayerUUID();
        if (currentUUID != null && currentUUID.equals(player1UUID)) {
            return roundsWon2;
        } else if (currentUUID != null && currentUUID.equals(player2UUID)) {
            return roundsWon1;
        }
        return 0;
    }
    
    /**
     * Получает ряды противника
     */
    public List<Card> getOpponentMeleeRow() {
        UUID currentUUID = getLocalPlayerUUID();
        if (currentUUID != null && currentUUID.equals(player1UUID)) {
            return getCardsByIds(melee2Ids);
        } else if (currentUUID != null && currentUUID.equals(player2UUID)) {
            return getCardsByIds(melee1Ids);
        }
        return new ArrayList<>();
    }
    
    public List<Card> getOpponentRangedRow() {
        UUID currentUUID = getLocalPlayerUUID();
        if (currentUUID != null && currentUUID.equals(player1UUID)) {
            return getCardsByIds(ranged2Ids);
        } else if (currentUUID != null && currentUUID.equals(player2UUID)) {
            return getCardsByIds(ranged1Ids);
        }
        return new ArrayList<>();
    }
    
    public List<Card> getOpponentSiegeRow() {
        UUID currentUUID = getLocalPlayerUUID();
        if (currentUUID != null && currentUUID.equals(player1UUID)) {
            return getCardsByIds(siege2Ids);
        } else if (currentUUID != null && currentUUID.equals(player2UUID)) {
            return getCardsByIds(siege1Ids);
        }
        return new ArrayList<>();
    }
    
    public int getCurrentRound() { return currentRound; }
    public UUID getCurrentPlayerUUID() { return currentPlayerUUID; }
    public FalkyeGameSession.WeatherType getWeather() { return weather; }
    public boolean hasPassed(ServerPlayer player) {
        UUID currentUUID = getLocalPlayerUUID();
        if (currentUUID != null && currentUUID.equals(player1UUID)) {
            return player1Passed;
        } else if (currentUUID != null && currentUUID.equals(player2UUID)) {
            return player2Passed;
        }
        return false;
    }
    
    public boolean isRoundEnded() {
        return player1Passed && player2Passed;
    }
    
    public boolean isGameEnded() {
        return roundsWon1 >= 2 || roundsWon2 >= 2;
    }
    
    public UUID getWinnerUUID() {
        if (roundsWon1 >= 2) {
            return player1UUID;
        } else if (roundsWon2 >= 2) {
            return player2UUID;
        }
        return null;
    }
    
    public boolean isPlayingWithVillager() {
        return isPlayer2AI;
    }
    
    public com.bmfalkye.cards.LeaderCard getLeader(ServerPlayer player) {
        UUID currentUUID = getLocalPlayerUUID();
        if (currentUUID != null && currentUUID.equals(player1UUID)) {
            return com.bmfalkye.cards.LeaderRegistry.getLeader(leader1Id);
        } else if (currentUUID != null && currentUUID.equals(player2UUID)) {
            return com.bmfalkye.cards.LeaderRegistry.getLeader(leader2Id);
        }
        return null;
    }
    
    /**
     * Получает список ID показанных карт оппонента
     */
    public List<String> getRevealedCards() {
        return new ArrayList<>(revealedCards);
    }
    
    /**
     * Проверяет, является ли карта показанной
     */
    public boolean isCardRevealed(String cardId) {
        return revealedCards.contains(cardId);
    }
    
    /**
     * Получает тип локации
     */
    public com.bmfalkye.game.LocationEffect.LocationType getLocationType() {
        return locationType;
    }
    
    /**
     * Получает режим отображения игры
     */
    public com.bmfalkye.settings.GameModeSettings.GameMode getGameMode() {
        return gameMode;
    }
}
