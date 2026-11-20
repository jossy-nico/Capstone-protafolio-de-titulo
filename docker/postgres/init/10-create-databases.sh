#!/bin/sh
set -eu

# Solo crea bases ausentes. Las bases existentes se conservan sin cambios.
# Si una base ya existe con otro propietario, se detiene para evitar un
# arranque posterior con permisos insuficientes; no altera su propietario.
for database in producto_db usuario_db carrito_db venta_db resenna_db; do
  owner="$(psql -X -v ON_ERROR_STOP=1 -d postgres -Atqc \
    "SELECT pg_get_userbyid(datdba) FROM pg_database WHERE datname = '$database'")"

  if [ -n "$owner" ]; then
    if [ "$owner" = "$PGUSER" ]; then
      echo "La base $database ya existe y pertenece a $PGUSER; se conserva."
    else
      echo "ERROR: la base $database ya existe y pertenece a '$owner', no a '$PGUSER'." >&2
      echo "No se modificó la base. Revisa los permisos antes de iniciar los microservicios." >&2
      exit 1
    fi
  else
    echo "Creando base $database..."
    psql -X -v ON_ERROR_STOP=1 -d postgres \
      -c "CREATE DATABASE \"$database\" OWNER \"$PGUSER\";"
  fi
done

echo "Inicialización de bases completada."
