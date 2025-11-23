package com.bmfalkye.init;

import com.bmfalkye.BMFalkye;
import com.bmfalkye.loot.CardLootModifier;
import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModLootModifiers {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
        DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, BMFalkye.MOD_ID);
    
    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> CARD_LOOT_MODIFIER =
        LOOT_MODIFIERS.register("card_loot", CardLootModifier.CODEC);
    
    public static void register(IEventBus eventBus) {
        LOOT_MODIFIERS.register(eventBus);
    }
}

