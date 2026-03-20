package com.alex.job.hunt.jobhunt.interview

import com.alex.job.hunt.jobhunt.TestHelper
import com.alex.job.hunt.jobhunt.repository.ApplicationNoteRepository
import com.alex.job.hunt.jobhunt.repository.ApplicationRepository
import com.alex.job.hunt.jobhunt.repository.CompanyRepository
import com.alex.job.hunt.jobhunt.repository.EmailVerificationTokenRepository
import com.alex.job.hunt.jobhunt.repository.InterviewNoteRepository
import com.alex.job.hunt.jobhunt.repository.InterviewRepository
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class InterviewNoteControllerIntegrationTests {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jsonMapper: JsonMapper
    @Autowired lateinit var interviewNoteRepository: InterviewNoteRepository
    @Autowired lateinit var interviewRepository: InterviewRepository
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
        interviewNoteRepository.deleteAll()
        interviewRepository.deleteAll()
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

    @Test
    fun `should create interview note with 201`() {
        val token = registerAndGetToken("note-create@test.com")
        val jobId = createJob(token, "Note Job")
        val appId = createApplication(token, jobId)
        val intId = createInterview(token, appId)

        mockMvc.perform(
            post("/api/interviews/$intId/notes")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"content":"Prepared for technical questions","noteType":"PREPARATION"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.content").value("Prepared for technical questions"))
            .andExpect(jsonPath("$.noteType").value("PREPARATION"))
            .andExpect(jsonPath("$.interviewId").value(intId))
    }

    @Test
    fun `should list interview notes`() {
        val token = registerAndGetToken("note-list@test.com")
        val jobId = createJob(token, "List Note Job")
        val appId = createApplication(token, jobId)
        val intId = createInterview(token, appId)

        createInterviewNote(token, intId, "Note 1")
        createInterviewNote(token, intId, "Note 2")

        mockMvc.perform(
            get("/api/interviews/$intId/notes")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(2))
    }

    @Test
    fun `should update interview note`() {
        val token = registerAndGetToken("note-update@test.com")
        val jobId = createJob(token, "Update Note Job")
        val appId = createApplication(token, jobId)
        val intId = createInterview(token, appId)
        val noteId = createInterviewNote(token, intId, "Original")

        mockMvc.perform(
            put("/api/interviews/$intId/notes/$noteId")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"content":"Updated content"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").value("Updated content"))
    }

    @Test
    fun `should delete interview note with 204`() {
        val token = registerAndGetToken("note-delete@test.com")
        val jobId = createJob(token, "Delete Note Job")
        val appId = createApplication(token, jobId)
        val intId = createInterview(token, appId)
        val noteId = createInterviewNote(token, intId, "To delete")

        mockMvc.perform(
            delete("/api/interviews/$intId/notes/$noteId")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isNoContent)

        mockMvc.perform(
            get("/api/interviews/$intId/notes")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(0))
    }

    @Test
    fun `should return 404 when interview not found`() {
        val token = registerAndGetToken("note-404@test.com")
        val randomId = UUID.randomUUID()

        mockMvc.perform(
            post("/api/interviews/$randomId/notes")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"content":"Orphan note"}""")
        ).andExpect(status().isNotFound)
    }

    @Test
    fun `should return 404 when creating note on archived interview`() {
        val token = registerAndGetToken("note-archived@test.com")
        val jobId = createJob(token, "Archived Note Job")
        val appId = createApplication(token, jobId)
        val intId = createInterview(token, appId)

        // Archive the interview
        mockMvc.perform(
            delete("/api/interviews/$intId").header("Authorization", "Bearer $token")
        ).andExpect(status().isNoContent)

        // Try to create a note on archived interview
        mockMvc.perform(
            post("/api/interviews/$intId/notes")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"content":"Should fail"}""")
        ).andExpect(status().isNotFound)
    }

    @Test
    fun `should not access notes of other user interview`() {
        val token1 = registerAndGetToken("user1-note@test.com")
        val jobId = createJob(token1, "Isolation Note Job")
        val appId = createApplication(token1, jobId)
        val intId = createInterview(token1, appId)

        val token2 = registerAndGetToken("user2-note@test.com")

        mockMvc.perform(
            post("/api/interviews/$intId/notes")
                .header("Authorization", "Bearer $token2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"content":"Unauthorized note"}""")
        ).andExpect(status().isNotFound)
    }

    // --- Helpers ---

    private fun registerAndGetToken(email: String): String =
        TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, email)

    private fun createJob(token: String, title: String): String =
        TestHelper.createJob(mockMvc, jsonMapper, token, title)

    private fun createApplication(token: String, jobId: String): String =
        TestHelper.createApplication(mockMvc, jsonMapper, token, jobId)

    private fun createInterview(token: String, appId: String): String =
        TestHelper.createInterview(mockMvc, jsonMapper, token, appId)

    private fun createInterviewNote(token: String, intId: String, content: String): String =
        TestHelper.createInterviewNote(mockMvc, jsonMapper, token, intId, content)
}
