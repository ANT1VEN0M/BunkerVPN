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
  <a href="https://t.me/BUNKER_K0TA">
    <img src="https://img.shields.io/badge/💬_Чат_Сообщества-BUNKER__K0TA-7C3AED?style=for-the-badge&logo=telegram&logoColor=white" alt="Telegram Chat"/>
  </a>
  &nbsp;
  <a href="https://github.com/ANT1VEN0M/BunkerVPN-Windows">
    <img src="https://img.shields.io/badge/💻_Клиент_для_Windows-BunkerVPN_PC-00E5FF?style=for-the-badge&logo=windows&logoColor=white" alt="BunkerVPN Windows"/>
  </a>
  &nbsp;
  <a href="https://ant1ven0m.github.io/Bunker-Auto-Engine/">
    <img src="https://img.shields.io/badge/📲_iOS_/_Портал-Bunker_Portal-0088cc?style=for-the-badge&logo=telegram&logoColor=white" alt="Bunker Portal"/>
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

---

## 📦 Скачать BunkerVPN v1.4.1

| Версия APK | Назначение | Прямая ссылка (РФ без VPN) | GitHub Release |
| :--- | :--- | :--- | :--- |
| **BunkerVPN-arm64-v8a.apk** | 99% современных смартфонов | [⚡ Скачать с ghfast.top](https://ghfast.top/https://github.com/ANT1VEN0M/BunkerVPN/releases/download/v1.4.1/BunkerVPN-arm64-v8a.apk) | [GitHub](https://github.com/ANT1VEN0M/BunkerVPN/releases/download/v1.4.1/BunkerVPN-arm64-v8a.apk) |
| **BunkerVPN-universal.apk** | Все архитектуры и эмуляторы | [⚡ Скачать с ghfast.top](https://ghfast.top/https://github.com/ANT1VEN0M/BunkerVPN/releases/download/v1.4.1/BunkerVPN-universal.apk) | [GitHub](https://github.com/ANT1VEN0M/BunkerVPN/releases/download/v1.4.1/BunkerVPN-universal.apk) |

---

## 💻 Клиент для ПК: BunkerVPN for Windows (v1.0.1)

Теперь у Бункера есть собственный нативный клиент для компьютеров и ноутбуков:
* 👉 **[Репозиторий BunkerVPN-Windows](https://github.com/ANT1VEN0M/BunkerVPN-Windows)**
* 🚀 **TUN-режим (WinTUN):** весь трафик ПК, Discord Voice (без лагов RTC) и онлайн-игры.
* 🛡️ **Умный обход (Bypass Russia):** сайты РФ напрямую, заблокированные — через туннель.
* 📥 [Скачать установщик (Setup.exe)](https://github.com/ANT1VEN0M/BunkerVPN-Windows/releases/download/v1.0.1/BunkerVPN-Windows-v1.0.1-Setup.exe) | [Портативная версия (ZIP)](https://github.com/ANT1VEN0M/BunkerVPN-Windows/releases/download/v1.0.1/BunkerVPN-Windows-v1.0.1-Portable.zip)

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
* 📢 **Канал Бункера:** [@BUNKER_KOTA](https://t.me/BUNKER_KOTA)
* 💬 **Чат сообщества:** [@BUNKER_K0TA](https://t.me/BUNKER_K0TA)
* 🤖 **Telegram Бот:** [@BunkerCatOSBot](https://t.me/BunkerCatOSBot)
* 👑 **Создатель проекта:** [@k0tlux](https://t.me/k0tlux)
