package com.bmfalkye.events;

import com.bmfalkye.BMFalkye;
import com.bmfalkye.game.GameManager;
import com.bmfalkye.game.FalkyeGameSession;
import com.bmfalkye.game.ReconnectManager;
import com.bmfalkye.network.NetworkErrorHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Обработчик отключения игрока
 */
@Mod.EventBusSubscriber(modid = BMFalkye.MOD_ID)
public class PlayerDisconnectHandler {
    
    @SubscribeEvent
    public static void onPlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Очищаем пакеты в очереди повторной отправки для отключившегося игрока
            NetworkErrorHandler.clearPlayerPackets(player);
            
            // Проверяем, есть ли активная игровая сессия
            FalkyeGameSession session = GameManager.getActiveGame(player);
            
            if (session != null && !session.isGameEnded()) {
                // Сохраняем сессию для возможного восстановления
                ReconnectManager.saveSessionForReconnect(player, session);
                
                BMFalkye.LOGGER.info("Player {} disconnected during active game, session saved for reconnect", 
                    player.getName().getString());
            }
            
            // Сбрасываем статистику античита (но сохраняем нарушения)
            com.bmfalkye.anticheat.AntiCheatSystem.resetPlayerStats(player.getUUID());
            
            // Очищаем батчи пакетов для отключившегося игрока
            com.bmfalkye.network.PacketBatcher.clearBatchesForPlayer(player.getUUID());
        }
    }
}

