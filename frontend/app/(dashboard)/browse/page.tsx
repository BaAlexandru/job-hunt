"use client"

import { useRouter } from "next/navigation"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Skeleton } from "@/components/ui/skeleton"
import { EmptyState } from "@/components/shared/empty-state"
import { BrowseCard } from "@/components/browse/browse-card"
import { useBrowseCompanies, useBrowseJobs } from "@/hooks/use-browse"

export default function BrowsePage() {
  const router = useRouter()
  const { data: companiesData, isLoading: companiesLoading, isError: companiesError } = useBrowseCompanies()
  const { data: jobsData, isLoading: jobsLoading, isError: jobsError } = useBrowseJobs()

  const companies = companiesData?.content ?? []
  const jobs = jobsData?.content ?? []

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-xl font-semibold">Browse Public Resources</h1>

      <Tabs defaultValue="companies">
        <TabsList>
          <TabsTrigger value="companies">Companies</TabsTrigger>
          <TabsTrigger value="jobs">Jobs</TabsTrigger>
        </TabsList>

        <TabsContent value="companies" className="mt-4">
          {companiesLoading ? (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {Array.from({ length: 6 }).map((_, i) => (
                <Skeleton key={i} className="h-36 rounded-xl" />
              ))}
            </div>
          ) : companiesError ? (
            <EmptyState
              heading="Failed to load companies"
              body="Something went wrong. Please try again later."
            />
          ) : companies.length === 0 ? (
            <EmptyState
              heading="No public resources yet"
              body="When users share companies or jobs publicly, they will appear here."
            />
          ) : (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {companies.map((company) => (
                <BrowseCard
                  key={company.id}
                  name={company.name}
                  subtitle={company.website}
                  location={company.location}
                  ownerEmail={company.ownerEmail}
                  onClick={() => router.push(`/companies/${company.id}`)}
                />
              ))}
            </div>
          )}
        </TabsContent>

        <TabsContent value="jobs" className="mt-4">
          {jobsLoading ? (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {Array.from({ length: 6 }).map((_, i) => (
                <Skeleton key={i} className="h-36 rounded-xl" />
              ))}
            </div>
          ) : jobsError ? (
            <EmptyState
              heading="Failed to load jobs"
              body="Something went wrong. Please try again later."
            />
          ) : jobs.length === 0 ? (
            <EmptyState
              heading="No public resources yet"
              body="When users share companies or jobs publicly, they will appear here."
            />
          ) : (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {jobs.map((job) => (
                <BrowseCard
                  key={job.id}
                  name={job.title}
                  subtitle={job.companyName}
                  location={job.location}
                  ownerEmail={job.ownerEmail}
                  onClick={() => router.push(`/jobs/${job.id}`)}
                />
              ))}
            </div>
          )}
        </TabsContent>
      </Tabs>
    </div>
  )
}
