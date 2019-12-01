#!/usr/bin/env python3

import re
from pathlib import Path
import os
import sys


def increment_and_set(mtc):
    v = int(mtc[1]) + 1
    stored_ver.append(v)
    return f'versionCode {v}'


os.chdir(Path(sys.argv[0]).parent)

gradle = Path('app/build.gradle')
t = gradle.read_text()
t = re.sub(r'versionName ".*?"', f'versionName "{sys.argv[1]}"', t)
stored_ver = []
t = re.sub(r'versionCode (\d+)', increment_and_set, t)
gradle.write_text(t)

Path(f'fastlane/metadata/android/en-US/changelogs/{stored_ver[0]}.txt').touch()
