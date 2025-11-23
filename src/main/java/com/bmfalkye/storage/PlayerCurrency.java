package com.bmfalkye.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Система валюты игроков (монеты и Пыль Душ)
 */
public class PlayerCurrency extends SavedData {
    private static final int STARTING_COINS = 800;
    private static final int STARTING_SOUL_DUST = 0;
    private final Map<UUID, Integer> playerCoins = new HashMap<>();
    private final Map<UUID, Integer> playerSoulDust = new HashMap<>();
    
    public static PlayerCurrency get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            PlayerCurrency::load,
            PlayerCurrency::new,
            "bm_falkye_currency"
        );
    }
    
    public static PlayerCurrency load(CompoundTag tag) {
        PlayerCurrency currency = new PlayerCurrency();
        CompoundTag coinsTag = tag.getCompound("coins");
        CompoundTag soulDustTag = tag.getCompound("soulDust");
        
        for (String key : coinsTag.getAllKeys()) {
            UUID playerUUID = UUID.fromString(key);
            int coins = coinsTag.getInt(key);
            currency.playerCoins.put(playerUUID, coins);
        }
        
        for (String key : soulDustTag.getAllKeys()) {
            UUID playerUUID = UUID.fromString(key);
            int soulDust = soulDustTag.getInt(key);
            currency.playerSoulDust.put(playerUUID, soulDust);
        }
        
        return currency;
    }
    
    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag coinsTag = new CompoundTag();
        CompoundTag soulDustTag = new CompoundTag();
        
        for (Map.Entry<UUID, Integer> entry : playerCoins.entrySet()) {
            coinsTag.putInt(entry.getKey().toString(), entry.getValue());
        }
        
        for (Map.Entry<UUID, Integer> entry : playerSoulDust.entrySet()) {
            soulDustTag.putInt(entry.getKey().toString(), entry.getValue());
        }
        
        tag.put("coins", coinsTag);
        tag.put("soulDust", soulDustTag);
        return tag;
    }
    
    /**
     * Получить баланс игрока (или начальный баланс, если игрок новый)
     */
    public int getCoins(ServerPlayer player) {
        return playerCoins.computeIfAbsent(player.getUUID(), k -> {
            setDirty();
            return STARTING_COINS;
        });
    }
    
    /**
     * Установить баланс игрока
     */
    public void setCoins(ServerPlayer player, int coins) {
        playerCoins.put(player.getUUID(), Math.max(0, coins));
        setDirty();
    }
    
    /**
     * Добавить монеты игроку
     */
    public void addCoins(ServerPlayer player, int amount) {
        int current = getCoins(player);
        setCoins(player, current + amount);
    }
    
    /**
     * Забрать монеты у игрока
     * @return true если успешно, false если недостаточно монет
     */
    public boolean removeCoins(ServerPlayer player, int amount) {
        int current = getCoins(player);
        if (current >= amount) {
            setCoins(player, current - amount);
            return true;
        }
        return false;
    }
    
    /**
     * Проверить, достаточно ли монет у игрока
     */
    public boolean hasEnoughCoins(ServerPlayer player, int amount) {
        return getCoins(player) >= amount;
    }
    
    // ========== ПЫЛЬ ДУШ ==========
    
    /**
     * Получить количество Пыли Душ игрока (или начальное количество, если игрок новый)
     */
    public int getSoulDust(ServerPlayer player) {
        return playerSoulDust.computeIfAbsent(player.getUUID(), k -> {
            setDirty();
            return STARTING_SOUL_DUST;
        });
    }
    
    /**
     * Установить количество Пыли Душ игрока
     */
    public void setSoulDust(ServerPlayer player, int soulDust) {
        playerSoulDust.put(player.getUUID(), Math.max(0, soulDust));
        setDirty();
    }
    
    /**
     * Добавить Пыль Душ игроку
     */
    public void addSoulDust(ServerPlayer player, int amount) {
        int current = getSoulDust(player);
        setSoulDust(player, current + amount);
    }
    
    /**
     * Забрать Пыль Душ у игрока
     * @return true если успешно, false если недостаточно Пыли Душ
     */
    public boolean removeSoulDust(ServerPlayer player, int amount) {
        int current = getSoulDust(player);
        if (current >= amount) {
            setSoulDust(player, current - amount);
            return true;
        }
        return false;
    }
    
    /**
     * Проверить, достаточно ли Пыли Душ у игрока
     */
    public boolean hasEnoughSoulDust(ServerPlayer player, int amount) {
        return getSoulDust(player) >= amount;
    }
}

