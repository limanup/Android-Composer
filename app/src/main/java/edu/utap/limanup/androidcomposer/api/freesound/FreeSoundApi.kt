package edu.utap.limanup.androidcomposer.api.freesound

import android.util.Log
import edu.utap.limanup.androidcomposer.model.AudioMeta
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface FreeSoundApi {
    @GET("/apiv2/search/text/?fields=id%2Curl%2Cname%2Cdescription%2Ccreated%2Clicense%2Ctype%2Cfilesize%2Cduration%2Cusername%2Cdownload&filter=duration%3A%5B4+TO+8%5D+filesize%3A%5B0+TO+1000000%5D+&format=json&page_size=150&query=music+loop")
    suspend fun getAudioSnippets(@Query("token") api_key: String): ListingResponse

    data class ListingResponse(
        val count: Int,
        val next: String?,
        val results: List<AudioMeta>,
        val previous: String?
    )

    companion object {
        // Keep the base URL simple
        var httpurl = HttpUrl.Builder()
            .scheme("https")
            .host("freesound.org")
            .build()

        fun create(): FreeSoundApi = create(httpurl)
        private fun create(httpUrl: HttpUrl): FreeSoundApi {
            val client = OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply {
                    // Enable basic HTTP logging to help with debugging.
                    this.level = HttpLoggingInterceptor.Level.BASIC
                })
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build()
            Log.d(javaClass.simpleName, "XXX api create function")
            return Retrofit.Builder()
                .baseUrl(httpUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(FreeSoundApi::class.java)
        }
    }
}