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
├── ViewModelFactory    appContainer() для фабрик ViewModel — [@lemonke68]
│
├── navigation/      маршруты, NavHost, выбор стартового экрана — [@lemonke68]
│
├── ui/              экраны и анимация              — [@zYafALL]
│   ├── onboarding/  знакомство, имя и внешность питомца
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
│   ├── game/        Game — команды игры; GameStore — команды с сохранением; Session — открытый профиль;
│   │                GameStorage, ActiveProfileStorage — интерфейсы хранилища
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
│   └── prefs/       DataStore — открытый профиль; настройки звука и анимаций (ещё не сделано)
│
└── content/         ЗАГРУЗКА УЧЕБНОГО КОНТЕНТА     — [@lemonke68]
                     ContentParser, ContentValidator, AssetContentLoader
```

Правило: `ui` не знает про `data`, обращается только к `domain`. Зависимости экраны получают из `AppContainer` через фабрики ViewModel и сами ничего не создают. Экономика не зависит от Compose — поэтому её можно покрыть обычными unit-тестами без устройства.

### Как устроен `domain`

`Game` — набор чистых функций: получает `GameState` и команду, возвращает новое состояние
или отказ с причиной (`Rejection`). Сам ничего не хранит и ничего не знает про Room.

Экраны работают не с `Game` напрямую, а с **`GameStore`** из `AppContainer` — точнее, с **`Session`** поверх него (ниже). Он берёт последнее
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

### Открытый профиль: `Session`

Экраны игры не передают `profileId`. `container.session` знает, какой профиль открыт
(id хранится в DataStore), и повторяет команды `GameStore` без id:

```kotlin
val session = container.session

session.activeGame                                  // Flow<SavedGame?>: null — профиль не выбран или удалён
session.createProfile(name, appearance)             // создаёт и сразу открывает профиль
session.execute { buy(it, "food_apple") }           // команда над открытым профилем
session.submitTask(taskId, input)
session.deleteActiveProfile()                       // сброс; открывается оставшийся профиль, если есть
session.restore()                                   // при старте: true — есть профиль, идём на главный
```

База и DataStore восстанавливаются из резервной копии независимо. Если сохранённый id указывает
на несуществующий профиль, `restore()` открывает первый из оставшихся.

## Как устроены экраны

Навигация — `navigation/`. Маршруты — `@Serializable`-объекты в `Routes.kt`, граф — `FinneyNavHost`.
Экран не знает про `NavController`: получает лямбды `onOpenShop`, `onBack` и т. п., куда они ведут —
решает только `FinneyNavHost`. Стартовый экран выбирает `StartViewModel` через `session.restore()`.

Каждый экран — пара «ViewModel + Composable» в своём пакете `ui/`. Образцы: `ui/home`, `ui/onboarding`, `ui/budget`.

```kotlin
class ShopViewModel(private val session: Session, private val game: Game) : ViewModel() {

    // Всё, что рисует экран, — одно состояние. Считается из сохранённой игры, поэтому обновляется само.
    val uiState: StateFlow<ShopUiState> = session.activeGame
        .filterNotNull()
        .map { saved -> ShopUiState.Ready(balance = saved.state.balance /* … */) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShopUiState.Loading)

    // Разовое: перейти на другой экран, показать отказ.
    private val _events = Channel<ShopEvent>(Channel.BUFFERED)
    val events: Flow<ShopEvent> = _events.receiveAsFlow()

    fun buy(itemId: String) {
        viewModelScope.launch {
            when (val r = session.execute { buy(it, itemId) }) {
                is GameResult.Ok -> Unit                               // uiState обновится сам
                is GameResult.Rejected -> _events.send(ShopEvent.Rejected(r.reason))
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { appContainer().let { ShopViewModel(it.session, it.game) } }
        }
    }
}

@Composable
fun ShopScreen(onBack: () -> Unit, viewModel: ShopViewModel = viewModel(factory = ShopViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.events.collect { /* … */ } }
    ShopContent(state, onBuy = viewModel::buy, onBack = onBack)   // без ViewModel — для @Preview
}
```

Правила:

- Команды запускаются в `viewModelScope`, а не в `rememberCoroutineScope`: уход с экрана
  не должен оборвать сохранение на середине.
- Баланс, шкалы, копилку на экране не пересчитывать и не кэшировать — брать из `activeGame`
  и чтений `Game` (`level`, `emotion`, `goalProgress`, `planReport`, `previewPurchase` …).
- Параметр маршрута (например, `taskId`) передаётся в фабрику: `TaskViewModel.factory(taskId)`.
- Тест ViewModel — наследник `ViewModelTest` в `app/src/test/.../ui`: игра в памяти, без эмулятора.

### Питомец: слои и анимации

Пакет `ui/pet`. Питомец не покадровый: `PetView` собирает его из слоёв одного холста
2048 × 2048 — ноги, руки, базовый слой с головой и туловищем, — а оживляет его код.
Слои лежат в одном холсте, поэтому совпадают по положению сами и координаты подбирать не нужно.

- `PetMood` — выражение лица: четыре базовых слоя, переключение через `Crossfade`.
- `PetPose` — мгновенное положение всех частей (масштаб, подъём, наклон, поворот каждой конечности).
  Анимации только заполняют эту структуру, рисование про них ничего не знает.
- `PetAnimation` (`rememberPetAnimation()`) — покоя (дыхание и покачивание, идёт всегда)
  и радости (`playJoy()`, по событию).
- Точки вращения конечностей — доли от стороны холста, поэтому не зависят от размера экспорта.

Базовый слой получается вычитанием конечностей из состояния лица: без вычитания под
повёрнутой рукой видно исходную. Делает это `tools/split_pet_base.py` — он же кладёт
в `res/drawable-nodpi` WebP без потерь. Исходные PNG живут в `design/exports/pet`.

`PetLabScreen` — черновой экран для подбора анимаций, в игру не входит. Удаляется
вместе с `PetLabRoute`, когда питомец переедет на настоящий главный экран.

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
