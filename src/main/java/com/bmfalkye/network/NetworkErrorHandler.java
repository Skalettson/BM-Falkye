package com.bmfalkye.network;

import com.bmfalkye.util.ModLogger;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Обработчик ошибок сети и потери пакетов
 * Предоставляет безопасную отправку пакетов с обработкой ошибок и механизм повторной отправки
 */
public class NetworkErrorHandler {
    private static final ScheduledExecutorService retryExecutor = Executors.newScheduledThreadPool(2);
    
    // Хранилище пакетов для повторной отправки
    private static final ConcurrentHashMap<UUID, PendingPacket> pendingPackets = new ConcurrentHashMap<>();
    
    // Максимальное количество попыток повторной отправки
    private static final int MAX_RETRY_ATTEMPTS = 3;
    
    // Интервал между попытками повторной отправки (в миллисекундах)
    private static final long RETRY_DELAY_MS = 1000;
    
    // Максимальное время хранения пакета в очереди (в миллисекундах)
    private static final long MAX_PACKET_AGE_MS = 10000;
    
    /**
     * Результат отправки пакета
     */
    public enum SendResult {
        SUCCESS,           // Пакет успешно отправлен
        FAILED,            // Пакет не удалось отправить
        CONNECTION_LOST,   // Соединение потеряно
        PLAYER_OFFLINE     // Игрок офлайн
    }
    
    /**
     * Критичность пакета
     */
    public enum PacketPriority {
        CRITICAL,    // Критичный пакет - требует гарантированной доставки (состояние игры, начало матча)
        HIGH,        // Важный пакет - желательна доставка (обновления UI, статистика)
        NORMAL,      // Обычный пакет - можно потерять (логи, уведомления)
        LOW          // Низкоприоритетный пакет - не критично (декоративные обновления)
    }
    
    /**
     * Информация о пакете в очереди повторной отправки
     */
    private static class PendingPacket {
        final Object packet;
        final ServerPlayer player;
        final NetworkDirection direction;
        final PacketPriority priority;
        final long timestamp;
        int retryCount;
        final Supplier<Object> packetSupplier; // Для создания нового экземпляра пакета при повторной отправке
        
        PendingPacket(Object packet, ServerPlayer player, NetworkDirection direction, 
                     PacketPriority priority, Supplier<Object> packetSupplier) {
            this.packet = packet;
            this.player = player;
            this.direction = direction;
            this.priority = priority;
            this.timestamp = System.currentTimeMillis();
            this.retryCount = 0;
            this.packetSupplier = packetSupplier;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > MAX_PACKET_AGE_MS;
        }
        
        boolean canRetry() {
            return retryCount < MAX_RETRY_ATTEMPTS && !isExpired();
        }
    }
    
    /**
     * Безопасная отправка пакета с обработкой ошибок
     * 
     * @param channel Канал для отправки
     * @param packet Пакет для отправки
     * @param player Игрок-получатель
     * @param direction Направление отправки
     * @param priority Приоритет пакета
     * @return Результат отправки
     */
    public static SendResult sendPacketSafely(SimpleChannel channel, Object packet, 
                                              ServerPlayer player, NetworkDirection direction,
                                              PacketPriority priority) {
        return sendPacketSafely(channel, packet, player, direction, priority, null);
    }
    
    /**
     * Безопасная отправка пакета с обработкой ошибок и возможностью повторной отправки
     * 
     * @param channel Канал для отправки
     * @param packet Пакет для отправки
     * @param player Игрок-получатель
     * @param direction Направление отправки
     * @param priority Приоритет пакета
     * @param packetSupplier Поставщик для создания нового экземпляра пакета при повторной отправке (null = не использовать повторную отправку)
     * @return Результат отправки
     */
    public static SendResult sendPacketSafely(SimpleChannel channel, Object packet, 
                                              ServerPlayer player, NetworkDirection direction,
                                              PacketPriority priority, Supplier<Object> packetSupplier) {
        if (player == null) {
            ModLogger.warn("Attempted to send packet to null player");
            return SendResult.PLAYER_OFFLINE;
        }
        
        // Проверяем состояние соединения
        if (!isPlayerConnected(player)) {
            ModLogger.logNetwork("Player is not connected, cannot send packet", 
                "player", player.getName().getString(),
                "priority", priority.name());
            
            // Если пакет критичный и есть поставщик, добавляем в очередь повторной отправки
            if (priority == PacketPriority.CRITICAL && packetSupplier != null) {
                scheduleRetry(channel, packet, player, direction, priority, packetSupplier);
            }
            
            return SendResult.CONNECTION_LOST;
        }
        
        try {
            // Пытаемся отправить пакет
            channel.sendTo(packet, player.connection.connection, direction);
            
            ModLogger.logNetwork("Packet sent successfully", 
                "player", player.getName().getString(),
                "priority", priority.name(),
                "packet", packet.getClass().getSimpleName());
            
            return SendResult.SUCCESS;
            
        } catch (Exception e) {
            ModLogger.error("Failed to send packet", e,
                "player", player.getName().getString(),
                "priority", priority.name(),
                "packet", packet.getClass().getSimpleName(),
                "error", e.getMessage());
            
            // Если пакет критичный или важный, добавляем в очередь повторной отправки
            if ((priority == PacketPriority.CRITICAL || priority == PacketPriority.HIGH) 
                && packetSupplier != null) {
                scheduleRetry(channel, packet, player, direction, priority, packetSupplier);
            }
            
            return SendResult.FAILED;
        }
    }
    
    /**
     * Проверяет, подключен ли игрок
     */
    private static boolean isPlayerConnected(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        
        try {
            Connection connection = player.connection != null ? player.connection.connection : null;
            if (connection == null) {
                return false;
            }
            
            // Проверяем, что соединение активно
            return connection.isConnected();
            
        } catch (Exception e) {
            ModLogger.warn("Error checking player connection", 
                "player", player.getName().getString(),
                "error", e.getMessage());
            return false;
        }
    }
    
    /**
     * Планирует повторную отправку пакета
     */
    private static void scheduleRetry(SimpleChannel channel, Object packet, ServerPlayer player,
                                     NetworkDirection direction, PacketPriority priority,
                                     Supplier<Object> packetSupplier) {
        UUID playerUUID = player.getUUID();
        String packetKey = playerUUID.toString() + "_" + packet.getClass().getSimpleName();
        
        // Создаём запись о пакете
        PendingPacket pendingPacket = new PendingPacket(packet, player, direction, priority, packetSupplier);
        pendingPackets.put(UUID.nameUUIDFromBytes(packetKey.getBytes()), pendingPacket);
        
        // Планируем повторную попытку
        retryExecutor.schedule(() -> {
            retryPacket(channel, pendingPacket);
        }, RETRY_DELAY_MS, TimeUnit.MILLISECONDS);
        
        ModLogger.logNetwork("Scheduled packet retry", 
            "player", player.getName().getString(),
            "priority", priority.name(),
            "packet", packet.getClass().getSimpleName(),
            "retryCount", String.valueOf(pendingPacket.retryCount + 1));
    }
    
    /**
     * Повторная попытка отправки пакета
     */
    private static void retryPacket(SimpleChannel channel, PendingPacket pendingPacket) {
        // Проверяем, не истёк ли пакет
        if (pendingPacket.isExpired()) {
            ModLogger.warn("Packet expired, removing from retry queue",
                "player", pendingPacket.player.getName().getString(),
                "packet", pendingPacket.packet.getClass().getSimpleName());
            removePendingPacket(pendingPacket);
            return;
        }
        
        // Проверяем, можно ли повторить
        if (!pendingPacket.canRetry()) {
            ModLogger.warn("Max retry attempts reached, removing packet from queue",
                "player", pendingPacket.player.getName().getString(),
                "packet", pendingPacket.packet.getClass().getSimpleName(),
                "retryCount", String.valueOf(pendingPacket.retryCount));
            removePendingPacket(pendingPacket);
            return;
        }
        
        // Проверяем, что игрок всё ещё онлайн
        if (!isPlayerConnected(pendingPacket.player)) {
            // Игрок офлайн, но пакет критичный - попробуем ещё раз позже
            if (pendingPacket.priority == PacketPriority.CRITICAL && pendingPacket.canRetry()) {
                pendingPacket.retryCount++;
                retryExecutor.schedule(() -> retryPacket(channel, pendingPacket), 
                    RETRY_DELAY_MS, TimeUnit.MILLISECONDS);
            } else {
                removePendingPacket(pendingPacket);
            }
            return;
        }
        
        // Пытаемся отправить пакет
        Object packetToSend = pendingPacket.packetSupplier != null 
            ? pendingPacket.packetSupplier.get() 
            : pendingPacket.packet;
        
        SendResult result = sendPacketSafely(channel, packetToSend, pendingPacket.player, 
            pendingPacket.direction, pendingPacket.priority, null); // Не используем повторную отправку для повторных попыток
        
        if (result == SendResult.SUCCESS) {
            ModLogger.logNetwork("Packet retry successful", 
                "player", pendingPacket.player.getName().getString(),
                "packet", pendingPacket.packet.getClass().getSimpleName(),
                "retryCount", String.valueOf(pendingPacket.retryCount + 1));
            removePendingPacket(pendingPacket);
        } else {
            // Не удалось отправить, увеличиваем счётчик и планируем следующую попытку
            pendingPacket.retryCount++;
            if (pendingPacket.canRetry()) {
                retryExecutor.schedule(() -> retryPacket(channel, pendingPacket), 
                    RETRY_DELAY_MS, TimeUnit.MILLISECONDS);
            } else {
                ModLogger.error("Failed to send packet after all retry attempts",
                    null,
                    "player", pendingPacket.player.getName().getString(),
                    "packet", pendingPacket.packet.getClass().getSimpleName(),
                    "retryCount", String.valueOf(pendingPacket.retryCount));
                removePendingPacket(pendingPacket);
            }
        }
    }
    
    /**
     * Удаляет пакет из очереди повторной отправки
     */
    private static void removePendingPacket(PendingPacket pendingPacket) {
        UUID playerUUID = pendingPacket.player.getUUID();
        String packetKey = playerUUID.toString() + "_" + pendingPacket.packet.getClass().getSimpleName();
        UUID key = UUID.nameUUIDFromBytes(packetKey.getBytes());
        pendingPackets.remove(key);
    }
    
    /**
     * Очищает все пакеты для указанного игрока
     * Вызывается при отключении игрока
     */
    public static void clearPlayerPackets(ServerPlayer player) {
        if (player == null) {
            return;
        }
        
        UUID playerUUID = player.getUUID();
        pendingPackets.entrySet().removeIf(entry -> 
            entry.getValue().player.getUUID().equals(playerUUID));
        
        ModLogger.logNetwork("Cleared pending packets for player",
            "player", player.getName().getString());
    }
    
    /**
     * Очищает устаревшие пакеты
     * Вызывается периодически для очистки очереди
     */
    public static void cleanupExpiredPackets() {
        int removed = 0;
        for (PendingPacket packet : pendingPackets.values()) {
            if (packet.isExpired()) {
                removePendingPacket(packet);
                removed++;
            }
        }
        
        if (removed > 0) {
            ModLogger.logNetwork("Cleaned up expired packets", "count", String.valueOf(removed));
        }
    }
    
    /**
     * Получает количество пакетов в очереди для указанного игрока
     */
    public static int getPendingPacketCount(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        
        UUID playerUUID = player.getUUID();
        return (int) pendingPackets.values().stream()
            .filter(p -> p.player.getUUID().equals(playerUUID))
            .count();
    }
    
    /**
     * Останавливает обработчик (вызывается при выключении сервера)
     */
    public static void shutdown() {
        retryExecutor.shutdown();
        try {
            if (!retryExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                retryExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            retryExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        pendingPackets.clear();
        ModLogger.info("NetworkErrorHandler shutdown complete");
    }
}

