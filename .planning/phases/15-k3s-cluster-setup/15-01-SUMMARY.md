---
phase: 15-k3s-cluster-setup
plan: 01
subsystem: infra
tags: [k3s, bash, ssh, kubectl, spring-boot, kubeconfig]

# Dependency graph
requires:
  - phase: 14-aws-provisioning
    provides: EC2 instance with Elastic IP, SSH key pair, OpenTofu outputs
provides:
  - K3s bootstrap script (SSH install on EC2)
  - Kubeconfig fetch and merge script
  - SSH tunnel script for kubectl access
  - Staging scale-up/down scripts
  - Spring Boot production profile with env var placeholders
affects: [15-02, 15-03, 16-backup-strategy, 17-sealed-secrets, 18-dns-tls]

# Tech tracking
tech-stack:
  added: []
  patterns: [tofu-output-in-scripts, ssh-tunnel-kubectl, env-var-placeholder-profile]

key-files:
  created:
    - infra/scripts/bootstrap-k3s.sh
    - infra/scripts/setup-kubeconfig.sh
    - infra/scripts/connect.sh
    - infra/scripts/staging-up.sh
    - infra/scripts/staging-down.sh
    - backend/src/main/resources/application-prod.yml
  modified: []

key-decisions:
  - "All scripts read tofu outputs dynamically via tofu -chdir — no hardcoded IPs"
  - "SSH tunnel pattern for kubectl (port 6443 not exposed in security group)"
  - "application-prod.yml uses env var placeholders matching future K8s ConfigMap/Secret keys"

patterns-established:
  - "Script preamble: SCRIPT_DIR + PROJECT_ROOT + tofu output for infrastructure values"
  - "Staging scale via kubectl scale deploy/statefulset --all in jobhunt-staging namespace"

requirements-completed: [K8S-02]

# Metrics
duration: 3min
completed: 2026-03-23
---

# Phase 15 Plan 01: K3s Operational Scripts Summary

**5 bash scripts for K3s lifecycle management (bootstrap, kubeconfig, SSH tunnel, staging scale) plus Spring Boot production profile with 23 env var placeholders**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-23T14:57:50Z
- **Completed:** 2026-03-23T15:01:08Z
- **Tasks:** 2
- **Files created:** 6

## Accomplishments
- Created 5 operational scripts for K3s cluster management, all passing bash -n syntax validation
- All SSH scripts dynamically read infrastructure values from OpenTofu outputs (no hardcoded IPs)
- Created application-prod.yml with all 23 env var placeholders for DB, Redis, MinIO, JWT, SMTP, and app config
- Production profile disables docker-compose, adds Flyway connect-retries, restricts health endpoint visibility

## Task Commits

Each task was committed atomically:

1. **Task 1: Create K3s bootstrap, kubeconfig, and connect scripts** - `fa3bb20` (feat)
2. **Task 2: Create staging scale scripts and backend production profile** - `eb0bcf5` (feat)

## Files Created/Modified
- `infra/scripts/bootstrap-k3s.sh` - SSH into EC2, install K3s with K3S_KUBECONFIG_MODE=644
- `infra/scripts/setup-kubeconfig.sh` - SCP kubeconfig, merge into ~/.kube/config, rename context to jobhunt-k3s
- `infra/scripts/connect.sh` - SSH tunnel on port 6443 for kubectl access
- `infra/scripts/staging-up.sh` - Scale jobhunt-staging namespace to replicas=1, wait for Ready
- `infra/scripts/staging-down.sh` - Scale jobhunt-staging namespace to replicas=0
- `backend/src/main/resources/application-prod.yml` - Spring Boot prod profile with env var placeholders

## Decisions Made
None - followed plan as specified.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required. Phase 14 must be complete (EC2 provisioned) before running these scripts.

## Next Phase Readiness
- Scripts ready for use once EC2 instance is provisioned (Phase 14)
- application-prod.yml ready for K8s ConfigMap/Secret integration (Phase 15-02/15-03)
- Staging scale scripts ready once K8s manifests are deployed (Phase 15-02)

## Self-Check: PASSED

All 6 created files verified on disk. Both task commits (fa3bb20, eb0bcf5) verified in git log.

---
*Phase: 15-k3s-cluster-setup*
*Completed: 2026-03-23*
