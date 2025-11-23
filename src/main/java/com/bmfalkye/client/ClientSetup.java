package com.bmfalkye.client;

import com.bmfalkye.BMFalkye;
import com.bmfalkye.client.gui.StyledCardCollectionButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;


@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = BMFalkye.MOD_ID, value = Dist.CLIENT)
public class ClientSetup {
    public static void init(FMLClientSetupEvent event) {
        // Инициализируем JEI интеграцию на клиенте
        event.enqueueWork(() -> {
            com.bmfalkye.integration.JEIIntegration.registerJEI();
        });
        BMFalkye.LOGGER.info("BM Falkye client setup...");
        // Регистрируем ItemModelProperties для карт (чтобы использовать разные текстуры по редкости)
        event.enqueueWork(() -> {
            net.minecraft.client.renderer.item.ItemProperties.register(
                com.bmfalkye.items.ModItems.CARD_ITEM.get(),
                new net.minecraft.resources.ResourceLocation(BMFalkye.MOD_ID, "rarity"),
                (stack, level, entity, seed) -> {
                    String cardId = com.bmfalkye.items.CardItem.getCardId(stack);
                    if (cardId != null) {
                        com.bmfalkye.cards.Card card = com.bmfalkye.cards.CardRegistry.getCard(cardId);
                        if (card != null) {
                            // Возвращаем значение редкости для выбора модели (0.0 = COMMON, 0.33 = RARE, 0.66 = EPIC, 1.0 = LEGENDARY)
                            return switch (card.getRarity()) {
                                case COMMON -> 0.0f;
                                case RARE -> 0.33f;
                                case EPIC -> 0.66f;
                                case LEGENDARY -> 1.0f;
                            };
                        }
                    }
                    return 0.0f; // По умолчанию обычная редкость
                }
            );
        });
    }
    
    /**
     * Добавляет кнопки справа от инвентаря
     * Кнопки: Админ-Панель (только для админов), Фальки (главное меню)
     */
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof InventoryScreen) {
            int screenWidth = event.getScreen().width;
            int screenHeight = event.getScreen().height;
            
            // Позиция инвентаря (стандартные координаты Minecraft)
            int leftPos = (screenWidth - 176) / 2;
            int topPos = (screenHeight - 166) / 2;
            
            // Размещаем кнопки СПРАВА от инвентаря (вне его границ)
            int buttonX = leftPos + 176 + 8; // Справа от инвентаря с отступом 8 пикселей
            int buttonY = topPos + 137; // Начальная позиция
            int buttonWidth = 100;
            int buttonHeight = 20;
            int buttonSpacing = 2; // Отступ между кнопками
            
            // Проверяем условия отображения
            if (net.minecraft.client.Minecraft.getInstance().player != null) {
                net.minecraft.world.entity.player.Player player = net.minecraft.client.Minecraft.getInstance().player;
                boolean isAdmin = AdminPanelScreen.hasAdminPermissions(player);
                
                int buttonIndex = 0;
                
                // Кнопка "Админ-Панель" (только для админов)
                if (isAdmin) {
                    Button.OnPress adminPanelPress = (button) -> {
                        if (net.minecraft.client.Minecraft.getInstance().player != null) {
                            com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
                                new com.bmfalkye.network.NetworkHandler.OpenAdminPanelPacket());
                        }
                    };
                    
                    Button adminPanelButton = new StyledCardCollectionButton(
                        buttonX, buttonY - (buttonHeight + buttonSpacing), 
                        buttonWidth, buttonHeight,
                        Component.translatable("screen.bm_falkye.admin_panel"),
                        adminPanelPress
                    );
                    event.addListener(adminPanelButton);
                    buttonIndex++;
                }
                
                // Кнопка "Фальки" (главное меню)
                Button.OnPress falkyeMenuPress = (button) -> {
                    if (net.minecraft.client.Minecraft.getInstance().player != null) {
                        net.minecraft.client.Minecraft.getInstance().setScreen(new FalkyeMainMenuScreen());
                    }
                };
                
                Button falkyeMenuButton = new StyledCardCollectionButton(
                    buttonX, buttonY, 
                    buttonWidth, buttonHeight,
                    Component.literal("§6§lФальки"),
                    falkyeMenuPress
                );
                event.addListener(falkyeMenuButton);
            }
        }
    }
    
}

