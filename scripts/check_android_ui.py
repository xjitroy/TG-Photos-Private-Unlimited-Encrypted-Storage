#!/usr/bin/env python3
"""ADB/UI-tree QA of the labelled design preview. No account, user files or live features tested."""
import json
import os
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET

root = Path('.toolchain/ui-evidence')
root.mkdir(parents=True, exist_ok=True)
serial = os.environ.get('TGPHOTOS_EMULATOR_SERIAL', 'emulator-5554')
package = 'app.tgphotos.android'
passed = []


def adb(*args, binary=False):
    return subprocess.check_output(['adb', '-s', serial, *args], text=not binary, timeout=30)


def tree(label):
    output = adb('exec-out', 'uiautomator', 'dump', '/dev/tty')
    start, end = output.find('<hierarchy'), output.rfind('</hierarchy>')
    if start < 0 or end < 0:
        raise RuntimeError('UI hierarchy is unavailable')
    xml = output[start:end + len('</hierarchy>')]
    (root / f'{label}.xml').write_text(xml)
    return ET.fromstring(xml)


def matches(node, text):
    return any(text == node.get(key, '') for key in ['text', 'content-desc'])


def nodes_with_enabled(ui, enabled=True):
    effective = enabled and ui.get('enabled', 'true') == 'true'
    if ui.tag == 'node':
        yield ui, effective
    for child in ui:
        yield from nodes_with_enabled(child, effective)


def bounds(node):
    values = list(map(int, re.findall(r'-?\d+', node.get('bounds', ''))))
    return values if len(values) == 4 and values[2] > values[0] and values[3] > values[1] else None


def tap(text, label, scroll=True):
    # Coordinates come exclusively from the UI tree, never from screenshots.
    for attempt in range(4 if scroll else 1):
        ui = tree(label + f'-before-{attempt}')
        candidates = []
        for node, enabled in nodes_with_enabled(ui):
            if matches(node, text) and enabled and bounds(node):
                candidates.append(bounds(node))
        if candidates:
            x1, y1, x2, y2 = max(candidates, key=lambda b: b[3])
            adb('shell', 'input', 'tap', str((x1 + x2) // 2), str((y1 + y2) // 2))
            time.sleep(0.6)
            return
        if scroll and attempt < 3:
            scroll_regions = [bounds(n) for n in ui.iter('node')
                              if n.get('scrollable') == 'true' and bounds(n)
                              and bounds(n)[3] - bounds(n)[1] > bounds(n)[2] - bounds(n)[0]]
            if not scroll_regions:
                break
            x1, y1, x2, y2 = max(scroll_regions, key=lambda b: b[3] - b[1])
            x = (x1 + x2) // 2
            adb('shell', 'input', 'swipe', str(x), str(y1 + (y2 - y1) * 4 // 5),
                str(x), str(y1 + (y2 - y1) // 5), '350')
            time.sleep(0.4)
    raise RuntimeError(f'UI target unavailable: {text}')


def verify(label, expected, screenshot=True):
    ui = tree(label)
    texts = '\n'.join(n.get('text', '') + ' ' + n.get('content-desc', '') for n in ui.iter('node'))
    if screenshot:
        (root / f'{label}.png').write_bytes(adb('exec-out', 'screencap', '-p', binary=True))
    for text in expected:
        if text not in texts:
            raise RuntimeError(f'Missing UI evidence: {label}: {text}')
    passed.append(label)
    return ui


try:
    adb('logcat', '-c')
    adb('shell', 'am', 'start', '-W', '-n', package + '/.MainActivity')
    time.sleep(1)
    login = verify('login', ['TG Photos', 'Phone number', 'Design preview'])
    phone = [(n, enabled) for n, enabled in nodes_with_enabled(login)
             if matches(n, 'Continue with Telegram')]
    if not phone or any(enabled for _, enabled in phone):
        raise RuntimeError('Preview must not accept Telegram authentication')
    tap('Explore the app preview', 'explore')
    verify('photos-light', ['Photos (Cloud)', 'Connect your private cloud'])
    for tab, evidence in [('Albums', 'Albums'), ('Files', 'Files'), ('Private Vault', 'Vault locked')]:
        tap(tab, 'open-' + tab.replace(' ', '-'))
        verify(tab.lower().replace(' ', '-'), [evidence], screenshot=tab != 'Private Vault')
    windows = adb('shell', 'dumpsys', 'window', 'windows')
    (root / 'vault-window.txt').write_text(windows)
    sections = re.split(r'(?=\n\s*Window #\d+)', windows)
    app_windows = [s for s in sections if package + '/' in s]
    if not any(re.search(r'\bSECURE\b', s) or any(int(x, 16) & 0x2000
                  for x in re.findall(r'\bfl=0x([0-9a-fA-F]+)', s)) for s in app_windows):
        raise RuntimeError('Vault window FLAG_SECURE is not present')
    passed.append('vault-secure-window')
    # A protected screenshot may fail or redact; never upload a vault screenshot as normal media evidence.
    tap('Photos', 'leave-vault')
    tap('More options', 'overflow', scroll=False)
    tap('Settings', 'settings-menu', scroll=False)
    verify('settings-light', ['APPEARANCE', 'System', 'Dark', 'Light'])
    tap('Dark', 'dark-theme')
    dark = verify('settings-dark', ['Dark', 'APPEARANCE'])
    selected = [n for n in dark.iter('node') if matches(n, 'Dark')]
    if not selected:
        raise RuntimeError('Dark appearance choice is unavailable')
    adb('shell', 'am', 'force-stop', package)
    adb('shell', 'am', 'start', '-W', '-n', package + '/.MainActivity')
    time.sleep(0.8)
    tap('More options', 'reopen-menu', scroll=False)
    tap('Settings', 'reopen-settings', scroll=False)
    persisted = verify('settings-dark-restart', ['APPEARANCE', 'Dark'])
    # Compose reports selection on its merged chip; inspect the containing UI-tree ancestry.
    def selected_dark(node, inherited=False):
        selected_here = inherited or node.get('selected') == 'true' or node.get('checked') == 'true'
        return (matches(node, 'Dark') and selected_here) or any(selected_dark(c, selected_here) for c in node)
    if not selected_dark(persisted):
        raise RuntimeError('Dark theme preference did not survive process restart')
    passed.append('theme-process-persistence')
    tap('Light', 'light-theme')
    tap('Back', 'settings-back', scroll=False)
    home = verify('home-after-settings', ['Photos', 'Albums', 'Files', 'Private Vault'])
    if any(matches(n, 'Settings') for n in home.iter('node')):
        raise RuntimeError('Settings must be accessible through overflow, not a fifth bottom tab')
    crash = adb('logcat', '-d', '-b', 'crash')
    if package in crash or 'FATAL EXCEPTION' in crash:
        raise RuntimeError('Android crash buffer contains a failure')
    (root / 'crash-buffer.txt').write_text(crash)
finally:
    (root / 'results.json').write_text(json.dumps({
        'scope': 'Labelled design-preview UI on Android emulator; not live Telegram/backup/vault/player acceptance',
        'passed': passed, 'device': serial,
    }, indent=2) + '\n')

print(f'PASS: {len(passed)} Android preview UI checks; no production/live integration implied')
