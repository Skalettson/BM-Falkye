package com.bmfalkye.loot;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRarity;
import com.bmfalkye.cards.CardRegistry;
import com.bmfalkye.items.CardItem;
import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Модификатор лута для добавления карт в любые структуры
 */
public class CardLootModifier extends LootModifier {
    public static final Supplier<Codec<CardLootModifier>> CODEC = Suppliers.memoize(() ->
        RecordCodecBuilder.create(inst -> codecStart(inst).and(
            Codec.FLOAT.fieldOf("commonChance").forGetter(m -> m.commonChance)
        ).and(
            Codec.FLOAT.fieldOf("rareChance").forGetter(m -> m.rareChance)
        ).and(
            Codec.FLOAT.fieldOf("epicChance").forGetter(m -> m.epicChance)
        ).and(
            Codec.FLOAT.fieldOf("legendaryChance").forGetter(m -> m.legendaryChance)
        ).apply(inst, CardLootModifier::new))
    );
    
    private final float commonChance;
    private final float rareChance;
    private final float epicChance;
    private final float legendaryChance;
    private static final Random RANDOM = new Random();
    
    public CardLootModifier(LootItemCondition[] conditions, float commonChance, float rareChance, float epicChance, float legendaryChance) {
        super(conditions);
        this.commonChance = commonChance;
        this.rareChance = rareChance;
        this.epicChance = epicChance;
        this.legendaryChance = legendaryChance;
    }
    
    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        ResourceLocation lootTableId = context.getQueriedLootTableId();
        
        com.bmfalkye.util.ModLogger.debug("CardLootModifier: doApply called for loot table {}", lootTableId);
        
        // Сначала проверяем условия (если они есть)
        if (conditions != null && conditions.length > 0) {
            com.bmfalkye.util.ModLogger.debug("CardLootModifier: Checking {} conditions", conditions.length);
            for (LootItemCondition condition : conditions) {
                if (!condition.test(context)) {
                    com.bmfalkye.util.ModLogger.debug("CardLootModifier: Condition failed for loot table {}", lootTableId);
                    return generatedLoot;
                }
            }
            com.bmfalkye.util.ModLogger.debug("CardLootModifier: All conditions passed");
        } else {
            com.bmfalkye.util.ModLogger.debug("CardLootModifier: No conditions to check");
        }
        
        // Проверяем, что это контейнер (если нет условий, проверяем по ID)
        if (lootTableId != null) {
            String lootTablePath = lootTableId.toString().toLowerCase();
            // Проверяем, что это лут-таблица контейнера
            boolean isContainer = lootTablePath.contains("chests/") || 
                                 lootTablePath.contains("barrels/") || 
                                 lootTablePath.contains("shulker_boxes/") ||
                                 lootTablePath.contains("chest") || // На случай других модов
                                 lootTablePath.contains("barrel") ||
                                 lootTablePath.contains("container");
            
            if (!isContainer) {
                com.bmfalkye.util.ModLogger.debug("CardLootModifier: Not a container loot table: {}", lootTableId);
                return generatedLoot;
            }
        } else {
            com.bmfalkye.util.ModLogger.debug("CardLootModifier: No loot table ID");
            return generatedLoot;
        }
        
        com.bmfalkye.util.ModLogger.logMinecraftInteraction("CardLootModifier: Processing loot table {}", lootTableId);
        
        // Определяем редкость карты на основе шансов
        CardRarity selectedRarity = selectRarity(context.getRandom());
        
        if (selectedRarity != null) {
            // Получаем список карт с выбранной редкостью
            List<Card> cardsOfRarity = CardRegistry.getCardsByRarity(selectedRarity);
            
            if (!cardsOfRarity.isEmpty()) {
                // Выбираем случайную карту
                Card selectedCard = cardsOfRarity.get(context.getRandom().nextInt(cardsOfRarity.size()));
                
                // Создаем ItemStack для карты
                ItemStack cardStack = CardItem.createCardStack(selectedCard.getId());
                if (cardStack != null && !cardStack.isEmpty()) {
                    generatedLoot.add(cardStack);
                    com.bmfalkye.util.ModLogger.logMinecraftInteraction("Added card {} (rarity: {}) to loot table {}", 
                        selectedCard.getId(), selectedRarity, lootTableId);
                } else {
                    com.bmfalkye.util.ModLogger.warn("Failed to create card stack for card {}", selectedCard.getId());
                }
            } else {
                // Если нет карт с выбранной редкостью, используем COMMON как fallback
                List<Card> commonCards = CardRegistry.getCardsByRarity(CardRarity.COMMON);
                if (!commonCards.isEmpty()) {
                    Card selectedCard = commonCards.get(context.getRandom().nextInt(commonCards.size()));
                    ItemStack cardStack = CardItem.createCardStack(selectedCard.getId());
                    if (cardStack != null && !cardStack.isEmpty()) {
                        generatedLoot.add(cardStack);
                        com.bmfalkye.util.ModLogger.logMinecraftInteraction("Added card {} (COMMON fallback) to loot table {}", 
                            selectedCard.getId(), lootTableId);
                    }
                } else {
                    com.bmfalkye.util.ModLogger.warn("No cards found for rarity {} in loot table {}. Total cards: {}", 
                        selectedRarity, lootTableId, CardRegistry.getTotalCardCount());
                }
            }
        } else {
            com.bmfalkye.util.ModLogger.debug("No card selected for loot table {} (chances: common={}, rare={}, epic={}, legendary={})", 
                lootTableId, commonChance, rareChance, epicChance, legendaryChance);
        }
        
        return generatedLoot;
    }
    
    /**
     * Выбирает редкость карты на основе шансов
     * Шансы независимые: каждый шанс проверяется отдельно
     * Приоритет: легендарные > эпические > редкие > обычные
     * Если выпало несколько редкостей, выбирается самая редкая
     */
    private CardRarity selectRarity(net.minecraft.util.RandomSource random) {
        // Проверяем каждый шанс независимо
        boolean legendaryRoll = random.nextFloat() < legendaryChance;
        boolean epicRoll = random.nextFloat() < epicChance;
        boolean rareRoll = random.nextFloat() < rareChance;
        boolean commonRoll = random.nextFloat() < commonChance;
        
        // Приоритет: самая редкая редкость
        if (legendaryRoll) {
            return CardRarity.LEGENDARY;
        }
        if (epicRoll) {
            return CardRarity.EPIC;
        }
        if (rareRoll) {
            return CardRarity.RARE;
        }
        if (commonRoll) {
            return CardRarity.COMMON;
        }
        
        return null; // Не выпала карта
    }
    
    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}

