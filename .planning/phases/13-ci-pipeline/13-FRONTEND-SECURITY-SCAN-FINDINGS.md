---
phase: 13-ci-pipeline
created: 2026-03-23
type: security-scan-findings
status: remediated
---

# Phase 13: Frontend Security Scan Findings

**Scan Date:** 2026-03-23
**Image:** ghcr.io/baalexandru/jobhunt-frontend:latest (Alpine 3.23.3)
**Scanner:** Trivy 0.35.0
**Total Vulnerabilities:** 24 (CRITICAL: 0, HIGH: 23, MEDIUM: 1)

## Summary

- 2 vulnerabilities in Alpine 3.23.3 OS packages (zlib)
- 22 vulnerabilities in system npm bundled with node:22-alpine (NOT application dependencies)
- Zero vulnerabilities in application node_modules (Next.js 16.2, React 19.2, pg, sharp, etc.)

## Alpine OS Findings (2)

### CVE-2026-22184 — zlib Buffer Overflow (HIGH)
- **Package:** zlib (1.3.1-r2)
- **Fixed in:** 1.3.2-r0
- **Impact:** Arbitrary code execution via buffer overflow in untgz utility
- **Risk:** HIGH — zlib is used by Node.js for HTTP compression

### CVE-2026-27171 — zlib CRC32 Infinite Loop (MEDIUM)
- **Package:** zlib (1.3.1-r2)
- **Fixed in:** 1.3.2-r0
- **Impact:** Denial of Service via infinite loop in CRC32 combine functions

## Node.js System Package Findings (22) — 10 Unique CVEs

All findings are in `/usr/local/lib/node_modules/npm/` — the system npm bundled with the node:22-alpine base image, not the application's dependencies.

| CVE | Library | Version | Fixed | Impact |
|-----|---------|---------|-------|--------|
| CVE-2026-23745 | tar | 6.2.1 / 7.4.3 | 7.5.3 | Arbitrary file overwrite via symlink poisoning |
| CVE-2026-23950 | tar | 6.2.1 / 7.4.3 | 7.5.4 | Arbitrary file overwrite via Unicode path collision |
| CVE-2026-24842 | tar | 6.2.1 | 7.5.7 | Arbitrary file creation via hardlink path traversal |
| CVE-2026-26960 | tar | 6.2.1 | 7.5.8 | Arbitrary file read/write via malicious archive hardlinks |
| CVE-2026-29786 | tar | 6.2.1 | 7.5.10 | Hardlink path traversal via drive-relative linkpath |
| CVE-2026-31802 | tar | 6.2.1 | 7.5.11 | File overwrite via drive-relative symlink traversal |
| CVE-2025-64756 | glob | 10.4.5 | 10.5.0 | Command injection via malicious filenames |
| CVE-2026-26996 | minimatch | 9.0.5 | 9.0.6 | DoS via specially crafted glob patterns |
| CVE-2026-27903 | minimatch | 9.0.5 | 9.0.7 | DoS via unbounded recursive backtracking |
| CVE-2026-27904 | minimatch | 9.0.5 | 9.0.7 | DoS via catastrophic backtracking in glob expressions |

**Note:** The 22 count comes from tar appearing in 3 locations (cacache, node-gyp, npm root) × 6 CVEs = 18, plus glob (1) + minimatch (3) = 22.

## Application Dependencies — Clean

All scanned application packages reported 0 vulnerabilities:
- Next.js 16.2, React 19.2.4, React DOM 19.2.4
- sharp 0.34.5, pg 8.20.0, styled-jsx 5.1.6
- All other node_modules dependencies

No action required for application-level dependencies.

## Remediation

### Actions Taken

1. **`apk upgrade`** added to Dockerfile runtime stage — patches zlib to latest, resolving both Alpine CVEs
2. **npm, corepack, and yarn removed** from runtime image — the Next.js standalone server only needs `node server.js`; package managers are unnecessary at runtime. This eliminates all 22 HIGH node-pkg findings and reduces image size.

### Dockerfile Change

```dockerfile
# Before
RUN addgroup -S app && adduser -S app -G app

# After
RUN apk update && apk upgrade --no-cache && \
    rm -rf /usr/local/lib/node_modules/npm /usr/local/lib/node_modules/corepack \
           /opt/yarn* /usr/local/bin/npm /usr/local/bin/npx /usr/local/bin/corepack \
           /usr/local/bin/yarn /usr/local/bin/yarnpkg \
           /var/cache/apk/* && \
    addgroup -S app && adduser -S app -G app
```

### Expected Outcome

- 2 Alpine CVEs resolved via package upgrades (zlib)
- 22 HIGH findings permanently eliminated by removing system npm/yarn/corepack
- Smaller image size due to removed packages (~50MB savings)
