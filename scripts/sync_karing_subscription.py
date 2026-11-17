#!/usr/bin/env python3
"""
Sync and convert Alexander's subscription (Xray JSON array)
into Karing-compatible and Clash-compatible formats:
1. bunker_karing.txt (standard URI links with metadata headers)
2. bunker_karing_b64.txt (base64 encoded URI list)
3. bunker_karing_clash.yaml (full Clash Meta configuration)
"""

import urllib.request
import json
import yaml
import base64
import urllib.parse
import os
import sys

SOURCE_URL = os.environ.get(
    "SUB_SOURCE_URL",
    "https://alexander-official.ru/sub/65d1b746a3a40f6368a9b609"
)

def fetch_and_convert():
    print(f"Fetching subscription from: {SOURCE_URL}")
    req = urllib.request.Request(
        SOURCE_URL,
        headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"}
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        raw = resp.read().decode("utf-8")
        data = json.loads(raw)

    links = []
    clash_proxies = []

    for item in data:
        remarks = item.get("remarks", "").strip()
        if "Автовыбор" in remarks:
            continue
        for ob in item.get("outbounds", []):
            proto = ob.get("protocol")
            stream = ob.get("streamSettings", {})
            network = stream.get("network", "tcp")
            security = stream.get("security", "none")
            tag = remarks or ob.get("tag", "proxy")
            tag_encoded = urllib.parse.quote(tag)

            if proto == "vless":
                vnext = ob["settings"]["vnext"][0]
                addr = vnext["address"]
                port = vnext["port"]
                user = vnext["users"][0]
                uuid = user["id"]
                flow = user.get("flow", "")

                params = {}
                if security: params["security"] = security
                if flow: params["flow"] = flow
                params["type"] = network

                if security == "reality":
                    rs = stream.get("realitySettings", {})
                    if rs.get("serverName"): params["sni"] = rs["serverName"]
                    if rs.get("fingerprint"): params["fp"] = rs["fingerprint"]
                    if rs.get("publicKey"): params["pbk"] = rs["publicKey"]
                    if rs.get("shortId"): params["sid"] = rs["shortId"]
                    if rs.get("spiderX"): params["spx"] = rs["spiderX"]
                elif security == "tls":
                    ts = stream.get("tlsSettings", {})
                    if ts.get("serverName"): params["sni"] = ts["serverName"]
                    if ts.get("fingerprint"): params["fp"] = ts["fingerprint"]
                    if ts.get("alpn"): params["alpn"] = ",".join(ts["alpn"])

                if network == "ws":
                    ws = stream.get("wsSettings", {})
                    if ws.get("path"): params["path"] = ws["path"]
                    if ws.get("headers", {}).get("Host"): params["host"] = ws["headers"]["Host"]
                elif network == "grpc":
                    gs = stream.get("grpcSettings", {})
                    if gs.get("serviceName"): params["serviceName"] = gs["serviceName"]
                elif network in ("xhttp", "httpupgrade"):
                    xs = stream.get("xhttpSettings", {}) or stream.get("httpupgradeSettings", {})
                    if xs.get("path"): params["path"] = xs["path"]
                    if xs.get("host"): params["host"] = xs["host"]

                query = urllib.parse.urlencode(params)
                links.append(f"vless://{uuid}@{addr}:{port}?{query}#{tag_encoded}")

                # Clash configuration item
                cp = {
                    "name": tag,
                    "type": "vless",
                    "server": addr,
                    "port": port,
                    "uuid": uuid,
                    "udp": True,
                    "network": network
                }
                if flow: cp["flow"] = flow
                if security == "reality":
                    rs = stream.get("realitySettings", {})
                    cp["tls"] = True
                    cp["reality-opts"] = {
                        "public-key": rs.get("publicKey", ""),
                        "short-id": rs.get("shortId", "")
                    }
                    if rs.get("serverName"): cp["servername"] = rs["serverName"]
                    if rs.get("fingerprint"): cp["client-fingerprint"] = rs["fingerprint"]
                elif security == "tls":
                    ts = stream.get("tlsSettings", {})
                    cp["tls"] = True
                    if ts.get("serverName"): cp["servername"] = ts["serverName"]
                    if ts.get("fingerprint"): cp["client-fingerprint"] = ts["fingerprint"]
                    if ts.get("alpn"): cp["alpn"] = ts["alpn"]

                if network == "ws":
                    ws = stream.get("wsSettings", {})
                    cp["ws-opts"] = {
                        "path": ws.get("path", "/"),
                        "headers": ws.get("headers", {})
                    }
                elif network == "grpc":
                    gs = stream.get("grpcSettings", {})
                    cp["grpc-opts"] = {
                        "grpc-service-name": gs.get("serviceName", "")
                    }
                elif network in ("xhttp", "httpupgrade"):
                    xs = stream.get("xhttpSettings", {}) or stream.get("httpupgradeSettings", {})
                    cp["network"] = "http"
                    cp["http-opts"] = {
                        "path": [xs.get("path", "/")],
                        "headers": {"Host": [xs.get("host", addr)]}
                    }
                clash_proxies.append(cp)

            elif proto == "hysteria":
                sett = ob.get("settings", {})
                hs = stream.get("hysteriaSettings", {})
                ts = stream.get("tlsSettings", {})
                addr = sett.get("address")
                port = sett.get("port")
                auth = hs.get("auth")
                sni = ts.get("serverName", "")
                alpn = ",".join(ts.get("alpn", ["h3"]))
                insecure = 1 if ts.get("allowInsecure") else 0
                links.append(f"hysteria2://{auth}@{addr}:{port}/?sni={sni}&alpn={alpn}&insecure={insecure}#{tag_encoded}")

                cp = {
                    "name": tag,
                    "type": "hysteria2",
                    "server": addr,
                    "port": port,
                    "password": auth,
                    "sni": sni,
                    "skip-cert-verify": bool(ts.get("allowInsecure")),
                    "alpn": ts.get("alpn", ["h3"])
                }
                clash_proxies.append(cp)

            elif proto == "shadowsocks":
                srv = ob["settings"]["servers"][0]
                method = srv["method"]
                pwd = srv["password"]
                addr = srv["address"]
                port = srv["port"]
                userinfo = base64.b64encode(f"{method}:{pwd}".encode()).decode()
                links.append(f"ss://{userinfo}@{addr}:{port}#{tag_encoded}")

                cp = {
                    "name": tag,
                    "type": "ss",
                    "server": addr,
                    "port": port,
                    "cipher": method,
                    "password": pwd,
                    "udp": True
                }
                clash_proxies.append(cp)

    print(f"Total extracted proxies: {len(links)}")

    header_lines = [
        "#profile-title: Бункер Кота ⚡",
        "#profile-update-interval: 1",
        "#support-url: https://t.me/BUNKER_KOTA",
        "#profile-web-page-url: https://t.me/BUNKER_KOTA",
        "# 🐈‍⬛ Бункер Кота | VPN • Proxy TG",
        "# 📢 Наш Telegram: https://t.me/BUNKER_KOTA",
        "# 💬 Чат: https://t.me/BUNKER_K0TA",
        ""
    ]
    txt_content = "\n".join(header_lines + links)
    b64_content = base64.b64encode("\n".join(links).encode("utf-8")).decode("utf-8")

    proxy_names = [p["name"] for p in clash_proxies]
    clash_config = {
        "port": 7890,
        "socks-port": 7891,
        "allow-lan": False,
        "mode": "rule",
        "log-level": "info",
        "unified-delay": True,
        "tcp-concurrent": True,
        "proxies": clash_proxies,
        "proxy-groups": [
            {
                "name": "⚡ БУНКЕР КОТА",
                "type": "select",
                "proxies": ["🚀 Автовыбор (Быстрый пинг)", "♻️ Fallback (Отказоустойчивый)", "DIRECT"] + proxy_names
            },
            {
                "name": "🚀 Автовыбор (Быстрый пинг)",
                "type": "url-test",
                "url": "https://cp.cloudflare.com/generate_204",
                "interval": 300,
                "tolerance": 50,
                "proxies": proxy_names
            },
            {
                "name": "♻️ Fallback (Отказоустойчивый)",
                "type": "fallback",
                "url": "https://cp.cloudflare.com/generate_204",
                "interval": 300,
                "proxies": proxy_names
            }
        ],
        "rules": [
            "MATCH,⚡ БУНКЕР КОТА"
        ]
    }
    yaml_content = yaml.dump(clash_config, allow_unicode=True, sort_keys=False)

    out_dir = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "subscriptions")
    os.makedirs(out_dir, exist_ok=True)

    with open(os.path.join(out_dir, "bunker_karing.txt"), "w", encoding="utf-8") as f:
        f.write(txt_content)
    with open(os.path.join(out_dir, "bunker_karing_b64.txt"), "w", encoding="utf-8") as f:
        f.write(b64_content)
    with open(os.path.join(out_dir, "bunker_karing_clash.yaml"), "w", encoding="utf-8") as f:
        f.write(yaml_content)

    print(f"Successfully saved files to {out_dir}")

if __name__ == "__main__":
    fetch_and_convert()
