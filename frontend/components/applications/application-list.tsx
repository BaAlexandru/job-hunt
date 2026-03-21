"use client"

import { useMemo } from "react"
import { format, formatDistanceToNow } from "date-fns"
import { toast } from "sonner"
import { type ColumnDef, DataTable } from "@/components/shared/data-table"
import { StatusBadge } from "@/components/applications/status-badge"
import { FilterBar, type FilterState } from "@/components/applications/filter-bar"
import { EmptyState } from "@/components/shared/empty-state"
import { Skeleton } from "@/components/ui/skeleton"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { STATUS_TRANSITIONS, STATUS_LABELS } from "@/types/api"
import type { ApplicationResponse, ApplicationStatus } from "@/types/api"
import { useUpdateApplicationStatus } from "@/hooks/use-applications"

interface ApplicationListProps {
  applications: ApplicationResponse[]
  onSelectApplication: (id: string) => void
  isLoading: boolean
  pagination: {
    page: number
    totalPages: number
    onPageChange: (page: number) => void
  }
  filters: FilterState
  onFiltersChange: (filters: FilterState) => void
}

function StatusCell({ row }: { row: ApplicationResponse }) {
  const updateStatus = useUpdateApplicationStatus()
  const transitions = STATUS_TRANSITIONS[row.status] ?? []

  return (
    <DropdownMenu modal={false}>
      <DropdownMenuTrigger
        onClick={(e) => e.stopPropagation()}
        className="cursor-pointer"
      >
        <StatusBadge status={row.status} />
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start" className="min-w-[180px]" sideOffset={4} collisionPadding={8}>
        {transitions.map((target) => (
          <DropdownMenuItem
            key={target}
            onClick={(e) => {
              e.stopPropagation()
              updateStatus.mutate(
                { id: row.id, status: target },
                {
                  onSuccess: () => {
                    toast.success(`Status updated to ${STATUS_LABELS[target]}.`)
                  },
                  onError: () => {
                    toast.error("Failed to update status.")
                  },
                },
              )
            }}
          >
            <StatusBadge status={target} />
            <span className="ml-1">{STATUS_LABELS[target]}</span>
          </DropdownMenuItem>
        ))}
        {transitions.length === 0 && (
          <DropdownMenuItem disabled>No transitions available</DropdownMenuItem>
        )}
      </DropdownMenuContent>
    </DropdownMenu>
  )
}

export function ApplicationList({
  applications,
  onSelectApplication,
  isLoading,
  pagination,
  filters,
  onFiltersChange,
}: ApplicationListProps) {
  const columns = useMemo<ColumnDef<ApplicationResponse, unknown>[]>(
    () => [
      {
        accessorKey: "companyName",
        header: "Company",
        cell: ({ row }) => (
          <span className="text-sm">
            {row.original.companyName ?? "--"}
          </span>
        ),
      },
      {
        accessorKey: "jobTitle",
        header: "Job Title",
        cell: ({ row }) => (
          <span className="text-sm font-medium">{row.original.jobTitle}</span>
        ),
      },
      {
        accessorKey: "status",
        header: "Status",
        enableSorting: false,
        cell: ({ row }) => <StatusCell row={row.original} />,
      },
      {
        accessorKey: "appliedDate",
        header: "Applied",
        cell: ({ row }) => (
          <span className="text-sm text-muted-foreground">
            {row.original.appliedDate
              ? format(new Date(row.original.appliedDate), "MMM d, yyyy")
              : "--"}
          </span>
        ),
      },
      {
        accessorKey: "nextActionDate",
        header: "Next Action",
        cell: ({ row }) => (
          <span className="text-sm text-muted-foreground">
            {row.original.nextActionDate
              ? format(new Date(row.original.nextActionDate), "MMM d, yyyy")
              : "--"}
          </span>
        ),
      },
      {
        accessorKey: "lastActivityDate",
        header: "Last Activity",
        cell: ({ row }) => (
          <span className="text-sm text-muted-foreground">
            {formatDistanceToNow(new Date(row.original.lastActivityDate), {
              addSuffix: true,
            })}
          </span>
        ),
      },
    ],
    [],
  )

  if (isLoading) {
    return (
      <div className="flex flex-col gap-2">
        {Array.from({ length: 6 }).map((_, i) => (
          <Skeleton key={i} className="h-12 w-full" />
        ))}
      </div>
    )
  }

  const hasFilters =
    !!filters.status?.length ||
    !!filters.companyId ||
    !!filters.search ||
    !!filters.fromDate ||
    !!filters.toDate

  return (
    <div className="flex flex-col">
      <FilterBar filters={filters} onFiltersChange={onFiltersChange} />
      {applications.length === 0 && hasFilters ? (
        <div className="py-8">
          <EmptyState
            heading="No applications match your filters"
            body="Try adjusting your filters or create a new application."
          />
        </div>
      ) : (
        <div>
          <DataTable
            columns={columns}
            data={applications}
            onRowClick={(row) => onSelectApplication(row.id)}
            pagination={{
              pageIndex: pagination.page,
              pageSize: 20,
              pageCount: pagination.totalPages,
              onPageChange: pagination.onPageChange,
            }}
          />
        </div>
      )}
    </div>
  )
}
