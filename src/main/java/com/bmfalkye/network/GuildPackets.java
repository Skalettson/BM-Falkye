package com.bmfalkye.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Сетевые пакеты для системы гильдий
 */
public class GuildPackets {
    
    public static class CreateGuildPacket {
        private final String name;
        private final String description;
        
        public CreateGuildPacket(String name, String description) {
            this.name = name;
            this.description = description;
        }
        
        public static void encode(CreateGuildPacket msg, FriendlyByteBuf buffer) {
            buffer.writeUtf(msg.name);
            buffer.writeUtf(msg.description);
        }
        
        public static CreateGuildPacket decode(FriendlyByteBuf buffer) {
            return new CreateGuildPacket(buffer.readUtf(), buffer.readUtf());
        }
        
        public static void handle(CreateGuildPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                net.minecraft.server.level.ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    // Валидация имени
                    if (msg.name == null || msg.name.trim().isEmpty()) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§cНазвание гильдии не может быть пустым"));
                        return;
                    }
                    
                    if (msg.name.length() > 30) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§cНазвание гильдии слишком длинное (максимум 30 символов)"));
                        return;
                    }
                    
                    // Проверяем, не состоит ли игрок уже в гильдии
                    if (com.bmfalkye.social.GuildSystem.getPlayerGuild(player) != null) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§cВы уже состоите в гильдии"));
                        return;
                    }
                    
                    // Создаём гильдию
                    com.bmfalkye.social.GuildSystem.Guild guild = 
                        com.bmfalkye.social.GuildSystem.createGuild(player, msg.name.trim(), 
                            msg.description != null ? msg.description.trim() : "");
                    
                    if (guild != null) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§aГильдия \"" + guild.getName() + "\" успешно создана!"));
                        
                        // Отправляем обновлённую информацию о гильдии
                        java.util.List<UUID> memberIds = new java.util.ArrayList<>(guild.getMembers());
                        java.util.List<String> memberNames = new java.util.ArrayList<>();
                        
                        for (UUID memberId : memberIds) {
                            net.minecraft.server.level.ServerPlayer member = 
                                player.server.getPlayerList().getPlayer(memberId);
                            if (member != null) {
                                memberNames.add(member.getName().getString());
                            } else {
                                memberNames.add(memberId.toString().substring(0, 8) + "...");
                            }
                        }
                        
                        com.bmfalkye.network.NetworkHandler.INSTANCE.sendTo(
                            new SendGuildInfoPacket(guild.getId(), guild.getName(), guild.getDescription(),
                                guild.getLeader(), memberIds, memberNames, guild.getGuildLevel(),
                                guild.getGuildXP(), guild.getMaxMembers()),
                            player.connection.connection,
                            net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
                        );
                    } else {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§cНе удалось создать гильдию"));
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    public static class LeaveGuildPacket {
        public LeaveGuildPacket() {}
        
        public static void encode(LeaveGuildPacket msg, FriendlyByteBuf buffer) {}
        
        public static LeaveGuildPacket decode(FriendlyByteBuf buffer) {
            return new LeaveGuildPacket();
        }
        
        public static void handle(LeaveGuildPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                net.minecraft.server.level.ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    com.bmfalkye.social.GuildSystem.Guild guild = 
                        com.bmfalkye.social.GuildSystem.getPlayerGuild(player);
                    
                    if (guild == null) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§cВы не состоите в гильдии"));
                        return;
                    }
                    
                    // Покидаем гильдию
                    com.bmfalkye.social.GuildSystem.leaveGuild(player);
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§aВы покинули гильдию \"" + guild.getName() + "\""));
                    
                    // Отправляем пустую информацию о гильдии
                    com.bmfalkye.network.NetworkHandler.INSTANCE.sendTo(
                        new SendGuildInfoPacket("", "", "", null, 
                            new java.util.ArrayList<>(), new java.util.ArrayList<>(), 0, 0, 0),
                        player.connection.connection,
                        net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
                    );
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    public static class RequestGuildInfoPacket {
        public RequestGuildInfoPacket() {}
        
        public static void encode(RequestGuildInfoPacket msg, FriendlyByteBuf buffer) {}
        
        public static RequestGuildInfoPacket decode(FriendlyByteBuf buffer) {
            return new RequestGuildInfoPacket();
        }
        
        public static void handle(RequestGuildInfoPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                net.minecraft.server.level.ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    com.bmfalkye.social.GuildSystem.Guild guild = 
                        com.bmfalkye.social.GuildSystem.getPlayerGuild(player);
                    
                    if (guild != null) {
                        // Отправляем информацию о гильдии
                        java.util.List<UUID> memberIds = new java.util.ArrayList<>(guild.getMembers());
                        java.util.List<String> memberNames = new java.util.ArrayList<>();
                        
                        for (UUID memberId : memberIds) {
                            net.minecraft.server.level.ServerPlayer member = 
                                player.server.getPlayerList().getPlayer(memberId);
                            if (member != null) {
                                memberNames.add(member.getName().getString());
                            } else {
                                memberNames.add(memberId.toString().substring(0, 8) + "...");
                            }
                        }
                        
                        com.bmfalkye.network.NetworkHandler.INSTANCE.sendTo(
                            new SendGuildInfoPacket(guild.getId(), guild.getName(), guild.getDescription(),
                                guild.getLeader(), memberIds, memberNames, guild.getGuildLevel(),
                                guild.getGuildXP(), guild.getMaxMembers()),
                            player.connection.connection,
                            net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
                        );
                    } else {
                        // Игрок не в гильдии, отправляем пустую информацию
                        com.bmfalkye.network.NetworkHandler.INSTANCE.sendTo(
                            new SendGuildInfoPacket("", "", "", null, 
                                new java.util.ArrayList<>(), new java.util.ArrayList<>(), 0, 0, 0),
                            player.connection.connection,
                            net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
                        );
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    public static class SendGuildInfoPacket {
        private final String guildId;
        private final String name;
        private final String description;
        private final UUID leader;
        private final List<UUID> memberIds;
        private final List<String> memberNames;
        private final int level;
        private final int xp;
        private final int maxMembers;
        
        public SendGuildInfoPacket(String guildId, String name, String description, UUID leader,
                                  List<UUID> memberIds, List<String> memberNames, 
                                  int level, int xp, int maxMembers) {
            this.guildId = guildId;
            this.name = name;
            this.description = description;
            this.leader = leader;
            this.memberIds = memberIds;
            this.memberNames = memberNames;
            this.level = level;
            this.xp = xp;
            this.maxMembers = maxMembers;
        }
        
        public static void encode(SendGuildInfoPacket msg, FriendlyByteBuf buffer) {
            buffer.writeUtf(msg.guildId != null ? msg.guildId : "");
            buffer.writeUtf(msg.name != null ? msg.name : "");
            buffer.writeUtf(msg.description != null ? msg.description : "");
            buffer.writeBoolean(msg.leader != null);
            if (msg.leader != null) {
                buffer.writeUUID(msg.leader);
            }
            buffer.writeInt(msg.memberIds.size());
            for (int i = 0; i < msg.memberIds.size(); i++) {
                buffer.writeUUID(msg.memberIds.get(i));
                buffer.writeUtf(i < msg.memberNames.size() ? msg.memberNames.get(i) : "");
            }
            buffer.writeInt(msg.level);
            buffer.writeInt(msg.xp);
            buffer.writeInt(msg.maxMembers);
        }
        
        public static SendGuildInfoPacket decode(FriendlyByteBuf buffer) {
            String guildId = buffer.readUtf();
            String name = buffer.readUtf();
            String description = buffer.readUtf();
            UUID leader = buffer.readBoolean() ? buffer.readUUID() : null;
            int memberCount = buffer.readInt();
            List<UUID> memberIds = new ArrayList<>();
            List<String> memberNames = new ArrayList<>();
            for (int i = 0; i < memberCount; i++) {
                memberIds.add(buffer.readUUID());
                memberNames.add(buffer.readUtf());
            }
            int level = buffer.readInt();
            int xp = buffer.readInt();
            int maxMembers = buffer.readInt();
            return new SendGuildInfoPacket(guildId, name, description, leader, 
                memberIds, memberNames, level, xp, maxMembers);
        }
        
        public static void handle(SendGuildInfoPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> {
                    com.bmfalkye.client.ClientPacketHandler.handleGuildInfo(msg);
                });
            });
            ctx.get().setPacketHandled(true);
        }
        
        public String getGuildId() { return guildId; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public UUID getLeader() { return leader; }
        public List<UUID> getMemberIds() { return memberIds; }
        public List<String> getMemberNames() { return memberNames; }
        public int getLevel() { return level; }
        public int getXp() { return xp; }
        public int getMaxMembers() { return maxMembers; }
    }
}

