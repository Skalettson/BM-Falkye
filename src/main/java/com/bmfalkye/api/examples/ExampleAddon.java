package com.bmfalkye.api.examples;

import com.bmfalkye.api.*;
import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRarity;
import com.bmfalkye.game.FalkyeGameSession;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;

/**
 * ПРИМЕР использования API для создания аддона
 * 
 * ВАЖНО: Этот файл является примером для модмейкеров.
 * Для использования в реальном аддоне:
 * 1. Создайте отдельный мод-проект
 * 2. Добавьте BM Falkye как зависимость
 * 3. Используйте этот код как шаблон
 * 
 * Этот класс НЕ должен быть частью основного мода BM Falkye.
 * Он здесь только для документации и примеров.
 * 
 * Пример кода для аддона:
 * 
 * @Mod("my_falkye_addon")
 * public class MyAddon {
 *     
 *     public MyAddon(IEventBus modBus) {
 *         // Регистрируем карту через CardBuilder
 *         CardBuilder builder = CardBuilder.create()
 *             .id("rock_troll")
 *             .name("Скальный Тролль")
 *             .type(Card.CardType.CREATURE)
 *             .power(8)
 *             .faction("Дозорные Руин")
 *             .rarity(CardRarity.RARE)
 *             .cost(15)
 *             .description("Мощное существо с защитой от первого удара")
 *             .ability(new Ability() {
 *                 @Override
 *                 public void onPlay(FalkyeGameSession session, ServerPlayer player, String cardId) {
 *                     // При розыгрыше получает щит от первого удара
 *                     // (это можно реализовать через систему баффов)
 *                 }
 *                 
 *                 @Override
 *                 public String getName() {
 *                     return "Каменная Кожа";
 *                 }
 *                 
 *                 @Override
 *                 public String getDescription() {
 *                     return "Игнорирует первый полученный урон";
 *                 }
 *             });
 *         
 *         FalkyeAPI.registerCard(modBus, builder);
 *         
 *         // Регистрируем игровое событие
 *         FalkyeAPI.registerGameEvent(modBus, new GameEvent(
 *             "mysterious_fog",
 *             "Таинственный Туман",
 *             "Сила всех карт дальнего боя уменьшается на 2",
 *             0.1 // 10% вероятность
 *         ) {
 *             @Override
 *             public void execute(FalkyeGameSession session, ServerPlayer player) {
 *                 // Применяем эффект тумана
 *                 // (реализация через систему погоды)
 *             }
 *         });
 *     }
 * }
 */
public class ExampleAddon {
    // Этот класс является только примером и не должен быть скомпилирован
    // Удалите этот файл или переместите его в документацию
}

