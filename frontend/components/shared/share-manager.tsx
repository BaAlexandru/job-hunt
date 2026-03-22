"use client"

import { useState } from "react"
import { toast } from "sonner"
import { useShares, useCreateShare, useRevokeShare } from "@/hooks/use-shares"
import { ApiError } from "@/lib/api-client"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Separator } from "@/components/ui/separator"
import { ConfirmDialog } from "@/components/shared/confirm-dialog"

interface ShareManagerProps {
  resourceType: "companies" | "jobs"
  resourceId: string
  resourceName: string
}

export function ShareManager({
  resourceType,
  resourceId,
  resourceName,
}: ShareManagerProps) {
  const [email, setEmail] = useState("")
  const [revokeTarget, setRevokeTarget] = useState<{
    shareId: string
    email: string
  } | null>(null)

  const { data: shares = [] } = useShares(resourceType, resourceId)
  const createShare = useCreateShare(resourceType)
  const revokeShare = useRevokeShare(resourceType)

  function handleShare(e: React.FormEvent) {
    e.preventDefault()
    const trimmed = email.trim()
    if (!trimmed) return

    createShare.mutate(
      { resourceId, email: trimmed },
      {
        onSuccess: () => {
          setEmail("")
          toast.success(`Shared with ${trimmed}`)
        },
        onError: (error) => {
          if (error instanceof ApiError) {
            if (error.status === 404) {
              toast.error(`No account found for ${trimmed}`)
            } else if (error.status === 409) {
              toast.error(error.message)
            } else {
              toast.error(error.message)
            }
          } else {
            toast.error("Failed to share")
          }
        },
      },
    )
  }

  function handleRevoke() {
    if (!revokeTarget) return

    revokeShare.mutate(
      { resourceId, shareId: revokeTarget.shareId },
      {
        onSuccess: () => {
          toast.success(`Access revoked for ${revokeTarget.email}`)
          setRevokeTarget(null)
        },
      },
    )
  }

  return (
    <div className="flex flex-col gap-3">
      {shares.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          Not shared with anyone yet
        </p>
      ) : (
        <ul className="flex flex-col gap-2">
          {shares.map((share) => (
            <li
              key={share.id}
              className="flex items-center justify-between gap-2"
            >
              <div className="min-w-0">
                <p className="truncate text-sm font-medium">{share.email}</p>
                <p className="text-xs text-muted-foreground">
                  Shared {new Date(share.sharedAt).toLocaleDateString()}
                </p>
              </div>
              <Button
                variant="ghost"
                size="sm"
                className="shrink-0 text-destructive"
                aria-label={`Revoke access for ${share.email}`}
                onClick={() =>
                  setRevokeTarget({ shareId: share.id, email: share.email })
                }
              >
                Revoke
              </Button>
            </li>
          ))}
        </ul>
      )}

      <Separator />

      <form onSubmit={handleShare} className="flex items-center gap-2">
        <Input
          type="email"
          placeholder="Enter email address"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="flex-1"
        />
        <Button type="submit" disabled={createShare.isPending || !email.trim()}>
          Share
        </Button>
      </form>

      <ConfirmDialog
        open={revokeTarget !== null}
        onOpenChange={(open) => {
          if (!open) setRevokeTarget(null)
        }}
        title="Revoke access?"
        description={`Remove ${revokeTarget?.email}'s access to ${resourceName}?`}
        actionLabel="Revoke"
        onConfirm={handleRevoke}
        variant="destructive"
      />
    </div>
  )
}
