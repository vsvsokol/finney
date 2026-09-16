# Питомец Финни

Мобильное приложение для Android: виртуальный питомец, через которого дети 7–11 лет
осваивают планирование бюджета, приоритезацию трат и накопления.

Хакатон «Лидеры цифровой трансформации» 2026. Техническое задание — [docs/manual.pdf](docs/manual.pdf).

> Игровая валюта условна. Реальные деньги, платежи, реклама и сбор персональных данных
> в приложении не используются (ТЗ п. 2.7, 3.5).

## Статус

| | |
|---|---|
| Стадия | скелет: зависимости, навигация, DI; ядро в разработке |
| Промежуточная сдача | 29 сентября 2026, 23:59 |
| Матрица требований | [docs/requirements-matrix.md](docs/requirements-matrix.md) |

## Стек

| Слой | Технология |
|---|---|
| Язык | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Навигация | Navigation Compose, типобезопасные маршруты |
| Зависимости | ручной контейнер `AppContainer`, без Hilt |
| Хранение | Room (SQLite) |
| Настройки | DataStore Preferences |
| Учебный контент | JSON в `app/src/main/assets/content/`, kotlinx.serialization |
| Тесты | JUnit, kotlinx-coroutines-test, room-testing |
| Минимальная версия | Android 8.0 (minSdk 26) |

Точные версии — только в [gradle/libs.versions.toml](gradle/libs.versions.toml). Библиотеку не добавляем
в `app/build.gradle.kts` напрямую, минуя каталог: это зона [@lemonke68] (CODEOWNERS).

Серверная часть не используется: основной игровой цикл работает офлайн (ТЗ п. 3.1).

## Требования к окружению

| Инструмент | Версия |
|---|---|
| Android Studio | Quail 4 (2026.1.4) или новее |
| JDK | 25 — JetBrains Runtime внутри Android Studio, отдельно ставить не нужно. Код компилируется в Java 17 |
| Android SDK | Platform 37 (compileSdk/targetSdk); эмулятор API 26, arm64-v8a — проверка нижней границы |
| Gradle / AGP | 9.6+ / 9.4+ (приходят с проектом) |
| Git | 2.40+ |
| Git LFS | нужен только для видео (`*.mp4`), для кода и графики не требуется |

## Быстрый запуск

```bash
git clone https://github.com/vsvsokol/finney.git
cd finney
```

Дальше: открыть папку в Android Studio, дождаться синхронизации Gradle, запустить конфигурацию `app`.

Сборка из командной строки:

```bash
./gradlew assembleDebug
```

Для `./gradlew` из терминала нужны переменные окружения (из Android Studio не нужны):

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

Если в системе стоит другая Java (например, 26), без `JAVA_HOME` сборка из терминала упадёт.

Релизная сборка описана в разделе «Подпись релиза».

## Состав репозитория

```
app/                 модуль приложения (Kotlin, Compose)
docs/
  manual.pdf                  техническое задание
  requirements-matrix.md      статус обязательных требований ТЗ
  architecture.md             архитектура, структура данных, формулы экономики
  content-tasks.md            задания и правила текстов
  assets-spec.md              спецификация графики для дизайнеров
design/exports/      готовые PNG от дизайнеров до переноса в app/
.github/             CI и правила ревью
CONTRIBUTING.md      правила работы с репозиторием
```

## Подпись релиза

Ключ подписи и пароли в репозиторий не попадают (ТЗ п. 3.4). Релиз подписывает [@lemonke68].

1. Ключ создаётся один раз, в корне репозитория (`*.jks` в `.gitignore`):
   ```bash
   keytool -genkey -v -keystore finney-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias finney
   ```
2. Скопировать `keystore.properties.example` в `keystore.properties` и заполнить. Файл в `.gitignore`.
3. Резервная копия `.jks` и паролей — ещё у одного человека в команде. Без ключа приложение
   нельзя обновить в RuStore: только публиковать заново под другим именем пакета.

```bash
./gradlew assembleRelease
```

Готовый файл: `app/build/outputs/apk/release/app-release.apk`. Если `keystore.properties` нет,
сборка не падает, а выдаёт `app-release-unsigned.apk` — так CI и остальная команда не зависят от ключа.
Неподписанный APK на устройство не установится.

## Команда

| Имя | Зона |
|---|---|
| [@lemonke68] | ядро: состояние питомца, экономика, хранилище, навигация, сборка APK |
| [@zYafALL] | экраны по макетам, анимация питомца в Compose, адаптивная сложность заданий |
| [@mitzzi4] | дизайн-система, ключевые экраны, облик питомца, арт-дирекшн |
| [@Lix2w78] | иконки, предметы, иллюстрации к заданиям, вторичные экраны |
| [@vsvsokol] | PM, контент заданий, баланс экономики, презентация и питч |

## Лицензии сторонних материалов

Перечень библиотек, шрифтов, изображений и звуков с лицензиями ведётся по ходу разработки
и обязателен к финальной сдаче (ТЗ п. 5.12).
