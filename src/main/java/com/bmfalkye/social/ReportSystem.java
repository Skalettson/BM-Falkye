package com.bmfalkye.social;

import com.bmfalkye.util.ModLogger;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Система репортов и модерации
 */
public class ReportSystem {
    public enum ReportReason {
        CHEATING,       // Читы
        HARASSMENT,     // Оскорбления
        SPAM,           // Спам
        INAPPROPRIATE,  // Неподходящее поведение
        OTHER           // Другое
    }
    
    // Репорты (ID репорта -> Репорт)
    private static final Map<String, Report> reports = new ConcurrentHashMap<>();
    // UUID игрока -> количество репортов
    private static final Map<UUID, Integer> playerReportCounts = new ConcurrentHashMap<>();
    
    /**
     * Создаёт репорт
     */
    public static String createReport(ServerPlayer reporter, ServerPlayer reported, ReportReason reason, String description) {
        if (reporter == null || reported == null || reporter.equals(reported)) {
            return null;
        }
        
        String reportId = UUID.randomUUID().toString();
        Report report = new Report(reportId, reporter.getUUID(), reported.getUUID(), reason, description);
        reports.put(reportId, report);
        
        // Увеличиваем счётчик репортов
        playerReportCounts.put(reported.getUUID(), 
            playerReportCounts.getOrDefault(reported.getUUID(), 0) + 1);
        
        ModLogger.warn("Player reported", "reporter", reporter.getName().getString(), 
            "reported", reported.getName().getString(), "reason", reason.name());
        
        return reportId;
    }
    
    /**
     * Получает все репорты на игрока
     */
    public static List<Report> getReportsForPlayer(ServerPlayer player) {
        if (player == null) {
            return Collections.emptyList();
        }
        
        List<Report> playerReports = new ArrayList<>();
        for (Report report : reports.values()) {
            if (report.getReportedPlayer().equals(player.getUUID())) {
                playerReports.add(report);
            }
        }
        return playerReports;
    }
    
    /**
     * Получает количество репортов на игрока
     */
    public static int getReportCount(ServerPlayer player) {
        return playerReportCounts.getOrDefault(player.getUUID(), 0);
    }
    
    /**
     * Репорт
     */
    public static class Report {
        private final String id;
        private final UUID reporter;
        private final UUID reportedPlayer;
        private final ReportReason reason;
        private final String description;
        private final long timestamp;
        private boolean reviewed = false;
        
        public Report(String id, UUID reporter, UUID reportedPlayer, ReportReason reason, String description) {
            this.id = id;
            this.reporter = reporter;
            this.reportedPlayer = reportedPlayer;
            this.reason = reason;
            this.description = description;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getId() { return id; }
        public UUID getReporter() { return reporter; }
        public UUID getReportedPlayer() { return reportedPlayer; }
        public ReportReason getReason() { return reason; }
        public String getDescription() { return description; }
        public long getTimestamp() { return timestamp; }
        public boolean isReviewed() { return reviewed; }
        public void setReviewed(boolean reviewed) { this.reviewed = reviewed; }
    }
}

