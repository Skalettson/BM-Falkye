package com.bmfalkye.territory;

import com.bmfalkye.game.GameManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Система захвата территорий
 */
public class TerritoryCaptureSystem {
    /**
     * Обрабатывает взаимодействие игрока с точкой силы
     */
    public static void handleTerritoryInteraction(ServerPlayer player, BlockPos position, ServerLevel level) {
        GuildTerritoryManager manager = GuildTerritoryManager.get(level);
        
        // TODO: Получить гильдию игрока
        // Пока используем заглушку
        UUID playerGuildId = getPlayerGuildId(player);
        String playerGuildName = getPlayerGuildName(player);
        
        if (playerGuildId == null) {
            player.sendSystemMessage(Component.literal("§cВы не состоите в гильдии!"));
            return;
        }
        
        // Проверяем, контролирует ли уже эта гильдия точку
        if (manager.isGuildControlling(position, playerGuildId)) {
            player.sendSystemMessage(Component.literal("§aВаша гильдия уже контролирует эту точку!"));
            return;
        }
        
        // Проверяем, есть ли активный процесс захвата
        GuildTerritoryManager.CaptureProcess existingProcess = manager.getCaptureProcess(position);
        if (existingProcess != null) {
            // Если другая гильдия захватывает, можно оспорить
            if (!existingProcess.guildId.equals(playerGuildId)) {
                // Запускаем дуэль для оспаривания
                startTerritoryDuel(player, position, level, existingProcess.guildId);
                return;
            } else {
                // Своя гильдия уже захватывает
                long remaining = existingProcess.getRemainingTime();
                player.sendSystemMessage(Component.literal(
                    "§eЗахват в процессе... Осталось: " + (remaining / 1000) + " секунд"));
                return;
            }
        }
        
        // Начинаем новый процесс захвата
        manager.startCapture(position, playerGuildId, playerGuildName);
        player.sendSystemMessage(Component.literal("§eНачался захват точки силы! Защищайте её 1 минуту."));
        
        // Запускаем таймер завершения захвата
        level.getServer().execute(() -> {
            try {
                Thread.sleep(60000); // 1 минута
                if (manager.getCaptureProcess(position) != null) {
                    manager.completeCapture(position, playerGuildId, playerGuildName);
                    // Уведомляем всех участников гильдии
                    notifyGuildMembers(level, playerGuildId, 
                        "§aГильдия захватила точку силы: " + manager.getTerritoryPoint(position).name);
                }
            } catch (InterruptedException e) {
                // Прервано (возможно, из-за дуэли)
            }
        });
    }
    
    /**
     * Запускает дуэль для оспаривания территории
     */
    private static void startTerritoryDuel(ServerPlayer challenger, BlockPos position, 
                                          ServerLevel level, UUID defendingGuildId) {
        // Находим защитника из другой гильдии
        ServerPlayer defender = findGuildMember(level, defendingGuildId);
        
        if (defender == null) {
            challenger.sendSystemMessage(Component.literal("§cНет защитников территории!"));
            return;
        }
        
        // Отменяем процесс захвата защитника
        GuildTerritoryManager manager = GuildTerritoryManager.get(level);
        manager.cancelCapture(position);
        
        // Запускаем дуэль
        challenger.sendSystemMessage(Component.literal("§6Вы оспариваете территорию! Начинается дуэль..."));
        defender.sendSystemMessage(Component.literal("§cВаша территория оспаривается! Начинается дуэль..."));
        
        // Используем существующую систему вызова на дуэль
        GameManager.sendDuelChallenge(challenger, defender);
    }
    
    /**
     * Получает ID гильдии игрока (заглушка)
     */
    private static UUID getPlayerGuildId(ServerPlayer player) {
        // TODO: Интегрировать с системой гильдий
        return null;
    }
    
    /**
     * Получает имя гильдии игрока (заглушка)
     */
    private static String getPlayerGuildName(ServerPlayer player) {
        // TODO: Интегрировать с системой гильдий
        return "Test Guild";
    }
    
    /**
     * Находит участника гильдии (заглушка)
     */
    private static ServerPlayer findGuildMember(ServerLevel level, UUID guildId) {
        // TODO: Интегрировать с системой гильдий
        return null;
    }
    
    /**
     * Уведомляет всех участников гильдии
     */
    private static void notifyGuildMembers(ServerLevel level, UUID guildId, String message) {
        // TODO: Интегрировать с системой гильдий
    }
}

