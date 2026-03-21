"use client"

import { useState, useMemo } from "react"
import { useRouter } from "next/navigation"
import { Plus } from "lucide-react"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { EmptyState } from "@/components/shared/empty-state"
import { ConfirmDialog } from "@/components/shared/confirm-dialog"
import { CompanyCard } from "@/components/companies/company-card"
import { CompanyForm } from "@/components/companies/company-form"
import { useCompanies, useDeleteCompany } from "@/hooks/use-companies"
import { useJobs } from "@/hooks/use-jobs"
import type { CompanyResponse } from "@/types/api"

export default function CompaniesPage() {
  const router = useRouter()
  const { data: companiesData, isLoading, isError } = useCompanies({ size: 1000 })
  const { data: jobsData } = useJobs({ size: 1000 })
  const deleteCompany = useDeleteCompany()

  const [formOpen, setFormOpen] = useState(false)
  const [editingCompany, setEditingCompany] = useState<CompanyResponse | undefined>()
  const [deleteTarget, setDeleteTarget] = useState<CompanyResponse | undefined>()

  const companies = companiesData?.content ?? []

  const jobCountMap = useMemo(() => {
    const map = new Map<string, number>()
    if (jobsData?.content) {
      for (const job of jobsData.content) {
        if (job.companyId) {
          map.set(job.companyId, (map.get(job.companyId) || 0) + 1)
        }
      }
    }
    return map
  }, [jobsData])

  const handleEdit = (company: CompanyResponse) => {
    setEditingCompany(company)
    setFormOpen(true)
  }

  const handleDelete = (company: CompanyResponse) => {
    setDeleteTarget(company)
  }

  const confirmDelete = () => {
    if (!deleteTarget) return
    deleteCompany.mutate(deleteTarget.id, {
      onSuccess: () => {
        toast.success("Company deleted")
        setDeleteTarget(undefined)
      },
      onError: () => {
        toast.error("Failed to delete company")
      },
    })
  }

  const handleAdd = () => {
    setEditingCompany(undefined)
    setFormOpen(true)
  }

  if (isError) {
    return (
      <div className="flex flex-col items-center justify-center gap-2 py-12 text-center">
        <p className="text-sm text-muted-foreground">
          Could not load companies. Check your connection and try again.
        </p>
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-xl font-semibold">Companies</h1>
        <Button onClick={handleAdd} className="w-full sm:w-auto">
          <Plus />
          Add Company
        </Button>
      </div>

      {isLoading ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-36 rounded-xl" />
          ))}
        </div>
      ) : companies.length === 0 ? (
        <EmptyState
          heading="No companies yet"
          body="Add a company to start organizing your job search."
        />
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {companies.map((company) => (
            <CompanyCard
              key={company.id}
              company={company}
              jobCount={jobCountMap.get(company.id) || 0}
              onClick={() => router.push(`/companies/${company.id}`)}
              onEdit={() => handleEdit(company)}
              onDelete={() => handleDelete(company)}
            />
          ))}
        </div>
      )}

      <CompanyForm
        open={formOpen}
        onOpenChange={setFormOpen}
        initialData={editingCompany}
      />

      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={(open) => {
          if (!open) setDeleteTarget(undefined)
        }}
        title="Delete Company"
        description={`This will permanently delete ${deleteTarget?.name ?? "this company"} and unlink all associated jobs. This cannot be undone.`}
        actionLabel="Delete"
        onConfirm={confirmDelete}
      />
    </div>
  )
}
