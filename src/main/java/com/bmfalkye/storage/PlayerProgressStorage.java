package com.bmfalkye.storage;

import com.bmfalkye.player.PlayerProgress;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Хранилище прогресса всех игроков
 */
public class PlayerProgressStorage extends SavedData {
    private final Map<UUID, PlayerProgress> playerProgress = new HashMap<>();

    public static PlayerProgressStorage load(CompoundTag tag) {
        PlayerProgressStorage storage = new PlayerProgressStorage();
        
        try {
            CompoundTag progressTag = tag.getCompound("progress");
            
            for (String key : progressTag.getAllKeys()) {
                // Безопасно парсим UUID
                UUID playerUUID = com.bmfalkye.util.DataLoadValidator.safeParseUUID(key);
                if (playerUUID == null) {
                    com.bmfalkye.util.ModLogger.warn("Skipping invalid UUID during progress storage load", "key", key);
                    continue;
                }
                
                try {
                    CompoundTag playerTag = progressTag.getCompound(key);
                    PlayerProgress progress = PlayerProgress.load(playerTag);
                    storage.playerProgress.put(playerUUID, progress);
                } catch (Exception e) {
                    java.util.Map<String, Object> context = com.bmfalkye.util.ErrorContextCollector.collectContext(e,
                        "uuid", playerUUID.toString(),
                        "key", key,
                        "operation", "loadPlayerProgress");
                    com.bmfalkye.util.ModLogger.error("Error loading progress for player", e, context);
                    // Пропускаем этого игрока, продолжаем загрузку остальных
                }
            }
        } catch (Exception e) {
            java.util.Map<String, Object> context = com.bmfalkye.util.ErrorContextCollector.collectDataOperationContext(
                "load", "PlayerProgressStorage", null);
            com.bmfalkye.BMFalkye.LOGGER.error("Error loading PlayerProgressStorage, using empty storage", e);
            com.bmfalkye.util.ModLogger.error("Error loading PlayerProgressStorage", e, context);
            // Возвращаем пустое хранилище
            return new PlayerProgressStorage();
        }
        
        return storage;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag progressTag = new CompoundTag();
        
        for (Map.Entry<UUID, PlayerProgress> entry : playerProgress.entrySet()) {
            progressTag.put(entry.getKey().toString(), entry.getValue().save());
        }
        
        tag.put("progress", progressTag);
        return tag;
    }

    public PlayerProgress getPlayerProgress(ServerPlayer player) {
        return playerProgress.computeIfAbsent(player.getUUID(), k -> new PlayerProgress());
    }

    public void setPlayerProgress(ServerPlayer player, PlayerProgress progress) {
        playerProgress.put(player.getUUID(), progress);
        setDirty();
    }

    public static PlayerProgressStorage get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            PlayerProgressStorage::load,
            PlayerProgressStorage::new,
            "bm_falkye_progress"
        );
    }
}

