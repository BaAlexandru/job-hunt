"use client"

import {
  useQuery,
  useMutation,
  useQueryClient,
} from "@tanstack/react-query"
import { apiClient } from "@/lib/api-client"
import type {
  CreateInterviewRequest,
  InterviewNoteResponse,
  InterviewResponse,
  PaginatedResponse,
  UpdateInterviewRequest,
} from "@/types/api"
import { applicationKeys } from "@/hooks/use-applications"

// ---------------------------------------------------------------------------
// Query key factory
// ---------------------------------------------------------------------------

export const interviewKeys = {
  all: ["interviews"] as const,
  lists: () => [...interviewKeys.all, "list"] as const,
  listByApp: (applicationId: string) =>
    [...interviewKeys.lists(), { applicationId }] as const,
  details: () => [...interviewKeys.all, "detail"] as const,
  detail: (id: string) => [...interviewKeys.details(), id] as const,
  notes: (interviewId: string) =>
    [...interviewKeys.detail(interviewId), "notes"] as const,
}

// ---------------------------------------------------------------------------
// Queries
// ---------------------------------------------------------------------------

export function useInterviews(applicationId: string) {
  return useQuery({
    queryKey: interviewKeys.listByApp(applicationId),
    queryFn: () =>
      apiClient<PaginatedResponse<InterviewResponse>>(
        `/interviews?applicationId=${applicationId}`,
      ),
    enabled: !!applicationId,
  })
}

export function useInterview(id: string) {
  return useQuery({
    queryKey: interviewKeys.detail(id),
    queryFn: () => apiClient<InterviewResponse>(`/interviews/${id}`),
    enabled: !!id,
  })
}

// ---------------------------------------------------------------------------
// Mutations
// ---------------------------------------------------------------------------

export function useCreateInterview() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateInterviewRequest) =>
      apiClient<InterviewResponse>("/interviews", {
        method: "POST",
        body: JSON.stringify(data),
      }),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: interviewKeys.listByApp(variables.applicationId),
      })
      queryClient.invalidateQueries({
        queryKey: applicationKeys.timeline(variables.applicationId),
      })
    },
  })
}

export function useUpdateInterview() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string
      applicationId: string
      data: UpdateInterviewRequest
    }) =>
      apiClient<InterviewResponse>(`/interviews/${id}`, {
        method: "PUT",
        body: JSON.stringify(data),
      }),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: interviewKeys.detail(variables.id),
      })
      queryClient.invalidateQueries({
        queryKey: interviewKeys.listByApp(variables.applicationId),
      })
    },
  })
}

export function useDeleteInterview() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      id,
    }: {
      id: string
      applicationId: string
    }) =>
      apiClient<void>(`/interviews/${id}`, { method: "DELETE" }),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: interviewKeys.listByApp(variables.applicationId),
      })
      queryClient.invalidateQueries({
        queryKey: applicationKeys.timeline(variables.applicationId),
      })
    },
  })
}

// ---------------------------------------------------------------------------
// Interview Notes
// ---------------------------------------------------------------------------

export function useInterviewNotes(interviewId: string) {
  return useQuery({
    queryKey: interviewKeys.notes(interviewId),
    queryFn: async () => {
      const page = await apiClient<PaginatedResponse<InterviewNoteResponse>>(
        `/interviews/${interviewId}/notes`,
      )
      return page.content
    },
    enabled: !!interviewId,
  })
}

export function useCreateInterviewNote() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      interviewId,
      content,
      noteType,
    }: {
      interviewId: string
      content: string
      noteType?: string
    }) =>
      apiClient<InterviewNoteResponse>(
        `/interviews/${interviewId}/notes`,
        { method: "POST", body: JSON.stringify({ content, noteType }) },
      ),
    onSuccess: (_data, { interviewId }) => {
      queryClient.invalidateQueries({
        queryKey: interviewKeys.notes(interviewId),
      })
    },
  })
}

export function useUpdateInterviewNote() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      interviewId,
      noteId,
      content,
    }: {
      interviewId: string
      noteId: string
      content: string
    }) =>
      apiClient<InterviewNoteResponse>(
        `/interviews/${interviewId}/notes/${noteId}`,
        { method: "PUT", body: JSON.stringify({ content }) },
      ),
    onSuccess: (_data, { interviewId }) => {
      queryClient.invalidateQueries({
        queryKey: interviewKeys.notes(interviewId),
      })
    },
  })
}

export function useDeleteInterviewNote() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      interviewId,
      noteId,
    }: {
      interviewId: string
      noteId: string
    }) =>
      apiClient<void>(
        `/interviews/${interviewId}/notes/${noteId}`,
        { method: "DELETE" },
      ),
    onSuccess: (_data, { interviewId }) => {
      queryClient.invalidateQueries({
        queryKey: interviewKeys.notes(interviewId),
      })
    },
  })
}
