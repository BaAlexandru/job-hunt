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
