package com.bmfalkye.items;

import com.bmfalkye.BMFalkye;
import com.bmfalkye.game.GameManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Mod.EventBusSubscriber(modid = BMFalkye.MOD_ID)
public class DeckOfCardsItem extends Item {
    public DeckOfCardsItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.bm_falkye.deck_of_cards.tooltip"));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST) // Самый высокий приоритет, чтобы перехватить до стандартного взаимодействия
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        // Проверяем только главную руку
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        
        ItemStack heldItem = event.getEntity().getItemInHand(event.getHand());
        if (!(heldItem.getItem() instanceof DeckOfCardsItem)) {
            return;
        }
        
        // Обрабатываем только на сервере
        if (event.getLevel().isClientSide) {
            return;
        }
        
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        BMFalkye.LOGGER.info("DeckOfCardsItem: EntityInteract event triggered for {} (player: {}, hand: {})", 
            event.getTarget().getClass().getSimpleName(), player.getName().getString(), event.getHand());
        
        // Взаимодействие с игроком
        if (event.getTarget() instanceof Player targetPlayer && !targetPlayer.equals(player)) {
            if (targetPlayer instanceof ServerPlayer targetServerPlayer) {
                BMFalkye.LOGGER.info("DeckOfCardsItem: Interacting with player {}", targetServerPlayer.getName().getString());
                // Отправляем вызов на дуэль
                GameManager.sendDuelChallenge(player, targetServerPlayer);
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
        }
        // Взаимодействие с деревенским жителем
        if (event.getTarget() instanceof net.minecraft.world.entity.npc.Villager villager) {
            BMFalkye.LOGGER.info("DeckOfCardsItem: Interacting with villager {}", villager.getName().getString());
            // Проверяем, что это не детский житель
            if (villager.isBaby()) {
                player.sendSystemMessage(Component.translatable("message.bm_falkye.villager_is_baby"));
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);
                return;
            }
            
            // Открываем предматчевое меню для villager
            com.bmfalkye.network.NetworkHandler.openPreMatchScreen(
                player, 
                villager.getUUID(), 
                villager.getName().getString(), 
                true
            );
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}

