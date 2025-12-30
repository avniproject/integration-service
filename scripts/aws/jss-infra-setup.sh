#!/bin/bash
# JSS Avni-Bahmni Integration - AWS Infrastructure Setup Script
# This script creates complete infrastructure: VPC, subnets, security groups, RDS, EC2, and Route53
# Reusable for both prerelease and production environments

set -e

# ============================================================================
# CONFIGURATION
# ============================================================================
AWS_REGION="ap-south-1"
PROJECT="jss-avni-bahmni"
HOSTED_ZONE_ID="Z1BN518G8E6H9U"  # avniproject.org
KEY_PAIR_NAME="openchs-infra"

# Prerelease VPC (existing)
PRERELEASE_VPC_ID="vpc-0132b5c63278b2c52"
PRERELEASE_SUBNET_A="subnet-016c5045517fb38f9"
PRERELEASE_SUBNET_B="subnet-094989bce9a2c6955"
PRERELEASE_SUBNET_C="subnet-0b4d26175373ac1f8"
PRERELEASE_SG="sg-022fe3f581de4d6f4"  # prerelease-sg (SSH, HTTP, HTTPS)

# AMI for Ubuntu 22.04 ARM64 (Graviton)
UBUNTU_ARM64_AMI="ami-00c6cc6ebf54a632b"  # ubuntu-jammy-22.04-arm64

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# ============================================================================
# SECURITY GROUP FUNCTIONS
# ============================================================================
create_jss_security_group() {
    local ENV_NAME=$1
    local VPC_ID=$2
    
    log_info "Creating JSS Bahmni security group for ${ENV_NAME}..."
    
    # Create Security Group for JSS Bahmni EC2
    EC2_SG_ID=$(aws ec2 create-security-group \
        --group-name "${PROJECT}-${ENV_NAME}-bahmni-sg" \
        --description "Security group for JSS ${ENV_NAME} Bahmni Docker" \
        --vpc-id $VPC_ID \
        --tag-specifications "ResourceType=security-group,Tags=[{Key=Name,Value=${PROJECT}-${ENV_NAME}-bahmni-sg},{Key=Project,Value=${PROJECT}},{Key=Environment,Value=${ENV_NAME}}]" \
        --region $AWS_REGION \
        --query 'GroupId' \
        --output text)
    
    # Allow SSH (port 22)
    aws ec2 authorize-security-group-ingress \
        --group-id $EC2_SG_ID \
        --protocol tcp \
        --port 22 \
        --cidr 0.0.0.0/0 \
        --region $AWS_REGION
    
    # Allow HTTP (port 80)
    aws ec2 authorize-security-group-ingress \
        --group-id $EC2_SG_ID \
        --protocol tcp \
        --port 80 \
        --cidr 0.0.0.0/0 \
        --region $AWS_REGION
    
    # Allow HTTPS (port 443)
    aws ec2 authorize-security-group-ingress \
        --group-id $EC2_SG_ID \
        --protocol tcp \
        --port 443 \
        --cidr 0.0.0.0/0 \
        --region $AWS_REGION
    
    # Allow Bahmni HTTP (port 8080)
    aws ec2 authorize-security-group-ingress \
        --group-id $EC2_SG_ID \
        --protocol tcp \
        --port 8080 \
        --cidr 0.0.0.0/0 \
        --region $AWS_REGION
    
    # Allow Bahmni HTTPS (port 8443)
    aws ec2 authorize-security-group-ingress \
        --group-id $EC2_SG_ID \
        --protocol tcp \
        --port 8443 \
        --cidr 0.0.0.0/0 \
        --region $AWS_REGION
    
    log_info "Created EC2 Security Group: $EC2_SG_ID"
    echo $EC2_SG_ID
}

create_mysql_security_group() {
    local ENV_NAME=$1
    local VPC_ID=$2
    local VPC_CIDR=$3
    
    log_info "Creating MySQL RDS security group for ${ENV_NAME}..."
    
    RDS_SG_ID=$(aws ec2 create-security-group \
        --group-name "${PROJECT}-${ENV_NAME}-mysql-sg" \
        --description "Security group for JSS ${ENV_NAME} MySQL RDS" \
        --vpc-id $VPC_ID \
        --tag-specifications "ResourceType=security-group,Tags=[{Key=Name,Value=${PROJECT}-${ENV_NAME}-mysql-sg},{Key=Project,Value=${PROJECT}},{Key=Environment,Value=${ENV_NAME}}]" \
        --region $AWS_REGION \
        --query 'GroupId' \
        --output text)
    
    # Allow MySQL access from within VPC
    aws ec2 authorize-security-group-ingress \
        --group-id $RDS_SG_ID \
        --protocol tcp \
        --port 3306 \
        --cidr $VPC_CIDR \
        --region $AWS_REGION
    
    log_info "Created RDS Security Group: $RDS_SG_ID"
    echo $RDS_SG_ID
}

# ============================================================================
# DB SUBNET GROUP FUNCTIONS
# ============================================================================
create_db_subnet_group() {
    local ENV_NAME=$1
    local SUBNET_IDS=$2  # Space-separated subnet IDs
    
    log_info "Creating DB subnet group for ${ENV_NAME}..."
    
    aws rds create-db-subnet-group \
        --db-subnet-group-name "${PROJECT}-${ENV_NAME}-db-subnet-group" \
        --db-subnet-group-description "DB subnet group for JSS ${ENV_NAME} in prerelease VPC" \
        --subnet-ids $SUBNET_IDS \
        --tags Key=Project,Value=${PROJECT} Key=Environment,Value=${ENV_NAME} \
        --region $AWS_REGION
    
    log_info "Created DB Subnet Group: ${PROJECT}-${ENV_NAME}-db-subnet-group"
}

# ============================================================================
# RDS FUNCTIONS
# ============================================================================
create_rds_instance() {
    local ENV_NAME=$1
    local DB_INSTANCE_ID=$2
    local DB_PASSWORD=$3
    local RDS_SG_ID=$4
    local DB_SUBNET_GROUP=$5
    local INSTANCE_CLASS=${6:-db.t3.small}  # MySQL 5.7 requires t3, not t4g
    
    log_info "Creating RDS instance ${DB_INSTANCE_ID} (${INSTANCE_CLASS})..."
    
    aws rds create-db-instance \
        --db-instance-identifier $DB_INSTANCE_ID \
        --db-instance-class $INSTANCE_CLASS \
        --engine mysql \
        --engine-version "5.7.44-rds.20250818" \
        --master-username openmrs_admin \
        --master-user-password "$DB_PASSWORD" \
        --allocated-storage 40 \
        --storage-type gp3 \
        --vpc-security-group-ids $RDS_SG_ID \
        --db-subnet-group-name $DB_SUBNET_GROUP \
        --backup-retention-period 7 \
        --no-multi-az \
        --no-publicly-accessible \
        --region $AWS_REGION \
        --tags Key=Project,Value=${PROJECT} Key=Environment,Value=${ENV_NAME}
    
    log_info "RDS instance $DB_INSTANCE_ID creation initiated (takes ~5-10 minutes)"
}

wait_for_rds() {
    local DB_INSTANCE_ID=$1
    
    log_info "Waiting for RDS instance $DB_INSTANCE_ID to be available..."
    aws rds wait db-instance-available --db-instance-identifier $DB_INSTANCE_ID --region $AWS_REGION
    
    RDS_ENDPOINT=$(aws rds describe-db-instances \
        --db-instance-identifier $DB_INSTANCE_ID \
        --region $AWS_REGION \
        --query 'DBInstances[0].Endpoint.Address' \
        --output text)
    
    log_info "RDS Endpoint: $RDS_ENDPOINT"
    echo $RDS_ENDPOINT
}

get_rds_endpoint() {
    local DB_INSTANCE_ID=$1
    aws rds describe-db-instances \
        --db-instance-identifier $DB_INSTANCE_ID \
        --region $AWS_REGION \
        --query 'DBInstances[0].Endpoint.Address' \
        --output text 2>/dev/null || echo "not-found"
}

# ============================================================================
# EC2 FUNCTIONS
# ============================================================================
create_ec2_instance() {
    local ENV_NAME=$1
    local INSTANCE_NAME=$2
    local SUBNET_ID=$3
    local SECURITY_GROUP_IDS=$4  # Comma-separated
    local INSTANCE_TYPE=${5:-t4g.small}
    local VOLUME_SIZE=${6:-30}
    
    log_info "Creating EC2 instance ${INSTANCE_NAME}..."
    
    # Create user data file
    local USER_DATA_FILE=$(mktemp)
    cat > "$USER_DATA_FILE" << 'USERDATA'
#!/bin/bash
set -e
apt-get update
apt-get upgrade -y
apt-get install -y apt-transport-https ca-certificates curl software-properties-common
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
echo "deb [arch=arm64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu jammy stable" | tee /etc/apt/sources.list.d/docker.list
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
usermod -aG docker ubuntu
systemctl enable docker
systemctl start docker
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-Linux-aarch64" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
apt-get install -y git htop vim mysql-client jq unzip
mkdir -p /home/ubuntu/bahmni-docker
chown ubuntu:ubuntu /home/ubuntu/bahmni-docker
echo "Setup complete" > /home/ubuntu/setup-complete.txt
USERDATA
    
    INSTANCE_ID=$(aws ec2 run-instances \
        --image-id $UBUNTU_ARM64_AMI \
        --instance-type $INSTANCE_TYPE \
        --key-name $KEY_PAIR_NAME \
        --subnet-id $SUBNET_ID \
        --security-group-ids $(echo $SECURITY_GROUP_IDS | tr ',' ' ') \
        --block-device-mappings "[{\"DeviceName\":\"/dev/sda1\",\"Ebs\":{\"VolumeSize\":${VOLUME_SIZE},\"VolumeType\":\"gp3\",\"DeleteOnTermination\":true}}]" \
        --user-data file://"$USER_DATA_FILE" \
        --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=${INSTANCE_NAME}},{Key=Project,Value=${PROJECT}},{Key=Environment,Value=${ENV_NAME}}]" \
        --region $AWS_REGION \
        --query 'Instances[0].InstanceId' \
        --output text)
    
    rm -f "$USER_DATA_FILE"
    
    log_info "Created EC2 instance: $INSTANCE_ID"
    log_info "Waiting for instance to be running..."
    
    aws ec2 wait instance-running --instance-ids $INSTANCE_ID --region $AWS_REGION
    
    # Get public IP
    PUBLIC_IP=$(aws ec2 describe-instances \
        --instance-ids $INSTANCE_ID \
        --query 'Reservations[0].Instances[0].PublicIpAddress' \
        --output text \
        --region $AWS_REGION)
    
    PRIVATE_IP=$(aws ec2 describe-instances \
        --instance-ids $INSTANCE_ID \
        --query 'Reservations[0].Instances[0].PrivateIpAddress' \
        --output text \
        --region $AWS_REGION)
    
    log_info "Instance $INSTANCE_ID is running"
    log_info "  Public IP:  $PUBLIC_IP"
    log_info "  Private IP: $PRIVATE_IP"
    
    echo "$INSTANCE_ID $PUBLIC_IP $PRIVATE_IP"
}

# ============================================================================
# ROUTE53 FUNCTIONS
# ============================================================================
create_dns_record() {
    local RECORD_NAME=$1
    local RECORD_VALUE=$2
    local RECORD_TYPE=${3:-A}
    local TTL=${4:-300}
    
    log_info "Creating DNS record: ${RECORD_NAME} -> ${RECORD_VALUE}"
    
    # Create change batch JSON
    CHANGE_BATCH=$(cat <<EOF
{
    "Changes": [
        {
            "Action": "UPSERT",
            "ResourceRecordSet": {
                "Name": "${RECORD_NAME}",
                "Type": "${RECORD_TYPE}",
                "TTL": ${TTL},
                "ResourceRecords": [
                    {
                        "Value": "${RECORD_VALUE}"
                    }
                ]
            }
        }
    ]
}
EOF
)
    
    aws route53 change-resource-record-sets \
        --hosted-zone-id $HOSTED_ZONE_ID \
        --change-batch "$CHANGE_BATCH"
    
    log_info "DNS record created/updated: ${RECORD_NAME}"
}

delete_dns_record() {
    local RECORD_NAME=$1
    local RECORD_VALUE=$2
    local RECORD_TYPE=${3:-A}
    local TTL=${4:-300}
    
    log_info "Deleting DNS record: ${RECORD_NAME}"
    
    CHANGE_BATCH=$(cat <<EOF
{
    "Changes": [
        {
            "Action": "DELETE",
            "ResourceRecordSet": {
                "Name": "${RECORD_NAME}",
                "Type": "${RECORD_TYPE}",
                "TTL": ${TTL},
                "ResourceRecords": [
                    {
                        "Value": "${RECORD_VALUE}"
                    }
                ]
            }
        }
    ]
}
EOF
)
    
    aws route53 change-resource-record-sets \
        --hosted-zone-id $HOSTED_ZONE_ID \
        --change-batch "$CHANGE_BATCH" 2>/dev/null || log_warn "DNS record not found or already deleted"
}

# ============================================================================
# FULL ENVIRONMENT SETUP (Using existing prerelease VPC)
# ============================================================================
setup_jss_prerelease() {
    local DB_PASSWORD=$1
    
    if [ -z "$DB_PASSWORD" ]; then
        log_error "Usage: $0 setup-prerelease <db-password>"
        exit 1
    fi
    
    log_info "Setting up JSS Bahmni prerelease environment in existing prerelease VPC..."
    
    # Use existing prerelease VPC infrastructure
    local VPC_ID=$PRERELEASE_VPC_ID
    local VPC_CIDR="172.1.0.0/16"
    
    # Step 1: Create MySQL security group (if not exists)
    log_info "Step 1: Creating MySQL security group..."
    MYSQL_SG=$(aws ec2 describe-security-groups \
        --filters "Name=group-name,Values=${PROJECT}-prerelease-mysql-sg" "Name=vpc-id,Values=${VPC_ID}" \
        --query 'SecurityGroups[0].GroupId' --output text --region $AWS_REGION 2>/dev/null)
    
    if [ "$MYSQL_SG" == "None" ] || [ -z "$MYSQL_SG" ]; then
        MYSQL_SG=$(create_mysql_security_group "prerelease" $VPC_ID $VPC_CIDR)
    else
        log_info "MySQL security group already exists: $MYSQL_SG"
    fi
    
    # Step 2: Create Bahmni EC2 security group (if not exists)
    log_info "Step 2: Creating Bahmni EC2 security group..."
    BAHMNI_SG=$(aws ec2 describe-security-groups \
        --filters "Name=group-name,Values=${PROJECT}-prerelease-bahmni-sg" "Name=vpc-id,Values=${VPC_ID}" \
        --query 'SecurityGroups[0].GroupId' --output text --region $AWS_REGION 2>/dev/null)
    
    if [ "$BAHMNI_SG" == "None" ] || [ -z "$BAHMNI_SG" ]; then
        BAHMNI_SG=$(create_jss_security_group "prerelease" $VPC_ID)
    else
        log_info "Bahmni security group already exists: $BAHMNI_SG"
    fi
    
    # Step 3: Create DB subnet group (if not exists)
    log_info "Step 3: Creating DB subnet group..."
    DB_SUBNET_GROUP="${PROJECT}-prerelease-db-subnet-group"
    if ! aws rds describe-db-subnet-groups --db-subnet-group-name $DB_SUBNET_GROUP --region $AWS_REGION > /dev/null 2>&1; then
        create_db_subnet_group "prerelease" "$PRERELEASE_SUBNET_A $PRERELEASE_SUBNET_B $PRERELEASE_SUBNET_C"
    else
        log_info "DB subnet group already exists: $DB_SUBNET_GROUP"
    fi
    
    # Step 4: Create RDS instance (if not exists)
    log_info "Step 4: Creating RDS instance..."
    RDS_INSTANCE_ID="jss-prerelease-mysql"
    RDS_STATUS=$(aws rds describe-db-instances --db-instance-identifier $RDS_INSTANCE_ID \
        --query 'DBInstances[0].DBInstanceStatus' --output text --region $AWS_REGION 2>/dev/null || echo "not-found")
    
    if [ "$RDS_STATUS" == "not-found" ]; then
        create_rds_instance "prerelease" $RDS_INSTANCE_ID "$DB_PASSWORD" $MYSQL_SG $DB_SUBNET_GROUP
        log_info "RDS instance creation initiated. Run '$0 wait-rds $RDS_INSTANCE_ID' to wait for completion."
    else
        log_info "RDS instance already exists (status: $RDS_STATUS)"
    fi
    
    # Step 5: Create EC2 instance
    log_info "Step 5: Creating EC2 instance for Bahmni Docker..."
    EC2_NAME="${PROJECT}-prerelease-bahmni"
    EXISTING_EC2=$(aws ec2 describe-instances \
        --filters "Name=tag:Name,Values=${EC2_NAME}" "Name=instance-state-name,Values=running,pending,stopped" \
        --query 'Reservations[0].Instances[0].InstanceId' --output text --region $AWS_REGION 2>/dev/null)
    
    if [ "$EXISTING_EC2" == "None" ] || [ -z "$EXISTING_EC2" ]; then
        # Use both prerelease-sg and bahmni-sg
        EC2_RESULT=$(create_ec2_instance "prerelease" $EC2_NAME $PRERELEASE_SUBNET_B "${PRERELEASE_SG},${BAHMNI_SG}" "t4g.small" 30)
        EC2_ID=$(echo $EC2_RESULT | awk '{print $1}')
        PUBLIC_IP=$(echo $EC2_RESULT | awk '{print $2}')
        
        # Validate PUBLIC_IP before creating DNS
        if [ -n "$PUBLIC_IP" ] && [[ "$PUBLIC_IP" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
            # Step 6: Create DNS record
            log_info "Step 6: Creating DNS record..."
            create_dns_record "jss-bahmni-prerelease.avniproject.org" "$PUBLIC_IP" "A" 300
            log_info "DNS record for EC2 created. RDS CNAME will be created after RDS is available."
        else
            log_warn "Could not get valid public IP. Create DNS manually: ./jss-infra-setup.sh dns jss-bahmni-prerelease.avniproject.org <public-ip>"
        fi
    else
        log_info "EC2 instance already exists: $EXISTING_EC2"
        PUBLIC_IP=$(aws ec2 describe-instances --instance-ids $EXISTING_EC2 \
            --query 'Reservations[0].Instances[0].PublicIpAddress' --output text --region $AWS_REGION)
    fi
    
    # Summary
    echo ""
    echo "=========================================="
    echo "  JSS Prerelease Environment Summary"
    echo "=========================================="
    echo "VPC:                 $VPC_ID (prerelease VPC)"
    echo "MySQL Security Group: $MYSQL_SG"
    echo "Bahmni Security Group: $BAHMNI_SG"
    echo "DB Subnet Group:     $DB_SUBNET_GROUP"
    echo "RDS Instance:        $RDS_INSTANCE_ID"
    echo "EC2 Instance:        ${EC2_ID:-$EXISTING_EC2}"
    echo "EC2 Public IP:       ${PUBLIC_IP:-N/A}"
    echo ""
    echo "DNS Records:"
    echo "  jss-bahmni-prerelease.avniproject.org -> ${PUBLIC_IP:-TBD}"
    echo "  jss-db-prerelease.avniproject.org     -> (RDS endpoint, create after RDS available)"
    echo ""
    echo "Next steps:"
    echo "  1. Wait for RDS: $0 wait-rds $RDS_INSTANCE_ID"
    echo "  2. Create RDS DNS: $0 dns-rds-prerelease"
    echo "  3. SSH to EC2: ssh -i ~/.ssh/openchs-infra.pem ubuntu@${PUBLIC_IP:-<public-ip>}"
    echo "  4. Restore backup: ./jss-restore-backup.sh restore <rds-endpoint> openmrs_admin '$DB_PASSWORD'"
    echo "=========================================="
}

setup_jss_prod() {
    local DB_PASSWORD=$1
    
    if [ -z "$DB_PASSWORD" ]; then
        log_error "Usage: $0 setup-prod <db-password>"
        exit 1
    fi
    
    log_info "Setting up JSS Bahmni production environment..."
    log_warn "Production setup uses prod VPC (vpc-2300a74b). Ensure this is correct."
    
    # Production VPC details (to be configured)
    local PROD_VPC_ID="vpc-2300a74b"
    local PROD_VPC_CIDR="10.100.0.0/16"
    
    # Similar steps as prerelease but for prod
    log_error "Production setup not yet implemented. Please configure PROD_* variables first."
    exit 1
}

# ============================================================================
# MAIN EXECUTION
# ============================================================================
main() {
    echo "=========================================="
    echo "  JSS Avni-Bahmni Infrastructure Setup"
    echo "=========================================="
    echo ""
    
    # Check AWS CLI configuration
    if ! aws sts get-caller-identity > /dev/null 2>&1; then
        log_error "AWS CLI not configured. Please run 'aws configure' first."
        exit 1
    fi
    
    log_info "AWS CLI configured. Account: $(aws sts get-caller-identity --query 'Account' --output text)"
    
    case "$1" in
        # ============ FULL ENVIRONMENT SETUP ============
        "setup-prerelease")
            setup_jss_prerelease "$2"
            ;;
        "setup-prod")
            setup_jss_prod "$2"
            ;;
        
        # ============ INDIVIDUAL COMPONENTS ============
        "sg-mysql")
            if [ -z "$2" ] || [ -z "$3" ]; then
                log_error "Usage: $0 sg-mysql <env-name> <vpc-id> [vpc-cidr]"
                exit 1
            fi
            create_mysql_security_group "$2" "$3" "${4:-172.1.0.0/16}"
            ;;
        "sg-bahmni")
            if [ -z "$2" ] || [ -z "$3" ]; then
                log_error "Usage: $0 sg-bahmni <env-name> <vpc-id>"
                exit 1
            fi
            create_jss_security_group "$2" "$3"
            ;;
        "db-subnet-group")
            if [ -z "$2" ] || [ -z "$3" ]; then
                log_error "Usage: $0 db-subnet-group <env-name> <subnet-ids-space-separated>"
                exit 1
            fi
            create_db_subnet_group "$2" "$3"
            ;;
        "rds")
            if [ -z "$2" ] || [ -z "$3" ] || [ -z "$4" ] || [ -z "$5" ]; then
                log_error "Usage: $0 rds <env-name> <password> <rds-sg-id> <db-subnet-group> [instance-class]"
                exit 1
            fi
            create_rds_instance "$2" "${PROJECT}-$2-mysql" "$3" "$4" "$5" "${6:-db.t3.small}"
            ;;
        "ec2")
            if [ -z "$2" ] || [ -z "$3" ] || [ -z "$4" ]; then
                log_error "Usage: $0 ec2 <env-name> <subnet-id> <security-group-ids> [instance-type] [volume-size]"
                exit 1
            fi
            create_ec2_instance "$2" "${PROJECT}-$2-bahmni" "$3" "$4" "${5:-t4g.small}" "${6:-30}"
            ;;
        "dns")
            if [ -z "$2" ] || [ -z "$3" ]; then
                log_error "Usage: $0 dns <record-name> <record-value> [record-type] [ttl]"
                exit 1
            fi
            create_dns_record "$2" "$3" "${4:-A}" "${5:-300}"
            ;;
        "dns-rds-prerelease")
            RDS_ENDPOINT=$(get_rds_endpoint "jss-prerelease-mysql")
            if [ "$RDS_ENDPOINT" == "not-found" ] || [ -z "$RDS_ENDPOINT" ]; then
                log_error "RDS instance not found or not available yet"
                exit 1
            fi
            create_dns_record "jss-db-prerelease.avniproject.org" "$RDS_ENDPOINT" "CNAME" 300
            ;;
        
        # ============ UTILITY COMMANDS ============
        "wait-rds")
            if [ -z "$2" ]; then
                log_error "Usage: $0 wait-rds <db-instance-id>"
                exit 1
            fi
            wait_for_rds "$2"
            ;;
        "status")
            log_info "Checking JSS infrastructure status..."
            echo ""
            echo "=== Security Groups ==="
            aws ec2 describe-security-groups \
                --filters "Name=tag:Project,Values=${PROJECT}" \
                --query 'SecurityGroups[*].[GroupId,GroupName,VpcId]' \
                --output table --region $AWS_REGION 2>/dev/null || echo "None found"
            echo ""
            echo "=== DB Subnet Groups ==="
            aws rds describe-db-subnet-groups \
                --query "DBSubnetGroups[?contains(DBSubnetGroupName, 'jss')].[DBSubnetGroupName,VpcId]" \
                --output table --region $AWS_REGION 2>/dev/null || echo "None found"
            echo ""
            echo "=== RDS Instances ==="
            aws rds describe-db-instances \
                --query "DBInstances[?contains(DBInstanceIdentifier, 'jss')].[DBInstanceIdentifier,DBInstanceStatus,DBInstanceClass,Endpoint.Address]" \
                --output table --region $AWS_REGION 2>/dev/null || echo "None found"
            echo ""
            echo "=== EC2 Instances ==="
            aws ec2 describe-instances \
                --filters "Name=tag:Project,Values=${PROJECT}" "Name=instance-state-name,Values=running,pending,stopped" \
                --query 'Reservations[*].Instances[*].[InstanceId,InstanceType,Tags[?Key==`Name`].Value|[0],State.Name,PublicIpAddress]' \
                --output table --region $AWS_REGION 2>/dev/null || echo "None found"
            echo ""
            echo "=== DNS Records (jss*) ==="
            aws route53 list-resource-record-sets \
                --hosted-zone-id $HOSTED_ZONE_ID \
                --query "ResourceRecordSets[?contains(Name, 'jss')].[Name,Type,ResourceRecords[0].Value]" \
                --output table 2>/dev/null || echo "None found"
            ;;
        "cleanup-prerelease")
            log_warn "This will delete JSS prerelease infrastructure!"
            read -p "Are you sure? (yes/no): " confirm
            if [ "$confirm" == "yes" ]; then
                # Delete EC2
                EC2_ID=$(aws ec2 describe-instances \
                    --filters "Name=tag:Name,Values=${PROJECT}-prerelease-bahmni" "Name=instance-state-name,Values=running,pending,stopped" \
                    --query 'Reservations[0].Instances[0].InstanceId' --output text --region $AWS_REGION 2>/dev/null)
                if [ "$EC2_ID" != "None" ] && [ -n "$EC2_ID" ]; then
                    log_info "Terminating EC2 instance: $EC2_ID"
                    aws ec2 terminate-instances --instance-ids $EC2_ID --region $AWS_REGION
                fi
                
                # Delete RDS
                log_info "Deleting RDS instance..."
                aws rds delete-db-instance --db-instance-identifier jss-prerelease-mysql --skip-final-snapshot --region $AWS_REGION 2>/dev/null || true
                
                # Delete DNS records
                log_info "Deleting DNS records..."
                PUBLIC_IP=$(aws ec2 describe-instances --instance-ids $EC2_ID \
                    --query 'Reservations[0].Instances[0].PublicIpAddress' --output text --region $AWS_REGION 2>/dev/null || echo "")
                if [ -n "$PUBLIC_IP" ] && [ "$PUBLIC_IP" != "None" ]; then
                    delete_dns_record "jss-bahmni-prerelease.avniproject.org" "$PUBLIC_IP" "A" 300
                fi
                
                RDS_ENDPOINT=$(get_rds_endpoint "jss-prerelease-mysql")
                if [ "$RDS_ENDPOINT" != "not-found" ] && [ -n "$RDS_ENDPOINT" ]; then
                    delete_dns_record "jss-db-prerelease.avniproject.org" "$RDS_ENDPOINT" "CNAME" 300
                fi
                
                log_info "Cleanup initiated. Security groups and DB subnet groups preserved for reuse."
            fi
            ;;
        *)
            echo "Usage: $0 <command> [options]"
            echo ""
            echo "=== Full Environment Setup ==="
            echo "  setup-prerelease <password>  Setup complete JSS prerelease environment"
            echo "  setup-prod <password>        Setup complete JSS production environment"
            echo ""
            echo "=== Individual Components ==="
            echo "  sg-mysql <env> <vpc-id>      Create MySQL RDS security group"
            echo "  sg-bahmni <env> <vpc-id>     Create Bahmni EC2 security group"
            echo "  db-subnet-group <env> <ids>  Create DB subnet group"
            echo "  rds <env> <pass> <sg> <grp>  Create RDS MySQL instance"
            echo "  ec2 <env> <subnet> <sg>      Create EC2 instance with Docker"
            echo "  dns <name> <value> [type]    Create/update DNS record"
            echo "  dns-rds-prerelease           Create DNS CNAME for prerelease RDS"
            echo ""
            echo "=== Utility Commands ==="
            echo "  wait-rds <instance-id>       Wait for RDS to be available"
            echo "  status                       Show all JSS infrastructure"
            echo "  cleanup-prerelease           Delete prerelease infrastructure"
            echo ""
            echo "=== Example: Full Prerelease Setup ==="
            echo "  $0 setup-prerelease 'YourSecurePassword123'"
            echo "  $0 wait-rds jss-prerelease-mysql"
            echo "  $0 dns-rds-prerelease"
            echo "  ./jss-restore-backup.sh restore <rds-endpoint> openmrs_admin 'YourPassword'"
            ;;
    esac
}

main "$@"
