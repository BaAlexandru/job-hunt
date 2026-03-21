# Phase 8: Frontend Core Views - Research

**Researched:** 2026-03-21
**Domain:** React frontend views -- kanban board, data table, CRUD forms, file upload
**Confidence:** HIGH

## Summary

Phase 8 builds the core interactive frontend pages on top of the Phase 7 shell. The two novel UI challenges are (1) a drag-and-drop kanban board for application status management and (2) a sortable/filterable data table. The remaining work is conventional CRUD pages using forms, modals, and the existing shadcn/ui component library.

For the kanban board, use `@dnd-kit/core` + `@dnd-kit/sortable` (the stable production API -- NOT the newer `@dnd-kit/react` which is still in beta at v0.3.2). Dice UI provides an installable `@diceui/kanban` component that wraps dnd-kit with shadcn/ui-compatible composable primitives. For the data table, use `@tanstack/react-table` v8 with the shadcn/ui data-table pattern (headless table + shadcn Table component). React Hook Form + Zod are already installed for forms. File upload uses `react-dropzone` for the drag-and-drop zone.

**Primary recommendation:** Use Dice UI's kanban component (`npx shadcn@latest add @diceui/kanban`) for the board, TanStack Table for the list view, and standard shadcn/ui Dialog/Sheet patterns for CRUD modals and detail panels.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- All 8 status columns visible with horizontal scroll if needed
- Compact cards: company name, job title, next action date (if set)
- Column headers show count badges (e.g., "Applied (3)")
- Invalid transitions prevented during drag -- invalid columns dim/gray out, card snaps back
- Clicking a card opens a right-side slide-over detail panel (Linear-style)
- Top-level "+ New Application" button above the board
- Mobile: horizontal scroll through columns (swipe)
- Archived applications hidden by default with toggle to show
- Essential table columns: Company, Job Title, Status (badge), Applied Date, Next Action Date, Last Activity
- Inline filter bar above table: Status dropdown, Company dropdown, Date range, Search input (GitHub issues style)
- Clicking a row opens the same slide-over detail panel as kanban
- Inline status dropdown in each row for quick status changes (shows valid transitions only)
- Right-side slide-over panel used from both kanban and list views (one shared component)
- Tabbed content inside panel: Overview | Notes | Interviews | Documents
- Create/edit forms appear as centered modal dialogs (shadcn/ui Dialog)
- Delete actions require confirmation dialog before archive/soft-delete
- Companies page: card grid layout (company name, website, location, job count)
- Jobs page: standalone page showing all jobs with company filter
- Jobs also visible within company detail (both access paths)
- Documents page: drag-and-drop upload zone + file browser
- Interviews: managed from within application detail panel (Interviews tab)
- Applications page: tab bar to toggle between "Board" (kanban) and "List" (table) views
- Dashboard: summary cards with key metrics (total applications, by-status breakdown, upcoming interviews, recent activity)
- View preference (Board vs List) remembered across sessions

### Claude's Discretion
- Drag-and-drop library choice (dnd-kit, @hello-pangea/dnd, or similar)
- Table library choice (TanStack Table or manual implementation)
- Exact card styling and column widths on kanban
- Dashboard chart/metric component details
- Loading states and skeleton designs
- Pagination approach for list view (infinite scroll vs numbered pages)
- Form field ordering and layout within modals
- Date picker component choice
- File upload library for drag-and-drop zone
- How "remember view preference" is persisted (localStorage, cookie, etc.)

### Deferred Ideas (OUT OF SCOPE)
- Analytics dashboard with charts (funnel, trends) -- v2 requirement (DASH-01, DASH-02, DASH-03)
- Tags and custom labels on applications -- v2 (TAGS-01)
- Salary comparison view -- v2 (SALA-01)
- Follow-up reminders -- v2 (RMND-01)
- Dark/light mode manual toggle -- currently system-preference only
- Keyboard shortcuts for kanban
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| APPL-03 | User can view applications as a kanban board with drag-and-drop between status columns | Dice UI kanban component with @dnd-kit/core + sortable; optimistic updates via TanStack Query; transition validation from GET /{id}/transitions endpoint |
| APPL-04 | User can view applications as a sortable, filterable table/list | TanStack Table v8 with shadcn data-table pattern; server-side filtering via existing paginated API; column definitions with sorting state |
</phase_requirements>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| @dnd-kit/core | ^6.3 | Drag-and-drop engine | Stable production API; most-used React DnD library since react-beautiful-dnd deprecated |
| @dnd-kit/sortable | ^10.0 | Sortable presets for columns/cards | Thin layer over core for reorderable lists -- exactly what kanban needs |
| @dnd-kit/modifiers | ^9.0 | Drag constraints/snapping | Restrict drag axis, constrain to container |
| @dnd-kit/utilities | ^4.0 | CSS transform helpers | Required by sortable for transform application |
| @tanstack/react-table | ^8.21 | Headless data table | Powers tables at Linear, Notion; shadcn/ui's official data-table recommendation |
| react-dropzone | ^14.3 | File upload drop zone | De facto standard for React file upload; actively maintained (Feb 2026 update) |
| react-day-picker | ^8 (via shadcn calendar) | Date picker | shadcn/ui Calendar component built on this; already compatible with project |

### Already Installed (from Phase 7)
| Library | Version | Purpose |
|---------|---------|---------|
| @tanstack/react-query | ^5.91 | Server state, caching, optimistic updates |
| react-hook-form | ^7.71 | Form state management |
| @hookform/resolvers | ^5.2 | Zod resolver for RHF |
| zod | ^4.3 | Schema validation |
| sonner | ^2.0 | Toast notifications |
| radix-ui | ^1.4 | Primitives (Dialog, Sheet, Select, etc.) |
| lucide-react | ^0.577 | Icons |

### Supporting (install via shadcn CLI)
| Component | shadcn CLI | Purpose |
|-----------|-----------|---------|
| Dialog | `npx shadcn@latest add dialog` | Create/edit modals |
| Badge | `npx shadcn@latest add badge` | Status badges in table and kanban |
| Table | `npx shadcn@latest add table` | Data table rendering |
| Popover | `npx shadcn@latest add popover` | Date picker container, filter dropdowns |
| Calendar | `npx shadcn@latest add calendar` | Date selection (installs react-day-picker) |
| Textarea | `npx shadcn@latest add textarea` | Notes input |
| Skeleton | `npx shadcn@latest add skeleton` | Loading states |
| Scroll Area | `npx shadcn@latest add scroll-area` | Kanban horizontal scroll |
| Command | `npx shadcn@latest add command` | Searchable dropdown for company select |
| Kanban (Dice UI) | `npx shadcn@latest add @diceui/kanban` | Pre-built kanban board primitives |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| @dnd-kit/core | @hello-pangea/dnd | Easier API for simple kanban but less control over transition validation; no column dimming support; performance overhead |
| @dnd-kit/core | @dnd-kit/react (v0.3.2) | Newer API but still beta -- not production-ready |
| @tanstack/react-table | Manual table with map() | Loses sorting, filtering, pagination primitives; fine for <20 rows but not scalable |
| react-dropzone | Native drag events | Edge cases around file type detection, multiple files, accessibility -- not worth hand-rolling |

### Discretion Decisions (Researcher Recommendations)

**Drag-and-drop library:** Use `@dnd-kit/core` + `@dnd-kit/sortable` via the Dice UI kanban component. Reasons: (1) Dice UI provides composable shadcn/ui-compatible kanban primitives out of the box, (2) @dnd-kit/core is the stable production API, (3) built-in accessibility, (4) fine-grained control needed for transition validation (dimming invalid columns).

**Table library:** Use `@tanstack/react-table` v8. It is the official recommendation from shadcn/ui for data tables and provides headless sorting, filtering, and pagination.

**Pagination approach:** Use numbered pages (not infinite scroll). Reasons: (1) matches the Spring Data `Pageable` API response which already returns `totalPages`/`totalElements`, (2) better for "jump to page" workflows, (3) simpler implementation.

**Date picker:** Use shadcn/ui Calendar component (built on react-day-picker v8). Install via `npx shadcn@latest add calendar` and `npx shadcn@latest add popover`.

**File upload library:** Use `react-dropzone` with the `useDropzone` hook. Lightweight, well-maintained, handles edge cases.

**View preference persistence:** Use `localStorage`. Simple, no server round-trip, survives page refresh. Key: `jobhunt:applications-view` with values `"board"` or `"list"`.

**Installation:**
```bash
cd frontend
pnpm add @tanstack/react-table react-dropzone
npx shadcn@latest add dialog badge table popover calendar textarea skeleton scroll-area command @diceui/kanban
```

## Architecture Patterns

### Recommended Project Structure
```
frontend/
├── app/(dashboard)/
│   ├── applications/
│   │   └── page.tsx              # Board/List toggle, shared state
│   ├── companies/
│   │   ├── page.tsx              # Company card grid
│   │   └── [id]/page.tsx         # Company detail with jobs list
│   ├── jobs/
│   │   └── page.tsx              # Jobs table with company filter
│   ├── documents/
│   │   └── page.tsx              # Upload zone + file browser
│   └── dashboard/
│       └── page.tsx              # Summary metrics cards
├── components/
│   ├── applications/
│   │   ├── application-board.tsx     # Kanban board wrapper
│   │   ├── application-list.tsx      # Data table wrapper
│   │   ├── application-card.tsx      # Kanban card (compact)
│   │   ├── application-detail.tsx    # Slide-over panel (Sheet)
│   │   ├── application-form.tsx      # Create/edit form (Dialog)
│   │   └── status-badge.tsx          # Colored status badge
│   ├── companies/
│   │   ├── company-card.tsx          # Grid card
│   │   └── company-form.tsx          # Create/edit form
│   ├── jobs/
│   │   ├── job-list.tsx              # Jobs data table
│   │   └── job-form.tsx              # Create/edit form
│   ├── documents/
│   │   ├── document-upload.tsx       # Dropzone upload area
│   │   └── document-list.tsx         # File browser table
│   ├── interviews/
│   │   ├── interview-list.tsx        # Interview rounds list
│   │   └── interview-form.tsx        # Create/edit interview
│   └── shared/
│       ├── confirm-dialog.tsx        # Reusable delete confirmation
│       ├── filter-bar.tsx            # Inline filter controls
│       ├── data-table.tsx            # Reusable TanStack Table wrapper
│       └── empty-state.tsx           # (existing)
├── hooks/
│   ├── use-applications.ts           # TanStack Query hooks for applications API
│   ├── use-companies.ts              # TanStack Query hooks for companies API
│   ├── use-jobs.ts                   # TanStack Query hooks for jobs API
│   ├── use-interviews.ts             # TanStack Query hooks for interviews API
│   ├── use-documents.ts              # TanStack Query hooks for documents API
│   └── use-view-preference.ts        # localStorage-backed view toggle
└── types/
    └── api.ts                        # TypeScript types mirroring backend DTOs
```

### Pattern 1: TanStack Query Hooks per Domain
**What:** Each domain entity gets a custom hooks file that wraps `useQuery` and `useMutation` for all CRUD operations.
**When to use:** Every page that fetches or mutates data.
**Example:**
```typescript
// hooks/use-applications.ts
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { apiClient } from "@/lib/api-client"
import type { ApplicationResponse, CreateApplicationRequest } from "@/types/api"

export const applicationKeys = {
  all: ["applications"] as const,
  lists: () => [...applicationKeys.all, "list"] as const,
  list: (filters: Record<string, unknown>) => [...applicationKeys.lists(), filters] as const,
  details: () => [...applicationKeys.all, "detail"] as const,
  detail: (id: string) => [...applicationKeys.details(), id] as const,
  transitions: (id: string) => [...applicationKeys.all, "transitions", id] as const,
}

export function useApplications(filters: Record<string, unknown> = {}) {
  return useQuery({
    queryKey: applicationKeys.list(filters),
    queryFn: () => {
      const params = new URLSearchParams()
      Object.entries(filters).forEach(([k, v]) => {
        if (v != null && v !== "") params.set(k, String(v))
      })
      return apiClient<PaginatedResponse<ApplicationResponse>>(
        `/applications?${params.toString()}`
      )
    },
  })
}

export function useUpdateApplicationStatus() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: string }) =>
      apiClient<ApplicationResponse>(`/applications/${id}/status`, {
        method: "PATCH",
        body: JSON.stringify({ status }),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: applicationKeys.all })
    },
  })
}
```

### Pattern 2: Optimistic Drag-and-Drop Status Updates
**What:** When a card is dropped on a valid column, update the UI immediately and revert on error.
**When to use:** Kanban board drag-and-drop.
**Example:**
```typescript
// Optimistic update pattern for drag-and-drop
const updateStatus = useMutation({
  mutationFn: ({ id, status }: { id: string; status: string }) =>
    apiClient(`/applications/${id}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status }),
    }),
  onMutate: async ({ id, status }) => {
    // Cancel outgoing refetches
    await queryClient.cancelQueries({ queryKey: applicationKeys.all })
    // Snapshot previous state
    const previous = queryClient.getQueryData(applicationKeys.lists())
    // Optimistically update the cache
    queryClient.setQueriesData(
      { queryKey: applicationKeys.lists() },
      (old: any) => {
        if (!old) return old
        return {
          ...old,
          content: old.content.map((app: ApplicationResponse) =>
            app.id === id ? { ...app, status } : app
          ),
        }
      }
    )
    return { previous }
  },
  onError: (_err, _vars, context) => {
    // Rollback on error
    if (context?.previous) {
      queryClient.setQueriesData(
        { queryKey: applicationKeys.lists() },
        context.previous
      )
    }
    toast.error("Failed to update status")
  },
  onSettled: () => {
    queryClient.invalidateQueries({ queryKey: applicationKeys.all })
  },
})
```

### Pattern 3: Shared Detail Panel (Sheet Component)
**What:** A single `ApplicationDetail` component rendered as a right-side slide-over, used from both kanban and list views.
**When to use:** Any view that shows application details.
**Example:**
```typescript
// components/applications/application-detail.tsx
"use client"
import { Sheet, SheetContent, SheetHeader, SheetTitle } from "@/components/ui/sheet"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"

interface ApplicationDetailProps {
  applicationId: string | null
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function ApplicationDetail({ applicationId, open, onOpenChange }: ApplicationDetailProps) {
  const { data: application } = useApplication(applicationId)

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="w-full sm:max-w-lg">
        <SheetHeader>
          <SheetTitle>{application?.jobTitle}</SheetTitle>
        </SheetHeader>
        <Tabs defaultValue="overview" className="mt-4">
          <TabsList className="w-full">
            <TabsTrigger value="overview">Overview</TabsTrigger>
            <TabsTrigger value="notes">Notes</TabsTrigger>
            <TabsTrigger value="interviews">Interviews</TabsTrigger>
            <TabsTrigger value="documents">Documents</TabsTrigger>
          </TabsList>
          <TabsContent value="overview">...</TabsContent>
          <TabsContent value="notes">...</TabsContent>
          <TabsContent value="interviews">...</TabsContent>
          <TabsContent value="documents">...</TabsContent>
        </Tabs>
      </SheetContent>
    </Sheet>
  )
}
```

### Pattern 4: Form Dialog with React Hook Form + Zod
**What:** Modal dialogs for create/edit operations with validation.
**When to use:** All CRUD create/edit forms.
**Example:**
```typescript
// Shared pattern for all entity forms
"use client"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog"

const companySchema = z.object({
  name: z.string().min(1, "Name is required").max(255),
  website: z.string().url().max(500).optional().or(z.literal("")),
  location: z.string().max(255).optional(),
  notes: z.string().optional(),
})

type CompanyFormData = z.infer<typeof companySchema>

export function CompanyForm({ open, onOpenChange, initialData, onSubmit }) {
  const form = useForm<CompanyFormData>({
    resolver: zodResolver(companySchema),
    defaultValues: initialData ?? { name: "", website: "", location: "", notes: "" },
  })

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{initialData ? "Edit" : "Add"} Company</DialogTitle>
        </DialogHeader>
        <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
          {/* form fields */}
        </form>
      </DialogContent>
    </Dialog>
  )
}
```

### Pattern 5: Reusable Data Table Component
**What:** A generic data-table wrapper using TanStack Table that any entity page can reuse.
**When to use:** Applications list, jobs list, documents list.
**Example:**
```typescript
// components/shared/data-table.tsx
"use client"
import {
  ColumnDef,
  flexRender,
  getCoreRowModel,
  useReactTable,
  getSortedRowModel,
  SortingState,
} from "@tanstack/react-table"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"

interface DataTableProps<TData, TValue> {
  columns: ColumnDef<TData, TValue>[]
  data: TData[]
  onRowClick?: (row: TData) => void
}

export function DataTable<TData, TValue>({
  columns, data, onRowClick,
}: DataTableProps<TData, TValue>) {
  const [sorting, setSorting] = useState<SortingState>([])
  const table = useReactTable({
    data, columns,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
    onSortingChange: setSorting,
    state: { sorting },
  })

  return (
    <Table>
      <TableHeader>
        {table.getHeaderGroups().map((headerGroup) => (
          <TableRow key={headerGroup.id}>
            {headerGroup.headers.map((header) => (
              <TableHead key={header.id}>
                {flexRender(header.column.columnDef.header, header.getContext())}
              </TableHead>
            ))}
          </TableRow>
        ))}
      </TableHeader>
      <TableBody>
        {table.getRowModel().rows.map((row) => (
          <TableRow
            key={row.id}
            onClick={() => onRowClick?.(row.original)}
            className={onRowClick ? "cursor-pointer" : ""}
          >
            {row.getVisibleCells().map((cell) => (
              <TableCell key={cell.id}>
                {flexRender(cell.column.columnDef.cell, cell.getContext())}
              </TableCell>
            ))}
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
```

### Anti-Patterns to Avoid
- **Fetching all applications client-side then filtering:** Use the backend's paginated/filtered API (GET /api/applications with query params). The backend already supports multi-status filtering, text search, date ranges, and pagination via Spring Pageable.
- **Separate query hooks per view (board vs list):** Both views show the same data. Use a single query hook with shared query keys so cache is shared.
- **Giant monolithic page components:** Extract kanban board, data table, detail panel, and forms into separate components. The applications page should be a thin orchestrator.
- **Storing application data in local state:** Use TanStack Query as the single source of truth. Local state only for UI concerns (selected card, open panel, view preference).
- **Calling GET /transitions for every card on load:** Fetch transitions only when needed (on drag start, or when opening status dropdown). The transition map is deterministic per status -- could also be replicated client-side as a constant.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Drag-and-drop kanban | Custom mouse event handlers | @dnd-kit/core + @dnd-kit/sortable (via Dice UI kanban) | Accessibility (keyboard, screen reader), touch support, collision detection, animation -- massive surface area |
| Data table with sort/filter | Manual `<table>` with onClick sort | @tanstack/react-table + shadcn Table | Column resizing, multi-sort, filter state, pagination math -- dozens of edge cases |
| File upload drop zone | `<input type="file">` with drag events | react-dropzone useDropzone hook | File type validation, multiple files, accessibility, paste support |
| Date picker | `<input type="date">` | shadcn Calendar + Popover | Consistent cross-browser styling, range selection, locale support |
| Toast notifications | Custom alert div | sonner (already installed) | Animation, auto-dismiss, stacking, deduplication |
| Form validation | Manual onChange handlers | React Hook Form + Zod | Field-level errors, dirty tracking, async validation, submit handling |
| Confirmation dialogs | window.confirm() | shadcn AlertDialog | Styled, accessible, async, non-blocking |
| Status transition validation | Client-only validation map | Backend GET /{id}/transitions endpoint | Source of truth is backend; client can mirror for UX but must validate server-side |

**Key insight:** The frontend has no novel algorithmic problems. Every UI pattern here (kanban, data table, forms, file upload) has mature, well-tested library solutions. The complexity is in *composition* -- wiring these together with the backend API correctly.

## Common Pitfalls

### Pitfall 1: Kanban Column as Drop Target vs Sort Container
**What goes wrong:** Treating each column as a simple "drop target" instead of a "sortable container" means cards can be dropped into columns but not reordered within them. The Dice UI kanban handles this correctly with KanbanColumn + KanbanItem composables.
**Why it happens:** Confusion between DndContext's basic droppable zones and SortableContext's reorderable lists.
**How to avoid:** Use the Dice UI kanban component which wraps both concepts correctly. Each column is both a droppable zone for cross-column movement and a sortable container.
**Warning signs:** Cards disappear on drop, or drop only works at the bottom of a column.

### Pitfall 2: Race Condition in Optimistic Updates
**What goes wrong:** User drags a card, optimistic update fires, then a background refetch from another mutation overwrites the optimistic state before the PATCH completes.
**Why it happens:** TanStack Query refetches on window focus or after related mutations.
**How to avoid:** Call `queryClient.cancelQueries()` in `onMutate` before applying the optimistic update. Always use `onSettled` (not `onSuccess`) to invalidate queries so it runs even on error.
**Warning signs:** Card briefly shows in new column then "jumps back" before settling.

### Pitfall 3: Stale Transition Validation on Drag
**What goes wrong:** Transitions fetched once on page load become stale after a status change. User drags to a now-invalid column.
**Why it happens:** Transition validity depends on *current* status, which changes after each drag.
**How to avoid:** Mirror the transition map client-side as a constant (it is deterministic per status) for instant validation. Use backend GET /{id}/transitions as the authoritative check only when needed for edge cases.
**Warning signs:** Backend returns 422 "Invalid status transition" after a successful drop.

### Pitfall 4: Spring Page Response Shape
**What goes wrong:** TanStack Query hooks try to access `data.items` or `data.results` but Spring returns `data.content`.
**Why it happens:** Different frameworks use different pagination shapes.
**How to avoid:** Define a `PaginatedResponse<T>` type that matches Spring's Page format: `{ content: T[], totalElements: number, totalPages: number, number: number, size: number }`.
**Warning signs:** "Cannot read property 'map' of undefined" on list pages.

### Pitfall 5: File Upload Content-Type Header Collision
**What goes wrong:** The `apiClient` wrapper sets `Content-Type: application/json` on all requests, which breaks `multipart/form-data` uploads.
**Why it happens:** The apiClient always adds JSON content type header.
**How to avoid:** When sending FormData, omit the Content-Type header and let the browser set it with the correct boundary. Modify apiClient or use a separate upload function.
**Warning signs:** 415 Unsupported Media Type or boundary parsing errors from the backend.

### Pitfall 6: Dialog/Sheet Z-Index Conflicts
**What goes wrong:** Opening a Dialog (modal) from inside a Sheet (slide-over) causes the Dialog to render behind the Sheet.
**Why it happens:** Both use Radix Portal but Sheet may have a higher z-index.
**How to avoid:** Ensure the confirmation dialog is portaled outside the Sheet, or use Radix's nested portal support. Test the flow: detail panel (Sheet) -> delete button -> confirmation dialog (AlertDialog).
**Warning signs:** Confirmation dialog is not visible or not clickable.

### Pitfall 7: Backend Auth Session Mismatch
**What goes wrong:** API calls from the frontend return 401 because the backend expects JWT but the frontend uses Better Auth session cookies.
**Why it happens:** Phase 7 CONTEXT.md notes: "backend security will be updated to accept Better Auth session tokens -- this is a Phase 8 concern."
**How to avoid:** Phase 8 must update the backend SecurityConfig to validate Better Auth session cookies. The frontend already sends `credentials: "include"` on all API calls.
**Warning signs:** All API calls fail with 401 despite being logged in.

## Code Examples

### Status Transition Map (Client-Side Mirror)
```typescript
// types/api.ts
export const APPLICATION_STATUSES = [
  "INTERESTED", "APPLIED", "PHONE_SCREEN", "INTERVIEW",
  "OFFER", "REJECTED", "ACCEPTED", "WITHDRAWN",
] as const

export type ApplicationStatus = typeof APPLICATION_STATUSES[number]

const ACTIVE_STATUSES: ApplicationStatus[] = [
  "INTERESTED", "APPLIED", "PHONE_SCREEN", "INTERVIEW", "OFFER",
]

export const STATUS_TRANSITIONS: Record<ApplicationStatus, ApplicationStatus[]> = {
  INTERESTED: ["APPLIED", "WITHDRAWN"],
  APPLIED: ["PHONE_SCREEN", "INTERVIEW", "OFFER", "REJECTED", "WITHDRAWN"],
  PHONE_SCREEN: ["INTERVIEW", "OFFER", "REJECTED", "WITHDRAWN"],
  INTERVIEW: ["PHONE_SCREEN", "OFFER", "REJECTED", "WITHDRAWN"],
  OFFER: ["ACCEPTED", "REJECTED", "WITHDRAWN"],
  REJECTED: ACTIVE_STATUSES,
  ACCEPTED: ACTIVE_STATUSES,
  WITHDRAWN: ACTIVE_STATUSES,
}

export function isValidTransition(from: ApplicationStatus, to: ApplicationStatus): boolean {
  return STATUS_TRANSITIONS[from]?.includes(to) ?? false
}
```

### Spring Page Response Type
```typescript
// types/api.ts
export interface PaginatedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number  // current page (0-indexed)
  size: number
  first: boolean
  last: boolean
  empty: boolean
}
```

### File Upload with react-dropzone
```typescript
// components/documents/document-upload.tsx
"use client"
import { useDropzone } from "react-dropzone"
import { useUploadDocument } from "@/hooks/use-documents"

const ACCEPTED_TYPES = {
  "application/pdf": [".pdf"],
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document": [".docx"],
}

export function DocumentUpload() {
  const upload = useUploadDocument()
  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    accept: ACCEPTED_TYPES,
    onDrop: (files) => {
      files.forEach((file) => {
        const formData = new FormData()
        formData.append("file", file)
        formData.append("title", file.name.replace(/\.[^.]+$/, ""))
        formData.append("category", "OTHER")
        upload.mutate(formData)
      })
    },
  })

  return (
    <div
      {...getRootProps()}
      className={cn(
        "border-2 border-dashed rounded-lg p-8 text-center cursor-pointer transition-colors",
        isDragActive ? "border-primary bg-primary/5" : "border-muted-foreground/25"
      )}
    >
      <input {...getInputProps()} />
      <p className="text-sm text-muted-foreground">
        {isDragActive ? "Drop files here" : "Drag & drop files here, or click to browse"}
      </p>
      <p className="text-xs text-muted-foreground mt-1">PDF and DOCX files only</p>
    </div>
  )
}
```

### Upload Mutation (FormData without JSON Content-Type)
```typescript
// hooks/use-documents.ts
export function useUploadDocument() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (formData: FormData) =>
      fetch(`${API_BASE}/documents`, {
        method: "POST",
        body: formData,
        credentials: "include",
        // Do NOT set Content-Type -- browser sets multipart boundary
      }).then(async (res) => {
        if (!res.ok) throw new ApiError(res.status, await res.json())
        return res.json()
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: documentKeys.all })
      toast.success("Document uploaded")
    },
    onError: () => toast.error("Upload failed"),
  })
}
```

### View Preference Hook
```typescript
// hooks/use-view-preference.ts
import { useState, useCallback } from "react"

type ViewPreference = "board" | "list"

export function useViewPreference(key: string, defaultView: ViewPreference = "board") {
  const [view, setViewState] = useState<ViewPreference>(() => {
    if (typeof window === "undefined") return defaultView
    return (localStorage.getItem(key) as ViewPreference) ?? defaultView
  })

  const setView = useCallback((v: ViewPreference) => {
    setViewState(v)
    localStorage.setItem(key, v)
  }, [key])

  return [view, setView] as const
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| react-beautiful-dnd | @dnd-kit/core + @dnd-kit/sortable | 2023 (rbd deprecated) | rbd no longer maintained; dnd-kit is the standard |
| @dnd-kit/react (new API) | Still beta (v0.3.2) | 2025 ongoing | NOT production-ready; use @dnd-kit/core for now |
| Manual table components | TanStack Table v8 (headless) | 2022 | Headless approach with shadcn Table renders -- industry standard |
| axios for HTTP | Native fetch wrapper | 2024+ | No dependency needed; project already uses thin fetch wrapper |
| Redux for state | TanStack Query for server state | 2023+ | Server state belongs in query cache, not Redux; already set up |

**Deprecated/outdated:**
- `react-beautiful-dnd`: Deprecated, no longer maintained. Do NOT use.
- `@dnd-kit/react`: Still in beta (v0.3.2 as of March 2026). Do NOT use for production.
- `axios`: Unnecessary when native fetch + thin wrapper works (project already has `apiClient`).

## Open Questions

1. **Backend auth integration with Better Auth sessions**
   - What we know: Frontend uses Better Auth session cookies. Backend currently validates JWT tokens. Phase 7 CONTEXT.md explicitly flags this as "a Phase 8 concern."
   - What's unclear: The exact approach for updating SecurityConfig to validate Better Auth sessions. Options include: (a) backend reads Better Auth session cookie and queries the `session` table directly, (b) Better Auth client-side provides a JWT that backend validates, (c) backend becomes a Better Auth resource server.
   - Recommendation: This needs to be planned as the first task in Phase 8 -- all other frontend pages depend on successful API calls. Research the Better Auth documentation for backend session verification patterns.

2. **Kanban card ordering within columns**
   - What we know: The backend API does not have a sort order field on applications. Cards within a column are ordered by the default query sort (createdAt desc).
   - What's unclear: Should drag-and-drop within the same column reorder cards? If so, there is no backend field to persist this.
   - Recommendation: Do NOT support within-column reordering for v1. Only support cross-column drag (status change). Within each column, sort by lastActivityDate desc or nextActionDate.

3. **Dashboard metrics data**
   - What we know: The dashboard needs summary cards (total applications, by-status breakdown, upcoming interviews, recent activity).
   - What's unclear: Whether the backend provides summary/aggregate endpoints or if the frontend must compute from paginated list data.
   - Recommendation: For v1, fetch applications with a large page size (or all) and compute counts client-side. If performance becomes an issue, add a backend summary endpoint later.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | Vitest 4.1 + React Testing Library 16.3 |
| Config file | frontend/vitest.config.ts (if exists) or vite.config.ts |
| Quick run command | `cd frontend && pnpm test` |
| Full suite command | `cd frontend && pnpm test:ci` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| APPL-03 | Kanban board renders 8 status columns with correct application cards | unit | `cd frontend && pnpm vitest run --reporter=verbose components/applications/application-board.test.tsx` | No -- Wave 0 |
| APPL-03 | Drag-and-drop changes status via API call | integration | Manual -- requires DnD event simulation | No -- Wave 0 |
| APPL-04 | Data table renders with sorting and filtering | unit | `cd frontend && pnpm vitest run --reporter=verbose components/applications/application-list.test.tsx` | No -- Wave 0 |
| APPL-04 | Filter bar updates query params and refetches | unit | `cd frontend && pnpm vitest run --reporter=verbose components/applications/application-list.test.tsx` | No -- Wave 0 |

### Sampling Rate
- **Per task commit:** `cd frontend && pnpm test`
- **Per wave merge:** `cd frontend && pnpm test:ci`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `frontend/components/applications/__tests__/application-board.test.tsx` -- covers APPL-03
- [ ] `frontend/components/applications/__tests__/application-list.test.tsx` -- covers APPL-04
- [ ] `frontend/hooks/__tests__/use-applications.test.ts` -- covers query/mutation hooks
- [ ] Vitest config may need MSW (Mock Service Worker) for API mocking in component tests

## Sources

### Primary (HIGH confidence)
- Backend controller source code -- ApplicationController.kt, CompanyController.kt, JobController.kt, InterviewController.kt, DocumentController.kt -- direct inspection of API endpoints, query params, request/response shapes
- Backend DTO source code -- ApplicationDtos.kt, CompanyDtos.kt, JobDtos.kt, InterviewDtos.kt, DocumentDtos.kt -- exact field names and types
- Backend ApplicationService.kt -- status transition map (lines 37-57)
- Frontend package.json -- installed dependencies and versions
- Frontend source code -- existing components, providers, api-client, query-client configuration
- shadcn/ui official docs (https://ui.shadcn.com/docs/components/radix/data-table) -- TanStack Table integration pattern

### Secondary (MEDIUM confidence)
- Dice UI kanban component docs (https://www.diceui.com/docs/components/radix/kanban) -- composable kanban primitives with dnd-kit
- npm @dnd-kit/react registry -- v0.3.2 still beta, confirms @dnd-kit/core is the stable API
- Marmelab blog (Jan 2026) -- kanban with optimistic updates pattern using @hello-pangea/dnd (alternative approach)
- TanStack Query official docs -- optimistic update pattern with onMutate/onError/onSettled
- react-dropzone docs (Feb 2026 update) -- actively maintained, useDropzone hook API

### Tertiary (LOW confidence)
- shadcn Calendar/date picker compatibility with react-day-picker v8 vs v9 -- versions may have shifted; verify on install

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - based on direct inspection of existing codebase, npm versions, and official docs
- Architecture: HIGH - patterns derived from existing project structure (Phase 7) and official shadcn/ui/TanStack docs
- Pitfalls: HIGH - derived from direct code inspection (apiClient Content-Type, auth mismatch) and well-documented TanStack Query patterns
- Kanban library choice: MEDIUM - @dnd-kit/core is clearly the right choice; Dice UI kanban component is newer and less battle-tested but built on proven primitives

**Research date:** 2026-03-21
**Valid until:** 2026-04-21 (30 days -- stable ecosystem, no major breaking changes expected)
