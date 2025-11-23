package com.bmfalkye.api;

import com.bmfalkye.cards.Card;
import com.bmfalkye.game.FalkyeGameSession;
import net.minecraft.server.level.ServerPlayer;

/**
 * Интерфейс для кастомных эффектов карт
 * 
 * <p>Позволяет создавать собственные эффекты для карт, которые будут применяться
 * при разыгрывании карты.</p>
 * 
 * <p>Пример использования:</p>
 * <pre>{@code
 * public class MyCardEffect implements ICardEffect {
 *     {@literal @}Override
 *     public void apply(FalkyeGameSession session, ServerPlayer player, Card card, 
 *                      FalkyeGameSession.CardRow row) {
 *         // Ваш код эффекта
 *     }
 * }
 * 
 * // Регистрация:
 * FalkyeAPI.getInstance().registerCardEffect("my_effect", new MyCardEffect());
 * }</pre>
 * 
 * @author BM Falkye Team
 */
public interface ICardEffect {
    
    /**
     * Применяет эффект карты
     * 
     * @param session игровая сессия
     * @param player игрок, который разыграл карту
     * @param card карта с эффектом
     * @param row ряд, в который была разыграна карта
     */
    void apply(FalkyeGameSession session, ServerPlayer player, Card card, 
               FalkyeGameSession.CardRow row);
}

