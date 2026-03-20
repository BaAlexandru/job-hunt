package com.alex.job.hunt.jobhunt.service

import com.alex.job.hunt.jobhunt.config.StorageProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import java.io.InputStream

@Service
class S3StorageService(
    private val s3Client: S3Client,
    private val storageProperties: StorageProperties
) : StorageService {

    private val logger = LoggerFactory.getLogger(S3StorageService::class.java)

    override fun upload(key: String, content: InputStream, contentLength: Long, contentType: String) {
        try {
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(storageProperties.bucket)
                    .key(key)
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .build(),
                RequestBody.fromInputStream(content, contentLength)
            )
            logger.debug("Uploaded file to S3: {}", key)
        } catch (e: S3Exception) {
            logger.error("Failed to upload file to S3: {}", key, e)
            throw StorageException("Failed to upload file: ${e.message}")
        }
    }

    override fun download(key: String): StorageDownload {
        try {
            val response = s3Client.getObject(
                GetObjectRequest.builder()
                    .bucket(storageProperties.bucket)
                    .key(key)
                    .build()
            )
            return StorageDownload(
                content = response,
                contentType = response.response().contentType() ?: "application/octet-stream",
                contentLength = response.response().contentLength()
            )
        } catch (e: NoSuchKeyException) {
            throw StorageException("File not found in storage: $key")
        } catch (e: S3Exception) {
            logger.error("Failed to download file from S3: {}", key, e)
            throw StorageException("Failed to download file: ${e.message}")
        }
    }

    override fun delete(key: String) {
        try {
            s3Client.deleteObject(
                DeleteObjectRequest.builder()
                    .bucket(storageProperties.bucket)
                    .key(key)
                    .build()
            )
            logger.debug("Deleted file from S3: {}", key)
        } catch (e: S3Exception) {
            logger.error("Failed to delete file from S3: {}", key, e)
            throw StorageException("Failed to delete file: ${e.message}")
        }
    }

    override fun exists(key: String): Boolean {
        return try {
            s3Client.headObject(
                HeadObjectRequest.builder()
                    .bucket(storageProperties.bucket)
                    .key(key)
                    .build()
            )
            true
        } catch (e: NoSuchKeyException) {
            false
        }
    }
}
