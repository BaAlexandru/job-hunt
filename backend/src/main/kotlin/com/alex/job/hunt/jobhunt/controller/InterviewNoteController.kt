package com.alex.job.hunt.jobhunt.controller

import com.alex.job.hunt.jobhunt.dto.CreateInterviewNoteRequest
import com.alex.job.hunt.jobhunt.dto.InterviewNoteResponse
import com.alex.job.hunt.jobhunt.dto.UpdateInterviewNoteRequest
import com.alex.job.hunt.jobhunt.security.SecurityContextUtil
import com.alex.job.hunt.jobhunt.service.InterviewNoteService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/interviews/{interviewId}/notes")
class InterviewNoteController(private val noteService: InterviewNoteService) {

    @PostMapping
    fun create(
        @PathVariable interviewId: UUID,
        @Valid @RequestBody request: CreateInterviewNoteRequest
    ): ResponseEntity<InterviewNoteResponse> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.status(HttpStatus.CREATED).body(noteService.create(interviewId, request, userId))
    }

    @GetMapping
    fun list(
        @PathVariable interviewId: UUID,
        pageable: Pageable
    ): ResponseEntity<Page<InterviewNoteResponse>> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.ok(noteService.list(interviewId, userId, pageable))
    }

    @PutMapping("/{noteId}")
    fun update(
        @PathVariable interviewId: UUID,
        @PathVariable noteId: UUID,
        @Valid @RequestBody request: UpdateInterviewNoteRequest
    ): ResponseEntity<InterviewNoteResponse> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.ok(noteService.update(interviewId, noteId, request, userId))
    }

    @DeleteMapping("/{noteId}")
    fun delete(
        @PathVariable interviewId: UUID,
        @PathVariable noteId: UUID
    ): ResponseEntity<Void> {
        val userId = SecurityContextUtil.getCurrentUserId()
        noteService.delete(interviewId, noteId, userId)
        return ResponseEntity.noContent().build()
    }
}
