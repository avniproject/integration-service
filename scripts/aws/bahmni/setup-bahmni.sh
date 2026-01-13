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

# Generate self-signed SSL certificates (fallback if certbot fails)
generate_ssl_certs() {
    local CERT_DIR="$BAHMNI_DIR/certs"
    log_info "Generating self-signed SSL certificates..."
    mkdir -p "$CERT_DIR"
    
    if [ -f "$CERT_DIR/cert.pem" ] && [ -f "$CERT_DIR/key.pem" ]; then
        log_info "SSL certificates already exist"
        return 0
    fi
    
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
        -keyout "$CERT_DIR/key.pem" \
        -out "$CERT_DIR/cert.pem" \
        -subj "/C=IN/ST=Chhattisgarh/L=Bilaspur/O=JSS/CN=jss-bahmni-${ENVIRONMENT}.avniproject.org"
    
    chmod 644 "$CERT_DIR"/*.pem
    log_info "Self-signed SSL certificates generated"
}

# Setup Let's Encrypt SSL certificate using certbot
setup_letsencrypt_ssl() {
    local DOMAIN=${1:-"jss-bahmni-${ENVIRONMENT}.avniproject.org"}
    local EMAIL=${2:-"admin@avniproject.org"}
    local CERT_DIR="$BAHMNI_DIR/certs"
    
    log_info "Setting up Let's Encrypt SSL certificate for $DOMAIN..."
    
    # Install certbot if not present
    if ! command -v certbot &> /dev/null; then
        log_info "Installing certbot..."
        sudo apt-get update
        sudo apt-get install -y certbot
    fi
    
    # Stop proxy to free port 80/443 for certbot
    log_info "Stopping proxy container for certificate generation..."
    cd "$BAHMNI_DIR"
    docker compose -f docker-compose.yml -f docker-compose.override.yml stop proxy 2>/dev/null || true
    
    # Get certificate using standalone mode
    log_info "Requesting certificate from Let's Encrypt..."
    sudo certbot certonly --standalone \
        -d "$DOMAIN" \
        --non-interactive \
        --agree-tos \
        --email "$EMAIL" \
        --keep-until-expiring
    
    if [ $? -eq 0 ]; then
        # Copy certificates to Bahmni certs directory
        log_info "Copying certificates to Bahmni..."
        mkdir -p "$CERT_DIR"
        sudo cp "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" "$CERT_DIR/cert.pem"
        sudo cp "/etc/letsencrypt/live/$DOMAIN/privkey.pem" "$CERT_DIR/key.pem"
        sudo chmod 644 "$CERT_DIR"/*.pem
        
        # Setup auto-renewal hook
        setup_certbot_renewal_hook "$DOMAIN"
        
        log_info "Let's Encrypt SSL certificate installed successfully!"
    else
        log_warn "Failed to get Let's Encrypt certificate. Using self-signed certificate as fallback."
        generate_ssl_certs
    fi
    
    # Restart proxy
    log_info "Starting proxy container..."
    docker compose -f docker-compose.yml -f docker-compose.override.yml start proxy
}

# Setup certbot renewal hook to copy certs after renewal
setup_certbot_renewal_hook() {
    local DOMAIN=$1
    local HOOK_DIR="/etc/letsencrypt/renewal-hooks/deploy"
    local HOOK_FILE="$HOOK_DIR/bahmni-cert-copy.sh"
    
    log_info "Setting up certbot renewal hook..."
    
    sudo mkdir -p "$HOOK_DIR"
    sudo tee "$HOOK_FILE" > /dev/null << EOF
#!/bin/bash
# Certbot renewal hook for Bahmni
# This script copies renewed certificates to Bahmni and restarts the proxy

DOMAIN="$DOMAIN"
BAHMNI_CERT_DIR="$BAHMNI_DIR/certs"

if [ "\$RENEWED_LINEAGE" = "/etc/letsencrypt/live/\$DOMAIN" ]; then
    cp "/etc/letsencrypt/live/\$DOMAIN/fullchain.pem" "\$BAHMNI_CERT_DIR/cert.pem"
    cp "/etc/letsencrypt/live/\$DOMAIN/privkey.pem" "\$BAHMNI_CERT_DIR/key.pem"
    chmod 644 "\$BAHMNI_CERT_DIR"/*.pem
    
    # Restart proxy to pick up new certificates
    cd "$BAHMNI_DIR"
    docker compose -f docker-compose.yml -f docker-compose.override.yml restart proxy
    
    echo "[\$(date)] Bahmni SSL certificates renewed and proxy restarted"
fi
EOF
    
    sudo chmod +x "$HOOK_FILE"
    log_info "Certbot renewal hook installed at $HOOK_FILE"
}

# Renew SSL certificate manually
renew_ssl_cert() {
    local DOMAIN=${1:-"jss-bahmni-${ENVIRONMENT}.avniproject.org"}
    
    log_info "Renewing SSL certificate for $DOMAIN..."
    
    # Stop proxy
    cd "$BAHMNI_DIR"
    docker compose -f docker-compose.yml -f docker-compose.override.yml stop proxy
    
    # Renew certificate
    sudo certbot renew --cert-name "$DOMAIN" --force-renewal
    
    # Copy new certificates
    local CERT_DIR="$BAHMNI_DIR/certs"
    sudo cp "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" "$CERT_DIR/cert.pem"
    sudo cp "/etc/letsencrypt/live/$DOMAIN/privkey.pem" "$CERT_DIR/key.pem"
    sudo chmod 644 "$CERT_DIR"/*.pem
    
    # Restart proxy
    docker compose -f docker-compose.yml -f docker-compose.override.yml start proxy
    
    log_info "SSL certificate renewed successfully!"
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

# Create external named volume for MySQL data
create_db_volume() {
    local VOLUME_NAME=${1:-openmrs_db_data}
    
    log_info "Creating external named volume: $VOLUME_NAME"
    
    if docker volume inspect $VOLUME_NAME >/dev/null 2>&1; then
        log_warn "Volume $VOLUME_NAME already exists"
        return 0
    fi
    
    docker volume create $VOLUME_NAME
    log_info "Volume $VOLUME_NAME created successfully"
}

# Full database restore with MySQL 5.6 (handles JSS historical dates correctly)
# IMPORTANT: MySQL 5.6 is required because JSS data contains dates from 1942-1945 (India DST period)
# MySQL JDBC 8.x with MySQL 5.7+ causes HOUR_OF_DAY errors on these historical dates
restore_database_fresh() {
    local BACKUP_FILE=$1
    local VOLUME_NAME=${2:-openmrs_db_data}
    
    if [ -z "$BACKUP_FILE" ]; then
        log_error "Usage: $0 restore-db-fresh <backup-file.sql.gz> [volume-name]"
        return 1
    fi
    
    if [ ! -f "$BACKUP_FILE" ]; then
        log_error "Backup file not found: $BACKUP_FILE"
        return 1
    fi
    
    log_info "=========================================="
    log_info "  Fresh Database Restore with MySQL 5.6"
    log_info "=========================================="
    log_info "Backup file: $BACKUP_FILE"
    log_info "Volume name: $VOLUME_NAME"
    
    cd "$BAHMNI_DIR"
    
    # Step 1: Stop all containers
    log_info "Step 1: Stopping all containers..."
    docker compose -f docker-compose.yml -f docker-compose.override.yml --profile emr down 2>/dev/null || true
    
    # Step 2: Remove old volume if exists and create fresh one
    log_info "Step 2: Creating fresh named volume..."
    docker volume rm $VOLUME_NAME 2>/dev/null || true
    docker volume create $VOLUME_NAME
    
    # Step 3: Start only MySQL 5.6 container
    log_info "Step 3: Starting MySQL 5.6 container..."
    docker compose -f docker-compose.yml -f docker-compose.override.yml up -d openmrsdb
    
    # Step 4: Wait for MySQL to be ready
    log_info "Step 4: Waiting for MySQL to be ready..."
    for i in {1..60}; do
        if docker compose -f docker-compose.yml -f docker-compose.override.yml exec -T openmrsdb mysqladmin ping -h localhost -u root -p${MYSQL_ROOT_PASSWORD:-OpenMRS_JSS2024} 2>/dev/null; then
            log_info "MySQL is ready!"
            break
        fi
        log_info "Waiting... ($i/60)"
        sleep 5
    done
    
    # Step 5: Create openmrs_admin user (required by OpenMRS)
    log_info "Step 5: Creating openmrs_admin user..."
    docker compose -f docker-compose.yml -f docker-compose.override.yml exec -T openmrsdb mysql -u root -p${MYSQL_ROOT_PASSWORD:-OpenMRS_JSS2024} -e "
        CREATE USER IF NOT EXISTS 'openmrs_admin'@'%' IDENTIFIED BY '${OPENMRS_DB_PASSWORD:-OpenMRS_JSS2024}';
        GRANT ALL PRIVILEGES ON openmrs.* TO 'openmrs_admin'@'%';
        FLUSH PRIVILEGES;
    " 2>/dev/null || log_warn "User may already exist"
    
    # Step 6: Restore database
    log_info "Step 6: Restoring database (this may take 30-60 minutes for large backups)..."
    CONTAINER_ID=$(docker compose -f docker-compose.yml -f docker-compose.override.yml ps -q openmrsdb)
    
    # Copy backup into container
    docker cp "$BACKUP_FILE" $CONTAINER_ID:/tmp/backup.sql.gz
    
    # Restore with progress indicator
    docker exec $CONTAINER_ID bash -c "zcat /tmp/backup.sql.gz | mysql -u root -p${MYSQL_ROOT_PASSWORD:-OpenMRS_JSS2024} openmrs"
    
    # Cleanup
    docker exec $CONTAINER_ID rm -f /tmp/backup.sql.gz
    
    # Step 7: Verify restoration
    log_info "Step 7: Verifying restoration..."
    TABLE_COUNT=$(docker exec $CONTAINER_ID mysql -u root -p${MYSQL_ROOT_PASSWORD:-OpenMRS_JSS2024} openmrs -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='openmrs';" 2>/dev/null | tail -1)
    log_info "Database restored. Table count: $TABLE_COUNT"
    
    if [ "$TABLE_COUNT" -lt 200 ]; then
        log_error "Table count seems too low. Expected ~246 tables. Please verify the backup."
        return 1
    fi
    
    log_info "=========================================="
    log_info "  Database Restore Complete!"
    log_info "=========================================="
    log_info ""
    log_info "Next steps:"
    log_info "  1. Start all Bahmni services:"
    log_info "     docker compose -f docker-compose.yml -f docker-compose.override.yml --profile emr up -d"
    log_info "  2. Setup SSL certificate:"
    log_info "     ./setup-bahmni.sh setup-ssl"
    log_info "  3. Wait 5-10 minutes for OpenMRS to start (Liquibase migrations)"
    log_info "  4. Access Bahmni at: https://jss-bahmni-${ENVIRONMENT}.avniproject.org/bahmni/home"
    log_info ""
}

# Restore database from backup file (into existing container)
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
    "restore-db-fresh")
        # Fresh restore with MySQL 5.6 - handles JSS historical dates correctly
        if [ -z "$2" ]; then
            log_error "Usage: $0 restore-db-fresh <backup-file.sql.gz> [volume-name]"
            exit 1
        fi
        restore_database_fresh "$2" "${3:-openmrs_db_data}"
        ;;
    "create-volume")
        # Create external named volume for MySQL data
        create_db_volume "${2:-openmrs_db_data}"
        ;;
    "setup-ssl")
        # Setup Let's Encrypt SSL certificate
        setup_letsencrypt_ssl "${2:-}" "${3:-}"
        ;;
    "renew-ssl")
        # Renew SSL certificate
        renew_ssl_cert "${2:-}"
        ;;
    "self-signed-ssl")
        # Generate self-signed SSL certificate
        generate_ssl_certs
        ;;
    "help"|"-h"|"--help")
        echo "JSS Bahmni Docker Setup Script"
        echo ""
        echo "Usage: $0 <command> [options]"
        echo ""
        echo "Setup Commands:"
        echo "  prerelease|prod           Full setup for specified environment"
        echo ""
        echo "Database Commands:"
        echo "  restore-db-fresh <file>   Fresh restore with MySQL 5.6 (RECOMMENDED)"
        echo "                            Creates named volume, starts MySQL 5.6, restores backup"
        echo "  restore-db <file>         Restore database into existing container"
        echo "  create-volume [name]      Create external named volume (default: openmrs_db_data)"
        echo ""
        echo "SSL Commands:"
        echo "  setup-ssl [domain] [email]  Setup Let's Encrypt SSL certificate"
        echo "  renew-ssl [domain]          Renew SSL certificate"
        echo "  self-signed-ssl             Generate self-signed SSL certificate"
        echo ""
        echo "Other:"
        echo "  help                      Show this help message"
        echo ""
        echo "Examples:"
        echo "  $0 restore-db-fresh /home/ubuntu/backups/openmrsdb_backup.sql.gz"
        echo "  $0 setup-ssl jss-bahmni-prerelease.avniproject.org admin@avniproject.org"
        echo ""
        echo "IMPORTANT: Use MySQL 5.6 for JSS data (contains 1942-1945 dates that cause"
        echo "           HOUR_OF_DAY errors with MySQL 5.7+ due to India DST period)"
        ;;
    *)
        # Run main function for setup
        main "$@"
        ;;
esac
