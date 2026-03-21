"use client"

import { useEffect } from "react"
import { useForm, Controller } from "react-hook-form"
import { standardSchemaResolver } from "@hookform/resolvers/standard-schema"
import { z } from "zod"
import { format } from "date-fns"
import { toast } from "sonner"
import { CalendarIcon, ChevronsUpDown, Check } from "lucide-react"
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover"
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from "@/components/ui/command"
import { Calendar } from "@/components/ui/calendar"
import { cn } from "@/lib/utils"
import { useCreateJob, useUpdateJob } from "@/hooks/use-jobs"
import { useCompanies } from "@/hooks/use-companies"
import type { JobResponse } from "@/types/api"

const jobSchema = z.object({
  title: z.string().min(1, "Title is required").max(255),
  description: z.string().optional().or(z.literal("")),
  url: z.string().url("Invalid URL").max(500).optional().or(z.literal("")),
  notes: z.string().optional().or(z.literal("")),
  location: z.string().max(255).optional().or(z.literal("")),
  workMode: z.string().optional(),
  jobType: z.string().optional(),
  companyId: z.string().optional(),
  salaryType: z.string().optional(),
  salaryMin: z.string().optional().or(z.literal("")),
  salaryMax: z.string().optional().or(z.literal("")),
  salaryText: z.string().optional().or(z.literal("")),
  currency: z.string().optional().or(z.literal("")),
  salaryPeriod: z.string().optional(),
  closingDate: z.string().optional(),
})

type JobFormValues = z.infer<typeof jobSchema>

interface JobFormProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  initialData?: JobResponse
  defaultCompanyId?: string
}

const WORK_MODES = [
  { value: "REMOTE", label: "Remote" },
  { value: "HYBRID", label: "Hybrid" },
  { value: "ONSITE", label: "Onsite" },
]

const JOB_TYPES = [
  { value: "FULL_TIME", label: "Full Time" },
  { value: "PART_TIME", label: "Part Time" },
  { value: "CONTRACT", label: "Contract" },
  { value: "FREELANCE", label: "Freelance" },
  { value: "INTERNSHIP", label: "Internship" },
]

const SALARY_TYPES = [
  { value: "FIXED", label: "Fixed" },
  { value: "RANGE", label: "Range" },
  { value: "TEXT", label: "Text" },
]

const SALARY_PERIODS = [
  { value: "HOURLY", label: "Hourly" },
  { value: "DAILY", label: "Daily" },
  { value: "MONTHLY", label: "Monthly" },
  { value: "ANNUAL", label: "Annual" },
]

export function JobForm({
  open,
  onOpenChange,
  initialData,
  defaultCompanyId,
}: JobFormProps) {
  const isEditing = !!initialData
  const createJob = useCreateJob()
  const updateJob = useUpdateJob()
  const { data: companiesData } = useCompanies({ size: 1000 })
  const companies = companiesData?.content ?? []

  const form = useForm<JobFormValues>({
    resolver: standardSchemaResolver(jobSchema),
    defaultValues: {
      title: "",
      description: "",
      url: "",
      notes: "",
      location: "",
      workMode: "",
      jobType: "",
      companyId: defaultCompanyId ?? "",
      salaryType: "",
      salaryMin: "",
      salaryMax: "",
      salaryText: "",
      currency: "",
      salaryPeriod: "",
      closingDate: "",
    },
  })

  useEffect(() => {
    if (open) {
      form.reset({
        title: initialData?.title ?? "",
        description: initialData?.description ?? "",
        url: initialData?.url ?? "",
        notes: initialData?.notes ?? "",
        location: initialData?.location ?? "",
        workMode: initialData?.workMode ?? "",
        jobType: initialData?.jobType ?? "",
        companyId: initialData?.companyId ?? defaultCompanyId ?? "",
        salaryType: initialData?.salaryType ?? "",
        salaryMin: initialData?.salaryMin != null ? String(initialData.salaryMin) : "",
        salaryMax: initialData?.salaryMax != null ? String(initialData.salaryMax) : "",
        salaryText: initialData?.salaryText ?? "",
        currency: initialData?.currency ?? "",
        salaryPeriod: initialData?.salaryPeriod ?? "",
        closingDate: initialData?.closingDate ?? "",
      })
    }
  }, [open, initialData, defaultCompanyId, form])

  const salaryType = form.watch("salaryType")

  const onSubmit = (values: JobFormValues) => {
    const data = {
      title: values.title,
      description: values.description || undefined,
      url: values.url || undefined,
      notes: values.notes || undefined,
      location: values.location || undefined,
      workMode: values.workMode || undefined,
      jobType: values.jobType || undefined,
      companyId: values.companyId || undefined,
      salaryType: values.salaryType || undefined,
      salaryMin: values.salaryMin ? Number(values.salaryMin) : undefined,
      salaryMax: values.salaryMax ? Number(values.salaryMax) : undefined,
      salaryText: values.salaryText || undefined,
      currency: values.currency || undefined,
      salaryPeriod: values.salaryPeriod || undefined,
      closingDate: values.closingDate || undefined,
    }

    if (isEditing) {
      updateJob.mutate(
        { id: initialData.id, data },
        {
          onSuccess: () => {
            toast.success("Job updated")
            onOpenChange(false)
          },
          onError: () => {
            toast.error("Failed to update job")
          },
        },
      )
    } else {
      createJob.mutate(data, {
        onSuccess: () => {
          toast.success("Job added")
          onOpenChange(false)
        },
        onError: () => {
          toast.error("Failed to add job")
        },
      })
    }
  }

  const isPending = createJob.isPending || updateJob.isPending

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{isEditing ? "Edit Job" : "Add Job"}</DialogTitle>
          <DialogDescription>
            {isEditing
              ? "Update the job details below."
              : "Add a new job posting to track."}
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={form.handleSubmit(onSubmit)} className="flex flex-col gap-4">
          {/* Basic */}
          <div className="flex flex-col gap-2">
            <Label htmlFor="title">Title *</Label>
            <Input
              id="title"
              placeholder="Job title"
              {...form.register("title")}
              aria-invalid={!!form.formState.errors.title}
            />
            {form.formState.errors.title && (
              <p className="text-xs text-destructive">
                {form.formState.errors.title.message}
              </p>
            )}
          </div>

          <div className="flex flex-col gap-2">
            <Label htmlFor="url">URL</Label>
            <Input
              id="url"
              placeholder="https://example.com/jobs/..."
              {...form.register("url")}
              aria-invalid={!!form.formState.errors.url}
            />
            {form.formState.errors.url && (
              <p className="text-xs text-destructive">
                {form.formState.errors.url.message}
              </p>
            )}
          </div>

          <div className="flex flex-col gap-2">
            <Label htmlFor="description">Description</Label>
            <Textarea
              id="description"
              placeholder="Job description..."
              {...form.register("description")}
            />
          </div>

          {/* Company & Location */}
          <div className="flex flex-col gap-2">
            <Label>Company</Label>
            <Controller
              name="companyId"
              control={form.control}
              render={({ field }) => (
                <Popover>
                  <PopoverTrigger asChild>
                    <Button
                      variant="outline"
                      role="combobox"
                      className="w-full justify-between font-normal"
                    >
                      {field.value
                        ? companies.find((c) => c.id === field.value)?.name ?? "Select company..."
                        : "Select company..."}
                      <ChevronsUpDown className="ml-2 size-4 shrink-0 opacity-50" />
                    </Button>
                  </PopoverTrigger>
                  <PopoverContent className="w-full p-0" align="start">
                    <Command>
                      <CommandInput placeholder="Search companies..." />
                      <CommandList>
                        <CommandEmpty>No companies found.</CommandEmpty>
                        <CommandGroup>
                          <CommandItem
                            value=""
                            onSelect={() => field.onChange("")}
                          >
                            <Check
                              className={cn(
                                "mr-2 size-4",
                                !field.value ? "opacity-100" : "opacity-0",
                              )}
                            />
                            None
                          </CommandItem>
                          {companies.map((company) => (
                            <CommandItem
                              key={company.id}
                              value={company.name}
                              onSelect={() => field.onChange(company.id)}
                            >
                              <Check
                                className={cn(
                                  "mr-2 size-4",
                                  field.value === company.id
                                    ? "opacity-100"
                                    : "opacity-0",
                                )}
                              />
                              {company.name}
                            </CommandItem>
                          ))}
                        </CommandGroup>
                      </CommandList>
                    </Command>
                  </PopoverContent>
                </Popover>
              )}
            />
          </div>

          <div className="flex flex-col gap-2">
            <Label htmlFor="location">Location</Label>
            <Input
              id="location"
              placeholder="City, Country"
              {...form.register("location")}
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-2">
              <Label>Work Mode</Label>
              <Controller
                name="workMode"
                control={form.control}
                render={({ field }) => (
                  <Select
                    value={field.value}
                    onValueChange={field.onChange}
                  >
                    <SelectTrigger className="w-full">
                      <SelectValue placeholder="Select..." />
                    </SelectTrigger>
                    <SelectContent>
                      {WORK_MODES.map((mode) => (
                        <SelectItem key={mode.value} value={mode.value}>
                          {mode.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
            </div>

            <div className="flex flex-col gap-2">
              <Label>Job Type</Label>
              <Controller
                name="jobType"
                control={form.control}
                render={({ field }) => (
                  <Select
                    value={field.value}
                    onValueChange={field.onChange}
                  >
                    <SelectTrigger className="w-full">
                      <SelectValue placeholder="Select..." />
                    </SelectTrigger>
                    <SelectContent>
                      {JOB_TYPES.map((type) => (
                        <SelectItem key={type.value} value={type.value}>
                          {type.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
            </div>
          </div>

          {/* Salary */}
          <div className="flex flex-col gap-2">
            <Label>Salary Type</Label>
            <Controller
              name="salaryType"
              control={form.control}
              render={({ field }) => (
                <Select
                  value={field.value}
                  onValueChange={field.onChange}
                >
                  <SelectTrigger className="w-full">
                    <SelectValue placeholder="Select..." />
                  </SelectTrigger>
                  <SelectContent>
                    {SALARY_TYPES.map((type) => (
                      <SelectItem key={type.value} value={type.value}>
                        {type.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
          </div>

          {(salaryType === "FIXED" || salaryType === "RANGE") && (
            <div className="grid grid-cols-2 gap-4">
              <div className="flex flex-col gap-2">
                <Label htmlFor="salaryMin">
                  {salaryType === "FIXED" ? "Salary" : "Min"}
                </Label>
                <Input
                  id="salaryMin"
                  type="number"
                  placeholder="0"
                  {...form.register("salaryMin")}
                />
              </div>
              {salaryType === "RANGE" && (
                <div className="flex flex-col gap-2">
                  <Label htmlFor="salaryMax">Max</Label>
                  <Input
                    id="salaryMax"
                    type="number"
                    placeholder="0"
                    {...form.register("salaryMax")}
                  />
                </div>
              )}
            </div>
          )}

          {salaryType === "TEXT" && (
            <div className="flex flex-col gap-2">
              <Label htmlFor="salaryText">Salary Text</Label>
              <Input
                id="salaryText"
                placeholder="e.g. Competitive"
                {...form.register("salaryText")}
              />
            </div>
          )}

          {(salaryType === "FIXED" || salaryType === "RANGE") && (
            <div className="grid grid-cols-2 gap-4">
              <div className="flex flex-col gap-2">
                <Label htmlFor="currency">Currency</Label>
                <Input
                  id="currency"
                  placeholder="USD"
                  {...form.register("currency")}
                />
              </div>
              <div className="flex flex-col gap-2">
                <Label>Period</Label>
                <Controller
                  name="salaryPeriod"
                  control={form.control}
                  render={({ field }) => (
                    <Select
                      value={field.value}
                      onValueChange={field.onChange}
                    >
                      <SelectTrigger className="w-full">
                        <SelectValue placeholder="Select..." />
                      </SelectTrigger>
                      <SelectContent>
                        {SALARY_PERIODS.map((period) => (
                          <SelectItem key={period.value} value={period.value}>
                            {period.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  )}
                />
              </div>
            </div>
          )}

          {/* Other */}
          <div className="flex flex-col gap-2">
            <Label>Closing Date</Label>
            <Controller
              name="closingDate"
              control={form.control}
              render={({ field }) => (
                <Popover>
                  <PopoverTrigger asChild>
                    <Button
                      variant="outline"
                      className={cn(
                        "w-full justify-start text-left font-normal",
                        !field.value && "text-muted-foreground",
                      )}
                    >
                      <CalendarIcon className="mr-2 size-4" />
                      {field.value
                        ? format(new Date(field.value), "PPP")
                        : "Pick a date"}
                    </Button>
                  </PopoverTrigger>
                  <PopoverContent className="w-auto p-0" align="start">
                    <Calendar
                      mode="single"
                      selected={field.value ? new Date(field.value) : undefined}
                      onSelect={(date) =>
                        field.onChange(
                          date ? date.toISOString().split("T")[0] : "",
                        )
                      }
                    />
                  </PopoverContent>
                </Popover>
              )}
            />
          </div>

          <div className="flex flex-col gap-2">
            <Label htmlFor="notes">Notes</Label>
            <Textarea
              id="notes"
              placeholder="Any notes about this job..."
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
              {isEditing ? "Update Job" : "Add Job"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
