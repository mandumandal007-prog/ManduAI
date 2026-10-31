package com.mandu.ai.core.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.mandu.ai.BuildConfig
import com.mandu.ai.core.data.ManduApi
import com.mandu.ai.core.data.local.ManduDatabase
import com.mandu.ai.core.data.local.ChatDao
import com.mandu.ai.core.data.local.SessionStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("mandu_prefs")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Provides @Singleton
    fun okHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // SSE friendly
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        })
        .build()

    @Provides @Singleton
    fun api(client: OkHttpClient, json: Json): ManduApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_URL.trimEnd('/') + "/")
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(ManduApi::class.java)
    }

    @Provides @Singleton
    fun database(@ApplicationContext ctx: Context): ManduDatabase =
        ManduDatabase.build(ctx)

    @Provides
    fun chatDao(db: ManduDatabase): ChatDao = db.chatDao()

    @Provides @Singleton
    fun sessionStore(@ApplicationContext ctx: Context, json: Json): SessionStore =
        SessionStore(ctx.dataStore, json)
}
