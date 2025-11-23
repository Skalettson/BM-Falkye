package com.bmfalkye.items;

import com.bmfalkye.BMFalkye;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BMFalkye.MOD_ID);

    public static final RegistryObject<Item> DECK_OF_CARDS = ITEMS.register("deck_of_cards",
            () -> new DeckOfCardsItem(new Item.Properties().stacksTo(1)));
    
    public static final RegistryObject<Item> CARD_ITEM = ITEMS.register("card",
            () -> new CardItem(new Item.Properties().stacksTo(1)));
    
    public static final RegistryObject<Item> QUEST_ITEM = ITEMS.register("quest_item",
            () -> new QuestItem(new Item.Properties().stacksTo(1)));
    
    public static final RegistryObject<Item> CARTOGRAPHY_TABLE_ITEM = ITEMS.register("cartography_table",
            () -> new CartographyTableItem(new Item.Properties()));
    
    public static final RegistryObject<Item> ALCHEMY_TABLE_ITEM = ITEMS.register("alchemy_table",
            () -> new net.minecraft.world.item.BlockItem(
                com.bmfalkye.blocks.ModBlocks.ALCHEMY_TABLE.get(),
                new Item.Properties()));
    
    public static final RegistryObject<Item> TROPHY_DISPLAY_ITEM = ITEMS.register("trophy_display",
            () -> new net.minecraft.world.item.BlockItem(
                com.bmfalkye.blocks.ModBlocks.TROPHY_DISPLAY.get(),
                new Item.Properties()));
    
    public static final RegistryObject<Item> TOURNAMENT_TICKET = ITEMS.register("tournament_ticket",
            () -> new TournamentTicketItem(new Item.Properties().stacksTo(16)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}

