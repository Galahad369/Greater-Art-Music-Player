#!/bin/bash
set -euo pipefail

# Get the current commit index and apply appropriate version bump
CURRENT_VERSION=$(grep 'versionName = ' greater-art/app/build.gradle.kts | sed 's/.*versionName = "\(.*\)".*/\1/')
CURRENT_CODE=$(grep 'versionCode = ' greater-art/app/build.gradle.kts | sed 's/.*versionCode = \(.*\)/\1/')

echo "Current: $CURRENT_VERSION / $CURRENT_CODE"

# Parse version
MAJOR=$(echo $CURRENT_VERSION | cut -d. -f1)
MINOR=$(echo $CURRENT_VERSION | cut -d. -f2)
PATCH=$(echo $CURRENT_VERSION | cut -d. -f3)
NEW_PATCH=$((PATCH + 1))
NEW_CODE=$((CURRENT_CODE + 1))
NEW_VERSION="${MAJOR}.${MINOR}.${NEW_PATCH}"

echo "Bumping to: $NEW_VERSION / $NEW_CODE"

sed -i "s/versionCode = $CURRENT_CODE/versionCode = $NEW_CODE/; s/versionName = \"$CURRENT_VERSION\"/versionName = \"$NEW_VERSION\"/" greater-art/app/build.gradle.kts
git add greater-art/app/build.gradle.kts
git commit --amend --no-edit