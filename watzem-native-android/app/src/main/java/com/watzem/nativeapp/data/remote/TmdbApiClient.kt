package com.watzem.nativeapp.data.remote

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object TmdbApiClient {
    const val TMDB_API_KEY = "4ef0d7355d9ffb5151e987764708ce96"
    private const val BASE_URL = "https://api.themoviedb.org/3/"

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val originalHttpUrl = original.url

        val url = originalHttpUrl.newBuilder()
            .addQueryParameter("api_key", TMDB_API_KEY)
            .build()

        val request = original.newBuilder()
            .url(url)
            .addHeader("Accept", "application/json")
            .build()

        chain.proceed(request)
    }

    private val okHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val service: TmdbApiService by lazy {
        retrofit.create(TmdbApiService::class.java)
    }
}

