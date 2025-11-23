package com.bmfalkye.quests;

import net.minecraft.world.level.Level;
import java.util.HashMap;
import java.util.Map;

/**
 * Реестр квестов
 */
public class QuestRegistry {
    private static final Map<String, Quest> quests = new HashMap<>();
    private static final Map<String, Quest> questsByItem = new HashMap<>(); // ID предмета -> квест
    
    /**
     * Регистрирует квест
     */
    public static void registerQuest(Quest quest) {
        quests.put(quest.getId(), quest);
    }
    
    /**
     * Регистрирует связь между предметом-ключом и квестом
     */
    public static void registerQuestItem(String itemId, String questId) {
        Quest quest = quests.get(questId);
        if (quest != null) {
            questsByItem.put(itemId, quest);
        }
    }
    
    /**
     * Получить квест по ID
     */
    public static Quest getQuest(String questId) {
        return quests.get(questId);
    }
    
    /**
     * Получить квест по ID предмета-ключа
     */
    public static Quest getQuestByItem(String itemId) {
        return questsByItem.get(itemId);
    }
    
    /**
     * Инициализирует все легендарные квесты
     */
    public static void initializeLegendaryQuests() {
        com.bmfalkye.BMFalkye.LOGGER.info("Initializing legendary quests...");
        
        // Регистрируем квесты
        Quest ghostShip = createGhostShipQuest();
        Quest dragonLair = createDragonLairQuest();
        Quest ancientLibrary = createAncientLibraryQuest();
        Quest voidRift = createVoidRiftQuest();
        Quest phoenixNest = createPhoenixNestQuest();
        
        registerQuest(ghostShip);
        registerQuest(dragonLair);
        registerQuest(ancientLibrary);
        registerQuest(voidRift);
        registerQuest(phoenixNest);
        
        // Регистрируем в QuestSystem
        QuestSystem.registerQuest(ghostShip);
        QuestSystem.registerQuest(dragonLair);
        QuestSystem.registerQuest(ancientLibrary);
        QuestSystem.registerQuest(voidRift);
        QuestSystem.registerQuest(phoenixNest);
        
        com.bmfalkye.BMFalkye.LOGGER.info("Initialized {} legendary quests", quests.size());
    }
    
    /**
     * Квест "Призрачный Корабль"
     */
    private static LegendaryQuest createGhostShipQuest() {
        java.util.List<Quest.QuestStep> steps = new java.util.ArrayList<>();
        
        // Шаг 1: Найти дневник капитана
        steps.add(new Quest.QuestStep(
            "find_diary",
            "Найти Выброшенный дневник капитана в сундуках у побережья",
            Quest.QuestStep.QuestStepType.FIND_ITEM,
            "captain_diary"
        ));
        
        // Шаг 2: Достичь локации
        steps.add(new Quest.QuestStep(
            "reach_location",
            "Приплыть на лодке к указанным координатам ночью в полнолуние",
            Quest.QuestStep.QuestStepType.REACH_LOCATION,
            new net.minecraft.core.BlockPos(0, 64, 0) // TODO: Генерировать случайные координаты
        ));
        
        // Шаг 3: Победить Капитана Призрака
        steps.add(new Quest.QuestStep(
            "defeat_ghost_captain",
            "Победить Капитана Призрака в дуэли",
            Quest.QuestStep.QuestStepType.WIN_DUEL,
            "ghost_captain"
        ));
        
        Quest.QuestReward reward = new Quest.QuestReward(
            java.util.List.of("ghost_ship"), // Карта "Призрачный Корабль"
            500, // Монеты
            200, // Опыт
            "curse_of_seven_seas", // Достижение
            java.util.List.of()
        );
        
        LegendaryQuest quest = new LegendaryQuest(
            "ghost_ship_quest",
            "Проклятие Семи Морей",
            "Легенда гласит о призрачном корабле, который появляется в полнолуние...",
            steps,
            reward,
            java.util.Set.of("captain_diary"), // Требуется дневник
            new net.minecraft.core.BlockPos(0, 64, 0), // Локация квеста
            (player) -> {
                // Условие: ночь и полнолуние
                Level level = player.level();
                long dayTime = level.getDayTime() % 24000;
                return dayTime >= 13000 && dayTime <= 23000; // Ночь
            },
            "Туман: сила карт дальнего боя уменьшена до 1"
        );
        
        // Регистрируем связь с предметом
        registerQuestItem("captain_diary", "ghost_ship_quest");
        
        return quest;
    }
    
    /**
     * Квест "Логово Дракона"
     */
    private static LegendaryQuest createDragonLairQuest() {
        java.util.List<Quest.QuestStep> steps = new java.util.ArrayList<>();
        
        steps.add(new Quest.QuestStep(
            "find_dragon_scale",
            "Найти Драконью Чешуйку в подземельях",
            Quest.QuestStep.QuestStepType.FIND_ITEM,
            "dragon_scale"
        ));
        
        steps.add(new Quest.QuestStep(
            "reach_dragon_lair",
            "Достичь Логова Дракона",
            Quest.QuestStep.QuestStepType.REACH_LOCATION,
            new net.minecraft.core.BlockPos(0, 40, 0) // TODO: Генерировать
        ));
        
        steps.add(new Quest.QuestStep(
            "defeat_ancient_dragon",
            "Победить Древнего Дракона в дуэли",
            Quest.QuestStep.QuestStepType.WIN_DUEL,
            "ancient_dragon"
        ));
        
        Quest.QuestReward reward = new Quest.QuestReward(
            java.util.List.of("ancient_dragon_card"),
            1000,
            300,
            "dragon_slayer",
            java.util.List.of()
        );
        
        LegendaryQuest quest = new LegendaryQuest(
            "dragon_lair_quest",
            "Логово Дракона",
            "Глубоко под землёй скрывается логово древнего дракона...",
            steps,
            reward,
            java.util.Set.of("dragon_scale"),
            new net.minecraft.core.BlockPos(0, 40, 0),
            null,
            "Жара: все карты получают +1 к силе, но теряют 1 здоровье каждый ход"
        );
        
        registerQuestItem("dragon_scale", "dragon_lair_quest");
        return quest;
    }
    
    /**
     * Квест "Древняя Библиотека"
     */
    private static LegendaryQuest createAncientLibraryQuest() {
        java.util.List<Quest.QuestStep> steps = new java.util.ArrayList<>();
        
        steps.add(new Quest.QuestStep(
            "find_library_key",
            "Найти Ключ от Древней Библиотеки",
            Quest.QuestStep.QuestStepType.FIND_ITEM,
            "library_key"
        ));
        
        steps.add(new Quest.QuestStep(
            "collect_ancient_cards",
            "Собрать 3 древние карты из библиотеки",
            Quest.QuestStep.QuestStepType.COLLECT_CARDS,
            java.util.List.of("ancient_tome_1", "ancient_tome_2", "ancient_tome_3")
        ));
        
        steps.add(new Quest.QuestStep(
            "defeat_librarian",
            "Победить Древнего Библиотекаря в дуэли",
            Quest.QuestStep.QuestStepType.WIN_DUEL,
            "ancient_librarian"
        ));
        
        Quest.QuestReward reward = new Quest.QuestReward(
            java.util.List.of("ancient_librarian_card", "knowledge_tome"),
            750,
            250,
            "keeper_of_knowledge",
            java.util.List.of()
        );
        
        LegendaryQuest quest = new LegendaryQuest(
            "ancient_library_quest",
            "Древняя Библиотека",
            "Заброшенная библиотека хранит древние знания и карты...",
            steps,
            reward,
            java.util.Set.of("library_key"),
            new net.minecraft.core.BlockPos(0, 50, 0),
            null,
            "Тишина: нельзя использовать заклинания"
        );
        
        registerQuestItem("library_key", "ancient_library_quest");
        return quest;
    }
    
    /**
     * Квест "Разлом Бездны"
     */
    private static LegendaryQuest createVoidRiftQuest() {
        java.util.List<Quest.QuestStep> steps = new java.util.ArrayList<>();
        
        steps.add(new Quest.QuestStep(
            "find_void_crystal",
            "Найти Кристалл Бездны в энд-городах",
            Quest.QuestStep.QuestStepType.FIND_ITEM,
            "void_crystal"
        ));
        
        steps.add(new Quest.QuestStep(
            "reach_void_rift",
            "Достичь Разлома Бездны",
            Quest.QuestStep.QuestStepType.REACH_LOCATION,
            new net.minecraft.core.BlockPos(0, 20, 0)
        ));
        
        steps.add(new Quest.QuestStep(
            "defeat_void_lord",
            "Победить Повелителя Бездны в дуэли",
            Quest.QuestStep.QuestStepType.WIN_DUEL,
            "void_lord"
        ));
        
        Quest.QuestReward reward = new Quest.QuestReward(
            java.util.List.of("void_lord_card"),
            1200,
            400,
            "void_walker",
            java.util.List.of()
        );
        
        LegendaryQuest quest = new LegendaryQuest(
            "void_rift_quest",
            "Разлом Бездны",
            "Трещина в реальности ведёт в Бездну...",
            steps,
            reward,
            java.util.Set.of("void_crystal"),
            new net.minecraft.core.BlockPos(0, 20, 0),
            null,
            "Бездна: случайные карты исчезают каждый ход"
        );
        
        registerQuestItem("void_crystal", "void_rift_quest");
        return quest;
    }
    
    /**
     * Квест "Гнездо Феникса"
     */
    private static LegendaryQuest createPhoenixNestQuest() {
        java.util.List<Quest.QuestStep> steps = new java.util.ArrayList<>();
        
        steps.add(new Quest.QuestStep(
            "find_phoenix_feather",
            "Найти Перо Феникса в пустыне",
            Quest.QuestStep.QuestStepType.FIND_ITEM,
            "phoenix_feather"
        ));
        
        steps.add(new Quest.QuestStep(
            "reach_phoenix_nest",
            "Достичь Гнезда Феникса на вершине горы",
            Quest.QuestStep.QuestStepType.REACH_LOCATION,
            new net.minecraft.core.BlockPos(0, 120, 0)
        ));
        
        steps.add(new Quest.QuestStep(
            "defeat_phoenix_king",
            "Победить Короля Фениксов в дуэли",
            Quest.QuestStep.QuestStepType.WIN_DUEL,
            "phoenix_king"
        ));
        
        Quest.QuestReward reward = new Quest.QuestReward(
            java.util.List.of("phoenix_king_card"),
            900,
            350,
            "phoenix_master",
            java.util.List.of()
        );
        
        LegendaryQuest quest = new LegendaryQuest(
            "phoenix_nest_quest",
            "Гнездо Феникса",
            "На вершине священной горы обитает Король Фениксов...",
            steps,
            reward,
            java.util.Set.of("phoenix_feather"),
            new net.minecraft.core.BlockPos(0, 120, 0),
            null,
            "Пламя: все карты получают +2 к силе, но теряют 1 здоровье"
        );
        
        registerQuestItem("phoenix_feather", "phoenix_nest_quest");
        return quest;
    }
}

