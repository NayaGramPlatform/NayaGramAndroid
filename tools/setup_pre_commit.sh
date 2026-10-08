#!/usr/bin/env bash
# Setup pre-commit hook to block committing keystores, certs, or hardcoded secrets
set -e

HOOK_PATH=".git/hooks/pre-commit"

if [ ! -d ".git" ]; then
  echo "Error: Not a git repository root."
  exit 1
fi

cat << 'EOF' > ""
#!/usr/bin/env bash
# Auto-generated NayaGram pre-commit security guard

echo "[NayaGram Guard] Scanning staged files for sensitive files and credentials..."

# 1. Check for blocked extensions or filenames
FORBIDDEN_FILES=

if [ -n "" ]; then
  echo "ERROR: Blocked sensitive files staged for commit:"
  echo ""
  echo "Aborting commit. Never commit keystores, certificates, or signing properties."
  exit 1
fi

# 2. Run Python secret scanner on staged diffs
if [ -f "tools/check_secrets.py" ]; then
  python3 tools/check_secrets.py || {
    echo "ERROR: tools/check_secrets.py detected potential secrets or credentials."
    exit 1
  }
fi

echo "[NayaGram Guard] Pre-commit security check passed."
exit 0
EOF

chmod +x ""
echo "Pre-commit hook installed successfully at "
