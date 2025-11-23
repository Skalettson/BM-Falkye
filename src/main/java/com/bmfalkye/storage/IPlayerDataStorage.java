package com.bmfalkye.storage;

import net.minecraft.server.level.ServerPlayer;

/**
 * Интерфейс для хранения данных игроков
 */
public interface IPlayerDataStorage<T> extends IStorage {
    /**
     * Получает данные игрока
     */
    T getData(ServerPlayer player);
    
    /**
     * Устанавливает данные игрока
     */
    void setData(ServerPlayer player, T data);
}

