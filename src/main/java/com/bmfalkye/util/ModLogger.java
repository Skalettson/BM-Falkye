package com.bmfalkye.util;

import com.bmfalkye.BMFalkye;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Расширенный логгер для мода с записью в отдельный файл
 */
public class ModLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(BMFalkye.MOD_ID);
    private static PrintWriter fileWriter;
    private static final ReentrantLock lock = new ReentrantLock();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static boolean initialized = false;
    
    /**
     * Инициализирует файловый логгер
     */
    public static void initialize() {
        if (initialized) return;
        
        try {
            // Создаём директорию для логов мода
            Path logsDir = Paths.get("logs", "bm_falkye");
            if (!Files.exists(logsDir)) {
                Files.createDirectories(logsDir);
            }
            
            // Создаём файл лога с датой в имени
            String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            File logFile = logsDir.resolve("bm_falkye_" + dateStr + ".log").toFile();
            
            // Если файл существует и больше 10MB, создаём новый с номером
            int fileNumber = 1;
            while (logFile.exists() && logFile.length() > 10 * 1024 * 1024) {
                logFile = logsDir.resolve("bm_falkye_" + dateStr + "_" + fileNumber + ".log").toFile();
                fileNumber++;
            }
            
            fileWriter = new PrintWriter(new FileWriter(logFile, true), true);
            initialized = true;
            
            info("=== BM Falkye Mod Logger Initialized ===");
            info("Log file: {}", logFile.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Failed to initialize mod logger file", e);
        }
    }
    
    /**
     * Закрывает файловый логгер
     */
    public static void shutdown() {
        lock.lock();
        try {
            if (fileWriter != null) {
                info("=== BM Falkye Mod Logger Shutting Down ===");
                fileWriter.close();
                fileWriter = null;
                initialized = false;
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Записывает сообщение в лог (консоль + файл)
     */
    private static void log(String level, String message, Object... args) {
        String formattedMessage = formatMessage(message, args);
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String logEntry = String.format("[%s] [%s] %s", timestamp, level, formattedMessage);
        
        // Записываем в консоль через стандартный логгер
        switch (level) {
            case "INFO" -> LOGGER.info(formattedMessage, args);
            case "DEBUG" -> LOGGER.debug(formattedMessage, args);
            case "WARN" -> LOGGER.warn(formattedMessage, args);
            case "ERROR" -> LOGGER.error(formattedMessage, args);
            case "TRACE" -> LOGGER.trace(formattedMessage, args);
        }
        
        // Записываем в файл
        lock.lock();
        try {
            if (fileWriter != null) {
                fileWriter.println(logEntry);
                fileWriter.flush();
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Форматирует сообщение с аргументами
     */
    private static String formatMessage(String message, Object... args) {
        if (args.length == 0) {
            return message;
        }
        
        try {
            // Простое форматирование с заменой {}
            String result = message;
            for (Object arg : args) {
                result = result.replaceFirst("\\{\\}", String.valueOf(arg));
            }
            return result;
        } catch (Exception e) {
            return message + " [Format error: " + e.getMessage() + "]";
        }
    }
    
    public static void info(String message, Object... args) {
        log("INFO", message, args);
    }
    
    public static void debug(String message, Object... args) {
        log("DEBUG", message, args);
    }
    
    public static void warn(String message, Object... args) {
        log("WARN", message, args);
    }
    
    public static void error(String message, Object... args) {
        log("ERROR", message, args);
    }
    
    public static void error(String message, Throwable throwable) {
        error(message, throwable, (Map<String, Object>) null);
    }
    
    /**
     * Логирует ошибку с расширенным контекстом
     */
    public static void error(String message, Throwable throwable, Map<String, Object> context) {
        String formattedMessage = formatMessage(message);
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        
        // Добавляем контекст к сообщению
        String contextStr = "";
        if (context != null && !context.isEmpty()) {
            contextStr = " | Context: " + ErrorContextCollector.formatContext(context);
        }
        
        String logEntry = String.format("[%s] [ERROR] %s%s", timestamp, formattedMessage, contextStr);
        
        LOGGER.error(formattedMessage + contextStr, throwable);
        
        lock.lock();
        try {
            if (fileWriter != null) {
                fileWriter.println(logEntry);
                throwable.printStackTrace(fileWriter);
                
                // Дополнительная информация о контексте
                if (context != null && !context.isEmpty()) {
                    fileWriter.println("Context details:");
                    for (Map.Entry<String, Object> entry : context.entrySet()) {
                        fileWriter.println("  " + entry.getKey() + " = " + entry.getValue());
                    }
                }
                
                fileWriter.flush();
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Логирует ошибку с автоматическим сбором контекста
     */
    public static void errorWithContext(String message, Throwable throwable, Object... contextData) {
        Map<String, Object> context = ErrorContextCollector.collectContext(throwable, contextData);
        error(message, throwable, context);
    }
    
    /**
     * Логирует ошибку и пытается восстановить состояние
     * 
     * @param message сообщение об ошибке
     * @param throwable исключение
     * @param player игрок, для которого произошла ошибка (может быть null)
     * @param errorType тип ошибки для восстановления
     * @param context контекст ошибки
     */
    public static void errorWithRecovery(String message, Throwable throwable, 
                                        net.minecraft.server.level.ServerPlayer player,
                                        com.bmfalkye.util.ErrorRecoverySystem.RecoverableErrorType errorType,
                                        Map<String, Object> context) {
        error(message, throwable, context);
        
        // Пытаемся восстановить состояние, если это некритичная ошибка
        if (player != null && errorType != null) {
            boolean recovered = com.bmfalkye.util.ErrorRecoverySystem.attemptRecovery(player, errorType, throwable);
            if (recovered) {
                info("Error recovery successful", "player", player.getName().getString(), "errorType", errorType.toString());
            }
        }
    }
    
    public static void trace(String message, Object... args) {
        log("TRACE", message, args);
    }
    
    /**
     * Логирует взаимодействие с Minecraft (сетевые пакеты, события и т.д.)
     */
    public static void logMinecraftInteraction(String action, Object... details) {
        StringBuilder sb = new StringBuilder("MINECRAFT INTERACTION: ").append(action);
        if (details.length > 0) {
            sb.append(" | Details: ");
            for (int i = 0; i < details.length; i += 2) {
                if (i + 1 < details.length) {
                    sb.append(details[i]).append("=").append(details[i + 1]);
                    if (i + 2 < details.length) sb.append(", ");
                }
            }
        }
        debug(sb.toString());
    }
    
    /**
     * Логирует игровую логику
     */
    public static void logGameLogic(String action, Object... details) {
        StringBuilder sb = new StringBuilder("GAME LOGIC: ").append(action);
        if (details.length > 0) {
            sb.append(" | Details: ");
            for (int i = 0; i < details.length; i += 2) {
                if (i + 1 < details.length) {
                    sb.append(details[i]).append("=").append(details[i + 1]);
                    if (i + 2 < details.length) sb.append(", ");
                }
            }
        }
        debug(sb.toString());
    }
    
    /**
     * Логирует сетевые операции
     */
    public static void logNetwork(String action, Object... details) {
        StringBuilder sb = new StringBuilder("NETWORK: ").append(action);
        if (details.length > 0) {
            sb.append(" | Details: ");
            for (int i = 0; i < details.length; i += 2) {
                if (i + 1 < details.length) {
                    sb.append(details[i]).append("=").append(details[i + 1]);
                    if (i + 2 < details.length) sb.append(", ");
                }
            }
        }
        debug(sb.toString());
    }
    
    /**
     * Логирует действия с картами
     */
    public static void logCardAction(String action, Object... details) {
        StringBuilder sb = new StringBuilder("CARD ACTION: ").append(action);
        if (details.length > 0) {
            sb.append(" | Details: ");
            for (int i = 0; i < details.length; i += 2) {
                if (i + 1 < details.length) {
                    sb.append(details[i]).append("=").append(details[i + 1]);
                    if (i + 2 < details.length) sb.append(", ");
                }
            }
        }
        info(sb.toString());
    }
    
    /**
     * Логирует действия игроков (ходы, пасы, смены ходов)
     */
    public static void logPlayerAction(String action, Object... details) {
        StringBuilder sb = new StringBuilder("PLAYER ACTION: ").append(action);
        if (details.length > 0) {
            sb.append(" | Details: ");
            for (int i = 0; i < details.length; i += 2) {
                if (i + 1 < details.length) {
                    sb.append(details[i]).append("=").append(details[i + 1]);
                    if (i + 2 < details.length) sb.append(", ");
                }
            }
        }
        info(sb.toString());
    }
    
    /**
     * Логирует события раундов (начало, конец, смена)
     */
    public static void logRoundEvent(String action, Object... details) {
        StringBuilder sb = new StringBuilder("ROUND EVENT: ").append(action);
        if (details.length > 0) {
            sb.append(" | Details: ");
            for (int i = 0; i < details.length; i += 2) {
                if (i + 1 < details.length) {
                    sb.append(details[i]).append("=").append(details[i + 1]);
                    if (i + 2 < details.length) sb.append(", ");
                }
            }
        }
        info(sb.toString());
    }
    
    /**
     * Логирует события игры (начало, конец, победа)
     */
    public static void logGameEvent(String action, Object... details) {
        StringBuilder sb = new StringBuilder("GAME EVENT: ").append(action);
        if (details.length > 0) {
            sb.append(" | Details: ");
            for (int i = 0; i < details.length; i += 2) {
                if (i + 1 < details.length) {
                    sb.append(details[i]).append("=").append(details[i + 1]);
                    if (i + 2 < details.length) sb.append(", ");
                }
            }
        }
        info(sb.toString());
    }
    
    /**
     * Логирует эффекты карт
     */
    public static void logCardEffect(String action, Object... details) {
        StringBuilder sb = new StringBuilder("CARD EFFECT: ").append(action);
        if (details.length > 0) {
            sb.append(" | Details: ");
            for (int i = 0; i < details.length; i += 2) {
                if (i + 1 < details.length) {
                    sb.append(details[i]).append("=").append(details[i + 1]);
                    if (i + 2 < details.length) sb.append(", ");
                }
            }
        }
        info(sb.toString());
    }
    
    /**
     * Логирует действия AI (жителя)
     */
    public static void logAIAction(String action, Object... details) {
        StringBuilder sb = new StringBuilder("AI ACTION: ").append(action);
        if (details.length > 0) {
            sb.append(" | Details: ");
            for (int i = 0; i < details.length; i += 2) {
                if (i + 1 < details.length) {
                    sb.append(details[i]).append("=").append(details[i + 1]);
                    if (i + 2 < details.length) sb.append(", ");
                }
            }
        }
        info(sb.toString());
    }
    
    /**
     * Логирует операции с данными (сохранение, загрузка)
     */
    public static void logDataOperation(String action, Object... details) {
        StringBuilder sb = new StringBuilder("DATA OPERATION: ").append(action);
        if (details.length > 0) {
            sb.append(" | Details: ");
            for (int i = 0; i < details.length; i += 2) {
                if (i + 1 < details.length) {
                    sb.append(details[i]).append("=").append(details[i + 1]);
                    if (i + 2 < details.length) sb.append(", ");
                }
            }
        }
        debug(sb.toString());
    }
}

