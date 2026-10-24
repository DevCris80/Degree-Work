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

    // Editar deja la fila pendiente de sincronizar, con su revisión de partida. La baja y la finca no se tocan.
    @Query(
        "UPDATE animal SET nombre = :nombre, raza = :raza, sexo = :sexo, etapa = :etapa, " +
            "fechaNacimiento = :fechaNacimiento, fechaNacimientoEsEstimada = :fechaNacimientoEsEstimada, " +
            "proposito = :proposito, fotoUri = :fotoUri, idPadre = :idPadre, idMadre = :idMadre, " +
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

    @Update
    suspend fun update(animal: AnimalEntity)

    // El servidor aceptó una edición que se volvió a editar mientras subía: la edición nueva parte de esa revisión.
    @Query("UPDATE animal SET rev = :rev, revBase = :rev WHERE idAnimal = :idAnimal")
    suspend fun rebasar(idAnimal: String, rev: Int)

    // Solo para un Animal que el servidor rechazó y no tiene (ADR-0004, regla 7).
    @Query("DELETE FROM animal WHERE idAnimal = :idAnimal")
    suspend fun delete(idAnimal: String)
}
