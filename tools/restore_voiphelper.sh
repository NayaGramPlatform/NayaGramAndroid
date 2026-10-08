#!/bin/bash
# Permanently restore VoIPHelper.java (fixes isVideo compile error + accidental overwrite)
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
TARGET="TMessagesProj/src/main/java/org/telegram/ui/Components/voip/VoIPHelper.java"
GOOD_URL="https://raw.githubusercontent.com/NayaGramPlatform/NayaGramAndroid/3a002cafdc33c9b41913da2f7ad18ef61d423589/TMessagesProj/src/main/java/org/telegram/ui/Components/voip/VoIPHelper.java"
echo "Downloading good VoIPHelper from commit 3a002caf..."
curl -fsSL "$GOOD_URL" -o "$TARGET"
echo "Applying isVideo -> videoCall fix..."
sed -i 's/user != null ? user.id : 0, isVideo)/user != null ? user.id : 0, videoCall)/g' "$TARGET"
if ! grep -q "public class VoIPHelper" "$TARGET"; then
  echo "ERROR: restore failed"
  exit 1
fi
echo "OK: $TARGET restored ($(wc -c < "$TARGET") bytes)"
echo "Now run:"
echo "  git add $TARGET && git commit -m 'fix: restore VoIPHelper isVideo->videoCall' && git push"
