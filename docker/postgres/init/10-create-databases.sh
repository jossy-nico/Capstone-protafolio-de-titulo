#!/bin/sh
set -eu

# Solo crea las bases que aún no existen. Nunca elimina ni modifica bases.
for database in producto_db usuario_db carrito_db venta_db resenna_db; do
  exists="$(psql -X -v ON_ERROR_STOP=1 -d postgres -Atqc \
    "SELECT 1 FROM pg_database WHERE datname = '$database'")"

  if [ "$exists" = "1" ]; then
    echo "La base $database ya existe; se conserva."
  else
    echo "Creando base $database..."
    psql -X -v ON_ERROR_STOP=1 -d postgres \
      -c "CREATE DATABASE \"$database\" OWNER \"$PGUSER\";"
  fi
done

echo "Inicialización de bases completada."
