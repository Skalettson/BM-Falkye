package com.bmfalkye.draft;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRarity;
import com.bmfalkye.cards.CardRegistry;
import com.bmfalkye.network.NetworkHandler;
import com.bmfalkye.storage.PlayerCardCollection;
import com.bmfalkye.storage.PlayerCurrency;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Система управления драфтом "Великий Турнир"
 */
public class DraftSystem {
    private static final int ENTRY_COST_COINS = 100;
    
    /**
     * Начинает драфт для игрока (проверяет входную плату)
     */
    public static boolean startDraft(ServerPlayer player, boolean useTicket) {
        if (player.level() instanceof ServerLevel serverLevel) {
            DraftStorage storage = DraftStorage.get(serverLevel);
            
            // Проверяем, нет ли уже активной сессии
            if (storage.hasActiveSession(player)) {
                player.sendSystemMessage(Component.literal("§cУ вас уже есть активная сессия драфта!"));
                return false;
            }
            
            // Проверяем входную плату
            if (!useTicket) {
                PlayerCurrency currency = PlayerCurrency.get(serverLevel);
                if (!currency.hasEnoughCoins(player, ENTRY_COST_COINS)) {
                    player.sendSystemMessage(Component.literal("§cНедостаточно монет! Требуется: " + ENTRY_COST_COINS));
                    return false;
                }
                
                // Списываем монеты
                currency.removeCoins(player, ENTRY_COST_COINS);
            } else {
                // Проверяем наличие билета в инвентаре
                boolean hasTicket = false;
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
                    if (!stack.isEmpty() && stack.getItem() == com.bmfalkye.items.ModItems.TOURNAMENT_TICKET.get()) {
                        hasTicket = true;
                        // Удаляем билет (будет удалён в TournamentTicketItem.use)
                        break;
                    }
                }
                
                if (!hasTicket) {
                    player.sendSystemMessage(Component.literal("§cУ вас нет билета турнира!"));
                    return false;
                }
            }
            
            // Создаём сессию драфта
            DraftSession session = storage.createSession(player);
            player.sendSystemMessage(Component.literal("§6Драфт начат! Выберите 30 карт из 90 предложенных."));
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Выбирает карту в драфте
     */
    public static boolean selectCard(ServerPlayer player, int choiceIndex, int cardIndex) {
        if (player.level() instanceof ServerLevel serverLevel) {
            DraftStorage storage = DraftStorage.get(serverLevel);
            DraftSession session = storage.getSession(player);
            
            if (session == null || !session.isActive()) {
                return false;
            }
            
            // Используем текущий выбор, а не переданный индекс
            DraftSession.CardChoice currentChoice = session.getCurrentChoice();
            if (currentChoice == null) {
                return false;
            }
            
            boolean success = session.selectCardFromChoice(currentChoice.getIndex(), cardIndex);
            if (success) {
                // Отправляем обновление на клиент
                NetworkHandler.INSTANCE.sendTo(new NetworkHandler.RequestDraftDataPacket(),
                    player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                
                // Проверяем, завершён ли драфт
                if (session.isCompleted()) {
                    player.sendSystemMessage(Component.literal("§aДрафт завершён! Теперь начните арену."));
                }
            }
            
            return success;
        }
        
        return false;
    }
    
    /**
     * Начинает арену после завершения драфта
     */
    public static boolean startArena(ServerPlayer player) {
        if (player.level() instanceof ServerLevel serverLevel) {
            DraftStorage storage = DraftStorage.get(serverLevel);
            DraftSession session = storage.getSession(player);
            
            if (session == null || !session.isCompleted()) {
                player.sendSystemMessage(Component.literal("§cСначала завершите драфт!"));
                return false;
            }
            
            if (session.isArenaCompleted()) {
                player.sendSystemMessage(Component.literal("§cАрена уже завершена!"));
                return false;
            }
            
            // Добавляем игрока в очередь арены
            com.bmfalkye.draft.DraftArenaManager.queueForArena(player);
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Выдаёт награды за арену
     */
    public static void giveArenaRewards(ServerPlayer player, int wins) {
        if (player.level() instanceof ServerLevel serverLevel) {
            DraftStorage storage = DraftStorage.get(serverLevel);
            DraftSession session = storage.getSession(player);
            
            if (session == null) {
                return;
            }
            
            // Награды зависят от количества побед
            int coins = calculateCoinReward(wins);
            int cards = calculateCardReward(wins);
            boolean legendaryCard = wins >= 7;
            boolean exclusiveCardBack = wins >= 7;
            
            PlayerCurrency currency = PlayerCurrency.get(serverLevel);
            currency.addCoins(player, coins);
            
            // Выдаём карты
            PlayerCardCollection collection = PlayerCardCollection.get(serverLevel);
            List<String> rewardedCards = new ArrayList<>();
            
            // Выдаём обычные карты
            for (int i = 0; i < cards; i++) {
                String randomCardId = getRandomCardId();
                if (randomCardId != null) {
                    collection.addCard(player, randomCardId);
                    rewardedCards.add(randomCardId);
                }
            }
            
            // Выдаём легендарную карту при 7 победах
            if (legendaryCard) {
                String legendaryCardId = getRandomLegendaryCardId();
                if (legendaryCardId != null) {
                    collection.addCard(player, legendaryCardId);
                    rewardedCards.add(legendaryCardId);
                }
            }
            
            // Выдаём эксклюзивную рубашку при 7 победах (TODO: реализовать систему косметики)
            if (exclusiveCardBack) {
                // TODO: Выдать эксклюзивную рубашку "Великий Турнир"
                // Пока что просто уведомляем
            }
            
            // Отправляем сообщения о наградах
            player.sendSystemMessage(Component.literal("§6§l═══════════════════════════"));
            player.sendSystemMessage(Component.literal("§6§lНАГРАДЫ ЗА АРЕНУ"));
            player.sendSystemMessage(Component.literal("§7Побед: §e" + wins + "§7/§e7"));
            player.sendSystemMessage(Component.literal("§eМонет: §f" + coins));
            player.sendSystemMessage(Component.literal("§6Карт: §f" + cards));
            
            if (legendaryCard) {
                player.sendSystemMessage(Component.literal("§6§l★ ЛЕГЕНДАРНАЯ КАРТА ★"));
                if (!rewardedCards.isEmpty()) {
                    Card legendaryCardObj = CardRegistry.getCard(rewardedCards.get(rewardedCards.size() - 1));
                    if (legendaryCardObj != null) {
                        player.sendSystemMessage(Component.literal("§6" + legendaryCardObj.getName()));
                    }
                }
            }
            
            if (exclusiveCardBack) {
                player.sendSystemMessage(Component.literal("§d§l★ ЭКСКЛЮЗИВНАЯ РУБАШКА ★"));
                player.sendSystemMessage(Component.literal("§7Рубашка \"Великий Турнир\" получена!"));
            }
            
            player.sendSystemMessage(Component.literal("§6§l═══════════════════════════"));
            
            // Показываем список полученных карт
            if (!rewardedCards.isEmpty()) {
                player.sendSystemMessage(Component.literal("§7Полученные карты:"));
                for (String cardId : rewardedCards) {
                    Card card = CardRegistry.getCard(cardId);
                    if (card != null) {
                        String rarityColor = getRarityColor(card.getRarity());
                        player.sendSystemMessage(Component.literal("  " + rarityColor + card.getName()));
                    }
                }
            }
            
            // Удаляем сессию
            storage.removeSession(player);
        }
    }
    
    /**
     * Получает случайную карту
     */
    private static String getRandomCardId() {
        List<Card> allCards = CardRegistry.getAllCards();
        if (allCards.isEmpty()) {
            return null;
        }
        Random random = new Random();
        Card randomCard = allCards.get(random.nextInt(allCards.size()));
        return randomCard != null ? randomCard.getId() : null;
    }
    
    /**
     * Получает случайную легендарную карту
     */
    private static String getRandomLegendaryCardId() {
        List<Card> allCards = CardRegistry.getAllCards();
        List<Card> legendaryCards = allCards.stream()
            .filter(card -> card != null && card.getRarity() == CardRarity.LEGENDARY)
            .collect(java.util.stream.Collectors.toList());
        
        if (legendaryCards.isEmpty()) {
            // Если нет легендарных карт, возвращаем случайную редкую или эпическую
            List<Card> rareCards = allCards.stream()
                .filter(card -> card != null && 
                    (card.getRarity() == CardRarity.RARE || card.getRarity() == CardRarity.EPIC))
                .collect(java.util.stream.Collectors.toList());
            
            if (!rareCards.isEmpty()) {
                Random random = new Random();
                Card randomCard = rareCards.get(random.nextInt(rareCards.size()));
                return randomCard != null ? randomCard.getId() : null;
            }
            
            // Если и редких нет, возвращаем любую карту
            return getRandomCardId();
        }
        
        Random random = new Random();
        Card legendaryCard = legendaryCards.get(random.nextInt(legendaryCards.size()));
        return legendaryCard != null ? legendaryCard.getId() : null;
    }
    
    /**
     * Получает цвет редкости для отображения
     */
    private static String getRarityColor(CardRarity rarity) {
        if (rarity == CardRarity.COMMON) return "§f";
        if (rarity == CardRarity.RARE) return "§b";
        if (rarity == CardRarity.EPIC) return "§5";
        if (rarity == CardRarity.LEGENDARY) return "§6";
        return "§f";
    }
    
    private static int calculateCoinReward(int wins) {
        // Награды: 0-2 победы = 50, 3-4 = 100, 5-6 = 200, 7 = 500
        if (wins >= 7) return 500;
        if (wins >= 5) return 200;
        if (wins >= 3) return 100;
        return 50;
    }
    
    private static int calculateCardReward(int wins) {
        // Награды: 0-2 победы = 1, 3-4 = 2, 5-6 = 3, 7 = 5
        if (wins >= 7) return 5;
        if (wins >= 5) return 3;
        if (wins >= 3) return 2;
        return 1;
    }
}

