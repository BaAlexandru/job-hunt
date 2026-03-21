"use client"

import { useMemo, useState, useCallback } from "react"
import type { DragStartEvent, UniqueIdentifier } from "@dnd-kit/core"
import { toast } from "sonner"
import {
  Kanban,
  KanbanBoard,
  KanbanColumn,
  KanbanItem,
  KanbanOverlay,
} from "@/components/ui/kanban"
import { ScrollArea, ScrollBar } from "@/components/ui/scroll-area"
import { Checkbox } from "@/components/ui/checkbox"
import { Skeleton } from "@/components/ui/skeleton"
import { StatusBadge } from "@/components/applications/status-badge"
import { ApplicationCard } from "@/components/applications/application-card"
import {
  APPLICATION_STATUSES,
  STATUS_TRANSITIONS,
  STATUS_LABELS,
  isValidTransition,
} from "@/types/api"
import type { ApplicationResponse, ApplicationStatus } from "@/types/api"
import { useUpdateApplicationStatus } from "@/hooks/use-applications"
import { cn } from "@/lib/utils"

interface ApplicationBoardProps {
  applications: ApplicationResponse[]
  onSelectApplication: (id: string) => void
  isLoading: boolean
  includeArchived: boolean
  onIncludeArchivedChange: (value: boolean) => void
}

export function ApplicationBoard({
  applications,
  onSelectApplication,
  isLoading,
  includeArchived,
  onIncludeArchivedChange,
}: ApplicationBoardProps) {
  const updateStatus = useUpdateApplicationStatus()
  const [draggingStatus, setDraggingStatus] = useState<ApplicationStatus | null>(
    null,
  )

  // Group applications by status, ensuring all 8 columns always exist
  const columns = useMemo(() => {
    const grouped: Record<string, ApplicationResponse[]> = {}
    for (const status of APPLICATION_STATUSES) {
      grouped[status] = []
    }
    for (const app of applications) {
      if (grouped[app.status]) {
        grouped[app.status].push(app)
      }
    }
    return grouped
  }, [applications])

  const handleDragStart = useCallback(
    (event: DragStartEvent) => {
      const activeId = event.active.id as string
      const app = applications.find((a) => a.id === activeId)
      if (app) {
        setDraggingStatus(app.status)
      }
    },
    [applications],
  )

  const validTargets = useMemo(() => {
    if (!draggingStatus) return new Set<string>()
    const targets = STATUS_TRANSITIONS[draggingStatus] ?? []
    // Include the current column as a valid target (reorder within)
    return new Set<string>([draggingStatus, ...targets])
  }, [draggingStatus])

  // We need to handle drag end at the Kanban level to validate transitions
  // The Kanban component internally handles onDragOver for visual reordering,
  // but we intercept onDragEnd for status change mutations
  const handleDragEnd = useCallback(() => {
    setDraggingStatus(null)
  }, [])

  // Track value changes from the Kanban component to detect cross-column moves
  const handleValueChange = useCallback(
    (newColumns: Record<UniqueIdentifier, ApplicationResponse[]>) => {
      // Find the item that moved to a different column
      for (const status of APPLICATION_STATUSES) {
        const newItems = newColumns[status] ?? []
        const oldItems = columns[status] ?? []

        // Find items that are in the new column but weren't in the old one
        const oldIds = new Set(oldItems.map((a) => a.id))
        for (const item of newItems) {
          if (!oldIds.has(item.id) && item.status !== status) {
            // This item moved from item.status to status
            const targetStatus = status as ApplicationStatus
            if (isValidTransition(item.status, targetStatus)) {
              updateStatus.mutate({ id: item.id, status: targetStatus })
            } else {
              toast.error(
                "Cannot move to that status from the current one.",
              )
              // Card snaps back automatically since we don't update state
              return
            }
          }
        }
      }
    },
    [columns, updateStatus],
  )

  if (isLoading) {
    return (
      <div className="flex gap-4 overflow-x-auto p-4">
        {APPLICATION_STATUSES.map((status) => (
          <div
            key={status}
            className="flex w-[280px] shrink-0 flex-col gap-2 rounded-lg bg-secondary p-2.5"
          >
            <Skeleton className="h-6 w-24" />
            <Skeleton className="h-20 w-full" />
            <Skeleton className="h-20 w-full" />
            {status === "APPLIED" && <Skeleton className="h-20 w-full" />}
          </div>
        ))}
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center gap-2 px-4">
        <Checkbox
          id="show-archived"
          checked={includeArchived}
          onCheckedChange={(checked) =>
            onIncludeArchivedChange(checked === true)
          }
        />
        <label
          htmlFor="show-archived"
          className="cursor-pointer text-sm text-muted-foreground"
        >
          Show archived
        </label>
      </div>

      <ScrollArea className="w-full">
        <div className="min-w-max p-4 pt-0">
          <Kanban<ApplicationResponse>
            value={columns}
            onValueChange={handleValueChange}
            getItemValue={(item) => item.id}
            onDragStart={handleDragStart}
            onDragEnd={handleDragEnd}
            flatCursor
          >
            <KanbanBoard>
              {APPLICATION_STATUSES.map((status) => {
                const items = columns[status] ?? []
                const isDimmed =
                  draggingStatus !== null && !validTargets.has(status)

                return (
                  <div
                    key={status}
                    className={cn(
                      "flex w-[280px] shrink-0 flex-col transition-opacity",
                      isDimmed && "opacity-40 pointer-events-none",
                    )}
                  >
                    <KanbanColumn
                      value={status}
                      className="bg-secondary min-h-[120px]"
                    >
                      <div className="flex items-center gap-2 px-1 pb-1">
                        <StatusBadge status={status as ApplicationStatus} />
                        <span className="text-xs text-muted-foreground">
                          ({items.length})
                        </span>
                      </div>
                      {items.map((app) => (
                        <KanbanItem key={app.id} value={app.id} asHandle>
                          <ApplicationCard
                            application={app}
                            onClick={() => onSelectApplication(app.id)}
                          />
                        </KanbanItem>
                      ))}
                    </KanbanColumn>
                  </div>
                )
              })}
            </KanbanBoard>
            <KanbanOverlay>
              {({ value }) => {
                const app = applications.find((a) => a.id === value)
                if (!app) return null
                return (
                  <ApplicationCard application={app} onClick={() => {}} />
                )
              }}
            </KanbanOverlay>
          </Kanban>
        </div>
        <ScrollBar orientation="horizontal" />
      </ScrollArea>
    </div>
  )
}
