#!/bin/bash

# Initialize the database schema

echo "Initializing database..."

# For RDS PostgreSQL
if [ "$1" == "rds" ]; then
  echo "Connecting to RDS instance..."
  ENDPOINT=$(aws rds describe-db-instances \
    --db-instance-identifier chinook-db \
    --query 'DBInstances[0].Endpoint.Address' \
    --output text)
  
  # You'll need psql installed
  PGPASSWORD=ChinookPass123! psql \
    -h $ENDPOINT \
    -U chinook_admin \
    -d postgres \
    -f ../../../chinook-domain/src/main/resources/create_schema.sql
    
  echo "RDS database initialized!"
fi

# For EFS/H2, the database will initialize on first Lambda invocation
# But we can trigger it manually
if [ "$1" == "efs" ]; then
  echo "Triggering Lambda to initialize H2 database..."
  
  # Get the API endpoint
  API_ID=$(aws apigatewayv2 get-apis --query "Items[?Name=='chinook-api'].ApiId" --output text)
  ENDPOINT="https://$API_ID.execute-api.$(aws configure get region).amazonaws.com/prod"
  
  # Make a health check request to trigger initialization
  curl -X GET "$ENDPOINT/health"
  
  echo "H2 database initialized on EFS!"
fi