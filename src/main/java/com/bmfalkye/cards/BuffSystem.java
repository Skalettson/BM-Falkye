package com.bmfalkye.cards;

import com.bmfalkye.game.FalkyeGameSession;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/**
 * Система управления баффами/дебаффами карт
 */
public class BuffSystem {
    
    /**
     * Применяет бафф к карте в сессии
     */
    public static void applyBuff(FalkyeGameSession session, ServerPlayer targetPlayer, Card card, CardBuff buff) {
        if (session == null || targetPlayer == null || card == null || buff == null) {
            return;
        }
        
        UUID playerId = targetPlayer.getUUID();
        String cardId = card.getId();
        
        // Получаем или создаём карту баффов для игрока
        Map<String, List<CardBuff>> playerBuffs = session.getCardBuffs().computeIfAbsent(playerId, k -> new HashMap<>());
        List<CardBuff> cardBuffs = playerBuffs.computeIfAbsent(cardId, k -> new ArrayList<>());
        
        // Проверяем, нет ли уже такого баффа
        boolean alreadyExists = cardBuffs.stream().anyMatch(b -> b.getId().equals(buff.getId()));
        if (!alreadyExists) {
            cardBuffs.add(buff);
        }
    }
    
    /**
     * Удаляет бафф с карты
     */
    public static void removeBuff(FalkyeGameSession session, ServerPlayer targetPlayer, Card card, String buffId) {
        if (session == null || targetPlayer == null || card == null) {
            return;
        }
        
        UUID playerId = targetPlayer.getUUID();
        String cardId = card.getId();
        
        Map<String, List<CardBuff>> playerBuffs = session.getCardBuffs().get(playerId);
        if (playerBuffs != null) {
            List<CardBuff> cardBuffs = playerBuffs.get(cardId);
            if (cardBuffs != null) {
                cardBuffs.removeIf(b -> b.getId().equals(buffId));
            }
        }
    }
    
    /**
     * Получает все баффы карты
     */
    public static List<CardBuff> getCardBuffs(FalkyeGameSession session, ServerPlayer targetPlayer, Card card) {
        if (session == null || targetPlayer == null || card == null) {
            return Collections.emptyList();
        }
        
        UUID playerId = targetPlayer.getUUID();
        String cardId = card.getId();
        
        Map<String, List<CardBuff>> playerBuffs = session.getCardBuffs().get(playerId);
        if (playerBuffs != null) {
            List<CardBuff> cardBuffs = playerBuffs.get(cardId);
            if (cardBuffs != null) {
                return new ArrayList<>(cardBuffs);
            }
        }
        
        return Collections.emptyList();
    }
    
    /**
     * Вычисляет итоговую силу карты с учётом баффов
     */
    public static int calculateEffectivePower(FalkyeGameSession session, ServerPlayer targetPlayer, Card card) {
        int basePower = card.getPower();
        List<CardBuff> buffs = getCardBuffs(session, targetPlayer, card);
        
        for (CardBuff buff : buffs) {
            if (buff.getType() == CardBuff.BuffType.FROZEN) {
                return 0; // Замороженные карты имеют силу 0
            }
            basePower = buff.applyPowerModifier(basePower);
        }
        
        return Math.max(0, basePower); // Сила не может быть отрицательной
    }
    
    /**
     * Обновляет длительность баффов в конце раунда
     */
    public static void updateBuffsEndOfRound(FalkyeGameSession session) {
        if (session == null) {
            return;
        }
        
        // Обновляем баффы для всех игроков
        for (Map.Entry<UUID, Map<String, List<CardBuff>>> playerEntry : session.getCardBuffs().entrySet()) {
            for (Map.Entry<String, List<CardBuff>> cardEntry : playerEntry.getValue().entrySet()) {
                List<CardBuff> buffs = cardEntry.getValue();
                buffs.forEach(CardBuff::decreaseDuration);
                buffs.removeIf(CardBuff::isExpired);
            }
        }
    }
    
    /**
     * Очищает все баффы игрока
     */
    public static void clearPlayerBuffs(FalkyeGameSession session, ServerPlayer player) {
        if (session == null || player == null) {
            return;
        }
        
        session.getCardBuffs().remove(player.getUUID());
    }
}

