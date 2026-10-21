#!/bin/sh
# Creates the least-privilege application role (docs/security.md §4.8).
#
# Runs once, when the postgres container initialises an EMPTY data volume
# (/docker-entrypoint-initdb.d). An existing volume keeps its old roles:
# `docker compose down -v` to re-initialise.
#
#   POSTGRES_USER  (= FLYWAY_USER)  schema owner; Flyway migrates as this role.
#   DB_USER / DB_PASSWORD           the app's runtime role: CONNECT + USAGE on
#                                   schema public + DML on the owner's tables.
#                                   No CREATE, no DDL, not superuser.
#
# Values come only from the environment (.env via docker-compose.yml). They
# are passed to psql as variables and quoted with format(%I / %L), so no value
# is ever pasted into SQL text by the shell.
set -eu

: "${POSTGRES_USER:?}" "${POSTGRES_DB:?}"
: "${DB_USER:?set DB_USER in .env}" "${DB_PASSWORD:?set DB_PASSWORD in .env}"

psql -v ON_ERROR_STOP=1 --no-psqlrc --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
    -v app_user="$DB_USER" -v app_password="$DB_PASSWORD" \
    -v owner="$POSTGRES_USER" -v db="$POSTGRES_DB" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION',
              :'app_user', :'app_password') \gexec
SELECT format('GRANT CONNECT ON DATABASE %I TO %I', :'db', :'app_user') \gexec
SELECT format('GRANT USAGE ON SCHEMA public TO %I', :'app_user') \gexec
-- Tables (and sequences) the owner creates later (Flyway) are readable/writable by the app, nothing more.
SELECT format('ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO %I',
              :'owner', :'app_user') \gexec
SELECT format('ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO %I',
              :'owner', :'app_user') \gexec
SQL
