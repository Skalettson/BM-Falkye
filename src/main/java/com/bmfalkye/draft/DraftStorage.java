package com.bmfalkye.draft;

import com.bmfalkye.util.DataLoadValidator;
import com.bmfalkye.util.ModLogger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Хранилище активных сессий драфта
 */
public class DraftStorage extends SavedData {
    // UUID игрока -> DraftSession
    private final Map<UUID, DraftSession> activeSessions = new HashMap<>();
    
    public static DraftStorage get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            DraftStorage::load,
            DraftStorage::new,
            "bm_falkye_draft_sessions"
        );
    }
    
    @NotNull
    public static DraftStorage load(@NotNull CompoundTag tag) {
        DraftStorage storage = new DraftStorage();
        
        try {
            CompoundTag sessionsTag = tag.getCompound("sessions");
            
            for (String key : sessionsTag.getAllKeys()) {
                UUID playerUUID = DataLoadValidator.safeParseUUID(key);
                if (playerUUID == null) {
                    ModLogger.warn("Skipping invalid UUID during draft load", "key", key);
                    continue;
                }
                
                try {
                    CompoundTag sessionTag = sessionsTag.getCompound(key);
                    DraftSession session = DraftSession.load(sessionTag);
                    if (session != null) {
                        storage.activeSessions.put(playerUUID, session);
                    }
                } catch (Exception e) {
                    ModLogger.error("Error loading draft session for player", 
                        "uuid", playerUUID.toString(), "error", e.getMessage());
                }
            }
        } catch (Exception e) {
            com.bmfalkye.BMFalkye.LOGGER.error("Error loading DraftStorage, using empty storage", e);
            ModLogger.error("Error loading DraftStorage", "error", e.getMessage());
            return new DraftStorage();
        }
        
        return storage;
    }
    
    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag) {
        CompoundTag sessionsTag = new CompoundTag();
        
        for (Map.Entry<UUID, DraftSession> entry : activeSessions.entrySet()) {
            CompoundTag sessionTag = entry.getValue().save(new CompoundTag());
            sessionsTag.put(entry.getKey().toString(), sessionTag);
        }
        
        tag.put("sessions", sessionsTag);
        return tag;
    }
    
    /**
     * Создаёт новую сессию драфта для игрока
     */
    public DraftSession createSession(ServerPlayer player) {
        DraftSession session = new DraftSession(player);
        activeSessions.put(player.getUUID(), session);
        setDirty();
        return session;
    }
    
    /**
     * Получает сессию драфта игрока
     */
    public DraftSession getSession(ServerPlayer player) {
        return activeSessions.get(player.getUUID());
    }
    
    /**
     * Удаляет сессию драфта
     */
    public void removeSession(ServerPlayer player) {
        activeSessions.remove(player.getUUID());
        setDirty();
    }
    
    /**
     * Проверяет, есть ли активная сессия у игрока
     */
    public boolean hasActiveSession(ServerPlayer player) {
        DraftSession session = activeSessions.get(player.getUUID());
        return session != null && session.isActive();
    }
}

