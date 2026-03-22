"use client"

import { useRouter } from "next/navigation"
import { MapPin, Users } from "lucide-react"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { EmptyState } from "@/components/shared/empty-state"
import { CompanyCard } from "@/components/companies/company-card"
import { useSharedCompanies, useSharedJobs } from "@/hooks/use-shared-with-me"

export default function SharedPage() {
  const router = useRouter()
  const { data: companiesData, isLoading: companiesLoading } = useSharedCompanies()
  const { data: jobsData, isLoading: jobsLoading } = useSharedJobs()

  const companies = companiesData?.content ?? []
  const jobs = jobsData?.content ?? []

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-xl font-semibold">Shared with me</h1>

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
          ) : companies.length === 0 ? (
            <EmptyState
              heading="Nothing shared with you"
              body="When someone shares a company or job with you, it will appear here."
            />
          ) : (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {companies.map((company) => (
                <CompanyCard
                  key={company.id}
                  company={company}
                  jobCount={0}
                  onClick={() => router.push(`/companies/${company.id}`)}
                  onEdit={() => {}}
                  onDelete={() => {}}
                  hideActions
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
          ) : jobs.length === 0 ? (
            <EmptyState
              heading="Nothing shared with you"
              body="When someone shares a company or job with you, it will appear here."
            />
          ) : (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {jobs.map((job) => (
                <Card
                  key={job.id}
                  className="cursor-pointer transition-shadow hover:shadow-md"
                  onClick={() => router.push(`/jobs/${job.id}`)}
                >
                  <CardHeader>
                    <div className="flex items-center gap-1">
                      <Users className="size-3.5 text-muted-foreground" aria-label="Shared" />
                      <CardTitle className="text-sm font-semibold">{job.title}</CardTitle>
                    </div>
                  </CardHeader>
                  <CardContent className="flex flex-col gap-1.5">
                    {job.companyName && (
                      <p className="truncate text-xs text-muted-foreground">
                        {job.companyName}
                      </p>
                    )}
                    {job.location && (
                      <div className="flex items-center gap-1 text-xs text-muted-foreground">
                        <MapPin className="size-3 shrink-0" />
                        <span className="truncate">{job.location}</span>
                      </div>
                    )}
                  </CardContent>
                </Card>
              ))}
            </div>
          )}
        </TabsContent>
      </Tabs>
    </div>
  )
}
