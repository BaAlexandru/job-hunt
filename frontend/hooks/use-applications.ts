"use client"

import {
  useQuery,
  useMutation,
  useQueryClient,
} from "@tanstack/react-query"
import { apiClient } from "@/lib/api-client"
import type {
  ApplicationResponse,
  ApplicationStatus,
  CreateApplicationRequest,
  NoteResponse,
  PaginatedResponse,
  TimelineEntry,
  UpdateApplicationRequest,
} from "@/types/api"

// ---------------------------------------------------------------------------
// Filters
// ---------------------------------------------------------------------------

export interface ApplicationFilters {
  status?: string
  companyId?: string
  q?: string
  dateFrom?: string
  dateTo?: string
  page?: number
  size?: number
  sort?: string
  includeArchived?: boolean
}

// ---------------------------------------------------------------------------
// Query key factory
// ---------------------------------------------------------------------------

export const applicationKeys = {
  all: ["applications"] as const,
  lists: () => [...applicationKeys.all, "list"] as const,
  list: (filters: ApplicationFilters) =>
    [...applicationKeys.lists(), filters] as const,
  details: () => [...applicationKeys.all, "detail"] as const,
  detail: (id: string) => [...applicationKeys.details(), id] as const,
  transitions: (id: string) =>
    [...applicationKeys.detail(id), "transitions"] as const,
  notes: (applicationId: string) =>
    [...applicationKeys.detail(applicationId), "notes"] as const,
  timeline: (applicationId: string) =>
    [...applicationKeys.detail(applicationId), "timeline"] as const,
}

// ---------------------------------------------------------------------------
// Queries
// ---------------------------------------------------------------------------

export function useApplications(filters: ApplicationFilters = {}) {
  const params = new URLSearchParams()
  if (filters.status) params.set("status", filters.status)
  if (filters.companyId) params.set("companyId", filters.companyId)
  if (filters.q) params.set("q", filters.q)
  if (filters.dateFrom) params.set("dateFrom", filters.dateFrom)
  if (filters.dateTo) params.set("dateTo", filters.dateTo)
  if (filters.page !== undefined) params.set("page", String(filters.page))
  if (filters.size !== undefined) params.set("size", String(filters.size))
  if (filters.sort) params.set("sort", filters.sort)
  if (filters.includeArchived) params.set("includeArchived", "true")
  const qs = params.toString()

  return useQuery({
    queryKey: applicationKeys.list(filters),
    queryFn: () =>
      apiClient<PaginatedResponse<ApplicationResponse>>(
        `/applications${qs ? `?${qs}` : ""}`,
      ),
  })
}

export function useApplication(id: string) {
  return useQuery({
    queryKey: applicationKeys.detail(id),
    queryFn: () => apiClient<ApplicationResponse>(`/applications/${id}`),
    enabled: !!id,
  })
}

export function useApplicationTransitions(id: string) {
  return useQuery({
    queryKey: applicationKeys.transitions(id),
    queryFn: () =>
      apiClient<ApplicationStatus[]>(`/applications/${id}/transitions`),
    enabled: !!id,
  })
}

// ---------------------------------------------------------------------------
// Mutations
// ---------------------------------------------------------------------------

export function useCreateApplication() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateApplicationRequest) =>
      apiClient<ApplicationResponse>("/applications", {
        method: "POST",
        body: JSON.stringify(data),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: applicationKeys.lists() })
    },
  })
}

export function useUpdateApplication() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string
      data: UpdateApplicationRequest
    }) =>
      apiClient<ApplicationResponse>(`/applications/${id}`, {
        method: "PUT",
        body: JSON.stringify(data),
      }),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: applicationKeys.detail(variables.id),
      })
      queryClient.invalidateQueries({ queryKey: applicationKeys.lists() })
    },
  })
}

export function useUpdateApplicationStatus() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: ApplicationStatus }) =>
      apiClient<ApplicationResponse>(`/applications/${id}/status`, {
        method: "PATCH",
        body: JSON.stringify({ status }),
      }),
    onMutate: async ({ id, status }) => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries({ queryKey: applicationKeys.lists() })
      await queryClient.cancelQueries({
        queryKey: applicationKeys.detail(id),
      })

      // Snapshot previous value
      const previousDetail = queryClient.getQueryData<ApplicationResponse>(
        applicationKeys.detail(id),
      )

      // Optimistically update detail cache
      if (previousDetail) {
        queryClient.setQueryData<ApplicationResponse>(
          applicationKeys.detail(id),
          { ...previousDetail, status },
        )
      }

      return { previousDetail }
    },
    onError: (_err, { id }, context) => {
      // Rollback on error
      if (context?.previousDetail) {
        queryClient.setQueryData(
          applicationKeys.detail(id),
          context.previousDetail,
        )
      }
    },
    onSettled: (_data, _err, { id }) => {
      queryClient.invalidateQueries({ queryKey: applicationKeys.lists() })
      queryClient.invalidateQueries({
        queryKey: applicationKeys.detail(id),
      })
      queryClient.invalidateQueries({
        queryKey: applicationKeys.transitions(id),
      })
    },
  })
}

export function useDeleteApplication() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) =>
      apiClient<void>(`/applications/${id}`, { method: "DELETE" }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: applicationKeys.lists() })
    },
  })
}

// ---------------------------------------------------------------------------
// Application Notes
// ---------------------------------------------------------------------------

export function useApplicationNotes(applicationId: string) {
  return useQuery({
    queryKey: applicationKeys.notes(applicationId),
    queryFn: async () => {
      const page = await apiClient<PaginatedResponse<NoteResponse>>(
        `/applications/${applicationId}/notes`,
      )
      return page.content
    },
    enabled: !!applicationId,
  })
}

export function useCreateApplicationNote() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      applicationId,
      content,
    }: {
      applicationId: string
      content: string
    }) =>
      apiClient<NoteResponse>(
        `/applications/${applicationId}/notes`,
        { method: "POST", body: JSON.stringify({ content }) },
      ),
    onSuccess: (_data, { applicationId }) => {
      queryClient.invalidateQueries({
        queryKey: applicationKeys.notes(applicationId),
      })
      queryClient.invalidateQueries({
        queryKey: applicationKeys.timeline(applicationId),
      })
    },
  })
}

export function useUpdateApplicationNote() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      applicationId,
      noteId,
      content,
    }: {
      applicationId: string
      noteId: string
      content: string
    }) =>
      apiClient<NoteResponse>(
        `/applications/${applicationId}/notes/${noteId}`,
        { method: "PUT", body: JSON.stringify({ content }) },
      ),
    onSuccess: (_data, { applicationId }) => {
      queryClient.invalidateQueries({
        queryKey: applicationKeys.notes(applicationId),
      })
    },
  })
}

export function useDeleteApplicationNote() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      applicationId,
      noteId,
    }: {
      applicationId: string
      noteId: string
    }) =>
      apiClient<void>(
        `/applications/${applicationId}/notes/${noteId}`,
        { method: "DELETE" },
      ),
    onSuccess: (_data, { applicationId }) => {
      queryClient.invalidateQueries({
        queryKey: applicationKeys.notes(applicationId),
      })
      queryClient.invalidateQueries({
        queryKey: applicationKeys.timeline(applicationId),
      })
    },
  })
}

// ---------------------------------------------------------------------------
// Timeline
// ---------------------------------------------------------------------------

export function useTimeline(applicationId: string) {
  return useQuery({
    queryKey: applicationKeys.timeline(applicationId),
    queryFn: () =>
      apiClient<TimelineEntry[]>(
        `/applications/${applicationId}/timeline`,
      ),
    enabled: !!applicationId,
  })
}
