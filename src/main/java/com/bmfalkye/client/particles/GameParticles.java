package com.bmfalkye.client.particles;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.bmfalkye.BMFalkye;

/**
 * Система партиклов для игровых эффектов
 */
@Mod.EventBusSubscriber(modid = BMFalkye.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class GameParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = 
        DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, BMFalkye.MOD_ID);
    
    public static final RegistryObject<SimpleParticleType> CARD_PLAY = 
        PARTICLE_TYPES.register("card_play", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> CARD_DRAW = 
        PARTICLE_TYPES.register("card_draw", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> VICTORY = 
        PARTICLE_TYPES.register("victory", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> DEFEAT = 
        PARTICLE_TYPES.register("defeat", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> FIRE_EFFECT = 
        PARTICLE_TYPES.register("fire_effect", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> FROST_EFFECT = 
        PARTICLE_TYPES.register("frost_effect", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> NATURE_EFFECT = 
        PARTICLE_TYPES.register("nature_effect", () -> new SimpleParticleType(false));
    
    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(CARD_PLAY.get(), CardPlayParticle.Provider::new);
        event.registerSpriteSet(CARD_DRAW.get(), CardDrawParticle.Provider::new);
        event.registerSpriteSet(VICTORY.get(), VictoryParticle.Provider::new);
        event.registerSpriteSet(DEFEAT.get(), DefeatParticle.Provider::new);
        event.registerSpriteSet(FIRE_EFFECT.get(), FireEffectParticle.Provider::new);
        event.registerSpriteSet(FROST_EFFECT.get(), FrostEffectParticle.Provider::new);
        event.registerSpriteSet(NATURE_EFFECT.get(), NatureEffectParticle.Provider::new);
    }
}

