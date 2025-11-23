package com.bmfalkye.storage;

import com.bmfalkye.util.DataLoadValidator;
import com.bmfalkye.util.ModLogger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/**
 * Хранилище просмотренных подсказок для игроков
 */
public class TutorialHintStorage extends SavedData {
    // UUID игрока -> Set ID просмотренных подсказок
    private final Map<UUID, Set<String>> playerHints = new HashMap<>();
    
    public static TutorialHintStorage load(CompoundTag tag) {
        TutorialHintStorage storage = new TutorialHintStorage();
        
        try {
            CompoundTag hintsTag = tag.getCompound("hints");
            
            for (String key : hintsTag.getAllKeys()) {
                // Безопасно парсим UUID
                UUID playerUUID = DataLoadValidator.safeParseUUID(key);
                if (playerUUID == null) {
                    ModLogger.warn("Skipping invalid UUID during tutorial hint storage load", "key", key);
                    continue;
                }
                
                try {
                    ListTag hintsList = hintsTag.getList(key, Tag.TAG_STRING);
                    Set<String> hints = new HashSet<>();
                    
                    for (int i = 0; i < hintsList.size(); i++) {
                        String hintId = hintsList.getString(i);
                        if (hintId != null && !hintId.isEmpty()) {
                            hints.add(hintId);
                        }
                    }
                    
                    storage.playerHints.put(playerUUID, hints);
                } catch (Exception e) {
                    java.util.Map<String, Object> context = com.bmfalkye.util.ErrorContextCollector.collectContext(e,
                        "uuid", playerUUID.toString(),
                        "key", key,
                        "operation", "loadPlayerHints");
                    ModLogger.error("Error loading hints for player", e, context);
                    // Пропускаем этого игрока, продолжаем загрузку остальных
                }
            }
        } catch (Exception e) {
            java.util.Map<String, Object> context = com.bmfalkye.util.ErrorContextCollector.collectDataOperationContext(
                "load", "TutorialHintStorage", null);
            com.bmfalkye.BMFalkye.LOGGER.error("Error loading TutorialHintStorage, using empty storage", e);
            ModLogger.error("Error loading TutorialHintStorage", e, context);
            // Возвращаем пустое хранилище
            return new TutorialHintStorage();
        }
        
        return storage;
    }
    
    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag hintsTag = new CompoundTag();
        
        for (Map.Entry<UUID, Set<String>> entry : playerHints.entrySet()) {
            ListTag hintsList = new ListTag();
            for (String hintId : entry.getValue()) {
                hintsList.add(StringTag.valueOf(hintId));
            }
            hintsTag.put(entry.getKey().toString(), hintsList);
        }
        
        tag.put("hints", hintsTag);
        return tag;
    }
    
    /**
     * Проверяет, видел ли игрок подсказку
     */
    public boolean hasSeenHint(ServerPlayer player, String hintId) {
        if (player == null || hintId == null) {
            return false;
        }
        
        Set<String> hints = playerHints.get(player.getUUID());
        return hints != null && hints.contains(hintId);
    }
    
    /**
     * Отмечает подсказку как просмотренную
     */
    public void markHintAsSeen(ServerPlayer player, String hintId) {
        if (player == null || hintId == null) {
            return;
        }
        
        Set<String> hints = playerHints.computeIfAbsent(player.getUUID(), k -> new HashSet<>());
        hints.add(hintId);
        setDirty();
    }
    
    /**
     * Очищает все подсказки для игрока
     */
    public void clearPlayerHints(ServerPlayer player) {
        if (player == null) {
            return;
        }
        
        playerHints.remove(player.getUUID());
        setDirty();
    }
    
    /**
     * Получает все просмотренные подсказки игрока
     */
    public Set<String> getPlayerHints(ServerPlayer player) {
        if (player == null) {
            return new HashSet<>();
        }
        
        return new HashSet<>(playerHints.getOrDefault(player.getUUID(), new HashSet<>()));
    }
    
    public static TutorialHintStorage get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            TutorialHintStorage::load,
            TutorialHintStorage::new,
            "bm_falkye_tutorial_hints"
        );
    }
}

