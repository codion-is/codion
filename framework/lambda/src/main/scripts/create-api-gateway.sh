#\!/bin/bash

# Create API Gateway for Lambda
API_ID=$(aws apigatewayv2 create-api \
  --name chinook-api \
  --protocol-type HTTP \
  --query 'ApiId' \
  --output text)

# Create Lambda integration
INTEGRATION_ID=$(aws apigatewayv2 create-integration \
  --api-id $API_ID \
  --integration-type AWS_PROXY \
  --integration-uri arn:aws:lambda:$(aws configure get region):$(aws sts get-caller-identity --query Account --output text):function:chinook-server \
  --payload-format-version 1.0 \
  --query 'IntegrationId' \
  --output text)

# Create routes
aws apigatewayv2 create-route \
  --api-id $API_ID \
  --route-key 'ANY /{proxy+}' \
  --target integrations/$INTEGRATION_ID

# Create deployment
aws apigatewayv2 create-deployment \
  --api-id $API_ID \
  --stage-name prod

# Grant API Gateway permission to invoke Lambda
aws lambda add-permission \
  --function-name chinook-server \
  --statement-id apigateway-invoke \
  --action lambda:InvokeFunction \
  --principal apigateway.amazonaws.com \
  --source-arn "arn:aws:execute-api:$(aws configure get region):$(aws sts get-caller-identity --query Account --output text):$API_ID/*/*/*" 2>/dev/null || true

echo "API Gateway created\!"
echo "Your API endpoint is:"
echo "https://$API_ID.execute-api.$(aws configure get region).amazonaws.com/prod/"
