package com.alex.job.hunt.jobhunt.job

import com.alex.job.hunt.jobhunt.TestHelper
import com.alex.job.hunt.jobhunt.dto.CreateJobRequest
import com.alex.job.hunt.jobhunt.dto.UpdateJobRequest
import com.alex.job.hunt.jobhunt.entity.JobType
import com.alex.job.hunt.jobhunt.entity.SalaryPeriod
import com.alex.job.hunt.jobhunt.entity.SalaryType
import com.alex.job.hunt.jobhunt.entity.WorkMode
import com.alex.job.hunt.jobhunt.repository.CompanyRepository
import com.alex.job.hunt.jobhunt.repository.EmailVerificationTokenRepository
import com.alex.job.hunt.jobhunt.repository.JobRepository
import com.alex.job.hunt.jobhunt.repository.PasswordResetTokenRepository
import com.alex.job.hunt.jobhunt.repository.TokenBlocklistRepository
import com.alex.job.hunt.jobhunt.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class JobControllerIntegrationTests {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var jsonMapper: JsonMapper

    @Autowired
    lateinit var jobRepository: JobRepository

    @Autowired
    lateinit var companyRepository: CompanyRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var emailVerificationTokenRepository: EmailVerificationTokenRepository

    @Autowired
    lateinit var passwordResetTokenRepository: PasswordResetTokenRepository

    @Autowired
    lateinit var tokenBlocklistRepository: TokenBlocklistRepository

    @BeforeEach
    fun setUp() {
        jobRepository.deleteAll()
        companyRepository.deleteAll()
        tokenBlocklistRepository.deleteAll()
        passwordResetTokenRepository.deleteAll()
        emailVerificationTokenRepository.deleteAll()
        userRepository.deleteAll()
    }

    // --- JOBS-01: Create with all fields ---

    @Test
    fun `should return 201 when creating job with all fields`() {
        val token = registerAndGetToken()
        val companyId = createCompany(token, "Test Corp")
        val request = CreateJobRequest(
            title = "Senior Kotlin Developer",
            description = "Build awesome backends",
            url = "https://example.com/jobs/123",
            notes = "Applied via referral",
            location = "London, UK",
            workMode = WorkMode.REMOTE,
            jobType = JobType.FULL_TIME,
            companyId = UUID.fromString(companyId),
            salaryType = SalaryType.RANGE,
            salaryMin = BigDecimal("50000.00"),
            salaryMax = BigDecimal("80000.00"),
            currency = "GBP",
            salaryPeriod = SalaryPeriod.ANNUAL,
            closingDate = LocalDate.of(2026, 6, 1)
        )

        mockMvc.perform(
            post("/api/jobs")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.title").value("Senior Kotlin Developer"))
            .andExpect(jsonPath("$.description").value("Build awesome backends"))
            .andExpect(jsonPath("$.url").value("https://example.com/jobs/123"))
            .andExpect(jsonPath("$.notes").value("Applied via referral"))
            .andExpect(jsonPath("$.location").value("London, UK"))
            .andExpect(jsonPath("$.workMode").value("REMOTE"))
            .andExpect(jsonPath("$.jobType").value("FULL_TIME"))
            .andExpect(jsonPath("$.companyId").value(companyId))
            .andExpect(jsonPath("$.companyName").value("Test Corp"))
            .andExpect(jsonPath("$.salaryType").value("RANGE"))
            .andExpect(jsonPath("$.salaryMin").value(50000.00))
            .andExpect(jsonPath("$.salaryMax").value(80000.00))
            .andExpect(jsonPath("$.currency").value("GBP"))
            .andExpect(jsonPath("$.salaryPeriod").value("ANNUAL"))
            .andExpect(jsonPath("$.closingDate").value("2026-06-01"))
            .andExpect(jsonPath("$.archived").value(false))
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.updatedAt").exists())
    }

    @Test
    fun `should return 201 when creating job with only title`() {
        val token = registerAndGetToken()
        val request = CreateJobRequest(title = "Software Engineer")

        mockMvc.perform(
            post("/api/jobs")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.title").value("Software Engineer"))
            .andExpect(jsonPath("$.description").isEmpty())
            .andExpect(jsonPath("$.url").isEmpty())
            .andExpect(jsonPath("$.companyId").isEmpty())
            .andExpect(jsonPath("$.companyName").isEmpty())
            .andExpect(jsonPath("$.workMode").isEmpty())
            .andExpect(jsonPath("$.jobType").isEmpty())
    }

    @Test
    fun `should return 400 when title is blank`() {
        val token = registerAndGetToken()
        val request = CreateJobRequest(title = "")

        mockMvc.perform(
            post("/api/jobs")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.fieldErrors.title").exists())
    }

    @Test
    fun `should return 201 when creating job with TEXT salary`() {
        val token = registerAndGetToken()
        val request = CreateJobRequest(
            title = "Designer",
            salaryType = SalaryType.TEXT,
            salaryText = "Competitive"
        )

        mockMvc.perform(
            post("/api/jobs")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.salaryType").value("TEXT"))
            .andExpect(jsonPath("$.salaryText").value("Competitive"))
    }

    // --- JOBS-02: Company linking ---

    @Test
    fun `should create job linked to company`() {
        val token = registerAndGetToken()
        val companyId = createCompany(token, "Acme Corp")

        val request = CreateJobRequest(
            title = "Backend Dev",
            companyId = UUID.fromString(companyId)
        )

        mockMvc.perform(
            post("/api/jobs")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.companyId").value(companyId))
            .andExpect(jsonPath("$.companyName").value("Acme Corp"))
    }

    @Test
    fun `should return 404 when linking to non-existent company`() {
        val token = registerAndGetToken()
        val request = CreateJobRequest(
            title = "Backend Dev",
            companyId = UUID.randomUUID()
        )

        mockMvc.perform(
            post("/api/jobs")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 404 when linking to another user's company`() {
        val tokenA = registerAndGetToken("userA@example.com")
        val companyId = createCompany(tokenA, "User A Corp")

        val tokenB = registerAndGetToken("userB@example.com")
        val request = CreateJobRequest(
            title = "Backend Dev",
            companyId = UUID.fromString(companyId)
        )

        mockMvc.perform(
            post("/api/jobs")
                .header("Authorization", "Bearer $tokenB")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should create job without company`() {
        val token = registerAndGetToken()
        val request = CreateJobRequest(title = "Freelance Gig")

        mockMvc.perform(
            post("/api/jobs")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.companyId").isEmpty())
            .andExpect(jsonPath("$.companyName").isEmpty())
    }

    // --- JOBS-03: Update and Delete ---

    @Test
    fun `should update job fields`() {
        val token = registerAndGetToken()
        val jobId = createJob(token, "Original Title")

        val updateRequest = UpdateJobRequest(
            title = "Updated Title",
            description = "New description",
            location = "Berlin, DE"
        )

        mockMvc.perform(
            put("/api/jobs/$jobId")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("Updated Title"))
            .andExpect(jsonPath("$.description").value("New description"))
            .andExpect(jsonPath("$.location").value("Berlin, DE"))
    }

    @Test
    fun `should change company link via update`() {
        val token = registerAndGetToken()
        val companyA = createCompany(token, "Company A")
        val companyB = createCompany(token, "Company B")
        val jobId = createJobWithCompany(token, "Dev Role", companyA)

        val updateRequest = UpdateJobRequest(
            title = "Dev Role",
            companyId = UUID.fromString(companyB)
        )

        mockMvc.perform(
            put("/api/jobs/$jobId")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.companyId").value(companyB))
            .andExpect(jsonPath("$.companyName").value("Company B"))
    }

    @Test
    fun `should clear company link via update`() {
        val token = registerAndGetToken()
        val companyId = createCompany(token, "Some Corp")
        val jobId = createJobWithCompany(token, "Dev Role", companyId)

        val updateRequest = UpdateJobRequest(title = "Dev Role")

        mockMvc.perform(
            put("/api/jobs/$jobId")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.companyId").isEmpty())
            .andExpect(jsonPath("$.companyName").isEmpty())
    }

    @Test
    fun `should return 404 when updating non-existent job`() {
        val token = registerAndGetToken()
        val updateRequest = UpdateJobRequest(title = "Whatever")

        mockMvc.perform(
            put("/api/jobs/${UUID.randomUUID()}")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 404 when updating another user's job`() {
        val tokenA = registerAndGetToken("userA@example.com")
        val jobId = createJob(tokenA, "User A Job")

        val tokenB = registerAndGetToken("userB@example.com")
        val updateRequest = UpdateJobRequest(title = "Hijacked")

        mockMvc.perform(
            put("/api/jobs/$jobId")
                .header("Authorization", "Bearer $tokenB")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should archive job`() {
        val token = registerAndGetToken()
        val jobId = createJob(token, "To Archive")

        mockMvc.perform(
            delete("/api/jobs/$jobId")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `should return 404 when archiving another user's job`() {
        val tokenA = registerAndGetToken("userA@example.com")
        val jobId = createJob(tokenA, "User A Job")

        val tokenB = registerAndGetToken("userB@example.com")

        mockMvc.perform(
            delete("/api/jobs/$jobId")
                .header("Authorization", "Bearer $tokenB")
        )
            .andExpect(status().isNotFound)
    }

    // --- JOBS-04: Full description text ---

    @Test
    fun `should store and retrieve full job description`() {
        val token = registerAndGetToken()
        val longDescription = "A".repeat(2000)
        val request = CreateJobRequest(
            title = "Detailed Role",
            description = longDescription
        )

        val result = mockMvc.perform(
            post("/api/jobs")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val jobId = jsonMapper.readTree(result.response.contentAsString)
            .get("id").textValue()

        mockMvc.perform(
            get("/api/jobs/$jobId")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.description").value(longDescription))
    }

    // --- Listing and filters ---

    @Test
    fun `should list jobs with pagination`() {
        val token = registerAndGetToken()
        createJob(token, "Job A")
        createJob(token, "Job B")
        createJob(token, "Job C")

        mockMvc.perform(
            get("/api/jobs")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.content.length()").value(3))
    }

    @Test
    fun `should filter by companyId`() {
        val token = registerAndGetToken()
        val companyA = createCompany(token, "Company A")
        val companyB = createCompany(token, "Company B")
        createJobWithCompany(token, "Job at A", companyA)
        createJobWithCompany(token, "Job at B", companyB)

        mockMvc.perform(
            get("/api/jobs")
                .param("companyId", companyA)
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].companyName").value("Company A"))
    }

    @Test
    fun `should filter by jobType`() {
        val token = registerAndGetToken()
        createJobWithType(token, "Full Time Job", JobType.FULL_TIME)
        createJobWithType(token, "Contract Job", JobType.CONTRACT)

        mockMvc.perform(
            get("/api/jobs")
                .param("jobType", "FULL_TIME")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Full Time Job"))
    }

    @Test
    fun `should filter by workMode`() {
        val token = registerAndGetToken()
        createJobWithWorkMode(token, "Remote Job", WorkMode.REMOTE)
        createJobWithWorkMode(token, "Onsite Job", WorkMode.ONSITE)

        mockMvc.perform(
            get("/api/jobs")
                .param("workMode", "REMOTE")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Remote Job"))
    }

    @Test
    fun `should search by title`() {
        val token = registerAndGetToken()
        createJob(token, "Kotlin Dev")
        createJob(token, "Java Dev")

        mockMvc.perform(
            get("/api/jobs")
                .param("q", "kotlin")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Kotlin Dev"))
    }

    @Test
    fun `should exclude archived jobs by default`() {
        val token = registerAndGetToken()
        createJob(token, "Active Job")
        val archivedJobId = createJob(token, "Archived Job")

        mockMvc.perform(
            delete("/api/jobs/$archivedJobId")
                .header("Authorization", "Bearer $token")
        )

        mockMvc.perform(
            get("/api/jobs")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Active Job"))
    }

    @Test
    fun `should not return other user's jobs`() {
        val tokenA = registerAndGetToken("userA@example.com")
        createJob(tokenA, "User A Job")

        val tokenB = registerAndGetToken("userB@example.com")

        mockMvc.perform(
            get("/api/jobs")
                .header("Authorization", "Bearer $tokenB")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(0))
    }

    // --- Company archive guard ---

    @Test
    fun `should return 409 when archiving company with active jobs`() {
        val token = registerAndGetToken()
        val companyId = createCompany(token, "Guarded Corp")
        createJobWithCompany(token, "Active Job", companyId)

        mockMvc.perform(
            delete("/api/companies/$companyId")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `should allow archiving company after all linked jobs are archived`() {
        val token = registerAndGetToken()
        val companyId = createCompany(token, "Guarded Corp")
        val jobId = createJobWithCompany(token, "Job To Archive", companyId)

        // Archive the job first
        mockMvc.perform(
            delete("/api/jobs/$jobId")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNoContent)

        // Now archiving company should succeed
        mockMvc.perform(
            delete("/api/companies/$companyId")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNoContent)
    }

    // --- Helpers ---

    private fun registerAndGetToken(email: String = "test@example.com", password: String = "Password123!"): String =
        TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, email, password)

    private fun createCompany(token: String, name: String): String =
        TestHelper.createCompany(mockMvc, jsonMapper, token, name)

    private fun createJob(token: String, title: String): String {
        val request = CreateJobRequest(title = title)
        val result = mockMvc.perform(
            post("/api/jobs")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn()

        return jsonMapper.readTree(result.response.contentAsString)
            .get("id").textValue()
    }

    private fun createJobWithCompany(token: String, title: String, companyId: String): String {
        val request = CreateJobRequest(
            title = title,
            companyId = UUID.fromString(companyId)
        )
        val result = mockMvc.perform(
            post("/api/jobs")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn()

        return jsonMapper.readTree(result.response.contentAsString)
            .get("id").textValue()
    }

    private fun createJobWithType(token: String, title: String, jobType: JobType): String {
        val request = CreateJobRequest(title = title, jobType = jobType)
        val result = mockMvc.perform(
            post("/api/jobs")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn()

        return jsonMapper.readTree(result.response.contentAsString)
            .get("id").textValue()
    }

    private fun createJobWithWorkMode(token: String, title: String, workMode: WorkMode): String {
        val request = CreateJobRequest(title = title, workMode = workMode)
        val result = mockMvc.perform(
            post("/api/jobs")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn()

        return jsonMapper.readTree(result.response.contentAsString)
            .get("id").textValue()
    }
}
