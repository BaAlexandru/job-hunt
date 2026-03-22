"use client"

import { useCallback, useState } from "react"
import { useDropzone } from "react-dropzone"
import { toast } from "sonner"
import { ChevronRight, Download, Loader2, Trash2, Upload } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { ConfirmDialog } from "@/components/shared/confirm-dialog"
import {
  useDocumentVersions,
  useCreateDocumentVersion,
  useSetCurrentVersion,
  useDeleteDocumentVersion,
  useDownloadVersionUrl,
} from "@/hooks/use-documents"
import type { DocumentResponse, DocumentVersionResponse } from "@/types/api"
import { cn } from "@/lib/utils"

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
// Version download link (extracted so hooks rules are satisfied)
// ---------------------------------------------------------------------------

function VersionDownloadButton({
  documentId,
  versionId,
}: {
  documentId: string
  versionId: string
}) {
  const downloadUrl = useDownloadVersionUrl(documentId, versionId)
  return (
    <Button variant="ghost" size="icon-xs" asChild>
      <a href={downloadUrl} download onClick={(e) => e.stopPropagation()}>
        <Download className="size-3.5" />
      </a>
    </Button>
  )
}

// ---------------------------------------------------------------------------
// Version Panel (expanded content for a document row)
// ---------------------------------------------------------------------------

function VersionPanel({ documentId }: { documentId: string }) {
  const { data: versions, isLoading } = useDocumentVersions(documentId)
  const createVersion = useCreateDocumentVersion()
  const setCurrentVersion = useSetCurrentVersion()
  const deleteVersion = useDeleteDocumentVersion()

  const [deleteTarget, setDeleteTarget] =
    useState<DocumentVersionResponse | null>(null)
  const [versionNote, setVersionNote] = useState("")

  const onDrop = useCallback(
    (acceptedFiles: File[]) => {
      const file = acceptedFiles[0]
      if (!file) return

      const formData = new FormData()
      formData.append("file", file)
      if (versionNote.trim()) {
        formData.append("note", versionNote.trim())
      }

      createVersion.mutate(
        { documentId, formData },
        {
          onSuccess: () => {
            toast.success("New version uploaded")
            setVersionNote("")
          },
          onError: () => {
            toast.error("Failed to upload version. Please try again.")
          },
        },
      )
    },
    [createVersion, documentId, versionNote],
  )

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    multiple: false,
  })

  if (isLoading) {
    return (
      <div className="border-l-2 border-muted ml-4 pl-4 pb-4 space-y-2">
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="h-8 w-full" />
        ))}
      </div>
    )
  }

  if (!versions || versions.length === 0) {
    return (
      <div className="border-l-2 border-muted ml-4 pl-4 pb-4">
        <p className="text-sm text-muted-foreground">No versions found.</p>
      </div>
    )
  }

  return (
    <div className="border-l-2 border-muted ml-4 pl-4 pb-4">
      {/* Version list */}
      <div className="space-y-2">
        {versions.map((version) => (
          <div
            key={version.id}
            className="flex items-center gap-3 py-1.5 flex-wrap"
          >
            <span className="text-sm font-medium whitespace-nowrap">
              v{version.versionNumber}
            </span>
            <span className="text-sm truncate max-w-[200px]">
              {version.originalFilename}
            </span>
            <span className="text-xs text-muted-foreground whitespace-nowrap">
              {formatFileSize(version.fileSize)}
            </span>
            {version.note && (
              <span className="text-xs text-muted-foreground italic truncate max-w-[200px]">
                {version.note}
              </span>
            )}
            <span className="text-xs text-muted-foreground whitespace-nowrap">
              {formatDate(version.createdAt)}
            </span>

            {version.isCurrent ? (
              <Badge variant="default">Current</Badge>
            ) : (
              <Button
                variant="outline"
                size="sm"
                disabled={setCurrentVersion.isPending}
                onClick={(e) => {
                  e.stopPropagation()
                  setCurrentVersion.mutate(
                    { documentId, versionId: version.id },
                    {
                      onSuccess: () => {
                        toast.success(
                          `Version ${version.versionNumber} set as current`,
                        )
                      },
                    },
                  )
                }}
              >
                {setCurrentVersion.isPending ? (
                  <Loader2 className="size-3.5 animate-spin" />
                ) : (
                  "Set as Current"
                )}
              </Button>
            )}

            <VersionDownloadButton
              documentId={documentId}
              versionId={version.id}
            />

            <Button
              variant="ghost"
              size="icon-xs"
              disabled={versions.length <= 1}
              onClick={(e) => {
                e.stopPropagation()
                setDeleteTarget(version)
              }}
            >
              <Trash2 className="size-3.5 text-destructive" />
            </Button>
          </div>
        ))}
      </div>

      {/* Upload new version */}
      <div className="mt-4 space-y-2">
        <Input
          placeholder="Version note (optional)"
          value={versionNote}
          onChange={(e) => setVersionNote(e.target.value)}
          className="max-w-sm"
        />
        <div
          {...getRootProps()}
          className={cn(
            "border-2 border-dashed rounded-md p-4 text-center cursor-pointer transition-colors",
            isDragActive
              ? "border-primary bg-primary/5"
              : "border-muted-foreground/25 hover:border-muted-foreground/50",
          )}
        >
          <input {...getInputProps()} />
          {createVersion.isPending ? (
            <div className="flex items-center justify-center gap-2 text-muted-foreground">
              <Loader2 className="size-4 animate-spin" />
              <span className="text-sm">Uploading...</span>
            </div>
          ) : isDragActive ? (
            <div className="flex flex-col items-center gap-1">
              <Upload className="size-5 text-primary" />
              <p className="text-sm font-medium text-primary">
                Drop file here
              </p>
            </div>
          ) : (
            <div className="flex flex-col items-center gap-1">
              <Upload className="size-5 text-muted-foreground" />
              <p className="text-sm font-medium">Upload New Version</p>
            </div>
          )}
        </div>
      </div>

      {/* Delete version confirmation */}
      <ConfirmDialog
        open={deleteTarget !== null}
        onOpenChange={(open) => {
          if (!open) setDeleteTarget(null)
        }}
        title="Delete Version"
        description={
          deleteTarget
            ? `Version ${deleteTarget.versionNumber} will be permanently deleted. This action cannot be undone.`
            : ""
        }
        actionLabel="Delete"
        variant="destructive"
        onConfirm={() => {
          if (!deleteTarget) return
          deleteVersion.mutate(
            { documentId, versionId: deleteTarget.id },
            {
              onSuccess: () => {
                toast.success("Version deleted")
              },
            },
          )
          setDeleteTarget(null)
        }}
      />
    </div>
  )
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
  const downloadUrl = doc.currentVersion
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
  const [expandedDocId, setExpandedDocId] = useState<string | null>(null)

  const toggleExpand = (id: string) =>
    setExpandedDocId((prev) => (prev === id ? null : id))

  if (isLoading) {
    return (
      <div className="space-y-3">
        {Array.from({ length: 5 }).map((_, i) => (
          <Skeleton key={i} className="h-10 w-full" />
        ))}
      </div>
    )
  }

  if (documents.length === 0) {
    return (
      <p className="text-sm text-muted-foreground py-8 text-center">
        No documents yet.
      </p>
    )
  }

  return (
    <div className="space-y-0">
      {/* Header row */}
      <div className="grid grid-cols-[24px_1fr_120px_100px_80px_100px_80px] gap-2 px-3 py-2 text-xs font-medium text-muted-foreground border-b">
        <span />
        <span>Title</span>
        <span>File Name</span>
        <span>Category</span>
        <span>Size</span>
        <span>Uploaded</span>
        <span />
      </div>

      {/* Document rows */}
      {documents.map((doc) => {
        const isExpanded = expandedDocId === doc.id
        return (
          <div key={doc.id} className="border-b last:border-b-0">
            <div
              className="grid grid-cols-[24px_1fr_120px_100px_80px_100px_80px] gap-2 px-3 py-2 items-center cursor-pointer hover:bg-muted/50 transition-colors"
              onClick={() => toggleExpand(doc.id)}
            >
              <ChevronRight
                className="size-4 transition-transform duration-200"
                style={{
                  transform: isExpanded ? "rotate(90deg)" : undefined,
                }}
              />
              <span className="text-sm font-medium truncate">
                {doc.title}
              </span>
              <span className="text-xs text-muted-foreground truncate">
                {doc.currentVersion?.originalFilename ?? "--"}
              </span>
              <span className="text-sm">
                {CATEGORY_LABELS[doc.category] ?? doc.category}
              </span>
              <span className="text-xs text-muted-foreground">
                {doc.currentVersion
                  ? formatFileSize(doc.currentVersion.fileSize)
                  : "--"}
              </span>
              <span className="text-xs text-muted-foreground">
                {formatDate(doc.createdAt)}
              </span>
              <div onClick={(e) => e.stopPropagation()}>
                <ActionCell doc={doc} onDelete={onDelete} />
              </div>
            </div>

            {isExpanded && <VersionPanel documentId={doc.id} />}
          </div>
        )
      })}
    </div>
  )
}
