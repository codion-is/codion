#!/bin/bash

# Deploy Lambda with EFS for H2 file database

# 1. Create VPC (Lambda needs VPC for EFS)
echo "Creating VPC..."
VPC_ID=$(aws ec2 create-vpc --cidr-block 10.0.0.0/16 --query 'Vpc.VpcId' --output text)
aws ec2 modify-vpc-attribute --vpc-id $VPC_ID --enable-dns-hostnames

# Create subnet
SUBNET_ID=$(aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.0.1.0/24 \
  --query 'Subnet.SubnetId' \
  --output text)

# 2. Create EFS filesystem
echo "Creating EFS filesystem..."
EFS_ID=$(aws efs create-file-system \
  --creation-token chinook-efs \
  --query 'FileSystemId' \
  --output text)

# Create mount target
MOUNT_TARGET_ID=$(aws efs create-mount-target \
  --file-system-id $EFS_ID \
  --subnet-id $SUBNET_ID \
  --query 'MountTargetId' \
  --output text)

echo "Waiting for EFS to be available..."
sleep 60

# 3. Create access point
ACCESS_POINT_ID=$(aws efs create-access-point \
  --file-system-id $EFS_ID \
  --posix-user Uid=1000,Gid=1000 \
  --root-directory Path="/chinook",CreationInfo={OwnerUid=1000,OwnerGid=1000,Permissions=755} \
  --query 'AccessPointId' \
  --output text)

# 4. Build Lambda package
echo "Building Lambda deployment package..."
cd ..
./gradlew :chinook-server-aws-lambda:shadowJar
cd deployment

# 5. Create Lambda function with EFS
echo "Creating Lambda function with EFS..."
aws lambda create-function \
  --function-name chinook-entity-server \
  --runtime java21 \
  --role arn:aws:iam::$(aws sts get-caller-identity --query Account --output text):role/chinook-lambda-role \
  --handler is.codion.demos.chinook.lambda.EntityProtocolHandler::handleRequest \
  --zip-file fileb://../build/libs/chinook-lambda.jar \
  --timeout 30 \
  --memory-size 1024 \
  --vpc-config SubnetIds=$SUBNET_ID \
  --file-system-configs "Arn=arn:aws:elasticfilesystem:$(aws configure get region):$(aws sts get-caller-identity --query Account --output text):access-point/$ACCESS_POINT_ID,LocalMountPath=/mnt/efs" \
  --environment Variables="{\
    DATABASE_URL=jdbc:h2:file:/mnt/efs/chinook;MODE=PostgreSQL;AUTO_SERVER=TRUE,\
    DATABASE_USER=scott,\
    DATABASE_PASSWORD=tiger\
  }"

echo "Lambda function created with EFS!"