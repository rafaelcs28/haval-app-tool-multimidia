#!/usr/bin/env bash
set -euo pipefail
command -v mac-mini-build >/dev/null 2>&1 || { echo 'mac-mini-build is unavailable on this authorized coordinator.' >&2; exit 69; }
repo=$(git rev-parse --show-toplevel)
if [[ ${1:-} == "--release" ]]; then
    [[ $# -ge 3 ]] || { echo 'usage: build-on-mac-mini.sh --release VERSION_CODE VERSION_NAME [COMMIT]' >&2; exit 64; }
    version_code=$2
    version_name=$3
    ref=${4:-HEAD}
    [[ $version_code =~ ^[1-9][0-9]*$ ]] || { echo 'invalid version code' >&2; exit 64; }
    [[ $version_name =~ ^[A-Za-z0-9._-]+$ ]] || { echo 'invalid version name' >&2; exit 64; }
    commit=$(git -C "$repo" rev-parse "$ref^{commit}")
    exec mac-mini-build submit-git "$repo" android-release "$commit" "$version_code" "$version_name"
fi
ref=${1:-HEAD}
commit=$(git -C "$repo" rev-parse "$ref^{commit}")
exec mac-mini-build submit-git "$repo" android-debug "$commit"
