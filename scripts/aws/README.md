# JSS Avni-Bahmni Integration - AWS Infrastructure & Bahmni Setup

## Current Working Setup

| Component | Value |
|-----------|-------|
| URL | https://jss-bahmni-prerelease.avniproject.org/bahmni/home/index.html#/dashboard |
| Instance | i-0e128ab9da4c8d30f (t3.large, 7.6 GiB RAM + 8 GiB swap) |
| MySQL | 5.6 (local Docker container) |
| OpenMRS | 1.0.0-644 |
| SSL | Let's Encrypt HTTPS |
| Config | jss-config from GitHub |

## Prerequisites

1. **AWS CLI** configured: `aws configure`
2. **SSH Key**: `~/.ssh/openchs-infra.pem`
3. **Make**: For running Makefile commands

## Quick Start - Using Makefile

```bash
# View all available commands
make help

# SSH to prerelease instance
make ssh-prerelease

# Check Bahmni container status
make bahmni-status

# View OpenMRS logs
make bahmni-logs-openmrs

# Start/Stop Bahmni
make bahmni-up
make bahmni-down
```

## Full Setup Workflow

### Step 1: Create EC2 Instance
```bash
./jss-infra-setup.sh ec2 jss-avni-bahmni-prerelease subnet-xxx sg-xxx t3.large 30
```

### Step 2: SSH and Configure Instance
```bash
make ssh-prerelease

# On EC2: Configure swap (required for t3.large)
sudo fallocate -l 8G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
```

### Step 3: Setup Bahmni Docker
```bash
# Clone repos
cd /home/ubuntu
git clone https://github.com/JanSwasthyaSahyog/bahmni-docker.git
git clone https://github.com/JanSwasthyaSahyog/jss-config.git
cd bahmni-docker

# Copy config files from this repo
scp -i ~/.ssh/openchs-infra.pem scripts/aws/bahmni/.env.prerelease ubuntu@<ip>:/home/ubuntu/bahmni-docker/.env
scp -i ~/.ssh/openchs-infra.pem scripts/aws/bahmni/docker-compose.override.yml ubuntu@<ip>:/home/ubuntu/bahmni-docker/
```

### Step 4: Create Volume and Restore Database
```bash
# Create external volume
docker volume create openmrs_db_data

# Start MySQL
docker compose -f docker-compose.yml -f docker-compose.override.yml up -d openmrsdb
sleep 30

# Restore backup
gunzip -c /path/to/backup.sql.gz | docker exec -i bahmni-docker-openmrsdb-1 mysql -u root -pOpenMRS_JSS2024 openmrs
```

### Step 5: Start All Services
```bash
docker compose -f docker-compose.yml -f docker-compose.override.yml --profile emr up -d
```

### Step 6: Wait for OpenMRS (10-15 minutes)
```bash
# Monitor startup
docker logs bahmni-docker-openmrs-1 -f | grep -E "Server startup"

# Or use wait loop
while ! curl -s http://localhost:8080/openmrs/ws/rest/v1/session -u admin:test > /dev/null; do
  echo "Waiting..."; sleep 30
done
echo "OpenMRS ready!"
```

### Step 7: Create AMI Snapshot
```bash
aws ec2 create-image --instance-id i-0e128ab9da4c8d30f \
  --name "bahmni-prerelease-$(date +%Y%m%d-%H%M%S)" \
  --description "Bahmni prerelease with restored database" --no-reboot
```

## Configuration Files

### .env.prerelease

Key settings:
- `OPENMRS_IMAGE_TAG=1.0.0-644` - OpenMRS image version
- `OPENMRS_DB_IMAGE_NAME=mysql:5.6` - MySQL 5.6 (required for historical dates)
- `OMRS_DB_EXTRA_ARGS=&zeroDateTimeBehavior=convertToNull` - Handle zero dates
- `CONFIG_VOLUME=/home/ubuntu/bahmni-docker/jss-config` - Path to jss-config

### docker-compose.override.yml

- MySQL 5.6 container (x86_64/amd64 platform)
- External volume `openmrs_db_data` for database persistence
- JSON-file logging (no Loki plugin required)
- Health checks for MySQL readiness

## SSL Certificate Setup

```bash
# Install certbot
sudo apt update && sudo apt install -y certbot

# Generate certificate
sudo certbot certonly --standalone -d jss-bahmni-prerelease.avniproject.org

# Copy certs to Bahmni location
sudo mkdir -p /bahmni/certs
sudo cp /etc/letsencrypt/live/jss-bahmni-prerelease.avniproject.org/fullchain.pem /bahmni/certs/cert.pem
sudo cp /etc/letsencrypt/live/jss-bahmni-prerelease.avniproject.org/privkey.pem /bahmni/certs/key.pem

# Restart proxy to load certs
docker compose restart proxy
```

## AMI Snapshot Creation

```bash
# Create snapshot before major changes
aws ec2 create-image --instance-id i-0e128ab9da4c8d30f \
  --name "bahmni-prerelease-$(date +%Y%m%d-%H%M%S)" \
  --description "Bahmni prerelease snapshot" --no-reboot

# List existing snapshots
aws ec2 describe-images --owners self --query 'Images[?starts_with(Name, `bahmni-prerelease`)].[ImageId,Name,CreationDate]' --output table
```

## Database Restore

```bash
# From EC2 instance
cd /home/ubuntu/bahmni-docker

# Option 1: Restore from local backup file
docker compose -f docker-compose.yml -f docker-compose.override.yml up -d openmrsdb
sleep 30  # Wait for MySQL to be ready
docker exec bahmni-docker-openmrsdb-1 mysql -u root -pOpenMRS_JSS2024 openmrs < /path/to/backup.sql

# Option 2: Restore from gzip compressed backup
gunzip -c /path/to/backup.sql.gz | docker exec -i bahmni-docker-openmrsdb-1 mysql -u root -pOpenMRS_JSS2024 openmrs

# Verify restore
docker exec bahmni-docker-openmrsdb-1 mysql -u root -pOpenMRS_JSS2024 openmrs -e "SELECT COUNT(*) as tables FROM information_schema.tables WHERE table_schema='openmrs';"
```

## Infrastructure Details

### Current Prerelease Environment

| Resource | Value |
|----------|-------|
| Instance ID | i-0e128ab9da4c8d30f |
| Instance Type | t3.large (7.6 GiB RAM) |
| OS | Ubuntu 22.04 LTS (x86_64) |
| Volume | 100GB gp3 |
| Swap | 8GB (configured) |
| MySQL | 5.6 (Docker container) |
| OpenMRS | 1.0.0-644 |
| Bahmni Web | 1.1.0-696 |
| DNS | jss-bahmni-prerelease.avniproject.org |
| SSL | Let's Encrypt HTTPS |

### Important Notes

**HOUR_OF_DAY Errors:**
- OpenMRS logs show HOUR_OF_DAY errors during Hibernate Search indexing
- These are **non-fatal** background indexing errors (production has them too)
- OpenMRS becomes fully functional after search index update completes
- Do NOT try to fix by changing timezone or JDBC connector version
- Do NOT disable search indexing in production

**Memory Requirements:**
- Minimum: 8 GiB RAM + 8 GiB swap
- Recommended: 16 GiB RAM
- t3.large (7.6 GiB) works but startup takes 10-15 minutes
- t3.xlarge (16 GiB) recommended for faster startup

**Startup Timeline:**
- MySQL ready: ~30 seconds
- OpenMRS Tomcat startup: ~2-3 minutes
- OpenMRS database initialization: ~2-5 minutes
- Hibernate Search indexing: ~5-10 minutes
- Total: 10-15 minutes (depends on RAM/swap)

## Waiting for Services to Come Up

```bash
# Monitor OpenMRS startup
docker logs bahmni-docker-openmrs-1 -f | grep -E "Server startup|search index|HOUR_OF_DAY"

# Check when OpenMRS is responsive
while ! curl -s http://localhost:8080/openmrs/ws/rest/v1/session -u admin:test > /dev/null; do
  echo "Waiting for OpenMRS..."
  sleep 10
done
echo "OpenMRS is ready!"

# Check all services are running
docker ps --filter "status=running" --format "table {{.Names}}\t{{.Status}}"
```

## Troubleshooting

### OpenMRS not starting
1. Check logs: `docker logs bahmni-docker-openmrs-1 | tail -100`
2. Verify MySQL is healthy: `docker exec bahmni-docker-openmrsdb-1 mysql -u root -pOpenMRS_JSS2024 -e "SELECT 1;"`
3. Check memory: `free -h` (should have at least 2 GiB free)
4. Wait longer - search indexing can take 10-15 minutes

### Bahmni UI shows 404 errors
1. Verify jss-config is mounted: `docker exec bahmni-docker-bahmni-web-1 ls /usr/local/apache2/htdocs/bahmni_config/openmrs/apps/home/`
2. Check CONFIG_VOLUME in .env: `grep CONFIG_VOLUME .env`
3. Restart bahmni-web: `docker compose restart bahmni-web`

### Cannot access HTTPS
1. Check proxy is running: `docker ps | grep proxy`
2. Verify SSL certificates: `ls -la /bahmni/certs/`
3. Check DNS resolution: `nslookup jss-bahmni-prerelease.avniproject.org`

### Database restore fails
1. Ensure MySQL is running: `docker ps | grep openmrsdb`
2. Check backup file: `ls -lh /path/to/backup.sql.gz`
3. Verify credentials in .env match restore command
4. Check available disk space: `df -h`

### High memory usage
1. Increase swap: `sudo fallocate -l 8G /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile`
2. Upgrade instance type: `aws ec2 modify-instance-attribute --instance-id i-xxx --instance-type t3.xlarge`
3. Disable unnecessary services: Remove profiles from docker-compose command

## Makefile Commands

```bash
make help                # Show all available commands

# SSH Access
make ssh-prerelease      # SSH to prerelease EC2

# Bahmni Operations
make bahmni-status       # Show container status
make bahmni-up           # Start all Bahmni services
make bahmni-down         # Stop all Bahmni services
make bahmni-restart      # Restart all services
make bahmni-logs         # View all logs
make bahmni-logs-openmrs # View OpenMRS logs only

# Infrastructure
make status              # Show AWS infrastructure status
make check-ec2-setup     # Check Docker/MySQL on EC2

# Snapshots
make create-ami          # Create AMI snapshot of instance
```
