package com.mercacerca.barriovivo.di

import android.content.Context
import androidx.room.Room
import com.google.android.gms.location.LocationServices
import com.mercacerca.barriovivo.core.location.FusedLocationProvider
import com.mercacerca.barriovivo.core.location.LocationProvider
import com.mercacerca.barriovivo.data.local.AppDatabase
import com.mercacerca.barriovivo.core.security.PasswordHasher
import com.mercacerca.barriovivo.data.repository.AuthRepositoryImpl
import com.mercacerca.barriovivo.core.Reloj
import com.mercacerca.barriovivo.core.RelojSistema
import com.mercacerca.barriovivo.data.repository.FavoritoRepositoryImpl
import com.mercacerca.barriovivo.data.repository.ViajeRepositoryImpl
import com.mercacerca.barriovivo.data.sync.SincronizadorNube
import com.mercacerca.barriovivo.data.sync.SolicitudSync
import com.mercacerca.barriovivo.data.repository.ReviewRepositoryImpl
import com.mercacerca.barriovivo.data.repository.TestimonioRepositoryImpl
import com.mercacerca.barriovivo.data.repository.TianguisRepositoryImpl
import com.mercacerca.barriovivo.domain.repository.AuthRepository
import com.mercacerca.barriovivo.domain.repository.FavoritoRepository
import com.mercacerca.barriovivo.domain.repository.ViajeRepository
import com.mercacerca.barriovivo.domain.repository.ReviewRepository
import com.mercacerca.barriovivo.domain.repository.TestimonioRepository
import com.mercacerca.barriovivo.domain.repository.TianguisRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class IoDispatcher
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class ApplicationScope
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class DefaultDispatcher

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NOMBRE).build()

    @Provides fun tianguisDao(db: AppDatabase) = db.tianguisDao()
    @Provides fun testimonioDao(db: AppDatabase) = db.testimonioDao()
    @Provides fun reviewDao(db: AppDatabase) = db.reviewDao()
    @Provides fun usuarioDao(db: AppDatabase) = db.usuarioDao()
    @Provides fun favoritoDao(db: AppDatabase) = db.favoritoDao()
    @Provides fun viajeDao(db: AppDatabase) = db.viajeDao()

    @Provides @IoDispatcher
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides @DefaultDispatcher
    fun defaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides @Singleton
    fun passwordHasher(): PasswordHasher = PasswordHasher()

    /** Scope que vive lo mismo que la app (para la carga inicial de datos). */
    @Provides @Singleton @ApplicationScope
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides @Singleton
    fun locationProvider(@ApplicationContext context: Context): LocationProvider =
        FusedLocationProvider(context, LocationServices.getFusedLocationProviderClient(context))
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun tianguis(impl: TianguisRepositoryImpl): TianguisRepository
    @Binds abstract fun testimonios(impl: TestimonioRepositoryImpl): TestimonioRepository
    @Binds abstract fun reviews(impl: ReviewRepositoryImpl): ReviewRepository
    @Binds abstract fun auth(impl: AuthRepositoryImpl): AuthRepository
    @Binds abstract fun favoritos(impl: FavoritoRepositoryImpl): FavoritoRepository
    @Binds abstract fun viajes(impl: ViajeRepositoryImpl): ViajeRepository
    @Binds abstract fun reloj(impl: RelojSistema): Reloj
    @Binds abstract fun solicitudSync(impl: SincronizadorNube): SolicitudSync
}
