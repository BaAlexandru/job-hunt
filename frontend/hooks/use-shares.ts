"use client"

import {
  useQuery,
  useMutation,
  useQueryClient,
} from "@tanstack/react-query"
import { apiClient } from "@/lib/api-client"
import type { ShareResponse } from "@/types/api"

export const shareKeys = {
  all: ["shares"] as const,
  resource: (type: "companies" | "jobs", id: string) =>
    [...shareKeys.all, type, id] as const,
}

export function useShares(
  resourceType: "companies" | "jobs",
  resourceId: string,
) {
  return useQuery({
    queryKey: shareKeys.resource(resourceType, resourceId),
    queryFn: () =>
      apiClient<ShareResponse[]>(
        `/${resourceType}/${resourceId}/shares`,
      ),
    enabled: !!resourceId,
  })
}

export function useCreateShare(resourceType: "companies" | "jobs") {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      resourceId,
      email,
    }: {
      resourceId: string
      email: string
    }) =>
      apiClient<ShareResponse>(
        `/${resourceType}/${resourceId}/shares`,
        {
          method: "POST",
          body: JSON.stringify({ email }),
        },
      ),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: shareKeys.resource(resourceType, variables.resourceId),
      })
    },
  })
}

export function useRevokeShare(resourceType: "companies" | "jobs") {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      resourceId,
      shareId,
    }: {
      resourceId: string
      shareId: string
    }) =>
      apiClient<void>(
        `/${resourceType}/${resourceId}/shares/${shareId}`,
        {
          method: "DELETE",
        },
      ),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: shareKeys.resource(resourceType, variables.resourceId),
      })
    },
  })
}
