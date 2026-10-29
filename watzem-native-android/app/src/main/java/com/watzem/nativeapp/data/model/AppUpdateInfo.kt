package com.watzem.nativeapp.data.model

import com.google.gson.annotations.SerializedName

data class AppUpdateInfo(
    @SerializedName("version")
    val version: String = "1.0.0",
    @SerializedName("latestVersion")
    val latestVersion: String? = null,
    @SerializedName("versionCode")
    val versionCode: Int = 1,
    @SerializedName("minSupportedVersion")
    val minSupportedVersion: String? = null,
    @SerializedName("releaseDate")
    val releaseDate: String? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("releaseNotes")
    val releaseNotes: List<String> = emptyList(),
    @SerializedName("downloadUrl")
    val downloadUrl: String? = null,
    @SerializedName("fileSize")
    val fileSize: String? = null,
    @SerializedName("mandatory")
    val mandatory: Boolean = false
) {
    val displayVersion: String
        get() = latestVersion ?: version
}

