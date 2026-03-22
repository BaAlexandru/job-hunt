"use client"

import { useState } from "react"
import { toast } from "sonner"
import type { Visibility } from "@/types/api"
import { ApiError } from "@/lib/api-client"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { ConfirmDialog } from "@/components/shared/confirm-dialog"
import { useSetCompanyVisibility } from "@/hooks/use-visibility"
import { useSetJobVisibility } from "@/hooks/use-visibility"

interface VisibilityControlProps {
  resourceType: "companies" | "jobs"
  resourceId: string
  resourceName: string
  currentVisibility: Visibility
}

export function VisibilityControl({
  resourceType,
  resourceId,
  resourceName,
  currentVisibility,
}: VisibilityControlProps) {
  const [confirmPublicOpen, setConfirmPublicOpen] = useState(false)
  const companyMutation = useSetCompanyVisibility()
  const jobMutation = useSetJobVisibility()

  const mutation = resourceType === "companies" ? companyMutation : jobMutation

  function handleChange(value: string) {
    const visibility = value as Visibility
    if (visibility === "PUBLIC") {
      setConfirmPublicOpen(true)
      return
    }
    mutation.mutate(
      { id: resourceId, visibility },
      {
        onSuccess: () => {
          toast.success(`Visibility updated to ${visibility}`)
        },
        onError: (error) => {
          toast.error(error instanceof ApiError ? error.message : "Failed to update visibility")
        },
      },
    )
  }

  function handleConfirmPublic() {
    mutation.mutate(
      { id: resourceId, visibility: "PUBLIC" },
      {
        onSuccess: () => {
          toast.success("Visibility updated to PUBLIC")
        },
        onError: (error) => {
          toast.error(error instanceof ApiError ? error.message : "Failed to update visibility")
        },
      },
    )
  }

  return (
    <>
      <Select value={currentVisibility} onValueChange={handleChange}>
        <SelectTrigger>
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="PRIVATE">Private</SelectItem>
          <SelectItem value="SHARED">Shared</SelectItem>
          <SelectItem value="PUBLIC">Public</SelectItem>
        </SelectContent>
      </Select>

      <ConfirmDialog
        open={confirmPublicOpen}
        onOpenChange={setConfirmPublicOpen}
        title="Make publicly visible?"
        description={`This will make ${resourceName} visible to all logged-in users.`}
        actionLabel="Make Public"
        onConfirm={handleConfirmPublic}
        variant="default"
      />
    </>
  )
}
