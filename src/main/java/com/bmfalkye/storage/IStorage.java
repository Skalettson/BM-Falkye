package com.bmfalkye.storage;

import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

/**
 * Базовый интерфейс для систем хранения данных игроков
 */
public interface IStorage {
    /**
     * Загружает данные игрока
     */
    void load(ServerPlayer player);
    
    /**
     * Сохраняет данные игрока
     */
    void save(ServerPlayer player);
    
    /**
     * Удаляет данные игрока
     */
    void remove(UUID playerId);
    
    /**
     * Проверяет, существуют ли данные для игрока
     */
    boolean exists(UUID playerId);
}

