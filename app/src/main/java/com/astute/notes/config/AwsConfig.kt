package com.astute.notes.config

import android.content.Context
import android.content.SharedPreferences
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials

object AwsConfig {
    private const val PREFS_NAME = "aws_credentials"
    private const val KEY_ACCESS_KEY_ID = "aws_access_key_id"
    private const val KEY_SECRET_ACCESS_KEY = "aws_secret_access_key"
    private const val KEY_REGION = "aws_region"
    private const val KEY_BUCKET_NAME = "s3_bucket_name"

    const val NOTES_PREFIX = "notes/"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    val isConfigured: Boolean
        get() = accessKeyId.isNotBlank()
                && secretAccessKey.isNotBlank()
                && bucketName.isNotBlank()

    var accessKeyId: String
        get() = prefs.getString(KEY_ACCESS_KEY_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ACCESS_KEY_ID, value).apply()

    var secretAccessKey: String
        get() = prefs.getString(KEY_SECRET_ACCESS_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SECRET_ACCESS_KEY, value).apply()

    var region: String
        get() = prefs.getString(KEY_REGION, "us-east-1") ?: "us-east-1"
        set(value) = prefs.edit().putString(KEY_REGION, value).apply()

    var bucketName: String
        get() = prefs.getString(KEY_BUCKET_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BUCKET_NAME, value).apply()

    fun createS3Client(): S3Client? {
        if (!isConfigured) return null
        return S3Client {
            this.region = this@AwsConfig.region
            credentialsProvider = StaticCredentialsProvider(
                Credentials(
                    accessKeyId = this@AwsConfig.accessKeyId,
                    secretAccessKey = this@AwsConfig.secretAccessKey
                )
            )
        }
    }

    fun clearCredentials() {
        prefs.edit().clear().apply()
    }
}
