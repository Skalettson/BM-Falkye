package com.bmfalkye.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Система валюты для жителей (NPC)
 */
public class VillagerCurrency extends SavedData {
    private static final int MIN_COINS = 100;
    private static final int MAX_COINS = 10000;
    private final Map<UUID, Integer> villagerCoins = new HashMap<>();
    private final Random random = new Random();
    
    public static VillagerCurrency get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            VillagerCurrency::load,
            VillagerCurrency::new,
            "bm_falkye_villager_currency"
        );
    }
    
    public static VillagerCurrency load(CompoundTag tag) {
        VillagerCurrency currency = new VillagerCurrency();
        CompoundTag coinsTag = tag.getCompound("coins");
        
        for (String key : coinsTag.getAllKeys()) {
            UUID villagerUUID = UUID.fromString(key);
            int coins = coinsTag.getInt(key);
            currency.villagerCoins.put(villagerUUID, coins);
        }
        
        return currency;
    }
    
    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag coinsTag = new CompoundTag();
        
        for (Map.Entry<UUID, Integer> entry : villagerCoins.entrySet()) {
            coinsTag.putInt(entry.getKey().toString(), entry.getValue());
        }
        
        tag.put("coins", coinsTag);
        return tag;
    }
    
    /**
     * Получить баланс жителя (или сгенерировать случайный, если житель новый)
     */
    public int getCoins(LivingEntity villager) {
        return villagerCoins.computeIfAbsent(villager.getUUID(), k -> {
            int coins = MIN_COINS + random.nextInt(MAX_COINS - MIN_COINS + 1);
            setDirty();
            return coins;
        });
    }
    
    /**
     * Установить баланс жителя
     */
    public void setCoins(LivingEntity villager, int coins) {
        villagerCoins.put(villager.getUUID(), Math.max(0, coins));
        setDirty();
    }
    
    /**
     * Добавить монеты жителю
     */
    public void addCoins(LivingEntity villager, int amount) {
        int current = getCoins(villager);
        setCoins(villager, current + amount);
    }
    
    /**
     * Забрать монеты у жителя
     */
    public boolean removeCoins(LivingEntity villager, int amount) {
        int current = getCoins(villager);
        if (current >= amount) {
            setCoins(villager, current - amount);
            return true;
        }
        return false;
    }
    
    /**
     * Проверить, достаточно ли монет у жителя
     */
    public boolean hasEnoughCoins(LivingEntity villager, int amount) {
        return getCoins(villager) >= amount;
    }
}

