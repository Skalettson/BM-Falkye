package com.bmfalkye.storage;

import com.bmfalkye.balance.CardBalanceSystem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Хранилище статистики балансировки карт
 */
public class CardBalanceStorage extends SavedData {
    
    public static CardBalanceStorage get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            CardBalanceStorage::load,
            CardBalanceStorage::new,
            "bm_falkye_card_balance"
        );
    }
    
    public static CardBalanceStorage load(CompoundTag tag) {
        CardBalanceStorage storage = new CardBalanceStorage();
        
        try {
            if (tag.contains("balanceStats", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                CompoundTag balanceTag = tag.getCompound("balanceStats");
                CardBalanceSystem.loadStatistics(balanceTag);
            }
        } catch (Exception e) {
            com.bmfalkye.BMFalkye.LOGGER.error("Error loading CardBalanceStorage, using empty storage", e);
            com.bmfalkye.util.ModLogger.error("Error loading CardBalanceStorage", "error", e.getMessage());
        }
        
        return storage;
    }
    
    @Override
    public CompoundTag save(CompoundTag tag) {
        try {
            CompoundTag balanceTag = CardBalanceSystem.saveStatistics();
            tag.put("balanceStats", balanceTag);
        } catch (Exception e) {
            com.bmfalkye.BMFalkye.LOGGER.error("Error saving CardBalanceStorage", e);
            com.bmfalkye.util.ModLogger.error("Error saving CardBalanceStorage", "error", e.getMessage());
        }
        
        return tag;
    }
    
    /**
     * Сохраняет данные (помечает как изменённые)
     */
    public void markDirty() {
        setDirty();
    }
}

