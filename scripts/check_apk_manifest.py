#!/usr/bin/env python3
"""Inspect the actual decoded QA APK manifest. Preview-only policy; not runtime traffic proof."""
import json
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

path = Path(sys.argv[1])
manifest = ET.parse(path).getroot()
android = '{http://schemas.android.com/apk/res/android}'
application = manifest.find('application')
if application is None or manifest.get('package') != 'app.tgphotos.android':
    raise SystemExit('Unexpected APK application identity')
if application.get(android + 'allowBackup') != 'false' or application.get(android + 'usesCleartextTraffic') != 'false':
    raise SystemExit('APK backup/cleartext restrictions are missing')
permissions = sorted(node.get(android + 'name', '') for node in manifest.findall('uses-permission'))
for permission in permissions:
    if permission == 'android.permission.INTERNET' or permission.startswith('android.permission.READ_MEDIA_') or permission in {
        'android.permission.READ_EXTERNAL_STORAGE', 'android.permission.WRITE_EXTERNAL_STORAGE', 'android.permission.MANAGE_EXTERNAL_STORAGE'}:
        raise SystemExit('Labelled preview must not have network or personal-media permission')
components = []
for node in application:
    if node.tag not in {'activity', 'receiver', 'service', 'provider'}:
        continue
    exported = node.get(android + 'exported') == 'true'
    name = node.get(android + 'name', '')
    guard = node.get(android + 'permission', '')
    if exported and name not in {'.MainActivity', 'app.tgphotos.android.MainActivity'} and guard != 'android.permission.DUMP':
        raise SystemExit('Unreviewed exported APK component')
    if node.tag == 'provider':
        initializers = {child.get(android + 'name', '') for child in node.findall('meta-data')}
        if name != 'androidx.startup.InitializationProvider' or exported or initializers - {
            'androidx.lifecycle.ProcessLifecycleInitializer', 'androidx.profileinstaller.ProfileInstallerInitializer'}:
            raise SystemExit('Unreviewed APK initializer/provider')
    components.append({'kind': node.tag, 'name': name, 'exported': exported, 'permission': guard,
                       'initializers': [child.get(android + 'name', '') for child in node.findall('meta-data')]})
report = {'scope': 'Actual development APK merged manifest; no runtime network/security audit implied',
          'package': manifest.get('package'), 'debuggable': application.get(android + 'debuggable') == 'true',
          'permissions': permissions, 'components': components}
(path.parent / 'manifest-review.json').write_text(json.dumps(report, indent=2) + '\n')
print(f'PASS: actual preview APK manifest, {len(permissions)} permissions and {len(components)} reviewed component declarations')
