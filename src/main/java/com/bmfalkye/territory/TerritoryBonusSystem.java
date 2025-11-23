package com.bmfalkye.territory;

import com.bmfalkye.storage.PlayerCurrency;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Система бонусов за контроль территорий
 */
public class TerritoryBonusSystem {
    private static final double REWARD_BONUS = 0.1; // +10% к награде
    private static final double EXTRA_COIN_CHANCE = 0.15; // 15% шанс дополнительной монеты
    
    /**
     * Применяет бонусы за контроль территорий после победы
     */
    public static void applyTerritoryBonuses(ServerPlayer player, int baseReward, ServerLevel level) {
        UUID playerGuildId = getPlayerGuildId(player);
        if (playerGuildId == null) {
            return;
        }
        
        GuildTerritoryManager manager = GuildTerritoryManager.get(level);
        int controlledTerritories = countControlledTerritories(manager, playerGuildId);
        
        if (controlledTerritories > 0) {
            // Бонус к награде
            int bonusReward = (int)(baseReward * REWARD_BONUS * controlledTerritories);
            if (bonusReward > 0) {
                PlayerCurrency currency = PlayerCurrency.get(level);
                currency.addCoins(player, bonusReward);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§aБонус за контроль территорий: +" + bonusReward + " монет"));
            }
            
            // Шанс дополнительной монеты
            if (Math.random() < EXTRA_COIN_CHANCE * controlledTerritories) {
                PlayerCurrency currency = PlayerCurrency.get(level);
                currency.addCoins(player, 1);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§eДополнительная монета за контроль территорий!"));
            }
        }
    }
    
    /**
     * Подсчитывает количество контролируемых территорий гильдией
     */
    private static int countControlledTerritories(GuildTerritoryManager manager, UUID guildId) {
        // TODO: Реализовать подсчёт
        return 0;
    }
    
    /**
     * Получает ID гильдии игрока (заглушка)
     */
    private static UUID getPlayerGuildId(ServerPlayer player) {
        // TODO: Интегрировать с системой гильдий
        return null;
    }
}

