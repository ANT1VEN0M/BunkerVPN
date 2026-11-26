<div align="center">

# 🐈‍⬛ Bunker VPN for Android (v1.4.6)

**Флагманский Android VPN-клиент экосистемы «Бункер Кота» на базе Xray Core Lite и Jetpack Compose.**  
Создан для скоростного и безотказного обхода любых типов блокировок ТСПУ РКН в России.

[![Release](https://img.shields.io/github/v/release/ANT1VEN0M/BunkerVPN?color=7C3AED&label=Релиз&style=for-the-badge)](https://github.com/ANT1VEN0M/BunkerVPN/releases/latest)
[![Platform](https://img.shields.io/badge/Платформа-Android_8.0+-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/ANT1VEN0M/BunkerVPN/releases/latest)
[![Direct Mirror](https://img.shields.io/badge/Зеркало_РФ-ghfast.top-00ADD8?style=for-the-badge)](https://ghfast.top/https://github.com/ANT1VEN0M/BunkerVPN/releases/download/v1.4.6/BunkerVPN-arm64-v8a.apk)

<br/>

<p align="center">
  <a href="https://t.me/BUNKER_KOTA">
    <img src="https://img.shields.io/badge/📢_Канал_Бункера-BUNKER__KOTA-0088cc?style=for-the-badge&logo=telegram&logoColor=white" alt="Telegram Channel"/>
  </a>
  &nbsp;
  <a href="https://t.me/BUNKER_KOTA">
    <img src="https://img.shields.io/badge/👥_Аудитория-1400+_подписчиков-10B981?style=for-the-badge&logo=telegram&logoColor=white" alt="1400 Subscribers"/>
  </a>
  &nbsp;
  <a href="https://finance.ozon.ru/apps/sbp/ozonbankpay/01a1daa7-59df-7009-9fee-796763044ce6" target="_blank">
    <img src="https://img.shields.io/badge/💖_Донат-Озон_Банк_СБП-FF0055?style=for-the-badge&logo=heart&logoColor=white" alt="Донат по СБП"/>
  </a>
  &nbsp;
  <a href="https://t.me/boost/BUNKER_KOTA" target="_blank">
    <img src="https://img.shields.io/badge/🚀_Boost-Telegram-24A1DE?style=for-the-badge&logo=telegram&logoColor=white" alt="Telegram Boost"/>
  </a>
  &nbsp;
  <a href="https://t.me/BUNKER_K0TA">
    <img src="https://img.shields.io/badge/💬_Чат-BUNKER__K0TA-7C3AED?style=for-the-badge&logo=telegram&logoColor=white" alt="Telegram Chat"/>
  </a>
  &nbsp;
  <a href="https://github.com/ANT1VEN0M/BunkerVPN-Windows">
    <img src="https://img.shields.io/badge/💻_Windows_PC-v1.0.12_Live-00E676?style=for-the-badge&logo=windows&logoColor=white" alt="BunkerVPN Windows"/>
  </a>
  &nbsp;
  <a href="https://ant1ven0m.github.io/Bunker-Auto-Engine/">
    <img src="https://img.shields.io/badge/📲_iOS_/_Портал-Bunker_Portal-FF6D00?style=for-the-badge&logo=googlechrome&logoColor=white" alt="Bunker Portal"/>
  </a>
</p>

</div>

---

## ⚡ Что нового в версии v1.4.6

### 🛠️ 1. Исправление верстки в меню «Фильтры и управление»
* **Устранено сплющивание кнопок:** на карточках «Личный WARP+», «Умная чистка» и «Telegram SOS» правые кнопки действий больше не сжимаются в вертикальную колонку букв (1 символ в ширину). Теперь разметка адаптивно рассчитывает ширину и держит аккуратный горизонтальный вид на любых экранах.

### 🗑️ 2. Управление подписками: перманентное удаление и отключение
* **Встроенные каталоги больше не воскресают:** при удалении дефолтных подписок («Микс», «Белый список», «Завод» и др.) выбор пользователя сохраняется навсегда. Фоновые службы и перезапуск приложения больше не пересоздают удалённые подписки.
* **Скрытие отключенных групп:** при отключении тумблера подписки в настройках групп вкладка моментально скрывается с главного экрана.
* **Быстрое восстановление:** добавлена кнопка «Восстановить подписки Бункера» для возврата всех стандартных каталогов в один клик.

### ⚡ 3. Устранение «✕ Таймаут» на карточке «АВТОВЫБОР»
* **Корректный замер TCPing:** исправлена проверка доступности балансировщиков Policy Group — карточка «АВТОВЫБОР» теперь отображает реальный наименьший пинг живых узлов подписки вместо ложного таймаута при активном трафике.

### 🚀 4. Стабильный спидтест без ошибки «connection closed»
* **Отказоустойчивость и новые зеркала:** добавлены проверенные тестовые серверы (Hetzner, Yandex, CacheFly). При закрытии сокета после достаточного замера тест больше не прерывается ошибкой, а выдаёт точный расчёт скорости.

### 🌐 5. Зеркала подписок и ускоренный переход
* **Сокращение таймаутов:** таймаут ожидания зеркал уменьшен до 4 секунд для моментального переключения на резервные прокси (`ghfast.top`, `raw.githack.com`, `ghproxy.net`).
* **Автопроверка пинга:** при ручном обновлении подписки сразу запускается замер пинга всех серверов для наглядного отображения доступности.

---

## ⚡ Что нового в версии v1.4.5

### 🇷🇺 1. 100% русификация всех экранов (устранены все недопереведённые места)
* **Полноценный русский язык из коробки:** базовые ресурсы приложения (`res/values/`) теперь по умолчанию содержат полноценный русский язык.
* **Фикс для Xiaomi HyperOS / MIUI и систем с языком English / CIS:** устранена проблема отката интерфейса на английский (`Config`, `Protection off`, `Collapse`, `Testing: 0 / 165`, `Auto`, `Ping`, `Filters`, `Test`, `All`, `Other`).
* **Понятный главный экран:** заголовок заменён с «Config / Профиль» на **«Серверы»**.
* **Отдельная локализация `values-en/`:** если пользователь явно выбирает английский язык в настройках, приложение остаётся на чистом английском.

### ⚡ 2. Режим «YouTube 4K Турбо» (Блокировка QUIC UDP:443)
* **Анти-Буферизация и пробитие 144p:** на уровне ядра Xray Core блокируются исходящие пакеты UDP:443 (QUIC/HTTP3), через которые ТСПУ РКН душит YouTube.
* **Мгновенное переключение на TCP:** официальное приложение YouTube, ReVanced, SmartTube и браузеры мгновенно переходят на высокоскоростной TCP TLS через VLESS-Reality.
* **Тумблер в меню «Фильтры и управление»:** включён по умолчанию с возможностью мгновенного переключения и перезапуска сервиса.

## ⚡ Что нового в версии v1.4.4 (WireGuard / AmneziaWG Import Fix)

### 📂 1. Нативный импорт `.conf` файлов (WireGuard / AmneziaWG)
* **Импорт из буфера обмена:** скопировал `.conf` файл целиком — приложение автоматически распознаёт блок `[Interface]...[Peer]` и добавляет профиль без лишних шагов.
* **Импорт из файла:** «+» ➔ «Импорт настроек из файла» — любой `.conf` (включая AmneziaWG с полями `Jc`, `Jmin`, `Jmax`, `S1`, `S2`, `I1`) добавляется корректно даже с комментарием-шапкой `# ===` в начале файла.
* **Поддержка `wireguard://` URI:** ссылка `wireguard://...@server:port?...` вставляется из буфера обмена в 1 касание — ключи с символами `/` и `+` больше не вызывают сбой парсера.
* **Авто-присвоение имени:** профиль получает имя из первой информативной строки комментария в `.conf` (например `# BUNKER VPN | Free Fire`).

### ⚙️ 2. Технические улучшения парсера
* `WireguardFmt.parse()` — авто-экранирование `PrivateKey` при наличии `/` или `+` перед передачей в Java URI.
* `BunkerUniversalImporter.extractConfigs()` — добавлен детектор WireGuard INI-блоков с конвертацией в `wireguard://` URI для единого pipeline импорта.
* `AngConfigManager.parseCustomConfigServer()` — проверка `[Interface]` теперь нечувствительна к регистру и расположению блока.

## ⚡ Что нового в версии v1.4.3 Hotfix

### 🛡️ 1. Исправление раздельного туннелирования (Split Tunneling / Выбор приложений)
* **Починен режим «Только выбранные приложения» (Allowlist):** устранено исключение ядра Android `VpnService` (`UnsupportedOperationException`), блокировавшее добавление приложений в туннель при активном тумблере «Использовать выбор» и выключенном «Обход».
* **Автоматическое подключение сервисов Google для YouTube:** выбор YouTube в списке приложений теперь автоматически проксирует `com.google.android.gms`, устраняя вечную буферизацию роликов и сбои авторизации.
* **Авто-перезапуск сервиса:** при переключении тумблеров «Использовать выбор» и «Обход» сервис VPN автоматически перезапускается с новыми правилами маршрутизации при возврате на главный экран.

## ⚡ Что нового в версии v1.4.2 Hotfix

### 🛡️ 1. Фикс раздельного туннелирования на Android 14 / 15 / 16 / 17
* **Устранён вылет Jetpack Compose:** на современных Android с включённым «Частным пространством» (Private Space), Dual Apps (Xiaomi/Samsung) или Work Profile список приложений возвращал дублирующиеся пакеты, вызывая сбой уникальных ключей в LazyColumn. Внедрена трёхуровневая дедупликация.
* **Защита IPC Binder от переполнения (1MB):** убран избыточный флаг `PackageManager.GET_PERMISSIONS`, вызывавший падение `TransactionTooLargeException`.
* **Безопасная обработка иконок Coil 3:** перехват исключений при рендеринге системных и адаптивных значков приложений.
* **Безопасный VpnService Builder:** перехват исключений при добавлении системных пакетов в туннель обхода.

### 🖼️ 2. Полноценная поддержка кастомных обоев (Pixel 8 / Android 14–17)
* **Исправлена установка фона из галереи:** режим Stealth больше не блокирует отображение выбранного фона.
* **Google Photo Picker API:** интеграция современного системного контракта `PickVisualMedia` взамен устаревшего `GetContent()`.
* **Безопасное сжатие (OOM Protection):** аппаратное масштабирование тяжелых 50-мегапиксельных снимков камер Pixel 8 во избежание переполнения памяти Android.
* **Мгновенное обновление:** смена фонового изображения применяется на экране без необходимости перезапуска приложения.

### 🖤 2. Режим «Stealth» (Минимализм)
* **Чистый OLED Black (`#000000`):** минимальное энергопотребление экрана для AMOLED-дисплеев.
* **Нативная компактная шапка (48dp):** лаконичный статус-бар с векторными системными иконками.
* **Чистый интерфейс без мусора (`unslop-ui`):** убран лишний визуальный шум и дублирующие элементы.

### 🛡️ 3. Безотказный Cloudflare WARP+ (Fail-Safe)
* **Порт 880 + Handshake Client ID:** обходит блокировки протокола WireGuard ТСПУ в РФ с ультра-низким пингом (~45–55 мс).
* **100% гарантия генерации офлайн:** встроенный пул готовых ключей, мгновенный подхват профиля.

### 🔒 4. Аппаратный Kill-Switch без падения Wi-Fi
* **Защита от утечек DNS и трафика:** системные демоны Android (`netd` / `dns_resolver`) корректно авторизованы в правилах.
* **Поддержка NDP / ICMPv6:** роутер не теряет связь со смартфоном по Wi-Fi при включенном Kill-Switch.

### 🎨 5. Поддержка Themed Icon в стиле Google Pixel (Material You / Android 13+)
* **Адаптивная векторная монохромная иконка:** силуэт кота Бункера автоматически окрашивается в акцентные цвета системной темы Google Pixel и Android 13+.
* **Идеальная гармония:** единый визуальный стиль рабочего стола с быстрыми переключателями в шторке.

---

## 📦 Скачать BunkerVPN v1.4.6

| Версия APK | Назначение | Прямая ссылка (РФ без VPN) | GitHub Release |
| :--- | :--- | :--- | :--- |
| **BunkerVPN-arm64-v8a.apk** | 99% современных смартфонов | [⚡ Скачать с ghfast.top](https://ghfast.top/https://github.com/ANT1VEN0M/BunkerVPN/releases/download/v1.4.6/BunkerVPN-arm64-v8a.apk) | [GitHub](https://github.com/ANT1VEN0M/BunkerVPN/releases/download/v1.4.6/BunkerVPN-arm64-v8a.apk) |
| **BunkerVPN-universal.apk** | Все архитектуры и эмуляторы | [⚡ Скачать с ghfast.top](https://ghfast.top/https://github.com/ANT1VEN0M/BunkerVPN/releases/download/v1.4.6/BunkerVPN-universal.apk) | [GitHub](https://github.com/ANT1VEN0M/BunkerVPN/releases/download/v1.4.6/BunkerVPN-universal.apk) |

---

## 📡 Сетевой монитор: Bunker URL Test (v1.1.2 Universal)

Автономный Live Network Monitor доступности интернет-сервисов и блокировок ТСПУ для Android:
* 👉 **[Репозиторий Bunker-Url-Test](https://github.com/ANT1VEN0M/Bunker-Url-Test)**
* 📐 **Edge-to-Edge UI:** нативная обработка WindowInsets под статус-бар и вырезы экрана.
* ⚡ **Параллельная асинхронная проверка coroutines:** мгновенный опрос 4 ключевых сервисов (Google, Telegram, Instagram, YouTube) за 1–2 секунды без зависаний.
* 🔐 **Постоянный релизный ключ:** универсальный APK с подписью V1+V2 (до 2054 года) для бесшовных обновлений поверх любых версий.
* 📥 **Скачать APK (v1.1.2):** [Bunker-Url-Test-v1.1.2-universal.apk](https://ghfast.top/https://github.com/ANT1VEN0M/Bunker-Url-Test/releases/download/v1.1.2/Bunker-Url-Test-v1.1.2-universal.apk) *(Зеркало РФ)* | [GitHub Релиз](https://github.com/ANT1VEN0M/Bunker-Url-Test/releases/tag/v1.1.2)

---

## 💻 Клиент для ПК: BunkerVPN for Windows (v1.0.12 — Anti-Freeze Edition)

Теперь у Бункера есть собственный мощный и лёгкий нативный клиент для компьютеров и ноутбуков (Windows 10 / 11):
* 👉 **[Репозиторий BunkerVPN-Windows](https://github.com/ANT1VEN0M/BunkerVPN-Windows)**
* 🎮 **Режим Game Booster & Discord Fix:** Full-Cone NAT (`endpoint_independent_nat: true`) и безопасный MTU `1340` без фрагментации UDP. Мгновенное подключение к войсам и стримам Discord без зависания на «RTC Connecting».
* ⚡ **Умный гейминг-сплит:** голосовые каналы Discord, серверы Supercell (Brawl Stars), Roblox и Steam Community идут через VPN, а игровой трафик CS2, Dota 2, Valorant, Мир Танков и загрузки обновлений Steam CDN — напрямую с родным пингом 5–15 мс и гигабитной скоростью.
* 🩺 **Сохранение и экспорт логов:** кнопки «💾 В файл», «📋 Копировать», «📂 Папка» в разделе «Консоль / Логи».
* 🛡️ **Real TLS Handshake Ping:** заменён слепой TCP-коннект на реальное TLS-рукопожатие. Недоступные серверы получают `—` и не поднимаются наверх.
* 🛡️ **Anti-Freeze при отключении:** процесс отключения переведён в полностью асинхронный фоновый поток.
* 🔄 **1-Click Автообновление & Трей:** сворачивается возле часов, автозапуск, автообновление в один клик.
* 📥 **Скачать v1.0.12:** [Установщик (Setup.exe)](https://github.com/ANT1VEN0M/BunkerVPN-Windows/releases/download/v1.0.12/BunkerVPN-Windows-v1.0.12-Setup.exe) *(Зеркало РФ: [ghfast.top](https://ghfast.top/https://github.com/ANT1VEN0M/BunkerVPN-Windows/releases/download/v1.0.12/BunkerVPN-Windows-v1.0.12-Setup.exe))* | [Портативная версия (ZIP)](https://github.com/ANT1VEN0M/BunkerVPN-Windows/releases/download/v1.0.12/BunkerVPN-Windows-v1.0.12-Portable.zip) *(Зеркало РФ: [ghfast.top](https://ghfast.top/https://github.com/ANT1VEN0M/BunkerVPN-Windows/releases/download/v1.0.12/BunkerVPN-Windows-v1.0.12-Portable.zip))*

---

## 💖 Поддержка проекта Бункер Кота (Донаты)

Бункер развивается полностью независимо на чистом энтузиазме! Если BunkerVPN помогает вам оставаться на связи на максимальной скорости — поддержите проект:
- 💳 **Перевести по СБП (Озон Банк в 1 клик):** [https://finance.ozon.ru/apps/sbp/ozonbankpay/01a1daa7-59df-7009-9fee-796763044ce6](https://finance.ozon.ru/apps/sbp/ozonbankpay/01a1daa7-59df-7009-9fee-796763044ce6)
- 🚀 **Прокачать канал (Telegram Boost):** [https://t.me/boost/BUNKER_KOTA](https://t.me/boost/BUNKER_KOTA)
- 💎 **Криптовалюта (USDT / TON) и связь:** [@k0tlux](https://t.me/k0tlux)

---

## 🔌 Фирменные плагины для Telegram (AyuGram / ExteraGram / NimarkoGram)

* 💫 **[Bunker Neon Loader v1.3.1](https://github.com/ANT1VEN0M/Bunker-Auto-Engine/raw/main/plugins/bunker_neon_loader.plugin)** — Неоновый RGB-спиннер/лоадер загрузки медиа, автообновление, кастомная палитра.
* 🌐 **[Bunker Chat Translator v1.2.7](https://github.com/ANT1VEN0M/Bunker-Auto-Engine/raw/main/plugins/bunker_chat_translator.plugin)** — Инлайн-переводчик чатов в реальном времени (Anti-Crash Edition, плавный запуск).
* 📋 **Репозиторий плагинов:** `https://raw.githubusercontent.com/ANT1VEN0M/Bunker-Auto-Engine/main/plugins/extera-plugins.json`

---

## 🍏 Подключение для пользователей iPhone (iOS)

Воспользуйтесь нашим официальным **[Веб-порталом Бункера](https://ant1ven0m.github.io/Bunker-Auto-Engine/)**:
* Добавление подписки в 1 клик через **Karing** (App Store)
* Автоматическое обновление серверов каждый час

---

## 🛠 Поддерживаемые протоколы
* **VLESS** (XTLS-Reality, Vision, xhttp, gRPC, WebSocket)
* **WireGuard WARP+** (порт 880 + handshake reserved bytes)
* **Cloudflare WARP-MASQUE** (HTTP/3 QUIC CONNECT-IP)
* **Hysteria 2** (быстрый UDP-туннель на базе QUIC)
* **Trojan** (TLS)
* **Shadowsocks** (2022 aead)
* **VMess**

---

## 💬 Сообщество и контакты
* 📢 **Канал Бункера:** [@BUNKER_KOTA](https://t.me/BUNKER_KOTA) *(1400+ подписчиков)*
* 💬 **Чат сообщества:** [@BUNKER_K0TA](https://t.me/BUNKER_K0TA)
* 🤖 **Telegram Бот:** [@BunkerCatOSBot](https://t.me/BunkerCatOSBot)
* 👑 **Создатель проекта:** [@k0tlux](https://t.me/k0tlux)

---

## ⚡ Взаимопиар и Реклама | Открыт к сотрудничеству
* 👥 **Аудитория канала:** 1400+ активных подписчиков
* 🤝 **Взаимопиар:** условия обсуждаются индивидуально
* 💰 **Прайс на рекламу в «Бункере Кота»:**
  * ▫️ 1 рекламный пост — **50 ₽**
  * ▫️ 2 рекламных поста — **80 ₽**
  * ▫️ 3 рекламных поста — **120 ₽**
  * ▫️ Закреп на 24 часа — **+30 ₽**
* 📩 **По вопросам рекламы и сотрудничества:** [@k0tlux](https://t.me/k0tlux)
