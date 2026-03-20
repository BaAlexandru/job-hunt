const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api"

export class ApiError extends Error {
  constructor(
    public status: number,
    public data: Record<string, unknown>,
  ) {
    super((data.message as string) || "Request failed")
  }
}

export async function apiClient<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const headers: HeadersInit = {
    "Content-Type": "application/json",
    ...options.headers,
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
    credentials: "include",
  })

  if (!response.ok) {
    const error = await response
      .json()
      .catch(() => ({ message: "Request failed" }))
    throw new ApiError(response.status, error)
  }

  if (response.status === 204) return undefined as T
  return response.json()
}
