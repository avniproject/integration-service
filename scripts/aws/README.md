# JSS Avni-Bahmni Integration - AWS Infrastructure Scripts

## Overview

These scripts automate the complete setup of AWS infrastructure for the JSS Avni-Bahmni integration project, including:
- Security Groups (MySQL RDS, Bahmni EC2)
- DB Subnet Groups
- RDS MySQL instances
- EC2 instances with Docker pre-installed
- Route53 DNS records

## Prerequisites

1. **AWS CLI** configured with appropriate credentials
   ```bash
aws configure
# Enter Access Key ID, Secret Access Key, Region (ap-south-1), Output format (json)
```

2. **MySQL Client** for database operations
   ```bash
brew install mysql-client
# Add to PATH: export PATH="/opt/homebrew/opt/mysql-client/bin:$PATH"
```

3. **SSH Key** - `openchs-infra` key pair must exist in AWS
   ```bash
# Verify key exists
aws ec2 describe-key-pairs --key-names openchs-infra --region ap-south-1
```

4. **Required IAM Permissions**
   - EC2: RunInstances, DescribeInstances, CreateSecurityGroup, AuthorizeSecurityGroupIngress
   - RDS: CreateDBInstance, CreateDBSubnetGroup, DescribeDBInstances
   - Route53: ChangeResourceRecordSets, ListResourceRecordSets

## Scripts

### 1. jss-infra-setup.sh

Complete infrastructure setup script with multiple commands.

#### Quick Start - Full Environment Setup

```bash
# Setup complete prerelease environment (security groups, RDS, EC2, DNS)
./jss-infra-setup.sh setup-prerelease "YourSecurePassword123"

# Wait for RDS to be available (~5-10 minutes)
./jss-infra-setup.sh wait-rds jss-prerelease-mysql

# Create DNS record for RDS
./jss-infra-setup.sh dns-rds-prerelease

# Check status of all JSS infrastructure
./jss-infra-setup.sh status
```

#### Individual Component Commands

```bash
# Security Groups
./jss-infra-setup.sh sg-mysql <env-name> <vpc-id> [vpc-cidr]
./jss-infra-setup.sh sg-bahmni <env-name> <vpc-id>

# DB Subnet Group
./jss-infra-setup.sh db-subnet-group <env-name> "<subnet-ids>"

# RDS Instance
./jss-infra-setup.sh rds <env-name> <password> <rds-sg-id> <db-subnet-group> [instance-class]

# EC2 Instance (with Docker pre-installed)
./jss-infra-setup.sh ec2 <env-name> <subnet-id> <security-group-ids> [instance-type] [volume-size]

# DNS Records
./jss-infra-setup.sh dns <record-name> <record-value> [record-type] [ttl]
./jss-infra-setup.sh dns-rds-prerelease
```

#### Utility Commands

```bash
# Check all JSS infrastructure status
./jss-infra-setup.sh status

# Wait for RDS to be available
./jss-infra-setup.sh wait-rds <db-instance-id>

# Cleanup prerelease environment
./jss-infra-setup.sh cleanup-prerelease
```

### 2. jss-restore-backup.sh

Restores OpenMRS database backup to RDS.

```bash
# Verify RDS connection
./jss-restore-backup.sh verify <rds-endpoint> openmrs_admin "YourPassword"

# Restore backup (from default location)
./jss-restore-backup.sh restore <rds-endpoint> openmrs_admin "YourPassword" openmrs

# Restore from custom backup file
BACKUP_FILE=/path/to/backup.sql.gz ./jss-restore-backup.sh restore <rds-endpoint> openmrs_admin "YourPassword"

# Create baseline snapshot after restore
./jss-restore-backup.sh snapshot jss-prerelease-mysql
```

## Infrastructure Details

### Prerelease Environment (Uses existing prerelease VPC)

| Resource | Value |
|----------|-------|
| VPC | vpc-0132b5c63278b2c52 (prerelease VPC) |
| VPC CIDR | 172.1.0.0/16 |
| Subnet A | subnet-016c5045517fb38f9 (ap-south-1a) |
| Subnet B | subnet-094989bce9a2c6955 (ap-south-1b) |
| Subnet C | subnet-0b4d26175373ac1f8 (ap-south-1c) |
| EC2 Instance Type | t4g.small (ARM64/Graviton) |
| EC2 Volume | 30GB gp3 |
| RDS Instance Type | db.t3.small (MySQL 5.7 requires x86) |
| RDS Storage | 40GB gp3 |
| MySQL Version | 5.7.44 |

### DNS Records (avniproject.org)

| Record | Type | Value |
|--------|------|-------|
| jss-bahmni-prerelease.avniproject.org | A | EC2 Public IP |
| jss-db-prerelease.avniproject.org | CNAME | RDS Endpoint |

## Complete Setup Workflow

```bash
# 1. Setup complete prerelease environment
./jss-infra-setup.sh setup-prerelease "OpenMRS_JSS2024"

# 2. Wait for RDS to be available (~5-10 minutes)
./jss-infra-setup.sh wait-rds jss-prerelease-mysql

# 3. Create DNS CNAME for RDS
./jss-infra-setup.sh dns-rds-prerelease

# 4. Restore OpenMRS backup
./jss-restore-backup.sh restore jss-db-prerelease.avniproject.org openmrs_admin "OpenMRS_JSS2024" openmrs

# 5. Create baseline snapshot
./jss-restore-backup.sh snapshot jss-prerelease-mysql

# 6. SSH to EC2 and setup Bahmni Docker
ssh -i ~/.ssh/openchs-infra.pem ubuntu@jss-bahmni-prerelease.avniproject.org

# On EC2: Clone Bahmni Docker repo and configure
git clone https://github.com/JanSwasthyaSahyog/bahmni-docker.git
cd bahmni-docker
# Edit .env to point to RDS endpoint
docker-compose up -d
```

## EC2 Instance Details

The EC2 instance is created with:
- **OS**: Ubuntu 22.04 LTS (ARM64)
- **Pre-installed**: Docker, Docker Compose, MySQL client, git, htop, vim, jq
- **Docker user**: ubuntu (added to docker group)
- **Bahmni directory**: /home/ubuntu/bahmni-docker

## Cleanup

```bash
# Delete prerelease infrastructure (EC2, RDS, DNS records)
./jss-infra-setup.sh cleanup-prerelease

# Security groups and DB subnet groups are preserved for reuse
```

## Troubleshooting

### Cannot connect to RDS
1. Check security group allows inbound MySQL (3306) from VPC CIDR
2. RDS is not publicly accessible - connect from within VPC (EC2)
3. Verify RDS status is "available"

### RDS creation fails
1. MySQL 5.7 requires t3 instance class (not t4g/Graviton)
2. Ensure DB subnet group has subnets in at least 2 AZs
3. Check security group is in the same VPC as DB subnet group

### EC2 creation fails
1. Verify key pair exists: `aws ec2 describe-key-pairs --key-names openchs-infra`
2. Check subnet has available IP addresses
3. Verify AMI ID is valid for the region

### Backup restore fails
1. Ensure MySQL client is installed: `brew install mysql-client`
2. Connect from EC2 (RDS is not publicly accessible)
3. Check backup file exists and is readable

### DNS not resolving
1. Wait a few minutes for DNS propagation
2. Verify hosted zone ID is correct
3. Check record was created: `./jss-infra-setup.sh status`

## Makefile Commands

A Makefile is provided for common operations:

```bash
# Show all available commands
make help

# Infrastructure
make status              # Show all JSS infrastructure
make setup-prerelease    # Full prerelease setup
make cleanup-prerelease  # Delete prerelease resources

# SSH Access
make ssh-prerelease      # SSH to prerelease EC2

# Database Operations
make copy-backup         # Copy backup file to EC2 via scp
make restore-backup      # Restore backup to RDS
make create-snapshot     # Create RDS snapshot
make full-restore        # Copy + restore + snapshot (all-in-one)

# Verification
make check-ec2-setup     # Check Docker/MySQL on EC2
make test-db-connection  # Test RDS connectivity
make verify-restore      # Verify database tables after restore

# Bahmni Docker
make setup-bahmni        # Clone Bahmni Docker repo
make start-bahmni        # Start Bahmni containers
make stop-bahmni         # Stop Bahmni containers
make bahmni-logs         # View Bahmni logs
```
