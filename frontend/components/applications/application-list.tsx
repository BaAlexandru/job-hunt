"use client"

import type { ApplicationResponse } from "@/types/api"
import type { FilterState } from "@/components/applications/filter-bar"

interface ApplicationListProps {
  applications: ApplicationResponse[]
  onSelectApplication: (id: string) => void
  isLoading: boolean
  pagination: {
    page: number
    totalPages: number
    onPageChange: (page: number) => void
  }
  filters: FilterState
  onFiltersChange: (filters: FilterState) => void
}

export function ApplicationList(_props: ApplicationListProps) {
  // Placeholder -- replaced in Task 2
  return <div>Loading list view...</div>
}
