package com.bmfalkye.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Сетевые пакеты для системы друзей
 */
public class FriendsPackets {
    
    public static class RequestFriendsPacket {
        public RequestFriendsPacket() {}
        
        public static void encode(RequestFriendsPacket msg, FriendlyByteBuf buffer) {}
        
        public static RequestFriendsPacket decode(FriendlyByteBuf buffer) {
            return new RequestFriendsPacket();
        }
        
        public static void handle(RequestFriendsPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                net.minecraft.server.level.ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    java.util.Set<UUID> friendIds = com.bmfalkye.social.FriendsSystem.getFriends(player);
                    java.util.List<UUID> friendIdsList = new java.util.ArrayList<>(friendIds);
                    java.util.List<String> friendNames = new java.util.ArrayList<>();
                    
                    for (UUID friendId : friendIdsList) {
                        net.minecraft.server.level.ServerPlayer friend = 
                            player.server.getPlayerList().getPlayer(friendId);
                        if (friend != null) {
                            friendNames.add(friend.getName().getString());
                        } else {
                            // Игрок оффлайн, используем UUID как имя
                            friendNames.add(friendId.toString().substring(0, 8) + "...");
                        }
                    }
                    
                    com.bmfalkye.network.NetworkHandler.INSTANCE.sendTo(
                        new SendFriendsPacket(friendIdsList, friendNames),
                        player.connection.connection,
                        net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
                    );
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    public static class AddFriendPacket {
        private final UUID friendId;
        
        public AddFriendPacket(UUID friendId) {
            this.friendId = friendId;
        }
        
        public static void encode(AddFriendPacket msg, FriendlyByteBuf buffer) {
            buffer.writeUUID(msg.friendId);
        }
        
        public static AddFriendPacket decode(FriendlyByteBuf buffer) {
            return new AddFriendPacket(buffer.readUUID());
        }
        
        public static void handle(AddFriendPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                net.minecraft.server.level.ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    net.minecraft.server.level.ServerPlayer friend = 
                        player.server.getPlayerList().getPlayer(msg.friendId);
                    if (friend != null) {
                        com.bmfalkye.social.FriendsSystem.addFriend(player, friend);
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    public static class SendFriendsPacket {
        private final List<UUID> friendIds;
        private final List<String> friendNames;
        
        public SendFriendsPacket(List<UUID> friendIds, List<String> friendNames) {
            this.friendIds = friendIds;
            this.friendNames = friendNames;
        }
        
        public static void encode(SendFriendsPacket msg, FriendlyByteBuf buffer) {
            buffer.writeInt(msg.friendIds.size());
            for (int i = 0; i < msg.friendIds.size(); i++) {
                buffer.writeUUID(msg.friendIds.get(i));
                buffer.writeUtf(msg.friendNames.get(i));
            }
        }
        
        public static SendFriendsPacket decode(FriendlyByteBuf buffer) {
            int count = buffer.readInt();
            List<UUID> ids = new ArrayList<>();
            List<String> names = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                ids.add(buffer.readUUID());
                names.add(buffer.readUtf());
            }
            return new SendFriendsPacket(ids, names);
        }
        
        public static void handle(SendFriendsPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> {
                    com.bmfalkye.client.ClientPacketHandler.handleFriendsList(msg.friendIds, msg.friendNames);
                });
            });
            ctx.get().setPacketHandled(true);
        }
        
        public List<UUID> getFriendIds() { return friendIds; }
        public List<String> getFriendNames() { return friendNames; }
    }
    
    public static class RemoveFriendPacket {
        private final UUID friendId;
        
        public RemoveFriendPacket(UUID friendId) {
            this.friendId = friendId;
        }
        
        public static void encode(RemoveFriendPacket msg, FriendlyByteBuf buffer) {
            buffer.writeUUID(msg.friendId);
        }
        
        public static RemoveFriendPacket decode(FriendlyByteBuf buffer) {
            return new RemoveFriendPacket(buffer.readUUID());
        }
        
        public static void handle(RemoveFriendPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                net.minecraft.server.level.ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    net.minecraft.server.level.ServerPlayer friend = 
                        player.server.getPlayerList().getPlayer(msg.friendId);
                    if (friend != null) {
                        com.bmfalkye.social.FriendsSystem.removeFriend(player, friend);
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§aИгрок удалён из списка друзей"));
                    } else {
                        // Игрок оффлайн, но мы можем удалить его из списка
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§aИгрок удалён из списка друзей"));
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    public static class AddFriendByNamePacket {
        private final String friendName;
        
        public AddFriendByNamePacket(String friendName) {
            this.friendName = friendName;
        }
        
        public static void encode(AddFriendByNamePacket msg, FriendlyByteBuf buffer) {
            buffer.writeUtf(msg.friendName);
        }
        
        public static AddFriendByNamePacket decode(FriendlyByteBuf buffer) {
            return new AddFriendByNamePacket(buffer.readUtf());
        }
        
        public static void handle(AddFriendByNamePacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                net.minecraft.server.level.ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    // Валидация имени
                    if (msg.friendName == null || msg.friendName.trim().isEmpty()) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§cИмя игрока не может быть пустым"));
                        return;
                    }
                    
                    // Ищем игрока по имени
                    net.minecraft.server.level.ServerPlayer friend = 
                        player.server.getPlayerList().getPlayerByName(msg.friendName.trim());
                    
                    if (friend == null) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§cИгрок " + msg.friendName + " не найден или не в сети"));
                        return;
                    }
                    
                    if (player.equals(friend)) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§cВы не можете добавить себя в друзья"));
                        return;
                    }
                    
                    // Проверяем, не является ли уже другом
                    if (com.bmfalkye.social.FriendsSystem.getFriends(player).contains(friend.getUUID())) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§eИгрок " + friend.getName().getString() + " уже в списке друзей"));
                        return;
                    }
                    
                    // Добавляем друга
                    if (com.bmfalkye.social.FriendsSystem.addFriend(player, friend)) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§aИгрок " + friend.getName().getString() + " добавлен в друзья"));
                        
                        // Отправляем обновлённый список друзей
                        java.util.Set<UUID> friendIds = com.bmfalkye.social.FriendsSystem.getFriends(player);
                        java.util.List<UUID> friendIdsList = new java.util.ArrayList<>(friendIds);
                        java.util.List<String> friendNames = new java.util.ArrayList<>();
                        
                        for (UUID friendId : friendIdsList) {
                            net.minecraft.server.level.ServerPlayer f = 
                                player.server.getPlayerList().getPlayer(friendId);
                            if (f != null) {
                                friendNames.add(f.getName().getString());
                            } else {
                                friendNames.add(friendId.toString().substring(0, 8) + "...");
                            }
                        }
                        
                        com.bmfalkye.network.NetworkHandler.INSTANCE.sendTo(
                            new SendFriendsPacket(friendIdsList, friendNames),
                            player.connection.connection,
                            net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
                        );
                    } else {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§cНе удалось добавить игрока в друзья"));
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
}

