"use client"

import { useState } from "react"
import { Plus } from "lucide-react"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { EmptyState } from "@/components/shared/empty-state"
import { ConfirmDialog } from "@/components/shared/confirm-dialog"
import { JobList } from "@/components/jobs/job-list"
import { JobForm } from "@/components/jobs/job-form"
import { useJobs, useDeleteJob } from "@/hooks/use-jobs"
import { useCompanies } from "@/hooks/use-companies"
import type { JobResponse } from "@/types/api"

const ALL_COMPANIES = "__all__"

export default function JobsPage() {
  const [companyFilter, setCompanyFilter] = useState<string>(ALL_COMPANIES)
  const [page, setPage] = useState(0)
  const [formOpen, setFormOpen] = useState(false)
  const [editingJob, setEditingJob] = useState<JobResponse | undefined>()
  const [deleteTarget, setDeleteTarget] = useState<JobResponse | undefined>()

  const companyId = companyFilter === ALL_COMPANIES ? undefined : companyFilter

  const { data: jobsData, isLoading, isError } = useJobs({
    companyId,
    page,
    size: 20,
  })
  const { data: companiesData } = useCompanies({ size: 1000 })
  const deleteJob = useDeleteJob()

  const companies = companiesData?.content ?? []
  const jobs = jobsData?.content ?? []
  const totalPages = jobsData?.totalPages ?? 0

  const handleEdit = (job: JobResponse) => {
    setEditingJob(job)
    setFormOpen(true)
  }

  const handleDelete = (job: JobResponse) => {
    setDeleteTarget(job)
  }

  const confirmDelete = () => {
    if (!deleteTarget) return
    deleteJob.mutate(deleteTarget.id, {
      onSuccess: () => {
        toast.success("Job deleted")
        setDeleteTarget(undefined)
      },
      onError: () => {
        toast.error("Failed to delete job")
      },
    })
  }

  const handleAdd = () => {
    setEditingJob(undefined)
    setFormOpen(true)
  }

  const handleCompanyChange = (value: string) => {
    setCompanyFilter(value)
    setPage(0)
  }

  if (isError) {
    return (
      <div className="flex flex-col items-center justify-center gap-2 py-12 text-center">
        <p className="text-sm text-muted-foreground">
          Could not load jobs. Check your connection and try again.
        </p>
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">Jobs</h1>
        <Button onClick={handleAdd}>
          <Plus />
          Add Job
        </Button>
      </div>

      <div className="flex items-center gap-4">
        <Select value={companyFilter} onValueChange={handleCompanyChange}>
          <SelectTrigger className="w-[200px]">
            <SelectValue placeholder="All Companies" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL_COMPANIES}>All Companies</SelectItem>
            {companies.map((company) => (
              <SelectItem key={company.id} value={company.id}>
                {company.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {isLoading ? (
        <div className="flex flex-col gap-2">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-12 w-full rounded-lg" />
          ))}
        </div>
      ) : jobs.length === 0 ? (
        <EmptyState
          heading="No jobs yet"
          body="Add a job posting to track opportunities you find."
        />
      ) : (
        <JobList
          jobs={jobs}
          isLoading={false}
          onEdit={handleEdit}
          onDelete={handleDelete}
          pagination={{
            page,
            totalPages,
            onPageChange: setPage,
          }}
        />
      )}

      <JobForm
        open={formOpen}
        onOpenChange={setFormOpen}
        initialData={editingJob}
      />

      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={(open) => {
          if (!open) setDeleteTarget(undefined)
        }}
        title="Delete Job"
        description={`This will permanently delete "${deleteTarget?.title ?? "this job"}". This cannot be undone.`}
        actionLabel="Delete"
        onConfirm={confirmDelete}
      />
    </div>
  )
}
