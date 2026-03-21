"use client"

import { Badge } from "@/components/ui/badge"
import { cn } from "@/lib/utils"
import type { ApplicationStatus } from "@/types/api"
import { STATUS_COLORS, STATUS_LABELS } from "@/types/api"

interface StatusBadgeProps {
  status: ApplicationStatus
  className?: string
}

export function StatusBadge({ status, className }: StatusBadgeProps) {
  const colors = STATUS_COLORS[status]
  const label = STATUS_LABELS[status]

  return (
    <Badge
      className={cn(
        colors.bg,
        colors.text,
        "font-semibold text-xs",
        className,
      )}
    >
      {label}
    </Badge>
  )
}
