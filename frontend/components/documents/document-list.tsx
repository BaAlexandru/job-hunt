"use client"

import { type ColumnDef, DataTable } from "@/components/shared/data-table"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { useDownloadVersionUrl } from "@/hooks/use-documents"
import type { DocumentResponse } from "@/types/api"
import { Download, Trash2 } from "lucide-react"

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

const CATEGORY_LABELS: Record<string, string> = {
  CV: "CV",
  COVER_LETTER: "Cover Letter",
  PORTFOLIO: "Portfolio",
  OTHER: "Other",
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  })
}

// ---------------------------------------------------------------------------
// Action cell (needs hooks, so it must be a component)
// ---------------------------------------------------------------------------

function ActionCell({
  doc,
  onDelete,
}: {
  doc: DocumentResponse
  onDelete: (doc: DocumentResponse) => void
}) {
  const downloadUrl =
    doc.currentVersion
      ? useDownloadVersionUrl(doc.id, doc.currentVersion.id)
      : null

  return (
    <div className="flex items-center gap-1">
      {downloadUrl && (
        <Button variant="ghost" size="icon-xs" asChild>
          <a
            href={downloadUrl}
            download
            onClick={(e) => e.stopPropagation()}
          >
            <Download className="size-3.5" />
          </a>
        </Button>
      )}
      <Button
        variant="ghost"
        size="icon-xs"
        onClick={(e) => {
          e.stopPropagation()
          onDelete(doc)
        }}
      >
        <Trash2 className="size-3.5 text-destructive" />
      </Button>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Column definitions factory
// ---------------------------------------------------------------------------

function getColumns(
  onDelete: (doc: DocumentResponse) => void,
): ColumnDef<DocumentResponse, unknown>[] {
  return [
    {
      accessorKey: "title",
      header: "Title",
      enableSorting: true,
    },
    {
      id: "fileName",
      header: "File Name",
      cell: ({ row }) => {
        const filename =
          row.original.currentVersion?.originalFilename ?? "--"
        return (
          <span className="text-xs text-muted-foreground">{filename}</span>
        )
      },
      enableSorting: false,
    },
    {
      accessorKey: "category",
      header: "Category",
      cell: ({ row }) =>
        CATEGORY_LABELS[row.original.category] ?? row.original.category,
      enableSorting: true,
    },
    {
      id: "size",
      header: "Size",
      cell: ({ row }) =>
        row.original.currentVersion
          ? formatFileSize(row.original.currentVersion.fileSize)
          : "--",
      enableSorting: false,
    },
    {
      id: "version",
      header: "Version",
      cell: ({ row }) =>
        row.original.currentVersion?.versionNumber ?? "--",
      enableSorting: false,
    },
    {
      id: "uploaded",
      header: "Uploaded",
      cell: ({ row }) => formatDate(row.original.createdAt),
      enableSorting: true,
    },
    {
      id: "actions",
      header: "",
      cell: ({ row }) => (
        <ActionCell doc={row.original} onDelete={onDelete} />
      ),
      enableSorting: false,
    },
  ]
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

interface DocumentListProps {
  documents: DocumentResponse[]
  isLoading: boolean
  onDelete: (doc: DocumentResponse) => void
}

export function DocumentList({
  documents,
  isLoading,
  onDelete,
}: DocumentListProps) {
  const columns = getColumns(onDelete)

  if (isLoading) {
    return (
      <div className="space-y-3">
        {Array.from({ length: 5 }).map((_, i) => (
          <Skeleton key={i} className="h-10 w-full" />
        ))}
      </div>
    )
  }

  return <DataTable columns={columns} data={documents} />
}
