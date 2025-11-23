package com.bmfalkye.social;

import net.minecraft.server.level.ServerPlayer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Система друзей
 */
public class FriendsSystem {
    // UUID игрока -> Set UUID друзей
    private static final Map<UUID, Set<UUID>> friends = new ConcurrentHashMap<>();
    // UUID игрока -> Set UUID заблокированных игроков
    private static final Map<UUID, Set<UUID>> blocked = new ConcurrentHashMap<>();
    
    /**
     * Добавляет друга
     */
    public static boolean addFriend(ServerPlayer player, ServerPlayer friend) {
        if (player == null || friend == null || player.equals(friend)) {
            return false;
        }
        
        UUID playerId = player.getUUID();
        UUID friendId = friend.getUUID();
        
        // Проверяем, не заблокирован ли
        if (isBlocked(playerId, friendId)) {
            return false;
        }
        
        friends.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet()).add(friendId);
        return true;
    }
    
    /**
     * Удаляет друга
     */
    public static boolean removeFriend(ServerPlayer player, ServerPlayer friend) {
        if (player == null || friend == null) {
            return false;
        }
        
        UUID playerId = player.getUUID();
        UUID friendId = friend.getUUID();
        
        Set<UUID> playerFriends = friends.get(playerId);
        if (playerFriends != null) {
            return playerFriends.remove(friendId);
        }
        return false;
    }
    
    /**
     * Получает список друзей
     */
    public static Set<UUID> getFriends(ServerPlayer player) {
        if (player == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(friends.getOrDefault(player.getUUID(), Collections.emptySet()));
    }
    
    /**
     * Проверяет, являются ли игроки друзьями
     */
    public static boolean areFriends(ServerPlayer player1, ServerPlayer player2) {
        if (player1 == null || player2 == null) {
            return false;
        }
        
        UUID id1 = player1.getUUID();
        UUID id2 = player2.getUUID();
        
        return friends.getOrDefault(id1, Collections.emptySet()).contains(id2) ||
               friends.getOrDefault(id2, Collections.emptySet()).contains(id1);
    }
    
    /**
     * Блокирует игрока
     */
    public static void blockPlayer(ServerPlayer player, ServerPlayer toBlock) {
        if (player == null || toBlock == null) {
            return;
        }
        
        UUID playerId = player.getUUID();
        UUID blockId = toBlock.getUUID();
        
        blocked.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet()).add(blockId);
        
        // Удаляем из друзей, если были друзьями
        removeFriend(player, toBlock);
        removeFriend(toBlock, player);
    }
    
    /**
     * Разблокирует игрока
     */
    public static void unblockPlayer(ServerPlayer player, ServerPlayer toUnblock) {
        if (player == null || toUnblock == null) {
            return;
        }
        
        UUID playerId = player.getUUID();
        UUID unblockId = toUnblock.getUUID();
        
        Set<UUID> blockedSet = blocked.get(playerId);
        if (blockedSet != null) {
            blockedSet.remove(unblockId);
        }
    }
    
    /**
     * Проверяет, заблокирован ли игрок
     */
    public static boolean isBlocked(UUID playerId, UUID otherId) {
        return blocked.getOrDefault(playerId, Collections.emptySet()).contains(otherId);
    }
}

