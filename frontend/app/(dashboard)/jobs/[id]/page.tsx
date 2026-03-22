"use client"

import { useState } from "react"
import { useParams, useRouter } from "next/navigation"
import {
  ArrowLeft,
  Building2,
  ExternalLink,
  Eye,
  MapPin,
  Pencil,
  Trash2,
} from "lucide-react"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { ConfirmDialog } from "@/components/shared/confirm-dialog"
import { VisibilityControl } from "@/components/shared/visibility-control"
import { ShareManager } from "@/components/shared/share-manager"
import { JobForm } from "@/components/jobs/job-form"
import { useJob, useDeleteJob } from "@/hooks/use-jobs"

function formatEnum(value: string | null): string {
  if (!value) return "--"
  return value
    .replace(/_/g, " ")
    .replace(/\b\w/g, (c) => c.toUpperCase())
}

function formatSalary(job: {
  salaryText: string | null
  salaryMin: number | null
  salaryMax: number | null
  currency: string | null
  salaryPeriod: string | null
}): string | null {
  if (job.salaryText) return job.salaryText
  if (job.salaryMin == null && job.salaryMax == null) return null

  const currency = job.currency ?? ""
  const period = job.salaryPeriod
    ? `/${job.salaryPeriod.toLowerCase()}`
    : ""

  const fmt = (n: number) => {
    if (n >= 1000) return `${Math.round(n / 1000)}k`
    return String(n)
  }

  if (job.salaryMin != null && job.salaryMax != null) {
    return `${currency}${fmt(job.salaryMin)}-${currency}${fmt(job.salaryMax)}${period}`
  }
  if (job.salaryMin != null) {
    return `${currency}${fmt(job.salaryMin)}+${period}`
  }
  return `${currency}${fmt(job.salaryMax!)}${period}`
}

export default function JobDetailPage() {
  const params = useParams<{ id: string }>()
  const router = useRouter()
  const jobId = params.id
  const { data: job, isLoading, isError } = useJob(jobId)
  const deleteJob = useDeleteJob()

  const [formOpen, setFormOpen] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(false)

  const isOwner = job?.isOwner !== false

  const handleDelete = () => {
    if (!job) return
    deleteJob.mutate(job.id, {
      onSuccess: () => {
        toast.success("Job deleted")
        router.push("/jobs")
      },
      onError: () => {
        toast.error("Failed to delete job")
      },
    })
  }

  if (isError) {
    return (
      <div className="flex flex-col items-center justify-center gap-2 py-12 text-center">
        <p className="text-sm text-muted-foreground">
          Could not load job. Check your connection and try again.
        </p>
      </div>
    )
  }

  if (isLoading) {
    return (
      <div className="flex flex-col gap-6">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-4 w-64" />
        <Skeleton className="h-64 w-full rounded-xl" />
      </div>
    )
  }

  if (!job) return null

  const salary = formatSalary(job)

  return (
    <div className="flex flex-col gap-6">
      {!isOwner && (
        <div className="flex items-center gap-2 rounded-md bg-muted px-4 py-2 text-sm text-muted-foreground">
          <Eye className="size-4" />
          {job.visibility === "PUBLIC"
            ? "You are viewing a public resource"
            : "You are viewing this as a shared resource"}
        </div>
      )}

      <div>
        <Button
          variant="ghost"
          size="sm"
          className="mb-4"
          onClick={() => router.push("/jobs")}
        >
          <ArrowLeft className="size-4" />
          Back to Jobs
        </Button>

        <div className="flex items-start justify-between">
          <div className="flex flex-col gap-1">
            <h1 className="text-xl font-semibold">{job.title}</h1>
            {job.companyName && (
              <div className="flex items-center gap-1 text-sm text-muted-foreground">
                <Building2 className="size-3.5" />
                {job.companyId ? (
                  <button
                    type="button"
                    className="hover:underline"
                    onClick={() => router.push(`/companies/${job.companyId}`)}
                  >
                    {job.companyName}
                  </button>
                ) : (
                  <span>{job.companyName}</span>
                )}
              </div>
            )}
            {job.location && (
              <div className="flex items-center gap-1 text-sm text-muted-foreground">
                <MapPin className="size-3.5" />
                {job.location}
              </div>
            )}
            {job.url && (
              <a
                href={job.url}
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center gap-1 text-sm text-muted-foreground hover:underline"
              >
                Job posting
                <ExternalLink className="size-3" />
              </a>
            )}
          </div>
          {isOwner && (
            <div className="flex gap-2">
              <Button variant="outline" size="sm" onClick={() => setFormOpen(true)}>
                <Pencil className="size-4" />
                Edit
              </Button>
              <Button
                variant="destructive"
                size="sm"
                onClick={() => setConfirmDelete(true)}
              >
                <Trash2 className="size-4" />
                Delete
              </Button>
            </div>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {job.jobType && (
          <div>
            <p className="text-xs font-medium text-muted-foreground">Type</p>
            <p className="text-sm">{formatEnum(job.jobType)}</p>
          </div>
        )}
        {job.workMode && (
          <div>
            <p className="text-xs font-medium text-muted-foreground">Work Mode</p>
            <p className="text-sm">{formatEnum(job.workMode)}</p>
          </div>
        )}
        {salary && (
          <div>
            <p className="text-xs font-medium text-muted-foreground">Salary</p>
            <p className="text-sm">{salary}</p>
          </div>
        )}
        {job.closingDate && (
          <div>
            <p className="text-xs font-medium text-muted-foreground">Closing Date</p>
            <p className="text-sm">
              {new Date(job.closingDate).toLocaleDateString()}
            </p>
          </div>
        )}
      </div>

      {job.description && (
        <div>
          <p className="mb-1 text-xs font-medium text-muted-foreground">
            Description
          </p>
          <p className="whitespace-pre-wrap text-sm">{job.description}</p>
        </div>
      )}

      {job.notes && isOwner && (
        <div>
          <p className="mb-1 text-xs font-medium text-muted-foreground">Notes</p>
          <p className="whitespace-pre-wrap text-sm">{job.notes}</p>
        </div>
      )}

      {isOwner && (
        <div className="flex flex-col gap-4">
          <VisibilityControl
            resourceType="jobs"
            resourceId={job.id}
            resourceName={job.title}
            currentVisibility={job.visibility}
          />
          <ShareManager
            resourceType="jobs"
            resourceId={job.id}
            resourceName={job.title}
          />
        </div>
      )}

      <JobForm
        open={formOpen}
        onOpenChange={setFormOpen}
        initialData={job}
      />

      <ConfirmDialog
        open={confirmDelete}
        onOpenChange={setConfirmDelete}
        title="Delete Job"
        description={`This will permanently delete "${job.title}". This cannot be undone.`}
        actionLabel="Delete"
        onConfirm={handleDelete}
      />
    </div>
  )
}
