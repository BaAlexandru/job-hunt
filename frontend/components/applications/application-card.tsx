"use client"

import { CalendarIcon } from "lucide-react"
import { format } from "date-fns"
import { Card, CardContent } from "@/components/ui/card"
import type { ApplicationResponse } from "@/types/api"

interface ApplicationCardProps {
  application: ApplicationResponse
  onClick: () => void
}

export function ApplicationCard({ application, onClick }: ApplicationCardProps) {
  return (
    <Card
      size="sm"
      className="cursor-pointer transition-shadow hover:shadow-md"
      onClick={onClick}
    >
      <CardContent className="space-y-1 p-2">
        {application.companyName && (
          <p className="truncate text-xs text-muted-foreground">
            {application.companyName}
          </p>
        )}
        <p className="truncate text-sm font-medium">{application.jobTitle}</p>
        {application.nextActionDate && (
          <div className="flex items-center gap-1 text-xs text-muted-foreground">
            <CalendarIcon className="size-3" />
            <span>{format(new Date(application.nextActionDate), "MMM d")}</span>
          </div>
        )}
      </CardContent>
    </Card>
  )
}
