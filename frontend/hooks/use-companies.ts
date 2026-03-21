"use client"

import {
  useQuery,
  useMutation,
  useQueryClient,
} from "@tanstack/react-query"
import { apiClient } from "@/lib/api-client"
import type {
  CompanyResponse,
  CreateCompanyRequest,
  PaginatedResponse,
  UpdateCompanyRequest,
} from "@/types/api"

// ---------------------------------------------------------------------------
// Filters
// ---------------------------------------------------------------------------

export interface CompanyFilters {
  q?: string
  includeArchived?: boolean
  page?: number
  size?: number
  sort?: string
}

// ---------------------------------------------------------------------------
// Query key factory
// ---------------------------------------------------------------------------

export const companyKeys = {
  all: ["companies"] as const,
  lists: () => [...companyKeys.all, "list"] as const,
  list: (filters: CompanyFilters) =>
    [...companyKeys.lists(), filters] as const,
  details: () => [...companyKeys.all, "detail"] as const,
  detail: (id: string) => [...companyKeys.details(), id] as const,
}

// ---------------------------------------------------------------------------
// Queries
// ---------------------------------------------------------------------------

export function useCompanies(filters: CompanyFilters = {}) {
  const params = new URLSearchParams()
  if (filters.q) params.set("q", filters.q)
  if (filters.includeArchived) params.set("includeArchived", "true")
  if (filters.page !== undefined) params.set("page", String(filters.page))
  if (filters.size !== undefined) params.set("size", String(filters.size))
  if (filters.sort) params.set("sort", filters.sort)
  const qs = params.toString()

  return useQuery({
    queryKey: companyKeys.list(filters),
    queryFn: () =>
      apiClient<PaginatedResponse<CompanyResponse>>(
        `/companies${qs ? `?${qs}` : ""}`,
      ),
  })
}

export function useCompany(id: string) {
  return useQuery({
    queryKey: companyKeys.detail(id),
    queryFn: () => apiClient<CompanyResponse>(`/companies/${id}`),
    enabled: !!id,
  })
}

// ---------------------------------------------------------------------------
// Mutations
// ---------------------------------------------------------------------------

export function useCreateCompany() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateCompanyRequest) =>
      apiClient<CompanyResponse>("/companies", {
        method: "POST",
        body: JSON.stringify(data),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: companyKeys.lists() })
    },
  })
}

export function useUpdateCompany() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string
      data: UpdateCompanyRequest
    }) =>
      apiClient<CompanyResponse>(`/companies/${id}`, {
        method: "PUT",
        body: JSON.stringify(data),
      }),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: companyKeys.detail(variables.id),
      })
      queryClient.invalidateQueries({ queryKey: companyKeys.lists() })
    },
  })
}

export function useDeleteCompany() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) =>
      apiClient<void>(`/companies/${id}`, { method: "DELETE" }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: companyKeys.lists() })
    },
  })
}
