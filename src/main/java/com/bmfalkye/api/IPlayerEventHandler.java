package com.bmfalkye.api;

import com.bmfalkye.game.FalkyeGameSession;
import net.minecraft.server.level.ServerPlayer;

/**
 * Интерфейс для обработки событий игроков
 * 
 * <p>Позволяет расширениям реагировать на события, связанные с игроками,
 * такие как вход в игру, получение опыта, разблокировка карт и т.д.</p>
 * 
 * <p>Пример использования:</p>
 * <pre>{@code
 * public class MyPlayerEventHandler implements IPlayerEventHandler {
 *     {@literal @}Override
 *     public void onPlayerLevelUp(ServerPlayer player, int newLevel) {
 *         // Ваш код
 *     }
 * }
 * }</pre>
 * 
 * @author BM Falkye Team
 */
public interface IPlayerEventHandler {
    
    /**
     * Вызывается при входе игрока в игру
     * 
     * @param player игрок
     */
    default void onPlayerJoin(ServerPlayer player) {}
    
    /**
     * Вызывается при выходе игрока из игры
     * 
     * @param player игрок
     */
    default void onPlayerLeave(ServerPlayer player) {}
    
    /**
     * Вызывается при повышении уровня игрока
     * 
     * @param player игрок
     * @param newLevel новый уровень
     */
    default void onPlayerLevelUp(ServerPlayer player, int newLevel) {}
    
    /**
     * Вызывается при получении опыта игроком
     * 
     * @param player игрок
     * @param amount количество опыта
     */
    default void onPlayerGainExperience(ServerPlayer player, int amount) {}
    
    /**
     * Вызывается при разблокировке карты игроком
     * 
     * @param player игрок
     * @param cardId ID разблокированной карты
     */
    default void onCardUnlocked(ServerPlayer player, String cardId) {}
    
    /**
     * Вызывается при победе игрока в игре
     * 
     * @param player игрок
     * @param session игровая сессия
     */
    default void onPlayerWin(ServerPlayer player, FalkyeGameSession session) {}
    
    /**
     * Вызывается при поражении игрока в игре
     * 
     * @param player игрок
     * @param session игровая сессия
     */
    default void onPlayerLose(ServerPlayer player, FalkyeGameSession session) {}
}

