package com.bmfalkye.api;

import com.bmfalkye.game.FalkyeGameSession;
import net.minecraft.server.level.ServerPlayer;

/**
 * Интерфейс для создания собственных способностей карт
 * Поддерживает хуки на различные события игры
 */
public interface Ability {
    /**
     * Вызывается при розыгрыше карты
     */
    default void onPlay(FalkyeGameSession session, ServerPlayer player, String cardId) {
        // Переопределить при необходимости
    }
    
    /**
     * Вызывается при смерти карты
     */
    default void onDeath(FalkyeGameSession session, ServerPlayer player, String cardId) {
        // Переопределить при необходимости
    }
    
    /**
     * Вызывается в конце раунда
     */
    default void onRoundEnd(FalkyeGameSession session, ServerPlayer player, String cardId) {
        // Переопределить при необходимости
    }
    
    /**
     * Вызывается в начале раунда
     */
    default void onRoundStart(FalkyeGameSession session, ServerPlayer player, String cardId) {
        // Переопределить при необходимости
    }
    
    /**
     * Получает имя способности
     */
    String getName();
    
    /**
     * Получает описание способности
     */
    String getDescription();
}

