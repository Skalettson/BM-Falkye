package com.bmfalkye.game;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;

/**
 * Эффекты окружения, влияющие на игру
 */
public class LocationEffect {
    public enum LocationType {
        NONE,
        DESERT,      // Пустыня - усиливает огненные карты
        SNOW,        // Снег - усиливает ледяные карты
        FOREST,      // Лес - усиливает природные карты
        OCEAN,       // Океан - усиливает водные карты
        MOUNTAIN,    // Горы - усиливает карты осады
        PLAINS,      // Равнины - нейтрально
        NETHER,      // Незер - усиливает демонические карты
        END          // Энд - усиливает мистические карты
    }
    
    /**
     * Определяет тип локации по позиции игрока
     */
    public static LocationType getLocationType(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) {
            return LocationType.NONE;
        }
        
        Biome biome = level.getBiome(pos).value();
        String biomeName = biome.toString().toLowerCase();
        
        if (biomeName.contains("desert")) {
            return LocationType.DESERT;
        } else if (biomeName.contains("snow") || biomeName.contains("frozen") || biomeName.contains("ice")) {
            return LocationType.SNOW;
        } else if (biomeName.contains("forest") || biomeName.contains("taiga") || biomeName.contains("jungle")) {
            return LocationType.FOREST;
        } else if (biomeName.contains("ocean") || biomeName.contains("beach") || biomeName.contains("river")) {
            return LocationType.OCEAN;
        } else if (biomeName.contains("mountain") || biomeName.contains("hills") || biomeName.contains("peaks")) {
            return LocationType.MOUNTAIN;
        } else if (biomeName.contains("nether") || biomeName.contains("soul") || biomeName.contains("warped")) {
            return LocationType.NETHER;
        } else if (biomeName.contains("end")) {
            return LocationType.END;
        }
        
        return LocationType.PLAINS;
    }
    
    /**
     * Вычисляет модификатор силы карты в зависимости от локации
     */
    public static int getLocationPowerModifier(LocationType location, String cardFaction) {
        switch (location) {
            case NONE:
            case PLAINS:
                return 0;
            case DESERT:
                if (cardFaction.equals("Дом Пламени")) {
                    return 2; // +2 к силе огненных карт в пустыне
                }
                break;
            case SNOW:
                if (cardFaction.equals("Дозорные Руин")) {
                    return 2; // +2 к силе ледяных карт в снегу
                }
                break;
            case FOREST:
                if (cardFaction.equals("Дом Пламени") || cardFaction.contains("Природа")) {
                    return 1; // +1 к силе природных карт в лесу
                }
                break;
            case OCEAN:
                if (cardFaction.contains("Вода") || cardFaction.contains("Океан")) {
                    return 2; // +2 к силе водных карт в океане
                }
                break;
            case MOUNTAIN:
                // Усиливает карты осады
                return 1;
            case NETHER:
                if (cardFaction.contains("Демон") || cardFaction.contains("Ад")) {
                    return 3; // +3 к силе демонических карт в незере
                }
                break;
            case END:
                if (cardFaction.contains("Мистик") || cardFaction.contains("Энд")) {
                    return 3; // +3 к силе мистических карт в энде
                }
                break;
        }
        
        return 0; // Нет модификатора
    }
}

