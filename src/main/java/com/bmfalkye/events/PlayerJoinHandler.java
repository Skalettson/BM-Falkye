package com.bmfalkye.events;

import com.bmfalkye.BMFalkye;
import com.bmfalkye.achievements.AchievementSystem;
import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRegistry;
import com.bmfalkye.player.PlayerProgress;
import com.bmfalkye.storage.PlayerCardCollection;
import com.bmfalkye.storage.PlayerProgressStorage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Обработчик входа игрока
 */
@Mod.EventBusSubscriber(modid = BMFalkye.MOD_ID)
public class PlayerJoinHandler {
    
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Пытаемся восстановить игровую сессию
            if (com.bmfalkye.game.ReconnectManager.tryReconnect(player)) {
                // Сессия восстановлена, пропускаем остальную инициализацию
                return;
            }
            
            if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                // Очищаем дубликаты из коллекции (если они есть)
                PlayerCardCollection collection = PlayerCardCollection.get(serverLevel);
                if (collection.removeDuplicates(player)) {
                    BMFalkye.LOGGER.info("Removed duplicate cards from player {} collection", player.getName().getString());
                }
                
                // Загружаем статистику балансировки карт (при первом обращении)
                com.bmfalkye.storage.CardBalanceStorage.get(serverLevel);
                
                // Инициализируем хранилище подсказок
                com.bmfalkye.storage.TutorialHintStorage.get(serverLevel);
            }
            
            // Выдаем стартовые карты при первом входе (секретно)
            giveStartingCards(player);
            
            // Проверяем достижения
            PlayerProgressStorage storage = PlayerProgressStorage.get((net.minecraft.server.level.ServerLevel) player.level());
            PlayerProgress progress = storage.getPlayerProgress(player);
            AchievementSystem.checkAchievements(player, progress);
            storage.setPlayerProgress(player, progress);
            
            // Проверяем активные события
            EventSystem.checkActiveEvents(player);
            
            // Вызываем событие входа игрока через API
            // TODO: Реализовать через GameEventSystem при необходимости
        }
    }
    
    /**
     * Выдает 10 случайных карт при первом входе игрока (секретно, без уведомлений)
     */
    private static void giveStartingCards(ServerPlayer player) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        
        PlayerCardCollection collection = PlayerCardCollection.get(serverLevel);
        
        // Проверяем, первый ли это вход
        if (!collection.isFirstJoin(player)) {
            return; // Уже выдавали карты
        }
        
        // Получаем все доступные карты
        List<Card> allCards = CardRegistry.getAllCards();
        if (allCards.isEmpty()) {
            BMFalkye.LOGGER.warn("No cards available to give to new player");
            return;
        }
        
        // Выбираем 10 случайных карт (исключая те, что уже есть в коллекции)
        Random random = new Random();
        List<Card> selectedCards = new ArrayList<>();
        List<Card> availableCards = new ArrayList<>(allCards);
        
        // Удаляем из доступных карт те, что уже есть в коллекции
        Set<String> existingCardIds = collection.getPlayerCollection(player);
        availableCards.removeIf(card -> existingCardIds.contains(card.getId()));
        
        for (int i = 0; i < 10 && !availableCards.isEmpty(); i++) {
            Card selectedCard = availableCards.get(random.nextInt(availableCards.size()));
            selectedCards.add(selectedCard);
            availableCards.remove(selectedCard);
        }
        
        // Добавляем карты в коллекцию (секретно, без уведомлений)
        // addCard использует Set, поэтому дубликаты автоматически не добавятся
        for (Card card : selectedCards) {
            collection.addCard(player, card.getId());
        }
        
        // Отправляем обновлённую коллекцию на клиент (но не уведомляем игрока о картах)
        java.util.List<Card> cards = collection.getCards(player);
        java.util.List<String> cardIds = cards.stream()
            .map(Card::getId)
            .collect(java.util.stream.Collectors.toList());
        com.bmfalkye.network.NetworkHandler.INSTANCE.sendTo(
            new com.bmfalkye.network.NetworkHandler.SendCardCollectionPacket(cardIds),
            player.connection.connection,
            net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
        );
        
        BMFalkye.LOGGER.info("Gave {} starting cards to player {} (secretly)", selectedCards.size(), player.getName().getString());
    }
}

