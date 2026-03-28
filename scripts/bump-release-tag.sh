#!/usr/bin/env bash

set -euo pipefail

usage() {
  cat <<'EOF'
Usage: ./scripts/bump-release-tag.sh [patch|minor|major|X.Y.Z]

Examples:
  ./scripts/bump-release-tag.sh
  ./scripts/bump-release-tag.sh minor
  ./scripts/bump-release-tag.sh 1.2.3
EOF
}

if [[ $# -gt 1 ]]; then
  usage
  exit 1
fi

git rev-parse --is-inside-work-tree >/dev/null 2>&1 || {
  printf 'Error: run this script inside the git repository.\n' >&2
  exit 1
}

if [[ -n "$(git status --porcelain)" ]]; then
  printf 'Error: working tree is not clean. Commit or stash changes first.\n' >&2
  exit 1
fi

current_branch="$(git branch --show-current)"
if [[ -z "$current_branch" ]]; then
  printf 'Error: detached HEAD is not supported. Check out a branch first.\n' >&2
  exit 1
fi

git fetch --tags origin

latest_tag=""
while IFS= read -r tag; do
  latest_tag="$tag"
  break
done < <(git tag --list 'v[0-9]*.[0-9]*.[0-9]*' --sort=-version:refname)

if [[ -z "$latest_tag" ]]; then
  latest_tag="v0.0.0"
fi

input="${1:-patch}"
version="${latest_tag#v}"
IFS='.' read -r major minor patch <<< "$version"

case "$input" in
  patch)
    patch=$((patch + 1))
    ;;
  minor)
    minor=$((minor + 1))
    patch=0
    ;;
  major)
    major=$((major + 1))
    minor=0
    patch=0
    ;;
  [0-9]*.[0-9]*.[0-9]*)
    IFS='.' read -r major minor patch <<< "$input"
    ;;
  *)
    usage
    exit 1
    ;;
esac

new_tag="v${major}.${minor}.${patch}"

if git rev-parse "$new_tag" >/dev/null 2>&1; then
  printf 'Error: tag %s already exists.\n' "$new_tag" >&2
  exit 1
fi

git tag -a "$new_tag" -m "Release $new_tag"
git push origin "$new_tag"

printf 'Created and pushed %s from branch %s.\n' "$new_tag" "$current_branch"
