package com.bmfalkye.social;

import net.minecraft.server.level.ServerPlayer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Система гильдий/клубов
 */
public class GuildSystem {
    // ID гильдии -> Гильдия
    private static final Map<String, Guild> guilds = new ConcurrentHashMap<>();
    // UUID игрока -> ID гильдии
    private static final Map<UUID, String> playerGuilds = new ConcurrentHashMap<>();
    
    /**
     * Создаёт гильдию
     */
    public static Guild createGuild(ServerPlayer leader, String name, String description) {
        if (name == null || name.trim().isEmpty() || name.length() > 30) {
            return null;
        }
        
        if (playerGuilds.containsKey(leader.getUUID())) {
            return null; // Игрок уже в гильдии
        }
        
        String guildId = UUID.randomUUID().toString();
        Guild guild = new Guild(guildId, name, description, leader.getUUID());
        guilds.put(guildId, guild);
        playerGuilds.put(leader.getUUID(), guildId);
        
        return guild;
    }
    
    /**
     * Приглашает игрока в гильдию
     */
    public static boolean invitePlayer(Guild guild, ServerPlayer player) {
        if (guild == null || player == null) {
            return false;
        }
        
        if (playerGuilds.containsKey(player.getUUID())) {
            return false; // Уже в гильдии
        }
        
        guild.addInvite(player.getUUID());
        return true;
    }
    
    /**
     * Принимает приглашение
     */
    public static boolean acceptInvite(ServerPlayer player, String guildId) {
        Guild guild = guilds.get(guildId);
        if (guild == null || !guild.hasInvite(player.getUUID())) {
            return false;
        }
        
        if (guild.getMembers().size() >= guild.getMaxMembers()) {
            return false; // Гильдия переполнена
        }
        
        guild.removeInvite(player.getUUID());
        guild.addMember(player.getUUID());
        playerGuilds.put(player.getUUID(), guildId);
        
        return true;
    }
    
    /**
     * Покидает гильдию
     */
    public static void leaveGuild(ServerPlayer player) {
        String guildId = playerGuilds.remove(player.getUUID());
        if (guildId != null) {
            Guild guild = guilds.get(guildId);
            if (guild != null) {
                guild.removeMember(player.getUUID());
                
                // Если лидер покинул, передаём лидерство
                if (guild.getLeader().equals(player.getUUID())) {
                    if (!guild.getMembers().isEmpty()) {
                        UUID newLeader = guild.getMembers().iterator().next();
                        guild.setLeader(newLeader);
                    } else {
                        // Гильдия пуста, удаляем
                        guilds.remove(guildId);
                    }
                }
            }
        }
    }
    
    /**
     * Получает гильдию игрока
     */
    public static Guild getPlayerGuild(ServerPlayer player) {
        String guildId = playerGuilds.get(player.getUUID());
        return guildId != null ? guilds.get(guildId) : null;
    }
    
    /**
     * Гильдия
     */
    public static class Guild {
        private final String id;
        private final String name;
        private final String description;
        private UUID leader;
        private final Set<UUID> members = ConcurrentHashMap.newKeySet();
        private final Set<UUID> invites = ConcurrentHashMap.newKeySet();
        private int maxMembers = 20;
        private int guildLevel = 1;
        private int guildXP = 0;
        
        public Guild(String id, String name, String description, UUID leader) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.leader = leader;
            this.members.add(leader);
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public UUID getLeader() { return leader; }
        public void setLeader(UUID leader) { this.leader = leader; }
        public Set<UUID> getMembers() { return new HashSet<>(members); }
        public int getMaxMembers() { return maxMembers; }
        public int getGuildLevel() { return guildLevel; }
        public int getGuildXP() { return guildXP; }
        
        public void addMember(UUID playerId) { members.add(playerId); }
        public void removeMember(UUID playerId) { members.remove(playerId); }
        public void addInvite(UUID playerId) { invites.add(playerId); }
        public void removeInvite(UUID playerId) { invites.remove(playerId); }
        public boolean hasInvite(UUID playerId) { return invites.contains(playerId); }
        
        public void addXP(int xp) {
            guildXP += xp;
            // Проверка повышения уровня
            int xpNeeded = guildLevel * 1000;
            if (guildXP >= xpNeeded) {
                guildXP -= xpNeeded;
                guildLevel++;
                maxMembers = 15 + guildLevel * 5; // Увеличиваем лимит участников
            }
        }
    }
}

