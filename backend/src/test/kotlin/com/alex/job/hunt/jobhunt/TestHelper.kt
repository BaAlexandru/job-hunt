package com.alex.job.hunt.jobhunt

import com.alex.job.hunt.jobhunt.dto.AuthRequest
import com.alex.job.hunt.jobhunt.dto.CreateApplicationRequest
import com.alex.job.hunt.jobhunt.dto.CreateCompanyRequest
import com.alex.job.hunt.jobhunt.dto.CreateJobRequest
import com.alex.job.hunt.jobhunt.dto.RegisterRequest
import java.util.UUID
import com.alex.job.hunt.jobhunt.repository.EmailVerificationTokenRepository
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper

@Suppress("LongParameterList")
object TestHelper {

    fun registerAndGetToken(
        mockMvc: MockMvc,
        jsonMapper: JsonMapper,
        emailVerificationTokenRepository: EmailVerificationTokenRepository,
        email: String = "test@example.com",
        password: String = "Password123!"
    ): String {
        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(RegisterRequest(email, password)))
        )
            .andExpect(status().isCreated)

        val verificationToken = emailVerificationTokenRepository.findAll()
            .last().token

        mockMvc.perform(get("/api/auth/verify").param("token", verificationToken))
            .andExpect(status().isOk)

        val loginResult = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(AuthRequest(email, password)))
        )
            .andExpect(status().isOk)
            .andReturn()

        return jsonMapper.readTree(loginResult.response.contentAsString)
            .get("accessToken").textValue()
    }

    fun createCompany(
        mockMvc: MockMvc,
        jsonMapper: JsonMapper,
        token: String,
        name: String
    ): String {
        val request = CreateCompanyRequest(name = name)
        val result = mockMvc.perform(
            post("/api/companies")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn()

        return jsonMapper.readTree(result.response.contentAsString)
            .get("id").textValue()
    }

    fun createJob(
        mockMvc: MockMvc,
        jsonMapper: JsonMapper,
        token: String,
        title: String
    ): String {
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

    fun createApplication(
        mockMvc: MockMvc,
        jsonMapper: JsonMapper,
        token: String,
        jobId: String
    ): String {
        val request = CreateApplicationRequest(jobId = UUID.fromString(jobId))
        val result = mockMvc.perform(
            post("/api/applications")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn()

        return jsonMapper.readTree(result.response.contentAsString)
            .get("id").textValue()
    }

    fun createInterview(
        mockMvc: MockMvc,
        jsonMapper: JsonMapper,
        token: String,
        applicationId: String,
        scheduledAt: String = "2026-04-01T10:00:00Z",
        interviewType: String = "VIDEO",
        stage: String = "SCREENING"
    ): String {
        val body = """{"applicationId":"$applicationId","scheduledAt":"$scheduledAt","interviewType":"$interviewType","stage":"$stage"}"""
        val result = mockMvc.perform(
            post("/api/interviews")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isCreated)
            .andReturn()
        return jsonMapper.readTree(result.response.contentAsString).get("id").textValue()
    }

    fun createInterviewNote(
        mockMvc: MockMvc,
        jsonMapper: JsonMapper,
        token: String,
        interviewId: String,
        content: String = "Test interview note",
        noteType: String = "GENERAL"
    ): String {
        val body = """{"content":"$content","noteType":"$noteType"}"""
        val result = mockMvc.perform(
            post("/api/interviews/$interviewId/notes")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isCreated)
            .andReturn()
        return jsonMapper.readTree(result.response.contentAsString).get("id").textValue()
    }
}
