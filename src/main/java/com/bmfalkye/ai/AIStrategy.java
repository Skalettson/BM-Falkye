package com.bmfalkye.ai;

import com.bmfalkye.cards.Card;
import com.bmfalkye.game.FalkyeGameSession;
import com.bmfalkye.game.MatchConfig;

import java.util.List;
import java.util.Random;

/**
 * Система стратегий для AI игрока
 * Обеспечивает разнообразие и непредсказуемость поведения AI
 */
public class AIStrategy {
    private static final Random RANDOM = new Random();
    
    /**
     * Типы стратегий AI
     */
    public enum StrategyType {
        AGGRESSIVE,    // Агрессивная - играет сильные карты, стремится к доминированию
        DEFENSIVE,     // Оборонительная - экономит карты, играет осторожно
        BALANCED,      // Сбалансированная - адаптируется к ситуации
        ADAPTIVE,      // Адаптивная - меняет стратегию в зависимости от противника
        BLUFF,         // Блеф - иногда делает неожиданные ходы
        RUSH           // Раш - играет быстро, стремится закончить раунд быстро
    }
    
    /**
     * Определяет стратегию для AI на основе сложности и ситуации
     */
    public static StrategyType determineStrategy(MatchConfig.Difficulty difficulty, 
                                                FalkyeGameSession session,
                                                int scoreDifference,
                                                int currentRound,
                                                int roundsWonAI,
                                                int roundsWonHuman) {
        // На EASY и NORMAL используем более простые стратегии
        if (difficulty == MatchConfig.Difficulty.EASY) {
            // EASY: случайный выбор между простыми стратегиями
            StrategyType[] easyStrategies = {StrategyType.BALANCED, StrategyType.DEFENSIVE};
            return easyStrategies[RANDOM.nextInt(easyStrategies.length)];
        }
        
        if (difficulty == MatchConfig.Difficulty.NORMAL) {
            // NORMAL: выбор с учётом ситуации
            if (scoreDifference < -10) {
                return RANDOM.nextDouble() < 0.6 ? StrategyType.AGGRESSIVE : StrategyType.BALANCED;
            } else if (scoreDifference > 10) {
                return RANDOM.nextDouble() < 0.7 ? StrategyType.DEFENSIVE : StrategyType.BALANCED;
            }
            return StrategyType.BALANCED;
        }
        
        // HARD и EXPERT используют продвинутые стратегии
        if (difficulty == MatchConfig.Difficulty.HARD) {
            // HARD: стратегический выбор с элементами случайности
            double rand = RANDOM.nextDouble();
            if (scoreDifference < -15) {
                return rand < 0.5 ? StrategyType.AGGRESSIVE : StrategyType.RUSH;
            } else if (scoreDifference > 15) {
                return rand < 0.6 ? StrategyType.DEFENSIVE : StrategyType.BALANCED;
            } else if (currentRound == 3 && roundsWonAI == roundsWonHuman) {
                return rand < 0.4 ? StrategyType.AGGRESSIVE : StrategyType.ADAPTIVE;
            }
            return rand < 0.3 ? StrategyType.BLUFF : StrategyType.ADAPTIVE;
        }
        
        // EXPERT: максимально стратегический выбор
        if (difficulty == MatchConfig.Difficulty.EXPERT) {
            // Анализируем ситуацию более глубоко
            boolean isLosing = roundsWonAI < roundsWonHuman;
            boolean isWinning = roundsWonAI > roundsWonHuman;
            boolean isTied = roundsWonAI == roundsWonHuman;
            
            // Критическая ситуация - нужна агрессия
            if (isLosing && currentRound >= 2 && scoreDifference < -8) {
                return RANDOM.nextDouble() < 0.7 ? StrategyType.AGGRESSIVE : StrategyType.RUSH;
            }
            
            // Впереди - можно играть оборонительно, но с элементами блефа
            if (isWinning && scoreDifference > 10) {
                return RANDOM.nextDouble() < 0.5 ? StrategyType.DEFENSIVE : StrategyType.BLUFF;
            }
            
            // Равная ситуация - адаптивная стратегия с элементами блефа
            if (isTied && currentRound >= 2) {
                double expertRand = RANDOM.nextDouble();
                if (expertRand < 0.3) return StrategyType.BLUFF;
                if (expertRand < 0.6) return StrategyType.ADAPTIVE;
                return StrategyType.AGGRESSIVE;
            }
            
            // По умолчанию - адаптивная стратегия
            return StrategyType.ADAPTIVE;
        }
        
        return StrategyType.BALANCED;
    }
    
    /**
     * Применяет стратегию для выбора карты
     */
    public static Card applyStrategyForCard(StrategyType strategy, List<Card> availableCards,
                                            int scoreDifference, int currentRound,
                                            int aiMeleePower, int aiRangedPower, int aiSiegePower,
                                            int humanMeleePower, int humanRangedPower, int humanSiegePower) {
        if (availableCards.isEmpty()) return null;
        
        switch (strategy) {
            case AGGRESSIVE:
                return selectAggressiveCard(availableCards, scoreDifference, 
                    aiMeleePower, aiRangedPower, aiSiegePower,
                    humanMeleePower, humanRangedPower, humanSiegePower);
                
            case DEFENSIVE:
                return selectDefensiveCard(availableCards, scoreDifference,
                    aiMeleePower, aiRangedPower, aiSiegePower,
                    humanMeleePower, humanRangedPower, humanSiegePower);
                
            case RUSH:
                return selectRushCard(availableCards, scoreDifference);
                
            case BLUFF:
                return selectBluffCard(availableCards, scoreDifference);
                
            case ADAPTIVE:
            case BALANCED:
            default:
                return selectBalancedCard(availableCards, scoreDifference,
                    aiMeleePower, aiRangedPower, aiSiegePower,
                    humanMeleePower, humanRangedPower, humanSiegePower);
        }
    }
    
    /**
     * Агрессивная стратегия: выбирает самые сильные карты
     */
    private static Card selectAggressiveCard(List<Card> cards, int scoreDifference,
                                            int aiMeleePower, int aiRangedPower, int aiSiegePower,
                                            int humanMeleePower, int humanRangedPower, int humanSiegePower) {
        // Сортируем по силе (убывание)
        cards.sort((c1, c2) -> Integer.compare(c2.getPower(), c1.getPower()));
        
        // Выбираем из топ-30% самых сильных карт
        int topCount = Math.max(1, (int)(cards.size() * 0.3));
        return cards.get(RANDOM.nextInt(topCount));
    }
    
    /**
     * Оборонительная стратегия: экономит сильные карты, играет средние
     */
    private static Card selectDefensiveCard(List<Card> cards, int scoreDifference,
                                            int aiMeleePower, int aiRangedPower, int aiSiegePower,
                                            int humanMeleePower, int humanRangedPower, int humanSiegePower) {
        // Сортируем по силе
        cards.sort((c1, c2) -> Integer.compare(c1.getPower(), c2.getPower()));
        
        // Выбираем из средних карт (не самые слабые, не самые сильные)
        int start = cards.size() / 3;
        int end = (cards.size() * 2) / 3;
        if (start >= end) start = 0;
        
        int range = Math.max(1, end - start);
        return cards.get(start + RANDOM.nextInt(range));
    }
    
    /**
     * Раш стратегия: играет быстро, выбирает карты с хорошим соотношением силы
     */
    private static Card selectRushCard(List<Card> cards, int scoreDifference) {
        // Предпочитаем карты с хорошей силой, но не обязательно самые сильные
        cards.sort((c1, c2) -> Integer.compare(c2.getPower(), c1.getPower()));
        
        // Выбираем из топ-50%
        int topCount = Math.max(1, cards.size() / 2);
        return cards.get(RANDOM.nextInt(topCount));
    }
    
    /**
     * Блеф стратегия: делает неожиданные ходы
     */
    private static Card selectBluffCard(List<Card> cards, int scoreDifference) {
        // 30% шанс выбрать слабую карту (блеф)
        if (RANDOM.nextDouble() < 0.3 && cards.size() > 1) {
            cards.sort((c1, c2) -> Integer.compare(c1.getPower(), c2.getPower()));
            int weakCount = Math.max(1, cards.size() / 3);
            return cards.get(RANDOM.nextInt(weakCount));
        }
        
        // 70% шанс выбрать случайную карту (непредсказуемость)
        return cards.get(RANDOM.nextInt(cards.size()));
    }
    
    /**
     * Сбалансированная стратегия: учитывает ситуацию
     */
    private static Card selectBalancedCard(List<Card> cards, int scoreDifference,
                                          int aiMeleePower, int aiRangedPower, int aiSiegePower,
                                          int humanMeleePower, int humanRangedPower, int humanSiegePower) {
        // Если проигрываем - выбираем сильные карты
        if (scoreDifference < -5) {
            cards.sort((c1, c2) -> Integer.compare(c2.getPower(), c1.getPower()));
            int topCount = Math.max(1, (int)(cards.size() * 0.4));
            return cards.get(RANDOM.nextInt(topCount));
        }
        
        // Если впереди - можем экономить
        if (scoreDifference > 10) {
            cards.sort((c1, c2) -> Integer.compare(c1.getPower(), c2.getPower()));
            int midCount = Math.max(1, (int)(cards.size() * 0.6));
            return cards.get(RANDOM.nextInt(midCount));
        }
        
        // Равная ситуация - выбираем из средних-сильных
        cards.sort((c1, c2) -> Integer.compare(c2.getPower(), c1.getPower()));
        int start = 0;
        int end = (int)(cards.size() * 0.6);
        int range = Math.max(1, end - start);
        return cards.get(start + RANDOM.nextInt(range));
    }
    
    /**
     * Применяет стратегию для выбора ряда
     */
    public static FalkyeGameSession.CardRow applyStrategyForRow(StrategyType strategy, Card card,
                                                                int aiMeleeCount, int aiRangedCount, int aiSiegeCount,
                                                                int humanMeleeCount, int humanRangedCount, int humanSiegeCount,
                                                                int aiMeleePower, int aiRangedPower, int aiSiegePower,
                                                                int humanMeleePower, int humanRangedPower, int humanSiegePower,
                                                                boolean weatherAffectsMelee, boolean weatherAffectsRanged, boolean weatherAffectsSiege) {
        switch (strategy) {
            case AGGRESSIVE:
                return selectAggressiveRow(aiMeleePower, aiRangedPower, aiSiegePower,
                    humanMeleePower, humanRangedPower, humanSiegePower,
                    weatherAffectsMelee, weatherAffectsRanged, weatherAffectsSiege);
                
            case DEFENSIVE:
                return selectDefensiveRow(aiMeleeCount, aiRangedCount, aiSiegeCount,
                    humanMeleeCount, humanRangedCount, humanSiegeCount,
                    weatherAffectsMelee, weatherAffectsRanged, weatherAffectsSiege);
                
            case RUSH:
                return selectRushRow(aiMeleeCount, aiRangedCount, aiSiegeCount,
                    aiMeleePower, aiRangedPower, aiSiegePower);
                
            case BLUFF:
                return selectBluffRow(aiMeleeCount, aiRangedCount, aiSiegeCount);
                
            case ADAPTIVE:
            case BALANCED:
            default:
                return selectBalancedRow(aiMeleeCount, aiRangedCount, aiSiegeCount,
                    humanMeleeCount, humanRangedCount, humanSiegeCount,
                    aiMeleePower, aiRangedPower, aiSiegePower,
                    humanMeleePower, humanRangedPower, humanSiegePower,
                    weatherAffectsMelee, weatherAffectsRanged, weatherAffectsSiege);
        }
    }
    
    /**
     * Агрессивный выбор ряда: атакует слабые ряды противника
     */
    private static FalkyeGameSession.CardRow selectAggressiveRow(int aiMeleePower, int aiRangedPower, int aiSiegePower,
                                                                 int humanMeleePower, int humanRangedPower, int humanSiegePower,
                                                                 boolean weatherAffectsMelee, boolean weatherAffectsRanged, boolean weatherAffectsSiege) {
        // Находим ряд противника с наименьшей силой
        int minOpponentPower = Math.min(Math.min(humanMeleePower, humanRangedPower), humanSiegePower);
        
        // Выбираем ряд, где можем доминировать
        if (humanMeleePower == minOpponentPower && !weatherAffectsMelee) {
            return FalkyeGameSession.CardRow.MELEE;
        } else if (humanRangedPower == minOpponentPower && !weatherAffectsRanged) {
            return FalkyeGameSession.CardRow.RANGED;
        } else if (!weatherAffectsSiege) {
            return FalkyeGameSession.CardRow.SIEGE;
        }
        
        // Если все ряды затронуты погодой, выбираем случайно
        return FalkyeGameSession.CardRow.values()[RANDOM.nextInt(3)];
    }
    
    /**
     * Оборонительный выбор ряда: избегает переполнения
     */
    private static FalkyeGameSession.CardRow selectDefensiveRow(int aiMeleeCount, int aiRangedCount, int aiSiegeCount,
                                                                int humanMeleeCount, int humanRangedCount, int humanSiegeCount,
                                                                boolean weatherAffectsMelee, boolean weatherAffectsRanged, boolean weatherAffectsSiege) {
        // Выбираем ряд с наименьшим количеством карт (избегаем переполнения)
        int minCount = Math.min(Math.min(aiMeleeCount, aiRangedCount), aiSiegeCount);
        
        if (aiMeleeCount == minCount && !weatherAffectsMelee) {
            return FalkyeGameSession.CardRow.MELEE;
        } else if (aiRangedCount == minCount && !weatherAffectsRanged) {
            return FalkyeGameSession.CardRow.RANGED;
        } else if (!weatherAffectsSiege) {
            return FalkyeGameSession.CardRow.SIEGE;
        }
        
        return FalkyeGameSession.CardRow.values()[RANDOM.nextInt(3)];
    }
    
    /**
     * Раш выбор ряда: быстро заполняет ряды
     */
    private static FalkyeGameSession.CardRow selectRushRow(int aiMeleeCount, int aiRangedCount, int aiSiegeCount,
                                                           int aiMeleePower, int aiRangedPower, int aiSiegePower) {
        // Выбираем ряд с наибольшей силой (усиливаем доминирование)
        int maxPower = Math.max(Math.max(aiMeleePower, aiRangedPower), aiSiegePower);
        
        if (aiMeleePower == maxPower) {
            return FalkyeGameSession.CardRow.MELEE;
        } else if (aiRangedPower == maxPower) {
            return FalkyeGameSession.CardRow.RANGED;
        } else {
            return FalkyeGameSession.CardRow.SIEGE;
        }
    }
    
    /**
     * Блеф выбор ряда: непредсказуемый
     */
    private static FalkyeGameSession.CardRow selectBluffRow(int aiMeleeCount, int aiRangedCount, int aiSiegeCount) {
        // Случайный выбор с небольшим предпочтением менее заполненных рядов
        double rand = RANDOM.nextDouble();
        if (aiMeleeCount <= aiRangedCount && aiMeleeCount <= aiSiegeCount && rand < 0.4) {
            return FalkyeGameSession.CardRow.MELEE;
        } else if (aiRangedCount <= aiSiegeCount && rand < 0.6) {
            return FalkyeGameSession.CardRow.RANGED;
        } else {
            return FalkyeGameSession.CardRow.SIEGE;
        }
    }
    
    /**
     * Сбалансированный выбор ряда: учитывает все факторы
     */
    private static FalkyeGameSession.CardRow selectBalancedRow(int aiMeleeCount, int aiRangedCount, int aiSiegeCount,
                                                               int humanMeleeCount, int humanRangedCount, int humanSiegeCount,
                                                               int aiMeleePower, int aiRangedPower, int aiSiegePower,
                                                               int humanMeleePower, int humanRangedPower, int humanSiegePower,
                                                               boolean weatherAffectsMelee, boolean weatherAffectsRanged, boolean weatherAffectsSiege) {
        // Оцениваем каждый ряд
        double meleeScore = evaluateRowForBalance(aiMeleeCount, humanMeleeCount, aiMeleePower, humanMeleePower, weatherAffectsMelee);
        double rangedScore = evaluateRowForBalance(aiRangedCount, humanRangedCount, aiRangedPower, humanRangedPower, weatherAffectsRanged);
        double siegeScore = evaluateRowForBalance(aiSiegeCount, humanSiegeCount, aiSiegePower, humanSiegePower, weatherAffectsSiege);
        
        // Выбираем лучший ряд
        if (meleeScore >= rangedScore && meleeScore >= siegeScore) {
            return FalkyeGameSession.CardRow.MELEE;
        } else if (rangedScore >= siegeScore) {
            return FalkyeGameSession.CardRow.RANGED;
        } else {
            return FalkyeGameSession.CardRow.SIEGE;
        }
    }
    
    /**
     * Оценивает ряд для сбалансированной стратегии
     */
    private static double evaluateRowForBalance(int myCount, int opponentCount, int myPower, int opponentPower, boolean weatherAffected) {
        double score = 0;
        
        // Бонус за меньшее количество карт (избегаем переполнения)
        score += (10 - myCount) * 0.5;
        
        // Бонус если проигрываем в этом ряду (нужно усилить)
        if (myPower < opponentPower) {
            score += (opponentPower - myPower) * 0.2;
        }
        
        // Штраф если ряд затронут погодой
        if (weatherAffected) {
            score -= 3;
        }
        
        return score;
    }
    
    /**
     * Определяет, нужно ли играть карту с учётом стратегии
     */
    public static boolean shouldPlayCardWithStrategy(StrategyType strategy, int scoreDifference,
                                                     int currentRound, int roundsWonAI, int roundsWonHuman,
                                                     int handSize) {
        if (handSize == 0) return false;
        
        switch (strategy) {
            case AGGRESSIVE:
            case RUSH:
                // Агрессивные стратегии почти всегда играют
                if (scoreDifference < 0) return true;
                return RANDOM.nextDouble() < 0.9; // 90% шанс даже если впереди
                
            case DEFENSIVE:
                // Оборонительная стратегия экономит карты
                if (scoreDifference < -5) return true;
                if (scoreDifference > 15) return RANDOM.nextDouble() < 0.3; // 30% шанс если сильно впереди
                return RANDOM.nextDouble() < 0.6; // 60% шанс в остальных случаях
                
            case BLUFF:
                // Блеф стратегия непредсказуема
                return RANDOM.nextDouble() < 0.75; // 75% шанс (непредсказуемость)
                
            case ADAPTIVE:
                // Адаптивная стратегия учитывает ситуацию
                if (currentRound >= 2 && roundsWonAI <= roundsWonHuman) return true;
                if (scoreDifference < 0) return true;
                return RANDOM.nextDouble() < 0.7;
                
            case BALANCED:
            default:
                // Сбалансированная стратегия
                if (scoreDifference < 0) return true;
                if (scoreDifference > 10) return RANDOM.nextDouble() < 0.4;
                return RANDOM.nextDouble() < 0.7;
        }
    }
}

