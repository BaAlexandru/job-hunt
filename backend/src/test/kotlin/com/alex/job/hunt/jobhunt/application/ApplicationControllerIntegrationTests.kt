package com.alex.job.hunt.jobhunt.application

import com.alex.job.hunt.jobhunt.TestHelper
import com.alex.job.hunt.jobhunt.dto.CreateApplicationRequest
import com.alex.job.hunt.jobhunt.dto.CreateJobRequest
import com.alex.job.hunt.jobhunt.dto.UpdateApplicationRequest
import com.alex.job.hunt.jobhunt.repository.ApplicationNoteRepository
import com.alex.job.hunt.jobhunt.repository.ApplicationRepository
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
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ApplicationControllerIntegrationTests {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jsonMapper: JsonMapper
    @Autowired lateinit var applicationNoteRepository: ApplicationNoteRepository
    @Autowired lateinit var applicationRepository: ApplicationRepository
    @Autowired lateinit var jobRepository: JobRepository
    @Autowired lateinit var companyRepository: CompanyRepository
    @Autowired lateinit var tokenBlocklistRepository: TokenBlocklistRepository
    @Autowired lateinit var passwordResetTokenRepository: PasswordResetTokenRepository
    @Autowired lateinit var emailVerificationTokenRepository: EmailVerificationTokenRepository
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var redisTemplate: StringRedisTemplate

    @BeforeEach
    fun setUp() {
        applicationNoteRepository.deleteAll()
        applicationRepository.deleteAll()
        jobRepository.deleteAll()
        companyRepository.deleteAll()
        tokenBlocklistRepository.deleteAll()
        passwordResetTokenRepository.deleteAll()
        emailVerificationTokenRepository.deleteAll()
        userRepository.deleteAll()
        redisTemplate.connectionFactory?.connection?.serverCommands()?.flushAll()
    }

    // --- APPL-01: Application CRUD ---

    @Test
    fun createApplicationSuccess() {
        val token = registerAndGetToken("create-app@test.com")
        val jobId = createJob(token, "Software Engineer")
        val appId = createApplication(token, jobId)

        mockMvc.perform(
            get("/api/applications/$appId")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.jobId").value(jobId))
            .andExpect(jsonPath("$.status").value("INTERESTED"))
            .andExpect(jsonPath("$.jobTitle").value("Software Engineer"))
    }

    @Test
    fun createApplicationDuplicateJob() {
        val token = registerAndGetToken("dup-app@test.com")
        val jobId = createJob(token, "Unique Job")
        createApplication(token, jobId)

        mockMvc.perform(
            post("/api/applications")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(CreateApplicationRequest(jobId = UUID.fromString(jobId))))
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun createApplicationInvalidJob() {
        val token = registerAndGetToken("invalid-job@test.com")

        mockMvc.perform(
            post("/api/applications")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(CreateApplicationRequest(jobId = UUID.randomUUID())))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun getApplicationById() {
        val token = registerAndGetToken("get-app@test.com")
        val jobId = createJob(token, "Test Job")
        val appId = createApplication(token, jobId)

        mockMvc.perform(
            get("/api/applications/$appId")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(appId))
            .andExpect(jsonPath("$.jobId").value(jobId))
            .andExpect(jsonPath("$.status").exists())
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.updatedAt").exists())
    }

    @Test
    fun listApplications() {
        val token = registerAndGetToken("list-app@test.com")
        val job1 = createJob(token, "Job A")
        val job2 = createJob(token, "Job B")
        createApplication(token, job1)
        createApplication(token, job2)

        mockMvc.perform(
            get("/api/applications")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content.length()").value(2))
    }

    @Test
    fun updateApplication() {
        val token = registerAndGetToken("update-app@test.com")
        val jobId = createJob(token, "Update Job")
        val appId = createApplication(token, jobId)

        val updateReq = UpdateApplicationRequest(
            quickNotes = "Great opportunity",
            nextActionDate = LocalDate.of(2026, 4, 1)
        )

        mockMvc.perform(
            put("/api/applications/$appId")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(updateReq))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.quickNotes").value("Great opportunity"))
            .andExpect(jsonPath("$.nextActionDate").value("2026-04-01"))
    }

    @Test
    fun archiveApplication() {
        val token = registerAndGetToken("archive-app@test.com")
        val jobId = createJob(token, "Archive Job")
        val appId = createApplication(token, jobId)

        mockMvc.perform(
            delete("/api/applications/$appId")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNoContent)
    }

    // --- APPL-02: Status Transitions ---

    @Test
    fun validStatusTransition() {
        val token = registerAndGetToken("valid-trans@test.com")
        val jobId = createJob(token, "Trans Job")
        val appId = createApplication(token, jobId)

        updateStatus(token, appId, "APPLIED")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("APPLIED"))
    }

    @Test
    fun invalidStatusTransition() {
        val token = registerAndGetToken("invalid-trans@test.com")
        val jobId = createJob(token, "Trans Job 2")
        val appId = createApplication(token, jobId)

        updateStatus(token, appId, "INTERVIEW")
            .andExpect(status().isUnprocessableEntity)
    }

    @Test
    fun terminalStatusReversal() {
        val token = registerAndGetToken("terminal-rev@test.com")
        val jobId = createJob(token, "Terminal Job")
        val appId = createApplication(token, jobId)

        updateStatus(token, appId, "WITHDRAWN")
            .andExpect(status().isOk)

        updateStatus(token, appId, "APPLIED")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("APPLIED"))
    }

    @Test
    fun withdrawnFromAnyActive() {
        val token = registerAndGetToken("withdrawn-any@test.com")
        val job1 = createJob(token, "W Job 1")
        val app1 = createApplication(token, job1)
        updateStatus(token, app1, "APPLIED")
        updateStatus(token, app1, "WITHDRAWN")
            .andExpect(status().isOk)

        val job2 = createJob(token, "W Job 2")
        val app2 = createApplication(token, job2)
        updateStatus(token, app2, "APPLIED")
        updateStatus(token, app2, "PHONE_SCREEN")
        updateStatus(token, app2, "WITHDRAWN")
            .andExpect(status().isOk)
    }

    @Test
    fun rejectedFromAfterApplied() {
        val token = registerAndGetToken("rejected@test.com")
        val jobId = createJob(token, "Reject Job")
        val appId = createApplication(token, jobId)
        updateStatus(token, appId, "APPLIED")
        updateStatus(token, appId, "REJECTED")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("REJECTED"))
    }

    @Test
    fun getValidTransitions() {
        val token = registerAndGetToken("transitions@test.com")
        val jobId = createJob(token, "Transitions Job")
        val appId = createApplication(token, jobId)

        mockMvc.perform(
            get("/api/applications/$appId/transitions")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
    }

    // --- APPL-05: Date Auto-tracking ---

    @Test
    fun appliedDateAutoSet() {
        val token = registerAndGetToken("auto-date@test.com")
        val jobId = createJob(token, "Auto Date Job")
        val appId = createApplication(token, jobId)

        updateStatus(token, appId, "APPLIED")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.appliedDate").value(LocalDate.now().toString()))
    }

    @Test
    fun appliedDateNotOverridden() {
        val token = registerAndGetToken("no-override@test.com")
        val jobId = createJob(token, "No Override Job")

        val request = CreateApplicationRequest(
            jobId = UUID.fromString(jobId),
            appliedDate = LocalDate.of(2026, 1, 15)
        )
        val result = mockMvc.perform(
            post("/api/applications")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val appId = jsonMapper.readTree(result.response.contentAsString).get("id").textValue()

        updateStatus(token, appId, "APPLIED")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.appliedDate").value("2026-01-15"))
    }

    @Test
    fun lastActivityDateUpdatesOnStatusChange() {
        val token = registerAndGetToken("activity-date@test.com")
        val jobId = createJob(token, "Activity Job")
        val appId = createApplication(token, jobId)

        val before = mockMvc.perform(
            get("/api/applications/$appId")
                .header("Authorization", "Bearer $token")
        ).andReturn()
        val dateBefore = jsonMapper.readTree(before.response.contentAsString).get("lastActivityDate").textValue()

        Thread.sleep(50)

        updateStatus(token, appId, "APPLIED")

        val after = mockMvc.perform(
            get("/api/applications/$appId")
                .header("Authorization", "Bearer $token")
        ).andReturn()
        val dateAfter = jsonMapper.readTree(after.response.contentAsString).get("lastActivityDate").textValue()

        assert(dateAfter > dateBefore) { "lastActivityDate should update after status change" }
    }

    // --- APPL-07: Search and Filter ---

    @Test
    fun searchByJobTitle() {
        val token = registerAndGetToken("search-title@test.com")
        val job1 = createJob(token, "Software Engineer")
        val job2 = createJob(token, "Product Manager")
        createApplication(token, job1)
        createApplication(token, job2)

        mockMvc.perform(
            get("/api/applications").param("q", "software")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].jobTitle").value("Software Engineer"))
    }

    @Test
    fun searchByQuickNotes() {
        val token = registerAndGetToken("search-notes@test.com")
        val job1 = createJob(token, "Job A")
        val job2 = createJob(token, "Job B")
        val app1 = createApplication(token, job1)
        createApplication(token, job2)

        mockMvc.perform(
            put("/api/applications/$app1")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(UpdateApplicationRequest(quickNotes = "Great opportunity")))
        ).andExpect(status().isOk)

        mockMvc.perform(
            get("/api/applications").param("q", "opportunity")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
    }

    @Test
    fun searchByCompanyName() {
        val token = registerAndGetToken("search-company@test.com")
        val companyId = createCompany(token, "Acme Corp")
        val job1 = createJobWithCompany(token, "Dev at Acme", companyId)
        val job2 = createJob(token, "Other Job")
        createApplication(token, job1)
        createApplication(token, job2)

        mockMvc.perform(
            get("/api/applications").param("q", "acme")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].companyName").value("Acme Corp"))
    }

    @Test
    fun searchByNoteContent() {
        val token = registerAndGetToken("search-note@test.com")
        val job1 = createJob(token, "Note Job")
        val job2 = createJob(token, "Other Job")
        val app1 = createApplication(token, job1)
        createApplication(token, job2)

        addNote(token, app1, "Follow up on Monday")

        mockMvc.perform(
            get("/api/applications").param("q", "monday")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
    }

    @Test
    fun searchByJobDescription() {
        val token = registerAndGetToken("search-desc@test.com")
        val job1 = createJobWithDescription(token, "Backend Dev", "Kotlin microservices")
        val job2 = createJob(token, "Frontend Dev")
        createApplication(token, job1)
        createApplication(token, job2)

        mockMvc.perform(
            get("/api/applications").param("q", "microservices")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
    }

    @Test
    fun filterByStatus() {
        val token = registerAndGetToken("filter-status@test.com")
        val job1 = createJob(token, "Status Job 1")
        val job2 = createJob(token, "Status Job 2")
        val job3 = createJob(token, "Status Job 3")
        val app1 = createApplication(token, job1)
        val app2 = createApplication(token, job2)
        createApplication(token, job3)
        updateStatus(token, app1, "APPLIED")
        updateStatus(token, app2, "APPLIED")
        updateStatus(token, app2, "INTERVIEW")

        mockMvc.perform(
            get("/api/applications").param("status", "APPLIED")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
    }

    @Test
    fun filterByMultipleStatuses() {
        val token = registerAndGetToken("multi-status@test.com")
        val job1 = createJob(token, "MS Job 1")
        val job2 = createJob(token, "MS Job 2")
        val job3 = createJob(token, "MS Job 3")
        val app1 = createApplication(token, job1)
        val app2 = createApplication(token, job2)
        createApplication(token, job3)
        updateStatus(token, app1, "APPLIED")
        updateStatus(token, app2, "APPLIED")
        updateStatus(token, app2, "INTERVIEW")

        mockMvc.perform(
            get("/api/applications").param("status", "APPLIED", "INTERVIEW")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(2))
    }

    @Test
    fun filterByDateRange() {
        val token = registerAndGetToken("date-range@test.com")
        val job1 = createJob(token, "Date Job 1")
        val job2 = createJob(token, "Date Job 2")
        val app1 = createApplication(token, job1)
        val app2 = createApplication(token, job2)

        mockMvc.perform(
            put("/api/applications/$app1")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(UpdateApplicationRequest(appliedDate = LocalDate.of(2026, 1, 10))))
        )
        mockMvc.perform(
            put("/api/applications/$app2")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(UpdateApplicationRequest(appliedDate = LocalDate.of(2026, 3, 15))))
        )

        mockMvc.perform(
            get("/api/applications")
                .param("dateFrom", "2026-01-01")
                .param("dateTo", "2026-02-01")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
    }

    @Test
    fun filterByHasNextAction() {
        val token = registerAndGetToken("next-action@test.com")
        val job1 = createJob(token, "Action Job 1")
        val job2 = createJob(token, "Action Job 2")
        val app1 = createApplication(token, job1)
        createApplication(token, job2)

        mockMvc.perform(
            put("/api/applications/$app1")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(UpdateApplicationRequest(nextActionDate = LocalDate.of(2026, 5, 1))))
        )

        mockMvc.perform(
            get("/api/applications").param("hasNextAction", "true")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
    }

    @Test
    fun userIsolation() {
        val tokenA = registerAndGetToken("userA-iso@test.com")
        val jobId = createJob(tokenA, "User A Job")
        createApplication(tokenA, jobId)

        val tokenB = registerAndGetToken("userB-iso@test.com")

        mockMvc.perform(
            get("/api/applications")
                .header("Authorization", "Bearer $tokenB")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(0))
    }

    @Test
    fun createApplicationAfterArchive() {
        val token = registerAndGetToken("archive-reapply@test.com")
        val jobId = createJob(token, "Reapply Job")
        val appId = createApplication(token, jobId)

        mockMvc.perform(
            delete("/api/applications/$appId")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isNoContent)

        val newAppResult = mockMvc.perform(
            post("/api/applications")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(CreateApplicationRequest(jobId = UUID.fromString(jobId))))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val newAppId = jsonMapper.readTree(newAppResult.response.contentAsString).get("id").textValue()
        assert(newAppId != appId) { "New application should have different ID" }
    }

    // --- Helpers ---

    private fun registerAndGetToken(email: String = "test@example.com", password: String = "Password123!"): String =
        TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, email, password)

    private fun createCompany(token: String, name: String): String =
        TestHelper.createCompany(mockMvc, jsonMapper, token, name)

    private fun createJob(token: String, title: String): String =
        TestHelper.createJob(mockMvc, jsonMapper, token, title)

    private fun createApplication(token: String, jobId: String): String =
        TestHelper.createApplication(mockMvc, jsonMapper, token, jobId)

    private fun createJobWithCompany(token: String, title: String, companyId: String): String {
        val request = CreateJobRequest(title = title, companyId = UUID.fromString(companyId))
        val result = mockMvc.perform(
            post("/api/jobs")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn()
        return jsonMapper.readTree(result.response.contentAsString).get("id").textValue()
    }

    private fun createJobWithDescription(token: String, title: String, description: String): String {
        val request = CreateJobRequest(title = title, description = description)
        val result = mockMvc.perform(
            post("/api/jobs")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn()
        return jsonMapper.readTree(result.response.contentAsString).get("id").textValue()
    }

    private fun updateStatus(token: String, appId: String, status: String) =
        mockMvc.perform(
            patch("/api/applications/$appId/status")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"$status\"}")
        )

    private fun addNote(token: String, appId: String, content: String) {
        mockMvc.perform(
            post("/api/applications/$appId/notes")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"$content\"}")
        ).andExpect(status().isCreated)
    }
}
