package com.bmfalkye.items;

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
 * Билет Турнира - позволяет войти в "Великий Турнир" без оплаты монетами
 */
public class TournamentTicketItem extends Item {
    public TournamentTicketItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            // Используем билет для начала драфта
            boolean success = com.bmfalkye.draft.DraftSystem.startDraft(serverPlayer, true);
            
            if (success) {
                // Удаляем билет из инвентаря
                stack.shrink(1);
                player.sendSystemMessage(Component.literal("§6Билет использован! Драфт начат."));
                return InteractionResultHolder.success(stack);
            } else {
                player.sendSystemMessage(Component.literal("§cНе удалось начать драфт. " +
                    "Возможно, у вас уже есть активная сессия."));
                return InteractionResultHolder.fail(stack);
            }
        }
        
        return InteractionResultHolder.pass(stack);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, 
                               List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6Билет Турнира"));
        tooltip.add(Component.literal("§7Позволяет войти в Великий Турнир"));
        tooltip.add(Component.literal("§7без оплаты монетами"));
        tooltip.add(Component.literal("§7"));
        tooltip.add(Component.literal("§eИспользуйте для начала драфта"));
    }
}

