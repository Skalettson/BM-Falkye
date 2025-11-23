package com.bmfalkye.items;

import com.bmfalkye.BMFalkye;
import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BMFalkye.MOD_ID);

    public static final RegistryObject<CreativeModeTab> BM_FALKYE_TAB = CREATIVE_MODE_TABS.register("bm_falkye_tab",
        () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(ModItems.DECK_OF_CARDS.get()))
            .title(Component.translatable("creativetab.bm_falkye.bm_falkye_tab"))
            .displayItems((parameters, output) -> {
                // Предметы - используем Item напрямую, Forge автоматически создаст ItemStack с count=1
                output.accept(ModItems.DECK_OF_CARDS.get());
                
                // Добавляем все карты из реестра
                List<Card> allCards = CardRegistry.getAllCards();
                for (Card card : allCards) {
                    ItemStack cardStack = CardItem.createCardStack(card.getId());
                    output.accept(cardStack);
                }
            })
            .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}

