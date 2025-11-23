package com.bmfalkye.client.gui.backup.logic;

import java.util.List;

/**
 * БЭКАП ЛОГИКИ ActionLogRenderer
 * 
 * Этот класс содержит всю логику работы с логом действий БЕЗ визуального оформления.
 */
public class ActionLogRendererLogic {
    
    /**
     * Вычисляет видимые записи лога с учётом прокрутки
     */
    public static VisibleLogEntries calculateVisibleEntries(List<String> actionLog, 
                                                           int logHeight, 
                                                           int logScrollOffset,
                                                           int fontLineHeight) {
        int headerHeight = 30;
        int maxVisibleEntries = Math.min(6, (logHeight - headerHeight) / (fontLineHeight + 3));
        
        int startIndex = Math.max(0, logScrollOffset);
        int endIndex = Math.min(startIndex + maxVisibleEntries, actionLog.size());
        
        return new VisibleLogEntries(startIndex, endIndex, maxVisibleEntries);
    }
    
    /**
     * Разбивает запись лога на строки с учётом ширины
     */
    public static List<String> splitLogEntry(String entry, int maxWidth) {
        // Упрощённая логика разбиения
        // В реальной реализации используется Font.split()
        return java.util.Arrays.asList(entry.split(" "));
    }
    
    /**
     * Структура для хранения информации о видимых записях
     */
    public static class VisibleLogEntries {
        public final int startIndex;
        public final int endIndex;
        public final int maxVisible;
        
        public VisibleLogEntries(int startIndex, int endIndex, int maxVisible) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.maxVisible = maxVisible;
        }
    }
}

