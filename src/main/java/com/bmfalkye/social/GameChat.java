package com.bmfalkye.social;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Глобальный чат для игроков
 */
public class GameChat {
    // Каналы чата
    public enum ChatChannel {
        GLOBAL,      // Глобальный чат
        GUILD,       // Чат гильдии
        FRIENDS,     // Чат с друзьями
        TOURNAMENT,  // Чат турнира
        GAME         // Чат во время игры
    }
    
    // UUID игрока -> канал
    private static final Map<UUID, ChatChannel> playerChannels = new ConcurrentHashMap<>();
    
    /**
     * Отправляет сообщение в канал
     */
    public static void sendMessage(ServerPlayer sender, ChatChannel channel, String message) {
        if (sender == null || message == null || message.trim().isEmpty()) {
            return;
        }
        
        Component chatMessage = Component.literal("§7[" + channel.name() + "] §f" + 
            sender.getName().getString() + ": §7" + message);
        
        switch (channel) {
            case GLOBAL:
                // Отправляем всем игрокам
                for (ServerPlayer player : sender.server.getPlayerList().getPlayers()) {
                    player.sendSystemMessage(chatMessage);
                }
                break;
                
            case GUILD:
                // Отправляем участникам гильдии
                com.bmfalkye.social.GuildSystem.Guild guild = GuildSystem.getPlayerGuild(sender);
                if (guild != null) {
                    for (UUID memberId : guild.getMembers()) {
                        ServerPlayer member = sender.server.getPlayerList().getPlayer(memberId);
                        if (member != null) {
                            member.sendSystemMessage(chatMessage);
                        }
                    }
                }
                break;
                
            case FRIENDS:
                // Отправляем друзьям
                Set<UUID> friends = FriendsSystem.getFriends(sender);
                for (UUID friendId : friends) {
                    ServerPlayer friend = sender.server.getPlayerList().getPlayer(friendId);
                    if (friend != null) {
                        friend.sendSystemMessage(chatMessage);
                    }
                }
                break;
                
            case GAME:
                // Отправляем участникам игры
                com.bmfalkye.game.FalkyeGameSession session = 
                    com.bmfalkye.game.GameManager.getActiveGame(sender);
                if (session != null) {
                    if (session.getPlayer1() != null) {
                        session.getPlayer1().sendSystemMessage(chatMessage);
                    }
                    if (session.getPlayer2() != null) {
                        session.getPlayer2().sendSystemMessage(chatMessage);
                    }
                }
                break;
        }
    }
    
    /**
     * Устанавливает канал для игрока
     */
    public static void setChannel(ServerPlayer player, ChatChannel channel) {
        if (player != null) {
            playerChannels.put(player.getUUID(), channel);
        }
    }
    
    /**
     * Получает канал игрока
     */
    public static ChatChannel getChannel(ServerPlayer player) {
        return playerChannels.getOrDefault(player.getUUID(), ChatChannel.GLOBAL);
    }
}

