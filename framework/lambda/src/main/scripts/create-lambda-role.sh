#!/bin/bash

# Create IAM role for Lambda
echo "Creating Lambda execution role..."

# Create the trust policy
cat > trust-policy.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF

# Create the role
aws iam create-role \
  --role-name chinook-lambda-role \
  --assume-role-policy-document file://trust-policy.json

# Attach basic Lambda execution policy
aws iam attach-role-policy \
  --role-name chinook-lambda-role \
  --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

# For VPC access (if using RDS)
aws iam attach-role-policy \
  --role-name chinook-lambda-role \
  --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole

# For EFS access (if using file-based DB)
aws iam attach-role-policy \
  --role-name chinook-lambda-role \
  --policy-arn arn:aws:iam::aws:policy/AmazonElasticFileSystemClientReadWriteAccess

echo "Role created! ARN:"
aws iam get-role --role-name chinook-lambda-role --query 'Role.Arn' --output text

# Clean up
rm trust-policy.json