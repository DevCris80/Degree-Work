package com.devcris80.prototipo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.devcris80.prototipo.data.local.dao.AnimalDao
import com.devcris80.prototipo.data.local.dao.ChapetaDao
import com.devcris80.prototipo.data.local.dao.EstadoSyncDao
import com.devcris80.prototipo.data.local.dao.EventoDao
import com.devcris80.prototipo.data.local.dao.PerfilFincaDao
import com.devcris80.prototipo.data.local.dao.PesajePendienteDao
import com.devcris80.prototipo.data.local.dao.RechazoDao
import com.devcris80.prototipo.data.local.dao.RegistroDao
import com.devcris80.prototipo.data.local.dao.ResolucionDao
import com.devcris80.prototipo.data.local.dao.SiblingDao
import com.devcris80.prototipo.data.local.dao.UsuarioDao
import com.devcris80.prototipo.data.local.entity.AnimalEntity
import com.devcris80.prototipo.data.local.entity.ChapetaEntity
import com.devcris80.prototipo.data.local.entity.EstadoSyncEntity
import com.devcris80.prototipo.data.local.entity.EventoEntity
import com.devcris80.prototipo.data.local.entity.PerfilFincaEntity
import com.devcris80.prototipo.data.local.entity.PesajePendienteEntity
import com.devcris80.prototipo.data.local.entity.RechazoEntity
import com.devcris80.prototipo.data.local.entity.RegistroEntity
import com.devcris80.prototipo.data.local.entity.ResolucionEntity
import com.devcris80.prototipo.data.local.entity.SiblingEntity
import com.devcris80.prototipo.data.local.entity.UsuarioEntity

@Database(
    entities = [
        AnimalEntity::class,
        EventoEntity::class,
        RegistroEntity::class,
        ChapetaEntity::class,
        PerfilFincaEntity::class,
        UsuarioEntity::class,
        PesajePendienteEntity::class,
        SiblingEntity::class,
        RechazoEntity::class,
        EstadoSyncEntity::class,
        ResolucionEntity::class,
    ],
    version = 17,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun animalDao(): AnimalDao
    abstract fun eventoDao(): EventoDao
    abstract fun registroDao(): RegistroDao
    abstract fun chapetaDao(): ChapetaDao
    abstract fun perfilFincaDao(): PerfilFincaDao
    abstract fun usuarioDao(): UsuarioDao
    abstract fun pesajePendienteDao(): PesajePendienteDao
    abstract fun siblingDao(): SiblingDao
    abstract fun rechazoDao(): RechazoDao
    abstract fun estadoSyncDao(): EstadoSyncDao
    abstract fun resolucionDao(): ResolucionDao

    companion object {
        const val INDICE_CHAPETA_CODIGO_ACTIVA = "index_chapeta_codigo_activa"

        // Room no declara índices parciales: una sola Chapeta activa por codigo se garantiza aquí.
        val CALLBACK_INDICES_CHAPETA = object : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS $INDICE_CHAPETA_CODIGO_ACTIVA " +
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
                    // La versión 10 es el primer esquema exportado: desde ahí cada cambio lleva su
                    // Migration. Una instalación anterior no tiene cómo migrarse y se recrea vacía.
                    .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2, 3, 4, 5, 6, 7, 8, 9)
                    .addMigrations(MIGRACION_10_11, MIGRACION_11_12, MIGRACION_12_13, MIGRACION_13_14, MIGRACION_14_15, MIGRACION_15_16, MIGRACION_16_17)
                    .addCallback(CALLBACK_INDICES_CHAPETA)
                    .build()
                    .also { instance = it }
            }
    }
}
