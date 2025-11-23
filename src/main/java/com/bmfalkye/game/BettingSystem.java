package com.bmfalkye.game;

import com.bmfalkye.storage.PlayerCurrency;
import com.bmfalkye.util.ModLogger;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Система ставок между игроками
 */
public class BettingSystem {
    
    /**
     * Блокирует ставку перед началом матча (с защитой от эксплойтов)
     */
    public static boolean lockBet(ServerPlayer player, int betAmount, ServerLevel level) {
        if (betAmount <= 0) {
            return true; // Нет ставки
        }
        
        // Используем систему защиты от эксплойтов
        if (!BetProtectionSystem.lockBetSafely(player, betAmount)) {
            return false;
        }
        
        // Проверяем, что ставка соответствует заблокированной
        if (!BetProtectionSystem.validateBetAmount(player, betAmount)) {
            BetProtectionSystem.unlockBet(player);
            player.sendSystemMessage(Component.literal("§cОшибка валидации ставки. Попробуйте ещё раз."));
            return false;
        }
        
        PlayerCurrency currency = PlayerCurrency.get(level);
        
        // Проверяем баланс (с учётом уже заблокированных ставок)
        int lockedBet = BetProtectionSystem.getLockedBetAmount(player);
        int availableCoins = currency.getCoins(player);
        
        // Если ставка уже была заблокирована ранее, проверяем, что у нас достаточно монет
        if (lockedBet > 0 && lockedBet != betAmount) {
            // Ставка изменилась - нужно вернуть старую и заблокировать новую
            currency.addCoins(player, lockedBet);
            BetProtectionSystem.unlockBet(player);
            if (!BetProtectionSystem.lockBetSafely(player, betAmount)) {
                return false;
            }
        }
        
        if (availableCoins < betAmount) {
            BetProtectionSystem.unlockBet(player);
            player.sendSystemMessage(Component.literal("§cУ вас недостаточно монет для ставки! Нужно: " + betAmount + ", у вас: " + availableCoins));
            return false;
        }
        
        // Блокируем монеты
        if (!currency.removeCoins(player, betAmount)) {
            BetProtectionSystem.unlockBet(player);
            player.sendSystemMessage(Component.literal("§cОшибка при блокировке монет. Попробуйте ещё раз."));
            return false;
        }
        
        ModLogger.logGameEvent("Bet locked", "player", player.getName().getString(), "amount", betAmount);
        return true;
    }
    
    /**
     * Выплачивает выигрыш победителю (с защитой от эксплойтов)
     */
    public static void payWinner(ServerPlayer winner, ServerPlayer loser, int betAmount, ServerLevel level) {
        if (betAmount <= 0) {
            return; // Нет ставки
        }
        
        // Валидация входных данных
        if (winner == null || loser == null || level == null) {
            ModLogger.warn("Invalid parameters in payWinner", 
                "winner", winner != null ? winner.getName().getString() : "null",
                "loser", loser != null ? loser.getName().getString() : "null");
            return;
        }
        
        // Проверяем, что ставка валидна
        if (!BetProtectionSystem.isValidBetAmount(betAmount)) {
            ModLogger.warn("Invalid bet amount in payWinner", "amount", betAmount);
            return;
        }
        
        // Проверяем, что ставки были заблокированы
        int winnerLockedBet = BetProtectionSystem.getLockedBetAmount(winner);
        int loserLockedBet = BetProtectionSystem.getLockedBetAmount(loser);
        
        if (winnerLockedBet != betAmount || loserLockedBet != betAmount) {
            ModLogger.warn("Bet amount mismatch in payWinner",
                "winnerLocked", winnerLockedBet,
                "loserLocked", loserLockedBet,
                "expected", betAmount);
            // Продолжаем, но логируем предупреждение
        }
        
        PlayerCurrency currency = PlayerCurrency.get(level);
        
        // Победитель получает свою ставку обратно + ставку проигравшего
        int winnings = betAmount * 2;
        
        // Проверяем на переполнение
        int winnerBalance = currency.getCoins(winner);
        if (winnerBalance > Integer.MAX_VALUE - winnings) {
            ModLogger.warn("Potential integer overflow in payWinner",
                "winnerBalance", winnerBalance,
                "winnings", winnings);
            winnings = Integer.MAX_VALUE - winnerBalance;
        }
        
        currency.addCoins(winner, winnings);
        
        // Разблокируем ставки
        BetProtectionSystem.unlockBet(winner);
        BetProtectionSystem.unlockBet(loser);
        
        winner.sendSystemMessage(Component.literal("§aВы выиграли " + winnings + " монет!"));
        loser.sendSystemMessage(Component.literal("§cВы проиграли " + betAmount + " монет."));
        
        ModLogger.logGameEvent("Bet paid", "winner", winner.getName().getString(), "winnings", winnings);
    }
    
    /**
     * Возвращает ставку при ничьей (с защитой от эксплойтов)
     */
    public static void refundBet(ServerPlayer player1, ServerPlayer player2, int betAmount, ServerLevel level) {
        if (betAmount <= 0) {
            return;
        }
        
        // Валидация входных данных
        if (player1 == null || player2 == null || level == null) {
            ModLogger.warn("Invalid parameters in refundBet");
            return;
        }
        
        // Проверяем, что ставка валидна
        if (!BetProtectionSystem.isValidBetAmount(betAmount)) {
            ModLogger.warn("Invalid bet amount in refundBet", "amount", betAmount);
            return;
        }
        
        PlayerCurrency currency = PlayerCurrency.get(level);
        
        // Проверяем на переполнение
        int player1Balance = currency.getCoins(player1);
        int player2Balance = currency.getCoins(player2);
        
        if (player1Balance > Integer.MAX_VALUE - betAmount) {
            betAmount = Integer.MAX_VALUE - player1Balance;
        }
        if (player2Balance > Integer.MAX_VALUE - betAmount) {
            betAmount = Math.min(betAmount, Integer.MAX_VALUE - player2Balance);
        }
        
        currency.addCoins(player1, betAmount);
        currency.addCoins(player2, betAmount);
        
        // Разблокируем ставки
        BetProtectionSystem.unlockBet(player1);
        BetProtectionSystem.unlockBet(player2);
        
        player1.sendSystemMessage(Component.literal("§eСтавка возвращена из-за ничьей."));
        player2.sendSystemMessage(Component.literal("§eСтавка возвращена из-за ничьей."));
        
        ModLogger.logGameEvent("Bet refunded", "player1", player1.getName().getString(), 
            "player2", player2.getName().getString(), "amount", betAmount);
    }
}

