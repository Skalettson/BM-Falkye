package com.bmfalkye.evolution;

import java.util.*;

/**
 * Система древа эволюции карт.
 * 
 * <p>Определяет доступные ветки улучшений для каждой карты на каждом уровне.
 * Каждая карта может иметь несколько веток улучшений (например, "Сила", "Тактика", "Защита").
 * 
 * <p>Пример для карты "Огненный Элементаль":
 * <ul>
 *   <li>Ветка 1 (Сила): +1 к базовой силе -> Способность "Огненная аура"</li>
 *   <li>Ветка 2 (Тактика): Способность "Иммунитет к морозу" -> Способность "Взрыв при выходе"</li>
 * </ul>
 */
public class CardEvolutionTree {
    // ID карты -> Древо эволюции
    private static final Map<String, EvolutionTreeData> evolutionTrees = new HashMap<>();
    
    /**
     * Инициализирует древа эволюции для всех карт
     */
    public static void initializeEvolutionTrees() {
        // Огненный Элементаль
        registerEvolutionTree("fire_elemental", createFireElementalTree());
        
        // Огненный Дракон Игнисар
        registerEvolutionTree("fire_dragon_ignisar", createFireDragonTree());
        
        // Ледяной Дракон Глацис
        registerEvolutionTree("ice_dragon_glacis", createIceDragonTree());
        
        // Странник Бездны
        registerEvolutionTree("void_walker", createVoidWalkerTree());
        
        // Огненный Феникс
        registerEvolutionTree("pyro_phoenix", createPhoenixTree());
        
        // Страж Природы
        registerEvolutionTree("nature_guardian", createNatureGuardianTree());
    }
    
    /**
     * Регистрирует древо эволюции для карты
     */
    public static void registerEvolutionTree(String cardId, EvolutionTreeData tree) {
        evolutionTrees.put(cardId, tree);
    }
    
    /**
     * Получить древо эволюции для карты
     */
    public static EvolutionTreeData getEvolutionTree(String cardId) {
        return evolutionTrees.get(cardId);
    }
    
    /**
     * Проверить, есть ли древо эволюции для карты
     */
    public static boolean hasEvolutionTree(String cardId) {
        return evolutionTrees.containsKey(cardId);
    }
    
    /**
     * Получить все доступные ветки для карты на определённом уровне
     */
    public static List<EvolutionBranch> getAvailableBranches(String cardId, int cardLevel) {
        EvolutionTreeData tree = getEvolutionTree(cardId);
        if (tree == null) {
            return Collections.emptyList();
        }
        
        List<EvolutionBranch> available = new ArrayList<>();
        for (EvolutionBranch branch : tree.getBranches()) {
            // Ветка доступна, если хотя бы одно улучшение в ней доступно на текущем уровне
            boolean hasAvailableUpgrade = branch.getUpgrades().stream()
                .anyMatch(upgrade -> upgrade.getRequiredLevel() <= cardLevel);
            if (hasAvailableUpgrade) {
                available.add(branch);
            }
        }
        return available;
    }
    
    /**
     * Получить следующее улучшение в ветке
     */
    public static EvolutionUpgrade getNextUpgrade(String cardId, String branchId, Set<String> unlockedUpgrades) {
        EvolutionTreeData tree = getEvolutionTree(cardId);
        if (tree == null) {
            return null;
        }
        
        EvolutionBranch branch = tree.getBranch(branchId);
        if (branch == null) {
            return null;
        }
        
        // Находим первое неоткрытое улучшение в ветке
        for (EvolutionUpgrade upgrade : branch.getUpgrades()) {
            if (!unlockedUpgrades.contains(upgrade.getId())) {
                return upgrade;
            }
        }
        
        return null; // Все улучшения в ветке открыты
    }
    
    /**
     * Создаёт древо эволюции для Огненного Элементаля
     */
    private static EvolutionTreeData createFireElementalTree() {
        List<EvolutionBranch> branches = new ArrayList<>();
        
        // Ветка 1: Сила
        List<EvolutionUpgrade> powerBranchUpgrades = new ArrayList<>();
        powerBranchUpgrades.add(new EvolutionUpgrade(
            "fire_elemental_power_1",
            "+1 к базовой силе",
            "Увеличивает базовую силу карты на 1",
            1, // Требуемый уровень карты
            EvolutionUpgrade.UpgradeType.POWER_BOOST,
            Map.of("power", 1)
        ));
        powerBranchUpgrades.add(new EvolutionUpgrade(
            "fire_elemental_aura",
            "Огненная аура",
            "Соседние карты получают +1 к силе",
            3, // Требуемый уровень карты
            EvolutionUpgrade.UpgradeType.ABILITY,
            Map.of("ability", "fire_aura", "aura_power", 1)
        ));
        
        branches.add(new EvolutionBranch("power", "Сила", powerBranchUpgrades));
        
        // Ветка 2: Тактика
        List<EvolutionUpgrade> tacticBranchUpgrades = new ArrayList<>();
        tacticBranchUpgrades.add(new EvolutionUpgrade(
            "fire_elemental_frost_immunity",
            "Иммунитет к морозу",
            "Карта игнорирует эффекты мороза",
            2, // Требуемый уровень карты
            EvolutionUpgrade.UpgradeType.ABILITY,
            Map.of("ability", "frost_immunity")
        ));
        tacticBranchUpgrades.add(new EvolutionUpgrade(
            "fire_elemental_explosion",
            "Взрыв при выходе",
            "При розыгрыше наносит 1 урон случайной карте противника",
            4, // Требуемый уровень карты
            EvolutionUpgrade.UpgradeType.ABILITY,
            Map.of("ability", "entry_explosion", "damage", 1)
        ));
        
        branches.add(new EvolutionBranch("tactic", "Тактика", tacticBranchUpgrades));
        
        return new EvolutionTreeData("fire_elemental", branches);
    }
    
    /**
     * Создаёт древо эволюции для Огненного Дракона Игнисара
     */
    private static EvolutionTreeData createFireDragonTree() {
        List<EvolutionBranch> branches = new ArrayList<>();
        
        // Ветка 1: Пламя
        List<EvolutionUpgrade> flameBranchUpgrades = new ArrayList<>();
        flameBranchUpgrades.add(new EvolutionUpgrade(
            "fire_dragon_flame_breath",
            "Дыхание пламени",
            "При розыгрыше наносит 2 урона всем картам дальнего боя противника",
            2,
            EvolutionUpgrade.UpgradeType.ABILITY,
            Map.of("ability", "flame_breath", "damage", 2, "target", "ranged")
        ));
        flameBranchUpgrades.add(new EvolutionUpgrade(
            "fire_dragon_inferno",
            "Инферно",
            "Усиливает все карты Дома Пламени на поле на +2",
            4,
            EvolutionUpgrade.UpgradeType.ABILITY,
            Map.of("ability", "faction_boost", "faction", "Дом Пламени", "power", 2)
        ));
        branches.add(new EvolutionBranch("flame", "Пламя", flameBranchUpgrades));
        
        // Ветка 2: Защита
        List<EvolutionUpgrade> defenseBranchUpgrades = new ArrayList<>();
        defenseBranchUpgrades.add(new EvolutionUpgrade(
            "fire_dragon_scales",
            "Огненные чешуйки",
            "Игнорирует первый полученный урон в каждом раунде",
            3,
            EvolutionUpgrade.UpgradeType.ABILITY,
            Map.of("ability", "damage_shield", "charges", 1)
        ));
        defenseBranchUpgrades.add(new EvolutionUpgrade(
            "fire_dragon_rebirth",
            "Возрождение",
            "При смерти возвращается в руку с -2 к силе",
            5,
            EvolutionUpgrade.UpgradeType.ABILITY,
            Map.of("ability", "rebirth", "power_penalty", -2)
        ));
        branches.add(new EvolutionBranch("defense", "Защита", defenseBranchUpgrades));
        
        return new EvolutionTreeData("fire_dragon_ignisar", branches);
    }
    
    /**
     * Создаёт древо эволюции для Ледяного Дракона Глациса
     */
    private static EvolutionTreeData createIceDragonTree() {
        List<EvolutionBranch> branches = new ArrayList<>();
        
        // Ветка 1: Мороз
        List<EvolutionUpgrade> frostBranchUpgrades = new ArrayList<>();
        frostBranchUpgrades.add(new EvolutionUpgrade(
            "ice_dragon_frost_aura",
            "Аура мороза",
            "Все карты противника в ближнем бою получают -1 к силе",
            2,
            EvolutionUpgrade.UpgradeType.ABILITY,
            Map.of("ability", "frost_aura", "power", -1, "target", "melee")
        ));
        frostBranchUpgrades.add(new EvolutionUpgrade(
            "ice_dragon_freeze",
            "Заморозка",
            "При розыгрыше замораживает случайную карту противника (не может атаковать 1 ход)",
            4,
            EvolutionUpgrade.UpgradeType.ABILITY,
            Map.of("ability", "freeze", "turns", 1)
        ));
        branches.add(new EvolutionBranch("frost", "Мороз", frostBranchUpgrades));
        
        // Ветка 2: Выносливость
        List<EvolutionUpgrade> enduranceBranchUpgrades = new ArrayList<>();
        enduranceBranchUpgrades.add(new EvolutionUpgrade(
            "ice_dragon_ice_armor",
            "Ледяная броня",
            "+3 к базовой силе, но карта не может быть усилена",
            1,
            EvolutionUpgrade.UpgradeType.POWER_BOOST,
            Map.of("power", 3, "cannot_boost", true)
        ));
        enduranceBranchUpgrades.add(new EvolutionUpgrade(
            "ice_dragon_immortality",
            "Бессмертие",
            "Не может быть уничтожена эффектами, только уроном",
            5,
            EvolutionUpgrade.UpgradeType.ABILITY,
            Map.of("ability", "effect_immunity")
        ));
        branches.add(new EvolutionBranch("endurance", "Выносливость", enduranceBranchUpgrades));
        
        return new EvolutionTreeData("ice_dragon_glacis", branches);
    }
    
    /**
     * Создаёт древо эволюции для Странника Бездны
     */
    private static EvolutionTreeData createVoidWalkerTree() {
        List<EvolutionBranch> branches = new ArrayList<>();
        
        // Ветка 1: Бездна
        List<EvolutionUpgrade> voidBranchUpgrades = new ArrayList<>();
        voidBranchUpgrades.add(new EvolutionUpgrade(
            "void_walker_void_blast",
            "Взрыв Бездны",
            "При розыгрыше уничтожает случайную карту противника с силой 5 или меньше",
            2,
            EvolutionUpgrade.UpgradeType.ABILITY,
            Map.of("ability", "void_blast", "max_power", 5)
        ));
        voidBranchUpgrades.add(new EvolutionUpgrade(
            "void_walker_darkness",
            "Тьма",
            "Все карты противника получают -2 к силе на 2 раунда",
            4,
            EvolutionUpgrade.UpgradeType.ABILITY,
            Map.of("ability", "darkness", "power", -2, "rounds", 2)
        ));
        branches.add(new EvolutionBranch("void", "Бездна", voidBranchUpgrades));
        
        // Ветка 2: Телепортация
        List<EvolutionUpgrade> teleportBranchUpgrades = new ArrayList<>();
        teleportBranchUpgrades.add(new EvolutionUpgrade(
            "void_walker_phase",
            "Фазирование",
            "Может быть разыграна в любой ряд, игнорируя ограничения",
            1,
            EvolutionUpgrade.UpgradeType.ABILITY,
            Map.of("ability", "phase", "any_row", true)
        ));
        teleportBranchUpgrades.add(new EvolutionUpgrade(
            "void_walker_return",
            "Возврат",
            "При смерти возвращается в руку в следующем раунде",
            3,
            EvolutionUpgrade.UpgradeType.ABILITY,
            Map.of("ability", "void_return", "delay", 1)
        ));
        branches.add(new EvolutionBranch("teleport", "Телепортация", teleportBranchUpgrades));
        
        return new EvolutionTreeData("void_walker", branches);
    }
    
    /**
     * Создаёт древо эволюции для Огненного Феникса
     */
    private static EvolutionTreeData createPhoenixTree() {
        List<EvolutionBranch> branches = new ArrayList<>();
        
        // Ветка 1: Возрождение
        List<EvolutionUpgrade> rebirthBranchUpgrades = new ArrayList<>();
        rebirthBranchUpgrades.add(new EvolutionUpgrade(
            "phoenix_rebirth_1",
            "Первое возрождение",
            "При смерти возвращается на поле с полной силой",
            2,
            EvolutionUpgrade.UpgradeType.ABILITY,
            Map.of("ability", "rebirth", "power_restore", 100)
        ));
        rebirthBranchUpgrades.add(new EvolutionUpgrade(
            "phoenix_eternal_flame",
            "Вечное пламя",
            "Может возрождаться до 3 раз за игру",
            5,
            EvolutionUpgrade.UpgradeType.ABILITY,
            Map.of("ability", "eternal_rebirth", "max_rebirths", 3)
        ));
        branches.add(new EvolutionBranch("rebirth", "Возрождение", rebirthBranchUpgrades));
        
        // Ветка 2: Пламя
        List<EvolutionUpgrade> fireBranchUpgrades = new ArrayList<>();
        fireBranchUpgrades.add(new EvolutionUpgrade(
            "phoenix_fire_storm",
            "Огненная буря",
            "При розыгрыше наносит 1 урон всем картам противника",
            3,
            EvolutionUpgrade.UpgradeType.ABILITY,
            Map.of("ability", "fire_storm", "damage", 1, "target", "all")
        ));
        fireBranchUpgrades.add(new EvolutionUpgrade(
            "phoenix_sunrise",
            "Восход солнца",
            "Усиливает все ваши карты на +1 при каждом возрождении",
            4,
            EvolutionUpgrade.UpgradeType.ABILITY,
            Map.of("ability", "sunrise_boost", "power", 1)
        ));
        branches.add(new EvolutionBranch("fire", "Пламя", fireBranchUpgrades));
        
        return new EvolutionTreeData("pyro_phoenix", branches);
    }
    
    /**
     * Создаёт древо эволюции для Стража Природы
     */
    private static EvolutionTreeData createNatureGuardianTree() {
        List<EvolutionBranch> branches = new ArrayList<>();
        
        // Ветка 1: Защита
        List<EvolutionUpgrade> protectionBranchUpgrades = new ArrayList<>();
        protectionBranchUpgrades.add(new EvolutionUpgrade(
            "nature_guardian_shield",
            "Щит природы",
            "Соседние карты получают +1 к силе и защиту от первого урона",
            2,
            EvolutionUpgrade.UpgradeType.ABILITY,
            Map.of("ability", "nature_shield", "power", 1, "damage_shield", 1)
        ));
        protectionBranchUpgrades.add(new EvolutionUpgrade(
            "nature_guardian_grove",
            "Роща",
            "Все карты Детей Рощения на поле получают +2 к силе",
            4,
            EvolutionUpgrade.UpgradeType.ABILITY,
            Map.of("ability", "grove_boost", "faction", "Дети Рощения", "power", 2)
        ));
        branches.add(new EvolutionBranch("protection", "Защита", protectionBranchUpgrades));
        
        // Ветка 2: Рост
        List<EvolutionUpgrade> growthBranchUpgrades = new ArrayList<>();
        growthBranchUpgrades.add(new EvolutionUpgrade(
            "nature_guardian_growth",
            "Рост",
            "В конце каждого раунда получает +1 к силе",
            1,
            EvolutionUpgrade.UpgradeType.ABILITY,
            Map.of("ability", "growth", "power_per_round", 1)
        ));
        growthBranchUpgrades.add(new EvolutionUpgrade(
            "nature_guardian_bloom",
            "Цветение",
            "При достижении 15 силы усиливает все ваши карты на +1",
            3,
            EvolutionUpgrade.UpgradeType.ABILITY,
            Map.of("ability", "bloom", "threshold", 15, "boost", 1)
        ));
        branches.add(new EvolutionBranch("growth", "Рост", growthBranchUpgrades));
        
        return new EvolutionTreeData("nature_guardian", branches);
    }
    
    /**
     * Данные древа эволюции карты
     */
    public static class EvolutionTreeData {
        private final String cardId;
        private final List<EvolutionBranch> branches;
        
        public EvolutionTreeData(String cardId, List<EvolutionBranch> branches) {
            this.cardId = cardId;
            this.branches = new ArrayList<>(branches);
        }
        
        public String getCardId() {
            return cardId;
        }
        
        public List<EvolutionBranch> getBranches() {
            return new ArrayList<>(branches);
        }
        
        public EvolutionBranch getBranch(String branchId) {
            return branches.stream()
                .filter(b -> b.getId().equals(branchId))
                .findFirst()
                .orElse(null);
        }
    }
    
    /**
     * Ветка улучшений
     */
    public static class EvolutionBranch {
        private final String id;
        private final String name;
        private final List<EvolutionUpgrade> upgrades;
        
        public EvolutionBranch(String id, String name, List<EvolutionUpgrade> upgrades) {
            this.id = id;
            this.name = name;
            this.upgrades = new ArrayList<>(upgrades);
        }
        
        public String getId() {
            return id;
        }
        
        public String getName() {
            return name;
        }
        
        public List<EvolutionUpgrade> getUpgrades() {
            return new ArrayList<>(upgrades);
        }
    }
    
    /**
     * Улучшение в ветке
     */
    public static class EvolutionUpgrade {
        private final String id;
        private final String name;
        private final String description;
        private final int requiredLevel;
        private final UpgradeType type;
        private final Map<String, Object> properties;
        
        public EvolutionUpgrade(String id, String name, String description, 
                               int requiredLevel, UpgradeType type, Map<String, Object> properties) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.requiredLevel = requiredLevel;
            this.type = type;
            this.properties = new HashMap<>(properties);
        }
        
        public String getId() {
            return id;
        }
        
        public String getName() {
            return name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public int getRequiredLevel() {
            return requiredLevel;
        }
        
        public UpgradeType getType() {
            return type;
        }
        
        public Map<String, Object> getProperties() {
            return new HashMap<>(properties);
        }
        
        /**
         * Типы улучшений
         */
        public enum UpgradeType {
            /** Увеличение базовой силы карты */
            POWER_BOOST,
            /** Новая способность */
            ABILITY,
            /** Улучшение существующей способности */
            ABILITY_UPGRADE,
            /** Пассивный эффект */
            PASSIVE
        }
    }
}

