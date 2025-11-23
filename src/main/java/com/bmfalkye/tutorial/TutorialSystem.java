package com.bmfalkye.tutorial;

import com.bmfalkye.player.PlayerProgress;
import com.bmfalkye.storage.PlayerProgressStorage;
import com.bmfalkye.storage.TutorialHintStorage;
import com.bmfalkye.util.ModLogger;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Система подсказок для новых игроков
 * Показывает контекстные подсказки в зависимости от прогресса игрока
 */
public class TutorialSystem {
    
    /**
     * Типы подсказок
     */
    public enum HintType {
        FIRST_GAME("first_game", "Добро пожаловать в Falkye! Это ваша первая игра. Цель - выиграть 2 раунда из 3."),
        HOW_TO_PLAY_CARD("how_to_play_card", "Кликните на карту в руке, затем выберите ряд для размещения."),
        HOW_TO_PASS("how_to_pass", "Нажмите кнопку 'Пас', чтобы завершить свой ход в раунде."),
        HOW_TO_USE_LEADER("how_to_use_leader", "Используйте способность лидера кнопкой 'Лидер' для мощного эффекта."),
        CARD_TYPES("card_types", "Существа идут в ряды, заклинания применяют эффекты, особые карты имеют уникальные способности."),
        ROUNDS("rounds", "Игра состоит из 3 раундов. Победитель раунда определяется по сумме силы карт на поле."),
        WEATHER("weather", "Погодные эффекты влияют на силу карт. Используйте их стратегически."),
        DECK_EDITOR("deck_editor", "Создайте свою колоду из карт в коллекции. Колода должна содержать 10 карт."),
        COLLECTION("collection", "Собирайте карты, побеждая противников и выполняя задания."),
        BETTING("betting", "Делайте ставки на матчи для получения дополнительных монет."),
        LOCATION_EFFECTS("location_effects", "Локация влияет на силу карт. Играйте стратегически в зависимости от биома.");
        
        private final String id;
        private final String message;
        
        HintType(String id, String message) {
            this.id = id;
            this.message = message;
        }
        
        public String getId() {
            return id;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    /**
     * Проверяет, является ли игрок новым
     */
    public static boolean isNewPlayer(ServerPlayer player, ServerLevel level) {
        if (player == null || level == null) {
            return false;
        }
        
        PlayerProgressStorage storage = PlayerProgressStorage.get(level);
        PlayerProgress progress = storage.getPlayerProgress(player);
        
        // Игрок считается новым, если:
        // - Уровень <= 2
        // - Сыграно <= 3 игр
        return progress.getLevel() <= 2 && progress.getTotalGamesPlayed() <= 3;
    }
    
    /**
     * Проверяет, нужно ли показать подсказку
     */
    public static boolean shouldShowHint(ServerPlayer player, ServerLevel level, HintType hintType) {
        if (player == null || level == null || hintType == null) {
            return false;
        }
        
        // Если игрок не новый, не показываем подсказки
        if (!isNewPlayer(player, level)) {
            return false;
        }
        
        // Проверяем, видел ли игрок эту подсказку
        TutorialHintStorage hintStorage = TutorialHintStorage.get(level);
        if (hintStorage.hasSeenHint(player, hintType.getId())) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Показывает подсказку игроку
     */
    public static void showHint(ServerPlayer player, ServerLevel level, HintType hintType) {
        if (player == null || level == null || hintType == null) {
            return;
        }
        
        if (!shouldShowHint(player, level, hintType)) {
            return;
        }
        
        // Отправляем подсказку игроку
        Component hintMessage = Component.literal("§e[Подсказка] §f" + hintType.getMessage());
        player.sendSystemMessage(hintMessage);
        
        // Отмечаем подсказку как просмотренную
        TutorialHintStorage hintStorage = TutorialHintStorage.get(level);
        hintStorage.markHintAsSeen(player, hintType.getId());
        
        ModLogger.logGameEvent("Tutorial hint shown",
            "player", player.getName().getString(),
            "hintType", hintType.getId());
    }
    
    /**
     * Показывает подсказку при первом входе в игру
     */
    public static void showFirstGameHint(ServerPlayer player, ServerLevel level) {
        showHint(player, level, HintType.FIRST_GAME);
    }
    
    /**
     * Показывает подсказку о том, как играть карты
     */
    public static void showPlayCardHint(ServerPlayer player, ServerLevel level) {
        showHint(player, level, HintType.HOW_TO_PLAY_CARD);
    }
    
    /**
     * Показывает подсказку о пасе
     */
    public static void showPassHint(ServerPlayer player, ServerLevel level) {
        showHint(player, level, HintType.HOW_TO_PASS);
    }
    
    /**
     * Показывает подсказку об использовании лидера
     */
    public static void showLeaderHint(ServerPlayer player, ServerLevel level) {
        showHint(player, level, HintType.HOW_TO_USE_LEADER);
    }
    
    /**
     * Показывает подсказку о типах карт
     */
    public static void showCardTypesHint(ServerPlayer player, ServerLevel level) {
        showHint(player, level, HintType.CARD_TYPES);
    }
    
    /**
     * Показывает подсказку о раундах
     */
    public static void showRoundsHint(ServerPlayer player, ServerLevel level) {
        showHint(player, level, HintType.ROUNDS);
    }
    
    /**
     * Показывает подсказку о погоде
     */
    public static void showWeatherHint(ServerPlayer player, ServerLevel level) {
        showHint(player, level, HintType.WEATHER);
    }
    
    /**
     * Показывает подсказку о редакторе колод
     */
    public static void showDeckEditorHint(ServerPlayer player, ServerLevel level) {
        showHint(player, level, HintType.DECK_EDITOR);
    }
    
    /**
     * Показывает подсказку о коллекции
     */
    public static void showCollectionHint(ServerPlayer player, ServerLevel level) {
        showHint(player, level, HintType.COLLECTION);
    }
    
    /**
     * Показывает подсказку о ставках
     */
    public static void showBettingHint(ServerPlayer player, ServerLevel level) {
        showHint(player, level, HintType.BETTING);
    }
    
    /**
     * Показывает подсказку о эффектах локации
     */
    public static void showLocationEffectsHint(ServerPlayer player, ServerLevel level) {
        showHint(player, level, HintType.LOCATION_EFFECTS);
    }
    
    /**
     * Получает список всех подсказок, которые игрок ещё не видел
     */
    public static List<HintType> getUnseenHints(ServerPlayer player, ServerLevel level) {
        List<HintType> unseenHints = new ArrayList<>();
        
        if (player == null || level == null) {
            return unseenHints;
        }
        
        TutorialHintStorage hintStorage = TutorialHintStorage.get(level);
        
        for (HintType hintType : HintType.values()) {
            if (!hintStorage.hasSeenHint(player, hintType.getId())) {
                unseenHints.add(hintType);
            }
        }
        
        return unseenHints;
    }
    
    /**
     * Сбрасывает все подсказки для игрока (для тестирования)
     */
    public static void resetHints(ServerPlayer player, ServerLevel level) {
        if (player == null || level == null) {
            return;
        }
        
        TutorialHintStorage hintStorage = TutorialHintStorage.get(level);
        hintStorage.clearPlayerHints(player);
        
        ModLogger.logGameEvent("Tutorial hints reset",
            "player", player.getName().getString());
    }
}
