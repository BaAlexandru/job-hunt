import { Card, CardContent } from "@/components/ui/card"

interface EmptyStateProps {
  heading: string
  body: string
}

export function EmptyState({ heading, body }: EmptyStateProps) {
  return (
    <Card className="mx-auto max-w-md">
      <CardContent className="flex flex-col items-center gap-2 py-12 text-center">
        <h2 className="text-xl font-semibold leading-tight">{heading}</h2>
        <p className="text-sm text-muted-foreground">{body}</p>
      </CardContent>
    </Card>
  )
}
