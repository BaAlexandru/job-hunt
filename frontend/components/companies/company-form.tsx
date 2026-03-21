"use client"

import { useEffect } from "react"
import { useForm } from "react-hook-form"
import { standardSchemaResolver } from "@hookform/resolvers/standard-schema"
import { z } from "zod"
import { toast } from "sonner"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Label } from "@/components/ui/label"
import { useCreateCompany, useUpdateCompany } from "@/hooks/use-companies"
import type { CompanyResponse } from "@/types/api"

const companySchema = z.object({
  name: z.string().min(1, "Name is required").max(255),
  website: z.union([z.string().url("Invalid URL").max(500), z.literal("")]).optional(),
  location: z.union([z.string().max(255), z.literal("")]).optional(),
  notes: z.union([z.string(), z.literal("")]).optional(),
})

type CompanyFormValues = z.infer<typeof companySchema>

interface CompanyFormProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  initialData?: CompanyResponse
}

export function CompanyForm({
  open,
  onOpenChange,
  initialData,
}: CompanyFormProps) {
  const isEditing = !!initialData
  const createCompany = useCreateCompany()
  const updateCompany = useUpdateCompany()

  const form = useForm<CompanyFormValues>({
    resolver: standardSchemaResolver(companySchema),
    defaultValues: {
      name: "",
      website: "",
      location: "",
      notes: "",
    },
  })

  useEffect(() => {
    if (open) {
      form.reset({
        name: initialData?.name ?? "",
        website: initialData?.website ?? "",
        location: initialData?.location ?? "",
        notes: initialData?.notes ?? "",
      })
    }
  }, [open, initialData, form])

  const onSubmit = (values: CompanyFormValues) => {
    const data = {
      name: values.name,
      website: values.website || undefined,
      location: values.location || undefined,
      notes: values.notes || undefined,
    }

    if (isEditing) {
      updateCompany.mutate(
        { id: initialData.id, data },
        {
          onSuccess: () => {
            toast.success("Company updated")
            onOpenChange(false)
          },
          onError: () => {
            toast.error("Failed to update company")
          },
        },
      )
    } else {
      createCompany.mutate(data, {
        onSuccess: () => {
          toast.success("Company added")
          onOpenChange(false)
        },
        onError: () => {
          toast.error("Failed to add company")
        },
      })
    }
  }

  const isPending = createCompany.isPending || updateCompany.isPending

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{isEditing ? "Edit Company" : "Add Company"}</DialogTitle>
          <DialogDescription>
            {isEditing
              ? "Update the company details below."
              : "Add a new company to track jobs."}
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={form.handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <div className="flex flex-col gap-2">
            <Label htmlFor="name">Name *</Label>
            <Input
              id="name"
              placeholder="Company name"
              {...form.register("name")}
              aria-invalid={!!form.formState.errors.name}
            />
            {form.formState.errors.name && (
              <p className="text-xs text-destructive">
                {form.formState.errors.name.message}
              </p>
            )}
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="website">Website</Label>
            <Input
              id="website"
              placeholder="https://example.com"
              {...form.register("website")}
              aria-invalid={!!form.formState.errors.website}
            />
            {form.formState.errors.website && (
              <p className="text-xs text-destructive">
                {form.formState.errors.website.message}
              </p>
            )}
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="location">Location</Label>
            <Input
              id="location"
              placeholder="City, Country"
              {...form.register("location")}
            />
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="notes">Notes</Label>
            <Textarea
              id="notes"
              placeholder="Any notes about this company..."
              {...form.register("notes")}
            />
          </div>
          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
            >
              {isEditing ? "Discard Changes" : "Close"}
            </Button>
            <Button type="submit" disabled={isPending}>
              {isEditing ? "Update Company" : "Add Company"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
