"use client"

import {
  useQuery,
  useMutation,
  useQueryClient,
} from "@tanstack/react-query"
import { apiClient, API_BASE } from "@/lib/api-client"
import type {
  DocumentApplicationLinkResponse,
  DocumentResponse,
  DocumentUpdateRequest,
  DocumentVersionResponse,
  LinkDocumentRequest,
  PaginatedResponse,
} from "@/types/api"

// ---------------------------------------------------------------------------
// Filters
// ---------------------------------------------------------------------------

export interface DocumentFilters {
  category?: string
  search?: string
  page?: number
  size?: number
}

// ---------------------------------------------------------------------------
// Query key factory
// ---------------------------------------------------------------------------

export const documentKeys = {
  all: ["documents"] as const,
  lists: () => [...documentKeys.all, "list"] as const,
  list: (filters: DocumentFilters) =>
    [...documentKeys.lists(), filters] as const,
  details: () => [...documentKeys.all, "detail"] as const,
  detail: (id: string) => [...documentKeys.details(), id] as const,
  versions: (documentId: string) =>
    [...documentKeys.detail(documentId), "versions"] as const,
  links: () => [...documentKeys.all, "links"] as const,
  linksForApp: (applicationId: string) =>
    [...documentKeys.links(), { applicationId }] as const,
}

// ---------------------------------------------------------------------------
// Queries
// ---------------------------------------------------------------------------

export function useDocuments(filters: DocumentFilters = {}) {
  const params = new URLSearchParams()
  if (filters.category) params.set("category", filters.category)
  if (filters.search) params.set("search", filters.search)
  if (filters.page !== undefined) params.set("page", String(filters.page))
  if (filters.size !== undefined) params.set("size", String(filters.size))
  const qs = params.toString()

  return useQuery({
    queryKey: documentKeys.list(filters),
    queryFn: () =>
      apiClient<PaginatedResponse<DocumentResponse>>(
        `/documents${qs ? `?${qs}` : ""}`,
      ),
  })
}

export function useDocument(id: string) {
  return useQuery({
    queryKey: documentKeys.detail(id),
    queryFn: () => apiClient<DocumentResponse>(`/documents/${id}`),
    enabled: !!id,
  })
}

// ---------------------------------------------------------------------------
// Document CRUD mutations
// ---------------------------------------------------------------------------

export function useUploadDocument() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (formData: FormData) => {
      // Use fetch directly for FormData to avoid Content-Type collision
      const response = await fetch(`${API_BASE}/documents`, {
        method: "POST",
        body: formData,
        credentials: "include",
      })
      if (!response.ok) {
        const error = await response
          .json()
          .catch(() => ({ message: "Upload failed" }))
        throw new Error(
          (error.message as string) || "Upload failed",
        )
      }
      return response.json() as Promise<DocumentResponse>
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: documentKeys.lists() })
    },
  })
}

export function useUpdateDocument() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string
      data: DocumentUpdateRequest
    }) =>
      apiClient<DocumentResponse>(`/documents/${id}`, {
        method: "PUT",
        body: JSON.stringify(data),
      }),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: documentKeys.detail(variables.id),
      })
      queryClient.invalidateQueries({ queryKey: documentKeys.lists() })
    },
  })
}

export function useDeleteDocument() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) =>
      apiClient<void>(`/documents/${id}`, { method: "DELETE" }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: documentKeys.lists() })
    },
  })
}

// ---------------------------------------------------------------------------
// Document Versions
// ---------------------------------------------------------------------------

export function useDocumentVersions(documentId: string) {
  return useQuery({
    queryKey: documentKeys.versions(documentId),
    queryFn: () =>
      apiClient<DocumentVersionResponse[]>(
        `/documents/${documentId}/versions`,
      ),
    enabled: !!documentId,
  })
}

export function useCreateDocumentVersion() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({
      documentId,
      formData,
    }: {
      documentId: string
      formData: FormData
    }) => {
      const response = await fetch(
        `${API_BASE}/documents/${documentId}/versions`,
        {
          method: "POST",
          body: formData,
          credentials: "include",
        },
      )
      if (!response.ok) {
        const error = await response
          .json()
          .catch(() => ({ message: "Upload failed" }))
        throw new Error(
          (error.message as string) || "Upload failed",
        )
      }
      return response.json() as Promise<DocumentVersionResponse>
    },
    onSuccess: (_data, { documentId }) => {
      queryClient.invalidateQueries({
        queryKey: documentKeys.versions(documentId),
      })
      queryClient.invalidateQueries({
        queryKey: documentKeys.detail(documentId),
      })
      queryClient.invalidateQueries({ queryKey: documentKeys.lists() })
    },
  })
}

export function useSetCurrentVersion() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      documentId,
      versionId,
    }: {
      documentId: string
      versionId: string
    }) =>
      apiClient<DocumentVersionResponse>(
        `/documents/${documentId}/versions/${versionId}/current`,
        { method: "PUT" },
      ),
    onSuccess: (_data, { documentId }) => {
      queryClient.invalidateQueries({
        queryKey: documentKeys.versions(documentId),
      })
      queryClient.invalidateQueries({
        queryKey: documentKeys.detail(documentId),
      })
    },
  })
}

export function getDownloadVersionUrl(
  documentId: string,
  versionId: string,
): string {
  return `${API_BASE}/documents/${documentId}/versions/${versionId}/download`
}

export function useDeleteDocumentVersion() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      documentId,
      versionId,
    }: {
      documentId: string
      versionId: string
    }) =>
      apiClient<void>(
        `/documents/${documentId}/versions/${versionId}`,
        { method: "DELETE" },
      ),
    onSuccess: (_data, { documentId }) => {
      queryClient.invalidateQueries({
        queryKey: documentKeys.versions(documentId),
      })
      queryClient.invalidateQueries({
        queryKey: documentKeys.detail(documentId),
      })
      queryClient.invalidateQueries({ queryKey: documentKeys.lists() })
    },
  })
}

// ---------------------------------------------------------------------------
// Document-Application Links
// ---------------------------------------------------------------------------

export function useDocumentLinksForApplication(applicationId: string) {
  return useQuery({
    queryKey: documentKeys.linksForApp(applicationId),
    queryFn: () =>
      apiClient<DocumentApplicationLinkResponse[]>(
        `/documents/links/application/${applicationId}`,
      ),
    enabled: !!applicationId,
  })
}

export function useLinkDocumentToApplication() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: LinkDocumentRequest) =>
      apiClient<DocumentApplicationLinkResponse>("/documents/links", {
        method: "POST",
        body: JSON.stringify(data),
      }),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: documentKeys.linksForApp(variables.applicationId),
      })
    },
  })
}

export function useUnlinkDocument() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      documentVersionId,
      applicationId,
    }: {
      documentVersionId: string
      applicationId: string
    }) =>
      apiClient<void>(
        `/documents/links?${new URLSearchParams({ documentVersionId, applicationId })}`,
        { method: "DELETE" },
      ),
    onSuccess: (_data, { applicationId }) => {
      queryClient.invalidateQueries({
        queryKey: documentKeys.linksForApp(applicationId),
      })
    },
  })
}
