<div align="center">

# 🐈‍⬛ Bunker VPN for Android (v1.4.1)

**Флагманский Android VPN-клиент экосистемы «Бункер Кота» на базе Xray Core Lite и Jetpack Compose.**  
Создан для скоростного и безотказного обхода любых типов блокировок ТСПУ РКН в России.

[![Release](https://img.shields.io/github/v/release/ANT1VEN0M/BunkerVPN?color=7C3AED&label=Релиз&style=for-the-badge)](https://github.com/ANT1VEN0M/BunkerVPN/releases/latest)
[![Platform](https://img.shields.io/badge/Платформа-Android_8.0+-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/ANT1VEN0M/BunkerVPN/releases/latest)
[![Direct Mirror](https://img.shields.io/badge/Зеркало_РФ-ghfast.top-00ADD8?style=for-the-badge)](https://ghfast.top/https://github.com/ANT1VEN0M/BunkerVPN/releases/download/v1.4.1/BunkerVPN-arm64-v8a.apk)

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
    <img src="https://img.shields.io/badge/💻_Windows_PC-v1.0.8_Live-00E676?style=for-the-badge&logo=windows&logoColor=white" alt="BunkerVPN Windows"/>
  </a>
  &nbsp;
  <a href="https://ant1ven0m.github.io/Bunker-Auto-Engine/">
    <img src="https://img.shields.io/badge/📲_iOS_/_Портал-Bunker_Portal-FF6D00?style=for-the-badge&logo=googlechrome&logoColor=white" alt="Bunker Portal"/>
  </a>
</p>

</div>

---

## ⚡ Что нового в версии v1.4.1

### 🖼️ 1. Полноценная поддержка кастомных обоев (Pixel 8 / Android 14–17)
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

## 📦 Скачать BunkerVPN v1.4.1

| Версия APK | Назначение | Прямая ссылка (РФ без VPN) | GitHub Release |
| :--- | :--- | :--- | :--- |
| **BunkerVPN-arm64-v8a.apk** | 99% современных смартфонов | [⚡ Скачать с ghfast.top](https://ghfast.top/https://github.com/ANT1VEN0M/BunkerVPN/releases/download/v1.4.1/BunkerVPN-arm64-v8a.apk) | [GitHub](https://github.com/ANT1VEN0M/BunkerVPN/releases/download/v1.4.1/BunkerVPN-arm64-v8a.apk) |
| **BunkerVPN-universal.apk** | Все архитектуры и эмуляторы | [⚡ Скачать с ghfast.top](https://ghfast.top/https://github.com/ANT1VEN0M/BunkerVPN/releases/download/v1.4.1/BunkerVPN-universal.apk) | [GitHub](https://github.com/ANT1VEN0M/BunkerVPN/releases/download/v1.4.1/BunkerVPN-universal.apk) |

---

## 💻 Клиент для ПК: BunkerVPN for Windows (v1.0.8 — Game Booster Edition)

Теперь у Бункера есть собственный мощный и лёгкий нативный клиент для компьютеров и ноутбуков (Windows 10 / 11):
* 👉 **[Репозиторий BunkerVPN-Windows](https://github.com/ANT1VEN0M/BunkerVPN-Windows)**
* 🎮 **Режим Game Booster & Discord Fix:** Full-Cone NAT (`endpoint_independent_nat: true`) и безопасный MTU `1340` без фрагментации UDP. Мгновенное подключение к войсам и стримам Discord без зависания на «RTC Connecting».
* ⚡ **Умный гейминг-сплит:** голосовые каналы Discord, серверы Supercell (Brawl Stars), Roblox и Steam Community идут через VPN, а игровой трафик CS2, Dota 2, Valorant, Мир Танков и загрузки обновлений Steam CDN — напрямую с родным пингом 5–15 мс и гигабитной скоростью.
* 🛡️ **Совместимость с Sing-box 1.11+:** TUN запускается стабильно без сбоев.
* 🚀 **TUN-режим (WinTUN):** весь трафик ПК, онлайн-игры, торренты и фоновые службы.
* 🛡️ **Виджет статуса сервисов ТСПУ:** живой статус разблокировки YouTube 4K, Discord, Telegram, AI, Insta/X, Банков и Госуслуг прямо на главном экране.
* 📶 **Автосортировка по пингу & URL Тест (HTTP 204):** автоматический выбор лучших серверов.
* 🛡️ **Умный обход (Smart Bypass Russia):** сайты РФ напрямую на скорости домашнего провайдера, заблокированные — через туннель.
* 🔄 **1-Click Автообновление & Трей:** сворачивается возле часов, автозапуск, автообновление в один клик.
* 📥 **Скачать v1.0.8:** [Установщик (Setup.exe)](https://github.com/ANT1VEN0M/BunkerVPN-Windows/releases/download/v1.0.8/BunkerVPN-Windows-v1.0.8-Setup.exe) *(Зеркало РФ: [ghfast.top](https://ghfast.top/https://github.com/ANT1VEN0M/BunkerVPN-Windows/releases/download/v1.0.8/BunkerVPN-Windows-v1.0.8-Setup.exe))* | [Портативная версия (ZIP)](https://github.com/ANT1VEN0M/BunkerVPN-Windows/releases/download/v1.0.8/BunkerVPN-Windows-v1.0.8-Portable.zip) *(Зеркало РФ: [ghfast.top](https://ghfast.top/https://github.com/ANT1VEN0M/BunkerVPN-Windows/releases/download/v1.0.8/BunkerVPN-Windows-v1.0.8-Portable.zip))*

---

## 💖 Поддержка проекта Бункер Кота (Донаты)

Бункер развивается полностью независимо на чистом энтузиазме! Если BunkerVPN помогает вам оставаться на связи на максимальной скорости — поддержите проект:
- 💳 **Перевести по СБП (Озон Банк в 1 клик):** [https://finance.ozon.ru/apps/sbp/ozonbankpay/01a1daa7-59df-7009-9fee-796763044ce6](https://finance.ozon.ru/apps/sbp/ozonbankpay/01a1daa7-59df-7009-9fee-796763044ce6)
- 🚀 **Прокачать канал (Telegram Boost):** [https://t.me/boost/BUNKER_KOTA](https://t.me/boost/BUNKER_KOTA)
- 💎 **Криптовалюта (USDT / TON) и связь:** [@k0tlux](https://t.me/k0tlux)

---

## 🔌 Фирменные плагины для Telegram (AyuGram / ExteraGram / NimarkoGram)

* 💫 **[Bunker Neon Loader v1.2.0](https://github.com/ANT1VEN0M/Bunker-Auto-Engine/raw/main/plugins/bunker_neon_loader.plugin)** — Неоновый RGB-спиннер/лоадер загрузки медиа, автообновление, кастомная палитра.
* 🌐 **[Bunker Chat Translator v1.2.6](https://github.com/ANT1VEN0M/Bunker-Auto-Engine/raw/main/plugins/bunker_chat_translator.plugin)** — Инлайн-переводчик чатов в реальном времени без сторонних API-ключей.
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
