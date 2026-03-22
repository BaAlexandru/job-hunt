import { Globe, Users } from "lucide-react"
import type { Visibility } from "@/types/api"

export function VisibilityBadge({ visibility }: { visibility: Visibility }) {
  if (visibility === "PUBLIC") {
    return (
      <Globe className="size-3.5 text-muted-foreground" aria-label="Public" />
    )
  }
  if (visibility === "SHARED") {
    return (
      <Users className="size-3.5 text-muted-foreground" aria-label="Shared" />
    )
  }
  return null
}
