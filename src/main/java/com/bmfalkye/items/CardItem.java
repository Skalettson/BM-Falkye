package com.bmfalkye.items;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRegistry;
import com.bmfalkye.cards.CardRarity;
import com.bmfalkye.storage.PlayerCardCollection;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Предмет карты - карта как физический предмет в игре
 */
public class CardItem extends Item {
    public CardItem(Properties properties) {
        super(properties);
    }
    
    /**
     * Получить ID карты из ItemStack
     */
    public static String getCardId(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("CardId")) {
            return stack.getTag().getString("CardId");
        }
        return null;
    }
    
    /**
     * Создать ItemStack для карты
     */
    public static ItemStack createCardStack(String cardId) {
        ItemStack stack = new ItemStack(com.bmfalkye.items.ModItems.CARD_ITEM.get());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("CardId", cardId);
        return stack;
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        String cardId = getCardId(stack);
        
        if (cardId == null) {
            return InteractionResultHolder.fail(stack);
        }
        
        Card card = CardRegistry.getCard(cardId);
        if (card == null) {
            return InteractionResultHolder.fail(stack);
        }
        
        // Начинаем использование (как тотем бессмертия) - это запустит анимацию поднятия предмета
        // Проверка наличия карты в коллекции будет в finishUsingItem
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }
    
    @Override
    public int getUseDuration(ItemStack stack) {
        // Длительность использования (как у тотема бессмертия - 40 тиков = 2 секунды)
        return 40;
    }
    
    @Override
    public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack) {
        // Используем анимацию тотема бессмертия для визуального эффекта
        // TOTEM не существует, используем TOOT_HORN (ближайший аналог)
        return net.minecraft.world.item.UseAnim.TOOT_HORN;
    }
    
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, net.minecraft.world.entity.LivingEntity entity) {
        if (!(entity instanceof ServerPlayer serverPlayer)) {
            return stack;
        }
        
        String cardId = getCardId(stack);
        if (cardId == null) {
            return stack;
        }
        
        Card card = CardRegistry.getCard(cardId);
        if (card == null) {
            return stack;
        }
        
        // Проверяем, есть ли уже эта карта в коллекции
        PlayerCardCollection collection = PlayerCardCollection.get((ServerLevel) level);
        if (collection.hasCard(serverPlayer, cardId)) {
            serverPlayer.sendSystemMessage(Component.translatable("message.bm_falkye.card_already_in_collection", card.getName()));
            return stack;
        }
        
        // Добавляем карту в коллекцию
        collection.addCard(serverPlayer, cardId);
        
        // Проигрываем анимацию и звук
        playCardAnimation(level, serverPlayer, card.getRarity());
        playCardSound(level, serverPlayer, card.getRarity());
        
        // Уменьшаем стак (или удаляем, если это последний)
        stack.shrink(1);
        
        serverPlayer.sendSystemMessage(Component.translatable("message.bm_falkye.card_added_to_collection", card.getName()));
        
        // Отправляем обновлённую коллекцию на клиент
        java.util.List<com.bmfalkye.cards.Card> cards = collection.getCards(serverPlayer);
        java.util.List<String> cardIds = cards.stream()
            .map(com.bmfalkye.cards.Card::getId)
            .collect(java.util.stream.Collectors.toList());
        com.bmfalkye.network.NetworkHandler.INSTANCE.sendTo(
            new com.bmfalkye.network.NetworkHandler.SendCardCollectionPacket(cardIds),
            serverPlayer.connection.connection,
            net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
        );
        
        return stack;
    }
    
    /**
     * Проигрывает анимацию использования карты (как тотем бессмертия)
     */
    private void playCardAnimation(Level level, ServerPlayer player, CardRarity rarity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        
        // Партиклы тотема бессмертия
        for (int i = 0; i < 20; i++) {
            double angle = (i / 20.0) * Math.PI * 2.0;
            double radius = 1.0 + (rarity.ordinal() * 0.2); // Больше радиус для редких карт
            double x = player.getX() + Math.cos(angle) * radius;
            double y = player.getY() + player.getEyeHeight();
            double z = player.getZ() + Math.sin(angle) * radius;
            
            serverLevel.sendParticles(
                ParticleTypes.TOTEM_OF_UNDYING,
                x, y, z,
                1,
                0.0, 0.1, 0.0,
                0.05
            );
        }
        
        // Дополнительные партиклы в зависимости от редкости
        int extraParticles = rarity.ordinal() * 10;
        for (int i = 0; i < extraParticles; i++) {
            double x = player.getX() + (Math.random() - 0.5) * 2.0;
            double y = player.getY() + player.getEyeHeight() + (Math.random() - 0.5) * 1.0;
            double z = player.getZ() + (Math.random() - 0.5) * 2.0;
            
            serverLevel.sendParticles(
                ParticleTypes.ENCHANT,
                x, y, z,
                1,
                0.0, 0.1, 0.0,
                0.05
            );
        }
    }
    
    /**
     * Проигрывает звук в зависимости от редкости карты
     */
    private void playCardSound(Level level, ServerPlayer player, CardRarity rarity) {
        SoundEvent sound = getSoundForRarity(rarity);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), 
            sound, player.getSoundSource(), 1.0f, 1.0f);
    }
    
    /**
     * Получить звук для редкости карты
     */
    private SoundEvent getSoundForRarity(CardRarity rarity) {
        return switch (rarity) {
            case COMMON -> SoundEvents.ITEM_PICKUP; // Обычный звук подбора
            case RARE -> SoundEvents.AMETHYST_BLOCK_CHIME; // Звук аметиста
            case EPIC -> SoundEvents.BEACON_ACTIVATE; // Звук активации маяка
            case LEGENDARY -> SoundEvents.TOTEM_USE; // Звук тотема бессмертия
        };
    }
    
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String cardId = getCardId(stack);
        if (cardId != null) {
            Card card = CardRegistry.getCard(cardId);
            if (card != null) {
                tooltip.add(Component.translatable("item.bm_falkye.card.tooltip.name", card.getName()));
                tooltip.add(Component.translatable("item.bm_falkye.card.tooltip.rarity", card.getRarity().getDisplayName()));
                tooltip.add(Component.translatable("item.bm_falkye.card.tooltip.power", card.getPower()));
                tooltip.add(Component.translatable("item.bm_falkye.card.tooltip.description", card.getDescription()));
            }
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
    
    @Override
    public Component getName(ItemStack stack) {
        String cardId = getCardId(stack);
        if (cardId != null) {
            Card card = CardRegistry.getCard(cardId);
            if (card != null) {
                return Component.translatable("item.bm_falkye.card", card.getName());
            }
        }
        return super.getName(stack);
    }
    
    /**
     * Максимальный размер стака - всегда 1 (карты не стакаются)
     */
    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }
}

