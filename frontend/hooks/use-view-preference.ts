"use client"

import { useState, useCallback, useEffect } from "react"

type ViewMode = "board" | "list"

export function useViewPreference(
  key: string,
  defaultView: ViewMode = "board",
): [ViewMode, (view: ViewMode) => void] {
  const [view, setViewState] = useState<ViewMode>(defaultView)

  // Hydrate from localStorage on mount (SSR-safe)
  useEffect(() => {
    if (typeof window === "undefined") return
    const stored = localStorage.getItem(key)
    if (stored === "board" || stored === "list") {
      setViewState(stored)
    }
  }, [key])

  const setView = useCallback(
    (newView: ViewMode) => {
      setViewState(newView)
      if (typeof window !== "undefined") {
        localStorage.setItem(key, newView)
      }
    },
    [key],
  )

  return [view, setView]
}
