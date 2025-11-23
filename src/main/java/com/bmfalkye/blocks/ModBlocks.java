package com.bmfalkye.blocks;

import com.bmfalkye.BMFalkye;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, BMFalkye.MOD_ID);
    
    public static final RegistryObject<Block> CARTOGRAPHY_TABLE = BLOCKS.register("cartography_table",
            () -> new CartographyTableBlock(BlockBehaviour.Properties.copy(Blocks.CARTOGRAPHY_TABLE)));
    
    public static final RegistryObject<Block> ALCHEMY_TABLE = BLOCKS.register("alchemy_table",
            () -> new AlchemyTableBlock(BlockBehaviour.Properties.copy(Blocks.ENCHANTING_TABLE)));
    
    public static final RegistryObject<Block> TROPHY_DISPLAY = BLOCKS.register("trophy_display",
            () -> new TrophyDisplayBlock(BlockBehaviour.Properties.copy(Blocks.GLASS)));
    
    // Блоки для точек силы
    public static final RegistryObject<Block> ABANDONED_ALTAR = BLOCKS.register("abandoned_altar",
            () -> new TerritoryPointBlock(BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS), "Заброшенный Алтарь"));
    
    public static final RegistryObject<Block> ANCIENT_OBELISK = BLOCKS.register("ancient_obelisk",
            () -> new TerritoryPointBlock(BlockBehaviour.Properties.copy(Blocks.OBSIDIAN), "Древний Обелиск"));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}

