"use client"

import { useState } from "react"
import { useParams, useRouter } from "next/navigation"
import { ArrowLeft, ExternalLink, MapPin, Pencil, Trash2, Plus, Eye } from "lucide-react"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { ConfirmDialog } from "@/components/shared/confirm-dialog"
import { DataTable, type ColumnDef } from "@/components/shared/data-table"
import { CompanyForm } from "@/components/companies/company-form"
import { VisibilityControl } from "@/components/shared/visibility-control"
import { ShareManager } from "@/components/shared/share-manager"
import { useCompany, useDeleteCompany } from "@/hooks/use-companies"
import { useJobs } from "@/hooks/use-jobs"
import type { JobResponse } from "@/types/api"

const jobColumns: ColumnDef<JobResponse, unknown>[] = [
  {
    accessorKey: "title",
    header: "Title",
  },
  {
    accessorKey: "location",
    header: "Location",
    cell: ({ row }) => row.original.location || "--",
  },
  {
    accessorKey: "jobType",
    header: "Type",
    cell: ({ row }) =>
      row.original.jobType
        ? row.original.jobType.replace(/_/g, " ").replace(/\b\w/g, (c) => c.toUpperCase())
        : "--",
  },
  {
    accessorKey: "workMode",
    header: "Work Mode",
    cell: ({ row }) =>
      row.original.workMode
        ? row.original.workMode.replace(/_/g, " ").replace(/\b\w/g, (c) => c.toUpperCase())
        : "--",
  },
  {
    accessorKey: "createdAt",
    header: "Posted",
    cell: ({ row }) =>
      new Date(row.original.createdAt).toLocaleDateString(),
  },
]

export default function CompanyDetailPage() {
  const params = useParams<{ id: string }>()
  const router = useRouter()
  const companyId = params.id
  const { data: company, isLoading: companyLoading, isError } = useCompany(companyId)
  const { data: jobsData, isLoading: jobsLoading } = useJobs({ companyId })
  const deleteCompany = useDeleteCompany()

  const [formOpen, setFormOpen] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(false)

  const jobs = jobsData?.content ?? []
  const isOwner = company?.isOwner !== false

  const handleDelete = () => {
    if (!company) return
    deleteCompany.mutate(company.id, {
      onSuccess: () => {
        toast.success("Company deleted")
        router.push("/companies")
      },
      onError: () => {
        toast.error("Failed to delete company")
      },
    })
  }

  if (isError) {
    return (
      <div className="flex flex-col items-center justify-center gap-2 py-12 text-center">
        <p className="text-sm text-muted-foreground">
          Could not load company. Check your connection and try again.
        </p>
      </div>
    )
  }

  if (companyLoading) {
    return (
      <div className="flex flex-col gap-6">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-4 w-64" />
        <Skeleton className="h-64 w-full rounded-xl" />
      </div>
    )
  }

  if (!company) return null

  return (
    <div className="flex flex-col gap-6">
      {!isOwner && (
        <div className="flex items-center gap-2 rounded-md bg-muted px-4 py-2 text-sm text-muted-foreground">
          <Eye className="size-4" />
          {company.visibility === "PUBLIC"
            ? "You are viewing a public resource"
            : "You are viewing this as a shared resource"}
        </div>
      )}

      <div>
        <Button
          variant="ghost"
          size="sm"
          className="mb-4"
          onClick={() => router.push("/companies")}
        >
          <ArrowLeft className="size-4" />
          Back to Companies
        </Button>

        <div className="flex items-start justify-between">
          <div className="flex flex-col gap-1">
            <h1 className="text-xl font-semibold">{company.name}</h1>
            {company.website && (
              <a
                href={company.website}
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center gap-1 text-sm text-muted-foreground hover:underline"
              >
                {company.website}
                <ExternalLink className="size-3" />
              </a>
            )}
            {company.location && (
              <div className="flex items-center gap-1 text-sm text-muted-foreground">
                <MapPin className="size-3.5" />
                {company.location}
              </div>
            )}
            {company.notes && (
              <p className="mt-2 text-sm text-muted-foreground">{company.notes}</p>
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

      {isOwner && (
        <div className="flex flex-col gap-4">
          <VisibilityControl
            resourceType="companies"
            resourceId={company.id}
            resourceName={company.name}
            currentVisibility={company.visibility}
          />
          <ShareManager
            resourceType="companies"
            resourceId={company.id}
            resourceName={company.name}
          />
        </div>
      )}

      {isOwner && (
        <div className="flex flex-col gap-4">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold">
              Jobs at {company.name}
            </h2>
            <Button
              size="sm"
              onClick={() => router.push(`/jobs?companyId=${company.id}`)}
            >
              <Plus className="size-4" />
              Add Job
            </Button>
          </div>

          {jobsLoading ? (
            <Skeleton className="h-48 w-full rounded-xl" />
          ) : (
            <DataTable columns={jobColumns} data={jobs} />
          )}
        </div>
      )}

      <CompanyForm
        open={formOpen}
        onOpenChange={setFormOpen}
        initialData={company}
      />

      <ConfirmDialog
        open={confirmDelete}
        onOpenChange={setConfirmDelete}
        title="Delete Company"
        description={`This will permanently delete ${company.name} and unlink all associated jobs. This cannot be undone.`}
        actionLabel="Delete"
        onConfirm={handleDelete}
      />
    </div>
  )
}
