package com.alex.job.hunt.jobhunt.service

import java.io.InputStream

data class StorageDownload(
    val content: InputStream,
    val contentType: String,
    val contentLength: Long
)

interface StorageService {
    fun upload(key: String, content: InputStream, contentLength: Long, contentType: String)
    fun download(key: String): StorageDownload
    fun delete(key: String)
    fun exists(key: String): Boolean
}
