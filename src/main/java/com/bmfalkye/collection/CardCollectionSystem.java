package com.bmfalkye.collection;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRegistry;
import com.bmfalkye.cards.CardRarity;
import com.bmfalkye.player.PlayerProgress;
import com.bmfalkye.storage.PlayerProgressStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Система коллекций - визуальный альбом всех карт
 */
public class CardCollectionSystem {
    
    /**
     * Получает статистику коллекции игрока
     */
    public static CollectionStats getCollectionStats(ServerPlayer player) {
        PlayerProgressStorage storage = PlayerProgressStorage.get((net.minecraft.server.level.ServerLevel) player.level());
        PlayerProgress progress = storage.getPlayerProgress(player);
        
        Set<String> unlockedCards = progress.getUnlockedCards();
        List<Card> allCards = CardRegistry.getAllCards();
        
        int totalCards = allCards.size();
        int unlockedCount = unlockedCards.size();
        int lockedCount = totalCards - unlockedCount;
        double completionPercentage = totalCards > 0 ? (unlockedCount * 100.0 / totalCards) : 0.0;
        
        // Статистика по редкости
        Map<CardRarity, Integer> rarityStats = new HashMap<>();
        Map<CardRarity, Integer> rarityUnlocked = new HashMap<>();
        
        for (CardRarity rarity : CardRarity.values()) {
            List<Card> cardsByRarity = CardRegistry.getCardsByRarity(rarity);
            rarityStats.put(rarity, cardsByRarity.size());
            
            int unlocked = 0;
            for (Card card : cardsByRarity) {
                if (unlockedCards.contains(card.getId())) {
                    unlocked++;
                }
            }
            rarityUnlocked.put(rarity, unlocked);
        }
        
        // Статистика по фракциям
        Map<String, Integer> factionStats = new HashMap<>();
        Map<String, Integer> factionUnlocked = new HashMap<>();
        
        Set<String> factions = allCards.stream()
            .map(Card::getFaction)
            .collect(Collectors.toSet());
        
        for (String faction : factions) {
            List<Card> cardsByFaction = CardRegistry.getCardsByFaction(faction);
            factionStats.put(faction, cardsByFaction.size());
            
            int unlocked = 0;
            for (Card card : cardsByFaction) {
                if (unlockedCards.contains(card.getId())) {
                    unlocked++;
                }
            }
            factionUnlocked.put(faction, unlocked);
        }
        
        return new CollectionStats(
            totalCards, unlockedCount, lockedCount, completionPercentage,
            rarityStats, rarityUnlocked, factionStats, factionUnlocked
        );
    }
    
    /**
     * Получает список всех карт с информацией о разблокировке
     */
    public static List<CardCollectionEntry> getAllCardsWithStatus(ServerPlayer player) {
        PlayerProgressStorage storage = PlayerProgressStorage.get((net.minecraft.server.level.ServerLevel) player.level());
        PlayerProgress progress = storage.getPlayerProgress(player);
        Set<String> unlockedCards = progress.getUnlockedCards();
        
        List<Card> allCards = CardRegistry.getAllCards();
        allCards.sort(Comparator
            .comparing((Card c) -> !unlockedCards.contains(c.getId())) // Сначала разблокированные
            .thenComparing(Card::getRarity, Comparator.reverseOrder()) // Потом по редкости
            .thenComparing(Card::getFaction) // Потом по фракции
            .thenComparing(Card::getName)); // Потом по имени
        
        List<CardCollectionEntry> entries = new ArrayList<>();
        for (Card card : allCards) {
            boolean unlocked = unlockedCards.contains(card.getId());
            entries.add(new CardCollectionEntry(card, unlocked));
        }
        
        return entries;
    }
    
    /**
     * Получает карты по фильтру
     */
    public static List<CardCollectionEntry> getFilteredCards(ServerPlayer player, 
                                                             CardRarity rarityFilter, 
                                                             String factionFilter,
                                                             Boolean unlockedFilter) {
        List<CardCollectionEntry> allEntries = getAllCardsWithStatus(player);
        
        return allEntries.stream()
            .filter(entry -> {
                if (rarityFilter != null && entry.getCard().getRarity() != rarityFilter) {
                    return false;
                }
                if (factionFilter != null && !entry.getCard().getFaction().equals(factionFilter)) {
                    return false;
                }
                if (unlockedFilter != null && entry.isUnlocked() != unlockedFilter) {
                    return false;
                }
                return true;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Отправляет информацию о коллекции игроку
     */
    public static void sendCollectionInfo(ServerPlayer player) {
        CollectionStats stats = getCollectionStats(player);
        
        player.sendSystemMessage(Component.literal("§6§l=== ВАША КОЛЛЕКЦИЯ ==="));
        player.sendSystemMessage(Component.literal("§7Всего карт: §f" + stats.getTotalCards()));
        player.sendSystemMessage(Component.literal("§aРазблокировано: §f" + stats.getUnlockedCount()));
        player.sendSystemMessage(Component.literal("§cЗаблокировано: §f" + stats.getLockedCount()));
        player.sendSystemMessage(Component.literal("§eПрогресс: §f" + String.format("%.1f", stats.getCompletionPercentage()) + "%"));
        
        player.sendSystemMessage(Component.literal("§6--- По редкости ---"));
        for (CardRarity rarity : CardRarity.values()) {
            int total = stats.getRarityStats().getOrDefault(rarity, 0);
            int unlocked = stats.getRarityUnlocked().getOrDefault(rarity, 0);
            String color = getRarityColor(rarity);
            player.sendSystemMessage(Component.literal(
                color + rarity.getDisplayName() + ": §f" + unlocked + "/" + total));
        }
        
        player.sendSystemMessage(Component.literal("§6--- По фракциям ---"));
        for (String faction : stats.getFactionStats().keySet()) {
            int total = stats.getFactionStats().get(faction);
            int unlocked = stats.getFactionUnlocked().get(faction);
            player.sendSystemMessage(Component.literal(
                "§7" + faction + ": §f" + unlocked + "/" + total));
        }
    }
    
    /**
     * Получает цвет для редкости
     */
    private static String getRarityColor(CardRarity rarity) {
        return switch (rarity) {
            case COMMON -> "§7";
            case RARE -> "§b";
            case EPIC -> "§5";
            case LEGENDARY -> "§6";
        };
    }
    
    /**
     * Статистика коллекции
     */
    public static class CollectionStats {
        private final int totalCards;
        private final int unlockedCount;
        private final int lockedCount;
        private final double completionPercentage;
        private final Map<CardRarity, Integer> rarityStats;
        private final Map<CardRarity, Integer> rarityUnlocked;
        private final Map<String, Integer> factionStats;
        private final Map<String, Integer> factionUnlocked;
        
        public CollectionStats(int totalCards, int unlockedCount, int lockedCount, double completionPercentage,
                              Map<CardRarity, Integer> rarityStats, Map<CardRarity, Integer> rarityUnlocked,
                              Map<String, Integer> factionStats, Map<String, Integer> factionUnlocked) {
            this.totalCards = totalCards;
            this.unlockedCount = unlockedCount;
            this.lockedCount = lockedCount;
            this.completionPercentage = completionPercentage;
            this.rarityStats = rarityStats;
            this.rarityUnlocked = rarityUnlocked;
            this.factionStats = factionStats;
            this.factionUnlocked = factionUnlocked;
        }
        
        public int getTotalCards() { return totalCards; }
        public int getUnlockedCount() { return unlockedCount; }
        public int getLockedCount() { return lockedCount; }
        public double getCompletionPercentage() { return completionPercentage; }
        public Map<CardRarity, Integer> getRarityStats() { return rarityStats; }
        public Map<CardRarity, Integer> getRarityUnlocked() { return rarityUnlocked; }
        public Map<String, Integer> getFactionStats() { return factionStats; }
        public Map<String, Integer> getFactionUnlocked() { return factionUnlocked; }
    }
    
    /**
     * Запись в коллекции
     */
    public static class CardCollectionEntry {
        private final Card card;
        private final boolean unlocked;
        
        public CardCollectionEntry(Card card, boolean unlocked) {
            this.card = card;
            this.unlocked = unlocked;
        }
        
        public Card getCard() { return card; }
        public boolean isUnlocked() { return unlocked; }
    }
}

