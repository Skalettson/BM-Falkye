package com.bmfalkye.api;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.LeaderCard;

import java.util.List;

/**
 * Главный API интерфейс для расширения мода BM Falkye
 * 
 * <p>Предоставляет методы для регистрации карт, лидеров, обработчиков событий
 * и других расширений функциональности мода.</p>
 * 
 * <p>Пример использования:</p>
 * <pre>{@code
 * IFalkyeAPI api = FalkyeAPI.getInstance();
 * api.registerCard(new CustomCard("my_card", "My Card", ...));
 * api.registerLeader(new CustomLeader("my_leader", ...));
 * }</pre>
 * 
 * @author BM Falkye Team
 * @version 1.0
 */
public interface IFalkyeAPI {
    
    /**
     * Регистрирует новую карту в системе
     * 
     * @param card карта для регистрации
     * @return true если регистрация успешна, false если карта с таким ID уже существует
     */
    boolean registerCard(Card card);
    
    /**
     * Регистрирует нового лидера в системе
     * 
     * @param leader лидер для регистрации
     * @return true если регистрация успешна, false если лидер с таким ID уже существует
     */
    boolean registerLeader(LeaderCard leader);
    
    /**
     * Регистрирует обработчик событий игры
     * 
     * @param handler обработчик событий
     */
    void registerGameEventHandler(IGameEventHandler handler);
    
    /**
     * Регистрирует обработчик событий карт
     * 
     * @param handler обработчик событий карт
     */
    void registerCardEventHandler(ICardEventHandler handler);
    
    /**
     * Регистрирует обработчик событий игроков
     * 
     * @param handler обработчик событий игроков
     */
    void registerPlayerEventHandler(IPlayerEventHandler handler);
    
    /**
     * Регистрирует кастомный эффект карты
     * 
     * @param effectId уникальный ID эффекта
     * @param effect обработчик эффекта
     */
    void registerCardEffect(String effectId, ICardEffect effect);
    
    /**
     * Получает карту по ID
     * 
     * @param cardId ID карты
     * @return карта или null если не найдена
     */
    Card getCard(String cardId);
    
    /**
     * Получает лидера по ID
     * 
     * @param leaderId ID лидера
     * @return лидер или null если не найден
     */
    LeaderCard getLeader(String leaderId);
    
    /**
     * Получает список всех зарегистрированных карт
     * 
     * @return список карт
     */
    List<Card> getAllCards();
    
    /**
     * Получает список всех зарегистрированных лидеров
     * 
     * @return список лидеров
     */
    List<LeaderCard> getAllLeaders();
}

