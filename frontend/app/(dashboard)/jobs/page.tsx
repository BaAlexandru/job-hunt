import { EmptyState } from "@/components/shared/empty-state"

export default function JobsPage() {
  return (
    <EmptyState
      heading="No job postings yet"
      body="Add job postings you've found."
    />
  )
}
