package com.bmfalkye.items;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
 * Предмет-ключ для квестов (дневники, артефакты и т.д.)
 */
public class QuestItem extends Item {
    public QuestItem(Properties properties) {
        super(properties);
    }
    
    /**
     * Получить ID предмета-ключа из ItemStack
     */
    public static String getQuestItemId(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("QuestItemId")) {
            return stack.getTag().getString("QuestItemId");
        }
        return null;
    }
    
    /**
     * Создать ItemStack для предмета-ключа
     */
    public static ItemStack createQuestItemStack(String questItemId, String name) {
        ItemStack stack = new ItemStack(com.bmfalkye.items.ModItems.QUEST_ITEM.get());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("QuestItemId", questItemId);
        tag.putString("QuestItemName", name);
        return stack;
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        String questItemId = getQuestItemId(stack);
        
        if (questItemId != null && player instanceof ServerPlayer serverPlayer) {
            // Проверяем, можно ли начать связанный квест
            com.bmfalkye.quests.Quest quest = com.bmfalkye.quests.QuestRegistry.getQuestByItem(questItemId);
            if (quest != null && quest.canStart(serverPlayer)) {
                quest.startQuest(serverPlayer);
                serverPlayer.sendSystemMessage(Component.literal(
                    "§6Вы начали квест: §f" + quest.getName()));
            } else if (quest != null) {
                // Показываем описание квеста
                serverPlayer.sendSystemMessage(Component.literal(
                    "§7" + quest.getDescription()));
            }
        }
        
        return InteractionResultHolder.success(stack);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, 
                               List<Component> tooltip, TooltipFlag flag) {
        String questItemId = getQuestItemId(stack);
        if (questItemId != null) {
            String name = stack.getTag() != null ? 
                stack.getTag().getString("QuestItemName") : "Предмет квеста";
            tooltip.add(Component.literal("§6" + name));
            tooltip.add(Component.literal("§7Используйте для начала квеста"));
        }
    }
}

