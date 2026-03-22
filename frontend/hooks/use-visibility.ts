"use client"

import { useMutation, useQueryClient } from "@tanstack/react-query"
import { apiClient } from "@/lib/api-client"
import type { CompanyResponse, JobResponse, Visibility } from "@/types/api"
import { companyKeys } from "./use-companies"
import { jobKeys } from "./use-jobs"
import { browseKeys } from "./use-browse"

export function useSetCompanyVisibility() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      id,
      visibility,
    }: {
      id: string
      visibility: Visibility
    }) =>
      apiClient<CompanyResponse>(`/companies/${id}/visibility`, {
        method: "PATCH",
        body: JSON.stringify({ visibility }),
      }),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: companyKeys.detail(variables.id),
      })
      queryClient.invalidateQueries({ queryKey: companyKeys.lists() })
      queryClient.invalidateQueries({ queryKey: browseKeys.all })
    },
  })
}

export function useSetJobVisibility() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      id,
      visibility,
    }: {
      id: string
      visibility: Visibility
    }) =>
      apiClient<JobResponse>(`/jobs/${id}/visibility`, {
        method: "PATCH",
        body: JSON.stringify({ visibility }),
      }),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: jobKeys.detail(variables.id),
      })
      queryClient.invalidateQueries({ queryKey: jobKeys.lists() })
      queryClient.invalidateQueries({ queryKey: browseKeys.all })
    },
  })
}
