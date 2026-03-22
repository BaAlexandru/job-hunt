"use client"

import { useQuery } from "@tanstack/react-query"
import { apiClient } from "@/lib/api-client"
import type {
  PaginatedResponse,
  BrowseCompanyResponse,
  BrowseJobResponse,
} from "@/types/api"

export const browseKeys = {
  all: ["browse"] as const,
  companies: (params?: { q?: string; page?: number }) =>
    [...browseKeys.all, "companies", params] as const,
  jobs: (params?: { q?: string; page?: number }) =>
    [...browseKeys.all, "jobs", params] as const,
}

export function useBrowseCompanies(
  params: { q?: string; page?: number } = {},
) {
  const searchParams = new URLSearchParams()
  if (params.q) searchParams.set("q", params.q)
  if (params.page !== undefined) searchParams.set("page", String(params.page))
  const qs = searchParams.toString()

  return useQuery({
    queryKey: browseKeys.companies(params),
    queryFn: () =>
      apiClient<PaginatedResponse<BrowseCompanyResponse>>(
        `/browse/companies${qs ? `?${qs}` : ""}`,
      ),
  })
}

export function useBrowseJobs(params: { q?: string; page?: number } = {}) {
  const searchParams = new URLSearchParams()
  if (params.q) searchParams.set("q", params.q)
  if (params.page !== undefined) searchParams.set("page", String(params.page))
  const qs = searchParams.toString()

  return useQuery({
    queryKey: browseKeys.jobs(params),
    queryFn: () =>
      apiClient<PaginatedResponse<BrowseJobResponse>>(
        `/browse/jobs${qs ? `?${qs}` : ""}`,
      ),
  })
}
