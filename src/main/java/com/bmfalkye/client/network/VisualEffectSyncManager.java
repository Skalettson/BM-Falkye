package com.bmfalkye.client.network;

import com.bmfalkye.client.effects.GuiEffectManager;
import com.bmfalkye.game.ClientFalkyeGameSession;
import com.bmfalkye.game.FalkyeGameSession;
import net.minecraft.client.Minecraft;

import java.util.UUID;

/**
 * Менеджер синхронизации визуальных эффектов между клиентом и сервером
 * Обеспечивает корректное отображение эффектов для всех игроков
 */
public class VisualEffectSyncManager {
    
    private static final VisualEffectSyncManager INSTANCE = new VisualEffectSyncManager();
    
    private VisualEffectSyncManager() {}
    
    public static VisualEffectSyncManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Синхронизирует визуальные эффекты при обновлении состояния игры
     */
    public void syncGameStateEffects(ClientFalkyeGameSession oldSession, ClientFalkyeGameSession newSession, 
                                    GuiEffectManager effectManager, int guiX, int guiY, int guiWidth, int guiHeight) {
        if (oldSession == null || newSession == null) {
            return;
        }
        
        UUID currentPlayerUUID = Minecraft.getInstance().player != null ? 
            Minecraft.getInstance().player.getUUID() : null;
        if (currentPlayerUUID == null) {
            return;
        }
        
        // Синхронизация эффектов окончания раунда
        if (!oldSession.isRoundEnded() && newSession.isRoundEnded()) {
            int playerScore = newSession.getRoundScore(null);
            int opponentScore = newSession.getOpponentRoundScore();
            boolean won = playerScore > opponentScore;
            effectManager.playRoundEndEffect(guiX, guiY, guiWidth, guiHeight, won);
        }
        
        // Синхронизация эффектов окончания игры
        if (!oldSession.isGameEnded() && newSession.isGameEnded()) {
            UUID winnerUUID = newSession.getWinnerUUID();
            if (winnerUUID != null) {
                boolean won = winnerUUID.equals(currentPlayerUUID);
                effectManager.playGameEndEffect(guiX, guiY, guiWidth, guiHeight, won);
            }
        }
        
        // Синхронизация эффектов изменения силы карт
        syncPowerChangeEffects(oldSession, newSession, effectManager);
        
        // Синхронизация эффектов комбо
        syncComboEffects(oldSession, newSession, effectManager, guiX, guiY, guiWidth, guiHeight);
    }
    
    /**
     * Синхронизирует эффекты изменения силы карт
     */
    private void syncPowerChangeEffects(ClientFalkyeGameSession oldSession, ClientFalkyeGameSession newSession,
                                       GuiEffectManager effectManager) {
        // Проверяем все ряды на изменения силы
        syncPowerChangesInRow(oldSession.getMeleeRow(null), newSession.getMeleeRow(null), 
                            oldSession, newSession, effectManager);
        syncPowerChangesInRow(oldSession.getRangedRow(null), newSession.getRangedRow(null), 
                            oldSession, newSession, effectManager);
        syncPowerChangesInRow(oldSession.getSiegeRow(null), newSession.getSiegeRow(null), 
                            oldSession, newSession, effectManager);
    }
    
    /**
     * Синхронизирует изменения силы в ряду
     */
    private void syncPowerChangesInRow(java.util.List<com.bmfalkye.cards.Card> oldCards, 
                                       java.util.List<com.bmfalkye.cards.Card> newCards,
                                       ClientFalkyeGameSession oldSession, ClientFalkyeGameSession newSession,
                                       GuiEffectManager effectManager) {
        java.util.Map<String, com.bmfalkye.cards.Card> oldCardsMap = new java.util.HashMap<>();
        for (com.bmfalkye.cards.Card card : oldCards) {
            oldCardsMap.put(card.getId(), card);
        }
        
        for (com.bmfalkye.cards.Card newCard : newCards) {
            com.bmfalkye.cards.Card oldCard = oldCardsMap.get(newCard.getId());
            if (oldCard != null) {
                Integer oldPower = oldSession.getEffectivePower(oldCard);
                int oldPowerValue = oldPower != null ? oldPower : oldCard.getPower();
                
                Integer newPower = newSession.getEffectivePower(newCard);
                int newPowerValue = newPower != null ? newPower : newCard.getPower();
                
                if (oldPowerValue != newPowerValue) {
                    // Создаем эффект изменения силы (позиция будет вычислена в FalkyeGameScreen)
                    // Здесь только отмечаем, что нужно создать эффект
                }
            }
        }
    }
    
    /**
     * Синхронизирует эффекты комбо
     * ВАЖНО: Проверяет комбо для всех карт на поле, чтобы оба игрока видели эффекты
     */
    private void syncComboEffects(ClientFalkyeGameSession oldSession, ClientFalkyeGameSession newSession,
                                  GuiEffectManager effectManager, int guiX, int guiY, int guiWidth, int guiHeight) {
        if (oldSession == null || newSession == null) return;
        
        // Вычисляем общую силу для обнаружения комбо
        // ВАЖНО: Учитываем все карты на поле (игрока и противника) с эффективной силой
        int oldTotalPower = calculateTotalPower(oldSession) + calculateOpponentTotalPower(oldSession);
        int newTotalPower = calculateTotalPower(newSession) + calculateOpponentTotalPower(newSession);
        
        int powerDiff = newTotalPower - oldTotalPower;
        
        // Если изменение силы больше 5, это может быть комбо
        if (powerDiff > 5) {
            // Определяем тип комбо
            // ВАЖНО: Проверяем все карты на поле (игрока и противника)
            java.util.List<com.bmfalkye.cards.Card> allCards = new java.util.ArrayList<>();
            allCards.addAll(newSession.getMeleeRow(null));
            allCards.addAll(newSession.getRangedRow(null));
            allCards.addAll(newSession.getSiegeRow(null));
            allCards.addAll(newSession.getOpponentMeleeRow());
            allCards.addAll(newSession.getOpponentRangedRow());
            allCards.addAll(newSession.getOpponentSiegeRow());
            
            String comboType = "power";
            int comboLevel = Math.min(powerDiff / 5, 5);
            
            // Проверяем комбо фракции
            java.util.Map<String, Integer> factionCount = new java.util.HashMap<>();
            for (com.bmfalkye.cards.Card card : allCards) {
                factionCount.put(card.getFaction(), 
                    factionCount.getOrDefault(card.getFaction(), 0) + 1);
            }
            
            for (java.util.Map.Entry<String, Integer> entry : factionCount.entrySet()) {
                if (entry.getValue() >= 3) {
                    comboType = "faction";
                    comboLevel = entry.getValue();
                    break;
                }
            }
            
            // Проверяем легендарные карты
            long legendaryCount = allCards.stream()
                .filter(c -> c.getRarity() == com.bmfalkye.cards.CardRarity.LEGENDARY)
                .count();
            if (legendaryCount >= 2) {
                comboType = "legendary";
                comboLevel = (int) legendaryCount;
            }
            
            // Создаем эффект комбо (отображается обоим игрокам)
            float centerX = guiX + guiWidth / 2.0f;
            float centerY = guiY + guiHeight / 2.0f;
            effectManager.playComboEffect(centerX, centerY, comboType, comboLevel);
        }
    }
    
    /**
     * Вычисляет общую силу всех карт игрока с учетом модификаторов
     * ВАЖНО: Использует эффективную силу для правильного отображения изменений
     */
    private int calculateTotalPower(ClientFalkyeGameSession session) {
        java.util.List<com.bmfalkye.cards.Card> melee = session.getMeleeRow(null);
        java.util.List<com.bmfalkye.cards.Card> ranged = session.getRangedRow(null);
        java.util.List<com.bmfalkye.cards.Card> siege = session.getSiegeRow(null);
        
        int total = 0;
        for (com.bmfalkye.cards.Card card : melee) {
            Integer power = session.getEffectivePower(card);
            total += power != null ? power : card.getPower();
        }
        for (com.bmfalkye.cards.Card card : ranged) {
            Integer power = session.getEffectivePower(card);
            total += power != null ? power : card.getPower();
        }
        for (com.bmfalkye.cards.Card card : siege) {
            Integer power = session.getEffectivePower(card);
            total += power != null ? power : card.getPower();
        }
        return total;
    }
    
    /**
     * Вычисляет общую силу всех карт противника с учетом модификаторов
     * ВАЖНО: Использует эффективную силу для правильного отображения изменений
     */
    private int calculateOpponentTotalPower(ClientFalkyeGameSession session) {
        java.util.List<com.bmfalkye.cards.Card> melee = session.getOpponentMeleeRow();
        java.util.List<com.bmfalkye.cards.Card> ranged = session.getOpponentRangedRow();
        java.util.List<com.bmfalkye.cards.Card> siege = session.getOpponentSiegeRow();
        
        int total = 0;
        for (com.bmfalkye.cards.Card card : melee) {
            Integer power = session.getEffectivePower(card);
            total += power != null ? power : card.getPower();
        }
        for (com.bmfalkye.cards.Card card : ranged) {
            Integer power = session.getEffectivePower(card);
            total += power != null ? power : card.getPower();
        }
        for (com.bmfalkye.cards.Card card : siege) {
            Integer power = session.getEffectivePower(card);
            total += power != null ? power : card.getPower();
        }
        return total;
    }
    
    /**
     * Синхронизирует эффекты погоды
     */
    public void syncWeatherEffects(FalkyeGameSession.WeatherType oldWeather, FalkyeGameSession.WeatherType newWeather) {
        if (oldWeather != newWeather && newWeather != FalkyeGameSession.WeatherType.NONE) {
            com.bmfalkye.client.sounds.SoundEffectManager.playWeatherSound(newWeather);
        }
    }
}

