package edu.utap.limanup.androidcomposer.model

import com.google.gson.annotations.SerializedName

data class AudioMeta(
    @SerializedName("id")
    var id: Long,
    @SerializedName("url")
    var url: String,
    @SerializedName("name")
    var name: String,
    @SerializedName("description")
    var description: String,
    @SerializedName("created")
    var created: String,
    @SerializedName("license")
    var license: String,
    @SerializedName("type")
    var type: String,
    @SerializedName("filesize")
    var filesize: Long,
    @SerializedName("duration")
    var duration: Float,
    @SerializedName("username")
    var username: String,
    @SerializedName("download")
    var download: String,
    @SerializedName("filename")
    var filename: String,
) {
    // Firebase error: FATAL EXCEPTION: ... does not define a no-argument constructor
    constructor() : this(
        0L, "", "", "", "", "", "",
        0L, 0F, "", "", ""
    )
}
