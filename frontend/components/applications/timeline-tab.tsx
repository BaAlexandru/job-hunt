"use client"

import { formatDistanceToNow } from "date-fns"
import {
  CalendarIcon,
  StickyNoteIcon,
  MessageSquareIcon,
} from "lucide-react"
import { Skeleton } from "@/components/ui/skeleton"
import { useTimeline } from "@/hooks/use-applications"
import type { TimelineEntry } from "@/types/api"

const TYPE_CONFIG: Record<string, { icon: typeof CalendarIcon; label: string }> = {
  INTERVIEW: { icon: CalendarIcon, label: "Interview" },
  APPLICATION_NOTE: { icon: StickyNoteIcon, label: "Note" },
  INTERVIEW_NOTE: { icon: MessageSquareIcon, label: "Interview Note" },
}

function getTypeConfig(type: string) {
  return TYPE_CONFIG[type] ?? { icon: StickyNoteIcon, label: type }
}

export function TimelineTab({ applicationId }: { applicationId: string }) {
  const { data: entries, isLoading } = useTimeline(applicationId)

  if (isLoading) {
    return (
      <div className="flex flex-col gap-4 pt-4">
        <Skeleton className="h-16 w-full" />
        <Skeleton className="h-16 w-full" />
        <Skeleton className="h-16 w-full" />
      </div>
    )
  }

  if (!entries || entries.length === 0) {
    return (
      <p className="pt-4 text-sm text-muted-foreground">
        No timeline entries yet.
      </p>
    )
  }

  return (
    <div className="flex flex-col gap-4 pt-4">
      {entries.map((entry) => (
        <TimelineEntryCard key={entry.id} entry={entry} />
      ))}
    </div>
  )
}

function TimelineEntryCard({ entry }: { entry: TimelineEntry }) {
  const config = getTypeConfig(entry.type)
  const Icon = config.icon

  return (
    <div className="flex flex-col gap-1.5 rounded-md border p-3">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Icon className="size-4 text-muted-foreground" />
          <span className="text-xs font-semibold">{config.label}</span>
        </div>
        <span className="text-xs text-muted-foreground">
          {formatDistanceToNow(new Date(entry.occurredAt), { addSuffix: true })}
        </span>
      </div>
      <p className="text-sm">{entry.title}</p>
      {entry.metadata && Object.keys(entry.metadata).length > 0 && (
        <div className="flex flex-col gap-0.5">
          {Object.entries(entry.metadata).map(([key, value]) => (
            <p key={key} className="text-xs text-muted-foreground">
              <span className="font-medium">{key}:</span>{" "}
              {String(value ?? "")}
            </p>
          ))}
        </div>
      )}
    </div>
  )
}
