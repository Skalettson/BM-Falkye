package com.bmfalkye.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Сборщик контекста для обработки ошибок
 * Собирает информацию о состоянии игры, игроках, сессиях и других важных данных
 * для более детального логирования ошибок
 */
public class ErrorContextCollector {
    
    /**
     * Собирает контекст для ошибки
     */
    public static Map<String, Object> collectContext(Throwable error, Object... additionalContext) {
        Map<String, Object> context = new LinkedHashMap<>();
        
        // Базовая информация об ошибке
        context.put("errorType", error.getClass().getSimpleName());
        context.put("errorMessage", error.getMessage());
        
        // Stack trace (первые 5 строк)
        StackTraceElement[] stackTrace = error.getStackTrace();
        if (stackTrace.length > 0) {
            List<String> stackTraceLines = new ArrayList<>();
            for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
                StackTraceElement element = stackTrace[i];
                stackTraceLines.add(String.format("%s.%s(%s:%d)",
                    element.getClassName(),
                    element.getMethodName(),
                    element.getFileName(),
                    element.getLineNumber()));
            }
            context.put("stackTrace", stackTraceLines);
        }
        
        // Причина ошибки (если есть)
        if (error.getCause() != null) {
            context.put("cause", error.getCause().getClass().getSimpleName() + ": " + error.getCause().getMessage());
        }
        
        // Дополнительный контекст из параметров
        for (int i = 0; i < additionalContext.length; i += 2) {
            if (i + 1 < additionalContext.length) {
                String key = String.valueOf(additionalContext[i]);
                Object value = additionalContext[i + 1];
                context.put(key, sanitizeValue(value));
            }
        }
        
        // Системная информация
        context.put("timestamp", System.currentTimeMillis());
        context.put("thread", Thread.currentThread().getName());
        
        return context;
    }
    
    /**
     * Собирает контекст для игрока
     */
    public static Map<String, Object> collectPlayerContext(ServerPlayer player) {
        Map<String, Object> context = new LinkedHashMap<>();
        
        if (player == null) {
            context.put("player", "null");
            return context;
        }
        
        context.put("playerName", player.getName().getString());
        context.put("playerUUID", player.getUUID().toString());
        context.put("playerLevel", player.experienceLevel);
        context.put("playerHealth", player.getHealth());
        context.put("playerMaxHealth", player.getMaxHealth());
        
        if (player.level() != null) {
            context.put("dimension", player.level().dimension().location().toString());
            context.put("position", String.format("x=%.2f, y=%.2f, z=%.2f",
                player.getX(), player.getY(), player.getZ()));
        }
        
        return context;
    }
    
    /**
     * Собирает контекст для сущности
     */
    public static Map<String, Object> collectEntityContext(Entity entity) {
        Map<String, Object> context = new LinkedHashMap<>();
        
        if (entity == null) {
            context.put("entity", "null");
            return context;
        }
        
        context.put("entityType", entity.getType().toString());
        context.put("entityName", entity.getName().getString());
        context.put("entityUUID", entity.getUUID().toString());
        
        if (entity.level() != null) {
            context.put("dimension", entity.level().dimension().location().toString());
            context.put("position", String.format("x=%.2f, y=%.2f, z=%.2f",
                entity.getX(), entity.getY(), entity.getZ()));
        }
        
        return context;
    }
    
    /**
     * Собирает контекст для игровой сессии
     */
    public static Map<String, Object> collectGameSessionContext(com.bmfalkye.game.FalkyeGameSession session) {
        Map<String, Object> context = new LinkedHashMap<>();
        
        if (session == null) {
            context.put("session", "null");
            return context;
        }
        
        context.put("currentRound", session.getCurrentRound());
        context.put("gameEnded", session.isGameEnded());
        
        if (session.getPlayer1() != null) {
            context.put("player1Name", session.getPlayer1().getName().getString());
            context.put("player1UUID", session.getPlayer1().getUUID().toString());
        }
        
        if (session.getPlayer2() != null) {
            context.put("player2Name", session.getPlayer2().getName().getString());
            context.put("player2UUID", session.getPlayer2().getUUID().toString());
        }
        
        context.put("playingWithVillager", session.isPlayingWithVillager());
        
        if (session.getCurrentPlayerUUID() != null) {
            context.put("currentPlayerUUID", session.getCurrentPlayerUUID().toString());
        }
        
        context.put("hand1Size", session.getHand(session.getPlayer1()).size());
        if (session.getPlayer2() != null) {
            context.put("hand2Size", session.getHand(session.getPlayer2()).size());
        }
        
        return context;
    }
    
    /**
     * Собирает контекст для сетевого пакета
     */
    public static Map<String, Object> collectNetworkContext(String packetType, Object packetData) {
        Map<String, Object> context = new LinkedHashMap<>();
        
        context.put("packetType", packetType);
        
        if (packetData != null) {
            context.put("packetData", sanitizeValue(packetData));
        }
        
        return context;
    }
    
    /**
     * Собирает контекст для операции с данными
     */
    public static Map<String, Object> collectDataOperationContext(String operation, String dataType, Object dataId) {
        Map<String, Object> context = new LinkedHashMap<>();
        
        context.put("operation", operation);
        context.put("dataType", dataType);
        
        if (dataId != null) {
            context.put("dataId", sanitizeValue(dataId));
        }
        
        return context;
    }
    
    /**
     * Форматирует контекст в строку для логирования
     */
    public static String formatContext(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return "";
        }
        
        return context.entrySet().stream()
            .map(entry -> entry.getKey() + "=" + sanitizeValue(entry.getValue()))
            .collect(Collectors.joining(", ", "[", "]"));
    }
    
    /**
     * Объединяет несколько контекстов
     */
    @SafeVarargs
    public static Map<String, Object> mergeContexts(Map<String, Object>... contexts) {
        Map<String, Object> merged = new LinkedHashMap<>();
        
        for (Map<String, Object> context : contexts) {
            if (context != null) {
                merged.putAll(context);
            }
        }
        
        return merged;
    }
    
    /**
     * Очищает значение для безопасного логирования
     */
    private static Object sanitizeValue(Object value) {
        if (value == null) {
            return "null";
        }
        
        // Ограничиваем длину строк
        if (value instanceof String) {
            String str = (String) value;
            if (str.length() > 200) {
                return str.substring(0, 197) + "...";
            }
            return str;
        }
        
        // Ограничиваем размер коллекций
        if (value instanceof Collection) {
            Collection<?> collection = (Collection<?>) value;
            if (collection.size() > 10) {
                return String.format("Collection(size=%d, first10=%s)", 
                    collection.size(),
                    collection.stream().limit(10).map(ErrorContextCollector::sanitizeValue).collect(Collectors.toList()));
            }
            return collection.stream().map(ErrorContextCollector::sanitizeValue).collect(Collectors.toList());
        }
        
        // Ограничиваем размер массивов
        if (value.getClass().isArray()) {
            Object[] array = (Object[]) value;
            if (array.length > 10) {
                return String.format("Array(length=%d, first10=%s)",
                    array.length,
                    Arrays.stream(array).limit(10).map(ErrorContextCollector::sanitizeValue).collect(Collectors.toList()));
            }
            return Arrays.stream(array).map(ErrorContextCollector::sanitizeValue).collect(Collectors.toList());
        }
        
        return value;
    }
}

