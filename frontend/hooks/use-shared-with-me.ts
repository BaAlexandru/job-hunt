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
  companies: (page?: number) =>
    [...sharedWithMeKeys.all, "companies", page] as const,
  jobs: (page?: number) =>
    [...sharedWithMeKeys.all, "jobs", page] as const,
}

export function useSharedCompanies(page: number = 0) {
  return useQuery({
    queryKey: sharedWithMeKeys.companies(page),
    queryFn: () =>
      apiClient<PaginatedResponse<CompanyResponse>>(
        `/shared/companies?page=${page}`,
      ),
  })
}

export function useSharedJobs(page: number = 0) {
  return useQuery({
    queryKey: sharedWithMeKeys.jobs(page),
    queryFn: () =>
      apiClient<PaginatedResponse<JobResponse>>(
        `/shared/jobs?page=${page}`,
      ),
  })
}
