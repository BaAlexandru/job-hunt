"use client"

import { MapPin, Briefcase, MoreHorizontal, Pencil, Trash2 } from "lucide-react"
import { Card, CardContent, CardHeader, CardTitle, CardAction } from "@/components/ui/card"
import { VisibilityBadge } from "@/components/shared/visibility-badge"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import type { CompanyResponse } from "@/types/api"

interface CompanyCardProps {
  company: CompanyResponse
  jobCount: number
  onEdit: () => void
  onDelete: () => void
  onClick: () => void
  hideActions?: boolean
}

export function CompanyCard({
  company,
  jobCount,
  onEdit,
  onDelete,
  onClick,
  hideActions = false,
}: CompanyCardProps) {
  return (
    <Card
      className="cursor-pointer transition-shadow hover:shadow-md"
      onClick={onClick}
    >
      <CardHeader>
        <div className="flex items-center gap-1">
          <VisibilityBadge visibility={company.visibility} />
          <CardTitle className="text-sm font-semibold">{company.name}</CardTitle>
        </div>
        {!hideActions && (
        <CardAction>
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
                  onEdit()
                }}
              >
                <Pencil />
                Edit
              </DropdownMenuItem>
              <DropdownMenuItem
                variant="destructive"
                onClick={(e) => {
                  e.stopPropagation()
                  onDelete()
                }}
              >
                <Trash2 />
                Delete
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </CardAction>
        )}
      </CardHeader>
      <CardContent className="flex flex-col gap-1.5">
        {company.website && (
          <a
            href={company.website}
            target="_blank"
            rel="noopener noreferrer"
            className="truncate text-xs text-muted-foreground hover:underline"
            onClick={(e) => e.stopPropagation()}
          >
            {company.website}
          </a>
        )}
        {company.location && (
          <div className="flex items-center gap-1 text-xs text-muted-foreground">
            <MapPin className="size-3 shrink-0" />
            <span className="truncate">{company.location}</span>
          </div>
        )}
        <div className="flex items-center gap-1 text-xs text-muted-foreground">
          <Briefcase className="size-3 shrink-0" />
          <span>{jobCount === 1 ? "1 job" : `${jobCount} jobs`}</span>
        </div>
      </CardContent>
    </Card>
  )
}
