package com.bmfalkye.combo;

import com.bmfalkye.cards.Card;
import com.bmfalkye.game.FalkyeGameSession;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Система комбо-эффектов и синергий между картами
 */
public class ComboSystem {
    
    /**
     * Проверяет и применяет комбо-эффекты при разыгрывании карты
     * ВАЖНО: Вызывается ПОСЛЕ того, как карта уже добавлена на поле
     * ВАЖНО: Комбо применяются ТОЛЬКО к картам того игрока, который их собрал
     */
    public static void checkAndApplyCombos(FalkyeGameSession session, ServerPlayer player, Card playedCard) {
        // Получаем все карты на поле ТОЛЬКО этого игрока (включая только что сыгранную)
        // ВАЖНО: getAllFieldCards возвращает только карты указанного игрока
        List<Card> fieldCards = getAllFieldCards(session, player);
        
        // Убеждаемся, что сыгранная карта включена в список (она уже должна быть на поле)
        // Проверяем, есть ли она в списке
        boolean playedCardInField = false;
        for (Card card : fieldCards) {
            if (card.getId().equals(playedCard.getId())) {
                playedCardInField = true;
                break;
            }
        }
        
        // Если карта еще не на поле, добавляем её в список для проверки комбо
        // (на случай, если метод вызван до добавления карты на поле)
        if (!playedCardInField) {
            fieldCards.add(playedCard);
        }
        
        // Флаг для отслеживания, было ли применено комбо
        boolean comboApplied = false;
        
        // Проверяем различные типы комбо
        if (checkFactionCombo(session, player, playedCard, fieldCards)) {
            comboApplied = true;
        }
        if (checkRarityCombo(session, player, playedCard, fieldCards)) {
            comboApplied = true;
        }
        if (checkTypeCombo(session, player, playedCard, fieldCards)) {
            comboApplied = true;
        }
        if (checkPowerCombo(session, player, playedCard, fieldCards)) {
            comboApplied = true;
        }
        
        // Обновляем состояние игры после применения комбо для визуального отображения
        if (comboApplied) {
            updateGameStateAfterCombo(session, player);
        }
    }
    
    /**
     * Обновляет состояние игры после применения комбо
     * Синхронизирует состояние игры с клиентами и пересчитывает очки
     */
    private static void updateGameStateAfterCombo(FalkyeGameSession session, ServerPlayer player) {
        // Пересчитываем очки раунда после применения комбо (на случай, если они изменились)
        session.recalculateRoundScore();
        
        // Обновляем состояние игры для всех игроков немедленно
        // Это критично для синхронизации визуальных эффектов и изменений силы карт
        if (session.getPlayer1() != null) {
            com.bmfalkye.network.NetworkHandler.updateGameStateImmediate(session.getPlayer1(), session);
        }
        if (session.getPlayer2() != null) {
            com.bmfalkye.network.NetworkHandler.updateGameStateImmediate(session.getPlayer2(), session);
        }
    }
    
    /**
     * Комбо одной фракции - если на поле 3+ карты одной фракции, усиливаем все
     * ВАЖНО: Комбо применяется ТОЛЬКО к картам того игрока, который его собрал
     * @return true если комбо было применено
     */
    private static boolean checkFactionCombo(FalkyeGameSession session, ServerPlayer player, 
                                         Card playedCard, List<Card> fieldCards) {
        // Сначала проверяем, была ли уже собрана комбо для фракции сыгранной карты
        String playedCardFaction = playedCard.getFaction();
        String playedCardComboId = "faction_" + playedCardFaction;
        
        // Если комбо для этой фракции уже собрано, не проверяем снова
        if (session.isComboCollected(player, playedCardComboId)) {
            return false;
        }
        
        Map<String, Integer> factionCount = new HashMap<>();
        
        // ВАЖНО: fieldCards уже включает сыгранную карту, так как она уже добавлена на поле
        // Считаем все карты на поле (включая сыгранную)
        for (Card card : fieldCards) {
            factionCount.put(card.getFaction(), 
                factionCount.getOrDefault(card.getFaction(), 0) + 1);
        }
        
        boolean comboApplied = false;
        for (Map.Entry<String, Integer> entry : factionCount.entrySet()) {
            if (entry.getValue() >= 3) {
                String faction = entry.getKey();
                String comboId = "faction_" + faction;
                
                // Проверяем, было ли это комбо уже собрано в этом раунде
                if (session.isComboCollected(player, comboId)) {
                    continue; // Пропускаем, если комбо уже было собрано
                }
                
                // Комбо активировано!
                int boost = entry.getValue() - 2; // +1 за каждую карту сверх 2
                
                // ВАЖНО: Отмечаем комбо как собранное ПЕРЕД применением эффекта,
                // чтобы предотвратить повторное срабатывание
                session.markComboAsCollected(player, comboId);
                
                boostFactionCards(session, player, faction, boost);
                
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§6§lКОМБО ФРАКЦИИ! §f" + faction + " §6усилены на §f" + boost + "!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§6§lКОМБО ФРАКЦИИ! " + faction + " усилены на " + boost);
                }
                
                // Уведомляем оппонента
                ServerPlayer opponent = com.bmfalkye.cards.CardEffects.getOpponent(session, player);
                if (opponent != null) {
                    com.bmfalkye.network.NetworkHandler.addActionLog(opponent, 
                        "§cПротивник активировал комбо фракции " + faction + "!");
                }
                
                comboApplied = true;
            }
        }
        return comboApplied;
    }
    
    /**
     * Комбо редкости - если на поле 2+ легендарных карты, особый эффект
     * ВАЖНО: Комбо применяется ТОЛЬКО к картам того игрока, который его собрал
     * @return true если комбо было применено
     */
    private static boolean checkRarityCombo(FalkyeGameSession session, ServerPlayer player,
                                        Card playedCard, List<Card> fieldCards) {
        Map<com.bmfalkye.cards.CardRarity, Integer> rarityCount = new HashMap<>();
        
        // ВАЖНО: fieldCards уже включает сыгранную карту, так как она уже добавлена на поле
        // Считаем все карты на поле (включая сыгранную)
        for (Card card : fieldCards) {
            rarityCount.put(card.getRarity(), 
                rarityCount.getOrDefault(card.getRarity(), 0) + 1);
        }
        
        boolean comboApplied = false;
        
        // Легендарное комбо
        int legendaryCount = rarityCount.getOrDefault(com.bmfalkye.cards.CardRarity.LEGENDARY, 0);
        if (legendaryCount >= 2) {
            String comboId = "legendary";
            
            // Проверяем, было ли это комбо уже собрано в этом раунде
            if (!session.isComboCollected(player, comboId)) {
                // ВАЖНО: Отмечаем комбо как собранное ПЕРЕД применением эффекта
                session.markComboAsCollected(player, comboId);
                
                // Усиливаем все карты на 5
                boostAllCards(session, player, 5);
                
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§6§lЛЕГЕНДАРНОЕ КОМБО! Все карты усилены на 5!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§6§lЛЕГЕНДАРНОЕ КОМБО! Все карты усилены на 5!");
                }
                
                // Уведомляем оппонента
                ServerPlayer opponent = com.bmfalkye.cards.CardEffects.getOpponent(session, player);
                if (opponent != null) {
                    com.bmfalkye.network.NetworkHandler.addActionLog(opponent, 
                        "§cПротивник активировал легендарное комбо!");
                }
                
                comboApplied = true;
            }
        }
        
        // Эпическое комбо
        int epicCount = rarityCount.getOrDefault(com.bmfalkye.cards.CardRarity.EPIC, 0);
        if (epicCount >= 3) {
            String comboId = "epic";
            
            // Проверяем, было ли это комбо уже собрано в этом раунде
            if (!session.isComboCollected(player, comboId)) {
                // ВАЖНО: Отмечаем комбо как собранное ПЕРЕД применением эффекта
                session.markComboAsCollected(player, comboId);
                
                // Лечим все карты на 3
                healAllCards(session, player, 3);
                
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§5§lЭПИЧЕСКОЕ КОМБО! Все карты исцелены на 3!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§5§lЭПИЧЕСКОЕ КОМБО! Все карты исцелены на 3!");
                }
                
                // Уведомляем оппонента
                ServerPlayer opponent = com.bmfalkye.cards.CardEffects.getOpponent(session, player);
                if (opponent != null) {
                    com.bmfalkye.network.NetworkHandler.addActionLog(opponent, 
                        "§cПротивник активировал эпическое комбо!");
                }
                
                comboApplied = true;
            }
        }
        
        return comboApplied;
    }
    
    /**
     * Комбо типа - если на поле 3+ заклинания, особый эффект
     * ВАЖНО: Комбо применяется ТОЛЬКО к картам того игрока, который его собрал
     * (кроме комбо заклинаний, которое наносит урон противнику)
     * @return true если комбо было применено
     */
    private static boolean checkTypeCombo(FalkyeGameSession session, ServerPlayer player,
                                      Card playedCard, List<Card> fieldCards) {
        int spellCount = 0;
        int creatureCount = 0;
        
        // ВАЖНО: fieldCards уже включает сыгранную карту, так как она уже добавлена на поле
        // Считаем все карты на поле (включая сыгранную)
        for (Card card : fieldCards) {
            switch (card.getType()) {
                case SPELL -> spellCount++;
                case CREATURE -> creatureCount++;
                case SPECIAL -> {} // Специальные карты не учитываются в комбо типа
            }
        }
        
        boolean comboApplied = false;
        
        // Комбо заклинаний
        if (spellCount >= 3) {
            String comboId = "spell";
            
            // Проверяем, было ли это комбо уже собрано в этом раунде
            if (!session.isComboCollected(player, comboId)) {
                // ВАЖНО: Отмечаем комбо как собранное ПЕРЕД применением эффекта
                session.markComboAsCollected(player, comboId);
                
                // Наносим урон противнику
                ServerPlayer opponent = com.bmfalkye.cards.CardEffects.getOpponent(session, player);
                int damage = spellCount;
                damageOpponentCards(session, opponent, damage);
                
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§b§lКОМБО ЗАКЛИНАНИЙ! Нанесено " + damage + " урона противнику!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§b§lКОМБО ЗАКЛИНАНИЙ! Нанесено " + damage + " урона противнику!");
                }
                
                // Уведомляем оппонента
                if (opponent != null) {
                    com.bmfalkye.network.NetworkHandler.addActionLog(opponent, 
                        "§cПротивник активировал комбо заклинаний! Нанесено " + damage + " урона!");
                }
                
                comboApplied = true;
            }
        }
        
        // Комбо существ
        if (creatureCount >= 5) {
            String comboId = "creature";
            
            // Проверяем, было ли это комбо уже собрано в этом раунде
            if (!session.isComboCollected(player, comboId)) {
                // ВАЖНО: Отмечаем комбо как собранное ПЕРЕД применением эффекта
                session.markComboAsCollected(player, comboId);
                
                // Усиливаем всех существ на 2
                boostCreatures(session, player, 2);
                
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§a§lКОМБО СУЩЕСТВ! Все существа усилены на 2!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§a§lКОМБО СУЩЕСТВ! Все существа усилены на 2!");
                }
                
                // Уведомляем оппонента
                ServerPlayer opponent = com.bmfalkye.cards.CardEffects.getOpponent(session, player);
                if (opponent != null) {
                    com.bmfalkye.network.NetworkHandler.addActionLog(opponent, 
                        "§cПротивник активировал комбо существ!");
                }
                
                comboApplied = true;
            }
        }
        
        return comboApplied;
    }
    
    /**
     * Комбо силы - если общая сила на поле превышает порог, особый эффект
     * ВАЖНО: Комбо применяется ТОЛЬКО к картам того игрока, который его собрал
     * @return true если комбо было применено
     */
    private static boolean checkPowerCombo(FalkyeGameSession session, ServerPlayer player,
                                       Card playedCard, List<Card> fieldCards) {
        int totalPower = 0;
        // ВАЖНО: fieldCards уже включает сыгранную карту, так как она уже добавлена на поле
        // Считаем общую силу всех карт на поле (включая сыгранную)
        // ВАЖНО: Используем игрока для правильного расчета эффективной силы
        for (Card card : fieldCards) {
            totalPower += session.getEffectivePower(card, player);
        }
        
        // Мощное комбо
        if (totalPower >= 50) {
            String comboId = "power";
            
            // Проверяем, было ли это комбо уже собрано в этом раунде
            if (!session.isComboCollected(player, comboId)) {
                // ВАЖНО: Отмечаем комбо как собранное ПЕРЕД применением эффекта
                session.markComboAsCollected(player, comboId);
                
                // ВАЖНО: Удваиваем силу всех карт ТОЛЬКО того игрока, который собрал комбо
                // Используем getAllFieldCards для получения только карт этого игрока
                List<Card> allPlayerCards = getAllFieldCards(session, player);
                for (Card card : allPlayerCards) {
                    int currentPower = session.getEffectivePower(card, player);
                    // ВАЖНО: Передаем игрока, чтобы модификатор применялся только к его картам
                    session.addPowerModifier(card, currentPower, player);
                }
                session.recalculateRoundScore();
                
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§c§lМОЩНОЕ КОМБО! Сила всех карт удвоена!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§c§lМОЩНОЕ КОМБО! Сила всех карт удвоена!");
                }
                
                // Уведомляем оппонента
                ServerPlayer opponent = com.bmfalkye.cards.CardEffects.getOpponent(session, player);
                if (opponent != null) {
                    com.bmfalkye.network.NetworkHandler.addActionLog(opponent, 
                        "§cПротивник активировал мощное комбо!");
                }
                
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Вспомогательные методы
     */
    
    /**
     * Получает все карты на поле ТОЛЬКО указанного игрока
     * ВАЖНО: Возвращает только карты этого игрока, не карты противника
     */
    private static List<Card> getAllFieldCards(FalkyeGameSession session, ServerPlayer player) {
        List<Card> cards = new ArrayList<>();
        cards.addAll(session.getMeleeRow(player));
        cards.addAll(session.getRangedRow(player));
        cards.addAll(session.getSiegeRow(player));
        return cards;
    }
    
    /**
     * Усиливает все карты указанной фракции ТОЛЬКО у указанного игрока
     */
    
    /**
     * Усиливает все карты указанной фракции ТОЛЬКО у указанного игрока
     * ВАЖНО: Эффект применяется только к картам этого игрока
     */
    private static void boostFactionCards(FalkyeGameSession session, ServerPlayer player, 
                                         String faction, int boost) {
        List<Card> allCards = getAllFieldCards(session, player);
        for (Card card : allCards) {
            if (card.getFaction().equals(faction)) {
                // ВАЖНО: Передаем игрока, чтобы модификатор применялся только к его картам
                session.addPowerModifier(card, boost, player);
            }
        }
        session.recalculateRoundScore();
    }
    
    /**
     * Усиливает все карты ТОЛЬКО у указанного игрока
     * ВАЖНО: Эффект применяется только к картам этого игрока
     */
    private static void boostAllCards(FalkyeGameSession session, ServerPlayer player, int boost) {
        List<Card> allCards = getAllFieldCards(session, player);
        for (Card card : allCards) {
            // ВАЖНО: Передаем игрока, чтобы модификатор применялся только к его картам
            session.addPowerModifier(card, boost, player);
        }
        session.recalculateRoundScore();
    }
    
    /**
     * Лечит все карты ТОЛЬКО у указанного игрока
     * ВАЖНО: Эффект применяется только к картам этого игрока
     */
    private static void healAllCards(FalkyeGameSession session, ServerPlayer player, int heal) {
        List<Card> allCards = getAllFieldCards(session, player);
        for (Card card : allCards) {
            int currentPower = session.getEffectivePower(card, player);
            int basePower = card.getPower();
            if (currentPower < basePower) {
                int actualHeal = Math.min(heal, basePower - currentPower);
                // ВАЖНО: Передаем игрока, чтобы модификатор применялся только к его картам
                session.addPowerModifier(card, actualHeal, player);
            }
        }
        session.recalculateRoundScore();
    }
    
    /**
     * Наносит урон картам противника (используется в комбо заклинаний)
     * ВАЖНО: Это единственный случай, когда эффект применяется к картам противника
     */
    private static void damageOpponentCards(FalkyeGameSession session, ServerPlayer opponent, int damage) {
        com.bmfalkye.cards.CardEffects.damageAllCardsOnField(session, opponent, damage);
    }
    
    /**
     * Усиливает всех существ ТОЛЬКО у указанного игрока
     * ВАЖНО: Эффект применяется только к картам этого игрока
     */
    private static void boostCreatures(FalkyeGameSession session, ServerPlayer player, int boost) {
        List<Card> allCards = getAllFieldCards(session, player);
        for (Card card : allCards) {
            if (card.getType() == Card.CardType.CREATURE) {
                // ВАЖНО: Передаем игрока, чтобы модификатор применялся только к его картам
                session.addPowerModifier(card, boost, player);
            }
        }
        session.recalculateRoundScore();
    }
}

