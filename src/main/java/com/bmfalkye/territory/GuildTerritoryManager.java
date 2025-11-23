package com.bmfalkye.territory;

import com.bmfalkye.util.ModLogger;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Менеджер территорий гильдий
 * Хранит состояние точек силы и контролирующих их гильдий
 */
public class GuildTerritoryManager extends SavedData {
    // Точки силы: позиция -> данные точки
    private final Map<BlockPos, TerritoryPoint> territoryPoints = new HashMap<>();
    
    // Процессы захвата: позиция -> данные захвата
    private final Map<BlockPos, CaptureProcess> captureProcesses = new HashMap<>();
    
    public static GuildTerritoryManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            GuildTerritoryManager::load,
            GuildTerritoryManager::new,
            "bm_falkye_guild_territories"
        );
    }
    
    @NotNull
    public static GuildTerritoryManager load(@NotNull CompoundTag tag) {
        GuildTerritoryManager manager = new GuildTerritoryManager();
        
        try {
            // Загружаем точки силы
            ListTag pointsTag = tag.getList("territoryPoints", 10);
            for (int i = 0; i < pointsTag.size(); i++) {
                CompoundTag pointTag = pointsTag.getCompound(i);
                TerritoryPoint point = TerritoryPoint.load(pointTag);
                if (point != null) {
                    manager.territoryPoints.put(point.position, point);
                }
            }
            
            // Загружаем процессы захвата
            ListTag capturesTag = tag.getList("captureProcesses", 10);
            for (int i = 0; i < capturesTag.size(); i++) {
                CompoundTag captureTag = capturesTag.getCompound(i);
                CaptureProcess process = CaptureProcess.load(captureTag);
                if (process != null) {
                    manager.captureProcesses.put(process.position, process);
                }
            }
        } catch (Exception e) {
            com.bmfalkye.BMFalkye.LOGGER.error("Error loading GuildTerritoryManager, using empty manager", e);
            ModLogger.error("Error loading GuildTerritoryManager", "error", e.getMessage());
            return new GuildTerritoryManager();
        }
        
        return manager;
    }
    
    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag) {
        // Сохраняем точки силы
        ListTag pointsTag = new ListTag();
        for (TerritoryPoint point : territoryPoints.values()) {
            pointsTag.add(point.save(new CompoundTag()));
        }
        tag.put("territoryPoints", pointsTag);
        
        // Сохраняем процессы захвата
        ListTag capturesTag = new ListTag();
        for (CaptureProcess process : captureProcesses.values()) {
            capturesTag.add(process.save(new CompoundTag()));
        }
        tag.put("captureProcesses", capturesTag);
        
        return tag;
    }
    
    /**
     * Регистрирует точку силы
     */
    public void registerTerritoryPoint(BlockPos position, String name) {
        territoryPoints.put(position, new TerritoryPoint(position, name));
        setDirty();
    }
    
    /**
     * Получает точку силы
     */
    public TerritoryPoint getTerritoryPoint(BlockPos position) {
        return territoryPoints.get(position);
    }
    
    /**
     * Начинает процесс захвата точки
     */
    public void startCapture(BlockPos position, UUID guildId, String guildName) {
        captureProcesses.put(position, new CaptureProcess(position, guildId, guildName));
        setDirty();
    }
    
    /**
     * Отменяет процесс захвата
     */
    public void cancelCapture(BlockPos position) {
        captureProcesses.remove(position);
        setDirty();
    }
    
    /**
     * Завершает захват точки
     */
    public void completeCapture(BlockPos position, UUID guildId, String guildName) {
        TerritoryPoint point = territoryPoints.get(position);
        if (point != null) {
            point.controllingGuildId = guildId;
            point.controllingGuildName = guildName;
            point.captureTime = System.currentTimeMillis();
            captureProcesses.remove(position);
            setDirty();
        }
    }
    
    /**
     * Получает контролирующую гильдию точки
     */
    public UUID getControllingGuild(BlockPos position) {
        TerritoryPoint point = territoryPoints.get(position);
        return point != null ? point.controllingGuildId : null;
    }
    
    /**
     * Проверяет, контролирует ли гильдия точку
     */
    public boolean isGuildControlling(BlockPos position, UUID guildId) {
        TerritoryPoint point = territoryPoints.get(position);
        return point != null && guildId != null && guildId.equals(point.controllingGuildId);
    }
    
    /**
     * Получает активный процесс захвата
     */
    public CaptureProcess getCaptureProcess(BlockPos position) {
        return captureProcesses.get(position);
    }
    
    /**
     * Точка силы
     */
    public static class TerritoryPoint {
        public final BlockPos position;
        public final String name;
        public UUID controllingGuildId;
        public String controllingGuildName;
        public long captureTime;
        
        public TerritoryPoint(BlockPos position, String name) {
            this.position = position;
            this.name = name;
            this.controllingGuildId = null;
            this.controllingGuildName = null;
            this.captureTime = 0;
        }
        
        public CompoundTag save(CompoundTag tag) {
            tag.putLong("position", position.asLong());
            tag.putString("name", name);
            if (controllingGuildId != null) {
                tag.putUUID("controllingGuildId", controllingGuildId);
                tag.putString("controllingGuildName", controllingGuildName != null ? controllingGuildName : "");
            }
            tag.putLong("captureTime", captureTime);
            return tag;
        }
        
        public static TerritoryPoint load(CompoundTag tag) {
            try {
                BlockPos pos = BlockPos.of(tag.getLong("position"));
                String name = tag.getString("name");
                TerritoryPoint point = new TerritoryPoint(pos, name);
                
                if (tag.hasUUID("controllingGuildId")) {
                    point.controllingGuildId = tag.getUUID("controllingGuildId");
                    point.controllingGuildName = tag.getString("controllingGuildName");
                    point.captureTime = tag.getLong("captureTime");
                }
                
                return point;
            } catch (Exception e) {
                ModLogger.error("Error loading TerritoryPoint", "error", e.getMessage());
                return null;
            }
        }
    }
    
    /**
     * Процесс захвата
     */
    public static class CaptureProcess {
        public final BlockPos position;
        public final UUID guildId;
        public final String guildName;
        public final long startTime;
        
        private static final long CAPTURE_DURATION_MS = 60 * 1000; // 1 минута
        
        public CaptureProcess(BlockPos position, UUID guildId, String guildName) {
            this.position = position;
            this.guildId = guildId;
            this.guildName = guildName;
            this.startTime = System.currentTimeMillis();
        }
        
        public boolean isComplete() {
            return System.currentTimeMillis() - startTime >= CAPTURE_DURATION_MS;
        }
        
        public long getRemainingTime() {
            return Math.max(0, CAPTURE_DURATION_MS - (System.currentTimeMillis() - startTime));
        }
        
        public CompoundTag save(CompoundTag tag) {
            tag.putLong("position", position.asLong());
            tag.putUUID("guildId", guildId);
            tag.putString("guildName", guildName);
            tag.putLong("startTime", startTime);
            return tag;
        }
        
        public static CaptureProcess load(CompoundTag tag) {
            try {
                return new CaptureProcess(
                    BlockPos.of(tag.getLong("position")),
                    tag.getUUID("guildId"),
                    tag.getString("guildName")
                );
            } catch (Exception e) {
                ModLogger.error("Error loading CaptureProcess", "error", e.getMessage());
                return null;
            }
        }
    }
}

