#!/bin/bash
/usr/bin/psql -w -h $POSTGRES_DB_HOST -U $POSTGRES_USER -lqt | /usr/bin/cut -d '|' -f 1 | grep -w metabaseappdb || \
(/usr/bin/psql -w -h $POSTGRES_DB_HOST -U $POSTGRES_USER -c "CREATE DATABASE metabaseappdb");

/usr/bin/psql -w -h $POSTGRES_DB_HOST -U $POSTGRES_USER -c "SELECT usename from pg_catalog.pg_user" | /usr/bin/cut -d '|' -f 1 | grep -w $MB_PG_USER || \
(/usr/bin/psql -w -h $POSTGRES_DB_HOST -U $POSTGRES_USER -c "CREATE USER $MB_PG_USER WITH encrypted PASSWORD '$MB_PG_PASSWORD';" \
    && /usr/bin/psql -w -h $POSTGRES_DB_HOST -U $POSTGRES_USER -d metabaseappdb -c "GRANT ALL ON DATABASE metabaseappdb TO $MB_PG_USER" \
    && /usr/bin/psql -w -h $POSTGRES_DB_HOST -U $POSTGRES_USER -d metabaseappdb -c "GRANT ALL PRIVILEGES ON SCHEMA public TO $MB_PG_USER" \
    && /usr/bin/psql -w -h $POSTGRES_DB_HOST -U $POSTGRES_USER -c "GRANT SELECT ON ALL TABLES IN SCHEMA public TO $MB_PG_USER;" \
    && /usr/bin/psql -w -h $POSTGRES_DB_HOST -U $POSTGRES_USER -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO $MB_PG_USER;"
);

