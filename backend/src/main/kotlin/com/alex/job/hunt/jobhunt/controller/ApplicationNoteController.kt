package com.alex.job.hunt.jobhunt.controller

import com.alex.job.hunt.jobhunt.dto.CreateNoteRequest
import com.alex.job.hunt.jobhunt.dto.NoteResponse
import com.alex.job.hunt.jobhunt.dto.UpdateNoteRequest
import com.alex.job.hunt.jobhunt.security.SecurityContextUtil
import com.alex.job.hunt.jobhunt.service.ApplicationNoteService
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
@RequestMapping("/api/applications/{applicationId}/notes")
class ApplicationNoteController(private val noteService: ApplicationNoteService) {

    @PostMapping
    fun create(
        @PathVariable applicationId: UUID,
        @Valid @RequestBody request: CreateNoteRequest
    ): ResponseEntity<NoteResponse> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.status(HttpStatus.CREATED).body(noteService.create(applicationId, request, userId))
    }

    @GetMapping
    fun list(
        @PathVariable applicationId: UUID,
        pageable: Pageable
    ): ResponseEntity<Page<NoteResponse>> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.ok(noteService.list(applicationId, userId, pageable))
    }

    @PutMapping("/{noteId}")
    fun update(
        @PathVariable applicationId: UUID,
        @PathVariable noteId: UUID,
        @Valid @RequestBody request: UpdateNoteRequest
    ): ResponseEntity<NoteResponse> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.ok(noteService.update(applicationId, noteId, request, userId))
    }

    @DeleteMapping("/{noteId}")
    fun delete(
        @PathVariable applicationId: UUID,
        @PathVariable noteId: UUID
    ): ResponseEntity<Void> {
        val userId = SecurityContextUtil.getCurrentUserId()
        noteService.delete(applicationId, noteId, userId)
        return ResponseEntity.noContent().build()
    }
}
