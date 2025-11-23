package com.bmfalkye.network;

import com.bmfalkye.util.ModLogger;
import net.minecraft.server.level.ServerPlayer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Очередь для сетевых запросов
 */
public class PacketQueue {
    private static final Queue<QueuedPacket> packetQueue = new ConcurrentLinkedQueue<>();
    private static final int MAX_QUEUE_SIZE = 1000;
    
    /**
     * Добавляет пакет в очередь
     */
    public static void enqueue(ServerPlayer player, Runnable packetHandler) {
        if (packetQueue.size() >= MAX_QUEUE_SIZE) {
            ModLogger.warn("Packet queue is full, dropping packet");
            return;
        }
        
        packetQueue.offer(new QueuedPacket(player, packetHandler));
    }
    
    /**
     * Обрабатывает пакеты из очереди
     */
    public static void processQueue() {
        int processed = 0;
        int maxPerTick = 10; // Максимум 10 пакетов за тик
        
        while (processed < maxPerTick && !packetQueue.isEmpty()) {
            QueuedPacket packet = packetQueue.poll();
            if (packet != null) {
                try {
                    // Проверяем, что игрок ещё онлайн
                    if (packet.getPlayer() != null && packet.getPlayer().isAlive()) {
                        packet.getHandler().run();
                    }
                } catch (Exception e) {
                    ModLogger.error("Error processing queued packet: {}", e.getMessage());
                }
                processed++;
            }
        }
    }
    
    /**
     * Очищает очередь для игрока
     */
    public static void clearForPlayer(ServerPlayer player) {
        if (player == null) {
            return;
        }
        packetQueue.removeIf(p -> p.getPlayer() != null && p.getPlayer().getUUID().equals(player.getUUID()));
    }
    
    /**
     * Очищает всю очередь
     */
    public static void clear() {
        packetQueue.clear();
    }
    
    /**
     * Возвращает размер очереди
     */
    public static int size() {
        return packetQueue.size();
    }
    
    /**
     * Пакет в очереди
     */
    private static class QueuedPacket {
        private final ServerPlayer player;
        private final Runnable handler;
        
        public QueuedPacket(ServerPlayer player, Runnable handler) {
            this.player = player;
            this.handler = handler;
        }
        
        public ServerPlayer getPlayer() {
            return player;
        }
        
        public Runnable getHandler() {
            return handler;
        }
    }
}

