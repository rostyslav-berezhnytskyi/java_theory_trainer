# Docker Deployment

This setup keeps environment-specific values in `.env`.

## Local or Server Files

Create `.env` from `.env.example` and change values for the machine.

Important server values:

- `APP_HOST_PORT`: host port for the web app.
- `DB_HOST_PORT`: host port for PostgreSQL, only needed if you want to connect from host tools.
- `DB_DATA_PATH`: persistent PostgreSQL data folder or Docker volume name.
- `APP_UPLOADS_PATH`: persistent uploaded images folder.
- `BACKUP_PATH`: folder for `.sql.gz` backups.
- `APP_IMAGE`: Docker image name, for example `your-user/java-theory-trainer:latest`.

For a Linux server, absolute paths are recommended:

```properties
DB_DATA_PATH=/opt/java-theory-trainer/data/postgres
APP_UPLOADS_PATH=/opt/java-theory-trainer/data/uploads
BACKUP_PATH=/opt/java-theory-trainer/backups
```

## Build Locally

```bash
docker compose build
docker compose up -d
```

## Push To Docker Hub

```bash
docker compose build java-theory-trainer-app
docker push your-user/java-theory-trainer:latest
```

On the server, set `APP_IMAGE=your-user/java-theory-trainer:latest`, then:

```bash
docker compose pull java-theory-trainer-app
docker compose up -d
```

## Backups

The `java-theory-trainer-backup` service runs `pg_dump` on a schedule.

Defaults:

- one backup every 24 hours: `BACKUP_INTERVAL_SECONDS=86400`
- keep backups for 14 days: `BACKUP_RETENTION_DAYS=14`

Manual backup:

```bash
docker compose exec java-theory-trainer-db pg_dump -U "$DB_USERNAME" -d "$DB_NAME" | gzip > backup.sql.gz
```

Restore example:

```bash
gunzip -c backup.sql.gz | docker compose exec -T java-theory-trainer-db psql -U "$DB_USERNAME" -d "$DB_NAME"
```

## Ports

Inside Docker, the app connects to PostgreSQL on `java-theory-trainer-db:5432`.
The host port can be anything via `DB_HOST_PORT`, so it will not conflict with other server apps.
