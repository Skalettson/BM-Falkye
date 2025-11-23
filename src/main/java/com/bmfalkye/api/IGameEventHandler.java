package com.bmfalkye.api;

import com.bmfalkye.game.FalkyeGameSession;
import net.minecraft.server.level.ServerPlayer;

/**
 * Интерфейс для обработки событий игры
 * 
 * <p>Позволяет расширениям реагировать на события игры, такие как начало игры,
 * окончание раунда, победа игрока и т.д.</p>
 * 
 * <p>Пример использования:</p>
 * <pre>{@code
 * public class MyGameEventHandler implements IGameEventHandler {
 *     {@literal @}Override
 *     public void onGameStart(FalkyeGameSession session) {
 *         // Ваш код
 *     }
 * }
 * }</pre>
 * 
 * @author BM Falkye Team
 */
public interface IGameEventHandler {
    
    /**
     * Вызывается при начале игры
     * 
     * @param session игровая сессия
     */
    default void onGameStart(FalkyeGameSession session) {}
    
    /**
     * Вызывается при окончании игры
     * 
     * @param session игровая сессия
     * @param winner победитель (может быть null)
     */
    default void onGameEnd(FalkyeGameSession session, ServerPlayer winner) {}
    
    /**
     * Вызывается при начале раунда
     * 
     * @param session игровая сессия
     * @param roundNumber номер раунда (1, 2 или 3)
     */
    default void onRoundStart(FalkyeGameSession session, int roundNumber) {}
    
    /**
     * Вызывается при окончании раунда
     * 
     * @param session игровая сессия
     * @param roundNumber номер раунда
     * @param winner победитель раунда (может быть null)
     */
    default void onRoundEnd(FalkyeGameSession session, int roundNumber, ServerPlayer winner) {}
    
    /**
     * Вызывается при смене хода
     * 
     * @param session игровая сессия
     * @param previousPlayer предыдущий игрок
     * @param currentPlayer текущий игрок
     */
    default void onTurnChange(FalkyeGameSession session, ServerPlayer previousPlayer, ServerPlayer currentPlayer) {}
}

