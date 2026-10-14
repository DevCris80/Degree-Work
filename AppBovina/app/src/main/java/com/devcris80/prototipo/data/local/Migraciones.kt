package com.devcris80.prototipo.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Una Migration por cada cambio de versión desde la 10, la primera con esquema exportado
// (app/schemas). Cada una deja la base igual que el esquema exportado de su versión destino.

/**
 * Base de toda Migration de la app. Al terminar de migrar, Room compara cada tabla con su esquema
 * y no conoce el índice parcial de Chapeta, así que lo toma como una diferencia y aborta. Se borra
 * antes de migrar y [AppDatabase.CALLBACK_INDICES_CHAPETA] lo vuelve a crear al abrir la base.
 */
private abstract class MigracionBovina(desde: Int, hasta: Int) : Migration(desde, hasta) {
    final override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP INDEX IF EXISTS `${AppDatabase.INDICE_CHAPETA_CODIGO_ACTIVA}`")
        migrar(db)
    }

    abstract fun migrar(db: SupportSQLiteDatabase)
}

/**
 * 10 → 11: Evento guarda el Usuario que lo registró (#44). SQLite no agrega a una tabla existente
 * una columna NOT NULL con FK, así que se recrea la tabla. Los Eventos anteriores quedan a nombre
 * del único Usuario del dispositivo: en la versión 10 no hay Eventos sin una cuenta creada.
 */
val MIGRACION_10_11: Migration = object : MigracionBovina(10, 11) {
    override fun migrar(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE `evento_nuevo` (`idEvento` TEXT NOT NULL, `idAnimal` TEXT NOT NULL, " +
                "`idUsuario` TEXT NOT NULL, `tipoEvento` TEXT NOT NULL, `fecha` INTEGER NOT NULL, " +
                "`detalle` TEXT NOT NULL, `fechaBaja` INTEGER, `sincronizado` INTEGER NOT NULL, " +
                "`fechaModificacion` INTEGER NOT NULL, PRIMARY KEY(`idEvento`), " +
                "FOREIGN KEY(`idAnimal`) REFERENCES `animal`(`idAnimal`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`idUsuario`) REFERENCES `usuario`(`idUsuario`) ON UPDATE NO ACTION ON DELETE NO ACTION )",
        )
        db.execSQL(
            "INSERT INTO `evento_nuevo` (idEvento, idAnimal, idUsuario, tipoEvento, fecha, detalle, " +
                "fechaBaja, sincronizado, fechaModificacion) " +
                "SELECT idEvento, idAnimal, (SELECT idUsuario FROM usuario LIMIT 1), tipoEvento, fecha, " +
                "detalle, fechaBaja, sincronizado, fechaModificacion FROM `evento`",
        )
        db.execSQL("DROP TABLE `evento`")
        db.execSQL("ALTER TABLE `evento_nuevo` RENAME TO `evento`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_evento_idAnimal` ON `evento` (`idAnimal`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_evento_idUsuario` ON `evento` (`idUsuario`)")
    }
}
