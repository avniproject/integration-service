#!/bin/bash
# JSS Avni-Bahmni Integration - Restore OpenMRS Backup to RDS
# This script restores the OpenMRS database backup to the RDS instance

set -e

AWS_REGION="ap-south-1"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Default backup file location
BACKUP_FILE="${BACKUP_FILE:-/Users/himeshr/integration-setup/JSS Avni Bahmni Integration/openmrsdb_backup.sql.gz}"

restore_backup() {
    local RDS_ENDPOINT=$1
    local DB_USERNAME=$2
    local DB_PASSWORD=$3
    local DB_NAME=${4:-openmrs}
    
    if [ ! -f "$BACKUP_FILE" ]; then
        log_error "Backup file not found: $BACKUP_FILE"
        exit 1
    fi
    
    log_info "Backup file: $BACKUP_FILE"
    log_info "File size: $(ls -lh "$BACKUP_FILE" | awk '{print $5}')"
    
    # Check if mysql client is installed
    if ! command -v mysql &> /dev/null; then
        log_error "MySQL client not installed. Install with: brew install mysql-client"
        exit 1
    fi
    
    # Test connection
    log_info "Testing connection to RDS..."
    if ! mysql -h "$RDS_ENDPOINT" -u "$DB_USERNAME" -p"$DB_PASSWORD" -e "SELECT 1" > /dev/null 2>&1; then
        log_error "Cannot connect to RDS. Check endpoint, credentials, and security group."
        exit 1
    fi
    log_info "Connection successful!"
    
    # Create database if not exists
    log_info "Creating database $DB_NAME if not exists..."
    mysql -h "$RDS_ENDPOINT" -u "$DB_USERNAME" -p"$DB_PASSWORD" -e "CREATE DATABASE IF NOT EXISTS $DB_NAME CHARACTER SET utf8 COLLATE utf8_general_ci;"
    
    # Restore backup
    log_info "Restoring backup to $DB_NAME... This may take several minutes."
    
    # Check if file is gzipped
    if [[ "$BACKUP_FILE" == *.gz ]]; then
        log_info "Decompressing and restoring gzipped backup..."
        gunzip -c "$BACKUP_FILE" | mysql -h "$RDS_ENDPOINT" -u "$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME"
    else
        log_info "Restoring uncompressed backup..."
        mysql -h "$RDS_ENDPOINT" -u "$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" < "$BACKUP_FILE"
    fi
    
    log_info "Backup restored successfully!"
    
    # Verify restoration
    log_info "Verifying restoration..."
    TABLE_COUNT=$(mysql -h "$RDS_ENDPOINT" -u "$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '$DB_NAME';")
    log_info "Tables in database: $TABLE_COUNT"
    
    # Show some key tables
    log_info "Key OpenMRS tables:"
    mysql -h "$RDS_ENDPOINT" -u "$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" -e "
        SELECT table_name, table_rows 
        FROM information_schema.tables 
        WHERE table_schema = '$DB_NAME' 
        AND table_name IN ('patient', 'person', 'encounter', 'obs', 'concept', 'visit')
        ORDER BY table_name;"
}

create_snapshot() {
    local DB_INSTANCE_ID=$1
    local SNAPSHOT_ID="${DB_INSTANCE_ID}-baseline-$(date +%Y%m%d)"
    
    log_info "Creating RDS snapshot: $SNAPSHOT_ID"
    
    aws rds create-db-snapshot \
        --db-instance-identifier "$DB_INSTANCE_ID" \
        --db-snapshot-identifier "$SNAPSHOT_ID" \
        --tags Key=Project,Value=jss-avni-bahmni Key=Type,Value=baseline \
        --region $AWS_REGION
    
    log_info "Snapshot creation initiated. Check status with:"
    log_info "  aws rds describe-db-snapshots --db-snapshot-identifier $SNAPSHOT_ID --region $AWS_REGION"
}

main() {
    case "$1" in
        "restore")
            if [ -z "$2" ] || [ -z "$3" ] || [ -z "$4" ]; then
                log_error "Usage: $0 restore <rds-endpoint> <username> <password> [database-name]"
                exit 1
            fi
            restore_backup "$2" "$3" "$4" "${5:-openmrs}"
            ;;
        "snapshot")
            if [ -z "$2" ]; then
                log_error "Usage: $0 snapshot <db-instance-id>"
                exit 1
            fi
            create_snapshot "$2"
            ;;
        "verify")
            if [ -z "$2" ] || [ -z "$3" ] || [ -z "$4" ]; then
                log_error "Usage: $0 verify <rds-endpoint> <username> <password>"
                exit 1
            fi
            log_info "Verifying database..."
            mysql -h "$2" -u "$3" -p"$4" -e "SHOW DATABASES;"
            ;;
        *)
            echo "Usage: $0 <command>"
            echo ""
            echo "Commands:"
            echo "  restore <endpoint> <user> <pass> [db]  Restore backup to RDS"
            echo "  snapshot <db-instance-id>              Create baseline snapshot"
            echo "  verify <endpoint> <user> <pass>        Verify RDS connection"
            echo ""
            echo "Environment variables:"
            echo "  BACKUP_FILE  Path to backup file (default: /Users/himeshr/integration-setup/JSS Avni Bahmni Integration/openmrsdb_backup.sql.gz)"
            echo ""
            echo "Example:"
            echo "  $0 restore jss-avni-bahmni-prerelease-mysql.xxx.ap-south-1.rds.amazonaws.com openmrs_admin 'MyPassword123' openmrs"
            ;;
    esac
}

main "$@"
