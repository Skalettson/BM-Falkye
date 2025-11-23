package com.bmfalkye;

import com.bmfalkye.commands.FalkyeCommand;
import com.bmfalkye.items.ModItems;
import com.bmfalkye.network.NetworkHandler;
import com.bmfalkye.util.ModLogger;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Главный класс мода BM Falkye
 * 
 * <p>BM Falkye - это карточная игра для Minecraft 1.20.1 (Forge), вдохновлённая стратегическими 
 * карточными играми в стиле Gwent. Мод добавляет полноценную карточную игру с обширной системой 
 * карт, механикой боя, социальными функциями и системой прогрессии.</p>
 * 
 * <p>Основные возможности мода:</p>
 * <ul>
 *   <li>150+ карт из лора вселенной BeforeMine</li>
 *   <li>Игра между игроками или игроком и NPC</li>
 *   <li>Система прогрессии с уровнями, достижениями и рейтингом</li>
 *   <li>Социальные системы (друзья, гильдии, турниры)</li>
 *   <li>Адаптивный интерфейс с поддержкой разных разрешений экрана</li>
 * </ul>
 * 
 * @author BM Falkye Team
 * @version 1.0
 * @since 1.20.1
 */
@Mod(BMFalkye.MOD_ID)
public class BMFalkye {
    /** Идентификатор мода */
    public static final String MOD_ID = "bm_falkye";
    
    /** Логгер для записи событий мода */
    public static final Logger LOGGER = LogUtils.getLogger();

    public BMFalkye() {
        // Инициализируем расширенный логгер
        ModLogger.initialize();
        ModLogger.info("=== BM Falkye Mod Starting ===");
        ModLogger.info("Mod ID: {}", MOD_ID);
        
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Регистрируем блоки
        com.bmfalkye.blocks.ModBlocks.register(modEventBus);
        
        // Регистрируем предметы
        ModItems.register(modEventBus);
        
        // Регистрируем креатив-таб
        com.bmfalkye.items.ModCreativeTabs.register(modEventBus);
        
        // Регистрируем партиклы
        com.bmfalkye.client.particles.GameParticles.PARTICLE_TYPES.register(modEventBus);
        
        // Регистрируем звуки
        com.bmfalkye.sounds.ModSounds.register(modEventBus);
        
        // Регистрируем модификаторы лута
        com.bmfalkye.init.ModLootModifiers.register(modEventBus);
        ModLogger.info("Registered loot modifiers");

        modEventBus.addListener(this::commonSetup);
        
        // Регистрируем clientSetup ТОЛЬКО на клиенте
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            modEventBus.addListener(this::clientSetup);
            ModLogger.info("Client setup listener registered");
        });

        MinecraftForge.EVENT_BUS.register(this);
        ModLogger.info("MinecraftForge event bus registered");
        ModLogger.info("Mod initialization complete");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ModLogger.info("Registering commands");
        FalkyeCommand.register(event.getDispatcher());
        ModLogger.info("Commands registered successfully");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        ModLogger.info("=== Common Setup Started ===");
        LOGGER.info("BM Falkye mod loading...");
        
        ModLogger.info("Registering network handler");
        NetworkHandler.register();
        ModLogger.info("Network handler registered");
        
        // Инициализируем карты и лидеров
        ModLogger.info("Initializing card registry");
        com.bmfalkye.cards.CardRegistry.initializeDefaultCards();
        ModLogger.info("Default cards initialized");
        
        ModLogger.info("Initializing expanded card registry");
        com.bmfalkye.cards.ExpandedCardRegistry.initializeAllCards(); // Загружаем все 150+ карт
        int totalCards = com.bmfalkye.cards.CardRegistry.getAllCards().size();
        ModLogger.info("All cards initialized. Total cards: {}", totalCards);
        
        ModLogger.info("Initializing leader registry");
        com.bmfalkye.cards.LeaderRegistry.initializeLeaders();
        ModLogger.info("Leaders initialized");
        
        // Инициализируем систему эволюции карт
        ModLogger.info("Initializing card evolution trees");
        com.bmfalkye.evolution.CardEvolutionTree.initializeEvolutionTrees();
        ModLogger.info("Card evolution trees initialized");
        
        // Инициализируем систему квестов
        ModLogger.info("Initializing quest system");
        com.bmfalkye.quests.QuestRegistry.initializeLegendaryQuests();
        ModLogger.info("Quest system initialized");
        
        // Инициализируем сюжетных NPC
        ModLogger.info("Initializing story NPCs");
        com.bmfalkye.npc.StoryNPCRegistry.initializeStoryNPCs();
        ModLogger.info("Story NPCs initialized");
        
        // Инициализируем достижения
        ModLogger.info("Initializing achievement system");
        com.bmfalkye.achievements.AchievementSystem.initializeAchievements();
        ModLogger.info("Achievement system initialized");
        
        // Инициализируем интеграции с другими модами
        ModLogger.info("Initializing mod integrations");
        com.bmfalkye.integration.LibraryIntegration.init();
        com.bmfalkye.integration.CuriosIntegration.registerCuriosSlot();
        com.bmfalkye.integration.PatchouliIntegration.registerTutorialBook();
        ModLogger.info("Mod integrations initialized");
        
        // API для расширений инициализируется автоматически через IEventBus
        ModLogger.info("FalkyeAPI ready for addon registration");
        
        ModLogger.info("=== Common Setup Completed ===");
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        // Инициализируем JEI интеграцию на клиенте
        event.enqueueWork(() -> {
            com.bmfalkye.integration.JEIIntegration.registerJEI();
        });
        ModLogger.info("=== Client Setup Started ===");
        // Вызываем клиентскую инициализацию через отдельный класс
        com.bmfalkye.client.ClientSetup.init(event);
        ModLogger.info("=== Client Setup Completed ===");
    }
}

