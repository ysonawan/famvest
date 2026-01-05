#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

# === CONFIGURATION ===
BACKUP_DIR="/opt/backups"
APP_DIR="/opt/app"

FAMVEST_DB_NAME="fam_vest_app"
NETLY_DB_NAME="netly_app"
DUEBOOK_DB_NAME="duebook_app"
DATE=$(date +"%Y-%m-%d_%H-%M-%S")
RETENTION_DAYS=30
BUCKET_NAME="fam-vest-app-bucket/app-backup"

# === SETUP ===
echo "[$DATE] Starting backup process..."
mkdir -p "$BACKUP_DIR"

# === BACKUP DATABASES ===
echo "[$DATE] Backing up PostgreSQL databases..."

for DB_NAME in "$FAMVEST_DB_NAME" "$NETLY_DB_NAME" "$DUEBOOK_DB_NAME"; do
  TMP_FILE="/tmp/${DB_NAME}_db_backup_${DATE}.sql"
  FINAL_FILE="${BACKUP_DIR}/${DB_NAME}_db_backup_${DATE}.sql"

  echo "[$DATE] Dumping database: $DB_NAME"
  if su - postgres -c "pg_dump ${DB_NAME} > '${TMP_FILE}'"; then
    mv "${TMP_FILE}" "${FINAL_FILE}"
    echo "[$DATE] Backup completed: ${FINAL_FILE}"
  else
    echo "[$DATE] ❌ Error: Failed to back up database ${DB_NAME}" >&2
    exit 1
  fi
done

# === BACKUP APPLICATION DIRECTORY ===
echo "[$DATE] Archiving application directory..."
APP_BACKUP_FILE="${BACKUP_DIR}/app_backup_${DATE}.tar.gz"
tar -czf "${APP_BACKUP_FILE}" -C "$APP_DIR" .
echo "[$DATE] Application backup created: ${APP_BACKUP_FILE}"

# === UPLOAD TO LINODE S3 ===
echo "[$DATE] Uploading backups to Linode S3..."
for FILE in "${BACKUP_DIR}/${FAMVEST_DB_NAME}_db_backup_${DATE}.sql" \
            "${BACKUP_DIR}/${NETLY_DB_NAME}_db_backup_${DATE}.sql" \
            "${BACKUP_DIR}/${DUEBOOK_DB_NAME}_db_backup_${DATE}.sql" \
            "${APP_BACKUP_FILE}"; do

ORIGINAL_NAME=$(basename "$FILE")
# Remove _YYYY-MM-DD_HH-MM-SS from filename for S3
S3_NAME=$(echo "$ORIGINAL_NAME" | \
    sed -E 's/_[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{2}-[0-9]{2}-[0-9]{2}//')
echo "[$DATE] Uploading ${ORIGINAL_NAME} as ${S3_NAME}..."
s3cmd put "$FILE" "s3://${BUCKET_NAME}/${S3_NAME}"

done

echo "[$DATE] ✅ Upload to Linode S3 completed."

# === CLEANUP OLD LOCAL BACKUPS ===
echo "[$DATE] Cleaning up local backups older than ${RETENTION_DAYS} days..."
find "$BACKUP_DIR" -type f -mtime +$RETENTION_DAYS -exec rm -f {} \;

echo "[$DATE] 🎉 Backup process completed successfully!"
