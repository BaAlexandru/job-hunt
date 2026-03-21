"use client"

import { Suspense, useState, useEffect } from "react"
import { useSearchParams, useRouter } from "next/navigation"
import { PlusIcon, LayoutGridIcon, ListIcon } from "lucide-react"
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { EmptyState } from "@/components/shared/empty-state"
import { ApplicationBoard } from "@/components/applications/application-board"
import { ApplicationList } from "@/components/applications/application-list"
import { ApplicationDetail } from "@/components/applications/application-detail"
import { ApplicationForm } from "@/components/applications/application-form"
import { useViewPreference } from "@/hooks/use-view-preference"
import { useApplications } from "@/hooks/use-applications"
import type { FilterState } from "@/components/applications/filter-bar"

function ApplicationsContent() {
  const [view, setView] = useViewPreference("jobhunt:applications-view", "board")
  const [selectedApplicationId, setSelectedApplicationId] = useState<
    string | null
  >(null)
  const [formOpen, setFormOpen] = useState(false)
  const [filters, setFilters] = useState<FilterState>({})
  const [page, setPage] = useState(0)
  const [includeArchived, setIncludeArchived] = useState(false)
  const searchParams = useSearchParams()
  const router = useRouter()

  useEffect(() => {
    const appId = searchParams.get("applicationId")
    if (appId) {
      setSelectedApplicationId(appId)
    }
  }, [searchParams])

  const { data, isLoading, isError } = useApplications({
    status: filters.status?.join(","),
    companyId: filters.companyId,
    q: filters.search,
    dateFrom: filters.fromDate,
    dateTo: filters.toDate,
    page,
    size: view === "list" ? 20 : 200,
    includeArchived,
  })

  const applications = data?.content ?? []

  return (
    <div className="flex flex-col gap-4">
      {/* Page header */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-xl font-semibold">Applications</h1>
        <div className="flex items-center gap-3">
          <Tabs
            value={view}
            onValueChange={(v) => setView(v as "board" | "list")}
          >
            <TabsList>
              <TabsTrigger value="board">
                <LayoutGridIcon className="size-3.5" />
                Board
              </TabsTrigger>
              <TabsTrigger value="list">
                <ListIcon className="size-3.5" />
                List
              </TabsTrigger>
            </TabsList>
          </Tabs>
          <Button onClick={() => setFormOpen(true)}>
            <PlusIcon className="size-4" />
            New Application
          </Button>
        </div>
      </div>

      {/* Content */}
      {isError ? (
        <div className="py-12 text-center text-sm text-muted-foreground">
          Could not load applications. Check your connection and try again.
        </div>
      ) : isLoading ? (
        view === "board" ? (
          <div className="flex gap-4 overflow-x-auto">
            {Array.from({ length: 8 }).map((_, i) => (
              <div
                key={i}
                className="flex w-[240px] shrink-0 flex-col gap-2 rounded-lg bg-secondary p-2.5 sm:w-[280px]"
              >
                <Skeleton className="h-6 w-24" />
                <Skeleton className="h-20 w-full" />
                <Skeleton className="h-20 w-full" />
              </div>
            ))}
          </div>
        ) : (
          <div className="flex flex-col gap-2">
            {Array.from({ length: 6 }).map((_, i) => (
              <Skeleton key={i} className="h-12 w-full" />
            ))}
          </div>
        )
      ) : applications.length === 0 && !filters.status && !filters.companyId && !filters.search && !filters.fromDate && !filters.toDate ? (
        <div className="py-8">
          <EmptyState
            heading="No applications yet"
            body="Start tracking your job search by creating your first application."
          />
        </div>
      ) : view === "board" ? (
        <ApplicationBoard
          applications={applications}
          onSelectApplication={setSelectedApplicationId}
          isLoading={false}
          includeArchived={includeArchived}
          onIncludeArchivedChange={setIncludeArchived}
        />
      ) : (
        <ApplicationList
          applications={applications}
          onSelectApplication={setSelectedApplicationId}
          isLoading={false}
          pagination={{
            page: data?.number ?? 0,
            totalPages: data?.totalPages ?? 1,
            onPageChange: setPage,
          }}
          filters={filters}
          onFiltersChange={setFilters}
        />
      )}

      {/* Detail panel */}
      <ApplicationDetail
        applicationId={selectedApplicationId}
        open={selectedApplicationId !== null}
        onOpenChange={(open) => {
          if (!open) {
            setSelectedApplicationId(null)
            if (searchParams.has("applicationId")) {
              router.replace("/applications", { scroll: false })
            }
          }
        }}
      />

      {/* Create/edit dialog */}
      <ApplicationForm open={formOpen} onOpenChange={setFormOpen} />
    </div>
  )
}

export default function ApplicationsPage() {
  return (
    <Suspense fallback={
      <div className="flex flex-col gap-4">
        <Skeleton className="h-8 w-48" />
        <div className="flex gap-4 overflow-x-auto">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-40 w-[280px] shrink-0" />
          ))}
        </div>
      </div>
    }>
      <ApplicationsContent />
    </Suspense>
  )
}
