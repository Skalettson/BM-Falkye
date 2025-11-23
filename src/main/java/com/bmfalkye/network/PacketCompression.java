package com.bmfalkye.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Утилита для сжатия пакетов
 */
public class PacketCompression {
    private static final int COMPRESSION_THRESHOLD = 1024; // Сжимаем пакеты больше 1KB
    
    /**
     * Сжимает данные, если они превышают порог
     */
    public static byte[] compressIfNeeded(byte[] data) {
        if (data == null || data.length < COMPRESSION_THRESHOLD) {
            return data; // Не сжимаем маленькие пакеты
        }
        
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
                gzos.write(data);
            }
            byte[] compressed = baos.toByteArray();
            
            // Возвращаем сжатые данные только если сжатие эффективно (уменьшило размер)
            if (compressed.length < data.length) {
                return compressed;
            }
            return data;
        } catch (Exception e) {
            com.bmfalkye.util.ModLogger.warn("Failed to compress packet data: {}", e.getMessage());
            return data; // Возвращаем несжатые данные в случае ошибки
        }
    }
    
    /**
     * Распаковывает данные
     */
    public static byte[] decompress(byte[] data) {
        if (data == null) {
            return null;
        }
        
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            try (GZIPInputStream gzis = new GZIPInputStream(bais)) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = gzis.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                return baos.toByteArray();
            }
        } catch (Exception e) {
            // Если распаковка не удалась, возможно данные не были сжаты
            return data;
        }
    }
    
    /**
     * Проверяет, нужно ли сжимать данные
     */
    public static boolean shouldCompress(byte[] data) {
        return data != null && data.length >= COMPRESSION_THRESHOLD;
    }
}

