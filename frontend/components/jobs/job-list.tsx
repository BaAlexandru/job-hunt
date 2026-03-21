"use client"

import { MoreHorizontal, Pencil, Trash2 } from "lucide-react"
import { DataTable, type ColumnDef } from "@/components/shared/data-table"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import type { JobResponse } from "@/types/api"

interface JobListProps {
  jobs: JobResponse[]
  isLoading: boolean
  onEdit: (job: JobResponse) => void
  onDelete: (job: JobResponse) => void
  pagination: {
    page: number
    totalPages: number
    onPageChange: (page: number) => void
  }
}

function formatSalary(job: JobResponse): string {
  if (job.salaryText) return job.salaryText
  if (job.salaryMin == null && job.salaryMax == null) return "--"

  const currency = job.currency ?? ""
  const period = job.salaryPeriod
    ? `/${job.salaryPeriod.toLowerCase()}`
    : ""

  const fmt = (n: number) => {
    if (n >= 1000) return `${Math.round(n / 1000)}k`
    return String(n)
  }

  if (job.salaryMin != null && job.salaryMax != null) {
    return `${currency}${fmt(job.salaryMin)}-${currency}${fmt(job.salaryMax)}${period}`
  }
  if (job.salaryMin != null) {
    return `${currency}${fmt(job.salaryMin)}+${period}`
  }
  return `${currency}${fmt(job.salaryMax!)}${period}`
}

function formatEnum(value: string | null): string {
  if (!value) return "--"
  return value
    .replace(/_/g, " ")
    .replace(/\b\w/g, (c) => c.toUpperCase())
}

export function JobList({
  jobs,
  isLoading,
  onEdit,
  onDelete,
  pagination,
}: JobListProps) {
  const columns: ColumnDef<JobResponse, unknown>[] = [
    {
      accessorKey: "title",
      header: "Title",
    },
    {
      accessorKey: "companyName",
      header: "Company",
      cell: ({ row }) => row.original.companyName || "--",
    },
    {
      accessorKey: "location",
      header: "Location",
      cell: ({ row }) => row.original.location || "--",
    },
    {
      accessorKey: "jobType",
      header: "Type",
      cell: ({ row }) => formatEnum(row.original.jobType),
    },
    {
      accessorKey: "workMode",
      header: "Work Mode",
      cell: ({ row }) => formatEnum(row.original.workMode),
    },
    {
      id: "salary",
      header: "Salary",
      cell: ({ row }) => formatSalary(row.original),
    },
    {
      id: "actions",
      header: "",
      enableSorting: false,
      cell: ({ row }) => (
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button
              variant="ghost"
              size="icon-sm"
              onClick={(e) => e.stopPropagation()}
            >
              <MoreHorizontal className="size-4" />
              <span className="sr-only">Actions</span>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem
              onClick={(e) => {
                e.stopPropagation()
                onEdit(row.original)
              }}
            >
              <Pencil />
              Edit
            </DropdownMenuItem>
            <DropdownMenuItem
              variant="destructive"
              onClick={(e) => {
                e.stopPropagation()
                onDelete(row.original)
              }}
            >
              <Trash2 />
              Delete
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      ),
    },
  ]

  if (isLoading) {
    return (
      <div className="flex flex-col gap-2">
        {Array.from({ length: 5 }).map((_, i) => (
          <Skeleton key={i} className="h-12 w-full rounded-lg" />
        ))}
      </div>
    )
  }

  return (
    <DataTable
      columns={columns}
      data={jobs}
      pagination={{
        pageIndex: pagination.page,
        pageSize: 20,
        pageCount: pagination.totalPages,
        onPageChange: pagination.onPageChange,
      }}
    />
  )
}
