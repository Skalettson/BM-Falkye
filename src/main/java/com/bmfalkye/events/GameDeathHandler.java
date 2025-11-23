package com.bmfalkye.events;

import com.bmfalkye.game.FalkyeGameSession;
import com.bmfalkye.game.GameManager;
import com.bmfalkye.util.ModLogger;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;

/**
 * Обработчик событий смерти для завершения игры при смерти жителя или игрока
 */
@Mod.EventBusSubscriber(modid = com.bmfalkye.BMFalkye.MOD_ID)
public class GameDeathHandler {
    
    /**
     * Обрабатывает смерть сущности
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) {
            return; // Только на сервере
        }
        
        LivingEntity entity = event.getEntity();
        
        // Проверяем, является ли это жителем
        if (entity instanceof Villager villager) {
            handleVillagerDeath(villager);
        }
        
        // Проверяем, является ли это игроком
        if (entity instanceof ServerPlayer player) {
            handlePlayerDeath(player);
        }
    }
    
    /**
     * Обрабатывает смерть жителя
     */
    private static void handleVillagerDeath(Villager villager) {
        Map<UUID, FalkyeGameSession> villagerGames = GameManager.getActiveVillagerGames();
        
        // Ищем активную игру с этим жителем
        for (Map.Entry<UUID, FalkyeGameSession> entry : villagerGames.entrySet()) {
            FalkyeGameSession session = entry.getValue();
            LivingEntity opponent = session.getVillagerOpponent();
            
            if (opponent != null && opponent.getUUID().equals(villager.getUUID())) {
                // Житель умер - игрок выигрывает
                ServerPlayer player = session.getPlayer1();
                if (player != null && !session.isGameEnded()) {
                    ModLogger.logGameLogic("Villager died during game - player wins", 
                        "player", player.getName().getString(), 
                        "villager", villager.getName().getString());
                    
                    // Завершаем игру в пользу игрока
                    session.forceGameEnd(player, true);
                    
                    // Уведомляем игрока
                    player.sendSystemMessage(Component.literal("§aПротивник погиб! Вы выиграли игру!"));
                    
                    // Добавляем в лог действий
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§aПротивник погиб! Вы выиграли игру!");
                }
                break;
            }
        }
    }
    
    /**
     * Обрабатывает смерть игрока
     */
    private static void handlePlayerDeath(ServerPlayer player) {
        Map<UUID, FalkyeGameSession> villagerGames = GameManager.getActiveVillagerGames();
        
        // Проверяем, есть ли активная игра с этим игроком
        FalkyeGameSession session = villagerGames.get(player.getUUID());
        
        if (session != null && !session.isGameEnded()) {
            ModLogger.logGameLogic("Player died during game - opponent wins", 
                "player", player.getName().getString());
            
            // Игрок умер - оппонент (житель) выигрывает
            session.forceGameEnd(player, false);
            
            // Уведомляем игрока
            player.sendSystemMessage(Component.literal("§cВы погибли! Противник выиграл игру."));
            
            // Добавляем в лог действий
            com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                "§cВы погибли! Противник выиграл игру.");
        }
    }
}

