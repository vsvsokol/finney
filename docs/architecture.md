# Архитектура и стек

Документ по ТЗ п. 5.3 (компонентная архитектура) и п. 5.4 (структура данных).

## Стек

| Слой | Технология | Почему |
|---|---|---|
| Язык | Kotlin | нативная разработка под Android, ТЗ п. 3.2 |
| UI | Jetpack Compose + Material 3 | декларативный UI, состояние и экран не рассинхронизируются |
| Навигация | Navigation Compose | один граф экранов, маршруты — `@Serializable`-объекты |
| Зависимости | ручной контейнер `AppContainer` | десяток синглтонов не окупает Hilt: минус кодогенерация и время сборки |
| Хранилище | Room (поверх SQLite) | ТЗ п. 3.2 прямо допускает; проверка SQL при компиляции, миграции, тесты |
| Настройки | DataStore Preferences | флаги: звук, анимации, демо-режим |
| Учебный контент | JSON в `assets/content/` + kotlinx.serialization | ТЗ п. 2.5.14: задание добавляется без переработки логики |
| Асинхронность | Coroutines + Flow | реактивное обновление экранов |
| Тесты | JUnit, kotlinx-coroutines-test | ТЗ п. 3.4 требует тесты на экономику |
| minSdk | 26 (Android 8.0) | ТЗ п. 3.1 |

Серверной части нет. Весь цикл работает офлайн (ТЗ п. 3.1.5).
ИИ и облако не используются (ТЗ п. 2.7.5 снимает обязательность).

## Разделение на компоненты

ТЗ п. 3.4 требует, чтобы игровая экономика, учебный контент, хранение и интерфейс были разделены.

```
ru.finney.pet
├── FinneyApplication   создаёт AppContainer          — [@lemonke68]
├── AppContainer        ручной DI: база, репозитории   — [@lemonke68]
│
├── navigation/      маршруты и NavHost             — [@lemonke68]
│
├── ui/              экраны и анимация              — [@zYafALL]
│   ├── home/        главный экран
│   ├── budget/      план бюджета
│   ├── shop/        покупки
│   ├── goals/       накопления и цели
│   ├── tasks/       задания
│   ├── progress/    история и прогресс
│   ├── adult/       раздел для взрослого
│   ├── pet/         композит питомца, анимация слоёв
│   └── theme/       палитра, типографика, компоненты
│
├── domain/          ИГРОВАЯ ЭКОНОМИКА              — [@lemonke68], только он
│   ├── game/        Game — команды игры; GameStore — команды с сохранением; GameStorage — интерфейс хранилища
│   ├── profile/     проверка имени питомца
│   ├── economy/     темп накоплений и срок до цели
│   ├── period/      план и факт периода
│   ├── progress/    очки развития, уровни, стадии
│   ├── pet/         шкалы, эмоция, подсказка по нужному
│   ├── tasks/       три движка заданий
│   └── model/       состояние игры и модели контента (@Serializable)
│
├── data/            ХРАНИЛИЩЕ                      — [@lemonke68], только он
│   ├── db/          Room: entity, dao, database, маппинг в domain
│   ├── repository/  RoomGameStorage — реализация domain/game/GameStorage
│   └── prefs/       DataStore — настройки звука и анимаций (ещё не сделано)
│
└── content/         ЗАГРУЗКА УЧЕБНОГО КОНТЕНТА     — [@lemonke68]
                     ContentParser, ContentValidator, AssetContentLoader
```

Правило: `ui` не знает про `data`, обращается только к `domain`. Зависимости экраны получают из `AppContainer` через фабрики ViewModel и сами ничего не создают. Экономика не зависит от Compose — поэтому её можно покрыть обычными unit-тестами без устройства.

### Как устроен `domain`

`Game` — набор чистых функций: получает `GameState` и команду, возвращает новое состояние
или отказ с причиной (`Rejection`). Сам ничего не хранит и ничего не знает про Room.

Экраны работают не с `Game` напрямую, а с **`GameStore`** из `AppContainer`. Он берёт последнее
сохранённое состояние, выполняет команду и сразу сохраняет результат. Команды идут строго
по очереди: двойное нажатие не спишет деньги дважды.

```kotlin
val store = container.gameStore

// создание профиля на экране питомца
when (val r = store.createProfile(name, PetAppearance(BodyColor.A, EyesVariant.ROUND))) {
    is ProfileResult.Saved -> openHome(r.profileId)
    is ProfileResult.InvalidName -> showError(r.error)          // BLANK / TOO_LONG
}

// любая команда игры
when (val r = store.execute(profileId) { buy(it, "food_apple") }) {
    is GameResult.Ok -> Unit                                    // экран обновится сам через observeGame
    is GameResult.Rejected -> showReason(r.reason)              // InsufficientFunds(needed, balance) и т. д.
}

// экран подписывается на состояние
store.observeGame(profileId).collect { saved -> /* saved.profile, saved.state; null — профиль удалён */ }
```

Задания — `store.submitTask(profileId, taskId, input)`: возвращает исход, числа для объяснения
и награду. Чтение без изменения состояния — через `container.game`: `level`, `stage`, `emotion`,
`previewPurchase` (цена, шкалы до и после, нехватка), `previewWithdraw` (накопления и срок до и после),
`needsHint` (сколько стоит закрыть нужное), `goalProgress` (накоплено, осталось, срок).

## Структура данных

### Room: локальный профиль

Правило: в базе хранятся **факты** — что произошло. Всё, что можно посчитать из фактов
(баланс, накопления, уровень, стадия, эмоция), не хранится и считается в `domain/`.
Так число на экране не может разойтись с историей операций. Формулы — [economy.md](economy.md).

Код — `data/db/Entities.kt`, схема базы каждой версии — `app/schemas/`.

| Таблица | Ключ | Что хранит |
|---|---|---|
| `profiles` | `id` | имя питомца, цвет тела (`A/B/C`), глаза (`ROUND/OVAL/SLY`), активная цель, `isDemo`, дата создания |
| `pet_state` | `profileId` | сытость, чистота, настроение |
| `periods` | `profileId + number` | номер, стадия, фаза; план (бюджет и три направления) после подтверждения; итоги (факт, незапланированный доход, флаги, очки) после закрытия |
| `ledger` | `id` | операции: тип, Δ баланса, Δ копилки, категория, товар, цель, задание, `unplanned`, время |
| `task_attempts` | `id` | попытки заданий: период, задание, исход, награда, время |

- Все таблицы ссылаются на профиль с `ON DELETE CASCADE`: удаление профиля убирает все его данные одним запросом.
- Перечисления хранятся строкой по имени: переименовал значение enum — нужна миграция.
- Операции и попытки заданий только дописываются. При сохранении в базу добавляется хвост, которого там ещё нет.
- Период адресуется номером и в `domain`, и в базе — отдельного id нет.

| Величина | Откуда берётся |
|---|---|
| Баланс | Σ `balanceDelta` |
| Копилка цели | Σ `savingsDelta` по `goalId` |
| Факт периода | Σ операций периода по типу и категории |
| Очки, уровень, стадия | Σ `periods.pointsEarned` |
| Купленные аксессуары | операции `PURCHASE` с `itemId` аксессуара |
| Завершённые задания | `task_attempts` с `outcome = SUCCESS` |
| Эмоция | шкалы `pet_state` |
| Подпись операции для ребёнка | `type` + название товара, цели или задания из контента, ТЗ п. 2.5.4 |

**Меняешь entity — поднимаешь `version` в `FinneyDatabase` и пишешь миграцию.** Без неё Room
при обновлении приложения упадёт, а удалять базу у экспертов нельзя.

### JSON: учебный контент

Лежит в `app/src/main/assets/content/`, правится [@vsvsokol] без участия разработчиков.

```
content/
├── tasks.json      задания
├── shop.json       товары и цены
├── goals.json      цели накопления
├── economy.json    баланс: доход, награды, очки, шкалы питомца
├── feedback.json   тексты эмоций, итогов периода, отказов
└── glossary.json   справочник терминов, ТЗ п. 2.5.11
```

Пример `economy.json` — значения по умолчанию из [economy.md](economy.md):

```json
{
  "incomeByStage": [50, 60, 70],
  "taskReward": { "success": 15, "fail": 5 },
  "parentBonus": { "step": 5, "maxPerPeriod": 20 },
  "planTolerance": 5,
  "points": {
    "needsCovered": 2,
    "planMatched": 2,
    "savingsAdded": 2,
    "taskSuccess": 1,
    "taskSuccessMaxPerPeriod": 2
  },
  "pointsPerLevel": 5,
  "maxLevel": 9,
  "stageStartLevels": [1, 4, 7],
  "pet": {
    "start": { "satiety": 70, "hygiene": 70, "mood": 70 },
    "decayByStage": [
      { "satiety": 40, "hygiene": 30, "mood": 20 },
      { "satiety": 50, "hygiene": 35, "mood": 25 },
      { "satiety": 60, "hygiene": 40, "mood": 30 }
    ],
    "needsThreshold": 50,
    "emotionLow": 30,
    "emotionHappy": 60
  }
}
```

## Игровая экономика

Правила периода, плана и факта, покупок, шкал, целей, очков, заданий и демо-режима —
в [economy.md](economy.md). Там же значения по умолчанию, симуляция баланса
и список инвариантов, каждый из которых покрывается unit-тестом (ТЗ п. 3.4).
