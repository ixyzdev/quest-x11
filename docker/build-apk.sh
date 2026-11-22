#!/bin/sh
set -eu

branch="${1:?usage: docker compose run --rm builder <branch>}"
repository_url=https://github.com/ixyzdev/quest-x11.git
source_directory="$HOME/workspace/quest-x11"
apk_output_directory="$HOME/apk"

if [ ! -d "$source_directory/.git" ]; then
    git clone --filter=blob:none "$repository_url" "$source_directory"
fi

cd "$source_directory"
git fetch origin "$branch"
git checkout --force --detach FETCH_HEAD
git submodule update --init --recursive --jobs 8 --depth 1

./gradlew --no-daemon -Pandroid.injected.build.abi=arm64-v8a :lorie-app:assembleStandaloneDebug

commit="$(git rev-parse --short HEAD)"
cp lorie-app/build/intermediates/apk/standalone/debug/termux-x11-universal-debug.apk \
    "$apk_output_directory/quest-x11-$commit-arm64-v8a-debug.apk"
echo "$apk_output_directory/quest-x11-$commit-arm64-v8a-debug.apk"
