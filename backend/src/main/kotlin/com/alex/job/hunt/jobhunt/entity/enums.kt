package com.alex.job.hunt.jobhunt.entity

enum class WorkMode { ONSITE, REMOTE, HYBRID }

enum class JobType { FULL_TIME, PART_TIME, CONTRACT, FREELANCE, INTERNSHIP }

enum class SalaryType { RANGE, FIXED, TEXT }

enum class SalaryPeriod { ANNUAL, MONTHLY, HOURLY, DAILY }

enum class ApplicationStatus {
    INTERESTED, APPLIED, PHONE_SCREEN, INTERVIEW, OFFER, REJECTED, ACCEPTED, WITHDRAWN
}

enum class NoteType {
    GENERAL, PHONE_CALL, EMAIL, FOLLOW_UP, STATUS_CHANGE
}

enum class InterviewType { PHONE, VIDEO, ONSITE, TAKE_HOME }

enum class InterviewStage { SCREENING, TECHNICAL, BEHAVIORAL, CULTURE_FIT, FINAL, SYSTEM_DESIGN, HOMEWORK, OTHER }

enum class InterviewOutcome { SCHEDULED, COMPLETED, CANCELLED, NO_SHOW }

enum class InterviewResult { PASSED, FAILED, PENDING, MIXED }

enum class InterviewNoteType { PREPARATION, QUESTION_ASKED, FEEDBACK, FOLLOW_UP, GENERAL }

enum class DocumentCategory { CV, COVER_LETTER, PORTFOLIO, OTHER }
