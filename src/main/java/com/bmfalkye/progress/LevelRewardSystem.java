package com.bmfalkye.progress;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRarity;
import com.bmfalkye.cards.CardRegistry;
import com.bmfalkye.player.PlayerProgress;
import com.bmfalkye.storage.PlayerCardCollection;
import com.bmfalkye.storage.PlayerProgressStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Система наград за повышение уровня
 */
public class LevelRewardSystem {
    
    /**
     * Выдаёт награды за повышение уровня
     * @param player игрок, который повысил уровень
     * @param newLevel новый уровень игрока
     */
    public static void giveLevelUpReward(ServerPlayer player, int newLevel) {
        if (player == null || !(player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        
        PlayerProgressStorage storage = PlayerProgressStorage.get(serverLevel);
        PlayerProgress progress = storage.getPlayerProgress(player);
        PlayerCardCollection collection = PlayerCardCollection.get(serverLevel);
        
        // Вычисляем награды в зависимости от уровня
        LevelReward reward = calculateReward(newLevel);
        
        // Выдаём монеты
        progress.addCoins(reward.coins);
        
        // Выдаём карты
        List<Card> rewardedCards = new ArrayList<>();
        for (int i = 0; i < reward.cardCount; i++) {
            Card randomCard = getRandomCardByRarity(reward.cardRarity);
            if (randomCard != null && !collection.hasCard(player, randomCard.getId())) {
                collection.addCard(player, randomCard.getId());
                progress.unlockCard(randomCard.getId());
                rewardedCards.add(randomCard);
            }
        }
        
        // Сохраняем прогресс
        storage.setPlayerProgress(player, progress);
        
        // Уведомляем игрока
        player.sendSystemMessage(Component.literal("§6§l═══════════════════════════"));
        player.sendSystemMessage(Component.literal("§e§lПОВЫШЕНИЕ УРОВНЯ!"));
        player.sendSystemMessage(Component.literal("§7Вы достигли уровня §e" + newLevel));
        player.sendSystemMessage(Component.literal("§7═══════════════════════════"));
        
        if (reward.coins > 0) {
            player.sendSystemMessage(Component.literal("§6+ " + reward.coins + " монет"));
        }
        
        if (!rewardedCards.isEmpty()) {
            player.sendSystemMessage(Component.literal("§b+ " + rewardedCards.size() + " карт:"));
            for (Card card : rewardedCards) {
                String rarityColor = getRarityColor(card.getRarity());
                player.sendSystemMessage(Component.literal("  " + rarityColor + "• " + card.getName()));
            }
        }
        
        player.sendSystemMessage(Component.literal("§6§l═══════════════════════════"));
    }
    
    /**
     * Вычисляет награды для уровня
     */
    private static LevelReward calculateReward(int level) {
        LevelReward reward = new LevelReward();
        
        // Базовые награды
        reward.coins = 100 + (level * 10); // 100 + 10 за каждый уровень
        
        // Количество карт зависит от уровня
        if (level <= 5) {
            reward.cardCount = 1;
            reward.cardRarity = CardRarity.COMMON;
        } else if (level <= 10) {
            reward.cardCount = 1;
            reward.cardRarity = CardRarity.RARE;
        } else if (level <= 20) {
            reward.cardCount = 2;
            reward.cardRarity = CardRarity.RARE;
        } else if (level <= 30) {
            reward.cardCount = 2;
            reward.cardRarity = CardRarity.EPIC;
        } else if (level <= 40) {
            reward.cardCount = 3;
            reward.cardRarity = CardRarity.EPIC;
        } else {
            reward.cardCount = 3;
            reward.cardRarity = CardRarity.LEGENDARY;
        }
        
        // Особые награды за круглые уровни
        if (level % 10 == 0) {
            reward.coins += 500; // Бонус 500 монет за каждый 10-й уровень
            reward.cardCount += 1; // Дополнительная карта
        }
        
        // Особые награды за уровни 25, 50
        if (level == 25) {
            reward.coins += 1000;
            reward.cardCount += 2;
        } else if (level == 50) {
            reward.coins += 5000;
            reward.cardCount += 5;
            reward.cardRarity = CardRarity.LEGENDARY; // Только легендарные карты на 50 уровне
        }
        
        return reward;
    }
    
    /**
     * Получает случайную карту по редкости
     */
    private static Card getRandomCardByRarity(CardRarity rarity) {
        Random random = new Random();
        List<Card> cards = CardRegistry.getCardsByRarity(rarity);
        
        if (cards.isEmpty()) {
            // Если нет карт нужной редкости, пробуем более низкую
            if (rarity == CardRarity.LEGENDARY) {
                cards = CardRegistry.getCardsByRarity(CardRarity.EPIC);
            }
            if (cards.isEmpty() && rarity.ordinal() >= CardRarity.EPIC.ordinal()) {
                cards = CardRegistry.getCardsByRarity(CardRarity.RARE);
            }
            if (cards.isEmpty()) {
                cards = CardRegistry.getCardsByRarity(CardRarity.COMMON);
            }
        }
        
        if (cards.isEmpty()) {
            return null;
        }
        
        return cards.get(random.nextInt(cards.size()));
    }
    
    /**
     * Получает цвет для редкости карты
     */
    private static String getRarityColor(CardRarity rarity) {
        return switch (rarity) {
            case COMMON -> "§7"; // Серый
            case RARE -> "§9"; // Синий
            case EPIC -> "§5"; // Фиолетовый
            case LEGENDARY -> "§6"; // Золотой
        };
    }
    
    /**
     * Класс для хранения информации о награде
     */
    private static class LevelReward {
        int coins = 0;
        int cardCount = 0;
        CardRarity cardRarity = CardRarity.COMMON;
    }
}

