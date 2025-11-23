package com.bmfalkye.util;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

/**
 * Пул объектов для переиспользования объектов и уменьшения нагрузки на GC
 * 
 * @param <T> Тип объектов в пуле
 */
public class ObjectPool<T> {
    private final Queue<T> pool = new ConcurrentLinkedQueue<>();
    private final Supplier<T> factory;
    private final int maxSize;
    
    /**
     * Создаёт пул объектов
     * 
     * @param factory Фабрика для создания новых объектов
     * @param maxSize Максимальный размер пула (0 = без ограничений)
     */
    public ObjectPool(Supplier<T> factory, int maxSize) {
        this.factory = factory;
        this.maxSize = maxSize;
    }
    
    /**
     * Получает объект из пула или создаёт новый
     */
    public T acquire() {
        T obj = pool.poll();
        if (obj == null) {
            obj = factory.get();
        }
        return obj;
    }
    
    /**
     * Возвращает объект в пул для переиспользования
     */
    public void release(T obj) {
        if (obj == null) {
            return;
        }
        
        if (maxSize > 0 && pool.size() >= maxSize) {
            // Пул переполнен, не добавляем
            return;
        }
        
        pool.offer(obj);
    }
    
    /**
     * Очищает пул
     */
    public void clear() {
        pool.clear();
    }
    
    /**
     * Возвращает текущий размер пула
     */
    public int size() {
        return pool.size();
    }
}

