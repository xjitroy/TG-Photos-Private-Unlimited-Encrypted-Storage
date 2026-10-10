#!/usr/bin/env python3
"""Regression checks for full push ranges and fail-closed CI history classification."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from classify_ci_changes import requires_android


class ClassificationChecks(unittest.TestCase):
    def setUp(self):
        self.before = Path.cwd()
        self.temp = tempfile.TemporaryDirectory()
        os.chdir(self.temp.name)
        self.git('init', '-q')
        self.git('config', 'user.name', 'CI Fixture')
        self.git('config', 'user.email', 'fixture@example.invalid')
        self.write('README.md', 'foundation')
        self.commit()
        self.base = self.git('rev-parse', 'HEAD').strip()

    def tearDown(self):
        os.chdir(self.before)
        self.temp.cleanup()

    def git(self, *args):
        return subprocess.check_output(['git', *args], text=True, stderr=subprocess.DEVNULL)

    def write(self, name, value):
        path = Path(name)
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(value)

    def commit(self):
        self.git('add', '.')
        self.git('commit', '-qm', 'Fixture change')

    def test_documentation_only_skips_android_but_not_policy_job(self):
        self.write('docs/PROGRESS.md', 'documented state')
        self.commit()
        self.assertFalse(requires_android(self.base, 'push'))

    def test_whole_push_range_includes_code_before_last_document_commit(self):
        self.write('app/src/main/Example.kt', 'package fixture')
        self.commit()
        self.write('README.md', 'new docs')
        self.commit()
        self.assertTrue(requires_android(self.base, 'push'))

    def test_unknown_or_new_branch_history_requires_android(self):
        for base in [None, '0' * 40, '1' * 40, 'invalid']:
            self.assertTrue(requires_android(base, 'push'))

    def test_manual_run_requires_android_without_changes(self):
        self.assertTrue(requires_android(self.base, 'workflow_dispatch'))


if __name__ == '__main__':
    unittest.main(verbosity=2)
