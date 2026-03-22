---
phase: 11-visibility-sharing
verified: 2026-03-22T16:00:00Z
status: human_needed
score: 14/14 automated must-haves verified
re_verification: false
human_verification:
  - test: "Set visibility on company detail page"
    expected: "Visibility dropdown changes to PUBLIC after confirming dialog, toast shows 'Visibility updated to PUBLIC', Globe icon appears on company card in list"
    why_human: "Visual confirmation of select dropdown, dialog, toast, and icon badge rendering requires browser"
  - test: "Share by email on company detail page"
    expected: "Entering email and clicking Share creates share, shows in list with Revoke button, toast 'Shared with {email}'"
    why_human: "Full UI CRUD flow with toast feedback and live list update requires running app"
  - test: "Browse public resources page"
    expected: "Browse page shows PUBLIC companies and jobs in tabbed grid with owner email 'Shared by {email}' on each BrowseCard, clicking card navigates to detail"
    why_human: "Requires running backend + frontend with actual public data"
  - test: "Shared-with-me page (requires second user)"
    expected: "Shared page shows companies/jobs shared with current user; clicking navigates to detail showing read-only banner"
    why_human: "Requires two user accounts and actual share creation"
  - test: "Read-only enforcement on detail pages"
    expected: "Non-owner sees 'You are viewing this as a shared resource' banner, no Edit/Delete/VisibilityControl/ShareManager buttons on both company and job detail pages"
    why_human: "UI authorization rendering is browser-only; cannot verify conditional rendering programmatically against live session"
---

# Phase 11: Visibility & Sharing Verification Report

**Phase Goal:** Users can control visibility of their companies and jobs as private, public, or shared with specific users
**Verified:** 2026-03-22T16:00:00Z
**Status:** human_needed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths (Automated)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Existing data remains private after migration — all rows receive PRIVATE default | VERIFIED | V16 migration: `ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE'` for both tables |
| 2 | Shares can be created, queried, and deleted via ResourceShareRepository | VERIFIED | ResourceShareRepository has `existsByResourceTypeAndResourceIdAndSharedWithId`, `findByResourceTypeAndResourceId`, `deleteByResourceTypeAndResourceId`, `findSharedWithUser` |
| 3 | CompanyResponse and JobResponse include visibility field and isOwner boolean | VERIFIED | `CompanyDtos.kt:42` has `val visibility: String`, `val isOwner: Boolean = true`; same in `JobDtos.kt:96-101` |
| 4 | findByIdWithVisibility returns entities accessible to owner, public viewers, and share recipients | VERIFIED | CompanyRepository.kt:39-55 has three-branch OR query; JobRepository.kt:43-59 mirrors it |
| 5 | Owner can set visibility via PATCH endpoint on companies and jobs | VERIFIED | CompanyController PATCH `/{id}/visibility` → `companyService.setVisibility`; JobController same pattern |
| 6 | ShareService handles create/revoke/list with ownership verification, self-share, duplicate, and user-not-found checks | VERIFIED | ShareService.kt lines 23-97 covers all validation branches |
| 7 | Browse endpoints return only PUBLIC resources with owner email via batch-fetch | VERIFIED | `CompanyService.browsePublic` batch-fetches `userRepository.findAllById(ownerIds)`; `BrowseController` exposes `/api/browse/companies` and `/api/browse/jobs` |
| 8 | Shared-with-me endpoints return resources shared with current user | VERIFIED | `SharedWithMeController` at `/api/shared` delegates to `companyService.sharedWithMe` and `jobService.sharedWithMe`, each using `findSharedWithUser` |
| 9 | Update/archive use findByIdAndUserId — owner-only writes (VISI-05) | VERIFIED | CompanyService.update() line 56 and archive() line 67 both use `findByIdAndUserId`; JobService same |
| 10 | isOwner is computed as entity.userId == requestUserId in service getById methods | VERIFIED | CompanyService.getById line 45: `val isOwner = entity.userId == userId`; JobService.getById line 60 same |
| 11 | Frontend types include Visibility type, isOwner field, and share-related interfaces | VERIFIED | `api.ts:358` has `export type Visibility = "PRIVATE" \| "PUBLIC" \| "SHARED"`, `CompanyResponse` has `isOwner?: boolean`, `ShareResponse`, `BrowseCompanyResponse`, `BrowseJobResponse` all present |
| 12 | Sidebar has Browse and Shared with me navigation links | VERIFIED | `sidebar.tsx:25-28` exports `secondaryNavItems` with `href: "/browse"` and `href: "/shared"`, rendered with `Separator` |
| 13 | Browse and Shared pages exist and wire to hooks | VERIFIED | `browse/page.tsx` imports and calls `useBrowseCompanies`/`useBrowseJobs`; `shared/page.tsx` calls `useSharedCompanies`/`useSharedJobs` |
| 14 | Company and job detail pages show VisibilityControl and ShareManager for owners, read-only banner for non-owners | VERIFIED | `companies/[id]/page.tsx`: `isOwner` guards VisibilityControl/ShareManager/Edit/Delete; non-owner shows Eye banner. `jobs/[id]/page.tsx`: same pattern |

**Score:** 14/14 truths verified

### Required Artifacts

| Artifact | Status | Details |
|----------|--------|---------|
| `backend/.../db/migration/V16__phase11_visibility_and_shares.sql` | VERIFIED | Creates visibility columns with PRIVATE default and resource_shares table with unique constraint and indexes |
| `backend/.../entity/Visibility.kt` | VERIFIED | `enum class Visibility { PRIVATE, PUBLIC, SHARED }` |
| `backend/.../entity/ResourceShareEntity.kt` | VERIFIED | `@Table(name = "resource_shares")` entity with resourceType, resourceId, ownerId, sharedWithId |
| `backend/.../repository/ResourceShareRepository.kt` | VERIFIED | All 4 required query methods present |
| `backend/.../repository/CompanyRepository.kt` | VERIFIED | `findByIdWithVisibility`, `findPublic`, `findSharedWithUser` all present |
| `backend/.../repository/JobRepository.kt` | VERIFIED | Same visibility-aware queries as CompanyRepository |
| `backend/.../dto/VisibilityDtos.kt` | VERIFIED | `SetVisibilityRequest` with `@NotNull` visibility field |
| `backend/.../dto/ShareDtos.kt` | VERIFIED | `CreateShareRequest` with email validation; `ShareResponse` with id, email, sharedAt |
| `backend/.../dto/BrowseDtos.kt` | VERIFIED | `BrowseCompanyResponse` and `BrowseJobResponse` with ownerEmail |
| `backend/.../service/ShareService.kt` | VERIFIED | Full implementation with ownership check, user lookup, self-share, duplicate, save, revoke, list with batch-fetch |
| `backend/.../controller/BrowseController.kt` | VERIFIED | `@RequestMapping("/api/browse")` with `/companies` and `/jobs` GET endpoints |
| `backend/.../controller/SharedWithMeController.kt` | VERIFIED | `@RequestMapping("/api/shared")` with `/companies` and `/jobs` GET endpoints |
| `frontend/types/api.ts` | VERIFIED | Visibility type, isOwner on CompanyResponse/JobResponse, ShareResponse, BrowseCompanyResponse, BrowseJobResponse |
| `frontend/hooks/use-visibility.ts` | VERIFIED | `useSetCompanyVisibility` and `useSetJobVisibility` mutations with cache invalidation including browseKeys |
| `frontend/hooks/use-shares.ts` | VERIFIED | `useShares`, `useCreateShare`, `useRevokeShare` with shareKeys factory |
| `frontend/hooks/use-browse.ts` | VERIFIED | `useBrowseCompanies`, `useBrowseJobs` with browseKeys factory |
| `frontend/hooks/use-shared-with-me.ts` | VERIFIED | `useSharedCompanies`, `useSharedJobs` with sharedWithMeKeys factory |
| `frontend/components/shared/visibility-badge.tsx` | VERIFIED | Globe for PUBLIC, Users for SHARED, null for PRIVATE |
| `frontend/components/shared/visibility-control.tsx` | VERIFIED | Select with confirm dialog for PUBLIC transition, immediate mutation for PRIVATE/SHARED |
| `frontend/components/shared/share-manager.tsx` | VERIFIED | Full share CRUD: list, add form, revoke with confirm dialog, toast success/error |
| `frontend/components/browse/browse-card.tsx` | VERIFIED | Card with Globe icon, name, subtitle, location, "Shared by {ownerEmail}" |
| `frontend/components/companies/company-card.tsx` | VERIFIED | VisibilityBadge rendered next to CardTitle; `hideActions` prop hides dropdown |
| `frontend/app/(dashboard)/browse/page.tsx` | VERIFIED | Tabbed Companies/Jobs grid using BrowseCard, empty state "No public resources yet" |
| `frontend/app/(dashboard)/shared/page.tsx` | VERIFIED | Tabbed Companies/Jobs grid, empty state "Nothing shared with you" |
| `frontend/app/(dashboard)/companies/[id]/page.tsx` | VERIFIED | isOwner gates Edit/Delete/VisibilityControl/ShareManager; non-owner sees Eye banner |
| `frontend/app/(dashboard)/jobs/[id]/page.tsx` | VERIFIED | Job detail page with full field display, same isOwner gating and read-only banner |
| `backend/src/test/kotlin/.../visibility/` (8 test files) | VERIFIED | 8 files present; VisibilityEnumTest has real assertions (no @Disabled); BrowsePublicControllerTest/CompanyVisibilityServiceTest have @SpringBootTest with real test bodies |

### Key Link Verification

| From | To | Via | Status | Evidence |
|------|----|-----|--------|---------|
| CompanyEntity.visibility | V16 companies.visibility column | `@Column + @Enumerated` | WIRED | CompanyEntity.kt:25-27: `@Column(name = "visibility") @Enumerated(EnumType.STRING) var visibility: Visibility` |
| ResourceShareEntity | V16 resource_shares table | `@Table(name = "resource_shares")` | WIRED | ResourceShareEntity.kt:15: `@Table(name = "resource_shares")` |
| CompanyController PATCH /visibility | CompanyService.setVisibility | controller delegates | WIRED | CompanyController.kt:82: `companyService.setVisibility(id, request.visibility, userId)` |
| CompanyController share endpoints | ShareService.createShare | controller delegates | WIRED | CompanyController.kt:97: `shareService.createShare(ResourceType.COMPANY, id, request.email, userId)` |
| CompanyService.getById | CompanyRepository.findByIdWithVisibility | visibility-aware read | WIRED | CompanyService.kt:43: `companyRepository.findByIdWithVisibility(id, userId)` |
| CompanyService.update/archive | CompanyRepository.findByIdAndUserId | owner-only write | WIRED | CompanyService.kt:56,67: both use `findByIdAndUserId` |
| CompanyService.browsePublic | userRepository.findAllById | batch-fetch emails | WIRED | CompanyService.kt:94: `userRepository.findAllById(ownerIds).associateBy(...)` |
| JobService.getById | JobRepository.findByIdWithVisibility | visibility-aware read | WIRED | JobService.kt:58: `jobRepository.findByIdWithVisibility(id, userId)` |
| sidebar.tsx Browse link | /browse page | Next.js Link href | WIRED | sidebar.tsx:26: `href: "/browse"` |
| browse/page.tsx | use-browse.ts hooks | useBrowseCompanies | WIRED | browse/page.tsx:8,12-13: imports and calls both hooks |
| shared/page.tsx | use-shared-with-me.ts hooks | useSharedCompanies | WIRED | shared/page.tsx:10: import; line 14-15: calls both hooks |
| companies/[id]/page.tsx | VisibilityControl + ShareManager | component composition | WIRED | companies/[id]/page.tsx:168-179: renders both components under `{isOwner &&` guard |
| jobs/[id]/page.tsx | VisibilityControl + ShareManager | component composition | WIRED | jobs/[id]/page.tsx:231-244: renders both components under `{isOwner &&` guard |
| use-visibility.ts | /api/companies/{id}/visibility | apiClient PATCH call | WIRED | use-visibility.ts:20-22: `apiClient<CompanyResponse>(\`/companies/${id}/visibility\`, { method: "PATCH" })` |
| use-shares.ts | /api/companies/{id}/shares | apiClient POST/GET/DELETE | WIRED | use-shares.ts:24,41-48,62-68: all three mutation/query paths present |
| use-browse.ts | /api/browse/companies | apiClient GET call | WIRED | use-browse.ts:31: `apiClient<PaginatedResponse<BrowseCompanyResponse>>(\`/browse/companies...\`)` |

### Requirements Coverage

| Requirement | Source Plans | Description | Status | Evidence |
|-------------|-------------|-------------|--------|---------|
| VISI-01 | 11-01, 11-02, 11-03, 11-04 | User can set visibility (PRIVATE/PUBLIC/SHARED) on companies and jobs | SATISFIED | PATCH `/api/companies/{id}/visibility` and `/api/jobs/{id}/visibility` implemented and wired through to VisibilityControl UI on both detail pages |
| VISI-02 | 11-01, 11-02, 11-03, 11-04 | User can share specific companies or jobs with other users (by email) | SATISFIED | ShareService.createShare with POST `/api/{type}/{id}/shares`; ShareManager component with email form and revoke |
| VISI-03 | 11-01, 11-02, 11-03, 11-04 | User can browse public companies and jobs from other users | SATISFIED | BrowseController at `/api/browse`; browse/page.tsx with tabbed BrowseCard grid and ownerEmail display |
| VISI-04 | 11-02, 11-03, 11-04 | User can view items shared with them | SATISFIED | SharedWithMeController at `/api/shared`; shared/page.tsx with tabbed grid |
| VISI-05 | 11-01, 11-02, 11-04 | Shared users can only VIEW (not edit/delete) — view-only always | SATISFIED | update/archive use `findByIdAndUserId` (owner-only); frontend `isOwner` gate hides Edit/Delete/VisibilityControl/ShareManager; read-only banner shown to non-owners |

All 5 VISI requirements are satisfied. No orphaned requirements — REQUIREMENTS.md lists exactly VISI-01 through VISI-05 for Phase 11, all claimed by plans 11-01 through 11-04.

### Anti-Patterns Found

| File | Pattern | Severity | Impact |
|------|---------|----------|--------|
| None found | — | — | — |

No stub implementations, no TODO/FIXME comments, no empty handlers, no placeholder returns detected in phase-modified files.

### Human Verification Required

#### 1. Visibility Control UI Flow

**Test:** Log in, navigate to a company detail page. Find the visibility dropdown. Change it from PRIVATE to PUBLIC and confirm the "Make publicly visible?" dialog.
**Expected:** Dialog appears on selecting PUBLIC. After confirming, toast shows "Visibility updated to PUBLIC". Go to the company list — Globe icon badge appears next to the company name.
**Why human:** Visual rendering of Select component, dialog overlay, toast notification, and icon badge requires a running browser session.

#### 2. Share by Email UI Flow

**Test:** On a company detail page, change visibility to SHARED. In the ShareManager section, enter a registered user's email and click Share.
**Expected:** Toast "Shared with {email}" appears. Share appears in the list with email, date, and Revoke button. Clicking Revoke opens confirm dialog; confirming shows "Access revoked for {email}".
**Why human:** Full CRUD flow with live API calls, toast feedback, and real-time list updates requires a running backend and frontend.

#### 3. Browse Public Resources Page

**Test:** Create a PUBLIC company or job. Log in (same or different user). Click "Browse" in sidebar. Switch between Companies and Jobs tabs.
**Expected:** Public resources appear as BrowseCard tiles with "Shared by {ownerEmail}" text. Clicking a card navigates to the resource's detail page.
**Why human:** Requires actual data in the database with visibility=PUBLIC and a running backend.

#### 4. Shared With Me Page (requires two users)

**Test:** As user A, share a company with user B (set visibility=SHARED, share by email). Log in as user B. Click "Shared with me" in sidebar.
**Expected:** The shared company appears in the Companies tab. Clicking it navigates to the company detail page showing "You are viewing this as a shared resource" banner. No Edit, Delete, VisibilityControl, or ShareManager is visible.
**Why human:** Requires two separate user accounts and an actual share operation.

#### 5. Read-Only View on Job Detail Page

**Test:** As a non-owner user, navigate to a PUBLIC or SHARED job's detail URL directly.
**Expected:** "You are viewing a public resource" (or "shared resource") banner at top. Job fields (title, description, salary, work mode etc.) display correctly. No Edit, Delete, VisibilityControl, or ShareManager buttons/sections visible. Notes section hidden for non-owners.
**Why human:** Requires real session as non-owner with an actual job record accessible to them.

### Summary

All 14 automated must-haves are fully verified in the codebase:

**Backend (Plans 01 + 02):** The data foundation is complete — V16 Flyway migration adds visibility columns with PRIVATE defaults and the polymorphic resource_shares table. All entities, repositories, DTOs, services, and controllers are implemented. ShareService covers the full share lifecycle with ownership, self-share, and duplicate validation. CompanyService and JobService use `findByIdWithVisibility` for reads and `findByIdAndUserId` for writes, computing `isOwner` dynamically. BrowseController and SharedWithMeController are wired to their respective service methods. Eight integration test files exist with substantive test bodies (not stubs).

**Frontend (Plans 03 + 04):** Types, hooks, and components are all present and wired. The `Visibility` type, `isOwner` optional field, and share-related response interfaces are on `api.ts`. Four hook files cover all API operations with proper query key invalidation. Four reusable components exist (VisibilityBadge, VisibilityControl, ShareManager, BrowseCard). The sidebar has Browse and Shared navigation links. Browse and Shared pages are implemented with tabbed grids. Company and job detail pages conditionally render owner controls and non-owner read-only banners via the `isOwner !== false` pattern.

Five human verification items remain — these are all UI/UX behaviors (dialogs, toasts, real-time updates, multi-user flows) that require a running application to confirm. The code paths that support each of them are fully implemented and wired.

---

_Verified: 2026-03-22T16:00:00Z_
_Verifier: Claude (gsd-verifier)_
