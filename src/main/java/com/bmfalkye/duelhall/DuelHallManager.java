package com.bmfalkye.duelhall;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/**
 * Менеджер Залов Дуэлей - управляет многоблочными структурами
 */
public class DuelHallManager extends SavedData {
    private final Map<BlockPos, DuelHall> halls = new HashMap<>();
    
    public static DuelHallManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            DuelHallManager::load,
            DuelHallManager::new,
            "bm_falkye_duel_halls"
        );
    }
    
    public static DuelHallManager load(CompoundTag tag) {
        DuelHallManager manager = new DuelHallManager();
        
        ListTag hallsTag = tag.getList("halls", 10); // TAG_Compound
        for (int i = 0; i < hallsTag.size(); i++) {
            CompoundTag hallTag = hallsTag.getCompound(i);
            DuelHall hall = DuelHall.load(hallTag);
            if (hall != null) {
                manager.halls.put(hall.getCenterPos(), hall);
            }
        }
        
        return manager;
    }
    
    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag hallsTag = new ListTag();
        for (DuelHall hall : halls.values()) {
            hallsTag.add(hall.save(new CompoundTag()));
        }
        tag.put("halls", hallsTag);
        return tag;
    }
    
    /**
     * Находит Зал Дуэлей по позиции блока
     */
    public DuelHall findHallAt(BlockPos pos) {
        for (DuelHall hall : halls.values()) {
            if (hall.contains(pos)) {
                return hall;
            }
        }
        return null;
    }
    
    /**
     * Проверяет структуру Зала Дуэлей и активирует её
     */
    public DuelHall checkAndActivateHall(ServerLevel level, BlockPos centerPos, ServerPlayer player) {
        // Проверяем структуру
        DuelHallStructure structure = DuelHallStructure.checkStructure(level, centerPos);
        if (structure == null) {
            return null;
        }
        
        // Создаём или обновляем Зал Дуэлей
        DuelHall hall = halls.get(centerPos);
        if (hall == null) {
            hall = new DuelHall(centerPos, player.getUUID());
            halls.put(centerPos, hall);
        }
        
        hall.setStructure(structure);
        hall.setActive(true);
        hall.setOwner(player.getUUID());
        setDirty();
        
        return hall;
    }
    
    /**
     * Получить все Залы Дуэлей игрока
     */
    public List<DuelHall> getPlayerHalls(UUID playerUUID) {
        List<DuelHall> playerHalls = new ArrayList<>();
        for (DuelHall hall : halls.values()) {
            if (hall.getOwner().equals(playerUUID)) {
                playerHalls.add(hall);
            }
        }
        return playerHalls;
    }
    
    /**
     * Зал Дуэлей
     */
    public static class DuelHall {
        private BlockPos centerPos;
        private UUID owner;
        private boolean active;
        private DuelHallStructure structure;
        private Set<UUID> invitedPlayers = new HashSet<>();
        
        public DuelHall(BlockPos centerPos, UUID owner) {
            this.centerPos = centerPos;
            this.owner = owner;
            this.active = false;
        }
        
        public static DuelHall load(CompoundTag tag) {
            BlockPos pos = BlockPos.of(tag.getLong("centerPos"));
            UUID owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
            if (owner == null) return null;
            
            DuelHall hall = new DuelHall(pos, owner);
            hall.active = tag.getBoolean("active");
            
            if (tag.contains("invitedPlayers")) {
                ListTag invitedTag = tag.getList("invitedPlayers", 11); // TAG_IntArray (UUID)
                for (int i = 0; i < invitedTag.size(); i++) {
                    hall.invitedPlayers.add(tag.getUUID("invited_" + i));
                }
            }
            
            return hall;
        }
        
        public CompoundTag save(CompoundTag tag) {
            tag.putLong("centerPos", centerPos.asLong());
            tag.putUUID("owner", owner);
            tag.putBoolean("active", active);
            
            ListTag invitedTag = new ListTag();
            int i = 0;
            for (UUID uuid : invitedPlayers) {
                tag.putUUID("invited_" + i, uuid);
                i++;
            }
            tag.put("invitedPlayers", invitedTag);
            
            return tag;
        }
        
        public boolean contains(BlockPos pos) {
            if (structure == null) return false;
            return structure.contains(pos);
        }
        
        public BlockPos getCenterPos() { return centerPos; }
        public UUID getOwner() { return owner; }
        public void setOwner(UUID owner) { this.owner = owner; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public DuelHallStructure getStructure() { return structure; }
        public void setStructure(DuelHallStructure structure) { this.structure = structure; }
        public Set<UUID> getInvitedPlayers() { return invitedPlayers; }
        public void addInvitedPlayer(UUID playerUUID) { this.invitedPlayers.add(playerUUID); }
        public void removeInvitedPlayer(UUID playerUUID) { this.invitedPlayers.remove(playerUUID); }
    }
}

