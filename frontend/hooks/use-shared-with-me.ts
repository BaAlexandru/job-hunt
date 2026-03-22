"use client"

import { useQuery } from "@tanstack/react-query"
import { apiClient } from "@/lib/api-client"
import type {
  PaginatedResponse,
  CompanyResponse,
  JobResponse,
} from "@/types/api"

export const sharedWithMeKeys = {
  all: ["shared-with-me"] as const,
  companies: (params?: { page?: number }) =>
    [...sharedWithMeKeys.all, "companies", params] as const,
  jobs: (params?: { page?: number }) =>
    [...sharedWithMeKeys.all, "jobs", params] as const,
}

export function useSharedCompanies(
  params: { page?: number } = {},
) {
  const searchParams = new URLSearchParams()
  if (params.page !== undefined) searchParams.set("page", String(params.page))
  const qs = searchParams.toString()

  return useQuery({
    queryKey: sharedWithMeKeys.companies(params),
    queryFn: () =>
      apiClient<PaginatedResponse<CompanyResponse>>(
        `/shared/companies${qs ? `?${qs}` : ""}`,
      ),
  })
}

export function useSharedJobs(params: { page?: number } = {}) {
  const searchParams = new URLSearchParams()
  if (params.page !== undefined) searchParams.set("page", String(params.page))
  const qs = searchParams.toString()

  return useQuery({
    queryKey: sharedWithMeKeys.jobs(params),
    queryFn: () =>
      apiClient<PaginatedResponse<JobResponse>>(
        `/shared/jobs${qs ? `?${qs}` : ""}`,
      ),
  })
}
