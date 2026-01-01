#!/bin/bash
# JSS Bahmni Docker Setup Script
# This script sets up Bahmni Docker on an EC2 instance with LOCAL MySQL container
# Usage: ./setup-bahmni.sh [prerelease|prod] [--restore-db <backup-file>]

set -e

ENVIRONMENT=${1:-prerelease}
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BAHMNI_DIR="/home/ubuntu/bahmni-docker"
BACKUP_DIR="/home/ubuntu/backups"

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Skip Loki plugin - we use json-file logging via docker-compose.override.yml
skip_loki_plugin() {
    log_info "Skipping Loki plugin installation (using json-file logging instead)..."
    log_info "All services will use json-file logging driver via docker-compose.override.yml"
}

# Clone Bahmni Docker repository
clone_bahmni_docker() {
    log_info "Cloning Bahmni Docker repository..."
    if [ -d "$BAHMNI_DIR" ]; then
        log_info "Bahmni Docker directory exists, pulling latest..."
        cd "$BAHMNI_DIR"
        git pull || true
    else
        git clone https://github.com/JanSwasthyaSahyog/bahmni-docker.git "$BAHMNI_DIR"
    fi
}

# Clone JSS Config repository
clone_jss_config() {
    log_info "Cloning JSS Config repository..."
    if [ -d "$BAHMNI_DIR/jss-config" ]; then
        log_info "JSS Config directory exists, pulling latest..."
        cd "$BAHMNI_DIR/jss-config"
        git pull || true
    else
        cd "$BAHMNI_DIR"
        git clone https://github.com/JanSwasthyaSahyog/jss-config.git
    fi
}

# Generate self-signed SSL certificates
generate_ssl_certs() {
    local CERT_DIR="$BAHMNI_DIR/certs"
    log_info "Generating SSL certificates..."
    mkdir -p "$CERT_DIR"
    
    if [ -f "$CERT_DIR/cert.pem" ] && [ -f "$CERT_DIR/key.pem" ]; then
        log_info "SSL certificates already exist"
        return 0
    fi
    
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
        -keyout "$CERT_DIR/key.pem" \
        -out "$CERT_DIR/cert.pem" \
        -subj "/C=IN/ST=Chhattisgarh/L=Bilaspur/O=JSS/CN=jss-bahmni-${ENVIRONMENT}.avniproject.org"
    
    log_info "SSL certificates generated"
}

# Copy environment configuration
setup_environment() {
    log_info "Setting up environment configuration..."
    
    # Copy environment file
    if [ -f "$SCRIPT_DIR/.env.${ENVIRONMENT}" ]; then
        cp "$SCRIPT_DIR/.env.${ENVIRONMENT}" "$BAHMNI_DIR/.env"
        log_info "Copied .env.${ENVIRONMENT} to $BAHMNI_DIR/.env"
    else
        log_error "Environment file .env.${ENVIRONMENT} not found in $SCRIPT_DIR"
        exit 1
    fi
    
    # Copy docker-compose override
    if [ -f "$SCRIPT_DIR/docker-compose.override.yml" ]; then
        cp "$SCRIPT_DIR/docker-compose.override.yml" "$BAHMNI_DIR/docker-compose.override.yml"
        log_info "Copied docker-compose.override.yml"
    fi
}

# Logging is handled by docker-compose.override.yml - no need to modify main file
fix_logging_config() {
    log_info "Logging configuration handled by docker-compose.override.yml"
    log_info "All services will use json-file logging driver (no loki plugin required)"
}

# Pull Docker images
pull_images() {
    log_info "Pulling Docker images (this may take a while)..."
    cd "$BAHMNI_DIR"
    docker compose --profile emr pull
}

# Start Bahmni containers
start_bahmni() {
    log_info "Starting Bahmni containers..."
    cd "$BAHMNI_DIR"
    
    # Use override file to bypass loki logging requirement
    docker compose -f docker-compose.yml -f docker-compose.override.yml --profile emr up -d
    
    log_info "Waiting for containers to start..."
    sleep 10
    docker compose -f docker-compose.yml -f docker-compose.override.yml ps
}

# Restore database from backup file
restore_database() {
    local BACKUP_FILE=$1
    
    if [ -z "$BACKUP_FILE" ]; then
        log_error "No backup file specified"
        return 1
    fi
    
    if [ ! -f "$BACKUP_FILE" ]; then
        log_error "Backup file not found: $BACKUP_FILE"
        return 1
    fi
    
    log_info "Restoring database from: $BACKUP_FILE"
    cd "$BAHMNI_DIR"
    
    # Wait for MySQL to be ready
    log_info "Waiting for MySQL container to be ready..."
    for i in {1..30}; do
        if docker compose -f docker-compose.yml -f docker-compose.override.yml exec -T openmrsdb mysqladmin ping -h localhost -u root -p${MYSQL_ROOT_PASSWORD:-OpenMRS_JSS2024} 2>/dev/null; then
            log_info "MySQL is ready!"
            break
        fi
        log_info "Waiting... ($i/30)"
        sleep 2
    done
    
    # Get container ID
    CONTAINER_ID=$(docker compose -f docker-compose.yml -f docker-compose.override.yml ps -q openmrsdb)
    
    # Copy backup into container
    log_info "Copying backup file into container..."
    docker cp "$BACKUP_FILE" $CONTAINER_ID:/tmp/backup.sql.gz
    
    # Restore database
    log_info "Restoring database (this may take a while for large backups)..."
    docker exec $CONTAINER_ID bash -c "zcat /tmp/backup.sql.gz | mysql -u root -p${MYSQL_ROOT_PASSWORD:-OpenMRS_JSS2024} openmrs"
    
    # Verify restoration
    TABLE_COUNT=$(docker exec $CONTAINER_ID mysql -u root -p${MYSQL_ROOT_PASSWORD:-OpenMRS_JSS2024} openmrs -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='openmrs';" 2>/dev/null | tail -1)
    log_info "Database restored. Table count: $TABLE_COUNT"
    
    # Cleanup
    docker exec $CONTAINER_ID rm -f /tmp/backup.sql.gz
}

# Health check
health_check() {
    log_info "Performing health check..."
    local MAX_ATTEMPTS=30
    local ATTEMPT=1
    
    while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do
        if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/openmrs/ | grep -q "200\|302"; then
            log_info "OpenMRS is responding!"
            return 0
        fi
        log_info "Waiting for OpenMRS to start (attempt $ATTEMPT/$MAX_ATTEMPTS)..."
        sleep 30
        ATTEMPT=$((ATTEMPT + 1))
    done
    
    log_warn "OpenMRS did not respond within expected time. Check logs with: docker logs bahmni-docker-openmrs-1"
    return 1
}

# Main setup function
main() {
    log_info "=========================================="
    log_info "  JSS Bahmni Docker Setup - ${ENVIRONMENT}"
    log_info "=========================================="
    
    # Skip Loki plugin - we use json-file logging
    skip_loki_plugin
    
    # Clone repositories
    clone_bahmni_docker
    clone_jss_config
    
    # Generate SSL certificates
    generate_ssl_certs
    
    # Setup environment
    setup_environment
    
    # Fix logging if needed
    fix_logging_config
    
    # Pull images
    pull_images
    
    # Start containers
    start_bahmni
    
    log_info "=========================================="
    log_info "  Setup Complete!"
    log_info "=========================================="
    log_info ""
    log_info "Bahmni is starting up with LOCAL MySQL container."
    log_info "First startup with restored DB may take 5-10 minutes"
    log_info "due to Liquibase migrations checking the database schema."
    log_info ""
    log_info "Access URLs:"
    log_info "  - Bahmni EMR: https://jss-bahmni-${ENVIRONMENT}.avniproject.org/bahmni/home"
    log_info "  - OpenMRS:    https://jss-bahmni-${ENVIRONMENT}.avniproject.org/openmrs"
    log_info ""
    log_info "Commands:"
    log_info "  - View logs:    docker logs -f bahmni-docker-openmrs-1"
    log_info "  - Check status: docker compose -f docker-compose.yml -f docker-compose.override.yml ps"
    log_info "  - Stop:         docker compose -f docker-compose.yml -f docker-compose.override.yml --profile emr down"
    log_info "  - Restart:      docker compose -f docker-compose.yml -f docker-compose.override.yml --profile emr restart"
    log_info ""
    log_info "Database Commands:"
    log_info "  - Restore DB:   ./setup-bahmni.sh restore-db /path/to/backup.sql.gz"
    log_info "  - Check tables: docker exec openmrsdb mysql -u root -pOpenMRS_JSS2024 openmrs -e 'SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=\"openmrs\";'"
    log_info ""
    
    # Optional health check
    if [ "${2:-}" == "--wait" ]; then
        health_check
    fi
}

# Handle command line arguments
case "${1:-}" in
    "restore-db")
        if [ -z "$2" ]; then
            log_error "Usage: $0 restore-db <backup-file.sql.gz>"
            exit 1
        fi
        restore_database "$2"
        ;;
    *)
        # Run main function for setup
        main "$@"
        ;;
esac
