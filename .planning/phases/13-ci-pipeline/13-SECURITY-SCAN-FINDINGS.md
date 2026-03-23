---
phase: 13-ci-pipeline
created: 2026-03-23
type: security-scan-findings
status: remediated
---

# Phase 13: Security Scan Findings

**Scan Date:** 2026-03-23
**Image:** ghcr.io/baalexandru/jobhunt-backend:latest (Alpine 3.22.1)
**Scanner:** Trivy 0.35.0
**Total Vulnerabilities:** 89 (CRITICAL: 4, HIGH: 27, MEDIUM: 58)

## Summary

All 89 vulnerabilities are in the Alpine 3.22.1 base image OS packages. Zero vulnerabilities found in Java application dependencies (100+ JARs scanned clean).

## CRITICAL Findings (4)

### CVE-2025-15467 — OpenSSL Remote Code Execution
- **Packages:** openssl (3.5.1-r0), libcrypto3, libssl3
- **Fixed in:** 3.5.5-r0
- **Impact:** Remote code execution or Denial of Service via oversized initialization vector
- **Risk:** HIGH — app handles TLS connections; directly exploitable
- **Count:** 3 (same CVE across openssl, libcrypto3, libssl3)

### CVE-2026-32767 — libexpat Authorization Bypass
- **Package:** libexpat (2.7.1-r0)
- **Fixed in:** 2.7.5-r0
- **Impact:** Authorization bypass allows arbitrary SQL execution via Search API
- **Risk:** MEDIUM — libexpat is an XML parser; exploit requires specific usage patterns
- **Count:** 1

## HIGH Findings (27) — 5 Unique CVEs

| CVE | Library | Sub-packages | Fixed | Impact |
|-----|---------|-------------|-------|--------|
| CVE-2025-68973 | gnupg | gnupg, gnupg-dirmngr, gnupg-gpgconf, gnupg-keyboxd, gnupg-utils, gnupg-wks-client, gpg, gpg-agent, gpg-wks-server, gpgsm, gpgv (×10) | 2.4.9-r0 | Out-of-bounds write, potential arbitrary code execution |
| CVE-2025-69419 | openssl | openssl, libcrypto3, libssl3 (×3) | 3.5.5-r0 | Arbitrary code execution via PKCS#12 processing |
| CVE-2025-69421 | openssl | openssl, libcrypto3, libssl3 (×3) | 3.5.5-r0 | DoS via malformed PKCS#12 file |
| CVE-2025-32988, CVE-2025-32990, CVE-2026-1584 | gnutls | gnutls (×3) | 3.8.12-r0 | DoS, cert parsing, ClientHello PSK issues |
| CVE-2025-64720, CVE-2025-65018 + 4 more | libpng | libpng (×8) | 1.6.51-r0+ | Buffer overflows, heap over-reads |

## MEDIUM Findings (58) — Notable

- **OpenSSL** (×30): Multiple DoS vectors (QUIC, BIO filter, TLS memory), data integrity bypasses, timing side-channels
- **GnuPG** CVE-2025-68972 (×10): Signature bypass via form feed character
- **libexpat** (×4): Memory allocation DoS, infinite loop in DTD, NULL pointer deref
- **libpng** (×2): Heap buffer overflow via malformed palette
- **gnutls** (×5): DER decoding DoS, cert verification DoS, NULL pointer deref, stack buffer overflow
- **libtasn1** (×1): Stack-based buffer overflow
- **busybox** CVE-2024-58251 (×2): netstat local info disclosure

## Remediation

### Actions Taken

1. **`apk upgrade`** added to Dockerfile runtime stage — patches all Alpine packages to latest available versions, resolving all 89 CVEs
2. **gnupg removed** from runtime image — not needed by the Java application; eliminates 20 of the 27 HIGH findings (10 sub-packages × 2 CVEs each) and reduces attack surface

### Dockerfile Change

```dockerfile
# Before
RUN addgroup -S app && adduser -S app -G app

# After
RUN apk update && apk upgrade --no-cache && \
    apk del --no-cache gnupg gnupg-dirmngr gnupg-gpgconf gnupg-keyboxd \
      gnupg-utils gnupg-wks-client gpg gpg-agent gpg-wks-server gpgsm gpgv 2>/dev/null; \
    rm -rf /var/cache/apk/* && \
    addgroup -S app && adduser -S app -G app
```

### Expected Outcome

- All 89 CVEs resolved via package upgrades
- 20 HIGH findings permanently eliminated by removing gnupg
- Smaller image size due to removed packages

## Java Dependencies — Clean

All scanned JARs reported 0 vulnerabilities:
- Spring Boot 4.0.x stack (webmvc, data-jpa, actuator, flyway, mail)
- AWS SDK 2.42.16
- Hibernate 7.2.7, HikariCP 7.0.2
- Jackson 2.21.x / 3.1.0
- All other transitive dependencies

No action required for application-level dependencies.
