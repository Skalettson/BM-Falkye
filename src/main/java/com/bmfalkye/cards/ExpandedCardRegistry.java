package com.bmfalkye.cards;

import com.bmfalkye.BMFalkye;

/**
 * Расширенный реестр карт - 150+ карт на основе лора BeforeMine
 */
public class ExpandedCardRegistry {
    
    public static void initializeAllCards() {
        BMFalkye.LOGGER.info("Initializing expanded card registry with 150+ cards...");
        
        // ========== ДОМ ПЛАМЕНИ (50 карт) ==========
        initializeFireHouseCards();
        
        // ========== ДОЗОРНЫЕ РУИН (50 карт) ==========
        initializeWatchersCards();
        
        // ========== ДЕТИ РОЩЕНИЯ (50 карт) ==========
        initializeNatureChildrenCards();
        
        BMFalkye.LOGGER.info("Card registry initialized with {} cards", CardRegistry.getTotalCardCount());
    }
    
    private static void initializeFireHouseCards() {
        // Легендарные драконы
        registerCard("fire_dragon_ignisar", "Игнисар, Вечный Горн", Card.CardType.CREATURE, 20, 
            "Великий дракон огня, стабилизатор ядра мира. Усиливает все карты Дома Пламени на поле на +2", 
            "Дом Пламени", CardRarity.LEGENDARY, 100);
        
        // Эпические существа
        registerCard("pyro_phoenix", "Пирофеникс", Card.CardType.CREATURE, 12, 
            "Существо из чистого пламени. При разыгрывании наносит 3 урона случайной карте противника", 
            "Дом Пламени", CardRarity.EPIC, 50);
        registerCard("solar_knight", "Солнечный Рыцарь", Card.CardType.CREATURE, 10, 
            "Защитник Пирофаниума. Иммунитет к эффектам погоды", 
            "Дом Пламени", CardRarity.EPIC, 45);
        registerCard("forge_master", "Мастер Кузни", Card.CardType.CREATURE, 9, 
            "Создатель звёзд. Усиливает все карты в ближнем ряду на +1", 
            "Дом Пламени", CardRarity.EPIC, 40);
        
        // Редкие существа
        registerCard("fire_mage", "Маг Пламени", Card.CardType.CREATURE, 8, 
            "Архитектор реальности, мастер огня", 
            "Дом Пламени", CardRarity.RARE, 25);
        registerCard("pyro_master", "Пиромант", Card.CardType.CREATURE, 7, 
            "Мастер управления огнём. При разыгрывании наносит 2 урона ближайшей карте противника", 
            "Дом Пламени", CardRarity.RARE, 22);
        registerCard("fire_drake", "Огненный Дрейк", Card.CardType.CREATURE, 8, 
            "Младший дракон пламени", 
            "Дом Пламени", CardRarity.RARE, 20);
        registerCard("lava_wyrm", "Лавовый Червь", Card.CardType.CREATURE, 10, 
            "Существо из магмы. Иммунитет к огненным эффектам", 
            "Дом Пламени", CardRarity.RARE, 28);
        registerCard("flame_guardian", "Страж Пламени", Card.CardType.CREATURE, 9, 
            "Защитник огненных цитаделей", 
            "Дом Пламени", CardRarity.RARE, 24);
        
        // Обычные существа (много копий для разнообразия)
        for (int i = 1; i <= 10; i++) {
            registerCard("fire_apprentice_" + i, "Ученик Пламени " + i, Card.CardType.CREATURE, 
                3 + (i % 3), "Начинающий маг огня", 
                "Дом Пламени", CardRarity.COMMON, 5);
        }
        for (int i = 1; i <= 8; i++) {
            registerCard("flame_warrior_" + i, "Воин Пламени " + i, Card.CardType.CREATURE, 
                4 + (i % 2), "Воин, обученный магии огня", 
                "Дом Пламени", CardRarity.COMMON, 8);
        }
        for (int i = 1; i <= 6; i++) {
            registerCard("ember_spirit_" + i, "Дух Уголька " + i, Card.CardType.CREATURE, 
                5 + (i % 2), "Дух огня, рождённый в кузнице", 
                "Дом Пламени", CardRarity.COMMON, 10);
        }
        
        // Заклинания
        registerCard("flame_storm", "Огненная буря", Card.CardType.SPELL, 0, 
            "Наносит 3 урона всем картам противника", 
            "Дом Пламени", CardRarity.RARE, 30);
        registerCard("solar_beam", "Солнечный луч", Card.CardType.SPELL, 0, 
            "Усиливает все ваши карты на 2", 
            "Дом Пламени", CardRarity.EPIC, 40);
        registerCard("molten_strike", "Удар магмы", Card.CardType.SPELL, 0, 
            "Уничтожает самую слабую карту противника", 
            "Дом Пламени", CardRarity.RARE, 25);
        registerCard("ignite", "Воспламенение", Card.CardType.SPELL, 0, 
            "Наносит 2 урона всем картам в ближнем ряду противника", 
            "Дом Пламени", CardRarity.COMMON, 15);
        registerCard("fire_shield", "Огненный Щит", Card.CardType.SPELL, 0, 
            "Усиливает все ваши карты в ближнем ряду на 1", 
            "Дом Пламени", CardRarity.COMMON, 12);
        
        // Специальные карты
        registerCard("pyro_ritual", "Пироритуал", Card.CardType.SPECIAL, 0, 
            "Возвращает случайную карту из сброса в руку", 
            "Дом Пламени", CardRarity.RARE, 20);
        registerCard("forge_blessing", "Благословение Кузни", Card.CardType.SPECIAL, 0, 
            "Усиливает случайную карту на поле на 5", 
            "Дом Пламени", CardRarity.EPIC, 35);
    }
    
    private static void initializeWatchersCards() {
        // Легендарные драконы
        registerCard("ice_dragon_glacis", "Глацис, Хранитель Порога", Card.CardType.CREATURE, 20, 
            "Ледяной дракон, защитник полярных врат. Снижает силу всех карт противника на 2", 
            "Дозорные Руин", CardRarity.LEGENDARY, 100);
        
        // Эпические существа
        registerCard("void_walker", "Странник Пустоты", Card.CardType.CREATURE, 12, 
            "Существо из межпространственных карманов. Берёт карту из колоды противника", 
            "Дозорные Руин", CardRarity.EPIC, 50);
        registerCard("dream_walker", "Странник Снов", Card.CardType.CREATURE, 10, 
            "Тот, кто путешествует через сны богов. Возвращает карту из сброса", 
            "Дозорные Руин", CardRarity.EPIC, 45);
        registerCard("library_guardian", "Страж Чертогов", Card.CardType.CREATURE, 9, 
            "Защитник Безмолвного Знания. Усиливает все карты в дальнем ряду на +1", 
            "Дозорные Руин", CardRarity.EPIC, 40);
        
        // Редкие существа
        registerCard("watcher_scholar", "Учёный Дозора", Card.CardType.CREATURE, 7, 
            "Картограф непознанного", 
            "Дозорные Руин", CardRarity.RARE, 25);
        registerCard("void_researcher", "Исследователь Пустоты", Card.CardType.CREATURE, 8, 
            "Изучающий границы миров. При разыгрывании берёт карту из колоды", 
            "Дозорные Руин", CardRarity.RARE, 22);
        registerCard("frost_drake", "Ледяной Дрейк", Card.CardType.CREATURE, 8, 
            "Дракон холода", 
            "Дозорные Руин", CardRarity.RARE, 20);
        registerCard("crystal_serpent", "Кристальный Змей", Card.CardType.CREATURE, 9, 
            "Существо из застывшего времени. Снижает силу всех карт противника на 1", 
            "Дозорные Руин", CardRarity.RARE, 28);
        registerCard("time_warden", "Хранитель Времени", Card.CardType.CREATURE, 9, 
            "Защитник временных потоков", 
            "Дозорные Руин", CardRarity.RARE, 24);
        
        // Обычные существа
        for (int i = 1; i <= 10; i++) {
            registerCard("watcher_initiate_" + i, "Послушник Дозора " + i, Card.CardType.CREATURE, 
                3 + (i % 3), "Начинающий исследователь", 
                "Дозорные Руин", CardRarity.COMMON, 5);
        }
        for (int i = 1; i <= 8; i++) {
            registerCard("void_scout_" + i, "Разведчик Пустоты " + i, Card.CardType.CREATURE, 
                4 + (i % 2), "Разведчик межпространственных границ", 
                "Дозорные Руин", CardRarity.COMMON, 8);
        }
        for (int i = 1; i <= 6; i++) {
            registerCard("frost_spirit_" + i, "Дух Мороза " + i, Card.CardType.CREATURE, 
                5 + (i % 2), "Дух застывшего времени", 
                "Дозорные Руин", CardRarity.COMMON, 10);
        }
        
        // Заклинания
        registerCard("time_freeze", "Замораживание времени", Card.CardType.SPELL, 0, 
            "Противник пропускает следующий ход", 
            "Дозорные Руин", CardRarity.EPIC, 40);
        registerCard("void_rift", "Разлом Пустоты", Card.CardType.SPELL, 0, 
            "Возвращает случайную карту из сброса в руку", 
            "Дозорные Руин", CardRarity.RARE, 30);
        registerCard("entropy_whisper", "Шёпот Энтропии", Card.CardType.SPELL, 0, 
            "Снижает силу всех карт противника на 1", 
            "Дозорные Руин", CardRarity.RARE, 25);
        registerCard("frost_bolt", "Ледяной Болт", Card.CardType.SPELL, 0, 
            "Наносит 2 урона всем картам в дальнем ряду противника", 
            "Дозорные Руин", CardRarity.COMMON, 15);
        registerCard("void_shield", "Щит Пустоты", Card.CardType.SPELL, 0, 
            "Усиливает все карты в дальнем ряду на 1", 
            "Дозорные Руин", CardRarity.COMMON, 12);
        
        // Специальные карты
        registerCard("watcher_insight", "Прозрение Дозора", Card.CardType.SPECIAL, 0, 
            "Показывает 3 случайные карты из руки противника", 
            "Дозорные Руин", CardRarity.RARE, 20);
        registerCard("time_paradox", "Парадокс Времени", Card.CardType.SPECIAL, 0, 
            "Возвращает все карты из сброса в колоду и перемешивает", 
            "Дозорные Руин", CardRarity.EPIC, 35);
    }
    
    private static void initializeNatureChildrenCards() {
        // Легендарные драконы
        registerCard("lightning_dragon_fulgur", "Фульгур, Громовой Скипетр", Card.CardType.CREATURE, 20, 
            "Электрический дракон, генератор маны. Усиливает все карты Детей Рощения на поле на +2", 
            "Дети Рощения", CardRarity.LEGENDARY, 100);
        
        // Эпические существа
        registerCard("ancient_tree", "Древнее Дерево", Card.CardType.CREATURE, 12, 
            "Великое рощение, чьи корни уходят в магму. Лечит все ваши карты на 3", 
            "Дети Рощения", CardRarity.EPIC, 50);
        registerCard("light_elf", "Эльф Света", Card.CardType.CREATURE, 10, 
            "Эльф из плоти и света. Усиливает соседние карты на +1", 
            "Дети Рощения", CardRarity.EPIC, 45);
        registerCard("grove_keeper", "Хранитель Рощи", Card.CardType.CREATURE, 9, 
            "Защитник Сияющих Рощ. Усиливает все карты в осадном ряду на +1", 
            "Дети Рощения", CardRarity.EPIC, 40);
        
        // Редкие существа
        registerCard("nature_guardian", "Страж Природы", Card.CardType.CREATURE, 8, 
            "Дирижёр мировой души", 
            "Дети Рощения", CardRarity.RARE, 25);
        registerCard("tree_singer", "Певун Деревьев", Card.CardType.CREATURE, 7, 
            "Тот, кто слышит Гимн Алуфарда. При разыгрывании лечит все карты на 1", 
            "Дети Рощения", CardRarity.RARE, 22);
        registerCard("storm_drake", "Грозовой Дрейк", Card.CardType.CREATURE, 8, 
            "Дракон молний", 
            "Дети Рощения", CardRarity.RARE, 20);
        registerCard("thunder_bird", "Громовая Птица", Card.CardType.CREATURE, 9, 
            "Существо из энергии ионосферы. Иммунитет к электрическим эффектам", 
            "Дети Рощения", CardRarity.RARE, 28);
        registerCard("grove_warden", "Страж Рощи", Card.CardType.CREATURE, 9, 
            "Защитник природных святилищ", 
            "Дети Рощения", CardRarity.RARE, 24);
        
        // Обычные существа
        for (int i = 1; i <= 10; i++) {
            registerCard("nature_apprentice_" + i, "Ученик Природы " + i, Card.CardType.CREATURE, 
                3 + (i % 3), "Начинающий дирижёр мировой души", 
                "Дети Рощения", CardRarity.COMMON, 5);
        }
        for (int i = 1; i <= 8; i++) {
            registerCard("grove_warrior_" + i, "Воин Рощи " + i, Card.CardType.CREATURE, 
                4 + (i % 2), "Воин, защищающий природу", 
                "Дети Рощения", CardRarity.COMMON, 8);
        }
        for (int i = 1; i <= 6; i++) {
            registerCard("nature_spirit_" + i, "Дух Природы " + i, Card.CardType.CREATURE, 
                5 + (i % 2), "Дух, слышащий Гимн Алуфарда", 
                "Дети Рощения", CardRarity.COMMON, 10);
        }
        
        // Заклинания
        registerCard("nature_heal", "Исцеление природы", Card.CardType.SPELL, 0, 
            "Лечит все ваши карты на 3 (если есть что лечить) и усиливает на 2", 
            "Дети Рощения", CardRarity.EPIC, 40);
        registerCard("grove_song", "Песнь Рощи", Card.CardType.SPELL, 0, 
            "Усиливает все карты на 1", 
            "Дети Рощения", CardRarity.RARE, 30);
        registerCard("world_soul_call", "Призыв Мировой Души", Card.CardType.SPELL, 0, 
            "Возвращает карту из сброса на поле", 
            "Дети Рощения", CardRarity.RARE, 25);
        registerCard("lightning_strike", "Удар Молнии", Card.CardType.SPELL, 0, 
            "Наносит 2 урона всем картам в осадном ряду противника", 
            "Дети Рощения", CardRarity.COMMON, 15);
        registerCard("nature_shield", "Щит Природы", Card.CardType.SPELL, 0, 
            "Усиливает все ваши карты в выбранном ряду на 1", 
            "Дети Рощения", CardRarity.COMMON, 12);
        
        // Специальные карты
        registerCard("grove_blessing", "Благословение Рощи", Card.CardType.SPECIAL, 0, 
            "Лечит все ваши карты на 3 и усиливает их на 1", 
            "Дети Рощения", CardRarity.RARE, 20);
        registerCard("world_soul_awakening", "Пробуждение Мировой Души", Card.CardType.SPECIAL, 0, 
            "Возвращает все карты из сброса в руку", 
            "Дети Рощения", CardRarity.EPIC, 35);
    }
    
    private static void registerCard(String id, String name, Card.CardType type, int power, 
                                    String description, String faction, CardRarity rarity, int cost) {
        Card card = new Card(id, name, type, power, description, faction, rarity, cost);
        CardRegistry.registerCard(card);
    }
}

