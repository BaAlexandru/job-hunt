import { Globe, MapPin } from "lucide-react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"

interface BrowseCardProps {
  name: string
  subtitle?: string | null
  location?: string | null
  ownerEmail: string
  onClick: () => void
}

export function BrowseCard({
  name,
  subtitle,
  location,
  ownerEmail,
  onClick,
}: BrowseCardProps) {
  return (
    <Card
      className="cursor-pointer transition-shadow hover:shadow-md"
      onClick={onClick}
    >
      <CardHeader>
        <div className="flex items-center gap-1">
          <Globe
            className="size-3.5 text-muted-foreground"
            aria-label="Public"
          />
          <CardTitle className="text-sm font-semibold">{name}</CardTitle>
        </div>
      </CardHeader>
      <CardContent className="flex flex-col gap-1.5">
        {subtitle && (
          <p className="truncate text-xs text-muted-foreground">{subtitle}</p>
        )}
        {location && (
          <div className="flex items-center gap-1 text-xs text-muted-foreground">
            <MapPin className="size-3 shrink-0" />
            <span className="truncate">{location}</span>
          </div>
        )}
        <p className="text-xs text-muted-foreground">Shared by {ownerEmail}</p>
      </CardContent>
    </Card>
  )
}
