package com.devcris80.prototipo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.devcris80.prototipo.data.local.entity.AnimalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimalDao {
    @Insert
    suspend fun insert(animal: AnimalEntity)

    @Query("SELECT * FROM animal WHERE fechaBaja IS NULL ORDER BY nombre ASC")
    fun observeActivos(): Flow<List<AnimalEntity>>

    @Query("SELECT * FROM animal WHERE sexo = :sexo ORDER BY fechaBaja IS NOT NULL, nombre ASC")
    fun observePorSexo(sexo: String): Flow<List<AnimalEntity>>

    @Query("SELECT * FROM animal WHERE idAnimal = :idAnimal")
    fun observeById(idAnimal: String): Flow<AnimalEntity?>

    @Query("SELECT * FROM animal WHERE idAnimal = :idAnimal")
    suspend fun findById(idAnimal: String): AnimalEntity?

    // Editar deja la fila pendiente de sincronizar, con su revisión de partida. La baja y la finca no se tocan. Una
    // foto nueva queda además por subir; una quitada, ya no (la del servidor vuelve a bajar: allá nadie borra). SQLite
    // calcula cada SET con la fila de antes.
    @Query(
        "UPDATE animal SET nombre = :nombre, raza = :raza, sexo = :sexo, etapa = :etapa, " +
            "fechaNacimiento = :fechaNacimiento, fechaNacimientoEsEstimada = :fechaNacimientoEsEstimada, " +
            "proposito = :proposito, idPadre = :idPadre, idMadre = :idMadre, " +
            "fotoPorSubir = CASE WHEN :fotoUri IS NULL THEN 0 WHEN fotoUri IS NOT :fotoUri THEN 1 ELSE fotoPorSubir END, " +
            "fotoVersion = CASE WHEN :fotoUri IS NULL THEN NULL ELSE fotoVersion END, fotoUri = :fotoUri, " +
            EDICION_LOCAL + " WHERE idAnimal = :idAnimal",
    )
    suspend fun editar(
        idAnimal: String,
        nombre: String,
        raza: String,
        sexo: String,
        etapa: String,
        fechaNacimiento: Long,
        fechaNacimientoEsEstimada: Boolean,
        proposito: String,
        fotoUri: String?,
        idPadre: String?,
        idMadre: String?,
        idVersion: String,
        fechaModificacion: Long,
    )

    // Dar de baja es una modificación: la fila vuelve a quedar pendiente de sincronizar.
    @Query("UPDATE animal SET fechaBaja = :fecha, " + EDICION_LOCAL + " WHERE idAnimal = :idAnimal")
    suspend fun darDeBaja(idAnimal: String, fecha: Long, idVersion: String, fechaModificacion: Long)

    // Lo que usa la subida (MotorSincronizacion). Sus escrituras no llevan EDICION_LOCAL.
    /** Los Animales con cambios por subir, del cambio más antiguo al más reciente. */
    @Query("SELECT * FROM animal WHERE sincronizado = 0 ORDER BY fechaModificacion")
    suspend fun findPendientes(): List<AnimalEntity>

    @Query("SELECT * FROM animal WHERE idPadre = :idAnimal OR idMadre = :idAnimal")
    suspend fun findCrias(idAnimal: String): List<AnimalEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM animal WHERE idPadre = :idAnimal OR idMadre = :idAnimal)")
    suspend fun tieneCrias(idAnimal: String): Boolean

    @Update
    suspend fun update(animal: AnimalEntity)

    // El servidor aceptó una edición que se volvió a editar mientras subía: la edición nueva parte de esa revisión.
    @Query("UPDATE animal SET rev = :rev, revBase = :rev WHERE idAnimal = :idAnimal")
    suspend fun rebasar(idAnimal: String, rev: Int)

    // Lo que usa la sincronización de fotos. Tampoco lleva EDICION_LOCAL: la foto no viaja en la fila.
    /** Las fotos por subir de los Animales que el servidor ya tiene: antes, el bucket no las acepta. */
    @Query("SELECT * FROM animal WHERE fotoPorSubir = 1 AND fotoUri IS NOT NULL AND rev IS NOT NULL")
    suspend fun findFotosPorSubir(): List<AnimalEntity>

    // Si la foto cambió mientras subía, la nueva sigue por subir.
    @Query("UPDATE animal SET fotoPorSubir = 0 WHERE idAnimal = :idAnimal AND fotoUri = :fotoUri")
    suspend fun quitarFotoPorSubir(idAnimal: String, fotoUri: String)

    // Storage no aceptará nunca esta foto: el Animal queda sin ella, y sin la fecha del servidor para que la del
    // servidor, si hay una, vuelva a bajar. Si la foto cambió mientras subía, la nueva sigue por subir.
    @Query(
        "UPDATE animal SET fotoPorSubir = 0, fotoUri = NULL, fotoVersion = NULL " +
            "WHERE idAnimal = :idAnimal AND fotoUri = :fotoUri",
    )
    suspend fun descartarFotoRechazada(idAnimal: String, fotoUri: String)

    // Nunca pisa una foto local que todavía no ha subido. Devuelve cuántas filas cambió: 0 si la local cambió.
    @Query(
        "UPDATE animal SET fotoUri = :fotoUri, fotoVersion = :fechaFotoServidor WHERE idAnimal = :idAnimal AND fotoPorSubir = 0",
    )
    suspend fun guardarFotoDelServidor(idAnimal: String, fotoUri: String?, fechaFotoServidor: String): Int

    @Query("SELECT fotoUri FROM animal WHERE fotoUri IS NOT NULL")
    suspend fun findFotoUris(): List<String>

    // Solo para un Animal que el servidor rechazó y no tiene (ADR-0004, regla 7).
    @Query("DELETE FROM animal WHERE idAnimal = :idAnimal")
    suspend fun delete(idAnimal: String)
}
