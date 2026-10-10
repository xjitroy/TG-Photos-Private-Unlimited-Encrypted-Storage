#!/usr/bin/env python3
"""Source policy checks only. This does not compile Kotlin or prove runtime security."""
from pathlib import Path
import re
import tomllib
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = "{http://schemas.android.com/apk/res/android}"

def runtime_sources():
    # Inspect declared application modules, not ignored upstream research, generated code or test fixtures.
    modules = set(re.findall(r'"(:[\w:]+)"', (ROOT / "settings.gradle.kts").read_text()))
    for module in sorted(modules):
        source = ROOT / module[1:].replace(":", "/") / "src/main"
        for suffix in ("*.kt", "*.java"):
            yield from source.rglob(suffix)

class SourcePolicyChecks(unittest.TestCase):
    def test_final_branding(self):
        for path in runtime_sources():
            self.assertNotIn("TG Photos E2EE", path.read_text(), str(path))
        strings = ET.parse(ROOT / "app/src/main/res/values/strings.xml").getroot()
        self.assertEqual("TG Photos", next(x.text for x in strings if x.attrib.get("name") == "app_name"))

    def test_four_tabs_no_settings_destination(self):
        code = (ROOT / "app/src/main/kotlin/app/tgphotos/android/MainActivity.kt").read_text()
        tabs = code.split("private enum class MainTab", 1)[1].split("private enum class Screen", 1)[0]
        self.assertEqual(["PHOTOS", "ALBUMS", "FILES", "VAULT"], re.findall(r'^\s+(\w+)\("', tabs, re.M))
        self.assertIn('DropdownMenuItem(text = { Text("Settings") }', code)

    def test_manifest_privacy_surface(self):
        manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
        self.assertEqual([], manifest.findall("uses-permission"))
        app = manifest.find("application")
        self.assertEqual("false", app.get(ANDROID + "allowBackup"))
        self.assertEqual("false", app.get(ANDROID + "usesCleartextTraffic"))
        providers = app.findall("provider")
        self.assertEqual(1, len(providers))
        provider = providers[0]
        self.assertEqual("androidx.startup.InitializationProvider", provider.get(ANDROID + "name"))
        self.assertEqual("false", provider.get(ANDROID + "exported"))
        self.assertEqual("${applicationId}.androidx-startup", provider.get(ANDROID + "authorities"))
        metadata = provider.findall("meta-data")
        self.assertEqual(1, len(metadata))
        self.assertEqual("androidx.emoji2.text.EmojiCompatInitializer", metadata[0].get(ANDROID + "name"))
        self.assertEqual("remove", metadata[0].get("{http://schemas.android.com/tools}node"))
        for component in list(app):
            if component.tag == "provider":
                continue
            self.assertEqual("activity", component.tag)
            self.assertEqual(".MainActivity", component.get(ANDROID + "name"))

    def test_no_app_runtime_endpoint_or_tracking_sdk(self):
        for path in list(runtime_sources()) + [ROOT / "gradle/libs.versions.toml"]:
            text = path.read_text().lower()
            for forbidden in ["https://", "http://", "firebase", "sentry", "bugsnag", "appsflyer", "facebook", "mixpanel", "amplitude", "crashlytics"]:
                self.assertNotIn(forbidden, text, str(path))

    def test_no_apk_or_bundle_exists(self):
        self.assertEqual([], list(ROOT.rglob("*.apk")) + list(ROOT.rglob("*.aab")))

    def test_xml_and_catalog_parse(self):
        for path in list((ROOT / "app/src/main").rglob("*.xml")) + list((ROOT / "core").glob("*/src/main/**/*.xml")) + list((ROOT / "feature").glob("*/src/main/**/*.xml")):
            ET.parse(path)
        data = tomllib.loads((ROOT / "gradle/libs.versions.toml").read_text())
        for dependency in list(data["libraries"].values()) + list(data["plugins"].values()):
            ref = dependency.get("version", {}).get("ref") if isinstance(dependency.get("version"), dict) else None
            if ref:
                self.assertIn(ref, data["versions"])

    def test_module_graph_has_no_missing_nodes_or_cycles(self):
        settings = (ROOT / "settings.gradle.kts").read_text()
        modules = set(re.findall(r'"(:[\w:]+)"', settings))
        self.assertEqual(16, len(modules))
        graph = {}
        for module in modules:
            path = ROOT / module[1:].replace(":", "/") / "build.gradle.kts"
            self.assertTrue(path.is_file(), module)
            dependencies = set(re.findall(r'project\("(:[\w:]+)"\)', path.read_text()))
            self.assertTrue(dependencies <= modules, module)
            graph[module] = dependencies
        def visit(node, parents):
            self.assertNotIn(node, parents, f"Cycle at {node}")
            for child in graph[node]:
                visit(child, parents | {node})
        for node in graph:
            visit(node, set())

    def test_secret_inputs_disabled_in_preview(self):
        auth = (ROOT / "feature/auth/src/main/kotlin/app/tgphotos/feature/auth/AuthScreen.kt").read_text()
        self.assertNotIn("submitCode(", auth)
        self.assertNotIn("submitTwoFactorPassword(", auth)
        self.assertIn('value = "", onValueChange = {}, enabled = false', auth)

    def test_vault_has_secure_window_and_no_unlock(self):
        vault = (ROOT / "feature/vault/src/main/kotlin/app/tgphotos/feature/vault/VaultScreen.kt").read_text()
        self.assertIn("addFlags(WindowManager.LayoutParams.FLAG_SECURE)", vault)
        self.assertIn("onDispose", vault)
        self.assertIn("onClick = {}, enabled = false", vault)

    def test_backup_has_no_success_or_schedule(self):
        worker = (ROOT / "core/backup/src/main/kotlin/app/tgphotos/core/backup/BackupWorker.kt").read_text()
        self.assertIn("Result.failure", worker)
        self.assertNotIn("Result.success", worker)
        for path in runtime_sources():
            self.assertNotIn("enqueueUniqueWork", path.read_text())

    def test_no_logs_of_sensitive_state(self):
        for path in runtime_sources():
            self.assertIsNone(re.search(r'\b(?:Log\.[vdiew]|println|print|Timber\.)\s*\(', path.read_text()), str(path))

    def test_packaging_guard_present(self):
        build = (ROOT / "build.gradle.kts").read_text()
        self.assertIn('providers.gradleProperty("apkBuildApproved")', build)
        self.assertIn('throw GradleException', build)
        self.assertIn('!it.name.endsWith("Resources")', build)

if __name__ == "__main__":
    unittest.main(verbosity=2)
