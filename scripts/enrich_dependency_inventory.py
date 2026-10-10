#!/usr/bin/env python3
"""Read already-resolved Gradle cache POM licenses. No network or private configuration is accessed."""
import hashlib
import json
import os
from pathlib import Path
import xml.etree.ElementTree as ET

output = Path('.toolchain/dependency-evidence')
inventory = json.loads((output / 'resolved-runtime.json').read_text())
cache = Path(os.environ['GRADLE_USER_HOME']) / 'caches/modules-2/files-2.1'
coordinates = sorted({artifact['coordinate'] for scope in inventory['configurations'] for artifact in scope['artifacts']})
reports = []
for coordinate in coordinates:
    group, name, version = coordinate.split(':')
    if any('/' in value or '\\' in value or value in {'.', '..'} for value in [group, name, version]):
        raise SystemExit('Invalid resolved dependency coordinate')
    metadata = []
    for path in sorted((cache / group / name / version).glob(f'*/{name}-{version}.pom')):
        if path.stat().st_size > 512 * 1024:
            raise SystemExit('Oversized dependency POM metadata')
        data = path.read_bytes()
        pom = ET.fromstring(data)
        namespace = {'m': 'http://maven.apache.org/POM/4.0.0'} if pom.tag.startswith('{') else {}
        prefix = 'm:' if namespace else ''
        licenses = []
        for license_node in pom.findall(f'{prefix}licenses/{prefix}license', namespace):
            values = {field: (license_node.findtext(prefix + field, default='', namespaces=namespace) or '').strip()
                      for field in ['name', 'url', 'distribution']}
            licenses.append(values)
        metadata.append({'pom': path.name, 'sha256': hashlib.sha256(data).hexdigest(),
                         'declaredLicenses': licenses, 'parentPresent': pom.find(prefix + 'parent', namespace) is not None})
    reports.append({'coordinate': coordinate, 'metadata': metadata,
                    'reviewStatus': 'declarations found; manual artifact/source review required' if any(m['declaredLicenses'] for m in metadata)
                    else 'license unresolved; inspect inherited/missing metadata and source'})
report = {'scope': 'Cached Maven POM declarations only; not a license clearance, vulnerability scan or native SBOM',
          'dependencies': reports}
(output / 'resolved-licenses.json').write_text(json.dumps(report, indent=2) + '\n')
found = sum(bool(any(m['declaredLicenses'] for m in dependency['metadata'])) for dependency in reports)
print(f'Resolved license metadata: {found}/{len(reports)} packages have direct POM declarations; manual review pending')
