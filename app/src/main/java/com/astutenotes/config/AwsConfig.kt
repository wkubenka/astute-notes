package com.astutenotes.config

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import com.astutenotes.BuildConfig

object AwsConfig {
    val s3Client: S3Client by lazy {
        S3Client {
            region = BuildConfig.AWS_REGION
            credentialsProvider = StaticCredentialsProvider(
                Credentials(
                    accessKeyId = BuildConfig.AWS_ACCESS_KEY_ID,
                    secretAccessKey = BuildConfig.AWS_SECRET_ACCESS_KEY
                )
            )
        }
    }

    val bucketName: String = BuildConfig.S3_BUCKET_NAME
    const val NOTES_PREFIX = "notes/"
}
