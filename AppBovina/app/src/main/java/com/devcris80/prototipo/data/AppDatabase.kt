package com.devcris80.prototipo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Animal::class, Evento::class, Registro::class, Chapeta::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun animalDao(): AnimalDao
    abstract fun eventoDao(): EventoDao
    abstract fun registroDao(): RegistroDao
    abstract fun chapetaDao(): ChapetaDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app-bovina.db",
                ).build().also { instance = it }
            }
    }
}
