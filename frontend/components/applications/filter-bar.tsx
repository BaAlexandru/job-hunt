"use client"

import { useState, useEffect } from "react"
import { SearchIcon, CalendarIcon, XIcon } from "lucide-react"
import { format } from "date-fns"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList } from "@/components/ui/command"
import { Checkbox } from "@/components/ui/checkbox"
import { Calendar } from "@/components/ui/calendar"
import { useCompanies } from "@/hooks/use-companies"
import { APPLICATION_STATUSES, STATUS_LABELS } from "@/types/api"
import type { ApplicationStatus } from "@/types/api"
import { cn } from "@/lib/utils"

export interface FilterState {
  status?: string[]
  companyId?: string
  search?: string
  fromDate?: string
  toDate?: string
}

interface FilterBarProps {
  filters: FilterState
  onFiltersChange: (filters: FilterState) => void
}

export function FilterBar({ filters, onFiltersChange }: FilterBarProps) {
  const [searchInput, setSearchInput] = useState(filters.search ?? "")
  const [statusOpen, setStatusOpen] = useState(false)
  const [companyOpen, setCompanyOpen] = useState(false)
  const [fromDateOpen, setFromDateOpen] = useState(false)
  const [toDateOpen, setToDateOpen] = useState(false)

  const { data: companiesData } = useCompanies({ size: 100 })
  const companies = companiesData?.content ?? []

  // Debounced search
  useEffect(() => {
    const timeout = setTimeout(() => {
      if (searchInput !== (filters.search ?? "")) {
        onFiltersChange({ ...filters, search: searchInput || undefined })
      }
    }, 300)
    return () => clearTimeout(timeout)
  }, [searchInput]) // eslint-disable-line react-hooks/exhaustive-deps

  const selectedStatuses = filters.status ?? []

  function toggleStatus(status: ApplicationStatus) {
    const current = [...selectedStatuses]
    const idx = current.indexOf(status)
    if (idx >= 0) {
      current.splice(idx, 1)
    } else {
      current.push(status)
    }
    onFiltersChange({
      ...filters,
      status: current.length > 0 ? current : undefined,
    })
  }

  const selectedCompany = companies.find((c) => c.id === filters.companyId)

  return (
    <div className="flex flex-wrap items-center gap-4 py-3">
      {/* Status multi-select */}
      <Popover open={statusOpen} onOpenChange={setStatusOpen}>
        <PopoverTrigger asChild>
          <Button variant="outline" size="sm" className="gap-1.5">
            Status
            {selectedStatuses.length > 0 && (
              <span className="ml-1 rounded-full bg-primary/10 px-1.5 text-xs font-semibold text-primary">
                {selectedStatuses.length}
              </span>
            )}
          </Button>
        </PopoverTrigger>
        <PopoverContent className="w-52 p-0" align="start">
          <Command>
            <CommandInput placeholder="Filter statuses..." />
            <CommandList>
              <CommandEmpty>No status found.</CommandEmpty>
              <CommandGroup>
                {APPLICATION_STATUSES.map((status) => (
                  <CommandItem
                    key={status}
                    onSelect={() => toggleStatus(status)}
                  >
                    <Checkbox
                      checked={selectedStatuses.includes(status)}
                      className="mr-2"
                    />
                    {STATUS_LABELS[status]}
                  </CommandItem>
                ))}
              </CommandGroup>
            </CommandList>
          </Command>
        </PopoverContent>
      </Popover>

      {/* Company combobox */}
      <Popover open={companyOpen} onOpenChange={setCompanyOpen}>
        <PopoverTrigger asChild>
          <Button variant="outline" size="sm" className="gap-1.5">
            {selectedCompany ? selectedCompany.name : "Company"}
          </Button>
        </PopoverTrigger>
        <PopoverContent className="w-56 p-0" align="start">
          <Command>
            <CommandInput placeholder="Search companies..." />
            <CommandList>
              <CommandEmpty>No company found.</CommandEmpty>
              <CommandGroup>
                {filters.companyId && (
                  <CommandItem
                    onSelect={() => {
                      onFiltersChange({ ...filters, companyId: undefined })
                      setCompanyOpen(false)
                    }}
                  >
                    <XIcon className="mr-2 size-3.5 text-muted-foreground" />
                    Clear selection
                  </CommandItem>
                )}
                {companies.map((company) => (
                  <CommandItem
                    key={company.id}
                    data-checked={filters.companyId === company.id}
                    onSelect={() => {
                      onFiltersChange({
                        ...filters,
                        companyId:
                          filters.companyId === company.id
                            ? undefined
                            : company.id,
                      })
                      setCompanyOpen(false)
                    }}
                  >
                    {company.name}
                  </CommandItem>
                ))}
              </CommandGroup>
            </CommandList>
          </Command>
        </PopoverContent>
      </Popover>

      {/* From date */}
      <Popover open={fromDateOpen} onOpenChange={setFromDateOpen}>
        <PopoverTrigger asChild>
          <Button
            variant="outline"
            size="sm"
            className={cn(
              "gap-1.5",
              !filters.fromDate && "text-muted-foreground",
            )}
          >
            <CalendarIcon className="size-3.5" />
            {filters.fromDate
              ? format(new Date(filters.fromDate), "MMM d, yyyy")
              : "From"}
          </Button>
        </PopoverTrigger>
        <PopoverContent className="w-auto p-0" align="start">
          <Calendar
            mode="single"
            selected={filters.fromDate ? new Date(filters.fromDate) : undefined}
            onSelect={(date) => {
              onFiltersChange({
                ...filters,
                fromDate: date ? format(date, "yyyy-MM-dd") : undefined,
              })
              setFromDateOpen(false)
            }}
          />
        </PopoverContent>
      </Popover>

      {/* To date */}
      <Popover open={toDateOpen} onOpenChange={setToDateOpen}>
        <PopoverTrigger asChild>
          <Button
            variant="outline"
            size="sm"
            className={cn(
              "gap-1.5",
              !filters.toDate && "text-muted-foreground",
            )}
          >
            <CalendarIcon className="size-3.5" />
            {filters.toDate
              ? format(new Date(filters.toDate), "MMM d, yyyy")
              : "To"}
          </Button>
        </PopoverTrigger>
        <PopoverContent className="w-auto p-0" align="start">
          <Calendar
            mode="single"
            selected={filters.toDate ? new Date(filters.toDate) : undefined}
            onSelect={(date) => {
              onFiltersChange({
                ...filters,
                toDate: date ? format(date, "yyyy-MM-dd") : undefined,
              })
              setToDateOpen(false)
            }}
          />
        </PopoverContent>
      </Popover>

      {/* Search input */}
      <div className="relative ml-auto w-full max-w-xs">
        <SearchIcon className="absolute left-2.5 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground" />
        <Input
          placeholder="Search applications..."
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          className="h-7 pl-8 text-sm"
        />
      </div>
    </div>
  )
}
