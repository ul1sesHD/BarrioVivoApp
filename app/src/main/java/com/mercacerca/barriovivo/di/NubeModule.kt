package com.mercacerca.barriovivo.di

import com.mercacerca.barriovivo.BuildConfig
import com.mercacerca.barriovivo.data.remote.NubeApagada
import com.mercacerca.barriovivo.data.remote.NubeRemota
import com.mercacerca.barriovivo.data.remote.SupabaseRemota
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NubeModule {

    /** Con llaves en local.properties → Supabase. Sin llaves → la app sigue 100% offline. */
    @Provides @Singleton
    fun nube(): NubeRemota {
        val url = BuildConfig.SUPABASE_URL
        val key = BuildConfig.SUPABASE_ANON_KEY
        if (url.isBlank() || key.isBlank() || !url.startsWith("https://")) return NubeApagada
        return SupabaseRemota(
            createSupabaseClient(supabaseUrl = url, supabaseKey = key) {
                install(Auth)
                install(Postgrest)
            }
        )
    }
}
