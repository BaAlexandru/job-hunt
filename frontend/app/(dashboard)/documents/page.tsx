"use client"

import { useState, useMemo } from "react"
import { toast } from "sonner"
import { DocumentUpload } from "@/components/documents/document-upload"
import { DocumentList } from "@/components/documents/document-list"
import { EmptyState } from "@/components/shared/empty-state"
import { ConfirmDialog } from "@/components/shared/confirm-dialog"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { useDocuments, useDeleteDocument } from "@/hooks/use-documents"
import type { DocumentResponse } from "@/types/api"

const CATEGORY_OPTIONS = [
  { value: "ALL", label: "All" },
  { value: "CV", label: "CV" },
  { value: "COVER_LETTER", label: "Cover Letter" },
  { value: "PORTFOLIO", label: "Portfolio" },
  { value: "OTHER", label: "Other" },
] as const

export default function DocumentsPage() {
  const [categoryFilter, setCategoryFilter] = useState("ALL")
  const [deleteTarget, setDeleteTarget] = useState<DocumentResponse | null>(
    null,
  )

  const filters =
    categoryFilter !== "ALL" ? { category: categoryFilter } : {}
  const { data, isLoading, isError } = useDocuments(filters)
  const deleteMutation = useDeleteDocument()

  const documents = useMemo(() => data?.content ?? [], [data])

  function handleDelete() {
    if (!deleteTarget) return
    deleteMutation.mutate(deleteTarget.id, {
      onSuccess: () => {
        toast.success("Document deleted")
        setDeleteTarget(null)
      },
      onError: () => {
        toast.error("Failed to delete document")
      },
    })
  }

  if (isError) {
    return (
      <div className="flex flex-col items-center gap-2 py-12 text-center">
        <p className="text-sm text-destructive">
          Could not load documents. Check your connection and try again.
        </p>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">Documents</h1>
        <Select value={categoryFilter} onValueChange={setCategoryFilter}>
          <SelectTrigger className="w-[160px]">
            <SelectValue placeholder="Category" />
          </SelectTrigger>
          <SelectContent>
            {CATEGORY_OPTIONS.map((opt) => (
              <SelectItem key={opt.value} value={opt.value}>
                {opt.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {/* Upload zone */}
      <DocumentUpload />

      {/* Document list or empty state */}
      {!isLoading && documents.length === 0 ? (
        <EmptyState
          heading="No documents yet"
          body="Upload your CVs, cover letters, and other documents to link them to applications."
        />
      ) : (
        <DocumentList
          documents={documents}
          isLoading={isLoading}
          onDelete={setDeleteTarget}
        />
      )}

      {/* Delete confirmation */}
      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
        title="Delete Document"
        description={`This will permanently delete ${deleteTarget?.title ?? "this document"} and all its versions. This cannot be undone.`}
        actionLabel="Delete"
        onConfirm={handleDelete}
        variant="destructive"
      />
    </div>
  )
}
