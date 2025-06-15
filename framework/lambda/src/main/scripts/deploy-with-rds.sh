#!/bin/bash

# Deploy Lambda with RDS PostgreSQL

# 1. Create RDS instance (smallest possible for testing)
echo "Creating RDS PostgreSQL instance..."
aws rds create-db-instance \
  --db-instance-identifier chinook-db \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --engine-version 15.4 \
  --master-username chinook_admin \
  --master-user-password ChinookPass123! \
  --allocated-storage 20 \
  --no-publicly-accessible \
  --backup-retention-period 0 \
  --no-multi-az

echo "Waiting for RDS instance to be available (this takes 5-10 minutes)..."
aws rds wait db-instance-available --db-instance-identifier chinook-db

# Get the endpoint
ENDPOINT=$(aws rds describe-db-instances \
  --db-instance-identifier chinook-db \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text)

echo "RDS instance created at: $ENDPOINT"

# 2. Build Lambda package
echo "Building Lambda deployment package..."
cd ..
./gradlew :chinook-server-aws-lambda:shadowJar
cd deployment

# 3. Create Lambda function
echo "Creating Lambda function..."
aws lambda create-function \
  --function-name chinook-entity-server \
  --runtime java21 \
  --role arn:aws:iam::$(aws sts get-caller-identity --query Account --output text):role/chinook-lambda-role \
  --handler is.codion.demos.chinook.lambda.EntityProtocolHandler::handleRequest \
  --zip-file fileb://../build/libs/chinook-lambda.jar \
  --timeout 30 \
  --memory-size 1024 \
  --environment Variables="{\
    DATABASE_URL=jdbc:postgresql://$ENDPOINT:5432/postgres,\
    DATABASE_USER=chinook_admin,\
    DATABASE_PASSWORD=ChinookPass123!\
  }"

# 4. Create API Gateway
echo "Creating API Gateway..."
API_ID=$(aws apigatewayv2 create-api \
  --name chinook-api \
  --protocol-type HTTP \
  --query 'ApiId' \
  --output text)

# Create Lambda integration
INTEGRATION_ID=$(aws apigatewayv2 create-integration \
  --api-id $API_ID \
  --integration-type AWS_PROXY \
  --integration-uri arn:aws:lambda:$(aws configure get region):$(aws sts get-caller-identity --query Account --output text):function:chinook-entity-server \
  --payload-format-version 2.0 \
  --query 'IntegrationId' \
  --output text)

# Create routes
aws apigatewayv2 create-route \
  --api-id $API_ID \
  --route-key 'POST /entities/serial/{proxy+}' \
  --target integrations/$INTEGRATION_ID

aws apigatewayv2 create-route \
  --api-id $API_ID \
  --route-key 'GET /health' \
  --target integrations/$INTEGRATION_ID

# Create deployment
aws apigatewayv2 create-deployment \
  --api-id $API_ID \
  --stage-name prod

# Grant API Gateway permission to invoke Lambda
aws lambda add-permission \
  --function-name chinook-entity-server \
  --statement-id apigateway-invoke \
  --action lambda:InvokeFunction \
  --principal apigateway.amazonaws.com

echo "API Gateway created!"
echo "Your API endpoint is:"
echo "https://$API_ID.execute-api.$(aws configure get region).amazonaws.com/prod/"