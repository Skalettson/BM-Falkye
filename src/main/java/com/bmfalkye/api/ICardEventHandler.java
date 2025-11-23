package com.bmfalkye.api;

import com.bmfalkye.cards.Card;
import com.bmfalkye.game.FalkyeGameSession;
import net.minecraft.server.level.ServerPlayer;

/**
 * Интерфейс для обработки событий карт
 * 
 * <p>Позволяет расширениям реагировать на события, связанные с картами,
 * такие как разыгрывание карты, применение эффекта и т.д.</p>
 * 
 * <p>Пример использования:</p>
 * <pre>{@code
 * public class MyCardEventHandler implements ICardEventHandler {
 *     {@literal @}Override
 *     public void onCardPlayed(FalkyeGameSession session, ServerPlayer player, Card card) {
 *         // Ваш код
 *     }
 * }
 * }</pre>
 * 
 * @author BM Falkye Team
 */
public interface ICardEventHandler {
    
    /**
     * Вызывается при разыгрывании карты
     * 
     * @param session игровая сессия
     * @param player игрок, который разыграл карту
     * @param card разыгранная карта
     * @param row ряд, в который была разыграна карта
     */
    default void onCardPlayed(FalkyeGameSession session, ServerPlayer player, Card card, 
                             FalkyeGameSession.CardRow row) {}
    
    /**
     * Вызывается перед применением эффекта карты
     * 
     * @param session игровая сессия
     * @param player игрок, который разыграл карту
     * @param card карта с эффектом
     * @param row ряд, в который была разыграна карта
     * @return true если эффект должен быть применён, false для отмены
     */
    default boolean onBeforeCardEffect(FalkyeGameSession session, ServerPlayer player, Card card, 
                                      FalkyeGameSession.CardRow row) {
        return true;
    }
    
    /**
     * Вызывается после применения эффекта карты
     * 
     * @param session игровая сессия
     * @param player игрок, который разыграл карту
     * @param card карта с эффектом
     * @param row ряд, в который была разыграна карта
     */
    default void onAfterCardEffect(FalkyeGameSession session, ServerPlayer player, Card card, 
                                   FalkyeGameSession.CardRow row) {}
    
    /**
     * Вызывается при удалении карты с поля
     * 
     * @param session игровая сессия
     * @param player владелец карты
     * @param card удаляемая карта
     */
    default void onCardRemoved(FalkyeGameSession session, ServerPlayer player, Card card) {}
    
    /**
     * Вызывается при изменении силы карты
     * 
     * @param session игровая сессия
     * @param player владелец карты
     * @param card карта
     * @param oldPower старая сила
     * @param newPower новая сила
     */
    default void onCardPowerChanged(FalkyeGameSession session, ServerPlayer player, Card card, 
                                    int oldPower, int newPower) {}
}

