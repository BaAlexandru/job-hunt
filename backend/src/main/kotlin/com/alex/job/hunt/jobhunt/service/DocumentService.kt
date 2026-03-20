package com.alex.job.hunt.jobhunt.service

import com.alex.job.hunt.jobhunt.dto.DocumentApplicationLinkResponse
import com.alex.job.hunt.jobhunt.dto.DocumentResponse
import com.alex.job.hunt.jobhunt.dto.DocumentUpdateRequest
import com.alex.job.hunt.jobhunt.dto.DocumentVersionResponse
import com.alex.job.hunt.jobhunt.dto.LinkDocumentRequest
import com.alex.job.hunt.jobhunt.entity.DocumentApplicationLinkEntity
import com.alex.job.hunt.jobhunt.entity.DocumentCategory
import com.alex.job.hunt.jobhunt.entity.DocumentEntity
import com.alex.job.hunt.jobhunt.entity.DocumentVersionEntity
import com.alex.job.hunt.jobhunt.repository.ApplicationRepository
import com.alex.job.hunt.jobhunt.repository.DocumentApplicationLinkRepository
import com.alex.job.hunt.jobhunt.repository.DocumentRepository
import com.alex.job.hunt.jobhunt.repository.DocumentVersionRepository
import org.apache.tika.Tika
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream
import java.time.Instant
import java.util.UUID

data class DocumentDownload(
    val content: InputStream,
    val contentType: String,
    val filename: String
)

@Service
@Transactional(readOnly = true)
class DocumentService(
    private val documentRepository: DocumentRepository,
    private val documentVersionRepository: DocumentVersionRepository,
    private val documentApplicationLinkRepository: DocumentApplicationLinkRepository,
    private val applicationRepository: ApplicationRepository,
    private val storageService: StorageService
) {

    private val tika = Tika()

    companion object {
        private val ALLOWED_MIME_TYPES = setOf(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        )
    }

    @Transactional
    fun createDocument(
        userId: UUID,
        file: MultipartFile,
        title: String,
        category: DocumentCategory,
        description: String?
    ): DocumentResponse {
        val fileBytes = file.bytes
        val detectedType = tika.detect(fileBytes)
        validateFileType(detectedType)

        val doc = DocumentEntity(
            userId = userId,
            title = title,
            category = category,
            description = description
        )
        val savedDoc = documentRepository.save(doc)

        val version = DocumentVersionEntity(
            documentId = savedDoc.id!!,
            versionNumber = 1,
            storageKey = "documents/${savedDoc.id}/${UUID.randomUUID()}.${getExtension(file.originalFilename)}",
            originalFilename = file.originalFilename ?: "unknown",
            contentType = detectedType,
            fileSize = file.size,
            note = null,
            isCurrent = true
        )
        val savedVersion = documentVersionRepository.save(version)

        storageService.upload(savedVersion.storageKey, fileBytes.inputStream(), file.size, detectedType)

        return toDocumentResponse(savedDoc, savedVersion)
    }

    fun getDocument(userId: UUID, documentId: UUID): DocumentResponse {
        val doc = findDocumentOrThrow(documentId, userId)
        val currentVersion = documentVersionRepository.findByDocumentIdAndIsCurrent(documentId, true)
        return toDocumentResponse(doc, currentVersion)
    }

    fun listDocuments(
        userId: UUID,
        category: DocumentCategory?,
        search: String?,
        pageable: Pageable
    ): Page<DocumentResponse> {
        val page = documentRepository.findAllByFilters(userId, category, search, pageable)
        val docIds = page.content.map { it.id!! }
        val currentVersions = docIds.associateWith { docId ->
            documentVersionRepository.findByDocumentIdAndIsCurrent(docId, true)
        }
        return page.map { doc ->
            toDocumentResponse(doc, currentVersions[doc.id!!])
        }
    }

    @Transactional
    fun updateDocument(userId: UUID, documentId: UUID, request: DocumentUpdateRequest): DocumentResponse {
        val doc = findDocumentOrThrow(documentId, userId)
        doc.title = request.title
        doc.description = request.description
        doc.category = request.category
        doc.updatedAt = Instant.now()
        val saved = documentRepository.save(doc)
        val currentVersion = documentVersionRepository.findByDocumentIdAndIsCurrent(documentId, true)
        return toDocumentResponse(saved, currentVersion)
    }

    @Transactional
    fun archiveDocument(userId: UUID, documentId: UUID) {
        val doc = findDocumentOrThrow(documentId, userId)
        doc.archived = true
        doc.archivedAt = Instant.now()
        doc.updatedAt = Instant.now()
        documentRepository.save(doc)
    }

    @Transactional
    fun createVersion(
        userId: UUID,
        documentId: UUID,
        file: MultipartFile,
        note: String?
    ): DocumentVersionResponse {
        val doc = findDocumentOrThrow(documentId, userId)
        val fileBytes = file.bytes
        val detectedType = tika.detect(fileBytes)
        validateFileType(detectedType)

        val nextVersionNumber = documentVersionRepository.countByDocumentId(documentId) + 1
        documentVersionRepository.clearCurrentFlag(documentId)

        val version = DocumentVersionEntity(
            documentId = documentId,
            versionNumber = nextVersionNumber,
            storageKey = "documents/${doc.id}/${UUID.randomUUID()}.${getExtension(file.originalFilename)}",
            originalFilename = file.originalFilename ?: "unknown",
            contentType = detectedType,
            fileSize = file.size,
            note = note,
            isCurrent = true
        )
        val savedVersion = documentVersionRepository.save(version)

        storageService.upload(savedVersion.storageKey, fileBytes.inputStream(), file.size, detectedType)

        doc.updatedAt = Instant.now()
        documentRepository.save(doc)

        return toVersionResponse(savedVersion)
    }

    fun listVersions(userId: UUID, documentId: UUID): List<DocumentVersionResponse> {
        findDocumentOrThrow(documentId, userId)
        return documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId)
            .map { toVersionResponse(it) }
    }

    @Transactional
    fun setCurrentVersion(userId: UUID, documentId: UUID, versionId: UUID): DocumentVersionResponse {
        val doc = findDocumentOrThrow(documentId, userId)
        val version = documentVersionRepository.findByIdAndDocumentId(versionId, documentId)
            ?: throw NotFoundException("Version not found")

        documentVersionRepository.clearCurrentFlag(documentId)
        version.isCurrent = true
        val saved = documentVersionRepository.save(version)

        doc.updatedAt = Instant.now()
        documentRepository.save(doc)

        return toVersionResponse(saved)
    }

    fun downloadVersion(userId: UUID, documentId: UUID, versionId: UUID): DocumentDownload {
        findDocumentOrThrow(documentId, userId)
        val version = documentVersionRepository.findByIdAndDocumentId(versionId, documentId)
            ?: throw NotFoundException("Version not found")

        val download = storageService.download(version.storageKey)
        return DocumentDownload(
            content = download.content,
            contentType = version.contentType,
            filename = version.originalFilename
        )
    }

    @Transactional
    fun deleteVersion(userId: UUID, documentId: UUID, versionId: UUID) {
        val doc = findDocumentOrThrow(documentId, userId)
        val version = documentVersionRepository.findByIdAndDocumentId(versionId, documentId)
            ?: throw NotFoundException("Version not found")

        val versionCount = documentVersionRepository.countByDocumentId(documentId)
        if (versionCount <= 1) {
            throw ConflictException("Cannot delete the only version. Archive the document instead.")
        }

        storageService.delete(version.storageKey)

        val wasCurrent = version.isCurrent
        documentVersionRepository.delete(version)

        if (wasCurrent) {
            val remaining = documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId)
            if (remaining.isNotEmpty()) {
                val latest = remaining.first()
                latest.isCurrent = true
                documentVersionRepository.save(latest)
            }
        }

        doc.updatedAt = Instant.now()
        documentRepository.save(doc)
    }

    @Transactional
    fun linkDocumentToApplication(userId: UUID, request: LinkDocumentRequest): DocumentApplicationLinkResponse {
        val version = documentVersionRepository.findById(request.documentVersionId).orElse(null)
            ?: throw NotFoundException("Document version not found")

        findDocumentOrThrow(version.documentId, userId)

        applicationRepository.findByIdAndUserId(request.applicationId, userId)
            ?: throw NotFoundException("Application not found")

        if (documentApplicationLinkRepository.existsByDocumentVersionIdAndApplicationId(
                request.documentVersionId, request.applicationId
            )
        ) {
            throw ConflictException("Already linked")
        }

        val link = DocumentApplicationLinkEntity(
            documentVersionId = request.documentVersionId,
            applicationId = request.applicationId
        )
        val saved = documentApplicationLinkRepository.save(link)
        return toLinkResponse(saved, false)
    }

    @Transactional
    fun unlinkDocument(userId: UUID, documentVersionId: UUID, applicationId: UUID) {
        val version = documentVersionRepository.findById(documentVersionId).orElse(null)
            ?: throw NotFoundException("Document version not found")
        findDocumentOrThrow(version.documentId, userId)
        applicationRepository.findByIdAndUserId(applicationId, userId)
            ?: throw NotFoundException("Application not found")

        documentApplicationLinkRepository.deleteByDocumentVersionIdAndApplicationId(documentVersionId, applicationId)
    }

    fun getLinksForApplication(userId: UUID, applicationId: UUID): List<DocumentApplicationLinkResponse> {
        applicationRepository.findByIdAndUserId(applicationId, userId)
            ?: throw NotFoundException("Application not found")

        val links = documentApplicationLinkRepository.findByApplicationId(applicationId)
        return links.map { link ->
            val versionExists = documentVersionRepository.findById(link.documentVersionId).isPresent
            toLinkResponse(link, !versionExists)
        }
    }

    private fun findDocumentOrThrow(documentId: UUID, userId: UUID): DocumentEntity {
        return documentRepository.findByIdAndUserIdAndArchivedFalse(documentId, userId)
            ?: throw NotFoundException("Document not found")
    }

    private fun validateFileType(detectedType: String) {
        if (detectedType !in ALLOWED_MIME_TYPES) {
            throw InvalidFileTypeException("File type '$detectedType' not allowed. Allowed: PDF, DOCX")
        }
    }

    private fun getExtension(filename: String?): String {
        if (filename == null) return "bin"
        val dotIndex = filename.lastIndexOf('.')
        return if (dotIndex >= 0) filename.substring(dotIndex + 1) else "bin"
    }

    private fun toDocumentResponse(entity: DocumentEntity, currentVersion: DocumentVersionEntity?): DocumentResponse {
        return DocumentResponse(
            id = entity.id!!,
            title = entity.title,
            description = entity.description,
            category = entity.category,
            currentVersion = currentVersion?.let { toVersionResponse(it) },
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    private fun toVersionResponse(entity: DocumentVersionEntity): DocumentVersionResponse {
        return DocumentVersionResponse(
            id = entity.id!!,
            versionNumber = entity.versionNumber,
            originalFilename = entity.originalFilename,
            contentType = entity.contentType,
            fileSize = entity.fileSize,
            note = entity.note,
            isCurrent = entity.isCurrent,
            createdAt = entity.createdAt
        )
    }

    private fun toLinkResponse(entity: DocumentApplicationLinkEntity, versionRemoved: Boolean): DocumentApplicationLinkResponse {
        return DocumentApplicationLinkResponse(
            id = entity.id!!,
            documentVersionId = entity.documentVersionId,
            applicationId = entity.applicationId,
            linkedAt = entity.linkedAt,
            versionRemoved = versionRemoved
        )
    }
}
