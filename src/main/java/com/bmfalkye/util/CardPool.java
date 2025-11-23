package com.bmfalkye.util;

import com.bmfalkye.cards.Card;
import java.util.ArrayList;
import java.util.List;

/**
 * Пул для переиспользования списков карт
 */
public class CardPool {
    private static final ObjectPool<List<Card>> LIST_POOL = 
        new ObjectPool<>(ArrayList::new, 100);
    
    /**
     * Получает список карт из пула
     */
    public static List<Card> acquireList() {
        List<Card> list = LIST_POOL.acquire();
        list.clear(); // Очищаем перед использованием
        return list;
    }
    
    /**
     * Возвращает список карт в пул
     */
    public static void releaseList(List<Card> list) {
        if (list != null) {
            list.clear();
            LIST_POOL.release(list);
        }
    }
    
    /**
     * Очищает пул
     */
    public static void clear() {
        LIST_POOL.clear();
    }
}

