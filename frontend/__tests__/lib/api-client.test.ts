import { describe, it, expect, vi, beforeEach } from "vitest"
import { apiClient, ApiError } from "@/lib/api-client"

const mockFetch = vi.fn()
vi.stubGlobal("fetch", mockFetch)

beforeEach(() => {
  mockFetch.mockReset()
})

describe("apiClient", () => {
  it("sends GET request to correct URL with default headers", async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      status: 200,
      json: () => Promise.resolve([{ id: "1", name: "Acme" }]),
    })

    const result = await apiClient("/companies")

    expect(mockFetch).toHaveBeenCalledWith(
      "http://localhost:8080/api/companies",
      expect.objectContaining({
        headers: expect.objectContaining({
          "Content-Type": "application/json",
        }),
        credentials: "include",
      }),
    )
    expect(result).toEqual([{ id: "1", name: "Acme" }])
  })

  it("throws ApiError on non-ok response", async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 404,
      json: () => Promise.resolve({ message: "Not found" }),
    })

    await expect(apiClient("/companies/999")).rejects.toThrow(ApiError)
    await expect(apiClient("/companies/999")).rejects.toMatchObject({
      status: 404,
      message: "Not found",
    })
  })

  it("returns undefined for 204 responses", async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      status: 204,
    })

    const result = await apiClient("/companies/1", { method: "DELETE" })
    expect(result).toBeUndefined()
  })

  it("merges custom headers with defaults", async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      status: 200,
      json: () => Promise.resolve({}),
    })

    await apiClient("/companies", {
      headers: { "X-Custom": "value" },
    })

    expect(mockFetch).toHaveBeenCalledWith(
      expect.any(String),
      expect.objectContaining({
        headers: expect.objectContaining({
          "Content-Type": "application/json",
          "X-Custom": "value",
        }),
      }),
    )
  })

  it("throws ApiError with fallback message on JSON parse failure", async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 500,
      json: () => Promise.reject(new Error("Invalid JSON")),
    })

    await expect(apiClient("/fail")).rejects.toMatchObject({
      status: 500,
      message: "Request failed",
    })
  })
})
