package com.bmfalkye.game;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRarity;
import com.bmfalkye.client.particles.GameParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Утилита для создания визуальных эффектов в игре
 */
public class VisualEffects {
    
    /**
     * Проигрывает визуальные эффекты при игре карты на поле
     */
    public static void playCardPlayEffects(ServerLevel level, ServerPlayer player, Card card, FalkyeGameSession.CardRow row) {
        if (level == null || player == null || card == null) {
            return;
        }
        
        double x = player.getX();
        double y = player.getY() + player.getEyeHeight();
        double z = player.getZ();
        
        // Используем кастомный партикл CARD_PLAY
        int particleCount = 20 + (card.getRarity().ordinal() * 15);
        
        for (int i = 0; i < particleCount; i++) {
            double angle = (i / (double) particleCount) * Math.PI * 2;
            double radius = 0.5 + (card.getRarity().ordinal() * 0.3);
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double offsetY = (level.random.nextDouble() - 0.5) * 1.5;
            
            // Используем кастомный партикл CARD_PLAY
            level.sendParticles(
                GameParticles.CARD_PLAY.get(),
                x + offsetX, y + offsetY, z + offsetZ,
                1,
                0.0, 0.1, 0.0,
                0.05
            );
        }
        
        // Дополнительные эффекты в зависимости от типа карты и редкости
        CardRarity rarity = card.getRarity();
        switch (card.getType()) {
            case CREATURE:
                // Для существ - дополнительные партиклы в зависимости от фракции
                if (card.getFaction().contains("Огонь") || card.getFaction().contains("Fire")) {
                    for (int i = 0; i < 10; i++) {
                        double offsetX = (level.random.nextDouble() - 0.5) * 1.5;
                        double offsetY = level.random.nextDouble() * 1.5;
                        double offsetZ = (level.random.nextDouble() - 0.5) * 1.5;
                        level.sendParticles(
                            GameParticles.FIRE_EFFECT.get(),
                            x + offsetX, y + offsetY, z + offsetZ,
                            1, 0.0, 0.1, 0.0, 0.05
                        );
                    }
                } else if (card.getFaction().contains("Лёд") || card.getFaction().contains("Frost")) {
                    for (int i = 0; i < 10; i++) {
                        double offsetX = (level.random.nextDouble() - 0.5) * 1.5;
                        double offsetY = level.random.nextDouble() * 1.5;
                        double offsetZ = (level.random.nextDouble() - 0.5) * 1.5;
                        level.sendParticles(
                            GameParticles.FROST_EFFECT.get(),
                            x + offsetX, y + offsetY, z + offsetZ,
                            1, 0.0, 0.1, 0.0, 0.05
                        );
                    }
                } else if (card.getFaction().contains("Природа") || card.getFaction().contains("Nature")) {
                    for (int i = 0; i < 10; i++) {
                        double offsetX = (level.random.nextDouble() - 0.5) * 1.5;
                        double offsetY = level.random.nextDouble() * 1.5;
                        double offsetZ = (level.random.nextDouble() - 0.5) * 1.5;
                        level.sendParticles(
                            GameParticles.NATURE_EFFECT.get(),
                            x + offsetX, y + offsetY, z + offsetZ,
                            1, 0.0, 0.1, 0.0, 0.05
                        );
                    }
                }
                break;
            case SPELL:
                // Для заклинаний - больше магических партиклов
                for (int i = 0; i < 15; i++) {
                    double angle = (i / 15.0) * Math.PI * 2;
                    double radius = 1.0;
                    double offsetX = Math.cos(angle) * radius;
                    double offsetZ = Math.sin(angle) * radius;
                    double offsetY = (level.random.nextDouble() - 0.5) * 2.0;
                    level.sendParticles(
                        ParticleTypes.ENCHANT,
                        x + offsetX, y + offsetY, z + offsetZ,
                        1, 0.0, 0.1, 0.0, 0.05
                    );
                }
                break;
            case SPECIAL:
                // Для особых карт - комбинация эффектов
                for (int i = 0; i < 20; i++) {
                    double angle = (i / 20.0) * Math.PI * 2;
                    double radius = 1.5;
                    double offsetX = Math.cos(angle) * radius;
                    double offsetZ = Math.sin(angle) * radius;
                    double offsetY = (i / 20.0) * 2.0;
                    level.sendParticles(
                        ParticleTypes.TOTEM_OF_UNDYING,
                        x + offsetX, y + offsetY, z + offsetZ,
                        1, 0.0, 0.1, 0.0, 0.1
                    );
                }
                break;
        }
        
        // Дополнительные партиклы для легендарных карт
        if (rarity == CardRarity.LEGENDARY) {
            for (int i = 0; i < 15; i++) {
                double offsetX = (level.random.nextDouble() - 0.5) * 2.0;
                double offsetY = level.random.nextDouble() * 2.0;
                double offsetZ = (level.random.nextDouble() - 0.5) * 2.0;
                level.sendParticles(
                    ParticleTypes.CRIT,
                    x + offsetX, y + offsetY, z + offsetZ,
                    1, 0.0, 0.2, 0.0, 0.1
                );
            }
        }
        
        // Звук в зависимости от редкости
        float volume = 0.5f + (rarity.ordinal() * 0.2f);
        float pitch = 0.8f + (rarity.ordinal() * 0.1f);
        
        switch (rarity) {
            case COMMON:
                level.playSound(null, x, y, z, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, volume, pitch);
                break;
            case RARE:
                level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, volume, pitch);
                break;
            case EPIC:
                level.playSound(null, x, y, z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, volume, pitch);
                break;
            case LEGENDARY:
                level.playSound(null, x, y, z, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, volume, pitch);
                break;
        }
    }
    
    /**
     * Проигрывает визуальные эффекты при окончании раунда
     */
    public static void playRoundEndEffects(ServerLevel level, ServerPlayer player, boolean won) {
        if (level == null || player == null) {
            return;
        }
        
        double x = player.getX();
        double y = player.getY() + player.getEyeHeight();
        double z = player.getZ();
        
        if (won) {
            // Используем кастомный партикл VICTORY
            for (int i = 0; i < 40; i++) {
                double angle = (i / 40.0) * Math.PI * 2;
                double radius = 1.5;
                double offsetX = Math.cos(angle) * radius;
                double offsetZ = Math.sin(angle) * radius;
                double offsetY = (level.random.nextDouble() - 0.5) * 2.0;
                level.sendParticles(
                    GameParticles.VICTORY.get(),
                    x + offsetX, y + offsetY, z + offsetZ,
                    1, 0.0, 0.2, 0.0, 0.1
                );
            }
            // Дополнительные золотые партиклы
            for (int i = 0; i < 20; i++) {
                double offsetX = (level.random.nextDouble() - 0.5) * 2.0;
                double offsetY = level.random.nextDouble() * 2.0;
                double offsetZ = (level.random.nextDouble() - 0.5) * 2.0;
                level.sendParticles(
                    ParticleTypes.CRIT,
                    x + offsetX, y + offsetY, z + offsetZ,
                    1, 0.0, 0.2, 0.0, 0.1
                );
            }
            level.playSound(null, x, y, z, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.0f);
        } else {
            // Используем кастомный партикл DEFEAT
            for (int i = 0; i < 30; i++) {
                double offsetX = (level.random.nextDouble() - 0.5) * 2.0;
                double offsetY = level.random.nextDouble() * 2.0;
                double offsetZ = (level.random.nextDouble() - 0.5) * 2.0;
                level.sendParticles(
                    GameParticles.DEFEAT.get(),
                    x + offsetX, y + offsetY, z + offsetZ,
                    1, 0.0, 0.1, 0.0, 0.05
                );
            }
            // Дополнительные серые партиклы дыма
            for (int i = 0; i < 15; i++) {
                double offsetX = (level.random.nextDouble() - 0.5) * 2.0;
                double offsetY = level.random.nextDouble() * 2.0;
                double offsetZ = (level.random.nextDouble() - 0.5) * 2.0;
                level.sendParticles(
                    ParticleTypes.SMOKE,
                    x + offsetX, y + offsetY, z + offsetZ,
                    1, 0.0, 0.1, 0.0, 0.05
                );
            }
            level.playSound(null, x, y, z, SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.5f, 0.8f);
        }
    }
    
    /**
     * Проигрывает визуальные эффекты при окончании игры
     */
    public static void playGameEndEffects(ServerLevel level, ServerPlayer player, boolean won) {
        if (level == null || player == null) {
            return;
        }
        
        double x = player.getX();
        double y = player.getY() + player.getEyeHeight();
        double z = player.getZ();
        
        if (won) {
            // Большой эффект победы с кастомными партиклами VICTORY
            for (int i = 0; i < 60; i++) {
                double angle = (i / 60.0) * Math.PI * 2;
                double radius = 2.0;
                double offsetX = Math.cos(angle) * radius;
                double offsetZ = Math.sin(angle) * radius;
                double offsetY = (level.random.nextDouble() - 0.5) * 3.0;
                level.sendParticles(
                    GameParticles.VICTORY.get(),
                    x + offsetX, y + offsetY, z + offsetZ,
                    1, 0.0, 0.2, 0.0, 0.15
                );
            }
            // Критические удары
            for (int i = 0; i < 40; i++) {
                double offsetX = (level.random.nextDouble() - 0.5) * 3.0;
                double offsetY = level.random.nextDouble() * 3.0;
                double offsetZ = (level.random.nextDouble() - 0.5) * 3.0;
                level.sendParticles(
                    ParticleTypes.CRIT,
                    x + offsetX, y + offsetY, z + offsetZ,
                    1, 0.0, 0.3, 0.0, 0.1
                );
            }
            // Партиклы тотема для дополнительного эффекта
            for (int i = 0; i < 30; i++) {
                double angle = (i / 30.0) * Math.PI * 2;
                double radius = 2.5;
                double offsetX = Math.cos(angle) * radius;
                double offsetZ = Math.sin(angle) * radius;
                double offsetY = (i / 30.0) * 3.0;
                level.sendParticles(
                    ParticleTypes.TOTEM_OF_UNDYING,
                    x + offsetX, y + offsetY, z + offsetZ,
                    1, 0.0, 0.2, 0.0, 0.15
                );
            }
            level.playSound(null, x, y, z, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
        } else {
            // Эффект поражения с кастомными партиклами DEFEAT
            for (int i = 0; i < 50; i++) {
                double offsetX = (level.random.nextDouble() - 0.5) * 3.0;
                double offsetY = level.random.nextDouble() * 3.0;
                double offsetZ = (level.random.nextDouble() - 0.5) * 3.0;
                level.sendParticles(
                    GameParticles.DEFEAT.get(),
                    x + offsetX, y + offsetY, z + offsetZ,
                    1, 0.0, 0.1, 0.0, 0.1
                );
            }
            // Большие партиклы дыма
            for (int i = 0; i < 30; i++) {
                double offsetX = (level.random.nextDouble() - 0.5) * 3.0;
                double offsetY = level.random.nextDouble() * 3.0;
                double offsetZ = (level.random.nextDouble() - 0.5) * 3.0;
                level.sendParticles(
                    ParticleTypes.LARGE_SMOKE,
                    x + offsetX, y + offsetY, z + offsetZ,
                    1, 0.0, 0.1, 0.0, 0.1
                );
            }
            level.playSound(null, x, y, z, SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 0.5f, 0.8f);
        }
    }
}

