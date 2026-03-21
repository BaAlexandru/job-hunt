"use client"

import { useMemo } from "react"
import Link from "next/link"
import { useApplications } from "@/hooks/use-applications"
import { EmptyState } from "@/components/shared/empty-state"
import { StatusBadge } from "@/components/applications/status-badge"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import {
  APPLICATION_STATUSES,
  STATUS_LABELS,
  type ApplicationStatus,
} from "@/types/api"

// ---------------------------------------------------------------------------
// Terminal statuses (not counted as "active")
// ---------------------------------------------------------------------------

const TERMINAL_STATUSES: ApplicationStatus[] = [
  "REJECTED",
  "ACCEPTED",
  "WITHDRAWN",
]

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function relativeTime(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime()
  const minutes = Math.floor(diff / 60_000)
  if (minutes < 60) return `${minutes}m ago`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}h ago`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days}d ago`
  return new Date(iso).toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
  })
}

// ---------------------------------------------------------------------------
// Metric card
// ---------------------------------------------------------------------------

function MetricCard({
  label,
  value,
  isLoading,
}: {
  label: string
  value: number | string
  isLoading: boolean
}) {
  return (
    <Card>
      <CardContent className="flex flex-col items-center justify-center py-6">
        {isLoading ? (
          <Skeleton className="mb-2 h-8 w-16" />
        ) : (
          <span className="text-[28px] font-semibold leading-none">
            {value}
          </span>
        )}
        <span className="mt-1 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
          {label}
        </span>
      </CardContent>
    </Card>
  )
}

// ---------------------------------------------------------------------------
// Dashboard page
// ---------------------------------------------------------------------------

export default function DashboardPage() {
  const { data, isLoading, isError } = useApplications({ size: 1000 })

  const applications = useMemo(() => data?.content ?? [], [data])

  const metrics = useMemo(() => {
    const total = applications.length
    const active = applications.filter(
      (a) => !TERMINAL_STATUSES.includes(a.status),
    ).length
    const offers = applications.filter((a) => a.status === "OFFER").length
    const interviews = applications.filter(
      (a) => a.status === "INTERVIEW",
    ).length

    const byStatus = APPLICATION_STATUSES.map((status) => ({
      status,
      label: STATUS_LABELS[status],
      count: applications.filter((a) => a.status === status).length,
    })).filter((s) => s.count > 0)

    const recent = [...applications]
      .sort(
        (a, b) =>
          new Date(b.lastActivityDate).getTime() -
          new Date(a.lastActivityDate).getTime(),
      )
      .slice(0, 5)

    return { total, active, offers, interviews, byStatus, recent }
  }, [applications])

  if (!isLoading && !isError && applications.length === 0) {
    return (
      <EmptyState
        heading="Welcome to JobHunt"
        body="Start by adding a company and creating your first application."
      />
    )
  }

  if (isError) {
    return (
      <div className="flex flex-col items-center gap-2 py-12 text-center">
        <p className="text-sm text-muted-foreground">
          Could not load dashboard data. Check your connection and try again.
        </p>
      </div>
    )
  }

  return (
    <div className="space-y-8">
      {/* Page heading */}
      <h1 className="text-xl font-semibold">Dashboard</h1>

      {/* Top metric cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <MetricCard
          label="Total Applications"
          value={metrics.total}
          isLoading={isLoading}
        />
        <MetricCard
          label="Active"
          value={metrics.active}
          isLoading={isLoading}
        />
        <MetricCard
          label="Offers"
          value={metrics.offers}
          isLoading={isLoading}
        />
        <MetricCard
          label="Interviews"
          value={metrics.interviews}
          isLoading={isLoading}
        />
      </div>

      {/* Status breakdown */}
      {!isLoading && metrics.byStatus.length > 0 && (
        <section>
          <CardHeader className="px-0">
            <CardTitle>Status Breakdown</CardTitle>
          </CardHeader>
          <div className="space-y-2">
            {metrics.byStatus.map(({ status, label, count }) => (
              <div
                key={status}
                className="flex items-center justify-between rounded-md border px-3 py-2"
              >
                <div className="flex items-center gap-2">
                  <StatusBadge status={status} />
                  <span className="text-sm">{label}</span>
                </div>
                <span className="text-sm font-semibold">{count}</span>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* Recent activity */}
      {!isLoading && metrics.recent.length > 0 && (
        <section>
          <CardHeader className="px-0">
            <CardTitle>Recent Activity</CardTitle>
          </CardHeader>
          <div className="space-y-2">
            {metrics.recent.map((app) => (
              <Link
                key={app.id}
                href="/applications"
                className="flex items-center justify-between rounded-md border px-3 py-2 transition-colors hover:bg-muted/50"
              >
                <div className="flex items-center gap-3">
                  <div>
                    <p className="text-sm font-medium">{app.jobTitle}</p>
                    {app.companyName && (
                      <p className="text-xs text-muted-foreground">
                        {app.companyName}
                      </p>
                    )}
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <StatusBadge status={app.status} />
                  <span className="text-xs text-muted-foreground">
                    {relativeTime(app.lastActivityDate)}
                  </span>
                </div>
              </Link>
            ))}
          </div>
        </section>
      )}
    </div>
  )
}
