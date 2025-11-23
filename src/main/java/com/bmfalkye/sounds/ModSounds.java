package com.bmfalkye.sounds;

import com.bmfalkye.BMFalkye;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Реестр звуков мода (использует звуки из ванильного Minecraft)
 */
public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = 
        DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, BMFalkye.MOD_ID);
    
    // Используем звуки из ванильного Minecraft через RegistryObject
    
    // Регистрируем кастомные звуковые события для sounds.json (которые ссылаются на ванильные звуки)
    public static final RegistryObject<SoundEvent> CARD_PLAY_EVENT = 
        registerSoundEvent("card_play");
    public static final RegistryObject<SoundEvent> CARD_DRAW_EVENT = 
        registerSoundEvent("card_draw");
    public static final RegistryObject<SoundEvent> VICTORY_EVENT = 
        registerSoundEvent("victory");
    public static final RegistryObject<SoundEvent> DEFEAT_EVENT = 
        registerSoundEvent("defeat");
    public static final RegistryObject<SoundEvent> ROUND_END_EVENT = 
        registerSoundEvent("round_end");
    public static final RegistryObject<SoundEvent> GAME_START_EVENT = 
        registerSoundEvent("game_start");
    public static final RegistryObject<SoundEvent> FIRE_EFFECT_EVENT = 
        registerSoundEvent("fire_effect");
    public static final RegistryObject<SoundEvent> FROST_EFFECT_EVENT = 
        registerSoundEvent("frost_effect");
    public static final RegistryObject<SoundEvent> NATURE_EFFECT_EVENT = 
        registerSoundEvent("nature_effect");
    
    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(
            new ResourceLocation(BMFalkye.MOD_ID, name)));
    }
    
    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}

