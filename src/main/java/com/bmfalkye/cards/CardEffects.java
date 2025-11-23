package com.bmfalkye.cards;

import com.bmfalkye.game.FalkyeGameSession;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Система применения эффектов карт в игре Falkye.
 * 
 * <p>Этот класс обрабатывает все эффекты карт при их разыгрывании, включая:
 * <ul>
 *   <li>Погодные эффекты (мороз, туман, дождь, ясная погода)</li>
 *   <li>Эффекты усиления/ослабления карт</li>
 *   <li>Эффекты лечения и урона</li>
 *   <li>Уникальные способности карт (например, возврат карт из сброса)</li>
 * </ul>
 * 
 * <p><b>Важно:</b> Положительные эффекты (усиление, лечение) применяются только к картам игрока,
 * который использовал способность. Отрицательные эффекты (урон, снижение силы) применяются
 * только к картам оппонента.
 * 
 * @author BeforeMine Team
 * @since 1.0
 */
public class CardEffects {
    
    /**
     * Применяет эффект карты при её разыгрывании.
     * 
     * <p>Метод определяет тип карты и вызывает соответствующий обработчик эффекта:
     * <ul>
     *   <li>{@link Card.CardType#SPELL SPELL} - вызывает {@link #applySpellEffect}</li>
     *   <li>{@link Card.CardType#SPECIAL SPECIAL} - вызывает {@link #applySpecialEffect}</li>
     *   <li>{@link Card.CardType#CREATURE CREATURE} - вызывает {@link #applyCreatureEffect}</li>
     * </ul>
     * 
     * @param session игровая сессия, в которой разыгрывается карта
     * @param player игрок, который разыграл карту
     * @param card карта, эффект которой нужно применить
     * @param row выбранный ряд (для карт способностей, которые работают на конкретный ряд)
     *            может быть {@code null} для карт, не требующих выбора ряда
     */
    public static void applyCardEffect(FalkyeGameSession session, ServerPlayer player, Card card, FalkyeGameSession.CardRow row) {
        com.bmfalkye.util.ModLogger.logCardEffect("Card effect triggered", 
            "player", player != null ? player.getName().getString() : "null",
            "card", card.getName(),
            "cardId", card.getId(),
            "cardType", card.getType().toString(),
            "row", row != null ? row.toString() : "null");
        
        // Проверяем, есть ли способность из API
        com.bmfalkye.api.Ability apiAbility = com.bmfalkye.api.AbilityRegistry.getCardAbility(card.getId());
        if (apiAbility != null) {
            apiAbility.onPlay(session, player, card.getId());
        }
        
        // Примеры эффектов на основе типа карты
        if (card.getType() == Card.CardType.SPELL) {
            applySpellEffect(session, player, card, row);
        } else if (card.getType() == Card.CardType.SPECIAL) {
            applySpecialEffect(session, player, card, row);
        } else if (card.getType() == Card.CardType.CREATURE) {
            applyCreatureEffect(session, player, card, row);
        }
    }
    
    /**
     * Применяет эффект заклинания (SPELL).
     * 
     * <p>Обрабатывает различные типы заклинаний:
     * <ul>
     *   <li>Погодные эффекты: мороз, туман, дождь, ясная погода</li>
     *   <li>Эффекты усиления/ослабления: изменение силы карт в рядах</li>
     *   <li>Эффекты лечения/урона: восстановление или нанесение урона картам</li>
     * </ul>
     * 
     * <p><b>ВАЖНО:</b> Положительные эффекты (усиление, лечение) применяются только к картам
     * игрока (player), отрицательные эффекты (урон, снижение силы) - только к картам
     * оппонента (opponent).
     * 
     * @param session игровая сессия
     * @param player игрок, который разыграл заклинание
     * @param card карта заклинания
     * @param row выбранный ряд (может быть {@code null} для глобальных эффектов)
     */
    private static void applySpellEffect(FalkyeGameSession session, ServerPlayer player, Card card, FalkyeGameSession.CardRow row) {
        com.bmfalkye.util.ModLogger.logCardEffect("Spell effect applied", 
            "player", player != null ? player.getName().getString() : "null",
            "card", card.getName(),
            "cardId", card.getId(),
            "row", row != null ? row.toString() : "null");
        
        String cardId = card.getId();
        String description = card.getDescription().toLowerCase();
        
        // Погодные карты - применяются только к картам оппонента
        if (cardId.equals("weather_frost") || description.contains("мороз") || description.contains("снижает силу всех ближних")) {
            ServerPlayer opponent = getOpponent(session, player);
            if (opponent != null || session.isPlayingWithVillager()) {
                session.playWeatherCard(FalkyeGameSession.WeatherType.FROST);
                // Снижаем силу ближних карт оппонента до 1
                reduceMeleeRowTo1(session, opponent);
                
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§bМороз снижает силу всех ближних карт противника до 1!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§bМороз снижает силу всех ближних карт противника до 1!");
                }
                // Сообщаем оппоненту
                if (opponent != null) {
                    com.bmfalkye.network.NetworkHandler.addActionLog(opponent, 
                        "§cПротивник использовал Мороз! Сила всех ваших ближних карт снижена до 1!");
                    opponent.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§cПротивник использовал Мороз! Сила всех ваших ближних карт снижена до 1!"));
                }
            }
            session.recalculateRoundScore();
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        if (cardId.equals("weather_fog") || description.contains("туман") || description.contains("снижает силу всех дальних")) {
            ServerPlayer opponent = getOpponent(session, player);
            if (opponent != null || session.isPlayingWithVillager()) {
                session.playWeatherCard(FalkyeGameSession.WeatherType.FOG);
                // Снижаем силу дальних карт оппонента до 1
                reduceRangedRowTo1(session, opponent);
                
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§7Туман снижает силу всех дальних карт противника до 1!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§7Туман снижает силу всех дальних карт противника до 1!");
                }
                // Сообщаем оппоненту
                if (opponent != null) {
                    com.bmfalkye.network.NetworkHandler.addActionLog(opponent, 
                        "§cПротивник использовал Туман! Сила всех ваших дальних карт снижена до 1!");
                    opponent.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§cПротивник использовал Туман! Сила всех ваших дальних карт снижена до 1!"));
                }
            }
            session.recalculateRoundScore();
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        if (cardId.equals("weather_rain") || description.contains("дождь") || description.contains("снижает силу всех осадных")) {
            // Дождь применяется только к картам оппонента
            ServerPlayer opponent = getOpponent(session, player);
            if (opponent != null || session.isPlayingWithVillager()) {
                // Устанавливаем погоду (но применяется только к оппоненту)
                session.playWeatherCard(FalkyeGameSession.WeatherType.RAIN);
                // Снижаем силу осадных карт оппонента до 1
                reduceSiegeRowTo1(session, opponent);
                
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§9Дождь снижает силу всех осадных карт противника до 1!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§9Дождь снижает силу всех осадных карт противника до 1!");
                }
                // Сообщаем оппоненту
                if (opponent != null) {
                    com.bmfalkye.network.NetworkHandler.addActionLog(opponent, 
                        "§cПротивник использовал Дождь! Сила всех ваших осадных карт снижена до 1!");
                    opponent.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§cПротивник использовал Дождь! Сила всех ваших осадных карт снижена до 1!"));
                }
            }
            session.recalculateRoundScore();
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        if (cardId.equals("weather_clear") || description.contains("ясная погода") || description.contains("снимает все эффекты погоды")) {
            session.playWeatherCard(FalkyeGameSession.WeatherType.NONE);
            // Сообщаем всем игрокам
            if (player != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§aЯсная погода снимает все эффекты погоды!"));
                com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                    "§aЯсная погода снимает все эффекты погоды!");
            }
            ServerPlayer opponent = getOpponent(session, player);
            if (opponent != null) {
                com.bmfalkye.network.NetworkHandler.addActionLog(opponent, 
                    "§aПротивник использовал Ясную погоду! Все эффекты погоды сняты!");
                opponent.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§aПротивник использовал Ясную погоду! Все эффекты погоды сняты!"));
            }
            session.recalculateRoundScore();
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Огненная буря - наносит урон всем картам противника на поле
        if (cardId.equals("flame_storm") || description.contains("наносит урон всем картам противника") || description.contains("наносит 3 урона всем картам противника")) {
            ServerPlayer opponent = getOpponent(session, player);
            int damage = description.contains("3 урона") ? 3 : 2;
            if (opponent != null) {
                damageAllCardsOnField(session, opponent, damage);
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§cОгненная буря наносит " + damage + " урона всем картам противника на поле!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§cОгненная буря наносит " + damage + " урона всем картам противника!");
                }
            } else if (session.isPlayingWithVillager()) {
                // Для villager
                damageAllCardsOnField(session, null, damage);
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§cОгненная буря наносит " + damage + " урона всем картам противника на поле!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§cОгненная буря наносит " + damage + " урона всем картам противника!");
                }
            }
            // Немедленно обновляем состояние игры для визуального отображения
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Исцеление природы - лечит все карты игрока на поле (если есть что лечить) и усиливает на 2
        if (cardId.equals("nature_heal") || (description.contains("исцеление природы") && description.contains("усиливает все ваши карты"))) {
            int healAmount = 3; // Лечим на 3
            int boost = 2; // Усиливаем на 2
            
            // Проверяем, есть ли карты, которые нужно лечить
            boolean hasCardsToHeal = false;
            List<Card> allCards = new ArrayList<>();
            allCards.addAll(session.getMeleeRow(player));
            allCards.addAll(session.getRangedRow(player));
            allCards.addAll(session.getSiegeRow(player));
            
            for (Card fieldCard : allCards) {
                int currentEffectivePower = session.getEffectivePower(fieldCard, player);
                int basePower = fieldCard.getPower();
                if (currentEffectivePower < basePower) {
                    hasCardsToHeal = true;
                    break;
                }
            }
            
            // Если есть что лечить, лечим
            if (hasCardsToHeal) {
                healAllCardsOnField(session, player, healAmount);
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§aИсцеление природы лечит все ваши карты на " + healAmount + "!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§aИсцеление природы лечит все ваши карты на " + healAmount + "!");
                }
            }
            
            // ВСЕГДА усиливаем все карты (даже если нечего было лечить)
            boostAllCardsOnField(session, player, boost);
            if (player != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§aИсцеление природы усиливает все ваши карты на " + boost + "!"));
                com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                    "§aИсцеление природы усиливает все ваши карты на " + boost + "!");
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Замораживание времени - противник пропускает ход
        if (cardId.equals("time_freeze") || description.contains("противник пропускает")) {
            ServerPlayer opponent = getOpponent(session, player);
            if (opponent != null) {
                // Заставляем противника пропустить ход
                session.pass(opponent);
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§bЗамораживание времени заставляет противника пропустить ход!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§bЗамораживание времени заставляет противника пропустить ход!");
                }
                // Сообщаем оппоненту в лог действий
                com.bmfalkye.network.NetworkHandler.addActionLog(opponent, 
                    "§cПротивник использовал Замораживание времени! Вы пропускаете ход!");
                opponent.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cПротивник использовал Замораживание времени! Вы пропускаете ход!"));
            } else if (session.isPlayingWithVillager()) {
                // Для villager
                session.passVillager();
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§bЗамораживание времени заставляет противника пропустить ход!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§bЗамораживание времени заставляет противника пропустить ход!");
                }
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Шёпот Энтропии - снижает силу всех карт противника на поле
        if (cardId.equals("entropy_whisper") || description.contains("снижает силу всех карт противника")) {
            ServerPlayer opponent = getOpponent(session, player);
            int reduction = description.contains("на 1") ? 1 : 1;
            if (opponent != null) {
                reduceAllCardsOnField(session, opponent, reduction);
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§5Шёпот Энтропии снижает силу всех карт противника на " + reduction + "!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§5Шёпот Энтропии снижает силу всех карт противника на " + reduction + "!");
                }
            } else if (session.isPlayingWithVillager()) {
                reduceAllCardsOnField(session, null, reduction);
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§5Шёпот Энтропии снижает силу всех карт противника на " + reduction + "!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§5Шёпот Энтропии снижает силу всех карт противника на " + reduction + "!");
                }
            }
            // Немедленно обновляем состояние игры для визуального отображения
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Солнечный луч - усиливает все карты игрока на поле на 2
        if (cardId.equals("solar_beam") || (description.contains("солнечный луч") && description.contains("усиливает все ваши карты"))) {
            int boost = description.contains("на 2") ? 2 : 2;
            boostAllCardsOnField(session, player, boost);
            if (player != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§eСолнечный луч усиливает все ваши карты на " + boost + "!"));
                com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                    "§eСолнечный луч усиливает все ваши карты на " + boost + "!");
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Удар магмы - уничтожает самую слабую карту противника
        if (cardId.equals("molten_strike") || description.contains("уничтожает самую слабую карту противника")) {
            ServerPlayer opponent = getOpponent(session, player);
            destroyWeakestCard(session, opponent);
            if (player != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cУдар магмы уничтожает самую слабую карту противника!"));
                com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                    "§cУдар магмы уничтожает самую слабую карту противника!");
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Воспламенение - наносит 2 урона всем картам в ближнем ряду противника
        if (cardId.equals("ignite") || (description.contains("воспламенение") && description.contains("ближний ряд противника"))) {
            ServerPlayer opponent = getOpponent(session, player);
            int damage = description.contains("2 урона") ? 2 : 2;
            if (opponent != null) {
                damageRow(session, opponent, FalkyeGameSession.CardRow.MELEE, damage);
            } else if (session.isPlayingWithVillager()) {
                damageRow(session, null, FalkyeGameSession.CardRow.MELEE, damage);
            }
            if (player != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cВоспламенение наносит " + damage + " урона всем картам в ближнем ряду противника!"));
                com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                    "§cВоспламенение наносит " + damage + " урона всем картам в ближнем ряду противника!");
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Огненный Щит - усиливает все карты ИГРОКА в выбранном ряду на 1
        if (cardId.equals("fire_shield") || (description.contains("огненный щит") && description.contains("усиливает"))) {
            int boost = 1; // Всегда усиливаем на 1
            // Используем выбранный ряд, по умолчанию ближний
            FalkyeGameSession.CardRow targetRow = row != null ? row : FalkyeGameSession.CardRow.MELEE;
            // ВАЖНО: Применяем эффект к картам ИГРОКА (player), который разыграл карту
            boostRow(session, player, targetRow, boost);
            if (player != null) {
                String rowName = switch (targetRow) {
                    case MELEE -> "ближнем";
                    case RANGED -> "дальнем";
                    case SIEGE -> "осадном";
                };
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§6Огненный Щит усиливает все ваши карты в " + rowName + " ряду на " + boost + "!"));
                com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                    "§6Огненный Щит усиливает все ваши карты в " + rowName + " ряду на " + boost + "!");
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Ледяная стрела - наносит 2 урона всем картам в дальнем ряду противника
        if (cardId.equals("frost_bolt") || (description.contains("ледяная стрела") && description.contains("дальний ряд противника"))) {
            ServerPlayer opponent = getOpponent(session, player);
            int damage = description.contains("2 урона") ? 2 : 2;
            if (opponent != null) {
                damageRow(session, opponent, FalkyeGameSession.CardRow.RANGED, damage);
            } else if (session.isPlayingWithVillager()) {
                damageRow(session, null, FalkyeGameSession.CardRow.RANGED, damage);
            }
            if (player != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§bЛедяная стрела наносит " + damage + " урона всем картам в дальнем ряду противника!"));
                com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                    "§bЛедяная стрела наносит " + damage + " урона всем картам в дальнем ряду противника!");
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Щит Пустоты - усиливает все карты ИГРОКА в выбранном ряду на 1
        if (cardId.equals("void_shield") || (description.contains("щит пустоты") && description.contains("усиливает"))) {
            int boost = 1; // Всегда усиливаем на 1
            // Используем выбранный ряд, по умолчанию дальний
            FalkyeGameSession.CardRow targetRow = row != null ? row : FalkyeGameSession.CardRow.RANGED;
            // ВАЖНО: Применяем эффект к картам ИГРОКА (player), который разыграл карту
            boostRow(session, player, targetRow, boost);
            if (player != null) {
                String rowName = switch (targetRow) {
                    case MELEE -> "ближнем";
                    case RANGED -> "дальнем";
                    case SIEGE -> "осадном";
                };
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§5Щит Пустоты усиливает все ваши карты в " + rowName + " ряду на " + boost + "!"));
                com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                    "§5Щит Пустоты усиливает все ваши карты в " + rowName + " ряду на " + boost + "!");
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Песнь Рощи - усиливает все карты игрока на 1
        if (cardId.equals("grove_song") || (description.contains("песнь рощи") && description.contains("усиливает все ваши карты"))) {
            int boost = description.contains("на 1") ? 1 : 1;
            boostAllCardsOnField(session, player, boost);
            if (player != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§aПеснь Рощи усиливает все ваши карты на " + boost + "!"));
                com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                    "§aПеснь Рощи усиливает все ваши карты на " + boost + "!");
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Удар молнии - наносит 2 урона всем картам в осадном ряду противника
        if (cardId.equals("lightning_strike") || (description.contains("удар молнии") && description.contains("осадный ряд противника"))) {
            ServerPlayer opponent = getOpponent(session, player);
            int damage = description.contains("2 урона") ? 2 : 2;
            if (opponent != null) {
                damageRow(session, opponent, FalkyeGameSession.CardRow.SIEGE, damage);
            } else if (session.isPlayingWithVillager()) {
                damageRow(session, null, FalkyeGameSession.CardRow.SIEGE, damage);
            }
            if (player != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§eУдар молнии наносит " + damage + " урона всем картам в осадном ряду противника!"));
                com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                    "§eУдар молнии наносит " + damage + " урона всем картам в осадном ряду противника!");
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Щит Природы - усиливает все карты ИГРОКА в выбранном ряду на 1
        if (cardId.equals("nature_shield") || (description.contains("щит природы") && description.contains("усиливает"))) {
            int boost = 1; // Всегда усиливаем на 1
            // Используем выбранный ряд, по умолчанию осадный
            FalkyeGameSession.CardRow targetRow = row != null ? row : FalkyeGameSession.CardRow.SIEGE;
            // ВАЖНО: Применяем эффект к картам ИГРОКА (player), который разыграл карту
            boostRow(session, player, targetRow, boost);
            if (player != null) {
                String rowName = switch (targetRow) {
                    case MELEE -> "ближнем";
                    case RANGED -> "дальнем";
                    case SIEGE -> "осадном";
                };
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§aЩит Природы усиливает все ваши карты в " + rowName + " ряду на " + boost + "!"));
                com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                    "§aЩит Природы усиливает все ваши карты в " + rowName + " ряду на " + boost + "!");
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Разлом Пустоты - возвращает случайную карту из сброса в руку (SPELL карта)
        if (cardId.equals("void_rift") || (description.contains("разлом пустоты") && description.contains("возвращает случайную карту из сброса"))) {
            if (player != null) {
                List<Card> graveyard = session.getGraveyard(player);
                List<Card> hand = session.getHand(player);
                
                if (!graveyard.isEmpty()) {
                    Card randomCard = graveyard.get(new java.util.Random().nextInt(graveyard.size()));
                    hand.add(randomCard);
                    graveyard.remove(randomCard);
                    
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§bРазлом Пустоты вернул карту: §f" + randomCard.getName()));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§bРазлом Пустоты вернул карту: " + randomCard.getName());
                } else {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§7В сбросе нет карт для возврата"));
                }
            } else if (session.isPlayingWithVillager()) {
                List<Card> graveyard = session.getGraveyard(null);
                List<Card> hand = session.getHand(null);
                
                if (!graveyard.isEmpty()) {
                    Card randomCard = graveyard.get(new java.util.Random().nextInt(graveyard.size()));
                    hand.add(randomCard);
                    graveyard.remove(randomCard);
                    
                    if (session.getPlayer1() != null) {
                        com.bmfalkye.network.NetworkHandler.addActionLog(session.getPlayer1(), 
                            "§7Противник вернул карту из сброса");
                    }
                }
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Призыв Мировой Души - возвращает случайную карту из сброса ИГРОКА на поле
        if (cardId.equals("world_soul_call") || (description.contains("призыв мировой души") && description.contains("возвращает карту из сброса"))) {
            if (player != null) {
                // Получаем сброс игрока
                List<Card> graveyard = session.getGraveyard(player);
                
                if (!graveyard.isEmpty()) {
                    // Выбираем случайную карту из сброса игрока
                    Card randomCard = graveyard.get(new java.util.Random().nextInt(graveyard.size()));
                    
                    // Удаляем из сброса
                    graveyard.remove(randomCard);
                    
                    // Размещаем карту на поле игрока в случайный ряд (или ближний по умолчанию)
                    FalkyeGameSession.CardRow targetRow = row != null ? row : FalkyeGameSession.CardRow.MELEE;
                    List<Card> targetRowList = switch (targetRow) {
                        case MELEE -> session.getMeleeRow(player);
                        case RANGED -> session.getRangedRow(player);
                        case SIEGE -> session.getSiegeRow(player);
                    };
                    targetRowList.add(randomCard);
                    
                    // Применяем эффект карты, если она существо
                    if (randomCard.getType() == Card.CardType.CREATURE) {
                        applyCardEffect(session, player, randomCard, targetRow);
                    }
                    
                    session.recalculateRoundScore();
                    
                    String rowName = switch (targetRow) {
                        case MELEE -> "ближний бой";
                        case RANGED -> "дальний бой";
                        case SIEGE -> "осада";
                    };
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§aПризыв Мировой Души вернул карту §f" + randomCard.getName() + " §aна поле в " + rowName + "!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§aПризыв Мировой Души вернул карту " + randomCard.getName() + " на поле!");
                } else {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§7В сбросе нет карт для возврата"));
                }
            } else if (session.isPlayingWithVillager()) {
                // Для villager
                List<Card> graveyard = session.getGraveyard(null);
                
                if (!graveyard.isEmpty()) {
                    Card randomCard = graveyard.get(new java.util.Random().nextInt(graveyard.size()));
                    graveyard.remove(randomCard);
                    
                    FalkyeGameSession.CardRow targetRow = row != null ? row : FalkyeGameSession.CardRow.MELEE;
                    List<Card> targetRowList = switch (targetRow) {
                        case MELEE -> session.getMeleeRow(null);
                        case RANGED -> session.getRangedRow(null);
                        case SIEGE -> session.getSiegeRow(null);
                    };
                    targetRowList.add(randomCard);
                    
                    if (randomCard.getType() == Card.CardType.CREATURE) {
                        applyCardEffect(session, null, randomCard, targetRow);
                    }
                    
                    session.recalculateRoundScore();
                    
                    if (session.getPlayer1() != null) {
                        com.bmfalkye.network.NetworkHandler.addActionLog(session.getPlayer1(), 
                            "§7Противник вернул карту из сброса на поле");
                    }
                }
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Обработка по умолчанию для неизвестных SPELL карт - логируем предупреждение
        com.bmfalkye.util.ModLogger.logCardEffect("WARNING: Unknown SPELL card effect", 
            "player", player != null ? player.getName().getString() : "null",
            "cardId", cardId,
            "cardName", card.getName(),
            "description", description);
        if (player != null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§7Заклинание не имеет эффекта: " + card.getName()));
        }
        updateGameStateAfterEffect(session, player);
    }
    
    /**
     * Применяет эффект особой карты (SPECIAL)
     * ВАЖНО: Положительные эффекты применяются к картам игрока (player), отрицательные - к картам оппонента (opponent)
     */
    private static void applySpecialEffect(FalkyeGameSession session, ServerPlayer player, Card card, FalkyeGameSession.CardRow row) {
        com.bmfalkye.util.ModLogger.logCardEffect("Special effect applied", 
            "player", player != null ? player.getName().getString() : "null",
            "card", card.getName(),
            "cardId", card.getId(),
            "row", row != null ? row.toString() : "null");
        
        String cardId = card.getId();
        String description = card.getDescription().toLowerCase();
        
        // Пробуждение Мировой Души - возвращает все карты из сброса в руку
        // Включает все карты: способности (SPELL, SPECIAL) и обычные карты (CREATURE), которые были уничтожены
        if (cardId.equals("world_soul_awakening") || description.contains("возвращает все карты из сброса в руку")) {
            if (player != null) {
                // Получаем сброс игрока
                List<Card> graveyard = session.getGraveyard(player);
                List<Card> hand = session.getHand(player);
                
                // Также получаем карты с поля игрока (если они есть) - они тоже считаются "сыгранными"
                List<Card> fieldCards = new ArrayList<>();
                fieldCards.addAll(session.getMeleeRow(player));
                fieldCards.addAll(session.getRangedRow(player));
                fieldCards.addAll(session.getSiegeRow(player));
                
                // Возвращаем все карты из сброса в руку (включая все типы карт)
                int returnedCount = 0;
                for (Card graveyardCard : new ArrayList<>(graveyard)) {
                    // Возвращаем ВСЕ карты, независимо от типа (SPELL, SPECIAL, CREATURE)
                    hand.add(graveyardCard);
                    graveyard.remove(graveyardCard);
                    returnedCount++;
                }
                
                // Также возвращаем карты с поля в руку (если они есть)
                // Это позволяет вернуть не только карты способностей из сброса, но и обычные карты с поля
                for (Card fieldCard : new ArrayList<>(fieldCards)) {
                    // Удаляем карту с поля
                    session.getMeleeRow(player).remove(fieldCard);
                    session.getRangedRow(player).remove(fieldCard);
                    session.getSiegeRow(player).remove(fieldCard);
                    // Модификаторы силы не нужно удалять - они не будут применяться, так как карта не на поле
                    // Добавляем в руку (не в сброс!)
                    hand.add(fieldCard);
                    returnedCount++;
                }
                
                // Пересчитываем очки после удаления карт с поля
                if (!fieldCards.isEmpty()) {
                    session.recalculateRoundScore();
                }
                
                if (returnedCount > 0) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§aПробуждение Мировой Души вернуло " + returnedCount + " карт в руку!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§aПробуждение Мировой Души вернуло " + returnedCount + " карт в руку");
                } else {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§7В сбросе нет карт для возврата"));
                }
                
                // Немедленно обновляем состояние игры для визуального отображения
                updateGameStateAfterEffect(session, player);
            } else if (session.isPlayingWithVillager()) {
                // Для villager
                List<Card> graveyard = session.getGraveyard(null);
                List<Card> hand = session.getHand(null);
                
                // Также получаем карты с поля villager
                List<Card> fieldCards = new ArrayList<>();
                fieldCards.addAll(session.getMeleeRow(null));
                fieldCards.addAll(session.getRangedRow(null));
                fieldCards.addAll(session.getSiegeRow(null));
                
                int returnedCount = 0;
                for (Card graveyardCard : new ArrayList<>(graveyard)) {
                    hand.add(graveyardCard);
                    graveyard.remove(graveyardCard);
                    returnedCount++;
                }
                
                // Возвращаем карты с поля в руку
                // Это позволяет вернуть не только карты способностей из сброса, но и обычные карты с поля
                for (Card fieldCard : new ArrayList<>(fieldCards)) {
                    session.getMeleeRow(null).remove(fieldCard);
                    session.getRangedRow(null).remove(fieldCard);
                    session.getSiegeRow(null).remove(fieldCard);
                    // Модификаторы силы не нужно удалять - они не будут применяться, так как карта не на поле
                    // Добавляем в руку (не в сброс!)
                    hand.add(fieldCard);
                    returnedCount++;
                }
                
                // Пересчитываем очки после удаления карт с поля
                if (!fieldCards.isEmpty()) {
                    session.recalculateRoundScore();
                }
                
                if (returnedCount > 0 && session.getPlayer1() != null) {
                    com.bmfalkye.network.NetworkHandler.addActionLog(session.getPlayer1(), 
                        "§7Противник вернул " + returnedCount + " карт из сброса");
                }
                
                // Немедленно обновляем состояние игры
                updateGameStateAfterEffect(session, null);
            }
            return;
        }
        
        // Прозрение Дозора - показывает 3 случайные карты из руки противника
        if (cardId.equals("watcher_insight") || (description.contains("прозрение дозора") && description.contains("показывает")) || (description.contains("показывает") && description.contains("карты оппонента"))) {
            ServerPlayer opponent = getOpponent(session, player);
            if (opponent != null || session.isPlayingWithVillager()) {
                // Получаем руку противника (для показа карт)
                List<Card> opponentHand = session.getHand(opponent);
                if (opponentHand.isEmpty() && session.isPlayingWithVillager()) {
                    opponentHand = session.getHand(null);
                }
                
                if (opponentHand.size() > 0) {
                    // Получаем 3 случайные карты из руки противника
                    List<Card> opponentHandCopy = new ArrayList<>(opponentHand);
                    java.util.Collections.shuffle(opponentHandCopy);
                    int cardsToShow = Math.min(3, opponentHandCopy.size());
                    List<String> shownCardIds = new ArrayList<>();
                    for (int i = 0; i < cardsToShow; i++) {
                        shownCardIds.add(opponentHandCopy.get(i).getId());
                    }
                    
                    // Сохраняем показанные карты в сессии для визуального отображения
                    if (player != null) {
                        session.setRevealedCards(player, shownCardIds);
                        
                        StringBuilder cardNames = new StringBuilder();
                        for (int i = 0; i < cardsToShow; i++) {
                            if (i > 0) cardNames.append(", ");
                            cardNames.append(opponentHandCopy.get(i).getName());
                        }
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§bПрозрение Дозора показывает карты противника: §f" + cardNames.toString()));
                        com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                            "§bПрозрение Дозора показывает " + cardsToShow + " карт противника");
                        
                        // Обновляем состояние игры, чтобы показанные карты отобразились на клиенте
                        com.bmfalkye.network.NetworkHandler.updateGameStateImmediate(player, session);
                    }
                } else {
                    if (player != null) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§7У противника нет карт в руке"));
                    }
                }
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Благословение Рощи - лечит все ваши карты на 3 и усиливает их на 1 (если нечего лечить, просто усиливает)
        if (cardId.equals("grove_blessing") || (description.contains("благословение рощи") && description.contains("лечит все ваши карты"))) {
            int healAmount = 3; // Лечим на 3
            int boost = 1; // Усиливаем на 1
            
            // Проверяем, есть ли карты, которые нужно лечить
            boolean hasCardsToHeal = false;
            List<Card> allCards = new ArrayList<>();
            allCards.addAll(session.getMeleeRow(player));
            allCards.addAll(session.getRangedRow(player));
            allCards.addAll(session.getSiegeRow(player));
            
            for (Card fieldCard : allCards) {
                int currentEffectivePower = session.getEffectivePower(fieldCard, player);
                int basePower = fieldCard.getPower();
                if (currentEffectivePower < basePower) {
                    hasCardsToHeal = true;
                    break;
                }
            }
            
            // Если есть что лечить, лечим и усиливаем
            if (hasCardsToHeal) {
                healAllCardsOnField(session, player, healAmount);
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§aБлагословение Рощи лечит все ваши карты на " + healAmount + "!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§aБлагословение Рощи лечит все ваши карты на " + healAmount + "!");
                }
            }
            
            // Всегда усиливаем (даже если нечего было лечить)
            boostAllCardsOnField(session, player, boost);
            if (player != null) {
                if (hasCardsToHeal) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§aБлагословение Рощи усиливает все ваши карты на " + boost + "!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§aБлагословение Рощи усиливает все ваши карты на " + boost + "!");
                } else {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§aБлагословение Рощи усиливает все ваши карты на " + boost + "!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§aБлагословение Рощи усиливает все ваши карты на " + boost + "!");
                }
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // ========== НОВЫЕ ЭФФЕКТЫ ДЛЯ РАСШИРЕННЫХ КАРТ ==========
        
        // Ритуал Инферно - усиливает все карты Дома Пламени на 3
        if (cardId.equals("inferno_ritual") || description.contains("ритуал инферно")) {
            int boost = 3;
            boostFactionCards(session, player, "Дом Пламени", boost);
            if (player != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cРитуал Инферно усиливает все карты Дома Пламени на " + boost + "!"));
                com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                    "§cРитуал Инферно усиливает все карты Дома Пламени на " + boost + "!");
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Огненный Барьер - защищает все карты от следующего урона (усиливает на 5 временно)
        if (cardId.equals("flame_barrier") || description.contains("огненный барьер")) {
            int boost = 5;
            boostAllCardsOnField(session, player, boost);
            if (player != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§6Огненный Барьер защищает все ваши карты!"));
                com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                    "§6Огненный Барьер защищает все ваши карты!");
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Расплавленное Ядро - удваивает силу всех карт в ближнем ряду
        if (cardId.equals("molten_core") || description.contains("расплавленное ядро") || description.contains("удваивает силу всех ваших карт в ближнем ряду")) {
            List<Card> melee = session.getMeleeRow(player);
            // Создаём копию списка для безопасной итерации
            List<Card> meleeCopy = new ArrayList<>(melee);
            for (Card meleeCard : meleeCopy) {
                // Проверяем, что карта всё ещё в ряду
                if (melee.contains(meleeCard)) {
                    int currentPower = session.getEffectivePower(meleeCard, player);
                    int boost = currentPower; // Удваиваем силу
                    // ВАЖНО: Передаем игрока, чтобы модификатор применялся только к его картам
                    session.addPowerModifier(meleeCard, boost, player);
                }
            }
            session.recalculateRoundScore();
            if (player != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cРасплавленное Ядро удваивает силу всех карт в ближнем ряду!"));
                com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                    "§cРасплавленное Ядро удваивает силу всех карт в ближнем ряду!");
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Временной Сдвиг - возвращает случайную карту противника в руку
        if (cardId.equals("temporal_shift") || description.contains("временной сдвиг")) {
            ServerPlayer opponent = getOpponent(session, player);
            if (opponent != null || session.isPlayingWithVillager()) {
                List<Card> opponentField = new ArrayList<>();
                if (opponent != null) {
                    opponentField.addAll(session.getMeleeRow(opponent));
                    opponentField.addAll(session.getRangedRow(opponent));
                    opponentField.addAll(session.getSiegeRow(opponent));
                } else {
                    opponentField.addAll(session.getMeleeRow(null));
                    opponentField.addAll(session.getRangedRow(null));
                    opponentField.addAll(session.getSiegeRow(null));
                }
                
                if (!opponentField.isEmpty()) {
                    Card randomCard = opponentField.get(new java.util.Random().nextInt(opponentField.size()));
                    // Удаляем с поля
                    if (opponent != null) {
                        session.removeCardFromField(opponent, randomCard);
                        List<Card> opponentHand = session.getHand(opponent);
                        opponentHand.add(randomCard);
                    } else {
                        session.getMeleeRow(null).remove(randomCard);
                        session.getRangedRow(null).remove(randomCard);
                        session.getSiegeRow(null).remove(randomCard);
                        session.getHand(null).add(randomCard);
                    }
                    
                    if (player != null) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§bВременной Сдвиг вернул карту противника: §f" + randomCard.getName()));
                        com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                            "§bВременной Сдвиг вернул карту противника: " + randomCard.getName());
                    }
                }
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Эхо Пустоты - копирует эффект последней сыгранной карты способности
        if (cardId.equals("void_echo") || description.contains("эхо пустоты") || description.contains("копирует эффект последней сыгранной карты способности")) {
            // Упрощённая версия - усиливает все карты на 2 (имитация копирования эффекта)
            // В будущем можно добавить отслеживание последней сыгранной карты способности
            boostAllCardsOnField(session, player, 2);
            if (player != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§5Эхо Пустоты копирует эффект последней карты способности!"));
                com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                    "§5Эхо Пустоты копирует эффект последней карты способности!");
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Печать Знания - показывает все карты в руке противника
        if (cardId.equals("knowledge_seal") || description.contains("печать знания")) {
            ServerPlayer opponent = getOpponent(session, player);
            if (opponent != null || session.isPlayingWithVillager()) {
                List<Card> opponentHand = session.getHand(opponent);
                if (opponentHand.isEmpty() && session.isPlayingWithVillager()) {
                    opponentHand = session.getHand(null);
                }
                
                if (!opponentHand.isEmpty()) {
                    List<String> allCardIds = new ArrayList<>();
                    for (Card handCard : opponentHand) {
                        allCardIds.add(handCard.getId());
                    }
                    
                    if (player != null) {
                        session.setRevealedCards(player, allCardIds);
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§bПечать Знания показывает все карты противника!"));
                        com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                            "§bПечать Знания показывает все карты противника!");
                        com.bmfalkye.network.NetworkHandler.updateGameStateImmediate(player, session);
                    }
                }
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Гнев Природы - наносит урон равный количеству ваших карт на поле
        if (cardId.equals("nature_wrath") || description.contains("гнев природы")) {
            ServerPlayer opponent = getOpponent(session, player);
            int cardCount = session.getMeleeRow(player).size() + 
                          session.getRangedRow(player).size() + 
                          session.getSiegeRow(player).size();
            int damage = cardCount;
            
            if (opponent != null) {
                damageAllCardsOnField(session, opponent, damage);
            } else if (session.isPlayingWithVillager()) {
                damageAllCardsOnField(session, null, damage);
            }
            
            if (player != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§aГнев Природы наносит " + damage + " урона всем картам противника!"));
                com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                    "§aГнев Природы наносит " + damage + " урона всем картам противника!");
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Пробуждение Рощи - усиливает все карты Детей Рощения на 2 и лечит их на 2 (если есть что лечить)
        if (cardId.equals("grove_awakening") || description.contains("пробуждение рощи")) {
            int boost = 2;
            int heal = 2;
            
            // ВСЕГДА усиливаем карты фракции
            boostFactionCards(session, player, "Дети Рощения", boost);
            
            // Проверяем, есть ли карты, которые нужно лечить
            boolean hasCardsToHeal = false;
            List<Card> allCards = new ArrayList<>();
            allCards.addAll(session.getMeleeRow(player));
            allCards.addAll(session.getRangedRow(player));
            allCards.addAll(session.getSiegeRow(player));
            
            for (Card fieldCard : allCards) {
                if (fieldCard.getFaction().equals("Дети Рощения")) {
                    int currentEffectivePower = session.getEffectivePower(fieldCard, player);
                    int basePower = fieldCard.getPower();
                    if (currentEffectivePower < basePower) {
                        hasCardsToHeal = true;
                        break;
                    }
                }
            }
            
            // Если есть что лечить, лечим карты фракции
            if (hasCardsToHeal) {
                healFactionCards(session, player, "Дети Рощения", heal);
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§aПробуждение Рощи усиливает и лечит все карты Детей Рощения!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§aПробуждение Рощи усиливает и лечит все карты Детей Рощения!");
                }
            } else {
                // Если нечего лечить, только усиливаем
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§aПробуждение Рощи усиливает все карты Детей Рощения на " + boost + "!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§aПробуждение Рощи усиливает все карты Детей Рощения на " + boost + "!");
                }
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Разлом Измерения - обменивает случайную карту из руки на карту из колоды противника
        if (cardId.equals("dimension_rift") || description.contains("разлом измерения")) {
            // Упрощённая версия - возвращает случайную карту из сброса
            if (player != null) {
                List<Card> graveyard = session.getGraveyard(player);
                List<Card> hand = session.getHand(player);
                
                if (!graveyard.isEmpty() && !hand.isEmpty()) {
                    Card randomFromHand = hand.get(new java.util.Random().nextInt(hand.size()));
                    Card randomFromGraveyard = graveyard.get(new java.util.Random().nextInt(graveyard.size()));
                    
                    hand.remove(randomFromHand);
                    graveyard.remove(randomFromGraveyard);
                    hand.add(randomFromGraveyard);
                    graveyard.add(randomFromHand);
                    
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§5Разлом Измерения обменял карты!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§5Разлом Измерения обменял карты!");
                }
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Весы Равновесия - выравнивает очки обоих игроков до среднего значения
        if (cardId.equals("balance_scale") || description.contains("весы равновесия")) {
            // Упрощённая версия - усиливаем карты игрока, если он проигрывает
            if (player != null) {
                int playerScore = session.getRoundScore(player);
                ServerPlayer opponent = getOpponent(session, player);
                int opponentScore = opponent != null ? session.getRoundScore(opponent) : (session.isPlayingWithVillager() ? session.getRoundScore(null) : 0);
                
                if (opponentScore > 0 && playerScore < opponentScore) {
                    int difference = opponentScore - playerScore;
                    int boost = difference / 4; // Усиливаем на четверть разницы
                    if (boost > 0) {
                        boostAllCardsOnField(session, player, boost);
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§7Весы Равновесия выравнивают очки! Ваши карты усилены на " + boost));
                        com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                            "§7Весы Равновесия выравнивают очки!");
                    }
                }
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Безмолвный Голод - иммунный ответ реальности (переработано: усиливает все ваши карты на 2, но снижает силу всех карт противника на 1)
        if (cardId.equals("silent_hunger") || description.contains("безмолвный голод") || description.contains("иммунный ответ реальности")) {
            // Сначала усиливаем карты игрока на 2
            boostAllCardsOnField(session, player, 2);
            // Затем снижаем силу всех карт противника на 1
            ServerPlayer opponent = getOpponent(session, player);
            if (opponent != null) {
                reduceAllCardsOnField(session, opponent, 1);
            } else if (session.isPlayingWithVillager()) {
                reduceAllCardsOnField(session, null, 1);
            }
            if (player != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§5Безмолвный Голод усиливает все ваши карты на 2 и ослабляет противника на 1!"));
                com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                    "§5Безмолвный Голод усиливает все ваши карты на 2 и ослабляет противника на 1!");
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Трещина Реальности - разлом в матрице бытия (переработано: снижает силу всех карт противника на 2, но усиливает случайную вашу карту на 3)
        if (cardId.equals("reality_crack") || description.contains("трещина реальности") || description.contains("разлом в матрице бытия")) {
            // Сначала снижаем силу всех карт противника на 2
            ServerPlayer opponent = getOpponent(session, player);
            if (opponent != null) {
                reduceAllCardsOnField(session, opponent, 2);
            } else if (session.isPlayingWithVillager()) {
                reduceAllCardsOnField(session, null, 2);
            }
            // Затем усиливаем случайную карту игрока на 3
            List<Card> allPlayerCards = new ArrayList<>();
            allPlayerCards.addAll(session.getMeleeRow(player));
            allPlayerCards.addAll(session.getRangedRow(player));
            allPlayerCards.addAll(session.getSiegeRow(player));
            if (!allPlayerCards.isEmpty()) {
                Card randomCard = allPlayerCards.get(new java.util.Random().nextInt(allPlayerCards.size()));
                // ВАЖНО: Передаем игрока, чтобы модификатор применялся только к его картам
                session.addPowerModifier(randomCard, 3, player);
                session.recalculateRoundScore();
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§cТрещина Реальности ослабляет противника на 2 и усиливает карту §f" + randomCard.getName() + " §cна 3!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§cТрещина Реальности ослабляет противника на 2 и усиливает карту " + randomCard.getName() + " на 3!");
                }
            } else {
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§cТрещина Реальности ослабляет противника на 2!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§cТрещина Реальности ослабляет противника на 2!");
                }
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Пироритуал - возвращает случайную карту из сброса в руку
        if (cardId.equals("pyro_ritual") || description.contains("пироритуал")) {
            if (player != null) {
                List<Card> graveyard = session.getGraveyard(player);
                List<Card> hand = session.getHand(player);
                
                if (!graveyard.isEmpty()) {
                    Card randomCard = graveyard.get(new java.util.Random().nextInt(graveyard.size()));
                    hand.add(randomCard);
                    graveyard.remove(randomCard);
                    
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§cПироритуал вернул карту: §f" + randomCard.getName()));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§cПироритуал вернул карту: " + randomCard.getName());
                } else {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§7В сбросе нет карт для возврата"));
                }
            } else if (session.isPlayingWithVillager()) {
                List<Card> graveyard = session.getGraveyard(null);
                List<Card> hand = session.getHand(null);
                
                if (!graveyard.isEmpty()) {
                    Card randomCard = graveyard.get(new java.util.Random().nextInt(graveyard.size()));
                    hand.add(randomCard);
                    graveyard.remove(randomCard);
                    
                    if (session.getPlayer1() != null) {
                        com.bmfalkye.network.NetworkHandler.addActionLog(session.getPlayer1(), 
                            "§7Противник вернул карту из сброса");
                    }
                }
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Благословение Кузни - усиливает случайную карту на поле на 5
        if (cardId.equals("forge_blessing") || description.contains("благословение кузни")) {
            List<Card> allPlayerCards = new ArrayList<>();
            allPlayerCards.addAll(session.getMeleeRow(player));
            allPlayerCards.addAll(session.getRangedRow(player));
            allPlayerCards.addAll(session.getSiegeRow(player));
            
            if (!allPlayerCards.isEmpty()) {
                Card randomCard = allPlayerCards.get(new java.util.Random().nextInt(allPlayerCards.size()));
                // ВАЖНО: Передаем игрока, чтобы модификатор применялся только к его картам
                session.addPowerModifier(randomCard, 5, player);
                session.recalculateRoundScore();
                
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§6Благословение Кузни усилило карту §f" + randomCard.getName() + " §6на 5!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§6Благословение Кузни усилило карту " + randomCard.getName() + " на 5!");
                }
            } else {
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§7На поле нет карт для усиления"));
                }
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Парадокс Времени - возвращает все карты из сброса в колоду и перемешивает (упрощённо: возвращает в руку)
        if (cardId.equals("time_paradox") || description.contains("парадокс времени")) {
            if (player != null) {
                List<Card> graveyard = session.getGraveyard(player);
                List<Card> hand = session.getHand(player);
                
                if (!graveyard.isEmpty()) {
                    int returnedCount = 0;
                    for (Card graveyardCard : new ArrayList<>(graveyard)) {
                        hand.add(graveyardCard);
                        graveyard.remove(graveyardCard);
                        returnedCount++;
                    }
                    
                    // Перемешиваем руку для эффекта перемешивания
                    java.util.Collections.shuffle(hand);
                    
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§bПарадокс Времени вернул " + returnedCount + " карт в руку!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§bПарадокс Времени вернул " + returnedCount + " карт в руку!");
                } else {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§7В сбросе нет карт для возврата"));
                }
            } else if (session.isPlayingWithVillager()) {
                List<Card> graveyard = session.getGraveyard(null);
                List<Card> hand = session.getHand(null);
                
                if (!graveyard.isEmpty()) {
                    for (Card graveyardCard : new ArrayList<>(graveyard)) {
                        hand.add(graveyardCard);
                        graveyard.remove(graveyardCard);
                    }
                    java.util.Collections.shuffle(hand);
                    
                    if (session.getPlayer1() != null) {
                        com.bmfalkye.network.NetworkHandler.addActionLog(session.getPlayer1(), 
                            "§7Противник вернул карты из сброса");
                    }
                }
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Обработка по умолчанию для неизвестных карт - логируем предупреждение
        com.bmfalkye.util.ModLogger.logCardEffect("WARNING: Unknown SPECIAL card effect", 
            "player", player != null ? player.getName().getString() : "null",
            "cardId", cardId,
            "cardName", card.getName(),
            "description", description);
        if (player != null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§7Карта способности не имеет эффекта: " + card.getName()));
        }
        updateGameStateAfterEffect(session, player);
    }
    
    /**
     * Усиливает все карты определённой фракции на поле
     */
    private static void boostFactionCards(FalkyeGameSession session, ServerPlayer player, String faction, int boost) {
        List<Card> allCards = new ArrayList<>();
        allCards.addAll(session.getMeleeRow(player));
        allCards.addAll(session.getRangedRow(player));
        allCards.addAll(session.getSiegeRow(player));
        
        for (Card card : allCards) {
            if (card.getFaction().equals(faction)) {
                // ВАЖНО: Передаем игрока, чтобы модификатор применялся только к его картам
                session.addPowerModifier(card, boost, player);
            }
        }
        session.recalculateRoundScore();
    }
    
    /**
     * Лечит все карты определённой фракции на поле
     */
    private static void healFactionCards(FalkyeGameSession session, ServerPlayer player, String faction, int heal) {
        List<Card> allCards = new ArrayList<>();
        allCards.addAll(session.getMeleeRow(player));
        allCards.addAll(session.getRangedRow(player));
        allCards.addAll(session.getSiegeRow(player));
        
        for (Card card : allCards) {
            if (card.getFaction().equals(faction)) {
                int currentPower = session.getEffectivePower(card, player);
                int basePower = card.getPower();
                if (currentPower < basePower) {
                    int actualHeal = Math.min(heal, basePower - currentPower);
                    // ВАЖНО: Передаем игрока, чтобы модификатор применялся только к его картам
                    session.addPowerModifier(card, actualHeal, player);
                }
            }
        }
        session.recalculateRoundScore();
    }
    
    /**
     * Применяет эффект существа (CREATURE)
     * ВАЖНО: Положительные эффекты применяются к картам игрока (player), отрицательные - к картам оппонента (opponent)
     */
    private static void applyCreatureEffect(FalkyeGameSession session, ServerPlayer player, Card card, FalkyeGameSession.CardRow row) {
        String cardId = card.getId();
        String description = card.getDescription().toLowerCase();
        
        com.bmfalkye.util.ModLogger.logCardEffect("Creature effect applied", 
            "player", player != null ? player.getName().getString() : "null",
            "card", card.getName(),
            "cardId", cardId,
            "row", row != null ? row.toString() : "null");
        
        // Глацис, Хранитель Порога - замораживает все карты противника (переделано: снижает силу всех карт противника на 2)
        if (cardId.equals("ice_dragon_glacis") || (description.contains("глацис") && description.contains("замораживает"))) {
            ServerPlayer opponent = getOpponent(session, player);
            if (opponent != null || session.isPlayingWithVillager()) {
                // Снижаем силу всех карт противника на 2
                reduceAllCardsOnField(session, opponent, 2);
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§bГлацис снижает силу всех карт противника на 2!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§bГлацис снижает силу всех карт противника на 2!");
                }
                if (opponent != null) {
                    com.bmfalkye.network.NetworkHandler.addActionLog(opponent, 
                        "§cПротивник сыграл Глациса! Сила всех ваших карт снижена на 2!");
                    opponent.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§cПротивник сыграл Глациса! Сила всех ваших карт снижена на 2!"));
                }
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Древнее Дерево - лечит все ваши карты на 3
        if (cardId.equals("ancient_tree") || (description.contains("древнее дерево") && description.contains("лечит"))) {
            healAllCardsOnField(session, player, 3);
            if (player != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§aДревнее Дерево лечит все ваши карты на 3!"));
                com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                    "§aДревнее Дерево лечит все ваши карты на 3!");
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Эльф Света - усиливает соседние карты на +1
        if (cardId.equals("light_elf") || (description.contains("эльф света") && description.contains("усиливает соседние"))) {
            // Используем выбранный ряд, если он передан, иначе получаем ряд, в который была сыграна карта
            FalkyeGameSession.CardRow targetRow = row != null ? row : getCardRow(session, player, card);
            if (targetRow != null) {
                boostRow(session, player, targetRow, 1);
                if (player != null) {
                    String rowName = switch (targetRow) {
                        case MELEE -> "ближнем";
                        case RANGED -> "дальнем";
                        case SIEGE -> "осадном";
                    };
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§aЭльф Света усиливает все карты в " + rowName + " ряду на 1!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§aЭльф Света усиливает все карты в " + rowName + " ряду на 1!");
                }
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Фульгур, Громовой Скипетр - усиливает все карты Детей Рощения на поле на +2
        // Проверяем, что это именно Фульгур и что он должен усиливать только карты Детей Рощения
        if (cardId.equals("lightning_dragon_fulgur")) {
            // Проверяем описание - если там указано "Детей Рощения", усиливаем только их
            if (description.contains("детей рощения") || description.contains("усиливает все карты детей рощения")) {
                boostFactionCards(session, player, "Дети Рощения", 2);
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§eФульгур усиливает все карты Детей Рощения на поле на 2!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§eФульгур усиливает все карты Детей Рощения на поле на 2!");
                }
            } else {
                // Старая логика для обратной совместимости - усиливаем все карты
                boostAllCardsOnField(session, player, 2);
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§eФульгур усиливает все ваши карты на поле на 2!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§eФульгур усиливает все ваши карты на поле на 2!");
                }
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Кристальный Змей - замедляет противника (переделано: снижает силу всех карт противника на 1)
        if (cardId.equals("crystal_serpent") || (description.contains("кристальный змей") && description.contains("замедляет"))) {
            ServerPlayer opponent = getOpponent(session, player);
            if (opponent != null || session.isPlayingWithVillager()) {
                reduceAllCardsOnField(session, opponent, 1);
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§bКристальный Змей снижает силу всех карт противника на 1!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§bКристальный Змей снижает силу всех карт противника на 1!");
                }
                if (opponent != null) {
                    com.bmfalkye.network.NetworkHandler.addActionLog(opponent, 
                        "§cПротивник сыграл Кристального Змея! Сила всех ваших карт снижена на 1!");
                    opponent.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§cПротивник сыграл Кристального Змея! Сила всех ваших карт снижена на 1!"));
                }
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Певун Деревьев - лечит все карты на 1 при разыгрывании
        if (cardId.equals("tree_singer") || (description.contains("певун деревьев") && description.contains("лечит все карты"))) {
            healAllCardsOnField(session, player, 1);
            if (player != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§aПевун Деревьев лечит все ваши карты на 1!"));
                com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                    "§aПевун Деревьев лечит все ваши карты на 1!");
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Страж Чертогов - усиливает все карты в дальнем ряду на +1
        if (cardId.equals("library_guardian") || (description.contains("страж чертогов") && description.contains("дальнем ряду"))) {
            boostRow(session, player, FalkyeGameSession.CardRow.RANGED, 1);
            if (player != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§aСтраж Чертогов усиливает все карты в дальнем ряду на 1!"));
                com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                    "§aСтраж Чертогов усиливает все карты в дальнем ряду на 1!");
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Хранитель Рощи - усиливает все карты в осадном ряду на +1
        if (cardId.equals("grove_keeper") || (description.contains("хранитель рощи") && description.contains("осадном ряду"))) {
            boostRow(session, player, FalkyeGameSession.CardRow.SIEGE, 1);
            if (player != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§aХранитель Рощи усиливает все карты в осадном ряду на 1!"));
                com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                    "§aХранитель Рощи усиливает все карты в осадном ряду на 1!");
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Игнисар, Вечный Горн - усиливает все карты Дома Пламени на поле на +2
        if (cardId.equals("fire_dragon_ignisar") || (description.contains("игнисар") && description.contains("усиливает все карты дома пламени"))) {
            boostFactionCards(session, player, "Дом Пламени", 2);
            if (player != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cИгнисар усиливает все карты Дома Пламени на поле на 2!"));
                com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                    "§cИгнисар усиливает все карты Дома Пламени на поле на 2!");
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Пирофеникс - наносит 3 урона случайной карте противника
        if (cardId.equals("pyro_phoenix") || (description.contains("пирофеникс") && description.contains("наносит 3 урона"))) {
            ServerPlayer opponent = getOpponent(session, player);
            if (opponent != null || session.isPlayingWithVillager()) {
                List<Card> allOpponentCards = new ArrayList<>();
                if (opponent != null) {
                    allOpponentCards.addAll(session.getMeleeRow(opponent));
                    allOpponentCards.addAll(session.getRangedRow(opponent));
                    allOpponentCards.addAll(session.getSiegeRow(opponent));
                } else {
                    allOpponentCards.addAll(session.getMeleeRow(null));
                    allOpponentCards.addAll(session.getRangedRow(null));
                    allOpponentCards.addAll(session.getSiegeRow(null));
                }
                
                if (!allOpponentCards.isEmpty()) {
                    Card randomCard = allOpponentCards.get(new java.util.Random().nextInt(allOpponentCards.size()));
                    int effectivePower = session.getEffectivePower(randomCard, opponent);
                    if (effectivePower <= 3) {
                        // Уничтожаем карту
                        session.getMeleeRow(opponent).remove(randomCard);
                        session.getRangedRow(opponent).remove(randomCard);
                        session.getSiegeRow(opponent).remove(randomCard);
                        session.getGraveyard(opponent).add(randomCard);
                    } else {
                        // ВАЖНО: Передаем оппонента, чтобы модификатор применялся только к его картам
                        session.addPowerModifier(randomCard, -3, opponent);
                    }
                    session.recalculateRoundScore();
                    
                    if (player != null) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§cПирофеникс наносит 3 урона карте противника: §f" + randomCard.getName()));
                        com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                            "§cПирофеникс наносит 3 урона карте противника: " + randomCard.getName());
                    }
                }
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Пиромант - наносит 2 урона ближайшей карте противника
        if (cardId.equals("pyro_master") || (description.contains("пиромант") && description.contains("наносит 2 урона ближайшей"))) {
            ServerPlayer opponent = getOpponent(session, player);
            if (opponent != null || session.isPlayingWithVillager()) {
                List<Card> meleeRow = opponent != null ? session.getMeleeRow(opponent) : session.getMeleeRow(null);
                if (!meleeRow.isEmpty()) {
                    Card targetCard = meleeRow.get(0); // Ближайшая карта
                    int effectivePower = session.getEffectivePower(targetCard, opponent);
                    if (effectivePower <= 2) {
                        meleeRow.remove(targetCard);
                        session.getGraveyard(opponent).add(targetCard);
                    } else {
                        // ВАЖНО: Передаем оппонента, чтобы модификатор применялся только к его картам
                        session.addPowerModifier(targetCard, -2, opponent);
                    }
                    session.recalculateRoundScore();
                    
                    if (player != null) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§cПиромант наносит 2 урона ближайшей карте противника: §f" + targetCard.getName()));
                        com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                            "§cПиромант наносит 2 урона ближайшей карте противника: " + targetCard.getName());
                    }
                }
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Мастер Кузни - усиливает все карты в ближнем ряду на +1
        if (cardId.equals("forge_master") || (description.contains("мастер кузни") && description.contains("ближнем ряду"))) {
            boostRow(session, player, FalkyeGameSession.CardRow.MELEE, 1);
            if (player != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§6Мастер Кузни усиливает все карты в ближнем ряду на 1!"));
                com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                    "§6Мастер Кузни усиливает все карты в ближнем ряду на 1!");
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Странник Пустоты - берёт карту из колоды противника (упрощённо: возвращает случайную карту из сброса)
        if (cardId.equals("void_walker") || (description.contains("странник пустоты") && description.contains("берёт карту"))) {
            if (player != null) {
                List<Card> graveyard = session.getGraveyard(player);
                List<Card> hand = session.getHand(player);
                
                if (!graveyard.isEmpty()) {
                    Card randomCard = graveyard.get(new java.util.Random().nextInt(graveyard.size()));
                    hand.add(randomCard);
                    graveyard.remove(randomCard);
                    
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§5Странник Пустоты вернул карту: §f" + randomCard.getName()));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§5Странник Пустоты вернул карту: " + randomCard.getName());
                }
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Странник Снов - возвращает карту из сброса (обрабатывается в handleDreamWalkerOnDiscard)
        // Эта способность срабатывает при сбросе, а не при разыгрывании
        
        // Исследователь Пустоты - берёт карту из колоды (упрощённо: возвращает случайную карту из сброса)
        if (cardId.equals("void_researcher") || (description.contains("исследователь пустоты") && description.contains("берёт карту"))) {
            if (player != null) {
                List<Card> graveyard = session.getGraveyard(player);
                List<Card> hand = session.getHand(player);
                
                if (!graveyard.isEmpty()) {
                    Card randomCard = graveyard.get(new java.util.Random().nextInt(graveyard.size()));
                    hand.add(randomCard);
                    graveyard.remove(randomCard);
                    
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§5Исследователь Пустоты вернул карту: §f" + randomCard.getName()));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§5Исследователь Пустоты вернул карту: " + randomCard.getName());
                }
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Фульгур должен усиливать только карты Детей Рощения, а не все карты
        // (Проверка выше уже обработала эту карту, здесь оставлена для обратной совместимости)
        if (cardId.equals("lightning_dragon_fulgur") && description.contains("усиливает все карты детей рощения")) {
            // Уже обработано выше, но на всякий случай
            boostFactionCards(session, player, "Дети Рощения", 2);
            if (player != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§eФульгур усиливает все карты Детей Рощения на поле на 2!"));
                com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                    "§eФульгур усиливает все карты Детей Рощения на поле на 2!");
            }
            updateGameStateAfterEffect(session, player);
            return;
        }
        
        // Обработка по умолчанию для неизвестных карт существ - логируем предупреждение
        // Обычные карты существ без особых способностей не должны логировать предупреждения
        // Логируем только если описание содержит признаки способности
        if (description.contains("усиливает") || description.contains("лечит") || 
            description.contains("наносит") || description.contains("снижает") ||
            description.contains("возвращает") || description.contains("уничтожает")) {
            com.bmfalkye.util.ModLogger.logCardEffect("WARNING: Creature card with ability not fully processed", 
                "player", player != null ? player.getName().getString() : "null",
                "cardId", cardId,
                "cardName", card.getName(),
                "description", description);
        }
        // Для обычных существ без способностей просто завершаем без эффекта
        updateGameStateAfterEffect(session, player);
    }
    
    /**
     * Получает ряд, в котором находится карта
     */
    private static FalkyeGameSession.CardRow getCardRow(FalkyeGameSession session, ServerPlayer player, Card card) {
        List<Card> melee = session.getMeleeRow(player);
        List<Card> ranged = session.getRangedRow(player);
        List<Card> siege = session.getSiegeRow(player);
        
        for (Card c : melee) {
            if (c.getId().equals(card.getId())) {
                return FalkyeGameSession.CardRow.MELEE;
            }
        }
        for (Card c : ranged) {
            if (c.getId().equals(card.getId())) {
                return FalkyeGameSession.CardRow.RANGED;
            }
        }
        for (Card c : siege) {
            if (c.getId().equals(card.getId())) {
                return FalkyeGameSession.CardRow.SIEGE;
            }
        }
        return null;
    }
    
    /**
     * Наносит урон всем картам игрока на поле (использует эффективную силу)
     */
    /**
     * Наносит урон всем картам игрока на поле
     * ВАЖНО: Применяется только к картам указанного игрока (player) - отрицательный эффект
     * Используется для нанесения вреда оппоненту, поэтому player здесь - это оппонент
     * Даже если у другого игрока есть одинаковые карты, они не будут затронуты
     */
    public static void damageAllCardsOnField(FalkyeGameSession session, ServerPlayer player, int damage) {
        // Получаем карты ТОЛЬКО указанного игрока
        List<Card> melee = session.getMeleeRow(player);
        List<Card> ranged = session.getRangedRow(player);
        List<Card> siege = session.getSiegeRow(player);
        
        // Создаем копии списков для безопасной итерации
        List<Card> meleeCopy = new ArrayList<>(melee);
        List<Card> rangedCopy = new ArrayList<>(ranged);
        List<Card> siegeCopy = new ArrayList<>(siege);
        
        List<Card> cardsToRemove = new ArrayList<>();
        
        // Применяем урон ко всем картам указанного игрока
        for (Card card : meleeCopy) {
            // Проверяем, что карта действительно в ряду этого игрока
            if (!melee.contains(card)) {
                continue;
            }
            int effectivePower = session.getEffectivePower(card, player);
            if (effectivePower <= damage) {
                // Карта уничтожена
                cardsToRemove.add(card);
            } else {
                // Уменьшаем силу карты
                // ВАЖНО: Передаем игрока, чтобы модификатор применялся только к его картам
                session.addPowerModifier(card, -damage, player);
            }
        }
        
        for (Card card : rangedCopy) {
            if (!ranged.contains(card)) {
                continue;
            }
            int effectivePower = session.getEffectivePower(card, player);
            if (effectivePower <= damage) {
                cardsToRemove.add(card);
            } else {
                // ВАЖНО: Передаем игрока, чтобы модификатор применялся только к его картам
                session.addPowerModifier(card, -damage, player);
            }
        }
        
        for (Card card : siegeCopy) {
            if (!siege.contains(card)) {
                continue;
            }
            int effectivePower = session.getEffectivePower(card, player);
            if (effectivePower <= damage) {
                cardsToRemove.add(card);
            } else {
                session.addPowerModifier(card, -damage);
            }
        }
        
        // Удаляем уничтоженные карты из рядов и отправляем в сброс
        for (Card card : cardsToRemove) {
            melee.remove(card);
            ranged.remove(card);
            siege.remove(card);
            session.getGraveyard(player).add(card);
        }
        
        // Пересчитываем очки
        session.recalculateRoundScore();
    }
    
    /**
     * Усиливает все карты игрока на поле (добавляет модификатор силы)
     * ВАЖНО: Применяется только к картам указанного игрока (player) - положительный эффект
     * Даже если у оппонента есть одинаковые карты, они не будут затронуты
     */
    private static void boostAllCardsOnField(FalkyeGameSession session, ServerPlayer player, int boost) {
        // Получаем карты ТОЛЬКО указанного игрока
        List<Card> melee = session.getMeleeRow(player);
        List<Card> ranged = session.getRangedRow(player);
        List<Card> siege = session.getSiegeRow(player);
        
        // Добавляем модификатор силы ТОЛЬКО для карт указанного игрока
        for (Card card : melee) {
            // Проверяем, что карта действительно в ряду этого игрока
            if (session.getMeleeRow(player).contains(card)) {
                // ВАЖНО: Передаем игрока, чтобы модификатор применялся только к его картам
                session.addPowerModifier(card, boost, player);
            }
        }
        for (Card card : ranged) {
            if (session.getRangedRow(player).contains(card)) {
                // ВАЖНО: Передаем игрока, чтобы модификатор применялся только к его картам
                session.addPowerModifier(card, boost, player);
            }
        }
        for (Card card : siege) {
            if (session.getSiegeRow(player).contains(card)) {
                // ВАЖНО: Передаем игрока, чтобы модификатор применялся только к его картам
                session.addPowerModifier(card, boost, player);
            }
        }
        
        // Пересчитываем очки
        session.recalculateRoundScore();
    }
    
    /**
     * Снижает силу всех карт игрока на поле
     * ВАЖНО: Применяется только к картам указанного игрока (player) - отрицательный эффект
     * Используется для нанесения вреда оппоненту, поэтому player здесь - это оппонент
     * Даже если у другого игрока есть одинаковые карты, они не будут затронуты
     */
    private static void reduceAllCardsOnField(FalkyeGameSession session, ServerPlayer player, int reduction) {
        // Получаем карты ТОЛЬКО указанного игрока
        List<Card> melee = session.getMeleeRow(player);
        List<Card> ranged = session.getRangedRow(player);
        List<Card> siege = session.getSiegeRow(player);
        
        // Снижаем силу ТОЛЬКО карт указанного игрока
        for (Card card : melee) {
            // Проверяем, что карта действительно в ряду этого игрока
            if (session.getMeleeRow(player).contains(card)) {
                // ВАЖНО: Передаем игрока, чтобы модификатор применялся только к его картам
                session.addPowerModifier(card, -reduction, player);
            }
        }
        for (Card card : ranged) {
            if (session.getRangedRow(player).contains(card)) {
                // ВАЖНО: Передаем игрока, чтобы модификатор применялся только к его картам
                session.addPowerModifier(card, -reduction, player);
            }
        }
        for (Card card : siege) {
            if (session.getSiegeRow(player).contains(card)) {
                // ВАЖНО: Передаем игрока, чтобы модификатор применялся только к его картам
                session.addPowerModifier(card, -reduction, player);
            }
        }
        
        // Пересчитываем очки
        session.recalculateRoundScore();
    }
    
    /**
     * Лечит все карты игрока на поле (восстанавливает урон, нанесённый эффектами)
     * ВАЖНО: Применяется только к картам указанного игрока (player) - положительный эффект
     * Даже если у оппонента есть одинаковые карты, они не будут затронуты
     */
    private static void healAllCardsOnField(FalkyeGameSession session, ServerPlayer player, int healAmount) {
        // Получаем карты ТОЛЬКО указанного игрока
        List<Card> melee = session.getMeleeRow(player);
        List<Card> ranged = session.getRangedRow(player);
        List<Card> siege = session.getSiegeRow(player);
        
        // Лечим ТОЛЬКО карты указанного игрока (увеличиваем модификатор силы, но не выше базовой силы)
        for (Card card : melee) {
            // Проверяем, что карта действительно в ряду этого игрока
            if (session.getMeleeRow(player).contains(card)) {
                int currentEffectivePower = session.getEffectivePower(card, player);
                int basePower = card.getPower();
                // Если текущая сила меньше базовой, восстанавливаем
                if (currentEffectivePower < basePower) {
                    int heal = Math.min(healAmount, basePower - currentEffectivePower);
                    // ВАЖНО: Передаем игрока, чтобы модификатор применялся только к его картам
                    session.addPowerModifier(card, heal, player);
                }
            }
        }
        for (Card card : ranged) {
            if (session.getRangedRow(player).contains(card)) {
                int currentEffectivePower = session.getEffectivePower(card, player);
                int basePower = card.getPower();
                if (currentEffectivePower < basePower) {
                    int heal = Math.min(healAmount, basePower - currentEffectivePower);
                    // ВАЖНО: Передаем игрока, чтобы модификатор применялся только к его картам
                    session.addPowerModifier(card, heal, player);
                }
            }
        }
        for (Card card : siege) {
            if (session.getSiegeRow(player).contains(card)) {
                int currentEffectivePower = session.getEffectivePower(card, player);
                int basePower = card.getPower();
                if (currentEffectivePower < basePower) {
                    int heal = Math.min(healAmount, basePower - currentEffectivePower);
                    // ВАЖНО: Передаем игрока, чтобы модификатор применялся только к его картам
                    session.addPowerModifier(card, heal, player);
                }
            }
        }
        
        // Пересчитываем очки
        session.recalculateRoundScore();
    }
    
    /**
     * Снижает силу всех ближних карт оппонента до 1
     * ВАЖНО: Применяется только к картам указанного оппонента (opponent) - отрицательный эффект
     */
    private static void reduceMeleeRowTo1(FalkyeGameSession session, ServerPlayer opponent) {
        List<Card> melee = session.getMeleeRow(opponent);
        
        for (Card card : melee) {
            int currentEffectivePower = session.getEffectivePower(card, opponent);
            if (currentEffectivePower > 1) {
                // Снижаем до 1
                int reduction = currentEffectivePower - 1;
                // ВАЖНО: Передаем оппонента, чтобы модификатор применялся только к его картам
                session.addPowerModifier(card, -reduction, opponent);
            }
        }
        
        // Пересчитываем очки
        session.recalculateRoundScore();
    }
    
    /**
     * Снижает силу всех дальних карт оппонента до 1
     * ВАЖНО: Применяется только к картам указанного оппонента (opponent) - отрицательный эффект
     */
    private static void reduceRangedRowTo1(FalkyeGameSession session, ServerPlayer opponent) {
        List<Card> ranged = session.getRangedRow(opponent);
        
        for (Card card : ranged) {
            int currentEffectivePower = session.getEffectivePower(card, opponent);
            if (currentEffectivePower > 1) {
                // Снижаем до 1
                int reduction = currentEffectivePower - 1;
                // ВАЖНО: Передаем оппонента, чтобы модификатор применялся только к его картам
                session.addPowerModifier(card, -reduction, opponent);
            }
        }
        
        // Пересчитываем очки
        session.recalculateRoundScore();
    }
    
    /**
     * Снижает силу всех осадных карт оппонента до 1
     * ВАЖНО: Применяется только к картам указанного оппонента (opponent) - отрицательный эффект
     */
    private static void reduceSiegeRowTo1(FalkyeGameSession session, ServerPlayer opponent) {
        List<Card> siege = session.getSiegeRow(opponent);
        
        for (Card card : siege) {
            int currentEffectivePower = session.getEffectivePower(card, opponent);
            if (currentEffectivePower > 1) {
                // Снижаем до 1
                int reduction = currentEffectivePower - 1;
                // ВАЖНО: Передаем оппонента, чтобы модификатор применялся только к его картам
                session.addPowerModifier(card, -reduction, opponent);
            }
        }
        
        // Пересчитываем очки
        session.recalculateRoundScore();
    }
    
    /**
     * Получает противника игрока (с учётом villager)
     * ВАЖНО: Этот метод используется для определения, к кому применять отрицательные эффекты
     * @param session Сессия игры
     * @param player Игрок, который использовал карту (для него получаем оппонента)
     * @return Оппонент игрока (null для villager)
     */
    public static ServerPlayer getOpponent(FalkyeGameSession session, ServerPlayer player) {
        if (player == null) {
            // Если player == null, это villager, оппонент - player1
            return session.getPlayer1();
        }
        if (session.isPlayingWithVillager()) {
            // Если играем с villager, противник - это player1 (человеческий игрок)
            return player.equals(session.getPlayer1()) ? null : session.getPlayer1();
        }
        ServerPlayer p1 = session.getPlayer1();
        ServerPlayer p2 = session.getPlayer2();
        if (p1 == null || p2 == null) {
            return null;
        }
        // Возвращаем другого игрока
        return player.equals(p1) ? p2 : p1;
    }
    
    /**
     * Немедленно обновляет состояние игры после применения эффекта для визуального отображения
     */
    private static void updateGameStateAfterEffect(FalkyeGameSession session, ServerPlayer player) {
        // ВАЖНО: Пересчитываем очки раунда перед обновлением состояния игры
        // Это гарантирует, что клиент получит актуальные очки
        session.recalculateRoundScore();
        
        // Обновляем состояние игры для всех игроков немедленно
        // Это гарантирует, что все изменения применятся на клиенте
        if (session.getPlayer1() != null) {
            com.bmfalkye.network.NetworkHandler.updateGameStateImmediate(session.getPlayer1(), session);
        }
        if (session.getPlayer2() != null) {
            com.bmfalkye.network.NetworkHandler.updateGameStateImmediate(session.getPlayer2(), session);
        }
    }
    
    /**
     * Наносит урон всем картам в определённом ряду игрока
     */
    /**
     * Наносит урон всем картам в указанном ряду игрока
     * ВАЖНО: Применяется только к картам указанного игрока (player) - отрицательный эффект
     * Используется для нанесения вреда оппоненту, поэтому player здесь - это оппонент
     */
    private static void damageRow(FalkyeGameSession session, ServerPlayer player, FalkyeGameSession.CardRow row, int damage) {
        List<Card> rowCards;
        
        // Получаем нужный ряд в зависимости от типа
        switch (row) {
            case MELEE:
                rowCards = session.getMeleeRow(player);
                break;
            case RANGED:
                rowCards = session.getRangedRow(player);
                break;
            case SIEGE:
                rowCards = session.getSiegeRow(player);
                break;
            default:
                return;
        }
        
        // Создаем копию списка для безопасной итерации
        List<Card> cardsToProcess = new ArrayList<>(rowCards);
        List<Card> cardsToRemove = new ArrayList<>();
        
        // Применяем урон ТОЛЬКО к картам указанного игрока в ряду
        for (Card card : cardsToProcess) {
            // Проверяем, что карта действительно в ряду этого игрока
            if (!rowCards.contains(card)) {
                continue; // Пропускаем, если карта не в ряду этого игрока
            }
            int effectivePower = session.getEffectivePower(card, player);
            if (effectivePower <= damage) {
                // Карта уничтожена
                cardsToRemove.add(card);
            } else {
                // Уменьшаем силу карты
                // ВАЖНО: Передаем игрока, чтобы модификатор применялся только к его картам
                session.addPowerModifier(card, -damage, player);
            }
        }
        
        // Удаляем уничтоженные карты из ряда и отправляем в сброс
        for (Card card : cardsToRemove) {
            rowCards.remove(card);
            session.getGraveyard(player).add(card);
        }
        
        // Пересчитываем очки
        session.recalculateRoundScore();
    }
    
    /**
     * Усиливает все карты в определённом ряду игрока
     * ВАЖНО: Применяется только к картам указанного игрока (player) - положительный эффект
     */
    private static void boostRow(FalkyeGameSession session, ServerPlayer player, FalkyeGameSession.CardRow row, int boost) {
        List<Card> rowCards;
        
        // Получаем нужный ряд в зависимости от типа
        switch (row) {
            case MELEE:
                rowCards = session.getMeleeRow(player);
                break;
            case RANGED:
                rowCards = session.getRangedRow(player);
                break;
            case SIEGE:
                rowCards = session.getSiegeRow(player);
                break;
            default:
                return;
        }
        
        // Добавляем модификатор силы ТОЛЬКО для карт указанного игрока в ряду
        for (Card card : rowCards) {
            // Проверяем, что карта действительно в ряду этого игрока
            if (rowCards.contains(card)) {
                // ВАЖНО: Передаем игрока, чтобы модификатор применялся только к его картам
                session.addPowerModifier(card, boost, player);
            }
        }
        
        // Пересчитываем очки
        session.recalculateRoundScore();
    }
    
    /**
     * Уничтожает самую слабую карту противника
     */
    private static void destroyWeakestCard(FalkyeGameSession session, ServerPlayer player) {
        List<Card> allCards = new ArrayList<>();
        allCards.addAll(session.getMeleeRow(player));
        allCards.addAll(session.getRangedRow(player));
        allCards.addAll(session.getSiegeRow(player));
        
        if (allCards.isEmpty()) {
            return;
        }
        
        // Находим самую слабую карту (по эффективной силе)
        Card weakestCard = null;
        int weakestPower = Integer.MAX_VALUE;
        for (Card card : allCards) {
            int power = session.getEffectivePower(card, player);
            if (power < weakestPower) {
                weakestPower = power;
                weakestCard = card;
            }
        }
        
        if (weakestCard != null) {
            // Удаляем карту из поля и отправляем в сброс
            session.getMeleeRow(player).remove(weakestCard);
            session.getRangedRow(player).remove(weakestCard);
            session.getSiegeRow(player).remove(weakestCard);
            session.getGraveyard(player).add(weakestCard);
            
            // Пересчитываем очки
            session.recalculateRoundScore();
        }
    }
}

