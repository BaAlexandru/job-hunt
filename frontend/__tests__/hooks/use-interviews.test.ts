import { describe, it, expect, vi, beforeEach } from "vitest"
import { apiClient } from "@/lib/api-client"

const mockFetch = vi.fn()
vi.stubGlobal("fetch", mockFetch)

beforeEach(() => {
  mockFetch.mockReset()
})

const mockNote = {
  id: "note-1",
  interviewId: "interview-1",
  content: "Test note content",
  noteType: "GENERAL",
  createdAt: "2026-03-22T12:00:00Z",
  updatedAt: "2026-03-22T12:00:00Z",
}

describe("interview note API calls", () => {
  it("creates a note with noteType via POST", async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      status: 200,
      json: () => Promise.resolve(mockNote),
    })

    await apiClient("/interviews/interview-1/notes", {
      method: "POST",
      body: JSON.stringify({ content: "Test note", noteType: "PREPARATION" }),
    })

    expect(mockFetch).toHaveBeenCalledWith(
      "http://localhost:8080/api/interviews/interview-1/notes",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ content: "Test note", noteType: "PREPARATION" }),
      }),
    )
  })

  it("updates a note with content only via PUT", async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      status: 200,
      json: () => Promise.resolve({ ...mockNote, content: "Updated content" }),
    })

    await apiClient("/interviews/interview-1/notes/note-1", {
      method: "PUT",
      body: JSON.stringify({ content: "Updated content" }),
    })

    expect(mockFetch).toHaveBeenCalledWith(
      "http://localhost:8080/api/interviews/interview-1/notes/note-1",
      expect.objectContaining({
        method: "PUT",
        body: JSON.stringify({ content: "Updated content" }),
      }),
    )
  })

  it("update does not send noteType in body", async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      status: 200,
      json: () => Promise.resolve(mockNote),
    })

    const body = JSON.stringify({ content: "Updated" })
    await apiClient("/interviews/interview-1/notes/note-1", {
      method: "PUT",
      body,
    })

    const callBody = mockFetch.mock.calls[0][1].body
    const parsed = JSON.parse(callBody)
    expect(parsed).not.toHaveProperty("noteType")
    expect(parsed).toEqual({ content: "Updated" })
  })

  it("deletes a note via DELETE", async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      status: 204,
    })

    await apiClient("/interviews/interview-1/notes/note-1", {
      method: "DELETE",
    })

    expect(mockFetch).toHaveBeenCalledWith(
      "http://localhost:8080/api/interviews/interview-1/notes/note-1",
      expect.objectContaining({
        method: "DELETE",
      }),
    )
  })

  it("fetches notes list via GET", async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      status: 200,
      json: () =>
        Promise.resolve({
          content: [mockNote],
          totalElements: 1,
          totalPages: 1,
          number: 0,
          size: 20,
          first: true,
          last: true,
          empty: false,
        }),
    })

    const result = await apiClient("/interviews/interview-1/notes")

    expect(mockFetch).toHaveBeenCalledWith(
      "http://localhost:8080/api/interviews/interview-1/notes",
      expect.objectContaining({
        credentials: "include",
      }),
    )
    expect(result).toHaveProperty("content")
    expect((result as { content: unknown[] }).content).toHaveLength(1)
  })
})
