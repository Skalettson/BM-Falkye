package com.bmfalkye.network;

import com.bmfalkye.util.ModLogger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Система батчинга сетевых пакетов для оптимизации трафика
 * Группирует пакеты и отправляет их батчами, уменьшая количество сетевых запросов
 */
public class PacketBatcher {
    
    /**
     * Батч пакетов для игрока
     */
    private static class PacketBatch {
        private final List<BatchedPacket> packets = new ArrayList<>();
        private long lastFlushTime = System.currentTimeMillis();
        private boolean hasCriticalPackets = false;
        
        void addPacket(BatchedPacket packet) {
            packets.add(packet);
            if (packet.priority == NetworkErrorHandler.PacketPriority.CRITICAL) {
                hasCriticalPackets = true;
            }
        }
        
        void clear() {
            packets.clear();
            hasCriticalPackets = false;
        }
        
        boolean isEmpty() {
            return packets.isEmpty();
        }
        
        int size() {
            return packets.size();
        }
    }
    
    /**
     * Пакет в батче
     */
    private static class BatchedPacket {
        final Object packet;
        final NetworkDirection direction;
        final NetworkErrorHandler.PacketPriority priority;
        final Supplier<Object> packetSupplier;
        final long timestamp;
        
        BatchedPacket(Object packet, NetworkDirection direction, 
                     NetworkErrorHandler.PacketPriority priority,
                     Supplier<Object> packetSupplier) {
            this.packet = packet;
            this.direction = direction;
            this.priority = priority;
            this.packetSupplier = packetSupplier;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    // Батчи пакетов для каждого игрока: UUID -> PacketBatch
    private static final Map<UUID, PacketBatch> playerBatches = new ConcurrentHashMap<>();
    
    // Интервал отправки батчей (мс) - отправляем батчи каждые 50мс
    private static final long BATCH_FLUSH_INTERVAL_MS = 50;
    
    // Максимальный размер батча (количество пакетов)
    private static final int MAX_BATCH_SIZE = 20;
    
    // Максимальное время ожидания для критичных пакетов (мс) - критичные отправляются сразу
    private static final long CRITICAL_PACKET_DELAY_MS = 0;
    
    // Максимальное время ожидания для обычных пакетов (мс)
    private static final long NORMAL_PACKET_DELAY_MS = 100;
    
    /**
     * Добавляет пакет в батч для игрока
     * Критичные пакеты отправляются немедленно, остальные батчатся
     */
    public static void addToBatch(SimpleChannel channel, Object packet, ServerPlayer player,
                                  NetworkDirection direction, NetworkErrorHandler.PacketPriority priority,
                                  Supplier<Object> packetSupplier) {
        if (player == null || packet == null) {
            return;
        }
        
        UUID playerUUID = player.getUUID();
        
        // Критичные пакеты отправляем сразу без батчинга
        if (priority == NetworkErrorHandler.PacketPriority.CRITICAL) {
            NetworkErrorHandler.sendPacketSafely(channel, packet, player, direction, priority, packetSupplier);
            return;
        }
        
        // Добавляем пакет в батч
        PacketBatch batch = playerBatches.computeIfAbsent(playerUUID, k -> new PacketBatch());
        batch.addPacket(new BatchedPacket(packet, direction, priority, packetSupplier));
        batch.lastFlushTime = System.currentTimeMillis();
        
        // Если батч достиг максимального размера, отправляем его
        if (batch.size() >= MAX_BATCH_SIZE) {
            flushBatch(channel, playerUUID, player);
        }
    }
    
    /**
     * Отправляет батч пакетов для игрока
     */
    public static void flushBatch(SimpleChannel channel, UUID playerUUID, ServerPlayer player) {
        if (playerUUID == null || player == null) {
            return;
        }
        
        PacketBatch batch = playerBatches.get(playerUUID);
        if (batch == null || batch.isEmpty()) {
            return;
        }
        
        // Сортируем пакеты по приоритету (HIGH -> NORMAL -> LOW)
        List<BatchedPacket> sortedPackets = new ArrayList<>(batch.packets);
        sortedPackets.sort((a, b) -> {
            int priorityOrderA = getPriorityOrder(a.priority);
            int priorityOrderB = getPriorityOrder(b.priority);
            return Integer.compare(priorityOrderA, priorityOrderB);
        });
        
        // Отправляем пакеты в порядке приоритета
        int sentCount = 0;
        int failedCount = 0;
        
        for (BatchedPacket batchedPacket : sortedPackets) {
            try {
                NetworkErrorHandler.SendResult result = NetworkErrorHandler.sendPacketSafely(
                    channel, batchedPacket.packet, player, batchedPacket.direction,
                    batchedPacket.priority, batchedPacket.packetSupplier);
                
                if (result == NetworkErrorHandler.SendResult.SUCCESS) {
                    sentCount++;
                } else {
                    failedCount++;
                }
            } catch (Exception e) {
                ModLogger.error("Error sending batched packet", 
                    "player", player.getName().getString(),
                    "error", e.getMessage());
                failedCount++;
            }
        }
        
        ModLogger.logNetwork("Batch flushed", 
            "player", player.getName().getString(),
            "totalPackets", sortedPackets.size(),
            "sent", sentCount,
            "failed", failedCount);
        
        // Очищаем батч
        batch.clear();
    }
    
    /**
     * Получает порядок приоритета (меньше = выше приоритет)
     */
    private static int getPriorityOrder(NetworkErrorHandler.PacketPriority priority) {
        switch (priority) {
            case HIGH: return 1;
            case NORMAL: return 2;
            case LOW: return 3;
            default: return 4;
        }
    }
    
    /**
     * Обрабатывает все батчи (вызывается периодически)
     * Отправляет батчи, которые достигли интервала отправки
     * @deprecated Используйте processBatches(SimpleChannel, MinecraftServer)
     */
    @Deprecated
    public static void processBatches(SimpleChannel channel) {
        // Этот метод больше не используется - используйте перегрузку с сервером
        // Оставлен для обратной совместимости
    }
    
    /**
     * Обрабатывает все батчи (перегрузка с сервером)
     * @param channel Канал для отправки
     * @param server Сервер для получения игроков
     */
    public static void processBatches(SimpleChannel channel, net.minecraft.server.MinecraftServer server) {
        if (server == null) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        List<UUID> playersToFlush = new ArrayList<>();
        
        // Находим игроков, чьи батчи нужно отправить
        for (Map.Entry<UUID, PacketBatch> entry : playerBatches.entrySet()) {
            UUID playerUUID = entry.getKey();
            PacketBatch batch = entry.getValue();
            
            if (batch.isEmpty()) {
                continue;
            }
            
            long timeSinceLastFlush = currentTime - batch.lastFlushTime;
            
            // Проверяем, нужно ли отправить батч
            boolean shouldFlush = false;
            
            // Если есть критичные пакеты (не должно быть, но на всякий случай)
            if (batch.hasCriticalPackets && timeSinceLastFlush >= CRITICAL_PACKET_DELAY_MS) {
                shouldFlush = true;
            }
            // Если прошло достаточно времени для обычных пакетов
            else if (timeSinceLastFlush >= BATCH_FLUSH_INTERVAL_MS) {
                shouldFlush = true;
            }
            // Если батч достиг максимального размера
            else if (batch.size() >= MAX_BATCH_SIZE) {
                shouldFlush = true;
            }
            
            if (shouldFlush) {
                playersToFlush.add(playerUUID);
            }
        }
        
        // Отправляем батчи
        for (UUID playerUUID : playersToFlush) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            if (player != null && player.isAlive()) {
                flushBatch(channel, playerUUID, player);
            } else {
                // Игрок офлайн - удаляем батч
                playerBatches.remove(playerUUID);
            }
        }
    }
    
    /**
     * Принудительно отправляет все батчи для игрока
     */
    public static void flushAllForPlayer(SimpleChannel channel, ServerPlayer player) {
        if (player == null) {
            return;
        }
        
        UUID playerUUID = player.getUUID();
        flushBatch(channel, playerUUID, player);
    }
    
    /**
     * Очищает батчи для игрока (при отключении)
     */
    public static void clearBatchesForPlayer(UUID playerUUID) {
        if (playerUUID != null) {
            playerBatches.remove(playerUUID);
        }
    }
    
    /**
     * Очищает все батчи
     */
    public static void clearAllBatches() {
        int count = playerBatches.size();
        playerBatches.clear();
        ModLogger.logNetwork("All packet batches cleared", "count", count);
    }
    
    /**
     * Получает статистику батчинга
     */
    public static BatchStats getStats() {
        int totalBatches = playerBatches.size();
        int totalPackets = 0;
        
        for (PacketBatch batch : playerBatches.values()) {
            totalPackets += batch.size();
        }
        
        return new BatchStats(totalBatches, totalPackets);
    }
    
    /**
     * Статистика батчинга
     */
    public static class BatchStats {
        private final int activeBatches;
        private final int queuedPackets;
        
        public BatchStats(int activeBatches, int queuedPackets) {
            this.activeBatches = activeBatches;
            this.queuedPackets = queuedPackets;
        }
        
        public int getActiveBatches() {
            return activeBatches;
        }
        
        public int getQueuedPackets() {
            return queuedPackets;
        }
    }
}

