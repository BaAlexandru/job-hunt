package com.alex.job.hunt.jobhunt.document

import com.alex.job.hunt.jobhunt.TestHelper
import com.alex.job.hunt.jobhunt.repository.ApplicationNoteRepository
import com.alex.job.hunt.jobhunt.repository.ApplicationRepository
import com.alex.job.hunt.jobhunt.repository.CompanyRepository
import com.alex.job.hunt.jobhunt.repository.DocumentApplicationLinkRepository
import com.alex.job.hunt.jobhunt.repository.DocumentRepository
import com.alex.job.hunt.jobhunt.repository.DocumentVersionRepository
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
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class DocumentControllerIntegrationTests {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jsonMapper: JsonMapper
    @Autowired lateinit var documentApplicationLinkRepository: DocumentApplicationLinkRepository
    @Autowired lateinit var documentVersionRepository: DocumentVersionRepository
    @Autowired lateinit var documentRepository: DocumentRepository
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

    private val pdfBytes: ByteArray by lazy {
        javaClass.classLoader.getResourceAsStream("fixtures/test.pdf")!!.readAllBytes()
    }

    private val docxBytes: ByteArray by lazy {
        javaClass.classLoader.getResourceAsStream("fixtures/test.docx")!!.readAllBytes()
    }

    @BeforeEach
    fun setUp() {
        documentApplicationLinkRepository.deleteAll()
        documentVersionRepository.deleteAll()
        documentRepository.deleteAll()
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

    // --- DOCS-01: Upload ---

    @Test
    fun uploadPdfDocument_returns201() {
        val token = registerAndGetToken("pdf-upload@test.com")

        val result = uploadDocument(token, "My CV", "CV", "Test CV", pdfBytes, "test.pdf")

        result.andExpect(status().isCreated)
            .andExpect(jsonPath("$.title").value("My CV"))
            .andExpect(jsonPath("$.category").value("CV"))
            .andExpect(jsonPath("$.description").value("Test CV"))
            .andExpect(jsonPath("$.currentVersion.versionNumber").value(1))
            .andExpect(jsonPath("$.currentVersion.contentType").value("application/pdf"))
    }

    @Test
    fun uploadDocxDocument_returns201() {
        val token = registerAndGetToken("docx-upload@test.com")

        val result = uploadDocument(token, "Cover Letter", "COVER_LETTER", null, docxBytes, "test.docx")

        result.andExpect(status().isCreated)
            .andExpect(jsonPath("$.category").value("COVER_LETTER"))
            .andExpect(jsonPath("$.currentVersion.contentType").value("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
    }

    @Test
    fun rejectInvalidFileType_returns400() {
        val token = registerAndGetToken("invalid-type@test.com")
        val txtBytes = "Hello world".toByteArray()

        val result = uploadDocument(token, "Notes", "OTHER", null, txtBytes, "notes.txt")

        result.andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not allowed")))
    }

    // --- DOCS-02: Linking ---

    @Test
    fun linkDocumentToApplication_returns201() {
        val token = registerAndGetToken("link-doc@test.com")
        val docId = uploadAndGetId(token, "CV for App", "CV")
        val versionId = getDocumentCurrentVersionId(token, docId)
        val appId = createTestApplication(token)

        mockMvc.perform(
            post("/api/documents/links")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"documentVersionId":"$versionId","applicationId":"$appId"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.documentVersionId").value(versionId))
            .andExpect(jsonPath("$.applicationId").value(appId))
    }

    @Test
    fun linkDuplicateReturns409() {
        val token = registerAndGetToken("dup-link@test.com")
        val docId = uploadAndGetId(token, "CV Dup Link", "CV")
        val versionId = getDocumentCurrentVersionId(token, docId)
        val appId = createTestApplication(token)

        val body = """{"documentVersionId":"$versionId","applicationId":"$appId"}"""

        mockMvc.perform(
            post("/api/documents/links")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/api/documents/links")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isConflict)
    }

    @Test
    fun getLinksForApplication_returnsLinks() {
        val token = registerAndGetToken("get-links@test.com")
        val doc1Id = uploadAndGetId(token, "CV 1", "CV")
        val doc2Id = uploadAndGetId(token, "CV 2", "CV")
        val v1Id = getDocumentCurrentVersionId(token, doc1Id)
        val v2Id = getDocumentCurrentVersionId(token, doc2Id)
        val appId = createTestApplication(token)

        linkDocument(token, v1Id, appId)
        linkDocument(token, v2Id, appId)

        mockMvc.perform(
            get("/api/documents/links/application/$appId")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun unlinkDocument_returns204() {
        val token = registerAndGetToken("unlink@test.com")
        val docId = uploadAndGetId(token, "CV Unlink", "CV")
        val versionId = getDocumentCurrentVersionId(token, docId)
        val appId = createTestApplication(token)

        linkDocument(token, versionId, appId)

        mockMvc.perform(
            delete("/api/documents/links")
                .param("documentVersionId", versionId)
                .param("applicationId", appId)
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isNoContent)

        mockMvc.perform(
            get("/api/documents/links/application/$appId")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    // --- DOCS-03: Versioning ---

    @Test
    fun createNewVersion_returns201() {
        val token = registerAndGetToken("new-version@test.com")
        val docId = uploadAndGetId(token, "CV Version", "CV")

        val file = MockMultipartFile("file", "v2.pdf", "application/pdf", pdfBytes)
        val note = MockMultipartFile("note", "", "text/plain", "Updated version".toByteArray())

        mockMvc.perform(
            multipart("/api/documents/$docId/versions")
                .file(file)
                .file(note)
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.versionNumber").value(2))
            .andExpect(jsonPath("$.isCurrent").value(true))
    }

    @Test
    fun listVersions_returnsAllVersions() {
        val token = registerAndGetToken("list-versions@test.com")
        val docId = uploadAndGetId(token, "CV List Ver", "CV")

        // Upload second version
        val file = MockMultipartFile("file", "v2.pdf", "application/pdf", pdfBytes)
        mockMvc.perform(
            multipart("/api/documents/$docId/versions")
                .file(file)
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isCreated)

        mockMvc.perform(
            get("/api/documents/$docId/versions")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].versionNumber").value(2))
            .andExpect(jsonPath("$[1].versionNumber").value(1))
    }

    @Test
    fun setCurrentVersion_returns200() {
        val token = registerAndGetToken("set-current@test.com")
        val docId = uploadAndGetId(token, "CV Set Current", "CV")

        // Get v1 id
        val v1Id = getDocumentCurrentVersionId(token, docId)

        // Upload v2
        val file = MockMultipartFile("file", "v2.pdf", "application/pdf", pdfBytes)
        mockMvc.perform(
            multipart("/api/documents/$docId/versions")
                .file(file)
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isCreated)

        // Set v1 as current
        mockMvc.perform(
            put("/api/documents/$docId/versions/$v1Id/current")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isCurrent").value(true))
            .andExpect(jsonPath("$.versionNumber").value(1))

        // Verify document shows v1 as current
        mockMvc.perform(
            get("/api/documents/$docId")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(jsonPath("$.currentVersion.versionNumber").value(1))
    }

    @Test
    fun deleteVersion_returns204() {
        val token = registerAndGetToken("delete-ver@test.com")
        val docId = uploadAndGetId(token, "CV Delete Ver", "CV")
        val v1Id = getDocumentCurrentVersionId(token, docId)

        // Upload v2
        val file = MockMultipartFile("file", "v2.pdf", "application/pdf", pdfBytes)
        mockMvc.perform(
            multipart("/api/documents/$docId/versions")
                .file(file)
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isCreated)

        // Delete v1
        mockMvc.perform(
            delete("/api/documents/$docId/versions/$v1Id")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isNoContent)

        // Verify only 1 version left
        mockMvc.perform(
            get("/api/documents/$docId/versions")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun deleteOnlyVersion_returns409() {
        val token = registerAndGetToken("delete-only@test.com")
        val docId = uploadAndGetId(token, "CV Only Ver", "CV")
        val v1Id = getDocumentCurrentVersionId(token, docId)

        mockMvc.perform(
            delete("/api/documents/$docId/versions/$v1Id")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isConflict)
    }

    // --- DOCS-04: Download ---

    @Test
    fun downloadDocument_returns200WithFile() {
        val token = registerAndGetToken("download@test.com")
        val docId = uploadAndGetId(token, "CV Download", "CV")
        val versionId = getDocumentCurrentVersionId(token, docId)

        mockMvc.perform(
            get("/api/documents/$docId/versions/$versionId/download")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "application/pdf"))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment; filename=")))
    }

    // --- DOCS-05: Categorization and Filtering ---

    @Test
    fun filterByCategory_returnsMatching() {
        val token = registerAndGetToken("filter-cat@test.com")
        uploadAndGetId(token, "CV 1", "CV")
        uploadAndGetId(token, "CV 2", "CV")
        uploadAndGetId(token, "Cover Letter 1", "COVER_LETTER")

        mockMvc.perform(
            get("/api/documents")
                .param("category", "CV")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(2))
    }

    @Test
    fun searchByTitle_returnsMatching() {
        val token = registerAndGetToken("search-title@test.com")
        uploadAndGetId(token, "Backend Developer CV", "CV")
        uploadAndGetId(token, "Frontend CV", "CV")

        mockMvc.perform(
            get("/api/documents")
                .param("search", "backend")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
    }

    @Test
    fun listDocuments_returnsPaginated() {
        val token = registerAndGetToken("paginate@test.com")
        uploadAndGetId(token, "Doc 1", "CV")
        uploadAndGetId(token, "Doc 2", "CV")
        uploadAndGetId(token, "Doc 3", "CV")

        mockMvc.perform(
            get("/api/documents")
                .param("size", "2")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.content.length()").value(2))
    }

    // --- Cross-cutting ---

    @Test
    fun getOtherUsersDocument_returns404() {
        val token1 = registerAndGetToken("user-a@test.com")
        val docId = uploadAndGetId(token1, "User A's CV", "CV")

        val token2 = registerAndGetToken("user-b@test.com")

        mockMvc.perform(
            get("/api/documents/$docId")
                .header("Authorization", "Bearer $token2")
        ).andExpect(status().isNotFound)
    }

    @Test
    fun archiveDocument_returns204() {
        val token = registerAndGetToken("archive@test.com")
        val docId = uploadAndGetId(token, "Archive CV", "CV")

        mockMvc.perform(
            delete("/api/documents/$docId")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isNoContent)

        mockMvc.perform(
            get("/api/documents/$docId")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isNotFound)
    }

    @Test
    fun updateDocument_returns200() {
        val token = registerAndGetToken("update-doc@test.com")
        val docId = uploadAndGetId(token, "Old Title", "CV")

        mockMvc.perform(
            put("/api/documents/$docId")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"New Title","category":"CV"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("New Title"))
    }

    // --- Helpers ---

    private fun registerAndGetToken(email: String): String =
        TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, email)

    private fun createTestApplication(token: String): String {
        val jobId = TestHelper.createJob(mockMvc, jsonMapper, token, "Test Job")
        return TestHelper.createApplication(mockMvc, jsonMapper, token, jobId)
    }

    private fun uploadDocument(
        token: String,
        title: String,
        category: String,
        description: String?,
        fileBytes: ByteArray,
        filename: String
    ): org.springframework.test.web.servlet.ResultActions {
        val file = MockMultipartFile("file", filename, "application/octet-stream", fileBytes)
        val titlePart = MockMultipartFile("title", "", "text/plain", title.toByteArray())
        val categoryPart = MockMultipartFile("category", "", "text/plain", category.toByteArray())

        val builder = multipart("/api/documents")
            .file(file)
            .file(titlePart)
            .file(categoryPart)
            .header("Authorization", "Bearer $token")

        if (description != null) {
            val descPart = MockMultipartFile("description", "", "text/plain", description.toByteArray())
            builder.file(descPart)
        }

        return mockMvc.perform(builder)
    }

    private fun uploadAndGetId(token: String, title: String, category: String): String {
        val result = uploadDocument(token, title, category, null, pdfBytes, "test.pdf")
            .andExpect(status().isCreated)
            .andReturn()
        return jsonMapper.readTree(result.response.contentAsString).get("id").textValue()
    }

    private fun getDocumentCurrentVersionId(token: String, docId: String): String {
        val result = mockMvc.perform(
            get("/api/documents/$docId")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andReturn()
        return jsonMapper.readTree(result.response.contentAsString)
            .get("currentVersion").get("id").textValue()
    }

    private fun linkDocument(token: String, versionId: String, appId: String) {
        mockMvc.perform(
            post("/api/documents/links")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"documentVersionId":"$versionId","applicationId":"$appId"}""")
        ).andExpect(status().isCreated)
    }
}
