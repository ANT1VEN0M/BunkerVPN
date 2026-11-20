<div align="center">

# 🐈‍⬛ Bunker VPN for Android (v1.3.9)

**Флагманский Android VPN-клиент экосистемы «Бункер Кота» на базе Xray Core Lite и Jetpack Compose.**  
Создан для скоростного и безотказного обхода любых типов блокировок ТСПУ РКН в России.

[![Release](https://img.shields.io/github/v/release/ANT1VEN0M/BunkerVPN?color=7C3AED&label=Релиз&style=for-the-badge)](https://github.com/ANT1VEN0M/BunkerVPN/releases/latest)
[![Platform](https://img.shields.io/badge/Платформа-Android_8.0+-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/ANT1VEN0M/BunkerVPN/releases/latest)
[![Direct Mirror](https://img.shields.io/badge/Зеркало_РФ-ghfast.top-00ADD8?style=for-the-badge)](https://ghfast.top/https://github.com/ANT1VEN0M/BunkerVPN/releases/download/v1.3.9/BunkerVPN-arm64-v8a.apk)

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
  <a href="https://ant1ven0m.github.io/Bunker-Auto-Engine/">
    <img src="https://img.shields.io/badge/🍏_iPhone_/_ПК-Web_Портал-10B981?style=for-the-badge&logo=safari&logoColor=white" alt="Web Portal"/>
  </a>
</p>

</div>

---

## ⚡ Что нового в версии v1.3.9

### 🖤 1. Режим «Stealth» (Минимализм) по умолчанию
* **Чистый OLED Black (`#000000`):** нулевое энергопотребление экрана, отсутствие фоновых картинок и неонового свечения.
* **Нативная компактная шапка (48dp):** вместо громоздкой плашки — лаконичный статус-бар с векторными системными иконками (щит/замок).
* **Дизайн без нейрослопа (`unslop-ui`):** убран эмодзи-спам с кнопок управления (`Авто`, `Пинг`, `Фильтры`, `Тест`) и дублирующие тексты.
* **Чистые имена вкладок:** автоматическая миграция названий вкладок (`Микс`, `Белый список`, `Завод 443/53`, `Турбо`, `Telegram SOS`) без лишних префиксов.

### 🛡️ 2. Безотказный Cloudflare WARP+ (Fail-Safe)
* **Порт 880 + Handshake Client ID:** обходит блокировки протокола WireGuard ТСПУ в РФ с ультра-низким пингом (~45–55 мс).
* **100% гарантия генерации офлайн:** устранена ошибка *«Не удалось зарегистрировать WARP (проверьте интернет)»*. В приложение вшит офлайн-пул готовых ключей, профиль создается мгновенно даже при выключенной сети или жестких блокировках Cloudflare API.

### 🔒 3. Аппаратный Kill-Switch без падения Wi-Fi
* **Защита от утечек трафика и DNS:** системные демоны Android (`netd` / `dns_resolver`) корректно авторизованы в правилах iptables.
* **Поддержка NDP / ICMPv6:** роутер больше не теряет связь со смартфоном по Wi-Fi.

### 🚀 4. Скоростные зеркала обновлений (`ghfast.top`)
* Проверка обновлений и скачивание новых APK работает напрямую в РФ без необходимости включать сторонний VPN.

---

## 📦 Скачать BunkerVPN v1.3.9

| Версия APK | Назначение | Прямая ссылка (РФ без VPN) | GitHub Release |
| :--- | :--- | :--- | :--- |
| **BunkerVPN-arm64-v8a.apk** | 99% современных смартфонов | [⚡ Скачать с ghfast.top](https://ghfast.top/https://github.com/ANT1VEN0M/BunkerVPN/releases/download/v1.3.9/BunkerVPN-arm64-v8a.apk) | [GitHub](https://github.com/ANT1VEN0M/BunkerVPN/releases/download/v1.3.9/BunkerVPN-arm64-v8a.apk) |
| **BunkerVPN-universal.apk** | Все архитектуры и эмуляторы | [⚡ Скачать с ghfast.top](https://ghfast.top/https://github.com/ANT1VEN0M/BunkerVPN/releases/download/v1.3.9/BunkerVPN-universal.apk) | [GitHub](https://github.com/ANT1VEN0M/BunkerVPN/releases/download/v1.3.9/BunkerVPN-universal.apk) |

---

## 🍏 Подключение для пользователей iPhone (iOS) и ПК (Windows / Mac)

Если у вас или ваших друзей iPhone или компьютер, воспользуйтесь нашим **[Веб-порталом Бункера](https://ant1ven0m.github.io/Bunker-Auto-Engine/)**:
* Подключение в 1 клик через приложение **Karing** (App Store)
* Поддержка **Hiddify**, **v2rayN**, **Streisand**, **Shadowrocket**
* 470+ актуальных серверов, обновляемых каждые 6 часов

---

## 🛠 Поддерживаемые протоколы
* **VLESS** (XTLS-Reality, Vision, xhttp, gRPC, WebSocket)
* **WireGuard WARP+** (порт 880 + handshake reserved bytes)
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
