#!/bin/bash
# Script to append a new line to testtrigger file and push multiple commits
# to trigger Jenkins webhooks repeatedly.

set -e

# --- Default Configuration ---
REPO_DIR="${1:-..}"     # Path to your local Git repo
BRANCH="${2:-main}"    # Target branch
COUNT="${3:-1}"        # Number of pushes (default: 1)
DELAY="${4:-3}"        # Delay (seconds) between pushes

FILE="testtrigger"     # File to modify for triggering

# --- Script Start ---
cd "$REPO_DIR"

if [ ! -f "$FILE" ]; then
  echo "Creating $FILE..."
  echo "# Test trigger file" > "$FILE"
fi

echo "=================================================="
echo "📦 Repo:     $REPO_DIR"
echo "🌿 Branch:   $BRANCH"
echo "🔁 Pushes:   $COUNT"
echo "⏱️ Delay:    ${DELAY}s"
echo "=================================================="

for ((i=1; i<=COUNT; i++)); do
  echo ">>> Push #$i of $COUNT"

  # Append timestamp line
  echo "Trigger #$i at $(date)" >> "$FILE"

  # Commit and push
  git add "$FILE"
  git commit -m "chore: test Jenkins webhook trigger #$i ($(date +'%Y-%m-%d %H:%M:%S'))"
  git push origin "$BRANCH"

  echo "✅ Commit #$i pushed successfully."

  # Delay between pushes
  if [ "$i" -lt "$COUNT" ]; then
    echo "⏳ Waiting ${DELAY}s before next push..."
    sleep "$DELAY"
  fi
done

echo "🎉 All $COUNT commits pushed successfully — Jenkins webhooks should have triggered!"
