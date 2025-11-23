package com.bmfalkye.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Менеджер кэша для часто используемых данных
 * 
 * @param <K> Тип ключа
 * @param <V> Тип значения
 */
public class CacheManager<K, V> {
    private final Map<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
    private final long defaultTtl; // Время жизни кэша в миллисекундах
    private final int maxSize;
    
    public CacheManager(long defaultTtlMs, int maxSize) {
        this.defaultTtl = defaultTtlMs;
        this.maxSize = maxSize;
    }
    
    /**
     * Получает значение из кэша или вычисляет его, если его нет
     */
    public V get(K key, Supplier<V> supplier) {
        CacheEntry<V> entry = cache.get(key);
        
        if (entry != null && !entry.isExpired()) {
            return entry.getValue();
        }
        
        // Вычисляем новое значение
        V value = supplier.get();
        put(key, value);
        return value;
    }
    
    /**
     * Получает значение из кэша или возвращает null
     */
    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            return entry.getValue();
        }
        return null;
    }
    
    /**
     * Добавляет значение в кэш
     */
    public void put(K key, V value) {
        put(key, value, defaultTtl);
    }
    
    /**
     * Добавляет значение в кэш с указанным TTL
     */
    public void put(K key, V value, long ttlMs) {
        // Проверяем размер кэша
        if (maxSize > 0 && cache.size() >= maxSize && !cache.containsKey(key)) {
            // Удаляем самое старое значение
            removeOldest();
        }
        
        cache.put(key, new CacheEntry<>(value, System.currentTimeMillis() + ttlMs));
    }
    
    /**
     * Удаляет значение из кэша
     */
    public void remove(K key) {
        cache.remove(key);
    }
    
    /**
     * Очищает весь кэш
     */
    public void clear() {
        cache.clear();
    }
    
    /**
     * Очищает истёкшие записи
     */
    public void cleanup() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    /**
     * Удаляет самую старую запись
     */
    private void removeOldest() {
        long oldestTime = Long.MAX_VALUE;
        K oldestKey = null;
        
        for (Map.Entry<K, CacheEntry<V>> entry : cache.entrySet()) {
            if (entry.getValue().getExpireTime() < oldestTime) {
                oldestTime = entry.getValue().getExpireTime();
                oldestKey = entry.getKey();
            }
        }
        
        if (oldestKey != null) {
            cache.remove(oldestKey);
        }
    }
    
    /**
     * Запись в кэше
     */
    private static class CacheEntry<V> {
        private final V value;
        private final long expireTime;
        
        public CacheEntry(V value, long expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }
        
        public V getValue() {
            return value;
        }
        
        public long getExpireTime() {
            return expireTime;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }
}

