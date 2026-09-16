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
| Android SDK | Platform 37 (compileSdk/targetSdk), Platform-Tools |
| Эмулятор | API 37 для работы и **API 26 (Android 8.0) для проверки нижней границы**. Образ под свой процессор: `arm64-v8a` на Apple Silicon, `x86_64` на Intel/AMD |
| Kotlin / Gradle / AGP | 2.4.20 / 9.6 / 9.4 — приходят с проектом, отдельно не ставятся |
| Git | 2.40+ |
| Git LFS | нужен только для видео (`*.mp4`), для кода и графики не требуется |

Версию Android Studio проверить: `Android Studio → About`. Если старше 2026.1.4 —
`Help → Check for Updates`, иначе синхронизация проекта может не пройти.

Физическое устройство обязательно (ТЗ п. 3.1): Android 8.0+, от 3 ГБ RAM, отладка по USB включена.

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

Windows: `gradlew.bat assembleDebug`.

### Переменные окружения

Нужны только для сборки и `adb` из терминала — из Android Studio всё работает и без них.
`JAVA_HOME` указывает на Java, встроенную в Android Studio: если в системе стоит другая
(например, 26), без этой переменной сборка из терминала упадёт.

macOS — в `~/.zshrc`:

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export PATH="$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator"
```

Linux — в `~/.bashrc` или `~/.zshrc` (путь к Studio зависит от способа установки, для snap — `/snap/android-studio/current`):

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export JAVA_HOME="/opt/android-studio/jbr"
export PATH="$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator"
```

Windows — `Параметры → Система → О системе → Дополнительные параметры → Переменные среды`:

```
ANDROID_HOME = %LOCALAPPDATA%\Android\Sdk
JAVA_HOME    = C:\Program Files\Android\Android Studio\jbr
Path        += %ANDROID_HOME%\platform-tools
```

### Проверка окружения

```bash
java -version      # должна быть 25 (JetBrains Runtime)
adb version
./gradlew --version
```

Все три команды отвечают без ошибок — окружение готово.

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

1. Ключ создаётся один раз и лежит **рядом с папкой репозитория, а не внутри неё** —
   именно такой путь `../finney-release.jks` прописан в `keystore.properties.example`.
   Из корня репозитория:
   ```bash
   keytool -genkeypair -v -keystore ../finney-release.jks -storetype PKCS12 -keyalg RSA -keysize 2048 -validity 10000 -alias finney
   ```
   В формате PKCS12 пароль ключа совпадает с паролем хранилища: `storePassword` и `keyPassword`
   в `keystore.properties` одинаковые.
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
