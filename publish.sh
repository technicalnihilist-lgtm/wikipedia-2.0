#!/usr/bin/env bash
# Pushes this folder to GitHub and turns on Pages.
# Usage: ./publish.sh <github-username> [repo-name]
set -e
USER="${1:?usage: ./publish.sh <github-username> [repo-name]}"
REPO="${2:-wikipedia-2.0}"
git init -q
git add -A
git commit -qm "Wikipedia 2.0 — live Wikipedia reader, web + Android" || true
git branch -M main
git remote remove origin 2>/dev/null || true
git remote add origin "https://github.com/$USER/$REPO.git"
git push -u origin main
echo "Pushed. Now enable Pages: Settings → Pages → Source: GitHub Actions"
echo "Site will appear at https://$USER.github.io/$REPO/"
