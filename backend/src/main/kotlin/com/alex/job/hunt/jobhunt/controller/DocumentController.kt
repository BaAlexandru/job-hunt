package com.alex.job.hunt.jobhunt.controller

import com.alex.job.hunt.jobhunt.dto.DocumentApplicationLinkResponse
import com.alex.job.hunt.jobhunt.dto.DocumentResponse
import com.alex.job.hunt.jobhunt.dto.DocumentUpdateRequest
import com.alex.job.hunt.jobhunt.dto.DocumentVersionResponse
import com.alex.job.hunt.jobhunt.dto.LinkDocumentRequest
import com.alex.job.hunt.jobhunt.entity.DocumentCategory
import com.alex.job.hunt.jobhunt.security.SecurityContextUtil
import com.alex.job.hunt.jobhunt.service.DocumentService
import com.alex.job.hunt.jobhunt.service.InvalidFileTypeException
import jakarta.validation.Valid
import org.springframework.core.io.InputStreamResource
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/documents")
class DocumentController(private val documentService: DocumentService) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createDocument(
        @RequestPart("file") file: MultipartFile,
        @RequestPart("title") title: String,
        @RequestPart("category") category: String,
        @RequestPart("description", required = false) description: String?
    ): ResponseEntity<DocumentResponse> {
        val userId = SecurityContextUtil.getCurrentUserId()
        val parsedCategory = try {
            DocumentCategory.valueOf(category.uppercase())
        } catch (e: IllegalArgumentException) {
            throw InvalidFileTypeException(
                "Invalid category: $category. Allowed: ${DocumentCategory.entries.joinToString()}"
            )
        }
        val result = documentService.createDocument(userId, file, title, parsedCategory, description)
        return ResponseEntity.status(HttpStatus.CREATED).body(result)
    }

    @GetMapping
    fun listDocuments(
        @RequestParam(required = false) category: DocumentCategory?,
        @RequestParam(required = false) search: String?,
        pageable: Pageable
    ): ResponseEntity<Page<DocumentResponse>> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.ok(documentService.listDocuments(userId, category, search, pageable))
    }

    @GetMapping("/{id}")
    fun getDocument(@PathVariable id: UUID): ResponseEntity<DocumentResponse> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.ok(documentService.getDocument(userId, id))
    }

    @PutMapping("/{id}")
    fun updateDocument(
        @PathVariable id: UUID,
        @Valid @RequestBody request: DocumentUpdateRequest
    ): ResponseEntity<DocumentResponse> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.ok(documentService.updateDocument(userId, id, request))
    }

    @DeleteMapping("/{id}")
    fun archiveDocument(@PathVariable id: UUID): ResponseEntity<Void> {
        val userId = SecurityContextUtil.getCurrentUserId()
        documentService.archiveDocument(userId, id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/versions", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createVersion(
        @PathVariable id: UUID,
        @RequestPart("file") file: MultipartFile,
        @RequestPart("note", required = false) note: String?
    ): ResponseEntity<DocumentVersionResponse> {
        val userId = SecurityContextUtil.getCurrentUserId()
        val result = documentService.createVersion(userId, id, file, note)
        return ResponseEntity.status(HttpStatus.CREATED).body(result)
    }

    @GetMapping("/{id}/versions")
    fun listVersions(@PathVariable id: UUID): ResponseEntity<List<DocumentVersionResponse>> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.ok(documentService.listVersions(userId, id))
    }

    @PutMapping("/{id}/versions/{versionId}/current")
    fun setCurrentVersion(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID
    ): ResponseEntity<DocumentVersionResponse> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.ok(documentService.setCurrentVersion(userId, id, versionId))
    }

    @GetMapping("/{id}/versions/{versionId}/download")
    fun downloadVersion(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID
    ): ResponseEntity<InputStreamResource> {
        val userId = SecurityContextUtil.getCurrentUserId()
        val download = documentService.downloadVersion(userId, id, versionId)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(download.contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${download.filename}\"")
            .body(InputStreamResource(download.content))
    }

    @DeleteMapping("/{id}/versions/{versionId}")
    fun deleteVersion(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID
    ): ResponseEntity<Void> {
        val userId = SecurityContextUtil.getCurrentUserId()
        documentService.deleteVersion(userId, id, versionId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/links")
    fun linkDocumentToApplication(
        @Valid @RequestBody request: LinkDocumentRequest
    ): ResponseEntity<DocumentApplicationLinkResponse> {
        val userId = SecurityContextUtil.getCurrentUserId()
        val result = documentService.linkDocumentToApplication(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(result)
    }

    @DeleteMapping("/links")
    fun unlinkDocument(
        @RequestParam documentVersionId: UUID,
        @RequestParam applicationId: UUID
    ): ResponseEntity<Void> {
        val userId = SecurityContextUtil.getCurrentUserId()
        documentService.unlinkDocument(userId, documentVersionId, applicationId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/links/application/{applicationId}")
    fun getLinksForApplication(@PathVariable applicationId: UUID): ResponseEntity<List<DocumentApplicationLinkResponse>> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.ok(documentService.getLinksForApplication(userId, applicationId))
    }
}
