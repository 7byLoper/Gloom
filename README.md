<div align="center">
  <h1>Gloom</h1>
  <p>AI-античит для Minecraft-серверов с анализом aim-поведения через внешний inference API.</p>

  <p>
    <img alt="Minecraft" src="https://img.shields.io/badge/Minecraft-1.16+-62B47A?style=flat">
    <img alt="Java" src="https://img.shields.io/badge/Java-17+-E76F00?style=flat&logo=openjdk&logoColor=white">
    <img alt="Paper" src="https://img.shields.io/badge/Paper-supported-222222?style=flat">
    <img alt="License" src="https://img.shields.io/badge/License-GPL--3.0-blue?style=flat">
  </p>

  <p>
    <a href="https://discord.gg/N3KKFuxdma">
      <img alt="Discord" src="https://img.shields.io/badge/Discord-Gloom-5865F2?style=flat&logo=discord&logoColor=white">
    </a>
    <a href="https://t.me/gloomac">
      <img alt="Telegram" src="https://img.shields.io/badge/Telegram-gloomac-26A5E4?style=flat&logo=telegram&logoColor=white">
    </a>
  </p>
</div>

## Что такое GloomAI

GloomAI - это Minecraft-плагин античита для серверов. Плагин собирает данные игрока во время боя, отправляет их во внешний API и на основе вероятности читерского поведения ведет буфер нарушений, алерты, историю и наказания.

Проект рассчитан на связку:

- Minecraft-сервер с установленными GloomAI и PacketEvents
- второй античит для анализа movement-поведения, например GrimAC, Vulcan или Intave
- внешний inference API для AI-анализа aim-поведения

## Возможности

- буферизация вероятности перед флагом, чтобы снижать случайные срабатывания
- алерты и verbose-режим для отладки и наблюдения
- меню игроков и история нарушений
- мониторинг вероятности игрока в реальном времени
- поддержка MySQL и SQLite для хранения данных
- Redis для межсерверной синхронизации
- WorldGuard-регионы для отключения AI-проверки в выбранных местах

## Важный момент перед установкой

AI-проверка работает только при доступном inference API. По умолчанию в конфигурации указан:

```yaml
ml_check:
  analyze_server: https://api.gloomai.pro/v1/inference
```

Если вы не приобрели доступ к API, проверки будут недоступны.

## Требования

- Java 17+
- Paper 1.16+
- PacketEvents
- WorldGuard опционально, если нужны bypass-настройки для регионов
- Redis опционально, если нужна синхронизация между серверами

## Установка

1. Соберите или скачайте актуальный `GloomAI` jar-файл.
2. Установите `packetevents` в папку `plugins/`.
3. Поместите `GloomAI.jar` в папку `plugins/`.
4. Один раз запустите сервер, чтобы плагин создал конфиги.
5. Остановите сервер и настройте файлы конфигурации.
6. Включите AI-проверку в `plugins/GloomAI/anticheat/checks.yml`.
7. Запустите сервер снова.

## Основные команды

| Команда | Permission | Что делает |
| --- | --- | --- |
| `/gloom alerts` | `gloom.command.alert` | Включает или выключает алерты для игрока |
| `/gloom verbose` | `gloom.command.verbose` | Включает или выключает подробную информацию |
| `/gloom holo` | `gloom.command.holograms` | Включает или выключает AI-голограммы |
| `/gloom hologram` | `gloom.command.holograms` | Алиас команды голограмм |
| `/gloom menu` | `gloom.command.menu` | Открывает меню игроков |
| `/gloom history <player> [page]` | `gloom.command.history` | Показывает историю нарушений игрока |
| `/gloom monitor [player]` | `gloom.command.monitor` | Запускает монитор данных игрока |
| `/gloom monitor stop` | `gloom.command.monitor` | Останавливает текущий монитор |
| `/gloom reload` | `gloom.command.reload` | Перезагружает конфигурацию |

Базовое право для основной команды:

```text
gloom.command.use
```

## Сборка из исходников

```bash
git clone https://github.com/fruzzexx/GloomAI.git
cd GloomAI
./gradlew :GloomAI:shadowJar
```

На Windows:

```bat
gradlew.bat :GloomAI:shadowJar
```

Готовый jar-файл будет находиться в:

```text
GloomAI/build/libs/
```

При создании issue рекомендуется приложить:

- версию Minecraft-сервера
- версию Java
- версию Paper
- версию PacketEvents
- версию GloomAI
- конфиги `settings.yml` и `anticheat/checks.yml` без паролей и приватных токенов
- логи запуска и stack trace, если есть ошибка
- шаги воспроизведения проблемы

## Лицензия

GloomAI распространяется на условиях лицензии [GNU General Public License v3.0](LICENSE).
