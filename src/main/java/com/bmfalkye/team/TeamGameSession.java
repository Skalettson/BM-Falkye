package com.bmfalkye.team;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardDeck;
import com.bmfalkye.cards.LeaderCard;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Игровая сессия для командного режима 2v2
 * Команды из двух игроков сражаются на общем поле
 */
public class TeamGameSession {
    // Команда 1
    private final ServerPlayer team1Player1;
    private final ServerPlayer team1Player2;
    private final CardDeck team1Deck1;
    private final CardDeck team1Deck2;
    private final LeaderCard team1Leader1;
    private final LeaderCard team1Leader2;
    
    // Команда 2
    private final ServerPlayer team2Player1;
    private final ServerPlayer team2Player2;
    private final CardDeck team2Deck1;
    private final CardDeck team2Deck2;
    private final LeaderCard team2Leader1;
    private final LeaderCard team2Leader2;
    
    // Руки команд (общие)
    private final List<Card> team1Hand;
    private final List<Card> team2Hand;
    
    // Поле (4 ряда: два ближних боя, два дальних)
    private final List<Card> team1MeleeRow1;
    private final List<Card> team1MeleeRow2;
    private final List<Card> team1RangedRow1;
    private final List<Card> team1RangedRow2;
    
    private final List<Card> team2MeleeRow1;
    private final List<Card> team2MeleeRow2;
    private final List<Card> team2RangedRow1;
    private final List<Card> team2RangedRow2;
    
    // Сбросы
    private final List<Card> team1Graveyard;
    private final List<Card> team2Graveyard;
    
    // Текущий ход
    private UUID currentTeam; // UUID первого игрока команды
    private int currentRound = 1;
    private boolean roundEnded = false;
    private boolean gameEnded = false;
    
    // Счёт раундов
    private int team1RoundsWon = 0;
    private int team2RoundsWon = 0;
    
    // Ожидание ходов от обоих игроков команды
    private final Map<UUID, Card> pendingPlays = new HashMap<>();
    private final Map<UUID, Boolean> passedPlayers = new HashMap<>();
    
    public TeamGameSession(ServerPlayer team1Player1, ServerPlayer team1Player2,
                          ServerPlayer team2Player1, ServerPlayer team2Player2,
                          CardDeck team1Deck1, CardDeck team1Deck2,
                          CardDeck team2Deck1, CardDeck team2Deck2,
                          LeaderCard team1Leader1, LeaderCard team1Leader2,
                          LeaderCard team2Leader1, LeaderCard team2Leader2) {
        this.team1Player1 = team1Player1;
        this.team1Player2 = team1Player2;
        this.team1Deck1 = team1Deck1;
        this.team1Deck2 = team1Deck2;
        this.team1Leader1 = team1Leader1;
        this.team1Leader2 = team1Leader2;
        
        this.team2Player1 = team2Player1;
        this.team2Player2 = team2Player2;
        this.team2Deck1 = team2Deck1;
        this.team2Deck2 = team2Deck2;
        this.team2Leader1 = team2Leader1;
        this.team2Leader2 = team2Leader2;
        
        this.team1Hand = new ArrayList<>();
        this.team2Hand = new ArrayList<>();
        
        this.team1MeleeRow1 = new ArrayList<>();
        this.team1MeleeRow2 = new ArrayList<>();
        this.team1RangedRow1 = new ArrayList<>();
        this.team1RangedRow2 = new ArrayList<>();
        
        this.team2MeleeRow1 = new ArrayList<>();
        this.team2MeleeRow2 = new ArrayList<>();
        this.team2RangedRow1 = new ArrayList<>();
        this.team2RangedRow2 = new ArrayList<>();
        
        this.team1Graveyard = new ArrayList<>();
        this.team2Graveyard = new ArrayList<>();
        
        this.currentTeam = team1Player1 != null ? team1Player1.getUUID() : null;
        
        // Раздаём начальные карты
        dealInitialCards();
    }
    
    /**
     * Раздаёт начальные карты командам
     */
    private void dealInitialCards() {
        // Команда 1 получает карты из обеих колод
        for (int i = 0; i < 5 && i < team1Deck1.getCards().size(); i++) {
            team1Hand.add(team1Deck1.getCards().get(i));
        }
        for (int i = 0; i < 5 && i < team1Deck2.getCards().size(); i++) {
            team1Hand.add(team1Deck2.getCards().get(i));
        }
        
        // Команда 2 получает карты из обеих колод
        for (int i = 0; i < 5 && i < team2Deck1.getCards().size(); i++) {
            team2Hand.add(team2Deck1.getCards().get(i));
        }
        for (int i = 0; i < 5 && i < team2Deck2.getCards().size(); i++) {
            team2Hand.add(team2Deck2.getCards().get(i));
        }
    }
    
    /**
     * Проверяет, является ли игрок частью команды 1
     */
    public boolean isTeam1Player(ServerPlayer player) {
        return (team1Player1 != null && team1Player1.getUUID().equals(player.getUUID())) ||
               (team1Player2 != null && team1Player2.getUUID().equals(player.getUUID()));
    }
    
    /**
     * Проверяет, является ли игрок частью команды 2
     */
    public boolean isTeam2Player(ServerPlayer player) {
        return (team2Player1 != null && team2Player1.getUUID().equals(player.getUUID())) ||
               (team2Player2 != null && team2Player2.getUUID().equals(player.getUUID()));
    }
    
    /**
     * Проверяет, чей ход
     */
    public boolean isTeamTurn(ServerPlayer player) {
        if (currentTeam == null) return false;
        if (isTeam1Player(player)) {
            return currentTeam.equals(team1Player1 != null ? team1Player1.getUUID() : null);
        } else if (isTeam2Player(player)) {
            return currentTeam.equals(team2Player1 != null ? team2Player1.getUUID() : null);
        }
        return false;
    }
    
    /**
     * Играет карту от игрока
     */
    public boolean playCard(ServerPlayer player, Card card, TeamCardRow row) {
        if (!isTeamTurn(player)) {
            return false;
        }
        
        // Определяем команду
        List<Card> hand = isTeam1Player(player) ? team1Hand : team2Hand;
        
        // Проверяем, есть ли карта в руке
        if (!hand.contains(card)) {
            return false;
        }
        
        // Сохраняем ход
        pendingPlays.put(player.getUUID(), card);
        
        // Удаляем карту из руки
        hand.remove(card);
        
        // Размещаем на поле
        List<Card> targetRow = getRow(isTeam1Player(player), row);
        targetRow.add(card);
        
        // Проверяем, все ли игроки команды сделали ход
        checkTeamTurnComplete(isTeam1Player(player));
        
        return true;
    }
    
    /**
     * Получает ряд для размещения карты
     */
    private List<Card> getRow(boolean isTeam1, TeamCardRow row) {
        if (isTeam1) {
            switch (row) {
                case MELEE_1: return team1MeleeRow1;
                case MELEE_2: return team1MeleeRow2;
                case RANGED_1: return team1RangedRow1;
                case RANGED_2: return team1RangedRow2;
            }
        } else {
            switch (row) {
                case MELEE_1: return team2MeleeRow1;
                case MELEE_2: return team2MeleeRow2;
                case RANGED_1: return team2RangedRow1;
                case RANGED_2: return team2RangedRow2;
            }
        }
        return new ArrayList<>();
    }
    
    /**
     * Проверяет, завершён ли ход команды
     */
    private void checkTeamTurnComplete(boolean isTeam1) {
        UUID player1UUID = isTeam1 ? 
            (team1Player1 != null ? team1Player1.getUUID() : null) :
            (team2Player1 != null ? team2Player1.getUUID() : null);
        UUID player2UUID = isTeam1 ?
            (team1Player2 != null ? team1Player2.getUUID() : null) :
            (team2Player2 != null ? team2Player2.getUUID() : null);
        
        boolean player1Played = player1UUID != null && pendingPlays.containsKey(player1UUID);
        boolean player2Played = player2UUID != null && pendingPlays.containsKey(player2UUID);
        boolean player1Passed = player1UUID != null && passedPlayers.getOrDefault(player1UUID, false);
        boolean player2Passed = player2UUID != null && passedPlayers.getOrDefault(player2UUID, false);
        
        if ((player1Played || player1Passed) && (player2Played || player2Passed)) {
            // Оба игрока команды сделали ход - переключаем ход
            switchTurn();
            pendingPlays.clear();
            passedPlayers.clear();
        }
    }
    
    /**
     * Переключает ход
     */
    private void switchTurn() {
        if (currentTeam != null && currentTeam.equals(team1Player1 != null ? team1Player1.getUUID() : null)) {
            currentTeam = team2Player1 != null ? team2Player1.getUUID() : null;
        } else {
            currentTeam = team1Player1 != null ? team1Player1.getUUID() : null;
        }
    }
    
    /**
     * Пас игрока
     */
    public void pass(ServerPlayer player) {
        if (!isTeamTurn(player)) {
            return;
        }
        
        passedPlayers.put(player.getUUID(), true);
        checkTeamTurnComplete(isTeam1Player(player));
    }
    
    /**
     * Вычисляет силу команды
     */
    public int calculateTeamPower(boolean isTeam1) {
        int power = 0;
        List<List<Card>> rows = isTeam1 ?
            Arrays.asList(team1MeleeRow1, team1MeleeRow2, team1RangedRow1, team1RangedRow2) :
            Arrays.asList(team2MeleeRow1, team2MeleeRow2, team2RangedRow1, team2RangedRow2);
        
        for (List<Card> row : rows) {
            for (Card card : row) {
                power += card.getPower();
            }
        }
        
        return power;
    }
    
    /**
     * Ряды для командного режима
     */
    public enum TeamCardRow {
        MELEE_1, MELEE_2, RANGED_1, RANGED_2
    }
    
    // Геттеры
    public List<Card> getTeam1Hand() { return team1Hand; }
    public List<Card> getTeam2Hand() { return team2Hand; }
    public int getTeam1RoundsWon() { return team1RoundsWon; }
    public int getTeam2RoundsWon() { return team2RoundsWon; }
    public int getCurrentRound() { return currentRound; }
    public boolean isGameEnded() { return gameEnded; }
}

