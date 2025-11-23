# BM Falkye Developer API Documentation

## Введение

BM Falkye предоставляет полноценный API для разработчиков, позволяющий расширять функциональность мода. С помощью API вы можете:

- Регистрировать новые карты и лидеров через Fluent API
- Создавать собственные способности карт
- Добавлять игровые события
- Реагировать на события игры

## Быстрый старт

### 1. Создание аддона

```java
import com.bmfalkye.api.*;
import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;

@Mod("my_falkye_addon")
public class MyAddon {
    
    public MyAddon(IEventBus modBus) {
        // Регистрируем карту через CardBuilder
        FalkyeAPI.registerCard(modBus, CardBuilder.create()
            .id("rock_troll")
            .name("Скальный Тролль")
            .type(Card.CardType.CREATURE)
            .power(8)
            .faction("Дозорные Руин")
            .rarity(CardRarity.RARE)
            .cost(15)
            .description("Мощное существо с защитой от первого удара")
            .ability(new Ability() {
                @Override
                public void onPlay(FalkyeGameSession session, ServerPlayer player, String cardId) {
                    // При розыгрыше получает щит от первого удара
                }
                
                @Override
                public String getName() {
                    return "Каменная Кожа";
                }
                
                @Override
                public String getDescription() {
                    return "Игнорирует первый полученный урон";
                }
            })
            .build()
        );
        
        // Регистрируем игровое событие
        FalkyeAPI.registerGameEvent(modBus, new GameEvent(
            "mysterious_fog",
            "Таинственный Туман",
            "Сила всех карт дальнего боя уменьшается на 2",
            0.1 // 10% вероятность
        ) {
            @Override
            public void execute(FalkyeGameSession session, ServerPlayer player) {
                // Применяем эффект тумана
            }
        });
    }
}
```

## API Reference

### FalkyeAPI

Главный класс API для регистрации расширений.

#### Методы регистрации

##### `void registerCard(IEventBus modBus, CardBuilder builder)`
Регистрирует новую карту через CardBuilder.

**Параметры:**
- `modBus` - IEventBus вашего мода
- `builder` - CardBuilder с настройками карты

**Пример:**
```java
FalkyeAPI.registerCard(modBus, CardBuilder.create()
    .id("my_card")
    .name("Моя Карта")
    .type(Card.CardType.CREATURE)
    .power(10)
    .faction("Дом Пламени")
    .build()
);
```

##### `void registerLeader(IEventBus modBus, LeaderBuilder builder)`
Регистрирует нового лидера через LeaderBuilder.

**Параметры:**
- `modBus` - IEventBus вашего мода
- `builder` - LeaderBuilder с настройками лидера

##### `void registerGameEvent(IEventBus modBus, GameEvent event)`
Регистрирует игровое событие.

**Параметры:**
- `modBus` - IEventBus вашего мода
- `event` - экземпляр GameEvent

### CardBuilder

Fluent API для создания карт.

#### Методы

##### `static CardBuilder create()`
Создаёт новый экземпляр CardBuilder.

##### `CardBuilder id(String id)`
Устанавливает уникальный ID карты.

##### `CardBuilder name(String name)`
Устанавливает имя карты.

##### `CardBuilder type(Card.CardType type)`
Устанавливает тип карты (CREATURE, SPELL, SPECIAL).

##### `CardBuilder power(int power)`
Устанавливает силу карты.

##### `CardBuilder faction(String faction)`
Устанавливает фракцию карты.

##### `CardBuilder rarity(CardRarity rarity)`
Устанавливает редкость карты (COMMON, RARE, EPIC, LEGENDARY).

##### `CardBuilder cost(int cost)`
Устанавливает стоимость карты.

##### `CardBuilder description(String description)`
Устанавливает описание карты.

##### `CardBuilder ability(Ability ability)`
Устанавливает способность карты.

##### `Card build()`
Создаёт карту. Выбрасывает `IllegalStateException` если не указаны обязательные поля.

**Пример:**
```java
Card card = CardBuilder.create()
    .id("fire_elemental")
    .name("Огненный Элементаль")
    .type(Card.CardType.CREATURE)
    .power(5)
    .faction("Дом Пламени")
    .rarity(CardRarity.COMMON)
    .cost(10)
    .description("Элементаль огня")
    .ability(new MyAbility())
    .build();
```

### LeaderBuilder

Fluent API для создания лидеров.

#### Методы

##### `static LeaderBuilder create()`
Создаёт новый экземпляр LeaderBuilder.

##### `LeaderBuilder id(String id)`
Устанавливает уникальный ID лидера.

##### `LeaderBuilder name(String name)`
Устанавливает имя лидера.

##### `LeaderBuilder faction(String faction)`
Устанавливает фракцию лидера.

##### `LeaderBuilder description(String description)`
Устанавливает описание лидера.

##### `LeaderBuilder ability(LeaderCard.LeaderAbility ability)`
Устанавливает способность лидера.

##### `LeaderCard build()`
Создаёт лидера.

**Пример:**
```java
LeaderCard leader = LeaderBuilder.create()
    .id("my_leader")
    .name("Мой Лидер")
    .faction("Дом Пламени")
    .description("Описание лидера")
    .ability((session, player) -> {
        // Логика способности
    })
    .build();
```

### Ability

Интерфейс для создания способностей карт.

#### Методы

##### `void onPlay(FalkyeGameSession session, ServerPlayer player, String cardId)`
Вызывается при розыгрыше карты.

##### `void onDeath(FalkyeGameSession session, ServerPlayer player, String cardId)`
Вызывается при смерти карты.

##### `void onRoundEnd(FalkyeGameSession session, ServerPlayer player, String cardId)`
Вызывается в конце раунда.

##### `void onRoundStart(FalkyeGameSession session, ServerPlayer player, String cardId)`
Вызывается в начале раунда.

##### `String getName()`
Возвращает имя способности.

##### `String getDescription()`
Возвращает описание способности.

**Пример:**
```java
Ability myAbility = new Ability() {
    @Override
    public void onPlay(FalkyeGameSession session, ServerPlayer player, String cardId) {
        // Усиливаем все карты игрока на 2
        List<Card> melee = session.getMeleeRow(player);
        for (Card card : melee) {
            session.addPowerModifier(card, 2, player);
        }
        session.recalculateRoundScore();
    }
    
    @Override
    public String getName() {
        return "Боевой Клич";
    }
    
    @Override
    public String getDescription() {
        return "Усиливает все карты ближнего боя на 2";
    }
};
```

### GameEvent

Абстрактный класс для создания игровых событий.

#### Конструктор

##### `GameEvent(String id, String name, String description, double probability)`
Создаёт новое игровое событие.

**Параметры:**
- `id` - уникальный ID события
- `name` - имя события
- `description` - описание события
- `probability` - вероятность срабатывания (0.0 - 1.0)

#### Методы

##### `abstract void execute(FalkyeGameSession session, ServerPlayer player)`
Вызывается при срабатывании события. Должен быть реализован.

##### `boolean canTrigger(FalkyeGameSession session)`
Проверяет, может ли событие произойти. По умолчанию возвращает `true`.

##### `String getId()`, `String getName()`, `String getDescription()`, `double getProbability()`
Геттеры для свойств события.

**Пример:**
```java
GameEvent fogEvent = new GameEvent(
    "mysterious_fog",
    "Таинственный Туман",
    "Сила всех карт дальнего боя уменьшается на 2",
    0.1
) {
    @Override
    public void execute(FalkyeGameSession session, ServerPlayer player) {
        // Применяем эффект тумана
        // Уменьшаем силу всех карт дальнего боя на 2
        List<Card> ranged = session.getRangedRow(player);
        for (Card card : ranged) {
            session.addPowerModifier(card, -2, player);
        }
        session.recalculateRoundScore();
    }
    
    @Override
    public boolean canTrigger(FalkyeGameSession session) {
        // Событие может произойти только после 3 раунда
        return session.getCurrentRound() >= 3;
    }
};
```

### AbilityRegistry

Реестр способностей карт.

#### Методы

##### `static void registerCardAbility(String cardId, Ability ability)`
Регистрирует способность для карты.

##### `static Ability getCardAbility(String cardId)`
Получает способность карты.

##### `static boolean hasAbility(String cardId)`
Проверяет, есть ли способность у карты.

## Примеры использования

### Пример 1: Создание мода с новыми картами

```java
@Mod("my_falkye_extension")
public class MyFalkyeExtension {
    
    public MyFalkyeExtension(IEventBus modBus) {
        modBus.addListener(this::commonSetup);
    }
    
    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
            
            // Регистрируем карту
            FalkyeAPI.registerCard(modBus, CardBuilder.create()
                .id("my_dragon")
                .name("Мой Дракон")
                .type(Card.CardType.CREATURE)
                .power(15)
                .faction("Дом Пламени")
                .rarity(CardRarity.LEGENDARY)
                .cost(20)
                .description("Мощный дракон")
                .ability(new Ability() {
                    @Override
                    public void onPlay(FalkyeGameSession session, ServerPlayer player, String cardId) {
                        // Логика способности
                    }
                    
                    @Override
                    public String getName() {
                        return "Дыхание Пламени";
                    }
                    
                    @Override
                    public String getDescription() {
                        return "Наносит 3 урона всем картам противника";
                    }
                })
                .build()
            );
        });
    }
}
```

### Пример 2: Создание игрового события

```java
@Mod("my_falkye_events")
public class MyFalkyeEvents {
    
    public MyFalkyeEvents(IEventBus modBus) {
        modBus.addListener(this::commonSetup);
    }
    
    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
            
            // Регистрируем событие
            FalkyeAPI.registerGameEvent(modBus, new GameEvent(
                "lucky_coin",
                "Счастливая Монета",
                "Игрок получает 50 дополнительных монет",
                0.05 // 5% вероятность
            ) {
                @Override
                public void execute(FalkyeGameSession session, ServerPlayer player) {
                    if (player != null && player.level() instanceof ServerLevel level) {
                        PlayerCurrency currency = PlayerCurrency.get(level);
                        currency.addCoins(player, 50);
                        player.sendSystemMessage(Component.literal("§eВы нашли счастливую монету! +50 монет"));
                    }
                }
            });
        });
    }
}
```

### Пример 3: Создание лидера

```java
FalkyeAPI.registerLeader(modBus, LeaderBuilder.create()
    .id("custom_leader")
    .name("Кастомный Лидер")
    .faction("Дозорные Руин")
    .description("Особый лидер с уникальной способностью")
    .ability((session, player) -> {
        // Уничтожаем самую слабую карту противника
        ServerPlayer opponent = session.getOpponent(player);
        if (opponent != null) {
            Card weakest = session.findWeakestCard(opponent);
            if (weakest != null) {
                session.removeCardFromField(opponent, weakest);
            }
        }
    })
    .build()
);
```

## Интеграция способностей в игру

Способности, зарегистрированные через `Ability`, автоматически вызываются в следующих случаях:

1. **При розыгрыше карты:** `onPlay()` вызывается в `CardEffects.applyCardEffect()`
2. **При смерти карты:** `onDeath()` вызывается при удалении карты с поля
3. **В конце раунда:** `onRoundEnd()` вызывается при завершении раунда
4. **В начале раунда:** `onRoundStart()` вызывается при начале раунда

## Игровые события

Игровые события проверяются и запускаются через `GameEventSystem.checkAndTriggerEvents()` в следующих случаях:

1. **При начале игры:** В `GameManager.startMatchWithConfig()`
2. **В процессе игры:** Может быть вызвано вручную

События проверяются на:
- Возможность срабатывания (`canTrigger()`)
- Вероятность срабатывания (`getProbability()`)

## Важные замечания

1. **Регистрация через IEventBus:** Все регистрации должны происходить через `IEventBus` вашего мода в `FMLCommonSetupEvent`.

2. **Уникальные ID:** Убедитесь, что ID ваших карт, лидеров и событий уникальны и не конфликтуют с существующими.

3. **Потокобезопасность:** API использует потокобезопасные коллекции, но при работе с игровыми сессиями будьте осторожны с многопоточностью.

4. **Производительность:** Способности и события вызываются синхронно, поэтому избегайте долгих операций.

5. **Валидация:** Все входные данные валидируются, но рекомендуется добавлять собственную проверку.

## Доступ к существующим системам

### CardRegistry
```java
Card card = CardRegistry.getCard("card_id");
List<Card> allCards = CardRegistry.getAllCards();
List<Card> factionCards = CardRegistry.getCardsByFaction("Дом Пламени");
```

### LeaderRegistry
```java
LeaderCard leader = LeaderRegistry.getLeader("leader_id");
LeaderCard factionLeader = LeaderRegistry.getLeaderForFaction("Дом Пламени");
```

### GameEventSystem
```java
List<GameEvent> events = GameEventSystem.getRegisteredEvents();
```

## Поддержка

Если у вас возникли вопросы или проблемы с API, обратитесь к документации мода или создайте issue в репозитории проекта.
