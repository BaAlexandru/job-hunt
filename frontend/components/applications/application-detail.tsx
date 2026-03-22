"use client"

import { useState } from "react"
import { format, formatDistanceToNow } from "date-fns"
import { toast } from "sonner"
import {
  PencilIcon,
  TrashIcon,
  PlusIcon,
  DownloadIcon,
  LinkIcon,
  UnlinkIcon,
  MapPinIcon,
  ChevronRight,
  Trash2,
} from "lucide-react"
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
} from "@/components/ui/sheet"
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { StatusBadge } from "@/components/applications/status-badge"
import { ConfirmDialog } from "@/components/shared/confirm-dialog"
import { STATUS_TRANSITIONS, STATUS_LABELS } from "@/types/api"
import type {
  ApplicationStatus,
  InterviewResponse,
  InterviewNoteResponse,
} from "@/types/api"
import {
  useApplication,
  useUpdateApplication,
  useUpdateApplicationStatus,
  useApplicationNotes,
  useCreateApplicationNote,
  useUpdateApplicationNote,
  useDeleteApplicationNote,
} from "@/hooks/use-applications"
import {
  useInterviews,
  useCreateInterview,
  useUpdateInterview,
  useDeleteInterview,
  useInterviewNotes,
  useCreateInterviewNote,
  useUpdateInterviewNote,
  useDeleteInterviewNote,
} from "@/hooks/use-interviews"
import {
  useDocumentLinksForApplication,
  useDocuments,
  useLinkDocumentToApplication,
  useUnlinkDocument,
  useDownloadVersionUrl,
} from "@/hooks/use-documents"
import { TimelineTab } from "@/components/applications/timeline-tab"

interface ApplicationDetailProps {
  applicationId: string | null
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function ApplicationDetail({
  applicationId,
  open,
  onOpenChange,
}: ApplicationDetailProps) {
  const { data: application, isLoading } = useApplication(applicationId ?? "")

  if (!applicationId) return null

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="flex w-full flex-col sm:max-w-lg">
        {isLoading || !application ? (
          <div className="flex flex-col gap-4 p-4">
            <Skeleton className="h-6 w-48" />
            <Skeleton className="h-4 w-32" />
            <Skeleton className="h-8 w-20" />
            <Skeleton className="h-40 w-full" />
          </div>
        ) : (
          <>
            <SheetHeader>
              <SheetTitle>{application.jobTitle}</SheetTitle>
              <SheetDescription>
                {application.companyName ?? "No company"}
              </SheetDescription>
              <div className="pt-1">
                <StatusBadge status={application.status} />
              </div>
            </SheetHeader>

            <div className="flex-1 min-h-0 overflow-y-auto">
              <Tabs defaultValue="overview" className="px-4 pb-4">
                <TabsList>
                  <TabsTrigger value="overview">Overview</TabsTrigger>
                  <TabsTrigger value="notes">Notes</TabsTrigger>
                  <TabsTrigger value="interviews">Interviews</TabsTrigger>
                  <TabsTrigger value="documents">Documents</TabsTrigger>
                  <TabsTrigger value="timeline">Timeline</TabsTrigger>
                </TabsList>

                <TabsContent value="overview">
                  <OverviewTab applicationId={applicationId} />
                </TabsContent>
                <TabsContent value="notes">
                  <NotesTab applicationId={applicationId} />
                </TabsContent>
                <TabsContent value="interviews">
                  <InterviewsTab applicationId={applicationId} />
                </TabsContent>
                <TabsContent value="documents">
                  <DocumentsTab applicationId={applicationId} />
                </TabsContent>
                <TabsContent value="timeline">
                  <TimelineTab applicationId={applicationId} />
                </TabsContent>
              </Tabs>
            </div>
          </>
        )}
      </SheetContent>
    </Sheet>
  )
}

// ==========================================================================
// Overview Tab
// ==========================================================================

function OverviewTab({ applicationId }: { applicationId: string }) {
  const { data: application } = useApplication(applicationId)
  const updateApp = useUpdateApplication()
  const updateStatus = useUpdateApplicationStatus()
  const [quickNotes, setQuickNotes] = useState<string | null>(null)
  const [archiveOpen, setArchiveOpen] = useState(false)

  if (!application) return null

  const transitions = STATUS_TRANSITIONS[application.status] ?? []
  const notesValue = quickNotes ?? application.quickNotes ?? ""

  function saveQuickNotes() {
    if (quickNotes === null) return
    updateApp.mutate(
      { id: applicationId, data: { quickNotes: quickNotes || undefined } },
      {
        onSuccess: () => {
          toast.success("Notes saved.")
          setQuickNotes(null)
        },
      },
    )
  }

  return (
    <div className="flex flex-col gap-4 pt-4">
      {/* Status */}
      <div className="flex flex-col gap-1.5">
        <Label>Status</Label>
        <Select
          value={application.status}
          onValueChange={(value) => {
            updateStatus.mutate(
              { id: applicationId, status: value as ApplicationStatus },
              {
                onSuccess: () =>
                  toast.success(
                    `Status updated to ${STATUS_LABELS[value as ApplicationStatus]}.`,
                  ),
              },
            )
          }}
        >
          <SelectTrigger className="w-48">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={application.status}>
              {STATUS_LABELS[application.status]}
            </SelectItem>
            {transitions.map((s) => (
              <SelectItem key={s} value={s}>
                {STATUS_LABELS[s]}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {/* Dates */}
      <div className="grid grid-cols-2 gap-4 text-sm">
        <div>
          <span className="text-muted-foreground">Applied</span>
          <p>
            {application.appliedDate
              ? format(new Date(application.appliedDate), "MMM d, yyyy")
              : "--"}
          </p>
        </div>
        <div>
          <span className="text-muted-foreground">Next Action</span>
          <p>
            {application.nextActionDate
              ? format(new Date(application.nextActionDate), "MMM d, yyyy")
              : "--"}
          </p>
        </div>
        <div>
          <span className="text-muted-foreground">Last Activity</span>
          <p>
            {formatDistanceToNow(new Date(application.lastActivityDate), {
              addSuffix: true,
            })}
          </p>
        </div>
      </div>

      {/* Quick Notes */}
      <div className="flex flex-col gap-1.5">
        <Label>Quick Notes</Label>
        <Textarea
          value={notesValue}
          onChange={(e) => setQuickNotes(e.target.value)}
          onBlur={saveQuickNotes}
          placeholder="Add a quick note..."
          className="min-h-[80px]"
        />
      </div>

      {/* Actions */}
      <div className="flex gap-2">
        <Button
          variant="outline"
          size="sm"
          onClick={() => setArchiveOpen(true)}
        >
          {application.archived ? "Unarchive" : "Archive"}
        </Button>
      </div>

      <ConfirmDialog
        open={archiveOpen}
        onOpenChange={setArchiveOpen}
        title="Archive Application"
        description={`This will archive the application for ${application.jobTitle}${application.companyName ? ` at ${application.companyName}` : ""}. You can restore it later.`}
        actionLabel="Archive"
        variant="default"
        onConfirm={() => {
          updateApp.mutate(
            { id: applicationId, data: {} },
            { onSuccess: () => toast.success("Application archived.") },
          )
        }}
      />
    </div>
  )
}

// ==========================================================================
// Notes Tab
// ==========================================================================

function NotesTab({ applicationId }: { applicationId: string }) {
  const { data: notes, isLoading } = useApplicationNotes(applicationId)
  const createNote = useCreateApplicationNote()
  const updateNote = useUpdateApplicationNote()
  const deleteNote = useDeleteApplicationNote()
  const [newNoteContent, setNewNoteContent] = useState("")
  const [editingNoteId, setEditingNoteId] = useState<string | null>(null)
  const [editContent, setEditContent] = useState("")
  const [deleteNoteId, setDeleteNoteId] = useState<string | null>(null)

  function handleAddNote() {
    if (!newNoteContent.trim()) return
    createNote.mutate(
      { applicationId, content: newNoteContent.trim() },
      {
        onSuccess: () => {
          toast.success("Note added.")
          setNewNoteContent("")
        },
      },
    )
  }

  function handleUpdateNote(noteId: string) {
    if (!editContent.trim()) return
    updateNote.mutate(
      { applicationId, noteId, content: editContent.trim() },
      {
        onSuccess: () => {
          toast.success("Note updated.")
          setEditingNoteId(null)
        },
      },
    )
  }

  if (isLoading) {
    return (
      <div className="flex flex-col gap-3 pt-4">
        <Skeleton className="h-8 w-full" />
        <Skeleton className="h-20 w-full" />
        <Skeleton className="h-20 w-full" />
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-4 pt-4">
      {/* Add note */}
      <div className="flex flex-col gap-2">
        <Textarea
          value={newNoteContent}
          onChange={(e) => setNewNoteContent(e.target.value)}
          placeholder="Add a note..."
          className="min-h-[60px]"
        />
        <Button
          size="sm"
          onClick={handleAddNote}
          disabled={!newNoteContent.trim() || createNote.isPending}
        >
          <PlusIcon className="size-3.5" />
          Add Note
        </Button>
      </div>

      {/* Notes list */}
      {(!notes || notes.length === 0) && (
        <p className="text-sm text-muted-foreground">No notes yet.</p>
      )}
      {notes
        ?.slice()
        .sort(
          (a, b) =>
            new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
        )
        .map((note) => (
          <div
            key={note.id}
            className="flex flex-col gap-1 rounded-md border p-3"
          >
            {editingNoteId === note.id ? (
              <div className="flex flex-col gap-2">
                <Textarea
                  value={editContent}
                  onChange={(e) => setEditContent(e.target.value)}
                  className="min-h-[60px]"
                />
                <div className="flex gap-2">
                  <Button
                    size="sm"
                    onClick={() => handleUpdateNote(note.id)}
                    disabled={updateNote.isPending}
                  >
                    Save
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => setEditingNoteId(null)}
                  >
                    Cancel
                  </Button>
                </div>
              </div>
            ) : (
              <>
                <p className="whitespace-pre-wrap text-sm">{note.content}</p>
                <div className="flex items-center justify-between">
                  <span className="text-xs text-muted-foreground">
                    {format(new Date(note.createdAt), "MMM d, yyyy h:mm a")}
                  </span>
                  <div className="flex gap-1">
                    <Button
                      variant="ghost"
                      size="icon-xs"
                      onClick={() => {
                        setEditingNoteId(note.id)
                        setEditContent(note.content)
                      }}
                    >
                      <PencilIcon className="size-3" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon-xs"
                      onClick={() => setDeleteNoteId(note.id)}
                    >
                      <TrashIcon className="size-3" />
                    </Button>
                  </div>
                </div>
              </>
            )}
          </div>
        ))}

      <ConfirmDialog
        open={deleteNoteId !== null}
        onOpenChange={(open) => {
          if (!open) setDeleteNoteId(null)
        }}
        title="Delete Note"
        description="Are you sure you want to delete this note? This cannot be undone."
        actionLabel="Delete"
        onConfirm={() => {
          if (deleteNoteId) {
            deleteNote.mutate(
              { applicationId, noteId: deleteNoteId },
              { onSuccess: () => toast.success("Note deleted.") },
            )
          }
        }}
      />
    </div>
  )
}

// ==========================================================================
// Interviews Tab
// ==========================================================================

const INTERVIEW_TYPES = ["PHONE", "VIDEO", "ONSITE", "TAKE_HOME"] as const
const INTERVIEW_STAGES = [
  "SCREENING",
  "TECHNICAL",
  "BEHAVIORAL",
  "CULTURE_FIT",
  "FINAL",
  "SYSTEM_DESIGN",
  "HOMEWORK",
  "OTHER",
] as const

const NOTE_TYPE_COLORS: Record<string, { bg: string; text: string }> = {
  PREPARATION: {
    bg: "bg-blue-100 dark:bg-blue-900/30",
    text: "text-blue-800 dark:text-blue-300",
  },
  QUESTION_ASKED: {
    bg: "bg-purple-100 dark:bg-purple-900/30",
    text: "text-purple-800 dark:text-purple-300",
  },
  FEEDBACK: {
    bg: "bg-green-100 dark:bg-green-900/30",
    text: "text-green-800 dark:text-green-300",
  },
  FOLLOW_UP: {
    bg: "bg-amber-100 dark:bg-amber-900/30",
    text: "text-amber-800 dark:text-amber-300",
  },
  GENERAL: {
    bg: "bg-gray-100 dark:bg-gray-900/30",
    text: "text-gray-800 dark:text-gray-300",
  },
}

const NOTE_TYPE_LABELS: Record<string, string> = {
  PREPARATION: "Preparation",
  QUESTION_ASKED: "Question Asked",
  FEEDBACK: "Feedback",
  FOLLOW_UP: "Follow Up",
  GENERAL: "General",
}

const NOTE_TYPES = [
  "PREPARATION",
  "QUESTION_ASKED",
  "FEEDBACK",
  "FOLLOW_UP",
  "GENERAL",
] as const

function InterviewsTab({ applicationId }: { applicationId: string }) {
  const { data: interviewsData, isLoading } = useInterviews(applicationId)
  const createInterview = useCreateInterview()
  const deleteInterview = useDeleteInterview()
  const [formOpen, setFormOpen] = useState(false)
  const [deleteId, setDeleteId] = useState<string | null>(null)
  const [expandedInterviewId, setExpandedInterviewId] = useState<string | null>(
    null,
  )

  const toggleExpand = (id: string) =>
    setExpandedInterviewId((prev) => (prev === id ? null : id))

  const interviews = interviewsData?.content ?? []

  if (isLoading) {
    return (
      <div className="flex flex-col gap-3 pt-4">
        <Skeleton className="h-16 w-full" />
        <Skeleton className="h-16 w-full" />
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-4 pt-4">
      <Button size="sm" onClick={() => setFormOpen(true)}>
        <PlusIcon className="size-3.5" />
        Add Interview
      </Button>

      {interviews.length === 0 && (
        <p className="text-sm text-muted-foreground">
          No interviews scheduled.
        </p>
      )}

      {interviews.map((interview) => (
        <div
          key={interview.id}
          className="flex flex-col gap-1.5 rounded-md border p-3"
        >
          <div
            className="flex cursor-pointer items-start justify-between"
            onClick={() => toggleExpand(interview.id)}
          >
            <div className="flex items-start gap-2">
              <ChevronRight
                className="mt-0.5 size-3.5 transition-transform duration-200"
                style={{
                  transform:
                    expandedInterviewId === interview.id
                      ? "rotate(90deg)"
                      : undefined,
                }}
              />
              <div>
                <p className="text-sm font-medium">
                  {interview.interviewType} - {interview.stage}
                  {interview.stageLabel ? ` (${interview.stageLabel})` : ""}
                </p>
                <p className="text-xs text-muted-foreground">
                  {format(
                    new Date(interview.scheduledAt),
                    "MMM d, yyyy 'at' h:mm a",
                  )}
                </p>
              </div>
            </div>
            <Button
              variant="ghost"
              size="icon-xs"
              onClick={(e) => {
                e.stopPropagation()
                setDeleteId(interview.id)
              }}
            >
              <TrashIcon className="size-3" />
            </Button>
          </div>
          {interview.location && (
            <div className="flex items-center gap-1 text-xs text-muted-foreground">
              <MapPinIcon className="size-3" />
              {interview.location}
            </div>
          )}
          {interview.durationMinutes && (
            <p className="text-xs text-muted-foreground">
              {interview.durationMinutes} min
            </p>
          )}
          {expandedInterviewId === interview.id && (
            <InterviewNotesPanel interviewId={interview.id} />
          )}
        </div>
      ))}

      {/* Add interview dialog */}
      <InterviewFormDialog
        open={formOpen}
        onOpenChange={setFormOpen}
        applicationId={applicationId}
        onSubmit={(data) => {
          createInterview.mutate(data, {
            onSuccess: () => {
              toast.success("Interview added.")
              setFormOpen(false)
            },
            onError: () => toast.error("Failed to add interview."),
          })
        }}
        isPending={createInterview.isPending}
      />

      <ConfirmDialog
        open={deleteId !== null}
        onOpenChange={(open) => {
          if (!open) setDeleteId(null)
        }}
        title="Delete Interview"
        description="Are you sure you want to delete this interview?"
        actionLabel="Delete"
        onConfirm={() => {
          if (deleteId) {
            deleteInterview.mutate(
              { id: deleteId, applicationId },
              { onSuccess: () => toast.success("Interview deleted.") },
            )
          }
        }}
      />
    </div>
  )
}

// ==========================================================================
// Interview Notes Panel (expandable per interview)
// ==========================================================================

function InterviewNotesPanel({ interviewId }: { interviewId: string }) {
  const { data: notes, isLoading } = useInterviewNotes(interviewId)
  const createNote = useCreateInterviewNote()
  const updateNote = useUpdateInterviewNote()
  const deleteNote = useDeleteInterviewNote()
  const [newContent, setNewContent] = useState("")
  const [newNoteType, setNewNoteType] = useState("GENERAL")
  const [deleteNoteId, setDeleteNoteId] = useState<string | null>(null)

  if (isLoading) {
    return (
      <div className="ml-4 mt-2 border-l-2 border-muted pl-4 pb-4">
        <Skeleton className="h-4 w-full" />
        <Skeleton className="mt-2 h-4 w-full" />
      </div>
    )
  }

  return (
    <div className="ml-4 mt-2 border-l-2 border-muted pl-4 pb-4">
      {(!notes || notes.length === 0) && (
        <div className="mb-3">
          <p className="text-sm text-muted-foreground">No notes yet</p>
          <p className="text-xs text-muted-foreground">
            Add your first note for this interview.
          </p>
        </div>
      )}

      {notes
        ?.slice()
        .sort(
          (a, b) =>
            new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
        )
        .map((note) => (
          <NoteRow
            key={note.id}
            note={note}
            interviewId={interviewId}
            updateNote={updateNote}
            onDelete={(noteId) => setDeleteNoteId(noteId)}
          />
        ))}

      {/* Add note form */}
      <div className="mt-3 flex flex-col gap-2">
        <Textarea
          value={newContent}
          onChange={(e) => setNewContent(e.target.value)}
          placeholder="Add a note..."
          className="min-h-[60px] text-sm"
        />
        <div className="flex items-center gap-2">
          <Select value={newNoteType} onValueChange={setNewNoteType}>
            <SelectTrigger className="w-40">
              <SelectValue placeholder="Note type" />
            </SelectTrigger>
            <SelectContent>
              {NOTE_TYPES.map((t) => (
                <SelectItem key={t} value={t}>
                  {NOTE_TYPE_LABELS[t]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Button
            size="sm"
            disabled={!newContent.trim() || createNote.isPending}
            onClick={() =>
              createNote.mutate(
                {
                  interviewId,
                  content: newContent.trim(),
                  noteType: newNoteType,
                },
                {
                  onSuccess: () => {
                    toast.success("Note added")
                    setNewContent("")
                    setNewNoteType("GENERAL")
                  },
                  onError: () =>
                    toast.error("Failed to save note. Please try again."),
                },
              )
            }
          >
            <PlusIcon className="size-3.5" />
            Add Note
          </Button>
        </div>
      </div>

      <ConfirmDialog
        open={deleteNoteId !== null}
        onOpenChange={(open) => {
          if (!open) setDeleteNoteId(null)
        }}
        title="Delete Note"
        description="This note will be permanently deleted. This action cannot be undone."
        actionLabel="Delete"
        onConfirm={() => {
          if (deleteNoteId) {
            deleteNote.mutate(
              { interviewId, noteId: deleteNoteId },
              {
                onSuccess: () => {
                  toast.success("Note deleted")
                  setDeleteNoteId(null)
                },
              },
            )
          }
        }}
      />
    </div>
  )
}

function NoteRow({
  note,
  interviewId,
  updateNote,
  onDelete,
}: {
  note: InterviewNoteResponse
  interviewId: string
  updateNote: ReturnType<typeof useUpdateInterviewNote>
  onDelete: (noteId: string) => void
}) {
  const [editContent, setEditContent] = useState(note.content)

  function handleBlur() {
    if (editContent.trim() !== note.content) {
      updateNote.mutate(
        { interviewId, noteId: note.id, content: editContent.trim() },
        {
          onSuccess: () => toast.success("Note updated"),
          onError: () => {
            toast.error("Failed to save note. Please try again.")
            setEditContent(note.content)
          },
        },
      )
    }
  }

  function handleKeyDown(e: React.KeyboardEvent) {
    if (e.key === "Escape") {
      setEditContent(note.content)
      ;(e.target as HTMLTextAreaElement).blur()
    }
  }

  const colors = NOTE_TYPE_COLORS[note.noteType]

  return (
    <div className="mb-2 flex items-start gap-2">
      <span
        className={`inline-flex shrink-0 items-center rounded-md px-2 py-0.5 text-xs font-medium ${colors?.bg ?? ""} ${colors?.text ?? ""}`}
      >
        {NOTE_TYPE_LABELS[note.noteType] ?? note.noteType}
      </span>
      <Textarea
        value={editContent}
        onChange={(e) => setEditContent(e.target.value)}
        onBlur={handleBlur}
        onKeyDown={handleKeyDown}
        className={`min-h-[2rem] flex-1 text-sm ${updateNote.isPending ? "opacity-50" : ""}`}
        disabled={updateNote.isPending}
      />
      <span className="shrink-0 text-xs text-muted-foreground whitespace-nowrap">
        {format(new Date(note.createdAt), "MMM d")}
      </span>
      <Button
        variant="ghost"
        size="icon-xs"
        onClick={() => onDelete(note.id)}
      >
        <Trash2 className="size-3 text-destructive" />
      </Button>
    </div>
  )
}

function InterviewFormDialog({
  open,
  onOpenChange,
  applicationId,
  onSubmit,
  isPending,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  applicationId: string
  onSubmit: (data: {
    applicationId: string
    scheduledAt: string
    interviewType: string
    stage: string
    stageLabel?: string
    durationMinutes?: number
    location?: string
  }) => void
  isPending: boolean
}) {
  const [scheduledAt, setScheduledAt] = useState("")
  const [interviewType, setInterviewType] = useState("PHONE")
  const [stage, setStage] = useState("SCREENING")
  const [stageLabel, setStageLabel] = useState("")
  const [durationMinutes, setDurationMinutes] = useState("60")
  const [location, setLocation] = useState("")

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!scheduledAt) return
    onSubmit({
      applicationId,
      scheduledAt: new Date(scheduledAt).toISOString(),
      interviewType,
      stage,
      stageLabel: stageLabel || undefined,
      durationMinutes: durationMinutes ? parseInt(durationMinutes, 10) : undefined,
      location: location || undefined,
    })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Add Interview</DialogTitle>
          <DialogDescription>
            Schedule a new interview round.
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="flex flex-col gap-2">
            <Label htmlFor="scheduled-at">Date & Time *</Label>
            <Input
              id="scheduled-at"
              type="datetime-local"
              value={scheduledAt}
              onChange={(e) => setScheduledAt(e.target.value)}
              required
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-2">
              <Label>Type</Label>
              <Select value={interviewType} onValueChange={setInterviewType}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {INTERVIEW_TYPES.map((t) => (
                    <SelectItem key={t} value={t}>
                      {t}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="flex flex-col gap-2">
              <Label>Stage</Label>
              <Select value={stage} onValueChange={setStage}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {INTERVIEW_STAGES.map((s) => (
                    <SelectItem key={s} value={s}>
                      {s}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="stage-label">Stage Label</Label>
            <Input
              id="stage-label"
              value={stageLabel}
              onChange={(e) => setStageLabel(e.target.value)}
              placeholder="e.g., Round 2 Technical"
            />
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="duration">Duration (minutes)</Label>
            <Input
              id="duration"
              type="number"
              value={durationMinutes}
              onChange={(e) => setDurationMinutes(e.target.value)}
              placeholder="60"
            />
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="location">Location</Label>
            <Input
              id="location"
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              placeholder="Office address or meeting link..."
            />
          </div>
          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={!scheduledAt || isPending}>
              Add Interview
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

// ==========================================================================
// Documents Tab
// ==========================================================================

function DocumentsTab({ applicationId }: { applicationId: string }) {
  const { data: links, isLoading } =
    useDocumentLinksForApplication(applicationId)
  const { data: documentsData } = useDocuments({ size: 100 })
  const linkDoc = useLinkDocumentToApplication()
  const unlinkDoc = useUnlinkDocument()
  const [linkDialogOpen, setLinkDialogOpen] = useState(false)
  const [selectedVersionId, setSelectedVersionId] = useState("")

  const documents = documentsData?.content ?? []

  if (isLoading) {
    return (
      <div className="flex flex-col gap-3 pt-4">
        <Skeleton className="h-12 w-full" />
        <Skeleton className="h-12 w-full" />
      </div>
    )
  }

  function handleLink() {
    if (!selectedVersionId) return
    linkDoc.mutate(
      { documentVersionId: selectedVersionId, applicationId },
      {
        onSuccess: () => {
          toast.success("Document linked.")
          setLinkDialogOpen(false)
          setSelectedVersionId("")
        },
        onError: () => toast.error("Failed to link document."),
      },
    )
  }

  // Build a map from version IDs to documents for display
  const versionToDoc = new Map<
    string,
    { title: string; category: string; filename: string; fileSize: number; versionNumber: number; documentId: string }
  >()
  for (const doc of documents) {
    if (doc.currentVersion) {
      versionToDoc.set(doc.currentVersion.id, {
        title: doc.title,
        category: doc.category,
        filename: doc.currentVersion.originalFilename,
        fileSize: doc.currentVersion.fileSize,
        versionNumber: doc.currentVersion.versionNumber,
        documentId: doc.id,
      })
    }
  }

  return (
    <div className="flex flex-col gap-4 pt-4">
      <Button size="sm" onClick={() => setLinkDialogOpen(true)}>
        <LinkIcon className="size-3.5" />
        Link Document
      </Button>

      {(!links || links.length === 0) && (
        <p className="text-sm text-muted-foreground">
          No documents linked. Link a document to this application.
        </p>
      )}

      {links?.map((link) => {
        const info = versionToDoc.get(link.documentVersionId)
        return (
          <div
            key={link.id}
            className="flex items-center justify-between rounded-md border p-3"
          >
            <div className="flex flex-col gap-0.5">
              <p className="text-sm font-medium">
                {info?.title ?? "Document"}
              </p>
              <p className="text-xs text-muted-foreground">
                {info?.category ?? "Unknown"} - v{info?.versionNumber ?? "?"} -{" "}
                {info?.filename ?? "file"}
                {info
                  ? ` (${(info.fileSize / 1024).toFixed(1)} KB)`
                  : ""}
              </p>
            </div>
            <div className="flex gap-1">
              {info && (
                <DownloadLink
                  documentId={info.documentId}
                  versionId={link.documentVersionId}
                />
              )}
              <Button
                variant="ghost"
                size="icon-xs"
                onClick={() =>
                  unlinkDoc.mutate(
                    {
                      documentVersionId: link.documentVersionId,
                      applicationId,
                    },
                    {
                      onSuccess: () => toast.success("Document unlinked."),
                    },
                  )
                }
              >
                <UnlinkIcon className="size-3" />
              </Button>
            </div>
          </div>
        )
      })}

      {/* Link document dialog */}
      <Dialog open={linkDialogOpen} onOpenChange={setLinkDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Link Document</DialogTitle>
            <DialogDescription>
              Select a document to link to this application.
            </DialogDescription>
          </DialogHeader>
          <div className="flex flex-col gap-2">
            <Label>Document</Label>
            <Select
              value={selectedVersionId}
              onValueChange={setSelectedVersionId}
            >
              <SelectTrigger>
                <SelectValue placeholder="Select a document..." />
              </SelectTrigger>
              <SelectContent>
                {documents
                  .filter((d) => d.currentVersion)
                  .map((doc) => (
                    <SelectItem
                      key={doc.currentVersion!.id}
                      value={doc.currentVersion!.id}
                    >
                      {doc.title} (v{doc.currentVersion!.versionNumber})
                    </SelectItem>
                  ))}
              </SelectContent>
            </Select>
          </div>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setLinkDialogOpen(false)}
            >
              Cancel
            </Button>
            <Button
              onClick={handleLink}
              disabled={!selectedVersionId || linkDoc.isPending}
            >
              Link
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}

function DownloadLink({
  documentId,
  versionId,
}: {
  documentId: string
  versionId: string
}) {
  const url = useDownloadVersionUrl(documentId, versionId)
  return (
    <Button variant="ghost" size="icon-xs" asChild>
      <a href={url} target="_blank" rel="noopener noreferrer">
        <DownloadIcon className="size-3" />
      </a>
    </Button>
  )
}
