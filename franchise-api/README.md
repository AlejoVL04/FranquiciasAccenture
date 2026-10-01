# Franchise API

API REST para gestionar franquicias, sus sucursales y los productos que ofrece cada sucursal, con control de stock.

**Stack:** Java 21 · Spring Boot 3.5 · Spring Data JPA · MySQL 8.4 · Flyway · springdoc-openapi · JUnit 5 / Mockito · Testcontainers · Docker.

---

## Modelo

```
Franquicia (nombre único global)
 └── Sucursal (nombre único dentro de la franquicia)
      └── Producto (nombre único dentro de la sucursal, stock >= 0)
```

- El esquema lo crea y versiona **Flyway** (`src/main/resources/db/migration`). Hibernate solo lo valida (`ddl-auto: validate`).
- Las reglas de unicidad se validan en el servicio (respuesta 409) y además están respaldadas por restricciones `UNIQUE` en la base de datos. Los nombres se comparan sin distinguir mayúsculas y se recortan los espacios.
- El stock nunca puede ser negativo: lo validan el DTO, el servicio y una restricción `CHECK` en MySQL.
- Eliminar una franquicia o sucursal elimina en cascada sus hijos (`ON DELETE CASCADE`).

## Endpoints

Base: `/api/v1`

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/franchises` | Crear franquicia |
| `GET` | `/franchises` | Listar franquicias |
| `GET` | `/franchises/{franchiseId}` | Consultar franquicia |
| `PATCH` | `/franchises/{franchiseId}/name` | Renombrar franquicia |
| `POST` | `/franchises/{franchiseId}/branches` | Agregar sucursal a una franquicia |
| `GET` | `/franchises/{franchiseId}/branches` | Listar sucursales de una franquicia |
| `GET` | `/franchises/{franchiseId}/top-stock-products` | Producto con más stock por sucursal |
| `PATCH` | `/branches/{branchId}/name` | Renombrar sucursal |
| `POST` | `/branches/{branchId}/products` | Agregar producto a una sucursal |
| `GET` | `/branches/{branchId}/products` | Listar productos de una sucursal |
| `DELETE` | `/branches/{branchId}/products/{productId}` | Eliminar producto de una sucursal |
| `PATCH` | `/branches/{branchId}/products/{productId}/stock` | Modificar stock de un producto |
| `PATCH` | `/branches/{branchId}/products/{productId}/name` | Renombrar producto |
| `GET` | `/products/{productId}` | Consultar producto |

La documentación interactiva (Swagger UI) queda en `http://localhost:8080/swagger-ui.html` y el contrato OpenAPI en `/v3/api-docs`. El health check está en `/actuator/health`.

### Producto con más stock por sucursal

`GET /franchises/{id}/top-stock-products` devuelve, para cada sucursal de la franquicia, el producto con mayor stock y la sucursal a la que pertenece. Se resuelve con **una sola consulta** usando `ROW_NUMBER() OVER (PARTITION BY branch_id ORDER BY stock DESC, id ASC)`, apoyada en el índice `(branch_id, stock DESC)`; no hay problema N+1.

- **Empates:** gana el producto más antiguo (menor id), así el resultado es determinista.
- **Stock 0:** es un máximo válido y se reporta.
- **Sucursales sin productos:** se omiten por defecto; con `?includeBranchesWithoutProducts=true` aparecen con los campos de producto en `null`.

### Errores

Todas las respuestas de error usan el mismo formato:

```json
{
  "timestamp": "2026-09-30T18:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Franchise with id 10 not found",
  "path": "/api/v1/franchises/10"
}
```

| Código | Cuándo |
|---|---|
| 400 | Validación fallida (`validationErrors` indica el campo), JSON mal formado, id no numérico, stock negativo |
| 404 | El recurso no existe, o el producto no pertenece a la sucursal indicada en la ruta |
| 409 | Nombre duplicado en su ámbito (al crear o al renombrar) |

## Ejecución

### Con Docker Compose (recomendado)

Requisitos: Docker Desktop.

```bash
cp .env.example .env
docker compose up -d --build
```

Levanta MySQL y la API; la API espera a que MySQL esté sano y Flyway aplica las migraciones al arrancar. La API queda en `http://localhost:8080`.

```bash
docker compose logs -f api   # ver logs
docker compose down          # detener
docker compose down -v       # detener y borrar los datos
```

### Local con Maven

Requisitos: JDK 21, Maven 3.9 y un MySQL accesible (por ejemplo, solo el servicio de base de datos: `docker compose up -d mysql`).

```bash
mvn spring-boot:run
```

Variables de entorno soportadas (con sus valores por defecto): `DB_HOST=localhost`, `DB_PORT=3306`, `DB_NAME=franchise_db`, `DB_USERNAME=franchise_user`, `DB_PASSWORD=franchise_password`, `SERVER_PORT=8080`.

## Ejemplo rápido

```bash
# Franquicia
curl -s -X POST localhost:8080/api/v1/franchises \
  -H 'Content-Type: application/json' -d '{"name":"Franquicia Medellin"}'

# Sucursal
curl -s -X POST localhost:8080/api/v1/franchises/1/branches \
  -H 'Content-Type: application/json' -d '{"name":"Sucursal El Poblado"}'

# Producto
curl -s -X POST localhost:8080/api/v1/branches/1/products \
  -H 'Content-Type: application/json' -d '{"name":"Laptop Lenovo","stock":25}'

# Modificar stock
curl -s -X PATCH localhost:8080/api/v1/branches/1/products/1/stock \
  -H 'Content-Type: application/json' -d '{"stock":50}'

# Renombrar producto
curl -s -X PATCH localhost:8080/api/v1/branches/1/products/1/name \
  -H 'Content-Type: application/json' -d '{"name":"Laptop Lenovo X1"}'

# Producto con más stock por sucursal
curl -s localhost:8080/api/v1/franchises/1/top-stock-products
```

## Pruebas

```bash
mvn test     # pruebas unitarias de servicios (Mockito), sin dependencias externas
mvn verify   # además, pruebas de integración contra MySQL real con Testcontainers
```

Las pruebas de integración (`*IT`) levantan un MySQL 8.4 en un contenedor y aplican las mismas migraciones de Flyway que producción, por lo que necesitan Docker en ejecución. No se usa H2 porque el reporte de stock depende de funciones de ventana de MySQL.

Sin JDK ni Maven instalados, se pueden ejecutar dentro de un contenedor:

```bash
docker run --rm -v "$PWD":/build -w /build \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
  maven:3.9-eclipse-temurin-21 mvn -B verify
```

## Estructura

```
src/main/java/com/example/franchiseapi
├── config/        OpenAPI
├── controller/    Endpoints REST
├── dto/           Request / response (records)
├── entity/        Entidades JPA
├── exception/     Excepciones de negocio y manejador global
├── mapper/        Entidad -> DTO
├── repository/    Spring Data JPA (+ proyección del reporte de stock)
└── service/       Interfaces y su implementación
src/main/resources/db/migration   Migraciones Flyway
```
