package com.alex.job.hunt.jobhunt.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "jobs")
class JobEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "company_id")
    var companyId: UUID? = null,

    @Column(nullable = false, length = 255)
    var title: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(length = 2000)
    var url: String? = null,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

    @Column(length = 255)
    var location: String? = null,

    @Column(name = "work_mode", length = 50)
    @Enumerated(EnumType.STRING)
    var workMode: WorkMode? = null,

    @Column(name = "job_type", length = 50)
    @Enumerated(EnumType.STRING)
    var jobType: JobType? = null,

    @Column(name = "salary_type", length = 50)
    @Enumerated(EnumType.STRING)
    var salaryType: SalaryType? = null,

    @Column(name = "salary_min", precision = 15, scale = 2)
    var salaryMin: BigDecimal? = null,

    @Column(name = "salary_max", precision = 15, scale = 2)
    var salaryMax: BigDecimal? = null,

    @Column(name = "salary_text", length = 255)
    var salaryText: String? = null,

    @Column(length = 10)
    var currency: String? = null,

    @Column(name = "salary_period", length = 50)
    @Enumerated(EnumType.STRING)
    var salaryPeriod: SalaryPeriod? = null,

    @Column(name = "closing_date")
    var closingDate: LocalDate? = null,

    @Column(nullable = false)
    var archived: Boolean = false,

    @Column(name = "archived_at")
    var archivedAt: Instant? = null,

    @Column(name = "created_at", updatable = false, nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JobEntity) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}
