"use client"

import {
  useQuery,
  useMutation,
  useQueryClient,
} from "@tanstack/react-query"
import { apiClient } from "@/lib/api-client"
import type {
  CreateJobRequest,
  JobResponse,
  PaginatedResponse,
  UpdateJobRequest,
} from "@/types/api"

// ---------------------------------------------------------------------------
// Filters
// ---------------------------------------------------------------------------

export interface JobFilters {
  companyId?: string
  page?: number
  size?: number
  sort?: string
}

// ---------------------------------------------------------------------------
// Query key factory
// ---------------------------------------------------------------------------

export const jobKeys = {
  all: ["jobs"] as const,
  lists: () => [...jobKeys.all, "list"] as const,
  list: (filters: JobFilters) =>
    [...jobKeys.lists(), filters] as const,
  details: () => [...jobKeys.all, "detail"] as const,
  detail: (id: string) => [...jobKeys.details(), id] as const,
}

// ---------------------------------------------------------------------------
// Queries
// ---------------------------------------------------------------------------

export function useJobs(filters: JobFilters = {}) {
  const params = new URLSearchParams()
  if (filters.companyId) params.set("companyId", filters.companyId)
  if (filters.page !== undefined) params.set("page", String(filters.page))
  if (filters.size !== undefined) params.set("size", String(filters.size))
  if (filters.sort) params.set("sort", filters.sort)
  const qs = params.toString()

  return useQuery({
    queryKey: jobKeys.list(filters),
    queryFn: () =>
      apiClient<PaginatedResponse<JobResponse>>(
        `/jobs${qs ? `?${qs}` : ""}`,
      ),
  })
}

export function useJob(id: string) {
  return useQuery({
    queryKey: jobKeys.detail(id),
    queryFn: () => apiClient<JobResponse>(`/jobs/${id}`),
    enabled: !!id,
  })
}

// ---------------------------------------------------------------------------
// Mutations
// ---------------------------------------------------------------------------

export function useCreateJob() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateJobRequest) =>
      apiClient<JobResponse>("/jobs", {
        method: "POST",
        body: JSON.stringify(data),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: jobKeys.lists() })
    },
  })
}

export function useUpdateJob() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateJobRequest }) =>
      apiClient<JobResponse>(`/jobs/${id}`, {
        method: "PUT",
        body: JSON.stringify(data),
      }),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: jobKeys.detail(variables.id),
      })
      queryClient.invalidateQueries({ queryKey: jobKeys.lists() })
    },
  })
}

export function useDeleteJob() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) =>
      apiClient<void>(`/jobs/${id}`, { method: "DELETE" }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: jobKeys.lists() })
    },
  })
}
