# Графика на замену эмодзи

Что в интерфейсе сейчас нарисовано эмодзи или символом шрифта, а не картинкой.
Правила имён и размеров — [assets-spec.md](assets-spec.md). Путей к этим файлам
в коде нет, пока самих файлов нет: предмет без картинки показывает эмодзи из
контента, а без эмодзи — первую букву названия (`ItemPicture`, `ui/tasks/games/GameParts.kt`).

Проверено 27 сентября по `app/src/main/java/ru/finney/pet/ui/` (кроме `ui/pet/`)
и `app/src/main/assets/content/`.

## Эмодзи в коде интерфейса

**Нет ни одного.** Всё, что не буквы, в `ui/` — служебные знаки шрифта Glina (ниже).
Картинки предметов, целей и игр уже идут из `res/drawable-nodpi`.

## Эмодзи в `content/tasks.json`

Закрыто 28 сентября: у дел «Подработки» (`game_chores`, `game_chores_2`, `game_chores_3`)
теперь есть рисунки — `item_chore_flowers`, `item_chore_dishes`, `item_chore_dog`,
`item_chore_cleaning` в `drawable-nodpi`, прописаны полем `art`. Эмодзи в JSON оставлены
запасным вариантом.

Осталось одно дело без рисунка — «Помочь соседке» (`neighbor`, 👵, `game_chores_2` и `_3`):

| Экран / файл | Что изображает | Сейчас | Имя файла | Размер | Приоритет |
|---|---|---|---|---|---|
| «Подработка», `ChoresGame.kt` — плитка дела 36 dp и строка дня 32 dp | Помочь соседке (`neighbor`) | 👵 | `item_chore_neighbor.png` | 256 × 256 | средний |

## Служебные знаки — не ассеты

Это символы шрифта. Они читаются как текст и в TalkBack, а их смысл всегда продублирован
словом (ТЗ п. 3.6). Заменять картинками не обязательно; если дизайнеры нарисуют
иконки из списка `assets-spec.md` (`ic_check`, `ic_close`, `ic_plus`, `ic_minus`),
их можно подставить позже.

| Знак | Где | Что значит | Иконка из spec |
|---|---|---|---|
| ✓ | `Gauge.kt` (`CheckBadge`), `TasksScreen`, `GoalsScreen`, `WardrobeScreen`, `Feedback.kt`, `HomeScreen`, мини-игры | готово, выбрано, полно | `ic_check` |
| ✕ | `FinneyScreen.kt`, `GameScene.kt`, `CareGame.kt` | закрыть | `ic_close` |
| ✗ | `ReceiptGame.kt` | строка чека отмечена как ошибка | — |
| ! | `Gauge.kt` (`AlertBadge`), `ReserveGame.kt` | не хватает, перебор | — |
| − + | `HoldRepeat.kt`, `ChoresGame.kt`, `TaskGames.kt` | убавить, прибавить, приход и расход | `ic_minus`, `ic_plus` |
| ↺ | `BudgetScreen.kt`, `PeriodResultScreen.kt` | попробуй ещё раз | — |
| ← → ↑ ↓ | `ShoppingGame.kt`, `StandGame.kt`, `ToyPlay.kt`, `CarePanel.kt`, `GameParts.kt` (`BouncingArrow`), `SavingsButtons.kt` | направление, «превращается в» | `ic_back` для ← |
| × ≈ | `AdultScreen.kt`, `StandGame.kt`, `ReceiptGame.kt`, `ChoresGame.kt`, `SimpleGames.kt` | умножение, количество | — |
| ♥ | `GoalRaceGame.kt` — «+♥» / «−♥» на кнопках соблазна | настроение | — (сердце уже есть в `HeartIcon.kt`) |
