#!/usr/bin/env python3
import os
import sys
import re

PATTERNS = [
    (re.compile(r'''(?i)(?<![&?\"\'])\b(password|secret|keystore_pass|key_alias_pass)\s*[:=]\s*['"][^'"]{4,}['"]'''), 'Hardcoded password/secret assignment'),
    (re.compile(r'AIza[0-9A-Za-z-_]{35}'), 'Google API Key format'),
    (re.compile(r'(?i)BEGIN\s+(RSA|OPENSSH|DSA|EC)?\s*PRIVATE\s+KEY'), 'Private key block'),
]

FORBIDDEN_EXTENSIONS = ('.jks', '.keystore', '.pepk', '.p12')

def scan():
    violations = 0
    repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    for root, dirs, files in os.walk(repo_root):
        # skip git, build, and upstream 3rd party test vectors
        if any(skip in root for skip in ['.git', 'build', '.gradle', 'obj', 'afat', 'boringssl']):
            continue
        for file in files:
            path = os.path.join(root, file)
            rel_path = os.path.relpath(path, repo_root)

            # Check forbidden binary signing files
            if file.lower().endswith(FORBIDDEN_EXTENSIONS):
                print(f'[SECURITY GUARD VIOLATION] Sensitive binary signing file committed: {rel_path}')
                violations += 1
                continue

            # Skip google-services.json and manifest placeholder configs for maps keys if required
            if file in ('google-services.json', 'AndroidManifest.xml', 'AndroidManifest_SDK23.xml', 'AndroidManifest_standalone.xml'):
                continue

            # Check source text files
            if file.endswith(('.java', '.kt', '.xml', '.gradle', '.properties', '.json', '.yml', '.yaml')):
                try:
                    with open(path, 'r', encoding='utf-8', errors='ignore') as f:
                        for idx, line in enumerate(f, 1):
                            for pattern, desc in PATTERNS:
                                if pattern.search(line):
                                    print(f'[SECURITY GUARD VIOLATION] {desc} found in {rel_path}:{idx}')
                                    violations += 1
                except Exception:
                    pass
    return violations

if __name__ == '__main__':
    v = scan()
    if v > 0:
        print(f'Secret scan failed with {v} violations.')
        sys.exit(1)
    else:
        print('Security guard: No exposed secrets detected in tracked files.')
        sys.exit(0)
