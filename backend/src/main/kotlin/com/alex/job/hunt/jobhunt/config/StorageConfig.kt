package com.alex.job.hunt.jobhunt.config

import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import java.net.URI

@Configuration
@EnableConfigurationProperties(StorageProperties::class)
class StorageConfig(private val props: StorageProperties) {

    private val logger = LoggerFactory.getLogger(StorageConfig::class.java)

    @Bean
    fun s3Client(): S3Client = S3Client.builder()
        .endpointOverride(URI.create(props.endpoint))
        .region(Region.of(props.region))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(props.accessKey, props.secretKey)
            )
        )
        .forcePathStyle(true) // Required for MinIO
        .build()

    @Bean
    fun ensureBucketExists(s3Client: S3Client): CommandLineRunner = CommandLineRunner {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(props.bucket).build())
            logger.info("S3 bucket '{}' exists", props.bucket)
        } catch (e: NoSuchBucketException) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(props.bucket).build())
            logger.info("Created S3 bucket: {}", props.bucket)
        }
    }
}
