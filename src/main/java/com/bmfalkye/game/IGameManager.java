package com.bmfalkye.game;

import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

/**
 * Интерфейс для управления игровыми сессиями
 */
public interface IGameManager {
    /**
     * Проверяет, есть ли у игрока активная игра
     */
    boolean hasActiveGame(ServerPlayer player);
    
    /**
     * Получает активную игровую сессию игрока
     */
    FalkyeGameSession getActiveGame(ServerPlayer player);
    
    /**
     * Отправляет вызов на дуэль
     */
    void sendDuelChallenge(ServerPlayer challenger, ServerPlayer target);
    
    /**
     * Начинает матч с конфигурацией
     */
    void startMatchWithConfig(ServerPlayer player, UUID opponentUUID, MatchConfig config);
    
    /**
     * Завершает игровую сессию
     */
    void endGame(FalkyeGameSession session);
}

