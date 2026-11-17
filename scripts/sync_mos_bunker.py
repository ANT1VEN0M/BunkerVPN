#!/usr/bin/env python3
"""
Sync Bunker subscriptions from Mos.ru GitLab (hub.mos.ru)
and generate high-performance Clash Meta (Mihomo) & Karing YAML configs:
1. bunker_mix_clash.yaml (Бункер МИКС ⚡)
2. bunker_whitelist_clash.yaml (Бункер Белый список 🛡)
3. bunker_full_clash.yaml (Все серверы МИКС + БС вместе)
"""

import urllib.request
import urllib.parse
import json
import base64
import yaml
import re
import os
import sys

MOS_BLWL_URL = os.environ.get(
    "MOS_BLWL_URL",
    "https://hub.mos.ru/akelladejavu/bunker/-/raw/main/BLWL_MIX.txt"
)
MOS_WHITELIST_URL = os.environ.get(
    "MOS_WHITELIST_URL",
    "https://hub.mos.ru/akelladejavu/bunker/-/raw/main/WHITE_LIST.txt"
)

FLAG_TO_COUNTRY = {
    '🇸🇪': 'Швеция',
    '🇩🇪': 'Германия',
    '🇧🇪': 'Бельгия',
    '🇵🇱': 'Польша',
    '🇪🇪': 'Эстония',
    '🇫🇮': 'Финляндия',
    '🇷🇺': 'Россия',
    '🇳🇱': 'Нидерланды',
    '🇦🇹': 'Австрия',
    '🇰🇵': 'Северная Корея',
    '🇱🇹': 'Литва',
    '🇮🇱': 'Израиль',
    '🇺🇸': 'США',
    '🇬🇧': 'Великобритания',
    '🇹🇷': 'Турция',
    '🇭🇰': 'Гонконг',
    '🇸🇬': 'Сингапур',
    '🇹🇼': 'Тайвань',
    '🇮🇷': 'Иран',
    '🇸🇮': 'Словения',
    '🇦🇩': 'Андорра',
    '🇭🇺': 'Венгрия',
    '🇺🇦': 'Украина',
    '🇨🇦': 'Канада',
    '🇨🇫': 'ЦАР',
    '🇯🇵': 'Япония',
    '🇧🇾': 'Беларусь'
}

def parse_node(line, idx):
    line = line.strip()
    if not line or line.startswith('#'):
        return None
    try:
        # VMess
        if line.startswith('vmess://'):
            b64 = line[8:]
            b64 += '=' * ((4 - len(b64) % 4) % 4)
            d = json.loads(base64.b64decode(b64).decode('utf-8', errors='ignore'))
            name = d.get('ps', f'VMess-{idx}').strip()
            p = {
                'name': name,
                'type': 'vmess',
                'server': d.get('add'),
                'port': int(d.get('port', 443)),
                'uuid': d.get('id'),
                'alterId': int(d.get('aid', 0)),
                'cipher': d.get('scy', 'auto'),
                'udp': True,
                'network': d.get('net', 'tcp')
            }
            tls = d.get('tls', '').lower()
            if tls in ('tls', '1', 'true'):
                p['tls'] = True
                if d.get('sni'):
                    p['servername'] = d.get('sni')
            if p['network'] == 'ws':
                p['ws-opts'] = {
                    'path': d.get('path', '/'),
                    'headers': {'Host': d.get('host', '')} if d.get('host') else {}
                }
            return p

        # VLESS
        elif line.startswith('vless://'):
            u = urllib.parse.urlparse(line)
            name = urllib.parse.unquote(u.fragment).strip() if u.fragment else f'VLESS-{idx}'
            user = u.username
            host = u.hostname
            port = u.port
            qs = urllib.parse.parse_qs(u.query)

            network = qs.get('type', ['tcp'])[0]
            security = qs.get('security', ['none'])[0]
            flow = qs.get('flow', [''])[0]

            p = {
                'name': name,
                'type': 'vless',
                'server': host,
                'port': port,
                'uuid': user,
                'udp': True,
                'network': network
            }
            if flow:
                p['flow'] = flow

            if security == 'reality':
                p['tls'] = True
                p['reality-opts'] = {
                    'public-key': qs.get('pbk', [''])[0],
                    'short-id': qs.get('sid', [''])[0]
                }
                if qs.get('sni'):
                    p['servername'] = qs.get('sni')[0]
                if qs.get('fp'):
                    p['client-fingerprint'] = qs.get('fp')[0]
            elif security == 'tls':
                p['tls'] = True
                if qs.get('sni'):
                    p['servername'] = qs.get('sni')[0]
                if qs.get('fp'):
                    p['client-fingerprint'] = qs.get('fp')[0]
                if qs.get('alpn'):
                    p['alpn'] = qs.get('alpn')[0].split(',')

            if network == 'ws':
                p['ws-opts'] = {
                    'path': qs.get('path', ['/'])[0],
                    'headers': {'Host': qs.get('host', [''])[0]} if qs.get('host') else {}
                }
            elif network == 'grpc':
                service_name = qs.get('serviceName', [''])[0]
                p['grpc-opts'] = {'grpc-service-name': service_name}
            elif network in ('xhttp', 'httpupgrade'):
                path = qs.get('path', ['/'])[0]
                h_host = qs.get('host', [host])[0]
                p['xhttp-opts'] = {
                    'path': path,
                    'headers': {'Host': h_host}
                }
                p['http-opts'] = {
                    'path': [path],
                    'headers': {'Host': [h_host]}
                }
            return p

        # Shadowsocks
        elif line.startswith('ss://'):
            u = urllib.parse.urlparse(line)
            name = urllib.parse.unquote(u.fragment).strip() if u.fragment else f'SS-{idx}'
            if '@' in u.netloc:
                userinfo, hostport = u.netloc.split('@', 1)
                b64 = userinfo + '=' * ((4 - len(userinfo) % 4) % 4)
                try:
                    decoded = base64.b64decode(b64).decode('utf-8')
                    cipher, password = decoded.split(':', 1)
                except Exception:
                    cipher, password = 'auto', userinfo
                host, port = hostport.split(':')
            else:
                b64 = u.netloc + '=' * ((4 - len(u.netloc) % 4) % 4)
                decoded = base64.b64decode(b64).decode('utf-8')
                userinfo, hostport = decoded.split('@', 1)
                cipher, password = userinfo.split(':', 1)
                host, port = hostport.split(':')

            return {
                'name': name,
                'type': 'ss',
                'server': host,
                'port': int(port),
                'cipher': cipher,
                'password': password,
                'udp': True
            }

        # Hysteria 2
        elif line.startswith(('hysteria2://', 'hy2://')):
            u = urllib.parse.urlparse(line)
            name = urllib.parse.unquote(u.fragment).strip() if u.fragment else f'Hy2-{idx}'
            qs = urllib.parse.parse_qs(u.query)
            return {
                'name': name,
                'type': 'hysteria2',
                'server': u.hostname,
                'port': u.port or 443,
                'password': u.username,
                'sni': qs.get('sni', [''])[0],
                'skip-cert-verify': bool(qs.get('insecure', ['0'])[0] == '1'),
                'alpn': qs.get('alpn', ['h3'])[0].split(','),
                'udp': True
            }
    except Exception as e:
        return None

def fetch_and_parse_url(url, label):
    print(f"Fetching {label} from: {url}")
    req = urllib.request.Request(
        url,
        headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"}
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        content = resp.read().decode('utf-8')

    seen_names = {}
    proxies = []
    lines = content.splitlines()
    for i, line in enumerate(lines):
        node = parse_node(line, i)
        if not node:
            continue
        base_name = node['name']
        if base_name in seen_names:
            seen_names[base_name] += 1
            node['name'] = f"{base_name} ({seen_names[base_name]})"
        else:
            seen_names[base_name] = 1
        proxies.append(node)

    print(f"[{label}] Successfully parsed {len(proxies)} proxies.")
    return proxies

def build_clash_config(title, proxies, enable_anti_rkn=True):
    proxy_names = [p['name'] for p in proxies]
    
    # Categorize by flags
    flag_groups = {}
    for name in proxy_names:
        flags = re.findall(r'[\U0001F1E6-\U0001F1FF]{2}', name)
        if flags:
            flag = flags[0]
            country = FLAG_TO_COUNTRY.get(flag, flag)
            group_name = f"{flag} {country}"
            flag_groups.setdefault(group_name, []).append(name)

    groups = []
    
    # 1. Main selector
    main_group_name = f"⚡ {title}"
    main_proxies = ["🚀 Автовыбор (Быстрый пинг)", "♻️ Отказоустойчивый (Fallback)"]
    # Add country group names
    for gname in sorted(flag_groups.keys()):
        if len(flag_groups[gname]) >= 2:
            main_proxies.append(gname)
    main_proxies += ["DIRECT"] + proxy_names
    
    groups.append({
        'name': main_group_name,
        'type': 'select',
        'proxies': main_proxies
    })

    # 2. Auto-select (url-test)
    groups.append({
        'name': '🚀 Автовыбор (Быстрый пинг)',
        'type': 'url-test',
        'url': 'http://cp.cloudflare.com/generate_204',
        'interval': 300,
        'tolerance': 50,
        'proxies': proxy_names
    })

    # 3. Fallback
    groups.append({
        'name': '♻️ Отказоустойчивый (Fallback)',
        'type': 'fallback',
        'url': 'http://cp.cloudflare.com/generate_204',
        'interval': 300,
        'proxies': proxy_names
    })

    # 4. Regional groups
    for gname, members in sorted(flag_groups.items()):
        if len(members) >= 2:
            groups.append({
                'name': gname,
                'type': 'url-test',
                'url': 'http://cp.cloudflare.com/generate_204',
                'interval': 300,
                'tolerance': 50,
                'proxies': members
            })

    # Rules
    rules = []
    if enable_anti_rkn:
        # Bank & Gov & RU direct bypass
        rules += [
            'DOMAIN-SUFFIX,ru,DIRECT',
            'DOMAIN-SUFFIX,su,DIRECT',
            'DOMAIN-SUFFIX,xn--p1ai,DIRECT',
            'DOMAIN-KEYWORD,sber,DIRECT',
            'DOMAIN-KEYWORD,tinkoff,DIRECT',
            'DOMAIN-KEYWORD,tbank,DIRECT',
            'DOMAIN-KEYWORD,alfa,DIRECT',
            'DOMAIN-KEYWORD,vtb,DIRECT',
            'DOMAIN-KEYWORD,raiffeisen,DIRECT',
            'DOMAIN-KEYWORD,gosuslugi,DIRECT',
            'DOMAIN-KEYWORD,nalog,DIRECT',
            'DOMAIN-KEYWORD,mos.ru,DIRECT',
            'DOMAIN-KEYWORD,yandex,DIRECT',
            'DOMAIN-KEYWORD,vk.com,DIRECT',
            'DOMAIN-KEYWORD,mail.ru,DIRECT',
            'DOMAIN-KEYWORD,kinopoisk,DIRECT',
            'DOMAIN-KEYWORD,ozon,DIRECT',
            'DOMAIN-KEYWORD,wildberries,DIRECT',
            'GEOIP,RU,DIRECT,no-resolve'
        ]

    rules.append(f"MATCH,{main_group_name}")

    config = {
        'port': 7890,
        'socks-port': 7891,
        'allow-lan': False,
        'mode': 'rule',
        'log-level': 'info',
        'unified-delay': True,
        'tcp-concurrent': True,
        'proxies': proxies,
        'proxy-groups': groups,
        'rules': rules
    }
    return config

def main():
    repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    out_dir = os.path.join(repo_root, "subscriptions")
    os.makedirs(out_dir, exist_ok=True)

    # 1. Parse BLWL_MIX
    mix_proxies = fetch_and_parse_url(MOS_BLWL_URL, "Бункер МИКС")
    # 2. Parse WHITE_LIST
    wl_proxies = fetch_and_parse_url(MOS_WHITELIST_URL, "Белый список")

    # Generate bunker_mix_clash.yaml
    mix_config = build_clash_config("БУНКЕР МИКС", mix_proxies, enable_anti_rkn=True)
    with open(os.path.join(out_dir, "bunker_mix_clash.yaml"), "w", encoding="utf-8") as f:
        yaml.dump(mix_config, f, allow_unicode=True, sort_keys=False)
    print(f"Generated: {os.path.join(out_dir, 'bunker_mix_clash.yaml')}")

    # Generate bunker_whitelist_clash.yaml
    wl_config = build_clash_config("БУНКЕР БС (Белый список)", wl_proxies, enable_anti_rkn=True)
    with open(os.path.join(out_dir, "bunker_whitelist_clash.yaml"), "w", encoding="utf-8") as f:
        yaml.dump(wl_config, f, allow_unicode=True, sort_keys=False)
    print(f"Generated: {os.path.join(out_dir, 'bunker_whitelist_clash.yaml')}")

    # Generate combined bunker_full_clash.yaml
    all_seen = set()
    full_proxies = []
    for p in mix_proxies + wl_proxies:
        p_copy = dict(p)
        base = p_copy['name']
        counter = 1
        while p_copy['name'] in all_seen:
            counter += 1
            p_copy['name'] = f"{base} ({counter})"
        all_seen.add(p_copy['name'])
        full_proxies.append(p_copy)

    full_config = build_clash_config("БУНКЕР ПОЛНЫЙ (МИКС + БС)", full_proxies, enable_anti_rkn=True)
    with open(os.path.join(out_dir, "bunker_full_clash.yaml"), "w", encoding="utf-8") as f:
        yaml.dump(full_config, f, allow_unicode=True, sort_keys=False)
    print(f"Generated: {os.path.join(out_dir, 'bunker_full_clash.yaml')}")

    print("All Clash subscriptions built successfully!")

if __name__ == "__main__":
    main()
