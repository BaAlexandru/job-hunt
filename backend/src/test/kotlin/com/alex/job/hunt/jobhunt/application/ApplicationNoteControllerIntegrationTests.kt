package com.alex.job.hunt.jobhunt.application

import com.alex.job.hunt.jobhunt.TestHelper
import com.alex.job.hunt.jobhunt.dto.CreateNoteRequest
import com.alex.job.hunt.jobhunt.dto.UpdateNoteRequest
import com.alex.job.hunt.jobhunt.entity.NoteType
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ApplicationNoteControllerIntegrationTests {

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

    @Test
    fun createNoteSuccess() {
        val token = registerAndGetToken("note-create@test.com")
        val jobId = createJob(token, "Note Job")
        val appId = createApplication(token, jobId)

        val request = CreateNoteRequest(content = "Initial thoughts", noteType = NoteType.GENERAL)
        mockMvc.perform(
            post("/api/applications/$appId/notes")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.content").value("Initial thoughts"))
            .andExpect(jsonPath("$.noteType").value("GENERAL"))
    }

    @Test
    fun listNotes() {
        val token = registerAndGetToken("note-list@test.com")
        val jobId = createJob(token, "List Note Job")
        val appId = createApplication(token, jobId)
        addNote(token, appId, "Note 1")
        addNote(token, appId, "Note 2")
        addNote(token, appId, "Note 3")

        mockMvc.perform(
            get("/api/applications/$appId/notes")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(3))
    }

    @Test
    fun updateNote() {
        val token = registerAndGetToken("note-update@test.com")
        val jobId = createJob(token, "Update Note Job")
        val appId = createApplication(token, jobId)
        val noteId = addNoteAndGetId(token, appId, "Original")
        val updateReq = UpdateNoteRequest(content = "Updated content")
        mockMvc.perform(
            put("/api/applications/$appId/notes/$noteId")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(updateReq))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").value("Updated content"))
    }

    @Test
    fun deleteNote() {
        val token = registerAndGetToken("note-delete@test.com")
        val jobId = createJob(token, "Delete Note Job")
        val appId = createApplication(token, jobId)
        val noteId = addNoteAndGetId(token, appId, "To delete")
        mockMvc.perform(
            delete("/api/applications/$appId/notes/$noteId")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isNoContent)
        mockMvc.perform(
            get("/api/applications/$appId/notes")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(0))
    }

    @Test
    fun cannotDeleteStatusChangeNote() {
        val token = registerAndGetToken("no-del-status@test.com")
        val jobId = createJob(token, "Status Note Job")
        val appId = createApplication(token, jobId)
        mockMvc.perform(
            patch("/api/applications/$appId/status")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"APPLIED\"}")
        ).andExpect(status().isOk)

        val notesResult = mockMvc.perform(
            get("/api/applications/$appId/notes")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isOk).andReturn()

        val notes = jsonMapper.readTree(notesResult.response.contentAsString).get("content")
        var statusNoteId = ""
        for (i in 0 until notes.size()) {
            if (notes.get(i).get("noteType").textValue() == "STATUS_CHANGE") {
                statusNoteId = notes.get(i).get("id").textValue()
                break
            }
        }

        mockMvc.perform(
            delete("/api/applications/$appId/notes/$statusNoteId")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isConflict)
    }

    @Test
    fun statusChangeNoteAutoCreated() {
        val token = registerAndGetToken("auto-note@test.com")
        val jobId = createJob(token, "Auto Note Job")
        val appId = createApplication(token, jobId)
        mockMvc.perform(
            patch("/api/applications/$appId/status")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"APPLIED\"}")
        ).andExpect(status().isOk)

        mockMvc.perform(
            get("/api/applications/$appId/notes")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].noteType").value("STATUS_CHANGE"))
            .andExpect(jsonPath("$.content[0].content").value("Status changed: INTERESTED -> APPLIED"))
    }

    @Test
    fun noteCreationUpdatesLastActivityDate() {
        val token = registerAndGetToken("note-activity@test.com")
        val jobId = createJob(token, "Activity Note Job")
        val appId = createApplication(token, jobId)

        val before = mockMvc.perform(
            get("/api/applications/$appId")
                .header("Authorization", "Bearer $token")
        ).andReturn()
        val dateBefore = jsonMapper.readTree(before.response.contentAsString).get("lastActivityDate").textValue()

        addNote(token, appId, "A new note")

        val after = mockMvc.perform(
            get("/api/applications/$appId")
                .header("Authorization", "Bearer $token")
        ).andReturn()
        val dateAfter = jsonMapper.readTree(after.response.contentAsString).get("lastActivityDate").textValue()

        assert(dateAfter > dateBefore) { "lastActivityDate should update after note creation" }
    }

    @Test
    fun noteWithType() {
        val token = registerAndGetToken("note-type@test.com")
        val jobId = createJob(token, "Type Note Job")
        val appId = createApplication(token, jobId)
        val request = CreateNoteRequest(content = "Had a call", noteType = NoteType.PHONE_CALL)
        mockMvc.perform(
            post("/api/applications/$appId/notes")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.noteType").value("PHONE_CALL"))
    }

    // --- Helpers ---

    private fun registerAndGetToken(email: String = "test@example.com", password: String = "Password123!"): String =
        TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, email, password)

    private fun createJob(token: String, title: String): String =
        TestHelper.createJob(mockMvc, jsonMapper, token, title)

    private fun createApplication(token: String, jobId: String): String =
        TestHelper.createApplication(mockMvc, jsonMapper, token, jobId)

    private fun addNote(token: String, appId: String, content: String) {
        val request = CreateNoteRequest(content = content)
        mockMvc.perform(
            post("/api/applications/$appId/notes")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        ).andExpect(status().isCreated)
    }

    private fun addNoteAndGetId(token: String, appId: String, content: String): String {
        val request = CreateNoteRequest(content = content)
        val result = mockMvc.perform(
            post("/api/applications/$appId/notes")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn()
        return jsonMapper.readTree(result.response.contentAsString).get("id").textValue()
    }
}
