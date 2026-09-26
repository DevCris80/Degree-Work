# Diagrama de relaciones — Sistema de trazabilidad bovina

Esquema Room final, con todas las decisiones confirmadas hasta la fecha.

```mermaid
erDiagram
    USUARIO }o--|| PERFIL_FINCA : pertenece_a
    ANIMAL }o--|| PERFIL_FINCA : pertenece_a
    USUARIO ||--o{ EVENTO : registra
    USUARIO ||--o{ REGISTRO : registra
    ANIMAL ||--o{ EVENTO : tiene
    ANIMAL ||--o{ REGISTRO : tiene
    ANIMAL ||--o{ CHAPETA : tiene
    ANIMAL |o--o{ ANIMAL : es_padre_madre_de
    EVENTO ||--o| EVENTO_VACUNACION : detalle
    EVENTO ||--o| EVENTO_PARTO : detalle
    EVENTO_PARTO }o--|| ANIMAL : cria_resultante

    USUARIO {
        string idUsuario PK
        string idPerfilFinca FK
        string rol
        boolean sincronizado
    }
    PERFIL_FINCA {
        string idPerfilFinca PK
        string nombreFinca
        double latitud
        double longitud
        boolean sincronizado
    }
    ANIMAL {
        string idAnimal PK
        string idPerfilFinca FK
        string idPadre FK
        string idMadre FK
        string nombre
        string raza
        string sexo
        string etapa
        string proposito
        long fechaNacimiento
        boolean fechaNacimientoEsEstimada
        string fotoUri
        long fechaBaja
        boolean sincronizado
    }
    EVENTO {
        string idEvento PK
        string idAnimal FK
        string idUsuario FK
        string tipoEvento
        long fecha
        long fechaBaja
        boolean sincronizado
    }
    EVENTO_VACUNACION {
        string idEvento PK
        string producto
        int diasRetiroLeche
        int diasRetiroCarne
    }
    EVENTO_PARTO {
        string idEvento PK
        string idCriaResultante FK
        double pesoAlNacerKg
    }
    REGISTRO {
        string idRegistro PK
        string idAnimal FK
        string idUsuario FK
        double peso
        long fecha
        long fechaBaja
        boolean sincronizado
    }
    CHAPETA {
        string idChapeta PK
        string idAnimal FK
        string codigo
        long fechaAsociacion
        long fechaDesasociacion
        boolean sincronizado
    }
```

## Notas

- `ANIMAL.idPadre` / `ANIMAL.idMadre`: FK autorreferencial, `onDelete = SET_NULL`.
- `ANIMAL.fechaBaja`, `EVENTO.fechaBaja`, `REGISTRO.fechaBaja`: soft delete (nunca hard-delete), por exigencia de trazabilidad completa de la Ley 1659.
- `EVENTO` es tabla base; `EVENTO_VACUNACION` y `EVENTO_PARTO` son tablas de detalle 1:1 (FK = PK compartida con `EVENTO`). El patrón se extiende igual para otros subtipos (monta, inseminación, desparasitación) cuando se necesiten — no están modelados aún.
- `PERFIL_FINCA` no lleva FK propia hacia `USUARIO` (se decidió que la dirección es `USUARIO → PERFIL_FINCA` y `ANIMAL → PERFIL_FINCA`, no al revés).
