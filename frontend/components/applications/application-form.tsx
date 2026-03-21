"use client"

import { useEffect, useState } from "react"
import { useForm } from "react-hook-form"
import { standardSchemaResolver } from "@hookform/resolvers/standard-schema"
import { z } from "zod"
import { format } from "date-fns"
import { toast } from "sonner"
import { CalendarIcon, ChevronsUpDownIcon, CheckIcon } from "lucide-react"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import { Label } from "@/components/ui/label"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from "@/components/ui/command"
import { Calendar } from "@/components/ui/calendar"
import { useJobs } from "@/hooks/use-jobs"
import {
  useCreateApplication,
  useUpdateApplication,
} from "@/hooks/use-applications"
import type { ApplicationResponse } from "@/types/api"
import { cn } from "@/lib/utils"

const applicationSchema = z.object({
  jobId: z.string().min(1, "Job is required"),
  appliedDate: z.string().optional(),
  nextActionDate: z.string().optional(),
  quickNotes: z.string().optional(),
})

type ApplicationFormValues = z.infer<typeof applicationSchema>

interface ApplicationFormProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  initialData?: ApplicationResponse
}

export function ApplicationForm({
  open,
  onOpenChange,
  initialData,
}: ApplicationFormProps) {
  const isEditing = !!initialData
  const createApp = useCreateApplication()
  const updateApp = useUpdateApplication()
  const { data: jobsData } = useJobs({ size: 100 })
  const jobs = jobsData?.content ?? []

  const [jobOpen, setJobOpen] = useState(false)
  const [appliedDateOpen, setAppliedDateOpen] = useState(false)
  const [nextActionDateOpen, setNextActionDateOpen] = useState(false)

  const form = useForm<ApplicationFormValues>({
    resolver: standardSchemaResolver(applicationSchema),
    defaultValues: {
      jobId: "",
      appliedDate: undefined,
      nextActionDate: undefined,
      quickNotes: "",
    },
  })

  useEffect(() => {
    if (open) {
      form.reset({
        jobId: initialData?.jobId ?? "",
        appliedDate: initialData?.appliedDate ?? undefined,
        nextActionDate: initialData?.nextActionDate ?? undefined,
        quickNotes: initialData?.quickNotes ?? "",
      })
    }
  }, [open, initialData, form])

  const selectedJobId = form.watch("jobId")
  const selectedJob = jobs.find((j) => j.id === selectedJobId)

  function onSubmit(values: ApplicationFormValues) {
    const data = {
      jobId: values.jobId,
      appliedDate: values.appliedDate || undefined,
      nextActionDate: values.nextActionDate || undefined,
      quickNotes: values.quickNotes || undefined,
    }

    if (isEditing) {
      updateApp.mutate(
        {
          id: initialData.id,
          data: {
            appliedDate: data.appliedDate,
            nextActionDate: data.nextActionDate,
            quickNotes: data.quickNotes,
          },
        },
        {
          onSuccess: () => {
            toast.success("Application updated")
            onOpenChange(false)
          },
          onError: () => toast.error("Failed to update application"),
        },
      )
    } else {
      createApp.mutate(data, {
        onSuccess: () => {
          toast.success("Application created")
          onOpenChange(false)
        },
        onError: () => toast.error("Failed to create application"),
      })
    }
  }

  const isPending = createApp.isPending || updateApp.isPending

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>
            {isEditing ? "Update Application" : "Create Application"}
          </DialogTitle>
          <DialogDescription>
            {isEditing
              ? "Update the application details."
              : "Create a new application to track."}
          </DialogDescription>
        </DialogHeader>
        <form
          onSubmit={form.handleSubmit(onSubmit)}
          className="flex flex-col gap-4"
        >
          {/* Job selector */}
          <div className="flex flex-col gap-2">
            <Label>Job *</Label>
            <Popover open={jobOpen} onOpenChange={setJobOpen}>
              <PopoverTrigger asChild>
                <Button
                  variant="outline"
                  role="combobox"
                  aria-expanded={jobOpen}
                  className="w-full justify-between font-normal"
                  disabled={isEditing}
                >
                  {selectedJob
                    ? `${selectedJob.title}${selectedJob.companyName ? ` - ${selectedJob.companyName}` : ""}`
                    : "Select a job..."}
                  <ChevronsUpDownIcon className="ml-auto size-3.5 opacity-50" />
                </Button>
              </PopoverTrigger>
              <PopoverContent className="w-full p-0" align="start">
                <Command>
                  <CommandInput placeholder="Search jobs..." />
                  <CommandList>
                    <CommandEmpty>No jobs found.</CommandEmpty>
                    <CommandGroup>
                      {jobs.map((job) => (
                        <CommandItem
                          key={job.id}
                          value={`${job.title} ${job.companyName ?? ""}`}
                          data-checked={selectedJobId === job.id}
                          onSelect={() => {
                            form.setValue("jobId", job.id, {
                              shouldValidate: true,
                            })
                            setJobOpen(false)
                          }}
                        >
                          <CheckIcon
                            className={cn(
                              "mr-2 size-3.5",
                              selectedJobId === job.id
                                ? "opacity-100"
                                : "opacity-0",
                            )}
                          />
                          <div className="flex flex-col">
                            <span className="text-sm">{job.title}</span>
                            {job.companyName && (
                              <span className="text-xs text-muted-foreground">
                                {job.companyName}
                              </span>
                            )}
                          </div>
                        </CommandItem>
                      ))}
                    </CommandGroup>
                  </CommandList>
                </Command>
              </PopoverContent>
            </Popover>
            {form.formState.errors.jobId && (
              <p className="text-xs text-destructive">
                {form.formState.errors.jobId.message}
              </p>
            )}
          </div>

          {/* Applied Date */}
          <div className="flex flex-col gap-2">
            <Label>Applied Date</Label>
            <Popover open={appliedDateOpen} onOpenChange={setAppliedDateOpen}>
              <PopoverTrigger asChild>
                <Button
                  variant="outline"
                  className={cn(
                    "w-full justify-start font-normal",
                    !form.watch("appliedDate") && "text-muted-foreground",
                  )}
                >
                  <CalendarIcon className="mr-2 size-3.5" />
                  {form.watch("appliedDate")
                    ? format(new Date(form.watch("appliedDate")!), "MMM d, yyyy")
                    : "Pick a date"}
                </Button>
              </PopoverTrigger>
              <PopoverContent className="w-auto p-0" align="start">
                <Calendar
                  mode="single"
                  selected={
                    form.watch("appliedDate")
                      ? new Date(form.watch("appliedDate")!)
                      : undefined
                  }
                  onSelect={(date) => {
                    form.setValue(
                      "appliedDate",
                      date ? format(date, "yyyy-MM-dd") : undefined,
                    )
                    setAppliedDateOpen(false)
                  }}
                />
              </PopoverContent>
            </Popover>
          </div>

          {/* Next Action Date */}
          <div className="flex flex-col gap-2">
            <Label>Next Action Date</Label>
            <Popover
              open={nextActionDateOpen}
              onOpenChange={setNextActionDateOpen}
            >
              <PopoverTrigger asChild>
                <Button
                  variant="outline"
                  className={cn(
                    "w-full justify-start font-normal",
                    !form.watch("nextActionDate") && "text-muted-foreground",
                  )}
                >
                  <CalendarIcon className="mr-2 size-3.5" />
                  {form.watch("nextActionDate")
                    ? format(
                        new Date(form.watch("nextActionDate")!),
                        "MMM d, yyyy",
                      )
                    : "Pick a date"}
                </Button>
              </PopoverTrigger>
              <PopoverContent className="w-auto p-0" align="start">
                <Calendar
                  mode="single"
                  selected={
                    form.watch("nextActionDate")
                      ? new Date(form.watch("nextActionDate")!)
                      : undefined
                  }
                  onSelect={(date) => {
                    form.setValue(
                      "nextActionDate",
                      date ? format(date, "yyyy-MM-dd") : undefined,
                    )
                    setNextActionDateOpen(false)
                  }}
                />
              </PopoverContent>
            </Popover>
          </div>

          {/* Quick Notes */}
          <div className="flex flex-col gap-2">
            <Label htmlFor="quick-notes">Quick Notes</Label>
            <Textarea
              id="quick-notes"
              placeholder="Any notes about this application..."
              {...form.register("quickNotes")}
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
              {isEditing ? "Update Application" : "Create Application"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
