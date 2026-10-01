#!/bin/bash
set -euo pipefail

# Add version bumps after each commit
# Commit 1: 7d0d32e -> 1.14.16/131
sed -i 's/versionCode = 130/versionCode = 131/; s/versionName = "1.14.15"/versionName = "1.14.16"/' greater-art/app/build.gradle.kts
git add greater-art/app/build.gradle.kts
git commit --amend --no-edit

# Commit 2: 68a88ff -> 1.14.17/132
sed -i 's/versionCode = 131/versionCode = 132/; s/versionName = "1.14.16"/versionName = "1.14.17"/' greater-art/app/build.gradle.kts
git add greater-art/app/build.gradle.kts
git commit --amend --no-edit

# Commit 3: dde1929 -> 1.14.18/133
sed -i 's/versionCode = 132/versionCode = 133/; s/versionName = "1.14.17"/versionName = "1.14.18"/' greater-art/app/build.gradle.kts
git add greater-art/app/build.gradle.kts
git commit --amend --no-edit