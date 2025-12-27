#!/bin/bash

# === CONFIGURATION ===
BACKUP_DIR="/opt/backups"
APP_DIR="/opt/app"
DB_NAME="fam_vest_app"
DATE=$(date +"%Y-%m-%d_%H-%M-%S")
RETENTION_DAYS=7
BUCKET_NAME="fam-vest-app-bucket/app-backup"

# Create backup directory if it doesn't exist
mkdir -p "$BACKUP_DIR"

# === BACKUP DATABASE ===
echo "[$DATE] Starting PostgreSQL backup..."

su - postgres -c "pg_dump $DB_NAME > /tmp/db_backup_$DATE.sql"

mv /tmp/db_backup_$DATE.sql "$BACKUP_DIR/"

if [ $? -ne 0 ]; then
  echo "[$DATE] PostgreSQL backup failed!"
  exit 1
fi

# === BACKUP APPLICATION DIRECTORY ===
echo "[$DATE] Archiving application directory..."
tar -czf "$BACKUP_DIR/app_backup_$DATE.tar.gz" -C "$APP_DIR" .

# === PUSH TO LINODE S3 ===
echo "[$DATE] Uploading backups to Linode S3..."
s3cmd put $BACKUP_DIR/db_backup_$DATE.sql s3://$BUCKET_NAME/db_backup.sql
s3cmd put $BACKUP_DIR/app_backup_$DATE.tar.gz s3://$BUCKET_NAME/app_backup.tar.gz
#s3cmd sync $BACKUP_DIR/ s3://$BUCKET_NAME/ -P

if [ $? -ne 0 ]; then
  echo "[$DATE] Upload to Linode S3 failed!"
  exit 1
fi

# === CLEANUP OLD BACKUPS (LOCAL) ===
echo "[$DATE] Cleaning up local backups older than $RETENTION_DAYS days..."
find "$BACKUP_DIR" -type f -mtime +$RETENTION_DAYS -exec rm -f {} \;

echo "[$DATE] Backup complete."

