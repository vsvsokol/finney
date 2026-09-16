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
│   ├── game/        Game — единственная точка команд: план, покупка, копилка, задание, закрытие периода
│   ├── economy/     темп накоплений и срок до цели
│   ├── period/      план и факт периода
│   ├── progress/    очки развития, уровни, стадии
│   ├── pet/         шкалы, эмоция, подсказка по нужному
│   ├── tasks/       три движка заданий
│   └── model/       состояние игры и модели контента (@Serializable)
│
├── data/            ХРАНИЛИЩЕ                      — [@lemonke68], только он
│   ├── db/          Room: entity, dao, database
│   ├── repository/  репозитории
│   └── prefs/       DataStore
│
└── content/         ЗАГРУЗКА УЧЕБНОГО КОНТЕНТА     — [@lemonke68]
                     ContentParser, ContentValidator, AssetContentLoader
```

Правило: `ui` не знает про `data`, обращается только к `domain`. Зависимости экраны получают из `AppContainer` через фабрики ViewModel и сами ничего не создают. Экономика не зависит от Compose — поэтому её можно покрыть обычными unit-тестами без устройства.

### Как устроен `domain`

`Game` — набор чистых функций: получает `GameState` и команду, возвращает новое состояние
или отказ с причиной (`Rejection`). Сам ничего не хранит и ничего не знает про Room.
Репозиторий в `data/` загружает состояние профиля, передаёт в `Game` и сохраняет результат.
Данных на профиль — десятки строк, поэтому состояние целиком держится в памяти.

```kotlin
when (val result = game.buy(state, "food_apple")) {
    is GameResult.Ok -> save(result.state)
    is GameResult.Rejected -> showReason(result.reason) // InsufficientFunds(needed, balance) и т. д.
}
```

Для экранов подтверждения есть предпросмотры без изменения состояния:
`previewPurchase` (цена, шкалы до и после, нехватка), `previewWithdraw` (накопления и срок до и после),
`needsHint` (сколько стоит закрыть нужное), `goalProgress` (накоплено, осталось, срок).

## Структура данных

### Room: локальный профиль

Правило: в базе хранятся **факты** — что произошло. Всё, что можно посчитать из фактов
(баланс, накопления, уровень, стадия, эмоция), не хранится и считается в `domain/`.
Так число на экране не может разойтись с историей операций. Формулы — [economy.md](economy.md).

```kotlin
@Entity
data class Profile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val petName: String,         // игровое имя, ТЗ п. 2.5.2
    val bodyColor: String,       // a / b / c          — кастомизация
    val eyesVariant: String,     // round / oval / sly — 3 × 3 = 9 комбинаций
    val activeGoalId: String?,   // id из goals.json
    val isDemo: Boolean,         // тестовый профиль, ТЗ п. 2.5.13
    val createdAt: Long,
)

@Entity
data class PetState(
    @PrimaryKey val profileId: Long,
    val satiety: Int,            // 0..100
    val hygiene: Int,            // 0..100
    val mood: Int,               // 0..100
)

@Entity
data class Period(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val number: Int,             // 1, 2, 3…
    val stage: Int,              // стадия на момент открытия: снижение шкал и доход
    val phase: String,           // PLANNING / ACTIVE / CLOSED
    val budget: Int?,            // баланс в момент подтверждения плана
    val plannedNeeds: Int?,
    val plannedWants: Int?,
    val plannedSavings: Int?,
    val pointsEarned: Int?,      // заполняется при закрытии
    val needsCovered: Boolean?,  // снимок итогов — для истории, ТЗ п. 2.5.11
    val planMatched: Boolean?,
)

@Entity
data class LedgerEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val periodId: Long,
    val type: String,            // INCOME / TASK_REWARD / PARENT_BONUS / PURCHASE /
                                 // SAVINGS_DEPOSIT / SAVINGS_WITHDRAW / GOAL_COMPLETE
    val balanceDelta: Int,       // изменение доступного баланса
    val savingsDelta: Int,       // изменение копилки цели
    val category: String?,       // NEEDS / WANTS — только для PURCHASE
    val itemId: String?,         // товар из shop.json
    val goalId: String?,         // цель из goals.json
    val taskId: String?,         // задание из tasks.json
    val unplanned: Boolean,      // доход после подтверждения плана
    val createdAt: Long,
)

@Entity
data class TaskAttempt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val periodId: Long,
    val taskId: String,
    val outcome: String,         // SUCCESS / FAIL
    val reward: Int,             // 0, если награда за это задание уже выдавалась
    val createdAt: Long,
)
```

| Величина | Откуда берётся |
|---|---|
| Баланс | Σ `balanceDelta` |
| Копилка цели | Σ `savingsDelta` по `goalId` |
| Факт периода | Σ операций периода по типу и категории |
| Очки, уровень, стадия | Σ `Period.pointsEarned` |
| Купленные аксессуары | операции `PURCHASE` с `itemId` аксессуара |
| Завершённые задания | `TaskAttempt` с `outcome = SUCCESS` |
| Эмоция | шкалы `PetState` |
| Подпись операции для ребёнка | `type` + название товара, цели или задания из контента, ТЗ п. 2.5.4 |

В `domain` период адресуется номером (`periodNumber`), в Room — `periodId`; сопоставление делает репозиторий.

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
