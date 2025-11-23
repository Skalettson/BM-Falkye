package com.bmfalkye.cards;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CardRegistry {
    private static final Map<String, Card> CARDS = new HashMap<>();

    public static void registerCard(Card card) {
        CARDS.put(card.getId(), card);
    }

    public static Card getCard(String id) {
        return CARDS.get(id);
    }
    
    public static int getTotalCardCount() {
        return CARDS.size();
    }
    
    public static List<Card> getAllCards() {
        return new ArrayList<>(CARDS.values());
    }
    
    public static List<Card> getCardsByFaction(String faction) {
        return CARDS.values().stream()
            .filter(card -> card.getFaction().equals(faction))
            .collect(java.util.stream.Collectors.toList());
    }
    
    public static List<Card> getCardsByRarity(CardRarity rarity) {
        return CARDS.values().stream()
            .filter(card -> card.getRarity() == rarity)
            .collect(java.util.stream.Collectors.toList());
    }
    
    public static List<Card> getCardsByType(Card.CardType type) {
        return CARDS.values().stream()
            .filter(card -> card.getType() == type)
            .collect(java.util.stream.Collectors.toList());
    }

    public static void initializeDefaultCards() {
        // ========== ДОМ ПЛАМЕНИ ==========
        // Драконы
        registerCard(new Card("fire_dragon_ignisar", "Игнисар, Вечный Горн", 
            Card.CardType.CREATURE, 15, "Великий дракон огня, стабилизатор ядра мира", "Дом Пламени"));
        registerCard(new Card("fire_drake", "Огненный Дрейк", 
            Card.CardType.CREATURE, 8, "Младший дракон пламени", "Дом Пламени"));
        registerCard(new Card("lava_wyrm", "Лавовый Червь", 
            Card.CardType.CREATURE, 10, "Существо из магмы", "Дом Пламени"));
        
        // Маги и существа
        registerCard(new Card("fire_mage", "Маг Пламени", 
            Card.CardType.CREATURE, 6, "Архитектор реальности, мастер огня", "Дом Пламени"));
        registerCard(new Card("pyro_master", "Пиромант", 
            Card.CardType.CREATURE, 7, "Мастер управления огнём", "Дом Пламени"));
        registerCard(new Card("forge_artisan", "Кузнец-Арканист", 
            Card.CardType.CREATURE, 5, "Создатель звёзд в наковальнях-нексусах", "Дом Пламени"));
        registerCard(new Card("pyro_phoenix", "Пирофеникс", 
            Card.CardType.CREATURE, 12, "Существо из чистого пламени", "Дом Пламени"));
        registerCard(new Card("solar_knight", "Солнечный Рыцарь", 
            Card.CardType.CREATURE, 9, "Защитник Пирофаниума", "Дом Пламени"));
        
        // Заклинания
        registerCard(new Card("flame_storm", "Огненная буря", 
            Card.CardType.SPELL, 0, "Наносит урон всем существам противника", "Дом Пламени"));
        registerCard(new Card("solar_beam", "Солнечный луч", 
            Card.CardType.SPELL, 0, "Усиливает все ваши карты на 2", "Дом Пламени"));
        registerCard(new Card("molten_strike", "Удар магмы", 
            Card.CardType.SPELL, 0, "Уничтожает самую слабую карту противника", "Дом Пламени"));

        // ========== ДОЗОРНЫЕ РУИН ==========
        // Драконы
        registerCard(new Card("ice_dragon_glacis", "Глацис, Хранитель Порога", 
            Card.CardType.CREATURE, 15, "Ледяной дракон, защитник полярных врат", "Дозорные Руин"));
        registerCard(new Card("frost_drake", "Ледяной Дрейк", 
            Card.CardType.CREATURE, 8, "Дракон холода", "Дозорные Руин"));
        registerCard(new Card("crystal_serpent", "Кристальный Змей", 
            Card.CardType.CREATURE, 9, "Существо из застывшего времени", "Дозорные Руин"));
        
        // Учёные и исследователи
        registerCard(new Card("watcher_scholar", "Учёный Дозора", 
            Card.CardType.CREATURE, 6, "Картограф непознанного", "Дозорные Руин"));
        registerCard(new Card("void_researcher", "Исследователь Пустоты", 
            Card.CardType.CREATURE, 7, "Изучающий границы миров", "Дозорные Руин"));
        registerCard(new Card("dream_walker", "Странник Снов", 
            Card.CardType.CREATURE, 8, "Тот, кто путешествует через сны богов", "Дозорные Руин"));
        registerCard(new Card("void_walker", "Странник Пустоты", 
            Card.CardType.CREATURE, 10, "Существо из межпространственных карманов", "Дозорные Руин"));
        registerCard(new Card("library_guardian", "Страж Чертогов", 
            Card.CardType.CREATURE, 9, "Защитник Безмолвного Знания", "Дозорные Руин"));
        
        // Заклинания
        registerCard(new Card("time_freeze", "Замораживание времени", 
            Card.CardType.SPELL, 0, "Противник пропускает следующий ход", "Дозорные Руин"));
        registerCard(new Card("void_rift", "Разлом Пустоты", 
            Card.CardType.SPELL, 0, "Возвращает случайную карту из сброса в руку", "Дозорные Руин"));
        registerCard(new Card("entropy_whisper", "Шёпот Энтропии", 
            Card.CardType.SPELL, 0, "Снижает силу всех карт противника на 1", "Дозорные Руин"));

        // ========== ДЕТИ РОЩЕНИЯ ==========
        // Драконы
        registerCard(new Card("lightning_dragon_fulgur", "Фульгур, Громовой Скипетр", 
            Card.CardType.CREATURE, 15, "Электрический дракон, генератор маны", "Дети Рощения"));
        registerCard(new Card("storm_drake", "Грозовой Дрейк", 
            Card.CardType.CREATURE, 8, "Дракон молний", "Дети Рощения"));
        registerCard(new Card("thunder_bird", "Громовая Птица", 
            Card.CardType.CREATURE, 9, "Существо из энергии ионосферы", "Дети Рощения"));
        
        // Стражи природы
        registerCard(new Card("nature_guardian", "Страж Природы", 
            Card.CardType.CREATURE, 7, "Дирижёр мировой души", "Дети Рощения"));
        registerCard(new Card("tree_singer", "Певун Деревьев", 
            Card.CardType.CREATURE, 6, "Тот, кто слышит Гимн Алуфарда", "Дети Рощения"));
        registerCard(new Card("ancient_tree", "Древнее Дерево", 
            Card.CardType.CREATURE, 12, "Великое рощение, чьи корни уходят в магму", "Дети Рощения"));
        registerCard(new Card("grove_keeper", "Хранитель Рощи", 
            Card.CardType.CREATURE, 8, "Защитник Сияющих Рощ", "Дети Рощения"));
        registerCard(new Card("light_elf", "Эльф Света", 
            Card.CardType.CREATURE, 9, "Эльф из плоти и света", "Дети Рощения"));
        
        // Заклинания
        registerCard(new Card("nature_heal", "Исцеление природы", 
            Card.CardType.SPELL, 0, "Лечит все ваши карты на 3 (если есть что лечить) и усиливает на 2", "Дети Рощения"));
        registerCard(new Card("grove_song", "Песнь Рощи", 
            Card.CardType.SPELL, 0, "Усиливает все карты на 1", "Дети Рощения"));
        registerCard(new Card("world_soul_call", "Призыв Мировой Души", 
            Card.CardType.SPELL, 0, "Возвращает карту из сброса на поле", "Дети Рощения"));

        // ========== ЭПОХА ПЕПЛА (Нейтральные) ==========
        // Отголоски прошлого
        registerCard(new Card("ruin_echo", "Эхо Руин", 
            Card.CardType.CREATURE, 5, "Отголосок прошлой жизни", "Нейтральная"));
        registerCard(new Card("fallen_aeris", "Павший Аэрис", 
            Card.CardType.CREATURE, 7, "Руины парящего города", "Нейтральная"));
        registerCard(new Card("whispering_stone", "Шепчущий Камень", 
            Card.CardType.CREATURE, 4, "Камень, хранящий обрывки воспоминаний", "Нейтральная"));
        
        // Сквернородные
        registerCard(new Card("corrupted_mage", "Сквернородный маг", 
            Card.CardType.CREATURE, 6, "Бывший маг, извращённый искажённой магией", "Нейтральная"));
        registerCard(new Card("twisted_creature", "Искажённое Существо", 
            Card.CardType.CREATURE, 5, "Существо, чья сущность была извращена", "Нейтральная"));
        registerCard(new Card("void_spawn", "Порождение Пустоты", 
            Card.CardType.CREATURE, 8, "Существо из области искажения", "Нейтральная"));
        
        // Академия Вечной Росы
        registerCard(new Card("academy_scholar", "Учёный Академии", 
            Card.CardType.CREATURE, 6, "Последний хранитель разума", "Нейтральная"));
        registerCard(new Card("rector_larintz", "Ректор Ларинц", 
            Card.CardType.CREATURE, 10, "Последний ректор Академии", "Нейтральная"));
        
        // Недра-Королевства
        registerCard(new Card("dwarf_engineer", "Гном-Инженер", 
            Card.CardType.CREATURE, 5, "Создатель механизмов на энергии Эфира", "Нейтральная"));
        registerCard(new Card("ether_machine", "Эфирная Машина", 
            Card.CardType.CREATURE, 7, "Механизм, работающий на чистой энергии", "Нейтральная"));
        
        // Специальные карты
        registerCard(new Card("silent_hunger", "Безмолвный Голод", 
            Card.CardType.SPECIAL, 0, "Усиливает все ваши карты на 2 и ослабляет противника на 1", "Нейтральная"));
        registerCard(new Card("reality_crack", "Трещина Реальности", 
            Card.CardType.SPECIAL, 0, "Ослабляет противника на 2 и усиливает случайную вашу карту на 3", "Нейтральная"));
        
        // Погода
        registerCard(new Card("weather_frost", "Мороз", 
            Card.CardType.SPELL, 0, "Снижает силу всех ближних карт до 1", "Нейтральная"));
        registerCard(new Card("weather_fog", "Туман", 
            Card.CardType.SPELL, 0, "Снижает силу всех дальних карт до 1", "Нейтральная"));
        registerCard(new Card("weather_rain", "Дождь", 
            Card.CardType.SPELL, 0, "Снижает силу всех осадных карт до 1", "Нейтральная"));
        registerCard(new Card("weather_clear", "Ясная погода", 
            Card.CardType.SPELL, 0, "Снимает все эффекты погоды", "Нейтральная"));
        
        // ========== НОВЫЕ КАРТЫ - РАСШИРЕНИЕ КОЛЛЕКЦИИ ==========
        
        // ДОМ ПЛАМЕНИ - Дополнительные карты
        registerCard(new Card("flame_guardian", "Страж Пламени", 
            Card.CardType.CREATURE, 7, "Защитник священных огней", "Дом Пламени"));
        registerCard(new Card("volcano_titan", "Титан Вулкана", 
            Card.CardType.CREATURE, 13, "Древний гигант из недр земли", "Дом Пламени"));
        registerCard(new Card("ash_phoenix", "Пепельный Феникс", 
            Card.CardType.CREATURE, 11, "Возрождается из пепла", "Дом Пламени"));
        registerCard(new Card("inferno_ritual", "Ритуал Инферно", 
            Card.CardType.SPELL, 0, "Усиливает все карты Дома Пламени на 3", "Дом Пламени"));
        registerCard(new Card("flame_barrier", "Огненный Барьер", 
            Card.CardType.SPELL, 0, "Защищает все ваши карты от следующего урона", "Дом Пламени"));
        registerCard(new Card("molten_core", "Расплавленное Ядро", 
            Card.CardType.SPECIAL, 0, "Удваивает силу всех ваших карт в ближнем ряду", "Дом Пламени"));
        
        // ДОЗОРНЫЕ РУИН - Дополнительные карты
        registerCard(new Card("time_weaver", "Ткач Времени", 
            Card.CardType.CREATURE, 8, "Манипулирует потоками времени", "Дозорные Руин"));
        registerCard(new Card("void_architect", "Архитектор Пустоты", 
            Card.CardType.CREATURE, 12, "Строитель межпространственных мостов", "Дозорные Руин"));
        registerCard(new Card("chrono_mage", "Хрономаг", 
            Card.CardType.CREATURE, 9, "Мастер временных парадоксов", "Дозорные Руин"));
        registerCard(new Card("temporal_shift", "Временной Сдвиг", 
            Card.CardType.SPELL, 0, "Возвращает случайную карту противника в руку", "Дозорные Руин"));
        registerCard(new Card("void_echo", "Эхо Пустоты", 
            Card.CardType.SPELL, 0, "Копирует эффект последней сыгранной карты способности", "Дозорные Руин"));
        registerCard(new Card("knowledge_seal", "Печать Знания", 
            Card.CardType.SPECIAL, 0, "Показывает все карты в руке противника", "Дозорные Руин"));
        
        // ДЕТИ РОЩЕНИЯ - Дополнительные карты
        registerCard(new Card("forest_ancient", "Древний Леса", 
            Card.CardType.CREATURE, 11, "Вековое дерево с корнями в прошлом", "Дети Рощения"));
        registerCard(new Card("storm_herald", "Глашатай Бури", 
            Card.CardType.CREATURE, 10, "Призывает грозы и молнии", "Дети Рощения"));
        registerCard(new Card("life_weaver", "Ткач Жизни", 
            Card.CardType.CREATURE, 8, "Восстанавливает силы природы", "Дети Рощения"));
        registerCard(new Card("nature_wrath", "Гнев Природы", 
            Card.CardType.SPELL, 0, "Наносит урон равный количеству ваших карт на поле", "Дети Рощения"));
        registerCard(new Card("grove_awakening", "Пробуждение Рощи", 
            Card.CardType.SPELL, 0, "Усиливает все карты Детей Рощения на 2 и лечит их на 2 (если есть что лечить)", "Дети Рощения"));
        registerCard(new Card("world_soul_awakening", "Пробуждение Мировой Души", 
            Card.CardType.SPECIAL, 0, "Возвращает все карты из сброса в руку", "Дети Рощения"));
        
        // НЕЙТРАЛЬНЫЕ - Дополнительные карты
        registerCard(new Card("eternal_guardian", "Вечный Страж", 
            Card.CardType.CREATURE, 9, "Защитник границ реальности", "Нейтральная"));
        registerCard(new Card("chaos_spawn", "Порождение Хаоса", 
            Card.CardType.CREATURE, 6, "Существо из нестабильной реальности", "Нейтральная"));
        registerCard(new Card("reality_anchor", "Якорь Реальности", 
            Card.CardType.CREATURE, 7, "Стабилизирует пространство вокруг себя", "Нейтральная"));
        registerCard(new Card("dimension_rift", "Разлом Измерения", 
            Card.CardType.SPELL, 0, "Обменивает случайную карту из руки на карту из колоды противника", "Нейтральная"));
        registerCard(new Card("balance_scale", "Весы Равновесия", 
            Card.CardType.SPECIAL, 0, "Выравнивает очки обоих игроков до среднего значения", "Нейтральная"));
        registerCard(new Card("watcher_insight", "Прозрение Дозора", 
            Card.CardType.SPECIAL, 0, "Показывает 3 случайные карты из руки противника", "Нейтральная"));
        registerCard(new Card("grove_blessing", "Благословение Рощи", 
            Card.CardType.SPECIAL, 0, "Лечит все ваши карты на 3 и усиливает их на 1", "Нейтральная"));
    }
}

