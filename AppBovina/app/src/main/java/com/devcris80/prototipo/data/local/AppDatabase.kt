package com.devcris80.prototipo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.devcris80.prototipo.data.local.dao.AnimalDao
import com.devcris80.prototipo.data.local.dao.ChapetaDao
import com.devcris80.prototipo.data.local.dao.EventoDao
import com.devcris80.prototipo.data.local.dao.PerfilFincaDao
import com.devcris80.prototipo.data.local.dao.RegistroDao
import com.devcris80.prototipo.data.local.dao.UsuarioDao
import com.devcris80.prototipo.data.local.entity.AnimalEntity
import com.devcris80.prototipo.data.local.entity.ChapetaEntity
import com.devcris80.prototipo.data.local.entity.EventoEntity
import com.devcris80.prototipo.data.local.entity.PerfilFincaEntity
import com.devcris80.prototipo.data.local.entity.RegistroEntity
import com.devcris80.prototipo.data.local.entity.UsuarioEntity

@Database(
    entities = [
        AnimalEntity::class,
        EventoEntity::class,
        RegistroEntity::class,
        ChapetaEntity::class,
        PerfilFincaEntity::class,
        UsuarioEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun animalDao(): AnimalDao
    abstract fun eventoDao(): EventoDao
    abstract fun registroDao(): RegistroDao
    abstract fun chapetaDao(): ChapetaDao
    abstract fun perfilFincaDao(): PerfilFincaDao
    abstract fun usuarioDao(): UsuarioDao

    companion object {
        // Room no declara índices parciales: una sola Chapeta activa por codigo se garantiza aquí.
        val CALLBACK_INDICES_CHAPETA = object : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_chapeta_codigo_activa " +
                        "ON chapeta(codigo) WHERE fechaDesasociacion IS NULL",
                )
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app-bovina.db",
                )
                    // Prototipo: aún no hay datos reales de usuarios en campo que proteger,
                    // así que se prioriza iterar el esquema sobre escribir migraciones.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(CALLBACK_INDICES_CHAPETA)
                    .build()
                    .also { instance = it }
            }
    }
}
