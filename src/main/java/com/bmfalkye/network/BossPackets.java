package com.bmfalkye.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/**
 * Сетевые пакеты для системы боссов
 */
public class BossPackets {
    
    public static class RequestBossesPacket {
        public RequestBossesPacket() {}
        
        public static void encode(RequestBossesPacket msg, FriendlyByteBuf buffer) {}
        
        public static RequestBossesPacket decode(FriendlyByteBuf buffer) {
            return new RequestBossesPacket();
        }
        
        public static void handle(RequestBossesPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                net.minecraft.server.level.ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    java.util.List<com.bmfalkye.npc.BossSystem.Boss> availableBosses = 
                        com.bmfalkye.npc.BossSystem.getAvailableBosses(player);
                    java.util.List<String> bossIds = new java.util.ArrayList<>();
                    java.util.List<String> bossNames = new java.util.ArrayList<>();
                    java.util.List<String> bossDescriptions = new java.util.ArrayList<>();
                    java.util.List<Integer> bossDifficulties = new java.util.ArrayList<>();
                    java.util.List<Boolean> bossUnlocked = new java.util.ArrayList<>();
                    
                    for (com.bmfalkye.npc.BossSystem.Boss boss : availableBosses) {
                        bossIds.add(boss.getId());
                        bossNames.add(boss.getName());
                        bossDescriptions.add(boss.getDescription());
                        bossDifficulties.add(boss.getDifficulty());
                        bossUnlocked.add(boss.isUnlocked(player));
                    }
                    
                    com.bmfalkye.network.NetworkHandler.INSTANCE.sendTo(
                        new SendBossesPacket(bossIds, bossNames, bossDescriptions, 
                            bossDifficulties, bossUnlocked),
                        player.connection.connection,
                        net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
                    );
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    public static class ChallengeBossPacket {
        private final String bossId;
        
        public ChallengeBossPacket(String bossId) {
            this.bossId = bossId;
        }
        
        public static void encode(ChallengeBossPacket msg, FriendlyByteBuf buffer) {
            buffer.writeUtf(msg.bossId);
        }
        
        public static ChallengeBossPacket decode(FriendlyByteBuf buffer) {
            return new ChallengeBossPacket(buffer.readUtf());
        }
        
        public static void handle(ChallengeBossPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                net.minecraft.server.level.ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    com.bmfalkye.npc.BossSystem.Boss boss = 
                        com.bmfalkye.npc.BossSystem.getBoss(msg.bossId);
                    if (boss != null && boss.isUnlocked(player)) {
                        // Начинаем бой с боссом через GameManager
                        // Создаём специальную конфигурацию для босса
                        com.bmfalkye.game.MatchConfig config = new com.bmfalkye.game.MatchConfig();
                        config.setDifficulty(com.bmfalkye.game.MatchConfig.Difficulty.values()[
                            Math.min(boss.getDifficulty() / 2, 3)]); // Преобразуем сложность босса
                        config.setAllowLeader(true);
                        config.setAllowWeather(true);
                        
                        // Создаём специального NPC-босса и начинаем игру
                        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                            // Создаём временного villager-босса рядом с игроком
                            net.minecraft.world.entity.npc.Villager bossNPC = 
                                com.bmfalkye.npc.BossSystem.createBossNPC(serverLevel, boss, player);
                            
                            if (bossNPC != null) {
                                // Начинаем игру с боссом
                                com.bmfalkye.game.GameManager.startBossMatch(player, bossNPC, boss, config);
                                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                    "§aВызов босса " + boss.getName() + " принят!"));
                            } else {
                                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                    "§cНе удалось создать NPC-босса"));
                            }
                        } else {
                            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                "§cОшибка: неверный уровень"));
                        }
                    } else {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§cБосс не разблокирован или не найден!"));
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    public static class SendBossesPacket {
        private final java.util.List<String> bossIds;
        private final java.util.List<String> bossNames;
        private final java.util.List<String> bossDescriptions;
        private final java.util.List<Integer> bossDifficulties;
        private final java.util.List<Boolean> bossUnlocked;
        
        public SendBossesPacket(java.util.List<String> bossIds, java.util.List<String> bossNames, 
                              java.util.List<String> bossDescriptions, java.util.List<Integer> bossDifficulties,
                              java.util.List<Boolean> bossUnlocked) {
            this.bossIds = bossIds;
            this.bossNames = bossNames;
            this.bossDescriptions = bossDescriptions;
            this.bossDifficulties = bossDifficulties;
            this.bossUnlocked = bossUnlocked;
        }
        
        public static void encode(SendBossesPacket msg, FriendlyByteBuf buffer) {
            buffer.writeInt(msg.bossIds.size());
            for (int i = 0; i < msg.bossIds.size(); i++) {
                buffer.writeUtf(msg.bossIds.get(i));
                buffer.writeUtf(i < msg.bossNames.size() ? msg.bossNames.get(i) : "");
                buffer.writeUtf(i < msg.bossDescriptions.size() ? msg.bossDescriptions.get(i) : "");
                buffer.writeInt(i < msg.bossDifficulties.size() ? msg.bossDifficulties.get(i) : 0);
                buffer.writeBoolean(i < msg.bossUnlocked.size() ? msg.bossUnlocked.get(i) : false);
            }
        }
        
        public static SendBossesPacket decode(FriendlyByteBuf buffer) {
            int count = buffer.readInt();
            java.util.List<String> ids = new java.util.ArrayList<>();
            java.util.List<String> names = new java.util.ArrayList<>();
            java.util.List<String> descriptions = new java.util.ArrayList<>();
            java.util.List<Integer> difficulties = new java.util.ArrayList<>();
            java.util.List<Boolean> unlocked = new java.util.ArrayList<>();
            for (int i = 0; i < count; i++) {
                ids.add(buffer.readUtf());
                names.add(buffer.readUtf());
                descriptions.add(buffer.readUtf());
                difficulties.add(buffer.readInt());
                unlocked.add(buffer.readBoolean());
            }
            return new SendBossesPacket(ids, names, descriptions, difficulties, unlocked);
        }
        
        public static void handle(SendBossesPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> {
                    com.bmfalkye.client.ClientPacketHandler.handleBossesList(msg);
                });
            });
            ctx.get().setPacketHandled(true);
        }
        
        public java.util.List<String> getBossIds() { return bossIds; }
        public java.util.List<String> getBossNames() { return bossNames; }
        public java.util.List<String> getBossDescriptions() { return bossDescriptions; }
        public java.util.List<Integer> getBossDifficulties() { return bossDifficulties; }
        public java.util.List<Boolean> getBossUnlocked() { return bossUnlocked; }
    }
}

