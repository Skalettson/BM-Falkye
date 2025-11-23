package com.bmfalkye.client.sounds;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRarity;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Менеджер звуковых эффектов для всех игровых действий
 * Создает подходящие звуки для каждого события в игре
 */
public class SoundEffectManager {
    
    private static final Random RANDOM = new Random();
    private static final Map<String, Long> lastPlayedTime = new HashMap<>();
    private static final long SOUND_COOLDOWN = 50; // Минимальная задержка между одинаковыми звуками (мс)
    
    /**
     * Воспроизводит звук игры карты
     */
    public static void playCardPlaySound(Card card) {
        SoundEvent sound = switch (card.getRarity()) {
            case COMMON -> SoundEvents.ITEM_PICKUP;
            case RARE -> SoundEvents.AMETHYST_BLOCK_CHIME;
            case EPIC -> SoundEvents.BEACON_ACTIVATE;
            case LEGENDARY -> SoundEvents.TOTEM_USE;
        };
        
        float volume = 0.5f + (card.getRarity().ordinal() * 0.15f);
        float pitch = 0.8f + (card.getRarity().ordinal() * 0.1f);
        
        playSound(sound, volume, pitch);
    }
    
    /**
     * Воспроизводит звук раздачи карты
     */
    public static void playCardDrawSound(CardRarity rarity) {
        SoundEvent sound = switch (rarity) {
            case COMMON -> SoundEvents.ITEM_PICKUP;
            case RARE -> SoundEvents.AMETHYST_BLOCK_CHIME;
            case EPIC -> SoundEvents.BEACON_ACTIVATE;
            case LEGENDARY -> SoundEvents.TOTEM_USE;
        };
        
        float volume = 0.3f + (rarity.ordinal() * 0.1f);
        float pitch = 0.9f + (rarity.ordinal() * 0.1f) + (RANDOM.nextFloat() * 0.2f - 0.1f);
        
        playSound(sound, volume, pitch);
    }
    
    /**
     * Воспроизводит звук комбо-эффекта
     */
    public static void playComboSound(String comboType, int comboLevel) {
        SoundEvent sound = switch (comboType) {
            case "faction" -> SoundEvents.BEACON_POWER_SELECT;
            case "legendary" -> SoundEvents.TOTEM_USE;
            case "epic" -> SoundEvents.BEACON_ACTIVATE;
            case "spell" -> SoundEvents.AMETHYST_BLOCK_CHIME;
            case "creature" -> SoundEvents.ITEM_PICKUP;
            case "power" -> SoundEvents.BEACON_POWER_SELECT;
            default -> SoundEvents.BEACON_ACTIVATE;
        };
        
        float volume = 0.7f + (comboLevel * 0.1f);
        float pitch = 1.0f + (comboLevel * 0.05f);
        
        playSound(sound, volume, pitch);
    }
    
    /**
     * Воспроизводит звук победы
     */
    public static void playVictorySound() {
        playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }
    
    /**
     * Воспроизводит звук поражения
     */
    public static void playDefeatSound() {
        playSound(SoundEvents.WITHER_DEATH, 0.5f, 0.8f);
    }
    
    /**
     * Воспроизводит звук окончания раунда
     */
    public static void playRoundEndSound(boolean won) {
        if (won) {
            playSound(SoundEvents.PLAYER_LEVELUP, 0.5f, 1.0f);
        } else {
            playSound(SoundEvents.VILLAGER_NO, 0.5f, 0.8f);
        }
    }
    
    /**
     * Воспроизводит звук изменения силы карты
     */
    public static void playPowerChangeSound(int oldPower, int newPower) {
        boolean increased = newPower > oldPower;
        SoundEvent sound = increased ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.ANVIL_LAND;
        float pitch = increased ? 1.2f : 0.8f;
        
        playSound(sound, 0.4f, pitch);
    }
    
    /**
     * Воспроизводит звук использования способности
     */
    public static void playAbilitySound(Card.CardType cardType) {
        SoundEvent sound = switch (cardType) {
            case SPELL -> SoundEvents.AMETHYST_BLOCK_CHIME;
            case SPECIAL -> SoundEvents.BEACON_ACTIVATE;
            case CREATURE -> SoundEvents.ITEM_PICKUP;
        };
        
        playSound(sound, 0.5f, 1.0f);
    }
    
    /**
     * Воспроизводит звук наведения на карту
     */
    public static void playCardHoverSound(CardRarity rarity) {
        // Только для редких карт, чтобы не было слишком много звуков
        if (rarity == CardRarity.LEGENDARY || rarity == CardRarity.EPIC) {
            String key = "hover_" + rarity.name();
            if (canPlaySound(key)) {
                playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.2f, 1.5f);
                lastPlayedTime.put(key, System.currentTimeMillis());
            }
        }
    }
    
    /**
     * Воспроизводит звук клика по кнопке
     */
    public static void playButtonClickSound() {
        playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f, 1.0f);
    }
    
    /**
     * Воспроизводит звук начала хода
     */
    public static void playTurnStartSound() {
        playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 0.4f, 1.0f);
    }
    
    /**
     * Воспроизводит звук паса
     */
    public static void playPassSound() {
        playSound(SoundEvents.VILLAGER_NO, 0.3f, 0.9f);
    }
    
    /**
     * Воспроизводит звук сдачи
     */
    public static void playSurrenderSound() {
        playSound(SoundEvents.WITHER_DEATH, 0.4f, 0.7f);
    }
    
    /**
     * Воспроизводит звук погодного эффекта
     */
    public static void playWeatherSound(com.bmfalkye.game.FalkyeGameSession.WeatherType weather) {
        SoundEvent sound = switch (weather) {
            case FROST -> SoundEvents.AMETHYST_BLOCK_CHIME;
            case FOG -> {
                // AMBIENT_CAVE возвращает Holder.Reference, используем альтернативный звук
                yield SoundEvents.AMETHYST_BLOCK_CHIME;
            }
            case RAIN -> {
                // WEATHER_RAIN возвращает Holder.Reference, используем альтернативный звук
                yield SoundEvents.AMETHYST_BLOCK_CHIME;
            }
            case NONE -> null;
        };
        
        if (sound != null) {
            playSound(sound, 0.3f, 1.0f);
        }
    }
    
    /**
     * Воспроизводит звук через игрока
     */
    private static void playSound(SoundEvent sound, float volume, float pitch) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.level != null && sound != null) {
            Player player = minecraft.player;
            try {
                minecraft.level.playLocalSound(
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    sound,
                    SoundSource.PLAYERS,
                    volume,
                    pitch,
                    false
                );
            } catch (Exception e) {
                // Игнорируем ошибки воспроизведения звука
            }
        }
    }
    
    /**
     * Проверяет, можно ли воспроизвести звук (защита от спама)
     */
    private static boolean canPlaySound(String key) {
        Long lastTime = lastPlayedTime.get(key);
        if (lastTime == null) {
            return true;
        }
        return System.currentTimeMillis() - lastTime >= SOUND_COOLDOWN;
    }
}

