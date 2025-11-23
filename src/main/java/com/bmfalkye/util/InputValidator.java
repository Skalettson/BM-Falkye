package com.bmfalkye.util;

import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

/**
 * Валидатор входных данных для проверки корректности действий игроков
 * С улучшенным логированием ошибок валидации
 */
public class InputValidator {
    
    /**
     * Валидирует UUID игрока с логированием
     */
    public static boolean isValidPlayerUUID(UUID uuid) {
        if (uuid == null) {
            ModLogger.warn("Invalid player UUID: null");
            return false;
        }
        return true;
    }
    
    /**
     * Валидирует UUID игрока с логированием и контекстом
     */
    public static boolean isValidPlayerUUID(UUID uuid, String context) {
        if (uuid == null) {
            ModLogger.warn("Invalid player UUID", "context", context != null ? context : "unknown");
            return false;
        }
        return true;
    }
    
    /**
     * Валидирует имя игрока с логированием
     */
    public static boolean isValidPlayerName(String name) {
        if (name == null) {
            ModLogger.warn("Invalid player name: null");
            return false;
        }
        if (name.trim().isEmpty()) {
            ModLogger.warn("Invalid player name: empty");
            return false;
        }
        if (name.length() > 16) {
            ModLogger.warn("Invalid player name: too long", "length", String.valueOf(name.length()), "max", "16");
            return false;
        }
        return true;
    }
    
    /**
     * Валидирует ID карты с логированием
     */
    public static boolean isValidCardId(String cardId) {
        if (cardId == null) {
            ModLogger.warn("Invalid card ID: null");
            return false;
        }
        if (cardId.trim().isEmpty()) {
            ModLogger.warn("Invalid card ID: empty");
            return false;
        }
        if (cardId.length() > 100) {
            ModLogger.warn("Invalid card ID: too long", "length", String.valueOf(cardId.length()), "max", "100", "cardId", cardId.substring(0, Math.min(50, cardId.length())));
            return false;
        }
        return true;
    }
    
    /**
     * Валидирует ID карты с логированием и контекстом
     */
    public static boolean isValidCardId(String cardId, String context) {
        if (!isValidCardId(cardId)) {
            ModLogger.warn("Invalid card ID", "context", context != null ? context : "unknown", "cardId", cardId);
            return false;
        }
        return true;
    }
    
    /**
     * Валидирует ID колоды
     */
    public static boolean isValidDeckName(String deckName) {
        return deckName != null && !deckName.trim().isEmpty() && deckName.length() <= 50;
    }
    
    /**
     * Валидирует количество монет
     */
    public static boolean isValidCoinAmount(int amount) {
        return amount >= 0 && amount <= 1000000; // Максимум 1 миллион
    }
    
    /**
     * Валидирует ставку с логированием (использует BetProtectionSystem для проверки лимитов)
     */
    public static boolean isValidBet(int bet) {
        // Используем систему защиты для проверки лимитов
        if (!com.bmfalkye.game.BetProtectionSystem.isValidBetAmount(bet)) {
            ModLogger.warn("Invalid bet amount", "bet", bet, 
                "min", com.bmfalkye.game.BetProtectionSystem.getMinBetAmount(),
                "max", com.bmfalkye.game.BetProtectionSystem.getMaxBetAmount());
            return false;
        }
        return true;
    }
    
    /**
     * Валидирует ставку (старый метод для обратной совместимости)
     */
    public static boolean isValidBetOld(int bet) {
        if (bet < 0) {
            ModLogger.warn("Invalid bet: negative", "bet", String.valueOf(bet));
            return false;
        }
        if (bet > 100000) {
            ModLogger.warn("Invalid bet: too large", "bet", String.valueOf(bet), "max", "100000");
            return false;
        }
        return true;
    }
    
    /**
     * Валидирует индекс карты в руке
     */
    public static boolean isValidHandIndex(int index, int handSize) {
        return index >= 0 && index < handSize;
    }
    
    /**
     * Валидирует ряд карт с логированием
     */
    public static boolean isValidCardRow(int row) {
        if (row < 0 || row > 2) {
            ModLogger.warn("Invalid card row", "row", String.valueOf(row), "validRange", "0-2");
            return false;
        }
        return true;
    }
    
    /**
     * Валидирует, что игрок онлайн и жив с логированием
     */
    public static boolean isPlayerValid(ServerPlayer player) {
        if (player == null) {
            ModLogger.warn("Invalid player: null");
            return false;
        }
        if (!player.isAlive()) {
            ModLogger.warn("Invalid player: not alive", "player", player.getName().getString());
            return false;
        }
        if (player.isDeadOrDying()) {
            ModLogger.warn("Invalid player: dead or dying", "player", player.getName().getString());
            return false;
        }
        return true;
    }
    
    /**
     * Валидирует, что игрок онлайн и жив с логированием и контекстом
     */
    public static boolean isPlayerValid(ServerPlayer player, String context) {
        if (!isPlayerValid(player)) {
            ModLogger.warn("Invalid player", "context", context != null ? context : "unknown", 
                "player", player != null ? player.getName().getString() : "null");
            return false;
        }
        return true;
    }
    
    /**
     * Валидирует ID турнира
     */
    public static boolean isValidTournamentId(String tournamentId) {
        return tournamentId != null && !tournamentId.trim().isEmpty() && tournamentId.length() <= 50;
    }
    
    /**
     * Валидирует ID события
     */
    public static boolean isValidEventId(String eventId) {
        return eventId != null && !eventId.trim().isEmpty() && eventId.length() <= 50;
    }
    
    /**
     * Валидирует уровень игрока
     */
    public static boolean isValidLevel(int level) {
        return level >= 1 && level <= 50;
    }
    
    /**
     * Валидирует опыт
     */
    public static boolean isValidExperience(int experience) {
        return experience >= 0 && experience <= 10000000; // Максимум 10 миллионов
    }
}

