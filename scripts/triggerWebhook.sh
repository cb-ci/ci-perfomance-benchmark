#!/bin/bash
# Script to append a new line to testtrigger file and push to trigger Jenkins webhook

set -e


# --- Configuration ---
REPO_DIR="${1:-..}"           # Path to your local Git repo (default = current dir)
BRANCH="${2:-main}"          # Target branch (default = main)
FILE="testtrigger"           # File to modify

# --- Script start ---
cd "$REPO_DIR"

if [ ! -f "$FILE" ]; then
  echo "Creating $FILE..."
  echo "# Test trigger file" > "$FILE"
fi

# Append a timestamp line
echo "Trigger at $(date)" >> "$FILE"

# Add and commit
git add "$FILE"
git commit -m "chore: trigger Jenkins webhook ($(date +'%Y-%m-%d %H:%M:%S'))"

# Push to remote
echo "Pushing to branch '$BRANCH'..."
git push origin "$BRANCH"

echo "✅ Commit pushed successfully — Jenkins webhook should be triggered."
