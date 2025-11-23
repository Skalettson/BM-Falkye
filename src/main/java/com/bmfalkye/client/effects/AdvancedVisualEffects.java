package com.bmfalkye.client.effects;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRarity;
import com.bmfalkye.integration.GeckoLibIntegration;
import com.bmfalkye.integration.LibraryIntegration;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.Random;

/**
 * Расширенная система визуальных эффектов с использованием GeckoLib, CreativeCore и Citadel
 * Создает потрясающие эффекты для всех игровых действий
 */
public class AdvancedVisualEffects {
    
    private static final Random RANDOM = new Random();
    
    /**
     * Создает эффект раздачи карт с использованием GeckoLib анимаций
     */
    public static void createCardDealEffect(GuiEffectManager effectManager, float x, float y, 
                                           Card card, int cardIndex, int totalCards) {
        if (LibraryIntegration.isGeckoLibLoaded()) {
            try {
                // Создаем GeckoLib анимацию для раздачи
                Object geckoAnim = GeckoLibIntegration.createCardAppearAnimation();
                if (geckoAnim != null) {
                    // Используем GeckoLib для сложной 3D анимации
                    createGeckoLibDealAnimation(x, y, card, cardIndex, totalCards, geckoAnim);
                }
            } catch (Exception e) {
                // Fallback на обычную анимацию
            }
        }
        
        // ОПТИМИЗИРОВАНО: Меньше частиц раздачи
        if (effectManager.particles.size() >= 150) {
            return; // Не создаём новые, если лимит достигнут
        }
        
        // Создаем частицы раздачи (уменьшено)
        int particleCount = Math.min(10 + card.getRarity().ordinal() * 5, 20); // Было 15+10, стало 10+5, макс 20
        int rarityColor = card.getRarity().getColor();
        
        for (int i = 0; i < particleCount; i++) {
            if (effectManager.particles.size() >= 150) break; // Проверяем лимит в цикле
            
            double angle = (i / (double) particleCount) * Math.PI * 2;
            double radius = 0.3 + (cardIndex * 0.1);
            float vx = (float) (Math.cos(angle) * radius * 0.15f);
            float vy = (float) (Math.sin(angle) * radius * 0.15f - 0.1f); // Вверх
            
            effectManager.particles.add(effectManager.getParticleFromPool(
                x, y, rarityColor, 1.5f + card.getRarity().ordinal() * 0.3f,
                25 + card.getRarity().ordinal() * 5, vx, vy
            ));
        }
        
        // Звук раздачи карты
        float volume = 0.3f + (cardIndex * 0.05f);
        float pitch = 0.9f + (cardIndex * 0.1f);
        effectManager.sounds.add(new GuiEffectManager.GuiSound(
            SoundEvents.ITEM_PICKUP, volume, pitch, cardIndex * 2
        ));
    }
    
    /**
     * Создает эффект игры карты с продвинутыми визуальными эффектами
     */
    public static void createCardPlayEffect(GuiEffectManager effectManager, float x, float y,
                                          Card card, com.bmfalkye.game.FalkyeGameSession.CardRow row) {
        // ОПТИМИЗИРОВАНО: Меньше частиц для лучшей производительности
        // Проверяем лимит частиц
        if (effectManager.particles.size() >= 150) {
            return; // Не создаём новые, если лимит достигнут
        }
        
        // Основные частицы в зависимости от редкости (уменьшено)
        int baseParticleCount = Math.min(20 + (card.getRarity().ordinal() * 10), 40); // Было 30+20, стало 20+10, макс 40
        int rarityColor = card.getRarity().getColor();
        
        // Взрыв частиц при игре карты
        for (int i = 0; i < baseParticleCount; i++) {
            if (effectManager.particles.size() >= 150) break; // Проверяем лимит в цикле
            
            double angle = (i / (double) baseParticleCount) * Math.PI * 2;
            double radius = 0.5 + (card.getRarity().ordinal() * 0.4);
            float vx = (float) (Math.cos(angle) * radius * 0.2f);
            float vy = (float) (Math.sin(angle) * radius * 0.2f);
            
            effectManager.particles.add(effectManager.getParticleFromPool(
                x, y, rarityColor, 2.0f + card.getRarity().ordinal() * 0.5f,
                35 + card.getRarity().ordinal() * 10, vx, vy
            ));
        }
        
        // Эффекты в зависимости от типа карты
        switch (card.getType()) {
            case CREATURE:
                createCreaturePlayEffect(effectManager, x, y, card);
                break;
            case SPELL:
                createSpellPlayEffect(effectManager, x, y, card);
                break;
            case SPECIAL:
                createSpecialPlayEffect(effectManager, x, y, card);
                break;
        }
        
        // Дополнительные эффекты для легендарных карт
        if (card.getRarity() == CardRarity.LEGENDARY) {
            createLegendaryCardEffect(effectManager, x, y, card);
        }
        
        // Звук в зависимости от редкости
        float volume = 0.6f + (card.getRarity().ordinal() * 0.15f);
        float pitch = 0.8f + (card.getRarity().ordinal() * 0.1f);
        
        SoundEvent sound = switch (card.getRarity()) {
            case COMMON -> SoundEvents.ITEM_PICKUP;
            case RARE -> SoundEvents.AMETHYST_BLOCK_CHIME;
            case EPIC -> SoundEvents.BEACON_ACTIVATE;
            case LEGENDARY -> SoundEvents.TOTEM_USE;
        };
        
        effectManager.sounds.add(new GuiEffectManager.GuiSound(sound, volume, pitch, 0));
    }
    
    /**
     * Создает эффект сброса карты на поле с анимацией и частицами
     */
    public static void createCardDropEffect(GuiEffectManager effectManager, float x, float y,
                                          Card card, com.bmfalkye.game.FalkyeGameSession.CardRow row) {
        // Эффект сброса с частицами, соответствующими типу и фракции
        createCardTypeAndFactionParticles(effectManager, x, y, card);
        
        // Дополнительные эффекты для карт со способностями
        if (card.getType() == Card.CardType.SPECIAL) {
            createSpecialCardDropEffect(effectManager, x, y, card);
        }
    }
    
    /**
     * Создает эффект использования карты способности (движение в центр и исчезновение)
     */
    public static void createAbilityCardEffect(GuiEffectManager effectManager, float x, float y,
                                             Card card) {
        // Магические спирали для карт способности - УВЕЛИЧЕНО для лучшей видимости
        for (int wave = 0; wave < 7; wave++) { // Увеличено количество волн
            for (int i = 0; i < 35; i++) { // Увеличено количество частиц
                double angle = (i / 35.0) * Math.PI * 2 + (wave * Math.PI / 2.5);
                float radius = 20.0f + (wave * 25.0f); // Увеличен радиус
                float px = x + (float) (Math.cos(angle) * radius);
                float py = y + (float) (Math.sin(angle) * radius);
                
                float vx = (float) (Math.cos(angle + Math.PI / 2) * 0.25f); // Увеличена скорость
                float vy = (float) (Math.sin(angle + Math.PI / 2) * 0.25f);
                
                // Цвет в зависимости от фракции
                int color = getFactionColor(card.getFaction());
                effectManager.particles.add(effectManager.getParticleFromPool(
                    px, py, color, 3.5f, 50, vx, vy // Увеличен размер и длительность
                ));
            }
        }
        
        // Взрыв частиц при исчезновении - УВЕЛИЧЕНО для лучшей видимости
        int particleCount = 80 + (card.getRarity().ordinal() * 25); // Увеличено
        int rarityColor = card.getRarity().getColor();
        
        for (int i = 0; i < particleCount; i++) {
            double angle = (i / (double) particleCount) * Math.PI * 2;
            float vx = (float) (Math.cos(angle) * 0.5f); // Увеличена скорость
            float vy = (float) (Math.sin(angle) * 0.5f);
            
            effectManager.particles.add(effectManager.getParticleFromPool(
                x, y, rarityColor, 4.0f + card.getRarity().ordinal() * 0.6f, // Увеличен размер
                50 + card.getRarity().ordinal() * 10, vx, vy // Увеличена длительность
            ));
        }
    }
    
    /**
     * Создает частицы на основе типа и фракции карты
     */
    private static void createCardTypeAndFactionParticles(GuiEffectManager effectManager, 
                                                          float x, float y, Card card) {
        String faction = card.getFaction();
        int particleCount = 50 + (card.getRarity().ordinal() * 20); // УВЕЛИЧЕНО в 2 раза
        
        // Частицы в зависимости от фракции
        if (faction.contains("Пламени") || faction.contains("Fire") || faction.equals("Дом Пламени")) {
            // Огненные частицы
            for (int i = 0; i < particleCount; i++) {
                float vx = (float) ((RANDOM.nextDouble() - 0.5) * 0.6f); // Увеличена скорость
                float vy = (float) ((RANDOM.nextDouble() - 0.5) * 0.6f - 0.2f); // Вверх
                effectManager.particles.add(effectManager.getParticleFromPool(
                    x, y, 0xFFFF4400, 4.0f, 40, vx, vy // Увеличен размер и длительность
                ));
            }
            // Дополнительные искры
            for (int i = 0; i < 30; i++) { // Увеличено количество
                double angle = (i / 30.0) * Math.PI * 2;
                float vx = (float) (Math.cos(angle) * 0.4f); // Увеличена скорость
                float vy = (float) (Math.sin(angle) * 0.4f - 0.15f);
                effectManager.particles.add(effectManager.getParticleFromPool(
                    x, y, 0xFFFFAA00, 3.0f, 35, vx, vy // Увеличен размер
                ));
            }
        } else if (faction.contains("Руин") || faction.contains("Watchers") || faction.equals("Дозорные Руин")) {
            // Магические частицы (фиолетовые/синие)
            for (int i = 0; i < particleCount; i++) {
                float vx = (float) ((RANDOM.nextDouble() - 0.5) * 0.35f);
                float vy = (float) ((RANDOM.nextDouble() - 0.5) * 0.35f);
                effectManager.particles.add(effectManager.getParticleFromPool(
                    x, y, 0xFFAA44FF, 2.2f, 35, vx, vy
                ));
            }
            // Магические кольца
            for (int wave = 0; wave < 3; wave++) {
                for (int i = 0; i < 20; i++) {
                    double angle = (i / 20.0) * Math.PI * 2;
                    float radius = wave * 25.0f;
                    float px = x + (float) (Math.cos(angle) * radius);
                    float py = y + (float) (Math.sin(angle) * radius);
                    float vx = (float) (Math.cos(angle) * 0.12f);
                    float vy = (float) (Math.sin(angle) * 0.12f);
                    effectManager.particles.add(effectManager.getParticleFromPool(
                        px, py, 0xFF8844FF, 1.8f, 30, vx, vy
                    ));
                }
            }
        } else if (faction.contains("Рощения") || faction.contains("Nature") || faction.equals("Дети Рощения")) {
            // Природные частицы (зеленые)
            for (int i = 0; i < particleCount; i++) {
                float vx = (float) ((RANDOM.nextDouble() - 0.5) * 0.3f);
                float vy = (float) ((RANDOM.nextDouble() - 0.5) * 0.3f);
                effectManager.particles.add(effectManager.getParticleFromPool(
                    x, y, 0xFF44FF44, 2.0f, 28, vx, vy
                ));
            }
            // Листья/лепестки
            for (int i = 0; i < 20; i++) {
                double angle = (i / 20.0) * Math.PI * 2;
                float vx = (float) (Math.cos(angle) * 0.2f);
                float vy = (float) (Math.sin(angle) * 0.2f + 0.05f); // Падают вниз
                effectManager.particles.add(effectManager.getParticleFromPool(
                    x, y, 0xFF66FF66, 1.5f, 32, vx, vy
                ));
            }
        } else {
            // Нейтральные частицы (белые/серые)
            for (int i = 0; i < particleCount; i++) {
                float vx = (float) ((RANDOM.nextDouble() - 0.5) * 0.3f);
                float vy = (float) ((RANDOM.nextDouble() - 0.5) * 0.3f);
                effectManager.particles.add(effectManager.getParticleFromPool(
                    x, y, 0xFFFFFFFF, 2.0f, 30, vx, vy
                ));
            }
        }
    }
    
    /**
     * Создает эффект сброса для специальных карт
     */
    private static void createSpecialCardDropEffect(GuiEffectManager effectManager, float x, float y, Card card) {
        // Дополнительные эффекты для специальных карт
        for (int i = 0; i < 30; i++) {
            double angle = (i / 30.0) * Math.PI * 2;
            float vx = (float) (Math.cos(angle) * 0.3f);
            float vy = (float) (Math.sin(angle) * 0.3f);
            
            // Чередуем цвета
            int color = (i % 2 == 0) ? 0xFFFFAA00 : 0xFFFF00AA;
            effectManager.particles.add(effectManager.getParticleFromPool(
                x, y, color, 2.8f, 40, vx, vy
            ));
        }
    }
    
    /**
     * Получает цвет фракции для эффектов
     */
    private static int getFactionColor(String faction) {
        if (faction.contains("Пламени") || faction.contains("Fire") || faction.equals("Дом Пламени")) {
            return 0xFFFF4400;
        } else if (faction.contains("Руин") || faction.contains("Watchers") || faction.equals("Дозорные Руин")) {
            return 0xFFAA44FF;
        } else if (faction.contains("Рощения") || faction.contains("Nature") || faction.equals("Дети Рощения")) {
            return 0xFF44FF44;
        }
        return 0xFFFFFFFF;
    }
    
    /**
     * Создает эффект для карт существ
     */
    private static void createCreaturePlayEffect(GuiEffectManager effectManager, float x, float y, Card card) {
        String faction = card.getFaction();
        
        // Эффекты в зависимости от фракции
        if (faction.contains("Огонь") || faction.contains("Fire")) {
            // Огненные частицы
            for (int i = 0; i < 20; i++) {
                float vx = (float) ((RANDOM.nextDouble() - 0.5) * 0.3f);
                float vy = (float) ((RANDOM.nextDouble() - 0.5) * 0.3f - 0.1f); // Вверх
                effectManager.particles.add(effectManager.getParticleFromPool(
                    x, y, 0xFFFF4400, 2.0f, 25, vx, vy
                ));
            }
        } else if (faction.contains("Лёд") || faction.contains("Frost")) {
            // Ледяные частицы
            for (int i = 0; i < 20; i++) {
                float vx = (float) ((RANDOM.nextDouble() - 0.5) * 0.3f);
                float vy = (float) ((RANDOM.nextDouble() - 0.5) * 0.3f);
                effectManager.particles.add(effectManager.getParticleFromPool(
                    x, y, 0xFF44AAFF, 1.8f, 30, vx, vy
                ));
            }
        } else if (faction.contains("Природа") || faction.contains("Nature")) {
            // Природные частицы
            for (int i = 0; i < 20; i++) {
                float vx = (float) ((RANDOM.nextDouble() - 0.5) * 0.3f);
                float vy = (float) ((RANDOM.nextDouble() - 0.5) * 0.3f);
                effectManager.particles.add(effectManager.getParticleFromPool(
                    x, y, 0xFF44FF44, 1.8f, 25, vx, vy
                ));
            }
        }
    }
    
    /**
     * Создает эффект для заклинаний
     */
    private static void createSpellPlayEffect(GuiEffectManager effectManager, float x, float y, Card card) {
        // Магические спирали
        for (int wave = 0; wave < 3; wave++) {
            for (int i = 0; i < 20; i++) {
                double angle = (i / 20.0) * Math.PI * 2 + (wave * Math.PI / 3);
                float radius = 20.0f + (wave * 15.0f);
                float px = x + (float) (Math.cos(angle) * radius);
                float py = y + (float) (Math.sin(angle) * radius);
                
                float vx = (float) (Math.cos(angle + Math.PI / 2) * 0.1f);
                float vy = (float) (Math.sin(angle + Math.PI / 2) * 0.1f);
                
                effectManager.particles.add(effectManager.getParticleFromPool(
                    px, py, 0xFFAA44FF, 1.5f, 30, vx, vy
                ));
            }
        }
    }
    
    /**
     * Создает эффект для особых карт
     */
    private static void createSpecialPlayEffect(GuiEffectManager effectManager, float x, float y, Card card) {
        // Комбинированный эффект
        for (int i = 0; i < 30; i++) {
            double angle = (i / 30.0) * Math.PI * 2;
            float vx = (float) (Math.cos(angle) * 0.25f);
            float vy = (float) (Math.sin(angle) * 0.25f);
            
            // Чередуем цвета
            int color = (i % 2 == 0) ? 0xFFFFAA00 : 0xFFFF00AA;
            effectManager.particles.add(effectManager.getParticleFromPool(
                x, y, color, 2.5f, 35, vx, vy
            ));
        }
    }
    
    /**
     * Создает эффект для легендарных карт
     */
    private static void createLegendaryCardEffect(GuiEffectManager effectManager, float x, float y, Card card) {
        // Золотые искры
        for (int i = 0; i < 25; i++) {
            float vx = (float) ((RANDOM.nextDouble() - 0.5) * 0.4f);
            float vy = (float) ((RANDOM.nextDouble() - 0.5) * 0.4f);
            effectManager.particles.add(effectManager.getParticleFromPool(
                x, y, 0xFFFFD700, 3.0f, 40, vx, vy
            ));
        }
        
        // Волновой эффект
        for (int wave = 0; wave < 5; wave++) {
            for (int i = 0; i < 15; i++) {
                double angle = (i / 15.0) * Math.PI * 2;
                float radius = wave * 25.0f;
                float px = x + (float) (Math.cos(angle) * radius);
                float py = y + (float) (Math.sin(angle) * radius);
                
                float vx = (float) (Math.cos(angle) * 0.15f);
                float vy = (float) (Math.sin(angle) * 0.15f);
                
                effectManager.particles.add(effectManager.getParticleFromPool(
                    px, py, 0xFFFFD700, 2.0f, 30, vx, vy
                ));
            }
        }
    }
    
    /**
     * Создает эффект комбо
     */
    public static void createComboEffect(GuiEffectManager effectManager, float centerX, float centerY,
                                        String comboType, int comboLevel) {
        // ОПТИМИЗИРОВАНО: Меньше частиц
        if (effectManager.particles.size() >= 150) {
            return; // Не создаём новые, если лимит достигнут
        }
        
        int color = switch (comboType) {
            case "faction" -> 0xFF00FFFF; // Голубой
            case "legendary" -> 0xFFFFD700; // Золотой
            case "epic" -> 0xFFAA00FF; // Фиолетовый
            case "spell" -> 0xFFFF00FF; // Розовый
            case "creature" -> 0xFF00FF00; // Зелёный
            case "power" -> 0xFFFF0000; // Красный
            default -> 0xFFFFFFFF; // Белый
        };
        
        // Уменьшенный взрыв частиц
        int particleCount = Math.min(30 + (comboLevel * 5), 50); // Было 50+10, стало 30+5, макс 50
        for (int i = 0; i < particleCount; i++) {
            if (effectManager.particles.size() >= 150) break; // Проверяем лимит в цикле
            
            double angle = (i / (double) particleCount) * Math.PI * 2;
            double radius = 0.8 + (comboLevel * 0.2);
            float vx = (float) (Math.cos(angle) * radius * 0.3f);
            float vy = (float) (Math.sin(angle) * radius * 0.3f);
            
            effectManager.particles.add(effectManager.getParticleFromPool(
                centerX, centerY, color, 3.0f + comboLevel * 0.5f,
                40 + comboLevel * 5, vx, vy
            ));
        }
        
        // Волновые эффекты
        for (int wave = 0; wave < comboLevel + 2; wave++) {
            for (int i = 0; i < 20; i++) {
                double angle = (i / 20.0) * Math.PI * 2;
                float radius = wave * 30.0f;
                float px = centerX + (float) (Math.cos(angle) * radius);
                float py = centerY + (float) (Math.sin(angle) * radius);
                
                float vx = (float) (Math.cos(angle) * 0.2f);
                float vy = (float) (Math.sin(angle) * 0.2f);
                
                effectManager.particles.add(effectManager.getParticleFromPool(
                    px, py, color, 2.0f, 35, vx, vy
                ));
            }
        }
        
        // Звук комбо
        float volume = 0.7f + (comboLevel * 0.1f);
        float pitch = 1.0f + (comboLevel * 0.05f);
        effectManager.sounds.add(new GuiEffectManager.GuiSound(
            SoundEvents.BEACON_POWER_SELECT, volume, pitch, 0
        ));
    }
    
    /**
     * Создает эффект победы
     */
    public static void createVictoryEffect(GuiEffectManager effectManager, float centerX, float centerY) {
        // Большой взрыв золотых частиц
        for (int i = 0; i < 80; i++) {
            double angle = (i / 80.0) * Math.PI * 2;
            float vx = (float) (Math.cos(angle) * 0.4f);
            float vy = (float) (Math.sin(angle) * 0.4f);
            
            effectManager.particles.add(effectManager.getParticleFromPool(
                centerX, centerY, 0xFFFFD700, 3.5f, 50, vx, vy
            ));
        }
        
        // Концентрические волны
        for (int wave = 0; wave < 8; wave++) {
            for (int i = 0; i < 25; i++) {
                double angle = (i / 25.0) * Math.PI * 2;
                float radius = wave * 40.0f;
                float px = centerX + (float) (Math.cos(angle) * radius);
                float py = centerY + (float) (Math.sin(angle) * radius);
                
                float vx = (float) (Math.cos(angle) * 0.25f);
                float vy = (float) (Math.sin(angle) * 0.25f);
                
                effectManager.particles.add(effectManager.getParticleFromPool(
                    px, py, 0xFFFFD700, 2.5f, 45, vx, vy
                ));
            }
        }
        
        // Звук победы
        effectManager.sounds.add(new GuiEffectManager.GuiSound(
            SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f, 0
        ));
    }
    
    /**
     * Создает эффект поражения
     */
    public static void createDefeatEffect(GuiEffectManager effectManager, float centerX, float centerY) {
        // Тёмные частицы
        for (int i = 0; i < 60; i++) {
            float vx = (float) ((RANDOM.nextDouble() - 0.5) * 0.4f);
            float vy = (float) ((RANDOM.nextDouble() - 0.5) * 0.4f);
            
            effectManager.particles.add(effectManager.getParticleFromPool(
                centerX, centerY, 0xFF444444, 2.0f, 40, vx, vy
            ));
        }
        
        // Звук поражения
        effectManager.sounds.add(new GuiEffectManager.GuiSound(
            SoundEvents.WITHER_DEATH, 0.5f, 0.8f, 0
        ));
    }
    
    /**
     * Создает GeckoLib анимацию для раздачи карт
     */
    private static void createGeckoLibDealAnimation(float x, float y, Card card, int cardIndex, 
                                                    int totalCards, Object geckoAnim) {
        // Используем GeckoLib для сложной 3D анимации
        // Реализация зависит от конкретного API GeckoLib
        // Здесь можно добавить логику для создания анимации через рефлексию
    }
    
    /**
     * Создает эффект изменения силы карты
     */
    public static void createPowerChangeEffect(GuiEffectManager effectManager, float x, float y,
                                               int oldPower, int newPower) {
        // ОПТИМИЗИРОВАНО: Меньше частиц
        if (effectManager.particles.size() >= 150) {
            return; // Не создаём новые, если лимит достигнут
        }
        
        int effectColor = newPower > oldPower ? 0xFF00FF00 : 0xFFFF0000;
        int particleCount = Math.min(Math.abs(newPower - oldPower) * 2 + 8, 20); // Было *3+10, стало *2+8, макс 20
        
        for (int i = 0; i < particleCount; i++) {
            if (effectManager.particles.size() >= 150) break; // Проверяем лимит в цикле
            
            double angle = (i / (double) particleCount) * Math.PI * 2;
            float vx = (float) (Math.cos(angle) * 0.15f);
            float vy = (float) (Math.sin(angle) * 0.15f - 0.1f); // Вверх
            
            effectManager.particles.add(effectManager.getParticleFromPool(
                x, y, effectColor, 2.0f, 25, vx, vy
            ));
        }
        
        // Звук изменения силы
        float pitch = newPower > oldPower ? 1.2f : 0.8f;
        effectManager.sounds.add(new GuiEffectManager.GuiSound(
            SoundEvents.AMETHYST_BLOCK_CHIME, 0.4f, pitch, 0
        ));
    }
}

