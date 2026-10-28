package com.example.domain.facebook

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class FacebookPageInfo(
    val pageId: String,
    val pageName: String?,
    val isConnected: Boolean,
    val permissions: List<String> = emptyList(),
    val errorMessage: String? = null
)

data class FacebookPublishResult(
    val success: Boolean,
    val postId: String? = null,
    val error: String? = null
)

data class FacebookCommentItem(
    val commentId: String,
    val postId: String,
    val fromName: String,
    val message: String,
    val createdTime: String
)

data class FacebookCommentsResult(
    val success: Boolean,
    val comments: List<FacebookCommentItem> = emptyList(),
    val error: String? = null
)

class FacebookApiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    suspend fun verifyPageConnection(
        pageId: String,
        accessToken: String,
        apiVersion: String = "v20.0"
    ): FacebookPageInfo = withContext(Dispatchers.IO) {
        val trimmedToken = accessToken.trim()
        val trimmedPageId = pageId.trim()

        if (trimmedToken.isEmpty() || trimmedToken == "UNCONFIGURED" || trimmedPageId.isEmpty() || trimmedPageId == "UNCONFIGURED") {
            return@withContext FacebookPageInfo(
                pageId = trimmedPageId,
                pageName = null,
                isConnected = false,
                errorMessage = "Facebook Page ID au Access Token haijawekwa (Unconfigured credentials)."
            )
        }

        val url = "https://graph.facebook.com/$apiVersion/$trimmedPageId?fields=id,name,category,tasks&access_token=$trimmedToken"
        val request = Request.Builder().url(url).get().build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    val id = json.optString("id", trimmedPageId)
                    val name = json.optString("name", "Authorized Page")
                    val tasks = mutableListOf<String>()
                    val tasksJson = json.optJSONArray("tasks")
                    if (tasksJson != null) {
                        for (i in 0 until tasksJson.length()) {
                            tasks.add(tasksJson.getString(i))
                        }
                    }
                    FacebookPageInfo(
                        pageId = id,
                        pageName = name,
                        isConnected = true,
                        permissions = if (tasks.isEmpty()) listOf("MANAGE", "CREATE_CONTENT", "MODERATE") else tasks
                    )
                } else {
                    val safeError = parseSafeGraphError(body, response.code)
                    FacebookPageInfo(
                        pageId = trimmedPageId,
                        pageName = null,
                        isConnected = false,
                        errorMessage = safeError
                    )
                }
            }
        } catch (e: IOException) {
            FacebookPageInfo(
                pageId = trimmedPageId,
                pageName = null,
                isConnected = false,
                errorMessage = "Hitilafu ya mtandao: ${e.message ?: "Network timeout connecting to Meta Graph API"}"
            )
        } catch (e: Exception) {
            FacebookPageInfo(
                pageId = trimmedPageId,
                pageName = null,
                isConnected = false,
                errorMessage = "Hitilafu ya uthibitishaji: ${e.localizedMessage}"
            )
        }
    }

    suspend fun publishPost(
        pageId: String,
        accessToken: String,
        message: String,
        apiVersion: String = "v20.0"
    ): FacebookPublishResult = withContext(Dispatchers.IO) {
        val trimmedToken = accessToken.trim()
        val trimmedPageId = pageId.trim()

        if (trimmedToken.isEmpty() || trimmedToken == "UNCONFIGURED" || trimmedPageId.isEmpty() || trimmedPageId == "UNCONFIGURED") {
            return@withContext FacebookPublishResult(
                success = false,
                error = "Hauwezi kuchapisha: FB_PAGE_ACCESS_TOKEN au FB_PAGE_ID haipo kwenye mipangilio."
            )
        }

        val url = "https://graph.facebook.com/$apiVersion/$trimmedPageId/feed"
        val formBody = FormBody.Builder()
            .add("message", message)
            .add("access_token", trimmedToken)
            .build()

        val request = Request.Builder().url(url).post(formBody).build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    val postId = json.optString("id")
                    FacebookPublishResult(success = true, postId = postId)
                } else {
                    val safeError = parseSafeGraphError(body, response.code)
                    FacebookPublishResult(success = false, error = safeError)
                }
            }
        } catch (e: IOException) {
            FacebookPublishResult(success = false, error = "Mtandao umeshindwa: ${e.message}")
        } catch (e: Exception) {
            FacebookPublishResult(success = false, error = "Hitilafu: ${e.localizedMessage}")
        }
    }

    suspend fun fetchRecentComments(
        pageId: String,
        accessToken: String,
        apiVersion: String = "v20.0"
    ): FacebookCommentsResult = withContext(Dispatchers.IO) {
        val trimmedToken = accessToken.trim()
        val trimmedPageId = pageId.trim()

        if (trimmedToken.isEmpty() || trimmedToken == "UNCONFIGURED" || trimmedPageId.isEmpty() || trimmedPageId == "UNCONFIGURED") {
            return@withContext FacebookCommentsResult(
                success = false,
                error = "FB_PAGE_ACCESS_TOKEN au FB_PAGE_ID haijawekwa."
            )
        }

        // Fetch recent posts with comments
        val url = "https://graph.facebook.com/$apiVersion/$trimmedPageId/feed?fields=id,message,comments{id,message,from,created_time}&limit=5&access_token=$trimmedToken"
        val request = Request.Builder().url(url).get().build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    val data = json.optJSONArray("data")
                    val commentsList = mutableListOf<FacebookCommentItem>()
                    if (data != null) {
                        for (i in 0 until data.length()) {
                            val postObj = data.getJSONObject(i)
                            val postId = postObj.optString("id")
                            val commentsObj = postObj.optJSONObject("comments")
                            if (commentsObj != null) {
                                val cData = commentsObj.optJSONArray("data")
                                if (cData != null) {
                                    for (j in 0 until cData.length()) {
                                        val cObj = cData.getJSONObject(j)
                                        val commentId = cObj.optString("id")
                                        val msg = cObj.optString("message")
                                        val fromObj = cObj.optJSONObject("from")
                                        val authorName = fromObj?.optString("name") ?: "Mtumiaji"
                                        val createdTime = cObj.optString("created_time")
                                        commentsList.add(
                                            FacebookCommentItem(
                                                commentId = commentId,
                                                postId = postId,
                                                fromName = authorName,
                                                message = msg,
                                                createdTime = createdTime
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    FacebookCommentsResult(success = true, comments = commentsList)
                } else {
                    val safeError = parseSafeGraphError(body, response.code)
                    FacebookCommentsResult(success = false, error = safeError)
                }
            }
        } catch (e: Exception) {
            FacebookCommentsResult(success = false, error = "Hitilafu: ${e.localizedMessage}")
        }
    }

    suspend fun replyToComment(
        commentId: String,
        accessToken: String,
        replyMessage: String,
        apiVersion: String = "v20.0"
    ): FacebookPublishResult = withContext(Dispatchers.IO) {
        val trimmedToken = accessToken.trim()
        if (trimmedToken.isEmpty() || trimmedToken == "UNCONFIGURED") {
            return@withContext FacebookPublishResult(
                success = false,
                error = "FB_PAGE_ACCESS_TOKEN haipo."
            )
        }

        val url = "https://graph.facebook.com/$apiVersion/$commentId/comments"
        val formBody = FormBody.Builder()
            .add("message", replyMessage)
            .add("access_token", trimmedToken)
            .build()

        val request = Request.Builder().url(url).post(formBody).build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    val id = json.optString("id")
                    FacebookPublishResult(success = true, postId = id)
                } else {
                    val safeError = parseSafeGraphError(body, response.code)
                    FacebookPublishResult(success = false, error = safeError)
                }
            }
        } catch (e: Exception) {
            FacebookPublishResult(success = false, error = "Hitilafu: ${e.localizedMessage}")
        }
    }

    private fun parseSafeGraphError(responseBody: String, httpCode: Int): String {
        return try {
            val json = JSONObject(responseBody)
            val errorObj = json.optJSONObject("error")
            val message = errorObj?.optString("message") ?: "HTTP error $httpCode"
            val code = errorObj?.optInt("code", httpCode)
            // Sanitize against token leakage
            val safeMessage = message.replace(Regex("EA[a-zA-Z0-9]+"), "[MASKED_TOKEN]")
            "Meta API Error ($code): $safeMessage"
        } catch (e: Exception) {
            "Meta Graph API HTTP $httpCode"
        }
    }
}
