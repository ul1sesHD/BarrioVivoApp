package com.mercacerca.barriovivo.data.seed

import android.content.Context
import androidx.room.withTransaction
import com.mercacerca.barriovivo.data.local.AppDatabase
import com.mercacerca.barriovivo.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Llena Room en el primer arranque (offline-first). Es idempotente:
 * si las tablas ya tienen datos no hace nada, así que se puede llamar siempre.
 */
@Singleton
class DatabaseSeeder @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val db: AppDatabase,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) {
    private val mutex = Mutex()

    suspend fun seedIfEmpty() = mutex.withLock {
        withContext(io) {
            if (db.tianguisDao().count() == 0) {
                val json = context.assets.open(ASSET_TIANGUIS).bufferedReader().use { it.readText() }
                val tianguis = TianguisJsonParser.parse(json)
                db.withTransaction { db.tianguisDao().insertAll(tianguis) }
            }
            if (db.testimonioDao().count() == 0) {
                db.testimonioDao().insertAll(SeedData.testimonios)
            }
        }
    }

    private companion object {
        const val ASSET_TIANGUIS = "tianguis_cdmx.json"
    }
}
