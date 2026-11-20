# Natur Cosecha — Docker Compose

Esta configuración levanta PostgreSQL y los cinco microservicios Spring Boot de Fase 2. No incluye MySQL ni contenedores para Android o React Admin.

## Servicios y bases

| Servicio Compose | Puerto | Base de datos |
|---|---:|---|
| productos | 8080 | producto_db |
| usuarios | 8181 | usuario_db |
| carrito | 8282 | carrito_db |
| ventas | 8383 | venta_db |
| resenas | 8484 | resenna_db |
| postgres | 5432 | cluster PostgreSQL |

Los contenedores se conectan a PostgreSQL mediante el hostname interno `postgres`, nunca mediante `localhost`.

## Requisitos

- Docker Engine y Docker Compose v2.
- Java 17 en las imágenes de compilación/runtime.
- OpenSSL disponible para generar claves RSA si todavía no existe el par de claves de la aplicación.

## Preparación de configuración y secretos

1. En la raíz del repositorio, copia `.env.example` a `.env`.
2. Sustituye los valores de ejemplo por credenciales reales. `.env` está excluido de Git.
3. Completa las credenciales de Cloudinary y Webpay. No guardes claves reales en el repositorio, Dockerfiles ni archivos de configuración versionados.
4. Crea `secrets/jwt/` y coloca allí `private.pem` y `public.pem`, correspondientes al mismo par RSA que usa la aplicación.

Ejemplo para generar un par nuevo (solo si corresponde; reemplazar las claves puede invalidar tokens emitidos previamente):

```powershell
New-Item -ItemType Directory -Force secrets/jwt
openssl genpkey -algorithm RSA -out secrets/jwt/private.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -pubout -in secrets/jwt/private.pem -out secrets/jwt/public.pem
```

Las claves se montan en modo solo lectura dentro de Usuarios y Ventas. La carpeta `secrets/` está excluida de Git.

## Volúmenes y datos existentes: leer antes de levantar

El volumen PostgreSQL persistente se llama por defecto `natur-cosecha-postgres-data`. Antes de ejecutar Compose:

1. Consulta los volúmenes existentes con `docker volume ls`.
2. Si ya existe un volumen PostgreSQL que contiene los datos que deben conservarse, define `POSTGRES_VOLUME_NAME` en `.env` con el nombre exacto de ese volumen.
3. Confirma que el volumen corresponde a PostgreSQL y que su versión mayor es compatible con la imagen seleccionada. **Nunca montes un volumen MySQL como PostgreSQL.**
4. Si el volumen existente pertenece a MySQL, consérvalo intacto y planifica una migración lógica independiente hacia PostgreSQL. No se realiza conversión automática.

Si se usa un volumen PostgreSQL ya inicializado, las variables `POSTGRES_USER` y `POSTGRES_PASSWORD` no recrean ni cambian automáticamente el rol existente. Deben coincidir con un usuario PostgreSQL ya válido en ese cluster.

## Levantar servicios

Desde la raíz del repositorio:

```powershell
docker compose up --build -d
docker compose ps
docker compose logs -f postgres db-init
```

El servicio `db-init` se ejecuta después de que PostgreSQL está saludable y antes de los microservicios. El script `docker/postgres/init/10-create-databases.sh` consulta cada base y solo crea las que faltan. Es idempotente: puede ejecutarse nuevamente sin borrar ni recrear bases existentes. Si una base ya existe con un propietario distinto de `POSTGRES_USER`, el script falla deliberadamente sin modificarla; revisa permisos/propietario antes de iniciar las APIs. El rol usado para inicializar debe tener privilegio `CREATEDB` en el cluster.

Para repetir únicamente la inicialización:

```powershell
docker compose run --rm db-init
```

## Verificación de las bases

```powershell
docker compose exec postgres psql -U naturcosecha -d postgres -c "\\l"
```

Si configuraste otro `POSTGRES_USER`, úsalo en el comando.

## Variables y comunicación entre servicios

- Los servicios reciben `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `DB_PASSWORD` desde Compose.
- Productos recibe variables Cloudinary.
- Usuarios recibe rutas de clave privada y pública RSA montadas desde `secrets/jwt`.
- Ventas recibe la clave pública RSA, `WEBPAY_API_KEY`, y las URLs internas de Productos y Usuarios.
- Reseñas recibe las URLs internas de Usuarios y Ventas; sus clientes HTTP leen esas URLs desde propiedades configurables. `depends_on: service_started` ordena el inicio, pero no garantiza que los endpoints HTTP estén listos; las llamadas entre servicios se realizan durante las operaciones y deben verificarse en la prueba de integración.
- Webpay permanece en modo de integración TEST en el código actual. Esta configuración no habilita pagos de producción.

## Frontends

Android y React Admin no se construyen en este Compose. Para Android emulador, `10.0.2.2` sigue siendo la dirección del host. Para un dispositivo físico, se requiere la IP LAN o un dominio accesible. React Admin debe usar URLs accesibles desde el navegador del usuario; `localhost` solo funciona cuando navegador y APIs están en el mismo equipo.

## Detener sin borrar datos

Para detener y eliminar los contenedores/red de Compose conservando el volumen:

```powershell
docker compose down
```

**No ejecutar** `docker compose down -v`, `docker volume rm` ni `docker volume prune` en este proyecto: pueden eliminar datos persistentes.

## Limitaciones de esta configuración

- No se ejecutó una construcción de imágenes ni pruebas de integración desde GitHub.
- Las imágenes se compilan usando el Gradle Wrapper de cada microservicio; los módulos mantienen sus propias versiones de Gradle y Spring Boot.
- No se modifican esquemas ni se migran datos MySQL.
- La inicialización crea bases, no tablas ni datos de negocio. Hibernate conserva la configuración existente `ddl-auto=update`.
- Antes de producción se deben revisar HTTPS, exposición de puertos, almacenamiento de secretos y políticas de backup/restore.
