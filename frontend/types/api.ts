// ============================================================================
// Paginated response (mirrors Spring Page shape)
// ============================================================================

export interface PaginatedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  first: boolean
  last: boolean
  empty: boolean
}

// ============================================================================
// Application status
// ============================================================================

export type ApplicationStatus =
  | "INTERESTED"
  | "APPLIED"
  | "PHONE_SCREEN"
  | "INTERVIEW"
  | "OFFER"
  | "REJECTED"
  | "ACCEPTED"
  | "WITHDRAWN"

export const APPLICATION_STATUSES: ApplicationStatus[] = [
  "INTERESTED",
  "APPLIED",
  "PHONE_SCREEN",
  "INTERVIEW",
  "OFFER",
  "REJECTED",
  "ACCEPTED",
  "WITHDRAWN",
]

const ACTIVE_STATUSES: ApplicationStatus[] = [
  "INTERESTED",
  "APPLIED",
  "PHONE_SCREEN",
  "INTERVIEW",
  "OFFER",
]

export const STATUS_TRANSITIONS: Record<
  ApplicationStatus,
  ApplicationStatus[]
> = {
  INTERESTED: ["APPLIED", "WITHDRAWN"],
  APPLIED: ["PHONE_SCREEN", "INTERVIEW", "OFFER", "REJECTED", "WITHDRAWN"],
  PHONE_SCREEN: ["INTERVIEW", "OFFER", "REJECTED", "WITHDRAWN"],
  INTERVIEW: ["PHONE_SCREEN", "OFFER", "REJECTED", "WITHDRAWN"],
  OFFER: ["ACCEPTED", "REJECTED", "WITHDRAWN"],
  REJECTED: ACTIVE_STATUSES,
  ACCEPTED: ACTIVE_STATUSES,
  WITHDRAWN: ACTIVE_STATUSES,
}

export function isValidTransition(
  from: ApplicationStatus,
  to: ApplicationStatus,
): boolean {
  return STATUS_TRANSITIONS[from]?.includes(to) ?? false
}

// ============================================================================
// Status display helpers
// ============================================================================

export const STATUS_COLORS: Record<
  ApplicationStatus,
  { bg: string; text: string }
> = {
  INTERESTED: { bg: "bg-blue-100", text: "text-blue-800" },
  APPLIED: { bg: "bg-indigo-100", text: "text-indigo-800" },
  PHONE_SCREEN: { bg: "bg-violet-100", text: "text-violet-800" },
  INTERVIEW: { bg: "bg-purple-100", text: "text-purple-800" },
  OFFER: { bg: "bg-amber-100", text: "text-amber-800" },
  ACCEPTED: { bg: "bg-green-100", text: "text-green-800" },
  REJECTED: { bg: "bg-red-100", text: "text-red-800" },
  WITHDRAWN: { bg: "bg-gray-100", text: "text-gray-800" },
}

export const STATUS_LABELS: Record<ApplicationStatus, string> = {
  INTERESTED: "Interested",
  APPLIED: "Applied",
  PHONE_SCREEN: "Phone Screen",
  INTERVIEW: "Interview",
  OFFER: "Offer",
  ACCEPTED: "Accepted",
  REJECTED: "Rejected",
  WITHDRAWN: "Withdrawn",
}

// ============================================================================
// Application
// ============================================================================

export interface ApplicationResponse {
  id: string
  jobId: string
  jobTitle: string
  companyName: string | null
  status: ApplicationStatus
  quickNotes: string | null
  appliedDate: string | null
  lastActivityDate: string
  nextActionDate: string | null
  archived: boolean
  archivedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateApplicationRequest {
  jobId: string
  quickNotes?: string
  appliedDate?: string
  nextActionDate?: string
}

export interface UpdateApplicationRequest {
  quickNotes?: string
  appliedDate?: string
  nextActionDate?: string
}

// ============================================================================
// Company
// ============================================================================

export interface CompanyResponse {
  id: string
  name: string
  website: string | null
  location: string | null
  notes: string | null
  visibility: Visibility
  archived: boolean
  archivedAt: string | null
  createdAt: string
  updatedAt: string
  isOwner?: boolean
}

export interface CreateCompanyRequest {
  name: string
  website?: string
  location?: string
  notes?: string
}

export interface UpdateCompanyRequest {
  name: string
  website?: string
  location?: string
  notes?: string
}

// ============================================================================
// Job
// ============================================================================

export interface JobResponse {
  id: string
  title: string
  description: string | null
  url: string | null
  notes: string | null
  location: string | null
  workMode: string | null
  jobType: string | null
  companyId: string | null
  companyName: string | null
  visibility: Visibility
  salaryType: string | null
  salaryMin: number | null
  salaryMax: number | null
  salaryText: string | null
  currency: string | null
  salaryPeriod: string | null
  closingDate: string | null
  archived: boolean
  archivedAt: string | null
  createdAt: string
  updatedAt: string
  isOwner?: boolean
}

export interface CreateJobRequest {
  title: string
  description?: string
  url?: string
  notes?: string
  location?: string
  workMode?: string
  jobType?: string
  companyId?: string
  salaryType?: string
  salaryMin?: number
  salaryMax?: number
  salaryText?: string
  currency?: string
  salaryPeriod?: string
  closingDate?: string
}

export interface UpdateJobRequest {
  title: string
  description?: string
  url?: string
  notes?: string
  location?: string
  workMode?: string
  jobType?: string
  companyId?: string
  salaryType?: string
  salaryMin?: number
  salaryMax?: number
  salaryText?: string
  currency?: string
  salaryPeriod?: string
  closingDate?: string
}

// ============================================================================
// Interview
// ============================================================================

export interface InterviewResponse {
  id: string
  applicationId: string
  roundNumber: number
  scheduledAt: string
  durationMinutes: number | null
  interviewType: string
  stage: string
  stageLabel: string | null
  outcome: string
  result: string
  location: string | null
  interviewerNames: string | null
  candidateFeedback: string | null
  companyFeedback: string | null
  archived: boolean
  createdAt: string
  updatedAt: string
}

export interface CreateInterviewRequest {
  applicationId: string
  scheduledAt: string
  interviewType: string
  stage: string
  stageLabel?: string
  durationMinutes?: number
  location?: string
  interviewerNames?: string
}

export interface UpdateInterviewRequest {
  scheduledAt?: string
  interviewType?: string
  stage?: string
  stageLabel?: string
  durationMinutes?: number
  location?: string
  interviewerNames?: string
  outcome?: string
  result?: string
  candidateFeedback?: string
  companyFeedback?: string
}

// ============================================================================
// Document
// ============================================================================

export interface DocumentResponse {
  id: string
  title: string
  description: string | null
  category: string
  currentVersion: DocumentVersionResponse | null
  createdAt: string
  updatedAt: string
}

export interface DocumentVersionResponse {
  id: string
  versionNumber: number
  originalFilename: string
  contentType: string
  fileSize: number
  note: string | null
  isCurrent: boolean
  createdAt: string
}

export interface DocumentApplicationLinkResponse {
  id: string
  documentVersionId: string
  applicationId: string
  linkedAt: string
  versionRemoved: boolean
}

export interface LinkDocumentRequest {
  documentVersionId: string
  applicationId: string
}

export interface DocumentUpdateRequest {
  title: string
  description?: string
  category: string
}

// ============================================================================
// Notes
// ============================================================================

export interface NoteResponse {
  id: string
  content: string
  createdAt: string
  updatedAt: string
}

export interface InterviewNoteResponse {
  id: string
  interviewId: string
  content: string
  noteType: string
  createdAt: string
  updatedAt: string
}

// ============================================================================
// Timeline
// ============================================================================

export interface TimelineEntry {
  id: string
  type: string
  title: string
  description: string | null
  occurredAt: string
  metadata: Record<string, unknown> | null
}

// ============================================================================
// Visibility & Sharing
// ============================================================================

export type Visibility = "PRIVATE" | "PUBLIC" | "SHARED"

export interface ShareResponse {
  id: string
  email: string
  sharedAt: string
}

export interface BrowseCompanyResponse {
  id: string
  name: string
  website: string | null
  location: string | null
  notes: string | null
  ownerEmail: string
  createdAt: string
}

export interface BrowseJobResponse {
  id: string
  title: string
  description: string | null
  location: string | null
  workMode: string | null
  jobType: string | null
  companyName: string | null
  ownerEmail: string
  createdAt: string
}
